/*
 * $Id: TestV3LcapMessage.java,v 1.12 2005-12-07 21:11:57 smorabito Exp $
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

  private String m_archivalID = "TestAU_1.0";
  private PeerIdentity m_testID;
  private V3LcapMessage m_testMsg;
  private List m_testVoteBlocks;
  private Comparator m_comparator;

  private LockssDaemon theDaemon;

  private byte[] m_testBytes =
    new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
      1, 2, 3, 4, 5, 6, 7, 8, 9, 0};

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
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID, m_testBytes,
                                                      m_testBytes);

    // now check the fields we expect to be valid
    assertEquals(V3LcapMessage.MSG_NO_OP, noopMsg.getOpcode());
    assertTrue(m_testID == noopMsg.getOriginatorId());
    assertEquals(m_testBytes, noopMsg.getPollerNonce());
    assertEquals(m_testBytes, noopMsg.getVoterNonce());
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
    assertFalse(noopMsg1.getPollerNonce() == noopMsg2.getPollerNonce());
    assertFalse(noopMsg1.getVoterNonce() == noopMsg2.getVoterNonce());
    assertEmpty(ListUtil.fromIterator(noopMsg1.getVoteBlockIterator()));
    assertEmpty(ListUtil.fromIterator(noopMsg2.getVoteBlockIterator()));
  }

  public void testNoOpMessageToString() throws IOException {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID,
                                                      m_testBytes,
                                                      m_testBytes);
    String expectedResult = "[V3LcapMessage: from " + m_testID.toString() +
      ", NoOp]";
    assertEquals(expectedResult, noopMsg.toString());
  }

  public void testTestMessageToString() throws IOException {
    String expectedResult = "[V3LcapMessage: from " + m_testID.toString() +
      ", http://www.example.com Vote " +
      "Key:key " +
      "PN:AQIDBAUGBwgJAAECAwQFBgcICQA= " +
      "VN:AQIDBAUGBwgJAAECAwQFBgcICQA= " +
      "B:10 ver 3]";
    assertEquals(expectedResult, m_testMsg.toString());
  }

  public void testNoOpEncoding() throws Exception {
    V3LcapMessage noopMsg = V3LcapMessage.makeNoOpMsg(m_testID,
                                                      m_testBytes,
                                                      m_testBytes);
    byte[] encodedBytes = noopMsg.encodeMsg();

    V3LcapMessage msg = new V3LcapMessage(encodedBytes);
    // now test to see if we got back what we started with
    assertTrue(m_testID == msg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_NO_OP, msg.getOpcode());
    assertEquals(m_testBytes, msg.getPollerNonce());
    assertEquals(m_testBytes, msg.getVoterNonce());
  }

  public void testRequestMessageCreation() throws Exception {
    PollSpec spec =
      new MockPollSpec("ArchivalID_2", "http://foo.com/", null, null,
                       "Plug42", Poll.V3_POLL);
    V3LcapMessage reqMsg =
      V3LcapMessage.makeRequestMsg(spec,
                                   "key",
				   m_testBytes,
                                   m_testBytes,
				   V3LcapMessage.MSG_REPAIR_REQ,
				   10000,
				   m_testID);

    for (Iterator ix = m_testVoteBlocks.iterator(); ix.hasNext(); ) {
      reqMsg.addVoteBlock((VoteBlock)ix.next());
    }

    assertEquals(3, spec.getProtocolVersion());
    assertEquals(3, reqMsg.getProtocolVersion());
    assertEquals("Plug42", reqMsg.getPluginVersion());
    assertTrue(m_testID == reqMsg.getOriginatorId());
    assertEquals(V3LcapMessage.MSG_REPAIR_REQ, reqMsg.getOpcode());
    assertEquals("ArchivalID_2", reqMsg.getArchivalId());
    assertEquals("http://foo.com/", reqMsg.getTargetUrl());
    assertEquals(m_testBytes, reqMsg.getPollerNonce());
    assertEquals(m_testBytes, reqMsg.getVoterNonce());
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
			 computeHash(fName), computeHash(fName),
                         VoteBlock.CONTENT_VOTE);
  }

  private void assertEqualMessages(V3LcapMessage a, V3LcapMessage b) {
    assertTrue(a.getOriginatorId() == b.getOriginatorId());
    assertEquals(a.getOpcode(), b.getOpcode());
    assertEquals(a.getTargetUrl(), b.getTargetUrl());
    assertEquals(a.getArchivalId(), b.getArchivalId());
    assertEquals(a.getProtocolVersion(), b.getProtocolVersion());
    assertEquals(a.getPollerNonce(), b.getPollerNonce());
    assertEquals(a.getVoterNonce(), b.getVoterNonce());
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

  private V3LcapMessage makeTestVoteMessage(Collection voteBlocks) {
    V3LcapMessage msg = new V3LcapMessage(V3LcapMessage.MSG_VOTE, "key",
                                          m_testID,
                                          m_url, 123456789, 987654321,
                                          m_testBytes, m_testBytes);

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
      byte[] hash = computeHash(fileName);
      VoteBlock vb =
	new VoteBlock("/test-" + ix + ".html", 1024, 0,
		      1024, 0, hash, hash, VoteBlock.CONTENT_VOTE);
      if (log.isDebug2()) {
	log.debug2("Creating voteblock: " + vb);
      }
      vbList.add(vb);
    }
    return vbList;
  }

  private byte[] computeHash(String s) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA");
      digest.update(s.getBytes());
      byte[] hashed = digest.digest();
      return hashed;
    } catch (java.security.NoSuchAlgorithmException e) {
      return new byte[0];
    }
  }
}
