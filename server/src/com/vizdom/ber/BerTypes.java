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

package com.vizdom.ber;

/**
 * Constants for identifiers: tag classes, forms, and universal-class 
 * tag numbers.
 */
public interface BerTypes
{
    // Tag class (bits 8, 7)

    /** BER universal class. */
    int UNIVERSAL = 0x00;

    /** BER application-wide class. */
    int APPLICATION = 0x40;

    /** BER context-specific class. */
    int CONTEXT = 0x80;

    /** BER private-use class. */
    int PRIVATE = 0xC0;

    // Form (bit 6)

    /** BER primitive encoding. */
    int PRIMITIVE = 0x00;

    /** BER constructed encoding. */
    int CONSTRUCTED = 0x20;

    // Tag numbers (UNIVERSAL types)

    /** BER end-of-contents marker. */
    int END_OF_CONTENTS = 0;

    /** BER BOOLEAN (unimplemented). */
    int BOOLEAN = 1;
        
    /** BER INTEGER. */
    int INTEGER = 2;

    /** BER BIT STRING (unimplemented). */
    int BIT_STRING = 3;

    /** BER OCTET STRING. */
    int OCTET_STRING = 4;

    /** BER NULL. */
    int NULL = 5;

    /** BER OBJECT IDENTIFIER (unimplemented). */
    int OBJECT_IDENTIFIER = 6;

    /** BER ObjectDescriptor (unimplemented). */
    int ObjectDescriptor = 7;

    /** BER EXTERNAL (unimplemented). */
    int EXTERNAL = 8;

    /** BER REAL (unimplemented). */
    int REAL = 9;

    /** BER ENUMERATED (unimplemented). */
    int ENUMERATED = 10;

    // reserved: 11-15

    /** BER SEQUENCE, SEQUENCE OF. */
    int SEQUENCE = 16;

    /** BER SET, SET OF (unimplemented). */
    int SET = 17;

    /** BER NumericString (unimplemented). */
    int NumericString = 18;

    /** BER PrintableString (unimplemented). */
    int PrintableString = 19;

    /** BER TeletexString (unimplemented). */
    int TeletexString = 20;

    /** BER T61String (unimplemented). */
    int T61String = 20;

    /** BER VideotexString (unimplemented). */
    int VideotexString = 21;

    /** BER IA5String (unimplemented). */
    int IA5String = 22;

    /** BER UTCTime (unimplemented). */
    int UTCTime = 23;

    /** BER GeneralizedTime (unimplemented). */
    int GeneralizedTime = 24;

    /** BER GraphicString (unimplemented). */
    int GraphicString = 25;

    /** BER VisibleString (unimplemented). */
    int VisibleString = 26;

    /** BER ISO646String (unimplemented). */
    int ISO646String = 26;

    /** BER GeneralString (unimplemented). */
    int GeneralString = 27;
}

