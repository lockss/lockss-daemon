/*
 * $Id: TestNodeStateImpl.java,v 1.24 2006-08-27 05:06:24 tlipkis Exp $
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

import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.protocol.IdentityManager;

public class TestNodeStateImpl extends LockssTestCase {
  private NodeStateImpl state;
  private MyHistoryRepositoryImpl historyRepo;
  private List polls;

  private static final int MAX_COUNT = 10;
  private static final long MAX_AGE = 2000;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(NodeStateImpl.PARAM_POLL_HISTORY_MAX_COUNT,
        Integer.toString(MAX_COUNT));
    p.setProperty(NodeStateImpl.PARAM_POLL_HISTORY_MAX_AGE,
        Long.toString(MAX_AGE));
    ConfigurationUtil.setCurrentConfigFromProps(p);
    MockArchivalUnit mau = new MockArchivalUnit();
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    MockCachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    historyRepo = new MyHistoryRepositoryImpl(mau, tempDirPath);
    historyRepo.startService();

    polls = new ArrayList(3);
    polls.add(new PollState(1, "lwr1", "upr1", 1, 0, Deadline.MAX, false));
    polls.add(new PollState(2, "lwr2", "upr3", 1, 0, Deadline.MAX, false));
    polls.add(new PollState(3, "lwr3", "upr3", 1, 0, Deadline.MAX, false));
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

    PollHistory history = new PollHistory(1, "test1lwr", "test1upr", 0, 321, 0, null, false);
    state.pollHistories = new ArrayList(3);
    state.pollHistories.add(history);
    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    history = (PollHistory) pollIt.next();
    assertEquals(1, history.type);
    assertEquals("test1lwr", history.lwrBound);
    assertEquals("test1upr", history.uprBound);
    assertFalse(pollIt.hasNext());

    history = new PollHistory(2, "test2lwr", "test2upr", 0, 123, 0, null, false);
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
    // avoid poll trimming
    TimeBase.setSimulated(1234);
    Iterator pollIt = state.getPollHistories();
    assertFalse(pollIt.hasNext());

    assertEquals(3, state.polls.size());
    PollHistory history = new PollHistory(1, "lwr1", "upr1", 0, 456, 0, null, false);
    state.closeActivePoll(history);
    assertEquals(2, state.polls.size());

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
    history = new PollHistory(1, "test1lwr", "test1upr", 0, 234, 0, null, false);
    state.pollHistories.add(history);
    history = new PollHistory(1, "test2lwr", "test2upr", 0, 123, 0, null, false);
    state.pollHistories.add(history);
    history = new PollHistory(1, "test1lwr", "test1upr", 0, 345, 0, null, false);
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

    // should be placed at head of list
    history = new PollHistory(1, "test1lwr", "test1upr", 0, 457, 0, null, false);
    state.closeActivePoll(history);
    assertSame(history, (PollHistory)state.getPollHistories().next());

    // should be placed at tail of list
    history = new PollHistory(1, "test1lwr", "test1upr", 0, 122, 0, null, false);
    state.closeActivePoll(history);
    List lst = ListUtil.fromIterator(state.getPollHistories());
    assertSame(history, (PollHistory)lst.get(lst.size() - 1));

    TimeBase.setReal();
  }

  public void testCloseAdditionalStates() {
    state.polls.add(new PollState(1, "lwr1", "upr1", 1, 0, Deadline.MAX, false));
    assertEquals(4, state.polls.size());
    PollHistory history = new PollHistory(1, "lwr1", "upr1", 0, 456, 0, null, false);
    state.closeActivePoll(history);
    assertEquals(2, state.polls.size());
  }

  public void testGetLastPollHistory() {
    // avoid poll trimming
    TimeBase.setSimulated(1234);
    assertNull(state.getLastPollHistory());

    PollHistory history = new PollHistory(1, "test1lwr", "test1upr", 0, 123, 0, null, false);
    state.pollHistories = new ArrayList(3);
    state.pollHistories.add(history);
    history = state.getLastPollHistory();
    assertNotNull(history);
    assertEquals(1, history.type);
    assertEquals("test1lwr", history.lwrBound);
    assertEquals("test1upr", history.uprBound);
    assertEquals(123, history.startTime);

    history = new PollHistory(1, "test2lwr", "test2upr", 0, 789, 0, null, false);
    state.closeActivePoll(history);
    history = new PollHistory(2, "test1lwr", "test1upr", 0, 456, 0, null, false);
    state.closeActivePoll(history);
    history = state.getLastPollHistory();
    assertEquals(1, history.type);
    assertEquals("test2lwr", history.lwrBound);
    assertEquals("test2upr", history.uprBound);
    assertEquals(789, history.startTime);

    history = new PollHistory(2, "test2lwr", "test2upr", 0, 890, 0, null, false);
    state.closeActivePoll(history);
    history = new PollHistory(2, "test1lwr", "test1upr", 0, 567, 0, null, false);
    state.closeActivePoll(history);
    history = state.getLastPollHistory();
    assertEquals(2, history.type);
    assertEquals("test2lwr", history.lwrBound);
    assertEquals("test2upr", history.uprBound);
    assertEquals(890, history.startTime);
    TimeBase.setReal();
  }

  public void testGetActivePolls() {
    Iterator expectedIter = polls.iterator();

    Iterator pollIter = state.getActivePolls();
    assertTrue(CollectionUtil.isIsomorphic(expectedIter, pollIter));
  }

  public void testAddPollState() {
    PollState state4 = new PollState(4, "lwr4", "upr4", 1, 0, Deadline.MAX, false);
    polls.add(state4);
    state.addPollState(state4);
    Iterator expectedIter = polls.iterator();

    Iterator pollIter = state.getActivePolls();
    assertTrue(CollectionUtil.isIsomorphic(expectedIter, pollIter));
  }

  public void testIsInternalNode() {
    MockCachedUrlSet mcus = new MockCachedUrlSet(null, null);
    mcus.setIsLeaf(true);
    state = new NodeStateImpl(mcus, -1, null, null, null);
    assertFalse(state.isInternalNode());

    mcus.setIsLeaf(false);
    assertTrue(state.isInternalNode());
  }

  public void testState() throws Exception {
    assertEquals(NodeState.INITIAL, state.getState());
    state.setState(NodeState.OK);
    assertEquals(NodeState.OK, state.getState());
  }

  public void testLastHashDuration() throws Exception {
    assertEquals(-1, state.getAverageHashDuration());
    state.setLastHashDuration(123);
    assertEquals(123, state.getAverageHashDuration());
  }

  public void testPollTrimmingCount() throws Exception {
    TimeBase.setSimulated(1234);
    Iterator pollIt = state.getPollHistories();
    assertFalse(pollIt.hasNext());
    state.pollHistories = new ArrayList(MAX_COUNT);

    for (int ii=0; ii<MAX_COUNT; ii++) {
      // fill the list
      state.pollHistories.add(new PollHistory(1, "test1", "test1", 0, 123 + ii,
          0, null, false));
    }
    trimHistoriesIfNeeded(state);
    assertEquals(MAX_COUNT, state.pollHistories.size());
    PollHistory history = (PollHistory)state.pollHistories.get(MAX_COUNT - 1);
    assertEquals(123, history.getStartTime());

    // exceed max
    state.pollHistories.add(0, new PollHistory(1, "test1", "test1", 0, 200,
        0, null, false));
    trimHistoriesIfNeeded(state);
    // still max
    assertEquals(MAX_COUNT, state.pollHistories.size());
    history = (PollHistory)state.pollHistories.get(MAX_COUNT - 1);
    // oldest was pushed out
    assertEquals(124, history.getStartTime());

    ConfigurationUtil.addFromArgs(NodeStateImpl.PARAM_POLL_HISTORY_TRIM_TO,
				  Integer.toString(MAX_COUNT - 3));
    
    // trim now should do nothing
    trimHistoriesIfNeeded(state);
    assertEquals(MAX_COUNT, state.pollHistories.size());
    assertSame(history, state.pollHistories.get(MAX_COUNT - 1));

    // exceed max again, should reduce to trimTo size
    state.pollHistories.add(0, new PollHistory(1, "test1", "test1", 0, 199,
        0, null, false));
    trimHistoriesIfNeeded(state);
    assertEquals(MAX_COUNT - 3, state.pollHistories.size());

    TimeBase.setReal();
  }

  public void testPollTrimmingAge() throws Exception {
    TimeBase.setSimulated(3000);
    Iterator pollIt = state.getPollHistories();
    assertFalse(pollIt.hasNext());
    state.pollHistories = new ArrayList(3);

    long cutOff = 3000 - MAX_AGE;
    // fill the list (in sorted order)
    state.pollHistories.add(new PollHistory(1, "test1", "test1", 0, cutOff + 1,
        0, null, false));
    state.pollHistories.add(new PollHistory(1, "test1", "test1", 0, cutOff,
        0, null, false));
    state.pollHistories.add(new PollHistory(1, "test1", "test1", 0, cutOff - 1,
        0, null, false));
    trimHistoriesIfNeeded(state);
    assertEquals(2, state.pollHistories.size());
    PollHistory history = (PollHistory)state.pollHistories.get(1);
    assertEquals(cutOff, history.getStartTime());
    assertFalse(historyRepo.storeCalled);

    TimeBase.setReal();
  }

  public void testPollTrimmingRewrite() throws Exception {
    TimeBase.setSimulated(1234);
    ConfigurationUtil.addFromArgs(NodeStateImpl.PARAM_POLL_HISTORY_TRIM_REWRITE,
				  "true");
    Iterator pollIt = state.getPollHistories();
    assertFalse(pollIt.hasNext());
    state.pollHistories = new ArrayList(MAX_COUNT);

    List lst = new ArrayList();
    for (int ii=0; ii<MAX_COUNT + 3; ii++) {
      // fill the list
      lst.add(new PollHistory(1, "test1", "test1", 0, 123 + ii,
			      0, null, false));
    }
    state.setPollHistoryList(lst);
    assertEquals(MAX_COUNT, state.pollHistories.size());
    PollHistory history = (PollHistory)state.pollHistories.get(MAX_COUNT - 1);
    assertEquals(126, history.getStartTime());
    assertTrue(historyRepo.storeCalled);
  }

  void trimHistoriesIfNeeded(NodeState state) {
    ((NodeStateImpl)state).sortPollHistories();
    ((NodeStateImpl)state).trimHistoriesIfNeeded();
  }

  static class MyHistoryRepositoryImpl extends HistoryRepositoryImpl {
    boolean storeCalled = false;

    MyHistoryRepositoryImpl(ArchivalUnit au, String path) {
      super(au, path);
    }
    public void storePollHistories(NodeState nodeState) {
      storeCalled = true;
      super.storePollHistories(nodeState);
    }
  }    

  public static void main(String[] argv) {
    String[] testCaseList = {TestNodeStateImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
