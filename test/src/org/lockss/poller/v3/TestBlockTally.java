/*
 * $Id: TestBlockTally.java,v 1.16 2013-04-10 16:34:03 barry409 Exp $
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
    BlockTally tally = new BlockTally();
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult(5, 75));
  }
  
  public void testLandslideMinimum() throws Exception {
    BlockTally tally = null;
    
    tally = new BlockTally();
    assertEquals(0, tally.landslideMinimum(50));
    assertEquals(0, tally.landslideMinimum(75));
    assertEquals(0, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[0]);
    assertEquals(1, tally.landslideMinimum(50));
    assertEquals(1, tally.landslideMinimum(75));
    assertEquals(1, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[1]);
    assertEquals(1, tally.landslideMinimum(50));
    assertEquals(2, tally.landslideMinimum(75));
    assertEquals(2, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[2]);
    assertEquals(2, tally.landslideMinimum(50));
    assertEquals(3, tally.landslideMinimum(75));
    assertEquals(3, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[3]);
    assertEquals(2, tally.landslideMinimum(50));
    assertEquals(3, tally.landslideMinimum(75));
    assertEquals(4, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[4]);
    assertEquals(3, tally.landslideMinimum(50));
    assertEquals(4, tally.landslideMinimum(75));
    assertEquals(4, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[5]);
    assertEquals(3, tally.landslideMinimum(50));
    assertEquals(5, tally.landslideMinimum(75));
    assertEquals(5, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[6]);
    assertEquals(4, tally.landslideMinimum(50));
    assertEquals(6, tally.landslideMinimum(75));
    assertEquals(6, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[7]);
    assertEquals(4, tally.landslideMinimum(50));
    assertEquals(6, tally.landslideMinimum(75));
    assertEquals(7, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[8]);
    assertEquals(5, tally.landslideMinimum(50));
    assertEquals(7, tally.landslideMinimum(75));
    assertEquals(8, tally.landslideMinimum(80));
    tally.addAgreeVoter(testPeers[9]);
    assertEquals(5, tally.landslideMinimum(50));
    assertEquals(8, tally.landslideMinimum(75));
    assertEquals(8, tally.landslideMinimum(80));
  }

  public void testWonPoll() throws Exception {
    BlockTally tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    tally.addAgreeVoter(testPeers[4]);
    assertEquals(BlockTally.Result.WON, tally.getTallyResult(5, 75));
  }

  public void testLostPoll() throws Exception {
    BlockTally tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(5, 75));
  }

  public void testResultTooCloseUnder() throws Exception {
    BlockTally tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    tally.addAgreeVoter(testPeers[4]);
    tally.addAgreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult(5, 75));
  }

  public void testResultTooCloseOver() throws Exception {
    BlockTally tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult(5, 75));
  }

  public void testResultTooCloseEqual() throws Exception {
    BlockTally tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult(5, 75));
  }

  public void testNoQuorum() throws Exception {
    BlockTally tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult(5, 75));
  }

  public void testPollerOnly() throws Exception {
    // A combination of disagree, and poller only
    BlockTally tally;

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    // Note: the reparing peer will be drawn from all the voters,
    // including the one who doesn't have it. This is wrong.
    tally = new BlockTally();
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    tally = new BlockTally();
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    tally = new BlockTally();
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addPollerOnlyVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult(4, 75));

    tally = new BlockTally();
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addPollerOnlyVoter(testPeers[2]);
    tally.addPollerOnlyVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult(4, 75));

    // Test regression: BlockTally had been testing poller-only voters
    // against the quorum rather than the landslide. Test that it
    // behaves correctly now.
    tally = new BlockTally();
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
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    tally = new BlockTally();
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
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(5, 75));
  }

  public void testAddVoters() {
    BlockTally tally;
    tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    assertEquals("1/0/0/0", tally.votes());

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    assertEquals("0/1/0/0", tally.votes());

    tally = new BlockTally();
    tally.addPollerOnlyVoter(testPeers[0]);
    assertEquals("0/1/1/0", tally.votes());

    tally = new BlockTally();
    tally.addVoterOnlyVoter(testPeers[0]);
    assertEquals("0/1/0/1", tally.votes());
  }

  // XXX: Tests for reputation system.
}
