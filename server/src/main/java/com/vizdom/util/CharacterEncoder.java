/*
 * Copyright 1999-2005,2008-2009 Vizdom Software, Inc. All Rights Reserved.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the same terms as the Perl Kit, namely, under 
 * the terms of either:
 *
 *     a) the GNU General Public License as published by the Free
 *     Software Foundation; either version 1 of the License, or 
 *     (at your option) any later version, or
 *
 *     b) the "Artistic License" that comes with the Perl Kit.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See either
 * the GNU General Public License or the Artistic License for more 
 * details.
 */

package com.vizdom.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import com.vizdom.util.UnreachableCodeException;
import org.apache.log4j.Logger;

/**
 * This class provides an interface to character encoding
 * functionality.  As of Java 1.4, you can add new character sets
 * using java.nio.charset.spi.CharsetProvider, but as noted in
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4619777,
 * this only works if you can add them to a location accessible
 * by the system class loader. In particular, you can't add them
 * to a web application.
 * <p>
 * Note that this class only works for single-byte character
 * encodings.
 * <p>
 * The great comedy here is that this class is not currently extensible,
 * either. It is hard-coded for the extra character encodings we happen
 * to need.
 *   
 * @author Gennis Emerson
 * @version $Revision: 1.31 $
 */
public abstract class CharacterEncoder implements CharacterEncodable
{
    /** Log instance for this class. */
    private static final Logger gLog =
        Logger.getLogger(CharacterEncoder.class);

    /** A cache of instantiated encoders. */
    private static HashMap<String, CharacterEncoder> gEncoders;


    static
    {
        // All supported non-Java encodings must be listed here.
        gEncoders = new HashMap<String, CharacterEncoder>();
        gEncoders.put("DEC_MCS", null);
        gEncoders.put("HP_Roman8", null);
        gEncoders.put("HP_Roman9", null);
        gEncoders.put("Europa_3", null);
    }


    /**
     * Returns the name of the platform's default encoding.
     *
     * @return the name of the default character encoding on 
     *      this platform
     */
    public static String getDefaultEncoding()
    {
        byte[] b = new byte[0];
        InputStreamReader reader = 
            new InputStreamReader(new ByteArrayInputStream(b));
        String encoding = reader.getEncoding();
        try { reader.close(); } catch (IOException e) {}
        return encoding;
    }

    
    /**
     * Converts the given string to a byte array, using the supplied 
     * encoding.
     *
     * @param aString a string to be converted to bytes
     * @param anEncoding an encoding name
     * @return the bytes corresponding to the characters in the string,
     *      using the given encoding
     * @exception UnsupportedEncodingException if the 
     *      encoding name is unknown or unsupported on the current platform
     */
    public static byte[] toByteArray(String aString, String anEncoding) 
        throws UnsupportedEncodingException
    {
        CharacterEncoder enc = gGetEncoder(anEncoding);
        if (enc == null)
            return aString.getBytes(anEncoding);
        else
            return enc.gConvertAll(aString);
    }


    /**
     * Converts the given byte array to a string, using the supplied 
     * encoding.
     *
     * @param aByteArray a byte array to be converted to a string
     * @param anEncoding an encoding name
     * @return the string corresponding to the bytes,
     *      using the given encoding
     * @exception UnsupportedEncodingException if the 
     *      encoding name is unknown or unsupported on the current platform
     */
    public static String toString(byte[] aByteArray, String anEncoding)
        throws UnsupportedEncodingException
    {
        CharacterEncoder enc = gGetEncoder(anEncoding);
        if (enc == null)
            return new String(aByteArray, anEncoding);
        else
            return enc.gConvertAll(aByteArray);
    }


    /**
     * Converts part of the given byte array to a string.
     *
     * @param aByteArray a byte array
     * @param anOffset the offset into the byte array at which to begin 
     *      converting bytes
     * @param aLength the number of bytes to convert
     * @param anEncoding an encoding name
     * @return the string corresponding to the bytes,
     *      using the given encoding
     * @exception UnsupportedEncodingException if the 
     *      encoding name is unknown or unsupported on the current platform
     */
    public static String toString(byte[] aByteArray, int anOffset, int aLength,
        String anEncoding) throws UnsupportedEncodingException
    {
        CharacterEncoder enc = gGetEncoder(anEncoding);
        if (enc == null)
            return new String(aByteArray, anOffset, aLength, anEncoding);
        else
            return enc.gConvert(aByteArray, anOffset, aLength);
    }


    /**
     * Wraps the supplied OutputStream in a Writer using the given character
     * encoding.
     *
     * @param anOut an OutputStream
     * @param anEncoding an encoding name
     * @return a Writer which writes to the given OutputStream
     *      using the given encoding
     * @exception UnsupportedEncodingException if the 
     *      encoding name is unknown or unsupported on the current platform
     */
    public static Writer toWriter(OutputStream anOut, 
        String anEncoding) throws UnsupportedEncodingException
    {
        CharacterEncoder enc = gGetEncoder(anEncoding);
        if (enc == null)
            return new OutputStreamWriter(anOut, anEncoding);
        else
            return new CharacterEncoderWriter(anOut, enc);
    }


