/*
 * Copyright 1999-2004,2008 Vizdom Software, Inc. All Rights Reserved.
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the same terms as the Perl Kit, namely, under 
 * the terms of either:
 *
 *     a) the GNU General Public License as published by the Free
 *     Software Foundation; either version 1 of the License, or 
 *     (at your option) any later version, or
 *
 *     b) the "Artistic License" that comes with the Perl Kit.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See either
 * the GNU General Public License or the Artistic License for more 
 * details.
 */

package com.vizdom.util;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A collection of helpful methods for debugging and assertions.
 * 
 * @author John Lacey
 * @version $Revision: 1.34 $
 */
public class Debug
{
    /** When true, debugging is enabled. */
    public static final boolean DEBUG = false;

    /** When true, assertions are enabled. */
    public static final boolean ASSERT = false;

    /** 
     * Turns off debugging output. This should not be used in a call to 
     * <code>getLogStream</code> or <code>getLogWriter</code>. 
     */
    public static final int SILENT = 0;

    /** Log messages that might interest users. */
    public static final int BRIEF = 1;

    /** Log generally useful debugging messages. */
    public static final int VERBOSE = 2;

    /** Log detailed message only useful for finding deeply buried bugs. */
    public static final int TEDIOUS = 3;

    /** Log detailed message only useful for filling up spare gigabytes. */
    public static final int ABUSIVE = 4;

    /** Messages at this level or lower will be printed by a log stream. */
    private static int gVerbosity = BRIEF;

    /** The standard log writer. */
    private static PrintWriter gLogWriter;

    /** The standard log stream. */
    private static PrintStream gLogStream;

    /** 
     * The null log writer, used when the verbosity passed to 
     * <code>getLogWriter</code> is higher than <code>gVerbosity</code>.
     * This Writer acts like a sink. Nothing you write to it will ever
     * show up anyplace.
     * <p>
     * Annoyingly, PrintWriter (and PrintStream) use a private method
     * to print the newline, so we also have to override every println
     * method in PrintWriter.
     */
    private static final PrintWriter gNullLogWriter = 
        new PrintWriter(System.err) {
            public void write(int b) {}
            public void write(char[] buf, int off, int len) {}
            public void write(String s, int off, int len) {}
            public void println() {}
            public void println(boolean b) {}
            public void println(char c) {}
            public void println(char[] c) {}
            public void println(double d) {}
            public void println(float f) {}
            public void println(int i) {}
            public void println(long l) {}
            public void println(Object o) {}
            public void println(String s) {}
        };

    /** 
     * The null log stream, used when the verbosity passed to 
     * <code>getLogStream</code> is higher than <code>gVerbosity</code>.
     * This stream acts like a sink. Nothing you write to it will ever
     * show up anyplace.
     * <p>
     * Annoyingly, PrintStream (and PrintWriter) use a private method
     * to print the newline, so we also have to override every println
     * method in PrintStream.
     */
    private static final PrintStream gNullLogStream = 
        new PrintStream(System.err) {
            public void write(int b) {}
            public void write(byte[] buf, int off, int len) {}
            public void println() {}
            public void println(boolean b) {}
            public void println(char c) {}
            public void println(char[] c) {}
            public void println(double d) {}
            public void println(float f) {}
            public void println(int i) {}
            public void println(long l) {}
            public void println(Object o) {}
            public void println(String s) {}
        };


    static
    {
        PrintStream defaultStream = System.err;
        String logFile = System.getProperty("debug.log");
        if (logFile == null)
            gLogStream = defaultStream;
        else
        {
            try
            {
                FileOutputStream fout = new FileOutputStream(logFile, true);
                OutputStream out = new BufferedOutputStream(fout);
                gLogStream = new PrintStream(out);
            }
            catch (FileNotFoundException e)
            {
                gLogStream = defaultStream;
            }
        }
        gLogWriter = new PrintWriter(gLogStream, true);
    }
    

