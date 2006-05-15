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
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;

/*
 * Test V3Serializer, V3PollerSerializer, and V3VoterSerializer.
 */

public class TestV3Serializer extends LockssTestCase {

  String tempDirPath;
  LockssDaemon theDaemon;
  IdentityManager idManager;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath();
    theDaemon = getMockLockssDaemon();
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idManager = theDaemon.getIdentityManager();
    idManager.startService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testNullDirThrows() throws Exception {
    try {
      V3PollerSerializer ser = new V3PollerSerializer(theDaemon, null);
      fail("Should have thrown NullPointerException");
    } catch (NullPointerException ex) {
      // Expected
    }
  }

  public void testClosePoll() throws Exception {
    // Ensure poll dir is deleted.
    V3PollerSerializer pollerSerializer = new V3PollerSerializer(theDaemon);
    File pollDir = (File)PrivilegedAccessor.getValue(pollerSerializer,
                                                     "pollDir");
    assertTrue(pollDir.exists());
    pollerSerializer.closePoll();
    assertFalse(pollDir.exists());

    V3VoterSerializer voterSerializer = new V3VoterSerializer(theDaemon);
    pollDir = (File)PrivilegedAccessor.getValue(voterSerializer,
                                                "pollDir");
    assertTrue(pollDir.exists());
    voterSerializer.closePoll();
    assertFalse(pollDir.exists());
  }

  public void testSaveAndLoadPollerState() throws Exception {
    V3PollerSerializer pollerSerializer = new V3PollerSerializer(theDaemon);
    File pollDir = (File)PrivilegedAccessor.getValue(pollerSerializer,
                                                     "pollDir");
    PollerStateBean vsb1 = makePollerStateBean(pollerSerializer);
    pollerSerializer.savePollerState(vsb1);
    PollerStateBean vsb2 = pollerSerializer.loadPollerState();
    V3TestUtil.assertEqualPollerStateBeans(vsb1, vsb2);
    pollerSerializer = new V3PollerSerializer(theDaemon, pollDir);
    PollerStateBean vsb3 = pollerSerializer.loadPollerState();
    V3TestUtil.assertEqualPollerStateBeans(vsb1, vsb3);
  }


  public void testSaveAndLoadPollerUserData() throws Exception {
    V3PollerSerializer pollerSerializer = new V3PollerSerializer(theDaemon);
    File pollDir = (File)PrivilegedAccessor.getValue(pollerSerializer,
                                                     "pollDir");
    ParticipantUserData ud1 = makePollerUserData(pollerSerializer);
    PeerIdentity id = ud1.getVoterId();
    pollerSerializer.savePollerUserData(ud1);
    ParticipantUserData ud2 = pollerSerializer.loadPollerUserData(id);
    V3TestUtil.assertEqualParticipantUserData(ud1, ud2);
    pollerSerializer = new V3PollerSerializer(theDaemon, pollDir);
    ParticipantUserData ud3 = pollerSerializer.loadPollerUserData(id);
    V3TestUtil.assertEqualParticipantUserData(ud1, ud3);
    PeerIdentity id2 = new MockPeerIdentity("192.168.1.1:9999");
    try {
      pollerSerializer.loadPollerUserData(id2);
      fail("Should have thrown");
    } catch (V3Serializer.PollSerializerException ex) {
      // expected.
    }
  }

  public void testSaveAndLoadVoterUserData() throws Exception {
    V3VoterSerializer voterSerializer = new V3VoterSerializer(theDaemon);
    File pollDir = (File)PrivilegedAccessor.getValue(voterSerializer,
                                                     "pollDir");
    VoterUserData ud1 = makeVoterUserData(voterSerializer);
    voterSerializer.saveVoterUserData(ud1);
    VoterUserData ud2 = voterSerializer.loadVoterUserData();
    V3TestUtil.assertEqualVoterUserData(ud1, ud2);
    voterSerializer = new V3VoterSerializer(theDaemon, pollDir);
    VoterUserData ud3 = voterSerializer.loadVoterUserData();
    V3TestUtil.assertEqualVoterUserData(ud1, ud3);
  }

