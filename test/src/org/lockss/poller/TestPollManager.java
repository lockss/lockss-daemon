/*
 * $Id: TestPollManager.java,v 1.64 2004-09-20 14:20:40 dshr Exp $
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
import java.net.UnknownHostException;
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

  protected PeerIdentity testID;
  protected LcapMessage[] testmsg;
  protected PollManager pollmanager;
  protected IdentityManager idmanager;

  protected void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.1.2.3");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    TimeBase.setSimulated();
    initRequiredServices();
    initTestAddr();
    initTestMsg();
  }


  public void tearDown() throws Exception {
    pollmanager.stopService();
    idmanager.stopService();
    theDaemon.getLockssRepository(testau).stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getRouterManager().stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  // Tests for the V1 PollFactory implementation

  // Start by testing the local mock poll factory

  public void testLocalMockV1PollFactory() {
    // This ensures that LocalMockV1PollFactory.canHashBeScheduledBefore() does
    // what I intended
    LocalMockPollManager mpm = new LocalMockPollManager();
    LocalMockV1PollFactory mpf = new LocalMockV1PollFactory();

    mpf.setMinPollDeadline(Deadline.in(1000));
    assertFalse(mpf.canHashBeScheduledBefore(100, Deadline.in(0), mpm));
    assertTrue(mpf.canHashBeScheduledBefore(100, Deadline.in(1000), mpm));
    assertTrue(mpf.canHashBeScheduledBefore(100, Deadline.in(1001), mpm));

  }

  public void testV1CallPoll() {
    LocalMockPollManager pm = new LocalMockPollManager();
    PollFactory pf = pm.getPollFactory(1);
    int[] opcode = {
      LcapMessage.NAME_POLL_REQ,
      LcapMessage.CONTENT_POLL_REQ,
      LcapMessage.VERIFY_POLL_REQ,
    };
    // NB - Verify polls are never called via callPoll()
    for (int i = 0; i < testmsg.length - 1; i++) {
      pm.msgSent = null;
      assertTrue("Calling this poll should succeed",
		 pf.callPoll(new PollSpec(testmsg[i]), pm,
			     idmanager));
      assertTrue(pm.msgSent instanceof LcapMessage);
      assertEquals(pm.msgSent.getOpcode(), opcode[i]);
    }
  }

  public void testV1CreatePoll() {
    PollFactory pf = pollmanager.getPollFactory(1);
    PollSpec[] ps = new PollSpec[3];
    for (int i = 0; i < testmsg.length; i++) {
      ps[i] = new PollSpec(testmsg[i]);
      try {
	BasePoll p = pf.createPoll(testmsg[i], ps[i], pollmanager,
				   idmanager);
	assertTrue(p instanceof V1Poll);
	// assertTrue(p.isMyPoll());
	assertTrue(p.getMessage() == testmsg[i]);
	assertTrue(p.getPollSpec().equals(ps[i]));
	assertTrue(p.getCallerID() == testmsg[i].getOriginatorID());
      } catch (ProtocolException pe) {
	fail("createPoll " + testmsg[i] + " threw " + pe);
      }
    }
  }

  /** test for V1 method PollFactory.pollShouldBeCalled(..) */
  public void testV1PollShouldBeCalled() throws Exception {
    // lets try to run two V1 content polls in the same location
    PollFactory pf = pollmanager.getPollFactory(1);

   LcapMessage[] sameroot = new LcapMessage[3];
   PollSpec[] spec = new PollSpec[3];
   int[] pollType = {
     Poll.NAME_POLL,
     Poll.CONTENT_POLL,
     Poll.VERIFY_POLL,
   };

   for(int i= 0; i<3; i++) {
     spec[i] = new MockPollSpec(testau, urlstr, lwrbnd, uprbnd, pollType[i]);
     sameroot[i] =
       LcapMessage.makeRequestMsg(spec[i],
				  testentries,
				  pollmanager.generateRandomBytes(),
				  pollmanager.generateRandomBytes(),
				  LcapMessage.NAME_POLL_REQ + (i * 2),
				  testduration,
				  testID);
   }

   // check content poll conflicts
   BasePoll c1 = pollmanager.makePoll(sameroot[1]);
   // differnt content poll should be ok

   assertTrue("different content poll s/b ok",
	      pf.pollShouldBeCreated(testmsg[1], new PollSpec(testmsg[1]),
				     pollmanager, idmanager));

   // same content poll same range s/b a conflict
   assertFalse("same content poll root s/b conflict",
	       pf.pollShouldBeCreated(sameroot[1], new PollSpec(sameroot[1]),
				     pollmanager, idmanager));

   // different name poll should be ok
   assertTrue("different name poll s/b ok",
	      pf.pollShouldBeCreated(testmsg[0], new PollSpec(testmsg[0]),
				     pollmanager, idmanager));

   // same name poll s/b conflict
   assertFalse("same name poll root s/b conflict",
	       pf.pollShouldBeCreated(sameroot[0], new PollSpec(sameroot[0]),
				     pollmanager, idmanager));

   // verify poll should be ok
   assertTrue("verify poll s/b ok",
	      pf.pollShouldBeCreated(testmsg[2], new PollSpec(testmsg[2]),
				     pollmanager, idmanager));

   // remove the poll
   pollmanager.removePoll(c1.m_key);
  }

  public void testGetPollActivity() {
    PollFactory pf = pollmanager.getPollFactory(1);
    assertEquals(pf.getPollActivity(testmsg[0],
				    new PollSpec(testmsg[0]),
				    pollmanager),
		 ActivityRegulator.STANDARD_NAME_POLL);
    int pa = pf.getPollActivity(testmsg[1],new PollSpec(testmsg[1]),
				pollmanager);
    assertTrue(pa == ActivityRegulator.STANDARD_CONTENT_POLL ||
	       pa == ActivityRegulator.SINGLE_NODE_CONTENT_POLL);
  }

  // Test for the V1 method PollFactory.calcDuration(...)
  public void testV1CalcDuration() {
    MockCachedUrlSet mcus =
      new MockCachedUrlSet((MockArchivalUnit)testau,
			   new RangeCachedUrlSetSpec("", "", ""));
    PollSpec ps = new PollSpec(mcus, Poll.CONTENT_POLL);
    LocalMockPollManager mpm = new LocalMockPollManager();
    LocalMockV1PollFactory pf = new LocalMockV1PollFactory();
    mpm.setPollFactory(1, pf);

    configPollTimes();
    pf.setBytesPerMsHashEstimate(100);
    pf.setSlowestHashSpeed(100);

    mcus.setEstimatedHashDuration(100);
    pf.setMinPollDeadline(Deadline.in(1000));
    assertEquals(1800, pf.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus, mpm));
    pf.setMinPollDeadline(Deadline.in(2000));
    assertEquals(2400, pf.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus, mpm));
    // this one should be limited by max content poll
    pf.setMinPollDeadline(Deadline.in(4000));
    assertEquals(4100, pf.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus, mpm));
    pf.setMinPollDeadline(Deadline.in(5000));
    assertEquals(-1, pf.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus, mpm));

    // calulated poll time will be less than min, should be adjusted up to min
    mcus.setEstimatedHashDuration(10);
    pf.setMinPollDeadline(Deadline.in(100));
    assertEquals(1000, pf.calcDuration(LcapMessage.CONTENT_POLL_REQ, mcus, mpm));

    // name poll duration is randomized so less predictable, but should
    // always be between min and max.
    long ndur = pf.calcDuration(LcapMessage.NAME_POLL_REQ, mcus, mpm);
    assertTrue(ndur >= pf.getMinNamePollDuration());
    assertTrue(ndur <= pf.getMaxNamePollDuration());
  }

  public void testV1CanSchedulePoll() {
    LocalMockPollManager mpm = new LocalMockPollManager();
    LocalMockV1PollFactory pf = new LocalMockV1PollFactory();

    mpm.setPollFactory(1, pf);
    // Accept polls that finish no earlier than this
    pf.setMinPollDeadline(Deadline.in(1000));
    // this one can't
    assertFalse(pf.canSchedulePoll(500, 100, mpm));
    // can
    assertTrue(pf.canSchedulePoll(2000, 100, mpm));
    // neededTime > duration
    assertFalse(pf.canSchedulePoll(500, 600, mpm));
  }

  //  Now test the PollManager itself

  /** test for method makePoll(..) */
  public void testMakePoll() throws Exception {
    // make a name poll
    BasePoll p1 = pollmanager.makePoll(testmsg[0]);
    // make sure we got the right type of poll here
    assertTrue(p1 instanceof V1NamePoll);

    // make a content poll
    BasePoll p2 = pollmanager.makePoll(testmsg[1]);
    // make sure we got the right type of poll here
    assertTrue(p2 instanceof V1ContentPoll);

    // make a verify poll
    BasePoll p3 = pollmanager.makePoll(testmsg[2]);
    // make sure we got the right type of poll here
    assertTrue(p3 instanceof V1VerifyPoll);
  }

  public void testMakePollDoesntIfPluginMismatch() throws Exception {
    // Make a string that's different from the plugin's version
    String bogus = testau.getPlugin().getVersion() + "cruft";

    // make a name poll witha bogus plugin version
    MockPollSpec spec =
      new MockPollSpec(testau, urlstr, lwrbnd, uprbnd, Poll.NAME_POLL);
    spec.setPluginVersion(bogus);
    LcapMessage msg1 =
      LcapMessage.makeRequestMsg(spec,
				 testentries,
				 pollmanager.generateRandomBytes(),
				 pollmanager.generateRandomBytes(),
				 LcapMessage.NAME_POLL_REQ,
				 testduration,
				 testID);

    BasePoll p1 = pollmanager.makePoll(msg1);
    assertNull("Shouldn't create poll with plugin version mismatch", p1);

    // make a content poll witha bogus plugin version
    LcapMessage msg2 =
      LcapMessage.makeRequestMsg(spec,
				 testentries,
				 pollmanager.generateRandomBytes(),
				 pollmanager.generateRandomBytes(),
				 LcapMessage.CONTENT_POLL_REQ,
				 testduration,
				 testID);

    BasePoll p2 = pollmanager.makePoll(msg2);
    assertNull("Shouldn't create poll with plugin version mismatch", p2);
  }

  /** test for method makePollRequest(..) */
  public void testMakePollRequest() throws Exception {
    try {
      CachedUrlSet cus = null;
      Plugin plugin = testau.getPlugin();
      cus = plugin.makeCachedUrlSet(testau,
				    new RangeCachedUrlSetSpec(rooturls[1]));
      PollSpec spec = new PollSpec(cus, lwrbnd, uprbnd, Poll.CONTENT_POLL);
      assertTrue(pollmanager.callPoll(spec));
    }
    catch (IllegalStateException e) {
      // ignore this for now
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

  /** test for method closeThePoll(..) */
  public void testCloseThePoll() throws Exception {
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

  /** test for method suspendPoll(...) */
  public void testSuspendPoll() throws Exception {
    BasePoll p1 = null;
    p1 = TestPoll.createCompletedPoll(theDaemon, testau, testmsg[0], 7, 2);
    pollmanager.addPoll(p1);
    // give it a pointless lock to avoid a null pointer
    p1.getVoteTally().
      setActivityLock(theDaemon.getActivityRegulator(testau).
		      getAuActivityLock(-1, 123));

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


  void configPollTimes() {
    Properties p = new Properties();
    addRequiredConfig(p);
    p.setProperty(V1PollFactory.PARAM_NAMEPOLL_DEADLINE, "10000");
    p.setProperty(V1PollFactory.PARAM_CONTENTPOLL_MIN, "1000");
    p.setProperty(V1PollFactory.PARAM_CONTENTPOLL_MAX, "4100");
    p.setProperty(V1PollFactory.PARAM_QUORUM, "5");
    p.setProperty(V1PollFactory.PARAM_DURATION_MULTIPLIER_MIN, "3");
    p.setProperty(V1PollFactory.PARAM_DURATION_MULTIPLIER_MAX, "7");
    p.setProperty(V1PollFactory.PARAM_NAME_HASH_ESTIMATE, "1s");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  //  Local mock classes

  // LocalMockPollManager allows us to override the PollFactory
  // used for a particular protocol,  and to override the
  // sendMessage() method.
  static class LocalMockPollManager extends PollManager {
    LcapMessage msgSent = null;
    void setPollFactory(int i, PollFactory fact) {
      pf[i] = fact;
    }
    void sendMessage(LcapMessage msg, ArchivalUnit au) throws IOException {
      msgSent = msg;
    }
  }

  // LocalMockV1PollFactory allows us to override the
  // canHashBeScheduledBefore() method and avoid the
  // complexity of mocking the hasher and scheduler.
  static class LocalMockV1PollFactory extends V1PollFactory {
    long bytesPerMsHashEstimate = 0;
    long slowestHashSpeed = 0;
    Deadline minPollDeadline = Deadline.EXPIRED;

    boolean canHashBeScheduledBefore(long duration,
				     Deadline when,
				     PollManager pm) {
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
    idmanager = theDaemon.getIdentityManager();

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
    idmanager.startService();
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
      testID = theDaemon.getIdentityManager().stringToPeerIdentity("127.0.0.1");
    }
    catch (UnknownHostException ex) {
      fail("can't open test host");
    }
  }

  private void initTestMsg() throws Exception {
    testmsg = new LcapMessage[3];
    int[] pollType = {
      Poll.NAME_POLL,
      Poll.CONTENT_POLL,
      Poll.VERIFY_POLL,
    };

    for(int i= 0; i<3; i++) {
      PollSpec spec = new MockPollSpec(testau, rooturls[i], lwrbnd, uprbnd,
				       pollType[i]);
      testmsg[i] =
	LcapMessage.makeRequestMsg(spec,
				   testentries,
				   pollmanager.makeVerifier(testduration),
				   pollmanager.makeVerifier(testduration),
				   LcapMessage.NAME_POLL_REQ + (i * 2),
				   testduration,
				   testID);
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
