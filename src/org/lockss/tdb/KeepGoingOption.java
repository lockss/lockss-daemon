/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.apache.commons.cli.*;

/**
 * <p>
 * Utilities defining a standard keep-going option.
 * </p>
 * <p>
 * If the keep-going option created by {@link #addOptions(Options)} is requested
 * on the command line processed by
 * {@link #processCommandLine(Map, CommandLineAccessor)},
 * {@link #isKeepGoing(Map)} will return <code>true</code> to indicate it; one
 * can then add errors to the list of errors via
 * {@link #addError(Map, Exception)} and retrieve them later via
 * {@link #getErrors(Map)}.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class KeepGoingOption {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.67
   */
  private KeepGoingOption() {
    // Prevent instantiation
  }

  /**
   * <p>
   * Key for the standard input option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_KEEP_GOING = "keep-going";
  
  /**
   * <p>
   * Single letter for the standard input option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_KEEP_GOING = 'k';
  
  /**
   * <p>
   * Standard keep-going option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_KEEP_GOING =
      Option.builder(Character.toString(LETTER_KEEP_GOING))
            .longOpt(KEY_KEEP_GOING)
            .desc(String.format("do not stop at the first error; keep going until all files are processed"))
            .build();
  

  /**
   * <p>
   * Key for the keep-going option's error list.
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_KEEP_GOING_ERRORS = KEY_KEEP_GOING + "__errors";
  
  /**
   * <p>
   * Adds the standard keep-going option to a Commons CLI {@link Options}
   * instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public static void addOptions(Options options) {
    options.addOption(OPTION_KEEP_GOING);
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
    options.put(KEY_KEEP_GOING, Boolean.valueOf(cmd.hasOption(KEY_KEEP_GOING)));
    options.put(KEY_KEEP_GOING_ERRORS, new ArrayList<Exception>());
  }

  /**
   * <p>
   * Determines from the options map if the keep-going option has been requested
   * on the command line.
   * </p>
   * 
   * @param options
   *          An options map.
   * @return <code>true</code> if and only if the keep-going option has been
   *         requested on the command line.
   * @since 1.67
   */
  public static boolean isKeepGoing(Map<String, Object> options) {
    Boolean keepGoing = (Boolean)options.get(KEY_KEEP_GOING);
    return keepGoing != null && keepGoing.booleanValue();
  }
  
  /**
   * <p>
   * Adds an error to the list kept internally in the options map and proceeds.
   * </p>
   * 
   * @param options
   *          The options map.
   * @param exc
   *          An exception.
   * @since 1.67
   */
  public static void addError(Map<String, Object> options,
                              Exception exc) {
    getErrors(options).add(exc);
  }
  
  /**
   * <p>
   * Retrieves the list of errors kept internally in the options map.
   * </p>
   * 
   * @param options
   *          The options map.
   * @return The list of errors stored in the options map.
   * @since 1.67
   */
  public static List<Exception> getErrors(Map<String, Object> options) {
    return (List<Exception>)options.get(KEY_KEEP_GOING_ERRORS);
  }

}
