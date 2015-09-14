/*
 *  Copyright 1999-2001,2003-2005,2008 Vizdom Software, Inc. All Rights Reserved.
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
import com.vizdom.util.AssertionFailedException;
import com.vizdom.util.Debug;
import com.vizdom.util.UnreachableCodeException;

/**
 * A BER identifier, which consists of the tag (class and number) and form.
 * 
 * @author John Lacey
 * @version $Revision: 1.18 $
 */
public class BerIdentifier
{
    /** Tag class mask (bits 8, 7). */
    private static final int gTAG_CLASS_MASK = 0xC0;

    /** Form mask (bit 6). */
    private static final int gFORM_MASK = 0x20;

    /** 
     * Tag number mask (bits 5 - 1). Also used to check for 
     * a multiple octet identifier. 
     */
    private static final int gTAG_NUMBER_MASK = 0x1F;


    /** 
     * Reads an encoded identifier from the input stream.
     *
     * @param anIn an input stream
     * @return an instance of <code>BerIdentifier</code>, not necessarily
     *     a newly created one
     * @exception IOException if an I/O error occurs. In particular,
     *     an <code>EOFException</code> may be thrown if the end of
     *     stream is reached before the identifier has been fully read
     */
    static BerIdentifier gReadIdentifier(InputStream anIn) throws IOException
    {
        // If we are at EOF, return null rather than throwing an exception
        // because it happened on an object boundary.
        int octet = anIn.read();
        if (octet == -1)
            return null;

        int tagClass = octet & gTAG_CLASS_MASK;
        int form = octet & gFORM_MASK;
        int tagNumber = octet & gTAG_NUMBER_MASK;
        long encodedOctets = octet;
        if (tagNumber == gTAG_NUMBER_MASK)
        {
            tagNumber = 0;
            do
            {
                octet = anIn.read();
                if (octet == -1)
                    throw new EOFException();

                tagNumber = (tagNumber << 7) | (octet & 0x7F);
                encodedOctets = (encodedOctets << 8) | (octet & 0xFF);
            }
            while ((octet & 0x80) == 0x80);
        }

        if (tagClass == BerTypes.UNIVERSAL)
        {
            switch (tagNumber)
            {
            case BerTypes.END_OF_CONTENTS:
                return BerModule.gEND_OF_CONTENTS_IDENTIFIER;

            case BerTypes.INTEGER:
                return BerInteger.gIDENTIFIER;

            case BerTypes.OCTET_STRING:
                return BerOctetString.gIDENTIFIER;
            
            case BerTypes.NULL:
                return BerNull.gIDENTIFIER;
            
            case BerTypes.SEQUENCE:
                return BerSequence.gIDENTIFIER;

            default:
                throw new UnreachableCodeException(tagNumber);
            }
        }
        else
        {
            // Reverse the encoded octets.
            long reversedOctets = 0;
            while (encodedOctets != 0)
            {
                reversedOctets <<= 8;
                reversedOctets |= (encodedOctets & 0xFF);
                encodedOctets >>>= 8;
            }

            // This line creates more objects than any other. The
            // objects are short-lived and small, so a good
            // generational garbage collector like the one in Hotspot
            // probably makes this OK. This line takes about 1% of the
            // execution time in the JDBC driver. I experimented with
            // the flyweight pattern using a perfect hash of the
            // observed identifiers, which took essentially zero time.
            // With a fully-fledged hash table with int keys, the
            // overhead may match or even exceed the object
            // construction time.
            // TODO: Experiment with a complete hash table.
            return new BerIdentifier(tagClass, form, tagNumber,
                reversedOctets);
        }
    }


    /** The tag class. */
    private final int mTagClass;

    /** The form. */
    private final int mForm;

    /** 
     * The tag number. BER does not restrict the size of the tag
     * number, but this implementation restricts them to 32 bits.
     * (The ASN.1 book p. 82 says that tag numbers above 31 are rare.
     * This isn't true with raw BER usage, but 32 bits do seem like
     * plenty.)
     */
    private final int mTagNumber; // ??? long, or even BigInteger?

