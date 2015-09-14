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

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.vizdom.util.Debug;

/**
 * A BER INTEGER. BER does not restrict the size of integers,
 * but this implementation restricts them to 32 bits.
 *
 * @author: John Lacey
 * @version: $Revision: 1.15 $
 */
/*
 * There is no reason why integers couldn't be extended to 64 bits.
 * The only real issue is performance. Ideally, separate classes
 * could be introduced for handling larger integers as longs and even
 * BigIntegers. 
 * 
 * Both encoding and decoding are straightforward. Encoding is trivial. 
 * Decoding would involve some work to decide which class to instantiate. 
 * The encoded length, however, is sufficient, since integers are required
 * by the BER to be encoded in the fewest number of octets. The set of 
 * BER INTEGER values that are encoded in four or fewer bytes is the same
 * as the set of values representable as a 32-bit signed integer.
 */
public class BerInteger extends BerObject
{
    /** The INTEGER identifier, [UNIVERSAL 2]. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.PRIMITIVE, BerTypes.INTEGER);

    /** The underlying int value of this BER INTEGER. */
    private int mValue;


    /** The decoding constructor. */
    protected BerInteger()
    {
    }


    /** 
     * The encoding constructor. 
     * 
     * @param aValue an int value
     */
    public BerInteger(int aValue)
    {
        mValue = aValue;
    }


    /**
     * Returns the BER identifier for BER INTEGER, [UNIVERSAL 2].
     * 
     * @return the BER identifier for BER INTEGER, [UNIVERSAL 2]
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
        int count;
        if (mValue == 0 || mValue == -1)
            count = 1;
        else
        {
            int octet = 0;
            count = 0;
            for (int n = mValue; n != 0 && n != -1; n >>= 8)
            {
                octet = n & 0xFF;
                ++count;
            }
            if ((octet & 0x80) == 0x80 && mValue >= 0)
                ++count;
        }
        return count;
    }


    /** 
     * Writes the encoded contents to the output stream.
     * 
     * @param anOut an output stream
     * @exception IOException if an I/O error occurs
     */
    protected final void mWriteContents(OutputStream anOut) throws IOException
    {
        if (mValue == 0 || mValue == -1)
            anOut.write(mValue);
        else
        {
            int r = 0;
            int count = 0;
            for (int n = mValue; n != 0 && n != -1; n >>= 8)
            {
                r <<= 8;
                r |= (n & 0xFF);
                ++count;
            }
            if ((r & 0x80) == 0x80 && mValue >= 0)
            {
                r <<= 8;
                ++count;
            }

            for (int i = 0; i < count; i++)
            {
                anOut.write(r & 0xFF);
                r >>= 8;
            }
        }
    }


    /** 
     * Reads the encoded contents from the input stream.
     * 
     * @param anIn an input stream
     * @param aModule a BER module for reading constructed encodings
     * @param anIdentifier the BER identifier of the encoded object
     * @param aLength the length in bytes of the encoded contents
     *
     * @exception IOException if an I/O error occurs. In particular,
     *     an <code>EOFException</code> may be thrown if the end of
     *     stream is reached before the contents have been fully read
     */
    protected void mReadContents(InputStream anIn, BerModule aModule,
        BerIdentifier anIdentifier, int aLength) throws IOException
    {
        if (Debug.ASSERT && getClass() == BerInteger.class)
            Debug.assertTrue(anIdentifier == gIDENTIFIER); // [sic; not equals]

        // Read the first byte.
        int octet = anIn.read();
        if (octet == -1)
            throw new EOFException();

        // Sign-extend the first byte by casting it to a byte (where it
        // has to fit) and then promoting it to an int (sign-extended).
        int n = (byte) octet;

        // Read the remaining bytes.
        for (int i = 1; i < aLength; i++)
        {
            octet = anIn.read();
            if (octet == -1)
                throw new EOFException();

            n <<= 8;
            n |= octet;
        }

        mValue = n;
    }


    /**
     * Gets the value of this encoded BER object as an <code>int</code>
     *
     * @return the integer value
     */
    public int intValue()
    {
        return mValue;
    }


    /**
     * Returns a string representation of the integer value.
     * 
     * @return a string representation of the integer value
     */
    public String toString()
    {
        return String.valueOf(mValue);
    }
}

