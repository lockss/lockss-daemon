/*
 * $Id: TestVoteBlockTallier.java,v 1.4 2012-06-25 23:30:10 barry409 Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.protocol.VoteBlock;
import org.lockss.test.*;


public class TestVoteBlockTallier extends LockssTestCase {
  private ParticipantUserData[] testPeers;

  public void setUp() throws Exception {
    super.setUp();
    setupPeers();
  }
  
  private void setupPeers() {
    testPeers = new ParticipantUserData[10];
    // todo(bhayes): None of the tests at present actually care about
    // the actual ParticipantUserData.
  }

  public void testConstructPollTally() {
    BlockTally tally = new BlockTally();
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult(5, 75));
  }

  public void testVoteWithBlockTallyPollerHas() {
    VoteBlockTallier voteBlockTallier;
    BlockTally tally;
    VoteBlockTallier.HashBlockComparer comparer =
      new VoteBlockTallier.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    voteBlockTallier = new VoteBlockTallier(comparer);
    tally = new BlockTally();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = new VoteBlockTallier(comparer);
    tally = new BlockTally();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    assertEquals("0/1/1/0", tally.votes());

    voteBlockTallier = new VoteBlockTallier(comparer);
    tally = new BlockTally();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, testPeers[0], 0);
    assertEquals("1/0/0/0", tally.votes());
  }

  public void testVoteWithBlockTallyPollerDoesntHave() {
    VoteBlockTallier voteBlockTallier;
    BlockTally tally;
    VoteBlockTallier.HashBlockComparer comparer =
      new VoteBlockTallier.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    voteBlockTallier = new VoteBlockTallier();
    tally = new BlockTally();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = new VoteBlockTallier();
    tally = new BlockTally();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    assertEquals("1/0/0/0", tally.votes());

    voteBlockTallier = new VoteBlockTallier();
    tally = new BlockTally();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, testPeers[0], 0);
    assertEquals("0/1/0/1", tally.votes());
  }

  public void testVoteWithParticipantUserData() {
    ParticipantUserData voter;
    VoteBlockTallier voteBlockTallier;
    VoteBlockTallier.VoteBlockTally tally;
    VoteBlockTallier.HashBlockComparer comparer =
      new VoteBlockTallier.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    voteBlockTallier = new VoteBlockTallier(comparer);
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, voter, 0);
    assertEquals("1/0/0/0/0/0", voter.getVoteCounts().votes());

    voteBlockTallier = new VoteBlockTallier(comparer);
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, voter, 1);
    assertEquals("0/1/0/0/0/0", voter.getVoteCounts().votes());

    voteBlockTallier = new VoteBlockTallier(comparer);
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(voter);
    assertEquals("0/0/1/0/0/0", voter.getVoteCounts().votes());

    voteBlockTallier = new VoteBlockTallier();
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, voter, 0);
    assertEquals("0/0/0/1/0/0", voter.getVoteCounts().votes());

    voteBlockTallier = new VoteBlockTallier(comparer);
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(voter);
    assertEquals("0/0/0/0/0/1", voter.getVoteCounts().votes());
  }
}