    /** 
     * The encoded octets, in reverse order. This value is used to
     * make writing and comparing identifiers fast. According to the
     * BER spec, the form should not be used when comparing identifiers
     * (mentioned as an aside in the ASN.1 book, p. 82). We don't
     * implement segmented strings but we do have clients that reuse
     * tag numbers with different forms in distinct types.     
     */
    private final long mEncodedOctets;
    

    /** 
     * One of the ASN.1 built-in types, with a tag class of UNIVERSAL.
     *
     * @param aForm the form, one of <code>PRIMITIVE</code> or
     *     <code>CONSTRUCTED</code>
     * @param aTagNumber the tag number
     */
    /* Only code inside the package can create a universal identifier. */
    BerIdentifier(int aForm, int aTagNumber)
    {
        if (Debug.ASSERT)
            Debug.assertTrue((aForm & ~gFORM_MASK) == 0);

        mTagClass = BerTypes.UNIVERSAL;
        mForm = aForm;
        mTagNumber = aTagNumber;
        mEncodedOctets = BerTypes.UNIVERSAL | aForm | aTagNumber;
    }


    /** 
     * The standard constructor.
     *
     * @param aTagClass the tag class, one of <code>APPLICATION</code>,
     *     <code>CONTEXT</code>, or <code>PRIVATE</code>
     * @param aForm the form, one of <code>PRIMITIVE</code> or
     *     <code>CONSTRUCTED</code>
     * @param aTagNumber the tag number
     */
    public BerIdentifier(int aTagClass, int aForm, int aTagNumber)
    {
        // N.B.: UNIVERSAL isn't permitted for user types.
        Debug.assertTrue(aTagClass != BerTypes.UNIVERSAL && 
            (aTagClass & ~gTAG_CLASS_MASK) == 0);
        Debug.assertTrue((aForm & ~gFORM_MASK) == 0);

        mTagClass = aTagClass;
        mForm = aForm;
        mTagNumber = aTagNumber;
        mEncodedOctets = mEncodeOctets();
    }


    /** 
     * An experimental constructor.
     *
     * @param aTagClass the tag class, one of <code>APPLICATION</code>,
     *     <code>CONTEXT</code>, or <code>PRIVATE</code>
     * @param aForm the form, one of <code>PRIMITIVE</code> or
     *     <code>CONSTRUCTED</code>
     * @param aTagNumber the tag number
     * @param anEncodedOctets the encoded octets in reverse order
     */
    private BerIdentifier(int aTagClass, int aForm, int aTagNumber, 
        long anEncodedOctets)
    {
        if (Debug.ASSERT)
        {
            // N.B.: UNIVERSAL is handled separately.
            Debug.assertTrue(aTagClass != BerTypes.UNIVERSAL && 
                (aTagClass & ~gTAG_CLASS_MASK) == 0);
            Debug.assertTrue((aForm & ~gFORM_MASK) == 0);
        }

        mTagClass = aTagClass;
        mForm = aForm;
        mTagNumber = aTagNumber;
        mEncodedOctets = anEncodedOctets;

        if (Debug.ASSERT)
        {
            long encodedOctets = mEncodeOctets();
            if (anEncodedOctets != encodedOctets)
            {
                throw new AssertionFailedException("Expected " + 
                    Long.toHexString(encodedOctets) + ", got " + 
                    Long.toHexString(anEncodedOctets));
            }
        }
    }


    /** 
     * Constructs the encoded octets from the tag and form.
     *
     * @return the encoded octets
     */
    private long mEncodeOctets()
    {
        int classForm = mTagClass | mForm;
        int tagNumber = mTagNumber;
        if (tagNumber < gTAG_NUMBER_MASK)
            return (classForm | tagNumber);
        else
        {
            // Reverse the tag number 7 bits at a time, adding a high
            // bit of 1 to all but the last septet, and 0 to the last.
            long reversedTagNumber = 0;
            boolean isFirst = true;
            while (tagNumber != 0)
            {
                int octet = (tagNumber & 0x7F);

                // The first byte in is the last byte in the identifier,
                // so it doesn't get a 1 in the high bit.
                if (isFirst)
                    isFirst = false;
                else
                    octet |= 0x80;

                reversedTagNumber <<= 8;
                reversedTagNumber |= octet;

                tagNumber >>>= 7;
            }

            // Write the tag class and form, with the extended tag number mask.
            reversedTagNumber <<= 8;
            reversedTagNumber |= (classForm | gTAG_NUMBER_MASK);

            return reversedTagNumber;
        }
    }


