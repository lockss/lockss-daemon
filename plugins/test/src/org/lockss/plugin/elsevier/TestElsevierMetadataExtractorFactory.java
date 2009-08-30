/*
 * $Id: TestElsevierMetadataExtractorFactory.java,v 1.1.2.2 2009-08-30 00:14:44 dshr Exp $
 */

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

package org.lockss.plugin.elsevier;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.elsevier.*;
import org.lockss.extractor.*;

public class TestElsevierMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestElsevierMetadataExtractorFactory");

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  private CrawlManager crawlMgr;
  private static final int DEFAULT_MAX_DEPTH = 1000;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static int maxDepth=DEFAULT_MAX_DEPTH;

  public static void main(String[] args) throws Exception {
    TestElsevierMetadataExtractorFactory test = new TestElsevierMetadataExtractorFactory();
    if (args.length>0) {
      try {
        maxDepth = Integer.parseInt(args[0]);
      } catch (NumberFormatException ex) { }
    }

    test.setUp(maxDepth);
    test.testDOI();
    test.tearDown();
  }

  public void setUp() throws Exception {
    super.setUp();
    this.setUp(DEFAULT_MAX_DEPTH);
  }

  public void setUp(int max) throws Exception {

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String auId = "org|lockss|plugin|elsevier|TestElsevierMetadataExtractorFactory$MySimulatedPlugin.root~" +
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
                      SimulatedPlugin.AU_PARAM_BRANCH, "3");
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_NUM_FILES, "7");
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_FILE_TYPES, "" +
                      (SimulatedContentGenerator.FILE_TYPE_PDF +
		       SimulatedContentGenerator.FILE_TYPE_XML));
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE, ""+fileSize);
    props.setProperty("org.lockss.au." + auId + "." +
                      SimulatedPlugin.AU_PARAM_DEFAULT_ARTICLE_MIME_TYPE,
		      "application/pdf");

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    crawlMgr = theDaemon.getCrawlManager();

    ConfigurationUtil.setCurrentConfigFromProps(props);

    sau =
        (SimulatedArchivalUnit)theDaemon.getPluginManager().getAllAus().get(0);
    theDaemon.getLockssRepository(sau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), sau);
    ArticleIteratorFactory aif = new ElsevierArticleIteratorFactory();
    sau.setArticleIteratorFactory(aif);
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testDOI() throws Exception {
    createContent();

    // get the root of the simContent
    String simDir = sau.getSimRoot();

    crawlContent();

    Plugin plugin = sau.getPlugin();
    String articleMimeType = "application/pdf";
    MetadataExtractor me = plugin.getMetadataExtractor(articleMimeType, sau);
    assertNotNull(me);
    assert(me instanceof ElsevierMetadataExtractorFactory.ElsevierMetadataExtractor);
    int count = 0;
    for (Iterator it = sau.getArticleIterator(); it.hasNext(); ) {
	BaseCachedUrl cu = (BaseCachedUrl)it.next();
	assertNotNull(cu);
	assert(cu instanceof CachedUrl);
	String contentType = cu.getContentType();
	assertNotNull(contentType);
	assert(contentType.toLowerCase().startsWith(articleMimeType));
	log.debug("count " + count + " url " + cu.getUrl() + " " + contentType);
	count++;
	Metadata md = me.extract(cu);
	assertNotNull(md);
	String doi = md.getDOI();
	assertNotNull(doi);
	log.debug(cu.getUrl() + " doi " + doi);
	String doi2 = md.getProperty(Metadata.KEY_DOI);
	assert(doi2.startsWith(Metadata.PROTOCOL_DOI));
	assertEquals(doi, doi2.substring(Metadata.PROTOCOL_DOI.length()));
    }
    log.debug("Article count is " + count);
    assertEquals(28, count);
  }

  private void createContent() {
    log.debug("Generating tree of size 3x1x2 with "+fileSize
	      +"byte files...");
    sau.generateContentTree();
  }

  private void crawlContent() {
    log.debug("Crawling tree...");
    CrawlSpec spec = new SpiderCrawlSpec(sau.getNewContentCrawlUrls(), null);
    NewContentCrawler crawler =
      new NewContentCrawler(sau, spec, new MockAuState());
    //crawler.setCrawlManager(crawlMgr);
    crawler.doCrawl();
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    /**
     * Returns the article iterator factory for the mime type, if any
     * @param contentType the content type
     * @return the ArticleIteratorFactory
     */
    public ArticleIteratorFactory getArticleIteratorFactory(String contentType) {
      MyElsevierArticleIteratorFactory ret =
	  new MyElsevierArticleIteratorFactory();
      ret.setSubTreeRoot("branch1/branch1");
      return ret;
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

  public static class MyElsevierArticleIteratorFactory
      extends ElsevierArticleIteratorFactory {
    MyElsevierArticleIteratorFactory() {
    }
    public void setSubTreeRoot(String root) {
      subTreeRoot = root;
      pat = Pattern.compile("branch[0-9]*/", Pattern.CASE_INSENSITIVE);
      log.debug("Set subTreeRoot: " + subTreeRoot);
    }
  }
}
