/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.io.*;
import java.net.*;

//import java.text.DateFormat;


/**
 * This is a flexible logging class which will be used for all error handling
 * in the LOCKSS daemon. 
 *
 * The target of logging is abstracted away, so seperate classes can be written
 * to log to STDERR, mail, or syslog
 */

public class Logger {

  public static final String CRITICAL = "critical";
  public static final String ERROR = "error";
  public static final String WARNING = "warning";
  public static final String INFO = "info";
  public static final String DEBUG = "debug";
  private static final String[] severityStrings = {
    CRITICAL,
    ERROR,
    WARNING,
    INFO,
    DEBUG
  };
  private static final String DEFAULT_OUTPUT_LEVEL = WARNING;
  private static final String CLASS_LOG_LEVEL_PROP_STUB =
    "org.lockss.log.level.";
  private static final String GLOBAL_OUTPUT_LEVEL_PROP = 
    "org.lockss.log.level.default";

  private static String globalDisplayLevel = null;

  private static Vector targets;

  //instance variables
  private String callerId;
  private String logLevel;
  



  static {
    targets = new Vector();
    loadProps();
    addTarget(new StdErrTarget()); //XXX this should go elsewhere
  }

  /**
   * Add a target object to the logger.  Targets take a log message and 
   * write it to a form of output (stderr, a file, syslog, etc)
   *
   * @param target target object to add
   */
  public static void addTarget(LogTarget target){
    targets.addElement(target);
  }

  public static boolean isLevelAboveThreshold(String level, String threshold){
    return (getLevel(level) <= getLevel(threshold));
  }

  private static final int getLevel(String name) {
    for (int ix = 0; ix < severityStrings.length; ix++) {
      if (severityStrings[ix].equalsIgnoreCase(name)) {
	return ix;
      }
    }
    throw new IllegalArgumentException("Unknown log level: " + name);
  }

  /**
   * The following 16 functions are of the form
   * <severity>(String base, String msg[, Exception e])
   * Each of then logs base, msg and optionally e in syslog at severity level <severity>
   */
  public static void critical(String base, String msg){
    log(base, msg, null, CRITICAL);
  }

  public static void critical(String base, String msg, Exception e){
    log(base, msg, e, CRITICAL);
  }

  public static void error(String base, String msg){
    log(base, msg, null, ERROR);
  }

  public static void error(String base, String msg, Exception e){
    log(base, msg, e, ERROR);
  }

  public static void warning(String base, String msg){
    log(base, msg, null, WARNING);
  }

  public static void warning(String base, String msg, Exception e){
    log(base, msg, e, WARNING);
  }
  public static void info(String base, String msg){
    log(base, msg, null, INFO);
  }

  public static void info(String base, String msg, Exception e){
    log(base, msg, e, INFO);
  }

  public static void debug(String base, String msg){
    log(base, msg, null, DEBUG);
  }

  public static void debug(String base, String msg, Exception e){
    log(base, msg, e, DEBUG);
  }

  private static void log(String base, String msg, 
			  Exception e, String severity) {
    log(base, msg, e, severity, globalDisplayLevel);
  }
  private static void log(String base, String msg, 
			  Exception e, String severity,
			  String logLevel) {
    writeMsg(base, msg + stackTraceString(e), severity, logLevel);
  }

  
  //private

  /**
   * Loads the global display level from system properties.  
   */
  public static void loadProps() {
    globalDisplayLevel = System.getProperty(GLOBAL_OUTPUT_LEVEL_PROP,
					    DEFAULT_OUTPUT_LEVEL);
  }

  /**
   * Translate an exception's stack trace to a string.
   */
  private static String stackTraceString(Exception exp) {
    if (exp==null)
      return "";
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    
    exp.printStackTrace(pw);
    return "\n"+sw.toString();
  }

  /**
   * Translate severity to a string.
   */
  private static String severityString(int severity) {
    if (severity >= 0 && severity <= severityStrings.length) {
      return severityStrings[severity];
    } else {
      return "Severity " + severity;
    }
  }

  private synchronized static void writeMsg(String callerId, String msg, 
					    String severity, String logLevel) {
    Enumeration enum = targets.elements();
    if (isLevelAboveThreshold(severity, logLevel)){
      while (enum.hasMoreElements()){
	LogTarget curTarget = (LogTarget) enum.nextElement();
	curTarget.handleMessage(callerId, msg, severity);
      }
    }
  }

  public static Logger getLogger(String callerId){
    if (callerId == null){
      return null;
    }
    String logLevel = 
      System.getProperty(CLASS_LOG_LEVEL_PROP_STUB+callerId,
			 globalDisplayLevel);
			 //			 DEFAULT_OUTPUT_LEVEL);
    return new Logger(callerId, logLevel);
  }
  //instance methods
  private Logger(String callerId, String logLevel){
    this.callerId = callerId;
    this.logLevel = logLevel;
  }

  public void critical(String msg){
    log(callerId, msg, null, CRITICAL, logLevel);
  }

  public void critical(String msg, Exception e){
    log(callerId, msg, e, CRITICAL, logLevel);
  }

  public void error(String msg){
    log(callerId, msg, null, ERROR, logLevel);
  }

  public void error(String msg, Exception e){
    log(callerId, msg, e, ERROR, logLevel);
  }

  public void warning(String msg){
    log(callerId, msg, null, WARNING, logLevel);
  }

  public void warning(String msg, Exception e){
    log(callerId, msg, e, WARNING, logLevel);
  }
  public void info(String msg){
    log(callerId, msg, null, INFO, logLevel);
  }

  public void info(String msg, Exception e){
    log(callerId, msg, e, INFO, logLevel);
  }

  public void debug(String msg){
    log(callerId, msg, null, DEBUG, logLevel);
  }

  public void debug(String msg, Exception e){
    log(callerId, msg, e, DEBUG, logLevel);
  }

}
