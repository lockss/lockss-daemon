/*
 * $Id: TestHistoryRepositoryImpl.java,v 1.38 2004-01-20 18:22:49 tlipkis Exp $
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


package org.lockss.state;

import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.daemon.TestConfiguration;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.protocol.LcapIdentity;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.exolab.castor.mapping.Mapping;

public class TestHistoryRepositoryImpl extends LockssTestCase {
  private String tempDirPath;
  private String idKey;
  private HistoryRepositoryImpl repository;
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = new MockLockssDaemon();
    theDaemon.startDaemon();
    mau = new MockArchivalUnit();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    configHistoryParams(tempDirPath);
    repository = new HistoryRepositoryImpl(tempDirPath);
    repository.startService();
    idKey = createIdentityKey();
  }

  public void tearDown() throws Exception {
    repository.stopService();
    super.tearDown();
  }

  public void testGetNodeLocation() throws Exception {
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    MockCachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    String location = repository.getNodeLocation(mcus);
    String expected = tempDirPath + repository.HISTORY_ROOT_NAME;
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
    String expectedStart = tempDirPath + repository.HISTORY_ROOT_NAME;
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

  public void testLoadMapping() throws Exception {
    assertNotNull(repository.mapping);
    assertNotNull(repository.getMapping());
  }

  public void testStorePollHistories() throws Exception {
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    CachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    NodeStateImpl nodeState = new NodeStateImpl(mcus, -1, null, null,
                                                repository);
    List histories = ListUtil.list(createPollHistoryBean(3), createPollHistoryBean(3),
                                   createPollHistoryBean(3), createPollHistoryBean(3),
                                   createPollHistoryBean(3));
    nodeState.setPollHistoryBeanList(histories);
    repository.storePollHistories(nodeState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath = LockssRepositoryImpl.mapUrlToFileLocation(filePath,
        "http://www.example.com/"+HistoryRepositoryImpl.HISTORY_FILE_NAME);
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    nodeState.setPollHistoryBeanList(new ArrayList());
    repository.loadPollHistories(nodeState);
    List loadedHistory = nodeState.getPollHistoryBeanList();
    assertEquals(histories.size(), loadedHistory.size());
    PollHistoryBean expect1 = (PollHistoryBean)histories.get(0);
    PollHistoryBean elem1 = (PollHistoryBean)loadedHistory.get(0);
    assertEquals(expect1.type, elem1.type);
    assertEquals(expect1.lwrBound, elem1.lwrBound);
    assertEquals(expect1.uprBound, elem1.uprBound);
    assertEquals(expect1.status, elem1.status);
    assertEquals(expect1.startTime, elem1.startTime);
    assertEquals(expect1.duration, elem1.duration);
    List expectBeans = (List)expect1.getVoteBeans();
    List elemBeans = (List)elem1.getVoteBeans();
    assertEquals(expectBeans.size(), elemBeans.size());
    VoteBean expectVote = (VoteBean)expectBeans.get(0);
    VoteBean elemVote = (VoteBean)elemBeans.get(0);
    assertEquals(expectVote.idStr, elemVote.idStr);
    assertEquals(expectVote.getAgreeState(), elemVote.getAgreeState());
    assertEquals(expectVote.challengeStr, elemVote.challengeStr);
    assertEquals(expectVote.verifierStr, elemVote.verifierStr);
    assertEquals(expectVote.hashStr, elemVote.hashStr);
  }

  public void testHandleEmptyFile() throws Exception {
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    CachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    NodeStateImpl nodeState = new NodeStateImpl(mcus, -1, null, null,
                                                repository);
    nodeState.setPollHistoryBeanList(new ArrayList());
    //storing empty vector
    repository.storePollHistories(nodeState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath = LockssRepositoryImpl.mapUrlToFileLocation(filePath,
        "http://www.example.com/"+HistoryRepositoryImpl.HISTORY_FILE_NAME);
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    nodeState.setPollHistoryBeanList(new ArrayList());
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

    nodeState.setPollHistoryBeanList(new ArrayList());
    repository.loadPollHistories(nodeState);
    assertEquals(0, nodeState.pollHistories.size());
    assertFalse(xmlFile.exists());
    xmlFile = new File(filePath + ".old");
    assertTrue(xmlFile.exists());
  }


  public void testStoreAuState() throws Exception {
    HashSet strCol = new HashSet();
    strCol.add("test");
    AuState auState = new AuState(mau, 123, 321, 456, strCol, repository);
    repository.storeAuState(auState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath += HistoryRepositoryImpl.AU_FILE_NAME;
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    auState = null;
    auState = repository.loadAuState(mau);
    assertEquals(123, auState.getLastCrawlTime());
    assertEquals(321, auState.getLastTopLevelPollTime());
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
    damNodes.nodes.add("test1");
    damNodes.nodes.add("test2");
    assertTrue(damNodes.contains("test1"));
    assertTrue(damNodes.contains("test2"));
    assertFalse(damNodes.contains("test3"));

    repository.storeDamagedNodeSet(damNodes);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath += HistoryRepositoryImpl.DAMAGED_NODES_FILE_NAME;
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    damNodes = null;
    damNodes = repository.loadDamagedNodeSet(mau);
    assertTrue(damNodes.contains("test1"));
    assertTrue(damNodes.contains("test2"));
    assertFalse(damNodes.contains("test3"));
    assertEquals(mau.getAuId(), damNodes.theAu.getAuId());
  }

  public void testStoreOverwrite() throws Exception {
    AuState auState = new AuState(mau, 123, 321, -1, null, repository);
    repository.storeAuState(auState);
    String filePath = LockssRepositoryImpl.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath += HistoryRepositoryImpl.AU_FILE_NAME;
    File xmlFile = new File(filePath);
    FileInputStream fis = new FileInputStream(xmlFile);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamUtil.copy(fis, baos);
    String expectedStr = baos.toString();

    auState = new AuState(mau, 1234, 4321, -1, null, repository);
    repository.storeAuState(auState);

    auState = null;
    auState = repository.loadAuState(mau);
    assertEquals(1234, auState.getLastCrawlTime());
    assertEquals(4321, auState.getLastTopLevelPollTime());
    assertEquals(mau.getAuId(), auState.getArchivalUnit().getAuId());

    auState = new AuState(mau, 123, 321, -1, null, repository);
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

  private String createIdentityKey() throws Exception {
    theDaemon.getIdentityManager().findIdentity(IPAddr.getByName("127.0.0.1"));
    return "127.0.0.1";
  }

  public static void configHistoryParams(String rootLocation)
    throws IOException {
    Properties p = new Properties();
    p.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION, rootLocation);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestHistoryRepositoryImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
