/*
 * $Id: WrapperLogger.java,v 1.1 2003-07-25 00:29:05 tyronen Exp $
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
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */
import java.util.*;

public class WrapperLogger {

  static Logger wrapLog = Logger.getLogger("wrappedPlugin");
  static final int LOG_LEVEL = Logger.LEVEL_DEBUG3;

  public static void record_call(String classname, String methodname,
                                 List args) {
    StringBuffer buf = new StringBuffer("Method");
    buf.append(methodname);
    buf.append(" of class ");
    buf.append(classname);
    buf.append(" was called with arguments ");
    Iterator it = args.iterator();

  }

  public static void record_throwable(String classname, String methodname,
                                      Throwable e) {
    wrapLog.log(LOG_LEVEL,"Method " + methodname + "of class " + classname, e);
  }

  public static void record_val(String classname, String methodname,
                                Object retval) {
    wrapLog.log(LOG_LEVEL,"Method " + methodname + "of class " + classname
                + "returned value " + retval.toString());
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


}