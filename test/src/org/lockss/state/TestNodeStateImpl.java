/*
 * $Id: TestNodeStateImpl.java,v 1.14 2003-04-09 23:50:55 aalto Exp $
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


package org.lockss.state;

import java.io.*;
import java.util.*;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.test.*;
import org.lockss.util.CollectionUtil;
import org.apache.commons.collections.TreeBag;
import org.lockss.util.Deadline;

public class TestNodeStateImpl extends LockssTestCase {
  private NodeStateImpl state;
  private HistoryRepository historyRepo;
  private List polls;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestHistoryRepositoryImpl.configHistoryParams(tempDirPath);
    MockArchivalUnit mau = new MockArchivalUnit();
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    MockCachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    historyRepo = new HistoryRepositoryImpl(tempDirPath);
    historyRepo.startService();

    polls = new ArrayList(3);
    polls.add(new PollState(1, "lwr1", "upr1", 1, 0, Deadline.MAX));
    polls.add(new PollState(2, "lwr2", "upr3", 1, 0, Deadline.MAX));
    polls.add(new PollState(3, "lwr3", "upr3", 1, 0, Deadline.MAX));
    state = new NodeStateImpl(mcus, -1, new CrawlState(-1, -1, -1), polls,
                              historyRepo);
  }

  public void tearDown() throws Exception {
    historyRepo.stopService();
    super.tearDown();
  }

  public void testActivePollImmutability() {
    Iterator pollIter = state.getActivePolls();
    try {
      pollIter.remove();
      fail("Iterator should be immutable.");
    }
    catch (Exception e) {}
  }

  public void testGetPollHistories() {
    Iterator pollIt = state.getPollHistories();
    assertFalse(pollIt.hasNext());

    PollHistory history = new PollHistory(1, "test1lwr", "test1upr", 0, 321, 0, null);
    state.pollHistories = new ArrayList(3);
    state.pollHistories.add(history);
    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    history = (PollHistory) pollIt.next();
    assertEquals(1, history.type);
    assertEquals("test1lwr", history.lwrBound);
    assertEquals("test1upr", history.uprBound);
    assertFalse(pollIt.hasNext());

    history = new PollHistory(2, "test2lwr", "test2upr", 0, 123, 0, null);
    state.pollHistories.add(history);
    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    pollIt.next();
    assertTrue(pollIt.hasNext());
    history = (PollHistory) pollIt.next();
    assertEquals(2, history.type);
    assertEquals("test2lwr", history.lwrBound);
    assertEquals("test2upr", history.uprBound);
    assertFalse(pollIt.hasNext());
  }

  public void testCloseActivePoll() {
    Iterator pollIt = state.getPollHistories();
    assertFalse(pollIt.hasNext());

    PollHistory history = new PollHistory(1, "lwr1", "upr1", 0, 456, 0, null);
    state.closeActivePoll(history);

    pollIt = state.getActivePolls();
    while (pollIt.hasNext()) {
      PollState pollState = (PollState) pollIt.next();
      assertNotEquals(pollState, history);
    }

    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    history = (PollHistory) pollIt.next();
    assertEquals(1, history.type);
    assertEquals("lwr1", history.lwrBound);
    assertEquals("upr1", history.uprBound);
    assertFalse(pollIt.hasNext());

    // test proper insert
    history = new PollHistory(1, "test1lwr", "test1upr", 0, 234, 0, null);
    state.pollHistories.add(history);
    history = new PollHistory(1, "test2lwr", "test2upr", 0, 123, 0, null);
    state.pollHistories.add(history);
    history = new PollHistory(1, "test1lwr", "test1upr", 0, 345, 0, null);
    state.closeActivePoll(history);

    Iterator histIt = state.getPollHistories();
    ArrayList histL = new ArrayList(4);
    while (histIt.hasNext()) {
      PollHistory hist = (PollHistory)histIt.next();
      histL.add("starttime=" + hist.startTime);
    }
    String[] expectedA = new String[] {
        "starttime=456",
        "starttime=345",
        "starttime=234",
        "starttime=123",
    };
    assertIsomorphic(expectedA, histL);
  }

  public void testGetLastPollHistory() {
    assertNull(state.getLastPollHistory());

    PollHistory history = new PollHistory(1, "test1lwr", "test1upr", 0, 123, 0, null);
    state.pollHistories = new ArrayList(3);
    state.pollHistories.add(history);
    history = state.getLastPollHistory();
    assertNotNull(history);
    assertEquals(1, history.type);
    assertEquals("test1lwr", history.lwrBound);
    assertEquals("test1upr", history.uprBound);
    assertEquals(123, history.startTime);

    history = new PollHistory(1, "test1lwr", "test1upr", 0, 789, 0, null);
    state.closeActivePoll(history);
    history = new PollHistory(2, "test2lwr", "test2upr", 0, 456, 0, null);
    state.closeActivePoll(history);
    history = state.getLastPollHistory();
    assertNotNull(history);
    assertEquals(1, history.type);
    assertEquals("test1lwr", history.lwrBound);
    assertEquals("test1upr", history.uprBound);
    assertEquals(789, history.startTime);
  }

  public void testGetActivePolls() {
    Iterator expectedIter = polls.iterator();

    Iterator pollIter = state.getActivePolls();
    assertTrue(CollectionUtil.isIsomorphic(expectedIter, pollIter));
  }

  public void testAddPollState() {
    PollState state4 = new PollState(4, "lwr4", "upr4", 1, 0, Deadline.MAX);
    polls.add(state4);
    state.addPollState(state4);
    Iterator expectedIter = polls.iterator();

    Iterator pollIter = state.getActivePolls();
    assertTrue(CollectionUtil.isIsomorphic(expectedIter, pollIter));
  }

  public void testIsInternalNode() {
    MockCachedUrlSet mcus = new MockCachedUrlSet(null, null);
    Vector childV = new Vector();
    mcus.setFlatIterator(childV.iterator());
    state = new NodeStateImpl(mcus, -1, null, null, null);
    assertFalse(state.isInternalNode());

    childV.addElement("test string");
    mcus.setFlatIterator(childV.iterator());
    assertTrue(state.isInternalNode());
  }

  public void testLastHashDuration() throws Exception {
    assertEquals(-1, state.getAverageHashDuration());
    state.setLastHashDuration(123);
    assertEquals(123, state.getAverageHashDuration());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestNodeStateImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
