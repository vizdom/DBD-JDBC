/*
 * Copyright 2008 Vizdom Software, Inc. All Rights Reserved.
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
 * A request for generated keys.
 */
class GetGeneratedKeysRequest extends BerSequence
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gGET_GENERATED_KEYS_REQUEST);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = new BerObjectFactory() {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }
        
        public BerObject createBerObject()
        {
            return new GetGeneratedKeysRequest();
        }
    };


    /** The catalog parameter. */
    private String mCatalog;

    /** The schema parameter. */
    private String mSchema;

    /** The table parameter. */
    private String mTable;

    /** The column parameter. */
    private String mColumn;
    

    /**
     * The decoding constructor.
     */
    private GetGeneratedKeysRequest()
    {
        super();
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
        if (mSequence.length >= 1)
        {
            if (mSequence[0] instanceof BerNull)
                mCatalog = null;
            else
                mCatalog = mSequence[0].toString(); 
        }
        if (mSequence.length >= 2)
        {
            if (mSequence[1] instanceof BerNull)
                mSchema = null;
            else
                mSchema = mSequence[1].toString(); 
        }
        if (mSequence.length >= 3)
        {
            if (mSequence[2] instanceof BerNull)
                mTable = null;
            else
                mTable = mSequence[2].toString(); 
        }
        if (mSequence.length >= 4)
        {
            if (mSequence[3] instanceof BerNull)
                mColumn = null;
            else
                mColumn = mSequence[3].toString(); 
        }
    }


    /**
     * Returns the catalog.
     *
     * @return the catalog
     */
    public String getCatalog()
    {
        return mCatalog; 
    }


    /**
     * Returns the schema.
     *
     * @return the schema
     */
    public String getSchema()
    {
        return mSchema; 
    }


    /**
     * Returns the table.
     *
     * @return the table
     */
    public String getTable()
    {
        return mTable; 
    }


    /**
     * Returns the column.
     *
     * @return the column
     */
    public String getColumn()
    {
        return mColumn; 
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
        StringBuffer b = new StringBuffer();
        if (mCatalog != null)
            b.append(mCatalog);
        if (mSchema != null && b.length() > 0)
            b.append(".").append(mSchema);
        else if (mSchema != null)
            b.append(mSchema);            
        if (mTable != null && b.length() > 0)
            b.append(".").append(mTable);
        else if (mTable != null)
            b.append(mTable);            
        if (mColumn != null && b.length() > 0)
            b.append(".").append(mColumn);
        else if (mColumn != null)
            b.append(mColumn);
        b.insert(0, "GetGeneratedKeys "); 
        return b.toString(); 
    }
}
