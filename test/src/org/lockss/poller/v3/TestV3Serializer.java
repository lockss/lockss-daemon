/*
 * $Id$
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

import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;

/*
 * Test V3Serializer, V3PollerSerializer, and V3VoterSerializer.
 */

public class TestV3Serializer extends LockssTestCase {
  
  String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath();
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testClosePoll() throws Exception {
    // Ensure poll dir is deleted.
    V3PollerSerializer pollerSerializer = new V3PollerSerializer();
    File pollDir = (File)PrivilegedAccessor.getValue(pollerSerializer, 
                                                     "pollDir");
    assertTrue(pollDir.exists());
    pollerSerializer.closePoll();
    assertFalse(pollDir.exists());
    
    V3VoterSerializer voterSerializer = new V3VoterSerializer();
    pollDir = (File)PrivilegedAccessor.getValue(voterSerializer,
                                                "pollDir");
    assertTrue(pollDir.exists());
    voterSerializer.closePoll();
    assertFalse(pollDir.exists());
  }
  
  public void testSaveAndLoadPollerState() throws Exception {
    V3PollerSerializer pollerSerializer = new V3PollerSerializer();
    File pollDir = (File)PrivilegedAccessor.getValue(pollerSerializer,
                                                     "pollDir");
    PollerStateBean vsb1 = makePollerStateBean(pollerSerializer);
    pollerSerializer.savePollerState(vsb1);
    PollerStateBean vsb2 = pollerSerializer.loadPollerState();
    assertEqualPollerStateBeans(vsb1, vsb2);
    pollerSerializer = new V3PollerSerializer(pollDir.getName());
    PollerStateBean vsb3 = pollerSerializer.loadPollerState();
    assertEqualPollerStateBeans(vsb1, vsb3);
  }
  
  
  public void testSaveAndLoadPollerUserData() throws Exception {
    V3PollerSerializer pollerSerializer = new V3PollerSerializer();
    File pollDir = (File)PrivilegedAccessor.getValue(pollerSerializer,
                                                     "pollDir");
    PollerUserData ud1 = makePollerUserData(pollerSerializer);
    PeerIdentity id = ud1.getVoterId();
    pollerSerializer.savePollerUserData(ud1);
    PollerUserData ud2 = pollerSerializer.loadPollerUserData(id);
    assertEqualPollerUserData(ud1, ud2);
    pollerSerializer = new V3PollerSerializer(pollDir.getName());
    PollerUserData ud3 = pollerSerializer.loadPollerUserData(id);
    assertEqualPollerUserData(ud1, ud3);
    PeerIdentity id2 = new MockPeerIdentity("192.168.1.1:9999");
    try {
      pollerSerializer.loadPollerUserData(id2);
      fail("Should have thrown");
    } catch (V3Serializer.PollSerializerException ex) {
      // expected.
    }
  }
  
  public void testSaveAndLoadPollerInterpState() throws Exception {
    V3PollerSerializer pollerSerializer = new V3PollerSerializer();
    File pollDir  = (File)PrivilegedAccessor.getValue(pollerSerializer,
                                                      "pollDir");
    PsmInterpStateBean ub1 = new PsmInterpStateBean();
    ub1.setLastRestorableStateName("TestState");
    PeerIdentity id = new MockPeerIdentity("10.1.2.3:8000");
    pollerSerializer.savePollerInterpState(id, ub1);
    PsmInterpStateBean ub2 = pollerSerializer.loadPollerInterpState(id);
    assertEquals(ub1.getLastRestorableStateName(),
                 ub2.getLastRestorableStateName());
    pollerSerializer = new V3PollerSerializer(pollDir.getName());
    PsmInterpStateBean ub3 = pollerSerializer.loadPollerInterpState(id);
    assertEquals(ub1.getLastRestorableStateName(),
                 ub3.getLastRestorableStateName());
  }
  
  public void testSaveAndLoadVoterUserData() throws Exception {
    V3VoterSerializer voterSerializer = new V3VoterSerializer();
    File pollDir = (File)PrivilegedAccessor.getValue(voterSerializer,
                                                     "pollDir");
    VoterUserData ud1 = makeVoterUserData(voterSerializer);
    voterSerializer.saveVoterUserData(ud1);
    VoterUserData ud2 = voterSerializer.loadVoterUserData();
    assertEqualVoterUserData(ud1, ud2);
    voterSerializer = new V3VoterSerializer(pollDir.getName());
    VoterUserData ud3 = voterSerializer.loadVoterUserData();
    assertEqualVoterUserData(ud1, ud3);
  }
  
  public void testSaveAndLoadVoterInterpState() throws Exception {
    V3VoterSerializer voterSerializer = new V3VoterSerializer();
    File pollDir = (File)PrivilegedAccessor.getValue(voterSerializer,
                                                     "pollDir");
    PsmInterpStateBean ub1 = new PsmInterpStateBean();
    ub1.setLastRestorableStateName("TestState");
    voterSerializer.saveVoterInterpState(ub1);
    PsmInterpStateBean ub2 = voterSerializer.loadVoterInterpState();
    assertEquals(ub1.getLastRestorableStateName(),
                 ub2.getLastRestorableStateName());
    voterSerializer = new V3VoterSerializer(pollDir.getName());
    PsmInterpStateBean ub3 = voterSerializer.loadVoterInterpState();
    assertEquals(ub1.getLastRestorableStateName(),
                 ub3.getLastRestorableStateName());
  }
  
