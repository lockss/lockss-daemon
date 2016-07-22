/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
    state = new NodeStateImpl(mcus, -1, polls, historyRepo);
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

    state.pollHistories = new ArrayList(3);
    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    assertFalse(pollIt.hasNext());

    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    pollIt.next();
    assertTrue(pollIt.hasNext());
    assertFalse(pollIt.hasNext());
  }

  public void testCloseActivePoll() {
    // avoid poll trimming
    TimeBase.setSimulated(1234);
    Iterator pollIt = state.getPollHistories();
    assertFalse(pollIt.hasNext());

    assertEquals(3, state.polls.size());
    assertEquals(2, state.polls.size());

    pollIt = state.getActivePolls();

    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    assertFalse(pollIt.hasNext());

    Iterator histIt = state.getPollHistories();
    ArrayList histL = new ArrayList(4);
    String[] expectedA = new String[] {
        "starttime=456",
        "starttime=345",
        "starttime=234",
        "starttime=123",
    };
    assertIsomorphic(expectedA, histL);

    TimeBase.setReal();
  }

  public void testCloseAdditionalStates() {
  }

  public void testGetLastPollHistory() {
    // avoid poll trimming
    TimeBase.setSimulated(1234);

    state.pollHistories = new ArrayList(3);

    TimeBase.setReal();
  }

  public void testGetActivePolls() {
    Iterator expectedIter = polls.iterator();

    Iterator pollIter = state.getActivePolls();
    assertTrue(CollectionUtil.isIsomorphic(expectedIter, pollIter));
  }

  public void testAddPollState() {
    Iterator expectedIter = polls.iterator();

    Iterator pollIter = state.getActivePolls();
    assertTrue(CollectionUtil.isIsomorphic(expectedIter, pollIter));
  }

  public void testIsInternalNode() {
    MockCachedUrlSet mcus = new MockCachedUrlSet(null, null);
    mcus.setIsLeaf(true);
    state = new NodeStateImpl(mcus, -1, null, null);
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

    trimHistoriesIfNeeded(state);
    assertEquals(MAX_COUNT, state.pollHistories.size());

    // exceed max
    trimHistoriesIfNeeded(state);
    // still max
    assertEquals(MAX_COUNT, state.pollHistories.size());

    ConfigurationUtil.addFromArgs(NodeStateImpl.PARAM_POLL_HISTORY_TRIM_TO,
				  Integer.toString(MAX_COUNT - 3));
    
    // trim now should do nothing
    trimHistoriesIfNeeded(state);
    assertEquals(MAX_COUNT, state.pollHistories.size());

    // exceed max again, should reduce to trimTo size
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
    trimHistoriesIfNeeded(state);
    assertEquals(2, state.pollHistories.size());
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
    state.setPollHistoryList(lst);
    assertEquals(MAX_COUNT, state.pollHistories.size());
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
