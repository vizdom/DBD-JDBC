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
 * A request to destroy a statement.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.9 $
 */
class StatementDestroyRequest extends BerInteger
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.PRIMITIVE, 
        BerDbdModule.gSTATEMENT_DESTROY_REQUEST);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = new BerObjectFactory() {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }
        
        public BerObject createBerObject()
        {
            return new StatementDestroyRequest();
        }
    };

    /**
     * The decoding constructor.
     */
    private StatementDestroyRequest()
    {
        super();
    }

    /**
     * Returns the handle of the statement to be destroyed
     *
     * @return  a statement handle 
     */
    int getHandle()
    {
        return intValue();
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
        return "Statement destroy request";
    }
}

