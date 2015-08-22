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
 * A request to retrieve the value of a statement property.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.9 $
 */
class GetStatementPropertyRequest extends BerSequence
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gGET_STATEMENT_PROPERTY_REQUEST);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = new BerObjectFactory() {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }
        
        public BerObject createBerObject()
        {
            return new GetStatementPropertyRequest();
        }
    };

    /** The index of the statement handle within the sequence. */
    private static final int sHANDLE = 0;
    /** The index of the property name within the sequence. */
    private static final int sNAME = 1;

    /**
     * The decoding constructor.
     */
    private GetStatementPropertyRequest()
    {
        super();
    }    

    /**
     * Returns the handle of the statement whose property should be returned.
     *
     * @return the handle of the statement whose property should be returned
     */
    int getHandle()
    {
        return ((BerInteger) mSequence[sHANDLE]).intValue();
    }

    /**
     * Returns the name of the property whose value should be returned.
     *
     * @return the name of the property whose value should be returned
     */
    String getPropertyName()
    {
        return ((BerOctetString) mSequence[sNAME]).toString();
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
        return "Get statement property: " + getPropertyName();
    }
}
