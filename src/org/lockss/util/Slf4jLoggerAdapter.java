/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * SLF4J wrapper for LOCKSS logger
 */
public final class Slf4jLoggerAdapter extends MarkerIgnoringBase {

  final org.lockss.util.Logger log;

  Slf4jLoggerAdapter(org.lockss.util.Logger log, String name) {
    this.log = log;
    this.name = name;
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isDebug3();
  }

  @Override
  public void trace(String msg) {
    log.debug3(msg);
  }

  @Override
  public void trace(String format, Object arg) {
    if (isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      log.debug3(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    if (isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      log.debug3(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void trace(String format, Object... arguments) {
    if (isTraceEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      log.debug3(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void trace(String msg, Throwable t) {
    log.debug3(msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isDebug();
  }

  @Override
  public void debug(String msg) {
    log.debug(msg);
  }

  @Override
  public void debug(String format, Object arg) {
    if (isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      log.debug(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    if (isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      log.debug(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void debug(String format, Object... arguments) {
    if (isDebugEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      log.debug(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void debug(String msg, Throwable t) {
    log.debug(msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return log.isLevel(org.lockss.util.Logger.LEVEL_DEBUG);
  }

  @Override
  public void info(String msg) {
    log.info(msg);
  }

  @Override
  public void info(String format, Object arg) {
    if (isInfoEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      log.info(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    if (isInfoEnabled()) {

      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      log.info(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void info(String format, Object... arguments) {
    if (isInfoEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      log.info(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void info(String msg, Throwable t) {
    log.info(msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return log.isLevel(org.lockss.util.Logger.LEVEL_WARNING);
  }

  @Override
  public void warn(String msg) {
    log.warning(msg);
  }

  @Override
  public void warn(String format, Object arg) {
    if (isWarnEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      log.warning(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    if (isWarnEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      log.warning(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void warn(String format, Object... arguments) {
    if (isWarnEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      log.warning(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void warn(String msg, Throwable t) {
    log.warning(msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return log.isLevel(org.lockss.util.Logger.LEVEL_ERROR);
  }

  @Override
  public void error(String msg) {
    log.error(msg);
  }

  @Override
  public void error(String format, Object arg) {
    if (isErrorEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg);
      log.error(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    if (isErrorEnabled()) {
      FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
      log.error(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void error(String format, Object... arguments) {
    if (isErrorEnabled()) {
      FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
      log.error(ft.getMessage(), ft.getThrowable());
    }
  }

  @Override
  public void error(String msg, Throwable t) {
    log.error(msg, t);
  }

}
