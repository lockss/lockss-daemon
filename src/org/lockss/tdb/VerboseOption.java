/*
 * $Id$
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

import java.util.Map;

import org.apache.commons.cli.*;

/**
 * <p>
 * Utilities defining a standard verbose option.
 * </p>
 * <p>
 * If the verbose option created by {@link #addOptions(Options)} is requested on
 * the command line processed by
 * {@link #processCommandLine(Map, CommandLineAccessor)},
 * {@link #isVerbose(Map)} will return <code>true</code> to indicate it.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class VerboseOption {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.67
   */
  private VerboseOption() {
    // Prevent instantiation
  }
  
  /**
   * <p>
   * Key for the standard verbose option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_VERBOSE = "verbose";
  
  /**
   * <p>
   * Single letter for the standard verbose option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_VERBOSE = 'v';
  
  /**
   * <p>
   * Standard verbose option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_VERBOSE =
      OptionBuilder.withLongOpt(KEY_VERBOSE)
                   .withDescription("output verbose error messages")
                   .create(LETTER_VERBOSE);

  /**
   * <p>
   * Adds the standard verbose option to a Commons CLI {@link Options} instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public static void addOptions(Options options) {
    options.addOption(OPTION_VERBOSE);
  }

  /**
   * <p>
   * Processes a {@link CommandLineAccessor} instance and stores appropriate
   * information in the given options map.
   * </p>
   * 
   * @param options
   *          An options map.
   * @param cmd
   *          A {@link CommandLineAccessor} instance.
   * @since 1.67
   */
  public static void processCommandLine(Map<String, Object> options,
                                        CommandLineAccessor cmd) {
    options.put(KEY_VERBOSE, Boolean.valueOf(cmd.hasOption(KEY_VERBOSE)));
  }

  /**
   * <p>
   * Determines from the options map if the verbose option has been requested
   * on the command line.
   * </p>
   * 
   * @param options
   *          An options map.
   * @return <code>true</code> if and only if the verbose option has been
   *         requested on the command line.
   * @since 1.67
   */
  public static boolean isVerbose(Map<String, Object> options) {
    Boolean verbose = (Boolean)options.get(KEY_VERBOSE);
    return verbose != null && verbose.booleanValue();
  }

}