  public void testLoadInnerCircleStates() throws Exception {
    V3PollerSerializer pollerSerializer = new V3PollerSerializer(theDaemon);
    File pollDir = (File)PrivilegedAccessor.getValue(pollerSerializer,
                                                     "pollDir");

    ArrayList uds1 =
      (ArrayList)ListUtil.list(makePollerUserData("10.1.1.1:8000",
                                                  pollerSerializer),
                               makePollerUserData("10.1.1.2:8000",
                                                  pollerSerializer),
                               makePollerUserData("10.1.1.3:8000",
                                                  pollerSerializer),
                               makePollerUserData("10.1.1.4:8000",
                                                  pollerSerializer),
                               makePollerUserData("10.1.1.5:8000",
                                                  pollerSerializer));

    for (Iterator iter = uds1.iterator(); iter.hasNext(); ) {
      pollerSerializer.savePollerUserData((ParticipantUserData)iter.next());
    }
    Collection uds2 = pollerSerializer.loadVoterStates();
    assertEqualInnerCircles(uds1, uds2);
    pollerSerializer = new V3PollerSerializer(theDaemon, pollDir);
    Collection uds3 = pollerSerializer.loadVoterStates();
    assertEqualInnerCircles(uds1, uds3);
  }


  private PollerStateBean makePollerStateBean(V3PollerSerializer serializer) {
    PollerStateBean vsb = new PollerStateBean();
    vsb.setAuId("testAu");
    vsb.setCachedUrlSet(new MockCachedUrlSet());
    vsb.setPollDeadline(10000);
    vsb.setHashAlgorithm("SHA1");
    vsb.setLastHashedBlock("http://www.example.com/file1.html");
    vsb.setPluginVersion("mock");
    vsb.setPollerId(new MockPeerIdentity("127.0.0.1:8080"));
    vsb.setPollKey("mock-poll-1");
    vsb.setProtocolVersion(1);
    return vsb;
  }

  private ParticipantUserData makePollerUserData(V3PollerSerializer serializer)
      throws IOException {
    return makePollerUserData("127.0.0.1:8080", serializer);
  }

  private ParticipantUserData makePollerUserData(String voterId,
                                                 V3PollerSerializer serializer)
                                                 throws IOException {
    ParticipantUserData ud = new ParticipantUserData();
    PeerIdentity id = idManager.findPeerIdentity(voterId);
    ud.setVoterId(id);
    ud.setHashAlgorithm("SHA1");
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
                                      ByteArray.makeRandomBytes(20),
                                      VoteBlock.CONTENT_VOTE));
    blocks.addVoteBlock(new VoteBlock("http://www.example.com/file2.html",
                                      1010L, 0L, 1010L, 0L,
                                      ByteArray.makeRandomBytes(20),
                                      ByteArray.makeRandomBytes(20),
                                      VoteBlock.CONTENT_VOTE));
    ud.setVoteBlocks(blocks);
    ud.setVoterNonce(ByteArray.makeRandomBytes(20));
    return ud;
  }

  private VoterUserData makeVoterUserData(V3VoterSerializer serializer)
      throws IOException {
    VoterUserData ud = new VoterUserData();
    ud.setAuId("mockAu");
    ud.setDeadline(10000);
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
    ud.setRepairTarget("http://www.example.com/file1.html");
    VoteBlocks blocks = new MemoryVoteBlocks();
    blocks.addVoteBlock(new VoteBlock("http://www.example.com/file1.html",
                                      1000L, 0L, 1000L, 0L,
                                      ByteArray.makeRandomBytes(20),
                                      ByteArray.makeRandomBytes(20),
                                      VoteBlock.CONTENT_VOTE));
    blocks.addVoteBlock(new VoteBlock("http://www.example.com/file2.html",
                                      1010L, 0L, 1010L, 0L,
                                      ByteArray.makeRandomBytes(20),
                                      ByteArray.makeRandomBytes(20),
                                      VoteBlock.CONTENT_VOTE));
    ud.setVoteBlocks(blocks);
    ud.setVoterNonce(ByteArray.makeRandomBytes(20));
    return ud;
  }

  private class PollerUserDataComparator implements Comparator {
    // Simply sort by voterID
    public int compare(Object a, Object b) {
      String pidA = ((ParticipantUserData)a).getVoterId().getIdString();
      String pidB = ((ParticipantUserData)b).getVoterId().getIdString();
      return pidA.compareToIgnoreCase(pidB);
    }
  }

  private void assertEqualInnerCircles(Collection a, Collection b) {
    if (a == null) {
      assertTrue(b == null);
      return;
    }
    Comparator c = new PollerUserDataComparator();
    ArrayList la = new ArrayList(a);
    ArrayList lb = new ArrayList(b);
    Collections.sort(la, c);
    Collections.sort(lb, c);
    assertTrue(a.size() == b.size());
    for(int i = 0 ; i < a.size(); i++) {
      ParticipantUserData ud1 = (ParticipantUserData)la.get(i);
      ParticipantUserData ud2 = (ParticipantUserData)lb.get(i);
      V3TestUtil.assertEqualParticipantUserData(ud1, ud2);
    }
  }

  public int compare(Object arg0, Object arg1) {
    // TODO Auto-generated method stub
    return 0;
  }
}
