/*
 * $Id: ParameterInterpreter.java,v 1.1 2014/04/14 23:08:24 clairegriffin Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.crawljax;

import org.apache.commons.cli.*;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.File;
import java.io.PrintWriter;

class ParameterInterpreter {

  static final String HELP_MESSAGE =
      "java -jar crawljax-lockss.jar url outDir";
  static final String HELP = "help";
  static final String LOG = "log";
  static final String DEBUG = "debug";
  static final String BUILDER = "builder";
  static final String CONFIG = "config";
  private static final int SPACES_AFTER_OPTION = 3;
  private static final int SPACES_BEFORE_OPTION = 5;
  private static final int ROW_WIDTH = 80;
  private final Options m_options;
  private final CommandLine m_parameters;

  ParameterInterpreter(String args[]) throws ParseException {
    m_options = getOptions();
    this.m_parameters = new GnuParser().parse(m_options, args);
  }

  /**
   * Create the CML Options.
   *
   * @return Options expected from command-line.
   */
  private Options getOptions() {
    Options options = new Options();
    options.addOption("b", BUILDER, true,
                      "LockssConfigurationBuilder class name");
    options.addOption("c",CONFIG, true, "Configuration file name");
    options.addOption("l", LOG, true, "File to log output");
    options.addOption("h", HELP, false, "print this message");
    options.addOption("d", DEBUG, false, "output verbose messages");
    return options;
  }


  /**
   * Do we have the expected number of command line arguments. There should be 3.
   * The url must be an allowed url and the config file must exist.
   *
   * @return true if we do, false if we don't
   */
  boolean necessaryArgsProvided() {
    if (m_parameters.getArgs().length == 2) {
      checkUrlValidity(getUrl());
      return true;
    } else {
      return false;
    }
  }

  /**
   * Determine if the url is an http or https url.
   *
   * @param urlValue the url to test.
   */
  private void checkUrlValidity(String urlValue) {
    String[] schemes = {"http", "https"};
    if (urlValue == null || !new UrlValidator(schemes).isValid(urlValue)) {
      throw new IllegalArgumentException("Invalid URL: " + urlValue+"" +
                              ". Provide a valid URL like http://example.com");
    }
  }


  /**
   * get the url we are crawling. This should be the first commandline argument.
   *
   * @return the string value of the first command line arguement
   */
  String getUrl() {
    return m_parameters.getArgs()[0];
  }

  /**
   * get the file to use for output. This should be the third commandline arg
   *
   * @return the string value of the directory to use for output files.
   */
  String getOutputDir() {
    return m_parameters.getArgs()[1];
  }

  /**
   * get the file to use to configure our crawl.  This should be the second
   * commandline argument
   *
   * @return the string value of the configuration file.
   */
  boolean hasConfigFile() {
    if(m_parameters.hasOption(CONFIG)) {
      File f_config = new File(m_parameters.getOptionValue(CONFIG));
      return f_config.exists() && f_config.canRead();
    }
    return false;
 }

  /**
   * Get the optional assigned value for -c or -config
   * @return -c or -config value or null
   */
  String getConfigFile() {
   if(hasConfigFile())
     return m_parameters.getOptionValue(CONFIG);
   return null;
 }
  /**
   * does the command line contain a request for help
   *
   * @return true if -h was passed as a command line argument.
   */
  boolean requestsHelp() {
    return m_parameters.hasOption(HELP);
  }

  boolean requestsDebug() {
    return m_parameters.hasOption(DEBUG);
  }

  public boolean requestsBuilder() {
    return m_parameters.hasOption(BUILDER);
  }

  public String getRequestedBuilder() {
    return m_parameters.getOptionValue(BUILDER);
  }

  boolean specifiesLogFile() {
    return m_parameters.hasOption(LOG);
  }

  String getSpecifiedLogFile() {
    return m_parameters.getOptionValue(LOG);
  }

  /**
   * Print out the help message.  For use on the command line when running at the
   * console.
   */
  void printHelp() {
    String cmlSyntax = HELP_MESSAGE;
    final PrintWriter writer = new PrintWriter(System.out);
    final HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp(writer, ROW_WIDTH, cmlSyntax, "",
                            m_options, SPACES_AFTER_OPTION,
                            SPACES_BEFORE_OPTION, "");
    writer.flush();
  }


}
