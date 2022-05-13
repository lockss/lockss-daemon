/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
    setUpDiskSpace();	     // Prevent ConfigManager from logging an error
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

  public void testLevelOf() {
    assertEquals(Logger.LEVEL_CRITICAL, Logger.levelOf("critical"));
    assertEquals(Logger.LEVEL_ERROR, Logger.levelOf("error"));
    assertEquals(Logger.LEVEL_SITE_ERROR, Logger.levelOf("siteError"));
    assertEquals(Logger.LEVEL_WARNING, Logger.levelOf("warning"));
    assertEquals(Logger.LEVEL_SITE_WARNING, Logger.levelOf("siteWarning"));
    assertEquals(Logger.LEVEL_DEBUG, Logger.levelOf("debug"));
    assertEquals(Logger.LEVEL_DEBUG1, Logger.levelOf("debug1"));
    assertEquals(Logger.LEVEL_DEBUG2, Logger.levelOf("debug2"));
    assertEquals(Logger.LEVEL_DEBUG3, Logger.levelOf("debug3"));
  }

  public void testNameOf() {
    assertEquals("Critical", Logger.nameOf(Logger.LEVEL_CRITICAL));
    assertEquals("Error", Logger.nameOf(Logger.LEVEL_ERROR));
    assertEquals("SiteError", Logger.nameOf(Logger.LEVEL_SITE_ERROR));
    assertEquals("Warning", Logger.nameOf(Logger.LEVEL_WARNING));
    assertEquals("SiteWarning", Logger.nameOf(Logger.LEVEL_SITE_WARNING));
    assertEquals("Info", Logger.nameOf(Logger.LEVEL_INFO));
    assertEquals("Debug", Logger.nameOf(Logger.LEVEL_DEBUG));
    assertEquals("Debug", Logger.nameOf(Logger.LEVEL_DEBUG1));
    assertEquals("Debug2", Logger.nameOf(Logger.LEVEL_DEBUG2));
    assertEquals("Debug3", Logger.nameOf(Logger.LEVEL_DEBUG3));
  }

  public void testIsLevel() {
    Logger l = Logger.getLogger("test-log");
    l.setLevel(Logger.LEVEL_WARNING);
    assertTrue(l.isLevel(Logger.LEVEL_WARNING));
    assertTrue(l.isLevel(Logger.LEVEL_ERROR));
    assertTrue(l.isLevel(Logger.LEVEL_CRITICAL));
    assertFalse(l.isLevel(Logger.LEVEL_INFO));
    assertFalse(l.isLevel(Logger.LEVEL_DEBUG));
    assertFalse(l.isLevel(Logger.LEVEL_DEBUG3));
  }

  public void testTargetInit() {
    Logger l = Logger.getLogger("test-log");
    MockLogTarget target = new MockLogTarget();
    assertEquals(0, target.initCount());
    Logger.addTarget(target);
    assertEquals(1, target.initCount());
    Logger.addTarget(target);
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
    ConfigurationUtil.setCurrentConfigFromString(s);
    List tgts = Logger.getTargets();
    List tgtClasses = classesOf(tgts);
    assertIsomorphic(ListUtil.list(MockLogTarget.class,
				   SyslogTarget.class),
		     tgtClasses);
  }

  public void testLevelFilter() {
    Logger l = Logger.getLogger("test-log");
    MockLogTarget target = new MockLogTarget();
    Logger.setTarget(target);
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
    "Warning: 1-test-log:  msg2 warning",
    "Error: 1-test-log: msg3 error",
  };

  public void testOutput() {
    Logger l = Logger.getLogger("test-log");
    MockLogTarget target = new MockLogTarget();
    Logger.setTarget(target);
    l.setLevel(Logger.LEVEL_WARNING);
    target.resetMessages();
    l.info("msg1 info");
    l.warning(" msg2 warning\r\n");
    l.error("msg3 error\n");
    l.setLevel(Logger.LEVEL_CRITICAL);
    l.error("msg4 error");
    Iterator iter = target.messageIterator();
    while (iter.hasNext()) {
      System.err.println((String)iter.next());
    }
    assertIsomorphic(testOutputOutput, target.getMessages());
  }

  public void testStackTrace() {
    Logger l = Logger.getLogger("test-log");
    MockLogTarget target = new MockLogTarget();
    Logger.setTarget(target);
    // at warning level...
    l.setLevel(Logger.LEVEL_WARNING);
    target.resetMessages();
    // log.error should produce stack trace
    l.error("errmsg", new ExpectedRuntimeException("ex msg"));
    assertMatchesRE("at.*testStackTrace", (String)target.getMessages().get(0));
    // log.warning should not produce stack trace
    target.resetMessages();
    l.warning("errmsg", new ExpectedRuntimeException("ex msg2"));
    assertNotMatchesRE("at.*testStackTrace",
		       (String)target.getMessages().get(0));
    // but at debug level...
    l.setLevel(Logger.LEVEL_DEBUG);
    target.resetMessages();
    // log.error should produce stack trace
    l.error("errmsg", new ExpectedRuntimeException("ex msg"));
    assertMatchesRE("at.*testStackTrace", (String)target.getMessages().get(0));
    // and log.warning should produce stack trace
    target.resetMessages();
    l.warning("errmsg", new ExpectedRuntimeException("ex msg2"));
    assertMatchesRE("at.*testStackTrace", (String)target.getMessages().get(0));
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
    ConfigurationUtil.setCurrentConfigFromString(s);
  }

  private void configLogLevelOnly(String logName, int level)
      throws IOException {
    String s =
      "org.lockss.log." + logName + ".level=" + Logger.nameOf(level) + "\n";
    ConfigurationUtil.setCurrentConfigFromString(s);
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

  public void testLevelConfig()
      throws IOException {
    String lName = "test-log1";
    Logger l = Logger.getLogger(lName);
    MockLogTarget target = new MockLogTarget();
    Logger.setTarget(target);
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
    assertIsomorphic(ListUtil.list("Warning: 1-test-log1: msg2 warning",
				   "Error: 1-test-log1: msg3 error",
				   "Info: 1-Config: Config updated, 4 keys from foo",
				   "Info: 1-Config: New TdbAus: 0"),
		     target.getMessages());
  }

  public void testgGetLoggerWithDefaultLevel() throws Exception {
    Logger l;
    l = Logger.getLoggerWithDefaultLevel("foo", "warning", "param_default");
    assertEquals(Logger.LEVEL_WARNING, l.level);
    configLogLevelOnly("foo", Logger.LEVEL_INFO);
    assertEquals(Logger.LEVEL_INFO, l.level);

    configLogLevelOnly("bar", Logger.LEVEL_INFO);
    l = Logger.getLoggerWithDefaultLevel("bar", "warning", "param_default");
    assertEquals(Logger.LEVEL_INFO, l.level);

    ConfigurationUtil.setFromArgs("param_default", "debug2");
    l = Logger.getLoggerWithDefaultLevel("baz", "warning", "param_default");
    assertEquals(Logger.LEVEL_DEBUG2, l.level);
  }

  public void testNoRecurse() {
    Logger l = Logger.getLogger("recurse");
    LocalMockLogTarget target = new LocalMockLogTarget();
    Logger.setTarget(target);
    l.setLevel(Logger.LEVEL_DEBUG);
    target.setDoRecurse(true);
    l.debug("debug message, shouldn't cause recursion");
  }

  // test that the thread id map doesn't prevent old threads from being
  // GCed
  public void testThreadId() throws Exception {
    Logger log = Logger.getLogger("testThreadId");
    int rpt = 100000;
    int mapsize = 0;
    final Logger tlog = Logger.getLogger("jack");
    MockLogTarget target = new MockLogTarget();
    Logger.setTarget(target);
    for (int ix = 0; ix < rpt; ix++) {
      Thread th = new Thread("iter " + ix) {
	  public void run() {
	    tlog.info(getName());
	  }};
      th.start();
      th.join();
      int s = tlog.getThreadMapSize();
      if (s < ix) {
	System.err.println("Map went from " + mapsize + " to " + s +
			     " on iteration " + ix);
	return;
      } else {
	mapsize = s;
      }
    }
    fail("No thread IDs were collected after " + rpt + " iterations.");
  }

  static class LocalMockLogTarget extends MockLogTarget {
    boolean doRecurse = false;
    public void handleMessage(Logger log, int msgLevel, String message) {
      super.handleMessage(log, msgLevel, message);
      if (doRecurse) {
	System.err.println("Recursive log call; should only happen once.");
	log.debug("Recursive log message.  Should not appear in log");
      }
    }
    public void setDoRecurse(boolean val) {
      doRecurse = val;
    }
  }
}

