/*
 *  Copyright 1999-2002 Vizdom Software, Inc. All Rights Reserved.
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

package com.vizdom.util;

/**
 * An assertion failed. This class is for development uses only. 
 * It's a bug if users ever see this exception.
 *
 * @author John Lacey
 * @version $Revision: 1.12 $
 * @see Debug#assert
 * @see UnreachableCodeException
 */
public class AssertionFailedException extends RuntimeException
{
    /** Constructor. */
    public AssertionFailedException()
    {
    }


    /**
     * Constructs an exception with the given message text. 
     *
     * @param aMessage message text for this exception
     */
    public AssertionFailedException(String aMessage)
    {
        super(aMessage);
    }
}

