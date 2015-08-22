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

import com.vizdom.util.Debug;

/**
 * A BER NULL. For efficiency, use <code>BerModule.NULL</code> in place 
 * of <code>new BerNull()</code>, to avoid creating new instances.
 * 
 * @author John Lacey
 * @version $Revision: 1.9 $
 * @see BerModule#NULL
 */
/*
 * BerModule treats this like a singleton class by keeping a single
 * instance around, akin to using Boolean.{TRUE,FALSE}. We don't want
 * to require the use of factories for just this class (inconsistent)
 * or all BerObject subclasses (annoying).
 */
public class BerNull extends BerObject
{
    /** The NULL identifier, [UNIVERSAL 5]. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.PRIMITIVE, BerTypes.NULL);


    /** Constructs a BER NULL object. */
    /* This is also the decoding constructor. */
    public BerNull()
    {
        // There is nothing to do. NULL values have no state.
    }


    /**
     * Returns the BER identifier for BER NULL, [UNIVERSAL 5].
     * 
     * @return the BER identifier for BER NULL, [UNIVERSAL 5]
     */
    public BerIdentifier getIdentifier()
    {
        return gIDENTIFIER;
    }


    /**
     * Returns the size of the encoded contents, in bytes.
     * 
     * @return the size of the encoded contents, in bytes
     */
    protected final int mGetLength()
    {
        return 0;
    }


    /** 
     * Writes the encoded contents to the output stream.
     * 
     * @param anOut an output stream
     */
    protected final void mWriteContents(java.io.OutputStream anOut)
    {
        // There is nothing to do. NULL values have no contents.
    }


    /** 
     * Reads the encoded contents from the input stream.
     * 
     * @param anIn an input stream
     * @param aModule a BER module for reading constructed encodings
     * @param anIdentifier the BER identifier of the encoded object
     * @param aLength the length in bytes of the encoded contents
     */
    protected void mReadContents(java.io.InputStream anIn, 
        BerModule aModule, BerIdentifier anIdentifier, int aLength)
    {
        if (Debug.ASSERT && getClass() == BerNull.class)
            Debug.assertTrue(anIdentifier == gIDENTIFIER); // [sic; not equals]
        Debug.assertTrue(aLength == 0);
    }


    /**
     * Returns a string representation of NULL.
     * 
     * @return a string representation of NULL
     */
    public String toString()
    {
        return "NULL";
    }
}

