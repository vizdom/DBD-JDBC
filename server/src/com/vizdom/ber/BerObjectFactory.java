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
 * A factory class for user-defined BER objects. This factory is used
 * to extend a module by defining tagged types, and new types.
 *
 * @author John Lacey
 * @version $Revision: 1.5 $
 */
public interface BerObjectFactory
{
    /**
     * Returns <code>true</code> if the object instantiated by this factory
     * implements the type described by the given BER identifier.
     *
     * @param anIdentifier a BER identifier
     * @return <code>true</code> if the object instantiated by this factory
     *     implements the type described by the given BER identifier
     */
    boolean acceptsIdentifier(BerIdentifier anIdentifier);


    /**
     * Creates a new BerObject instance. The BerModule that calls this
     * method will also call <code>mReadContents</code> on the new instance.
     * 
     * @return a new instance of BerObject
     */
    BerObject createBerObject();
}