  public void testLoadInnerCircleStates() throws Exception {
    V3PollerSerializer pollerSerializer = new V3PollerSerializer();
    File pollDir = (File)PrivilegedAccessor.getValue(pollerSerializer,
                                                     "pollDir");
    ArrayList uds1 =
      (ArrayList)ListUtil.list(makePollerUserData(new MockPeerIdentity("10.1.1.1:8000"),
                                                  pollerSerializer),
                               makePollerUserData(new MockPeerIdentity("10.1.1.2:8000"),
                                                  pollerSerializer),
                               makePollerUserData(new MockPeerIdentity("10.1.1.3:8000"),
                                                  pollerSerializer),
                               makePollerUserData(new MockPeerIdentity("10.1.1.4:8000"),
                                                  pollerSerializer),
                               makePollerUserData(new MockPeerIdentity("10.1.1.5:8000"),
                                                  pollerSerializer));
    
    for (Iterator iter = uds1.iterator(); iter.hasNext(); ) {
      pollerSerializer.savePollerUserData((PollerUserData)iter.next());
    }
    List uds2 = pollerSerializer.loadInnerCircleStates();
    assertEqualInnerCircles(uds1, uds2);
    pollerSerializer = new V3PollerSerializer(pollDir.getName());
    List uds3 = pollerSerializer.loadInnerCircleStates();
    assertEqualInnerCircles(uds1, uds3);
  }
  
  
  private PollerStateBean makePollerStateBean(V3PollerSerializer serializer) {
    PollerStateBean vsb = new PollerStateBean(serializer);
    vsb.setAuId("testAu");
    vsb.setCachedUrlSet(new MockCachedUrlSet());
    vsb.setDeadline(10000);
    vsb.setHashAlgorithm("SHA1");
    vsb.setLastHashedBlock("http://www.example.com/file1.html");
    vsb.setPluginVersion("mock");
    vsb.setPollerId(new MockPeerIdentity("127.0.0.1:8080"));
    vsb.setPollKey("mock-poll-1");
    vsb.setPollVersion(1);
    return vsb;
  }
  
  private PollerUserData makePollerUserData(V3PollerSerializer serializer) {
    return makePollerUserData(new MockPeerIdentity("127.0.0.1:8080"),
                              serializer);
  }
  
  private PollerUserData makePollerUserData(PeerIdentity voterId,
                                            V3PollerSerializer serializer) {
    PollerUserData ud = new PollerUserData(serializer);
    ud.setVoterId(voterId);
    ud.setHashAlgorithm("SHA1");
    ud.setRepairTarget("http://www.example.com/file3.html");
    ud.setIntroEffortProof(ByteArray.makeRandomBytes(20));
    ud.setNominees(ListUtil.list("10.0.0.1:8999", "10.0.0.2:8999"));
    ud.setPollAckEffortProof(ByteArray.makeRandomBytes(20));
    ud.setPollerNonce(ByteArray.makeRandomBytes(20));
    ud.setReceiptEffortProof(ByteArray.makeRandomBytes(20));
    ud.setRemainingEffortProof(ByteArray.makeRandomBytes(20));
    ud.setRepairEffortProof(ByteArray.makeRandomBytes(20));
    VoteBlocks blocks = new MemoryVoteBlocks();
    blocks.addVoteBlock(new VoteBlock("http://www.example.com/file1.html",
                                      1000L, 0L, 1000L, 0L,
                                      ByteArray.makeRandomBytes(20),
                                      VoteBlock.CONTENT_VOTE));
    blocks.addVoteBlock(new VoteBlock("http://www.example.com/file2.html",
                                      1010L, 0L, 1010L, 0L,
                                      ByteArray.makeRandomBytes(20),
                                      VoteBlock.CONTENT_VOTE));
    ud.setVoteBlocks(blocks);
    ud.setVoterNonce(ByteArray.makeRandomBytes(20));
    return ud;
  }
  
