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

import java.io.*;
import java.util.Map;

import org.apache.commons.cli.*;

/**
 * <p>
 * Utilities defining a standard directory output option.
 * </p>
 * <p>
 * If the directory output option created by {@link #addOptions(Options)} is
 * requested on the command line processed by
 * {@link #processCommandLine(Map, CommandLineAccessor)},
 * {@link #isMultipleOutput(Map)} will return true and
 * {@link #getMultipleOutput(Map, String, String)} will return an open
 * {@link PrintStream} into which output should be written for the given input
 * file.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class OutputDirectoryOption {

  /**
   * <p>
   * Key for the standard output directory option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_OUTPUT_DIRECTORY = "output-dir";
  
  /**
   * <p>
   * Argument name for the standard output directory option ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String ARG_OUTPUT_DIRECTORY = "DIR";
  
  /**
   * <p>
   * Standard output directory option.
   * </p>
   * 
   * @since 1.67
   */
  protected static final Option OPTION_OUTPUT_DIRECTORY =
      OptionBuilder.withLongOpt(KEY_OUTPUT_DIRECTORY)
                   .hasArg()
                   .withArgName(ARG_OUTPUT_DIRECTORY)
                   .withDescription(String.format("write output for each input file to a file in %s", ARG_OUTPUT_DIRECTORY))
                   .create();

  /**
   * <p>
   * Adds the standard output directory option to a Commons CLI {@link Options}
   * instance.
   * </p>
   * 
   * @param options
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public static void addOptions(Options options) {
    options.addOption(OPTION_OUTPUT_DIRECTORY);
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
    if (cmd.hasOption(KEY_OUTPUT_DIRECTORY)) {
      File dir = new File(cmd.getOptionValue(KEY_OUTPUT_DIRECTORY));
      if (!dir.exists()) {
        AppUtil.error("%s: directory does not exist", dir);
      }
      if (!dir.isDirectory()) {
        AppUtil.error("%s: not a directory", dir);
      }
      options.put(KEY_OUTPUT_DIRECTORY, dir);
    }
  }

  /**
   * <p>
   * Determines if this option has been requested on the command line.
   * </p>
   * 
   * @param options
   *          An options map.
   * @return True if and only if this option has been requested on the command
   *         line.
   * @since 1.67
   */
  public static boolean isMultipleOutput(Map<String, Object> options) {
    return options.containsKey(KEY_OUTPUT_DIRECTORY);
  }
  
  /**
   * <p>
   * Returns an open output print stream for the given input file, using the
   * given extension for the output file.
   * </p>
   * <p>
   * The requested output file extension is substituted for <code>.tdb</code> if
   * that is the extension of the input file's base name, or appended if not.
   * For example if the requested output file extension is <code>.xml</code>:
   * </p>
   * <ul>
   * <li><code>/path/to/input/file.tdb</code> goes to
   * <code>/path/to/output/file.xml</code></li>
   * <li><code>/path/to/input/file.tdb.bak</code> goes to
   * <code>/path/to/output/file.tdb.bak.xml</code></li>
   * </ul>
   * 
   * @param options
   *          The options map.
   * @param inputFileName
   *          The name of the input file.
   * @param outputExtension
   *          The output file extension.
   * @return An open print stream corresponding to the input file, in the output
   *         directory requested at the command line, using the output extension.
   * @throws FileNotFoundException
   *           If the output file cannot be created, or if some other error
   *           occurs while opening or creating the output file
   * @since 1.67
   */
  public static PrintStream getMultipleOutput(Map<String, Object> options,
                                              String inputFileName,
                                              String outputExtension)
      throws FileNotFoundException {
    String baseName = new File(inputFileName).getName();
    if (baseName.endsWith(".tdb")) {
      baseName = baseName.substring(0, baseName.length() - 4);
    }
    baseName = baseName + outputExtension;
    File outputDir = (File)options.get(KEY_OUTPUT_DIRECTORY);
    File outputFile = new File(outputDir, baseName);
    return new PrintStream(outputFile);
  }
  
}
