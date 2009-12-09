/*
 * $Id: FuncTarExploder2.java,v 1.12 2009-12-09 00:08:19 tlipkis Exp $
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

package org.lockss.crawler;

import java.io.*;
import java.util.*;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.exploded.*;
import org.lockss.plugin.elsevier.*;
import org.lockss.plugin.base.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.extractor.*;

/**
 * Functional tests for the TAR file exploder.  It
 * does not test the non-TAR file functionality,
 * which is provided by FollowLinkCrawler.
 *
 * Uses ActualTarContentGenerator to create a
 * web site with a permission page that links to
 * a TAR file containing the rest of the content
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class FuncTarExploder2 extends LockssTestCase {
  static Logger log = Logger.getLogger("FuncTarExploder2");

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  PluginManager pluginMgr;

  private static final int DEFAULT_MAX_DEPTH = 1000;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static int maxDepth=DEFAULT_MAX_DEPTH;

  // Three articles in issue 2007004 of ISSN 1356689X
  private static String issn = "1356689X";
  private static String URL_PREFIX =
    "http://elsevier.clockss.org/" + issn + "/20070004";
  static String[] url = {
    URL_PREFIX + "/07700618/main.raw",
    URL_PREFIX + "/07700618/main.pdf",
    URL_PREFIX + "/07700618/main.xml",
    URL_PREFIX + "/07700618/checkmd5.fil",
    URL_PREFIX + "/07700606/main.pdf",
    URL_PREFIX + "/07700606/main.xml",
    URL_PREFIX + "/07700606/main.raw",
    URL_PREFIX + "/07700606/checkmd5.fil",
    URL_PREFIX + "/0770062X/main.raw",
    URL_PREFIX + "/0770062X/main.pdf",
    URL_PREFIX + "/0770062X/main.xml",
    URL_PREFIX + "/0770062X/checkmd5.fil",
  };

  // The DOIs in the sample
  private static final Set<String> doiSet = new HashSet<String>();
  static {
    doiSet.add("10.1016/S1365-6937(07)70060-6");
    doiSet.add("10.1016/S1365-6937(07)70061-8");
    doiSet.add("10.1016/S1365-6937(07)70062-X");
  };  

  static String[] url2 = {
    "http://www.example.com/index.html",
    "http://www.example.com/" + issn + ".tar",
    "http://www.example.com/001file.bin",
    "http://www.example.com/002file.bin",
    "http://www.example.com/branch1/001file.bin",
    "http://www.example.com/branch1/002file.bin",
    "http://www.example.com/branch1/branch1/001file.bin",
    "http://www.example.com/branch1/branch1/002file.bin",
    "http://www.example.com/branch1/branch1/branch1/001file.bin",
    "http://www.example.com/branch1/branch1/branch1/002file.bin",
    "http://www.example.com/branch1/branch1/branch1/index.html",
    "http://www.example.com/branch1/branch1/index.html",
    "http://www.example.com/branch1/index.html",
  };

  public static void main(String[] args) throws Exception {
    // XXX should be much simpler.
    FuncTarExploder2 test = new FuncTarExploder2();
    if (args.length>0) {
      try {
        maxDepth = Integer.parseInt(args[0]);
      } catch (NumberFormatException ex) { }
    }

    log.info("Setting up for depth " + maxDepth);
    test.setUp(maxDepth);
    log.info("Running up for depth " + maxDepth);
    test.testRunSelf();
    test.tearDown();
  }

  public void setUp() throws Exception {
    super.setUp();
    this.setUp(DEFAULT_MAX_DEPTH);
  }

  public void setUp(int max) throws Exception {

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String auId = "org|lockss|crawler|FuncTarExploder2$MySimulatedPlugin.root~" +
      PropKeyEncoder.encode(tempDirPath);
    Properties props = new Properties();
    props.setProperty(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH, ""+max);
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
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_DEFAULT_ARTICLE_MIME_TYPE,
		      "application/pdf");
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.doTarFile", "true");
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.actualTarFile", "true");
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.actualTarFileName", issn + ".tar");

    props.setProperty(FollowLinkCrawler.PARAM_EXPLODE_ARCHIVES, "true");
    props.setProperty(FollowLinkCrawler.PARAM_STORE_ARCHIVES, "true");
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(HistoryRepositoryImpl.PARAM_HISTORY_LOCATION, tempDirPath);
    String explodedPluginName =
      "org.lockss.plugin.elsevier.ClockssElsevierExplodedPlugin";
    props.setProperty(Exploder.PARAM_EXPLODED_PLUGIN_NAME, explodedPluginName);
    props.setProperty(Exploder.PARAM_EXPLODED_AU_YEAR, "1997");

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    pluginMgr = new NonVersionCheckingPluginManager();
    pluginMgr.initService(theDaemon);
    theDaemon.setPluginManager(pluginMgr);

    // pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginMgr.startService();
    pluginMgr.startLoadablePlugins();
    String explodedPluginKey = pluginMgr.pluginKeyFromName(explodedPluginName);
    pluginMgr.ensurePluginLoaded(explodedPluginKey);

    ConfigurationUtil.setCurrentConfigFromProps(props);

    sau =
      (SimulatedArchivalUnit)theDaemon.getPluginManager().getAllAus().get(0);
    theDaemon.getLockssRepository(sau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), sau);
    ArticleIteratorFactory aif = new ElsevierArticleIteratorFactory();
    sau.setArticleIteratorFactory(aif);
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testRunSelf() throws Exception {
    log.debug3("About to create content");
    createContent();

    // get the root of the simContent
    String simDir = sau.getSimRoot();
    assertTrue("No simulated content", simDir != null);

    log.debug3("About to crawl content");
    crawlContent();

    // read all the files links from the root of the simcontent
    // check the link level of the file and see if it contains
    // in myCUS (check if the crawler crawl within the max. depth)
    CachedUrlSet myCUS = sau.getAuCachedUrlSet();
    File dir = new File(simDir);
    if(dir.isDirectory()) {
      File f[] = dir.listFiles();
      log.debug("Checking simulated content.");
      checkThruFileTree(f, myCUS);
      log.debug("Checking simulated content done.");
      checkUnExplodedUrls();
      checkExplodedUrls();

      log.debug("Check finished.");
    } else {
      log.error("Error: The root path of the simulated" +
		" content ["+ dir +"] is not a directory");
    }

    // Test PluginManager.getAuContentSize(), just because this is a
    // convenient place to do it.  If the simulated AU params are changed, or
    // SimulatedContentGenerator is changed, this number may have to
    // change.  NB - because the TAR files are compressed,  their
    // size varies randomly by a small amount.
    long expected = 261173;
    long actual = AuUtil.getAuContentSize(sau, true);
    long error = expected - actual;
    long absError = (error < 0 ? -error : error);
    assertTrue("size mismatch " + expected + " vs. " + actual, absError < 60);

    if (false) {
      List sbc = ((MySimulatedArchivalUnit)sau).sbc;
      Bag b = new HashBag(sbc);
      Set uniq = new HashSet(b.uniqueSet());
      for (Iterator iter = uniq.iterator(); iter.hasNext(); ) {
	b.remove(iter.next(), 1);
      }
      // Permission pages get checked twice.  Hard to avoid that, so allow it
      b.removeAll(sau.getCrawlSpec().getPermissionPages());
      // archives get checked twice - from checkThruFileTree & checkExplodedUrls
      b.remove("http://www.example.com/issn.tar");
      // This test is screwed up by the use of shouldBeCached() in
      // TarExploder() to find the AU to store the URL in.
      //assertEmpty("shouldBeCached() called multiple times on same URLs.", b);
    }
    // Now check the DOIs
    checkDOIs();
  }

  private void checkDOIs() {
    List<ArchivalUnit> auList =
      theDaemon.getPluginManager().getAllAus();
    for (int i = 0; i < auList.size(); i++) {
      ArchivalUnit au = auList.get(i);
      assertNotNull(au);
      log.debug("AU " + i + " : " + au);
      Plugin plugin = au.getPlugin();
      assertNotNull(plugin);
      log.debug("Exploded Plugin: " + plugin);
      if (plugin instanceof MockExplodedPlugin) {
	MockExplodedPlugin mep = (MockExplodedPlugin)plugin;
	String articleMimeType = "application/pdf";
	mep.setDefaultArticleMimeType(articleMimeType);
	mep.setArticleIteratorFactory(new ElsevierArticleIteratorFactory());
	mep.setMetadataExtractorFactory(new ElsevierMetadataExtractorFactory());
	MetadataExtractor me = plugin.getMetadataExtractor(articleMimeType, au);
	assertNotNull(me);
	assert(me instanceof
	       ElsevierMetadataExtractorFactory.ElsevierMetadataExtractor);
	int count = 0;
	Set foundDoiSet = new HashSet();
	for (Iterator it = au.getArticleIterator(); it.hasNext(); ) {
	  BaseCachedUrl cu = (BaseCachedUrl)it.next();
	  assertNotNull(cu);
	  assert(cu instanceof CachedUrl);
	  String contentType = cu.getContentType();
	  assertNotNull(contentType);
	  assert(contentType.toLowerCase().startsWith(articleMimeType));
	  log.debug("count " + count + " url " + cu.getUrl() + " " + contentType);
	  count++;
	  try {
	    Metadata md = me.extract(cu);
	    assertNotNull(md);
	    String doi = md.getDOI();
	    assertNotNull(doi);
	    log.debug(cu.getUrl() + " doi " + doi);
	    String doi2 = md.getProperty(Metadata.KEY_DOI);
	    assert(doi2.startsWith(Metadata.PROTOCOL_DOI));
	    assertEquals(doi, doi2.substring(Metadata.PROTOCOL_DOI.length()));
	    foundDoiSet.add(doi);
	  } catch (Exception ex) {
	    fail(ex.toString());
	  }
	}
	log.debug("Article count is " + count);
	assertEquals(doiSet.size(), count);
	assertEquals(doiSet, foundDoiSet);
      }
    }
  }
  //recursive caller to check through the whole file tree
  private void checkThruFileTree(File f[], CachedUrlSet myCUS){
    for (int ix=0; ix<f.length; ix++) {
      log.debug3("Check: " + f[ix].getAbsolutePath());
      if (f[ix].isDirectory()) {
	// get all the files and links there and iterate
	checkThruFileTree(f[ix].listFiles(), myCUS);
      } else {

	// get the f[ix] 's level information
	String fileUrl = sau.mapContentFileNameToUrl(f[ix].getAbsolutePath());
	int fileLevel = sau.getLinkDepth(fileUrl);
	log.debug2("File: " + fileUrl + " in Level " + fileLevel);

	CachedUrl cu = theDaemon.getPluginManager().findCachedUrl(fileUrl);
	if (fileLevel <= maxDepth) {
	  assertNotNull("Can't find CU for " + fileUrl, cu);
	  assertTrue(cu + " has no content", cu.hasContent());
	} else {
	  assertFalse(cu + " has content when it shouldn't",
		      cu.hasContent());
	}
      }
    }
    return; // when all "File" in the array are checked
  }

  private void checkExplodedUrls() {
    log.debug2("Checking Exploded URLs.");
    for (int i = 0; i < url.length; i++) {
      CachedUrl cu = theDaemon.getPluginManager().findCachedUrl(url[i]);
      assertTrue(url[i] + " not in any AU", cu != null);
      log.debug2("Check: " + url[i] + " cu " + cu + " au " + cu.getArchivalUnit().getAuId());
      assertTrue(cu + " has no content", cu.hasContent());
      assertTrue(cu + " isn't ExplodedArchivalUnit",
		 !(cu instanceof ExplodedArchivalUnit));
      assertNotEquals(sau, cu.getArchivalUnit());
    }
    log.debug2("Checking Exploded URLs done.");
  }

  private void checkUnExplodedUrls() {
    log.debug2("Checking UnExploded URLs.");
    for (int i = 0; i < url2.length; i++) {
      CachedUrl cu = theDaemon.getPluginManager().findCachedUrl(url2[i]);
      assertTrue(url2[i] + " not in any AU", cu != null);
      log.debug2("Check: " + url2[i] + " cu " + cu + " au " + cu.getArchivalUnit().getAuId());
      assertTrue(cu + " has no content", cu.hasContent());
      assertTrue(cu + " isn't MySimulatedArchivalUnit",
		 !(cu instanceof MySimulatedArchivalUnit));
      assertEquals(sau, cu.getArchivalUnit());
    }
    log.debug2("Checking UnExploded URLs done.");
  }


  private void createContent() {
    log.debug("Generating tree of size 3x1x2 with "+fileSize
	      +"byte files...");
    sau.generateContentTree();
  }

  private void crawlContent() {
    log.debug("Crawling tree...");
    List urls = sau.getNewContentCrawlUrls();
    CrawlSpec spec =
      new SpiderCrawlSpec(urls,
			  urls, // permissionUrls
			  new MyCrawlRule(), // crawl rules
			  1,    // refetch depth
			  null, // PermissionChecker
			  null, // LoginPageChecker
			  ".tar$", // exploder pattern
			  new ElsevierExploderHelper() );
    Crawler crawler = new NewContentCrawler(sau, spec, new MockAuState());
    crawler.doCrawl();
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
	throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new MySimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }
    public MetadataExtractor getMetadataExtractor(String contentType,
						    ArchivalUnit au) {
      MetadataExtractorFactory mef =
        new ElsevierMetadataExtractorFactory();
      MetadataExtractor ret = null;
      try {
        ret = mef.createMetadataExtractor(contentType);
      } catch (PluginException ex) {
        // Do nothing
      }
      return ret;
    }
  }

  public static class MySimulatedArchivalUnit extends SimulatedArchivalUnit {
    List sbc = new ArrayList();

    public MySimulatedArchivalUnit(Plugin owner) {
      super(owner);
    }

    protected CrawlRule makeRules() {
      return new MyCrawlRule();
    }

    public boolean shouldBeCached(String url) {
      if (false) {
	// This can be helpful to track down problems - h/t TAL.
	log.debug3("shouldBeCached: " + url, new Throwable());
      } else {
	log.debug3("shouldBeCached: " + url);
      }
      for (int i = 0; i < url2.length; i++) {
	if (url2[i].equals(url)) {
	  sbc.add(url);
	  return super.shouldBeCached(url);
	}
      }
      return (false);
    }
  }

  public static class MyCrawlRule implements CrawlRule {
    public int match(String url) {
      if (url.startsWith("http://www.example.com")) {
	return CrawlRule.INCLUDE;
      }
      return CrawlRule.EXCLUDE;
    }
  }

}
