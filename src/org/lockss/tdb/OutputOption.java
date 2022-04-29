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
import java.util.*;

import org.apache.commons.cli.*;

/**
 * <p>
 * Utilities defining a standard output option.
 * </p>
 * <p>
 * If the output option created by {@link #addOptions(Options)} is requested on
 * the command line processed by
 * {@link #processCommandLine(Map, CommandLineAccessor)},
 * {@link #getOutput(Map)} will return an open {@link PrintStream} into which
 * output should be written. If the output option is not requested or if the
 * file name used is <code>"-"</code>, this is a wrapper for {@link System#out}
 * that cannot actually be closed.
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
      Option.builder(Character.toString(LETTER_OUTPUT))
            .longOpt(KEY_OUTPUT)
            .hasArg()
            .argName(ARG_OUTPUT)
            .desc(String.format("write output to %s instead of stdout", ARG_OUTPUT))
            .build();

  /**
   * <p>
   * Key for an explicit output option flag ({@value}).
   * </p>
   * 
   * @since 1.67
   */
  protected static final String KEY_OUTPUT_REQUESTED = KEY_OUTPUT + "_requested";
  
  /**
   * <p>
   * Adds the standard output option to a Commons CLI {@link Options}
   * instance.
   * </p>
   * 
   * @param opts
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public static void addOptions(Options opts) {
    opts.addOption(Option.builder(Character.toString(LETTER_OUTPUT))
                   .longOpt(KEY_OUTPUT)
                   .hasArg()
                   .argName("OUTFILE")
                   .desc("write output to OUTFILE instead of stdout")
                   .build());
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
    options.put(KEY_OUTPUT_REQUESTED, Boolean.valueOf(cmd.hasOption(KEY_OUTPUT)));
    String f = cmd.getOptionValue(KEY_OUTPUT);
    if (f == null || "-".equals(f)) {
      options.put(KEY_OUTPUT, new PrintStream(System.out) {
        @Override
        public void close() {
          // Don't close stdout
        }
      });
    }
    else {
      try {
        options.put(KEY_OUTPUT, new PrintStream(f));
      }
      catch (FileNotFoundException fnfe) {
        AppUtil.error(options, fnfe, "%s: error opening file for writing", f);
      }
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
  public static boolean isSingleOutput(Map<String, Object> options) {
    return ((Boolean)options.get(KEY_OUTPUT_REQUESTED)).booleanValue();
  }
  
  /**
   * <p>
   * Returns an open print stream for the output file requested at the command
   * line, or if not requested (or if the requested file name is <code>-</code>
   * ), a wrapper for {@link System.out} that cannot actually be closed.
   * </p>
   * 
   * @param options
   *          The options maps.
   * @return An open print stream.
   * @since 1.67
   */
  public static PrintStream getSingleOutput(Map<String, Object> options) {
    return (PrintStream)options.get(KEY_OUTPUT);
  }
  
}
