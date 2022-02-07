package org.lockss.laaws;

import static org.lockss.laaws.V2AuMover.compileRegexps;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.protocol.MockIdentityManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockPlugin;

public class TestV2AuMover extends LockssTestCase {

  V2AuMover auMover;
  String tempDirPath;
  String testUser = "tester";
  String testPassword = "testPass";
  String defaultCfgUrl = "http://"+ V2AuMover.DEFAULT_HOSTNAME + ":" + V2AuMover.DEFAULT_CFG_PORT;
  String defaultRsUrl = "http://"+ V2AuMover.DEFAULT_HOSTNAME + ":" + V2AuMover.DEFAULT_RS_PORT;
  private long numAus;
  private long numUrls;
  private long numArtifacts;
  private long totTime;
  private long numErrors;
  private long numBytes;
  private long numContentBytes;
  private String aPluginRegex="(aplugin)";
  private String bPluginRegex="(bplugin)";
  private String cPluginRegex="(cplugin)";
  private static int HEADER_LENGTH=4;
  private MockIdentityManager idmgr;
  private MockLockssDaemon theDaemon;

  String[] reportLines = {
      "AU Name: au1",
      "AU ID: au1",
      "urlsMoved: 10  artifactsMoved: 10  contentBytesMoved: 900  totalBytesMoved: 1000  byteRate: 3.26KB/s  errors: 0  totalRuntime: 300ms",
      "",
      "AU Name: au2",
      "AU ID: au2",
      "urlsMoved: 3517  artifactsMoved: 35723  contentBytesMoved: 17261845523  totalBytesMoved: 17307972727  byteRate: 628KB/s  errors: 1  totalRuntime: 7h28m34s",
      " cu2 Attempt to move artifact failed.",
      "",
      "AU Name: au3",
      "AU ID: au3",
      "urlsMoved: 11  artifactsMoved: 22  contentBytesMoved: 1626829  totalBytesMoved: 1661366  byteRate: 400KB/s  errors: 3  totalRuntime: 4054ms",
      " cu1: Attempt to move artifact failed.",
      " cu5: Attempt to commit artifact failed.",
      " cu80: Attempt to commit artifact failed.",
      "",
      "AusMoved: 3  urlsMoved: 3538  artifactsMoved: 35755  contentBytesMoved: 17263473252  totalBytesMoved: 17309635093  byteRate: 628KB/s  errors: 4  totalRuntime: 7h28m38s"
  };

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    idmgr = new MockIdentityManager();
    idmgr.initService(theDaemon);
    theDaemon.setIdentityManager(idmgr);
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.addFromArgs(V2AuMover.PARAM_REPORT_DIR, tempDirPath);
    String reportFile = new File(tempDirPath, "v2AuMigration.txt").toString();
    auMover = new V2AuMover();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testIsAvailable() {
    MockArchivalUnit mau = new MockArchivalUnit();
    assertTrue(auMover.isAvailable());
    auMover.getAuMoveQueue().add(mau);
    assertFalse(auMover.isAvailable());
  }

  public void testInitRequest() {
    auMover = new V2AuMover();
    // test file defaults
    auMover.initRequest(null, testUser,testPassword);
    // check the locals
    assertEquals(defaultCfgUrl,auMover.getCfgAccessUrl());
    assertEquals(defaultRsUrl,auMover.getRepoAccessUrl());
    assertEquals(testUser,auMover.getUserName());
    assertEquals(testPassword, auMover.getUserPass());

    // test a different host
    String base="http://mockhost:";
    String user="user1";
    String passwd="passwd1";
    auMover = new V2AuMover();
    auMover.initRequest("mockhost", user,passwd);
    assertEquals(base+24620,auMover.getCfgAccessUrl());
    assertEquals(base+24610,auMover.getRepoAccessUrl());
    assertEquals(user,auMover.getUserName());
    assertEquals(passwd, auMover.getUserPass());

    // test config from configuration file
    ConfigurationUtil.addFromArgs(
      V2AuMover.PARAM_HOSTNAME,"test.com",
      V2AuMover.PARAM_CFG_PORT, "25620",
      V2AuMover.PARAM_RS_PORT, "25610");
    base="http://test.com:";
    user="user2";
    passwd="passwd2";
    auMover = new V2AuMover();
    auMover.initRequest(null, user, passwd);
    assertEquals(base+25620,auMover.getCfgAccessUrl());
    assertEquals(base+25610,auMover.getRepoAccessUrl());
    assertEquals(user,auMover.getUserName());
    assertEquals(passwd, auMover.getUserPass());

    auMover = new V2AuMover();
    try {
      // test null entries
      auMover.initRequest(null, null, null);
      fail("null input should throw IllegalArgumentException");
    }
    catch(IllegalArgumentException iae) {

    }

  }

