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
 * Utilities defining a standard help option.
 * </p>
 * <p>
 * If the help option created by {@link #addOptions(Options)} is requested on
 * the command line,
 * {@link #processCommandLine(CommandLineAccessor, Options, Class)} will display
 * a usage and help message to {@link System#out} <b>and then will exit with
 * {@link System#exit(int)}</b>.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class HelpOption {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.67
   */
  private HelpOption() {
    // Prevent instantiation
  }

  /**
   * <p>
   * Key for the standard help option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_HELP = "help";
  
  /**
   * <p>
   * Single letter for the standard help option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_HELP = 'h';
  
  /**
   * <p>
   * Standard help option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_HELP =
      Option.builder(Character.toString(LETTER_HELP))
            .longOpt(KEY_HELP)
            .desc("display this help message and exit")
            .build();
  
  /**
   * <p>
   * Adds the standard help option to a Commons CLI {@link Options} instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public static void addOptions(Options options) {
    options.addOption(OPTION_HELP);
  }

  /**
   * <p>
   * If the help option has been requested on the command line, displays a usage
   * and help message to {@link System#out} <b>and then exits with
   * {@link System#exit(int)} and a return code of <code>0</code></b>.
   * </p>
   * 
   * @param cmd
   *          A {@link CommandLineAccessor} instance.
   * @param options
   *          A Commons CLI {@link Options} instance (not an options map).
   * @param clazz
   *          The {@link Class} instance of the program with a main method.
   * @since 1.67
   */
  public static void processCommandLine(CommandLineAccessor cmd,
                                        Options options,
                                        Class<?> clazz) {
    if (cmd.hasOption(KEY_HELP)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.setOptionComparator(new LongOptComparator());
      formatter.printHelp("java " + clazz.getName(), options);
      System.exit(0);
    }
  }

  /**
   * <p>
   * A comparator that sorts options in alphabetical order of their long option
   * (or by their single letter if unavailable).
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @since 1.67
   */
  protected static class LongOptComparator implements Comparator<Option> {
    @Override
    public int compare(Option o1, Option o2) {
      String s1 = o1.hasLongOpt() ? o1.getLongOpt() : o1.getOpt();
      String s2 = o2.hasLongOpt() ? o2.getLongOpt() : o2.getOpt();
      return s1.compareTo(s2);
    }
  }
  
}