    /**
     * Wraps the supplied InputStream in a Reader using the given character
     * encoding.
     *
     * @param anIn an InputStream
     * @param anEncoding an encoding name
     * @return a Reader which reads from the given InputStream
     *      using the given encoding
     * @exception UnsupportedEncodingException if the 
     *      encoding name is unknown or unsupported on the current platform
     */
    public static Reader toReader(InputStream anIn, 
        String anEncoding) throws UnsupportedEncodingException
    {
        CharacterEncoder enc = gGetEncoder(anEncoding);
        if (enc == null)
            return new InputStreamReader(anIn, anEncoding);
        else
            return new CharacterEncoderReader(anIn, enc);
    }


    /**
     * Returns a CharacterEncodable instance for the given
     * encoding. A CharacterEncodable is in a way a stub
     * implementation of java.nio.charset.CharsetEncoder
     * providing only the canEncode method.
     *
     * @param anEncoding an encoding name
     * @return a CharacterEncodable using the given encoding
     * @throws UnsupportedEncodingException if the 
     *      encoding name is unknown or unsupported on the current platform
     */
    public static CharacterEncodable getCharacterEncodable(String anEncoding)
        throws UnsupportedEncodingException
    {
        CharacterEncoder enc = gGetEncoder(anEncoding);
        if (enc == null)
        {
            try
            {
                return new CharsetEncodable(anEncoding);
            }
            catch (UnsupportedCharsetException e)
            {
                UnsupportedEncodingException ue = 
                    new UnsupportedEncodingException(anEncoding);
                ue.initCause(e);
                throw ue;
            }
        }
        else
            return (CharacterEncodable) enc;
    }


    /**
     * Returns a CharacterEncoder which uses the given encoding, if the
     * encoding name is a custom encoding. Returns <code>null</code>
     * otherwise.
     *
     * @param anEncoding an encoding name
     * @return a CharacterEncoder which uses the given encoding, or
     *     <code>null</code> if the encoding is not a custom encoding (for
     *     example, if the encoding is a standard Java encoding)
     */
    private static synchronized CharacterEncoder gGetEncoder(String anEncoding)
    {
        CharacterEncoder enc = gEncoders.get(anEncoding);

        // All supported encodings must have an entry in the map, which
        // may be null. Java or unknown encodings will not be in the map.
        if (enc == null && gEncoders.containsKey(anEncoding))
        {
            if (anEncoding.equals("DEC_MCS"))
            {
                enc = new CharacterEncoderDecMcs();
                gEncoders.put(anEncoding, enc);
            }
            else if (anEncoding.equals("HP_Roman8"))
            {
                enc = new CharacterEncoderHpRoman8();
                gEncoders.put(anEncoding, enc);
            }
            else if (anEncoding.equals("HP_Roman9"))
            {
                enc = new CharacterEncoderHpRoman9();
                gEncoders.put(anEncoding, enc);
            }
            else if (anEncoding.equals("Europa_3"))
            {
                enc = new CharacterEncoderEuropa3();
                gEncoders.put(anEncoding, enc);
            }
            else
                throw new UnreachableCodeException();
        }
        return enc;
    }


    /** The byte to character mapping. */
    protected int[] mByteToCharTable;

    /**
     * The character to byte mapping. This table applies to characters with
     * Unicode values below 256.
     */
    protected int[] mCharToByteTable;

    /** The Unicode characters in the encoding with values over 255. */
    protected char[] mCharExceptions;

    /**
     * The byte values corresponding to the Unicode characters in the
     * encoding with values over 255.
     */
    protected byte[] mByteExceptions;

    /** 
     * This character is used when there's no mapping for a byte value.
     * This should never happen; the tables should always have all 256
     * values defined.
     */
    protected char mUnicodeUnknownCharacter = '\ufffd';

    /**
     * This byte is sent to the server if a Unicode character is provided
     * for which no server byte is known. This should only happen
     * if a character above 255 is provided; other cases should already be 
     * handled in the mapping tables.
     */
    protected byte mNativeUnknownCharacter;

    // ??? Are the rest of these methods supposed to be private, protected,
    // package, or public? (The class used to be package, so they could have
    // been intended as any of the above.)


    /** 
     * Converts a string to bytes.
     *
     * @param aString a string to be converted
     * @return the bytes corresponding to the characters in the string
     */
    private byte[] gConvertAll(String aString)
    {
        byte[] bytes = new byte[aString.length()];
        for (int i = 0; i < aString.length(); i++)
            bytes[i] = getNative(aString.charAt(i));
        return bytes;
    }


    /**
     * Converts a character array to bytes. This is useful for a Writer.
     *
     * @param aCharacterArray an array of characters to be converted
     *      to bytes
     * @return the bytes corresponding to the given characters
     */
    private byte[] gConvertAll(char[] aCharacterArray)
    {
        return gConvert(aCharacterArray, 0, aCharacterArray.length);
    }


