/*
 * $Id$
 */

/*

Copyright (c) 2001-2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.File;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.repository.LockssRepositoryImpl;

public class TestActivityRegulator extends LockssTestCase {
  private ActivityRegulator regulator;
  private ActivityRegulator.Lock lock;
  private MockArchivalUnit mau;
  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    mau = new MockArchivalUnit();
    theDaemon = getMockLockssDaemon();
    regulator = theDaemon.getActivityRegulator(mau);
    regulator.startService();
    TimeBase.setSimulated(123);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  public void testGetAuActivity() {
    mau.setAuId("testid");
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getAuActivity());

    regulator.setAuActivity(ActivityRegulator.NEW_CONTENT_CRAWL, 123);
    assertEquals(ActivityRegulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
  }

  public void testGetCusActivity() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec("test url"));
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getCusActivity(mcus));

    regulator.setCusActivity(mcus, ActivityRegulator.REPAIR_CRAWL, 123);
    assertEquals(ActivityRegulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));

    MockCachedUrlSet mcus2 = new MockCachedUrlSet("test url2");
    mcus2.setArchivalUnit(mau);
    mcus2.setSpec(new RangeCachedUrlSetSpec("test url2"));
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getCusActivity(mcus2));

    regulator.setCusActivity(mcus2, ActivityRegulator.BACKGROUND_CRAWL, 123);
    assertEquals(ActivityRegulator.BACKGROUND_CRAWL, regulator.getCusActivity(mcus2));
    assertEquals(ActivityRegulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
  }

  public void testStopService() {
    // can get a lock fine
    ActivityRegulator.Lock lock = regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 123);
    assertNotNull(lock);
    lock.expire();
    // can get a second lock
    lock = regulator.getAuActivityLock(ActivityRegulator.TOP_LEVEL_POLL, 123);
    assertNotNull(lock);
    lock.expire();
    // stopping service
    assertTrue(regulator.serviceActive);
    regulator.stopService();
    assertFalse(regulator.serviceActive);
    // can no longer get a lock
    assertNull(regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 123));
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    assertNull(regulator.getCusActivityLock(mcus, ActivityRegulator.STANDARD_CONTENT_POLL, 123));
  }

  public void testAuActivityAllowed() {
    assertNotNull(regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 123));
    assertEquals(ActivityRegulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    assertNull(regulator.getAuActivityLock(ActivityRegulator.TOP_LEVEL_POLL, 123));
  }

  public void testCusActivityAllowed() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec("test url"));
    lock = regulator.getCusActivityLock(mcus, ActivityRegulator.REPAIR_CRAWL, 123);
    assertNotNull(lock);
    assertEquals(ActivityRegulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    assertNull(regulator.getCusActivityLock(mcus, ActivityRegulator.BACKGROUND_CRAWL, 123));
    lock.expire();

    mcus = new MockCachedUrlSet("test url2");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec("test url2"));
    lock = regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 123);
    assertNull(regulator.getCusActivityLock(mcus, ActivityRegulator.REPAIR_CRAWL, 123));

    lock.expire();
    assertNotNull(regulator.getCusActivityLock(mcus, ActivityRegulator.REPAIR_CRAWL, 123));
  }

  public void testAuToCusConversion() {
    ActivityRegulator.Lock auLock =
        regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 123);
    assertNotNull(auLock);
    assertFalse(auLock.isExpired());
    assertEquals(ActivityRegulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    ActivityRegulator.Lock cusLock =
        regulator.changeAuLockToCusLock(auLock, mcus, ActivityRegulator.REPAIR_CRAWL,
        123);
    assertNotNull(cusLock);
    assertTrue(auLock.isExpired());
    assertEquals(ActivityRegulator.CUS_ACTIVITY, regulator.getAuActivity());
    assertEquals(ActivityRegulator.REPAIR_CRAWL, cusLock.getActivity());
  }

  public void testAuToCusConversionMultiple() {
    ActivityRegulator.Lock auLock =
        regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 123);
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    MockCachedUrlSet mcus2 = new MockCachedUrlSet("test url2");
    // set the first AU and spec to avoid null comparisons
    mcus.setArchivalUnit(new MockArchivalUnit());
    mcus.setSpec(new RangeCachedUrlSetSpec("test"));
    mcus2.setArchivalUnit(new MockArchivalUnit());
    mcus2.setSpec(new RangeCachedUrlSetSpec("test2"));
    ActivityRegulator.CusLockRequest req1 =
        new ActivityRegulator.CusLockRequest(mcus, ActivityRegulator.REPAIR_CRAWL, 234);
    ActivityRegulator.CusLockRequest req2 =
        new ActivityRegulator.CusLockRequest(mcus2, ActivityRegulator.BACKGROUND_CRAWL,
        123);
    List locks = regulator.changeAuLockToCusLocks(auLock,
        ListUtil.list(req1, req2));
    assertEquals(2, locks.size());

    assertTrue(auLock.isExpired());
    assertEquals(ActivityRegulator.CUS_ACTIVITY, regulator.getAuActivity());
    assertEquals(234,
        regulator.curAuActivityLock.expiration.getRemainingTime());

    ActivityRegulator.Lock cusLock = (ActivityRegulator.Lock)locks.get(0);
    assertNotNull(cusLock);
    assertEquals(ActivityRegulator.REPAIR_CRAWL, cusLock.getActivity());
    assertEquals("test url", cusLock.cus.getUrl());

    cusLock = (ActivityRegulator.Lock)locks.get(1);
    assertNotNull(cusLock);
    assertEquals(ActivityRegulator.BACKGROUND_CRAWL, cusLock.getActivity());
    assertEquals("test url2", cusLock.cus.getUrl());
  }

  public void testAuToCusConversionMultipleConflict() {
    // get a lock for later use
    ActivityRegulator.Lock tempLock =
        regulator.getAuActivityLock(ActivityRegulator.BACKGROUND_CRAWL, 123);
    tempLock.expire();


    ActivityRegulator.Lock auLock =
        regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 123);
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    MockCachedUrlSet mcus2 = new MockCachedUrlSet("test url2");
    // set the AU and spec to avoid null comparisons
    mcus.setArchivalUnit(new MockArchivalUnit());
    mcus.setSpec(new RangeCachedUrlSetSpec("test"));
    mcus2.setArchivalUnit(new MockArchivalUnit());
    mcus2.setSpec(new RangeCachedUrlSetSpec("test2"));
    ActivityRegulator.CusLockRequest req1 =
        new ActivityRegulator.CusLockRequest(mcus, ActivityRegulator.REPAIR_CRAWL, 234);
    ActivityRegulator.CusLockRequest req2 =
        new ActivityRegulator.CusLockRequest(mcus2, ActivityRegulator.BACKGROUND_CRAWL,
        123);
    // put a lock in under the CUS (can't be an expired lock)
    tempLock.extend(123321);
    assertFalse(tempLock.isExpired());
    regulator.cusMap.put(mcus, tempLock);

    List locks = regulator.changeAuLockToCusLocks(auLock,
        ListUtil.list(req1, req2));
    assertEquals(1, locks.size());

    ActivityRegulator.Lock cusLock = (ActivityRegulator.Lock)locks.get(0);
    assertNotNull(cusLock);
    assertEquals(ActivityRegulator.BACKGROUND_CRAWL, cusLock.getActivity());
    assertEquals("test url2", cusLock.cus.getUrl());
  }

  public void testAuFinished() {
    lock = regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 123);
    assertNotNull(lock);
    assertEquals(ActivityRegulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    lock.expire();
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getAuActivity());
  }

  public void testCusFinished() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec("test url"));
    lock = regulator.getCusActivityLock(mcus, ActivityRegulator.REPAIR_CRAWL, 123);
    assertNotNull(lock);
    assertEquals(ActivityRegulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    lock.expire();
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getCusActivity(mcus));
  }

  public void testCusBlocking() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec("test url"));
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getAuActivity());
    lock = regulator.getCusActivityLock(mcus, ActivityRegulator.REPAIR_CRAWL, 123);
    assertNotNull(lock);
    assertEquals(ActivityRegulator.CUS_ACTIVITY, regulator.getAuActivity());
    assertNull(regulator.getAuActivityLock(ActivityRegulator.TOP_LEVEL_POLL, 123));

    MockCachedUrlSet mcus2 = new MockCachedUrlSet("test url2");
    mcus2.setArchivalUnit(mau);
    mcus2.setSpec(new RangeCachedUrlSetSpec("test url2"));
    ActivityRegulator.Lock lock2 = regulator.getCusActivityLock(mcus2, ActivityRegulator.BACKGROUND_CRAWL, 123);
    assertNotNull(lock2);

    lock.expire();
    assertNull(regulator.getAuActivityLock(ActivityRegulator.TOP_LEVEL_POLL, 123));
    assertEquals(ActivityRegulator.CUS_ACTIVITY, regulator.getAuActivity());

    lock2.expire();
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getAuActivity());
    assertNotNull(regulator.getAuActivityLock(ActivityRegulator.TOP_LEVEL_POLL, 123));
  }

  public void testCusRelationBlocking() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com/test");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec(mcus.getUrl()));
    assertNotNull(regulator.getCusActivityLock(mcus, ActivityRegulator.STANDARD_CONTENT_POLL, 123));

    // child should be blocked
    MockCachedUrlSet mcus2 = new MockCachedUrlSet("http://www.example.com/test/branch1");
    mcus2.setArchivalUnit(mau);
    mcus2.setSpec(new RangeCachedUrlSetSpec(mcus2.getUrl()));
    assertNull(regulator.getCusActivityLock(mcus2, ActivityRegulator.BACKGROUND_CRAWL, 123));

    // parent should be blocked on polls, but not crawls
    mcus2 = new MockCachedUrlSet("http://www.example.com");
    mcus2.setArchivalUnit(mau);
    mcus2.setSpec(new RangeCachedUrlSetSpec(mcus2.getUrl()));
    assertNull(regulator.getCusActivityLock(mcus2, ActivityRegulator.STANDARD_CONTENT_POLL, 123));
    assertNotNull(regulator.getCusActivityLock(mcus2, ActivityRegulator.REPAIR_CRAWL, 123));

    // peer should be ok
    mcus2 = new MockCachedUrlSet("http://www.example.com/test2");
    mcus2.setArchivalUnit(mau);
    mcus2.setSpec(new RangeCachedUrlSetSpec(mcus2.getUrl()));
    assertNotNull(regulator.getCusActivityLock(mcus2, ActivityRegulator.BACKGROUND_CRAWL, 123));
  }

  public void testCusRangeAllowance() {
    MockCachedUrlSet mcus = new MockCachedUrlSet(
        new RangeCachedUrlSetSpec("http://www.example.com/test", "file1", "file3"));
    mcus.setArchivalUnit(mau);
    assertNotNull(regulator.getCusActivityLock(mcus, ActivityRegulator.STANDARD_CONTENT_POLL, 123));

    // different range should be allowed
    MockCachedUrlSet mcus2 = new MockCachedUrlSet(
        new RangeCachedUrlSetSpec("http://www.example.com/test", "file4", "file6"));
    mcus2.setArchivalUnit(mau);
    assertNotNull(regulator.getCusActivityLock(mcus2, ActivityRegulator.STANDARD_CONTENT_POLL, 123));
  }

  public void testCusSimultaneousPollAllowance() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com/test");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec(mcus.getUrl()));
    assertNotNull(regulator.getCusActivityLock(mcus, ActivityRegulator.STANDARD_CONTENT_POLL, 123));

    // name poll should be allowed
    mcus = new MockCachedUrlSet("http://www.example.com/test");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec(mcus.getUrl()));
    assertNotNull(regulator.getCusActivityLock(mcus, ActivityRegulator.STANDARD_NAME_POLL, 123));

    // content poll on child should be allowed
    mcus = new MockCachedUrlSet("http://www.example.com/test/branch");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec(mcus.getUrl()));
    assertNotNull(regulator.getCusActivityLock(mcus, ActivityRegulator.STANDARD_CONTENT_POLL, 123));

  }

  public void testIsAllowedOnAu() {
    // using NO_ACTIVITY as a generic activity marker, since these functions
    // don't specifically handle it

    // nothing allowed on these
    assertFalse(ActivityRegulator.isAllowedOnAu(ActivityRegulator.NO_ACTIVITY,
                                                ActivityRegulator.NEW_CONTENT_CRAWL));
    assertFalse(ActivityRegulator.isAllowedOnAu(ActivityRegulator.NO_ACTIVITY,
                                                ActivityRegulator.TREEWALK));

    // only other polls are allowed, but not standard name polls
    assertFalse(ActivityRegulator.isAllowedOnAu(ActivityRegulator.NO_ACTIVITY,
                                                ActivityRegulator.TOP_LEVEL_POLL));
    assertFalse(ActivityRegulator.isAllowedOnAu(ActivityRegulator.STANDARD_NAME_POLL,
                                                ActivityRegulator.TOP_LEVEL_POLL));
    assertTrue(ActivityRegulator.isAllowedOnAu(ActivityRegulator.TOP_LEVEL_POLL,
                                               ActivityRegulator.TOP_LEVEL_POLL));
    assertTrue(ActivityRegulator.isAllowedOnAu(ActivityRegulator.STANDARD_CONTENT_POLL,
                                               ActivityRegulator.TOP_LEVEL_POLL));


    // only CUS activity allowed on CUS_ACTIVITY
    assertTrue(ActivityRegulator.isAllowedOnAu(ActivityRegulator.BACKGROUND_CRAWL,
                                               ActivityRegulator.CUS_ACTIVITY));
    assertTrue(ActivityRegulator.isAllowedOnAu(ActivityRegulator.REPAIR_CRAWL,
                                               ActivityRegulator.CUS_ACTIVITY));
    assertTrue(ActivityRegulator.isAllowedOnAu(ActivityRegulator.STANDARD_CONTENT_POLL,
                                               ActivityRegulator.CUS_ACTIVITY));
    assertFalse(ActivityRegulator.isAllowedOnAu(ActivityRegulator.NEW_CONTENT_CRAWL,
                                                ActivityRegulator.CUS_ACTIVITY));
  }

  public void testIsAllowedOnCus() {
    // using NO_ACTIVITY as a generic activity marker, since these functions
    // don't specifically handle it

    // if a crawl-
    //   allow only name poll if same
    assertFalse(ActivityRegulator.isAllowedOnCus(ActivityRegulator.NO_ACTIVITY, ActivityRegulator.BACKGROUND_CRAWL, CachedUrlSet.SAME_LEVEL_OVERLAP));
//    assertTrue(ActivityRegulator.isAllowedOnCus(regulator.STANDARD_NAME_POLL, regulator.REPAIR_CRAWL, CachedUrlSet.SAME_LEVEL_OVERLAP));
    //   allow anything if parent
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.NO_ACTIVITY, ActivityRegulator.BACKGROUND_CRAWL, CachedUrlSet.ABOVE));
    //   allow only crawls if child
    assertFalse(ActivityRegulator.isAllowedOnCus(ActivityRegulator.NO_ACTIVITY, ActivityRegulator.BACKGROUND_CRAWL, CachedUrlSet.BELOW));
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.REPAIR_CRAWL, ActivityRegulator.BACKGROUND_CRAWL, CachedUrlSet.BELOW));

    // if a poll-
    //   allow only name poll or repair crawl if same
    assertFalse(ActivityRegulator.isAllowedOnCus(ActivityRegulator.NO_ACTIVITY, ActivityRegulator.STANDARD_CONTENT_POLL, CachedUrlSet.SAME_LEVEL_OVERLAP));
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.STANDARD_NAME_POLL, ActivityRegulator.STANDARD_CONTENT_POLL, CachedUrlSet.SAME_LEVEL_OVERLAP));
//    assertTrue(ActivityRegulator.isAllowedOnCus(regulator.REPAIR_CRAWL, regulator.STANDARD_CONTENT_POLL, CachedUrlSet.SAME_LEVEL_OVERLAP));
    //   allow only content polls and repairs on sub-nodes if parent with name poll
    assertFalse(ActivityRegulator.isAllowedOnCus(ActivityRegulator.NO_ACTIVITY,
                                                 ActivityRegulator.STANDARD_CONTENT_POLL, CachedUrlSet.ABOVE));
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.STANDARD_CONTENT_POLL,
                                                ActivityRegulator.STANDARD_NAME_POLL, CachedUrlSet.ABOVE));
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.REPAIR_CRAWL,
                                                ActivityRegulator.STANDARD_NAME_POLL, CachedUrlSet.ABOVE));
    //   allow only crawls and single node polls if child
    assertFalse(ActivityRegulator.isAllowedOnCus(ActivityRegulator.NO_ACTIVITY,
                                                 ActivityRegulator.STANDARD_CONTENT_POLL, CachedUrlSet.BELOW));
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.REPAIR_CRAWL,
                                                ActivityRegulator.STANDARD_CONTENT_POLL, CachedUrlSet.BELOW));
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.SINGLE_NODE_CONTENT_POLL,
                                                ActivityRegulator.STANDARD_CONTENT_POLL, CachedUrlSet.BELOW));
    //   for single node polls, allow only repair crawl if same
    assertFalse(ActivityRegulator.isAllowedOnCus(ActivityRegulator.STANDARD_NAME_POLL,
                                                 ActivityRegulator.SINGLE_NODE_CONTENT_POLL,
                                                 CachedUrlSet.SAME_LEVEL_OVERLAP));
//  assertTrue(ActivityRegulator.isAllowedOnCus(regulator.REPAIR_CRAWL, regulator.SINGLE_NODE_CONTENT_POLL, CachedUrlSet.SAME_LEVEL_OVERLAP));
    //   allow anything if parent
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.NO_ACTIVITY,
                                                ActivityRegulator.SINGLE_NODE_CONTENT_POLL,
                                                CachedUrlSet.ABOVE));
    //   allow only crawls if child
    assertFalse(ActivityRegulator.isAllowedOnCus(ActivityRegulator.STANDARD_NAME_POLL,
                                                 ActivityRegulator.SINGLE_NODE_CONTENT_POLL,
                                                 CachedUrlSet.BELOW));
    assertTrue(ActivityRegulator.isAllowedOnCus(ActivityRegulator.REPAIR_CRAWL,
                                                ActivityRegulator.SINGLE_NODE_CONTENT_POLL,
                                                CachedUrlSet.BELOW));
  }

  public void testAuExpiration() {
    assertNotNull(regulator.getAuActivityLock(ActivityRegulator.NEW_CONTENT_CRAWL, 10));
    assertEquals(ActivityRegulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    TimeBase.step(5);
    assertEquals(ActivityRegulator.NEW_CONTENT_CRAWL, regulator.getAuActivity());
    TimeBase.step(5);
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getAuActivity());
  }

  public void testCusExpiration() {
    MockCachedUrlSet mcus = new MockCachedUrlSet("test url");
    mcus.setArchivalUnit(mau);
    mcus.setSpec(new RangeCachedUrlSetSpec(mcus.getUrl()));
    assertNotNull(regulator.getCusActivityLock(mcus, ActivityRegulator.REPAIR_CRAWL, 10));
    assertEquals(ActivityRegulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    assertEquals(ActivityRegulator.CUS_ACTIVITY, regulator.getAuActivity());
    TimeBase.step(5);
    assertEquals(ActivityRegulator.REPAIR_CRAWL, regulator.getCusActivity(mcus));
    assertEquals(ActivityRegulator.CUS_ACTIVITY, regulator.getAuActivity());
    TimeBase.step(5);
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getCusActivity(mcus));
    assertEquals(ActivityRegulator.NO_ACTIVITY, regulator.getAuActivity());
  }
}
