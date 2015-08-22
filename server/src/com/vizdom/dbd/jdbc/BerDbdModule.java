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

import com.vizdom.ber.BerModule;

/**
 * This class contains the package-specific BER packet type constants
 * as well as a BerModule instance for use by classes in this package.
 *
 * @author Gennis Emerson
 * @version $Revision: 1.14 $
 */
class BerDbdModule extends BerModule implements Cloneable
{
    /** 
     * This value is added to request codes to get the corresponding response
     * code. 
     */
    /* This constant is used in the definitions below, so it can't go after
     * the package-level constants as the coding standards require. 
     */
    private static final int sGAP = 1000;

    /** An error response packet. */
    static final int gERROR_RESPONSE =                   (int) 0xA + sGAP;

    /** A connect request. */
    static final int gCONNECT_REQUEST =                  (int) 0xB;

    /** A connect response. */
    static final int gCONNECT_RESPONSE = 
        gCONNECT_REQUEST + sGAP;    
    
    /** A disconnect request. */
    static final int gDISCONNECT_REQUEST =               (int) 0xC;

    /** A disconnect response. */
    static final int gDISCONNECT_RESPONSE = 
        gDISCONNECT_REQUEST + sGAP;

    /** A commit request. */
    static final int gCOMMIT_REQUEST =                   (int) 0xD;

    /** A commit response. */
    static final int gCOMMIT_RESPONSE = 
        gCOMMIT_REQUEST + sGAP;

    /** A rollback request. */
    static final int gROLLBACK_REQUEST =                 (int) 0xE;

    /** A rollback response. */
    static final int gROLLBACK_RESPONSE = 
        gROLLBACK_REQUEST + sGAP;

    /** A prepare request. */
    static final int gPREPARE_REQUEST =                  (int) 0xF;

    /** A prepare response. */
    static final int gPREPARE_RESPONSE = 
        gPREPARE_REQUEST + sGAP;

    /** An execute request. */
    static final int gEXECUTE_REQUEST =                  (int) 0x10;

    /** An execute response. */
    static final int gEXECUTE_RESPONSE = 
        gEXECUTE_REQUEST + sGAP;

    /** A fetch request. */
    static final int gFETCH_REQUEST =                    (int) 0x11;

    /** A fetch response. */
    static final int gFETCH_RESPONSE = 
        gFETCH_REQUEST + sGAP;

    /** A row count response. */
    static final int gEXECUTE_ROWS_RESPONSE =            (int) 0x12 + sGAP;

    /** A result set response. */
    static final int gEXECUTE_RESULTSET_RESPONSE =       (int) 0x13 + sGAP;

    /** A request to return a connection property. */
    static final int gGET_CONNECTION_PROPERTY_REQUEST =  (int) 0x14;

    /** A response containing a connection property value. */
    static final int gGET_CONNECTION_PROPERTY_RESPONSE = 
        gGET_CONNECTION_PROPERTY_REQUEST + sGAP;

    /** A request to return a statement property. */
    static final int gGET_STATEMENT_PROPERTY_REQUEST =   (int) 0x15;

    /** A response containing a statement property value. */
    static final int gGET_STATEMENT_PROPERTY_RESPONSE = 
        gGET_STATEMENT_PROPERTY_REQUEST + sGAP;

    /** A request to set a connection property. */
    static final int gSET_CONNECTION_PROPERTY_REQUEST =  (int) 0x16;

    /** A response indicating that a connection property was set. */
    static final int gSET_CONNECTION_PROPERTY_RESPONSE = 
        gSET_CONNECTION_PROPERTY_REQUEST + sGAP;

    /** A request to set a statement property. */
    static final int gSET_STATEMENT_PROPERTY_REQUEST =   (int) 0x17;

    /** A response indicating that a statement property was set. */
    static final int gSET_STATEMENT_PROPERTY_RESPONSE = 
        gSET_STATEMENT_PROPERTY_REQUEST + sGAP;

    /** A statement finish request. */
    static final int gSTATEMENT_FINISH_REQUEST =         (int) 0x18;

    /** A statement finish response. */
    static final int gSTATEMENT_FINISH_RESPONSE = 
        gSTATEMENT_FINISH_REQUEST + sGAP;

    /** A statement destroy request. */
    static final int gSTATEMENT_DESTROY_REQUEST =          (int) 0x19;

    /** A statement destroy response. */
    static final int gSTATEMENT_DESTROY_RESPONSE = 
        gSTATEMENT_DESTROY_REQUEST + sGAP;

