/*
 * $Id: WrapperLogger.java,v 1.4 2004-01-27 00:41:49 tyronen Exp $
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

import java.util.*;

/**
 * Static class for use in the wrapper layer.
 * @author Tyrone Nicholas
 */

public class WrapperLogger {

  public static final String WRAPPED_LOG_NAME = "WrappedPlugin";

  private static Logger wrapLog;

  static {
   wrapLog = Logger.getLogger(WRAPPED_LOG_NAME);
  }

  public static void record_call(String classname, String methodname,
                                 List args) {
    StringBuffer buf = new StringBuffer("Method ");
    buf.append(methodname);
    buf.append(" of class ");
    buf.append(classname);
    buf.append(" was called with arguments ");
    Iterator it = args.iterator();
    while (it.hasNext()) {
      try {
        buf.append(it.next().toString());
      } catch (NullPointerException e) {
        // Plugin.configureAU is supposed to be called with a null parameter,
        // that is its normal operation
        if (!classname.equals("Plugin") || !methodname.equals("configureAU")) {
          wrapLog.warning("Null argument passed to method " + methodname +
                          " of class " + classname);
        }
      }
      buf.append(", ");
    }
    int len = buf.length();
    buf.delete(len-2,len);
    wrapLog.debug3(buf.toString());
  }


  public static void record_throwable(String classname, String methodname,
                                      Throwable e) {
    wrapLog.warning("Method " + methodname + " of class " + classname, e);
  }

  public static void record_val(String classname, String methodname,
                                Object retval) {
    String value = (retval==null) ? "null" : retval.toString();
    wrapLog.debug3("Method " + methodname + " of class " + classname
                + " returned " + value);
  }

  public static void record_val(String classname, String methodname,
                                float retval) {
    record_val(classname, methodname, new Float(retval));
  }

  public static void record_val(String classname, String methodname,
                                double retval) {
    record_val(classname, methodname, new Double(retval));
  }

  public static void record_val(String classname, String methodname,
                                boolean retval) {
    record_val(classname, methodname, new Boolean(retval));

  }

  public static void record_val(String classname, String methodname,
                                byte retval) {
    record_val(classname, methodname, new Byte(retval));

  }

  public static void record_val(String classname, String methodname,
                                char retval) {
    record_val(classname, methodname, new Character(retval));

  }

  public static void record_val(String classname, String methodname,
                                short retval) {
    record_val(classname, methodname, new Short(retval));
  }

  public static void record_val(String classname, String methodname,
                                int retval) {
    record_val(classname, methodname, new Integer(retval));
  }

  public static void record_val(String classname, String methodname,
                                long retval) {
    record_val(classname, methodname, new Long(retval));
  }

  public static void setLevel(int level) {
    wrapLog.setLevel(level);
  }

  public static int getLevel() {
    return wrapLog.level;
  }

}
