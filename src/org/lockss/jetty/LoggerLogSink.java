/*
 * $Id: LoggerLogSink.java,v 1.1 2003-03-12 22:11:20 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.jetty;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.mortbay.util.*;

/**
 * Jetty LogSink that logs to LOCKSS logger
 */
public class LoggerLogSink implements LogSink {

  private static StringMap levelMap = new StringMap();
  static {
    levelMap.put(Log.DEBUG, new Integer(Logger.LEVEL_DEBUG));
    levelMap.put(Log.EVENT, new Integer(Logger.LEVEL_INFO));
    levelMap.put(Log.WARN, new Integer(Logger.LEVEL_WARNING));
    levelMap.put(Log.ASSERT, new Integer(Logger.LEVEL_WARNING));
    levelMap.put(Log.FAIL, new Integer(Logger.LEVEL_ERROR));
  }
    
  private Logger logger;
    
  /** Construct a LogSink with the name Jetty. */
  public LoggerLogSink() {
    this("Jetty");
  }
    
  /** Construct a LogSink with the given name. */
  public LoggerLogSink(String name) {
    logger = Logger.getLogger(name);
    logger.debug("jetty logger created");
  }

  public void setOptions(String options) {
    logger.debug("setOptions " + options);
  }
    
  public String getOptions() {
    logger.debug("getOptions()");
    return null;
  }
    
  public void log(String tag, Object msg, Frame frame, long time) {
    int level = ((Integer)levelMap.get(tag)).intValue();
//     if (frame!=null) {
//       StackTraceElement ste = frame.getStackTraceElement();
//       lr.setSourceMethodName(ste.getMethodName());
//       lr.setSourceClassName(ste.getClassName());
//     }
    logger.log(level, msg.toString());
  }
    
  public void log(String formattedLog) {
    logger.log(Logger.LEVEL_INFO, formattedLog);
  }

  // LifeCycle stuff we don't care about
  private boolean started = true;

  public void start() {
    logger.debug("start()");
    started = true;
  }
  public void stop () {
    logger.debug("stop()");
    started = false;
  }
  public boolean isStarted() {
    return started;
  }
}