    /**
     * Sets the standard log writer. Sets the standard log stream
     * to the null log stream.
     *
     * @param aLogWriter a PrintWriter for logging.
     */
    public static synchronized void setLogWriter(Writer aLogWriter)
    {
        gLogWriter = new PrintWriter(aLogWriter, true);
        gLogStream = gNullLogStream; // Nothing else to do; WriterOutputStream?
    }


    /**
     * Gets a log writer that always writes to the standard log writer.
     *
     * @return a log writer that always writes to the standard log writer
     */
    public static synchronized PrintWriter getLogWriter() 
    {
        return gLogWriter;
    }


    /**
     * Gets a log writer corresponding to the given verbosity.
     *
     * @param aVerbosity a verbosity level
     * @return the standard log writer if 
     *     <code>aVerbosity <= gVerbosity</code>, or the null log writer 
     *     otherwise
     */
    public static synchronized PrintWriter getLogWriter(int aVerbosity) 
    {
        return (aVerbosity <= gVerbosity) ? gLogWriter : gNullLogWriter;
    }


    /**
     * Sets the standard log stream.
     *
     * @param aLogStream a print stream for logging.
     */
    public static synchronized void setLogStream(OutputStream aLogStream)
    {
        gLogWriter = new PrintWriter(aLogStream, true);
        gLogStream = new PrintStream(aLogStream);
    }


    /**
     * Gets a log stream that always writes to the standard log stream.
     *
     * @return a log stream that always writes to the standard log stream
     * @deprecated Use {@link #getLogWriter()} instead
     */
    @Deprecated
    public static synchronized PrintStream getLogStream() 
    {
        return gLogStream;
    }


    /**
     * Gets a log stream corresponding to the given verbosity.
     *
     * @param aVerbosity a verbosity level
     * @return the standard log stream if 
     *     <code>aVerbosity <= gVerbosity</code>, or the null log stream
     *     otherwise
     * @deprecated Use {@link #getLogWriter(int)} instead
     */
    @Deprecated
    public static synchronized PrintStream getLogStream(int aVerbosity) 
    {
        return (aVerbosity <= gVerbosity) ? gLogStream : gNullLogStream;
    }


    /**
     * Gets the current verbosity level.
     *
     * @return the current verbosity level.
     */
    public static synchronized int getVerbosity() 
    {
        return gVerbosity;
    }


    /**
     * Sets the current verbosity level.
     *
     * @param aVerbosity the new verbosity level.
     */
    public static synchronized void setVerbosity(int aVerbosity) 
    {
        gVerbosity = aVerbosity;
    }


    /**
     * Sets the current verbosity level.
     *
     * @param aVerbosity the new verbosity level.
     */
    public static synchronized void setVerbosity(String aVerbosity) 
    {
        if (aVerbosity.equalsIgnoreCase("silent"))
            gVerbosity = SILENT;
        else if (aVerbosity.equalsIgnoreCase("brief"))
            gVerbosity = BRIEF;
        else if (aVerbosity.equalsIgnoreCase("verbose"))
            gVerbosity = VERBOSE;
        else if (aVerbosity.equalsIgnoreCase("tedious"))
            gVerbosity = TEDIOUS;
        else if (aVerbosity.equalsIgnoreCase("abusive"))
            gVerbosity = ABUSIVE;
        else
            ; // ignored
    }


    /**
     * Asserts that the boolean condition passed in is true.
     * 
     * @param aCondition a boolean condition
     * @exception AssertionFailedException if the condition is 
     *     <code>false</code>.
     */
    public static final void assertTrue(boolean aCondition)
    {
        if (!aCondition)
            throw new AssertionFailedException();
    }


    /**
     * Asserts that the boolean condition passed in is true of the
     * given parameter. Use to check method parameters.
     * 
     * @param aPosition the index of the parameter
     * @param aCondition a boolean condition on the parameter
     * @exception IllegalArgumentException if the condition is 
     *     <code>false</code>.
     */
    public static final void assertParameter(int aPosition, boolean aCondition)
    {
        if (!aCondition)
            throw new IllegalArgumentException(Integer.toString(aPosition));
    }


    /**
     * A <code>private</code> constructor to disallow instances of this class.
     */
    private Debug()
    {
        throw new UnreachableCodeException();
    }
}
