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
 * A request to set the value of a connection property.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.8 $
 */
class SetConnectionPropertyRequest extends BerSequence
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gSET_CONNECTION_PROPERTY_REQUEST);

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
            return new SetConnectionPropertyRequest();
        }
    };

    /**
     * The decoding constructor.
     */
    private SetConnectionPropertyRequest()
    {
        super();
    }    

    /**
     * Returns the name of the property whose value should be changed.
     *
     * @return the name of the property whose value should be changed
     */
    String getPropertyName()
    {
        return ((BerOctetString) mSequence[0]).toString();
    }

    /**
     * Returns the new property value
     *
     * @return the new property value
     */
    String getPropertyValue()
    {
        return ((BerOctetString) mSequence[1]).toString();
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
        return "Set connection property: " + getPropertyName();
    }
}

