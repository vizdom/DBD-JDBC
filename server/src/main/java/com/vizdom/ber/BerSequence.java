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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import com.vizdom.util.Debug;
import com.vizdom.util.UnreachableCodeException;

/**
 * A BER sequence.
 *
 * @author: John Lacey
 * @version: $Revision: 1.17 $
 */
public class BerSequence extends BerObject
{
    /** The SEQUENCE identifier, [UNIVERSAL 16]. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.CONSTRUCTED, BerTypes.SEQUENCE);

    /** The underlying BerObject array of this BER SEQUENCE. */
    protected BerObject[] mSequence;

    /** The cached encoded contents. */
    private ByteArrayOutputStream mCachedContents;


    /** The decoding constructor. */
    protected BerSequence()
    {
    }


    /**
     * The encoding constructor. The array parameter is copied.
     * (The underlying BerObjects are immutable; they don't need
     * to be copied.)
     *
     * @param aSequence a BerObject array
     */
    public BerSequence(BerObject[] aSequence)
    {
        mSequence = new BerObject[aSequence.length];
        System.arraycopy(aSequence, 0, mSequence, 0, aSequence.length);
    }


    /**
     * Returns the BER identifier for BER SEQUENCE, [UNIVERSAL 16].
     * 
     * @return the BER identifier for BER SEQUENCE, [UNIVERSAL 16]
     */
    public BerIdentifier getIdentifier()
    {
        return gIDENTIFIER;
    }


    /**
     * Writes the encoded contents to a temporary cache 
     * (a ByteArrayOutputStream). This allows us to compute
     * the size of the contents in bytes, and gives us the 
     * actual contents to write out.
     */
    private synchronized void mCacheContents()
    {
        if (mCachedContents == null)
        {
            try
            {
                mCachedContents = new ByteArrayOutputStream();
                for (int i = 0; i < mSequence.length; i++)
                    mSequence[i].writeTo(mCachedContents);
            }
            catch (IOException e)
            {
                // ByteArrayOutputStreams don't throw IOExceptions
                // (except for BAOS.writeTo), but BerObject.writeTo
                // doesn't know that.
                throw new UnreachableCodeException();
            }
        }
    }


    /**
     * Returns the size of the encoded contents, in bytes.
     * 
     * @return the size of the encoded contents, in bytes
     */
    protected final int mGetLength()
    {
        if (mCachedContents == null)
            mCacheContents();
        return mCachedContents.size();
    }


    /** 
     * Writes the encoded contents to the output stream.
     * 
     * @param anOut an output stream
     * @exception IOException if an I/O error occurs
     */
    protected final void mWriteContents(OutputStream anOut) throws IOException
    {
        if (mCachedContents == null)
            mCacheContents();
        mCachedContents.writeTo(anOut);
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
        if (Debug.ASSERT && getClass() == BerSequence.class)
            Debug.assertTrue(anIdentifier == gIDENTIFIER); // [sic; not equals]

        // Construct a governed stream that only reads aLength bytes.
        BerContentsInputStream inContents = 
            new BerContentsInputStream(anIn, aLength);

        // We don't know how many elements are in the sequence.
        Vector<BerObject> beroV = new Vector<BerObject>();

        // readFrom will return null if the end of the stream is reached
        // on an object boundary.
        while (true)
        {
            BerObject bero = aModule.readFrom(inContents);
            if (bero == null)
                break;
            beroV.addElement(bero);
        }

        // Make sure we read as many bytes as we were supposed to.
        if (inContents.getCount() != aLength)
            throw new EOFException();

        // Copy the vector into our array field.
        mSequence = new BerObject[beroV.size()];
        beroV.copyInto(mSequence);
    }


    /**
     * Returns an enumeration of the underlying array of BER objects.
     *
     * @return an enumeration of the underlying array of BER objects
     */
    /* 
     * TODO: This method appears to be unused. Maybe it's used in the
     * DBI driver?
     */
    public Enumeration<?> elements()
    {
        return new BerObjectEnumeration(mSequence);
    }


    /**
     * Returns a copy of the underlying array of BER objects.
     *
     * @return a copy of the underlying array of BER objects
     */
    /*
     * XXX: Should we change this to asArray and not copy the array,
     * for efficiency?
     */
    public BerObject[] toArray()
    {
        BerObject[] sequence = new BerObject[mSequence.length];
        System.arraycopy(mSequence, 0, sequence, 0, mSequence.length);
        return sequence;
    }


    /**
     * Returns a string representation of this BER sequence.
     *
     * @return a string representation of this BER sequence
     */
    public String toString()
    {
        if (mSequence.length == 0)
            return "[]";
        else
        {
            StringBuffer buf = new StringBuffer();
            buf.append("[");
            buf.append(mSequence[0].toString());
            for (int i = 1; i < mSequence.length; i++) 
            {
                buf.append(", ");
                buf.append(mSequence[i].toString());
            }
            buf.append("]");
            return buf.toString();
        }
    }
}
