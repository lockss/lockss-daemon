/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.state;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.Test;

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.Vote;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;

public abstract class TestHistoryRepositoryImpl extends LockssTestCase {

  /**
   * <p>A version of {@link TestHistoryRepositoryImpl} that forces the
   * serialization compatibility mode to
   * {@link CXSerializer#CASTOR_MODE}.</p>
   * @author Thib Guicherd-Callin
   */
//   public static class WithCastor extends TestHistoryRepositoryImpl {
//     public void setUp() throws Exception {
//       super.setUp();
//       ConfigurationUtil.addFromArgs(CXSerializer.PARAM_COMPATIBILITY_MODE,
//                                     Integer.toString(CXSerializer.CASTOR_MODE));
//     }

//     public void testStoreAuState() throws Exception {
//       // Not bothering to update castor mapping file
//     }
//   }

  /**
   * <p>A version of {@link TestHistoryRepositoryImpl} that forces the
   * serialization compatibility mode to
   * {@link CXSerializer#XSTREAM_MODE}.</p>
   * @author Thib Guicherd-Callin
   */
  public static class WithXStream extends TestHistoryRepositoryImpl {
    public void setUp() throws Exception {
      super.setUp();
      ConfigurationUtil.addFromArgs(CXSerializer.PARAM_COMPATIBILITY_MODE,
                                    Integer.toString(CXSerializer.XSTREAM_MODE));
    }
    public void testStorePollHistories() {
      log.critical("Not executing this Castor-centric test."); // FIXME
    }
  }

  public static Test suite() {
    return variantSuites(TestHistoryRepositoryImpl.class);
  }

