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
 * An arbitrary function request.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.2 $
 */
class ConnectionFuncRequest extends BerSequence
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gCONNECTION_FUNC_REQUEST);

    /** The factory for this object. */
    static final BerObjectFactory gFACTORY = new BerObjectFactory() {
        public boolean acceptsIdentifier(BerIdentifier anIdentifier)
        {
            return anIdentifier.equals(gIDENTIFIER);
        }
        
        public BerObject createBerObject()
        {
            return new ConnectionFuncRequest();
        }
    };

    /** The parameters to be used in this execution. */
    private Parameter[] mParameters;

    /**
     * The decoding constructor.
     */
    private ConnectionFuncRequest()
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

        // Method name is mSequence[0]
        int parameterCount = (mSequence.length - 1) / 2;
        mParameters = new Parameter[parameterCount];
        int paramIndex = 0;
        for (int i = 1; i <= mSequence.length - 2; i += 2)
        {
            BerOctetString value;
            if (mSequence[i] instanceof BerNull)
                value = null;
            else
                value = (BerOctetString) mSequence[i];
            int type = ((BerInteger) mSequence[i + 1]).intValue();
            mParameters[paramIndex++] = new Parameter(value, type);
        }
    }

    /**
     * Returns the name of the method to call. 
     *
     * @return the name of the method to call
     */
    String getMethodName()
    {
        return mSequence[0].toString();
    }
    /**
     * Returns the number of parameters.
     *
     * @return the number of parameters
     */
    int getParameterCount()
    {
        return mSequence.length;
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
        return "ConnectionFunc";
    }
}
