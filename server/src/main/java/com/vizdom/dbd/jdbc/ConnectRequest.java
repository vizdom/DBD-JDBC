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
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

/**
 * A connection request.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.11 $
 */
class ConnectRequest extends BerSequence
{
    /* ConnectRequest {
     *    OctetString url
     *    OctetString username
     *    OctetString password
     *    BerHash properties
     * }
     *
     * It probably wouldn't hurt to move the url/username/password into 
     * the hash.
     */
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gCONNECT_REQUEST);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = 
        new BerObjectFactory()
    {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }
        
        public BerObject createBerObject()
        {
            return new ConnectRequest();
        }
    };

    /** The index of the url within the sequence. */
    private static final int sURL                               = 0;
    /** The index of the username within the sequence. */
    private static final int sUSERNAME                          = 1;
    /** The index of the password within the sequence. */
    private static final int sPASSWORD                          = 2;
    /** The index of the character encoding within the sequence. */
    private static final int sCHARACTER_ENCODING                = 3;
    /** The index of the properties hash within the sequence. */
    private static final int sPROPERTIES                        = 4;

    /**
     * The decoding constructor.
     */
    private ConnectRequest()
    {
        super();
    }

    /**
     * Returns the url portion of the connection request.
     *
     * @return the url portion of the connection request
     */
    String getURL(String aCharacterEncoding) 
        throws UnsupportedEncodingException
    {
        return ((BerOctetString) mSequence[sURL]).toString(
            aCharacterEncoding);
    }

    /**
     * Returns the username portion of the connection request.
     *
     * @return the username portion of the connection request
     */
    String getUser(String aCharacterEncoding)
        throws UnsupportedEncodingException
    {
        if (mSequence[sUSERNAME] instanceof BerNull)
            return null;
        return ((BerOctetString) mSequence[sUSERNAME]).toString(
            aCharacterEncoding);
    }

    /**
     * Returns the password portion of the connection request.
     *
     * @return the password portion of the connection request
     */
    String getPassword(String aCharacterEncoding)
        throws UnsupportedEncodingException
    {
        if (mSequence[sPASSWORD] instanceof BerNull)
            return null;
        return ((BerOctetString) mSequence[sPASSWORD]).toString(
            aCharacterEncoding);
    }

    /**
     * Returns the character encoding portion of the connection request.
     *
     * @return the character encoding portion of the connection request
     */
    String getCharacterEncoding() throws UnsupportedEncodingException
    {
        return ((BerOctetString) mSequence[sCHARACTER_ENCODING])
            .toString("ASCII");
    }

    /**
     * Returns the name/value pairs sent by the client. May
     * return an empty Properties object.
     *
     * @return the name/value pairs sent by the client */
    Properties getProperties(String aCharacterEncoding)
        throws UnsupportedEncodingException
    {
        Hashtable h = ((BerHash) mSequence[sPROPERTIES])
            .toHashtable(aCharacterEncoding);
        Properties p = new Properties();
        Enumeration keys = h.keys();
        while (keys.hasMoreElements())
        {
            String key = (String) keys.nextElement();
            p.put(key, h.get(key));
        }
        return p;
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
        return "Connect";
    }
}
