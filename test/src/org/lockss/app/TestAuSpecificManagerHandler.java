/*
 * $Id: TestAuSpecificManagerHandler.java,v 1.1 2003-06-26 01:03:13 eaalto Exp $
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

package org.lockss.app;

import java.io.File;
import java.util.*;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.state.*;

public class TestAuSpecificManagerHandler extends LockssTestCase {
  MockArchivalUnit mau;
  MockLockssDaemon theDaemon;
  AuSpecificManagerHandler handler;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
                      tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = new MockLockssDaemon();
    mau = new MockArchivalUnit();

    handler = new AuSpecificManagerHandler(theDaemon, 3);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testGetLockssManager() {
    String auId = mau.getAUId();
    LockssManager man1 = handler.getAUSpecificManager(LockssDaemon.NODE_MANAGER,
        mau);
    assertNull(man1);

    handler.addAuSpecificManager(LockssDaemon.NODE_MANAGER, mau);
    man1 = handler.getAUSpecificManager(LockssDaemon.NODE_MANAGER, mau);
    assertNotNull(man1);

    mau = new MockArchivalUnit();
    handler.addAuSpecificManager(LockssDaemon.NODE_MANAGER, mau);
    LockssManager man2 = handler.getAUSpecificManager(LockssDaemon.NODE_MANAGER,
        mau);
    assertNotSame(man1, man2);

    mau = new MockArchivalUnit();
    man1 = handler.getAUSpecificManager(LockssDaemon.NODE_MANAGER, mau);
    assertNull(man1);
  }

  public void testGetNewManager() {
    LockssManager manager = handler.getNewManager(
        LockssDaemon.ACTIVITY_REGULATOR, mau);
    assertNotNull(manager);
    assertTrue(manager instanceof ActivityRegulator);

    manager = handler.getNewManager(LockssDaemon.LOCKSS_REPOSITORY, mau);
    assertNotNull(manager);
    assertTrue(manager instanceof LockssRepository);

    manager = handler.getNewManager(LockssDaemon.NODE_MANAGER, mau);
    assertNotNull(manager);
    assertTrue(manager instanceof NodeManager);

    try {
      manager = handler.getNewManager(LockssDaemon.CRAWL_MANAGER, mau);
      fail("Should have thrown IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }
  }

  public void testStartAUManagers() {
    LockssManager manager = handler.getAUSpecificManager(
        LockssDaemon.ACTIVITY_REGULATOR, mau);
    assertNull(manager);
    manager = handler.getAUSpecificManager(LockssDaemon.LOCKSS_REPOSITORY, mau);
    assertNull(manager);
    manager = handler.getAUSpecificManager(LockssDaemon.NODE_MANAGER, mau);
    assertNull(manager);

    handler.startAUManagers(mau);

    manager = handler.getAUSpecificManager(LockssDaemon.ACTIVITY_REGULATOR,
                                           mau);
    assertNotNull(manager);
    manager = handler.getAUSpecificManager(LockssDaemon.LOCKSS_REPOSITORY, mau);
    assertNotNull(manager);
    manager = handler.getAUSpecificManager(LockssDaemon.NODE_MANAGER, mau);
    assertNotNull(manager);
  }

  public void testGetEntries() {
    Iterator entries = handler.getEntries(LockssDaemon.LOCKSS_REPOSITORY);
    assertFalse(entries.hasNext());

    handler.addAuSpecificManager(LockssDaemon.LOCKSS_REPOSITORY, mau);
    MockArchivalUnit mau2 = new MockArchivalUnit();
    handler.addAuSpecificManager(LockssDaemon.LOCKSS_REPOSITORY, mau2);
    entries = handler.getEntries(LockssDaemon.LOCKSS_REPOSITORY);
    assertTrue(entries.hasNext());
    entries.next();
    assertTrue(entries.hasNext());
    entries.next();
    assertFalse(entries.hasNext());
  }

  public void testStopAllManagers() {
    HashMap map =
        handler.getAUSpecificManagerMap(LockssDaemon.LOCKSS_REPOSITORY);
    MyMockLockssManager man1 = new MyMockLockssManager();
    map.put(mau, man1);

    map = handler.getAUSpecificManagerMap(LockssDaemon.NODE_MANAGER);
    MyMockLockssManager man2 = new MyMockLockssManager();
    map.put(mau, man2);

    mau = new MockArchivalUnit();
    MyMockLockssManager man3 = new MyMockLockssManager();
    map.put(mau, man3);

    assertFalse(man1.isStopped);
    assertFalse(man2.isStopped);
    assertFalse(man3.isStopped);

    handler.stopAllManagers();

    assertTrue(man1.isStopped);
    assertTrue(man2.isStopped);
    assertTrue(man3.isStopped);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestAuSpecificManagerHandler.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  static class MyMockLockssManager extends NullLockssManager {
    boolean isStopped = false;

    public void stopService() {
      isStopped = true;
    }
  }
}