    /**
     * Converts a character array to bytes. This is useful for a Writer.
     *
     * @param aCharacterArray an array of characters to be converted
     *      to bytes
     * @param anOffset the offset in the character array at which to begin
     *      converting characters
     * @param aLength the number of characters to convert
     * @return the bytes corresponding to the given characters
     * @exception ArrayIndexOutOfBoundsException if the offset or
     *      length are invalid
     */
    final byte[] gConvert(char[] aCharacterArray, int anOffset, int aLength)
    {
        byte[] bytes = new byte[aLength];
        for (int i = anOffset; i < anOffset + aLength; i++)
            bytes[i] = getNative(aCharacterArray[i]);
        return bytes;
    }

    
    /**
     * Converts a byte array to a string.
     *
     * @param aByteArray the byte array to be converted
     * @return the String corresponding to the given bytes
     */
    private String gConvertAll(byte[] aByteArray)
    {
        return gConvert(aByteArray, 0, aByteArray.length);
    }


    /**
     * Converts part of the given byte array to a string.
     *
     * @param aByteArray a byte array
     * @param anOffset the offset into the byte array at which to begin 
     *      converting bytes
     * @param aLength the number of bytes to convert
     * @return the string corresponding to the bytes,
     *      using the given encoding
     */
    private String gConvert(byte[] aByteArray, int anOffset, int aLength)
    {
        StringBuffer buffer = new StringBuffer(aLength);
        for (int i = anOffset; i < anOffset + aLength; i++)
            buffer.append(getUnicode(aByteArray[i]));
        return buffer.toString();
    }


    /**
     * Returns the native byte representation of the given character.
     *
     * @param aCharacter a Unicode character
     * @return the native byte representation of the given character,
     *      or a default value if no encoding is known
     */
    private byte getNative(char aCharacter)
    {
        int index = (int) aCharacter;
        if (index < mCharToByteTable.length)
            return (byte) (mCharToByteTable[index] & 0xFF);
        else if (mCharExceptions != null)
        {
            for (int i = 0; i < mCharExceptions.length; i++)
            {
                if (mCharExceptions[i] == aCharacter)
                    return mByteExceptions[i];
            }
            return mNativeUnknownCharacter;
        }
        else
            return mNativeUnknownCharacter;
    }


    /**
     * Returns the Unicode character corresponding to the given byte.
     *
     * @param aByte a native byte value
     * @return the Unicode character corresponding to the given byte,
     *      or a default value if no encoding is known
     */
    final char getUnicode(byte aByte)
    {
        int index = aByte & 0xFF;
        return (char) mByteToCharTable[index];
    }


    /**
     * Returns true if the given character can be encoded in the
     * current encoding.
     *
     * @param aCharacter a character to test
     * @return true if the character can be encoded, false otherwise
     * @see java.nio.charset.CharsetEncoder#canEncode
     */
    public boolean canEncode(char aCharacter)
    {
        int index = (int) aCharacter;
        if (index < mCharToByteTable.length && 
            (((byte) aCharacter) == mNativeUnknownCharacter))
        {
            return true;
        }
        return getNative(aCharacter) != mNativeUnknownCharacter;
    }
}


/**
 * Embodies the DEC_MCS character encoding. The encoding tables were taken
 * from the Resource File for CMLIB (cmlib.rc), modified to include
 * mappings from http://anubis.dkuug.dk/i18n/charmaps/HP-ROMAN8 for 0x80
 * through 0x9F, as well as 0xD7 (u0152), 0xDD (u0178)and 0xF7 (u0153).
 *
 * @author John Lacey
 */
class CharacterEncoderDecMcs extends CharacterEncoder 
{
    /** The server-to-client map. */
    private static final int[] gServerToClient = {
        0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
        0x0008, 0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F,
        0x0010, 0x0011, 0x0012, 0x0013, 0x0014, 0x0015, 0x0016, 0x0017,
        0x0018, 0x0019, 0x001A, 0x001B, 0x001C, 0x001D, 0x001E, 0x001F,
        0x0020, 0x0021, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0027,
        0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F,
        0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
        0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,

        0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
        0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F,
        0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057,
        0x0058, 0x0059, 0x005A, 0x005B, 0x005C, 0x005D, 0x005E, 0x005F,
        0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
        0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F,
        0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077,
        0x0078, 0x0079, 0x007A, 0x007B, 0x007C, 0x007D, 0x007E, 0x007F,

        0x0080, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087,
        0x0088, 0x0089, 0x008A, 0x008B, 0x008C, 0x008D, 0x008E, 0x008F,
        0x0090, 0x0091, 0x0092, 0x0093, 0x0094, 0x0095, 0x0096, 0x0097,
        0x0098, 0x0099, 0x009A, 0x009B, 0x009C, 0x009D, 0x009E, 0x009F,
        0x002E, 0x00A1, 0x00A2, 0x00A3, 0x002E, 0x00A5, 0x002E, 0x00A7,
        0x00A4, 0x00A9, 0x00AA, 0x00AB, 0x002E, 0x002E, 0x002E, 0x002E,
        0x00B0, 0x00B1, 0x00B2, 0x00B3, 0x002E, 0x00B5, 0x00B6, 0x00B7,
        0x002E, 0x00B9, 0x00BA, 0x00BB, 0x00BC, 0x00BD, 0x002E, 0x00BF,

        0x00C0, 0x00C1, 0x00C2, 0x00C3, 0x00C4, 0x00C5, 0x00C6, 0x00C7,
        0x00C8, 0x00C9, 0x00CA, 0x00CB, 0x00CC, 0x00CD, 0x00CE, 0x00CF,
        0x002E, 0x00D1, 0x00D2, 0x00D3, 0x00D4, 0x00D5, 0x00D6, 0x0152,
        0x00D8, 0x00D9, 0x00DA, 0x00DB, 0x00DC, 0x0178, 0x002E, 0x00DF,
        0x00E0, 0x00E1, 0x00E2, 0x00E3, 0x00E4, 0x00E5, 0x00E6, 0x00E7,
        0x00E8, 0x00E9, 0x00EA, 0x00EB, 0x00EC, 0x00ED, 0x00EE, 0x00EF,
        0x002E, 0x00F1, 0x00F2, 0x00F3, 0x00F4, 0x00F5, 0x00F6, 0x0153,
        0x00F8, 0x00F9, 0x00FA, 0x00FB, 0x00FC, 0x00FF, 0x002E, 0x002E,
    };