  private String tempDirPath;
  private HistoryRepositoryImpl repository;
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;
  private IdentityManager idmgr;
  private String idKey;
  private PeerIdentity testID1 = null;
  private PeerIdentity testID2 = null;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    theDaemon.startDaemon();
    mau = new MockArchivalUnit(new MockPlugin(theDaemon));
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    configHistoryParams(tempDirPath);
    repository = (HistoryRepositoryImpl)
        HistoryRepositoryImpl.createNewHistoryRepository(mau);
    repository.initService(theDaemon);
    repository.startService();
    if (idmgr == null) {
      idmgr = theDaemon.getIdentityManager();
      idmgr.startService();
    }
    testID1 = idmgr.stringToPeerIdentity("127.1.2.3");
    testID2 = idmgr.stringToPeerIdentity("127.4.5.6");
  }

  public void tearDown() throws Exception {
    if (idmgr != null) {
      idmgr.stopService();
      idmgr = null;
    }
    repository.stopService();
    super.tearDown();
  }

  public void testGetNodeLocation() throws Exception {
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    MockCachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    String location = repository.getNodeLocation(mcus);
    String expected = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							       mau);
    expected = LockssRepositoryImpl.mapUrlToFileLocation(expected,
        "http://www.example.com");

    assertEquals(expected, location);
  }

  public void testDotUrlHandling() throws Exception {
    //testing correction of nodes with bad '..'-including urls,
    //filtering the first '..' but resolving the second
    // should filter out the first '..' line but resolve the second
    MockCachedUrlSetSpec mspec = new MockCachedUrlSetSpec(
        "http://www.example.com/branch/test/../test2", null);
    MockCachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    String location = repository.getNodeLocation(mcus);
    String expectedStart =
      LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    String expected = LockssRepositoryImpl.mapUrlToFileLocation(
        expectedStart, "http://www.example.com/branch/test2");

    assertEquals(expected, location);

    mspec = new MockCachedUrlSetSpec("http://www.example.com/branch/./test",
                                     null);
    mcus = new MockCachedUrlSet(mau, mspec);
    location = repository.getNodeLocation(mcus);
    expected = LockssRepositoryImpl.mapUrlToFileLocation(expectedStart,
        "http://www.example.com/branch/test");

    assertEquals(expected, location);

    try {
      mspec = new MockCachedUrlSetSpec("http://www.example.com/..", null);
      mcus = new MockCachedUrlSet(mau, mspec);
      location = repository.getNodeLocation(mcus);
      fail("Should have thrown MalformedURLException.");
    } catch (MalformedURLException mue) { }

    try {
      mspec = new MockCachedUrlSetSpec(
          "http://www.example.com/test/../../test2", null);
      mcus = new MockCachedUrlSet(mau, mspec);
      location = repository.getNodeLocation(mcus);
      fail("Should have thrown MalformedURLException.");
    } catch (MalformedURLException mue) { }
  }

  public void testStorePollHistories() throws Exception {
    TimeBase.setSimulated(123321);
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    CachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    NodeStateImpl nodeState = new NodeStateImpl(mcus, -1, null, null,
                                                repository);
    List histories = ListUtil.list(createPollHistoryBean(3), createPollHistoryBean(3),
                                   createPollHistoryBean(3), createPollHistoryBean(3),
                                   createPollHistoryBean(3));

    /*
     * CASTOR: [summary] Rewrite test in non-Castor way
     * This is obviously not an appropriate way of writing this test,
     * Right now it creates sample data in Castor format, from legacy
     * code back when Castor was the built-in serialization engine.
     * TODO: Rewrite test in non-Castor way
     */
    //nodeState.setPollHistoryBeanList(histories);
    nodeState.setPollHistoryList(NodeHistoryBean.fromBeanListToList(histories));

    repository.storePollHistories(nodeState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							       mau);
    filePath = LockssRepositoryImpl.mapUrlToFileLocation(filePath,
        "http://www.example.com/"+HistoryRepositoryImpl.HISTORY_FILE_NAME);
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    nodeState.setPollHistoryList(new ArrayList());
    repository.loadPollHistories(nodeState);
    List loadedHistory = nodeState.getPollHistoryList();
    assertEquals(histories.size(), loadedHistory.size());
    // CASTOR: some Castor-tailored stuff here
    // PollHistoryBean expect1 = (PollHistoryBean)histories.get(0);
    // PollHistoryBean elem1 = (PollHistoryBean)loadedHistory.get(0);
    PollHistory expect1 = (PollHistory)histories.get(0);
    PollHistory elem1 = (PollHistory)loadedHistory.get(0);
    assertEquals(expect1.type, elem1.type);
    assertEquals(expect1.lwrBound, elem1.lwrBound);
    assertEquals(expect1.uprBound, elem1.uprBound);
    assertEquals(expect1.status, elem1.status);
    assertEquals(expect1.startTime, elem1.startTime);
    assertEquals(expect1.duration, elem1.duration);
    // CASTOR: some Castor-tailored stuff here
    // List expectBeans = (List)expect1.getVoteBeans();
    // List elemBeans = (List)elem1.getVoteBeans();
    Iterator expectIter = (Iterator)expect1.getVotes();
    Iterator elemIter = (Iterator)elem1.getVotes();
    while (expectIter.hasNext() && elemIter.hasNext()) {
      Vote expectVote = (Vote)expectIter.next();
      Vote elemVote = (Vote)elemIter.next();
      assertEquals(expectVote.getVoterIdentity().getIdString(),
                   elemVote.getVoterIdentity().getIdString());
      assertEquals(expectVote.isAgreeVote(),
                   elemVote.isAgreeVote());
      assertEquals(expectVote.getChallengeString(),
                   elemVote.getChallengeString());
      assertEquals(expectVote.getVerifierString(),
                   elemVote.getVerifierString());
      assertEquals(expectVote.getHashString(),
                   elemVote.getHashString());
    }
    assertFalse(expectIter.hasNext());
    assertFalse(expectIter.hasNext());
    TimeBase.setReal();
  }

  public void testHandleEmptyFile() throws Exception {
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    CachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    NodeStateImpl nodeState = new NodeStateImpl(mcus, -1, null, null,
                                                repository);
    nodeState.setPollHistoryList(new ArrayList());
    //storing empty vector
    repository.storePollHistories(nodeState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							       mau);
    filePath = LockssRepositoryImpl.mapUrlToFileLocation(filePath,
        "http://www.example.com/"+HistoryRepositoryImpl.HISTORY_FILE_NAME);
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    nodeState.setPollHistoryList(new ArrayList());
    repository.loadPollHistories(nodeState);
    assertEquals(0, nodeState.pollHistories.size());

    mspec = new MockCachedUrlSetSpec("http://www.example2.com", null);
    mcus = new MockCachedUrlSet(mau, mspec);
    nodeState = new NodeStateImpl(mcus, -1, null, null, repository);
    filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath, mau);
    filePath = LockssRepositoryImpl.mapUrlToFileLocation(filePath,
        "http://www.example2.com/");
    xmlFile = new File(filePath);
    assertFalse(xmlFile.exists());
    xmlFile.mkdirs();
    filePath += HistoryRepositoryImpl.HISTORY_FILE_NAME;
    xmlFile = new File(filePath);
    OutputStream os = new BufferedOutputStream(new FileOutputStream(xmlFile));
    os.write(new byte[0]);
    os.close();
    assertTrue(xmlFile.exists());

    nodeState.setPollHistoryList(new ArrayList());
    repository.loadPollHistories(nodeState);
    assertEquals(0, nodeState.pollHistories.size());
    assertFalse(xmlFile.exists());
    xmlFile = new File(filePath + CurrentConfig.getParam(ObjectSerializer.PARAM_FAILED_DESERIALIZATION_EXTENSION,
                                                         ObjectSerializer.DEFAULT_FAILED_DESERIALIZATION_EXTENSION));
    assertTrue(xmlFile.exists());
  }

  public void testStoreAuEmptyState() throws Exception {
    HashSet strCol = new HashSet();
    strCol.add("test");
    AuState origState = new AuState(mau, repository);
    repository.storeAuState(origState);
    AuState loadedState = repository.loadAuState();
    assertEquals(-1, loadedState.getLastCrawlTime());
    assertEquals(-1, loadedState.getLastCrawlAttempt());
    assertEquals(-1, loadedState.getLastCrawlResult());
    assertEquals("Unknown code -1", loadedState.getLastCrawlResultMsg());
    assertEquals(-1, loadedState.getLastTopLevelPollTime());
    assertEquals(-1, loadedState.getLastPollStart());
    assertEquals(-1, loadedState.getLastPollResult());
    assertEquals(null, loadedState.getLastPollResultMsg());

    assertEquals(-1, loadedState.getLastPoPPoll());
    assertEquals(-1, loadedState.getLastPoPPollResult());
    assertEquals(-1, loadedState.getLastLocalHashScan());
    assertEquals(0, loadedState.getNumAgreePeersLastPoR());
    assertEquals(0, loadedState.getNumWillingRepairers());
    assertEquals(0, loadedState.getNumCurrentSuspectVersions());
    assertEmpty(loadedState.getCdnStems());
    loadedState.addCdnStem("http://this.is.new/");
    assertEquals(ListUtil.list("http://this.is.new/"), loadedState.getCdnStems());
    loadedState.addCdnStem("http://this.is.new/");
    assertEquals(ListUtil.list("http://this.is.new/"), loadedState.getCdnStems());

    assertEquals(0, loadedState.getPollDuration());
    assertEquals(0, loadedState.getClockssSubscriptionStatus());
    assertEquals(null, loadedState.getAccessType());
    assertEquals(SubstanceChecker.State.Unknown, loadedState.getSubstanceState());
    assertEquals(null, loadedState.getFeatureVersion(Plugin.Feature.Substance));
    assertEquals(null, loadedState.getFeatureVersion(Plugin.Feature.Metadata));
    assertEquals(-1, loadedState.getLastMetadataIndex());
    assertEquals(0, loadedState.getLastContentChange());
    assertEquals(mau.getAuId(), loadedState.getArchivalUnit().getAuId());
  }

  public void testStoreAuState() throws Exception {
    HashSet strCol = new HashSet();
    strCol.add("test");
    AuState origState = new AuState(mau,
				    123000, 123123, 41, "woop woop",
				    -1, -1, -1, "deep woop", -1,
				    321000, 222000, 3, "pollres", 12345,
				    456000, strCol,
				    AuState.AccessType.OpenAccess,
				    2, 1.0, 1.0,
				    SubstanceChecker.State.Yes,
				    "SubstVer3", "MetadatVer7", 111444,
				    12345,
				    111222, // lastPoPPoll
				    7, // lastPoPPollResult
				    222333, // lastLocalHashScan
				    444777, // numAgreePeersLastPoR
				    777444, // numWillingRepairers
				    747474, // numCurrentSuspectVersions
				    ListUtil.list("http://hos.t/pa/th"),
				    repository);

    assertEquals("SubstVer3",
		 origState.getFeatureVersion(Plugin.Feature.Substance));
    assertEquals("MetadatVer7",
		 origState.getFeatureVersion(Plugin.Feature.Metadata));
    assertEquals(111444, origState.getLastMetadataIndex());

    repository.storeAuState(origState);

    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							       mau);
    filePath += HistoryRepositoryImpl.AU_FILE_NAME;
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    origState = null;
    AuState loadedState = repository.loadAuState();
    assertEquals(123000, loadedState.getLastCrawlTime());
    assertEquals(123123, loadedState.getLastCrawlAttempt());
    assertEquals(41, loadedState.getLastCrawlResult());
    assertEquals("woop woop", loadedState.getLastCrawlResultMsg());
    assertEquals(321000, loadedState.getLastTopLevelPollTime());
    assertEquals(222000, loadedState.getLastPollStart());
    assertEquals(3, loadedState.getLastPollResult());
    assertEquals("Inviting Peers", loadedState.getLastPollResultMsg());

    assertEquals(111222, loadedState.getLastPoPPoll());
    assertEquals(7, loadedState.getLastPoPPollResult());
    assertEquals(222333, loadedState.getLastLocalHashScan());

    assertEquals(444777, loadedState.getNumAgreePeersLastPoR());
    assertEquals(777444, loadedState.getNumWillingRepairers());
    assertEquals(747474, loadedState.getNumCurrentSuspectVersions());
    assertEquals(ListUtil.list("http://hos.t/pa/th"),
		 loadedState.getCdnStems());
    loadedState.addCdnStem("http://this.is.new/");
    assertEquals(ListUtil.list("http://hos.t/pa/th", "http://this.is.new/"),
		 loadedState.getCdnStems());

    assertEquals(12345, loadedState.getPollDuration());
    assertEquals(2, loadedState.getClockssSubscriptionStatus());
    assertEquals(AuState.AccessType.OpenAccess, loadedState.getAccessType());
    assertEquals(SubstanceChecker.State.Yes, loadedState.getSubstanceState());
    assertEquals("SubstVer3",
		 loadedState.getFeatureVersion(Plugin.Feature.Substance));
    assertEquals("MetadatVer7",
		 loadedState.getFeatureVersion(Plugin.Feature.Metadata));
    assertEquals(111444, loadedState.getLastMetadataIndex());
    assertEquals(12345, loadedState.getLastContentChange());
    assertEquals(mau.getAuId(), loadedState.getArchivalUnit().getAuId());

    // check crawl urls
    Collection col = loadedState.getCrawlUrls();
    Iterator colIter = col.iterator();
    assertTrue(colIter.hasNext());
    assertEquals("test", colIter.next());
    assertFalse(colIter.hasNext());
  }

  public void testStoreDamagedNodeSet() throws Exception {
    DamagedNodeSet damNodes = new DamagedNodeSet(mau, repository);
    damNodes.nodesWithDamage.add("test1");
    damNodes.nodesWithDamage.add("test2");
    damNodes.cusToRepair.put("cus1", ListUtil.list("cus1-1", "cus1-2"));
    damNodes.cusToRepair.put("cus2", ListUtil.list("cus2-1"));
    assertTrue(damNodes.containsWithDamage("test1"));
    assertTrue(damNodes.containsWithDamage("test2"));
    assertFalse(damNodes.containsWithDamage("test3"));

    repository.storeDamagedNodeSet(damNodes);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							       mau);
    filePath += HistoryRepositoryImpl.DAMAGED_NODES_FILE_NAME;
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    damNodes = null;
    damNodes = repository.loadDamagedNodeSet();
    // check damage
    assertTrue(damNodes.containsWithDamage("test1"));
    assertTrue(damNodes.containsWithDamage("test2"));
    assertFalse(damNodes.containsWithDamage("test3"));

    MockCachedUrlSet mcus1 = new MockCachedUrlSet("cus1");
    MockCachedUrlSet mcus2 = new MockCachedUrlSet("cus2");

    // check repairs
    assertTrue(damNodes.containsToRepair(mcus1, "cus1-1"));
    assertTrue(damNodes.containsToRepair(mcus1, "cus1-2"));
    assertFalse(damNodes.containsToRepair(mcus1, "cus2-1"));
    assertTrue(damNodes.containsToRepair(mcus2, "cus2-1"));
    assertEquals(mau.getAuId(), damNodes.theAu.getAuId());

    // check remove
    damNodes.removeFromRepair(mcus1, "cus1-1");
    assertFalse(damNodes.containsToRepair(mcus1, "cus1-1"));
    assertTrue(damNodes.containsToRepair(mcus1, "cus1-2"));
    damNodes.removeFromRepair(mcus1, "cus1-2");
    assertFalse(damNodes.containsToRepair(mcus1, "cus1-2"));
    assertNull(damNodes.cusToRepair.get(mcus1));

    // check remove from damaged nodes
    damNodes.removeFromDamage("test1");
    damNodes.removeFromDamage("test2");
    repository.storeDamagedNodeSet(damNodes);
    damNodes = repository.loadDamagedNodeSet();
    assertNotNull(damNodes);
    assertFalse(damNodes.containsWithDamage("test1"));
    assertFalse(damNodes.containsWithDamage("test2"));
  }

  public void testStoreOverwrite() throws Exception {
    AuState auState = new AuState(mau,
				  123, // lastCrawlTime
				  321, // lastCrawlAttempt
				  -1, // lastCrawlResult
				  null, // lastCrawlResultMsg,
				  -1, // lastDeepCrawlTime
				  -1, // lastDeepCrawlAttempt
				  -1, // lastDeepCrawlResult
				  null, // lastDeepCrawlResultMsg,
				  -1, // lastDeepCrawlDepth
				  321, // lastTopLevelPoll
				  333, // lastPollStart
				  -1, // lastPollresult
				  null, // lastPollresultMsg
				  0, // pollDuration
				  -1, // lastTreeWalk
				  null, // crawlUrls
				  null, // accessType
				  1, // clockssSubscriptionState
				  1.0, // v3Agreement
				  1.0, // highestV3Agreement
				  SubstanceChecker.State.Unknown,
				  null, // substanceVersion
				  null, // metadataVersion
				  -1, // lastMetadataIndex
				  0, // lastContentChange
				  444, // lastPoPPoll
				  8, // lastPoPPollResult
				  -1, // lastLocalHashScan
				  27, // numAgreePeersLastPoR
				  72, // numWillingRepirers
				  19, // numCurrentSuspectVersions
				  ListUtil.list("http://foo/"), // cdnStems
				  repository);

    repository.storeAuState(auState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							       mau);
    filePath += HistoryRepositoryImpl.AU_FILE_NAME;
    File xmlFile = new File(filePath);
    FileInputStream fis = new FileInputStream(xmlFile);
    UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream();
    StreamUtil.copy(fis, baos);
    fis.close();
    String expectedStr = baos.toString();

    auState = new AuState(mau,
			  1234, // lastCrawlTime
			  4321, // lastCrawlAttempt
			  -1, // lastCrawlResult
			  null, // lastCrawlResultMsg,
			  -1, // lastDeepCrawlTime
			  -1, // lastDeepCrawlAttempt
			  -1, // lastDeepCrawlResult
			  null, // lastDeepCrawlResultMsg,
			  -1, // lastDeepCrawlDepth
			  4321, // lastTopLevelPoll
			  5555, // lastPollStart
			  -1, // lastPollresult
			  null, // lastPollresultMsg
			  0, // pollDuration
			  -1, // lastTreeWalk
			  null, // crawlUrls
			  null, // accessType
			  1, // clockssSubscriptionState
			  1.0, // v3Agreement
			  1.0, // highestV3Agreement
			  SubstanceChecker.State.Unknown,
			  null, // substanceVersion
			  null, // metadataVersion
			  -1, // lastMetadataIndex
			  0, // lastContentChange
			  -1, // lastPoPPoll
			  -1, // lastPoPPollResult
			  -1, // lastLocalHashScan
			  13, // numAgreePeersLastPoR
			  31, // numWillingRepairers
			  91, // numCurrentSuspectVersions
			  ListUtil.list("http://foo/"), // cdnStems
			  repository);
    repository.storeAuState(auState);
    assertEquals(1234, auState.getLastCrawlTime());
    assertEquals(4321, auState.getLastCrawlAttempt());
    assertEquals(4321, auState.getLastTopLevelPollTime());
    assertEquals(5555, auState.getLastPollStart());
    assertEquals(13, auState.getNumAgreePeersLastPoR());
    assertEquals(31, auState.getNumWillingRepairers());
    assertEquals(91, auState.getNumCurrentSuspectVersions());
    assertEquals(mau.getAuId(), auState.getArchivalUnit().getAuId());
    assertEquals(ListUtil.list("http://foo/"), auState.getCdnStems());

    fis = new FileInputStream(xmlFile);
    baos = new UnsynchronizedByteArrayOutputStream(expectedStr.length());
    StreamUtil.copy(fis, baos);
    fis.close();
    log.info(baos.toString());

    auState = null;
    auState = repository.loadAuState();
    assertEquals(1234, auState.getLastCrawlTime());
    assertEquals(4321, auState.getLastCrawlAttempt());
    assertEquals(4321, auState.getLastTopLevelPollTime());
    assertEquals(5555, auState.getLastPollStart());
    assertEquals(13, auState.getNumAgreePeersLastPoR());
    assertEquals(31, auState.getNumWillingRepairers());
    assertEquals(91, auState.getNumCurrentSuspectVersions());
    assertEquals(mau.getAuId(), auState.getArchivalUnit().getAuId());

    auState = new AuState(mau,
			  123, // lastCrawlTime
			  321, // lastCrawlAttempt
			  -1, // lastCrawlResult
			  null, // lastCrawlResultMsg,
			  -1, // lastDeepCrawlTime
			  -1, // lastDeepCrawlAttempt
			  -1, // lastDeepCrawlResult
			  null, // lastDeepCrawlResultMsg,
			  -1, // lastDeepCrawlDepth
			  321, // lastTopLevelPoll
			  333, // lastPollStart
			  -1, // lastPollresult
			  null, // lastPollresultMsg
			  0, // pollDuration
			  -1, // lastTreeWalk
			  null, // crawlUrls
			  null, // accessType
			  1, // clockssSubscriptionState
			  1.0, // v3Agreement
			  1.0, // highestV3Agreement
			  SubstanceChecker.State.Unknown,
			  null, // substanceVersion
			  null, // metadataVersion
			  -1, // lastMetadataIndex
			  0, // lastContentChange
			  444, // lastPoPPoll
			  8, // lastPoPPollResult
			  -1, // lastLocalHashScan
			  27, // numAgreePeersLastPoR
			  72, // numWillingRepairers
			  19, // numCurrentSuspectVersions
			  ListUtil.list("http://foo/"), // cdnStems
			  repository);
    repository.storeAuState(auState);
    fis = new FileInputStream(xmlFile);
    baos = new UnsynchronizedByteArrayOutputStream(expectedStr.length());
    StreamUtil.copy(fis, baos);
    fis.close();
    assertEquals(expectedStr, baos.toString());
  }

  public void testStoreNodeState() throws Exception {
    TimeBase.setSimulated(100);
    CachedUrlSet mcus = new MockCachedUrlSet(mau, new RangeCachedUrlSetSpec(
        "http://www.example.com"));
    CrawlState crawl = new CrawlState(1, 2, 123);
    List polls = new ArrayList(2);
    PollState poll1 = new PollState(1, "sdf", "jkl", 2, 123, Deadline.at(456), false);
    PollState poll2 = new PollState(2, "abc", "def", 3, 321, Deadline.at(654), false);
    polls.add(poll1);
    polls.add(poll2);
    NodeState nodeState = new NodeStateImpl(mcus, 123321, crawl, polls,
                                            repository);
    ((NodeStateImpl)nodeState).setState(NodeState.DAMAGE_AT_OR_BELOW);
    repository.storeNodeState(nodeState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
							       mau);
    filePath = LockssRepositoryImpl.mapUrlToFileLocation(filePath,
        "http://www.example.com/"+HistoryRepositoryImpl.NODE_FILE_NAME);
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    nodeState = null;
    nodeState = repository.loadNodeState(mcus);
    assertSame(mcus, nodeState.getCachedUrlSet());

    assertEquals(123321, nodeState.getAverageHashDuration());
    assertEquals(1, nodeState.getCrawlState().getType());
    assertEquals(2, nodeState.getCrawlState().getStatus());
    assertEquals(123, nodeState.getCrawlState().getStartTime());
    assertEquals(NodeState.DAMAGE_AT_OR_BELOW, nodeState.getState());

    Iterator pollIt = nodeState.getActivePolls();
    assertTrue(pollIt.hasNext());
    PollState loadedPoll = (PollState)pollIt.next();
    assertEquals(1, loadedPoll.getType());
    assertEquals("sdf", loadedPoll.getLwrBound());
    assertEquals("jkl", loadedPoll.getUprBound());
    assertEquals(2, loadedPoll.getStatus());
    assertEquals(123, loadedPoll.getStartTime());
    assertEquals(456, loadedPoll.getDeadline().getExpirationTime());

    assertTrue(pollIt.hasNext());
    loadedPoll = (PollState)pollIt.next();
    assertEquals(2, loadedPoll.getType());
    assertEquals("abc", loadedPoll.getLwrBound());
    assertEquals("def", loadedPoll.getUprBound());
    assertEquals(3, loadedPoll.getStatus());
    assertEquals(321, loadedPoll.getStartTime());
    assertEquals(654, loadedPoll.getDeadline().getExpirationTime());
    assertFalse(pollIt.hasNext());

    TimeBase.setReal();
  }

  // TODO: Decide how to split this test between here and TestPeerAgreements
