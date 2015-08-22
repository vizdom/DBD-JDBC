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
import java.util.Vector;

/**
 * An execute request.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.10 $
 */
class ExecuteRequest extends BerSequence
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gEXECUTE_REQUEST);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = new BerObjectFactory() {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }
        
        public BerObject createBerObject()
        {
            return new ExecuteRequest();
        }
    };

    /** The parameters to be used in this execution. */
    private Parameter[] mParameters;

    /**
     * The decoding constructor.
     */
    private ExecuteRequest()
    {
        super();
    }

    /** 
     * Reads the encoded contents from the input stream.
     * 
     * @param anIn an input stream
     * @param aModule a BER module for reading constructed encodings
     * @param anIdentifier the BER identifier of the encoded object
     * @param aLength the length in bytes of the encoded contents
     * @exception IOException if an error occurs reading the input
     */
    /* What we probably want is a Parameter packet type. */
    protected void mReadContents(java.io.InputStream anIn, 
        BerModule aModule, BerIdentifier anIdentifier, int aLength) 
        throws java.io.IOException
    {
        super.mReadContents(anIn, aModule, anIdentifier, aLength);
        // mSequence[0] is the statement handle
        int parameterCount = ((BerInteger) mSequence[1]).intValue();
        mParameters = new Parameter[parameterCount];
        for (int i = 0; i < parameterCount; i++)
        {
            int paramIndex = 2 * i + 2;
            BerOctetString value;
            if (mSequence[paramIndex] instanceof BerNull)
                value = null;
            else
                value = (BerOctetString) mSequence[paramIndex];
            int type = ((BerInteger) mSequence[paramIndex + 1]).intValue();
            mParameters[i] = new Parameter(value, type);
        }
    }

    /**
     * Returns the statement handle identifying the statement to be
     * executed.
     *
     * @return a statement handle
     */
    int getHandle()
    {
        return ((BerInteger) mSequence[0]).intValue();
    }

    /**
     * Returns the number of parameters.
     *
     * @return the number of parameters
     */
    int getParameterCount()
    {
        return ((BerInteger) mSequence[1]).intValue();
    }

    /**
     * Returns the parameters to be used in this execution.
     *
     * @return the parameters to be used in this execution
     */
    Parameter[] getParameters()
    {
        return mParameters;
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
        return "Execute";
    }
}


