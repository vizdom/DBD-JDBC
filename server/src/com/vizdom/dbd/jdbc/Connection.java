/*
 * Copyright 1999-2002,2004-2006 Vizdom Software, Inc. All Rights Reserved.
 * 
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the same terms as the Perl Kit, namely, under 
 *  the terms of either:
 *
 *      a) the GNU General Public License as published by the Free
 *      Software Foundation; either version 1 of the License, or 
 *      (at your option) any later version, or
 *
 *      b) the "Artistic License" that comes with the Perl Kit.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See either
 *  the GNU General Public License or the Artistic License for more 
 *  details. 
 */

package com.vizdom.dbd.jdbc;

import com.vizdom.ber.BerObject;
import com.vizdom.ber.BerIdentifier;
import com.vizdom.ber.BerTypes;
import com.vizdom.util.UnreachableCodeException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.*;
import java.util.Hashtable;
import java.util.Properties;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * This class implements a single DBD server connection. It's 
 * intended to be run as a single thread started by a
 * connection-accepting server.
 *
 * @version $Revision: 1.42 $
 */
/* The handleMessage methods may throw only SQLExceptions and 
 * RuntimeExceptions, the first if they can recover, the second
 * if the connection should exit.
 */
public class Connection implements Runnable
{
    /** The log4j logger. */
    private static final Logger gLog = LogManager.getLogger(Connection.class);

    /** The DBI constant for non-nullable columns. */
    private static final Integer sDbiNoNulls = new Integer(0);

    /** The DBI constant for nullable columns. */
    private static final Integer sDbiNullable = new Integer(1);   

    /** The DBI constant for nullable-unknown columns. */
    private static final Integer sDbiNullableUnknown = new Integer(2);

    /** LONG fields will be read in chunks this size. */
    private static final int sLONG_READ_BUFFER_SIZE = 8192;

    /** The current thread name, used in tracing messages. */
    private String mThreadId;

    /** The client socket. */
    private Socket mSocket;

    /**
     * The client socket's input stream. This is a BufferedInputStream 
     * in order to avoid a problem I had with <code>available</code> 
     * being > 0 but <code>read</code> returning -1.
     */
    private BufferedInputStream mIn;

    /** The client socket's output stream. */
    private BufferedOutputStream mOut;

    /** This connection's BerModule. */
    private BerDbdModule mBerModule;

    /** The JDBC Connection. */
    private java.sql.Connection mConn;

    /** 
     * True if this Connection was constructed with a pre-existing
     * JDBC connection.
     */
    private boolean preExistingConnection;

    /** A collection of the Statements currently in use. */
    private Hashtable<Integer, StatementHolder> mStatementTable
        = new Hashtable<Integer, StatementHolder>();

    /** 
     * The count of Statements created. This value is used as the
     * key into the statement table for each newly-created Statement.
     */
    /* Starts at 1 in case we want the statement handle to be true in perl. */
    private int mNextHandle;

    /** The user name. */
    String mUser;

    /** The password. */
    String mPassword;

    /** The JDBC URL. */
    String mUrl;

    /** Other connection properties. */
    Properties mProperties;
        
    /** Flag indicating whether generated keys are supported by the driver. */
    private boolean mSupportsGetGeneratedKeys; 

    /** The cached generated keys. */
    private GeneratedKey[] mGeneratedKeys; 

    /**
     * Constructor - initializes fields.
     *
     * @param aClient the client socket for this connection
     * @param aBerModule the BerModule this connection will use
     *      to read from/write to the socket
     * @exception IOException if an error occurs getting the socket's
     *      input or output streams
     */
    Connection(Socket aClient, BerDbdModule aBerModule) throws IOException
    {
        mSocket = aClient;

        mIn = new BufferedInputStream(mSocket.getInputStream());
        mOut = new BufferedOutputStream(mSocket.getOutputStream());
        mNextHandle = 1;
        mBerModule = aBerModule;
        preExistingConnection = false;
    }

    /**
     * Constructor. Uses a pre-existing JDBC connection.
     *
     * @param aClient the client socket for this connection
     * @param aBerModule the BerModule this connection will use
     *      to read from/write to the socket
     * @param aJdbcConnection a JDBC connection to be used
     *      by the client
     * @exception IOException if an error occurs getting the socket's
     *      input or output streams
     */
    Connection(Socket aClient, BerDbdModule aBerModule, 
        java.sql.Connection aJdbcConnection) throws IOException
    {
        this(aClient, aBerModule);
        mConn = aJdbcConnection;
        preExistingConnection = true;
    }

    /**
     * Accepts requests from client and dispatches them to the appropriate
     * method for handling. Sends response to client on request
     * completion.
     */
    public void run()
    {
        mThreadId = "[" + Thread.currentThread().getName() + "]";

        try (CloseableThreadContext.Instance ctc =
            CloseableThreadContext.push(mThreadId))
        {

            BerObject request;
            BerObject response = null;
            boolean connected = true;

            gLog.info("Client started");

            while (connected)
            {
                try
                {
                    /* Re-implement this, treating Connection as a Visitor
                     * on the fooRequest classes. For example,
                     * this switch would become request.handleMessage(this)
                     * and each fooRequest would have a 
                     * handleMessage(Connection conn) {conn.handleMessage(this)}
                     */
                    request = mBerModule.readFrom(mIn);
                    if (request == null)
                        throw new FatalException("Client disconnected");
                    if (gLog.isDebugEnabled())
                        gLog.debug("Request: " + request);

                    BerIdentifier id = request.getIdentifier(); 
                    if (id.getTagClass() != BerTypes.APPLICATION)
                        throw new FatalException("Unknown request received " + id);

                    int tagNumber = id.getTagNumber();
                    switch (tagNumber)
                    {
                    case BerDbdModule.gDISCONNECT_REQUEST:
                        connected = false;
                        response = handleRequest((DisconnectRequest) request); 
                        break;
                
                    case BerDbdModule.gCONNECT_REQUEST:
                        try
                        {
                            response = handleRequest((ConnectRequest) request);
                        }
                        catch (SQLException sql)
                        {
                            connected = false;
                            throw sql;
                        }
                        break;

                    case BerDbdModule.gPING_REQUEST:
                        response = handleRequest((PingRequest) request);
                        break;
                    case BerDbdModule.gCOMMIT_REQUEST: 
                        response = handleRequest((CommitRequest) request); 
                        break;
                    case BerDbdModule.gROLLBACK_REQUEST: 
                        response = handleRequest((RollbackRequest) request); 
                        break;
                    case BerDbdModule.gPREPARE_REQUEST: 
                        response = handleRequest((PrepareRequest) request); 
                        break;
                    case BerDbdModule.gEXECUTE_REQUEST: 
                        response = handleRequest((ExecuteRequest) request); 
                        break;
                    case BerDbdModule.gFETCH_REQUEST: 
                        response = handleRequest((FetchRequest) request); 
                        break;
                    case BerDbdModule.gGET_CONNECTION_PROPERTY_REQUEST: 
                        response = handleRequest(
                            (GetConnectionPropertyRequest) request); 
                        break;
                    case BerDbdModule.gSET_CONNECTION_PROPERTY_REQUEST: 
                        response = handleRequest(
                            (SetConnectionPropertyRequest) request); 
                        break;
                    case BerDbdModule.gGET_STATEMENT_PROPERTY_REQUEST: 
                        response = handleRequest(
                            (GetStatementPropertyRequest) request); 
                        break;
                    case BerDbdModule.gSET_STATEMENT_PROPERTY_REQUEST: 
                        response = handleRequest(
                            (SetStatementPropertyRequest) request); 
                        break;
                    case BerDbdModule.gSTATEMENT_FINISH_REQUEST: 
                        response = handleRequest(
                            (StatementFinishRequest) request); 
                        break;
                    case BerDbdModule.gSTATEMENT_DESTROY_REQUEST: 
                        response = handleRequest(
                            (StatementDestroyRequest) request); 
                        break;

                    case BerDbdModule.gCONNECTION_FUNC_REQUEST: 
                        response = handleRequest(
                            (ConnectionFuncRequest) request); 
                        break;

                    case BerDbdModule.gSTATEMENT_FUNC_REQUEST: 
                        response = handleRequest(
                            (StatementFuncRequest) request); 
                        break;

                    case BerDbdModule.gGET_GENERATED_KEYS_REQUEST:
                        response = handleRequest(
                            (GetGeneratedKeysRequest) request);
                        break;
                    
                    default: 
                        throw new DbdException(DbdException.gUNKNOWN_REQUEST,
                            new String[] { String.valueOf(tagNumber) });
                    }
                
                    if (response != null)
                    {
                        response.writeTo(mOut);
                        mOut.flush(); 
                        if (gLog.isDebugEnabled())
                            gLog.debug("Response: " + response);
                        response = null;
                    }
                    else
                        throw new DbdException(DbdException.gNO_RESPONSE);
                }
                catch (SQLException sqlError)
                {
                    gLog.warn("Error", sqlError);
                    try
                    {
                        mSendError(sqlError);
                    }
                    catch (FatalException fatal)
                    {
                        gLog.warn("Failed to send error", fatal);
                    }
                }
                catch (Throwable throwable)
                {
                    connected = false;
                    gLog.warn("Rollback due to fatal error"); 
                    mRollback();
                    gLog.fatal("Error; ending connection", throwable);
                    try
                    {
                        mSendError(new DbdException(
                                DbdException.gGENERIC_EXCEPTION,
                                new String[] { throwable.toString() }));
                    }
                    catch (FatalException fatal)
                    {
                        gLog.warn("Failed to send error", fatal);
                    }
                }
            }

            mDoDisconnect(true); 
            try { mOut.close(); } catch (IOException e) { }
            mOut = null; 
            try { mIn.close(); } catch (IOException e) { }
            mIn = null; 
            try { mSocket.close(); } catch (IOException e) { }
            mSocket = null;
        
            gLog.info("Client done");
        }
    }

