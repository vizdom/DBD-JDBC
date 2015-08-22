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
 * A statement property is returned as a sequence of values, where all
 * values for a given property are of the same type. The only currently 
 * recognized value types are BerInteger, BerOctetString, and BerNull.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.10 $
 */
class GetStatementPropertyResponse extends BerSequence
{
    /* A property value is either a String[] or an Integer[], with null
     * array values allowed.
     */

    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gGET_STATEMENT_PROPERTY_RESPONSE);

    /**
     * Constructor - initializes sequence values.
     *
     * @param aDataList an array of property values
     * @param aCharacterEncoding the character encoding to use in 
     *      encoding any String data
     * @exception UnsupportedEncodingException if the application
     *      selects an unsupported character encoding
     */
    GetStatementPropertyResponse(Object[] aDataList, 
        String aCharacterEncoding) throws java.io.UnsupportedEncodingException
    {
        super();
        mSequence = new BerObject[aDataList.length];
        if (aDataList instanceof Integer[])
        {
            Integer[] data = (Integer []) aDataList;
            for (int i = 0; i < data.length; i++)
            {
                if (data[i] == null)
                    mSequence[i] = BerDbdModule.NULL;
                else
                    mSequence[i] = new BerInteger(data[i].intValue());
            }
        }
        else if (aDataList instanceof String[])
        {
            String[] data = (String []) aDataList;
            for (int i = 0; i < data.length; i++)
            {
                if (data[i] == null)
                    mSequence[i] = BerDbdModule.NULL;
                else  
                    mSequence[i] = new BerOctetString(data[i], 
                    aCharacterEncoding);
            }
        }
        else 
        {
            // Could probably just call toString on everything. So far,
            // there are no other types of connection properties.
            throw new com.vizdom.util.UnreachableCodeException();
        }
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
        return "Statement property data being returned";
    }
}




