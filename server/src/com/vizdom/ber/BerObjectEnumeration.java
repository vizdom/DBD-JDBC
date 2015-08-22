/*
 *  Copyright 1999-2005 Vizdom Software, Inc. All Rights Reserved.
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

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A BER sequence enumerator.
 *
 * @author: John Lacey
 * @version: $Revision: 1.1 $
 */
/* 
 * ??? This is just an Object[] enumerator in disguise. 
 * Move to com.vizdom.util? 
 */
final class BerObjectEnumeration implements Enumeration<BerObject>
{
    /** The array being enumerated. */
    private final BerObject[] mSequence;

    /** The current position. */
    private int mIndex;


    /**
     * Constructs an enumerator for the given array.
     * 
     * @param aSequence an array of BerObjects
     */
    BerObjectEnumeration(BerObject[] aSequence) 
    {
        mSequence = aSequence;
        mIndex = 0;
    }


    /**
     * Tests if this enumeration contains more elements.
     * 
     * @return <code>true</code> if and only if this enumeration object 
     *     contains at least one more element to provide;
     *     <code>false</code> otherwise.
     */
    public boolean hasMoreElements() 
    {
        return mIndex < mSequence.length;
    }


    /**
     * Returns the next element of this enumeration if this enumeration 
     * object has at least one more element to provide.
     * 
     * @return the next element of this enumeration
     * @exception NoSuchElementException if no more elements exist
     */
    public BerObject nextElement() 
    {
        if (mIndex < mSequence.length)
            return mSequence[mIndex++];
        else
            throw new NoSuchElementException("BerObjectEnumeration");
    }
}
