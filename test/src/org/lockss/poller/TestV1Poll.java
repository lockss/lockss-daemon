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

package org.lockss.poller;

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.app.*;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.LockssRepositoryImpl;

/** JUnitTest case for class: org.lockss.poller.Poll */
public class TestV1Poll extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestV1Poll");

  protected MockArchivalUnit testau;
  private MockIdentityManager idmgr;
  private MockLockssDaemon theDaemon;
  protected PollManager pollmanager;

  String testurl = "http://test/";
  V1Poll poll;
  PeerIdentity peer1;
  PeerIdentity peer2;
  LcapIdentity id1;

  protected void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    theDaemon = getMockLockssDaemon();
    pollmanager = theDaemon.getPollManager();
    idmgr = new MockIdentityManager();
    idmgr.initService(theDaemon);
    theDaemon.setIdentityManager(idmgr);
    theDaemon.setDaemonInited(true);
    pollmanager.startService();
    testau = new MockArchivalUnit();
    try {
      peer1 = idmgr.stringToPeerIdentity("127.0.0.1");
      peer2 = idmgr.stringToPeerIdentity("1.1.1.1");
    }
    catch (IdentityManager.MalformedIdentityKeyException ex) {
      fail("can't create test PeerIdentity: " + ex.toString());
    }
  }

  public void tearDown() throws Exception {
    pollmanager.stopService();
    super.tearDown();
  }

  private void setVerifyParams(int agree, int disagree) {
    Properties p = new Properties();
    p.put("org.lockss.poll.agreeVerify", Integer.toString(agree));
    p.put("org.lockss.poll.disagreeVerify", Integer.toString(disagree));
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testShouldVerify() {
    Vote vote = new Vote(ByteArray.makeRandomBytes(20),
			 ByteArray.makeRandomBytes(20),
			 ByteArray.makeRandomBytes(20),
			 peer1, true /*boolean agree*/);
    // verify agreement 40$, disagreement 60%, weighted by reputation
    setVerifyParams(40, 60);
    V1ContentPoll poll = makeV1ContentPoll();

    // if his reputation is 1000, never verify agreement,
    // verify disagreement 60% of time
    idmgr.setMaxReputation(1000);
    idmgr.setReputation(peer1, 1000);
    assertEquals(0.0, poll.calcVerifyProb(vote, true), .01);
    assertEquals(.6, poll.calcVerifyProb(vote, false), .01);

    // if his reputation is 750, verify agreement .25*40%=10%,
    // verify disagreement .75*60%=45% of time
    idmgr.setReputation(peer1, 750);
    assertEquals(.1, poll.calcVerifyProb(vote, true), .01);
    assertEquals(.45, poll.calcVerifyProb(vote, false), .01);

    // if his reputation is 500, verify agreement .5*40%=20%,
    // verify disagreement .4*60%=30% of time
    idmgr.setReputation(peer1, 500);
    assertEquals(.2, poll.calcVerifyProb(vote, true), .01);
    assertEquals(.3, poll.calcVerifyProb(vote, false), .01);

    // if his reputation is 250, verify agreement .75*40%=30%,
    // verify disagreement .25*60%=15% of time
    idmgr.setReputation(peer1, 250);
    assertEquals(.3, poll.calcVerifyProb(vote, true), .01);
    assertEquals(.15, poll.calcVerifyProb(vote, false), .01);

    // verify agreement 10%, disagreement 10%, weighted by reputation
    setVerifyParams(10, 10);
    poll = makeV1ContentPoll();

    // if his reputation is 1000, never verify agreement,
    // verify disagreement 10% of time
    idmgr.setReputation(peer1, 1000);
    assertEquals(0.0, poll.calcVerifyProb(vote, true), .01);
    assertEquals(.1, poll.calcVerifyProb(vote, false), .01);

    // if his reputation is 250, verify agreement .75*10%=7.5%,
    // verify disagreement .25*10%=2.5% of time
    idmgr.setReputation(peer1, 250);
    assertEquals(.075, poll.calcVerifyProb(vote, true), .001);
    assertEquals(.025, poll.calcVerifyProb(vote, false), .001);
  }

  private V1ContentPoll makeV1ContentPoll() {
    PollSpec spec =
      new MockPollSpec(testau, testurl, null, null, Poll.V1_CONTENT_POLL);

    V1ContentPoll poll = new V1ContentPoll(spec,
					   pollmanager,
					   peer1,
					   ByteArray.makeRandomBytes(20),
					   1000,
					   null);
    return poll;
  }
}
