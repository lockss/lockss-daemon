/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.plugin.*;
import org.lockss.poller.v3.*;
import org.lockss.poller.v3.V3Poller.PollVariant;
import org.lockss.repository.*;
import org.lockss.util.*;

public class TestAuState extends LockssTestCase {
  MockLockssDaemon daemon;
  MyMockHistoryRepository historyRepo;
  MockPlugin mplug;
  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();

    historyRepo = new MyMockHistoryRepository();
    mplug = new MockPlugin(daemon);
    mau = new MockArchivalUnit(mplug);
  }


  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  /*
   * Abbreviate the verbose constructor
   */
  private AuState makeAuState(ArchivalUnit au,
			      long lastCrawlTime,
			      long lastCrawlAttempt,
			      long lastTopLevelPoll,
			      long lastPollStart,
			      long lastTreeWalk,
			      HashSet crawlUrls,
			      int clockssSubscriptionStatus,
			      double v3Agreement,
			      double highestV3Agreement,
			      HistoryRepository historyRepo) {
    return new AuState(au,
		       lastCrawlTime,
		       lastCrawlAttempt,
		       -1,
		       null,
		       -1,
		       -1,
		       -1,
		       null,
		       -1,
		       lastTopLevelPoll,
		       lastPollStart,
		       -1,
		       null,
		       0,
		       lastTreeWalk,
		       crawlUrls,
		       null,
		       clockssSubscriptionStatus,
		       v3Agreement,
		       highestV3Agreement,
		       SubstanceChecker.State.Unknown,
		       null,                          // substanceFeatureVersion
		       null,                          // metadataFeatureVersion
		       -1,                            // lastMetadataIndex
		       TimeBase.nowMs(),              // lastContentChange
		       -1,
		       -1,
		       -1,
		       -1, // numWillingRepairers
		       -1, // numCurrentSuspectVersions
		       0,
		       null,
		       historyRepo);
  }

  int t1 = 456;
  int t2 = 12000;
  int t3 = 14000;
  int t4 = 17000;
  int t5 = 23000;
  int t6 = 25000;
  int t7 = 25001;

  public void testCrawlStatus() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(-1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastCrawlResult());
    assertEquals("Unknown code -1", aus.getLastCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(-1, aus.getLastDeepCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlResult());
    assertEquals("Unknown code -1", aus.getLastDeepCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlDepth());
    assertFalse(aus.isCrawlActive());
    assertFalse(aus.hasCrawled());
    assertNull(historyRepo.theAuState);
    assertEquals(0, historyRepo.getAuStateStoreCount());

    TimeBase.setSimulated(t1);
    aus.newCrawlStarted();
    // these should now reflect the previous crawl, not the active one
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(-1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastCrawlResult());
    assertEquals("Unknown code -1", aus.getLastCrawlResultMsg());

    // not a deep crawl so these are unaffected
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(-1, aus.getLastDeepCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlResult());
    assertEquals("Unknown code -1", aus.getLastDeepCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlDepth());

    assertTrue(aus.isCrawlActive());
    assertFalse(aus.hasCrawled());
    assertNotNull(historyRepo.theAuState);
    assertEquals(1, historyRepo.getAuStateStoreCount());

    TimeBase.setSimulated(t2);
    aus.newCrawlFinished(Crawler.STATUS_ERROR, "Plorg");
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(-1, aus.getLastDeepCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlResult());
    assertEquals("Unknown code -1", aus.getLastDeepCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlDepth());
    assertEquals(Crawler.STATUS_ERROR, aus.getLastCrawlResult());
    assertEquals("Plorg", aus.getLastCrawlResultMsg());
    assertFalse(aus.isCrawlActive());
    assertFalse(aus.hasCrawled());
    assertEquals(2, historyRepo.getAuStateStoreCount());

    TimeBase.setSimulated(t3);
    aus.newCrawlFinished(Crawler.STATUS_SUCCESSFUL, "Syrah");
    assertEquals(t3, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(-1, aus.getLastDeepCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlResult());
    assertEquals("Unknown code -1", aus.getLastDeepCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlDepth());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals("Syrah", aus.getLastCrawlResultMsg());
    assertFalse(aus.isCrawlActive());
    assertTrue(aus.hasCrawled());
    assertEquals(3, historyRepo.getAuStateStoreCount());

    aus = aus.simulateStoreLoad();
    assertEquals(t3, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(-1, aus.getLastDeepCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlResult());
    assertEquals("Unknown code -1", aus.getLastDeepCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlDepth());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals("Syrah", aus.getLastCrawlResultMsg());
    assertFalse(aus.isCrawlActive());
    assertTrue(aus.hasCrawled());

    TimeBase.setSimulated(t4-1);
    aus.deepCrawlStarted(999 /* unused */);
    TimeBase.setSimulated(t4);
    aus.newCrawlFinished(Crawler.STATUS_SUCCESSFUL, "Syrah", 43);
    assertEquals(t4, aus.getLastCrawlTime());
    assertEquals(t4-1, aus.getLastCrawlAttempt());
    assertEquals(t4, aus.getLastDeepCrawlTime());
    assertEquals(t4-1, aus.getLastDeepCrawlAttempt());
    assertEquals(43, aus.getLastDeepCrawlDepth());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals("Syrah", aus.getLastCrawlResultMsg());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastDeepCrawlResult());
    assertEquals("Syrah", aus.getLastDeepCrawlResultMsg());

    TimeBase.setSimulated(t5);
    aus.newCrawlStarted();
    assertEquals(t4, aus.getLastCrawlTime());
    assertEquals(t4-1, aus.getLastCrawlAttempt());
    assertEquals(t4, aus.getLastDeepCrawlTime());
    assertEquals(t4-1, aus.getLastDeepCrawlAttempt());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals("Syrah", aus.getLastCrawlResultMsg());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastDeepCrawlResult());
    assertEquals("Syrah", aus.getLastDeepCrawlResultMsg());
    assertTrue(aus.hasCrawled());

    TimeBase.setSimulated(t6);
    aus.newCrawlFinished(Crawler.STATUS_SUCCESSFUL, "Shiraz");
    assertEquals(t6, aus.getLastCrawlTime());
    assertEquals(t5, aus.getLastCrawlAttempt());
    assertEquals(t4, aus.getLastDeepCrawlTime());
    assertEquals(43, aus.getLastDeepCrawlDepth());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals("Shiraz", aus.getLastCrawlResultMsg());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastDeepCrawlResult());
    assertEquals("Syrah", aus.getLastDeepCrawlResultMsg());


    TimeBase.setSimulated(t6);
    aus.newCrawlFinished(Crawler.STATUS_FETCH_ERROR, "Syrah", 999);
    assertEquals(t6, aus.getLastCrawlTime());
    assertEquals(t5, aus.getLastCrawlAttempt());
    assertEquals(t4, aus.getLastDeepCrawlTime());
    assertEquals(43, aus.getLastDeepCrawlDepth());
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
    // these should now reflect the previous crawl, not the active one
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
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(-1, aus.getLastDeepCrawlAttempt());
    assertEquals("Plorg", aus.getLastCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlResult());
    assertEquals("Unknown code -1", aus.getLastDeepCrawlResultMsg());
  }

  public void testDaemonCrashedDuringDeepCrawl() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(-1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastCrawlResult());
    assertEquals("Unknown code -1", aus.getLastCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(-1, aus.getLastDeepCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlResult());
    assertEquals("Unknown code -1", aus.getLastDeepCrawlResultMsg());
    assertFalse(aus.isCrawlActive());
    assertNull(historyRepo.theAuState);

    TimeBase.setSimulated(t1);
    aus.deepCrawlStarted(444);
    // these should now reflect the previous crawl, not the active one
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(-1, aus.getLastCrawlAttempt());
    assertEquals(-1, aus.getLastCrawlResult());
    assertEquals("Unknown code -1", aus.getLastCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(-1, aus.getLastDeepCrawlAttempt());
    assertEquals(-1, aus.getLastDeepCrawlResult());
    assertEquals("Unknown code -1", aus.getLastDeepCrawlResultMsg());

    assertTrue(aus.isCrawlActive());
    assertNotNull(historyRepo.theAuState);

    TimeBase.setSimulated(t2);
    aus = aus.simulateStoreLoad();
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_RUNNING_AT_CRASH, aus.getLastCrawlResult());
    assertEquals("Interrupted by plugin reload or daemon exit", aus.getLastCrawlResultMsg());
    assertEquals(-1, aus.getLastDeepCrawlTime());
    assertEquals(456, aus.getLastDeepCrawlAttempt());
    assertEquals(Crawler.STATUS_RUNNING_AT_CRASH, aus.getLastDeepCrawlResult());
    assertEquals("Interrupted by plugin reload or daemon exit", aus.getLastDeepCrawlResultMsg());
    assertFalse(aus.isCrawlActive());

    TimeBase.setSimulated(t3);
    aus.deepCrawlStarted(444);
    assertEquals(-1, aus.getLastCrawlTime());
    assertEquals(t1, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_RUNNING_AT_CRASH, aus.getLastCrawlResult());

    TimeBase.setSimulated(t4);
    aus.newCrawlFinished(Crawler.STATUS_SUCCESSFUL, "Sirah", 789);
    assertEquals(t4, aus.getLastCrawlTime());
    assertEquals(t3, aus.getLastCrawlAttempt());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastCrawlResult());
    assertEquals(t4, aus.getLastDeepCrawlTime());
    assertEquals(t3, aus.getLastDeepCrawlAttempt());
    assertEquals("Sirah", aus.getLastCrawlResultMsg());
    assertEquals(Crawler.STATUS_SUCCESSFUL, aus.getLastDeepCrawlResult());
    assertEquals("Sirah", aus.getLastDeepCrawlResultMsg());
  }

  public void testPollDuration() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(0, aus.getPollDuration());
    aus.setPollDuration(1000);
    assertEquals(1000, aus.getPollDuration());
    aus.setPollDuration(2000);
    assertEquals(1500, aus.getPollDuration());
  }

  public void testPollTimeAndResult() throws Exception {
    MyAuState aus = new MyAuState(mau, historyRepo);
    assertEquals(-1, aus.getLastTopLevelPollTime());
    assertEquals(-1, aus.getLastPollStart());
    assertEquals(-1, aus.getLastPollResult());
    assertEquals(null, aus.getLastPollResultMsg());
    assertEquals(-1, aus.getLastPoPPoll());
    assertEquals(-1, aus.getLastPoPPollResult());
    assertEquals(null, aus.getLastPoPPollResultMsg());
    assertEquals(-1, aus.getLastLocalHashScan());
    assertEquals(-1, aus.getLastTimePollCompleted());
    assertEquals(0, aus.getPollDuration());
    assertNull(historyRepo.theAuState);

    TimeBase.setSimulated(t1);
    aus.pollStarted();
    // running poll
    assertEquals(t1, aus.getLastPollStart());
    // These haven't been updated yet
    assertEquals(-1, aus.getLastTopLevelPollTime());
    assertEquals(-1, aus.getLastPollResult());
    assertEquals(0, aus.getPollDuration());
    assertEquals(-1, aus.getLastTimePollCompleted());
    assertNotNull(historyRepo.theAuState);

    TimeBase.setSimulated(t2);
    aus.pollFinished(V3Poller.POLLER_STATUS_ERROR, PollVariant.PoR);
    assertEquals(-1, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_ERROR, aus.getLastPollResult());
    assertEquals("Error", aus.getLastPollResultMsg());
    assertEquals(t2, aus.getPollDuration());
    assertEquals(-1, aus.getLastPoPPoll());
    assertEquals(-1, aus.getLastPoPPollResult());
    assertEquals(null, aus.getLastPoPPollResultMsg());
    assertEquals(-1, aus.getLastLocalHashScan());
    assertEquals(-1, aus.getLastTimePollCompleted());

    TimeBase.setSimulated(t3);
    aus.pollFinished(V3Poller.POLLER_STATUS_COMPLETE, PollVariant.PoR);
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPollResult());
    assertEquals("Complete", aus.getLastPollResultMsg());
    assertEquals((t3 + t2) / 2, aus.getPollDuration());
    assertEquals(-1, aus.getLastPoPPoll());
    assertEquals(-1, aus.getLastPoPPollResult());
    assertEquals(null, aus.getLastPoPPollResultMsg());
    assertEquals(-1, aus.getLastLocalHashScan());
    assertEquals(t3, aus.getLastTimePollCompleted());

    TimeBase.setSimulated(t4);
    aus.pollFinished(V3Poller.POLLER_STATUS_NO_QUORUM, PollVariant.PoP);
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPollResult());
    assertEquals("Complete", aus.getLastPollResultMsg());
    assertEquals((t3 + t2) / 2, aus.getPollDuration());
    assertEquals(-1, aus.getLastPoPPoll());
    assertEquals(V3Poller.POLLER_STATUS_NO_QUORUM, aus.getLastPoPPollResult());
    assertEquals(-1, aus.getLastLocalHashScan());
    assertEquals("No Quorum", aus.getLastPoPPollResultMsg());
    assertEquals(t3, aus.getLastTimePollCompleted());

    TimeBase.setSimulated(t5);
    aus.pollFinished(V3Poller.POLLER_STATUS_COMPLETE, PollVariant.PoP);
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPollResult());
    assertEquals("Complete", aus.getLastPollResultMsg());
    assertEquals((t3 + t2) / 2, aus.getPollDuration());
    assertEquals(t5, aus.getLastPoPPoll());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPoPPollResult());
    assertEquals("Complete", aus.getLastPoPPollResultMsg());
    assertEquals(-1, aus.getLastLocalHashScan());
    assertEquals(t5, aus.getLastTimePollCompleted());
    aus.pollFinished(V3Poller.POLLER_STATUS_NO_QUORUM, PollVariant.PoP);

    TimeBase.setSimulated(t6);
    aus.pollFinished(V3Poller.POLLER_STATUS_COMPLETE, PollVariant.Local);
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPollResult());
    assertEquals("Complete", aus.getLastPollResultMsg());
    assertEquals((t3 + t2) / 2, aus.getPollDuration());
    assertEquals(t5, aus.getLastPoPPoll());
    assertEquals(V3Poller.POLLER_STATUS_NO_QUORUM, aus.getLastPoPPollResult());
    assertEquals("No Quorum", aus.getLastPoPPollResultMsg());
    assertEquals(t6, aus.getLastLocalHashScan());
    assertEquals(t5, aus.getLastTimePollCompleted());

    aus = aus.simulateStoreLoad();
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t1, aus.getLastPollStart());
    assertEquals(V3Poller.POLLER_STATUS_COMPLETE, aus.getLastPollResult());

    TimeBase.setSimulated(t7);
    aus.pollStarted();
    assertEquals(t3, aus.getLastTopLevelPollTime());
    assertEquals(t7, aus.getLastPollStart());
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
    AuState auState = makeAuState(mau, -1, -1, -1, -1, 123, null,
				  1, -1.0, 1.0, historyRepo);
    assertEquals(123, auState.getLastTreeWalkTime());

    TimeBase.setSimulated(456);
    auState.setLastTreeWalkTime();
    assertEquals(456, auState.getLastTreeWalkTime());
  }

  public void testGetUrls() {
    HashSet stringCollection = new HashSet();
    stringCollection.add("test");

    AuState auState = makeAuState(mau, -1, -1, -1, -1, 123,
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
    assertEquals(1, historyRepo.getAuStateStoreCount());
    assertEquals(SubstanceChecker.State.Yes, aus.getSubstanceState());
    assertFalse(aus.hasNoSubstance());
    aus.setSubstanceState(SubstanceChecker.State.No);
    assertEquals(2, historyRepo.getAuStateStoreCount());
    assertEquals(SubstanceChecker.State.No, aus.getSubstanceState());
    assertTrue(aus.hasNoSubstance());
    assertNotEquals("2", aus.getFeatureVersion(Plugin.Feature.Substance));
    mplug.setFeatureVersionMap(MapUtil.map(Plugin.Feature.Substance, "2"));
    aus.setSubstanceState(SubstanceChecker.State.Yes);
    // changing both the substance state and feature version should store
    // only once
    assertEquals(3, historyRepo.getAuStateStoreCount());
    assertEquals(SubstanceChecker.State.Yes, aus.getSubstanceState());
    assertEquals("2", aus.getFeatureVersion(Plugin.Feature.Substance));
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

  public void testLastMetadataIndex() {
    AuState aus = new AuState(mau, historyRepo);
    assertEquals(-1, aus.getLastMetadataIndex());
    aus.setLastMetadataIndex(123);
    assertEquals(123, aus.getLastMetadataIndex());
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
    
  public void testNumCurrentSuspectVersions() {
    MyMockLockssRepository repo = new MyMockLockssRepository();
    MyAuSuspectUrlVersions asuv = new MyAuSuspectUrlVersions();
    repo.setAsuv(asuv);

    daemon.setLockssRepository(repo, mau);
    AuState aus = new AuState(mau, historyRepo);
    assertEquals(0, aus.getNumCurrentSuspectVersions());
    // ensure this isn't automatically recomputed, as that would happen
    // when historyRepo loads the object during startAuManagers, before the
    // AU is fully created.
    aus.setNumCurrentSuspectVersions(-1);
    assertEquals(-1, aus.getNumCurrentSuspectVersions());
    asuv.setCountResult(17);
    aus.recomputeNumCurrentSuspectVersions();
    assertEquals(17, aus.getNumCurrentSuspectVersions());
    aus.incrementNumCurrentSuspectVersions(-1);
    assertEquals(16, aus.getNumCurrentSuspectVersions());

    aus.setNumCurrentSuspectVersions(-1);
    asuv.setCountResult(6);
    aus.incrementNumCurrentSuspectVersions(-1);
    assertEquals(5, aus.getNumCurrentSuspectVersions());
  }

  public void testCdnStems() {
    AuState aus = new AuState(mau, historyRepo);
    assertEquals(Collections.EMPTY_LIST, aus.getCdnStems());
    aus.addCdnStem("http://fff.uselesstld");
    assertClass(ArrayList.class, aus.getCdnStems());
    assertEquals(ListUtil.list("http://fff.uselesstld"), aus.getCdnStems());
    aus.addCdnStem("ccc");
    assertEquals(ListUtil.list("http://fff.uselesstld", "ccc"),
		 aus.getCdnStems());

    aus.setCdnStems(new LinkedList(ListUtil.list("a", "b")));
    assertClass(ArrayList.class, aus.getCdnStems());
    assertEquals(ListUtil.list("a", "b"), aus.getCdnStems());
    aus.setCdnStems(null);
    assertEmpty(aus.getCdnStems());
    aus.addCdnStem("https://a.b/");
    aus.addCdnStem("https://b.a/");
    assertEquals(ListUtil.list("https://a.b/", "https://b.a/"),
		 aus.getCdnStems());
  }

  public void testBatch() {
    AuState aus = new AuState(mau, historyRepo);
    assertEquals(0, historyRepo.getAuStateStoreCount());
    aus.setNumAgreePeersLastPoR(1);
    aus.setNumWillingRepairers(3);
    aus.setNumCurrentSuspectVersions(5);
    assertEquals(3, historyRepo.getAuStateStoreCount());

    aus.batchSaves();
    aus.setNumAgreePeersLastPoR(2);
    aus.setNumWillingRepairers(4);
    aus.setNumCurrentSuspectVersions(6);
    assertEquals(3, historyRepo.getAuStateStoreCount());
    aus.unBatchSaves();
    assertEquals(4, historyRepo.getAuStateStoreCount());

    aus.batchSaves();
    aus.setNumAgreePeersLastPoR(4);
    aus.batchSaves();
    aus.setNumWillingRepairers(8);
    aus.setNumCurrentSuspectVersions(12);
    assertEquals(4, historyRepo.getAuStateStoreCount());
    aus.unBatchSaves();
    assertEquals(4, historyRepo.getAuStateStoreCount());
    aus.unBatchSaves();
    assertEquals(5, historyRepo.getAuStateStoreCount());
  }

  // Return the serialized representation of the object
  String ser(LockssSerializable o) throws Exception {
    File tf = getTempFile("ser", ".xml");
    new XStreamSerializer().serialize(tf, o);
    return StringUtil.fromFile(tf);
  }

  // Deserialize and return a Holder from the string
  AuState deser(String s) throws Exception {
    return (AuState)(new XStreamSerializer().deserialize(new StringReader(s)));
  }

  // Ensure that fields added to AuState get their default value when
  // deserialized from old files not containing the field
  public void testNewField() throws Exception {
    AuState aus = makeAuState(mau, -1, -1, -1, -1, 123, null,
			      1, -1.0, 1.0, historyRepo);

    String ser = ser(aus);
    String edser = ser.replaceAll(".*numAgreePeersLastPoR.*", "");
    log.debug2("old: " + ser);
    log.debug2("new: " + edser);
    AuState newaus = deser(edser);
    assertEquals(-1, newaus.getNumAgreePeersLastPoR());
    assertEquals(Collections.EMPTY_LIST, newaus.getCdnStems());
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

  static class MyMockHistoryRepository extends MockHistoryRepository {
    int auStateStoreCount = 0;

    @Override
    public void storeAuState(AuState auState) {
      super.storeAuState(auState);
      auStateStoreCount++;
    }

    public int getAuStateStoreCount() {
      return auStateStoreCount;
    }
  }

  static class MyMockLockssRepository extends MockLockssRepository {
    AuSuspectUrlVersions asuv;

    @Override
    public AuSuspectUrlVersions getSuspectUrlVersions(ArchivalUnit au) {
      return asuv;
    }

    @Override
    public boolean hasSuspectUrlVersions(ArchivalUnit au) {
      return asuv != null;
    }

    public void setAsuv(AuSuspectUrlVersions asuv) {
      this.asuv = asuv;
    }
  }

  static class MyAuSuspectUrlVersions extends AuSuspectUrlVersions {
    int countResult;

    @Override
    public int countCurrentSuspectVersions(ArchivalUnit au) {
      return countResult;
    }

    public void setCountResult(int n) {
      countResult = n;
    }
  }

}
