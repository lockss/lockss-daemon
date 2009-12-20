/*
 * $Id: TestBioOneMetadataExtractor.java,v 1.1 2009-12-20 00:05:00 dshr Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.bioone;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.simulated.*;

public class TestBioOneMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestBioOneMetadataExtractor");

  private SimulatedArchivalUnit sau;
  private MockLockssDaemon theDaemon;
  private CrawlManager crawlMgr;
  private static int exceptionCount;
  private static final int DEFAULT_MAX_DEPTH = 1000;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static int maxDepth = DEFAULT_MAX_DEPTH;
  private static int urlCount = 7;
  private static int testExceptions = 3;
  private static final String issnTemplate = "%1%2%3%1-%3%1%2%3";
  private static final String volumeTemplate = "%1%3";
  private static final String issueTemplate = "%2";
  private static final String fPageTemplate = "%2%3";
  private static final String lPageTemplate = "%3%1%2";  

  public static void main(String[] args) throws Exception {
    TestBioOneMetadataExtractor test = new TestBioOneMetadataExtractor();
    if (args.length > 0) {
      try {
        maxDepth = Integer.parseInt(args[0]);
      } catch (NumberFormatException ex) {
      }
    }

    test.setUp(maxDepth);
    test.testExtraction();
    test.tearDown();
  }

  public void setUp() throws Exception {
    super.setUp();
    this.setUp(DEFAULT_MAX_DEPTH);
  }

  public void setUp(int max) throws Exception {

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String auId = "org|lockss|plugin|bioone|TestBioOneMetadataExtractor$MySimulatedPlugin.root~" + PropKeyEncoder.encode(tempDirPath);
    Properties props = new Properties();
    props.setProperty(NewContentCrawler.PARAM_MAX_CRAWL_DEPTH, "" + max);
    maxDepth = max;
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);

    props.setProperty("org.lockss.au." + auId + "." + SimulatedPlugin.AU_PARAM_ROOT, tempDirPath);
    // the simulated Content's depth will be (AU_PARAM_DEPTH + 1)
    props.setProperty("org.lockss.au." + auId + "." + SimulatedPlugin.AU_PARAM_DEPTH, "3");
    props.setProperty("org.lockss.au." + auId + "." + SimulatedPlugin.AU_PARAM_BRANCH, "3");
    props.setProperty("org.lockss.au." + auId + "." + SimulatedPlugin.AU_PARAM_NUM_FILES, "7");
    props.setProperty("org.lockss.au." + auId + "." + SimulatedPlugin.AU_PARAM_FILE_TYPES, "" + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    props.setProperty("org.lockss.au." + auId + "." + SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE, "" + fileSize);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    crawlMgr = theDaemon.getCrawlManager();

    ConfigurationUtil.setCurrentConfigFromProps(props);

    sau = (SimulatedArchivalUnit) theDaemon.getPluginManager().getAllAus().get(0);
    theDaemon.getLockssRepository(sau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), sau);
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testExtraction() throws Exception {
    createContent();

    // get the root of the simContent
    String simDir = sau.getSimRoot();

    crawlContent();

    exceptionCount = 0;
    int count = 0;
    for (Iterator it = sau.getArticleIterator(); it.hasNext();) {
      BaseCachedUrl cu = (BaseCachedUrl) it.next();
      assertNotNull(cu);
      assertTrue(cu instanceof CachedUrl);
      log.debug3("count " + count + " url " + cu.getUrl());
      MetadataExtractor me = cu.getMetadataExtractor();
      log.debug3("Extractor: " + me.toString());
      assertTrue(me instanceof BioOneMetadataExtractorFactory.BioOneMetadataExtractor);
      Metadata md = me.extract(cu);
      assertNotNull(md);
      checkMetadata(md);
      count++;
    }
    log.debug("Article count is " + count);
    assertEquals(urlCount, count);
  }

  String goodDOI = "10.1640/0002-8444-99.2.61";
  String goodVolume = "13";
  String goodIssue = "4";
  String goodStartPage = "123";
  String goodEndPage = "134";
  String goodOnlineISSN = "1234-5679";
  String goodPrintISSN = "2345-567X";
  String goodDate = "4/1/2000";
  String goodAuthor = "Fred Bloggs";
  String goodTitle = "Spurious Results";
  String goodAbsUrl = "http://www.example.org/doi/abs/10.1640/0002-8444-99.2.61";
  String goodHtmUrl = "http://www.example.org/doi/full/10.1640/0002-8444-99.2.61";
  String goodPdfUrl = "http://www.example.org/doi/pdf/10.1640/0002-8444-99.2.61";

  String goodContent =
          "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" +
                  "<p><strong>Print ISSN: </strong>" + goodPrintISSN + " </p>\n" +
                  "<p><strong>Online ISSN: </strong>" + goodOnlineISSN + "</p>\n" +
                  "<p><strong>Current: </strong>Apr 2009 : Volume " + goodVolume + " Issue " + goodIssue + "</p>\n" +
                  "<span class=\"title\">\n" +
                  "        \n" +
                  "                pg(s) " + goodStartPage + "-" + goodEndPage + "\n" +
                  "            \n" +
                  "            </span>\n" +
                  "    \n" +
                  "</BODY></HTML>";

  public void testExtractFromGoodContent() throws Exception {
    Metadata md = extractFromTestContent(goodContent);
    assertTrue(MetadataUtil.isDOI(md.getDOI()));
    assertEquals(goodDOI, md.getDOI());
    assertEquals(goodVolume, md.getVolume());
    assertEquals(goodIssue, md.getIssue());
    assertEquals(goodStartPage, md.getStartPage());
    assertTrue(MetadataUtil.isISSN(md.getISSN()));
    assertEquals(goodOnlineISSN, md.getISSN());
  }

  String badContent =
          "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" +
                  "<p><strong>ISSN: </strong>" + goodPrintISSN + " </p>\n" +
                  "<p><strong>ISSN: </strong>" + goodOnlineISSN + "</p>\n" +
                  "<p><strong>foo: </strong>Apr 2009 : Volume " + goodVolume + " Issue " + goodIssue + "</p>\n" +
                  "<span class=\"title\">\n" +
                  "        \n" +
                  "                pages " + goodStartPage + "-" + goodEndPage + "\n" +
                  "            \n" +
                  "            </span>\n" +
                  "    \n" +
                  "</BODY></HTML>";

  public void testExtractFromBadContent() throws Exception {
    Metadata md = extractFromTestContent(badContent);

    assertNull(md.getVolume());
    assertNull(md.getIssue());
    assertNull(md.getStartPage());
    assertNull(md.getISSN());

    assertEquals(1, md.size());
  }

  private Metadata extractFromTestContent(String content) throws Exception {
    String url = "http://www.example.org/doi/abs/10.1640/0002-8444-99.2.61";
    MockCachedUrl cu = new MockCachedUrl(url, sau);
    cu.setContent(content);
    cu.setContentSize(content.length());
    MetadataExtractorFactory mef = new BioOneMetadataExtractorFactory();
    MetadataExtractor me = mef.createMetadataExtractor("text/html");
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    assertTrue(me instanceof BioOneMetadataExtractorFactory.BioOneMetadataExtractor);
    Metadata md = me.extract(cu);
    assertNotNull(md);
    return md;
  }


  private void createContent() {
    log.debug("Generating tree of size 3x1x2 with " + fileSize + "byte files...");
    sau.generateContentTree();
  }

  private void crawlContent() {
    log.debug("Crawling tree...");
    CrawlSpec spec = new SpiderCrawlSpec(sau.getNewContentCrawlUrls(), null);
    NewContentCrawler crawler = new NewContentCrawler(sau, spec, new MockAuState());
    //crawler.setCrawlManager(crawlMgr);
    crawler.doCrawl();
  }

  private static String getFieldContent(String content, int fileNum, int depth, int branchNum) {
    content = StringUtil.replaceString(content, "%1", "" + fileNum);
    content = StringUtil.replaceString(content, "%2", "" + depth);
    content = StringUtil.replaceString(content, "%3", "" + branchNum);
    return content;
  }

  public void checkMetadata(Metadata md) {
    String temp = null;
    temp = (String) md.get("lockss.filenum");
    int fileNum = -1;
    try {
      fileNum = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      log.error(temp + " caused " + ex);
      fail();
    }
    temp = (String) md.get("lockss.depth");
    int depth = -1;
    try {
      depth = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      log.error(temp + " caused " + ex);
      fail();
    }
    temp = (String) md.get("lockss.branchnum");
    int branchNum = -1;
    try {
      branchNum = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      log.error(temp + " caused " + ex);
      fail();
    }

    // Do the accessors return the expected values?
    assertEquals(getFieldContent(volumeTemplate, fileNum, depth, branchNum), md.getVolume());
    assertEquals(getFieldContent(issnTemplate, fileNum, depth, branchNum), md.getISSN());
    assertEquals(getFieldContent(issueTemplate, fileNum, depth, branchNum), md.getIssue());
    assertEquals(getFieldContent(fPageTemplate, fileNum, depth, branchNum), md.getStartPage());
    // can't assert doi here as it's extracted from URL
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {

    public ArchivalUnit createAu0(Configuration auConfig) throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    /**
     * Returns the article iterator factory for the mime type, if any
     *
     * @param contentType the content type
     * @return the ArticleIteratorFactory
     */
    public ArticleIteratorFactory getArticleIteratorFactory(String contentType) {
      MySubTreeArticleIteratorFactory ret =
              new MySubTreeArticleIteratorFactory();
      ret.setSubTreeRoot("branch1/branch1/branch1");
      return ret;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

    public MetadataExtractor getMetadataExtractor(String fileType, ArchivalUnit au) {

      MetadataExtractorFactory mef = new BioOneMetadataExtractorFactory();
      MetadataExtractor me = null;
      try {
        me = mef.createMetadataExtractor("text/html");
      } catch (PluginException ex) {
        log.error("createMetadataExtractor threw: " + ex);
      }
      return me;
    }

  }

  public static class MySubTreeArticleIteratorFactory implements ArticleIteratorFactory {
    String subTreeRoot;

    MySubTreeArticleIteratorFactory() {
    }

    /**
     * Create an Iterator that iterates through the AU's articles, pointing
     * to the appropriate CachedUrl of type mimeType for each, or to the plugin's
     * choice of CachedUrl if mimeType is null
     *
     * @param mimeType the MIME type desired for the CachedUrls
     * @param au       the ArchivalUnit to iterate through
     * @return the ArticleIterator
     * 
     */
    public Iterator createArticleIterator(String mimeType, ArchivalUnit au) throws PluginException {
      Iterator ret;
      Pattern pat = Pattern.compile("^.*[0-9][0-9][0-9]file.html$");
      if (exceptionCount == 0) {
        ret = new SubTreeArticleIterator(mimeType, au, subTreeRoot, pat);
      } else {
        ret = new MySubTreeArticleIterator(mimeType, au, subTreeRoot, exceptionCount);
      }
      return ret;
    }

    public void setSubTreeRoot(String root) {
      subTreeRoot = root;
      log.debug("Set subTreeRoot: " + subTreeRoot);
    }
  }

  public static class MySubTreeArticleIterator extends SubTreeArticleIterator {
    int exceptionCount;

    MySubTreeArticleIterator(String mimeType, ArchivalUnit au, String subTreeRoot, int exceptionCount) {
      super(mimeType, au, subTreeRoot);
      this.exceptionCount = exceptionCount;
    }

    protected void processCachedUrl(CachedUrl cu) {
      if (exceptionCount > 0 && cu.getUrl().endsWith(".html")) {
        exceptionCount--;
        throw new UnsupportedOperationException();
      }
      super.processCachedUrl(cu);
    }
  }

  public static class MySimulatedContentGenerator extends SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
      file_content += "  <meta name=\"lockss.filenum\" content=\"" + fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";
      file_content += "<p><strong>Print ISSN: </strong>" + getFieldContent(issnTemplate, fileNum, depth, branchNum) + " </p>\n";
      if (fileNum % 2 == 0) {
        file_content += "<p><strong>Online ISSN: </strong>" + getFieldContent(issnTemplate, fileNum, depth, branchNum) + "</p>\n";
      }
      file_content += "<p><strong>Current: </strong>Apr 2009 : Volume " + getFieldContent(volumeTemplate, fileNum, depth, branchNum) + " Issue " + getFieldContent(issueTemplate, fileNum, depth, branchNum) + "</p>\n";
      file_content += "<span class=\"title\">\n" +
              "        \n" +
              "                pg(s) " + getFieldContent(fPageTemplate, fileNum, depth, branchNum) + "-" + getFieldContent(lPageTemplate, fileNum, depth, branchNum) + "\n" +
              "            \n" +
              "            </span>\n" +
              "    ";

      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug3("MySimulatedContentGenerator.getHtmlFileContent: " + file_content);

      return file_content;
    }
  }

}
