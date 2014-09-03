/*
 * $Id: OutputOption.java,v 1.1 2014-09-03 20:35:58 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.tdb;

import java.io.*;
import java.util.Map;

import org.apache.commons.cli.*;

/**
 * <p>
 * Utilities defining a standard output option.
 * </p>
 * <p>
 * If the output option created by {@link #addOptions(Options)} is requested on
 * the command line processed by {@link #processCommandLine(Map, CommandLine)},
 * {@link #getOutput(Map)} will return an open {@link PrintStream} into which
 * output should be written. If the output option is not requested or if the
 * file name used is <code>"-"</code>, this is {@link System#out}.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class OutputOption {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.67
   */
  private OutputOption() {
    // Prevent instantiation
  }

  /**
   * <p>
   * Key for the standard output option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_OUTPUT = "output";
  
  /**
   * <p>
   * Single letter for the standard output option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_OUTPUT = 'o';
  
  /**
   * <p>
   * Argument name for the standard output option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ARG_OUTPUT = "FILE";
  
  /**
   * <p>
   * Standard output option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_OUTPUT =
      OptionBuilder.withLongOpt(KEY_OUTPUT)
                   .hasArg()
                   .withArgName(ARG_OUTPUT)
                   .withDescription(String.format("write output to %s instead of stdout", ARG_OUTPUT))
                   .create(LETTER_OUTPUT);

  /**
   * <p>
   * Adds the standard output option to a Commons CLI {@link Options}
   * instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public static void addOptions(Options options) {
    options.addOption(OPTION_OUTPUT);
  }

  /**
   * <p>
   * Processes a Commons CLI {@link CommandLine} instance and stores appropriate
   * information in the given options map.
   * </p>
   * 
   * @param options
   *          An options map.
   * @param cmd
   *          A Commons CLI {@link CommandLine} instance.
   * @since 1.67
   */
  public static void processCommandLine(Map<String, Object> options,
                                        CommandLine cmd) {
    String f = null;
    if (cmd.hasOption(KEY_OUTPUT)) {
      f = cmd.getOptionValue(KEY_OUTPUT);
    }
    if (f == null || "-".equals(f)) {
      options.put(KEY_OUTPUT, System.out);
    }
    else {
      try {
        options.put(KEY_OUTPUT, new PrintStream(f));
      }
      catch (FileNotFoundException fnfe) {
        AppUtil.error(VerboseOption.isVerbose(options), fnfe, "Cannot output to %s", f);
      }
    }
  }

  /**
   * <p>
   * Determines from the options map the {@link PrintStream} into which output
   * should be written, by default {@link System#out}.
   * </p>
   * 
   * @param options
   *          An options map.
   * @return An open {@link PrintStream} instance.
   * @since 1.67
   */
  public static PrintStream getOutput(Map<String, Object> options) {
    return (PrintStream)options.get(KEY_OUTPUT);
  }

}
