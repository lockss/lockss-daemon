/*
 * $Id: TestHistoryRepositoryImpl.java,v 1.10 2003-02-26 21:34:52 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.lockss.test.*;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.daemon.TestConfiguration;
import org.lockss.util.*;
import org.exolab.castor.mapping.Mapping;
import java.util.Collection;
import org.lockss.protocol.LcapIdentity;
import java.net.InetAddress;
import org.lockss.protocol.IdentityManager;

public class TestHistoryRepositoryImpl extends LockssTestCase {
  private String tempDirPath;
  private String idKey;
  private HistoryRepositoryImpl repository;
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  public TestHistoryRepositoryImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = new MockLockssDaemon(null);
    theDaemon.startDaemon();
    mau = new MockArchivalUnit();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    configHistoryParams(tempDirPath);
    repository = new HistoryRepositoryImpl(tempDirPath);
    idKey = createIdentityKey();
  }

  public void testGetNodeLocation() throws Exception {
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    MockCachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    String location = repository.getNodeLocation(mcus);
    String expected = tempDirPath + repository.HISTORY_ROOT_NAME;
    expected = FileLocationUtil.mapAuToFileLocation(expected, mau);
    expected = FileLocationUtil.mapUrlToFileLocation(expected,
        "http://www.example.com");

    assertEquals(expected, location);
  }

  public void testGetMapping() throws Exception {
    assertNotNull(repository.getMapping());
  }

  public void testStorePollHistories() throws Exception {
    MockCachedUrlSetSpec mspec =
        new MockCachedUrlSetSpec("http://www.example.com", null);
    CachedUrlSet mcus = new MockCachedUrlSet(mau, mspec);
    NodeStateImpl nodeState = new NodeStateImpl(mcus, null, null, repository);
    List histories = ListUtil.list(createPollHistoryBean(3), createPollHistoryBean(3),
                                   createPollHistoryBean(3), createPollHistoryBean(3),
                                   createPollHistoryBean(3));
    nodeState.setPollHistoryBeanList(histories);
    repository.storePollHistories(nodeState);
    String filePath = FileLocationUtil.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath = FileLocationUtil.mapUrlToFileLocation(filePath,
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

  public void testStoreAuState() throws Exception {
    AuState auState = new AuState(mau, 123, 321);
    repository.storeAuState(auState);
    String filePath = FileLocationUtil.mapAuToFileLocation(tempDirPath +
        HistoryRepositoryImpl.HISTORY_ROOT_NAME, mau);
    filePath += HistoryRepositoryImpl.AU_FILE_NAME;
    System.out.println("path: "+filePath);
    File xmlFile = new File(filePath);
    assertTrue(xmlFile.exists());

    auState = null;
    auState = repository.loadAuState(mau);
    assertEquals(123, auState.getLastCrawlTime());
    assertEquals(321, auState.getLastTopLevelPollTime());
    assertEquals(mau.getAUId(), auState.getArchivalUnit().getAUId());
  }

  private PollHistoryBean createPollHistoryBean(int voteCount) throws Exception {
    PollState state = new PollState(1, "lwr", "upr", 2, 5, null);
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
    theDaemon.getIdentityManager().findIdentity(InetAddress.getByName("127.0.0.1"));
    return "127.0.0.1";
  }

  public static void configHistoryParams(String rootLocation)
    throws IOException {
    String s = HistoryRepositoryImpl.PARAM_HISTORY_LOCATION + "=" + rootLocation;
    String s2 = HistoryRepositoryImpl.PARAM_MAPPING_FILE_LOCATION +
               "=src/org/lockss/state/pollmapping.xml";
    TestConfiguration.setCurrentConfigFromUrlList(ListUtil.list(FileUtil.urlOfString(s),
        FileUtil.urlOfString(s2)));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestHistoryRepositoryImpl.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
