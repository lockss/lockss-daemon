/*
 * $Id: TestWrapperLogger.java,v 1.1 2003-09-04 23:11:18 tyronen Exp $
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
import java.io.*;
import junit.framework.TestCase;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.util.TestWrapperLogger
 *
 * @author  Tyrone Nicholas
 */

public class TestWrapperLogger extends LockssTestCase {

  private MockLogTarget tgt;
  private final String classname = "TestClass";
  private final String methodname = "TestMethod";
  private final String teststr = "Debug3: " + WrapperLogger.WRAPPED_LOG_NAME +
      ": Method " + methodname + " of class " + classname + ' ';

  public static Class testedClasses[] = {
    org.lockss.util.WrapperLogger.class
  };

  public void setUp() throws Exception {
    tgt = new MockLogTarget();
    Logger.addTarget(tgt);
    WrapperLogger.setLevel(Logger.LEVEL_DEBUG3);
    super.setUp();
  }

  private void check4() throws IOException {
    check("returned 4");
  }

  private void check(String expected) throws IOException {
    assertTrue(tgt.hasMessage(teststr + expected));
  }

  public void testByte() throws IOException {
    byte four = 4;
    WrapperLogger.record_val(classname,methodname,four);
    check4();
  }

  public void testShort() throws IOException {
    short four = 4;
    WrapperLogger.record_val(classname,methodname,four);
    check4();
  }
  public void testInt() throws IOException {
    int four = 4;
    WrapperLogger.record_val(classname,methodname,four);
    check4();
  }

  public void testLong() throws IOException {
    long four = 4;
    WrapperLogger.record_val(classname,methodname,four);
    check4();
  }

  public void testChar() throws IOException {
    char four = '4';
    WrapperLogger.record_val(classname, methodname, four);
    check4();
  }

  public void testString() throws IOException {
    String four = "4";
    WrapperLogger.record_val(classname, methodname, four);
    check4();
  }

  public void testBoolean() throws IOException {
    boolean boolval = true;
    WrapperLogger.record_val(classname,methodname,boolval);
    check("returned true");
  }

  public void testNull() throws IOException {
    Object nullobj = null;
    WrapperLogger.record_val(classname,methodname,nullobj);
    check("returned null");
  }

  public void testCall() throws IOException {
    List list = new ArrayList();
    list.add(new String("mikey"));
    list.add(new Integer(4));
    list.add(new Boolean(true));
    FileDescriptor fd = new FileDescriptor();
    list.add(fd);
    WrapperLogger.record_call(classname,methodname,list);
    check("was called with arguments mikey, 4, true, " + fd.toString());
  }

  public void testThrowable() throws IOException {
    WrapperLogger.setLevel(Logger.LEVEL_WARNING);
    Exception e = new Exception("dummy exception");
    WrapperLogger.record_throwable(classname,methodname,e);
    assertTrue(tgt.hasMessage(getExceptionLogMessage(e)));
    WrapperLogger.setLevel(Logger.LEVEL_CRITICAL);
    WrapperLogger.record_throwable(classname,methodname,e);
    assertEquals(tgt.messageCount(),1);
  }

  private String getExceptionLogMessage(Throwable e) {
    StringBuffer sb = new StringBuffer();
    sb.append("Warning: ");
    sb.append(WrapperLogger.WRAPPED_LOG_NAME);
    sb.append(": Method ");
    sb.append(methodname);
    sb.append(" of class ");
    sb.append(classname);
    sb.append(": ");
    String emsg = e.toString();
    sb.append(emsg);
    sb.append("\n    ");
    sb.append(StringUtil.trimStackTrace(emsg,
                                        StringUtil.stackTraceString(e)));
    return sb.toString();
  }

  public void testLevel() {
    WrapperLogger.setLevel(Logger.LEVEL_DEBUG2);
    tgt.resetMessages();
    WrapperLogger.record_val(classname,methodname,4);
    assertEquals(tgt.messageCount(),0);
    WrapperLogger.setLevel(Logger.LEVEL_DEBUG3);
    WrapperLogger.record_val(classname,methodname,4);
    assertEquals(tgt.messageCount(),1);
  }

  public void testConfigureAU() {
    tgt.resetMessages();
    WrapperLogger.record_throwable("Plugin","configureAU",new NullPointerException());
    assertEquals(tgt.messageCount(),1);
  }
}