    /**
     * Attempts to rollback any existing transaction without
     * triggering any errors (in case, for example, no
     * transaction is in process). Intended to be used when
     * ending the connection due to an error.
     */
    private void mRollback()
    {
        if (mConn == null)
            return;
        try
        {
            mConn.rollback();
        }
        catch (SQLException e)
        {
            gLog.warn("SQLException during rollback", e); 
        }
    }

    /**
     * Sets up the connection's character encoding, if the client 
     * requested a specific encoding. If this Connection was not
     * constructed with a pre-existing JDBC connection, 
     * establishes a JDBC connection.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if an error occurs establishing the
     *      JDBC connection
     */
     /* When we read the ConnectRequest, we don't yet know the client's 
      * character set. Therefore, the client is responsible for sending
      * the character set as an ASCII string. All other strings are
      * expected to be sent encoded in the client's character set.
      * We must explicitly pass the charset to the getXXX methods here,
      * since the charset wasn't set when the module decoded the
      * request.
      *
      * The rest of the code expects that the character encoding in the
      * BerModule is valid, based on its use here, and proceeds to
      * ignore UnsupportedEncodingExceptions.
      */
    BerObject handleRequest(ConnectRequest aRequest) 
        throws SQLException
    {
        // The property is always sent, but it might be "".
        String charset = null; 
        try
        {
            charset = aRequest.getCharacterEncoding();
            if (!charset.equals(""))
            {
                if (gLog.isDebugEnabled())
                    gLog.debug("Setting character encoding to " + charset);
                // Trigger UnsupportedEncodingException as soon as possible.
                com.vizdom.util.CharacterEncoder.toByteArray("test string", 
                    charset);
                mBerModule.setCharacterEncoding(charset);
            }
            

            if (!preExistingConnection)
            {
                mUrl =  aRequest.getURL(charset);
                mUser = aRequest.getUser(charset);
                mPassword = aRequest.getPassword(charset);
                mProperties = aRequest.getProperties(charset);
                if (gLog.isDebugEnabled())
                {
                    gLog.debug("url = " + mUrl);
                    gLog.debug("user = " + mUser);
                    java.util.Enumeration keys = mProperties.keys();
                    while (keys.hasMoreElements())
                    {
                        String key = (String) keys.nextElement();
                        gLog.debug(key + " = " + mProperties.get(key));
                    }
                }
                if (mUser == null && mPassword == null)
                    mConn = DriverManager.getConnection(mUrl, mProperties);
                else
                    mConn = DriverManager.getConnection(mUrl, mUser, mPassword);

                DatabaseMetaData dbmd = mConn.getMetaData(); 
                gLog.debug("Created database connection to " + 
                    dbmd.getDatabaseProductName() + " v" + 
                    dbmd.getDatabaseProductVersion()); 
                mSupportsGetGeneratedKeys = dbmd.supportsGetGeneratedKeys(); 
                if (gLog.isDebugEnabled())
                {
                    gLog.debug("Driver supports getGeneratedKeys? " + 
                        mSupportsGetGeneratedKeys);
                }
            }
        }
        catch (UnsupportedEncodingException unsupEnc)
        {
            throw new DbdException(DbdException.gUNSUPPORTED_ENCODING,
                new String[] { charset });
        }
        return new ConnectResponse();
    }

    /**
     * Closes and reopens the underlying JDBC connection using
     * the same connection properties. This is not currently
     * exposed via DBD::JDBC; it was part of an experiment with
     * exception listeners.
     *
     * @throws SQLException if a database error occurs
     */
    public void reconnect() throws SQLException
    {
        mDoDisconnect(true);
        if (mUser == null && mPassword == null)
            mConn = DriverManager.getConnection(mUrl, mProperties);
        else
            mConn = DriverManager.getConnection(mUrl, mUser, mPassword);
    }

    /**
     * Closes the current set of Statements and the JDBC connection.
     * Does nothing if this Connection was constructed with a 
     * pre-existing JDBC connection.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if an error occurs while closing the
     *      Connection
     */
    /* StatementHolder.close doesn't throw exceptions, so we should always
     * get the chance to close the connection. 
     */
    BerObject handleRequest(DisconnectRequest aRequest)
        throws SQLException
    {
        mDoDisconnect(!preExistingConnection); 
        return new DisconnectResponse();
    }
    
    /**
     * Closes statements and the JDBC connection.
     *
     * @param shouldCloseConnection true if the connection should
     * be closed. If a pre-existing connection was passed in, the
     * connection should not be closed.
     */
    private void mDoDisconnect(boolean shouldCloseConnection)
    {
        if (mStatementTable.size() > 0)
        {
            java.util.Enumeration elements = mStatementTable.elements();
            while (elements.hasMoreElements())
                ((StatementHolder) elements.nextElement()).close();
            if (gLog.isDebugEnabled())
            {
                gLog.debug("Closed " + mStatementTable.size() + 
                    " Statements on disconnect");
            }
        }
        // Don't empty the handle cache. Leave older statement
        // handles around for destroy requests in case we're in a
        // reconnect situation. Destroy shouldn't throw
        // exceptions back to the Perl client even if the handle
        // has already been closed.  

        // mStatementTable.clear();
        if (mConn != null && shouldCloseConnection)
        {
            try
            { 
                gLog.debug("Closing database connection"); 
                mConn.close();
            } 
            catch (Throwable t)
            {
                gLog.warn("Error closing connection: " + t.getMessage());
            }
        }
        mConn = null; 
    }

    /**
     * Checks to see whether the JDBC Connection is still open.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if the Connection is closed
     */
    BerObject handleRequest(PingRequest aRequest)
        throws SQLException
    {
        return new PingResponse(mConn.isClosed() ? 0 : 1);
    }

    /**
     * Calls <code>Connection.commit</code>.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if the <code>commit</code> fails
     */
    BerObject handleRequest(CommitRequest aRequest)
        throws SQLException
    {
        mConn.commit();
        return new CommitResponse();
    }
    
    /**
     * Calls <code>Connection.rollback</code>. 
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if the <code>rollback</code> fails
     */
    BerObject handleRequest(RollbackRequest aRequest)
        throws SQLException
    {
        mConn.rollback();
        return new RollbackResponse();
    }
    
