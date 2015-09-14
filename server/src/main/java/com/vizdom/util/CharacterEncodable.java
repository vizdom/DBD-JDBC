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


/**
 * An interface for objects which can determine whether or not a
 * given character is encodable.
 */
public interface CharacterEncodable
{
    /**
     * Returns true if the given character can be encoded in the
     * current encoding.
     *
     * @param aCharacter a character to test
     * @return true if the character can be encoded, false otherwise
     */
    boolean canEncode(char aCharacter);
}       
