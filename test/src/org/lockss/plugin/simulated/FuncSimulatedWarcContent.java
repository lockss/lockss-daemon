/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.state.HistoryRepositoryImpl;
import junit.framework.*;

/**
 * Functional test of the WARC file simulated content
 * generator.  It does not test the regular simulated
 * content generator which is used to generate the
 * content that is then packed into the WARC file and
 * deleted.
 *
 * @author  David S. H. Rosenthal
 * @author  Felix Ostrowski
 * @version 0.0
 */

public class FuncSimulatedWarcContent extends LockssTestCase {
  static final Logger log = Logger.getLogger("FuncSimulatedWarcContent");

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;

  String warcFileName = null;

  public FuncSimulatedWarcContent(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.doWarcFile", "true");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.getHashService();

    theDaemon.setDaemonInited(true);

    theDaemon.getPluginManager().startService();
    theDaemon.getHashService().startService();

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
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

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "2");
    conf.put("fileTypes",
	     "" + (SimulatedContentGenerator.FILE_TYPE_HTML +
		   SimulatedContentGenerator.FILE_TYPE_TXT));
    conf.put("badCachedFileLoc", "2,2");
    conf.put("badCachedFileNum", "2");
    return conf;
  }

  public void testSimulatedWarcContent() throws Exception {
    createContent();
    crawlContent();
    checkContent();
  }

  protected void createContent() {
    log.debug("createContent()");
    SimulatedContentGenerator scgen = sau.getContentGenerator();
    scgen.setOddBranchesHaveContent(true);

    sau.deleteContentTree();
    sau.generateContentTree();
    assertTrue(scgen.isContentTree());
    // Now find the WARC file
    String fileRoot = sau.getRootDir() + SimulatedContentGenerator.ROOT_NAME;
    log.debug("root dir " + fileRoot);
    File rootDir = new File(fileRoot);
    assertTrue(rootDir.exists());
    assertTrue(rootDir.isDirectory());
    String[] names = rootDir.list();
    for (int i = 0; i < names.length; i++) {
      log.debug3("Dir entry " + names[i]);
      if (names[i].endsWith(".warc.gz") || names[i].endsWith(".warc")) {
        warcFileName = names[i];
        break;
      }
    }
    assertNotNull(warcFileName);
    log.debug("WARC file name " + warcFileName);
  }

  protected void crawlContent() {
    log.debug("crawlContent()");
    Crawler crawler =
      new NoCrawlEndActionsFollowLinkCrawler(sau, new MockAuState());
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

    // XXX assumpiton here that simulated content fits into a single warc.gz
    expectedA = new String[2];
    expectedA[0] = sau.getUrlRoot() + "/" + warcFileName;
    expectedA[1] = sau.getUrlRoot() + "/index.html";
    assertIsomorphic(expectedA, childL);
  }

  public static void main(String[] argv) {
    String[] testCaseList = {
      FuncSimulatedWarcContent.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  public static Test suite() {
    return new TestSuite(FuncSimulatedWarcContent.class);
  }

}
