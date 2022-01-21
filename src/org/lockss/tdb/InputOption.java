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

import java.io.File;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

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
   * Adds the standard input option to a Commons CLI {@link Options}
   * instance.
   * </p>
   * 
   * @param opts
   *          A Commons CLI {@link Options} instance.
   * @since 1.67
   */
  public static void addOptions(Options opts) {
    opts.addOption(Option.builder(Character.toString(LETTER_INPUT))
                   .longOpt(KEY_INPUT)
                   .hasArg()
                   .argName("INFILE")
                   .desc("read input from INFILE instead of list of files")
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
    String[] args = cmd.getArgs();
    if (cmd.hasOption(KEY_INPUT) && args.length > 0) {
      AppUtil.error("--%s cannot be used with a list of input files", KEY_INPUT);
    }
    if (cmd.hasOption(KEY_INPUT)) {
    	/* This must be a file - check happens farther on */
    	options.put(KEY_INPUT, Arrays.asList(cmd.getOptionValue(KEY_INPUT)));
    }
    else {
      if (args.length > 0) {
        options.put(KEY_INPUT, fileListFromArgs(args));
      }
    }
  }
  
  /*
   * Loop over the given arguments - 
   * For file arguments, just add them to the list of input arguments
   * For directory arguments, add any ".tdb" files that live in or below that directory to the
   * list of input arguments
   */
  private static List<String> fileListFromArgs(String[] argList) {
	  List<String> retList = new ArrayList<String>();
	  for (String oneArg : argList) {
		  retList.addAll(fileListFromOneArg(oneArg));
	  }
	  return retList;
  }

  private static final String[] TDBSUFFIX = {"tdb"};  // use the suffix to filter - dot is assumed in listFiles

  private static List<String> fileListFromOneArg(String oneArg) {
	  File aFile = new File(oneArg);
	  if (aFile.isDirectory()) {
		  List<String> retList = new ArrayList<String>();
		  for (File tdbfile : FileUtils.listFiles(aFile, TDBSUFFIX, true)) {
			  retList.add(tdbfile.toString());
		  }
		  retList.sort(String::compareTo);
		  return retList;
	  }
	  return Arrays.asList(oneArg);
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
