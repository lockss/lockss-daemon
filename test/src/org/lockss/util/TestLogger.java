/*
 * $Id: TestLogger.java,v 1.6 2002-09-19 22:18:34 tal Exp $
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

package org.lockss.util;

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.util.TestLogger
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class TestLogger extends LockssTestCase{
  public static Class testedClasses[] = {
    org.lockss.util.Logger.class
  };


  //expected debugging levels, in ascending order of seriousness
  private static final String[] levels = {
    "debug",
    "info",
    "warning",
    "error",
    "critical"
  };

  public TestLogger(String msg){
    super(msg);
  }

  public void setUp(){
  }

  public void testNames() {
    assertEquals("Critical", Logger.nameOf(Logger.LEVEL_CRITICAL));
    assertEquals("Info", Logger.nameOf(Logger.LEVEL_INFO));
    assertEquals(Logger.LEVEL_WARNING, Logger.levelOf("warning"));
    assertEquals(Logger.LEVEL_DEBUG, Logger.levelOf("debug"));
  }

  public void testLevels() {
    Logger l = Logger.getLogger("test-log");
    l.setLevel(Logger.LEVEL_WARNING);
    assertTrue(l.isLevel(Logger.LEVEL_WARNING));
    assertTrue(l.isLevel(Logger.LEVEL_ERROR));
    assertTrue(l.isLevel(Logger.LEVEL_CRITICAL));
    assertTrue( ! l.isLevel(Logger.LEVEL_INFO));
    assertTrue( ! l.isLevel(Logger.LEVEL_DEBUG));
  }

  public void testLevelFilter() {
    Logger l = Logger.getLogger("test-log");
    MockLogTarget target = new MockLogTarget();
    l.setTarget(target);
    l.setLevel(Logger.LEVEL_WARNING);
    target.resetMessages();
    assertEquals(0, target.messageCount());
    l.info("no");
    assertEquals(0, target.messageCount());
    l.warning("yes");
    assertEquals(1, target.messageCount());
    l.error("yes");
    assertEquals(2, target.messageCount());
    l.setLevel(Logger.LEVEL_CRITICAL);
    l.error("yes");
    assertEquals(2, target.messageCount());
    l.defaultTarget();
  }

  static String testOutputOutput[] = {
    "Warning: test-log: msg2 warning",
    "Error: test-log: msg3 error",
  };

  public void testOutput() {
    Logger l = Logger.getLogger("test-log");
    MockLogTarget target = new MockLogTarget();
    l.setTarget(target);
    l.setLevel(Logger.LEVEL_WARNING);
    target.resetMessages();
    l.info("msg1 info");
    l.warning("msg2 warning");
    l.error("msg3 error");
    l.setLevel(Logger.LEVEL_CRITICAL);
    l.error("msg4 error");
    Iterator iter = target.messageIterator();
    while (iter.hasNext()) {
      System.err.println((String)iter.next());
    }
    assertIsomorphic(testOutputOutput, target.messageIterator());
    l.defaultTarget();
  }

  private static final String c1 = "prop1=12\nprop2=foobar\nprop3=true\n"; 
  private static final String c1a = "prop2=xxx\nprop4=yyy\n"; 

  // Load a config with desired log level.
  // Set default to critical to suppress Config log, which would change
  // expected output.
  private void configLogLevel(String logName, int level)
      throws IOException {
    String s =
      "org.lockss.log." + logName + ".level=" + Logger.nameOf(level) + "\n" +
      "org.lockss.log.default.level=critical\n";
    TestConfiguration.
      setCurrentConfigFromUrlList(ListUtil.list(FileUtil.urlOfString(s)));
  }

  public void testLevelconfig()
      throws IOException {
    String lName = "test-log";
    Logger l = Logger.getLogger(lName);
    MockLogTarget target = new MockLogTarget();
    l.setTarget(target);
    configLogLevel(lName, Logger.LEVEL_WARNING);
    target.resetMessages();
    l.info("msg1 info");
    l.warning("msg2 warning");
    l.error("msg3 error");
    configLogLevel(lName, Logger.LEVEL_CRITICAL);
    l.error("msg4 error");
    Iterator iter = target.messageIterator();
    while (iter.hasNext()) {
      System.err.println((String)iter.next());
    }
    assertIsomorphic(testOutputOutput, target.messageIterator());
    l.defaultTarget();
  }

  public void testNoRecurse() {
    Logger l = Logger.getLogger("recurse");
    MockLogTarget target = new MockLogTarget();
    l.setTarget(target);
    l.setLevel(Logger.LEVEL_DEBUG);
    l.debug("debug message, shouldn't cause recursion");
  }
}

