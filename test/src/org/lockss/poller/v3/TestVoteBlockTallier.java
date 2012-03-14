/*
 * $Id: TestVoteBlockTallier.java,v 1.1 2012-03-14 22:20:21 barry409 Exp $
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
  private String[] testPeers;

  public void setUp() throws Exception {
    super.setUp();
    setupPeers();
  }
  
  private void setupPeers() {
    testPeers = new String[10];
    testPeers[0] = "TCP:[192.168.0.1]:9900";
    testPeers[1] = "TCP:[192.168.0.1]:9901";
    testPeers[2] = "TCP:[192.168.0.1]:9902";
    testPeers[3] = "TCP:[192.168.0.1]:9903";
    testPeers[4] = "TCP:[192.168.0.1]:9904";
    testPeers[5] = "TCP:[192.168.0.1]:9905";
    testPeers[6] = "TCP:[192.168.0.1]:9906";
    testPeers[7] = "TCP:[192.168.0.1]:9907";
    testPeers[8] = "TCP:[192.168.0.1]:9908";
    testPeers[9] = "TCP:[192.168.0.1]:9909";
  }

  public void testConstructPollTally() {
    BlockTally<String> tally = new BlockTally<String>();
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult(5, 75));
  }

//  public void testVersionAgreedVoters() {
//    BlockTally<String> tally;
//    Collection<String> versionAgreedVoters;
//
//    tally = new BlockTally<String>();
//    tally.voteAgreed(testPeers[0]);
//    tally.voteDisagreed(testPeers[1]);
//    versionAgreedVoters = tally.getVersionAgreedVoters();
//    assertEquals(0, versionAgreedVoters.size());
//
//    tally = new BlockTally<String>(new BlockTally.HashBlockComparer() {
//	public boolean compare(VoteBlock voteBlock, int participantIndex) {
//	  fail("Should not be called.");
//	  return true;
//	}
//      });
//    tally.voteAgreed(testPeers[0]);
//    tally.voteDisagreed(testPeers[1]);
//    versionAgreedVoters = tally.getVersionAgreedVoters();
//    assertEquals(1, versionAgreedVoters.size());
//    assertContains(versionAgreedVoters, testPeers[0]);
//  }

  public void testVoteWithBlockTallyPollerHas() {
    VoteBlockTallier<String> voteBlockTallier;
    BlockTally<String> tally;
    VoteBlockTallier.HashBlockComparer comparer =
      new VoteBlockTallier.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    voteBlockTallier = new VoteBlockTallier<String>(comparer);
    tally = new BlockTally<String>();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = new VoteBlockTallier<String>(comparer);
    tally = new BlockTally<String>();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    assertEquals("0/1/1/0", tally.votes());

    voteBlockTallier = new VoteBlockTallier<String>(comparer);
    tally = new BlockTally<String>();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, testPeers[0], 0);
    assertEquals("1/0/0/0", tally.votes());
  }

  public void testVoteWithBlockTallyPollerDoesntHave() {
    VoteBlockTallier<String> voteBlockTallier;
    BlockTally<String> tally;
    VoteBlockTallier.HashBlockComparer comparer =
      new VoteBlockTallier.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    voteBlockTallier = new VoteBlockTallier<String>();
    tally = new BlockTally<String>();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(testPeers[0]);
    assertEquals("0/0/0/0", tally.votes());

    voteBlockTallier = new VoteBlockTallier<String>();
    tally = new BlockTally<String>();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(testPeers[0]);
    assertEquals("1/0/0/0", tally.votes());

    voteBlockTallier = new VoteBlockTallier<String>();
    tally = new BlockTally<String>();
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, testPeers[0], 0);
    assertEquals("0/1/0/1", tally.votes());
  }

  public void testVoteWithParticipantUserData() {
    ParticipantUserData voter;
    VoteBlockTallier<ParticipantUserData> voteBlockTallier;
    VoteBlockTallier.VoteBlockTally<ParticipantUserData> tally;
    VoteBlockTallier.HashBlockComparer comparer =
      new VoteBlockTallier.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    voteBlockTallier = new VoteBlockTallier<ParticipantUserData>(comparer);
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, voter, 0);
    assertEquals("1/0/0/0/0/0", voter.votes());

    voteBlockTallier = new VoteBlockTallier<ParticipantUserData>(comparer);
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, voter, 1);
    assertEquals("0/1/0/0/0/0", voter.votes());

    voteBlockTallier = new VoteBlockTallier<ParticipantUserData>(comparer);
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteMissing(voter);
    assertEquals("0/0/1/0/0/0", voter.votes());

    voteBlockTallier = new VoteBlockTallier<ParticipantUserData>();
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.vote(null, voter, 0);
    assertEquals("0/0/0/1/0/0", voter.votes());

    voteBlockTallier = new VoteBlockTallier<ParticipantUserData>(comparer);
    voter = new ParticipantUserData();
    tally = ParticipantUserData.voteTally;
    voteBlockTallier.addTally(tally);
    voteBlockTallier.voteSpoiled(voter);
    assertEquals("0/0/0/0/0/1", voter.votes());
  }
}
