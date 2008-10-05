/*
 * $Id: TestPollState.java,v 1.3 2004-12-15 00:35:22 troberts Exp $
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

public class TestPollState extends LockssTestCase {

  public void setUp() throws Exception {
    super.setUp();
    PollState state = new PollState(1, "none", null, 1, 0, null, false);
  }

  public void testCompareToEqual() {
    PollState state = new PollState(1, null, null, 1, 0, null, false);
    PollState state2 = new PollState(1, null, null, 1, 0, null, false);
    assertEquals(0, state.compareTo(state2));
    assertEquals(0, state2.compareTo(state));

    state = new PollState(1, "blah", null, 1, 0, null, false);
    state2 = new PollState(1, "blah", null, 1, 0, null, false);
    assertEquals(0, state.compareTo(state2));
    assertEquals(0, state2.compareTo(state));

    state = new PollState(1, null, "blah", 1, 0, null, false);
    state2 = new PollState(1, null, "blah", 1, 0, null, false);
    assertEquals(0, state.compareTo(state2));
    assertEquals(0, state2.compareTo(state));
  }

  public void testCompareToDiffType() {
    PollState state = new PollState(1, "none", null, 1, 0, null, false);
    PollState state2 = new PollState(2, "none", null, 1, 0, null, false);
    assertCompareIsGreaterThan(state2, state);
  }

  public void testCompareToDiffLower() {
    PollState state = new PollState(1, "none", null, 1, 0, null, false);
    PollState state2 = new PollState(1, "none2", null, 1, 0, null, false);
    PollState state3 = new PollState(1, null, null, 1, 0, null, false);
    assertCompareIsGreaterThan(state2, state);

    assertCompareIsGreaterThan(state, state3);

    assertCompareIsGreaterThan(state2, state3);
  }

  public void testCompareToDiffUpper() {
    PollState state = new PollState(1, null, "none", 1, 0, null, false);
    PollState state2 = new PollState(1, null, "none2", 1, 0, null, false);
    PollState state3 = new PollState(1, null, null, 1, 0, null, false);
    assertCompareIsGreaterThan(state2, state);

    assertCompareIsGreaterThan(state, state3);

    assertCompareIsGreaterThan(state2, state3);
  }

  public void testHashCodeEquals() {
    PollState state = new PollState(1, null, null, 1, 0, null, false);
    PollState state2 = new PollState(1, null, null, 1, 0, null, false);
    assertEquals(state.hashCode(), state2.hashCode());
  }

  /**
   * While it is allowed for hashCode for different objects to be equal, these
   * tests are meant to catch simple errors which break the hash function by
   * making it insensitive to one of the unique vars
   */
  public void testHashCodeDifferentType() {
    PollState state = new PollState(1, "none", null, 1, 0, null, false);
    PollState state2 = new PollState(2, "none", null, 1, 0, null, false);
    assertNotEquals(state.hashCode(), state2.hashCode());
  }

  public void testHashCodeDifferentLwr() {
    PollState state = new PollState(1, "none", null, 1, 0, null, false);
    PollState state2 = new PollState(1, "none2", null, 1, 0, null, false);
    PollState state3 = new PollState(1, null, null, 1, 0, null, false);
    assertNotEquals(state.hashCode(), state2.hashCode());
    assertNotEquals(state.hashCode(), state3.hashCode());
    assertNotEquals(state2.hashCode(), state3.hashCode());
  }

  public void testHashCodeDifferentUpr() {
    PollState state = new PollState(1, null, "none", 1, 0, null, false);
    PollState state2 = new PollState(1, null, "none2", 1, 0, null, false);
    PollState state3 = new PollState(1, null, null, 1, 0, null, false);
    assertNotEquals(state.hashCode(), state2.hashCode());
    assertNotEquals(state.hashCode(), state3.hashCode());
    assertNotEquals(state2.hashCode(), state3.hashCode());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestPollState.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
