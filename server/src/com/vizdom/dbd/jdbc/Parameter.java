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
import java.util.Vector;

/**
 * A Parameter contains a parameter value and type hint.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.2 $
 */
class Parameter
{
    /** The parameter value. */
    /* This is a BerOctetString (and not a String or a byte[]) 
     * because we may need either the raw bytes or the character-encoded
     * string when we call setXXX.
     */
    BerOctetString value;
    /** The parameter type (from <code>java.sql.Types</code>. */
    int type;

    /**
     * Constructor  - initializes fields.
     *
     * @param aValue the parameter value
     * @param aType the parameter type (from <code>java.sql.Types</code>. 
     */
    Parameter(BerOctetString aValue, int aType)
    {
        value = aValue;
        type = aType;
    }
}
