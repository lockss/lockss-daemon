/*
 * $Id: TestHistoryRepositoryImpl.java,v 1.60 2006-09-16 07:17:05 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
  public static class WithCastor extends TestHistoryRepositoryImpl {
    public void setUp() throws Exception {
      super.setUp();
      ConfigurationUtil.addFromArgs(CXSerializer.PARAM_COMPATIBILITY_MODE,
                                    Integer.toString(CXSerializer.CASTOR_MODE));
    }
  }

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
    mau = new MockArchivalUnit();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    configHistoryParams(tempDirPath);
    repository = (HistoryRepositoryImpl)
        HistoryRepositoryImpl.createNewHistoryRepository(mau);
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
    String expected = tempDirPath + HistoryRepositoryImpl.HISTORY_ROOT_NAME;
    expected = LockssRepositoryImpl.mapAuToFileLocation(expected, mau);
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
    String expectedStart = tempDirPath + HistoryRepositoryImpl.HISTORY_ROOT_NAME;
    expectedStart = LockssRepositoryImpl.mapAuToFileLocation(expectedStart,
        mau);
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
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
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
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
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
    filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
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


  public void testStoreAuState() throws Exception {
    HashSet strCol = new HashSet();
    strCol.add("test");
    AuState auState = new AuState(mau, 123000, 321000, 456000,
				  strCol, 2, repository);
    repository.storeAuState(auState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath += HistoryRepositoryImpl.AU_FILE_NAME;
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    auState = null;
    auState = repository.loadAuState();
    assertEquals(123000, auState.getLastCrawlTime());
    assertEquals(321000, auState.getLastTopLevelPollTime());
    assertEquals(2, auState.getClockssSubscriptionStatus());
    // doesn't store last treewalk time, so should reset to -1
    assertEquals(-1, auState.getLastTreeWalkTime());
    assertEquals(mau.getAuId(), auState.getArchivalUnit().getAuId());

    // check crawl urls
    Collection col = auState.getCrawlUrls();
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
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
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
    AuState auState = new AuState(mau, 123, 321, -1, null, 1, repository);
    repository.storeAuState(auState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath += HistoryRepositoryImpl.AU_FILE_NAME;
    File xmlFile = new File(filePath);
    FileInputStream fis = new FileInputStream(xmlFile);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamUtil.copy(fis, baos);
    String expectedStr = baos.toString();

    auState = new AuState(mau, 1234, 4321, -1, null, 1, repository);
    repository.storeAuState(auState);

    auState = null;
    auState = repository.loadAuState();
    assertEquals(1234, auState.getLastCrawlTime());
    assertEquals(4321, auState.getLastTopLevelPollTime());
    assertEquals(mau.getAuId(), auState.getArchivalUnit().getAuId());

    auState = new AuState(mau, 123, 321, -1, null, 1, repository);
    repository.storeAuState(auState);
    fis = new FileInputStream(xmlFile);
    baos = new ByteArrayOutputStream(expectedStr.length());
    StreamUtil.copy(fis, baos);
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
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
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

  public void testStoreIdentityAgreements() throws Exception {
    IdentityManager.IdentityAgreement id1 =
      new IdentityManager.IdentityAgreement(testID1);
    id1.setLastAgree(123);
    id1.setLastDisagree(321);
    IdentityManager.IdentityAgreement id2 =
      new IdentityManager.IdentityAgreement(testID2);
    id2.setLastAgree(456);
    id2.setLastDisagree(654);

    repository.storeIdentityAgreements(ListUtil.list(id1, id2));
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath += HistoryRepositoryImpl.IDENTITY_AGREEMENT_FILE_NAME;
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    List idList = repository.loadIdentityAgreements();
    assertEquals(2, idList.size());
    id1 = (IdentityManager.IdentityAgreement)idList.get(0);
    assertNotNull(id1);
    assertSame(testID1, idmgr.stringToPeerIdentity(id1.getId()));
    assertEquals(123, id1.getLastAgree());
    assertEquals(321, id1.getLastDisagree());

    id2 = (IdentityManager.IdentityAgreement)idList.get(1);
    assertSame(testID2, idmgr.stringToPeerIdentity(id2.getId()));
    assertEquals(456, id2.getLastAgree());
    assertEquals(654, id2.getLastDisagree());
  }

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
