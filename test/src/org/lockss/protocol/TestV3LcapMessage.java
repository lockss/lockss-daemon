/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.security.*;
import java.io.DataInputStream;
import org.lockss.util.*;
import org.lockss.test.MockCachedUrlSetSpec;
import java.net.*;
import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Events;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.util.*;
import org.lockss.app.LockssDaemon;
import org.mortbay.util.B64Code;

/** JUnitTest case for class: org.lockss.protocol.Message */
public class TestV3LcapMessage extends LockssTestCase {

  private String m_url = "http://www.example.com";

  private String m_archivalID = "TestAU_1.0";
  private PeerIdentity m_testID;
  private V3LcapMessage m_testMsg;
  private List m_testVoteBlocks;
  private MockPollManager mPollMgr;

  private MockLockssDaemon theDaemon;

  private File tempDir;

  private byte[] m_testBytes =
    new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
                1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
  
  private byte[] m_repairData =
    ByteArray.makeRandomBytes(1000);
  private CIProperties m_repairProps = null;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    tempDir = getTempDir();
    String tempDirPath = tempDir.getAbsolutePath();
    System.setProperty("java.io.tmpdir", tempDirPath);

    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(V3LcapMessage.PARAM_REPAIR_DATA_THRESHOLD, "4096");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    IdentityManager idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    mPollMgr = new MockPollManager();
    theDaemon.setPollManager(mPollMgr);
    try {
      m_testID = idmgr.stringToPeerIdentity("127.0.0.1");
    } catch (IOException ex) {
      fail("can't open test host 127.0.0.1: " + ex);
    }
    m_repairProps = new CIProperties();
    m_repairProps.setProperty("key1", "val1");
    m_repairProps.setProperty("key2", "val2");
    m_repairProps.setProperty("key3", "val3");
    
