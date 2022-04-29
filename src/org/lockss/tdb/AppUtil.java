/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
    if (options != null && Verbose.isVerbose(options) && exceptions != null) {
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
    if (Verbose.isVerbose(options) && exc != null) {
      exc.printStackTrace(System.err);
    }
    System.err.println(msg);
    if (!KeepGoing.isKeepGoing(options)) {
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
  
}