    /** The client-to-server map. */
    private static final int[] gClientToServer = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
        0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
        0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
        0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,

        0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
        0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
        0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
        0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67,
        0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
        0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77,
        0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F,

        0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
        0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F,
        0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97,
        0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F,
        0x2E, 0xA1, 0xA2, 0xA3, 0xA8, 0xA5, 0x2E, 0xA7,
        0x2E, 0xA9, 0xAA, 0xAB, 0x2E, 0x2E, 0x2E, 0x2E,
        0xB0, 0xB1, 0xB2, 0xB3, 0x2E, 0xB5, 0xB6, 0xB7,
        0x2E, 0xB9, 0xBA, 0xBB, 0xBC, 0xBD, 0x2E, 0xBF,

        0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7,
        0xC8, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE, 0xCF,
        0x2E, 0xD1, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0x2E,
        0xD8, 0xD9, 0xDA, 0xDB, 0xDC, 0x2E, 0x2E, 0xDF,
        0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7,
        0xE8, 0xE9, 0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF,
        0x2E, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0x2E,
        0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0x2E, 0x2E, 0xFD,
    };
    
    /** 
     * The character to send to the server in place of unmapped
     * Unicode characters.
     */
    private static final byte gUnknownCharacter = (byte) 0x2e;
    

    /**
     * Constructor. Initializes the mapping tables. 
     */
    CharacterEncoderDecMcs()
    {
        mCharToByteTable = gClientToServer;
        mCharExceptions = new char[] { '\u0152', '\u0178', '\u0153' };
        mByteExceptions = new byte[] { (byte) 0xD7, (byte) 0xDD, (byte) 0xF7 };
        mByteToCharTable = gServerToClient;
        mNativeUnknownCharacter = gUnknownCharacter;
    }
}


/**
 * Embodies the HP Roman-8 character encoding. The encoding tables were
 * taken from the Resource File for CMLIB (cmlib.rc), modified to include
 * mappings from http://anubis.dkuug.dk/i18n/charmaps/DEC-MCS for 0x80
 * through 0x9F, as well as 0xA9 (u02CB), 0xAA (u02C6), 0xAC (u02DC), 0xAF
 * (u20A4), 0xBE (u0192), 0xEB (u0160), 0xEC (u0161), 0xEE (u0178), 0xF6
 * (should be u2014; is u00AD), and 0xFC (u25A0).
 *
 * <p>
 *
 * I haven't found a definitive references for HP Roman-8. In particular,
 * there is a conflict for 0xF6 -- is it u00AD (soft hyphen) or u2014
 * (em-dash)? At HP, I found a description of the characters, but no code
 * points:
 * 
 * http://www.hp.com/cposupport/printers/support_doc/bpl02461.html
 * 
 * In support of the em-dash character, I have found the following:
 * 
 * http://anubis.dkuug.dk/i18n/charmaps/HP-ROMAN8
 * 
 * http://groups.google.com/groups?q=HP+roman-8+roman8&start=10&hl=en&rnum=12&selm=MAGUIRE.91Mar25185349%40cs.cs.columbia.edu
 * 
 * http://oss.software.ibm.com/cvs/icu/charset/data/ucm/glibc-HP_ROMAN8-2.1.2.ucm?rev=1.1&content-type=text/x-cvsweb-markup
 * 
 * And, in support of the soft hyphen, there is the original BASIS
 * translation table, and this references:
 * 
 * http://www.kostis.net/charsets/hproman8.htm
 *
 * @author John Lacey
 */
