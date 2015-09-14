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
 * A request to retrieve the value of a connection property.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.8 $
 */
class GetConnectionPropertyRequest extends BerOctetString
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.PRIMITIVE, 
        BerDbdModule.gGET_CONNECTION_PROPERTY_REQUEST);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = new BerObjectFactory() {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }
        
        public BerObject createBerObject()
        {
            return new GetConnectionPropertyRequest();
        }
    };

    /**
     * The decoding constructor.
     */
    private GetConnectionPropertyRequest()
    {
        super();
    }    

    /**
     * Returns the name of the property whose value should be returned.
     *
     * @return the name of the property whose value should be returned
     */
    String getPropertyName()
    {
        return super.toString();
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
        return "Get connection property: " + getPropertyName();
    }
}
