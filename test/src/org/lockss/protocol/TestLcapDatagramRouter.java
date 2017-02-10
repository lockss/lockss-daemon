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

package org.lockss.protocol;

import java.net.*;
import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.poller.*;

/** JUnitTest case for class: org.lockss.protocol.Message */
public class TestLcapDatagramRouter extends LockssTestCase {
  static Logger log = Logger.getLogger("TestLcapDatagramRouter");

  private LcapDatagramRouter rtr;

  private final static String urlstr = "http://www.example.com";
  private final static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private final static String lwrbnd = "test1.doc";
  private final static String uprbnd = "test3.doc";
  private final static String archivalID = "TestAU_1.0";

  private MockLockssDaemon daemon;
  private IdentityManager idmgr;
  protected IPAddr testaddr;
  protected PeerIdentity testID;
  protected V1LcapMessage testmsg;
  LockssDatagram dg;
  LockssReceivedDatagram rdg;
  private ArrayList testentries;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    setConfig();
    idmgr = daemon.getIdentityManager();
    // this causes error messages, but need to start comm so it gets idmgr.
    daemon.getDatagramCommManager().startService();
    rtr = daemon.getDatagramRouterManager();
    rtr.startService();
    TimeBase.setSimulated(20000);
  }

  public void tearDown() throws Exception {
    LcapDatagramComm comm = daemon.getDatagramCommManager();
    if (comm != null) {
      comm.stopService();
    }
    super.tearDown();
  }

  public void testIsEligibleToForward() throws Exception {
    //     LcapMessage msg = createTestMsg("127.0.0.1", 3);
    createTestMsg("1.2.3.4", 3);
    TimeBase.step(100000);
    assertTrue(rtr.isEligibleToForward(rdg, testmsg));
    createTestMsg("1.2.3.4", 0);
    TimeBase.step(100000);
    assertFalse(rtr.isEligibleToForward(rdg, testmsg));
    createTestMsg("127.0.0.1", 3);
    TimeBase.step(100000);
    assertFalse(rtr.isEligibleToForward(rdg, testmsg));
  }

  void setConfig() {
    Properties p = new Properties();
    p.put(LcapDatagramRouter.PARAM_FWD_MSG_RATE, "100/1");
    p.put(LcapDatagramRouter.PARAM_BEACON_INTERVAL, "1m");
    p.put(LcapDatagramRouter.PARAM_INITIAL_HOPCOUNT, "3");
    p.put(LcapDatagramRouter.PARAM_PROB_PARTNER_ADD, "100");
    p.put(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  V1LcapMessage createTestMsg(String originator, int hopCount)
      throws IOException {
    try {
      testaddr = IPAddr.getByName(originator);
      testID =
	idmgr.stringToPeerIdentity(originator);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = new V1LcapMessage();
    }
    catch (IOException ex) {
      fail("can't create test message");
    }
    // assign the data
    testmsg.m_targetUrl = urlstr;
    testmsg.m_lwrBound = lwrbnd;
    testmsg.m_uprBound = uprbnd;

    testmsg.m_originatorID = testID;
    testmsg.m_hashAlgorithm = LcapMessage.getDefaultHashAlgorithm();
    testmsg.m_startTime = 123456789;
    testmsg.m_stopTime = 987654321;
    testmsg.m_multicast = false;
    testmsg.m_hopCount = (byte)hopCount;

    // testmsg.m_ttl = 5;
    testmsg.m_challenge = testbytes;
    testmsg.m_verifier = testbytes;
    testmsg.m_hashed = testbytes;
    testmsg.m_opcode = V1LcapMessage.CONTENT_POLL_REQ;
    testmsg.m_entries = testentries = TestPoll.makeEntries(1, 25);
    testmsg.m_archivalID = archivalID;
    dg = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP, testmsg.encodeMsg());
    rdg = new LockssReceivedDatagram(dg.makeSendPacket(testaddr, 0));
    rdg.setMulticast(true);
    return testmsg;
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestLcapDatagramRouter.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}