    /**
     * Prepares a statement for later execution.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if the statement preparation fails
     */
    /* We use PreparedStatements here since we don't know if there are
     * any substitutable parameters. 
     */
    BerObject handleRequest(PrepareRequest aRequest)
        throws SQLException
    {
        PreparedStatement stmt = null;
        if (mSupportsGetGeneratedKeys)
        {
            if ("name".equals(aRequest.getKeyType()))
            {
                stmt = mConn.prepareStatement(aRequest.getStatement(), 
                    aRequest.getColumnNames()); 
            }
            else if ("index".equals(aRequest.getKeyType()))
            {
                stmt = mConn.prepareStatement(aRequest.getStatement(),
                    aRequest.getColumnIndexes()); 
            }
            else
            {
                stmt = mConn.prepareStatement(aRequest.getStatement(),
                    Statement.RETURN_GENERATED_KEYS);
            }
        }
        else
            stmt = mConn.prepareStatement(aRequest.getStatement());
        int stmtHandle = mNextHandle++;
        mStatementTable.put(new Integer(stmtHandle), 
            new StatementHolder(stmt));
        if (gLog.isTraceEnabled())
            gLog.trace("Assigned statement handle " + stmtHandle);
        return new PrepareResponse(stmtHandle);
    }
    
    /**
     * Sets statement parameters and executes a previously prepared 
     * statement. This method will try to convert the bytes sent to 
     * the type corresponding to the provided type hint and call the 
     * appropriate setXXX method. Type conversions are taken from
     * Table 21.2, p. 394, in JDBC Data Access with Java.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if an error occurs when setting a parameter
     *      with setXXX or executing the statement
     */
    /* The request will contain the statement handle for the statement
     * to be executed and a list of parameters and parameter type hints.
     * This method will throw an exception if one of the setXXX methods 
     * fails (or if a data conversion fails). We're playing some games
     * to make sure that the parameter number is included in the 
     * error message, since we're setting all the parameters at once
     * and the user might not know otherwise which one failed.
     */
    BerObject handleRequest(ExecuteRequest aRequest)
        throws SQLException
    {
        if (gLog.isTraceEnabled())
            gLog.trace("Executing statement handle " + aRequest.getHandle());
        StatementHolder holder = mGetStatementHolder(aRequest.getHandle());
        PreparedStatement stmt = holder.getStatement();
        Parameter[] params = aRequest.getParameters();
        if (gLog.isDebugEnabled())
            gLog.debug("setting " + params.length + " parameters");
        for (int i = 0; i < params.length; i++)
        {
            try 
            {
                if (params[i].value == null)
                {
                    if (gLog.isTraceEnabled())
                    {
                        gLog.trace("setting parameter " + (i + 1) + 
                            "; value null; type " + params[i].type);
                    }
                    stmt.setNull(i + 1, params[i].type);
                    continue;
                }
                if (gLog.isTraceEnabled())
                    gLog.trace("setting parameter " + (i + 1) + "; value ");
                switch (params[i].type) 
                {
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    stmt.setBytes(i + 1, params[i].value.toByteArray());
                    break;
                case Types.TINYINT:
                case Types.SMALLINT:
                    stmt.setShort(i + 1, 
                        Short.parseShort(params[i].value.toString()));
                    break;
                case Types.INTEGER:
                    stmt.setInt(i + 1, 
                        Integer.parseInt(params[i].value.toString()));
                    break;
                case Types.BIGINT: 
                    stmt.setLong(i + 1, 
                        Long.parseLong(params[i].value.toString()));
                    break;
                case Types.REAL: 
                    stmt.setFloat(i + 1, 
                        new Float(params[i].value.toString()).floatValue());
                    break;
                case Types.FLOAT: 
                case Types.DOUBLE: 
                    stmt.setDouble(i + 1, 
                        new Double(params[i].value.toString()).doubleValue());
                    break;
                case Types.DECIMAL: 
                case Types.NUMERIC:
                    stmt.setBigDecimal(i + 1, 
                        new BigDecimal(params[i].value.toString()));
                    break;
                case Types.BIT:   // Clients must send "0" or "1"
                    stmt.setBoolean(i + 1, 
                        params[i].value.toString().equals("1"));
                    break;
                case Types.CHAR: 
                case Types.VARCHAR:
                case Types.LONGVARCHAR:  // Use a stream here?
                    stmt.setString(i + 1, params[i].value.toString());
                    break;
                case Types.DATE:      
                case Types.TIME:      
                case Types.TIMESTAMP: 
                    // TODO: Documentation in the Changes file for DBI
                    // 1.41 indicates that if a type was
                    // explicitly supplied, we can assume the
                    // data is in the standard form for that
                    // type. For example, dates should be
                    // yyyy-MM-dd. This would let us create a
                    // Date and call setDate here.

                case Types.OTHER: 
                default: 
                    stmt.setString(i + 1, params[i].value.toString());
                    break;
                }
                if (params[i].type == Types.BINARY ||
                    params[i].type == Types.VARBINARY ||
                    params[i].type == Types.LONGVARBINARY)
                {
                    if (gLog.isTraceEnabled())
                    {
                        gLog.trace("(binary; length " + 
                            params[i].value.toByteArray().length + 
                            "); type " + params[i].type);
                    }
                }
                else
                {
                    if (gLog.isTraceEnabled())
                    {
                        gLog.trace(params[i].value.toString() + "; type " + 
                            params[i].type);
                    }
                }
            }
            catch (NumberFormatException ne)
            {
                throw new DbdException(DbdException.gSET_PARAMETER,
                    new String[] { String.valueOf(i + 1), ne.toString() });
            }
            catch (SQLException se)
            {
                DbdException dbd = new DbdException(
                    DbdException.gSET_PARAMETER,
                    new String[] { String.valueOf(i + 1), se.toString() });
                dbd.setNextException(se);
                throw dbd;
            }            
        }
        
        ExecuteResponse resp;
        if (stmt.execute())
        {
            // execute returned a result set.
            gLog.debug("Getting and returning a result set");
            ResultSet rs = stmt.getResultSet();
            holder.setResultSet(rs);
            ResultSetMetaData rsmd = holder.getResultSetMetaData();
            int cols = rsmd.getColumnCount();
            resp = 
                new ExecuteResponse(
                new ExecuteResultSetResponse(cols));
        }
        else
        {
            // execute returned a row count.
            gLog.debug("Getting and returning a row count");
            resp = new ExecuteResponse(new ExecuteRowsResponse(
                stmt.getUpdateCount()));
            if (mSupportsGetGeneratedKeys)
            {
                ResultSet rs = stmt.getGeneratedKeys();
                ResultSetMetaData rsmd = rs.getMetaData(); 
                if (rsmd.getColumnCount() > 0)
                {
                    // We might not be able to jump to the last
                    // row, so we just have to read each row,
                    // letting the last row be the last value
                    // cached.
                    mGeneratedKeys = new GeneratedKey[rsmd.getColumnCount()]; 
                    while (rs.next())
                    {
                        for (int i = 1; i <= rsmd.getColumnCount(); i++)
                        {
                            mGeneratedKeys[i - 1] = new GeneratedKey(
                                rsmd.getCatalogName(i),
                                rsmd.getSchemaName(i),
                                rsmd.getTableName(i),
                                rsmd.getColumnName(i),
                                rs.getString(i)); 
                            if (gLog.isTraceEnabled())
                                gLog.trace("Key: " + mGeneratedKeys[i - 1]); 
                        }
                    }
                }
            }
        }
        return resp;
    }

