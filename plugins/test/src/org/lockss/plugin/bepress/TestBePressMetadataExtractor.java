/*
 * $Id: TestBePressMetadataExtractor.java,v 1.7 2010-06-17 18:41:27 tlipkis Exp $
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

package org.lockss.plugin.bepress;

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


public class TestBePressMetadataExtractor extends LockssTestCase{

  static Logger log = Logger.getLogger("TestBePressMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit bau;		// BePress AU
  private static final String issnTemplate = "%1%2%3%1-%3%1%2%3";

  private static final Map<String, String> tagMap =
    new HashMap<String, String>();
  static {
    tagMap.put("bepress_citation_journal_title", "JOURNAL %1 %2 %3");
    //tagMap.put("issn", "%1%2%3%1-%3%1%2%3"); // issn in bepress is in a div tag
    tagMap.put("bepress_citation_authors", "AUTHOR %1 %2 %3");
    tagMap.put("bepress_citation_title", "TITLE %1 %2 %3");
    tagMap.put("bepress_citation_date", "%1/%2/%3");
    tagMap.put("bepress_citation_volume", "%1%2");
    tagMap.put("bepress_citation_issue", "%3%2%1");
    tagMap.put("bepress_citation_firstpage", "%2%1%3");
    tagMap.put("bepress_citation_pdf_url", "http://www.example.com/cgi/viewcontent.cgi?article=%1%2%3%1&amp;context=%2%1");
    tagMap.put("bepress_citation_abstract_html_url",
	       "http://www.example.com/ev/vol%1/iss%2/art%3");
    tagMap.put("bepress_citation_doi", "10.2202/%1%2%3%1-%3%2%1%2-%2%1%3%1");
  };

  private static String PLUGIN_NAME = "org.lockss.plugin.bepress.BePressPlugin";

  private static String BASE_URL = "http://www.bepress.com/";
  private static String SIM_ROOT = BASE_URL + "xyzjn/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
					     simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, bePressAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  Configuration bePressAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume", "1");
    conf.put("journal_abbr", "xyzjn");
    return conf;
  }

  public void testExtraction() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat = "branch(\\d+)/branch(\\d+)/(\\d+)file\\.html";
    String rep = "vol$1/iss$2/art$3";
    PluginTestUtil.copyAu(sau, bau, null, pat, rep);

    Plugin plugin = bau.getPlugin();
    ArticleMetadataExtractor me = plugin.getArticleMetadataExtractor(null, bau);
    log.debug("Extractor: " + me.toString());
    assertTrue(me instanceof BePressArticleIteratorFactory.BePressArticleMetadataExtractor);
    int count = 0;
    for (Iterator<ArticleFiles> it = bau.getArticleIterator(); it.hasNext(); ) {
      ArticleFiles af = it.next();
      CachedUrl cu = af.getFullTextCu();
      assertNotNull(cu);
      log.debug3("count " + count + " url " + cu.getUrl());
      Metadata md = me.extract(af);
      assertNotNull(md);
      checkMetadata(md);
      count++;
    }
    log.debug("Article count is " + count);
    assertEquals(21, count);
  }

  String goodDOI = "10.2202/bogus.13.4.123";
  String goodVolume = "13";
  String goodIssue = "4";
  String goodStartPage = "123";
  String goodISSN = "1234-5678";
  String goodDate = "4/1/2000";
  String goodAuthor = "Gandhi, Pankaj J.; Talia, Yogen H.; Murthy, Z.V.P.";
  String goodTitle = "Spurious Results";
  String goodAbsUrl = "http://www.example.com/bogus/vol13/iss4/art123/abs";
  String goodPdfUrl = "http://www.example.com/bogus/vol13/iss4/art123/pdf";
  String goodHtmUrl = "http://www.example.com/bogus/vol13/iss4/art123/full";
  String[] dublinCoreField = {
    "dc.Identifier",
    "dc.Date",
    "dc.Contributor",
    "dc.Title",
  };
  String[] dublinCoreValue = {
    goodDOI,
    goodDate,
    goodAuthor,
    goodTitle,
  };
  String goodContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"bepress_citation_journal_title\"" + 
      " content=\"Bogus\">\n" +	
    "<meta name=\"bepress_citation_authors\"" + 
      " content=\"" + goodAuthor + "\">\n" + 
    "<meta name=\"bepress_citation_title\" content=\"" + goodTitle + "\">\n" +
    "<meta name=\"bepress_citation_date\" content=\"" + goodDate + "\">\n" +
    "<meta name=\"bepress_citation_volume\"" +
      " content=\"" + goodVolume + "\">\n" +
    "<meta name=\"bepress_citation_issue\" content=\"" + goodIssue + "\">\n" +
    "<meta name=\"bepress_citation_firstpage\"" +
      " content=\"" + goodStartPage + "\">\n" +
    "<meta name=\"bepress_citation_pdf_url\"" +
      " content=\"" + goodPdfUrl + "\">\n" +
    "<meta name=\"bepress_citation_abstract_html_url\"" +
      " content=\"" + goodAbsUrl + "\">\n" +
    "<meta name=\"bepress_citation_doi\"" +
      " content=\"" + goodDOI + "\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->ISSN: " +
    goodISSN + " </div>\n";

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
      new BePressHtmlMetadataExtractorFactory.BePressHtmlMetadataExtractor();
    Metadata md = me.extract(cu);
    assertNotNull(md);
    assertEquals(goodDOI, md.getDOI());
    assertEquals(goodVolume, md.getVolume());
    assertEquals(goodIssue, md.getIssue());
    assertEquals(goodStartPage, md.getStartPage());
    assertEquals(goodISSN, md.getISSN());

    goodAuthor = goodAuthor.replaceAll(",", "");
    goodAuthor = goodAuthor.replaceAll(";", ",");
    
    assertEquals(goodAuthor, md.getAuthor());
    assertEquals(goodTitle, md.getArticleTitle());
    for (int i = 1; i < dublinCoreField.length; i++) {
      assertEquals(dublinCoreValue[i], md.getProperty(dublinCoreField[i]));
    }
  }

  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodISSN + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
      new BePressHtmlMetadataExtractorFactory.BePressHtmlMetadataExtractor();
    Metadata md = me.extract(cu);
    assertNotNull(md);
    assertNull(md.getDOI());
    assertNull(md.getVolume());
    assertNull(md.getIssue());
    assertNull(md.getStartPage());
    assertNull(md.getISSN());
    for (int i = 1; i < dublinCoreField.length; i++) {
      assertNull(md.getProperty(dublinCoreField[i]));
    }
    assertEquals(1, md.size());
    assertEquals("bar", md.getProperty("foo"));
  }

  private static String getFieldContent(String content, int fileNum, int depth, int branchNum) {
    content = StringUtil.replaceString(content, "%1", ""+fileNum);
    content = StringUtil.replaceString(content, "%2", ""+depth);
    content = StringUtil.replaceString(content, "%3", ""+branchNum);
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

    // Does md have all the fields in the meta tags with the right content?
    for (Iterator it = tagMap.keySet().iterator(); it.hasNext(); ) {
      String expected_name = (String)it.next();

      if (expected_name.startsWith("lockss")) {
	    continue;
      }
      String expected_content = getFieldContent(tagMap.get(expected_name), fileNum, depth, branchNum);      

      assertNotNull(expected_content);
      log.debug("key: " + expected_name + " value: " + expected_content);
      String actual_content;

      actual_content = (String)md.get(expected_name.toLowerCase());

      assertNotNull(actual_content);
      log.debug("expected: " + expected_content + " actual: " + actual_content);
      assertEquals(expected_content, actual_content);
    }

    // Do the accessors return the expected values?    
    assertEquals(getFieldContent(tagMap.get("bepress_citation_volume"), fileNum, depth, branchNum), md.getVolume());
    assertEquals(getFieldContent(issnTemplate, fileNum, depth, branchNum),  md.getISSN());    
    assertEquals(getFieldContent(tagMap.get("bepress_citation_issue"), fileNum, depth, branchNum), md.getIssue());
    assertEquals(getFieldContent(tagMap.get("bepress_citation_firstpage"), fileNum, depth, branchNum), md.getStartPage());
    assertEquals(getFieldContent(tagMap.get("bepress_citation_doi"), fileNum, depth, branchNum), md.getDOI());

  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
	throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  public static class MySimulatedContentGenerator extends SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
      for (Iterator it = tagMap.keySet().iterator(); it.hasNext(); ) {
        String name = (String)it.next();
        String content = tagMap.get(name);
        file_content += "  <meta name=\"" + name + "\" content=\"" + getFieldContent(content, fileNum, depth, branchNum) + "\">\n";
      }
      file_content += "  <meta name=\"lockss.filenum\" content=\"" + fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";
      file_content += "  <div id=\"issn\"><!-- FILE: /main/production/doc/data/templates/www.example.com/proto_bpjournal/assets/issn.inc -->ISSN: "+getFieldContent(issnTemplate, fileNum, depth, branchNum)+" </div>\n";

      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
		    + file_content);
      
      return file_content;
    }
  }
}
