/*
 * $Id: TestNodeStateImpl.java,v 1.3 2003-01-25 02:22:20 aalto Exp $
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

import java.util.*;
import org.lockss.daemon.CachedUrlSet;
import org.lockss.test.*;
import org.lockss.util.CollectionUtil;
import java.io.*;

public class TestNodeStateImpl extends LockssTestCase {
  private NodeStateImpl state;
  private List polls;

  public TestNodeStateImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    TestHistoryRepositoryImpl.configHistoryParams(tempDirPath);
    MockArchivalUnit mau = new MockArchivalUnit();
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    MockCachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);

    polls = new ArrayList(3);
    polls.add(new PollState(1, "none1", 1, 0, null));
    polls.add(new PollState(2, "none2", 1, 0, null));
    polls.add(new PollState(3, "none3", 1, 0, null));
    state = new NodeStateImpl(mcus, null, polls,
                              new HistoryRepositoryImpl(tempDirPath));
  }

  public void testActivePollImmutability() {
    Iterator pollIter = state.getActivePolls();
    try {
      pollIter.remove();
      fail("Iterator should be immutable.");
    } catch (Exception e) { }
  }

  public void testGetPollHistories() {
    Iterator pollIt = state.getPollHistories();
    assertTrue(!pollIt.hasNext());

    PollHistory history = new PollHistory(1, "test", 0, 0, 0, null);
    state.pollHistories = new ArrayList(1);
    state.pollHistories.add(history);
    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    history = (PollHistory)pollIt.next();
    assertEquals(1, history.type);
    assertEquals("test", history.regExp);
    assertTrue(!pollIt.hasNext());

    history = new PollHistory(2, "test2", 0, 0, 0, null);
    state.pollHistories.add(history);
    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    pollIt.next();
    assertTrue(pollIt.hasNext());
    history = (PollHistory)pollIt.next();
    assertEquals(2, history.type);
    assertEquals("test2", history.regExp);
    assertTrue(!pollIt.hasNext());
  }

  public void testCloseActivePoll() {
    Iterator pollIt = state.getPollHistories();
    assertTrue(!pollIt.hasNext());

    PollHistory history = new PollHistory(1, "none1", 0, 0, 0, null);
    state.closeActivePoll(history);

    pollIt = state.getActivePolls();
    while (pollIt.hasNext()) {
      PollState pollState = (PollState)pollIt.next();
      assertTrue(!pollState.equals(history));
    }

    pollIt = state.getPollHistories();
    assertTrue(pollIt.hasNext());
    history = (PollHistory)pollIt.next();
    assertEquals(1, history.type);
    assertEquals("none1", history.regExp);
    assertTrue(!pollIt.hasNext());
  }

  public void testGetActivePolls() {
    Iterator expectedIter = polls.iterator();

    Iterator pollIter = state.getActivePolls();
    assertTrue(CollectionUtil.isIsomorphic(expectedIter, pollIter));
  }

  public void testAddPollState() {
    PollState state4 = new PollState(4, "none4", 1, 0, null);
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
    state = new NodeStateImpl(mcus, null, null, null);
    assertTrue(!state.isInternalNode());

    childV.addElement("test string");
    mcus.setFlatIterator(childV.iterator());
    assertTrue(state.isInternalNode());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestNodeStateImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}