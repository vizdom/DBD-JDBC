/*
 *  Copyright 1999-2001 Vizdom Software, Inc. All Rights Reserved.
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

package com.vizdom.ber;

/**
 * A BER ENUMERATED type. As far as the BER are concerned, ENUMERATED
 * is just a tagged subtype of INTEGER.
 *
 * @author: John Lacey
 * @version: $Revision: 1.3 $
 */
public class BerEnumerated extends BerInteger
{
    /** The ENUMERATED identifier, [UNIVERSAL 10]. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.PRIMITIVE, BerTypes.ENUMERATED);


    /**
     * Returns the BER identifier for BER ENUMERATED, [UNIVERSAL 10].
     * 
     * @return the BER identifier for BER ENUMERATED, [UNIVERSAL 10]
     */
    public BerIdentifier getIdentifier()
    {
        return gIDENTIFIER;
    }
}