class CharacterEncoderHpRoman8 extends CharacterEncoder
{
    /** The server-to-client map. */
    private static final int[] gServerToClient = {
        0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
        0x0008, 0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F,
        0x0010, 0x0011, 0x0012, 0x0013, 0x0014, 0x0015, 0x0016, 0x0017,
        0x0018, 0x0019, 0x001A, 0x001B, 0x001C, 0x001D, 0x001E, 0x001F,
        0x0020, 0x0021, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0027,
        0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F,
        0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
        0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,

        0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
        0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F,
        0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057,
        0x0058, 0x0059, 0x005A, 0x005B, 0x005C, 0x005D, 0x005E, 0x005F,
        0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
        0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F,
        0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077,
        0x0078, 0x0079, 0x007A, 0x007B, 0x007C, 0x007D, 0x007E, 0x007F,

        0x0080, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087,
        0x0088, 0x0089, 0x008A, 0x008B, 0x008C, 0x008D, 0x008E, 0x008F,
        0x0090, 0x0091, 0x0092, 0x0093, 0x0094, 0x0095, 0x0096, 0x0097,
        0x0098, 0x0099, 0x009A, 0x009B, 0x009C, 0x009D, 0x009E, 0x009F,
        0x00A0, 0x00C0, 0x00C2, 0x00C8, 0x00CA, 0x00CB, 0x00CE, 0x00CF,
        0x00B4, 0x02CB, 0x02C6, 0x00A8, 0x02DC, 0x00D9, 0x00DB, 0x20A4,
        0x00AF, 0x00DD, 0x00FD, 0x00B0, 0x00C7, 0x00E7, 0x00D1, 0x00F1,
        0x00A1, 0x00BF, 0x00A4, 0x00A3, 0x00A5, 0x00A7, 0x0192, 0x00A2,

        0x00E2, 0x00EA, 0x00F4, 0x00FB, 0x00E1, 0x00E9, 0x00F3, 0x00FA,
        0x00E0, 0x00E8, 0x00F2, 0x00F9, 0x00E4, 0x00EB, 0x00F6, 0x00FC,
        0x00C5, 0x00EE, 0x00D8, 0x00C6, 0x00E5, 0x00ED, 0x00F8, 0x00E6,
        0x00C4, 0x00EC, 0x00D6, 0x00DC, 0x00C9, 0x00EF, 0x00DF, 0x00D4,
        0x00C1, 0x00C3, 0x00E3, 0x00D0, 0x00F0, 0x00CD, 0x00CC, 0x00D3,
        0x00D2, 0x00D5, 0x00F5, 0x0160, 0x0161, 0x00DA, 0x0178, 0x00FF,
        0x00DE, 0x00FE, 0x00B7, 0x00B5, 0x00B6, 0x00BE, 0x2014, 0x00BC,
        0x00BD, 0x00AA, 0x00BA, 0x00AB, 0x25A0, 0x00BB, 0x00B1, 0x002E,
    };
    
    /** The client-to-server map. */
    private static final int[] gClientToServer = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
        0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
        0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
        0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,

        0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
        0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
        0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
        0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67,
        0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
        0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77,
        0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F,

        0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
        0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F,
        0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97,
        0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F,
        0xA0, 0xB8, 0xBF, 0xBB, 0xBA, 0xBC, 0x2E, 0xBD,
        0xAB, 0x2E, 0xF9, 0xFB, 0x2E, 0x2E, 0x2E, 0xB0,
        0xB3, 0xFE, 0x2E, 0x2E, 0xA8, 0xF3, 0xF4, 0xF2,
        0x2E, 0x2E, 0xFA, 0xFD, 0xF7, 0xF8, 0xF5, 0xB9,

        0xA1, 0xE0, 0xA2, 0xE1, 0xD8, 0xD0, 0xD3, 0xB4,
        0xA3, 0xDC, 0xA4, 0xA5, 0xE6, 0xE5, 0xA6, 0xA7,
        0xE3, 0xB6, 0xE8, 0xE7, 0xDF, 0xE9, 0xDA, 0x2E,
        0xD2, 0xAD, 0xED, 0xAE, 0xDB, 0xB1, 0xF0, 0xDE,
        0xC8, 0xC4, 0xC0, 0xE2, 0xCC, 0xD4, 0xD7, 0xB5,
        0xC9, 0xC5, 0xC1, 0xCD, 0xD9, 0xD5, 0xD1, 0xDD,
        0xE4, 0xB7, 0xCA, 0xC6, 0xC2, 0xEA, 0xCE, 0x2E,
        0xD6, 0xCB, 0xC7, 0xC3, 0xCF, 0xB2, 0xF1, 0xEF,
    };

    /** 
     * The character to send to the server in place of unmapped
     * Unicode characters.
     */
    private static final byte gUnknownCharacter = (byte) 0x2e;


    /**
     * Constructor. Initializes the mapping tables. 
     */
    CharacterEncoderHpRoman8()
    {
        mCharToByteTable = gClientToServer;
        mCharExceptions = new char[] {
            '\u02CB', '\u02C6', '\u02DC', '\u20A4',
            '\u0192', '\u0160', '\u0161', '\u0178',
            '\u2014', '\u25A0'
        };
        mByteExceptions = new byte[] {
            (byte) 0xA9, (byte) 0xAA, (byte) 0xAC, (byte) 0xAF,
            (byte) 0xBE, (byte) 0xEB, (byte) 0xEC, (byte) 0xEE,
            (byte) 0xF6, (byte) 0xFC
        };
        mByteToCharTable = gServerToClient;
        mNativeUnknownCharacter = gUnknownCharacter;
    }
}