  public void testReportFile() {
    auMover.startReportFile();
    numAus=0;
    numUrls=0;
    numArtifacts=0;
    totTime=0;
    numErrors=0;
    numBytes=0;
    numContentBytes=0;
    auMover.setCurrentAu("au1");
    List<String> errs = new ArrayList<>();
    addAu("au1", 10,10,1000, 900, 300, 0, errs);
    auMover.updateReport("au1");
    errs.add("cu2 Attempt to move artifact failed.");
    addAu("au2", 3517,35723, 17307972727L, 17261845523L,26914000,1, errs);
    auMover.updateReport("au2");
    errs.clear();
    errs.add("cu1: Attempt to move artifact failed.");
    errs.add("cu5: Attempt to commit artifact failed.");
    errs.add("cu80: Attempt to commit artifact failed.");
    addAu("au3",11,22,1661366,1626829,  4054, 3, errs);
    auMover.updateReport("au3");
    auMover.closeReport();
    Path myPath = auMover.getReportFile().toPath();
    List< String > lines = null;
    try {
      lines = Files.readAllLines(myPath, StandardCharsets.UTF_8);
    } catch (IOException ioe) {
      fail("IOException thrown",ioe);
    }
     // TODO - Add test for error exits.
    assertEquals(reportLines.length + HEADER_LENGTH, lines.size());
    // we're only checking the report lines.
    for(int i=0; i < reportLines.length; i++) {
      assertEquals(reportLines[i],lines.get(i+HEADER_LENGTH));
    }

  }

  public void testMoveOneAu() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit("mockAuId");
    MockV2AuMover mover = new MockV2AuMover();
    mover.moveOneAu(null, testUser,testPassword, mau);
    assertTrue(mover.getAuMoveQueue().isEmpty());
    List<String> movedAus = mover.getMovedAus();
    assertEquals(1, movedAus.size());
    assertEquals("mockAuId",movedAus.get(0));
  }

  public void testMoveAllAus() throws Exception{
    MockV2AuMover mover = new MockV2AuMover();
    MockLockssDaemon daemon = getMockLockssDaemon();
    MyMockPluginManager mpm = new MyMockPluginManager();
    daemon.setPluginManager(mpm);
    List<ArchivalUnit> aus = makeMockAuList();
    mpm.setAllAus(aus);
    mover.setPluginManager(mpm);
    // move all unfiltered aus
    mover.moveAllAus(null, testUser,testPassword, null);
    assertTrue(mover.getAuMoveQueue().isEmpty());
    List<String> movedAus = mover.getMovedAus();
    assertEquals(aus.size(), movedAus.size());
    for (ArchivalUnit au : aus) {
      assertTrue(movedAus.contains(au.getAuId()));
    }
  }

  public void testMoveAllAusWithFilter() throws Exception{
    MockV2AuMover mover = new MockV2AuMover();
    MockLockssDaemon daemon = getMockLockssDaemon();
    MyMockPluginManager mpm = new MyMockPluginManager();
    daemon.setPluginManager(mpm);
    List<ArchivalUnit> aus = makeMockAuList();
    mpm.setAllAus(aus);
    mover.setPluginManager(mpm);
    List<String> filters=Arrays.asList(aPluginRegex,bPluginRegex);
    List<Pattern> selPatterns = compileRegexps(filters);
    // move all unfiltered aus
    mover.moveAllAus(null, testUser,testPassword, selPatterns);
    assertTrue(mover.getAuMoveQueue().isEmpty());
    List<String> movedAus = mover.getMovedAus();
    assertNotEquals(aus.size(),movedAus.size());
    assertEquals(5, movedAus.size());
    assertDoesNotContain(movedAus,aus.get(5).getAuId());
  }

  private void addAu(String auName, long urls, long artifacts, long bytes, long contentBytes, long runTime, long errors,List<String> errs){
    numAus++;
    numUrls+=urls;
    numArtifacts+=artifacts;
    numBytes+=bytes;
    numContentBytes+=contentBytes;
    totTime+=runTime;
    numErrors+=errors;
    auMover.setCurrentAu(auName);
    auMover.setAuCounters(urls, artifacts, bytes, contentBytes, runTime, errors,errs);
    auMover.setTotalCounters(numAus, numUrls, numArtifacts, numBytes, numContentBytes, totTime, numErrors);

  }

  List<ArchivalUnit> makeMockAuList() {
    // Test with dummy AUs
    MockPlugin plugin = new MockPlugin();
    plugin.setPluginId("aplugin");
    ArchivalUnit au1 = new MockArchivalUnit(plugin, "aplugin|auid1");
    ArchivalUnit au2 = new MockArchivalUnit(plugin, "aplugin|auid2");
    ArchivalUnit au3 = new MockArchivalUnit(plugin, "aplugin|auid3");
    plugin = new MockPlugin();
    plugin.setPluginId("bplugin");
    ArchivalUnit au4 = new MockArchivalUnit(plugin, "bplugin|auid1");
    ArchivalUnit au5 = new MockArchivalUnit(plugin, "bplugin|auid2");
    plugin.setPluginId("cplugin");
    ArchivalUnit au6 = new MockArchivalUnit(plugin, "cplugin|auid1");
    return Arrays.asList(au1, au2, au3,au4, au5,au6);
  }

  static class MockV2AuMover extends V2AuMover {
    List<String> movedAus= new ArrayList<>();

   public MockV2AuMover() {
      super();
    }

    protected void moveAu(ArchivalUnit au) throws IOException {
      movedAus.add(au.getAuId());
      getAuMoveQueue().remove(au);
      setCurrentAu(null);
      moveNextAu();
    }

    @Override
    void getV2Aus() throws IOException {

    }

    @Override
    boolean v2ServicesUnavailable()  throws IOException {
     return false;
    }

    protected List<String> getMovedAus() {
      return movedAus;
    }

    protected void setPluginManager(PluginManager pmgr) {
     super.pluginManager = pmgr;
    }

  }

  static class MyMockPluginManager extends PluginManager {
    List<ArchivalUnit> allAus = new ArrayList<>();

    void setAllAus(List<ArchivalUnit> allAus) {
      this.allAus = allAus;
    }

    public List<ArchivalUnit> getAllAus() {
      return allAus;
    }

  }

}
