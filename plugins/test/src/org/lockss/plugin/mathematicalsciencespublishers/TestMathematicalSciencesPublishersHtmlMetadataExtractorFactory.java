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

package org.lockss.plugin.mathematicalsciencespublishers;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://msp.org/camcos/2012/7-2/p04.xhtml
 */
public class TestMathematicalSciencesPublishersHtmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(
      TestMathematicalSciencesPublishersHtmlMetadataExtractorFactory.class);
  
  //Simulated AU to generate content
  private SimulatedArchivalUnit sau; 
  //MathematicalSciencesPublishers AU
  private ArchivalUnit hau; 
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = 
      "org.lockss.plugin.mathematicalsciencespublishers.ClockssMathematicalSciencesPublishersPlugin";

  private static String BASE_URL = "http://msp.org/";
  private static String SIM_ROOT = BASE_URL;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
        simAuConfig(tempDirPath));
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, mspAuConfig());
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
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "7");
    conf.put(
        "fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_PDF 
            + SimulatedContentGenerator.FILE_TYPE_XHTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration mspAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();

    conf.put("base_url", BASE_URL);
    conf.put("journal_issn", "1559-3940");
    conf.put("journal_id", "camcos");
    conf.put("year", "2012");
    return conf;
  }

  String goodDate = "2012";
  String goodJournalTitle = 
      "Communications in Applied Mathematics and Computational Science";
  String goodPublisher = "Mathematical Sciences Publishers";
  String goodSubject = "Subject";
  String goodDescription = "Description";
  String goodType = "Type";
  String goodArticle = "Title";
  String goodFormat = "Format";
  String goodAuthor = "Name1";
  String goodDoi = "10.2140/camcos.2012.7.247";
  String goodLanguage = "Language";
  String goodCoverage = "Coverage";
  String goodSource = "Source";
  String goodISSN = "1559-3940";
  String goodVolume = "7";
  String goodIssue = "2";
  String goodStartPage = "247";
  String goodEndPage = "271";

  // Unfortunately, it has to be on one line for an accurate representation (and
  // to work)
  String goodContent =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
      "<head>" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />" +
      "<meta name=\"citation_publisher\" content=\n" +
      "  \"Mathematical Sciences Publishers\" />" +
      "<meta name=\"citation_title\" content=\n\"Title\" />" +
      "<meta name=\"citation_journal_title\" content=\n" +
      "  \"Communications in Applied Mathematics and Computational Science\" />" +
      "<meta name=\"citation_volume\" content=\"7\" />" +
      "<meta name=\"citation_issue\" content=\"2\" />" +
      "<meta name=\"citation_firstpage\" content=\"247\" />" +
      "<meta name=\"citation_lastpage\" content=\"271\" />" +
      "<meta name=\"citation_publication_date\" content=\"2013-01-08\" />" +
      "<meta name=\"citation_pdf_url\" content=" +
      "  \"http://msp.org/camcos/2012/7-2/camcos-v7-n2-p04-s.pdf\" />" +
      "<meta name=\"citation_doi\" content=\"10.2140/camcos.2012.7.247\" />" +
      "<meta name=\"citation_issn\" content=\"1559-3940\" />" +
      "<meta name=\"citation_author\" content=\"Name1, f1\" />" +
      "<meta name=\"citation_author\" content=\"Name2, f2\" />" +
      "<title>Communications in Applied Mathematics and Computational Science" +
      "  Vol. 7, No. 2, 2012</title>" +
      "<link href=\"/camcos/etc/journal.css\" type=\"text/css\" rel=\"stylesheet\" />" +
      "<link href=\"/camcos/etc/abstract.css\" type=\"text/css\" rel=\"stylesheet\" />" +
      "<link rel=\"shortcut icon\" href=\"/camcos/etc/favicon.ico\" />" +
      "<script type=\"text/javascript\" src=\"/camcos/etc/cover.js\"></script>" +
      "</head>" +
      "</html>";

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://msp.org/camcos/2012/7-2/p04.xhtml";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = 
        new MathematicalSciencesPublishersHtmlMetadataExtractorFactory.
            MathematicalSciencesPublishersHtmlMetadataExtractor("text/html");
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEndPage, md.get(MetadataField.FIELD_END_PAGE));
  }

  String badContent = 
      "<HTML><HEAD><TITLE>" + goodJournalTitle + 
      "</TITLE>\n" + "<meta name=\"foo\" content=\"bar\">\n</HEAD><BODY>" + 
      "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " + 
      goodDescription + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me = 
        new MathematicalSciencesPublishersHtmlMetadataExtractorFactory.
            MathematicalSciencesPublishersHtmlMetadataExtractor("text/html");
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertNull(md.get(MetadataField.FIELD_PUBLICATION_TITLE));

    assertEquals(1, md.rawSize());
  }
  
  // test scraping text from content 
  String missingMetaContent = 
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
      "<head>" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" +
      "<title>Editorial, Title Page</title>" +
      "</head><body><table>" +
      "<tr>\n<td class=\"content-column\"></td></tr>" +
      "<tr>\n<td class=\"title-area\"><div class=\"title\">" +
      "Editorial</div>" +
      "<h3>fName lName</h3>" +
      "</td></tr></table>" +
      "<table><tr><td>" +
      "<p class=\"noindent\">DOI: 10.2140/gt.2013.17.2061</p>" +
      "</td></tr></table>" +
      "</body>";
  
  public void testMissingMetaContent() throws Exception {
    
    String goodTitle = "Editorial";
    
    String url = "http://www.example.com/jid/2008/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(missingMetaContent);
    cu.setContentSize(missingMetaContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    FileMetadataExtractor me = 
        new MathematicalSciencesPublishersHtmlMetadataExtractorFactory.
            MathematicalSciencesPublishersHtmlMetadataExtractor("text/xml");
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNotNull(md.get(MetadataField.FIELD_DOI));
  }
  
  String psuedoContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" + 
  		"    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" + 
  		"\n" + 
  		"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" + 
  		"<head>\n" + 
  		"  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" + 
  		"  <meta name=\"citation_title\" content=\n" + 
  		"  \"The Good Title\" />\n" + 
  		"  <meta name=\"citation_journal_title\" content=\n" + 
  		"  \"Algebra &amp; Number Theory\" />\n" + 
  		"  <meta name=\"citation_volume\" content=\"0\" />\n" + 
  		"  <meta name=\"citation_issue\" content=\"0\" />\n" + 
  		"  <meta name=\"citation_firstpage\" content=\"0\" />\n" + 
  		"  <meta name=\"citation_lastpage\" content=\"99\" />\n" + 
  		"  <meta name=\"citation_publication_date\" content=\"2012-08-15\" />\n" + 
  		"  <meta name=\"citation_pdf_url\" content=\n" + 
  		"  \"/ant/2012/0/ant-v0-n0-p01-s.pdf\" />\n" + 
  		"  <title>Algebra &amp; Number Theory Vol. 0, No. 0, 2012</title>\n" + 
  		"</head>\n" + 
  		"\n" + 
  		"<body onload=\"javascript:void(0);\">\n" + 
  		"  <table cellspacing=\"0\" cellpadding=\"0\" class=\"main\" id=\"main-area\">\n" + 
  		"    <tr>\n" + 
  		"      <td class=\"activity-column\" id=\"activity-area\">\n" + 
  		"      </td>\n" + 
  		"      <td class=\"content-column\" id=\"content-area\">\n" + 
  		"        <table cellspacing=\"0\" cellpadding=\"0\">\n" + 
  		"          <tr>\n" + 
  		"            <td class=\"title-area\">\n" + 
  		"              <a class=\"title\" href=\"/ant/2012/0/ant-v0-n0-p01-s.pdf\">The\n" + 
  		"              Good Title</a>\n" + 
  		"              <h3>An Author</h3>\n" + 
  		"            </td>\n" + 
  		"          </tr>\n" + 
  		"          <tr>\n" + 
  		"            <td>\n" + 
  		"              <div class=\"page-numbers\">\n" + 
  		"                Vol. 0 (2012), No. 0, 0–99\n" + 
  		"              </div>\n" + 
  		"            </td>\n" + 
  		"          </tr>\n" + 
  		"          <tr>\n" + 
  		"            <td>\n" + 
  		"              <div class=\"paper-doi\">\n" + 
  		"                DOI: <a href=\n" + 
  		"                \"http://dx.doi.org/10.2140/ant.2012.0.1\">10.2140/ant.2012.0.1</a>\n" + 
  		"              </div>\n" + 
  		"            </td>\n" + 
  		"          </tr>\n" + 
  		"        </table>\n" + 
  		"        <table cellspacing=\"0\" cellpadding=\"0\" class=\"article\">\n" + 
  		"          <tr>\n" + 
  		"            <td class=\"article-area\">\n" + 
  		"              <h5>Abstract</h5>\n" + 
  		"            </td>\n" + 
  		"          </tr>\n" + 
  		"          <tr>\n" + 
  		"            <td class=\"article-area\">\n" + 
  		"            \n" + 
  		"<p class=\"noindent\">some teXt we do not need\n" + 
  		" </p>\n" + 
  		"            </td>\n" + 
  		"          </tr>\n" + 
  		"        </table>\n" + 
  		"      </td>\n" + 
  		"    </tr>\n" + 
  		"  </table>\n" + 
  		"</body>\n" + 
  		"</html>\n";
  
  // test contentType application/ 
  public void testMspXmlMetaContent() throws Exception {
    
    String goodTitle = "The Good Title";
    
    String url = "http://www.example.com/ant/2012/6-1/p01.xhtml";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    
    cu.setContent(psuedoContent);
    cu.setContentSize(psuedoContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xhtml+xml");
    FileMetadataExtractor me = 
        new MathematicalSciencesPublishersHtmlMetadataExtractorFactory.
            MathematicalSciencesPublishersHtmlMetadataExtractor("application/xhtml+xml");
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNotNull(md.get(MetadataField.FIELD_DOI));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
  }
  
  
  /**
   * Inner class that where a number of Archival Units can be created
   * 
   */
  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
        throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf,
        String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create a html source code simulated content
   */
  public static class MySimulatedContentGenerator extends
      SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth,
        int branchNum, boolean isAbnormal) {

      String file_content = "<HTML><HEAD><TITLE>" + filename
          + "</TITLE></HEAD><BODY>\n";

      file_content += "  <meta name=\"lockss.filenum\" content=\"" + fileNum
          + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth
          + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\""
          + branchNum + "\">\n";

      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
          + file_content);

      return file_content;
    }
  }
}