/*
 * $Id: TestBlockTally.java,v 1.3 2005-10-11 05:50:29 tlipkis Exp $
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

//  public void setUp() throws Exception {
//    super.setUp();
//    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
//    theDaemon = getMockLockssDaemon();
//    Properties p = new Properties();
//    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
//    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
//    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
//    p.setProperty(BlockTally.PARAM_VOTE_MARGIN, "73");
//    p.setProperty(BlockTally.PARAM_QUORUM, "5");
//    p.setProperty(BlockTally.PARAM_TRUSTED_WEIGHT, "300");
//    ConfigurationUtil.setCurrentConfigFromProps(p);
//    idmgr = theDaemon.getIdentityManager();
//    idmgr.startService();
//  }
//
//  public void tearDown() throws Exception {
//    idmgr.stopService();
//    super.tearDown();
//  }
//
//  public void testConstructPollTally() throws Exception {
//    BlockTally tally = new BlockTally();
//    assertTrue(0.73 == tally.m_voteMargin);
//    assertTrue(300.0 == tally.m_trustedWeight);
//    assertEquals(5, tally.m_quorum);
//    assertEquals(BlockTally.RESULT_POLLING, tally.getResult());
//  }
//
//  public void testWonPoll() throws Exception {
//    BlockTally tally = new BlockTally();
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.1"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.2"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.3"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.4"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.5"));
//    tally.tallyVotes();
//    assertEquals(BlockTally.RESULT_WON, tally.getResult());
//  }
//
//  public void testLostPoll() throws Exception {
//    BlockTally tally = new BlockTally();
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.1"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.2"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.3"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.4"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.5"));
//    tally.tallyVotes();
//    assertEquals(BlockTally.RESULT_LOST, tally.getResult());
//  }
//
//  public void testResultTooCloseUnder() throws Exception {
//    BlockTally tally = new BlockTally();
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.1"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.2"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.3"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.4"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.5"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.6"));
//    tally.tallyVotes();
//    assertEquals(BlockTally.RESULT_TOO_CLOSE, tally.getResult());
//  }
//
//  public void testResultTooCloseOver() throws Exception {
//    BlockTally tally = new BlockTally();
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.1"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.2"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.3"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.4"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.5"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.6"));
//    tally.tallyVotes();
//    assertEquals(BlockTally.RESULT_TOO_CLOSE, tally.getResult());
//  }
//
//  public void testResultTooCloseEqual() throws Exception {
//    BlockTally tally = new BlockTally();
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.1"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.2"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.3"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.4"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.5"));
//    tally.addDisagreeVote(idmgr.stringToPeerIdentity("192.168.0.6"));
//    tally.tallyVotes();
//    assertEquals(BlockTally.RESULT_TOO_CLOSE, tally.getResult());
//  }
//
//  public void testNoQuorum() throws Exception {
//    BlockTally tally = new BlockTally();
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.1"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.2"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.3"));
//    tally.addAgreeVote(idmgr.stringToPeerIdentity("192.168.0.4"));
//    tally.tallyVotes();
//    assertEquals(BlockTally.RESULT_NOQUORUM, tally.getResult());
//  }

  public void testFoo() throws Exception {
    //
  }

  // XXX: Tests for reputation system.
}
