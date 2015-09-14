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

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * A filtered input stream with a definite length. This is used when
 * decoding constructed contents. After the given number of bytes have
 * been read, this stream simulates EOF.
 *
 * @author: John Lacey
 * @version: $Revision: 1.9 $
 */
/*
 * We could be more efficient and cache the knowledge that the 
 * underlying stream has hit EOF.
 */
final class BerContentsInputStream extends FilterInputStream
{
    /** 
     * The total number of bytes expected to be read by this 
     * <code>InputStream</code>. 
     */
    private final int mMaxCount;
    
    /** The current count of bytes read. */
    private int mCount = 0;


    /**
     * Constructor.
     * 
     * @param anIn an <code>InputStream</code> whose size should 
     *      throttled during reading
     * @param aMaxCount the number of bytes expected in this 
     *     <code>InputStream</code>
     */
    BerContentsInputStream(InputStream anIn, int aMaxCount)
    {
        super(anIn);
        mMaxCount = aMaxCount;
    }


    /**
     * Returns the number of bytes read from this stream.
     *
     * @return the number of bytes read from this stream
     */
    public int getCount()
    {
        return mCount;
    }


    /**
     * Reads a single byte.
     *
     * @return the byte read, or -1 if the end of the stream is reached
     * @exception IOException if an I/O error occurs
     */
    public int read() throws IOException
    {
        if (mCount == mMaxCount)
            return -1;
        else
        {
            int b = in.read();
            if (b != -1)
                ++mCount;
            return b;
        }
    }


    /**
     * Reads bytes into <code>aBuffer</code>, starting at 
     * <code>anOffset</code> and reading up to 
     * <code>aLength</code> bytes.
     *
     * @param aBuffer a byte array into which to read the data
     * @param anOffset the index in the buffer at which to begin 
     *      storing the read bytes
     * @param aLength the maximum number of bytes to read
     *
     * @return the number of characters read, or -1 if the end of the 
     *      stream is reached
     * @exception IOException if an I/O error occurs
     */
    public int read(byte[] aBuffer, int anOffset, int aLength) 
        throws IOException
    {
        if (mCount == mMaxCount)
            return -1;
        else
        {
            int length = Math.min(aLength, mMaxCount - mCount);
            int count = in.read(aBuffer, anOffset, length);
            if (count != -1)
                mCount += count;
            return count;
        }
    }
}