    /**
     * Fetches the next row of data from the ResultSet associated
     * with a given Statement. Implements the DBI specification 
     * with regard to LongReadLen, LongTruncOk, and ChopBlanks.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if <code>next</code> or <code>getXXX</code>
     *      fail, or if long data is truncated
     * @exception Exception if the statement has no result set, or the
     *      provided statement handle is invalid
     */
     /* Code here relies on prepare (or some other earlier method) to 
      * set the LongReadLen, LongTruncOk, and ChopBlanks properties.
      *
      * Some data types get special handling, but mostly we pass 
      * everything back as a string and let the client sort it out.
      */
    BerObject handleRequest(FetchRequest aRequest)
        throws SQLException
    {
        if (gLog.isTraceEnabled())
        {
            gLog.trace("Fetching row from statement handle " + 
                aRequest.getHandle());
        }
        StatementHolder holder = mGetStatementHolder(aRequest.getHandle());
        ResultSet rs = holder.getResultSet();
        if (rs == null)
            throw new DbdException(DbdException.gNO_RESULT_SET);
        
        Object[] row = null;
        boolean hasData;
        if (hasData = rs.next())
        {
            ResultSetMetaData rsmd = holder.getResultSetMetaData();
            int cols = rsmd.getColumnCount();
            row = new Object[cols];
            int longReadLen = ((Integer) 
                holder.getProperties().get("LongReadLen")).intValue();
            boolean longTruncOk = ((Boolean) 
                holder.getProperties().get("LongTruncOk")).booleanValue();
            boolean chopBlanks = ((Boolean) 
                holder.getProperties().get("ChopBlanks")).booleanValue();
            boolean readAll = ((Boolean) 
                holder.getProperties().get("jdbc_longreadall")).booleanValue();
            for (int i = 0; i < cols; i++)
            {
                try
                {
                    int type = rsmd.getColumnType(i + 1);
                    if (gLog.isTraceEnabled())
                    {
                        gLog.trace("getting column " + (i + 1) + "/" + 
                            rsmd.getColumnName(i + 1) + "; type " + type);
                    }
                    switch (type)
                    {
                    case Types.BINARY: 
                    case Types.VARBINARY: 
                        row[i] = rs.getBytes(i + 1);
                        break;

                    case Types.LONGVARBINARY: 
                        if (longReadLen == 0)
                            row[i] = null;
                        else
                        {
                            row[i] = mReadLong(i + 1, 
                                rs.getBinaryStream(i + 1),
                                longReadLen, longTruncOk, readAll);
                        }
                        break;

                    case Types.BLOB: 
                        if (longReadLen == 0)
                            row[i] = null;
                        else
                        {
                            Blob blob = rs.getBlob(i + 1); 
                            if (blob == null)
                                row[i] = null;
                            else
                            {
                                row[i] = mReadLong(i + 1, 
                                    blob.getBinaryStream(), longReadLen, 
                                    longTruncOk, readAll);
                            }
                        }
                        break;

                        // The JDBC spec says to prefer
                        // getCharacterStream for
                        // LONGVARCHAR. However, getString is
                        // also supported. We could use getString
                        // here instead, or create a parameter
                        // which lets a caller specify that
                        // getString should be used.
                    case Types.LONGVARCHAR:
                        if (longReadLen == 0)
                            row[i] = null;
                        else
                        {
                            char[] chars = mReadLong(i + 1, 
                                rs.getCharacterStream(i + 1), longReadLen, 
                                longTruncOk, readAll);
                            row[i] = (chars == null) ? 
                                null : new String(chars); 
                        }
                        break;

                    case Types.CLOB: 
                        if (longReadLen == 0)
                            row[i] = null;
                        else
                        {
                            Clob clob = rs.getClob(i + 1);
                            if (clob == null)
                                row[i] = null;
                            else
                            {
                                char[] chars = mReadLong(i + 1, 
                                    clob.getCharacterStream(), longReadLen, 
                                    longTruncOk, readAll);
                                row[i] = (chars == null) ? 
                                    null : new String(chars);
                            }
                        }
                        break;

                        // The driver I mostly use returns arrays
                        // in a convenient string format. This
                        // may be updated later to get an Array
                        // object, build a string, and allow
                        // callers to specify a term separator.
                    case Types.ARRAY: 
                        row[i] = rs.getString(i + 1);
                        break;

                    case Types.CHAR: 
                        if (chopBlanks)
                        {
                            row[i] = mChopBlanks(rs.getString(i + 1));
                            break;
                        }
                        // Fall through when chopBlanks is not set.

                    default:  // This will include any Types.OTHER columns.
                        row[i] = rs.getString(i + 1);
                        break;
                    }
                }
                catch (IOException ioError)
                {
                    throw new DbdException(DbdException.gFETCH_EXCEPTION,
                        new String[] { String.valueOf(i + 1), 
                                       ioError.toString() });
                }
            }
        }
        try
        {
            return new FetchResponse(hasData, row, 
                mBerModule.getCharacterEncoding());
        }
        catch (UnsupportedEncodingException unsupEnc)
        {
            throw new UnreachableCodeException();
        }
    }
    


    /**
     * Returns the value of a connection property. 
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception Exception if the requested connection property name
     *      is not recognized
     */
    BerObject handleRequest(GetConnectionPropertyRequest aRequest)
        throws SQLException
    {
        String property = aRequest.getPropertyName();
        if (property.equals("AutoCommit"))
        {
            Integer[] response = new Integer[1];
            response[0] = new Integer(mConn.getAutoCommit() ? 1 : 0);
            try
            {
                return new GetConnectionPropertyResponse(response, 
                    mBerModule.getCharacterEncoding());
            }
            catch (UnsupportedEncodingException unsupEnc)
            {
                throw new UnreachableCodeException();
            }
        }            
        throw new DbdException(DbdException.gUNKNOWN_PROPERTY, 
            new String[] { property });
    }

    /**
     * Sets a connection property value.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception Exception if the connection property name
     *      is not recognized
     */
    BerObject handleRequest(SetConnectionPropertyRequest aRequest)
        throws SQLException
    {
        String property = aRequest.getPropertyName();
        if (property.equals("AutoCommit"))
        {
            boolean autoCommit = aRequest.getPropertyValue().equals("1"); 
            gLog.debug("Settting AutoCommit to " + autoCommit); 
            mConn.setAutoCommit(autoCommit);
            return new SetConnectionPropertyResponse();
        }
        throw new DbdException(DbdException.gUNKNOWN_PROPERTY, 
            new String[] { property });
    }

    /**
     * Gets a statement property value.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception SQLException if a database access error occurs
     * @exception Exception if the property name is unknown, or
     *      if the statement handle is invalid or has no ResultSet
     *      associated with it
     */
    BerObject handleRequest(GetStatementPropertyRequest aRequest)
        throws SQLException
    {
        try
        {
            StatementHolder holder = mGetStatementHolder(aRequest.getHandle());
            String property = aRequest.getPropertyName();
            if (property.equals("CursorName")) 
            {
                ResultSet rs = holder.getResultSet();
                if (rs == null)
                    throw new DbdException(DbdException.gNO_CURSOR);
                String[] cursorname = new String[1];
                // DBI specifies that the cursor name should be
                // returned as undef if cursor names are not
                // supported. JDBC specifies that getCursorName
                // should throw an exception if named cursors are
                // not supported.
                try
                {
                    cursorname[0] = rs.getCursorName();
                }
                catch (SQLException e)
                {
                    cursorname[0] = null;
                    gLog.warn("getCursorName threw an exception", e);
                }
                return new GetStatementPropertyResponse(cursorname,
                    mBerModule.getCharacterEncoding());
            }
            
            ResultSetMetaData rsmd = holder.getResultSetMetaData();
            if (rsmd == null)
                throw new DbdException(DbdException.gNO_METADATA);
            int colcount = rsmd.getColumnCount();
            
            if (property.equals("NAME"))
            {
                String[] data = new String[colcount];
                for (int i = 1; i <= colcount; i++)
                    data[i - 1] = rsmd.getColumnName(i);
                return new GetStatementPropertyResponse(data, 
                    mBerModule.getCharacterEncoding());
            }
            else if (property.equals("TYPE"))
            {
                Integer[] data = new Integer[colcount];
                for (int i = 1; i <= colcount; i++)
                    data[i - 1] = new Integer(rsmd.getColumnType(i));
                return new GetStatementPropertyResponse(data,
                    mBerModule.getCharacterEncoding());
            }
            else if (property.equals("PRECISION"))
            {
                Integer[] data = new Integer[colcount];
                for (int i = 1; i <= colcount; i++)
                    data[i - 1] = new Integer(rsmd.getPrecision(i));
                return new GetStatementPropertyResponse(data,
                    mBerModule.getCharacterEncoding());
            }
            else if (property.equals("SCALE"))
            {
                Integer[] data = new Integer[colcount];
                for (int i = 1; i <= colcount; i++)
                {
                    // Scale might reasonably be unsupported on some columns.
                    try
                    {
                        data[i - 1] = new Integer(rsmd.getScale(i));
                    }
                    catch (SQLException e)
                    {
                        data[i - 1] = null;
                    }
                }
                return new GetStatementPropertyResponse(data,
                    mBerModule.getCharacterEncoding());
            }
            else if (property.equals("NULLABLE"))
            {
                Integer[] data = new Integer[colcount];
                for (int i = 1; i <= colcount; i++)
                {
                    int nullable = rsmd.isNullable(i);
                    switch (nullable)
                    {
                    case ResultSetMetaData.columnNoNulls:
                        data[i - 1] = sDbiNoNulls;
                        break;
                    case ResultSetMetaData.columnNullable:
                        data[i - 1] = sDbiNullable;
                        break;
                    case ResultSetMetaData.columnNullableUnknown:
                        data[i - 1] = sDbiNullableUnknown;
                        break;
                    default:
                        data[i - 1] = null;
                        gLog.warn("isNullable returned an unknown value " + 
                            nullable);
                        break;
                    }
                }
                return new GetStatementPropertyResponse(data,
                    mBerModule.getCharacterEncoding());
            }
            throw new DbdException(DbdException.gUNKNOWN_PROPERTY, 
                new String[] { property });
            
        }
        catch (UnsupportedEncodingException unsupEnc)
        {       
            throw new UnreachableCodeException();
        }
    }


