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

package org.lockss.crawler;

import java.io.File;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.poller.PollManager;
import org.lockss.crawler.NewContentCrawler;
import org.lockss.protocol.*;
import org.lockss.test.*;
import java.util.Properties;
import java.util.Iterator;

public class FuncNewContentCrawler extends LockssTestCase {
  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
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
    String auId = "org|lockss|plugin|simulated|SimulatedPlugin.root~" +
      PropKeyEncoder.encode(tempDirPath);
    Properties props = new Properties();
    props.setProperty(NewContentCrawler.PARAM_MAX_CRAWL_DEPTH, ""+max);
    maxDepth=max;
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);

    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_ROOT, tempDirPath);
    // the simulated Content's depth will be (AU_PARAM_DEPTH + 1)
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_DEPTH, "3");
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_BRANCH, "1");
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_NUM_FILES, "2");
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_FILE_TYPES,
                      ""+SimulatedContentGenerator.FILE_TYPE_BIN);
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE, ""+fileSize);

//     theDaemon = new MockLockssDaemon();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();

    ConfigurationUtil.setCurrentConfigFromProps(props);

    sau =
        (SimulatedArchivalUnit)theDaemon.getPluginManager().getAllAus().get(0);
    theDaemon.getLockssRepository(sau);
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testRunSelf() throws Exception {
    createContent();

    // get the root of the simContent
    String simDir = sau.getSimRoot();

    crawlContent();

    // read all the files links from the root of the simcontent
    // check the link level of the file and see if it contains
    // in myCUS (check if the crawler crawl within the max. depth)
    CachedUrlSet myCUS = sau.getAuCachedUrlSet();
    File dir = new File(simDir);
    if(dir.isDirectory()) {
       File f[] = dir.listFiles();
       System.out.println("Starting to check through the simulated content. " +
			  "Be patient to let it finish.");
       checkThruFileTree(f, myCUS);
       
       System.out.println("Check finish.");
    } else {
      System.out.println("Error: The root path of the simulated" + 
			 " content ["+ dir +"] is not a directory");
    }

  }

  //recursive caller to check through the whole file tree
  private void checkThruFileTree(File f[], CachedUrlSet myCUS){
    for (int ix=0; ix<f.length; ix++) {
	 if (f[ix].isDirectory()) {
	   // get all the files and links there and iterate
	   checkThruFileTree(f[ix].listFiles(), myCUS);
	 } else {

	   // get the f[ix] 's level information
	   String fileUrl = sau.mapContentFileNameToUrl(f[ix].getAbsolutePath());
	   int fileLevel = sau.getLinkDepth(fileUrl);
	   System.out.println("File: " + fileUrl + " in Level " + fileLevel);
	   
	   CachedUrl cu = sau.getPlugin().makeCachedUrl(myCUS, fileUrl);
	   if (fileLevel <= maxDepth) { 
	     assertTrue(cu !=null && cu.hasContent());
	   } else {
	     assertFalse(cu.hasContent());
	   }
	 }
    }
    return; // when all "File" in the array are checked
  }

  private void createContent() {
    System.out.println("Generating tree of size 3x1x2 with "+fileSize
                       +"byte files...");
    sau.generateContentTree();
  }

  private void crawlContent() {
    System.out.println("Crawling tree...");
    CrawlSpec spec = new CrawlSpec(sau.SIMULATED_URL_START, null);
    Crawler crawler = new NewContentCrawler(sau, spec, new MockAuState());
    crawler.doCrawl();
  }


}

