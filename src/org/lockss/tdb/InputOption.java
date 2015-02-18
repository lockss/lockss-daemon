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

import org.apache.commons.cli.*;

/**
 * <p>
 * Utilities defining standard input option.
 * </p>
 * <p>
 * If the input option created by {@link #addOptions(Options)} is requested on
 * the command line processed by
 * {@link #processCommandLine(Map, CommandLineAccessor)}, {@link #getInput(Map)}
 * will return a list of strings representing files from which input should be
 * read. If the input option itself is used, the list will contain its argument
 * as the only element, otherwise the list will contain one or more arguments
 * from the invoking program's command line. It is an error if no input file are
 * specified or if input files are specified both via the input option and
 * arguments to the invoking program. To request input from {@link System#in},
 * use <code>"-"</code> as a file name.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class InputOption {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.67
   */
  private InputOption() {
    // Prevent instantiation
  }
  
  /**
   * <p>
   * Key for the standard input option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_INPUT = "input";
  
  /**
   * <p>
   * Single letter for the standard input option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final char LETTER_INPUT = 'i';
  
  /**
   * <p>
   * Argument name for the standard input option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ARG_INPUT = "FILE";
  
  /**
   * <p>
   * Standard input option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_INPUT =
      OptionBuilder.withLongOpt(KEY_INPUT)
                   .hasArg()
                   .withArgName(ARG_INPUT)
                   .withDescription(String.format("read input from %s instead of list of input files", ARG_INPUT))
                   .create(LETTER_INPUT);
  
  /**
   * <p>
   * Adds the standard input option to a Commons CLI {@link Options}
   * instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public static void addOptions(Options options) {
    options.addOption(OPTION_INPUT);
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
    String[] args = cmd.getArgs();
    if (cmd.hasOption(KEY_INPUT) && args.length > 0) {
      AppUtil.error("--%s cannot be used with a list of input files", KEY_INPUT);
    }
    if (cmd.hasOption(KEY_INPUT)) {
      options.put(KEY_INPUT, Arrays.asList(cmd.getOptionValue(KEY_INPUT)));
    }
    else {
      if (args.length == 0) {
        AppUtil.error("No input files specified");
      }
      options.put(KEY_INPUT, Arrays.asList(args));
    }
  }

  /**
   * <p>
   * Determines from the options map the list of files from which input is
   * requested on the command line.
   * </p>
   * 
   * @param options
   *          An options map.
   * @return A list of file names from which input is requedted, either a single
   *         file from the input option itself or one or more files from the
   *         invoking program's command line, with <code>"-"</code> meaning
   *         {@link System#in}.
   * @since 1.67
   */
  public static List<String> getInput(Map<String, Object> options) {
    return (List<String>)options.get(KEY_INPUT);
  }

}
