/*
 * $Id: TestMockLcapStreamRouter.java,v 1.1.2.3 2004-11-18 15:45:10 dshr Exp $
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
  LcapStreamRouter lsr1 = null;
  LcapStreamRouter lsr2 = null;
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
    lsr1 = new MockLcapStreamRouter(q1, q2);
    lsr2 = new MockLcapStreamRouter(q2, q1);
    lsr1.setConfig(config, config, null);
    lsr2.setConfig(config, config, null);
    {
      MockLcapStreamRouter mlsr1 = (MockLcapStreamRouter) lsr1;
      MockLcapStreamRouter mlsr2 = (MockLcapStreamRouter) lsr2;
      mlsr1.setPartner(lsr2);
      mlsr2.setPartner(lsr1);
    }
    lsr1.startService();
    lsr2.startService();
    // Register handlers
    handler1 = new MyMessageHandler();
    handler2 = new MyMessageHandler();
    lsr1.registerMessageHandler(handler1);
    lsr2.registerMessageHandler(handler2);
  }

  /** tearDown method for test case
   * @throws Exception if removePoll failed
   */
  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
    if (lsr1 != null) {
      lsr1.unregisterMessageHandler(handler1);
      lsr1.stopService();
      lsr1 = null;
    }
    if (lsr2 != null) {
      lsr2.unregisterMessageHandler(handler2);
      lsr2.stopService();
      lsr2 = null;
    }
  }


  public TestMockLcapStreamRouter(String msg){
    super(msg);
  }

  public void testMessageExchange() {
    {
      MockLcapStreamRouter mlsr1 = (MockLcapStreamRouter) lsr1;
      MockLcapStreamRouter mlsr2 = (MockLcapStreamRouter) lsr2;
      assertNotNull(mlsr1.getReceiveQueue());
      assertNotNull(mlsr1.getSendQueue());
      assertNotNull(mlsr2.getReceiveQueue());
      assertNotNull(mlsr2.getSendQueue());
      assertEquals(mlsr1.getReceiveQueue(), mlsr2.getSendQueue());
      assertEquals(mlsr2.getReceiveQueue(), mlsr1.getSendQueue());
    }
    // Create a message
    V3LcapMessage sent = null;
    try {
      sent = new MockV3LcapMessage();
    } catch (IOException ex) {
      fail("new MockV3LcapMessage() threw " + ex);
    }
    // Tell lsr1 to send it to p2
    try {
      lsr1.sendTo(sent, null, null);
    } catch (IOException ex) {
      fail("lsr1.sendTo() threw " + ex);
    }
    // Step time
    // Thread.yield();
    TimeBase.step(500);
    // Get message from handler
    LcapMessage received = handler2.getMessage();
    assertNotNull(received);
    assertTrue(received instanceof V3LcapMessage);
    assertEquals(sent, received);
    try {
      sent = new MockV3LcapMessage();
    } catch (IOException ex) {
      fail("new MockV3LcapMessage() threw " + ex);
    }
    try {
      lsr2.sendTo(sent, null, null);
    } catch (IOException ex) {
      fail("lsr2.sendTo() threw " + ex);
    }
    // Step time
    // Thread.yield();
    TimeBase.step(500);
    // Get message from handler
    received = handler1.getMessage();
    assertNotNull(received);
    assertTrue(received instanceof V3LcapMessage);
    assertEquals(sent, received);
  }

  public class MyMessageHandler implements LcapStreamRouter.MessageHandler {
    V3LcapMessage received = null;
    public MyMessageHandler() {
      received = null;
    }
    public void handleMessage(V3LcapMessage msg) {
      log.debug("handleMessage(" + msg);
      received = msg;
    }
    public LcapMessage getMessage() {
      return received;
    }
  }

}
