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
 * A successful execute response will either contain a row count
 * or a column count, depending on the type of statement executed.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.9 $
 */
class ExecuteResponse extends BerSequence
{
    /* I'm keeping ExecuteResponse as a wrapper, for now, 
     * in case I want to extend the response with more information. We might
     * actually want to use the OPTIONAL feature in the client
     * to allow the two subtypes to be sent directly.
     */

    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.CONSTRUCTED, 
        BerDbdModule.gEXECUTE_RESPONSE);

    /**
     * Constructor - sets the given BerObject as the content of this 
     * response.
     *
     * @param aResponse the response object to be sent
     */
    ExecuteResponse(BerObject aResponse)
    {
        super();
        mSequence = new BerObject[] { aResponse };
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
        return "Execute complete; " + mSequence[0].toString();
    }
}

/**
 * This response class is used when the statement executed returned a 
 * row count.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.9 $
 */ 
class ExecuteRowsResponse extends BerInteger
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.PRIMITIVE, 
        BerDbdModule.gEXECUTE_ROWS_RESPONSE);

    /**
     * Constructor - sets the value of this object.
     *
     * @param aColumnCount the number of rows affected
     *      by the just-executed statement
     */
    ExecuteRowsResponse(int aRowCount)
    {
        super(aRowCount);
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
        return "Rows affected: " + intValue();
    }
}


/**
 * This response class is used when the statement executed returned a 
 * result set; the integer value is the number of columns in the result set.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.9 $
 */ 
class ExecuteResultSetResponse extends BerInteger
{
    /** This object's identifier. */
    static final BerIdentifier gIDENTIFIER = 
        new BerIdentifier(BerTypes.APPLICATION, BerTypes.PRIMITIVE, 
        BerDbdModule.gEXECUTE_RESULTSET_RESPONSE);

    /**
     * Constructor - sets the value of this object.
     *
     * @param aColumnCount the number of columns in the result set
     *      returned by the just-executed statement
     */
    ExecuteResultSetResponse(int aColumnCount)
    {
        super(aColumnCount);
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
        return "Result set columns: " + intValue();
    }
}


