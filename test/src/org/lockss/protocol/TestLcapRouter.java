/*
 * $Id: TestLcapRouter.java,v 1.16 2005-05-18 05:41:06 tlipkis Exp $
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

import java.util.*;
import java.io.*;
import java.net.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.protocol.LcapRouter
 */
public class TestLcapRouter extends LockssTestCase {
  public static Class testedClasses[] = {
    LcapRouter.class,
  };

  static Logger log = Logger.getLogger("TestRouter");

  private MockLockssDaemon daemon;
  private IdentityManager idmgr;
  MyLcapRouter rtr;

  PeerIdentity pid1;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    ConfigurationUtil.setFromArgs(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    idmgr = new MyIdentityManager();
    daemon.setIdentityManager(idmgr);
    idmgr.initService(daemon);
    daemon.setDaemonInited(true);
    idmgr.startService();
    rtr = new MyLcapRouter();
    pid1 = idmgr.findPeerIdentity(IdentityManager.ipAddrToKey("129.3.3.3", 4321));
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private PeerIdentity newPI(String key) throws Exception {
    return (PeerIdentity)PrivilegedAccessor.
      invokeConstructor(PeerIdentity.class.getName(), key);
  }


  public void testMakePeerMessage() throws Exception {
    V3LcapMessage lmsg = LcapMessageTestUtil.makeTestVoteMessage(pid1);
    PeerMessage pmsg = rtr.makePeerMessage(lmsg);
    assertNull(pmsg.getSender());
    pmsg.setSender(pid1);
    V3LcapMessage msg2 = rtr.makeV3LcapMessage(pmsg);
    assertEqualMessages(lmsg, msg2);
  }

  private void assertEqualMessages(V3LcapMessage a, V3LcapMessage b) {
    assertTrue(a.getOriginatorId() == b.getOriginatorId());
    assertEquals(a.getOpcode(), b.getOpcode());
    assertEquals(a.getTargetUrl(), b.getTargetUrl());
    assertEquals(a.getArchivalId(), b.getArchivalId());
    assertEquals(a.getPollVersion(), b.getPollVersion());
    assertEquals(a.getChallenge(), b.getChallenge());
    assertEquals(a.getPluginVersion(), b.getPluginVersion());
    assertEquals(a.getHashAlgorithm(), b.getHashAlgorithm());
    List aVoteBlocks = ListUtil.fromIterator(a.getVoteBlockIterator());
    List bVoteBlocks = ListUtil.fromIterator(b.getVoteBlockIterator());
    assertTrue(aVoteBlocks.equals(bVoteBlocks));

    // TODO: Figure out how to test time.

  }

  static class MyLcapRouter extends LcapRouter {
    PeerMessage newPeerMessage() {
      return new MemoryPeerMessage();
    }
  }


  static class MyIdentityManager extends IdentityManager {
    public void storeIdentities() throws ProtocolException {
    }
  }

}
