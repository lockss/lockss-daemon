/*
 * $Id: TestPollManager.java,v 1.59.2.1 2004-02-03 01:03:40 tlipkis Exp $
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

  protected IPAddr testaddr;
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
      BasePoll p1 = pollmanager.makePoll(testmsg[0]);
      // make sure we got the right type of poll here
      assertTrue(p1 instanceof V1NamePoll);
    }
    catch (IOException ex) {
      fail("unable to make a name poll");
    }

    // make a content poll
    try {
      BasePoll p2 = pollmanager.makePoll(testmsg[1]);
      // make sure we got the right type of poll here
      assertTrue(p2 instanceof V1ContentPoll);

    }
    catch (IOException ex) {
      fail("unable to make a content poll");
    }


    // make a verify poll
    try {
      BasePoll p3 = pollmanager.makePoll(testmsg[2]);
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
      Plugin plugin = testau.getPlugin();
      cus = plugin.makeCachedUrlSet(testau,
				    new RangeCachedUrlSetSpec(rooturls[1]));
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
      BasePoll p1 = pollmanager.makePoll(testmsg[0]);
      BasePoll p2 = pollmanager.findPoll(testmsg[0]);
      assertEquals(p1, p2);
    }
    catch (IOException ex) {
      fail("name poll couldn't be found");
    }
  }


  /** test for method removePoll(..) */
  public void testRemovePoll() {
    try {
      BasePoll p1 = pollmanager.makePoll(testmsg[0]);
      assertNotNull(p1);
      BasePoll p2 = pollmanager.removePoll(p1.m_key);
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
	CachedUrlSetSpec cuss =
	  new RangeCachedUrlSetSpec(urlstr, lwrbnd, uprbnd);
	Plugin plugin = testau.getPlugin();
	PollSpec spec =
	  new PollSpec(testau.getAuId(),
		       urlstr,lwrbnd,uprbnd,
		       plugin.makeCachedUrlSet(testau, cuss));
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
      BasePoll c1 = pollmanager.makePoll(sameroot[1]);
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
      BasePoll p1 = pollmanager.makePoll(testmsg[0]);

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
      assertTrue(pollmanager.isPollClosed(p1.m_key));
      assertFalse(pollmanager.isPollActive(p1.m_key));
      pollmanager.closeThePoll(p1.m_key);

   }
    catch (IOException ex) {
      fail("unable to make test poll");
    }
  }
  /** test for method suspendPoll(...) */

  public void testSuspendPoll() {
    BasePoll p1 = null;
    try {
      p1 = TestPoll.createCompletedPoll(theDaemon, testau, testmsg[0], 7, 2);
      pollmanager.addPoll(p1);
      // give it a pointless lock to avoid a null pointer
      p1.getVoteTally().setActivityLock(
          theDaemon.getActivityRegulator(testau).getAuActivityLock(-1, 123));
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

  public void testMockPollManager() {
    // This ensures that MockPollManager.canHashBeScheduledBefore() does
    // what I intended
    MockPollManager mpm = new MockPollManager();

    mpm.setMinPollDeadline(Deadline.in(1000));
    assertFalse(mpm.canHashBeScheduledBefore(100, Deadline.in(0)));
    assertTrue(mpm.canHashBeScheduledBefore(100, Deadline.in(1000)));
    assertTrue(mpm.canHashBeScheduledBefore(100, Deadline.in(1001)));

  }

  public void testCanSchedulePoll() {
    MockPollManager mpm = new MockPollManager();

    // Accept polls that finish no earlier than this
    mpm.setMinPollDeadline(Deadline.in(1000));
    // this one can't
    assertFalse(mpm.canSchedulePoll(500, 100));
    // can
    assertTrue(mpm.canSchedulePoll(2000, 100));
    // neededTime > duration
    assertFalse(mpm.canSchedulePoll(500, 600));
  }

  void configPollTimes() {
    Properties p = new Properties();
    addRequiredConfig(p);
    p.setProperty(PollManager.PARAM_NAMEPOLL_DEADLINE, "10000");
    p.setProperty(PollManager.PARAM_CONTENTPOLL_MIN, "1000");
    p.setProperty(PollManager.PARAM_CONTENTPOLL_MAX, "4100");
    p.setProperty(PollManager.PARAM_QUORUM, "5");
    p.setProperty(PollManager.PARAM_DURATION_MULTIPLIER_MIN, "3");
    p.setProperty(PollManager.PARAM_DURATION_MULTIPLIER_MAX, "7");
    p.setProperty(PollManager.PARAM_NAME_HASH_ESTIMATE, "1s");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testCalcDuration() {
    MockCachedUrlSet mcus =
      new MockCachedUrlSet((MockArchivalUnit)testau,
			   new RangeCachedUrlSetSpec("", "", ""));
    PollSpec ps = new PollSpec(mcus);
    MockPollManager mpm = new MockPollManager();

    configPollTimes();
    mpm.setBytesPerMsHashEstimate(100);
    mpm.setSlowestHashSpeed(100);

    mcus.setEstimatedHashDuration(100);
    mpm.setMinPollDeadline(Deadline.in(1000));
    assertEquals(1800, mpm.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus));
    mpm.setMinPollDeadline(Deadline.in(2000));
    assertEquals(2400, mpm.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus));
    // this one should be limited by max content poll
    mpm.setMinPollDeadline(Deadline.in(4000));
    assertEquals(4100, mpm.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus));
    mpm.setMinPollDeadline(Deadline.in(5000));
    assertEquals(-1, mpm.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus));

    // calulated poll time will be less than min, should be adjusted up to min
    mcus.setEstimatedHashDuration(10);
    mpm.setMinPollDeadline(Deadline.in(100));
    assertEquals(1000, mpm.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus));

    // name poll duration is randomized so less predictable, but should
    // always be between min and max.
    long ndur = mpm.calcDuration(LcapMessage.NAME_POLL_REQ, mcus);
    assertTrue(ndur >= mpm.m_minNamePollDuration);
    assertTrue(ndur <= mpm.m_maxNamePollDuration);
  }

  static class MockPollManager extends PollManager {
    long bytesPerMsHashEstimate = 0;
    long slowestHashSpeed = 0;
    Deadline minPollDeadline = Deadline.EXPIRED;

    boolean canHashBeScheduledBefore(long duration, Deadline when) {
      return !when.before(minPollDeadline);
    }
    void setMinPollDeadline(Deadline when) {
      minPollDeadline = when;
    }
    long getSlowestHashSpeed() {
      return slowestHashSpeed;
    }
    void setSlowestHashSpeed(long speed) {
      slowestHashSpeed = speed;
    }
    long getBytesPerMsHashEstimate() {
      return bytesPerMsHashEstimate;
    }
    void setBytesPerMsHashEstimate(long est) {
      bytesPerMsHashEstimate = est;
    }
  }

  private void initRequiredServices() {
    theDaemon = new MockLockssDaemon();
    pollmanager = theDaemon.getPollManager();

    theDaemon.getPluginManager();
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    ((MockArchivalUnit)testau).setPlugin(new MyMockPlugin());
    PluginUtil.registerArchivalUnit(testau);

    Properties p = new Properties();
    addRequiredConfig(p);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    theDaemon.getSchedService().startService();
    theDaemon.getHashService().startService();
    theDaemon.getRouterManager().startService();
    theDaemon.getActivityRegulator(testau).startService();

    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }

  private void addRequiredConfig(Properties p) {
    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
  }

  private void initTestAddr() {
    try {
      testaddr = IPAddr.getByName("127.0.0.1");
      testID = theDaemon.getIdentityManager().findIdentity(testaddr);
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
  }

  private void initTestMsg() {
    try {
      testmsg = new LcapMessage[3];

      Plugin plugin = testau.getPlugin();
      for(int i= 0; i<3; i++) {
	CachedUrlSetSpec cuss =
	  new RangeCachedUrlSetSpec(rooturls[i], lwrbnd, uprbnd);
        PollSpec spec = new PollSpec(testau.getAuId(),
                                     rooturls[i],lwrbnd, uprbnd,
                                     plugin.makeCachedUrlSet(testau, cuss));
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

  public class MyMockPlugin extends MockPlugin {
    public CachedUrlSet makeCachedUrlSet(ArchivalUnit owner,
					 CachedUrlSetSpec cuss) {
      return new PollTestPlugin.PTCachedUrlSet((MockArchivalUnit)owner, cuss);
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
