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
 * A fetch response will return a sequence of column values as
 * octet strings or will indicate that no more data is available.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.9 $
 */
class FetchResponse extends BerSequence
{
    /* The first sequence element indicates whether or not there's
     * any data in this packet. A 'No data' value is used to indicate
     * that the end of the result set has been reached.
     */

    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gFETCH_RESPONSE);

    /**
     * Constructor - initializes response data.
     *
     * @param aContainsDataFlag true if there are column values; 
     *      false otherwise
     * @param aRow the column values; either String, byte[], or
     *      null values
     * @param aCharacterEncoding the character encoding to use in 
     *      encoding any String data
     * @exception UnsupportedEncodingException if the application
     *      selects an unsupported character encoding
     */
    FetchResponse(boolean aContainsDataFlag, Object[] aRow, 
        String aCharacterEncoding) throws java.io.UnsupportedEncodingException
    {
        super();
        mSequence = new BerObject[((aRow != null) ? aRow.length : 0) + 1];
        mSequence[0] = new BerInteger(aContainsDataFlag ? 1 : 0);
        if (aContainsDataFlag && aRow != null)
        {
            for (int i = 0; i < aRow.length; i++)
            {
                if (aRow[i] == null)
                    mSequence[i + 1] = BerDbdModule.NULL;
                else if (aRow[i] instanceof Boolean)
                {
                    mSequence[i + 1] = new BerBoolean((Boolean) aRow[i]);
                }
                else if (aRow[i] instanceof Integer)
                {
                    mSequence[i + 1] = new BerInteger((Integer) aRow[i]);
                }
                /* TODO support REAL TYPE in BER
                else if (aRow[i] instanceof BigDecimal)
                {
                    mSequence[i + 1] = new BerReal((BigDecimal) aRow[i]);
                }
                */
                /* TODO support REAL TYPE in BER
                else if (aRow[i] instanceof Float)
                {
                    mSequence[i + 1] = new BerReal((Float) aRow[i]);
                }
                */
                else if (aRow[i] instanceof String)
                {
                    mSequence[i + 1] = new BerOctetString((String) aRow[i],
                        aCharacterEncoding);
                }
                else if (aRow[i] instanceof byte[])
                {
                    mSequence[i + 1] = new BerOctetString((byte[]) aRow[i],
                        aCharacterEncoding);
                }
            }
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
        return "Fetch complete";
    }
}