//  public void testStoreIdentityAgreements() throws Exception {
//    IdentityManager.IdentityAgreement id1 =
//      new IdentityManager.IdentityAgreement(testID1);
//    id1.setLastAgree(123);
//    id1.setLastDisagree(321);
//    id1.setPercentAgreement(0.5f);
//    IdentityManager.IdentityAgreement id2 =
//      new IdentityManager.IdentityAgreement(testID2);
//    id2.setLastAgree(456);
//    id2.setLastDisagree(654);
//    id2.setPercentAgreementHint(0.8f);
//
//    repository.storeIdentityAgreements(ListUtil.list(id1, id2));
//    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath,
//							       mau);
//    filePath += HistoryRepositoryImpl.IDENTITY_AGREEMENT_FILE_NAME;
//    File xmlFile = new File(filePath);
//    assertTrue(xmlFile.exists());
//
//    List idList = repository.loadIdentityAgreements();
//    assertEquals(2, idList.size());
//    id1 = (IdentityManager.IdentityAgreement)idList.get(0);
//    assertNotNull(id1);
//    assertSame(testID1, idmgr.stringToPeerIdentity(id1.getId()));
//    assertEquals(123, id1.getLastAgree());
//    assertEquals(321, id1.getLastDisagree());
//    assertEquals(.5f, id1.getPercentAgreement());
//    assertEquals(.5f, id1.getHighestPercentAgreement());
//    assertEquals(-1.0f, id1.getPercentAgreementHint());
//    assertEquals(-1.0f, id1.getHighestPercentAgreementHint());
//
//    id2 = (IdentityManager.IdentityAgreement)idList.get(1);
//    assertSame(testID2, idmgr.stringToPeerIdentity(id2.getId()));
//    assertEquals(456, id2.getLastAgree());
//    assertEquals(654, id2.getLastDisagree());
//    assertEquals(0.0f, id2.getPercentAgreement());
//    assertEquals(0.0f, id2.getHighestPercentAgreement());
//    assertEquals(0.8f, id2.getPercentAgreementHint());
//    assertEquals(0.8f, id2.getHighestPercentAgreementHint());
//  }

  /**
   * <p>Verifies that the serializers in use by the history repository
   * are in the correct modes.</p>
   * @throws Exception if an unexpected error occurs
   */
  public void testSerializerModes() throws Exception {
    abstract class SerializerFactory {
      public abstract ObjectSerializer makeSerializer();
    }

    SerializerFactory[] actions = new SerializerFactory[] {
        new SerializerFactory() {
          public ObjectSerializer makeSerializer() {
            return repository.makeAuStateSerializer();
          }
        },
        new SerializerFactory() {
          public ObjectSerializer makeSerializer() {
            return repository.makeDamagedNodeSetSerializer();
          }
        },
        new SerializerFactory() {
          public ObjectSerializer makeSerializer() {
            return repository.makeIdentityAgreementListSerializer();
          }
        },
        new SerializerFactory() {
          public ObjectSerializer makeSerializer() {
            return repository.makeNodeStateSerializer();
          }
        },
        new SerializerFactory() {
          public ObjectSerializer makeSerializer() {
            return repository.makePollHistoriesSerializer();
          }
        },
    };

    // For each variant action...
    for (int action = 0 ; action < actions.length ; ++action) {
      log.debug("Starting with action " + action);
      ObjectSerializer serializer = actions[action].makeSerializer();
      assertEquals(ObjectSerializer.FAILED_DESERIALIZATION_RENAME,
                   serializer.getFailedDeserializationMode());

      // CASTOR: Which concrete class is returned may change over time
      assertTrue(serializer instanceof CXSerializer);
      CXSerializer cxSerializer = (CXSerializer)serializer;
      assertEquals(CXSerializer.getCompatibilityModeFromConfiguration(),
                   cxSerializer.getCompatibilityMode());
    }
  }
  
  /**
   *  Make sure that we have one (and only one) dated peer id set 
   */
  public void testGetNoAuPeerSet() {
    DatedPeerIdSet dpis1;
    DatedPeerIdSet dpis2;
    
    dpis1 = repository.getNoAuPeerSet();
    assertNotNull(dpis1);
    
    dpis2 = repository.getNoAuPeerSet();
    assertNotNull(dpis2);
    
    assertSame(dpis1, dpis2);
  }
  
  

  private PollHistoryBean createPollHistoryBean(int voteCount) throws Exception {
    PollState state = new PollState(1, "lwr", "upr", 2, 5, null, false);
    List votes = new ArrayList(voteCount);
    for (int ii=0; ii<voteCount; ii++) {
      VoteBean bean = new VoteBean();
      bean.setId(idKey);
      bean.setAgreeState(true);
      bean.setChallengeString("1234");
      bean.setHashString("2345");
      bean.setVerifierString("3456");
      votes.add(bean.getVote());
    }
    return new PollHistoryBean(new PollHistory(state, 0, votes));
  }

  public static void configHistoryParams(String rootLocation)
    throws IOException {
    ConfigurationUtil.addFromArgs(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
                                  rootLocation,
                                  LockssRepositoryImpl.PARAM_CACHE_LOCATION,
                                  rootLocation,
                                  IdentityManager.PARAM_LOCAL_IP,
                                  "127.0.0.7");
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestHistoryRepositoryImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
