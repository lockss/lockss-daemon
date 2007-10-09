package org.lockss.poller.v3;

import java.util.*;
import java.io.*;

import org.lockss.config.ConfigManager;
import org.lockss.daemon.*;
import org.lockss.app.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.*;
import org.lockss.util.ByteArray;
import org.lockss.util.TimeBase;
import org.lockss.poller.*;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.V3LcapMessage;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.hasher.*;

public class TestV3PollFactory extends LockssTestCase {
  
  private MockLockssDaemon theDaemon;
  private ArchivalUnit testAu;
  private PollManager pollManager;
  private HashService hashService;
  private IdentityManager idmgr;
  private File tempDir;
  private String tempDirPath;
  private String pluginVer;
  private MockPollSpec ps;
  private V3LcapMessage testMsg;
  private V3PollFactory thePollFactory;
  private PeerIdentity testId;

  public void setUp() throws Exception {
    super.setUp();

    File tempDir = getTempDir();
    String tempDirPath = tempDir.getAbsolutePath() + File.separator;

    TimeBase.setSimulated();
    initRequiredServices();
    pluginVer = "2";
    
    testId = idmgr.findPeerIdentity("TCP:[127.0.0.1]:9000");
    testAu = setupAu();
    ps = new MockPollSpec(testAu.getAuCachedUrlSet(), null, null,
                          Poll.V3_POLL);
    testMsg = makePollMsg();
    thePollFactory = new V3PollFactory();
  }
  
  private String[] urls =
  {
   "http://www.example.com/",
   "http://www.example.com/index.html"
  };
  
  public V3LcapMessage makePollMsg() {
    long msgDeadline = TimeBase.nowMs() + 500000;
    long voteDuration = 1000;
    V3LcapMessage msg =
      new V3LcapMessage(testAu.getAuId(), "key", "3",
                        ByteArray.makeRandomBytes(20),
                        ByteArray.makeRandomBytes(20),
                        V3LcapMessage.MSG_POLL,
                        10000, testId, tempDir, theDaemon);
    msg.setVoteDuration(voteDuration);
    msg.setEffortProof(ByteArray.makeRandomBytes(20));
    return msg;
  }

  private MockArchivalUnit setupAu() {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId("mock");
    MockPlugin plug = new MockPlugin(theDaemon);
    mau.setPlugin(plug);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.setEstimatedHashDuration(1000);
    List files = new ArrayList();
    for (int ix = 0; ix < urls.length; ix++) {
      MockCachedUrl cu = (MockCachedUrl)mau.addUrl(urls[ix], false, true);
      // Add mock file content.
      cu.setContent("This is content for CUS file " + ix);
      files.add(cu);
    }
    cus.setHashItSource(files);
    cus.setFlatItSource(files);
   
    return mau;
  }
  
  private void initRequiredServices() throws Exception {
    theDaemon = getMockLockssDaemon();
    pollManager = theDaemon.getPollManager();
    hashService = theDaemon.getHashService();

    theDaemon.getPluginManager();

    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();

    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(V3Poller.PARAM_QUORUM, "3");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(V3Serializer.PARAM_V3_STATE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_V3_IDENTITY, "TCP:[127.0.0.1]:9729");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    theDaemon.getSchedService().startService();
    hashService.startService();
    MockAuState aus = new MockAuState(testAu);
    aus.setLastCrawlTime(100);
    MockNodeManager nm = new MockNodeManager();
    nm.setAuState(aus);
    theDaemon.setNodeManager(nm, testAu);
    theDaemon.getActivityRegulator(testAu).startService();
    pollManager.startService();
  }
  
  public void testCreatePollPoller() throws Exception {
    Poll p = thePollFactory.createPoll(ps, theDaemon, testId, 1000,
                                       "SHA1", null);
    assertNotNull(p);
    assertTrue(p instanceof V3Poller);
  }
   
}
