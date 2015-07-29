/*
 * $Id: LockssCrawljaxRunner.java,v 1.1 2014/04/14 23:08:24 clairegriffin Exp $
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
package org.lockss.crawljax;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.apache.commons.cli.ParseException;
import  org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class LockssCrawljaxRunner {

  /**
   * The message to display when we are missing a required command line arg
   */
  static final String MISSING_ARGUMENT_MESSAGE =
      "Missing required argument URL, configFile and/or output directory.";

  /* ----------------------------------------------------------------------- */
  /*                Member Variables                                         */
  /* ----------------------------------------------------------------------- */

  /**
   * The ParameterInterpreter which manages our command line options.
   */
  private final ParameterInterpreter m_options;
  /**
   * The CrawljaxConfiguration object for this crawl.
   */
  private  CrawljaxConfiguration m_config;

  /**
   * The CrawljaxConfigurationBuilder for this crawl.
   */
  private  CrawljaxConfigurationBuilder m_builder;

  /**
   * The main execution class for a LOCKSS crawljax crawl
   *
   * @param args the command line arguments recieved on startup
   */
  LockssCrawljaxRunner(String args[]) {
    try {
      this.m_options = new ParameterInterpreter(args);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
    if (m_options.necessaryArgsProvided()) {
      setupLogging();
      LockssConfigurationBuilder config_builder = setupBuilder();
      m_builder = config_builder.configure(m_options.getUrl(),
                                           m_options.getOutputDir(),
                                           m_options.getConfigFile());
    } else {
      if (!m_options.requestsHelp()) {
        System.out.println(MISSING_ARGUMENT_MESSAGE);
      }
      m_options.printHelp();
      m_config = null;
    }
  }

  public CrawljaxConfiguration getConfig() {
    return m_config;
  }

  public CrawljaxConfigurationBuilder getBuilder() {
    return m_builder;
  }

  public ParameterInterpreter getOptions() {
    return m_options;
  }

  /**
   * Main executable method of LOCKSS Crawljax Runner.
   *
   * @param args the command line arguments.
   */
  public static void main(String[] args) {
    try {
      LockssCrawljaxRunner lockss_runner = new LockssCrawljaxRunner(args);
      CrawljaxConfiguration config = lockss_runner.getBuilder().build();
      CrawljaxRunner runner = new CrawljaxRunner(config);
      runner.call();
    } catch (NumberFormatException e) {
      System.err.println("Could not parse number " + e.getMessage());
      System.exit(1);
    } catch (RuntimeException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }


  /**
   * setup a LockssConfigurationBuilder which will be used to turn our
   * properties file into a CrawljaxConfiguration.
   *
   * @return a LockssConfigurationBuilder
   *
   * @throws java.lang.IllegalStateException if a class was requested but was
   * unable to instantiated.
   */
  LockssConfigurationBuilder setupBuilder() {
    if (m_options.requestsBuilder()) {
      String builder_class_name = m_options.getRequestedBuilder();
      try {
        Class clazz = Class.forName(builder_class_name);
        return LockssConfigurationBuilder.class.cast(clazz.newInstance());
      } catch (final InstantiationException e) {
        throw new IllegalStateException(e);
      } catch (final IllegalAccessException e) {
        throw new IllegalStateException(e);
      } catch (final ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    } else {
      return new DefLockssConfigurationBuilder();
    }
  }

  /**
   * configure logging to console vs file and set the log level
   */
  void setupLogging() {
    if (m_options.requestsDebug()) {
      Logger rootLogger =
          (Logger) LoggerFactory.getLogger("org.lockss.crawljax");
      rootLogger.setLevel(Level.DEBUG);
    }
    if (m_options.specifiesLogFile()) {
      File f = new File(m_options.getSpecifiedLogFile());
      try {
        if (!f.exists()) {
          Files.createParentDirs(f);
          Files.touch(f);
        }
      } catch (IOException e) {
        System.out.println("Could not create log file: " + e.getMessage());
      }
      Preconditions.checkArgument(f.canWrite());
      logToFile(f.getPath());
    }
  }

  /**
   * Log to a file with the given filename
   *
   * @param filename the name of the file to log in
   */
  void logToFile(String filename) {
    Logger rootLogger =
        (Logger)  LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    FileAppender<ILoggingEvent> fileappender =
        new FileAppender<ILoggingEvent>();
    fileappender.setContext(rootLogger.getLoggerContext());
    fileappender.setFile(filename);
    fileappender.setName("FILE");
    ConsoleAppender<?> console =
        (ConsoleAppender<?>) rootLogger.getAppender("STDOUT");
    fileappender.setEncoder((Encoder<ILoggingEvent>) console.getEncoder());
    fileappender.start();
    rootLogger.addAppender(fileappender);
    console.stop();
  }
}