    /**
     * Sets a statement property value.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception Exception if the statement handle is invalid or
     *      the property name is not recognized
     */
    BerObject handleRequest(SetStatementPropertyRequest aRequest)
        throws SQLException
    {
        StatementHolder holder = mGetStatementHolder(aRequest.getHandle());
        String property = aRequest.getPropertyName();

        if (property.equals("LongReadLen"))
        {
            holder.getProperties().put(property, 
                new Integer(aRequest.getPropertyValue()));
            return new SetStatementPropertyResponse();
        }
        if (property.equals("LongTruncOk"))
        {
            holder.getProperties().put(property, 
                new Boolean(aRequest.getPropertyValue().equals("1")));
            return new SetStatementPropertyResponse();
        }
        if (property.equals("ChopBlanks"))
        {
            holder.getProperties().put(property, 
                new Boolean(aRequest.getPropertyValue().equals("1")));
            return new SetStatementPropertyResponse();
        }
        if (property.equals("jdbc_longreadall"))
        {
            holder.getProperties().put(property, 
                new Boolean(aRequest.getPropertyValue().equals("1")));
            return new SetStatementPropertyResponse();
        }

        throw new DbdException(DbdException.gUNKNOWN_PROPERTY, 
            new String[] { property });
    }    
    

    /**
     * Finishes a statement. A statement is finished when all its data
     * has been read, so this method closes the result set. 
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception Exception if the statement handle is invalid
     */
    /* Accept this message, but don't do anything. DBI forbids finish
     * to have any effect on the session's transaction state, and we
     * can't guarantee that if we close the ResultSet, which is the 
     * obvious implementation here.
     *
     * JDBC 2.0 also has implications here, since reading all a 
     * result set's data doesn't necessarily mean you're done with it.
     */   
    BerObject handleRequest(StatementFinishRequest aRequest)
        throws SQLException
    {
        StatementHolder holder = mGetStatementHolder(aRequest.getHandle());
        //holder.finish();
        return new StatementFinishResponse();
    }    
    
    /**
     * Frees a statement for garbage collection.
     *
     * @param aRequest the request received from the client
     * @return a BER response object
     * @exception Exception if the statement handle is invalid
     */
    /* This should be called by $sth->DESTROY: the statement has gone
     * out of scope and is no longer usable, so remove it from the
     * cache.
     */
    BerObject handleRequest(StatementDestroyRequest aRequest)
        throws DbdException
    {
        // Note: we want to call mStatementTable.remove, so don't use
        // mGetStatementHolder.
        int handle = aRequest.getHandle();
        StatementHolder holder = (StatementHolder) mStatementTable.remove(
            new Integer(handle));
        // XXX: Should this be a runtime exception? Can the user cause this?
        if (holder == null)
            throw new DbdException(DbdException.gINVALID_STATEMENT_HANDLE);
        else
        {
            if (gLog.isTraceEnabled())
                gLog.trace("Destroying statement " + handle);
            
            // Close the statement and result set. This may cause
            // a commit or rollback in the JDBC driver in
            // AutoCommit mode, but not closing these objects
            // seems to be worse.
            holder.close();
            holder = null;
        }
        return new StatementDestroyResponse();
    }    



    /**
     * Uses reflection to call a method on the current Connection object.
     *
     * @param aRequest contains the method name and parameter values
     * @return a response packet containing the return value from the 
     *      called method
     * @exception SQLException if the invoked method throws a SQLException
     * @exception DbdException if an error occurs
     */
    BerObject handleRequest(ConnectionFuncRequest aRequest)
        throws SQLException,DbdException
    {
        String methodName = aRequest.getMethodName();
        if (gLog.isTraceEnabled())
            gLog.trace("Func method: " + methodName);
        String value =  mHandleFunc(mConn, aRequest.getMethodName(), 
            aRequest.getParameters());
        if (gLog.isDebugEnabled())
            gLog.debug(methodName + " returned '" + value + "'");
        try
        {
            return new ConnectionFuncResponse(value, 
                mBerModule.getCharacterEncoding());
        }
        catch (UnsupportedEncodingException unsupEnc)
        { 
            throw new DbdException(DbdException.gUNSUPPORTED_ENCODING,
                new String[] { mBerModule.getCharacterEncoding() });
        }
    }

    /**
     * Uses reflection to call a method on a Statement,
     * ResultSet, or ResultSetMetaData object. The particular
     * statement is indicated by a statement handle in the
     * request. The object is indicated by a
     * <code>Statement.</code>, <code>ResultSet.</code>, or
     * <code>ResultSetMetaData.</code> prefix on the method name.
     *
     * @param aRequest contains a statement handle, 
     *      the method name and parameter values
     * @return a response packet containing the return value from the 
     *      called method 
     * @exception SQLException if the invoked method throws a SQLException
     * @exception DbdException if an error occurs
     */
    BerObject handleRequest(StatementFuncRequest aRequest)
        throws SQLException,DbdException
    {
        String methodName = aRequest.getMethodName();
        if (gLog.isTraceEnabled())
            gLog.trace("Statement func method: " +  methodName);

        int dot = methodName.lastIndexOf(".");
        if (dot == -1)
            throw new DbdException(DbdException.gREFLECTION_OBJECT_MISSING);

        StatementHolder holder = mGetStatementHolder(aRequest.getHandle());
        if (holder == null)
            throw new DbdException(DbdException.gINVALID_STATEMENT_HANDLE);
            
        String objectName = methodName.substring(0, dot);
        if (gLog.isTraceEnabled())
            gLog.trace("Looking up object type " + objectName);
        Object object = null;
        if (objectName.equals("Statement") || 
            objectName.equals("PreparedStatement"))
        {
            object = holder.getStatement();
        }
        else if (objectName.equals("ResultSet"))
        {
            object = holder.getResultSet();
            if (object == null)
                throw new DbdException(DbdException.gNO_RESULT_SET);
        }
        else if (objectName.equals("ResultSetMetaData"))
        {
            object = holder.getResultSetMetaData();
            if (object == null)
                throw new DbdException(DbdException.gNO_METADATA);
        }
        else
        {
            throw new DbdException(DbdException.gREFLECTION_INVALID_OBJECT,
                new String[] { objectName } );
        }

        if (gLog.isDebugEnabled())
        {
            gLog.debug("Underlying object type: " + 
                object.getClass().getName());
        }

        String value = mHandleFunc(object, methodName.substring(dot + 1),
            aRequest.getParameters());
        if (gLog.isDebugEnabled())
            gLog.debug(methodName + " returned '" + value + "'");
        try
        {
            return new StatementFuncResponse(value, 
                mBerModule.getCharacterEncoding());
        }
        catch (UnsupportedEncodingException unsupEnc)
        { 
            throw new DbdException(DbdException.gUNSUPPORTED_ENCODING,
                new String[] { mBerModule.getCharacterEncoding() });
        }
    }