/**
 * Embodies the HP Roman-9 character encoding. The encoding tables
 * are based on those for HP Roman-8.
 *
 * @author John Lacey
 */
class CharacterEncoderHpRoman9 extends CharacterEncoder
{
    /** The server-to-client map. */
    private static final int[] gServerToClient = {
        0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
        0x0008, 0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F,
        0x0010, 0x0011, 0x0012, 0x0013, 0x0014, 0x0015, 0x0016, 0x0017,
        0x0018, 0x0019, 0x001A, 0x001B, 0x001C, 0x001D, 0x001E, 0x001F,
        0x0020, 0x0021, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0027,
        0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F,
        0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
        0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,

        0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
        0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F,
        0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057,
        0x0058, 0x0059, 0x005A, 0x005B, 0x005C, 0x005D, 0x005E, 0x005F,
        0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
        0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F,
        0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077,
        0x0078, 0x0079, 0x007A, 0x007B, 0x007C, 0x007D, 0x007E, 0x007F,

        0x0080, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087,
        0x0088, 0x0089, 0x008A, 0x008B, 0x008C, 0x008D, 0x008E, 0x008F,
        0x0090, 0x0091, 0x0092, 0x0093, 0x0094, 0x0095, 0x0096, 0x0097,
        0x0098, 0x0099, 0x009A, 0x009B, 0x009C, 0x009D, 0x009E, 0x009F,
        0x00A0, 0x00C0, 0x00C2, 0x00C8, 0x00CA, 0x00CB, 0x00CE, 0x00CF,
        0x00B4, 0x02CB, 0x02C6, 0x00A8, 0x02DC, 0x00D9, 0x00DB, 0x20A4,
        0x00AF, 0x00DD, 0x00FD, 0x00B0, 0x00C7, 0x00E7, 0x00D1, 0x00F1,
        0x00A1, 0x00BF, 0x20AC, 0x00A3, 0x00A5, 0x00A7, 0x0192, 0x00A2,

        0x00E2, 0x00EA, 0x00F4, 0x00FB, 0x00E1, 0x00E9, 0x00F3, 0x00FA,
        0x00E0, 0x00E8, 0x00F2, 0x00F9, 0x00E4, 0x00EB, 0x00F6, 0x00FC,
        0x00C5, 0x00EE, 0x00D8, 0x00C6, 0x00E5, 0x00ED, 0x00F8, 0x00E6,
        0x00C4, 0x00EC, 0x00D6, 0x00DC, 0x00C9, 0x00EF, 0x00DF, 0x00D4,
        0x00C1, 0x00C3, 0x00E3, 0x00D0, 0x00F0, 0x00CD, 0x00CC, 0x00D3,
        0x00D2, 0x00D5, 0x00F5, 0x0160, 0x0161, 0x00DA, 0x0178, 0x00FF,
        0x00DE, 0x00FE, 0x00B7, 0x00B5, 0x00B6, 0x00BE, 0x2014, 0x00BC,
        0x00BD, 0x00AA, 0x00BA, 0x00AB, 0x25A0, 0x00BB, 0x00B1, 0x002E,
    };
    
    /** The client-to-server map. */
    private static final int[] gClientToServer = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
        0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
        0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
        0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,

        0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
        0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
        0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
        0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67,
        0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
        0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77,
        0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F,

        0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
        0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F,
        0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97,
        0x98, 0x99, 0x9A, 0x9B, 0x9C, 0x9D, 0x9E, 0x9F,
        0xA0, 0xB8, 0xBF, 0xBB, 0x2E, 0xBC, 0x2E, 0xBD,
        0xAB, 0x2E, 0xF9, 0xFB, 0x2E, 0x2E, 0x2E, 0xB0,
        0xB3, 0xFE, 0x2E, 0x2E, 0xA8, 0xF3, 0xF4, 0xF2,
        0x2E, 0x2E, 0xFA, 0xFD, 0xF7, 0xF8, 0xF5, 0xB9,

        0xA1, 0xE0, 0xA2, 0xE1, 0xD8, 0xD0, 0xD3, 0xB4,
        0xA3, 0xDC, 0xA4, 0xA5, 0xE6, 0xE5, 0xA6, 0xA7,
        0xE3, 0xB6, 0xE8, 0xE7, 0xDF, 0xE9, 0xDA, 0x2E,
        0xD2, 0xAD, 0xED, 0xAE, 0xDB, 0xB1, 0xF0, 0xDE,
        0xC8, 0xC4, 0xC0, 0xE2, 0xCC, 0xD4, 0xD7, 0xB5,
        0xC9, 0xC5, 0xC1, 0xCD, 0xD9, 0xD5, 0xD1, 0xDD,
        0xE4, 0xB7, 0xCA, 0xC6, 0xC2, 0xEA, 0xCE, 0x2E,
        0xD6, 0xCB, 0xC7, 0xC3, 0xCF, 0xB2, 0xF1, 0xEF,
    };

    /** 
     * The character to send to the server in place of unmapped
     * Unicode characters.
     */
    private static final byte gUnknownCharacter = (byte) 0x2e;


    /**
     * Constructor. Initializes the mapping tables. 
     */
    CharacterEncoderHpRoman9()
    {
        mCharToByteTable = gClientToServer;
        mCharExceptions = new char[] {
            '\u02CB', '\u02C6', '\u02DC', '\u20A4',
            '\u20AC', '\u0192', '\u0160', '\u0161',
            '\u0178', '\u2014', '\u25A0'
        };
        mByteExceptions = new byte[] {
            (byte) 0xA9, (byte) 0xAA, (byte) 0xAC, (byte) 0xAF,
            (byte) 0xBA, (byte) 0xBE, (byte) 0xEB, (byte) 0xEC,
            (byte) 0xEE, (byte) 0xF6, (byte) 0xFC
        };
        mByteToCharTable = gServerToClient;
        mNativeUnknownCharacter = gUnknownCharacter;
    }
}


