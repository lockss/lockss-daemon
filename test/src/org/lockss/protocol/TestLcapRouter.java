/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.PluginTestUtil;
import org.lockss.poller.Poll;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.IDUtil;
import org.lockss.util.Logger;
import org.lockss.util.TimeBase;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * This is the test class for org.lockss.protocol.LcapRouter
 */
public class TestLcapRouter extends LockssTestCase {
  public static Class testedClasses[] = {
    LcapRouter.class,
  };

  static Logger log = Logger.getLogger("TestRouter");

  private MockLockssDaemon daemon;
  MyLcapRouter rtr;
  MyBlockingStreamComm scomm;

  PeerIdentity myPeerId;
  PeerIdentity pid1;
  PeerIdentity pid2;
  File tempDir;

  public void setUp() throws Exception {
    super.setUp();
    tempDir = getTempDir();
    ConfigurationUtil.setFromArgs(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.addFromArgs(IdentityManager.PARAM_LOCAL_V3_IDENTITY,
        "TCP:[127.0.0.1]:1234");
    daemon = getMockLockssDaemon();
    // V3LcapMessage.decode needs idmgr
    daemon.getIdentityManager().startService();
    scomm = spy(new MyBlockingStreamComm());
    scomm.initService(daemon);
    daemon.setStreamCommManager(scomm);
    rtr = new MyLcapRouter();
    rtr.initService(daemon);
    daemon.setRouterManager(rtr);
    rtr.startService();
    myPeerId = daemon.getIdentityManager()
        .getLocalPeerIdentity(Poll.V3_PROTOCOL);
    pid1 = newPI(IDUtil.ipAddrToKey("129.3.3.3", "4321"));
    pid2 = newPI(IDUtil.ipAddrToKey("129.3.3.33", "1234"));
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private PeerIdentity newPI(String key) throws Exception {
    return (PeerIdentity)PrivilegedAccessor.
      invokeConstructor(PeerIdentity.class.getName(), key);
  }


  public void testMakePeerMessage() throws Exception {
    V3LcapMessage lmsg =
      LcapMessageTestUtil.makeTestVoteMessage(pid1, pid2, tempDir, daemon);
    PeerMessage pmsg = rtr.makePeerMessage(lmsg);
    assertNull(pmsg.getSender());
    pmsg.setSender(pid1);
    V3LcapMessage msg2 = rtr.makeV3LcapMessage(pmsg);
    assertEqualMessages(lmsg, msg2);
  }

  public void testNoRateLimiter() throws Exception {
    TimeBase.setSimulated(1000);
    V3LcapMessage lmsg =
      LcapMessageTestUtil.makeTestVoteMessage(pid1, pid2, tempDir, daemon);

    rtr.sendTo(lmsg, pid1);
    assertEquals(1, scomm.sentMsgs.size());
    for (int ix = 0; ix < 100; ix++) {
      rtr.sendTo(lmsg, pid1);
    }
    assertEquals(101, scomm.sentMsgs.size());
  }

  public void testHandleOrForwardInboundMessage() throws Exception {
    // Forwarding disabled; dropped message
    {
      LcapRouter s_rtr = spy(rtr);
      V3LcapMessage lmsg =
          LcapMessageTestUtil.makeTestVoteMessage(pid1, pid2, tempDir, daemon);
      PeerMessage pmsg = s_rtr.makePeerMessage(lmsg);
      doReturn(lmsg).when(s_rtr).makeV3LcapMessage(pmsg);
      doNothing().when(s_rtr).handleLocalInboundMessage(pmsg, lmsg);
      s_rtr.handleOrForwardInboundPeerMessage(pmsg);
      verify(s_rtr, never()).handleLocalInboundMessage(pmsg, lmsg);
    }

    // Forwarding disabled; handle message locally
    {
      LcapRouter s_rtr = spy(rtr);
      V3LcapMessage lmsg = LcapMessageTestUtil
          .makeTestVoteMessage(pid1, myPeerId, tempDir, daemon);
      PeerMessage pmsg = s_rtr.makePeerMessage(lmsg);
      doReturn(lmsg).when(s_rtr).makeV3LcapMessage(pmsg);
      doNothing().when(s_rtr).handleLocalInboundMessage(pmsg, lmsg);
      s_rtr.handleOrForwardInboundPeerMessage(pmsg);
      verify(s_rtr, times(1))
          .handleLocalInboundMessage(pmsg, lmsg);
    }

    // Enable LCAP forwarding
    String forwardToV2 = "TCP:[127.0.0.1]:4321";
    ConfigurationUtil.addFromArgs(LcapRouter.PARAM_MIGRATE_TO,
        forwardToV2);

    // Forwarding enabled; forward-outbound message from V2
    {
      V3LcapMessage lmsg =
          LcapMessageTestUtil.makeTestVoteMessage(pid1, pid2, tempDir, daemon);
      PeerMessage pmsg = rtr.makePeerMessage(lmsg);
      reset(scomm);
      rtr.handleOrForwardInboundPeerMessage(pmsg);
      ArgumentCaptor<PeerIdentity> peerIdCaptor =
          ArgumentCaptor.forClass(PeerIdentity.class);
      verify(scomm, times(1))
          .sendTo(eq(pmsg), peerIdCaptor.capture());
      assertEquals(pid2.getKey(), peerIdCaptor.getValue().getKey());
    }

    // Forwarding enabled; NotStarted -> handle inbound message locally
    {
      LcapRouter s_rtr = spy(rtr);
      String auid = "testAuid";
      mockAuMigrationState(auid, AuState.MigrationState.NotStarted);
      V3LcapMessage lmsg = LcapMessageTestUtil
          .makeTestVoteMessage(pid1, myPeerId, tempDir, daemon);
      lmsg.setArchivalId(auid);
      PeerMessage pmsg = s_rtr.makePeerMessage(lmsg);
      doReturn(lmsg).when(s_rtr).makeV3LcapMessage(pmsg);
      doNothing().when(s_rtr).handleLocalInboundMessage(pmsg, lmsg);
      reset(scomm);
      s_rtr.handleOrForwardInboundPeerMessage(pmsg);
      verifyZeroInteractions(scomm);
      verify(s_rtr, times(1))
          .handleLocalInboundMessage(pmsg, lmsg);
    }

    // Forwarding enabled; InProgress -> forward-inbound message to V2
    {
      LcapRouter s_rtr = spy(rtr);
      String auid = "testAuid";
      mockAuMigrationState(auid, AuState.MigrationState.InProgress);
      V3LcapMessage lmsg = LcapMessageTestUtil
          .makeTestVoteMessage(pid1, myPeerId, tempDir, daemon);
      lmsg.setArchivalId(auid);
      PeerMessage pmsg = s_rtr.makePeerMessage(lmsg);
      reset(scomm);
      s_rtr.handleOrForwardInboundPeerMessage(pmsg);
      ArgumentCaptor<PeerIdentity> peerIdCaptor =
          ArgumentCaptor.forClass(PeerIdentity.class);
      verifyZeroInteractions(scomm);
      verify(s_rtr, times(0))
          .handleLocalInboundMessage(pmsg, lmsg);
    }

    // Forwarding enabled; Finished -> forward-inbound message to V2
    {
      String auid = "testAuid";
      mockAuMigrationState(auid, AuState.MigrationState.Finished);
      V3LcapMessage lmsg = LcapMessageTestUtil
          .makeTestVoteMessage(pid1, myPeerId, tempDir, daemon);
      lmsg.setArchivalId(auid);
      PeerMessage pmsg = rtr.makePeerMessage(lmsg);
      reset(scomm);
      rtr.handleOrForwardInboundPeerMessage(pmsg);
      ArgumentCaptor<PeerIdentity> peerIdCaptor =
          ArgumentCaptor.forClass(PeerIdentity.class);
      verify(scomm, times(1))
          .sendTo(eq(pmsg), peerIdCaptor.capture());
      assertEquals(forwardToV2, peerIdCaptor.getValue().getKey());
    }
  }

  private MockArchivalUnit mockAuMigrationState(
      String auid, AuState.MigrationState mState) {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId(auid);
    mau.setPlugin(new MockPlugin(daemon));
    PluginTestUtil.registerArchivalUnit(mau);
    MockAuState aus = new MockAuState();
    aus.setMigrationState(mState);
    MockNodeManager nodeManager = new MockNodeManager();
    getMockLockssDaemon().setNodeManager(nodeManager, mau);
    nodeManager.setAuState(aus);
    return mau;
  }

  private void assertEqualMessages(V3LcapMessage a, V3LcapMessage b)
      throws Exception {
    assertTrue(a.getOriginatorId() == b.getOriginatorId());
    assertEquals(a.getOpcode(), b.getOpcode());
    assertEquals(a.getTargetUrl(), b.getTargetUrl());
    assertEquals(a.getArchivalId(), b.getArchivalId());
    assertEquals(a.getProtocolVersion(), b.getProtocolVersion());
    assertEquals(a.getPollerNonce(), b.getPollerNonce());
    assertEquals(a.getVoterNonce(), b.getVoterNonce());
    assertEquals(a.getPluginVersion(), b.getPluginVersion());
    assertEquals(a.getHashAlgorithm(), b.getHashAlgorithm());
    List aVoteBlocks = new ArrayList();
    for (VoteBlocksIterator iter = a.getVoteBlockIterator(); iter.hasNext(); ) {
      aVoteBlocks.add(iter.next());
    }
    List bVoteBlocks = new ArrayList();
    for (VoteBlocksIterator iter = b.getVoteBlockIterator(); iter.hasNext(); ) {
      bVoteBlocks.add(iter.next());
    }
    assertTrue(aVoteBlocks.equals(bVoteBlocks));

    // TODO: Figure out how to test time.

  }

  static class MyLcapRouter extends LcapRouter {
    @Override
    PeerMessage newPeerMessage(long estSize) {
      return new MemoryPeerMessage();
    }
  }
  static class MyBlockingStreamComm extends BlockingStreamComm {

    List <PeerMessage>sentMsgs = new ArrayList <PeerMessage>();

    @Override
    protected boolean isRunning() {
      return true;
    }

    @Override
      protected void sendToChannel(PeerMessage msg, PeerIdentity id)
	throws IOException {
      sentMsgs.add(msg);
    }


  }
}
