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

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import com.vizdom.util.CharacterEncoder;
import com.vizdom.util.Debug;
import com.vizdom.util.UnreachableCodeException;

/**
 * A BER OCTET STRING.
 *
 * @author: John Lacey
 * @version: $Revision: 1.23 $
 */
public class BerOctetString extends BerObject
{
    /** The OCTET STRING identifier, [UNIVERSAL 4]. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.PRIMITIVE, BerTypes.OCTET_STRING);

    /** The underlying byte array of this BER OCTET STRING. */
    private byte[] mByteArray;

    /** 
     * The String value associated with <code>mByteArray</code>. This
     * is assigned as late as possible to avoid pointlessly encoding
     * binary data.
     */
    private String mString;

    /**
     * The character encoding to be used if we need to convert
     * <code>mByteArray</code> to a string. If this is null, the
     * platform default encoding will be used.
     */
    private String mCharacterEncoding;
    

    /** The decoding constructor. */
    protected BerOctetString()
    {
    }


    /** 
     * An encoding constructor. The given character encoding is
     * used to convert the string to an array of bytes. Using 
     * <code>new BerOctetString(s, enc)</code> is a shortcut for
     * <code>new BerOctetString(s.getBytes(enc))</code>.
     * 
     * @param aString a string
     * @param aCharacterEncoding a character encoding
     * @exception UnsupportedEncodingException if the 
     *      encoding name is unknown or unsupported on the current platform
     */
    public BerOctetString(String aString, String aCharacterEncoding)
        throws UnsupportedEncodingException
    {
        mByteArray = CharacterEncoder.toByteArray(aString, aCharacterEncoding);
        mString = aString;
    }


    /** 
     * An encoding constructor. The given character encoding is used
     * to convert the byte array to a String (for <code>toString()</code>).
     * 
     * @param aByteArray a byte array
     * @param aCharacterEncoding a character encoding
     * @exception UnsupportedEncodingException if the 
     *     encoding name is unknown or unsupported on the current platform
     */
    public BerOctetString(byte[] aByteArray, String aCharacterEncoding)
        throws UnsupportedEncodingException
    {
        mByteArray = aByteArray;
        mCharacterEncoding = aCharacterEncoding;
        mString = null;
    }


    /** 
     * An encoding constructor. The default character encoding is used
     * to convert the byte array to a String (for <code>toString()</code>).
     * 
     * @param aByteArray a byte array
     */
    public BerOctetString(byte[] aByteArray)
    {
        mByteArray = aByteArray;

        // We will probably never need a string, so avoid creating it now.
        mString = null;
        mCharacterEncoding = null;
    }


    /**
     * Returns the BER identifier for BER OCTET STRING, [UNIVERSAL 4].
     * 
     * @return the BER identifier for BER OCTET STRING, [UNIVERSAL 4]
     */
    /*
     * This may return one of two identifiers (with the same tag) if
     * constructed string encodings are implemented.
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
        return mByteArray.length;
    }


    /** 
     * Writes the encoded contents to the output stream.
     * 
     * @param anOut an output stream
     * @exception IOException if an I/O error occurs
     */
    protected final void mWriteContents(OutputStream anOut) throws IOException
    {
        anOut.write(mByteArray);
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
        if (Debug.ASSERT && getClass() == BerOctetString.class)
            Debug.assertTrue(anIdentifier == gIDENTIFIER); // [sic; not equals]

        // Read exactly aLength bytes (which may be more than are
        // available all at once). Any EOF is unexpected.
        mByteArray = new byte[aLength];
        int count;
        for (int offset = 0; offset < aLength; offset += count)
        {
            count = anIn.read(mByteArray, offset, aLength - offset);
            if (count == -1)
                throw new EOFException(); // ??? BerException?
        }

        mCharacterEncoding = aModule.getCharacterEncoding();
        mString = null;
    }


    /**
     * Returns the byte array held by this object. Changes made to the
     * returned array will be reflected in the contents of this string.
     * 
     * @return the byte array held by this object
     */
    public byte[] toByteArray()
    {
        return mByteArray;
    }


    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object
     */
    /* 
     * This string had to be created earlier because we can't throw
     * UnsupportedEncodingException here.
     */
    public synchronized String toString()
    {
        if (mString == null)
        {
            if (mByteArray.length == 0)
                mString = "";
            else if (mCharacterEncoding == null)
                mString = new String(mByteArray);
            else
            {
                try
                {
                    mString = CharacterEncoder.toString(mByteArray,
                        mCharacterEncoding);
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new UnreachableCodeException(e);
                }
            }
        }

        return mString;
    }


    /**
     * Returns a string representation of the object. The given
     * character encoding is used to create a new String object
     * from the bytes.
     * 
     * @param aCharacterEncoding a character encoding
     * @return a string representation of the object
     * @exception UnsupportedEncodingException if the 
     *      encoding name is unknown or unsupported on the current platform
     */
    /* 
     * We don't try to cache these, which may be called with different
     * character encodings.
     *
     * TODO: This method appears to be unused. Maybe it's used in the
     * DBI driver?
     */
    public String toString(String aCharacterEncoding) 
        throws UnsupportedEncodingException
    {
        return CharacterEncoder.toString(mByteArray, aCharacterEncoding);
    }
}

