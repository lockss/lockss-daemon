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

import org.apache.commons.cli.*;

/**
 * <p>
 * Utility class defining a help option.
 * </p>
 * <p>
 * If the help option created by {@link #addOptions(Options)} is requested on
 * the command line, {@link #parse(CommandLineAccessor, Options, Class)} will
 * display a usage and help message to {@link System#out} <b>and then will exit
 * with {@link System#exit(int)}</b>.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.72
 */
public class Help {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.72
   */
  private Help() {
    // Prevent instantiation
  }

  /**
   * <p>
   * Key for the standard help option ({@value}).
   * </p>
   * 
   * @since 1.72
   */
  public static final String KEY = "help";
  
  /**
   * <p>
   * Returns an instance of the help option.
   * </p>
   * 
   * @return An {@link Option} instance.
   * @since 1.72
   */
  public static Option option() {
    return Option.builder(Character.toString('h'))
                 .longOpt(KEY)
                 .desc("show this help message and exit")
                 .build();
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
   * @param opts
   *          A Commons CLI {@link Options} instance (not an options map).
   * @param clazz
   *          The {@link Class} instance of the program with a main method.
   * @since 1.72
   */
  public static void parse(CommandLineAccessor cmd,
                           Options opts,
                           Class<?> clazz) {
    if (cmd.hasOption(KEY)) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.setOptionComparator(new LongOptComparator());
      formatter.printHelp("java " + clazz.getName(), opts);
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
   * @since 1.72
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
