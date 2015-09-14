/*
 * Copyright 2009 Vizdom Software, Inc. All Rights Reserved.
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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException; 
import java.nio.charset.UnsupportedCharsetException; 


/**
 * An implementation of CharacterEncodable which uses a
 * java.nio.charset.CharacterEncoder.  This class is usable for
 * supported character encodings.
 */
class CharsetEncodable implements CharacterEncodable
{
    /** The encoder. */
    private final CharsetEncoder mCharsetEncoder;
    

    /**
     * Gets a CharsetEncoder for the given encoding.
     *
     * @param anEncoding an encoding name
     * @throws IllegalCharsetNameException if the encoding name is invalid
     * @throws UnsupportedCharsetException if the encoding is not supported
     */
    CharsetEncodable(String anEncoding) 
        throws IllegalCharsetNameException, UnsupportedCharsetException
    {
        mCharsetEncoder = Charset.forName(anEncoding).newEncoder();
    }


    /**
     * Returns true if the given character can be encoded in the
     * current encoding.
     *
     * @param aCharacter a character to test
     * @return true if the character can be encoded, false otherwise
     */
    public boolean canEncode(char aCharacter)
    {
        return mCharsetEncoder.canEncode(aCharacter); 
    }
}
