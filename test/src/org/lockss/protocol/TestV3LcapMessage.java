/*
 * $Id: TestV3LcapMessage.java,v 1.1 2005-03-18 09:09:21 smorabito Exp $
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

    m_comparator = new V3LcapMessage.VoteBlockComparator();

    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(PollSpec.PARAM_USE_POLL_VERSION, "3");
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
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg.m_opcode);
    assertTrue(m_testID == noopMsg.getOriginatorId());
    assertEquals(m_testBytes, noopMsg.getVerifier());
    assertEmpty(ListUtil.fromIterator(noopMsg.getVoteBlockIterator()));
  }

  public void testNoOpMessageToString() throws IOException {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID, m_testBytes);
    String expectedResult = "[V3LcapMessage: from [Peer: 127.0.0.1], NoOp]";
    assertEquals(expectedResult, noopMsg.toString());
  }

  public void testTestMessageToString() throws IOException {
    String expectedResult = "[V3LcapMessage: from " +
      "[Peer: 127.0.0.1], http://www.example.com Vote " +
      "C:AAECAwQFBgcICQoLDA0ODxAREhMU V:AAECAwQFBgcICQoLDA0ODxAREhMU " +
      "H:AAECAwQFBgcICQoLDA0ODxAREhMU B:10 ver 3]";
    assertEquals(expectedResult, m_testMsg.toString());
  }

  public void testNoOpEncoding() throws Exception {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID, m_testBytes);
    byte[] encodedBytes = noopMsg.encodeMsg();

    V3LcapMessage msg = new V3LcapMessage(encodedBytes);
    // now test to see if we got back what we started with
    assertTrue(m_testID == msg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_NO_OP, msg.m_opcode);
    assertEquals(m_testBytes, msg.m_verifier);
  }

  public void testReplyMessageCreation() throws Exception {
    V3LcapMessage repMsg =
      V3LcapMessage.makeReplyMsg(m_testMsg,
				 m_testBytes,
				 m_testBytes,
				 V3LcapMessage.MSG_POLL_ACK,
				 100000,
				 m_testID);

    assertTrue(m_testID == repMsg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_POLL_ACK, repMsg.getOpcode());
    assertEquals(m_testMsg.getTimeToLive(), repMsg.getTimeToLive());
    assertEquals(m_testMsg.getTargetUrl(), repMsg.getTargetUrl());
    assertEquals(m_testMsg.getArchivalId(), repMsg.getArchivalId());
    assertEquals(m_testMsg.getHashAlgorithm(), repMsg.getHashAlgorithm());
    assertEquals(m_testMsg.getPluginVersion(), repMsg.getPluginVersion());
    assertEquals(m_testMsg.getChallenge(), repMsg.getChallenge());
    assertEquals(m_testMsg.getVerifier(), repMsg.getVerifier());
    assertEquals(m_testMsg.getHashed(), repMsg.getHashed());
    ArrayList testMsgVoteBlocks =
      new ArrayList(ListUtil.fromIterator(m_testMsg.getVoteBlockIterator()));
    ArrayList repMsgVoteBlocks =
      new ArrayList(ListUtil.fromIterator(repMsg.getVoteBlockIterator()));
    assertSortedIsomorphic(testMsgVoteBlocks, repMsgVoteBlocks);
    // Ensure both are equal to m_testVoteBlocks.
    assertSortedIsomorphic(m_testVoteBlocks, repMsgVoteBlocks);
  }

  public void testRequestMessageCreation() throws Exception {
    PollSpec spec =
      new MockPollSpec("ArchivalID_2", "http://foo.com/", null, null, "Plug42", -1);

    V3LcapMessage reqMsg =
      V3LcapMessage.makeRequestMsg(spec,
				   m_testVoteBlocks,
				   m_testBytes,
				   m_testBytes,
				   V3LcapMessage.MSG_REPAIR_REQ,
				   100000,
				   m_testID);

    assertEquals(3, spec.getPollVersion());
    assertEquals(3, reqMsg.getPollVersion());
    assertEquals("Plug42", reqMsg.getPluginVersion());
    assertTrue(m_testID == reqMsg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_REPAIR_REQ, reqMsg.getOpcode());
    assertEquals("ArchivalID_2", reqMsg.getArchivalId());
    assertEquals("http://foo.com/", reqMsg.getTargetUrl());
    assertEquals(m_testBytes, reqMsg.getChallenge());
    assertEquals(m_testBytes, reqMsg.getVerifier());
    assertNull(reqMsg.getHashed());
    ArrayList testMsgVoteBlocks =
      new ArrayList(ListUtil.fromIterator(m_testMsg.getVoteBlockIterator()));
    ArrayList reqMsgVoteBlocks =
      new ArrayList(ListUtil.fromIterator(reqMsg.getVoteBlockIterator()));
    assertSortedIsomorphic(testMsgVoteBlocks, reqMsgVoteBlocks);
    assertSortedIsomorphic(m_testVoteBlocks, reqMsgVoteBlocks);
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
    ByteArrayOutputStream baos = m_testMsg.getOutputStream();

    // Bridge the outputstream/inputstream with a byte array.
    byte[] buf = baos.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);

    V3LcapMessage decodedMsg  = new V3LcapMessage(bais);

    // Ensure that the decoded message matches the test message.
    assertEqualMessages(m_testMsg, decodedMsg);
  }

  public void testSortVoteBlocks() {

    // Ensure sorting by unfiltered size works and
    // is preferred.
    V3LcapMessage.VoteBlock vb1 =
      makeTestVoteBlock("a", 2, 0, 1, 0);
    V3LcapMessage.VoteBlock vb2 =
      makeTestVoteBlock("b", 3, 0, 2, 0);
    V3LcapMessage.VoteBlock vb3 =
      makeTestVoteBlock("c", 1, 0, 3, 0);

    List msgs = ListUtil.list(vb1, vb2, vb3);
    List expectedOrder = ListUtil.list(vb3, vb1, vb2);

    Collections.sort(msgs, m_comparator);
    assertIsomorphic(expectedOrder, msgs);

    // Ensure sorting by filtered size works if
    // unfiltered sizes are the same.
    V3LcapMessage.VoteBlock vb4 =
      makeTestVoteBlock("a", 1, 0, 2, 0);
    V3LcapMessage.VoteBlock vb5 =
      makeTestVoteBlock("b", 1, 0, 3, 0);
    V3LcapMessage.VoteBlock vb6 =
      makeTestVoteBlock("c", 1, 0, 1, 0);

    msgs = ListUtil.list(vb4, vb5, vb6);
    expectedOrder = ListUtil.list(vb6, vb4, vb5);

    Collections.sort(msgs, m_comparator);
    assertIsomorphic(expectedOrder, msgs);

    // Ensure sorting by file name works if filtered sizes
    // and unfiltered sizes are the same.
    V3LcapMessage.VoteBlock vb7 =
      makeTestVoteBlock("c", 1, 0, 2, 0);
    V3LcapMessage.VoteBlock vb8 =
      makeTestVoteBlock("a", 1, 0, 2, 0);
    V3LcapMessage.VoteBlock vb9 =
      makeTestVoteBlock("b", 1, 0, 2, 0);

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
  private V3LcapMessage.VoteBlock makeTestVoteBlock(String fName,
						    int uLength, int uOffset,
						    int fLength, int fOffset) {
    return new V3LcapMessage.VoteBlock(fName, uLength, uOffset, fLength, fOffset,
				       computeHash(fName + "a"),
				       computeHash(fName + "b"),
				       computeHash(fName + "c"));
  }

  private void assertEqualMessages(V3LcapMessage a, V3LcapMessage b) {
    assertTrue(a.getOriginatorId() == b.getOriginatorId());
    assertEquals(a.getOpcode(), b.getOpcode());
    assertEquals(a.getTimeToLive(), b.getTimeToLive());
    assertEquals(a.getTargetUrl(), b.getTargetUrl());
    assertEquals(a.getArchivalId(), b.getArchivalId());
    assertEquals(a.getPollVersion(), b.getPollVersion());
    assertEquals(a.getChallenge(), b.getChallenge());
    assertEquals(a.getVerifier(), b.getVerifier());
    assertEquals(a.getHashed(), b.getHashed());
    assertEquals(a.getPluginVersion(), b.getPluginVersion());
    assertEquals(a.getHashAlgorithm(), b.getHashAlgorithm());
    ArrayList aVoteBlocks =
      new ArrayList(ListUtil.fromIterator(a.getVoteBlockIterator()));
    ArrayList bVoteBlocks =
      new ArrayList(ListUtil.fromIterator(b.getVoteBlockIterator()));
    assertSortedIsomorphic(aVoteBlocks, bVoteBlocks);

    // TODO: Figure out how to test time.

  }

  private V3LcapMessage makeTestVoteMessage() {
    return makeTestVoteMessage(null);
  }

  private V3LcapMessage makeTestVoteMessage(Collection voteBlocks) {
    V3LcapMessage msg = null;
    msg = new V3LcapMessage(V3LcapMessage.MSG_VOTE, m_testID, m_url,
			    123456789, 987654321, (byte)5, m_testBytes,
			    m_testBytes, m_testBytes, voteBlocks);

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
      V3LcapMessage.VoteBlock vb =
	new V3LcapMessage.VoteBlock("/test-" + ix + ".html", 1024, 0,
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

  private void assertSortedIsomorphic(List a, List b) {
    Collections.sort(a, m_comparator);
    Collections.sort(b, m_comparator);
    assertIsomorphic(a, b);
  }
}
