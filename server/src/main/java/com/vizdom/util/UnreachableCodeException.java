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

package com.vizdom.util;

/**
 * Unreachable code was executed. This class is for development uses only. 
 * It's a bug if users ever see this exception.
 *
 * @author John Lacey
 * @version $Revision: 1.12 $
 * @see AssertionFailedException
 */
public class UnreachableCodeException extends AssertionFailedException
{
    /** Constructor. */
    public UnreachableCodeException()
    {
    }


    /** 
     * Used for invalid string values, most likely unsupported
     * character encodings.
     *
     * @param aMessage the message text for this exception
     */
    public UnreachableCodeException(String aMessage)
    {
        super(aMessage);
    }


    /** 
     * Usually called in the default case of a switch. 
     *
     * @param aValue an integer to be used as the message text
     */
    public UnreachableCodeException(int aValue)
    {
        super(Integer.toString(aValue));
    }


    /** 
     * Used when an object is not an instance of a supported class. 
     *
     * @param aClass a class whose name will be used as this exception's
     *      message text
     */
    public UnreachableCodeException(Class aClass)
    {
        super(aClass.getName());
    }


    /** 
     * Used when a declared but unexpected exception is thrown. 
     *
     * @param cause an exception whose string representation will be
     *     used as this exception's message text
     */
    public UnreachableCodeException(Throwable cause)
    {
        // TODO: super(cause), once we require JDK 1.4.
        super(cause.toString());
    }
}
