/*
 * $Id: TestLogger.java,v 1.18 2003-06-20 22:34:56 claire Exp $
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
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.util.TestLogger
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class TestLogger extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.Logger.class
  };

  public TestLogger(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    Logger.setDefaultTarget();
  }

  public void tearDown() throws Exception {
    Logger.setDefaultTarget();
    super.tearDown();
  }

  public void testGetDefaultTarget() {
    String deftgtprop = System.getProperty(Logger.SYSPROP_DEFAULT_LOG_TARGET);
    try {
      System.setProperty(Logger.SYSPROP_DEFAULT_LOG_TARGET, "");
      assertTrue(Logger.getDefaultTarget() instanceof StdErrTarget);
      System.setProperty(Logger.SYSPROP_DEFAULT_LOG_TARGET, "noSuchClass");
      assertTrue(Logger.getDefaultTarget() instanceof StdErrTarget);
      System.setProperty(Logger.SYSPROP_DEFAULT_LOG_TARGET,
			 "org.lockss.util.StdErrTarget");
      assertTrue(Logger.getDefaultTarget() instanceof StdErrTarget);
    } finally {
      System.setProperty(Logger.SYSPROP_DEFAULT_LOG_TARGET, deftgtprop);
    }
  }

  // This needs the ant hierarchy to compile, which breaks under JBuilder, etc.
//   public void testAntTaskTarget() {
//     // Skip this test if not running under Ant.  AntHelper will throw
//     // during creation if it can't find the Ant environment
//     try {
//       new org.lockss.ant.AntHelper();
//     } catch (Exception e) {
//       return;
//     }
//     String deftgtprop = System.getProperty(Logger.SYSPROP_DEFAULT_LOG_TARGET);
//     try {
//       System.setProperty(Logger.SYSPROP_DEFAULT_LOG_TARGET,
// 			 "org.lockss.util.AntTaskTarget");
//       assertTrue(Logger.getDefaultTarget() instanceof AntTaskTarget);
//     } finally {
//       System.setProperty(Logger.SYSPROP_DEFAULT_LOG_TARGET, deftgtprop);
//     }
//   }

  public void testNames() {
    assertEquals("Critical", Logger.nameOf(Logger.LEVEL_CRITICAL));
    assertEquals("Info", Logger.nameOf(Logger.LEVEL_INFO));
    assertEquals(Logger.LEVEL_WARNING, Logger.levelOf("warning"));
    assertEquals(Logger.LEVEL_DEBUG, Logger.levelOf("debug"));
    assertEquals(Logger.LEVEL_DEBUG1, Logger.levelOf("debug1"));
    assertEquals(Logger.LEVEL_DEBUG2, Logger.levelOf("debug2"));
    assertEquals(Logger.LEVEL_DEBUG3, Logger.levelOf("debug3"));
  }

  public void testLevels() {
    Logger l = Logger.getLogger("test-log");
    l.setLevel(Logger.LEVEL_WARNING);
    assertTrue(l.isLevel(Logger.LEVEL_WARNING));
    assertTrue(l.isLevel(Logger.LEVEL_ERROR));
    assertTrue(l.isLevel(Logger.LEVEL_CRITICAL));
    assertTrue( ! l.isLevel(Logger.LEVEL_INFO));
    assertTrue( ! l.isLevel(Logger.LEVEL_DEBUG));
    assertTrue( ! l.isLevel(Logger.LEVEL_DEBUG3));
  }

  public void testTargetInit() {
    Logger l = Logger.getLogger("test-log");
    MockLogTarget target = new MockLogTarget();
    assertEquals(0, target.initCount());
    l.addTarget(target);
    assertEquals(1, target.initCount());
    l.addTarget(target);
    assertEquals(1, target.initCount());
  }

  List classesOf(List instances) {
    List res = new ArrayList();
    for (Iterator iter = instances.iterator(); iter.hasNext(); ) {
      res.add(iter.next().getClass());
    }
    return res;
  }

  public void testTargetListFromString() {
    String t1 = "org.lockss.test.MockLogTarget";
    String t2 = "org.lockss.util.SyslogTarget";
    String t3 = "org.lockss.util.StringUtil"; // not a LogTarget
    String t4 = "org.lockss.util.noSuchClass";	// not a class
    String s1 = StringUtil.separatedString(ListUtil.list(t1, t3), ";");
    String s2 = StringUtil.separatedString(ListUtil.list(t1, t4), ";");
    String s3 = StringUtil.separatedString(ListUtil.list(t1, t2), ";");
    assertEquals(null, Logger.targetListFromString(s1));
    assertEquals(null, Logger.targetListFromString(s2));
    List tgts = Logger.targetListFromString(s3);
    List tgtClasses = classesOf(tgts);
    assertEquals(SetUtil.set(MockLogTarget.class,
			     SyslogTarget.class),
		 new HashSet(tgtClasses));
  }

  public void testConfigureTargets() throws Exception {
    Logger l = Logger.getLogger("test-log");
    String s =
      "org.lockss.log.targets=" +
      "org.lockss.test.MockLogTarget;" +
      "org.lockss.util.SyslogTarget\n";
    TestConfiguration.setCurrentConfigFromString(s);
    List tgts = Logger.getTargets();
    List tgtClasses = classesOf(tgts);
    assertIsomorphic(ListUtil.list(MockLogTarget.class,
				   SyslogTarget.class),
		     tgtClasses);
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
    TestConfiguration.setCurrentConfigFromString(s);
  }

  private void configLogLevelOnly(String logName, int level)
      throws IOException {
    String s =
      "org.lockss.log." + logName + ".level=" + Logger.nameOf(level) + "\n";
    TestConfiguration.setCurrentConfigFromString(s);
  }

  public void testGetConfiguredLevel()
      throws IOException {
    System.getProperties().setProperty("org.lockss.defaultLogLevel", "debug2");
    Logger.setInitialDefaultLevel();
    assertEquals(Logger.LEVEL_DEBUG2, Logger.getConfiguredLevel("foobar"));
    configLogLevelOnly("foo", Logger.LEVEL_WARNING);
    assertEquals(Logger.LEVEL_WARNING, Logger.getConfiguredLevel("foo"));
    assertEquals(Logger.LEVEL_DEBUG2, Logger.getConfiguredLevel("foobar"));
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
  }

  public void testNoRecurse() {
    Logger l = Logger.getLogger("recurse");
    MockLogTarget target = new MockLogTarget();
    l.setTarget(target);
    l.setLevel(Logger.LEVEL_DEBUG);
    l.debug("debug message, shouldn't cause recursion");
  }
}

