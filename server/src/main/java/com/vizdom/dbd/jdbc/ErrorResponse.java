/*
 * Copyright 1999-2001 Vizdom Software, Inc. All Rights Reserved.
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

import com.vizdom.ber.*;
import java.sql.SQLException;

/**
 * An ErrorResponse is a sequence of one or more Error packets. Each
 * Error packet is formed from a SQLException or subclass thereof.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.12 $
 */
class ErrorResponse extends BerSequence
{
    /** The BER identifier for this object. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gERROR_RESPONSE);

    /** For use in {@link #toString}. */
    private String mMessage;

    /**
     * Constructor - initializes sequence elements with an Error
     * object for <code>aSQLException</code> and each exception 
     * chained to it.
     *
     * @param aSQLException the SQLException to be sent to the client
     * @param aCharacterEncoding the character encoding to use in 
     *      encoding the message string
     * @exception UnsupportedEncodingException if the application
     *      selects an unsupported character encoding
     */
    ErrorResponse(SQLException aSQLException, String aCharacterEncoding) 
        throws java.io.UnsupportedEncodingException
    {
        super();
        mMessage = aSQLException.getMessage();
        java.util.Vector<Error> errors = new java.util.Vector<Error>();
        do
        {
            errors.addElement(new Error(aSQLException, aCharacterEncoding));
        } while ((aSQLException = aSQLException.getNextException()) != null);

        mSequence = new BerObject[errors.size()];
        errors.copyInto(mSequence);
    }

    /**
     * Returns the identifier for this BerObject. 
     *
     * @return the identifier for this BerObject
     */
    public BerIdentifier getIdentifier()
    {
        return gIDENTIFIER;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString()
    {
        return ((Error) mSequence[0]).toString();
    }
}

/**
 * An Error encapsulates a single exception. Note that all three 
 * elements (message, error code, sql state) will always be sent, 
 * even if one or more are empty.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.12 $
 */
class Error extends BerSequence
{
    /**
     * Some JDBC drivers (Oracle, for example) don't seem to
     * provide a SqlState for all SQLExceptions. DBI requires
     * that the SqlState be exactly 5 characters. This value is
     * used as a default to ensure that there's a SqlState
     * available on the DBI side. (SqlState values are defined in
     * the standard to be 5 characters, so if a driver provides
     * anything else, it's out to lunch and we can't help.)  
     */
    private static final String gDEFAULT_SQL_STATE = "IDRVR";

    /** The BER identifier for this object. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gERROR);

    /** The exception message. */
    private String mMessage;
    /** The exception error code. */
    private String mCode;
    /** The exception SQL state. */
    private String mSqlState;

    /**
     * Constructor - initializes the sequence members for this object.
     *
     * @param aSQLException the SQLException to be sent to the client
     * @param aCharacterEncoding the character encoding to use in 
     *      encoding the message string
     * @exception UnsupportedEncodingException if the application
     *      selects an unsupported character encoding
     */
    Error(SQLException aSQLException, String aCharacterEncoding) 
        throws java.io.UnsupportedEncodingException
    {
        super();
        mMessage = aSQLException.getMessage();
        if (mMessage == null)
            mMessage = "";
        mCode = String.valueOf(aSQLException.getErrorCode());
        mSqlState = aSQLException.getSQLState();
        // Oracle's driver seems to use "" for SQLState from time to time.
        if (mSqlState == null || mSqlState.equals(""))
            mSqlState = gDEFAULT_SQL_STATE; 
        
        mSequence = new BerObject[3];
        mSequence[0] = new BerOctetString(mMessage, aCharacterEncoding);
        mSequence[1] = new BerOctetString(mCode, aCharacterEncoding);
        mSequence[2] = new BerOctetString(mSqlState, aCharacterEncoding);
    }

    /**
     * Returns the identifier for this BerObject. 
     *
     * @return the identifier for this BerObject
     */
    public BerIdentifier getIdentifier()
    {
        return gIDENTIFIER;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString()
    {
        return mMessage + "(" + mCode + "/" + mSqlState + ")";
    }
}
