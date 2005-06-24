/*
 * $Id: TestV3LcapMessage.java,v 1.4 2005-06-24 20:21:07 smorabito Exp $
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

import java.security.*;
import java.io.DataInputStream;
import org.lockss.util.*;
import org.lockss.test.MockCachedUrlSetSpec;
import java.net.*;
import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.poller.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.util.*;
import org.lockss.app.LockssDaemon;
import org.mortbay.util.B64Code;

/** JUnitTest case for class: org.lockss.protocol.Message */
public class TestV3LcapMessage extends LockssTestCase {

  private String m_url = "http://www.example.com";
  private byte[] m_testBytes = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
    11, 12, 13, 14, 15, 16, 17, 18, 19, 20
  };
  private String m_archivalID = "TestAU_1.0";
  private PeerIdentity m_testID;
  private V3LcapMessage m_testMsg;
  private List m_testVoteBlocks;
  private Comparator m_comparator;

  private LockssDaemon theDaemon;


  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }

    m_comparator = new VoteBlockComparator();

    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    // p.setProperty(PollSpec.PARAM_USE_V3_POLL_VERSION, "1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    IdentityManager idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    try {
      m_testID = idmgr.stringToPeerIdentity("127.0.0.1");
    } catch (IOException ex) {
      fail("can't open test host 127.0.0.1: " + ex);
    }

    // Construct a test message used by several tests.
    m_testVoteBlocks = makeVoteBlockList(10);
    m_testMsg = makeTestVoteMessage(m_testVoteBlocks);
  }

  public void testNoOpMessageCreation() throws Exception {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID, m_testBytes);

    // now check the fields we expect to be valid
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg.getOpcode());
    assertTrue(m_testID == noopMsg.getOriginatorId());
    assertEquals(m_testBytes, noopMsg.getChallenge());
    assertEmpty(ListUtil.fromIterator(noopMsg.getVoteBlockIterator()));
  }

  public void testRandomNoOpMessageCreation() throws Exception {
    V3LcapMessage noopMsg1 = V3LcapMessage.makeNoOpMsg(m_testID);
    V3LcapMessage noopMsg2 = V3LcapMessage.makeNoOpMsg(m_testID);

    // now check the fields we expect to be valid
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg1.getOpcode());
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg2.getOpcode());
    assertTrue(noopMsg1.getOriginatorId() == m_testID);
    assertTrue(noopMsg1.getOriginatorId() == noopMsg2.getOriginatorId());
    assertFalse(noopMsg1.getChallenge() == noopMsg2.getChallenge());
    assertEmpty(ListUtil.fromIterator(noopMsg1.getVoteBlockIterator()));
    assertEmpty(ListUtil.fromIterator(noopMsg2.getVoteBlockIterator()));
  }

  public void testNoOpMessageToString() throws IOException {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID, m_testBytes);
    String expectedResult = "[V3LcapMessage: from " + m_testID.toString() +
      ", NoOp]";
    assertEquals(expectedResult, noopMsg.toString());
  }

  public void testTestMessageToString() throws IOException {
    String expectedResult = "[V3LcapMessage: from " + m_testID.toString() +
      ", http://www.example.com Vote " +
      "C:AAECAwQFBgcICQoLDA0ODxAREhMU B:10 ver 1]";
    assertEquals(expectedResult, m_testMsg.toString());
  }

  public void testNoOpEncoding() throws Exception {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID, m_testBytes);
    byte[] encodedBytes = noopMsg.encodeMsg();

    V3LcapMessage msg = new V3LcapMessage(encodedBytes);
    // now test to see if we got back what we started with
    assertTrue(m_testID == msg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_NO_OP, msg.getOpcode());
    assertEquals(m_testBytes, msg.getChallenge());
  }

  public void testReplyMessageCreation() throws Exception {
    V3LcapMessage repMsg =
      V3LcapMessage.makeReplyMsg(m_testMsg,
				 V3LcapMessage.MSG_POLL_ACK,
				 100000,
				 m_testID);

    assertTrue(m_testID == repMsg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_POLL_ACK, repMsg.getOpcode());
    assertEquals(m_testMsg.getTargetUrl(), repMsg.getTargetUrl());
    assertEquals(m_testMsg.getArchivalId(), repMsg.getArchivalId());
    assertEquals(m_testMsg.getHashAlgorithm(), repMsg.getHashAlgorithm());
    assertEquals(m_testMsg.getPluginVersion(), repMsg.getPluginVersion());
    assertEquals(m_testMsg.getChallenge(), repMsg.getChallenge());
    List testMsgVoteBlocks = ListUtil.fromIterator(m_testMsg.getVoteBlockIterator());
    List repMsgVoteBlocks = ListUtil.fromIterator(repMsg.getVoteBlockIterator());
    assertTrue(testMsgVoteBlocks.equals(repMsgVoteBlocks));
    // Ensure both are equal to m_testVoteBlocks.
    assertTrue(m_testVoteBlocks.equals(repMsgVoteBlocks));
  }

  public void testRequestMessageCreation() throws Exception {
    PollSpec spec =
      new MockPollSpec("ArchivalID_2", "http://foo.com/", null, null, "Plug42", -1);

    V3LcapMessage reqMsg =
      V3LcapMessage.makeRequestMsg(spec,
				   m_testBytes,
				   V3LcapMessage.MSG_REPAIR_REQ,
				   100000,
				   m_testID);

    for (Iterator ix = m_testVoteBlocks.iterator(); ix.hasNext(); ) {
      reqMsg.addVoteBlock((VoteBlock)ix.next());
    }

    assertEquals(1, spec.getPollVersion());
    assertEquals(1, reqMsg.getPollVersion());
    assertEquals("Plug42", reqMsg.getPluginVersion());
    assertTrue(m_testID == reqMsg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_REPAIR_REQ, reqMsg.getOpcode());
    assertEquals("ArchivalID_2", reqMsg.getArchivalId());
    assertEquals("http://foo.com/", reqMsg.getTargetUrl());
    assertEquals(m_testBytes, reqMsg.getChallenge());
    List testMsgVoteBlocks = ListUtil.fromIterator(m_testMsg.getVoteBlockIterator());
    List reqMsgVoteBlocks = ListUtil.fromIterator(reqMsg.getVoteBlockIterator());
    assertTrue(testMsgVoteBlocks.equals(reqMsgVoteBlocks));
    assertTrue(m_testVoteBlocks.equals(reqMsgVoteBlocks));
  }

  public void testMessageByteEncoding() throws Exception {
    // Encode the test message.
    byte[] msgbytes = m_testMsg.encodeMsg();

    // Construct a new msg from the encoded bytes.
    V3LcapMessage decodedMsg = new V3LcapMessage(msgbytes);

    // Ensure that the decoded message matches the test message.
    assertEqualMessages(m_testMsg, decodedMsg);
  }

  public void testMsgStreamEncoding() throws Exception {
    // Encode the test message.
    InputStream is = m_testMsg.getInputStream();
    V3LcapMessage decodedMsg  = new V3LcapMessage(is);

    // Ensure that the decoded message matches the test message.
    assertEqualMessages(m_testMsg, decodedMsg);
  }

  public void testSortVoteBlocks() {

    // Ensure sorting by unfiltered size works and
    // is preferred.
    VoteBlock vb1 =  makeTestVoteBlock("a", 2, 0, 1, 0);
    VoteBlock vb2 =  makeTestVoteBlock("b", 3, 0, 2, 0);
    VoteBlock vb3 =  makeTestVoteBlock("c", 1, 0, 3, 0);

    List msgs = ListUtil.list(vb1, vb2, vb3);
    List expectedOrder = ListUtil.list(vb3, vb1, vb2);

    Collections.sort(msgs, m_comparator);
    assertIsomorphic(expectedOrder, msgs);

    // Ensure sorting by filtered size works if
    // unfiltered sizes are the same.
    VoteBlock vb4 = makeTestVoteBlock("a", 1, 0, 2, 0);
    VoteBlock vb5 = makeTestVoteBlock("b", 1, 0, 3, 0);
    VoteBlock vb6 = makeTestVoteBlock("c", 1, 0, 1, 0);

    msgs = ListUtil.list(vb4, vb5, vb6);
    expectedOrder = ListUtil.list(vb6, vb4, vb5);

    Collections.sort(msgs, m_comparator);
    assertIsomorphic(expectedOrder, msgs);

    // Ensure sorting by file name works if filtered sizes
    // and unfiltered sizes are the same.
    VoteBlock vb7 = makeTestVoteBlock("c", 1, 0, 2, 0);
    VoteBlock vb8 = makeTestVoteBlock("a", 1, 0, 2, 0);
    VoteBlock vb9 = makeTestVoteBlock("b", 1, 0, 2, 0);

    msgs = ListUtil.list(vb7, vb8, vb9);
    expectedOrder = ListUtil.list(vb8, vb9, vb7);

    Collections.sort(msgs, m_comparator);
    assertIsomorphic(expectedOrder, msgs);
  }

  //
  // Utility methods
  //

  /**
   * Construct a VoteBlock useful for testing with.  Hashes are completely
   * contrived.
   */
  private VoteBlock makeTestVoteBlock(String fName,
				      int uLength, int uOffset,
				      int fLength, int fOffset) {
    return new VoteBlock(fName, uLength, uOffset, fLength, fOffset,
			 computeHash(fName + "a"),
			 computeHash(fName + "b"),
			 computeHash(fName + "c"));
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
    assertEquals(a.isVoteComplete(), b.isVoteComplete());
    assertEquals(a.getLastVoteBlockURL(), b.getLastVoteBlockURL());
    assertIsomorphic(a.getNominees(), b.getNominees());
    List aVoteBlocks = ListUtil.fromIterator(a.getVoteBlockIterator());
    List bVoteBlocks = ListUtil.fromIterator(b.getVoteBlockIterator());
    assertTrue(aVoteBlocks.equals(bVoteBlocks));

    // TODO: Figure out how to test time.

  }

  private V3LcapMessage makeTestVoteMessage() {
    return makeTestVoteMessage(null);
  }

  private V3LcapMessage makeTestVoteMessage(Collection voteBlocks) {
    V3LcapMessage msg = new V3LcapMessage(V3LcapMessage.MSG_VOTE, m_testID, m_url,
					  123456789, 987654321, m_testBytes);

    // Set msg vote blocks.
    for (Iterator ix = voteBlocks.iterator(); ix.hasNext(); ) {
      msg.addVoteBlock((VoteBlock)ix.next());
    }
    msg.setHashAlgorithm(LcapMessage.getDefaultHashAlgorithm());
    msg.setArchivalId(m_archivalID);
    msg.setPluginVersion("PlugVer42");
    return msg;
  }

  private List makeVoteBlockList(int size) {
    ArrayList vbList = new ArrayList();
    for (int ix = 0; ix < size; ix++) {
      String fileName = "/test-" + ix + ".html";
      byte[] plHash = computeHash(fileName + "a");
      byte[] chHash = computeHash(fileName + "b");
      byte[] proof = computeHash(fileName + "c");
      VoteBlock vb =
	new VoteBlock("/test-" + ix + ".html", 1024, 0,
		      1024, 0, plHash, chHash, proof);
      if (log.isDebug2()) {
	log.debug2("Creating voteblock: " + vb);
      }
      vbList.add(vb);
    }
    return vbList;
  }

  private byte[] computeHash(String s) {
    try {
      MessageDigest hasher = MessageDigest.getInstance("SHA");
      hasher.update(s.getBytes());
      byte[] hashed = hasher.digest();
      return hashed;
    } catch (java.security.NoSuchAlgorithmException e) {
      return new byte[0];
    }
  }
}
