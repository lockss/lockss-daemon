/*
 * $Id: TestPollManager.java,v 1.98 2008-11-08 08:15:46 tlipkis Exp $
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
import org.lockss.state.*;
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

  protected static MockArchivalUnit testau;
  private MockLockssDaemon theDaemon;
  private Plugin plugin;


  protected PeerIdentity testID;
  protected V1LcapMessage[] v1Testmsg;
  protected V3LcapMessage[] v3Testmsg;
  protected MyPollManager pollmanager;
  protected IdentityManager idmanager;
  private File tempDir;

  protected void setUp() throws Exception {
    super.setUp();

    File tempDir = getTempDir();
    String tempDirPath = tempDir.getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.1.2.3");
    p.setProperty(LcapDatagramComm.PARAM_ENABLED, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    plugin = new MockPlugin(getMockLockssDaemon());
    TimeBase.setSimulated();
    initRequiredServices();
    initTestAddr();
    initTestMsg();
  }


  public void tearDown() throws Exception {
    TimeBase.setReal();
    pollmanager.stopService();
    idmanager.stopService();
    theDaemon.getLockssRepository(testau).stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getDatagramRouterManager().stopService();
    theDaemon.getRouterManager().stopService();
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

  public void testMyV1PollFactory() {
    // This ensures that MyV1PollFactory.canHashBeScheduledBefore() does
    // what I intended
    MyV1PollFactory mpf = new MyV1PollFactory();

    mpf.setMinPollDeadline(Deadline.in(1000));
    assertFalse(mpf.canHashBeScheduledBefore(100, Deadline.in(0),
					     pollmanager));
    assertTrue(mpf.canHashBeScheduledBefore(100, Deadline.in(1000),
					    pollmanager));
    assertTrue(mpf.canHashBeScheduledBefore(100, Deadline.in(1001),
					    pollmanager));

  }

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
  
  MockArchivalUnit[] makeMockAus(int n) {
    MockArchivalUnit[] res = new MockArchivalUnit[n];
    for (int ix = 0; ix < n; ix++) {
      res[ix] = newMockArchivalUnit("mau" + ix);
    }
    return res;
  }

  MockArchivalUnit newMockArchivalUnit(String auid) {
    MockArchivalUnit mau = new MockArchivalUnit(plugin, auid);
    MockNodeManager nodeMgr = new MockNodeManager();
    theDaemon.setNodeManager(nodeMgr, mau);
    return mau;
  }

  void setAu(MockArchivalUnit mau,
	     long lastPollStart, long lastTopLevelPoll, int lastPollResult,
	     long pollDuration, double agreement) {
    MockAuState aus = (MockAuState)AuUtil.getAuState(mau);
    aus.setLastCrawlTime(100);
    aus.setLastPollStart(lastPollStart);
    aus.setLastToplevalPoll(lastTopLevelPoll);
    aus.setLastPollResult(lastPollResult);
    aus.setPollDuration(pollDuration);
    aus.setV3Agreement(agreement);
  }	   

  void registerAus(MockArchivalUnit[] aus) {
    List lst = ListUtil.fromArray(aus);
    List<MockArchivalUnit> rand = CollectionUtil.randomPermutation(lst);
    for (MockArchivalUnit mau : rand) {
      PluginTestUtil.registerArchivalUnit(plugin, mau);
    }
  }    

  static final int C = V3Poller.POLLER_STATUS_COMPLETE;
  static final int NC = V3Poller.POLLER_STATUS_NO_QUORUM;

  public void testPollQueue() throws Exception {
    testau.setShouldCallTopLevelPoll(false);

    Properties p = new Properties();
    p.put(PollManager.PARAM_REBUILD_POLL_QUEUE_INTERVAL, "");
    p.put(PollManager.PARAM_POLL_QUEUE_MAX, "8");
    p.put(PollManager.PARAM_POLL_INTERVAL_AGREEMENT_CURVE,
	  "[50,75],[50,500]");
    p.put(PollManager.PARAM_POLL_INTERVAL_AGREEMENT_LAST_RESULT, "1;6");
    p.put(PollManager.PARAM_TOPLEVEL_POLL_INTERVAL, "300");
    p.put(PollManager.PARAM_MIN_POLL_ATTEMPT_INTERVAL, "1");

    ConfigurationUtil.addFromProps(p);
    theDaemon.setAusStarted(true);
    TimeBase.setSimulated(1000);

    MockArchivalUnit[] aus = makeMockAus(16);
    registerAus(aus);

    //    setAu(mau, lastStart, lastComplete, lastResult, duration, agmnt);
    setAu(aus[0], 900, 950,  C, 5, .9);
    setAu(aus[1], 900, 500, NC, 5, .9);
    setAu(aus[2], 900, 950,  C, 5, .2);
    setAu(aus[3], 900, 500, NC, 5, .2);

    setAu(aus[4], 850, 950,  C, 10, .9);
    setAu(aus[5], 850, 500, NC, 10, .9);
    setAu(aus[6], 850, 950,  C, 10, .2);
    setAu(aus[7], 850, 500, NC, 10, .2);

    setAu(aus[ 8], 650, 750,  C, 10, .9);
    setAu(aus[ 9], 650, 400, NC, 10, .9);
    setAu(aus[10], 650, 750,  C, 10, .2);
    setAu(aus[11], 650, 400, NC, 10, .2);

    setAu(aus[12], 350, 450,  C, 10, .9);
    setAu(aus[13], 350, 100, NC, 10, .9);
    setAu(aus[14], 350, 450,  C, 10, .2);
    setAu(aus[15], 350, 100, NC, 10, .2);

    pollmanager.rebuildPollQueue();

    final Map<PollManager.PollReq,Double> weightMap =
      pollmanager.getWeightMap();
    ArrayList<PollManager.PollReq> queued = new ArrayList(weightMap.keySet());
    Collections.sort(queued, new Comparator<PollManager.PollReq>() {
	public int compare(PollManager.PollReq req1,
			   PollManager.PollReq req2) {
	  int res = - weightMap.get(req1).compareTo(weightMap.get(req2)); 
	  if (res == 0) {
	    res = req1.getAu().getAuId().compareTo(req2.getAu().getAuId());
	  }
	  return res;
	}});
    List exp = ListUtil.list(aus[14], aus[10], aus[13], aus[15],
			     aus[11], aus[9] , aus[1], aus[3],
			     aus[5], aus[7], aus[12]);
    assertEquals(exp, ausOfReqs(queued));
    List queue = pollmanager.pollQueue;
    assertEquals(8, queue.size());
    assertTrue(queue+"", exp.containsAll(ausOfReqs(queue)));
  }

  List<ArchivalUnit> ausOfReqs(List<PollManager.PollReq> reqs) {
    List<ArchivalUnit> res = new ArrayList();
    for (PollManager.PollReq req : reqs) {
      res.add(req.getAu());
    }
    return res;
  }

  public void testAtRiskMap() throws Exception {
    String p1 = "TCP:[127.0.0.1]:12";
    String p2 = "TCP:[127.0.0.2]:12";
    String p3 = "TCP:[127.0.0.3]:12";
    String p4 = "TCP:[127.0.0.4]:12";
    String p5 = "TCP:[127.0.0.5]:12";
  
    PeerIdentity peer1 = idmanager.stringToPeerIdentity(p1);
    PeerIdentity peer2 = idmanager.stringToPeerIdentity(p2);
    PeerIdentity peer3 = idmanager.stringToPeerIdentity(p3);
    PeerIdentity peer4 = idmanager.stringToPeerIdentity(p4);
    PeerIdentity peer5 = idmanager.stringToPeerIdentity(p5);

    String auid1 = "org|lockss|plugin|absinthe|AbsinthePlugin&base_url~http%3A%2F%2Fabsinthe-literary-review%2Ecom%2F&year~2003";
    String auid2 = "org|lockss|plugin|absinthe|AbsinthePlugin&base_url~http%3A%2F%2Fabsinthe-literary-review%2Ecom%2F&year~2004";
    MockArchivalUnit mau1 = new MockArchivalUnit(auid1);
    MockArchivalUnit mau2 = new MockArchivalUnit(auid2);

    String atRiskString =
      auid1 + "," + p1 + "," + p2 + "," + p5 + ";" +
      auid2 + "," + p3 + "," + p2 + "," + p4;

    ConfigurationUtil.addFromArgs(V3Poller.PARAM_AT_RISK_AU_INSTANCES,
				  atRiskString);
    assertEquals(SetUtil.set(peer1, peer2, peer5),
		 pollmanager.getPeersWithAuAtRisk(mau1));
    assertEquals(SetUtil.set(peer2, peer3, peer4),
		 pollmanager.getPeersWithAuAtRisk(mau2));
  }

  public void testGetNoAuSet() throws Exception {
    MockPlugin plugin = new MockPlugin(theDaemon);
    String auid1 = "auid111";
    MockArchivalUnit mau1 = new MockArchivalUnit(plugin, auid1);
    MockHistoryRepository historyRepo1 = new MyMockHistoryRepository();
    theDaemon.setHistoryRepository(historyRepo1, mau1);
    String auid2 = "auid222";
    MockArchivalUnit mau2 = new MockArchivalUnit(plugin, auid2);
    MockHistoryRepository historyRepo2 = new MyMockHistoryRepository();
    theDaemon.setHistoryRepository(historyRepo2, mau2);

    DatedPeerIdSet s1 = pollmanager.getNoAuPeerSet(mau1);
    DatedPeerIdSet s2 = pollmanager.getNoAuPeerSet(mau2);
    DatedPeerIdSet s3 = pollmanager.getNoAuPeerSet(mau1);
    assertNotSame(s1, s2);
    assertSame(s1, s3);
  }

  class MyMockHistoryRepository extends MockHistoryRepository {
    public DatedPeerIdSet getNoAuPeerSet() {
      return new DatedPeerIdSetImpl(new File("foo.bar"), idmanager);
    }
  }

  public void testAgeNoAuSet() throws Exception {
    String p1 = "TCP:[127.0.0.1]:12";
    String p2 = "TCP:[127.0.0.2]:12";
    PeerIdentity peer1 = idmanager.stringToPeerIdentity(p1);
    PeerIdentity peer2 = idmanager.stringToPeerIdentity(p2);
    List<PeerIdentity> both = ListUtil.list(peer1, peer2);

    ConfigurationUtil.addFromArgs(PollManager.PARAM_NO_AU_RESET_INTERVAL_CURVE,
 				  "[2000,500],[10000,500],[10000,5000]");

    TimeBase.setSimulated(1000);
    String auid = "auid111";
    MockPlugin plugin = new MockPlugin(theDaemon);
    MockArchivalUnit mau = new MockArchivalUnit(plugin, auid);
    MockNodeManager nodeMgr = new MockNodeManager();
    theDaemon.setNodeManager(nodeMgr, mau);
    MockAuState maus = new MockAuState();
    nodeMgr.setAuState(maus);
    File file = FileTestUtil.tempFile("noau");
    DatedPeerIdSet noAuSet = new DatedPeerIdSetImpl(file, idmanager);
    assertTrue(noAuSet.isEmpty());
    assertTrue(noAuSet.getDate() < 0);
    pollmanager.ageNoAuSet(mau, noAuSet);
    assertTrue(noAuSet.isEmpty());
    assertTrue(noAuSet.getDate() < 0);
    maus.setAuCreationTime(1000);
    noAuSet.addAll(both);
    noAuSet.setDate(TimeBase.nowMs());
    assertTrue(noAuSet.containsAll(both));

    pollmanager.ageNoAuSet(mau, noAuSet);
    assertTrue(noAuSet.containsAll(both));

    TimeBase.step(1000);
    pollmanager.ageNoAuSet(mau, noAuSet);
    assertTrue(noAuSet.isEmpty());
    noAuSet.addAll(both);
    noAuSet.setDate(TimeBase.nowMs());
    assertTrue(noAuSet.containsAll(both));
    TimeBase.step(499);
    pollmanager.ageNoAuSet(mau, noAuSet);
    assertTrue(noAuSet.containsAll(both));
    TimeBase.step(1);
    pollmanager.ageNoAuSet(mau, noAuSet);
    assertTrue(noAuSet.isEmpty());
    TimeBase.step(12000);
    noAuSet.addAll(both);
    noAuSet.setDate(TimeBase.nowMs());
    pollmanager.ageNoAuSet(mau, noAuSet);
    assertTrue(noAuSet.containsAll(both));
    TimeBase.step(4999);
    pollmanager.ageNoAuSet(mau, noAuSet);
    assertTrue(noAuSet.containsAll(both));
    TimeBase.step(1);
    pollmanager.ageNoAuSet(mau, noAuSet);
    assertTrue(noAuSet.isEmpty());
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

  // MyPollManager allows us to override the PollFactory
  // used for a particular protocol,  and to override the
  // sendMessage() method.
  static class MyPollManager extends PollManager {
    LcapMessage msgSent = null;
    Map weightMap;

    public void setPollFactory(int i, PollFactory fact) {
      pf[i] = fact;
    }
    public void sendMessage(V1LcapMessage msg, ArchivalUnit au)
        throws IOException {
      msgSent = msg;
    }

    @Override
    protected List weightedRandomSelection(Map weightMap, int n) {
      log.debug("weightMap: " + weightMap);
      this.weightMap = weightMap;
      return super.weightedRandomSelection(weightMap, n);
    }

    Map getWeightMap() {
      return weightMap;
    }
  }


  // MyV1PollFactory allows us to override the
  // canHashBeScheduledBefore() method and avoid the
  // complexity of mocking the hasher and scheduler.
  static class MyV1PollFactory extends V1PollFactory {
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
    pollmanager = new MyPollManager();
    pollmanager.initService(theDaemon);
    theDaemon.setPollManager(pollmanager);
    idmanager = theDaemon.getIdentityManager();

    theDaemon.getPluginManager();
    testau =
      (MockArchivalUnit)PollTestPlugin.PTArchivalUnit.createFromListOfRootUrls(rooturls);
    testau.setPlugin(new MockPlugin(theDaemon));
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
    p.setProperty(LcapDatagramComm.PARAM_ENABLED, "false");
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
