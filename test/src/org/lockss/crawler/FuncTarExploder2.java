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

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.*;
import org.apache.commons.collections.map.*;
import org.lockss.config.*;
import org.lockss.crawler.FuncWarcExploder.MyCrawlRule;
import org.lockss.crawler.FuncWarcExploder.MyExploderHelper;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.exploded.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.ExploderHelper;
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
  private CrawlManagerImpl crawlMgr;

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
    Properties props = new Properties();
    props.setProperty(FollowLinkCrawler.PARAM_MAX_CRAWL_DEPTH, ""+max);
    maxDepth=max;
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);

    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.doTarFile", "true");
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.actualTarFile", "true");
    props.setProperty("org.lockss.plugin.simulated.SimulatedContentGenerator.actualTarFileName", issn + ".tar");

    props.setProperty(FollowLinkCrawler.PARAM_STORE_ARCHIVES, "true");
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    String explodedPluginName =
      "org.lockss.crawler.FuncTarExploder2MockExplodedPlugin";
    props.setProperty(Exploder.PARAM_EXPLODED_PLUGIN_NAME, explodedPluginName);
    props.setProperty(Exploder.PARAM_EXPLODED_AU_YEAR, "1997");
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

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    ArticleIteratorFactory aif = new MyArticleIteratorFactory();
    sau.setArticleIteratorFactory(aif);
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
    conf.put(SimulatedPlugin.AU_PARAM_DEFAULT_ARTICLE_MIME_TYPE,
	     "application/pdf");
    return conf;
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
      b.removeAll(sau.getPermissionUrls());
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
	mep.setArticleIteratorFactory(new MyArticleIteratorFactory());
	mep.setArticleMetadataExtractorFactory(new MyArticleMetadataExtractorFactory());
	mep.setFileMetadataExtractorFactory(new MyXmlMetadataExtractorFactory());
	ArticleMetadataExtractor me =
	  plugin.getArticleMetadataExtractor(MetadataTarget.Any(), au);
	assertNotNull(me);
	assertTrue(""+me.getClass(),
		   me instanceof MyXmlMetadataExtractorFactory.MyXmlMetadataExtractor);
	ArticleMetadataListExtractor mle = new ArticleMetadataListExtractor(me);
	int count = 0;
	Set<String> foundDoiSet = new HashSet<String>();
	for (Iterator<ArticleFiles> it = au.getArticleIterator();
	     it.hasNext(); ) {
	  ArticleFiles af = it.next();
	  CachedUrl cu = it.next().getFullTextCu();
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
    sau.setRule(new MyCrawlRule());
    sau.setExploderPattern(".tar$");
    sau.setExploderHelper(new MyExploderHelper());
    FollowLinkCrawler crawler = 
        new FollowLinkCrawler(sau, new MockAuState());
    crawler.setCrawlManager(crawlMgr);
    crawler.doCrawl();
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

  static class MyArticleIteratorFactory implements ArticleIteratorFactory {
    /*
     * The Elsevier exploded URL structure means that the metadata for an
     * article is at a URL like
     * http://elsevier.clockss.org/<issn>/<issue>/<article>/main.xml The
     * DOI is between <ce:doi> and </ce:doi>.
     */
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
							MetadataTarget target)
	throws PluginException {
      return new SubTreeArticleIterator(au,
					new SubTreeArticleIterator.Spec()
					.setTarget(target)) {
	protected ArticleFiles createArticleFiles(CachedUrl cu) {
	  ArticleFiles res = new ArticleFiles();
	  res.setFullTextCu(cu);
	  // cu points to a file whose name is .../main.pdf
	  // but the DOI we want is in a file whose name is .../main.xml
	  // The rest of the metadata is in the dataset.toc file that
	  // describes the package in which the article was delivered.
	  String pdfUrl = cu.getUrl();
	  if (StringUtil.endsWithIgnoreCase(pdfUrl, ".pdf")) {
	    String xmlUrl = pdfUrl.substring(0, pdfUrl.length()-4) + ".xml";
	    CachedUrl xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	    try {
	      if (xmlCu != null && xmlCu.hasContent()) {
		res.setRoleCu("xml", xmlCu);
	      }
	    } finally {
	      AuUtil.safeRelease(xmlCu);
	    }
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
   * Documentation of the Elsevier format is at:
   * http://info.sciencedirect.com/techsupport/sdos/effect41.pdf
   * http://info.sciencedirect.com/techsupport/sdos/sdos30.pdf
   *
   * This ExploderHelper encapsulates knowledge about the way
   * Elsevier delivers source files.  They come as TAR
   * archives containing additions to a directory tree whose
   * layers are:
   *
   * 1. <code>${JOURNAL_ID}</code> JOURNAL_ID is the ISSN (or an
   *    ISSN-like string) without the dash. The tar file is named
   *    ${JOURNAL_ID}.tar
   *
   * 2. <code>${ISSUE_ID}</code> ISSUE_ID is string unique name for the
   *    issue within the journal.
   *
   * 3. <code>${ARTICLE_ID}</code> ARTICLE_ID is a similar string naming
   *    the article.
   * This directory contains files called
   * - *.pdf PDF
   * - *.raw ASCII
   * - *.sgm SGML
   * - *.gif figures etc
   * - *.jpg images etc.
   * - *.xml
   * - stripin.toc see Appendix 2
   * - checkmd5.fil md5 sums for files
   *
   * This class maps this into base URLs that look like:
   *
   * <code>http://elsevier.clockss.org/JOU=${JOURNAL_ID}/
   *
   * and the rest of the url inside the AU is the rest of the name of the entry.
   * It synthesizes suitable header fields for the files based on their
   * extensions.
   *
   * If the input ArchiveEntry contains a name matching this pattern the
   * baseUrl, restOfUrl, headerFields fields are set.  Otherwise,
   * they are left null.
   */
  static class MyExploderHelper implements ExploderHelper {
    private static final int ISS_INDEX = 0;
    private static final int ART_INDEX = 1;
    private static final String BASE_URL = "http://elsevier.clockss.org/";
    static final int endOfBase = 0;
    static final int minimumPathLength = 3;
    private static final String[] ignoreMe = {
      "checkmd5.fil",
    };
    private static final String extension = ".tar";

    public MyExploderHelper() {
    }

    public void process(ArchiveEntry ae) {
      String issn = archiveNameToISSN(ae);
      if (issn == null) {
	ae.setRestOfUrl(null);
	return;
      }
      // The base URL contains the ISSN from the archive name
      String baseUrlStem = BASE_URL;
      String baseUrl = baseUrlStem + issn + "/";
      // Parse the name
      String fullName = ae.getName();
      String[] pathElements = fullName.split("/");
      if (pathElements.length < minimumPathLength) {
	for (int i = 0; i < ignoreMe.length; i++) {
	  if (fullName.toLowerCase().endsWith(ignoreMe[i])) {
	    ae.setBaseUrl(baseUrl);
	    ae.setRestOfUrl(null);
	    log.debug("Path " + fullName + " ignored");
	    return;
	  }
	}
	log.warning("Path " + fullName + " too short");
	return;
      }
      for (int i = 0; i < endOfBase; i++) {
	baseUrl += pathElements[i] + "/";
      }
      String restOfUrl = "";
      for (int i = endOfBase; i < pathElements.length ; i++) {
	restOfUrl += pathElements[i];
	if ((i + 1) < pathElements.length) {
	  restOfUrl += "/";
	}
      }
      CIProperties headerFields =
	Exploder.syntheticHeaders(baseUrl + restOfUrl, ae.getSize());
      log.debug(ae.getName() + " mapped to " +
		   baseUrl + " plus " + restOfUrl);
      log.debug2(baseUrl + restOfUrl + " props " + headerFields);
      ae.setBaseUrl(baseUrl);
      ae.setRestOfUrl(restOfUrl);
      ae.setHeaderFields(headerFields);
      if (restOfUrl.endsWith(".pdf")) {
	// Add a link to the article to the journal TOC page at
	// ${JOURNAL_ID}/index.html
	Hashtable addText = new Hashtable();
	String journalTOC = baseUrl + "index.html";
	String link = "<li><a href=\"" + baseUrl + restOfUrl + "\">" +
	  "issue #" + pathElements[ISS_INDEX] +
	  " art #" + pathElements[ART_INDEX] + "</a></li>\n";
	log.debug3("journalTOC " + journalTOC + " link " + link);
	ae.addTextTo(journalTOC, link);
      } else if (restOfUrl.endsWith(".xml")) {
	// XXX it would be great to be able to get the DOI from the
	// XXX metadata files and put it in the text here
      }
      CIProperties props = new CIProperties();
      props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
      props.put(ConfigParamDescr.PUBLISHER_NAME.getKey(),
		"Elsevier");
      props.put(ConfigParamDescr.JOURNAL_ISSN.getKey(),
		issn);
      ae.setAuProps(props);
    }

    private boolean checkISSN(String s) {
      char[] c = s.toCharArray();
      // The ISSN is 7 digits followed by either a digit or X
      // The last digit is a check digit as described here:
      // http://en.wikipedia.org/wiki/ISSN
      if (c.length != 8) {
	return false;
      }
      int checksum = 0;
      for (int i = 0; i < c.length-1; i++) {
	if (!Character.isDigit(c[i])) {
	  return false;
	}
	try {
	  int j = Integer.parseInt(Character.toString(c[i]));
	  checksum += j * (c.length - i);
	} catch (NumberFormatException ex) {
	  return false;
	}
      }
      if (Character.isDigit(c[c.length-1])) {
	try {
	  int j = Integer.parseInt(Character.toString(c[c.length-1]));
	  checksum += j;
	} catch (NumberFormatException ex) {
	  return false;
	}
      } else if (c[c.length-1] == 'X') {
	checksum += 10;
      } else {
	return false;
      }
      if ((checksum % 11) != 0) {
	return false;
      }
      return true;
    }

    private String archiveNameToISSN(ArchiveEntry ae) {
      String ret = null;
      String an = ae.getArchiveName();
      if (an != null) {
	// The ISSN is the part of an from the last / to the .tar
	int slash = an.lastIndexOf("/");
	int dot = an.lastIndexOf(extension);
	if (slash > 0 && dot > slash) {
	  String maybe = an.substring(slash + 1, dot);
	  if (checkISSN(maybe)) {
	    ret = maybe;
	    log.debug3("ISSN: " + ret);
	  } else {
	    log.warning("Bad ISSN in archive name " + an);
	  }
	} else {
	  log.warning("Malformed archive name " + an);
	}
      } else {
	log.error("Null archive name");
      }
      return ret;
    }

    @Override
    public void setWatchdog(LockssWatchdog wdog) {
      //do nothing
      
    }

    @Override
    public void pokeWDog() {
      //do nothing
      
    }
  }

  static class MyXmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {

    public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							     String contentType)
	throws PluginException {
      return new MyXmlMetadataExtractor();
    }

    public static class MyXmlMetadataExtractor
      extends SimpleFileMetadataExtractor {

      private static MultiMap tagMap = new MultiValueMap();
      static {
	// Elsevier doesn't prefix the DOI in dc.Identifier with doi:
	tagMap.put("ce:doi", MetadataField.DC_FIELD_IDENTIFIER);
	tagMap.put("ce:doi", MetadataField.FIELD_DOI);
      };

      public MyXmlMetadataExtractor() {
      }

      public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
	  throws IOException, PluginException {
	SimpleFileMetadataExtractor extr = new SimpleXmlMetadataExtractor(tagMap);
	ArticleMetadata am = extr.extract(target, cu);
	// extract metadata from BePress specific metadata tags
	am.cook(tagMap);
	return am;
      }
    }
  }
}