/**
 * Embodies the Europa-3 character encoding. The encoding tables
 * were taken from the Resource File for CMLIB (cmlib.rc).
 *
 * @author John Lacey
 */
class CharacterEncoderEuropa3 extends CharacterEncoder
{
    /** The server-to-client map. */
    private static final int[] gServerToClient = {
        0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
        0x0008, 0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F,
        0x0010, 0x0011, 0x0012, 0x0013, 0x0014, 0x0015, 0x0016, 0x0017,
        0x0018, 0x0019, 0x001A, 0x001B, 0x001C, 0x001D, 0x001E, 0x001F,
        0x0020, 0x0021, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0027,
        0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F,
        0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
        0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,

        0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
        0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F,
        0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057,
        0x0058, 0x0059, 0x005A, 0x005B, 0x005C, 0x005D, 0x005E, 0x005F,
        0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
        0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F,
        0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077,
        0x0078, 0x0079, 0x007A, 0x007B, 0x007C, 0x007D, 0x007E, 0x002E,

        0x00C7, 0x00FC, 0x00E9, 0x00E2, 0x00E4, 0x00E0, 0x00E5, 0x00E7,
        0x00EA, 0x00EB, 0x00E8, 0x00EF, 0x00EE, 0x00EC, 0x00C4, 0x00C5,
        0x00C9, 0x00E6, 0x00C6, 0x00F4, 0x00F6, 0x00F2, 0x00FB, 0x00F9,
        0x002E, 0x00D6, 0x00DC, 0x00F8, 0x002E, 0x00D8, 0x002E, 0x002E,
        0x00E1, 0x00ED, 0x00F3, 0x00FA, 0x00F1, 0x00D1, 0x002E, 0x002E,
        0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E,
        0x002E, 0x002E, 0x002E, 0x002E, 0x00AA, 0x00C1, 0x00C2, 0x00C0,
        0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x00CF, 0x002E,

        0x002E, 0x00BA, 0x00A1, 0x00BF, 0x002E, 0x002E, 0x00E3, 0x00C3,
        0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E,
        0x002E, 0x002E, 0x00CA, 0x002E, 0x002E, 0x00B5, 0x00CD, 0x002E,
        0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E,
        0x00D3, 0x00DF, 0x00D4, 0x002E, 0x00F5, 0x00D5, 0x002E, 0x002E,
        0x002E, 0x00DA, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E,
        0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E,
        0x002E, 0x002E, 0x00B7, 0x002E, 0x002E, 0x002E, 0x002E, 0x002E,
    };

    /** The client-to-server map. */
    private static final int[] gClientToServer = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
        0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
        0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F,
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
        0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,

        0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
        0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F,
        0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
        0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F,
        0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67,
        0x68, 0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F,
        0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77,
        0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x2E,

        0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
        0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
        0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
        0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
        0x2E, 0xC2, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
        0x2E, 0x2E, 0xB4, 0x2E, 0x2E, 0x2E, 0x2E, 0x2E,
        0x2E, 0x2E, 0x2E, 0x2E, 0x2E, 0xD5, 0x2E, 0xFA,
        0x2E, 0x2E, 0xC1, 0x2E, 0x2E, 0x2E, 0x2E, 0xC3,

        0xB7, 0xB5, 0xB6, 0xC7, 0x8E, 0x8F, 0x92, 0x80,
        0x2E, 0x90, 0xD2, 0x2E, 0x2E, 0xD6, 0x2E, 0xBE,
        0x2E, 0xA5, 0x2E, 0xE0, 0xE2, 0xE5, 0x99, 0x2E,
        0x9D, 0x2E, 0xE9, 0x2E, 0x9A, 0x2E, 0x2E, 0xE1,
        0x85, 0xA0, 0x83, 0xC6, 0x84, 0x86, 0x91, 0x87,
        0x8A, 0x82, 0x88, 0x89, 0x8D, 0xA1, 0x8C, 0x8B,
        0x2E, 0xA4, 0x95, 0xA2, 0x93, 0xE4, 0x94, 0x2E,
        0x9B, 0x97, 0xA3, 0x96, 0x81, 0x2E, 0x2E, 0x2E,
    };

    /** 
     * The character to send to the server in place of unmapped
     * Unicode characters.
     */
    private static final byte gUnknownCharacter = (byte) 0x2e;


    /**
     * Constructor. Initializes the mapping tables. 
     */
    CharacterEncoderEuropa3()
    {
        mCharToByteTable = gClientToServer;
        mCharExceptions = null;
        mByteExceptions = null;
        mByteToCharTable = gServerToClient;
        mNativeUnknownCharacter = gUnknownCharacter;
    }
}


