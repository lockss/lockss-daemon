/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import org.lockss.test.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import java.util.*;
import java.io.*;

public class TestBlockTally extends LockssTestCase {
  private ParticipantUserData[] testPeers;

  public void setUp() throws Exception {
    super.setUp();
    setupPeers();
  }
  
  private void setupPeers() {
    testPeers = new ParticipantUserData[10];
    // todo(bhayes): None of the tests at present actually care about
    // the actual ParticipantUserData. It would improve the situation
    // to make mocks [or use the no-arg constructor] and check the
    // get*Voters methods in BlockTally which return the actual
    // arrays.
  }

  public void testConstructPollTally() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult());
  }
  
  public void testLandslideMinimum() throws Exception {
    BlockTally tally = null;
    
    assertEquals(0, BlockTally.landslideMinimum(0, 50));
    assertEquals(0, BlockTally.landslideMinimum(0, 75));
    assertEquals(0, BlockTally.landslideMinimum(0, 80));

    assertEquals(1, BlockTally.landslideMinimum(1, 50));
    assertEquals(1, BlockTally.landslideMinimum(1, 75));
    assertEquals(1, BlockTally.landslideMinimum(1, 80));

    assertEquals(1, BlockTally.landslideMinimum(2, 50));
    assertEquals(2, BlockTally.landslideMinimum(2, 75));
    assertEquals(2, BlockTally.landslideMinimum(2, 80));

    assertEquals(2, BlockTally.landslideMinimum(3, 50));
    assertEquals(3, BlockTally.landslideMinimum(3, 75));
    assertEquals(3, BlockTally.landslideMinimum(3, 80));

    assertEquals(2, BlockTally.landslideMinimum(4, 50));
    assertEquals(3, BlockTally.landslideMinimum(4, 75));
    assertEquals(4, BlockTally.landslideMinimum(4, 80));

    assertEquals(3, BlockTally.landslideMinimum(5, 50));
    assertEquals(4, BlockTally.landslideMinimum(5, 75));
    assertEquals(4, BlockTally.landslideMinimum(5, 80));

    assertEquals(3, BlockTally.landslideMinimum(6, 50));
    assertEquals(5, BlockTally.landslideMinimum(6, 75));
    assertEquals(5, BlockTally.landslideMinimum(6, 80));

    assertEquals(4, BlockTally.landslideMinimum(7, 50));
    assertEquals(6, BlockTally.landslideMinimum(7, 75));
    assertEquals(6, BlockTally.landslideMinimum(7, 80));

    assertEquals(4, BlockTally.landslideMinimum(8, 50));
    assertEquals(6, BlockTally.landslideMinimum(8, 75));
    assertEquals(7, BlockTally.landslideMinimum(8, 80));

    assertEquals(5, BlockTally.landslideMinimum(9, 50));
    assertEquals(7, BlockTally.landslideMinimum(9, 75));
    assertEquals(8, BlockTally.landslideMinimum(9, 80));

    assertEquals(5, BlockTally.landslideMinimum(10, 50));
    assertEquals(8, BlockTally.landslideMinimum(10, 75));
    assertEquals(8, BlockTally.landslideMinimum(10, 80));
  }

  public void testWonPoll() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    tally.addAgreeVoter(testPeers[4]);
    assertEquals(BlockTally.Result.WON, tally.getTallyResult());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }
  }

  public void testLostPoll() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    tally.addAgreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());
    // NOTE: getRepairVoters contains nothing useful (since the
    // BlockTally's VersionCounts isn't being told about votes) but
    // does not throw. See TestVersionCounts for unit tests for
    // VersionCounts.
    tally.getRepairVoters();
  }

  public void testResultTooCloseUnder() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    tally.addAgreeVoter(testPeers[4]);
    tally.addAgreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult());
    assertFalse(tally.isVoterOnly());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }
  }

  public void testResultTooCloseOver() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult());
    assertFalse(tally.isVoterOnly());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }
  }

  public void testResultTooCloseEqual() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult());
    assertFalse(tally.isVoterOnly());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }
  }

  public void testVoterOnlyNoQuorum() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    tally.addVoterOnlyVoter(testPeers[1]);
    tally.addVoterOnlyVoter(testPeers[2]);
    tally.addVoterOnlyVoter(testPeers[3]);
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult());
    assertTrue(tally.isVoterOnly());
    tally.addDisagreeVoter(testPeers[3]);
    assertFalse(tally.isVoterOnly());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }
  }

  public void testNoQuorum() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult());
    try {
      tally.getRepairVoters();
      fail("expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException ex) {
      // Expected
    }
  }

  public void testVoterOnly() throws Exception {
    BlockTally tally = new BlockTally(5, 75);
    tally.addAgreeVoter(testPeers[0]);
    tally.addVoterOnlyVoter(testPeers[1]);
    tally.addVoterOnlyVoter(testPeers[2]);
    tally.addVoterOnlyVoter(testPeers[3]);
    tally.addVoterOnlyVoter(testPeers[4]);
    assertEquals(BlockTally.Result.LOST_VOTER_ONLY_BLOCK,
		 tally.getTallyResult());
    assertTrue(tally.isVoterOnly());
    // NOTE: getRepairVoters contains nothing useful (since the
    // BlockTally's VersionCounts isn't being told about votes) but
    // does not throw.
    tally.getRepairVoters();
  }

  public void testPollerOnly() throws Exception {
    // A combination of disagree, and poller only
    BlockTally tally;

    tally = new BlockTally(4, 75);
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());
    assertFalse(tally.isVoterOnly());

    // Note: the reparing peer will be drawn from all the voters,
    // including the one who doesn't have it. This is wrong.
    tally = new BlockTally(4, 75);
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());

    tally = new BlockTally(4, 75);
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());

    tally = new BlockTally(4, 75);
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addPollerOnlyVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult());

    tally = new BlockTally(4, 75);
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addPollerOnlyVoter(testPeers[2]);
    tally.addPollerOnlyVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult());

    // Test regression: BlockTally had been testing poller-only voters
    // against the quorum rather than the landslide. Test that it
    // behaves correctly now.
    tally = new BlockTally(4, 75);
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addPollerOnlyVoter(testPeers[2]);
    tally.addPollerOnlyVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    tally.addDisagreeVoter(testPeers[6]);
    tally.addDisagreeVoter(testPeers[7]);
    tally.addDisagreeVoter(testPeers[8]);
    tally.addDisagreeVoter(testPeers[9]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());

    tally = new BlockTally(5, 75);
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addPollerOnlyVoter(testPeers[2]);
    tally.addPollerOnlyVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    tally.addDisagreeVoter(testPeers[6]);
    tally.addDisagreeVoter(testPeers[7]);
    tally.addDisagreeVoter(testPeers[8]);
    tally.addDisagreeVoter(testPeers[9]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult());
  }

  public void testAddVoters() {
    BlockTally tally;
    tally = new BlockTally(-1, -1);
    tally.addAgreeVoter(testPeers[0]);
    assertEquals("1/0/0/0", tally.votes());

    tally = new BlockTally(-1, -1);
    tally.addDisagreeVoter(testPeers[0]);
    assertEquals("0/1/0/0", tally.votes());

    tally = new BlockTally(-1, -1);
    tally.addPollerOnlyVoter(testPeers[0]);
    assertEquals("0/1/1/0", tally.votes());

    tally = new BlockTally(-1, -1);
    tally.addVoterOnlyVoter(testPeers[0]);
    assertEquals("0/1/0/1", tally.votes());
  }

  // XXX: Tests for reputation system.
}
