/*
 * $Id: TestMockLcapStreamRouter.java,v 1.1.2.5 2004-11-28 23:08:32 dshr Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import junit.framework.TestCase;
import java.util.*;
import java.net.*;
import java.io.*;
import org.lockss.test.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.config.*;
import java.io.IOException;

public class TestMockLcapStreamRouter extends LockssTestCase{
  MockLcapStreamRouter loopbackRouter = null;
  MockLcapStreamRouter bitbucketRouter = null;
  MyMessageHandler handler1 = null;
  MyMessageHandler handler2 = null;
  static Logger log = Logger.getLogger("TestMockLcapStreamRouter");

  protected void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Configuration config = Configuration.getCurrentConfig();
    // Make two FifoQueue objects
    FifoQueue q1 = new FifoQueue();
    FifoQueue q2 = new FifoQueue();
    assertNotNull(q1);
    assertNotNull(q2);
    // Use the two to connect two MockLcapStreamRouter objects
    // back-to-back.
    loopbackRouter = new MockLcapStreamRouter(q1, null);
    bitbucketRouter = new MockLcapStreamRouter(q1, q2);
    loopbackRouter.setConfig(config, config, null);
    bitbucketRouter.setConfig(config, config, null);
    loopbackRouter.startService();
    bitbucketRouter.startService();
    // Register handlers
    handler1 = new MyMessageHandler("handler1");
    handler2 = new MyMessageHandler("handler2");
    loopbackRouter.registerMessageHandler(handler1);
    bitbucketRouter.registerMessageHandler(handler2);
  }

  /** tearDown method for test case
   * @throws Exception if removePoll failed
   */
  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
    if (loopbackRouter != null) {
      loopbackRouter.unregisterMessageHandler(handler1);
      loopbackRouter.stopService();
      loopbackRouter = null;
    }
    if (bitbucketRouter != null) {
      bitbucketRouter.unregisterMessageHandler(handler2);
      bitbucketRouter.stopService();
      bitbucketRouter = null;
    }
  }


  public TestMockLcapStreamRouter(String msg){
    super(msg);
  }

  public void testLoopbackRouter() {
    {
      MockLcapStreamRouter mloopbackRouter = (MockLcapStreamRouter) loopbackRouter;
      assertNotNull(mloopbackRouter.getReceiveQueue());
      assertNull(mloopbackRouter.getSendQueue());
    }
    // Create a message
    MockV3LcapMessage sent = null;
    try {
      sent = new MockV3LcapMessage();
    } catch (IOException ex) {
      fail("new MockV3LcapMessage() threw " + ex);
    }
    // Tell loopbackRouter to send it to p2
    try {
      loopbackRouter.sendTo(sent, null, null);
    } catch (IOException ex) {
      fail("loopbackRouter.sendTo() threw " + ex);
    }
    // Step time
    // Thread.yield();
    TimeBase.step(500);
    // Get message from handler
    LcapMessage received = handler1.getMessage();
    assertNotNull(received);
    assertTrue(received instanceof V3LcapMessage);
    assertEquals(sent, received);
  }

  public void testBitbucketRouter() {
    assertTrue(bitbucketRouter.sendQueueEmpty());
    // Create a message
    MockV3LcapMessage sent = null;
    try {
      sent = new MockV3LcapMessage();
    } catch (IOException ex) {
      fail("new MockV3LcapMessage() threw " + ex);
    }
    try {
      bitbucketRouter.sendTo(sent, null, null);
    } catch (IOException ex) {
      fail("bitbucketRouter.sendTo() threw " + ex);
    }
    // Step time
    // Thread.yield();
    TimeBase.step(500);
    // Get message from handler
    assertFalse(bitbucketRouter.sendQueueEmpty());
    Deadline dl = Deadline.in(10);
    V3LcapMessage received = bitbucketRouter.getSentMessage(dl);
    assertNotNull(received);
    assertTrue(received instanceof V3LcapMessage);
    assertEquals(sent, received);
    assertTrue(bitbucketRouter.sendQueueEmpty());
  }

  public void testKeyMapping() {
    // Create a message
    MockV3LcapMessage sent = null;
    byte[] key1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    byte[] key2 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };
    try {
      sent = new MockV3LcapMessage();
      sent.setChallenge(key1);
    } catch (IOException ex) {
      fail("new MockV3LcapMessage() threw " + ex);
    }
    loopbackRouter.setKeyMap(key1, key2);
    // Tell loopbackRouter to send it to p2
    try {
      loopbackRouter.sendTo(sent, null, null);
    } catch (IOException ex) {
      fail("loopbackRouter.sendTo() threw " + ex);
    }
    // Step time
    // Thread.yield();
    TimeBase.step(500);
    // Get message from handler
    LcapMessage received = handler1.getMessage();
    assertNotNull(received);
    assertTrue(received instanceof V3LcapMessage);
    assertEquals(sent, received);
    assertEquals(key2, received.getChallenge());
  }

  public class MyMessageHandler implements LcapStreamRouter.MessageHandler {
    V3LcapMessage received = null;
    String handlerName = null;
    public MyMessageHandler(String n) {
      received = null;
      handlerName = n;
    }
    public void handleMessage(V3LcapMessage msg) {
      log.debug(handlerName + ": handleMessage(" + msg + ")");
      received = msg;
    }
    public LcapMessage getMessage() {
      log.debug(handlerName + ": getMessage() -> " + received);
      return received;
    }
  }

}
