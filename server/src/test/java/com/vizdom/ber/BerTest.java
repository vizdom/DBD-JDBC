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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import junit.framework.TestCase;

/**
 * Unit testing for the package.
 */
public class BerTest extends TestCase
{
    private static class TestString extends BerOctetString
    {
        private static final int TAG_NUMBER = 75;

        static final BerIdentifier gIDENTIFIER = 
            new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED,
                TAG_NUMBER);


        TestString(String s, String enc) throws UnsupportedEncodingException
        {
            super(s, enc);
        }


        TestString()
        {
        }


        public BerIdentifier getIdentifier()
        {
            return gIDENTIFIER;
        }
    }


    private static class TestStringFactory implements BerObjectFactory
    {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(TestString.gIDENTIFIER);
        }


        public BerObject createBerObject()
        {
            return new TestString();
        }
    }


    /**
     * Constructs an instance to run the given test.
     *
     * @param testName the name of a test method to run
     */
    public BerTest(String testName)
    {
        super(testName);
    }


    /**
     * Tests BER STRING encoding/decoding.
     *
     * @throws IOException if an error occurs
     */
    public void testBerOctetString() throws IOException
    {
        BerModule module = new BerModule();
        module.setCharacterEncoding("ASCII");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        BerOctetString berString = new BerOctetString("hello, world", "ASCII");
        berString.writeTo(bout);
        byte[] bytes = bout.toByteArray();
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        BerOctetString inputString = (BerOctetString) module.readFrom(bin);
        assertEquals("hello, world", inputString.toString());
    }


    /**
     * Tests BER STRING encoding/decoding for strings longer than 255 bytes.
     *
     * @throws IOException if an error occurs
     */
    public void testLongBerOctetString() throws IOException
    {
        BerModule module = new BerModule();
        module.setCharacterEncoding("ASCII");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] octets = new byte[260];
        for (int i = 0; i < octets.length; i++)
            octets[i] = (byte) i;
        BerOctetString berString = new BerOctetString(octets, "ASCII");
        berString.writeTo(bout);
        byte[] bytes = bout.toByteArray();
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        BerOctetString inputString = (BerOctetString) module.readFrom(bin);
        byte[] inputOctets = inputString.toByteArray();
        assertEquals(octets.length, inputOctets.length);
        for (int i = 0; i < inputOctets.length; i++)
            assertEquals(octets[i], inputOctets[i]);
    }


    /**
     * Tests BER STRING encoding/decoding for a specialized string type.
     *
     * @throws IOException if an error occurs
     */
    public void testTestString() throws IOException
    {
        BerModule module = new BerModule();
        module.setCharacterEncoding("ASCII");
        module.registerFactory(new TestStringFactory());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        TestString berString = new TestString("ciao, bella", "ASCII");
        berString.writeTo(bout);
        byte[] bytes = bout.toByteArray();
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        TestString inputString = (TestString) module.readFrom(bin);
        assertEquals("ciao, bella", inputString.toString());
    }


    /**
     * Tests BER INTEGER encoding/decoding.
     *
     * @throws IOException if an error occurs
     */
    public void testBerInteger() throws IOException
    {
        BerModule module = new BerModule();

        int[] ints = { 1729, 0, -1, 128, 256, -32768, 32767, 0xFFFFFFFF,
            0x80000000, 0x80000001, 0x80808080, 129, 254, 255, 32768 };
        for (int i = 0; i < ints.length; i++)
        {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            BerInteger berInt = new BerInteger(ints[i]);
            berInt.writeTo(bout);
            byte[] bytes = bout.toByteArray();
            ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
            BerInteger inputInt = (BerInteger) module.readFrom(bin);
            assertEquals(ints[i], inputInt.intValue());
        }
    }


    /**
     * Tests partial BER decoding.
     *
     * @throws Exception if an error occurs
     */
    public void testStream() throws Exception
    {
        final int prefixSize = 13;
        String s = "All good dogs go to heaven.";
        byte[] bytes = new byte[24];
        int count;

        ByteArrayInputStream bin = new ByteArrayInputStream(s.getBytes());
        BerContentsInputStream bers =
            new BerContentsInputStream(bin, prefixSize);
        int b;
        int i = 0;
        while ((b = bers.read()) != -1)
            bytes[i++] = (byte) b;
        assertEquals(s.substring(0, prefixSize), new String(bytes, 0, i));

        count = bin.read(bytes);
        assertEquals(s.substring(prefixSize), new String(bytes, 0, count));

        bin = new ByteArrayInputStream(s.getBytes());
        bers = new BerContentsInputStream(bin, prefixSize);
        count = bers.read(bytes);
        assertEquals(s.substring(0, prefixSize), new String(bytes, 0, count));

        count = bin.read(bytes);
        assertEquals(s.substring(prefixSize), new String(bytes, 0, count));
    }


    /**
     * Tests BER sequence encoding/decoding.
     *
     * @throws Exception if an error occurs
     */
    public void testSequence() throws Exception
    {
        BerOctetString berString = new BerOctetString("hello, world", "ASCII");
        BerInteger berInt = new BerInteger(42);
        BerNull berNull = new BerNull();

        byte[] berEncoding = {
            (byte) (BerTypes.UNIVERSAL | BerTypes.CONSTRUCTED |
                BerTypes.SEQUENCE), (byte) ("hello, world".length() + 7),
            (byte) BerTypes.OCTET_STRING, (byte) "hello, world".length(),
            (byte) 'h', (byte) 'e', (byte) 'l', (byte) 'l', (byte) 'o',
            (byte) ',', (byte) ' ', (byte) 'w', (byte) 'o', (byte) 'r',
            (byte) 'l', (byte) 'd', (byte) BerTypes.INTEGER, (byte) 1,
            (byte) 42, (byte) BerTypes.NULL, (byte) 0
        };
            
        BerModule module = new BerModule();
        module.setCharacterEncoding("ASCII");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        BerSequence berSeq = 
            new BerSequence(new BerObject[] { berString, berInt, berNull });
        berSeq.writeTo(bout);
        byte[] bytes = bout.toByteArray();
        assertEquals("byte array length", berEncoding.length, bytes.length);
        for (int i = 0; i < bytes.length; i++)
            assertEquals("byte array element " + i, berEncoding[i], bytes[i]);

        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        BerSequence inputSeq = (BerSequence) module.readFrom(bin);
        assertEquals("[hello, world, 42, NULL]",
            inputSeq.toString());
    }
}
