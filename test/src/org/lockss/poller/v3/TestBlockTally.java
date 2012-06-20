/*
 * $Id: TestBlockTally.java,v 1.5.72.1 2012-06-20 00:03:08 nchondros Exp $
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

  public void testConstructPollTally() throws Exception {
    BlockTally<String> tally = new BlockTally<String>();
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult(5, 75));
  }
  
  public void testIsWithinMargin() throws Exception {
    BlockTally<String> tally = null;
    
    tally = new BlockTally<String>();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));

    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));
    
    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));
    
    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));

    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(50));
    
    
    tally = new BlockTally<String>();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(75));

    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(75));
    
    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertFalse(tally.isWithinMargin(75));
    
    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(75));

    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(75));
    

    tally = new BlockTally<String>();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(80));

    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertFalse(tally.isWithinMargin(80));
    
    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertFalse(tally.isWithinMargin(80));
    
    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertFalse(tally.isWithinMargin(80));

    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertTrue(tally.isWithinMargin(80));
  }

  public void testWonPoll() throws Exception {
    BlockTally<String> tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    tally.addAgreeVoter(testPeers[4]);
    assertEquals(BlockTally.Result.WON, tally.getTallyResult(5, 75));
  }

  public void testLostPoll() throws Exception {
    BlockTally<String> tally = new BlockTally<String>();
    tally.addAgreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(5, 75));
  }

  public void testResultTooCloseUnder() throws Exception {
    BlockTally<String> tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    tally.addAgreeVoter(testPeers[4]);
    tally.addAgreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult(5, 75));
  }

  public void testResultTooCloseOver() throws Exception {
    BlockTally<String> tally = new BlockTally<String>();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult(5, 75));
  }

  public void testResultTooCloseEqual() throws Exception {
    BlockTally<String> tally = new BlockTally<String>();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    tally.addDisagreeVoter(testPeers[4]);
    tally.addDisagreeVoter(testPeers[5]);
    assertEquals(BlockTally.Result.TOO_CLOSE, tally.getTallyResult(5, 75));
  }

  public void testNoQuorum() throws Exception {
    BlockTally<String> tally = new BlockTally<String>();
    tally.addAgreeVoter(testPeers[0]);
    tally.addAgreeVoter(testPeers[1]);
    tally.addAgreeVoter(testPeers[2]);
    tally.addAgreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.NOQUORUM, tally.getTallyResult(5, 75));
  }

  public void testPollerOnly() throws Exception {
    // A combination of disagree, and poller only
    BlockTally<String> tally;

    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    // Note: the reparing peer will be drawn from all the voters,
    // including the one who doesn't have it. This is wrong.
    tally = new BlockTally<String>();
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addDisagreeVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    tally = new BlockTally<String>();
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addDisagreeVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    // Note: a landslide of voters say it doesn't exist, yet a repair
    // will be requested from a random voter. This is wrong.
    tally = new BlockTally<String>();
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addPollerOnlyVoter(testPeers[2]);
    tally.addDisagreeVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST, tally.getTallyResult(4, 75));

    tally = new BlockTally<String>();
    tally.addPollerOnlyVoter(testPeers[0]);
    tally.addPollerOnlyVoter(testPeers[1]);
    tally.addPollerOnlyVoter(testPeers[2]);
    tally.addPollerOnlyVoter(testPeers[3]);
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult(4, 75));

    // The LOST_POLLER_ONLY_BLOCK result is returned when the number
    // of "poller only" voters is greater than the quorum, even if
    // there are a lot of disagree voters. This is wrong.
    tally = new BlockTally<String>();
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
    assertEquals(BlockTally.Result.LOST_POLLER_ONLY_BLOCK,
		 tally.getTallyResult(4, 75));

    tally = new BlockTally<String>();
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
    BlockTally<String> tally;
    tally = new BlockTally<String>();
    tally.addAgreeVoter(testPeers[0]);
    assertEquals("1/0/0/0", tally.votes());

    tally = new BlockTally<String>();
    tally.addDisagreeVoter(testPeers[0]);
    assertEquals("0/1/0/0", tally.votes());

    tally = new BlockTally<String>();
    tally.addPollerOnlyVoter(testPeers[0]);
    assertEquals("0/1/1/0", tally.votes());

    tally = new BlockTally<String>();
    tally.addVoterOnlyVoter(testPeers[0]);
    assertEquals("0/1/0/1", tally.votes());
  }

  // XXX: Tests for reputation system.
}
