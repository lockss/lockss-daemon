/*
 * $Id$
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.mail;

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.mail.SmtpMailService
 */
public class TestSmtpMailService extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestSMS");

  private MockLockssDaemon daemon;
  MyMockSmtpMailService svc;
  MyMockSmtpClient client;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();

    client = new MyMockSmtpClient();
    svc = new MyMockSmtpMailService(client);
    daemon.setMailService(svc);
    daemon.setDaemonInited(true);
    svc.initService(daemon);
    Properties p = new Properties();
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }


  public void tearDown() throws Exception {
    TimeBase.setReal();
    if (svc != null) {
      svc.stopService();
      svc = null;
      client = null;
    }
    super.tearDown();
  }

  public void testEnqueue() throws IOException {
    MailMessage msg = new MockMailMessage("bawdeep");
    assertTrue(svc.sendMail("frump", "toop", msg));
    Queue q = svc.getQueue();
    assertTrue(svc.threadKicked);
    assertFalse(q.isEmpty());
    SmtpMailService.Req req = (SmtpMailService.Req)q.peek();
    assertEquals("frump", req.sender);
    assertEquals("toop", req.recipient);
    assertEquals(msg, req.msg);
  }

  // test that smtp client is called to send the message
  public void testDequeue() throws IOException, InterruptedException {
    svc.setRunThread(true);
    SimpleBinarySemaphore sem = client.getSentSem();
    MailMessage msg = new MockMailMessage("bawdeep");
    svc.sendMail("fff", "ttt", msg);
    sem.take(TIMEOUT_SHOULDNT);
    assertEquals("fff", client.sender);
    assertEquals("ttt", client.recipient);
    assertEquals(msg, client.msg);
  }

  // test that msg.delete() is called after the message is sent
  public void testDelete() throws IOException, InterruptedException {
    svc.setRunThread(true);
    SimpleBinarySemaphore sem = svc.getProcessSem();
    MockMailMessage msg = new MockMailMessage("bawdeep");
    assertFalse(msg.isDeleted());
    svc.sendMail("fff", "ttt", msg);
    sem.take(TIMEOUT_SHOULDNT);
    assertTrue(msg.isDeleted());
  }

  // test that msg.delete() is not called if message is requeued
  public void testNoDelete() throws IOException, InterruptedException {
    svc.setRunThread(true);
    SimpleBinarySemaphore sem = svc.getProcessSem();
    MockMailMessage msg = new MockMailMessage("bawdeep");
    assertFalse(msg.isDeleted());
    int[] results = {SmtpClient.RESULT_RETRY};
    client.setResults(results);
    svc.sendMail("fff", "ttt", msg);
    sem.take(TIMEOUT_SHOULDNT);
    assertFalse(msg.isDeleted());
  }

  public void testRateLimit() throws IOException, InterruptedException {
    ConfigurationUtil.setFromArgs(SmtpMailService.PARAM_MAX_MAIL_RATE, "3/10");
    TimeBase.setSimulated(100);
    svc.setRunThread(true);
    SimpleBinarySemaphore sem = client.getSentSem();
    MailMessage msg = new MockMailMessage("bawdeep");
    svc.sendMail("m1", "t", msg);
    svc.sendMail("m2", "t", msg);
    svc.sendMail("m3", "t", msg);
    svc.sendMail("m4", "t", msg);
    assertNSent(sem, 3);
    sem.take(TIMEOUT_SHOULD);
    assertFalse(client.senders.size() + " messages arrived, expected only 3",
	       client.senders.size() > 3);
    TimeBase.step(10);
    assertNSent(sem, 4);
  }

  public void testRetry() throws IOException, InterruptedException {
    MailMessage msg = new MockMailMessage("bawdeep");

    ConfigurationUtil.setFromArgs(SmtpMailService.PARAM_RETRY_INTERVAL,
				  "101");
    TimeBase.setSimulated(100);
    svc.setRunThread(true);
    SimpleBinarySemaphore sem = client.getSentSem();
    int[] results = {SmtpClient.RESULT_RETRY, SmtpClient.RESULT_OK};
    client.setResults(results);
    assertEquals(0, client.senders.size());
    svc.sendMail("m1", "t", msg);
    assertTrue("message wasn't sent", sem.take(TIMEOUT_SHOULDNT));
    assertEquals(1, client.senders.size());
    svc.sendMail("m2", "t", msg);
    assertTrue("message wasn't sent", sem.take(TIMEOUT_SHOULDNT));
    assertEquals(2, client.senders.size());
    Queue q = svc.getQueue();
    assertFalse(q.isEmpty());
    TimeBase.step(102);
    assertTrue("message wasn't sent", sem.take(TIMEOUT_SHOULDNT));
    assertEquals(ListUtil.list("m1", "m2", "m1"), client.senders);
    // thread may not have emptied queue yet.  need better way to test this
//     assertTrue(q.isEmpty());
  }

  public void testMaxQueuelen() throws IOException, InterruptedException {
    MailMessage msg = new MockMailMessage("bawdeep");
    ConfigurationUtil.setFromArgs(SmtpMailService.PARAM_MAX_QUEUELEN,
				  "2");
    TimeBase.setSimulated(100);
    Queue q = svc.getQueue();
    SimpleBinarySemaphore sem = client.getSentSem();
    assertTrue(svc.sendMail("m1", "t", msg));
    assertTrue(svc.sendMail("m2", "t", msg));
    assertEquals(2, q.size());
    assertFalse(svc.sendMail("m3", "t", msg));
    svc.setRunThread(true);
    // can't try to add another to queue until certain at least one of the
    // previous ones has been removed (which happens after send semaphore
    // is posted), so must wait until *second* is sent
    assertNSent(sem, 2);
    assertTrue(svc.sendMail("m4", "t", msg));
  }

  /** assert that n senders appear on the client's senders list, failing if
   * TIMEOUT_SHOULDNT elapses before that happens */
  void assertNSent(SimpleBinarySemaphore sem, int n) {
    assertNSent(sem, n, null);
  }

  /** assert that n senders appear on the client's senders list, failing if
   * TIMEOUT_SHOULDNT elapses before that happens */
  void assertNSent(SimpleBinarySemaphore sem, int n,
		   SimpleBinarySemaphore postSem) {
    while (client.senders.size() < n) {
      if (postSem != null) postSem.give();
      if (! sem.take(TIMEOUT_SHOULDNT)) {
	fail("only " + client.senders.size() + " messages arrived");
      }
    }
  }

  static class MyMockSmtpMailService extends SmtpMailService {
    MyMockSmtpClient client;
    IOException e;
    boolean threadKicked = false;
    boolean runThread = false;
    SimpleBinarySemaphore processSem;

    MyMockSmtpMailService(MyMockSmtpClient client) {
      this.client = client;
    }

    SmtpClient makeClient() throws IOException {
      if (e != null) throw e;
      return client;
    }

    synchronized void ensureThreadRunning() {
      threadKicked = true;
      if (runThread) super.ensureThreadRunning();
    }

    void setException(IOException e) {
      this.e = e;
    }

    void setRunThread(boolean runThread) {
      this.runThread = runThread;
      if (runThread && threadKicked) {
	ensureThreadRunning();
      }
    }

    void processReq(Req req) {
      super.processReq(req);
      if (processSem != null) processSem.give();
    }

    SimpleBinarySemaphore getProcessSem() {
      if (processSem == null) {
	processSem = new SimpleBinarySemaphore();
      }
      return processSem;
    }

    Queue getQueue() {
      return queue;
    }
  }

  static class MyMockSmtpClient extends SmtpClient {
    volatile String sender;
    volatile String recipient;
    volatile MailMessage msg;
    int[] results;
    int resIx;
    SimpleBinarySemaphore sentSem;
    SimpleBinarySemaphore waitSem;
    List senders = new ArrayList();
    RuntimeException e;

    MyMockSmtpClient() throws IOException {
      super();
    }

    public int sendMsg(String sender, String recipient, MailMessage msg) {
      if (false) super.sendMsg(sender, recipient, msg);
      if (e != null) throw e;
      if (waitSem != null) {
	if (!waitSem.take()) {
	  throw new RuntimeException("interrupted");
	}
      }
      senders.add(sender);
      this.sender = sender;
      this.recipient = recipient;
      this.msg = msg;
      if (sentSem != null) sentSem.give();
      if (results != null && resIx < results.length) {
	return results[resIx++];
      }
      return SmtpClient.RESULT_OK;
    }

    SimpleBinarySemaphore getSentSem() {
      if (sentSem == null) {
	sentSem = new SimpleBinarySemaphore();
      }
      return sentSem;
    }

    SimpleBinarySemaphore getWaitSem() {
      if (waitSem == null) {
	waitSem = new SimpleBinarySemaphore();
      }
      return waitSem;
    }

    void setException(RuntimeException e) {
      this.e = e;
    }

    void setResults(int[] results) {
      this.results = results;
      resIx = 0;
    }

  }
}
