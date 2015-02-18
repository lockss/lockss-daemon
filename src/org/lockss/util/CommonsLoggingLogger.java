/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

/**
 * Implementation of Jakarta Commons logging interface that logs using
 * {@link org.lockss.util.Logger}.  This is installed via the file
 * src/commons-logging.properties, which is copied to top level in the jar
 * by build.xml.  <p>Until Logger has been configured for the first time
 * (in order to set log levels), the initial log level for these logs is
 * "info", unless overridden by the system property
 * org.lockss.defaultCommonsLogLevel.  This is because HttpClient is used
 * to read the LOCKSS configuration from the property server, and an
 * initial level of debug (which we often use) causes excessive output
 * during startup.
 */
public class CommonsLoggingLogger implements org.apache.commons.logging.Log {
  static final String PARAM_DEFAULT_COMMONS_LOG_LEVEL =
    "org.lockss.defaultCommonsLogLevel";
  static final String DEFAULT_DEFAULT_COMMONS_LOG_LEVEL = "info";

  private Logger log;

  /** Create a Logger with the specified name,  */
  public CommonsLoggingLogger(String name) {
    String s = System.getProperty(PARAM_DEFAULT_COMMONS_LOG_LEVEL);
    if (StringUtil.isNullString(s)) {
      s = DEFAULT_DEFAULT_COMMONS_LOG_LEVEL;
    }
    int pos = name.lastIndexOf('.');
    if (pos > 0) {
      name = name.substring(pos + 1);
    }
    log = Logger.getLoggerWithDefaultLevel(name, s,
					   PARAM_DEFAULT_COMMONS_LOG_LEVEL);
  }

  public boolean isDebugEnabled() {
    return log.isLevel(Logger.LEVEL_DEBUG);
  }

  public boolean isErrorEnabled() {
    return log.isLevel(Logger.LEVEL_ERROR);
  }

  public boolean isFatalEnabled() {
    return log.isLevel(Logger.LEVEL_CRITICAL);
  }

  public boolean isInfoEnabled() {
    return log.isLevel(Logger.LEVEL_INFO);
  }

  public boolean isTraceEnabled() {
    return log.isLevel(Logger.LEVEL_DEBUG3);
  }

  public boolean isWarnEnabled() {
    return log.isLevel(Logger.LEVEL_WARNING);
  }

  public void trace(Object message) {
    log(Logger.LEVEL_DEBUG3, message);
//     log(Logger.LEVEL_DEBUG3, message, new Exception());
  }

  public void trace(Object message, Throwable t) {
    log(Logger.LEVEL_DEBUG3, message, t);
  }

  public void debug(Object message) {
    log(Logger.LEVEL_DEBUG, message);
  }

  public void debug(Object message, Throwable t) {
    log(Logger.LEVEL_DEBUG, message, t);
  }

  public void info(Object message) {
    log(Logger.LEVEL_INFO, message);
  }

  public void info(Object message, Throwable t) {
    log(Logger.LEVEL_INFO, message, t);
  }

  public void warn(Object message) {
    log(Logger.LEVEL_WARNING, message);
  }

  public void warn(Object message, Throwable t) {
    log(Logger.LEVEL_WARNING, message, t);
  }

  public void error(Object message) {
    log(Logger.LEVEL_ERROR, message);
  }

  public void error(Object message, Throwable t) {
    log(Logger.LEVEL_ERROR, message, t);
  }

  public void fatal(Object message) {
    log(Logger.LEVEL_CRITICAL, message);
  }

  public void fatal(Object message, Throwable t) {
    log(Logger.LEVEL_CRITICAL, message, t);
  }

  private void log(int level, Object message) {
    log(level, message, null);
  }

  private void log(int level, Object message, Throwable t) {
    log.log(level, String.valueOf(message), t);
  }
}
