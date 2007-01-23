/*
 * $Id: TestPollManager.java,v 1.91 2007-01-23 21:44:37 smorabito Exp $
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
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.v3.*;
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

  private static ArrayList testentries =
    (ArrayList)ListUtil.list(new PollTally.NameListEntry(true,"test1.doc"),
			     new PollTally.NameListEntry(true,"test2.doc"),
			     new PollTally.NameListEntry(true,"test3.doc"));

  protected static ArchivalUnit testau;
  private MockLockssDaemon theDaemon;

  protected PeerIdentity testID;
  protected V1LcapMessage[] v1Testmsg;
  protected V3LcapMessage[] v3Testmsg;
  protected LocalMockPollManager pollmanager;
  protected IdentityManager idmanager;
  private File tempDir;

  protected void setUp() throws Exception {
    super.setUp();

    File tempDir = getTempDir();
    String tempDirPath = tempDir.getAbsolutePath() + File.separator;
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
    theDaemon.getDatagramRouterManager().stopService();
    theDaemon.getRouterManager().stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testGetPollFactoryByVersion() throws Exception {
    PollFactory pf1 = pollmanager.getPollFactory(1);
    PollFactory pf2 = pollmanager.getPollFactory(2);
    PollFactory pf3 = pollmanager.getPollFactory(3);
    assertNotNull(pf1);
    assertTrue(pf1 instanceof V1PollFactory);
    assertNull(pf2);
    assertNotNull(pf3);
    assertTrue(pf3 instanceof V3PollFactory);
  }

  public void testGetPollFactoryByPollSpec() throws Exception {
    Plugin plugin = new MockPlugin();
    CachedUrlSet cus =
      new MockCachedUrlSet(new MockArchivalUnit(plugin),
                           new SingleNodeCachedUrlSetSpec("foo"));
    PollSpec bad1 = new MockPollSpec(cus, -1);
    PollSpec bad2 = new MockPollSpec(cus, 99);
    PollSpec v1 = new MockPollSpec(cus, Poll.V1_CONTENT_POLL);
    PollSpec v3 = new MockPollSpec(cus, Poll.V3_POLL);
    PollFactory pfBad1 = pollmanager.getPollFactory(bad1);
    assertNull(pfBad1);
    PollFactory pfBad2 = pollmanager.getPollFactory(bad2);
    assertNull(pfBad2);
    PollFactory pfV1 = pollmanager.getPollFactory(v1);
    PollFactory pfV3 = pollmanager.getPollFactory(v3);
    assertNotNull(pfV1);
    assertTrue(pfV1 instanceof V1PollFactory);
    assertNotNull(pfV3);
    assertTrue(pfV3 instanceof V3PollFactory);
  }

  // Tests for the V1 PollFactory implementation

  // Start by testing the local mock poll factory

  public void testLocalMockV1PollFactory() {
    // This ensures that LocalMockV1PollFactory.canHashBeScheduledBefore() does
    // what I intended
    LocalMockV1PollFactory mpf = new LocalMockV1PollFactory();

    mpf.setMinPollDeadline(Deadline.in(1000));
    assertFalse(mpf.canHashBeScheduledBefore(100, Deadline.in(0),
					     pollmanager));
    assertTrue(mpf.canHashBeScheduledBefore(100, Deadline.in(1000),
					    pollmanager));
    assertTrue(mpf.canHashBeScheduledBefore(100, Deadline.in(1001),
					    pollmanager));

  }

  public void testV1CallPoll() {
    pollmanager.stopService();
    LocalMockPollManager pm = new LocalMockPollManager();
    pm.initService(theDaemon);
    pm.startService();
    theDaemon.setPollManager(pm);
    int[] opcode = {
      V1LcapMessage.NAME_POLL_REQ,
      V1LcapMessage.CONTENT_POLL_REQ,
      V1LcapMessage.VERIFY_POLL_REQ,
    };
    // pre-checks
    assertNotNull(theDaemon.getActivityRegulator(testau));
    // NB - Verify polls are never called via callPoll()
    for (int i = 0; i < v1Testmsg.length - 1; i++) {
      pm.msgSent = null;

      assertNotNull("Calling this poll should succeed",
		    pm.callPoll(new PollSpec(v1Testmsg[i])));
      assertNotNull(pm.msgSent);
      assertTrue(pm.msgSent instanceof V1LcapMessage);
      assertEquals(pm.msgSent.getOpcode(), opcode[i]);
    }
  }

  public void testV1CreatePoll() {
    PollFactory pf = pollmanager.getPollFactory(1);
    PollSpec[] ps = new PollSpec[3];
    for (int i = 0; i < v1Testmsg.length; i++) {
      ps[i] = new PollSpec(v1Testmsg[i]);
      try {
	BasePoll p = pf.createPoll(ps[i], theDaemon,
				   v1Testmsg[i].getOriginatorId(),
				   v1Testmsg[i].getDuration(),
				   v1Testmsg[i].getHashAlgorithm(),
                                   v1Testmsg[i]);
	assertTrue(p instanceof V1Poll);
	// assertTrue(p.isMyPoll());
	assertTrue(p.getPollSpec().equals(ps[i]));
	assertTrue(p.getCallerID() == v1Testmsg[i].getOriginatorId());
	p.setMessage(v1Testmsg[i]);
	assertTrue(p.getMessage() == v1Testmsg[i]);
      } catch (ProtocolException pe) {
	fail("createPoll " + v1Testmsg[i] + " threw " + pe);
      }
    }
  }

  /** test for V1 method PollFactory.pollShouldBeCalled(..) */
  public void testV1PollShouldBeCalled() throws Exception {
    // lets try to run two V1 content polls in the same location
    PollFactory ppf = pollmanager.getPollFactory(1);
    assertNotNull(ppf);
    assertTrue(ppf instanceof V1PollFactory);
    V1PollFactory pf = (V1PollFactory) ppf;

    V1LcapMessage[] sameroot = new V1LcapMessage[3];
    PollSpec[] spec = new PollSpec[3];
    int[] pollType = {
      Poll.V1_NAME_POLL,
      Poll.V1_CONTENT_POLL,
      Poll.V1_VERIFY_POLL,
    };

    for(int i= 0; i<3; i++) {
      spec[i] = new MockPollSpec(testau, urlstr, lwrbnd, uprbnd, pollType[i]);
      sameroot[i] =
	V1LcapMessage.makeRequestMsg(spec[i],
				     testentries,
				     ByteArray.makeRandomBytes(20),
				     ByteArray.makeRandomBytes(20),
				     V1LcapMessage.NAME_POLL_REQ + (i * 2),
				     testduration,
				     testID);
    }

    // check content poll conflicts
    BasePoll c1 = pollmanager.makePoll(sameroot[1]);
    // differnt content poll should be ok

    V1LcapMessage msg;
    msg = v1Testmsg[1];
    assertTrue("different content poll s/b ok",
	       pf.shouldPollBeCreated(new PollSpec(msg),
				      pollmanager,
                                      idmanager,
				      msg.getChallenge(),
				      msg.getOriginatorId()));

    // same content poll same range s/b a conflict
    msg = sameroot[1];
    assertFalse("same content poll root s/b conflict",
		pf.shouldPollBeCreated(new PollSpec(msg),
				       pollmanager,
                                       idmanager,
				       msg.getChallenge(),
				       msg.getOriginatorId()));

    // different name poll should be ok
    msg = v1Testmsg[0];
    assertTrue("different name poll s/b ok",
	       pf.shouldPollBeCreated(new PollSpec(msg),
				      pollmanager,
                                      idmanager,
				      msg.getChallenge(),
				      msg.getOriginatorId()));

    // same name poll s/b conflict
    msg = sameroot[0];
    assertFalse("same name poll root s/b conflict",
		pf.shouldPollBeCreated(new PollSpec(msg),
				       pollmanager,
                                       idmanager,
				       msg.getChallenge(),
				       msg.getOriginatorId()));

    // verify poll should be ok
    msg = v1Testmsg[2];
    assertTrue("verify poll s/b ok",
	       pf.shouldPollBeCreated(new PollSpec(msg),
				      pollmanager,
                                      idmanager,
				      msg.getChallenge(),
				      msg.getOriginatorId()));

    // remove the poll
    pollmanager.removePoll(c1.getKey());
  }

  public void testGetPollActivity() {
    PollFactory pf = pollmanager.getPollFactory(1);
    assertEquals(pf.getPollActivity(new PollSpec(v1Testmsg[0]),
				    pollmanager),
		 ActivityRegulator.STANDARD_NAME_POLL);
    int pa = pf.getPollActivity(new PollSpec(v1Testmsg[1]),
				pollmanager);
    assertTrue(pa == ActivityRegulator.STANDARD_CONTENT_POLL ||
	       pa == ActivityRegulator.SINGLE_NODE_CONTENT_POLL);
  }

  // Test for the method PollFactory.calcDuration(...)
  public void testV1CalcDuration() {
    MockCachedUrlSet mcus =
      new MockCachedUrlSet((MockArchivalUnit)testau,
			   new RangeCachedUrlSetSpec("", "", ""));
    PollSpec ps = new PollSpec(mcus, Poll.V1_CONTENT_POLL);
    LocalMockV1PollFactory pf = new LocalMockV1PollFactory();
    pollmanager.setPollFactory(1, pf);

    configPollTimes();
    pf.setBytesPerMsHashEstimate(100);
    pf.setSlowestHashSpeed(100);

    mcus.setEstimatedHashDuration(100);
    pf.setMinPollDeadline(Deadline.in(1000));
    assertEquals(1800, pf.calcDuration(ps, pollmanager));
    pf.setMinPollDeadline(Deadline.in(2000));
    assertEquals(2400, pf.calcDuration(ps, pollmanager));
    // this one should be limited by max content poll
    pf.setMinPollDeadline(Deadline.in(4000));
    assertEquals(4100, pf.calcDuration(ps, pollmanager));
    pf.setMinPollDeadline(Deadline.in(5000));
    assertEquals(-1, pf.calcDuration(ps, pollmanager));

    // calulated poll time will be less than min, should be adjusted up to min
    mcus.setEstimatedHashDuration(10);
    pf.setMinPollDeadline(Deadline.in(100));
    assertEquals(1000, pf.calcDuration(ps, pollmanager));

    // name poll duration is randomized so less predictable, but should
    // always be between min and max.
    long ndur =
      pf.calcDuration(new PollSpec(mcus, Poll.V1_NAME_POLL),
                      pollmanager);
    assertTrue(ndur >= pf.getMinPollDuration(Poll.V1_NAME_POLL));
    assertTrue(ndur <= pf.getMaxPollDuration(Poll.V1_NAME_POLL));
  }

  public void testV1CanSchedulePoll() {
    LocalMockV1PollFactory pf = new LocalMockV1PollFactory();

    pollmanager.setPollFactory(1, pf);
    // Accept polls that finish no earlier than this
    pf.setMinPollDeadline(Deadline.in(1000));
    // this one can't
    assertFalse(pf.canPollBeScheduled(500, 100, pollmanager));
    // can
    assertTrue(pf.canPollBeScheduled(2000, 100, pollmanager));
    // neededTime > duration
    assertFalse(pf.canPollBeScheduled(500, 600, pollmanager));
  }

  //  Now test the PollManager itself

  /** test for method makePoll(..) */
  public void testMakePoll() throws Exception {
    // make a name poll
    BasePoll p1 = pollmanager.makePoll(v1Testmsg[0]);
    // make sure we got the right type of poll here
    assertTrue(p1 instanceof V1NamePoll);

    // make a content poll
    BasePoll p2 = pollmanager.makePoll(v1Testmsg[1]);
    // make sure we got the right type of poll here
    assertTrue(p2 instanceof V1ContentPoll);

    // make a verify poll
    BasePoll p3 = pollmanager.makePoll(v1Testmsg[2]);
    // make sure we got the right type of poll here
    assertTrue(p3 instanceof V1VerifyPoll);

    // XXX: Uncomment when ready.
    // BasePoll p4 = pollmanager.makePoll(v3Testmsg[0]);
    // assertTrue(p4 instanceof V3Voter);
  }

  public void testMakePollDoesntIfPluginMismatch() throws Exception {
    // Make a string that's different from the plugin's version
    String bogus = testau.getPlugin().getVersion() + "cruft";

    // make a name poll witha bogus plugin version
    MockPollSpec spec =
      new MockPollSpec(testau, urlstr, lwrbnd, uprbnd, Poll.V1_NAME_POLL);
    spec.setPluginVersion(bogus);
    V1LcapMessage msg1 =
      V1LcapMessage.makeRequestMsg(spec,
				   testentries,
				   ByteArray.makeRandomBytes(20),
				   ByteArray.makeRandomBytes(20),
				   V1LcapMessage.NAME_POLL_REQ,
				   testduration,
				   testID);

    BasePoll p1 = pollmanager.makePoll(msg1);
    assertNull("Shouldn't create poll with plugin version mismatch", p1);

    // make a content poll witha bogus plugin version
    V1LcapMessage msg2 =
      V1LcapMessage.makeRequestMsg(spec,
				   testentries,
				   ByteArray.makeRandomBytes(20),
				   ByteArray.makeRandomBytes(20),
				   V1LcapMessage.CONTENT_POLL_REQ,
				   testduration,
				   testID);

    BasePoll p2 = pollmanager.makePoll(msg2);
    assertNull("Shouldn't create poll with plugin version mismatch", p2);
  }

  /** test for method makePollRequest(..) */
  public void testMakePollRequest() throws Exception {
    try {
      CachedUrlSet cus = null;
      cus = testau.makeCachedUrlSet( new RangeCachedUrlSetSpec(rooturls[1]));
      PollSpec spec = new PollSpec(cus, lwrbnd, uprbnd, Poll.V1_CONTENT_POLL);
      assertNotNull(pollmanager.callPoll(spec));
    }
    catch (IllegalStateException e) {
      // ignore this for now
    }
  }

  /** test for method findPoll(..) */
  public void testFindPoll() {
    // lets see if we can find our name poll
    try {
      BasePoll p1 = pollmanager.makePoll(v1Testmsg[0]);
      BasePoll p2 = pollmanager.findPoll(v1Testmsg[0]);
      assertEquals(p1, p2);
    }
    catch (IOException ex) {
      fail("name poll couldn't be found");
    }
  }


  /** test for method removePoll(..) */
  public void testRemovePoll() {
    try {
      BasePoll p1 = pollmanager.makePoll(v1Testmsg[0]);
      assertNotNull(p1);
      BasePoll p2 = pollmanager.removePoll(p1.getKey());
      assertEquals(p1, p2);
    }
    catch (IOException ex) {
      fail("name poll couldn't be found");
    }

  }

  /** test for method closeThePoll(..) */
  public void testCloseThePoll() throws Exception {
    BasePoll p1 = pollmanager.makePoll(v1Testmsg[0]);
    String key = p1.getKey();

    // we should now be active
    assertTrue(pollmanager.isPollActive(key));
    // we should not be closed
    assertFalse(pollmanager.isPollClosed(key));


    pollmanager.closeThePoll(key);
    // we should not be active
    assertFalse(pollmanager.isPollActive(key));
    // we should now be closed
    assertTrue(pollmanager.isPollClosed(key));
    // we should reject an attempt to handle a packet with this key
    pollmanager.handleIncomingMessage(v1Testmsg[0]);
    assertTrue(pollmanager.isPollClosed(key));
    assertFalse(pollmanager.isPollActive(key));
    pollmanager.closeThePoll(key);
  }

  /** test for method suspendPoll(...) */
  public void testSuspendPoll() throws Exception {
    BasePoll p1 = null;
    p1 = TestPoll.createCompletedPoll(theDaemon, testau, v1Testmsg[0], 7, 2,
				      pollmanager);
    String key = p1.getKey();
    pollmanager.addPoll(p1);
    // give it a pointless lock to avoid a null pointer
    ActivityRegulator.Lock lock = theDaemon.getActivityRegulator(testau).
      getAuActivityLock(-1, 123);

    // check our suspend
    pollmanager.suspendPoll(key);
    assertTrue(pollmanager.isPollSuspended(key));
    assertFalse(pollmanager.isPollClosed(key));

    // now we resume...
    pollmanager.resumePoll(false, key, lock);
    assertFalse(pollmanager.isPollSuspended(key));
  }

  /** Test for getPollsForAu(String auId) */
  public void testGetV3PollStatus() throws Exception {
    String auId = testau.getAuId();
    PollManager.V3PollStatusAccessor accessor = 
      pollmanager.getV3Status();
    
    assertEquals(0, accessor.getNumPolls(auId));
    assertEquals(0.0, accessor.getAgreement(auId), 0.001);
    assertEquals(-1, accessor.getLastPollTime(auId));

    addCompletedV3Poll(100000L, 0.99f);
    assertEquals(1, accessor.getNumPolls(auId));
    assertEquals(0.99, accessor.getAgreement(auId), 0.001);
    assertEquals(100000L, accessor.getLastPollTime(auId));
    
    addCompletedV3Poll(987654321L, 1.0f);
    assertEquals(2, accessor.getNumPolls(auId));
    assertEquals(1.0, accessor.getAgreement(auId), 0.001);
    assertEquals(987654321L, accessor.getLastPollTime(auId));
    
    addCompletedV3Poll(1000L, 0.25f);
    assertEquals(3, accessor.getNumPolls(auId));
    assertEquals(0.25, accessor.getAgreement(auId), 0.001);
    assertEquals(1000L, accessor.getLastPollTime(auId));
  }
  
  private void addCompletedV3Poll(long timestamp, 
                                  float agreement) throws Exception {
    PollSpec spec = new MockPollSpec(testau, rooturls[0], lwrbnd, uprbnd,
                                     Poll.V3_POLL);
    V3Poller poll = new V3Poller(spec, theDaemon, testID, "akeyforthispoll",
                                 1234567, "SHA-1");
    pollmanager.addPoll(poll);
    poll.stopPoll();
    PollManager.V3PollStatusAccessor v3status =
      pollmanager.getV3Status();
    v3status.incrementNumPolls(testau.getAuId());
    v3status.setAgreement(testau.getAuId(), agreement);
    v3status.setLastPollTime(testau.getAuId(), timestamp);
  }
  
  private BasePoll makeTestV3Voter() throws Exception {
    PollSpec spec = new MockPollSpec(testau, rooturls[0], lwrbnd, uprbnd,
                                     Poll.V3_POLL);

    V3LcapMessage pollMsg = 
      new V3LcapMessage(testau.getAuId(), "akeyforthispoll", "3",
                        ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20),
                        V3LcapMessage.MSG_POLL,
                        TimeBase.nowMs() + 50000, 
                        testID, tempDir, theDaemon);
    
    pollMsg.setVoteDuration(20000);
      
    return new V3Voter(theDaemon, pollMsg);
  }
  
  // XXX:  Move these tests to TestV1PollFactory
  /** test for method getMessageDigest(..) */
  public void testGetMessageDigest() {
    V1PollFactory pf = (V1PollFactory)pollmanager.getPollFactory(Poll.V1_PROTOCOL);
    MessageDigest md = pf.getMessageDigest(null);
    assertNotNull(md);
  }

  /** test for method makeVerifier(..) */
  public void testMakeVerifier() {
    V1PollFactory pf = (V1PollFactory)pollmanager.getPollFactory(Poll.V1_PROTOCOL);

    // test for make verifier - this will also store the verify/secret pair
    byte[] verifier = pf.makeVerifier(10000);
    assertNotNull("unable to make and store a verifier", verifier);

    // retrieve our secret
    byte[] secret = pf.getSecret(verifier);
    assertNotNull("unable to retrieve secret for verifier", secret);

    // confirm that the verifier is the hash of the secret
    MessageDigest md = pf.getMessageDigest(null);
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
    public void setPollFactory(int i, PollFactory fact) {
      pf[i] = fact;
    }
    public void sendMessage(V1LcapMessage msg, ArchivalUnit au)
        throws IOException {
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
    theDaemon = getMockLockssDaemon();
    pollmanager = new LocalMockPollManager();
    pollmanager.initService(theDaemon);
    theDaemon.setPollManager(pollmanager);
    idmanager = theDaemon.getIdentityManager();

    theDaemon.getPluginManager();
    testau = PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    ((MockArchivalUnit)testau).setPlugin(new MockPlugin());
    PluginTestUtil.registerArchivalUnit(testau);

    Properties p = new Properties();
    addRequiredConfig(p);
    ConfigurationUtil.setCurrentConfigFromProps(p);

    theDaemon.getSchedService().startService();
    theDaemon.getHashService().startService();
    theDaemon.getDatagramRouterManager().startService();
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
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
  }

  private void initTestAddr() {
    try {
      testID = theDaemon.getIdentityManager().stringToPeerIdentity("127.0.0.1");
    }
    catch (IdentityManager.MalformedIdentityKeyException ex) {
      fail("can't open test host");
    }
  }

  private void initTestMsg() throws Exception {
    // V1 Messages
    V1PollFactory pf = (V1PollFactory)pollmanager.getPollFactory(Poll.V1_PROTOCOL);

    v1Testmsg = new V1LcapMessage[3];
    int[] pollType = {
      Poll.V1_NAME_POLL,
      Poll.V1_CONTENT_POLL,
      Poll.V1_VERIFY_POLL,
    };

    for(int i= 0; i<3; i++) {
      PollSpec spec = new MockPollSpec(testau, rooturls[i], lwrbnd, uprbnd,
				       pollType[i]);
      v1Testmsg[i] =
	V1LcapMessage.makeRequestMsg(spec,
				     testentries,
				     pf.makeVerifier(testduration),
				     pf.makeVerifier(testduration),
				     V1LcapMessage.NAME_POLL_REQ + (i * 2),
				     testduration,
				     testID);
    }

    // V3 Messages.
    v3Testmsg = new V3LcapMessage[1];
//    PollSpec v3Spec = new MockPollSpec(testau, rooturls[0], null, null,
//                                       Poll.V3_POLL);
    v3Testmsg[0] = new V3LcapMessage(testau.getAuId(), "testpollid", "2",
                                     ByteArray.makeRandomBytes(20),
                                     ByteArray.makeRandomBytes(20),
                                     V3LcapMessage.MSG_POLL,
                                     12345678, testID, tempDir, theDaemon);
    v3Testmsg[0].setArchivalId(testau.getAuId());
  }

  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {TestPollManager.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
