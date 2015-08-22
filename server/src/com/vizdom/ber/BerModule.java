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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.vizdom.util.CharacterEncoder;
import com.vizdom.util.Debug;

/**
 * A BER module is a collection of types. All modules implicitly contain 
 * the universal types. Users can define multiple unrelated modules by
 * creating instances of BerModule. User types can be registered with a
 * module instance. All BER objects are read using a given module and 
 * only types defined within that module can be read.
 *
 * @author John Lacey
 * @version $Revision: 1.31 $
 */
public class BerModule
{
    /** Identifier for the end-of-contents marker. */
    static final BerIdentifier gEND_OF_CONTENTS_IDENTIFIER =
        new BerIdentifier(BerTypes.PRIMITIVE, BerTypes.END_OF_CONTENTS);
    
    /**
     * The BER end-of-contents marker. This class is identical to BerNull 
     * modulo the value of <code>gIDENTIFIER</code>, <code>toString()</code>
     * and comments.
     *
     * @see BerNull
     */
    /* 
     * ??? Should this be a subclass of BerNull?
     */
    public static final BerObject END_OF_CONTENTS = new BerObject() {
        public BerIdentifier getIdentifier() { 
            return gEND_OF_CONTENTS_IDENTIFIER; }
        protected int mGetLength() { return 0; }
        protected void mWriteContents(OutputStream anOut) {}
        protected void mReadContents(InputStream anIn, 
            BerModule aModule, BerIdentifier anIdentifier, int aLength) {
            // FIXME: The tests never execute this assertion.
            Debug.assertTrue(aLength == 0); }
        public String toString() { return "end-of-contents"; }
    };

    /** 
     * A single instance of BerNull used for decoding, and available
     * to clients for encoding efficiency.
     */
    public static final BerNull NULL = new BerNull();

    /** The BerObject factories tied to a particular BerIdentifier. */
    private Map<BerIdentifier, BerObjectFactory> mTiedFactories =
        new HashMap<BerIdentifier, BerObjectFactory>();

    /** The BerObject factories that may support multiple BerIdentifiers. */
    private List<BerObjectFactory> mUntiedFactories =
        new ArrayList<BerObjectFactory>();

    /** The character encoding in use for BER OCTET STRING values. */
    private String mCharacterEncoding;


    /** Constructor. */
    public BerModule()
    {
    }


    /**
     * Register a BerObject factory. This defines the types recognized
     * by the factory in this module.
     * 
     * @param aFactory a BER object factory
     * @throws NullPointerException if the factory is <code>null</code>
     */
    public void registerFactory(BerObjectFactory aFactory)
    {
        if (aFactory == null)
            throw new NullPointerException();
        mUntiedFactories.add(aFactory);
    }


    /**
     * Register a BerObject factory with a single type. This defines that 
     * type in this module.
     * 
     * @param aFactory a BER object factory
     * @param anIdentifier a BER identifier
     * @throws IllegalStateException if the type is already registered
     * @throws NullPointerException if the factory or identifier is
     *     <code>null</code>
     */
    public void registerFactory(BerObjectFactory aFactory,
        BerIdentifier anIdentifier)
    {
        if (aFactory == null || anIdentifier == null)
            throw new NullPointerException();
        Object previous = mTiedFactories.put(anIdentifier, aFactory);
        if (previous != null)
        {
            // ??? BerException?
            throw new IllegalStateException(
                "Type " + anIdentifier + " is already registered with " +
                previous);
        }
    }


    /**
     * Sets the character encoding for BER OCTET STRING values.
     * 
     * @param encoding a character encoding
     * @throws UnsupportedEncodingException if the encoding name is
     *     unknown or unsupported on the current platform
     */
    public void setCharacterEncoding(String encoding)
        throws UnsupportedEncodingException
    {
        // Make sure the character encoding is supported by this JVM.
        CharacterEncoder.toByteArray("hello", encoding);
        mCharacterEncoding = encoding;
    }


    /**
     * Gets the character encoding for BER OCTET STRING values.
     * 
     * @return a character encoding
     */
    public String getCharacterEncoding()
    {
        // ??? Instead of this, we should either assign a default encoding
        // (via file.encoding?) and handle null or "" in CharacterEncoder.
        if (Debug.ASSERT)
            Debug.assertTrue(mCharacterEncoding != null);

        return mCharacterEncoding;
    }


    /**
     * Returns a new BER object decoded from the input stream.
     * 
     * @param anIn an input stream
     * @return a new BER object decoded from the input stream, or
     *     <code>null</code> if <code>anIn</code> is at EOF.
     * @exception IOException if an I/O error occurs
     */
    public BerObject readFrom(InputStream anIn) throws IOException
    {
        // Read the identifier; gReadIdentifier will return
        // null if we are already at EOF on anIn. We return that null
        // since we are on an object boundary.
        BerIdentifier identifier = BerIdentifier.gReadIdentifier(anIn);
        if (identifier == null)
            return null;

        // Read the length.
        int length = BerObject.gReadLength(anIn);
        
        // Decide which BerObject to instantiate from the contents.
        // First, try the universal types.
        if (identifier.getTagClass() == BerTypes.UNIVERSAL)
        {
            BerObject o;
            switch (identifier.getTagNumber())
            {
            case BerTypes.BOOLEAN:
                o = new BerBoolean();
                break;

            case BerTypes.INTEGER:
                o = new BerInteger();
                break;

            case BerTypes.OCTET_STRING:
                o = new BerOctetString();
                break;

            case BerTypes.NULL:
                // Return the cached BerNull object.
                o = NULL;
                break;

            case BerTypes.ENUMERATED:
                o = new BerEnumerated();
                break;

            case BerTypes.SEQUENCE:
                o = new BerSequence();
                break;

            default:
                // ??? BerException?
                throw new IOException(
                    "Unimplemented BER built-in type: " + identifier);
            }
            o.mReadContents(anIn, this, identifier, length);
            return o;
        }
        else
        {
            // Try the factories, first the tied and then the untied.
            BerObjectFactory factory = mTiedFactories.get(identifier);
            if (factory == null)
            {
                for (int i = 0; i < mUntiedFactories.size(); i++)
                {
                    BerObjectFactory untiedFactory = mUntiedFactories.get(i);
                    if (untiedFactory.acceptsIdentifier(identifier))
                    {
                        factory = untiedFactory;
                        break;
                    }
                }
            }
            if (factory == null)
            {
                throw new IOException("Unrecognized BER object identifier: " +
                    identifier);
            }
            else
            {
                BerObject bero = factory.createBerObject();
                bero.mReadContents(anIn, this, identifier, length);
                return bero;
            }
        }
    }
}
