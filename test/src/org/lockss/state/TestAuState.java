/*
 * $Id: TestAuState.java,v 1.20 2013-07-18 03:14:12 dshr Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.state;

import java.util.*;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.plugin.*;
import org.lockss.poller.v3.*;
import org.lockss.util.TimeBase;

public class TestAuState extends LockssTestCase {
  MockHistoryRepository historyRepo;
  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();

    historyRepo = new MockHistoryRepository();
    mau = new MockArchivalUnit();
  }


  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  int t1 = 456;
  int t2 = 12000;
  int t3 = 14000;
  int t4 = 17000;

  public void testCrawlStarted() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(-1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastCrawlResult());
    assertFalse(aus.isCrawlActive());
    assertFalse(aus.hasCrawled());
    assertNull(historyRepo.theAuState);

    TimeBase.setSimulated(t1);
    aus.newCrawlStarted();
    // these should now reflect the previoud crawl, not the active one
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(-1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastCrawlResult());
    assertTrue(aus.isCrawlActive());
    assertFalse(aus.hasCrawled());
    assertNotNull(historyRepo.theAuState);

    TimeBase.setSimulated(t2);
    aus.newCrawlFinished(Crawler.STATUS_ERROR, "Plorg");
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_ERROR, aus.getLastCrawlResult());
    assertEquals("Plorg", aus.getLastCrawlResultMsg());
    assertFalse(aus.isCrawlActive());
    assertFalse(aus.hasCrawled());

    TimeBase.setSimulated(t3);
    aus.newCrawlFinished(Crawler.STATUS_SUCCESSFUL, "Syrah");
    assertEquals(t3, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals("Syrah", aus.getLastCrawlResultMsg());
    assertFalse(aus.isCrawlActive());
    assertTrue(aus.hasCrawled());

    aus = aus.simulateStoreLoad();
    assertEquals(t3, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals("Syrah", aus.getLastCrawlResultMsg());
    assertFalse(aus.isCrawlActive());
    assertTrue(aus.hasCrawled());

    TimeBase.setSimulated(t4);
    aus.newCrawlStarted();
    assertEquals(t3, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals("Syrah", aus.getLastCrawlResultMsg());
    assertTrue(aus.hasCrawled());
  }

  public void testDaemonCrashedDuringCrawl() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(-1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastCrawlResult());
    assertFalse(aus.isCrawlActive());
    assertNull(historyRepo.theAuState);

    TimeBase.setSimulated(t1);
    aus.newCrawlStarted();
    // these should now reflect the previoud crawl, not the active one
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(-1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastCrawlResult());
    assertTrue(aus.isCrawlActive());
    assertNotNull(historyRepo.theAuState);

    TimeBase.setSimulated(t2);
    aus = aus.simulateStoreLoad();
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_RUNNING_AT_CRASH, aus.getLastCrawlResult());
    assertFalse(aus.isCrawlActive());

    TimeBase.setSimulated(t3);
    aus.newCrawlStarted();
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_RUNNING_AT_CRASH, aus.getLastCrawlResult());

    TimeBase.setSimulated(t4);
    aus.newCrawlFinished(Crawler.STATUS_SUCCESSFUL, "Plorg");
    assertEquals(t4, aus.getLastCrawlTime());
    assertEquals(t3, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
  }

  public void testPollDuration() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(0, aus.getPollDuration());
    aus.setPollDuration(1000);
    assertEquals(1000, aus.getPollDuration());
    aus.setPollDuration(2000);
    assertEquals(1500, aus.getPollDuration());
  }

  public void testPollStarted() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(-1, aus.getLastTopLevelPollTime());
    assertEquals(-1, aus.getLastPollStart());
    assertEquals(-1, aus.getLastPollResult());
    assertEquals(0, aus.getPollDuration());
    assertFalse(aus.isPollActive());
    assertNull(historyRepo.theAuState);

    TimeBase.setSimulated(t1);
    aus.pollStarted();
    // these should now reflect the previous poll, not the active one
    assertEquals(-1, aus.getLastTopLevelPollTime());
    assertEquals(-1, aus.getLastPollStart());
    assertEquals(-1, aus.getLastPollResult());
    assertEquals(0, aus.getPollDuration());
    assertTrue(aus.isPollActive());
    assertNotNull(historyRepo.theAuState);

    TimeBase.setSimulated(t2);
    aus.pollFinished(V3Poller.POLLER_STATUS_ERROR);
    assertEquals(-1, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_ERROR, aus.getLastPollResult());
    assertEquals(t2, aus.getPollDuration());
    assertFalse(aus.isPollActive());

    TimeBase.setSimulated(t3);
    aus.pollFinished(V3Poller.POLLER_STATUS_COMPLETE);
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPollResult());
    assertEquals((t3 + t2) / 2, aus.getPollDuration());
    assertFalse(aus.isPollActive());

    aus = aus.simulateStoreLoad();
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPollResult());
    assertFalse(aus.isPollActive());

    TimeBase.setSimulated(t4);
    aus.pollStarted();
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPollResult());
  }

  public void testV3Agreement() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(-1.0, aus.getV3Agreement());
    assertEquals(-1.0, aus.getHighestV3Agreement());
    assertNull(historyRepo.theAuState);

    aus.setV3Agreement(0.0);
    assertEquals(0.0, aus.getV3Agreement());
    assertEquals(0.0, aus.getHighestV3Agreement());
    assertNotNull(historyRepo.theAuState);

    aus.setV3Agreement(0.5);
    assertEquals(0.5, aus.getV3Agreement());
    assertEquals(0.5, aus.getHighestV3Agreement());

    aus.setV3Agreement(0.3);
    assertEquals(0.3, aus.getV3Agreement());
    assertEquals(0.5, aus.getHighestV3Agreement());
  }

  public void testTreeWalkFinished() {
    AuState auState = new AuState(mau, -1, -1, -1, -1, 123, null,
				  1, -1.0, 1.0, historyRepo);
    assertEquals(123, auState.getLastTreeWalkTime());

    TimeBase.setSimulated(456);
    auState.setLastTreeWalkTime();
    assertEquals(456, auState.getLastTreeWalkTime());
  }

  public void testGetUrls() {
    HashSet stringCollection = new HashSet();
    stringCollection.add("test");

    AuState auState =
      new AuState(mau, -1, -1, -1, -1, 123,
		  stringCollection, 1, -1.0, 1.0, historyRepo);
    Collection col = auState.getCrawlUrls();
    Iterator colIter = col.iterator();
    assertTrue(colIter.hasNext());
    assertEquals("test", colIter.next());
    assertFalse(colIter.hasNext());
  }

  public void testUpdateUrls() {
    AuState auState = new AuState(mau, historyRepo);
    assertNull(historyRepo.theAuState);

    Collection col = auState.getCrawlUrls();
    for (int ii=1; ii<AuState.URL_UPDATE_LIMIT; ii++) {
      col.add("test" + ii);
      auState.updatedCrawlUrls(false);
      assertEquals(ii, auState.urlUpdateCntr);
      assertNull(historyRepo.theAuState);
    }
    col.add("test-limit");
    auState.updatedCrawlUrls(false);
    assertEquals(0, auState.urlUpdateCntr);
    assertNotNull(historyRepo.theAuState);

    // clear, and check that counter is reset
    historyRepo.theAuState = null;
    if (AuState.URL_UPDATE_LIMIT > 1) {
      col.add("test");
      auState.updatedCrawlUrls(false);
      assertNull(historyRepo.theAuState);
    }
  }

  public void testForceUpdateUrls() {
    AuState auState = new AuState(mau, historyRepo);
    assertNull(historyRepo.theAuState);

    Collection col = auState.getCrawlUrls();
    if (AuState.URL_UPDATE_LIMIT > 1) {
      col.add("test");
      auState.updatedCrawlUrls(true);
      assertEquals(0, auState.urlUpdateCntr);
      assertNotNull(historyRepo.theAuState);
    }
  }

  public void testClockssSubscriptionStatus() {
    AuState aus = new AuState(mau, historyRepo);
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    assertEquals("Unknown", aus.getClockssSubscriptionStatusString());

    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
    assertEquals("Yes", aus.getClockssSubscriptionStatusString());

    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
    assertEquals("No", aus.getClockssSubscriptionStatusString());

    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
    assertEquals("Inaccessible", aus.getClockssSubscriptionStatusString());
  }    

  public void testAccessType() {
    AuState aus = new AuState(mau, historyRepo);
    assertFalse(aus.isOpenAccess());
    aus.setAccessType(AuState.AccessType.Subscription);
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertFalse(aus.isOpenAccess());
    aus.setAccessType(AuState.AccessType.OpenAccess);
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
    assertTrue(aus.isOpenAccess());
  }

  public void testSubstanceState() {
    AuState aus = new AuState(mau, historyRepo);
    assertEquals(SubstanceChecker.State.Unknown, aus.getSubstanceState());
    assertFalse(aus.hasNoSubstance());
    aus.setSubstanceState(SubstanceChecker.State.Yes);
    assertEquals(SubstanceChecker.State.Yes, aus.getSubstanceState());
    assertFalse(aus.hasNoSubstance());
    aus.setSubstanceState(SubstanceChecker.State.No);
    assertEquals(SubstanceChecker.State.No, aus.getSubstanceState());
    assertTrue(aus.hasNoSubstance());
  }

  public void testFeatureVersion() {
    AuState aus = new AuState(mau, historyRepo);
    assertNull(aus.getFeatureVersion(Plugin.Feature.Substance));
    assertNull(aus.getFeatureVersion(Plugin.Feature.Metadata));
    assertNull(aus.getFeatureVersion(Plugin.Feature.Poll));
    aus.setFeatureVersion(Plugin.Feature.Metadata, "foo");
    assertNull(aus.getFeatureVersion(Plugin.Feature.Substance));
    assertEquals("foo", aus.getFeatureVersion(Plugin.Feature.Metadata));
    aus.setFeatureVersion(Plugin.Feature.Substance, "sub_42");
    assertEquals("sub_42", aus.getFeatureVersion(Plugin.Feature.Substance));
    assertEquals("foo", aus.getFeatureVersion(Plugin.Feature.Metadata));
    assertNull(aus.getFeatureVersion(Plugin.Feature.Poll));
  }

  public void testLastContentChange() {
    TimeBase.setSimulated(10);
    AuState aus = new AuState(mau, historyRepo);
    aus.newCrawlStarted();
    TimeBase.step(10);
    aus.contentChanged();
    assertEquals(20,aus.getLastContentChange());
    TimeBase.step(10);
    aus.contentChanged();
    assertEquals(20,aus.getLastContentChange());
    TimeBase.step(10);
    aus.newCrawlFinished(1, "foo");
    TimeBase.step(10);
    aus.contentChanged();
    assertEquals(50,aus.getLastContentChange());
  }
    
  public static void main(String[] argv) {
    String[] testCaseList = { TestAuState.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }


  static class MyAuState extends AuState implements Cloneable {
    public MyAuState(ArchivalUnit au, HistoryRepository historyRepo) {
      super(au, historyRepo);
    }
    MyAuState simulateStoreLoad() throws CloneNotSupportedException {
      MyAuState ret = (MyAuState)this.clone();
      ret.previousCrawlState = null;
      return ret;
    }
  }
}
