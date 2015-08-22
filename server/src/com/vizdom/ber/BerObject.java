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

import java.io.IOException;

/**
 * A BER object. This is the base class for all encoded types.
 * It provides core implementations of some encoding/decoding 
 * methods for subclasses.
 *
 * @author: John Lacey
 * @version: $Revision: 1.11 $
 */
/*
 * This should arguably be an interface, but implementing <code>writeTo<code>
 * here seems to outweight the benefits of an interface. This hides 
 * essentially all of the actual encoding rules from subclasses. Also, 
 * this the abstract protected methods can be protected, where an interface
 * would require them to be public.
 */
public abstract class BerObject
{
    /**
     * Reads the encoded length from the input stream.
     * 
     * @param anIn an input stream
     * @return the decoded length
     * @exception IOException if an I/O error occurs. In particular,
     *     an <code>EOFException</code> may be thrown if the end of
     *     stream is reached before the length has been fully read
     */
    static int gReadLength(java.io.InputStream anIn) throws IOException
    {
        int octet = anIn.read();
        if (octet == -1)
            throw new java.io.EOFException();

        if (octet <= 127)
            return octet;
        else
        {
            int count = (octet & 0x7F);
            int length = 0;
            for (int i = 0; i < count; i++)
            {
                octet = anIn.read();
                if (octet == -1)
                    throw new java.io.EOFException();

                length = (length << 8) | octet;
            }
            return length;
        }
    }


    /**
     * Writes the encoded length to the output stream.
     * 
     * @param aLength the encoded length
     * @param anOut an output stream
     * @exception IOException if an I/O error occurs
     */
    private static void gWriteLength(int aLength, java.io.OutputStream anOut)
        throws IOException
    {
        if (aLength <= 127)
            anOut.write(aLength);
        else
        {
            // Figure out how many octets are needed to encode the length.
            int count = 0;
            for (int l = aLength; l != 0; l >>>= 8)
                ++count;

            // Write the count.
            anOut.write(0x80 | count);

            // Write the octets for the length itself.
            int mask = 0xFF << ((count - 1) * 8);
            for (int i = count * 8; i > 0; i -= 8)
            {
                int octet = (aLength & mask) >>> (i - 8);
                anOut.write(octet);
                mask >>>= 8;
            }
        }
    }


    /**
     * Writes an encoding of this object to the stream, using the BER.
     *
     * @param anOut an output stream
     * @exception IOException if an I/O error occurs
     */
    /*
     * This implementation is responsible for encoding and decoding
     * the identifier and length. Subclasses are only responsible 
     * for providing the length and encoding and decoding the contents.
     */
    public final void writeTo(java.io.OutputStream anOut)
        throws IOException
    {
        // Write the identifier.
        getIdentifier().mWrite(anOut);

        // Write the length.
        gWriteLength(mGetLength(), anOut);
        
        // Write the contents.
        mWriteContents(anOut);
    }


    /**
     * Returns the size of the encoded contents, in bytes.
     * 
     * @return the size of the encoded contents, in bytes
     */
    protected abstract int mGetLength();


    /** 
     * Writes the encoded contents to the output stream.
     * 
     * @param anOut an output stream
     * @exception IOException if an I/O error occurs
     */
    protected abstract void mWriteContents(java.io.OutputStream anOut)
        throws IOException;


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
    protected abstract void mReadContents(java.io.InputStream anIn,
        BerModule aModule, BerIdentifier anIdentifier, int aLength) 
        throws IOException;


    /**
     * Returns the identifier for this object. A subclass may implement
     * more than one BER type, so getIdentifier may return different
     * and indeed unrelated identifiers when called on different instances
     * of the same subclass.
     * 
     * @return the identifier for this object
     */
    public abstract BerIdentifier getIdentifier();


    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object
     */
    public abstract String toString();
}