    /**
     * Returns the cached key that best matches the request.
     *
     * @param aRequest may contain a table and column name to use
     * in key lookup
     * @return a response packet containing a key; may be the empty string
     * @exception DbdException if an error occurs
     */
    /* We're ignoring the catalog and schema values. */
    BerObject handleRequest(GetGeneratedKeysRequest aRequest) 
        throws DbdException
    {
        try
        {
            String table = aRequest.getTable();
            String column = aRequest.getColumn(); 
            String key = ""; 
            if (mGeneratedKeys == null || mGeneratedKeys.length == 0)
            {                
                if (gLog.isDebugEnabled())
                    gLog.debug("Generated key requested, but no keys are available"); 
            }
            else if (table == null && column == null)
            {
                // No particular table or column requested; return the first key. 
                key = mGeneratedKeys[0].value; 
                if (gLog.isTraceEnabled())
                    gLog.trace("No specific key column or table requested"); 
            }
            else if (table == null && column != null)
            {
                for (int i = 0; i < mGeneratedKeys.length; i++)
                {
                    if (column.equalsIgnoreCase(mGeneratedKeys[i].columnName))
                    {
                        key = mGeneratedKeys[i].value;
                        break;
                    }
                }
                if (gLog.isTraceEnabled())
                    gLog.trace("Key column '" + column + "' requested"); 
            }
            else if (table != null && column == null)
            {
                for (int i = 0; i < mGeneratedKeys.length; i++)
                {
                    if (table.equalsIgnoreCase(mGeneratedKeys[i].table))
                    {
                        key = mGeneratedKeys[i].value;
                        break;
                    }
                }
                if (gLog.isTraceEnabled())
                    gLog.trace("Key table '" + table + "' requested"); 
            }
            else if (table != null && column != null)
            {
                for (int i = 0; i < mGeneratedKeys.length; i++)
                {
                    if (column.equalsIgnoreCase(mGeneratedKeys[i].columnName) &&
                        table.equalsIgnoreCase(mGeneratedKeys[i].table))
                    {
                        key = mGeneratedKeys[i].value;
                        break;
                    }
                }
                if (gLog.isTraceEnabled())
                {
                    gLog.trace("Key table and column '" + table + "'.'" + 
                        column + "' requested"); 
                }
            }
            return new GetGeneratedKeysResponse(key,
                mBerModule.getCharacterEncoding()); 
        }
        catch (UnsupportedEncodingException unsupEnc)
        { 
            throw new DbdException(DbdException.gUNSUPPORTED_ENCODING,
                new String[] { mBerModule.getCharacterEncoding() });
        }
    }

    /**
     * Uses reflection to call the given method on the given
     * object with the given parameter list.
     *
     * @param anObject the object on which to call the method
     * @param aMethodName the method to be called
     * @param aParameterList a list of parameters to be passed to the method;
     *      may not be null, but may have length 0
     * @return the value of calling <code>toString</code> on the
     *      Object returned from the method call
     * @exception SQLException if the invoked method throws a SQLException
     * @exception DbdException if an error occurs 
     */
    private String mHandleFunc(Object anObject, String aMethodName,  
        Parameter[] aParameterList) throws SQLException,DbdException  
    { 
        if (gLog.isDebugEnabled())
            gLog.debug("Method has " + aParameterList.length + " parameters"); 
        Object[] parameterObjects = new Object[aParameterList.length];
        Class[] parameterClasses = new Class[aParameterList.length];
        try
        {
            for (int i = 0; i < aParameterList.length; i++)
            {
                if (aParameterList[i].value == null)
                {
                    parameterObjects[i] = null;
                    parameterClasses[i] = mGetClass(aParameterList[i].type);
                    if (gLog.isTraceEnabled())
                    {
                        gLog.trace("Parameter " + (i + 1) + ": null; class " + 
                            parameterClasses[i].getName());
                    }
                    continue;
                }

                if (gLog.isTraceEnabled())                  
                    gLog.trace("Parameter " + (i + 1) + ": ");
                switch (aParameterList[i].type) 
                {
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    parameterObjects[i] = 
                        aParameterList[i].value.toByteArray();
                    break;
                case Types.TINYINT:
                    parameterObjects[i] = 
                        new Byte(aParameterList[i].value.toString());
                    break;
                case Types.SMALLINT:
                    parameterObjects[i] = 
                        new Short(aParameterList[i].value.toString());
                    break;
                case Types.INTEGER:
                    parameterObjects[i] = 
                        new Integer(aParameterList[i].value.toString());
                    break;
                case Types.BIGINT: 
                    parameterObjects[i] = 
                        new Long(aParameterList[i].value.toString());
                    break;
                case Types.REAL: 
                    parameterObjects[i] = 
                        new Float(aParameterList[i].value.toString());
                    break;
                case Types.FLOAT: 
                case Types.DOUBLE: 
                    parameterObjects[i] = 
                        new Double(aParameterList[i].value.toString());
                    break;
                case Types.DECIMAL: 
                case Types.NUMERIC:
                    parameterObjects[i] = 
                        new BigDecimal(aParameterList[i].value.toString());
                    break;
                case Types.BIT:   // Clients must send "0" or "1"
                    parameterObjects[i] = new Boolean(
                        aParameterList[i].value.toString().equals("1"));
                    break;
                case Types.CHAR: 
                case Types.VARCHAR:
                case Types.LONGVARCHAR:  // Use a stream here?
                    parameterObjects[i] = aParameterList[i].value.toString();
                    break;
                case Types.DATE:      
                    parameterObjects[i] = 
                        java.sql.Date.valueOf(aParameterList[i].value.toString());
                    break;
                case Types.TIME: 
                    parameterObjects[i] = 
                        java.sql.Time.valueOf(aParameterList[i].value.toString());
                    break;
                case Types.TIMESTAMP: 
                    parameterObjects[i] = 
                        java.sql.Timestamp.valueOf(aParameterList[i].value.toString());
                    break;
                case Types.OTHER: 
                default: 
                    parameterObjects[i] = aParameterList[i].value.toString();
                    break;
                }

                parameterClasses[i] = mGetClass(aParameterList[i].type);

                if (aParameterList[i].type == Types.BINARY ||
                    aParameterList[i].type == Types.VARBINARY ||
                    aParameterList[i].type == Types.LONGVARBINARY)
                {
                    if (gLog.isTraceEnabled())
                    {
                        gLog.trace("byte[], length " + 
                            ((byte[]) parameterObjects[i]).length +
                            "; class " + parameterClasses[i].getName());
                    }
                }
                else
                {
                    if (gLog.isTraceEnabled())
                    {
                        gLog.trace(parameterObjects[i].toString() + 
                            "; class " + parameterClasses[i].getName());
                    }
                }
            }
            
            try 
            {
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6342411
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4283544
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4071957

                // Class.getMethod claims to get the public
                // method requested from the class or a
                // superclass or an interface. There's a bug
                // which prevents this from working in some
                // cases. One such case can be where a JDBC
                // implementation class implements a JDBC
                // interface, say, ResultSet, but the
                // implementation class is not public. Using
                // reflection to get e.g. the "close" method on
                // the implementation class will fail. You have
                // to get the ResultSet interface first. It's an
                // IllegalAccessException problem, not a
                // NoSuchMethodException problem.

                if (aParameterList.length == 0)
                    parameterClasses = null;
                Object returnValue = mInvokeMethod(anObject,
                    aMethodName, parameterClasses, parameterObjects); 

                // Null should be turned into BerNull. Boolean is
                // turned into 1 or 0 so that the return value
                // will be appropriately true or false in Perl.
                if (returnValue == null)
                    return null;
                else if (returnValue instanceof Boolean)
                    return ((Boolean) returnValue).booleanValue() ? "1" : "0";
                else
                    return returnValue.toString();
            }
            catch (SecurityException security)
            {
                throw new DbdException(DbdException.gREFLECTION_EXCEPTION,
                    new String[] { security.toString() });
            }
            catch (IllegalArgumentException argument)
            {
                throw new DbdException(DbdException.gREFLECTION_EXCEPTION,
                    new String[] { argument.toString() });
            }
            catch (InvocationTargetException target)
            {
                Throwable t = target.getTargetException();
                if (t != null)
                {
                    if (t instanceof SQLException)
                        throw (SQLException) t;
                    throw new DbdException(DbdException.gREFLECTION_EXCEPTION,
                        new String[] { t.toString() });
                }
                throw new DbdException(DbdException.gREFLECTION_EXCEPTION,
                    new String[] { target.toString() });
            }
        }
        catch (Exception e) 
        { 
            gLog.debug(e, e); 
            throw new DbdException(DbdException.gGENERIC_EXCEPTION,
                new String[] { e.toString() });
        }            
    }

