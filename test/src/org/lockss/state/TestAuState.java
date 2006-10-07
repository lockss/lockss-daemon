/*
 * $Id: TestAuState.java,v 1.5.2.1 2006-10-07 01:57:29 smorabito Exp $
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


package org.lockss.state;

import java.util.*;
import org.lockss.test.*;
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

  public void testCrawlFinished() {
    AuState auState = new AuState(mau, 123, -1, -1, null, 1, 0, historyRepo);
    assertEquals(123, auState.getLastCrawlTime());
    assertNull(historyRepo.theAuState);

    TimeBase.setSimulated(456);
    auState.newCrawlFinished();
    assertEquals(456, auState.getLastCrawlTime());
    assertNotNull(historyRepo.theAuState);
  }

  public void testPollFinished() {
    AuState auState = new AuState(mau, -1, 123, -1, null, 1, 0, historyRepo);
    assertEquals(123, auState.getLastTopLevelPollTime());
    assertNull(historyRepo.theAuState);

    TimeBase.setSimulated(456);
    auState.newPollFinished();
    assertEquals(456, auState.getLastTopLevelPollTime());
    assertNotNull(historyRepo.theAuState);
  }

  public void testTreeWalkFinished() {
    AuState auState = new AuState(mau, -1, -1, 123, null, 1, 0, historyRepo);
    assertEquals(123, auState.getLastTreeWalkTime());

    TimeBase.setSimulated(456);
    auState.setLastTreeWalkTime();
    assertEquals(456, auState.getLastTreeWalkTime());
  }

  public void testGetUrls() {
    HashSet stringCollection = new HashSet();
    stringCollection.add("test");

    AuState auState =
      new AuState(mau, -1, -1, 123, stringCollection, 1, 0, historyRepo);
    Collection col = auState.getCrawlUrls();
    Iterator colIter = col.iterator();
    assertTrue(colIter.hasNext());
    assertEquals("test", colIter.next());
    assertFalse(colIter.hasNext());
  }

  public void testUpdateUrls() {
    AuState auState =
      new AuState(mau, -1, -1, 123, new HashSet(), 1, 0, historyRepo);
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
    AuState auState =
      new AuState(mau, -1, -1, 123, new HashSet(), 1, 0, historyRepo);
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


  public static void main(String[] argv) {
    String[] testCaseList = { TestAuState.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
