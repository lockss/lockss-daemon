/*
 * $Id: TestLcapMessage.java,v 1.11 2003-02-20 00:57:28 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.Logger;
import org.lockss.test.MockCachedUrlSetSpec;
import java.net.*;
import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.lockss.test.*;
import gnu.regexp.*;
import org.lockss.poller.TestPoll;

/** JUnitTest case for class: org.lockss.protocol.Message */
public class TestLcapMessage extends TestCase {

  private static String urlstr = "http://www.test.org";
  private static byte[] testbytes = {1,2,3,4,5,6,7,8,9,10};
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static String[] testentries;

  private static MockLockssDaemon daemon = new MockLockssDaemon(null);
  private IdentityManager idmgr = daemon.getIdentityManager();
  protected InetAddress testaddr;
  protected LcapIdentity testID;
  protected LcapMessage testmsg;
  protected static String pluginID = "TestPlugin_1.0";

  public TestLcapMessage(String _name) {
    super(_name);
  }


  public void setUp() {

    try {
      testaddr = InetAddress.getByName("127.0.0.1");
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
    testmsg.m_hopCount = 2;

    testmsg.m_ttl = 5;
    testmsg.m_challenge = testbytes;
    testmsg.m_verifier = testbytes;
    testmsg.m_hashed = testbytes;
    testmsg.m_opcode = LcapMessage.CONTENT_POLL_REQ;
    testmsg.m_entries = testentries = TestPoll.makeEntries(1, 25);
    testmsg.m_pluginID = pluginID;

  }

  public void testEntriesTranslation() {
    String encstr = testmsg.entriesToString(10000);
    String[] decoded = testmsg.stringToEntries(encstr);
    assertTrue(Arrays.equals(testmsg.m_entries,decoded));

    //test our entries remainder by artificially setting our size to very small
    encstr = testmsg.entriesToString(50);
    decoded = testmsg.stringToEntries(encstr);
    assertNotNull(decoded);
    assertNotNull(testmsg.m_lwrRem);
    assertNotNull(testmsg.m_uprRem);

    encstr = testmsg.entriesToString(0);
    decoded = testmsg.stringToEntries(encstr);
    assertNull(decoded);
    assertEquals(testentries[0], testmsg.m_lwrRem);
    assertEquals(testmsg.m_uprBound, testmsg.m_uprRem);

  }


  public void testReplyMessageCreation() {
    LcapMessage rep_msg = null;

    try {
      rep_msg = LcapMessage.makeReplyMsg(testmsg,
                                         testbytes,
                                         testbytes,
                                         testentries,
                                         LcapMessage.CONTENT_POLL_REP,
                                         100000,
                                         testID);
    }
    catch(IOException ex) {
      fail("message reply creation failed");
    }

    // now test to see if we got back what we expected

    assertEquals(rep_msg.m_originAddr, testaddr);
    assertEquals(rep_msg.m_ttl,5);
    assertEquals(rep_msg.m_opcode,LcapMessage.CONTENT_POLL_REP);
    assertEquals(rep_msg.m_hashAlgorithm,testmsg.m_hashAlgorithm);
    assertEquals(rep_msg.m_pluginID, testmsg.m_pluginID);
    // TODO: figure out how to test time
    assertEquals(rep_msg.m_multicast ,false);
    assertEquals(rep_msg.m_hopCount,2);

    assertTrue(Arrays.equals(rep_msg.m_challenge,testmsg.m_challenge));
    assertTrue(Arrays.equals(rep_msg.m_verifier,testmsg.m_verifier));
    assertTrue(Arrays.equals(rep_msg.m_hashed,testmsg.m_hashed));
    assertTrue(Arrays.equals(rep_msg.m_entries,testentries));

  }

  public void testRequestMessageCreation() {
    LcapMessage req_msg = null;
    try {
      req_msg = LcapMessage.makeRequestMsg(urlstr,
                                           lwrbnd,
                                           uprbnd,
                                           testentries,
                                           testbytes,
                                           testbytes,
                                           LcapMessage.CONTENT_POLL_REQ,
                                           100000,
                                           testID,
                                           pluginID);
    }
    catch (Exception ex) {
      fail("message request creation failed.");
    }
    assertEquals(req_msg.m_originAddr, testaddr);
    assertEquals(req_msg.m_opcode,LcapMessage.CONTENT_POLL_REQ);
    assertEquals(req_msg.m_multicast ,false);
    assertEquals(req_msg.m_pluginID, pluginID);
    assertTrue(Arrays.equals(req_msg.m_challenge,testbytes));
    assertTrue(Arrays.equals(req_msg.m_verifier,testbytes));
    assertEquals(null,req_msg.m_hashed);
    assertTrue(Arrays.equals(req_msg.m_entries,testentries));
    assertEquals(req_msg.m_lwrBound, lwrbnd);
    assertEquals(req_msg.m_uprBound, uprbnd);

  }

  public void testMessageEncoding() {

    byte[] msgbytes = new byte[0];
    try {
      msgbytes = testmsg.encodeMsg();
    }
    catch (IOException ex) {
      fail("encode failed!");
    }

    try {
      LcapMessage msg = new LcapMessage(msgbytes);
      // now test to see if we got back what we started with
      assertEquals(msg.m_originAddr, testaddr);
      assertEquals(msg.m_ttl,5);
      assertEquals(msg.m_opcode,LcapMessage.CONTENT_POLL_REQ);
      // TODO: figure out how to test time
      assertEquals(msg.m_multicast ,false);
      assertEquals(msg.m_hopCount,1); // we decremented this on decode

      assertTrue(Arrays.equals(msg.m_challenge,testbytes));
      assertTrue(Arrays.equals(msg.m_verifier,testbytes));
      assertTrue(Arrays.equals(msg.m_hashed,testbytes));
      assertTrue(Arrays.equals(msg.m_entries,testentries));
      assertEquals(msg.m_lwrBound, lwrbnd);
      assertEquals(msg.m_uprBound, uprbnd);
    }
    catch (IOException ex) {
      fail("message decode failed");
    }
  }

  public void testMessageEncodingHandlesAllowableNulls(){
    testmsg.m_entries = null;
    testmsg.m_lwrBound = null;
    testmsg.m_uprBound = null;
    testmsg.m_lwrRem = null;
    testmsg.m_uprRem = null;
    try {
      testmsg.encodeMsg();
    }
    catch (IOException ex) {
      assertTrue("message encode with nulls failed!", true);
    }

  }

  public void testMessageDecodingHandlesAllowableNulls() throws IOException {
    testmsg.m_entries = null;
    testmsg.m_lwrBound = null;
    testmsg.m_uprBound = null;
    testmsg.m_lwrRem = null;
    testmsg.m_uprRem = null;
    byte[] msgbytes = testmsg.encodeMsg();

    LcapMessage msg = null;
    try {
      msg = new LcapMessage(msgbytes);
    }
    catch (IOException ex) {
      assertTrue("message decode with nulls failed!", true);
    }

    // now make sure we're still null
    assertNull(msg.m_entries);
    assertNull(msg.m_lwrBound);
    assertNull(msg.m_uprBound);
    assertNull(msg.m_lwrRem);
    assertNull(msg.m_uprRem);

  }


  /** Executes the test case */
  public static void main(String[] argv) {
    String[] testCaseList = {TestLcapMessage.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}