    /**
     * Attempts to invoke a method on the given object.
     *
     * @param anObject the object
     * @param aMethodName the method name
     * @param parameterClasses the class objects for the method parameters
     * @param parameterObjects the parameter objects
     * @return the method return value    
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws DbdException if an otherwise unreported problem
     * causes the method to go uninvoked
     */
    private Object mInvokeMethod(Object anObject, String aMethodName, 
        Class[] parameterClasses, Object[] parameterObjects) 
        throws IllegalArgumentException, InvocationTargetException,
        DbdException
    {
        Class<?> currentClass = anObject.getClass(); 
        while (currentClass != null)
        {
            try
            {
                Method method = currentClass.getMethod(aMethodName, 
                    parameterClasses);
                gLog.trace("Invoking " + currentClass.getName() + "." + 
                    aMethodName); 
                return method.invoke(anObject, parameterObjects);
            }
            catch (NoSuchMethodException noMethod)
            {
                gLog.trace(currentClass.getName() + "/" + noMethod.toString()); 
            }
            catch (SecurityException security)
            {
                gLog.trace(currentClass.getName() + "/" + security.toString()); 
            }
            catch (IllegalAccessException access)
            {
                gLog.trace(currentClass.getName() + "/" + access.toString()); 
            }

            Class<?>[] interfaces = currentClass.getInterfaces(); 
            for (int i = 0; i < interfaces.length; i++)
            {
                try
                {
                    Method method = interfaces[i].getMethod(
                        aMethodName, parameterClasses);
                    gLog.trace("Invoking " + interfaces[i].getName() + "." + 
                        aMethodName); 
                    return method.invoke(anObject, parameterObjects); 
                }
                catch (NoSuchMethodException noMethod)
                {
                    gLog.trace(currentClass.getName() + "/" + 
                        noMethod.toString()); 
                }
                catch (SecurityException security)
                {
                    gLog.trace(currentClass.getName() + "/" + 
                        security.toString()); 
                }
                catch (IllegalAccessException access)
                {
                    gLog.trace(currentClass.getName() + "/" + 
                        access.toString()); 
                }
            }

            currentClass = currentClass.getSuperclass();
        }
        throw new DbdException(DbdException.gREFLECTION_EXCEPTION,
            new String[] { "Unable to invoke method" }); 
    }

    /*
    private Method mTestInterfaces(Class c, String aMethodName, 
        Class[] parameterClasses) 
    {
        Class[] interfaces = c.getInterfaces(); 
        gLog.debug("Found " + interfaces.length + 
            " other interfaces of " + c.getName() + " to try"); 
        for (int i = 0; i < interfaces.length; i++)
        {
            int mod = interfaces[i].getModifiers();
            if (Modifier.isPublic(mod))
            {
                gLog.debug("interface " + interfaces[i].getName() +
                    " is public"); 
                try
                {
                    Method method = interfaces[i].getMethod(
                        aMethodName, parameterClasses);
                    if (gLog.isTraceEnabled())
                    {
                        gLog.trace("Found public interface " +
                            interfaces[i].getName() + 
                            " with method " + aMethodName);
                    }
                    return method;
                }
                catch (NoSuchMethodException e)
                {
                }
                catch (SecurityException e)
                {
                }
            }
        }
        return null; 
    }
    */


    // Support methods.


    /** 
     * Returns a <code>java.lang.Class</code> object
     * corresponding to the argument, which should be a value
     * from <code>java.sql.Types</code>.  This method returns the
     * primitive Class objects (e.g. Integer.TYPE) rather than
     * the wrapper class objects (Integer.class). This means that
     * you can't call a method which takes one of the
     * <code>java.lang</code> wrapper types. Since primitive
     * types are much more common as arguments, this doesn't seem
     * like too much of a problem.
     *   <p>
     * The JDBC type to Class mappings are based on the JDBC type
     * to Java type tables in the JDBC books:
     * <ul>
     * <li>Table 21.6.1 in <i>JDBC Data Access with Java</i>,
     *      Hamilton, Cattell, Fisher
     * <li>Table 47.9.1 in <i>JDBC API Tutorial and Reference,
     *      Second Edition</i>, White, Fisher, Cattell, Hamilton,
     *      Hapner
     * </ul>
     *
     * @param aJdbcType a JDBC type code from <code>java.sql.Types</code>
     * @return the class object corresponding to the given type
     */
    private Class mGetClass(int aJdbcType)
    {
        switch (aJdbcType) 
        {
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
            byte[] b = new byte[0];
            return b.getClass();

        case Types.TINYINT:             return Byte.TYPE;
        case Types.SMALLINT:            return Short.TYPE;
        case Types.INTEGER:             return Integer.TYPE;
        case Types.BIGINT:              return Long.TYPE;
        case Types.REAL:                return Float.TYPE;
        case Types.FLOAT: 
        case Types.DOUBLE:              return Double.TYPE;
        case Types.DECIMAL: 
        case Types.NUMERIC:             return BigDecimal.class;
        case Types.BIT:                 return Boolean.TYPE;
        case Types.CHAR: 
        case Types.VARCHAR:
        case Types.LONGVARCHAR:         return String.class;
        case Types.DATE:                return java.sql.Date.class;
        case Types.TIME:                return java.sql.Time.class;
        case Types.TIMESTAMP:           return java.sql.Timestamp.class;
        case Types.OTHER: 
        default: 
            return String.class;  // String will be the actual type.
        }
    }

    /**
     * Retrieves a StatementHolder from the statement table.
     *
     * @param aStatementHandle the statement handle to retrieve
     * @return the corresponding StatementHolder from the cache
     * @exception Exception if the statement handle does not correspond
     *      to an entry in the statement table */
    private StatementHolder mGetStatementHolder(int aStatementHandle)
        throws DbdException
    {
        StatementHolder holder = (StatementHolder) mStatementTable.get(
            new Integer(aStatementHandle));
        // XXX: Should this be a runtime exception? Can the user cause this?
        if (holder == null)
            throw new DbdException(DbdException.gINVALID_STATEMENT_HANDLE);
        return holder;
    }

    /**
     * Removes trailing blanks from the given string.
     *
     * @param aString a string, potentially with trailing blanks
     * @return the same string with any trailing blanks removed
     */
    private String mChopBlanks(String aString)
    {
        if (aString != null)
        {
            int last = aString.length() - 1;
            while (last >= 0 && aString.charAt(last) == ' ')
                last--;
            return aString.substring(0, last + 1);
        }
        else
            return null;
    }

    /**
     * Reads a LONG field. Implements the DBI semantics associated with
     * the LongTruncOk and LongReadLen properties.
     *
     * @param aColumnIndex the column index being read, for error reporting
     * @param anInputStream an input stream returned by a getXXXStream method
     * @param aLongReadLen the LongReadLen property for this statement
     * @param aLongTruncOk the LongTruncOk property for this statement
     * @return a byte array read from the stream, or null if the input
     *      stream is null
     * @exception DataTruncation if the data is truncated
     * @exception IOException if an error occurs reading from the stream
     */
    private byte[] mReadLong(int aColumnIndex, InputStream anInputStream, 
        int aLongReadLen, boolean aLongTruncOk, boolean readAll) 
        throws DbdException, IOException
    {
        if (aLongReadLen == 0)
            return null;
        if (anInputStream == null)
            return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[sLONG_READ_BUFFER_SIZE];
        int read;
        int totalread = 0;

        if (readAll)
        {
            while ((read = anInputStream.read(buffer, 0, buffer.length)) != -1)
            {
                totalread += read;
                baos.write(buffer, 0, read);
            }
        }
        else
        {
            while (totalread < aLongReadLen &&
                (read = anInputStream.read(buffer, 0, 
                    Math.min(buffer.length, (aLongReadLen - totalread)))) != -1)
            {
                totalread += read;
                baos.write(buffer, 0, read);
            }
        }
        baos.close();
        if (gLog.isDebugEnabled())
        {
            gLog.debug("Read " + totalread + " bytes from LONG column " + 
                aColumnIndex + "(LongReadLen=" + aLongReadLen + 
                ";LongTruncOk=" + aLongTruncOk + ";jdbc_longreadall=" + 
                readAll + ")");
        }
        if (!readAll && (!aLongTruncOk && anInputStream.read() != -1))
        {
            anInputStream.close();
            throw new DbdException(DbdException.gDATA_TRUNCATION);
        }
        anInputStream.close();
        return baos.toByteArray();
    }