/**
 * This class is similar to OutputStreamWriter, except that
 * it uses a custom character mapping.
 *
 * @author John Lacey
 */
class CharacterEncoderWriter extends Writer
{
    /** This stream's character encoder. */
    private CharacterEncoder mEncoder;

    /** The underlying output stream. */
    private OutputStream mOutputStream;


    /**
     * Constructor.
     *
     * @param anOutputStream the output stream to which this Writer
     *      will write
     * @param anEncoder a character encoder to use
     */
    CharacterEncoderWriter(OutputStream anOutputStream, 
        CharacterEncoder anEncoder)
    {
        mEncoder = anEncoder;
        mOutputStream = anOutputStream;
    }

    
    /**
     * Throws an exception if this object has been closed.
     *
     * @exception IOException if this object is closed
     */
    private void mEnsureValid() throws IOException
    {
        if (mOutputStream == null || mEncoder == null)
            throw new IOException();
    }


    /**
     * Writes the character data to the underlying stream, converting 
     * the characters to bytes using this object's character encoder.
     *
     * @param aCharacterBuffer the data to be written
     * @param anOffset the offset into the buffer from which to begin
     *      reading the data to write
     * @param aLength the number of characters to write
     * @exception IOException if an error occurs writing to 
     *      the underlying stream
     */
    public void write(char[] aCharacterBuffer, int anOffset, int aLength) 
        throws IOException
    {
        mEnsureValid();

        // As per the InputStream documentation.
        if (aCharacterBuffer == null)
            throw new NullPointerException();
        else if (anOffset < 0 || aLength < 0 || 
            anOffset + aLength > aCharacterBuffer.length)
        {
            throw new IndexOutOfBoundsException();
        }
        else if (aLength == 0)
            return;

        byte[] b = mEncoder.gConvert(aCharacterBuffer, anOffset, aLength);
        mOutputStream.write(b);
    }


    /**
     * Does nothing, but must be implemented when extending Writer.
     *
     * @exception IOException never
     */
    public void flush() throws IOException
    {
    }


    /**
     * Closes the underlying stream.
     *
     * @exception IOException if an error occurs closing
     *      the underlying stream
     */
    public void close() throws IOException
    {
        mEnsureValid();
        mOutputStream.close();
        mOutputStream = null;
        mEncoder = null;
    }
}


/**
 * Reads data from an InputStream and converts the bytes to characters
 * using the supplied character encoder.
 *
 * @author John Lacey
 */
/* TODO: Could improve performance here with buffering, I expect. */
class CharacterEncoderReader extends Reader
{
    /** The character encoder to use. */
    private CharacterEncoder mEncoder;

    /** The underlying input stream. */
    private InputStream mInputStream;


    /**
     * Constructor.
     *
     * @param anInputStream the input stream from which this object
     *      will read
     * @param anEncoder the character encoder which this object
     *      will use to convert the data
     */
    CharacterEncoderReader(InputStream anInputStream, 
        CharacterEncoder anEncoder)
    {
        mEncoder = anEncoder;
        mInputStream = anInputStream;
    }


    /**
     * Throws an exception if this object has been closed.
     *
     * @exception IOException if this object is closed
     */
    private void mEnsureValid() throws IOException
    {
        if (mInputStream == null || mEncoder == null)
            throw new IOException();
    }


    /**
     * Reads data into the given buffer.
     *
     * @param aCharacterBuffer the buffer into which to read data
     * @param anOffset the offset into the buffer at which to begin 
     *      storing data
     * @param aLength the maximum number of characters to read into
     *      the buffer
     * @return the number of characters read, or -1 if the end of the 
     *      stream was reached
     * @exception IOException if an error occurs reading from
     *      the underlying stream
     */
    public int read(char[] aCharacterBuffer, int anOffset, int aLength) 
        throws IOException
    {
        mEnsureValid();

        // As per the InputStream documentation.
        if (aCharacterBuffer == null)
            throw new NullPointerException();
        else if (anOffset < 0 || aLength < 0 || 
            anOffset + aLength > aCharacterBuffer.length)
        {
            throw new IndexOutOfBoundsException();
        }
        else if (aLength == 0)
            return 0;

        byte[] bytes = new byte[aLength];
        int count = mInputStream.read(bytes);
        if (count == -1)
            return count;
        
        for (int i = 0; i < bytes.length; i++)
            aCharacterBuffer[anOffset + i] = mEncoder.getUnicode(bytes[i]);

        return count;
    }


    /**
     * Closes the underlying stream.
     *
     * @exception IOException if an error occurs closing
     *      the underlying stream
     */
    public void close() throws IOException
    {
        mEnsureValid();
        mInputStream.close();
        mInputStream = null;
        mEncoder = null;
    }
}
