/*
 * $Id: TestLockssRepositoryServiceImpl.java,v 1.1 2003-03-04 00:16:12 aalto Exp $
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

package org.lockss.repository;

import java.io.File;
import java.util.Vector;
import org.lockss.test.*;
import org.lockss.util.Logger;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import java.io.IOException;
import org.lockss.daemon.TestConfiguration;

public class TestLockssRepositoryServiceImpl extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestLRSI");
  private MockLockssDaemon theDaemon = new MockLockssDaemon(null);
  private LockssRepositoryService lrs;
  private MockArchivalUnit mau;

  public TestLockssRepositoryServiceImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    configCacheLocation(tempDirPath);

    mau = new MockArchivalUnit();

    lrs = new LockssRepositoryServiceImpl();
    lrs.initService(theDaemon);
    lrs.startService();
  }

  public void tearDown() throws Exception {
    lrs.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testGetLockssRepository() {
    String auId = mau.getAUId();
    lrs.addLockssRepository(mau);
    LockssRepository repo1 = lrs.getLockssRepository(mau);
    assertNotNull(repo1);
    mau.setAuId(auId + "test");
    LockssRepository repo2 = lrs.addLockssRepository(mau);
    assertTrue(repo1 != repo2);
    mau.setAuId(auId);
    repo2 = lrs.getLockssRepository(mau);
    assertEquals(repo1, repo2);

    mau.setAuId(auId + "test2");
    try {
      lrs.getLockssRepository(mau);
      fail("Should throw IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }
  }

  public static void configCacheLocation(String location)
    throws IOException {
    String s = LockssRepositoryServiceImpl.PARAM_CACHE_LOCATION + "=" + location;
    TestConfiguration.setCurrentConfigFromString(s);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestLockssRepositoryServiceImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
