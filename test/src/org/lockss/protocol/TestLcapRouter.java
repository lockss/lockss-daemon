/*
 * $Id: TestLcapRouter.java,v 1.10.10.1 2004-02-03 01:03:39 tlipkis Exp $
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

import java.io.DataInputStream;
import java.net.*;
import java.io.*;
import java.util.*;
import gnu.regexp.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.poller.*;

/** JUnitTest case for class: org.lockss.protocol.Message */
public class TestLcapRouter extends LockssTestCase {
  static Logger log = Logger.getLogger("TestLcapRouter");

  private LcapRouter rtr;

  private static String urlstr = "http://www.example.com";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static ArrayList testentries;

  private static MockLockssDaemon daemon = new MockLockssDaemon(null);
  private IdentityManager idmgr;
  protected IPAddr testaddr;
  protected LcapIdentity testID;
  protected LcapMessage testmsg;
  protected static String archivalID = "TestAU_1.0";
  LockssDatagram dg;
  LockssReceivedDatagram rdg;

  public void setUp() throws Exception {
    super.setUp();
    setConfig();
    idmgr = daemon.getIdentityManager();
    // this causes error messages, but need to start comm so it gets idmgr.
    daemon.getCommManager().startService();
    rtr = daemon.getRouterManager();
    rtr.startService();
    TimeBase.setSimulated(20000);
  }

  public void tearDown() throws Exception {
    LcapComm comm = daemon.getCommManager();
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
    p.put(LcapRouter.PARAM_FWD_PKTS_PER_INTERVAL, "100");
    p.put(LcapRouter.PARAM_FWD_PKT_INTERVAL, "1");
    p.put(LcapRouter.PARAM_BEACON_INTERVAL, "1m");
    p.put(LcapRouter.PARAM_INITIAL_HOPCOUNT, "3");
    p.put(LcapRouter.PARAM_PROB_PARTNER_ADD, "100");
    p.put(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  LcapMessage createTestMsg(String originator, int hopCount)
      throws IOException {
    try {
      testaddr = IPAddr.getByName(originator);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
    try {
      testmsg = new LcapMessage();
    }
    catch (IOException ex) {
      fail("can't create test message");
    }
    // assign the data
    testmsg.m_targetUrl = urlstr;
    testmsg.m_lwrBound = lwrbnd;
    testmsg.m_uprBound = uprbnd;
    testID = new LcapIdentity(testaddr);

    testmsg.m_originAddr = testaddr;
    testmsg.m_hashAlgorithm = LcapMessage.getDefaultHashAlgorithm();
    testmsg.m_startTime = 123456789;
    testmsg.m_stopTime = 987654321;
    testmsg.m_multicast = false;
    testmsg.m_hopCount = (byte)hopCount;

    testmsg.m_ttl = 5;
    testmsg.m_challenge = testbytes;
    testmsg.m_verifier = testbytes;
    testmsg.m_hashed = testbytes;
    testmsg.m_opcode = LcapMessage.CONTENT_POLL_REQ;
    testmsg.m_entries = testentries = TestPoll.makeEntries(1, 25);
    testmsg.m_archivalID = archivalID;
    dg = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP, testmsg.encodeMsg());
    rdg = new LockssReceivedDatagram(dg.makeSendPacket(testaddr, 0));
    rdg.setMulticast(true);
    return testmsg;
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestLcapRouter.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}