    m_testVoteBlocks = V3TestUtils.makeVoteBlockList(10);
    m_testMsg = this.makeTestVoteMessage(m_testVoteBlocks);
  }

  public void testNoOpMessageCreation() throws Exception {
    // Without voterNonce2
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID, m_testBytes,
                                                      m_testBytes,
                                                      theDaemon);
    // With voterNonce2
    V3LcapMessage noopMsg2 = V3LcapMessage.makeNoOpMsg(m_testID, m_testBytes,
						       m_testBytes, m_testBytes,
                                                      theDaemon);

    // now check the fields we expect to be valid
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg.getOpcode());
    assertTrue(m_testID == noopMsg.getOriginatorId());
    assertEquals(m_testBytes, noopMsg.getPollerNonce());
    assertEquals(m_testBytes, noopMsg.getVoterNonce());
    assertEquals(null, noopMsg.getVoterNonce2());
    assertEquals(null, noopMsg.getVoteBlocks());
    assertEquals(V3LcapMessage.EST_ENCODED_HEADER_LENGTH,
		 noopMsg.getEstimatedEncodedLength());

    // Same for msg with voterNonce2
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg2.getOpcode());
    assertTrue(m_testID == noopMsg2.getOriginatorId());
    assertEquals(m_testBytes, noopMsg2.getPollerNonce());
    assertEquals(m_testBytes, noopMsg2.getVoterNonce());
    assertEquals(m_testBytes, noopMsg2.getVoterNonce2());
    assertEquals(null, noopMsg2.getVoteBlocks());
    assertEquals(V3LcapMessage.EST_ENCODED_HEADER_LENGTH,
		 noopMsg2.getEstimatedEncodedLength());
  }

  public void testRandomNoOpMessageCreation() throws Exception {
    V3LcapMessage noopMsg1 = V3LcapMessage.makeNoOpMsg(m_testID, theDaemon);
    V3LcapMessage noopMsg2 = V3LcapMessage.makeNoOpMsg(m_testID, theDaemon);

    // now check the fields we expect to be valid
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg1.getOpcode());
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg2.getOpcode());
    assertTrue(noopMsg1.getOriginatorId() == m_testID);
    assertTrue(noopMsg1.getOriginatorId() == noopMsg2.getOriginatorId());
    assertFalse(noopMsg1.getPollerNonce() == noopMsg2.getPollerNonce());
    assertFalse(noopMsg1.getVoterNonce() == noopMsg2.getVoterNonce());
    assertEquals(null, noopMsg1.getVoterNonce2());
    assertEquals(null, noopMsg2.getVoterNonce2());
    assertEquals(null, noopMsg1.getVoteBlocks());
    assertEquals(null, noopMsg2.getVoteBlocks());
  }

  public void testNoOpMessageToString() throws IOException {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID,
                                                      m_testBytes,
                                                      m_testBytes,
                                                      theDaemon);
    String expectedResult = "[V3LcapMessage: from " + m_testID.toString() +
      ", NoOp]";
    assertEquals(expectedResult, noopMsg.toString());
  }

  public void testTestMessageToString() throws IOException {
    String expectedResult = "[V3LcapMessage: from " + m_testID.toString() +
      ", Vote AUID: TestAU_1.0 " +
      "Key:key " +
      "PN:AQIDBAUGBwgJAAECAwQFBgcICQA= " +
      "VN:AQIDBAUGBwgJAAECAwQFBgcICQA= " +
      "B:10 ver 3 rev 5]";
    assertEquals(expectedResult, m_testMsg.toString());
  }
  
  public void getGroup() throws Exception {
    m_testMsg.setGroups(null);
    assertNull(m_testMsg.getGroups());
    assertNull(m_testMsg.getGroupList());
    m_testMsg.setGroups("foo");
    assertEquals("foo", m_testMsg.getGroups());
    assertEquals(ListUtil.list("foo"), m_testMsg.getGroupList());
    m_testMsg.setGroups("foo;bar");
    assertEquals("foo;bar", m_testMsg.getGroups());
    assertEquals(ListUtil.list("foo", "bar"), m_testMsg.getGroupList());
    m_testMsg.setGroups("foo;bar;baz");
    assertEquals("foo;bar;baz", m_testMsg.getGroups());
    assertEquals(ListUtil.list("foo", "bar", "baz"), m_testMsg.getGroupList());
  }

  public void testNoOpEncoding() throws Exception {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID,
                                                      m_testBytes,
                                                      m_testBytes,
                                                      theDaemon);
    InputStream fromMsg = noopMsg.getInputStream();
    V3LcapMessage msg = new V3LcapMessage(fromMsg, tempDir, theDaemon);
    // now test to see if we got back what we started with
    assertTrue(m_testID == msg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_NO_OP, msg.getOpcode());
    assertEquals(m_testBytes, msg.getPollerNonce());
    assertEquals(m_testBytes, msg.getVoterNonce());
    assertEquals(null, msg.getVoterNonce2());
  }

  public void testMemoryRepairMessage() throws Exception {
    int len = 1024;
    byte[] repairData = ByteArray.makeRandomBytes(len);
    V3LcapMessage src = makeRepairMessage(repairData);
    assertEquals(len, src.getRepairDataLength());
    assertEquals(V3LcapMessage.EST_ENCODED_HEADER_LENGTH + len,
		 src.getEstimatedEncodedLength());
    InputStream srcStream = src.getInputStream();
    V3LcapMessage copy = new V3LcapMessage(srcStream, tempDir, theDaemon);
    assertEqualMessages(src, copy);
    assertEquals(len, copy.getRepairDataLength());
    assertEquals(V3LcapMessage.EST_ENCODED_HEADER_LENGTH + len,
		 src.getEstimatedEncodedLength());
    InputStream in = copy.getRepairDataInputStream();
    assertTrue(in+"", in instanceof ByteArrayInputStream);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamUtil.copy(in, out);
    byte[] repairCopy = out.toByteArray();
    assertEquals(repairData, repairCopy);
    // ensure that repeated delete doesn't cause error
    copy.delete();
    copy.delete();
  }

  public void testDiskRepairMessage() throws Exception {
    int len = 100 * 1024;
    byte[] repairData = ByteArray.makeRandomBytes(len);
    V3LcapMessage src = makeRepairMessage(repairData);
    assertEquals(len, src.getRepairDataLength());
    assertEquals(V3LcapMessage.EST_ENCODED_HEADER_LENGTH + len,
		 src.getEstimatedEncodedLength());
    InputStream srcStream = src.getInputStream();
    V3LcapMessage copy = new V3LcapMessage(srcStream, tempDir, theDaemon);
    assertEqualMessages(src, copy);
    assertEquals(len, copy.getRepairDataLength());
    assertEquals(V3LcapMessage.EST_ENCODED_HEADER_LENGTH + len,
		 src.getEstimatedEncodedLength());
    InputStream in = copy.getRepairDataInputStream();
    assertTrue(in+"", in instanceof FileInputStream);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamUtil.copy(in, out);
    byte[] repairCopy = out.toByteArray();
    assertEquals(repairData, repairCopy);
    // ensure that repeated delete doesn't cause error
    copy.delete();
    copy.delete();
  }
  
  public void testPollDuration() throws Exception {
    TimeBase.setSimulated(TimeBase.nowMs());
    V3LcapMessage src = this.makePollMessage(6 * Constants.WEEK);
    InputStream srcStream = src.getInputStream();
    V3LcapMessage copy = new V3LcapMessage(srcStream, tempDir, theDaemon);
    assertEqualMessages(src, copy);
    assertEquals(6 * Constants.WEEK, copy.getDuration());
  }
  
  public void testNullPollNak() throws Exception {
    V3LcapMessage src = this.makePollAckMessage(null);
    InputStream srcStream = src.getInputStream();
    V3LcapMessage copy = new V3LcapMessage(srcStream, tempDir, theDaemon);
    assertEqualMessages(src, copy);
    assertNull(src.getNak());
    assertNull(copy.getNak());
  }
  
  public void testNonNullPollNak1() throws Exception {
    V3LcapMessage src =
      this.makePollAckMessage(V3LcapMessage.PollNak.NAK_GROUP_MISMATCH);
    InputStream srcStream = src.getInputStream();
    V3LcapMessage copy = new V3LcapMessage(srcStream, tempDir, theDaemon);
    assertEqualMessages(src, copy);
    assertNotNull(src.getNak());
    assertNotNull(copy.getNak());
    assertEquals(V3LcapMessage.PollNak.NAK_GROUP_MISMATCH, src.getNak());
    assertEquals(V3LcapMessage.PollNak.NAK_GROUP_MISMATCH, copy.getNak());
  }
  
  public void testNonNullPollNak2() throws Exception {
    V3LcapMessage src =
      this.makePollAckMessage(V3LcapMessage.PollNak.NAK_NO_TIME);
    InputStream srcStream = src.getInputStream();
    V3LcapMessage copy = new V3LcapMessage(srcStream, tempDir, theDaemon);
    assertEqualMessages(src, copy);
    assertNotNull(src.getNak());
    assertNotNull(copy.getNak());
    assertEquals(V3LcapMessage.PollNak.NAK_NO_TIME, src.getNak());
    assertEquals(V3LcapMessage.PollNak.NAK_NO_TIME, copy.getNak());
  }

  public void testUnknownPollNak() throws Exception {
    MyV3LcapMessage src =
      makePollAckMessage(V3LcapMessage.PollNak.NAK_NO_TIME);
    src.setTestNak("KNACKERED");
    InputStream srcStream = src.getInputStream();
    V3LcapMessage copy = new V3LcapMessage(srcStream, tempDir, theDaemon);
    assertEqualMessages(src, copy);
    assertNotNull(src.getNak());
    assertNotNull(copy.getNak());
    assertEquals(V3LcapMessage.PollNak.NAK_NO_TIME, src.getNak());
    assertEquals(V3LcapMessage.PollNak.NAK_UNKNOWN, copy.getNak());
  }

  public void testRequestMessageCreation() throws Exception {
    V3LcapMessage reqMsg =
      new V3LcapMessage("ArchivalID_2", "key", "Plug42",
                        m_testBytes,
                        m_testBytes,
                        V3LcapMessage.MSG_REPAIR_REQ,
                        987654321, m_testID, tempDir, theDaemon);
    reqMsg.setTargetUrl("http://foo.com/");

    for (Iterator ix = m_testVoteBlocks.iterator(); ix.hasNext(); ) {
      reqMsg.addVoteBlock((VoteBlock)ix.next());
    }

    assertEquals(3, reqMsg.getProtocolVersion());
    assertEquals("Plug42", reqMsg.getPluginVersion());
    assertTrue(m_testID == reqMsg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_REPAIR_REQ, reqMsg.getOpcode());
    assertEquals("ArchivalID_2", reqMsg.getArchivalId());
    assertEquals("http://foo.com/", reqMsg.getTargetUrl());
    assertEquals(m_testBytes, reqMsg.getPollerNonce());
    assertEquals(m_testBytes, reqMsg.getVoterNonce());
    assertEquals(null, reqMsg.getVoterNonce2());
    List aBlocks = new ArrayList();
    List bBlocks = new ArrayList();
    for (VoteBlocksIterator iter = m_testMsg.getVoteBlockIterator(); iter.hasNext(); ) {
      aBlocks.add(iter.next());
    }
    for (VoteBlocksIterator iter = reqMsg.getVoteBlockIterator(); iter.hasNext(); ) {
      bBlocks.add(iter.next());
    }
    assertEquals(aBlocks, bBlocks);

    // Actual size of test vote blocks is unpredictable
    assertTrue(reqMsg.getEstimatedEncodedLength() >
	       V3LcapMessage.EST_ENCODED_HEADER_LENGTH);
  }
  
  public void testReceiptMessage() throws Exception {
    V3LcapMessage msg = makeReceiptMessage();
    assertEquals(-1.0, msg.getAgreementHint());
    assertEquals(-1.0, msg.getWeightedAgreementHint());

    InputStream fromMsg = msg.getInputStream();
    V3LcapMessage msg2 = new V3LcapMessage(fromMsg, tempDir, theDaemon);
    // now test to see if we got back what we started with
    assertEquals(V3LcapMessage.MSG_EVALUATION_RECEIPT, msg.getOpcode());
    assertEquals(-1.0, msg.getAgreementHint());
    assertEquals(-1.0, msg.getWeightedAgreementHint());

    msg.setAgreementHint(0.25);
    assertEquals(0.25, msg.getAgreementHint());
    assertEquals(-1.0, msg.getWeightedAgreementHint());
    fromMsg = msg.getInputStream();
    msg2 = new V3LcapMessage(fromMsg, tempDir, theDaemon);
    assertEquals(0.25, msg.getAgreementHint());
    assertEquals(-1.0, msg.getWeightedAgreementHint());

    msg.setWeightedAgreementHint(0.75);
    assertEquals(0.25, msg.getAgreementHint());
    assertEquals(0.75, msg.getWeightedAgreementHint());
    fromMsg = msg.getInputStream();
    msg2 = new V3LcapMessage(fromMsg, tempDir, theDaemon);
    assertEquals(0.25, msg.getAgreementHint());
    assertEquals(0.75, msg.getWeightedAgreementHint());
  }

  public void testDiskBasedStreamEncodingTest() throws Exception {
    // Make a list of vote blocks large enough to trigger on-disk
    // vote message creation.
    List testVoteBlocks = V3TestUtils.makeVoteBlockList(21);
    V3LcapMessage testMsg = makeTestVoteMessage(testVoteBlocks);
    assertTrue(testMsg.m_voteBlocks instanceof DiskVoteBlocks);
    // Encode the test message.
    InputStream is = testMsg.getInputStream();
    V3LcapMessage decodedMsg  = new V3LcapMessage(is, tempDir, theDaemon);
    // Ensure that the decoded message matches the test message.
    assertEqualMessages(testMsg, decodedMsg);
    
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
    assertEquals(a.getVoterNonce2(), b.getVoterNonce2());
    assertEquals(a.getPluginVersion(), b.getPluginVersion());
    assertEquals(a.getHashAlgorithm(), b.getHashAlgorithm());
    assertEquals(a.isVoteComplete(), b.isVoteComplete());
    assertEquals(a.getRepairDataLength(), b.getRepairDataLength());
    assertEquals(a.getLastVoteBlockURL(), b.getLastVoteBlockURL());
    assertIsomorphic(a.getNominees(), b.getNominees());
    List aBlocks = new ArrayList();
    List bBlocks = new ArrayList();
    for (VoteBlocksIterator iter = a.getVoteBlockIterator(); iter.hasNext(); ) {
      aBlocks.add(iter.next());
    }
    for (VoteBlocksIterator iter = b.getVoteBlockIterator(); iter.hasNext(); ) {
      bBlocks.add(iter.next());
    }
    assertTrue(aBlocks.equals(bBlocks));

    //  TODO: Figure out how to test time.

  }
  
  private V3LcapMessage makePollMessage(long duration) {
    V3LcapMessage msg = new V3LcapMessage("ArchivalID_2", "key", "Plug42",
                                          m_testBytes,
                                          m_testBytes,
                                          V3LcapMessage.MSG_POLL,
                                          TimeBase.nowMs() + duration,
					  m_testID, tempDir,
                                          theDaemon);
    return msg;
  }
  
  private MyV3LcapMessage makePollAckMessage(V3LcapMessage.PollNak nak) {
    MyV3LcapMessage msg = new MyV3LcapMessage("ArchivalID_2", "key", "Plug42",
					      m_testBytes,
					      m_testBytes,
					      V3LcapMessage.MSG_POLL_ACK,
					      987654321, m_testID, tempDir,
					      theDaemon);
    if (nak != null) {
      msg.setNak(nak);
    }
    
    return msg;
  }
  
  private V3LcapMessage makeRepairMessage(byte[] repairData) {
    V3LcapMessage msg = new V3LcapMessage("ArchivalID_2", "key", "Plug42",
                                          m_testBytes,
                                          m_testBytes,
                                          V3LcapMessage.MSG_REPAIR_REP,
                                          987654321, m_testID, tempDir,
                                          theDaemon);
    msg.setHashAlgorithm(LcapMessage.getDefaultHashAlgorithm());
    msg.setTargetUrl(m_url);
    msg.setArchivalId(m_archivalID);
    msg.setPluginVersion("PlugVer42");
    msg.setRepairDataLength(repairData.length);
    msg.setRepairProps(m_repairProps);
    msg.setInputStream(new ByteArrayInputStream(repairData));
    return msg;
  }

  private V3LcapMessage makeTestVoteMessage(Collection voteBlocks)
      throws IOException {
    mPollMgr.setStateDir("key", tempDir);
    V3LcapMessage msg = new V3LcapMessage("ArchivalID_2", "key", "Plug42",
                                          m_testBytes,
                                          m_testBytes,
                                          V3LcapMessage.MSG_VOTE,
                                          987654321, m_testID, tempDir,
                                          theDaemon);

    // Set msg vote blocks.
    for (Iterator ix = voteBlocks.iterator(); ix.hasNext(); ) {
      msg.addVoteBlock((VoteBlock)ix.next());
    }

    msg.setHashAlgorithm(LcapMessage.getDefaultHashAlgorithm());
    msg.setArchivalId(m_archivalID);
    msg.setPluginVersion("PlugVer42");
    return msg;
  }

  private V3LcapMessage makeReceiptMessage() {
    V3LcapMessage msg = new V3LcapMessage("ArchivalID_2", "key", "Plug42",
					  m_testBytes,
					  m_testBytes,
					  V3LcapMessage.MSG_EVALUATION_RECEIPT,
					  987654321, m_testID, tempDir,
					  theDaemon);
    return msg;
  }


  static class MyV3LcapMessage extends V3LcapMessage {
    String testNak;

    public MyV3LcapMessage(File messageDir, LockssApp daemon) {
      super(messageDir, daemon);
    }

    public MyV3LcapMessage(String auId, String pollKey, String pluginVersion,
			   byte[] pollerNonce, byte[] voterNonce, int opcode,
			   long deadline, PeerIdentity origin, File messageDir,
			   LockssApp daemon) {
      super(auId, pollKey, pluginVersion, pollerNonce, voterNonce, opcode,
	    deadline, origin, messageDir, daemon);
    }

    public MyV3LcapMessage(String auId, String pollKey, String pluginVersion,
			   byte[] pollerNonce, byte[] voterNonce,
			   byte[] voterNonce2, int opcode,
			   long deadline, PeerIdentity origin, File messageDir,
			   LockssApp daemon) {
      super(auId, pollKey, pluginVersion, pollerNonce, voterNonce,
	    voterNonce2, opcode,
	    deadline, origin, messageDir, daemon);
    }

    public MyV3LcapMessage(byte[] encodedBytes, File messageDir,
			   LockssApp daemon)
	throws IOException {
      super(encodedBytes, messageDir, daemon);
    }

    public MyV3LcapMessage(InputStream inputStream, File messageDir,
			   LockssApp daemon) throws IOException {
      super(inputStream, messageDir, daemon);
    }

    void setTestNak(String nakName) {
      testNak = nakName;
    }

    public void storeProps() throws IOException {
      super.storeProps();
      if (testNak != null) {
	m_props.setProperty("nak", testNak);
      }
    }
  }

}
