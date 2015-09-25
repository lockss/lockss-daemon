/*
 * $Id$
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

public class TestPollHistoryImpl extends LockssTestCase {
  private PollHistory history;

  public void setUp() throws Exception {
    super.setUp();
    PollState state = new PollState(1, "none", null, 1, 0, null, false);
    Collection votes = new ArrayList();
    votes.add(new String("test"));
    history = new PollHistory(state, 0, votes);
  }

  public void testVotesImmutability() {
    Iterator voteIter = history.getVotes();
    try {
      voteIter.remove();
      fail("Iterator should be immutable.");
    } catch (Exception e) { }
  }

  public void testCompareTo() {
    PollState state = new PollState(1, "none", null, 1, 0, null, false);
    assertEquals(0, history.compareTo(state));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestPollHistoryImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
