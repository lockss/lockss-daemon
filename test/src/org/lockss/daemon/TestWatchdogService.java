/*
 * $Id: TestWatchdogService.java,v 1.2 2003-05-23 17:10:38 tal Exp $
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

  // ensure it doesn't do anything untoward when it's not configured
  public void testWdogOff() throws Exception {
    wdog.startService();
  }

  // nor if the file doesn't exist
  public void testNoFile() throws Exception {
    String tmpfile = "/tmp/nexist-pas";
    assertFalse(new File(tmpfile).exists());

    // configure a 1 second watchdog
    Properties props = new Properties();
    props.put(WatchdogService.PARAM_PLATFORM_WDOG_FILE, tmpfile.toString());
    props.put(WatchdogService.PARAM_PLATFORM_WDOG_INTERVAL, "10s");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    TimeBase.setSimulated(9000);
    wdog.startService();
  }

  public void testWdogOn() throws Exception {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    File tmpfile = new MockFile(FileUtil.tempFile("wdog").toString(), sem);

    // configure a 10 second watchdog
    Properties props = new Properties();
    props.put(WatchdogService.PARAM_PLATFORM_WDOG_FILE, tmpfile.toString());
    props.put(WatchdogService.PARAM_PLATFORM_WDOG_INTERVAL, "10s");
    ConfigurationUtil.setCurrentConfigFromProps(props);
    // substitute our MockFile for the File, just so we can wait for
    // setLastModified to be called
    wdog.setWatchedFile(tmpfile);

    TimeBase.setSimulated(9000);
    wdog.startService();
    // should happen immediately, when service is started
    sem.take(TIMEOUT_SHOULDNT);

    // ensure file mod time is correct
    long e1 = TimeBase.nowMs();
    assertEquals(e1, tmpfile.lastModified());

    // 9 seconds later, it shouldn't get updated
    TimeBase.step(9000);
    TimerUtil.guaranteedSleep(50);
    assertEquals(e1, tmpfile.lastModified());

    // 1 more second later, it should be updated again
    TimeBase.step(1000);
    long e2 = TimeBase.nowMs();
    sem.take(TIMEOUT_SHOULDNT);
    assertEquals(e2, tmpfile.lastModified());
  }

  /** File that posts a semaphore when setLastModified is called */
  static class MockFile extends File {
    SimpleBinarySemaphore slmSem;

    public MockFile(String pathname, SimpleBinarySemaphore slmSem) {
      super(pathname);
      this.slmSem = slmSem;
    }

    public boolean setLastModified(long time) {
      boolean ret = super.setLastModified(time);
      slmSem.give();
      return ret;
    }
  }
}
