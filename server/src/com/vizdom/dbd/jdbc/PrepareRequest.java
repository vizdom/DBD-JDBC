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

/**
 * A prepare request.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.8 $
 */
class PrepareRequest extends BerSequence
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gPREPARE_REQUEST);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = new BerObjectFactory() {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }
        
        public BerObject createBerObject()
        {
            return new PrepareRequest();
        }
    };


    /** The statement. */
    private String mStatement;

    /** The type of generated key request; may be null. */
    private String mKeyType;

    /** The column names for keys to return; may be null. */
    private String[] mColumnNames;

    /** The column indexes for keys to return; may be null. */
    private int[] mColumnIndexes;
    

    /**
     * The decoding constructor.
     */
    private PrepareRequest()
    {
        super();
    }

    /**
     * Returns the statement to be prepared.
     *
     * @return the statement to be prepared
     */
    String getStatement()
    {
        return mStatement; 
    }


    /**
     * Returns the key type; null, "name", or "index".
     *
     * @return the key type
     */
    public String getKeyType()
    {
        return mKeyType; 
    }


    /**
     * Returns the column names if the type was "name".
     * 
     * @return the column names if the type was "name"
     */
    public String[] getColumnNames()
    {
        return mColumnNames;
    }


    /**
     * Returns the column indexes if the type was "index".
     * 
     * @return the column indexes if the type was "index"
     */
    public int[] getColumnIndexes()
    {
        return mColumnIndexes;
    }


    /**
     * Reads the parameters.
     *
     * @param anIn an input stream
     * @param aModule a BER module for reading constructed encodings
     * @param anIdentifier the BER identifier of the encoded object
     * @param aLength the length in bytes of the encoded contents
     * @exception IOException if an error occurs reading the input
     */
    protected void mReadContents(java.io.InputStream anIn,
        BerModule aModule, BerIdentifier anIdentifier, int aLength) 
        throws java.io.IOException
    {
        super.mReadContents(anIn, aModule, anIdentifier, aLength);
        mStatement = ((BerOctetString) mSequence[0]).toString();
        if (mSequence.length <= 2) // No columns were provided, even if a type was. 
        {
            mKeyType = null;
            return;
        }
        BerObject keyType = mSequence[1];
        if (keyType instanceof BerNull)
        {
            mKeyType = null;
            return;
        }
        else
            mKeyType = ((BerOctetString) mSequence[1]).toString();
        if ("name".equals(mKeyType))
        {
            mColumnNames = new String[mSequence.length - 2];
            for (int i = 2; i < mSequence.length; i++)
                mColumnNames[i-2] = mSequence[i].toString(); 
        }
        else if ("index".equals(mKeyType))
        {
            mColumnIndexes = new int[mSequence.length - 2];
            for (int i = 2; i < mSequence.length; i++)
                mColumnIndexes[i-2] = ((BerInteger) mSequence[i]).intValue(); 
        }
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
        return "Prepare: " + super.toString();
    }
}