    /** A ping request. */
    static final int gPING_REQUEST =                     (int) 0x1A;

    /** A ping response. */
    static final int gPING_RESPONSE = 
        gPING_REQUEST + sGAP;

    /** A packet whose contents are interpreted as a hash. */
    static final int gBER_HASH =                         (int) 0x1B;

    /** A single error message. */
    static final int gERROR =                            (int) 0x1C;

    /** A func request for the connection object. */
    static final int gCONNECTION_FUNC_REQUEST =          (int) 0x1D;

    /** A func response for the connection object. */
    static final int gCONNECTION_FUNC_RESPONSE = 
        gCONNECTION_FUNC_REQUEST + sGAP;

    //Either Convert::BER or the Java BER classes are failing
    //around 1E and 1F. Just skip those cases.

    /** A func request for the statement object. */
    static final int gSTATEMENT_FUNC_REQUEST =           (int) 0x20;
    /** A func response for the statement object. */
    static final int gSTATEMENT_FUNC_RESPONSE = 
        gSTATEMENT_FUNC_REQUEST + sGAP;

    /** A request. */
    static final int gGET_GENERATED_KEYS_REQUEST =       (int) 0x21;
    /** A response. */
    static final int gGET_GENERATED_KEYS_RESPONSE = 
        gGET_GENERATED_KEYS_REQUEST + sGAP;

    /** A module instance for use by classes in this package. */
    static final BerDbdModule gBerModule = new BerDbdModule();

    /* Set up the application-specific BER types. */
    static
    {
        gBerModule.registerFactory(ConnectRequest.gFACTORY, 
            ConnectRequest.gIDENTIFIER);
        gBerModule.registerFactory(DisconnectRequest.gFACTORY, 
            DisconnectRequest.gIDENTIFIER);
        gBerModule.registerFactory(CommitRequest.gFACTORY, 
            CommitRequest.gIDENTIFIER);
        gBerModule.registerFactory(RollbackRequest.gFACTORY, 
            RollbackRequest.gIDENTIFIER);
        gBerModule.registerFactory(PrepareRequest.gFACTORY, 
            PrepareRequest.gIDENTIFIER);
        gBerModule.registerFactory(ExecuteRequest.gFACTORY, 
            ExecuteRequest.gIDENTIFIER);
        gBerModule.registerFactory(FetchRequest.gFACTORY, 
            FetchRequest.gIDENTIFIER);
        gBerModule.registerFactory(GetConnectionPropertyRequest.gFACTORY, 
            GetConnectionPropertyRequest.gIDENTIFIER);
        gBerModule.registerFactory(GetStatementPropertyRequest.gFACTORY, 
            GetStatementPropertyRequest.gIDENTIFIER);
        gBerModule.registerFactory(SetConnectionPropertyRequest.gFACTORY, 
            SetConnectionPropertyRequest.gIDENTIFIER);
        gBerModule.registerFactory(SetStatementPropertyRequest.gFACTORY, 
            SetStatementPropertyRequest.gIDENTIFIER);
        gBerModule.registerFactory(StatementFinishRequest.gFACTORY, 
            StatementFinishRequest.gIDENTIFIER);
        gBerModule.registerFactory(StatementDestroyRequest.gFACTORY, 
            StatementDestroyRequest.gIDENTIFIER);
        gBerModule.registerFactory(PingRequest.gFACTORY, 
            PingRequest.gIDENTIFIER);
        gBerModule.registerFactory(ConnectionFuncRequest.gFACTORY, 
            ConnectionFuncRequest.gIDENTIFIER);

        gBerModule.registerFactory(StatementFuncRequest.gFACTORY, 
            StatementFuncRequest.gIDENTIFIER);
        gBerModule.registerFactory(GetGeneratedKeysRequest.gFACTORY, 
            GetGeneratedKeysRequest.gIDENTIFIER);

        gBerModule.registerFactory(BerHash.gFACTORY, 
            BerHash.gIDENTIFIER);
    }


    /** 
     * A default constructor.
     */
    BerDbdModule()
    {
        super();
    }


    /**
     * Create a clone of this object.
     *
     * @return a clone of this object
     */
    /* Per the Java Programmer's FAQ. */
    public Object clone() 
    {
        try
        {
            return super.clone();
        }
        catch (CloneNotSupportedException noClone)
        {
            throw new com.vizdom.util.UnreachableCodeException();
        }
    }
}
