/*
 * $Id: HelpOption.java 39864 2015-02-18 09:10:24Z thib_gc $
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

import org.apache.commons.cli.*;

/**
 * <p>
 * Utilities defining a standard version option.
 * </p>
 * <p>
 * If the version option created by {@link #addOptions(Options)} is requested on
 * the command line,
 * <b>and then will exit with {@link System#exit(int)}</b>.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.68
 */
public class VersionOption {

  /**
   * <p>
   * Key for the version option ({@value}).
   * </p>
   * 
   * @since 1.68
   */
  protected static final String KEY_VERSION = "version";

  /**
   * <p>
   * The version option.
   * </p>
   * 
   * @since 1.68
   */
  protected static final Option OPTION_VERSION =
      Option.builder()
            .longOpt(KEY_VERSION)
            .desc("output version information and quit")
            .build();
  
  /**
   * <p>
   * Adds the standard version option to a Commons CLI {@link Options} instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.68
   */
  public static void addOptions(Options options) {
    options.addOption(OPTION_VERSION);
  }

  /**
   * <p>
   * 
   * </p>
   * 
   * @param cmd
   * @param versionStrings
   * @since 1.68
   */
  public static void processCommandLine(CommandLineAccessor cmd,
                                        String... versionStrings) {
    if (cmd.hasOption(KEY_VERSION)) {
      StringBuilder sb = new StringBuilder();
      for (String versionString : versionStrings) {
        sb.append(versionString);
      }
      System.out.println(sb.toString());
      System.exit(0);
    }
  }
  
}
