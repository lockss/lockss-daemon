/*
 * $Id$
 */

/*

Copyright (c) 2007-2014 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.*;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.exploded.*;
import org.lockss.plugin.base.*;
import org.lockss.repository.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.extractor.*;

/**
 * Functional tests for the ZIP file exploder.  It
 * does not test the non-ZIP file functionality,
 * which is provided by FollowLinkCrawler.
 *
 * Uses ActualZipContentGenerator to create a
 * web site with a permission page that links to
 * a ZIP file containing the rest of the content
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class FuncZipExploder2 extends LockssTestCase {
  static Logger log = Logger.getLogger("FuncZipExploder2");

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  PluginManager pluginMgr;
  private CrawlManagerImpl crawlMgr;

  private static String URL_PREFIX =
    "http://springer.clockss.org/JOU=00109/VOL=83/ISU=12";
  static String[] url = {
    URL_PREFIX + "/ART=2005_719/109_2005_Article_719.xml.meta",
    URL_PREFIX + "/ART=2005_719/BodyRef/PDF/109_2005_Article_719.pdf",
    URL_PREFIX + "/ART=2005_721/109_2005_Article_721.xml.meta",
    URL_PREFIX + "/ART=2005_721/BodyRef/PDF/109_2005_Article_721.pdf",
    URL_PREFIX + "/ART=2005_724/109_2005_Article_724.xml.meta",
    URL_PREFIX + "/ART=2005_724/BodyRef/PDF/109_2005_Article_724.pdf",
  };

  // The DOIs in the sample
  private static final Set<String> doiSet = new HashSet<String>();
  static {
    doiSet.add("10.1007/s00109-005-0721-x");
    doiSet.add("10.1007/s00109-005-0724-7");
    doiSet.add("10.1007/s00109-005-0719-4");
  };

  static String[] url2 = {
    "http://www.example.com/index.html",
    "http://www.example.com/SpringerSample.zip",
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

  static final String GOOD_YEAR = "2005";

  private static final int DEFAULT_MAX_DEPTH = 1000;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static int maxDepth=DEFAULT_MAX_DEPTH;

  public static void main(String[] args) throws Exception {
    // XXX should be much simpler.
    FuncZipExploder2 test = new FuncZipExploder2();
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
    Properties props = new Properties();
    props.setProperty(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH, ""+max);
    maxDepth=max;
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);

    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.doZipFile", "true");
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.actualZipFile", "true");

    props.setProperty(FollowLinkCrawler.PARAM_STORE_ARCHIVES, "true");
    String explodedPluginName =
      "org.lockss.crawler.FuncZipExploder2MockExplodedPlugin";
    props.setProperty(Exploder.PARAM_EXPLODED_PLUGIN_NAME, explodedPluginName);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    pluginMgr = new NonVersionCheckingPluginManager();
    pluginMgr.initService(theDaemon);
    theDaemon.setPluginManager(pluginMgr);
    crawlMgr = new NoPauseCrawlManagerImpl();
    theDaemon.setCrawlManager(crawlMgr);
    crawlMgr.initService(theDaemon);
    theDaemon.getRepositoryManager().startService();
    theDaemon.suppressStartAuManagers(false);

    // pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginMgr.startService();
    pluginMgr.startLoadablePlugins();
    String explodedPluginKey = pluginMgr.pluginKeyFromName(explodedPluginName);
    pluginMgr.ensurePluginLoaded(explodedPluginKey);

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
					     simAuConfig(tempDirPath));
    sau.setUrlConsumerFactory(new ExplodingUrlConsumerFactory());
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
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_BIN);
    conf.put("binFileSize", ""+fileSize);
    return conf;
  }

  public void testRunSelf() throws Exception {
    log.debug3("About to create content");
    createContent();

    // get the root of the simContent
    String simDir = sau.getSimRoot();

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
    // change.  NB - because the ZIP files are compressed,  their
    // size varies randomly by a small amount.
    long expected = 285227;
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
      b.removeAll(sau.getPermissionUrls());
      // archives get checked twice - from checkThruFileTree & checkExplodedUrls
      b.remove("http://www.example.com/content.zip");
      // This test is screwed up by the use of shouldBeCached() in
      // ZipExploder() to find the AU to store the URL in.
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
	mep.setArticleIteratorFactory(new MyArticleIteratorFactory());
	mep.setArticleMetadataExtractorFactory(new MyArticleMetadataExtractorFactory());
	ArticleMetadataExtractor me =
	  plugin.getArticleMetadataExtractor(MetadataTarget.Any(), au);
	assertNotNull(me);
	ArticleMetadataListExtractor mle = new ArticleMetadataListExtractor(me);
	int count = 0;
	Set<String> foundDoiSet = new HashSet<String>();
	for (Iterator<ArticleFiles> it = au.getArticleIterator(); it.hasNext(); ) {
	  ArticleFiles af = it.next();
	  CachedUrl cu = af.getFullTextCu();
	  assertNotNull(cu);
	  String contentType = cu.getContentType();
	  assertNotNull(contentType);
	  assertTrue(contentType.toLowerCase().startsWith(articleMimeType));
	  log.debug("count " + count + " url " + cu.getUrl() + " " + contentType);
	  count++;
	  try {
	    List<ArticleMetadata> mdlist =
	      mle.extract(MetadataTarget.Any(), af);
	    assertNotEmpty(mdlist);
	    ArticleMetadata md = mdlist.get(0);
	    assertNotNull(md);
	    String doi = md.get(MetadataField.FIELD_DOI);
	    log.debug(cu.getUrl() + " doi " + doi);
	    assertTrue(MetadataUtil.isDoi(doi));
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
      ArchivalUnit explodedAU = cu.getArchivalUnit();
      log.debug2("Check: " + url[i] + " cu " + cu + " au " +
		 explodedAU.getAuId());
      assertTrue(cu + " has no content", cu.hasContent());
      assertTrue(cu + " isn't ExplodedArchivalUnit",
		 (explodedAU instanceof ExplodedArchivalUnit));
      assertNotEquals(sau, explodedAU);
      Configuration explodedConfig = explodedAU.getConfiguration();
      log.debug3(cu + " config " + explodedConfig);
      assertEquals(cu + " wrong year", GOOD_YEAR,
		   explodedConfig.get(ConfigParamDescr.YEAR.getKey()));
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
    Collection<String> urls = sau.getStartUrls();
    
    sau.setStartUrls(urls);
    sau.setRule(new MyCrawlRule());
    sau.setExploderPattern(".zip$");
    sau.setExploderHelper(new MyExploderHelper());
    FollowLinkCrawler crawler =
      new FollowLinkCrawler(sau, new MockAuState());
    crawler.setCrawlManager(crawlMgr);
    crawler.doCrawl();
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

  public static class MyArticleIteratorFactory
    implements ArticleIteratorFactory {

    /*
     * The Springer URL structure means that the metadata for an article
     * is at a URL like
     * http://springer.clockss.org/PUB=./JOU=./VOL=./ISU=./ART=./..xml.Meta
     * and the PDF at
     * http://springer.clockss.org/PUB=./JOU=./VOL=./ISU=./ART=./BodyRef/PDF/..pdf
     */
    //   protected Pattern pat = Pattern.compile(".*\\.xml\\.Meta$",
    // 					  Pattern.CASE_INSENSITIVE);
    Pattern pat = null;

    private static final String part1 = "/BodyRef/PDF";
    private static final String part2 = "\\.pdf";
    private static final String regex = ".*" + part1 + "/.*" + part2;


    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
							MetadataTarget target)
	throws PluginException {
      return new SubTreeArticleIterator(au,
					new SubTreeArticleIterator.Spec()
					.setTarget(target)) {
	protected ArticleFiles createArticleFiles(CachedUrl cu) {
	  ArticleFiles res = new ArticleFiles();
	  res.setFullTextCu(cu);
	  // cu points to a file whose name is ....pdf
	  // but the metadata we want is in a file whose name is ....xml.Meta
	  String pdfUrl = cu.getUrl();
	  if (pdfUrl.matches(regex)) {
	    String xmlUrl =
	      pdfUrl.replaceFirst(part1, "").replaceFirst(part2, ".xml.Meta");
	    CachedUrl xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	    if (xmlCu == null || !xmlCu.hasContent()) {
	      if (xmlCu == null) {
		log.debug2("xmlCu is null");
	      } else {
		log.debug2(xmlCu.getUrl() + " no content");
	      }
	      xmlUrl = 
		pdfUrl.replaceFirst(part1, "").replaceFirst(part2, ".xml.meta");
	      xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	    }
	    try {
	      if (xmlCu != null && xmlCu.hasContent()) {
		String mimeType =
		  HeaderUtil.getMimeTypeFromContentType(xmlCu.getContentType());
		if ("text/xml".equalsIgnoreCase(mimeType)) {
		  res.setRoleCu("xml", xmlCu);
		} else {
		  log.debug2("xml.meta wrong mime type: " + mimeType + ": "
			     + xmlCu.getUrl());
		}
	      } else {
		if (xmlCu == null) {
		  log.debug2("xmlCu is null");
		} else {
		  log.debug2(xmlCu.getUrl() + " no content");
		}
	      }
	    } finally {
	      AuUtil.safeRelease(xmlCu);
	    }
	  } else {
	    log.debug2(pdfUrl + " doesn't match " + regex);
	  }
	  if (log.isDebug3()) {
	    log.debug3("Iter: " + res);
	  }
	  return res;
	}
      };
    }
  }

  static class MyArticleMetadataExtractorFactory
    implements ArticleMetadataExtractorFactory {

    public ArticleMetadataExtractor
      createArticleMetadataExtractor(MetadataTarget target)
	throws PluginException {
      return new BaseArticleMetadataExtractor("xml");
    }

  }

  /**
   * Follows layout of Springer source files.  They come as a ZIP archive
   * containing additions to a directory tree whose layers are:
   *
   * 1. <code>PUB=${PUBLISHER}</code> As of 11/24/09 this layer removed
   *
   * 2. <code>JOU=${JOURNAL_ID}</code>
   *
   * 3. <code>VOL=${VOLUME_ID}</code>
   *
   * 4. <code>ISU=${ISSUE_ID}</code>
   *
   * 5. <code>ART=${YEAR}_${ARTICLE_ID}</code>
   * This directory contains files called
   * <code>${JOURNAL_ID}_${YEAR}_Article_${ARTICLE_ID}.xml</code> and
   * <code>${JOURNAL_ID}_${YEAR}_Article_${ARTICLE_ID}.xml.Meta</code>
   * and a directory tree whose layers are:
   *
   * 6. BodyRef
   *
   * 7. PDF
   * This directory contains 
   * <code>${JOURNAL_ID}_${YEAR}_Article_${ARTICLE_ID}.pdf</code>
   *
   * This class maps this into base URLs that look like:
   *
   * <code>http://springer.clockss.org/JOU=${JOURNAL_ID}/
   *
   * and the rest of the url inside the AU is the rest of the name of the entry.
   * It synthesizes suitable header fields for the files based on their
   * extensions.
   *
   * If the input ArchiveEntry contains a name matching this pattern the
   * baseUrl, restOfUrl, headerFields fields are set.  Otherwise,
   * they are left null.
   */
  public static class MyExploderHelper implements ExploderHelper {
    private static final String BASE_URL_STEM = "http://springer.clockss.org/";
    static final String[] tags = { "JOU=", "VOL=", "ISU=", "ART=" };
    private static final String PUB_FLAG = "PUB=";
    private static final String PUB_NAME = "Springer";
    private static final int JOU_INDEX = 0;
    private static final int VOL_INDEX = 1;
    private static final int ISU_INDEX = 2;
    private static final int ART_INDEX = 3;
    static final int endOfBase = 0;
    static final int minimumPathLength = 4;

    public void process(ArchiveEntry ae) {
      String baseUrl = BASE_URL_STEM;
      // Parse the name
      String entryName = ae.getName();
      // Remove PUB= prefix
      if (entryName.startsWith(PUB_FLAG)) {
	int firstSlash = entryName.indexOf("/");
	if (firstSlash > 0) {
	  entryName = entryName.substring(firstSlash+1);
	} else {
	  log.warning("Path " + entryName + " malformaeed");
	  return;
	}
      }
      String[] pathElements = entryName.split("/");
      if (pathElements.length < minimumPathLength) {
	log.warning("Path " + entryName + " too short");
	return;
      }
      for (int i = 0; i < pathElements.length; i++) {
	log.debug3("pathElements[" + i + "] = " + pathElements[i]);
      }
      for (int i = 0; i <= endOfBase; i++) {
	if (pathElements[i].startsWith(tags[i])) {
	  baseUrl += pathElements[i] + "/";
	} else {
	  log.warning("Element " + i + " of " + entryName +
			 " should be " + tags[i]);
	  return;
	}
      }
      String restOfUrl = "";
      for (int i = (endOfBase + 1); i < pathElements.length ; i++) {
	if (i <= ART_INDEX) {
	  if (!pathElements[i].startsWith(tags[i])) {
	    log.warning("Element " + i + " of " + entryName +
			   " should be " + tags[i]);
	    return;
	  }
	}
	restOfUrl += pathElements[i];
	if ((i + 1) < pathElements.length) {
	  restOfUrl += "/";
	}
      }
      CIProperties headerFields = Exploder.syntheticHeaders(baseUrl + restOfUrl,
							    ae.getSize());
      log.debug(entryName + " mapped to " +
		   baseUrl + " plus " + restOfUrl);
      log.debug3(baseUrl + restOfUrl + " props " + headerFields);
      ae.setBaseUrl(baseUrl);
      ae.setRestOfUrl(restOfUrl);
      ae.setHeaderFields(headerFields);
      if (restOfUrl.endsWith(".pdf")) {
	// XXX should have issue TOC
	// Now add a link for the URL to the volume TOC page at
	// baseUrl + /VOL=bletch/index.html
	Hashtable addText = new Hashtable();
	String volTOC = baseUrl + pathElements[VOL_INDEX] + "/index.html";
	String link = "<li><a href=\"" + baseUrl + restOfUrl + "\">" +
	  "art #" + pathElements[ART_INDEX].substring(4) + "</a></li>\n";
	log.debug3("volTOC = " + volTOC + " link " + link);
	ae.addTextTo(volTOC, link);
	// Now add a link to the volume TOC page to the journal TOC at
	// baseUrl + index.html
	String journalTOC = baseUrl + "index.html";
	link = "<li><a href=\"" + volTOC + "\">" +
	  "vol #" + pathElements[VOL_INDEX].substring(4) + "</a></li>\n";
	log.debug3("journalTOC = " + journalTOC + " link " + link);
	ae.addTextTo(journalTOC, link);
      } else if (restOfUrl.endsWith(".xml")) {
	// XXX it would be great to be able to get the DOI from the
	// XXX metadata files and put it in the text here
      }
      CIProperties props = new CIProperties();
      props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
      props.put(ConfigParamDescr.PUBLISHER_NAME.getKey(),
		PUB_NAME);
      props.put(ConfigParamDescr.JOURNAL_ISSN.getKey(),
		pathElements[JOU_INDEX].substring(4));
      props.put(ConfigParamDescr.YEAR.getKey(),
		pathElements[ART_INDEX].substring(4,8));
      ae.setAuProps(props);
    }

    @Override
    public void setWatchdog(LockssWatchdog wdog) {
      // Do nothing
      
    }

    @Override
    public void pokeWDog() {
      // Do nothing
      
    }
  }
}