    /**
     * Reads a LONG field. Implements the DBI semantics associated with
     * the LongTruncOk and LongReadLen properties.
     *
     * @param aColumnIndex the column index being read, for error reporting
     * @param aReader a reader returned by a getXXXStream method
     * @param aLongReadLen the LongReadLen property for this statement
     * @param aLongTruncOk the LongTruncOk property for this statement
     * @return a char array read from the stream, or null if the input
     *      stream is null
     * @exception DataTruncation if the data is truncated
     * @exception IOException if an error occurs reading from the stream
     */
    private char[] mReadLong(int aColumnIndex, Reader aReader, 
        int aLongReadLen, boolean aLongTruncOk, boolean readAll) 
        throws DbdException, IOException
    {
        if (aLongReadLen == 0)
            return null;
        if (aReader == null)
            return null;
        CharArrayWriter out = new CharArrayWriter(aLongReadLen); 
        char[] buffer = new char[sLONG_READ_BUFFER_SIZE]; 
        int read;
        int totalread = 0;

        if (readAll)
        {
            while ((read = aReader.read(buffer, 0, buffer.length)) != -1)
            {
                totalread += read;
                out.write(buffer, 0, read);
            }
        }
        else
        {
            while (totalread < aLongReadLen &&
                (read = aReader.read(buffer, 0, 
                    Math.min(buffer.length, (aLongReadLen - totalread)))) != -1)
            {
                totalread += read;
                out.write(buffer, 0, read);
            }
        }
        out.close();
        if (gLog.isDebugEnabled())
        {
            gLog.debug("Read " + totalread + " bytes from LONG column " + 
                aColumnIndex + "(LongReadLen=" + aLongReadLen + 
                ";LongTruncOk=" + aLongTruncOk + ";jdbc_longreadall=" +
                readAll + ")");
        }
        if (!readAll && (!aLongTruncOk && aReader.read() != -1))
        {
            aReader.close();
            throw new DbdException(DbdException.gDATA_TRUNCATION);
        }
        aReader.close();
        return out.toCharArray();
    }

    /** 
     * Sends the given error information to the client.
     *
     * @param aMessage the error text
     * @param aCode the error code
     * @param aSQLState the SQL state associated with this error
     * @exception FatalException if the error message can't be sent
     */
    private void mSendError(SQLException aSQLException)
    {
        try 
        {
            ErrorResponse error = new ErrorResponse(aSQLException,
                mBerModule.getCharacterEncoding());
            if (gLog.isTraceEnabled())
                gLog.trace("Sending error: " + aSQLException.getMessage());
            error.writeTo(mOut);
            mOut.flush();
        }
        catch (UnsupportedEncodingException unsupEnc)
        {
            throw new UnreachableCodeException();
        }
        catch (Exception e )
        {
            throw new FatalException("Failed to send error message " +
                "to client: " + e.toString());
        }
    }
}


/**
 * This class encapsulates a PreparedStatement and its associated 
 * ResultSet, ResultSetMetaData, and properties.
 */
class StatementHolder
{
    /** The PreparedStatement. */
    PreparedStatement mStatement;
    /** The statement's result set, if relevant. */
    ResultSet mResultSet;
    /** The result set meta data. */
    ResultSetMetaData mResultSetMetaData;
    /** The statement properties (LongReadLen, etc.). */
    Hashtable<String, Object> mStatementProperties;

    /**
     * Constructor - initializes fields. 
     *
     * @param aStatement a PreparedStatement
     */
    StatementHolder(PreparedStatement aStatement)
    {
        mStatement = aStatement;
        mStatementProperties = new Hashtable<String, Object>();
        
        mResultSet = null;
        mResultSetMetaData = null;
    }

    /**
     * Closes this statement. Ignores any exceptions thrown by 
     * PreparedStatement.close.
     */
    void close()
    {
        if (mStatement != null)
        {
            try { mStatement.close(); } catch (Exception e) { }
        }
        mStatementProperties = null;
        mResultSetMetaData = null;
        mResultSet = null;
        mStatement = null;
    }

    /**
     * Finishes this statement. Ignores any exceptions thrown by 
     * ResultSet.close. 
     */
    /* This is like DBI's finish - no more data to be read from the 
     * statement, but execute may be called again. However, in JDBC, 
     * closing the result set may commit a transaction in AutoCommit 
     * mode, which is forbidden by the DBI spec. I'm leaving this 
     * here, but it shouldn't be called unless we decide to support
     * this behavior.
     */
    void finish()
    {
        if (mResultSet != null)
        {
            try { mResultSet.close(); } catch (Exception e) { }
        }
        mResultSet = null;
        mResultSetMetaData = null;
    }

    /**
     * Returns this holder's PreparedStatement.
     *
     * @return this holder's PreparedStatement
     */
    PreparedStatement getStatement()
    {
        return mStatement;
    }

    /**
     * Returns this holder's ResultSet.
     *
     * @return this holder's ResultSet
     */
    ResultSet getResultSet()
    {
        return mResultSet;
    }
    /**
     * Updates this holder's ResultSet and ResultSetMetaData
     * when the statement has been executed.
     *
     * @param aResultSet a new ResultSet for this statement
     * @exception SQLException if ResultSet.getMetaData fails
     */
    void setResultSet(ResultSet aResultSet) throws SQLException
    {
        mResultSet = aResultSet;
        mResultSetMetaData = 
            (aResultSet == null) ? null : aResultSet.getMetaData();
    }


    /**
     * Returns this holder's ResultSetMetaData.
     *
     * @return this holder's ResultSetMetaData
     */
    ResultSetMetaData getResultSetMetaData()
    {
        return mResultSetMetaData;
    }

    /**
     * Returns this holder's property list.
     *
     * @return this holder's property list
     */
    Hashtable<String, Object> getProperties()
    {
        return mStatementProperties;
    }
}


/**
 * Caches the value and metadata for a generated key.
 */
class GeneratedKey
{
    /** The catalog name. */
    String catalog;

    /** The schema name. */
    String schema;

    /** The table name. */
    String table;

    /** The column name. */
    String columnName;

    /** The key value. */
    String value;


    /**
     * Constructor - caches values.
     *
     * @param aCatalog the catalog
     * @param aSchema the schema
     * @param aTable the table
     * @param aColumnName the column name
     * @param aValue the value
     */
    GeneratedKey(String aCatalog, String aSchema, String aTable, 
        String aColumnName, String aValue)
    {
        catalog = aCatalog;
        schema = aSchema;
        table = aTable;
        columnName = aColumnName;
        value = aValue;
    }


    /**
     * Returns a string representation of the key.
     *
     * @return a string representation of the key
     */
    public String toString()
    {
        StringBuffer b = new StringBuffer();
        if (catalog != null)
            b.append(catalog);
        if (schema != null && b.length() > 0)
            b.append(".").append(schema);
        else if (schema != null)
            b.append(schema);            
        if (table != null && b.length() > 0)
            b.append(".").append(table);
        else if (table != null)
            b.append(table);            
        if (columnName != null && b.length() > 0)
            b.append(".").append(columnName);
        else if (columnName != null)
            b.append(columnName);
        b.append(" = "); 
        if (value != null)
            b.append(value);
        else
            b.append("NULL");
        return b.toString(); 
    }
}