    /**
     * Writes this identifier to the output stream.
     *
     * @param anOut an output stream
     * @exception IOException if an I/O error occurs
     */
    void mWrite(OutputStream anOut) throws IOException
    {
        long encodedOctets = mEncodedOctets;

        // Write the first byte.
        int octet = (int) (encodedOctets & 0xFF);
        anOut.write(octet);
        encodedOctets >>>= 8;

        if ((octet & gTAG_NUMBER_MASK) == gTAG_NUMBER_MASK)
        {
            // It's a multiple octet identifier.
            // Write out each tag number byte from low byte to high byte.
            // All but the last byte have the high-bit set.
            do
            {
                octet = (int) (encodedOctets & 0xFF);
                anOut.write(octet);
                encodedOctets >>>= 8;
            }
            while ((octet & 0x80) == 0x80);
        }
    }


    /**
     * Gets the tag class of this identifier.
     *
     * @return the tag class of this identifier
     */
    public int getTagClass()
    {
        return mTagClass;
    }


    /**
     * Gets the form of this identifier.
     *
     * @return the form of this identifier
     */
    public int getForm()
    {
        return mForm;
    }


    /**
     * Gets the tag number of this identifier.
     *
     * @return the tag number of this identifier
     */
    public int getTagNumber()
    {
        return mTagNumber;
    }


    /**
     * Compares two <code>BerIdentifier</code> instances for equality.
     * Both the tag and form are considered for comparisons. The BER
     * spec calls for the form to be excluded, but it is not.
     *
     * @param anObject an object to compare to this one
     * @return <code>true</code> if this identifier is the same as 
     *     <code>anObject</code>; <code>false</code> otherwise.
     */
    public boolean equals(Object anObject)
    {
        if (anObject == null || !(anObject instanceof BerIdentifier))
            return false;
        else
        {
            long encodedOctets = ((BerIdentifier) anObject).mEncodedOctets;
            return mEncodedOctets == encodedOctets;
        }
    }


    /**
     * Returns a hash code value for this identifier. Both the tag and
     * form are used to construct the hash code, since
     * <code>equals</code> uses them both.
     *
     * @return a hash code value for this identifier
     */
    /*
     *  (This is what Long.hashCode returns.) We could just
     *  return the lower four bytes, but in most cases this value is
     *  exactly that, and if you do have huge tag numbers, you
     *  probably have more than one, so this helps hash such
     *  groups more efficiently.
     */
    public int hashCode()
    {
        return (int) (mEncodedOctets ^ (mEncodedOctets >> 32));
    }


    /**
     * Returns a human-readable string representing this identifier.
     * The syntax mirrors ASN.1 tag syntax, with the form added in.
     *
     * @return a human-readable String representing this identifier
     */
    public String toString()
    {
        // ??? Eliminate this entire method in non-debug builds?

        StringBuffer buffer = new StringBuffer();
        buffer.append('[');
        switch (mTagClass)
        {
        case BerTypes.UNIVERSAL: buffer.append("UNIVERSAL "); break;
        case BerTypes.APPLICATION: buffer.append("APPLICATION "); break;
        case BerTypes.CONTEXT: break;
        case BerTypes.PRIVATE: buffer.append("PRIVATE "); break;
        default:
            throw new UnreachableCodeException(mTagClass);
        }
        buffer.append(mTagNumber);
        switch (mForm)
        {
        case BerTypes.PRIMITIVE: buffer.append(" primitive]"); break;
        case BerTypes.CONSTRUCTED: buffer.append(" constructed]"); break;
        default:
            throw new UnreachableCodeException(mForm);
        }
        return buffer.toString();
    }
}
