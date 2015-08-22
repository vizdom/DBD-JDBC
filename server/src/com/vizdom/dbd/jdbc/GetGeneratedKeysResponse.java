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

/**
 * A generated key.
 */
class GetGeneratedKeysResponse extends BerOctetString
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.PRIMITIVE, 
        BerDbdModule.gGET_GENERATED_KEYS_RESPONSE);


    /**
     * Constructor. 
     *
     * @param key the key to return
     * @param aCharacterEncoding the character encoding to use in 
     *      encoding any String data
     * @exception UnsupportedEncodingException if the application
     *      selects an unsupported character encoding
     */
    GetGeneratedKeysResponse(String key, 
        String aCharacterEncoding) throws java.io.UnsupportedEncodingException
    {
        super(key, aCharacterEncoding);
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
        return "Generated key being returned: " + super.toString();
    }
}



