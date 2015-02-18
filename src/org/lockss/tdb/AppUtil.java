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

import java.util.*;

/**
 * <p>
 * Miscellaneous utilities for command line tools in this package.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class AppUtil {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.67
   */
  private AppUtil() {
    // Prevent instantiation
  }

  /**
   * <p>
   * Displays an error message to the error console, <b>and then exits with
   * {@link System#exit(int)}</b>.
   * </p>
   * 
   * @param options
   *          The options map.
   * @param exceptions
   *          A list of exceptions whose stack traces are output to the error
   *          console if non null and the verbose option has been requested on
   *          the command line.
   * @param format
   *          A format string.
   * @param args
   *          Arguments for the format string.
   * @since 1.67
   */
  public static void error(Map<String, Object> options,
                           List<Exception> exceptions,
                           String format,
                           Object... args) {
    String msg = String.format(format, args);
    if (options != null && VerboseOption.isVerbose(options) && exceptions != null) {
      for (Exception exc : exceptions) {
        exc.printStackTrace(System.err);
      }
    }
    System.err.println(msg);
    System.exit(1);
  }

  /**
   * <p>
   * Displays an error message to the error console, <b>and then exits with
   * {@link System#exit(int)}</b>.
   * </p>
   * 
   * @param options
   *          The options map.
   * @param exc
   *          An exception whose stack trace is output to the error console if
   *          non null and the verbose option has been requested on the command
   *          line.
   * @param format
   *          A format string.
   * @param args
   *          Arguments for the format string.
   * @since 1.67
   */
  public static void error(Map<String, Object> options,
                           Exception exc,
                           String format,
                           Object... args) {
    error(options, exc == null ? null : Arrays.asList(exc), format, args);
  }

  /**
   * <p>
   * Displays an error message to the error console, <b>and then exits with
   * {@link System#exit(int)}</b>.
   * </p>
   * 
   * @param options
   *          The options map.
   * @param format
   *          A format string.
   * @param args
   *          Arguments for the format string.
   * @since 1.67
   */
  public static void error(String format,
                           Object... args) {
    error(null, (List<Exception>)null, format, args);
  }

  /**
   * <p>
   * Displays a warning to the error console, <b>and then exits with
   * {@link System#exit(int)}</b> unless the keep-going options has been
   * requested on the command line.
   * </p>
   * 
   * @param options
   *          The options map.
   * @param exc
   *          An exception whose stack trace is output to the error console if
   *          non null and the verbose option has been requested on the command
   *          line.
   * @param format
   *          A format string.
   * @param args
   *          Arguments for the format string.
   * @since 1.67
   * @see KeepGoingOption
   * @see VerboseOption
   */
  public static void warning(Map<String, Object> options,
                             Exception exc,
                             String format,
                             Object... args) {
    String msg = String.format(format, args);
    if (VerboseOption.isVerbose(options) && exc != null) {
      exc.printStackTrace(System.err);
    }
    System.err.println(msg);
    if (!KeepGoingOption.isKeepGoing(options)) {
      System.exit(1);
    }
  }
  
  /**
   * <p>
   * Makes an unmodifiable list from the given arguments.
   * </p>
   * 
   * @param args
   *          Zero or more arguments (varargs).
   * @return An unmodifiable list of the given arguments (in that order).
   * @since 1.67
   */
  public static <T> List<T> ul(T... args) {
    return Collections.unmodifiableList(Arrays.asList(args));
  }
  
  /**
   * <p>
   * Commons CLI seems to get confused by query strings that end in '"'.
   * Use this utility method to work around it.
   * </p>
   * 
   * @param mainArgs Arguments from main.
   * @since 1.67
   */
  public static void fixMainArgsForCommonsCli(String[] mainArgs) {
    for (int i = 0 ; i < mainArgs.length ; ++i) {
      String str = mainArgs[i];
      if (str.endsWith("\"")) {
        mainArgs[i] = str + " ";
      }
    }
  }

}