  private VoterUserData makeVoterUserData(V3VoterSerializer serializer) {
    VoterUserData ud = new VoterUserData(serializer);
    ud.setAuId("mockAu");
    ud.setDeadline(10000L);
    ud.setHashAlgorithm("SHA1");
    ud.setIntroEffortProof(ByteArray.makeRandomBytes(20));
    ud.setNominees(ListUtil.list("10.1.0.1:8000", "10.1.0.2:8000"));
    ud.setPluginVersion("1.0");
    ud.setPollAckEffortProof(ByteArray.makeRandomBytes(20));
    ud.setPollerId(new MockPeerIdentity("10.2.0.1:9000"));
    ud.setPollerNonce(ByteArray.makeRandomBytes(20));
    ud.setPollKey("pollkey");
    ud.setPollVersion(1);
    ud.setReceiptEffortProof(ByteArray.makeRandomBytes(20));
    ud.setRemainingEffortProof(ByteArray.makeRandomBytes(20));
    ud.setRepairEffortProof(ByteArray.makeRandomBytes(20));
    ud.setUrl("http://www.example.com/file1.html");
    VoteBlocks blocks = new MemoryVoteBlocks();
    blocks.addVoteBlock(new VoteBlock("http://www.example.com/file1.html",
                                      1000L, 0L, 1000L, 0L,
                                      ByteArray.makeRandomBytes(20),
                                      VoteBlock.CONTENT_VOTE));
    blocks.addVoteBlock(new VoteBlock("http://www.example.com/file2.html",
                                      1010L, 0L, 1010L, 0L,
                                      ByteArray.makeRandomBytes(20),
                                      VoteBlock.CONTENT_VOTE));
    ud.setVoteBlocks(blocks);
    ud.setVoterNonce(ByteArray.makeRandomBytes(20));
    return ud;
  }
  
  private void assertEqualPollerStateBeans(PollerStateBean b1,
                                           PollerStateBean b2) {
    assertEquals(b1.getAuId(), b2.getAuId());
    assertEquals(b1.getHashAlgorithm(), b2.getHashAlgorithm());
    assertEquals(b1.getLastHashedBlock(), b2.getLastHashedBlock());
    assertEquals(b1.getPluginVersion(), b2.getPluginVersion());
    assertEquals(b1.getUrl(), b2.getUrl());
    assertEquals(b1.getDeadline(), b2.getDeadline());
    assertEquals(b1.getPollerId().getIdString(),
                 b2.getPollerId().getIdString());
    assertEquals(b1.readyToHash(), b2.readyToHash());
  }
  
  private void assertEqualPollerUserData(PollerUserData d1,
                                         PollerUserData d2) {
    assertEquals(d1.getVoterId().getIdString(),
                 d2.getVoterId().getIdString());
    assertEquals(d1.getRepairTarget(), d2.getRepairTarget());
    // XXX: Could use a better comparison for vote blocks
    assertEquals(d1.getVoteBlocks().size(),
                 d2.getVoteBlocks().size());
    assertEquals(d1.getNominees(), d2.getNominees());
    assertEquals(d1.getHashAlgorithm(), d2.getHashAlgorithm());
    assertEquals(d1.getPollerNonce(), d2.getPollerNonce());
    assertEquals(d1.getVoterNonce(), d2.getVoterNonce());
    assertEquals(d1.getPollAckEffortProof(), d2.getPollAckEffortProof());
    assertEquals(d1.getIntroEffortProof(), d2.getIntroEffortProof());
    assertEquals(d1.getReceiptEffortProof(), d2.getReceiptEffortProof());
    assertEquals(d1.getRemainingEffortProof(), d2.getRemainingEffortProof());
    assertEquals(d1.getRepairEffortProof(), d2.getRepairEffortProof());
  }
  
  private void assertEqualVoterUserData(VoterUserData d1,
                                        VoterUserData d2) {
    assertEquals(d1.getAuId(), d2.getAuId());
    assertEquals(d1.getHashAlgorithm(), d2.getHashAlgorithm());
    assertEquals(d1.getIntroEffortProof(), d2.getIntroEffortProof());
    assertEquals(d1.getPluginVersion(), d2.getPluginVersion());
    assertEquals(d1.getPollAckEffortProof(), d2.getPollAckEffortProof());
    assertEquals(d1.getPollerNonce(), d2.getPollerNonce());
    assertEquals(d1.getPollKey(), d2.getPollKey());
    assertEquals(d1.getReceiptEffortProof(), d2.getReceiptEffortProof());
    assertEquals(d1.getRemainingEffortProof(), d2.getRemainingEffortProof());
    assertEquals(d1.getRepairEffortProof(), d2.getRepairEffortProof());
    assertEquals(d1.getUrl(), d2.getUrl());
    assertEquals(d1.getVoterNonce(), d2.getVoterNonce());
    assertEquals(d1.getDeadline(), d2.getDeadline());
    assertEquals(d1.getNominees(), d2.getNominees());
    assertEquals(d1.getPollerId().getIdString(),
                 d2.getPollerId().getIdString());
    assertEquals(d1.getPollVersion(), d2.getPollVersion());
    assertEquals(d1.getVoteBlocks().size(), d2.getVoteBlocks().size());
  }
  
  private void assertEqualInnerCircles(List a, List b) {
    if (a == null) {
      assertTrue(b == null);
      return;
    }
    assertTrue(a.size() == b.size());
    for(int i = 0 ; i < a.size(); i++) {
      PollerUserData ud1 = (PollerUserData)a.get(i);
      PollerUserData ud2 = (PollerUserData)b.get(i);
      assertEqualPollerUserData(ud1, ud2);
    }
  }
}
