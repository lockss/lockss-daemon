/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.io.*;
import java.util.*;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.simulated.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class FuncNewContentCrawler extends LockssTestCase {
  static Logger log = Logger.getLogger("FuncNewContentCrawler");

  private MySimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  private NoPauseCrawlManagerImpl crawlMgr;
  private static final int DEFAULT_MAX_DEPTH = 1000;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static int maxDepth=DEFAULT_MAX_DEPTH;

  public static void main(String[] args) throws Exception {
    FuncNewContentCrawler test = new FuncNewContentCrawler();
    if (args.length>0) {
      try {
        maxDepth = Integer.parseInt(args[0]);
      } catch (NumberFormatException ex) { }
    }

    test.setUp(maxDepth);
    test.testRunSelf();
    test.tearDown();
  }

  public void setUp() throws Exception {
    super.setUp();
    this.setUp(DEFAULT_MAX_DEPTH);
  }

  public void setUp(int max) throws Exception {

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH, ""+max);
    maxDepth=max;
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);
    // crawlMgr.startService(); below is needed so init happens, this
    // prevents crawl starter thread from doing anything.
    props.setProperty(CrawlManagerImpl.PARAM_START_CRAWLS_INTERVAL, "-1");

    //test that we don't cache a file that is globally excluded
    props.setProperty(CrawlManagerImpl.PARAM_EXCLUDE_URL_PATTERN,
		      ".*(branch1/.*){3,}");
    // Crawl rule check in BaseCachedUrl.hasContent() interferes with
    // shouldBeCached() counter below - disable it.
    props.setProperty(BaseCachedUrl.PARAM_INCLUDED_ONLY, "false");
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    crawlMgr = new NoPauseCrawlManagerImpl();
    theDaemon.setCrawlManager(crawlMgr);
    crawlMgr.initService(theDaemon);
    crawlMgr.startService();


    sau =
      (MySimulatedArchivalUnit)
      PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
					 simAuConfig(tempDirPath));
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }


  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "3");
    conf.put("branch", "1");
    conf.put("numFiles", "2");
    conf.put("fileTypes",
	     "" + SimulatedContentGenerator.FILE_TYPE_BIN);
    conf.put("binFileSize", ""+fileSize);
    return conf;
  }

  public void testRunSelf() throws Exception {
    createContent();

    // get the root of the simContent
    String simDir = sau.getSimRoot();

    NoCrawlEndActionsFollowLinkCrawler crawler = crawlContent();

    // read all the files links from the root of the simcontent
    // check the link level of the file and see if it contains
    // in myCUS (check if the crawler crawl within the max. depth)
    CachedUrlSet myCUS = sau.getAuCachedUrlSet();
    File dir = new File(simDir);
    if(dir.isDirectory()) {
       File f[] = dir.listFiles();
       log.debug("Checking simulated content.");
       checkThruFileTree(f, myCUS);

       log.debug("Check finished.");
    } else {
      log.error("Error: The root path of the simulated" +
		" content ["+ dir +"] is not a directory");
    }

    // Test PluginManager.getAuContentSize(), just because this is a
    // convenient place to do it.  (And NewContentCrawler calls it at the
    // end.)  If the simulated AU params are changed, or
    // SimulatedContentGenerator is changed, this number may have to
    // change.
    assertEquals(19262, AuUtil.getAuContentSize(sau, true));

    List sbc = ((MySimulatedArchivalUnit)sau).sbc;
    Bag b = new HashBag(sbc);
    Set uniq = new HashSet(b.uniqueSet());
    for (Iterator iter = uniq.iterator(); iter.hasNext(); ) {
      b.remove(iter.next(), 1);
    }
    // Permission pages get checked twice.  Hard to avoid that, so allow it
    b.removeAll(sau.getPermissionUrls());
    assertEmpty("shouldBeCached() called multiple times on same URLs.", b);

    String th = "text/html";
    String tp = "text/plain";
    String[] ct = { null, null, null, tp, tp, th, th, tp, tp, th, tp};
    Bag ctb = new HashBag(ListUtil.fromArray(ct));
    CrawlRateLimiter crl = crawlMgr.getCrawlRateLimiter(crawler);
    assertEquals(ctb, new HashBag(crawlMgr.getPauseContentTypes(crawler)));
  }

  //recursive caller to check through the whole file tree
  private void checkThruFileTree(File f[], CachedUrlSet myCUS){
    String exclUrlStem = "http://www.example.com/branch1/branch1/branch1/";

    for (int ix=0; ix<f.length; ix++) {
	 if (f[ix].isDirectory()) {
	   // get all the files and links there and iterate
	   checkThruFileTree(f[ix].listFiles(), myCUS);
	 } else {

	   // get the f[ix] 's level information
	   String fileUrl = sau.mapContentFileNameToUrl(f[ix].getAbsolutePath());
	   int fileLevel = sau.getLinkDepth(fileUrl);
	   log.debug2("File: " + fileUrl + " in Level " + fileLevel);

	   CachedUrl cu = sau.makeCachedUrl(fileUrl);
	   if (fileLevel <= maxDepth
	       && !StringUtil.startsWithIgnoreCase(fileUrl, exclUrlStem)) {
	     assertTrue(cu + " has no content", cu.hasContent());
	   } else {
	     assertFalse(cu + " has content when it shouldn't",
			 cu.hasContent());
	   }
	 }
    }
    return; // when all "File" in the array are checked
  }

  private void createContent() {
    log.debug("Generating tree of size 3x1x2 with "+fileSize
	      +"byte files...");
    sau.generateContentTree();
  }

  private NoCrawlEndActionsFollowLinkCrawler crawlContent() {
    log.debug("Crawling tree...");
    NoCrawlEndActionsFollowLinkCrawler crawler =
      new NoCrawlEndActionsFollowLinkCrawler(sau, new MockAuState());
    crawler.setCrawlManager(crawlMgr);
    crawlMgr.addToRunningCrawls(crawler.getAu(), crawler);
    crawler.doCrawl();
    crawlMgr.removeFromRunningCrawls(crawler);
    return crawler;
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
	throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new MySimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }
  }

  public static class MySimulatedArchivalUnit extends SimulatedArchivalUnit {
    List sbc = new ArrayList();

    public MySimulatedArchivalUnit(Plugin owner) {
      super(owner);
    }

    public boolean shouldBeCached(String url) {
      sbc.add(url);
      return super.shouldBeCached(url);
    }
  }

}
