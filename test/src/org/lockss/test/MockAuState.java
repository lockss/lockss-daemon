/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.util.*;
import junit.framework.*;
import org.lockss.state.*;
import org.lockss.plugin.ArchivalUnit;

/**
 * This is a mock version of <code>ArchivalUnit</code> used for testing
 */

public class MockAuState extends AuState {

  HashSet crawlUrls = new HashSet();
  int updatedCrawlUrlsCalled = 0;
  private long auCreate = -1;

  public MockAuState(ArchivalUnit au) {
    this(au, -1, -1, -1, new MockHistoryRepository());
  }

  public MockAuState() {
    this(null, -1, -1, -1, new MockHistoryRepository());
  }

  public MockAuState(ArchivalUnit au,
		     long lastCrawlTime, long lastPollTime,
                     long lastTreeWalk, HistoryRepository historyRepo) {
    this(au, lastCrawlTime, -1, lastPollTime, -1, lastTreeWalk,
	 null, historyRepo);
  }

  public MockAuState(ArchivalUnit au,
		     long lastCrawlTime, long lastCrawlAttempt,
		     long lastPollTime, long lastPollStart,
                     long lastTreeWalk, HistoryRepository historyRepo) {
    this(au, lastCrawlTime, lastCrawlAttempt, lastPollTime, lastPollStart,
	 lastTreeWalk, null, historyRepo);
  }

  public MockAuState(ArchivalUnit au,
		     long lastCrawlTime, long lastCrawlAttempt,
		     long lastPollTime, long lastPollStart,
                     long lastTreeWalk, HashSet crawlUrls,
                     HistoryRepository historyRepo) {
    super(au,
	  lastCrawlTime,
	  lastCrawlAttempt,
	  -1, //lastCrawlResult
	  null, // lastCrawlResultMsg
	  lastPollTime, //lastTopLevelPoll
	  lastPollStart,
	  -1, // lastPollResult
	  null, // lastPollResultMsg
	  0L, // pollDuration
	  lastTreeWalk,
	  crawlUrls,
	  null, // accessType
	  CLOCKSS_SUB_UNKNOWN, // clockssSubscriptionState
	  -1.0, // v3Agreement
	  -1.0, // highestV3Agreement
	  SubstanceChecker.State.Unknown,
	  null, // substanceVersion
	  null, // metadataVersion
	  -1, //lastMetadataIndex
	  0L, // lastContentChange
	  -1L, // lastPoPPoll
	  -1, // lastPoPPollResult
	  -1L, // lastLocalHashScan
	  -1, // lastLocalHashMismatch
	  -1, // numAgreePeersLastPoR
	  -1, // numWillingRepairers
	  -1, // numCurrentSuspectVersions
	  historyRepo);
  }

  public long getAuCreationTime() {
    if (auCreate >= 0) {
      return auCreate;
    }
    return super.getAuCreationTime();
  }

  public void setAuCreationTime(long time) {
    auCreate = time;
  }

  public void setLastCrawlTime(long newCrawlTime) {
    lastCrawlTime = newCrawlTime;
  }

  public void setLastContentChange(long newTime) {
    lastContentChange = newTime;
  }

  public void setLastTopLevelPollTime(long newPollTime) {
    lastTopLevelPoll = newPollTime;
  }

  @Override
  public void setV3Agreement(double d) {
    v3Agreement = d;
  }

  public void setHighestV3Agreement(double d) {
    highestV3Agreement = d;
  }

  public void setLastPollResult(int result) {
    lastPollResult = result;
  }

  public void setLastPollStart(long time) {
    lastPollStart = time;
  }

  public void setLastToplevalPoll(long time) {
    lastTopLevelPoll = time;
  }

  public void setLastTreeWalkTime(long newTreeWalkTime) {
    lastTreeWalk = newTreeWalkTime;
  }

  public void setLastCrawlAttempt(long lastCrawlAttempt) {
    this.lastCrawlAttempt = lastCrawlAttempt;
  }

  public void newCrawlFinished(int result, String resultMsg) {
    super.newCrawlFinished(result, resultMsg);
  }

  public void setLastCrawlResult(int result, String resultMsg) {
    lastCrawlResult = result;
    lastCrawlResultMsg = resultMsg;
  }

  public HashSet getCrawlUrls() {
    return crawlUrls;
  }

  public void updatedCrawlUrls(boolean forceUpdate) {
    updatedCrawlUrlsCalled++;
  }

  public void assertUpdatedCrawlListCalled(int numTimes) {
    if (numTimes != updatedCrawlUrlsCalled) {
      Assert.fail("updatedCrawlUrls was only called "+updatedCrawlUrlsCalled
	   +" but should have been called "+numTimes+" times");
    }
  }
}

