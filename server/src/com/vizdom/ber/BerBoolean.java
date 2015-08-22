/*
 *  Copyright 1999-2006 Vizdom Software, Inc. All Rights Reserved.
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

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.vizdom.util.Debug;

/**
 * A BER BOOLEAN.
 *
 * @author: John Lacey
 * @version: $Revision: 1.11 $
 */
/* TODO: It would be nice to encode booleans according to the DER. */
public class BerBoolean extends BerObject
{
    /** The BOOLEAN identifier, [UNIVERSAL 1]. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.PRIMITIVE, BerTypes.BOOLEAN);

    /** The underlying boolean value of this BER BOOLEAN. */
    private boolean mValue;


    /** The decoding constructor. */
    protected BerBoolean()
    {
    }


    /** 
     * The encoding constructor. 
     * 
     * @param aValue a boolean value
     */
    public BerBoolean(boolean aValue)
    {
        mValue = aValue;
    }


    /**
     * Returns the BER identifier for BER BOOLEAN, [UNIVERSAL 1].
     * 
     * @return the BER identifier for BER BOOLEAN, [UNIVERSAL 1]
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
        return 1;
    }


    /** 
     * Writes the encoded contents to the output stream.
     * 
     * @param anOut an output stream
     * @exception IOException if an I/O error occurs
     */
    protected final void mWriteContents(OutputStream anOut) throws IOException
    {
        anOut.write(mValue ? 1 : 0);
    }


    /** 
     * Reads the encoded contents from the input stream.
     * 
     * @param anIn an input stream
     * @param aModule a BER module for reading constructed encodings
     * @param anIdentifier the BER identifier of the encoded object
     * @param aLength the length in bytes of the encoded contents
     * @exception IOException if an I/O error occurs. In particular,
     *     an <code>EOFException</code> may be thrown if the end of
     *     stream is reached before the contents have been fully read
     */
    protected void mReadContents(InputStream anIn, BerModule aModule,
        BerIdentifier anIdentifier, int aLength) throws IOException
    {
        // FIXME: The tests never execute this assertion.
        if (Debug.ASSERT && getClass() == BerBoolean.class)
            Debug.assertTrue(anIdentifier == gIDENTIFIER); // [sic; not equals]

        // Read the byte.
        int octet = anIn.read();
        if (octet == -1)
            throw new EOFException();

        mValue = (octet != 0);
    }


    /**
     * Returns the value of this object as a boolean primitive.
     *
     * @return the primitive <code>boolean</code> value of this object
     */
    public boolean booleanValue()
    {
        return mValue;
    }


    /**
     * Returns a string representation of the boolean value.
     * 
     * @return a string representation of the boolean value
     */
    public String toString()
    {
        return String.valueOf(mValue);
    }
}
