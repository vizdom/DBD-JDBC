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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;


/**
 * This class can be used in two ways: as a stand-alone application
 * which acts as a connection server, or as a thread which
 * receives and services a single client connection.
 * <p>
 * As an application, this class listens to a specified port
 * for incoming connections and creates threads to 
 * handle them. Multiple connections may be made at once; 
 * each will have its own thread and JDBC connection.
 * The following system properties should be set
 * on the command line using the -D option:
 * <ul>
 * <li> <code>jdbc.drivers</code>: the names of the JDBC drivers
 *      which this server should load on startup
 * <li> <code>dbd.port</code>: the port on which this server will listen
 * <li> <code>dbd.trace</code>: (optional) the logging level for the server
 *      <p>This property should be set to one of "off" or
 *      "silent", "fatal", "error", "warn" or "brief", "info" or
 *      "verbose", "debug" or "tedious", "trace" or "abusive" or
 *      "all". The default is "off". Logging may also be
 *      controlled using a log4j.properties file.
 * </ul>
 * For example,
 * <pre>
 * java -Djdbc.drivers=oracle.jdbc.driver.OracleDriver -Ddbd.port=12345 \
 * -Ddbd.trace=info com.vizdom.dbd.jdbc.Server
 * </pre>
 * <p>
 * As a thread, this class is intended to be used within another 
 * application to provide scripting access to an existing JDBC
 * connection. For example, a Java servlet might allow its output
 * to be generated by a Perl script using DBI. The servlet would
 * create a Server object with its JDBC connection, then exec
 * a Perl script, passing it the port number on which the Server
 * is listening. The Perl script would then be able to use DBI
 * to interact with the existing JDBC connection. The servlet would
 * receive the script output and do with it as it wished.
 *
 * @version $Revision: 1.22 $
 * @see CgiProcess
 */
public class Server implements Runnable
{
    /** log4j logger. */
    private static final Logger gLog = Logger.getLogger(Server.class); 


    /**
     * Starts a server listening to a given socket for connection
     * requests.
     *
     * @param args command-line arguments; there are none for this
     *      application
     */
    public static void main(String[] args)
    {
        if (args.length  > 0)
        {
            System.err.println("DBD::JDBC server " + Version.version);
            System.err.println("Required system properties:");
            System.err.println("  -Djdbc.drivers=[driverlist]");
            System.err.println("  -Ddbd.port=[portnum]");
            System.err.println("Optional system properties:");
            System.err.println(
                "  -Ddbd.trace=[silent|brief|verbose|tedious|abusive]");
            return;
        }

        String port;
        int portnum;
        try
        {
            port = System.getProperty("dbd.port");
            if (port == null)
            {
                throw new FatalException(
                    "Property dbd.port was not specified.");
            }
            portnum = Integer.parseInt(port);
        }
        catch (NumberFormatException nf)
        {
            throw new FatalException("Property dbd.port is not an integer.");
        }
        catch (SecurityException se)
        {
            throw new FatalException("Unable to access property dbd.port.");
        }

        NDC.push("[Server]");

        String trace = System.getProperty("dbd.trace");
        if (trace != null)
        {
            Logger appLogger = gLog.getLogger("com.vizdom.dbd.jdbc");
            if ("silent".equalsIgnoreCase(trace) ||
                "off".equalsIgnoreCase(trace))
            {
                appLogger.setLevel((Level) Level.OFF);
            }
            else if ("fatal".equalsIgnoreCase(trace))
            {
                appLogger.setLevel((Level) Level.FATAL); 
            }
            else if ("error".equalsIgnoreCase(trace))
            {
                appLogger.setLevel((Level) Level.ERROR); 
            }
            else if ("brief".equalsIgnoreCase(trace) ||
                "warn".equalsIgnoreCase(trace))
            {
                appLogger.setLevel((Level) Level.WARN);
            }
            else if ("verbose".equalsIgnoreCase(trace) ||
                "info".equalsIgnoreCase(trace))
            {
                appLogger.setLevel((Level) Level.INFO);
            }
            else if ("tedious".equalsIgnoreCase(trace) ||
                "debug".equalsIgnoreCase(trace))
            {
                appLogger.setLevel((Level) Level.DEBUG);
            }
            else if ("abusive".equalsIgnoreCase(trace) ||
                "trace".equalsIgnoreCase(trace) ||
                "all".equalsIgnoreCase(trace))
            {
                appLogger.setLevel((Level) Level.TRACE);
            }
        }

        // Set the default character encoding. If the client transmits
        // another character encoding, it must use ASCII to do so.
        try
        {
            BerDbdModule.gBerModule.setCharacterEncoding("ASCII");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new FatalException(
                "ASCII character encoding is not supported"); 
        }

        ServerSocket ss = null;
        try
        {
            ss = new ServerSocket(portnum);
            gLog.info("[Server] accepting connections");
            while (true)
            {
                try
                {
                    gCreateThread(ss.accept());
                }
                catch (Exception e)
                {
                    gLog.warn("[Server] " + e.toString());
                }
            }
        }
        catch (IOException ioError)
        {
            throw new FatalException(ioError.toString());
        }
        finally
        {
            try { if (ss != null) ss.close(); } catch (Throwable t) {}
            NDC.pop();
        }
    }


    /**
     * Creates the client thread for an initial request.
     *
     * @param socket the client socket
     * @throws Exception if the thread can't be created
     */
    private static void gCreateThread(Socket socket) throws Exception
    {
        gLog.info("[Server] received a connection from " + 
            socket.getInetAddress());
        Thread t = new Thread(new Connection(socket, 
            (BerDbdModule) BerDbdModule.gBerModule.clone()));
        if (t == null)
            throw new Exception("Failed to create client thread.");
        t.start();
        t = null; 
    }



    // The thread implementation.

    /** The socket for the client to connect to. */
    private ServerSocket mSocket;
    /** The JDBC connection. */
    private java.sql.Connection mConn;

    
    /**
     * Creates a ServerSocket listening to a random port.
     * Calls <code>Server(java.sql.Connection, 0)</code>.
     *
     * @param aConnection a JDBC Connection object
     * @exception IOException if an error occurs creating the socket
     */
    public Server(java.sql.Connection aConnection) throws IOException
    {
        this(aConnection, 0);
    }

    /**
     * Creates a ServerSocket listening to the specified port.
     *
     * @param aConnection a JDBC Connection object
     * @param aPortNumber the port number the socket should use
     * @exception IOException if an error occurs creating the socket
     */
    public Server(java.sql.Connection aConnection, int aPortNumber) 
        throws IOException
    {
        mConn = aConnection;
        mSocket = new ServerSocket(aPortNumber);
    }

    /**
     * Returns the port used by this object's socket.
     *
     * @return the port used by this object's socket
     */
    public int getPort()
    {
        return mSocket.getLocalPort();
    }

 
    /**
     * Listens for a client connection and creates a Connection object
     * which runs until disconnect.
     */
    public void run()
    {
        try
        {
            // Set the default character encoding. If the client transmits
            // another character encoding, it must use ASCII to do so.
            BerDbdModule.gBerModule.setCharacterEncoding("ASCII");
            
            Connection dbiConn = new Connection(mSocket.accept(),
                (BerDbdModule) BerDbdModule.gBerModule.clone(), mConn);
            dbiConn.run(); // loops until disconnect
            dbiConn = null;
            mConn = null; 
        }
        catch (IOException ioEx)
        {
            // If used by a servlet, logging errors to System.out
            // may be reasonable. 
            // System.out.println("Server.run: " + ioEx.toString());
        }
    }
}
