/*
 * $Id: TestPollManager.java,v 1.49 2003-06-26 01:50:27 clairegriffin Exp $
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

package org.lockss.poller;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.mortbay.util.*;
import org.lockss.hasher.HashService;
import org.lockss.repository.TestLockssRepositoryImpl;
import org.lockss.repository.*;
/** JUnitTest case for class: org.lockss.poller.PollManager */
public class TestPollManager extends LockssTestCase {

  private static String[] rooturls = {"http://www.test.org",
    "http://www.test1.org",
    "http://www.test2.org"};

  private static String urlstr = "http://www.test3.org";
  private static String lwrbnd = "test1.doc";
  private static String uprbnd = "test3.doc";
  private static long testduration = Constants.HOUR;

  private static ArrayList testentries = (ArrayList)ListUtil.list(
      new PollTally.NameListEntry(true,"test1.doc"),
      new PollTally.NameListEntry(true,"test2.doc"),
      new PollTally.NameListEntry(true,"test3.doc"));

  protected static ArchivalUnit testau;
  private MockLockssDaemon theDaemon;

  protected InetAddress testaddr;
  protected LcapIdentity testID;
  protected LcapMessage[] testmsg;
  protected PollManager pollmanager;

  protected void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
    initRequiredServices();
    initTestAddr();
    initTestMsg();
  }


  public void tearDown() throws Exception {
    pollmanager.stopService();
    theDaemon.getLockssRepository(testau).stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getRouterManager().stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  /** test for method makePoll(..) */
  public void testMakePoll() {
    // make a name poll
    try {
      Poll p1 = pollmanager.makePoll(testmsg[0]);
      // make sure we got the right type of poll here
      assertTrue(p1 instanceof V1NamePoll);
    }
    catch (IOException ex) {
      fail("unable to make a name poll");
    }

    // make a content poll
    try {
      Poll p2 = pollmanager.makePoll(testmsg[1]);
      // make sure we got the right type of poll here
      assertTrue(p2 instanceof V1ContentPoll);

    }
    catch (IOException ex) {
      fail("unable to make a content poll");
    }


    // make a verify poll
    try {
      Poll p3 = pollmanager.makePoll(testmsg[2]);
      // make sure we got the right type of poll here
      assertTrue(p3 instanceof V1VerifyPoll);
    }
    catch (IOException ex) {
      fail("unable to make a verify poll");
    }

  }

  /** test for method makePollRequest(..) */
  public void testMakePollRequest() {
    try {
      CachedUrlSet cus = null;
      cus = testau.makeCachedUrlSet(new RangeCachedUrlSetSpec(rooturls[1]));
      PollSpec spec = new PollSpec(cus, lwrbnd, uprbnd);
      pollmanager.sendPollRequest(LcapMessage.VERIFY_POLL_REQ, spec);
    }
    catch (IllegalStateException e) {
      // ignore this for now
    }
    catch (IOException ex) {
      fail("unable to make a poll request message");
    }
  }

  /** test for method findPoll(..) */
  public void testFindPoll() {
    // lets see if we can find our name poll
    try {
      Poll p1 = pollmanager.makePoll(testmsg[0]);
      Poll p2 = pollmanager.findPoll(testmsg[0]);
      assertEquals(p1, p2);
    }
    catch (IOException ex) {
      fail("name poll couldn't be found");
    }
  }


  /** test for method removePoll(..) */
  public void testRemovePoll() {
    try {
      Poll p1 = pollmanager.makePoll(testmsg[0]);
      assertNotNull(p1);
      Poll p2 = pollmanager.removePoll(p1.m_key);
      assertEquals(p1, p2);
    }
    catch (IOException ex) {
      fail("name poll couldn't be found");
    }

  }

  /** test for method checkForConflicts(..) */
  public void testCheckForConflicts() {
    // lets try to run two content polls in the same location

   LcapMessage[] sameroot = new LcapMessage[3];

    try {
      for(int i= 0; i<3; i++) {
        PollSpec spec =
	  new PollSpec(testau.getAUId(),
		       urlstr,lwrbnd,uprbnd,
		       testau.makeCachedUrlSet(new RangeCachedUrlSetSpec(urlstr,
          lwrbnd, uprbnd)));
        sameroot[i] =  LcapMessage.makeRequestMsg(
          spec,
          testentries,
          pollmanager.generateRandomBytes(),
          pollmanager.generateRandomBytes(),
          LcapMessage.NAME_POLL_REQ + (i * 2),
          testduration,
          testID);
      }
    }
    catch (IOException ex) {
      fail("Unable to make test messages");
    }

    // check content poll conflicts
    try {
      Poll c1 = pollmanager.makePoll(sameroot[1]);
      // differnt content poll should be ok

      CachedUrlSet cus = pollmanager.checkForConflicts(testmsg[1],
          makeCachedUrlSet(testmsg[1]));
      assertNull("different content poll s/b ok", cus);

      // same content poll same range s/b a conflict
      cus = pollmanager.checkForConflicts(sameroot[1],
          makeCachedUrlSet(sameroot[1]));
      assertNotNull("same content poll root s/b conflict", cus);

      // different name poll should be ok
      cus = pollmanager.checkForConflicts(testmsg[0],
          makeCachedUrlSet(testmsg[0]));
      assertNull("name poll with different root s/b ok", cus);

      // same name poll s/b conflict
      cus = pollmanager.checkForConflicts(sameroot[0],
          makeCachedUrlSet(sameroot[0]));
      assertNotNull("same name poll root s/b conflict", cus);

      // verify poll should be ok
      cus = pollmanager.checkForConflicts(testmsg[2],
          makeCachedUrlSet(testmsg[2]));
      assertNull("verify poll s/b ok", cus);

      // remove the poll
      pollmanager.removePoll(c1.m_key);
    }
    catch (IOException ex) {
      fail("unable to make content poll");
    }
  }


  /** test for method closeThePoll(..) */
  public void testCloseThePoll() {
    try {
      Poll p1 = pollmanager.makePoll(testmsg[0]);

      // we should now be active
      assertTrue(pollmanager.isPollActive(p1.m_key));
      // we should not be closed
      assertFalse(pollmanager.isPollClosed(p1.m_key));


      pollmanager.closeThePoll(p1.m_key);
      // we should not be active
      assertFalse(pollmanager.isPollActive(p1.m_key));
      // we should now be closed
      assertTrue(pollmanager.isPollClosed(p1.m_key));
      // we should reject an attempt to handle a packet with this key
      pollmanager.handleIncomingMessage(testmsg[0]);
      assertFalse(pollmanager.isPollActive(p1.m_key));

   }
    catch (IOException ex) {
      fail("unable to make test poll");
    }
  }
  /** test for method suspendPoll(...) */

  public void testSuspendPoll() {
    Poll p1 = null;
    try {
      p1 = TestPoll.createCompletedPoll(theDaemon, testau, testmsg[0], 7, 2);
      pollmanager.addPoll(p1);

    }
    catch (Exception ex) {
      fail("unable to make a test poll");
    }

    // check our suspend
    pollmanager.suspendPoll(p1.m_key);
    assertTrue(pollmanager.isPollSuspended(p1.m_key));
    assertFalse(pollmanager.isPollClosed(p1.m_key));

    // now we resume...
    pollmanager.resumePoll(false, p1.m_key);
    assertFalse(pollmanager.isPollSuspended(p1.m_key));
  }


  /** test for method getHasher(..) */
  public void testGetHasher() {
    MessageDigest md = pollmanager.getHasher(null);
    assertNotNull(md);
  }

  /** test for method makeVerifier(..) */
  public void testMakeVerifier() {
    // test for make verifier - this will also store the verify/secret pair
    byte[] verifier = pollmanager.makeVerifier(10000);
    assertNotNull("unable to make and store a verifier", verifier);

    // retrieve our secret
    byte[] secret = pollmanager.getSecret(verifier);
    assertNotNull("unable to retrieve secret for verifier", secret);

    // confirm that the verifier is the hash of the secret
    MessageDigest md = pollmanager.getHasher(null);
    md.update(secret, 0, secret.length);
    byte[] verifier_check = md.digest();
    assertTrue("secret does not match verifier",
               Arrays.equals(verifier, verifier_check));

  }

  public void testCanSchedulePoll() {

   long pollTime = Constants.DAY;
   long neededTime = pollTime/4;

   CachedUrlSet cus = null;
   cus = testau.makeCachedUrlSet(new RangeCachedUrlSetSpec(rooturls[1]));
   PollSpec spec = new PollSpec(cus, lwrbnd, uprbnd);
   assertTrue(spec.canSchedulePoll(pollTime,neededTime,pollmanager));

  }

  private void initRequiredServices() {
    theDaemon = new MockLockssDaemon();
    pollmanager = theDaemon.getPollManager();

    theDaemon.getPluginManager();
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    PluginUtil.registerArchivalUnit(testau);

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

    theDaemon.getHashService().startService();
    theDaemon.getRouterManager().startService();

    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }

  private void initTestAddr() {
    try {
      testaddr = InetAddress.getByName("127.0.0.1");
      testID = theDaemon.getIdentityManager().findIdentity(testaddr);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
  }

  private void initTestMsg() {
    try {
      testmsg = new LcapMessage[3];

      for(int i= 0; i<3; i++) {
        PollSpec spec = new PollSpec(testau.getAUId(),
                                     rooturls[i],lwrbnd, uprbnd,
                                     testau.makeCachedUrlSet(
            new RangeCachedUrlSetSpec(rooturls[i], lwrbnd, uprbnd)));
        testmsg[i] =  LcapMessage.makeRequestMsg(
          spec,
          testentries,
          pollmanager.generateRandomBytes(),
          pollmanager.generateRandomBytes(),
          LcapMessage.NAME_POLL_REQ + (i * 2),
          testduration,
          testID);
      }
    }
    catch (IOException ex) {
      fail("can't create test message" + ex.toString());
    }
  }

  private CachedUrlSet makeCachedUrlSet(LcapMessage msg) {

    try {
      PollSpec ps = new PollSpec(msg);
      return theDaemon.getPluginManager().findCachedUrlSet(ps);
    }
    catch (Exception ex) {
      return null;
    }
  }

  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {TestPollManager.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
