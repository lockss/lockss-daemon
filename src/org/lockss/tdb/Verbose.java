/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import java.util.Map;

import org.apache.commons.cli.*;

/**
 * <p>
 * Utility class defining a verbose option.
 * </p>
 * <p>
 * If the verbose option created by {@link #option()} is requested on the
 * command line by {@link #parse(Map, CommandLineAccessor)},
 * {@link #isVerbose(Map)} will return <code>true</code>.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.72
 */
public class Verbose {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.72
   */
  private Verbose() {
    // Prevent instantiation
  }
  
  /**
   * <p>
   * Key for the standard verbose option ({@value}).
   * </p>
   * 
   * @since 1.72
   */
  public static final String KEY = "verbose";
  
  /**
   * <p>
   * Returns an instance of the output data option.
   * </p>
   * 
   * @return An {@link Option} instance.
   * @since 1.72
   */
  public static Option option() {
    return Option.builder(Character.toString('v'))
                 .longOpt(KEY)
                 .desc("output verbose error messages")
                 .build();
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
   * @since 1.72
   */
  public static void parse(Map<String, Object> options,
                           CommandLineAccessor cmd) {
    options.put(KEY, Boolean.valueOf(cmd.hasOption(KEY)));
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
   * @since 1.72
   */
  public static boolean isVerbose(Map<String, Object> options) {
    Boolean verbose = (Boolean)options.get(KEY);
    return verbose != null && verbose.booleanValue();
  }

}
