/*
 * $Id: TestBioOneMetadataExtractor.java,v 1.6 2010-08-23 16:38:47 dsferopoulos Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.lang.StringEscapeUtils;
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

/**
 * Two of the articles used to get the html source for this plugin is:
 * http://www.bioone.org/doi/abs/10.4202/app.2009.0087
 * http://www.bioone.org/doi/abs/10.3161/150811010X504554
 *
 */
public class TestBioOneMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestBioOneMetadataExtractor");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit bau;		// Bioone AU
  private MockLockssDaemon theDaemon;
  private static int exceptionCount;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;
  private static final String issnTemplate = "%1%2%3%1-%3%1%2%3";
  private static final String volumeTemplate = "%1%3";
  private static final String issueTemplate = "%2";
  private static final String fPageTemplate = "%2%3";
  private static final String lPageTemplate = "%3%1%2";  

  private static String PLUGIN_NAME = "org.lockss.plugin.bioone.BioOnePlugin";

  private static String BASE_URL = "http://www.bioone.org/";

  /**
   * Method to set up the daemon and create the simulated Archival Unit (AU). To be copied as it is to all future plugins
   */
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    PluginManager pluginMgr = theDaemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginMgr.startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
					     simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, biooneAuConfig());
  }

  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configure the simulated archival unit.
   * @param rootPath
   * @return
   */
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "4");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    //     conf.put("default_article_mime_type", "application/pdf");
    return conf;
  }

  /**
   * Configure the bioone AU
   * @return
   */
  Configuration biooneAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "0002-8444");
    conf.put("volume", "92");
    return conf;
  }

  public void testExtraction() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    PluginTestUtil.copyAu(sau, bau);
    exceptionCount = 0;
    int count = 0;
    for (Iterator<ArticleFiles> it = bau.getArticleIterator(); it.hasNext();) {
      CachedUrl cu = it.next().getFullTextCu();
      assertNotNull(cu);
      log.debug3("count " + count + " url " + cu.getUrl());
      ArticleMetadataExtractor me =
	bau.getPlugin().getArticleMetadataExtractor(null, bau);
      log.debug3("Extractor: " + me.toString());
      assertTrue(""+me,
		 me instanceof BioOneArticleIteratorFactory.BioOneArticleMetadataExtractor);
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      ArticleMetadata md = me.extract(af);
      assertNotNull(md);
      checkMetadata(md);
      count++;
    }
    log.debug("Article count is " + count);
    //assertEquals(28, count); // XXX Uncomment when iterators and extractors are back
  }

  	String goodDOI = "10.1640/0002-8444-99.2.61";
	String goodVolume = "13";
	String goodIssue = "4";
	String goodStartPage = "123";
	String goodEndPage = "134";
	String goodOnlineISSN = "1234-5679";
	String goodPrintISSN = "2345-567X";
	String goodDate = "2008-10-23";
	String goodAuthor1 = "Fred Bloggs";
	String goodAuthor2 = "Patr&#237;cia Hadler";
	String goodAuthor3 = "Ana Maria Ribeiro";
	String goodArticleTitle = "Spurious Results";
	String goodJournalTitle = "Acta Chiropterologica";
	String goodAbsUrl = "http://www.bioone.org/doi/abs/10.1640/0002-8444-99.2.61";
	String goodHtmUrl = "http://www.bioone.org/doi/full/10.1640/0002-8444-99.2.61";
	String goodPdfUrl = "http://www.bioone.org/doi/pdf/10.1640/0002-8444-99.2.61";

	// A String holding a chunk of relevant HTML of a Bioone article. Only relevant chunks are added to minimise the length of the string and
	// maximise readability. 
	String goodContent = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en-US\" lang=\"en-US\">\n" +
    "<head>\n" +
    "    <title>\n" +
    "        BioOne Online Journals\n" +
    "        -\n" +
    "        \n" +
    "                "+goodArticleTitle+"\n"+					    
    "            \n" +
    "    </title>" +
    "\n" +
    "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link><meta name=\"dc.Title\" content=\""+goodArticleTitle+"\"></meta><meta name=\"dc.Creator\" content=\" "+goodAuthor1+" \"></meta><meta name=\"dc.Creator\" content=\" "+goodAuthor2+" \"></meta><meta name=\"dc.Creator\" content=\" "+goodAuthor3+" \"></meta><meta name=\"dc.Date\" scheme=\"WTN8601\" content=\""+goodDate+"\"></meta>"+
    "        <div class=\"clearFloats\">&nbsp;</div>\n" +
    "         \n" +
    "\n" +
    "\n" +
    " <h1 class=\"pageTitleNoRule\">"+goodJournalTitle+"</h1>\n" +
    " "+
    "        <p><strong>Print ISSN: </strong>"+goodPrintISSN+" </p>\n" +
    "        \n" +
    "        \n" +
    "        <p><strong>Online ISSN: </strong>"+goodOnlineISSN+"</p>\n" +
    "        \n" +
    "        <p><strong>Current: </strong>Jun 2010 : Volume "+goodVolume+" Issue "+goodIssue+"</p>" +
    "\n" +
    "        / <span class=\"title\">\n" +
    "        \n" +
    "                pg(s) "+goodStartPage+"-"+goodEndPage+"\n" +
    "            \n" +
    "            </span>"+
    "    \n" + "</BODY></HTML>";

	
  /**
   * Method that asserts all metadata extracted from the html. This is the method where we test whether the BePressHtmlMetadataExtractor can extract
   * the data that should be extracted
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    ArticleMetadata md = extractFromTestContent(goodContent);
    
    assertTrue(MetadataUtil.isDOI(md.getDOI()));
	assertEquals(goodDOI, md.getDOI());				
	assertEquals(goodVolume, md.getVolume());
	assertEquals(goodIssue, md.getIssue());
	assertEquals(goodStartPage, md.getStartPage());
	assertTrue(MetadataUtil.isISSN(md.getISSN()));
	assertEquals(goodOnlineISSN, md.getISSN());
	String authors = StringEscapeUtils.unescapeHtml(goodAuthor1)+", "+StringEscapeUtils.unescapeHtml(goodAuthor2)+", "+StringEscapeUtils.unescapeHtml(goodAuthor3);
	assertEquals(authors, md.getAuthor());
	assertEquals(goodArticleTitle, md.getArticleTitle());
	assertEquals(goodJournalTitle, md.getJournalTitle());
	assertEquals(goodDate, md.getDate());
  }

  // A string representing bad html markup for a bioone article. Metadata from this code should NOT be extracted
  String badContent =
          "<HTML><HEAD><TITLE>" + goodArticleTitle + "</TITLE></HEAD><BODY>\n" +
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

  /**
   * Method that asserts that data is NOT extracted from the badContend string
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    ArticleMetadata md = extractFromTestContent(badContent);

    assertNull(md.getVolume());
    assertNull(md.getIssue());
    assertNull(md.getStartPage());
    assertNull(md.getISSN());

    assertEquals(1, md.size());
  }

  /**
   * Method that makes use of the BioOneHtmlMetadataExtractor to extract the metadata of the html content passed as a parameter.
   * @param content The html code we want to extract the metadata from
   * @return An articleMetadata object where we can retreive all relevant metadata.
   * @throws Exception
   */
  private ArticleMetadata extractFromTestContent(String content) throws Exception {
    String url = "http://www.example.org/doi/abs/10.1640/0002-8444-99.2.61";
    MockCachedUrl cu = new MockCachedUrl(url, sau);
    cu.setContent(content);
    cu.setContentSize(content.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    cu.setFileMetadataExtractor(new BioOneHtmlMetadataExtractorFactory.BioOneHtmlMetadataExtractor());
    ArticleMetadataExtractorFactory mef = new BioOneArticleIteratorFactory();
    ArticleMetadataExtractor me =
      mef.createArticleMetadataExtractor(null);
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    assertTrue(""+me,
	       me instanceof BioOneArticleIteratorFactory.BioOneArticleMetadataExtractor);
    ArticleFiles af = new ArticleFiles();
    af.setFullTextCu(cu);
    ArticleMetadata md = me.extract(af);
    assertNotNull(md);
    return md;
  }

  /**
   * Method to replace strings containing for example "%1%3" with ints according to the fileNum, depth and branchNum
   * @param content The string to replace
   * @param fileNum
   * @param depth
   * @param branchNum
   * @return
   */
  private static String getFieldContent(String content, int fileNum, int depth, int branchNum) {
    content = StringUtil.replaceString(content, "%1", "" + fileNum);
    content = StringUtil.replaceString(content, "%2", "" + depth);
    content = StringUtil.replaceString(content, "%3", "" + branchNum);
    return content;
  }

  public void checkMetadata(ArticleMetadata md) {
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

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class that generates simulated content. It is not an essential part of this test class. In this case the content representing the AU does
   * not even resembles actual BioOne html article content.
   *    
   */
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
