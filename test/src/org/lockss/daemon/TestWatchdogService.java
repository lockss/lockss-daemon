/*
 * $Id: TestWatchdogService.java,v 1.5 2003-06-20 22:34:53 claire Exp $
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

package org.lockss.daemon;

import java.util.*;
import java.io.*;
import junit.framework.TestCase;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.daemon.WatchdogService
 */

public class TestWatchdogService extends LockssTestCase {
//   protected static Logger log = Logger.getLogger("TestWdogSvc");

  LockssDaemon daemon;
  WatchdogService wdog;

  public void setUp() throws Exception {
    super.setUp();
    daemon = new MockLockssDaemon();
    wdog = daemon.getWatchdogService();
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    wdog.stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  private void config(String file, String interval) {
    Properties props = new Properties();
    props.put(WatchdogService.PARAM_PLATFORM_WDOG_FILE, file);
    props.put(WatchdogService.PARAM_PLATFORM_WDOG_INTERVAL, interval);
    ConfigurationUtil.setCurrentConfigFromProps(props);
  }

  // ensure it doesn't do anything untoward when it's not configured
  public void testWdogOff() throws Exception {
    config("", "0");
    wdog.startService();
    config("", "1");
    wdog.startService();
    config("foo", "0");
    wdog.startService();
  }

  public void testCreate() throws Exception {
    // arrange for a file that doesn't exist
    File tmpfile = FileUtil.tempFile("wdog");
    tmpfile.delete();
    assertFalse(tmpfile.exists());

    // Configure and start a watchdog.
    // It should go off once and create the file
    config(tmpfile.toString(), "1s");
    TimeBase.setSimulated(9000);
    wdog.startService();
    assertTrue(tmpfile.exists());
  }

  public void testWdogOn() throws Exception {
    File tmpfile = FileUtil.tempFile("wdog");

    // configure a 10 second watchdog
    config(tmpfile.toString(), "10s");

    TimeBase.setSimulated(9000);
    wdog.startService();
    // should happen immediately, when service is started
    // ensure file mod time is correct
    long e1 = TimeBase.nowMs();
    assertEquals(e1, tmpfile.lastModified());

    // 9 seconds later, it shouldn't get updated
    TimeBase.step(9000);
    assertEquals(e1, tmpfile.lastModified());

    // 1 more second later, it should be updated again
    TimeBase.step(1000);
    assertEquals(TimeBase.nowMs(), tmpfile.lastModified());
  }

  public void testDisable() throws Exception {
    File tmpfile = FileUtil.tempFile("wdog");

    // configure a 10 second watchdog
    config(tmpfile.toString(), "10s");

    wdog.startService();
    assertTrue(tmpfile.exists());

    // now unconfigure it.  the file should be deleted
    config(tmpfile.toString(), "0");
    assertFalse(tmpfile.exists());
  }
}
