/*
 * Copyright 2005 Vizdom Software, Inc. All Rights Reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the version number of the DBD::JDBC module. The build
 * process keeps this in sync with the Perl side.
 */
public class Version
{
	
	private static final Logger LOG = LoggerFactory.getLogger(Version.class);
	
	static {
		InputStream is = Version.class.getResourceAsStream("/dbd-jdbc-server.properties");
		Properties p = new Properties();
		try {
			p.load(is);
		} catch (IOException e) {
			LOG.error("Problem getting version!", e);
		}
		version = p.getProperty("application.version");
	}
    /** The version string. */
    public static final String version;
    
    
}
