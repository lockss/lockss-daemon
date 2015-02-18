/*
 * $Id$
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
    daemon = getMockLockssDaemon();
    wdog = daemon.getWatchdogService();
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    wdog.stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  private void config(String file, String interval) {
    config(new Properties(), file, interval);
  }

  private void config(Properties props, String file, String interval) {
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
    File tmpfile = FileTestUtil.tempFile("wdog");
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
    File tmpfile = FileTestUtil.tempFile("wdog");

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
    assertFalse("DNS lookup shouldn't have happened", wdog.dnsProbeAttempted);
  }

  // test disabled to avoid DNS lookups in test
  public void xxxtestDns() throws Exception {
    File tmpfile = FileTestUtil.tempFile("wdog");

    // configure a 10 second watchdog with dns probes
    Properties p = new Properties();
    p.put(WatchdogService.PARAM_PLATFORM_WDOG_DNS, "true");
    p.put(WatchdogService.PARAM_PLATFORM_WDOG_DNS_DOMAIN, "example.com");
    config(p, tmpfile.toString(), "10s");

    TimeBase.setSimulated(9000);
    wdog.startService();
    // should happen immediately, when service is started
    // ensure file mod time is correct
    assertEquals("1.example.com", wdog.dnsProbeHost);
    assertTrue("DNS lookup should have happened", wdog.dnsProbeAttempted);
  }

  public void testDisable() throws Exception {
    File tmpfile = FileTestUtil.tempFile("wdog");

    // configure a 10 second watchdog
    config(tmpfile.toString(), "10s");

    wdog.startService();
    assertTrue(tmpfile.exists());

    // now unconfigure it.  the file should be deleted
    config(tmpfile.toString(), "0");
    assertFalse(tmpfile.exists());
  }

  public void testForceStop() throws Exception {
    File tmpfile = FileTestUtil.tempFile("wdog");

    // configure a 10 second watchdog
    config(tmpfile.toString(), "10s");

    TimeBase.setSimulated(9000);
    wdog.startService();
    // should happen immediately, when service is started
    // ensure file mod time is correct
    assertTrue(tmpfile.exists());
    long e1 = TimeBase.nowMs();
    assertEquals(e1, tmpfile.lastModified());

    wdog.forceStop();

    // 20 seconds later, it should still be there, but not get updated
    TimeBase.step(20 * Constants.SECOND);
    assertTrue(tmpfile.exists());
    assertEquals(e1, tmpfile.lastModified());

    // now unconfigure it.  the file should not be deleted
    config(tmpfile.toString(), "0");
    assertTrue(tmpfile.exists());
  }
}
