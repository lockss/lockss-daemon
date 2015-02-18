/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.cxf.common.logging.AbstractDelegatingLogger;

/**
 * A logging redirector for CXF logging.
 */
public class CXFLoggingLogger extends AbstractDelegatingLogger {
  static final String PARAM_DEFAULT_COMMONS_LOG_LEVEL =
      "org.lockss.defaultCommonsLogLevel";
  static final String DEFAULT_DEFAULT_COMMONS_LOG_LEVEL = "info";

  private Logger log;

  /**
   * Constructor.
   *
   * @param name A String with the name of the logger.
   * @param resourceBundleName A String with the name of the resource bundle.
   */
  public CXFLoggingLogger(String name, String resourceBundleName) {
    super(name, resourceBundleName);

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

  /**
   * Provides the logging level.
   * 
   * @return a Level with the logging level.
   */
  @Override
  public Level getLevel() {
    Level level;

    // Verify from the wider (trace) to the narrower (error).
    if (log.isLevel(Logger.LEVEL_DEBUG3)) {
      level = Level.FINEST;
    } else if (log.isLevel(Logger.LEVEL_DEBUG2)) {
      // Map to the lowest between FINER, FINE and CONFIG.
      level = Level.FINER;
    } else if (log.isLevel(Logger.LEVEL_INFO)) {
      level = Level.INFO;
    } else if (log.isLevel(Logger.LEVEL_WARNING)) {
      level = Level.WARNING;
    } else if (log.isLevel(Logger.LEVEL_ERROR)) {
      level = Level.SEVERE;
    } else {
      level = Level.OFF;
    }

    return level;
  }

  /**
   * Performs the actual log.
   * 
   * @param message A String with the logging message.
   * @param record A LogRecord with details of the logging request.
   */
  @Override
  protected void internalLogFormatted(String message, LogRecord record) {
    Level level = record.getLevel();
    Throwable t = record.getThrown();

    if (Level.FINE.equals(level)) {
      log.debug1(message, t);
    } else if (Level.INFO.equals(level)) {
      log.info(message, t);
    } else if (Level.WARNING.equals(level)) {
      log.warning(message, t);
    } else if (Level.FINER.equals(level)) {
      log.debug2(message, t);
    } else if (Level.FINEST.equals(level)) {
      log.debug3(message, t);
    } else if (Level.ALL.equals(level)) {
      log.error(message, t);
    } else if (Level.SEVERE.equals(level)) {
      log.error(message, t);
    } else if (Level.CONFIG.equals(level)) {
      log.debug(message, t);
    }
  }
}
