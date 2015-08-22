/*
 * Copyright 1999-2001 Vizdom Software, Inc. All Rights Reserved.
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

package com.vizdom.dbd.jdbc;

import com.vizdom.ber.*;
import com.vizdom.util.Debug;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;


/**
 * This class treats a BerSequence as a sequence of name/value pairs
 * and returns them to the user as a Hashtable.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.9 $
 */
class BerHash extends BerSequence
{
    /* Each BerObject in the sequence is converted to a String using
     * its toString method. Alternating objects are used as hash keys
     * and values. In the future, values may be allowed to be of
     * other types.
     */

    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER =
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED,
        BerDbdModule.gBER_HASH);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = new BerObjectFactory() {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }

        public BerObject createBerObject()
        {
            return new BerHash();
        }
    };

    /** The Hashtable constructed from the BerSequence. */
    private Hashtable<String, String> mHashtable;

    /**
     * The decoding constructor.
     */
    private BerHash()
    {
        super();
    }

    /**
     * Reads the encoded contents from the input stream.
     *
     * @param anIn an input stream
     * @param aModule a BER module for reading constructed encodings
     * @param anIdentifier the BER identifier of the encoded object
     * @param aLength the length in bytes of the encoded contents
     * @exception IOException if an error occurs while reading
     * @exception AssertionFailedException if the length of the
     *      sequence is not even
     */
    protected void mReadContents(java.io.InputStream anIn,
        BerModule aModule, BerIdentifier anIdentifier, int aLength)
        throws java.io.IOException
    {
        super.mReadContents(anIn, aModule, anIdentifier, aLength);
        Debug.assertTrue(mSequence.length % 2 == 0);
    }

    /**
     * Returns a Hashtable consisting of String name/value pairs,
     * constructed by treating each pair of items in the sequence as a
     * name/value pair.
     *
     * @return a Hashtable containing the sequence elements
     */
    /* A BerHash is expected a sequence of BerOctetString objects. This
     * expectation will probably be relaxed at some point.
     */
    Hashtable<String, String> toHashtable()
    {
        if (mHashtable == null)
        {
            mHashtable = new Hashtable<String, String>();

            for (int i = 0; i < mSequence.length; i += 2)
            {
                mHashtable.put(mSequence[i].toString(), 
                    mSequence[i + 1].toString());
            }
        }
        return mHashtable;
    }


    /**
     * Constructs a Hashtable from the underlying sequence, using
     * the indicated character encoding to convert the
     * BerOctetString elements in the sequence to Java Strings.
     *
     * @param aCharacterEncoding a character encoding name
     * @return a Hashtable containing the sequence elements
     * @exception UnsupportedEncodingException if the encoding is unknown
     */
    Hashtable<String, String> toHashtable(String aCharacterEncoding)
        throws UnsupportedEncodingException
    {
        Hashtable<String, String> h = new Hashtable<String, String>();
        for (int i = 0; i < mSequence.length; i += 2)
        {
            h.put(((BerOctetString) mSequence[i]).toString(aCharacterEncoding), 
                ((BerOctetString) mSequence[i + 1])
                .toString(aCharacterEncoding));
        }
        return h;
    }


    /**
     * Returns the identifier for this BerObject. 
     *
     * @return the identifier for this BerObject
     */
    public BerIdentifier getIdentifier()
    {
        return gIDENTIFIER;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString()
    {
        return "Hash";
    }
}

