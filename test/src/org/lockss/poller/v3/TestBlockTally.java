/*
 * $Id: TestBlockTally.java,v 1.9 2012-03-09 20:51:45 barry409 Exp $
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
  private IdentityManager idmgr;
  private LockssDaemon theDaemon;
  private PeerIdentity[] testPeers;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    theDaemon = getMockLockssDaemon();
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(V3Poller.PARAM_V3_VOTE_MARGIN, "73");
    p.setProperty(V3Poller.PARAM_V3_TRUSTED_WEIGHT, "300");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    setupPeers();
  }
  
  private void setupPeers() throws Exception {
    testPeers = new PeerIdentity[10];
    testPeers[0] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9900");
    testPeers[1] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9901");
    testPeers[2] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9902");
    testPeers[3] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9903");
    testPeers[4] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9904");
    testPeers[5] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9905");
    testPeers[6] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9906");
    testPeers[7] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9907");
    testPeers[8] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9908");
    testPeers[9] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9909");
  }

  public void tearDown() throws Exception {
    idmgr.stopService();
    super.tearDown();
  }

  public void testConstructPollTally() throws Exception {
    BlockTally tally = new BlockTally();
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult(5, 75));
  }
  
  public void testIsWithinMargin() throws Exception {
    BlockTally tally = null;
    
    tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));
    
    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));
    
    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));
    
    
    tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(75));

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(75));
    
    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertFalse(tally.isWithinMargin(75));
    
    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(75));

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(75));
    

    tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(80));

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertFalse(tally.isWithinMargin(80));
    
    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertFalse(tally.isWithinMargin(80));
    
    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertFalse(tally.isWithinMargin(80));

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(80));
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
    tally.addPollerOnlyBlockVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    tally = new BlockTally();
    tally.addPollerOnlyBlockVoter(testPeers[0]);
    tally.addPollerOnlyBlockVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    // Note: a landslide of voters say it doesn't exist, yet a repair
    // will be requested from a random voter. This is wrong.
    tally = new BlockTally();
    tally.addPollerOnlyBlockVoter(testPeers[0]);
    tally.addPollerOnlyBlockVoter(testPeers[1]);
    tally.addPollerOnlyBlockVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    tally = new BlockTally();
    tally.addPollerOnlyBlockVoter(testPeers[0]);
    tally.addPollerOnlyBlockVoter(testPeers[1]);
    tally.addPollerOnlyBlockVoter(testPeers[2]);
    tally.addPollerOnlyBlockVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult(4, 75));

    // The LOST_POLLER_ONLY_BLOCK result is returned when the number
    // of "poller only" voters is greater than the quorum, even if
    // there are a lot of disagree voters. This is wrong.
    tally = new BlockTally();
    tally.addPollerOnlyBlockVoter(testPeers[0]);
    tally.addPollerOnlyBlockVoter(testPeers[1]);
    tally.addPollerOnlyBlockVoter(testPeers[2]);
    tally.addPollerOnlyBlockVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    tally.addDisagreeVoter(testPeers[6]);
    tally.addDisagreeVoter(testPeers[7]);
    tally.addDisagreeVoter(testPeers[8]);
    tally.addDisagreeVoter(testPeers[9]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult(4, 75));

    tally = new BlockTally();
    tally.addPollerOnlyBlockVoter(testPeers[0]);
    tally.addPollerOnlyBlockVoter(testPeers[1]);
    tally.addPollerOnlyBlockVoter(testPeers[2]);
    tally.addPollerOnlyBlockVoter(testPeers[3]);
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
    assertEquals(1, tally.getAgreeVoters().size());
    assertEquals(0, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());

    tally = new BlockTally();
    tally.addDisagreeVoter(testPeers[0]);
    assertEquals(0, tally.getAgreeVoters().size());
    assertEquals(1, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());

    tally = new BlockTally();
    tally.addPollerOnlyBlockVoter(testPeers[0]);
    assertEquals(0, tally.getAgreeVoters().size());
    assertEquals(1, tally.getDisagreeVoters().size());
    assertEquals(1, tally.getPollerOnlyBlockVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());

    tally = new BlockTally();
    tally.addVoterOnlyBlockVoter(testPeers[0]);
    assertEquals(0, tally.getAgreeVoters().size());
    assertEquals(1, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());
    assertEquals(1, tally.getVoterOnlyBlockVoters().size());
  }

  public void testTalliedVoters() {
    BlockTally tally;
    Collection<PeerIdentity> talliedVoters;

    tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    talliedVoters = tally.getTalliedVoters();
    assertEquals(2, talliedVoters.size());
    assertContains(talliedVoters, testPeers[0]);
    assertContains(talliedVoters, testPeers[1]);
  }

  public void testVersionAgreedVoters() {
    BlockTally tally;
    Collection<PeerIdentity> versionAgreedVoters;

    tally = new BlockTally();
    tally.addAgreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    versionAgreedVoters = tally.getVersionAgreedVoters();
    assertEquals(0, versionAgreedVoters.size());

    tally = new BlockTally(new BlockTally.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  fail("Should not be called.");
	  return true;
	}
      });
    tally.addAgreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    versionAgreedVoters = tally.getVersionAgreedVoters();
    assertEquals(1, versionAgreedVoters.size());
    assertContains(versionAgreedVoters, testPeers[0]);
  }

  public void testVoteWithBlock() {
    BlockTally tally;
    BlockTally.HashBlockComparer comparer = new BlockTally.HashBlockComparer() {
	public boolean compare(VoteBlock voteBlock, int participantIndex) {
	  return participantIndex == 0;
	}
      };

    tally = new BlockTally(comparer);
    tally.voteSpoiled(testPeers[0]);
    assertEquals(0, tally.getAgreeVoters().size());
    assertEquals(0, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());

    tally = new BlockTally(comparer);
    tally.voteMissing(testPeers[0]);
    assertEquals(0, tally.getAgreeVoters().size());
    assertEquals(1, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());
    assertEquals(1, tally.getPollerOnlyBlockVoters().size());

    tally = new BlockTally(comparer);
    tally.vote(null, testPeers[0], 0);
    assertEquals(1, tally.getAgreeVoters().size());
    assertEquals(0, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());

    tally = new BlockTally(comparer);
    tally.vote(null, testPeers[1], 1);
    assertEquals(0, tally.getAgreeVoters().size());
    assertEquals(1, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());

    tally = new BlockTally();
    tally.voteSpoiled(testPeers[0]);
    assertEquals(0, tally.getAgreeVoters().size());
    assertEquals(0, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());

    tally = new BlockTally();
    tally.voteMissing(testPeers[0]);
    assertEquals(1, tally.getAgreeVoters().size());
    assertEquals(0, tally.getDisagreeVoters().size());
    assertEquals(0, tally.getVoterOnlyBlockVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());

    tally = new BlockTally();
    tally.vote(null, testPeers[0], 0);
    assertEquals(0, tally.getAgreeVoters().size());
    assertEquals(1, tally.getDisagreeVoters().size());
    assertEquals(1, tally.getVoterOnlyBlockVoters().size());
    assertEquals(0, tally.getPollerOnlyBlockVoters().size());
  }

  // XXX: Tests for reputation system.
}
