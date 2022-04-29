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
      Option.builder()
            .longOpt(KEY_OUTPUT_DIRECTORY)
            .hasArg()
            .argName(ARG_OUTPUT_DIRECTORY)
            .desc(String.format("write output for each input file to a file in %s", ARG_OUTPUT_DIRECTORY))
            .build();

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
