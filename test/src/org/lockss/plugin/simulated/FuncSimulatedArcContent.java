/*
 * $Id: FuncSimulatedArcContent.java,v 1.4 2008-05-06 21:35:36 dshr Exp $
 */

/*
 Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import java.util.*;
import java.io.*;
import java.security.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.crawler.NewContentCrawler;
import org.lockss.state.HistoryRepositoryImpl;
import junit.framework.*;

/**
 * Functional test of the ARC file simulated content
 * generator.  It does not test the regular simulated
 * content generator which is used to generate the
 * content that is then packed into the ARC file and
 * deleted.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class FuncSimulatedArcContent extends LockssTestCase {
  static final Logger log = Logger.getLogger("FuncSimulatedArcContent");

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  private String auId;
  private String auId2;

  String arcFileName = null;

  public FuncSimulatedArcContent(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String tempDirPath2 = getTempDir().getAbsolutePath() + File.separator;
    String auIdStr = "org|lockss|plugin|simulated|SimulatedPlugin.root~" +
      PropKeyEncoder.encode(tempDirPath);
    String auId2Str = "org|lockss|plugin|simulated|SimulatedPlugin.root~" +
      PropKeyEncoder.encode(tempDirPath2);
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION,
                      tempDirPath);
    props.setProperty("org.lockss.au." + auIdStr + ".root", tempDirPath);
    props.setProperty("org.lockss.au." + auIdStr + ".depth", "2");
    props.setProperty("org.lockss.au." + auIdStr + ".branch", "2");
    props.setProperty("org.lockss.au." + auIdStr + ".numFiles", "2");

    props.setProperty("org.lockss.au." + auIdStr + ".badCachedFileLoc", "2,2");
    props.setProperty("org.lockss.au." + auIdStr + ".badCachedFileNum", "2");
    props.setProperty("org.lockss.au." + auId2Str + ".badCachedFileLoc", "2,2");
    props.setProperty("org.lockss.au." + auId2Str + ".badCachedFileNum", "2");

    props.setProperty("org.lockss.au." + auId2Str + ".root", tempDirPath2);
    props.setProperty("org.lockss.au." + auId2Str + ".depth", "2");
    props.setProperty("org.lockss.au." + auId2Str + ".branch", "2");
    props.setProperty("org.lockss.au." + auId2Str + ".numFiles", "2");
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.doArcFile", "true");

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.getHashService();

    theDaemon.setDaemonInited(true);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    // form proper ids
    auId = auIdStr.replace('.', '&');
    auId2 = auId2Str.replace('.', '&');

    sau =
      (SimulatedArchivalUnit)theDaemon.getPluginManager().getAuFromId(auId);

    theDaemon.getPluginManager().startService();

    theDaemon.getHashService().startService();

    theDaemon.getHistoryRepository(sau).startService();
    theDaemon.getLockssRepository(sau).startService();
    theDaemon.getNodeManager(sau).startService();
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepository(sau).stopService();
    theDaemon.getNodeManager(sau).stopService();
    theDaemon.getPluginManager().stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getSystemMetrics().stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testSimulatedArcContent() throws Exception {
    createContent();
    crawlContent();
    checkContent();
  }

  protected void createContent() {
    log.debug("createContent()");
    SimulatedContentGenerator scgen = sau.getContentGenerator();
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_HTML +
                       SimulatedContentGenerator.FILE_TYPE_TXT);
    scgen.setOddBranchesHaveContent(true);

    sau.deleteContentTree();
    sau.generateContentTree();
    assertTrue(scgen.isContentTree());
    // Now find the ARC file
    String fileRoot = sau.getRootDir() + SimulatedContentGenerator.ROOT_NAME;
    log.debug("root dir " + fileRoot);
    File rootDir = new File(fileRoot);
    assertTrue(rootDir.exists());
    assertTrue(rootDir.isDirectory());
    String[] names = rootDir.list();
    for (int i = 0; i < names.length; i++) {
        log.debug3("Dir entry " + names[i]);
	if (names[i].endsWith(".arc.gz") || names[i].endsWith(".arc")) {
	    arcFileName = names[i];
	    break;
	}
    }
    assertNotNull(arcFileName);
    log.debug("ARC file name " + arcFileName);
  }

  protected void crawlContent() {
    log.debug("crawlContent()");
    CrawlSpec spec =
      new SpiderCrawlSpec(sau.getNewContentCrawlUrls(), null);
    Crawler crawler = new NewContentCrawler(sau, spec, new MockAuState());
    crawler.doCrawl();
  }

  protected void checkContent() throws IOException {
    log.debug("checkContent()");
    checkRoot();
  }

  protected void checkRoot() {
    log.debug("checkRoot()");
    CachedUrlSet set = sau.getAuCachedUrlSet();
    Iterator setIt = set.flatSetIterator();
    ArrayList childL = new ArrayList(1);
    CachedUrlSet cus = null;
    while (setIt.hasNext()) {
      cus = (CachedUrlSet) setIt.next();
      childL.add(cus.getUrl());
    }

    String[] expectedA = new String[1];
    expectedA[0] = sau.getUrlRoot();
    assertIsomorphic(expectedA, childL);

    setIt = cus.flatSetIterator();
    childL = new ArrayList(7);
    while (setIt.hasNext()) {
      childL.add( ( (CachedUrlSetNode) setIt.next()).getUrl());
    }

    expectedA = new String[2];
    expectedA[0] = sau.getUrlRoot() + "/" + arcFileName;
    expectedA[1] = sau.getUrlRoot() + "/index.html";
    assertIsomorphic(expectedA, childL);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
      FuncSimulatedArcContent.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  public static Test suite() {
    return new TestSuite(FuncSimulatedArcContent.class);
  }

}
