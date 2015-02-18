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

package org.lockss.plugin.americanmathematicalsociety;

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
public class TestAmericanMathematicalSocietyHtmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(
      TestAmericanMathematicalSocietyHtmlMetadataExtractorFactory.class);
  
  //Simulated AU to generate content
  private SimulatedArchivalUnit sau; 
  //AmericanMathematicalSociety AU
  private ArchivalUnit hau; 
  private MockLockssDaemon theDaemon;
  
  private static String PLUGIN_NAME = 
      "org.lockss.plugin.americanmathematicalsociety.ClockssAmericanMathematicalSocietyPlugin";
  
  private static String BASE_URL = "http://www.ams.org/";
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
    
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
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
    conf.put("depth", "0");
    conf.put("branch", "0");
    conf.put("numFiles", "4");
    conf.put(
        "fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_PDF 
            + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }
  
  Configuration mspAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "jams");
    conf.put("year", "2013");
    return conf;
  }
  
  String goodDate = "2013";
  String goodJournalTitle = "Journal of the American Mathematical Society";
//  String goodPublisher = "Mathematical Sciences Publishers";
  String goodType = "Type";
  String goodArticle = "Title";
  String goodFormat = "Format";
  String goodAuthor = "Name1";
  String goodDoi = "10.1090/S0894-0347-2012-00756-5";
  String goodISSN = "0894-0347";
  String goodVolume = "26";
  String goodIssue = "2";
  String goodStartPage = "295";
  String goodEndPage = "340";
  String goodDescription = "Description";
  
  String goodContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
      "<head>\n" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" + 
      "<title>Journal of the American Mathematical Society</title>\n" + 
      "<meta name=\"citation_journal_title\" content=\"Journal of the American Mathematical Society\">\n" + 
      "<meta name=\"citation_journal_abbrev\" content=\"J. Amer. Math. Soc.\">\n" + 
      "<meta name=\"citation_abstract_html_url\" content=\"http://www.ams.org/jams/2013-26-02/S0894-0347-2012-00756-5/\">\n" + 
      "<meta name=\"citation_pdf_url\" content=\"http://www.ams.org/jams/2013-26-02/S0894-0347-2012-00756-5/S0894-0347-2012-00756-5.pdf\">\n" + 
      "<meta name=\"citation_issn\" content=\"0894-0347\">\n" + 
      "<meta name=\"citation_issn\" content=\"1088-6834\">\n" + 
      "<meta name=\"citation_author\" content=\"Name1, A\">\n" + 
      "<meta name=\"citation_author_institution\" content=\"Department of Mathematics, ETH-Zürich, Rämistrasse 101, 8092 Zürich, Switzerland\">\n" + 
      "<meta name=\"citation_title\" content=\"Title\">\n" + 
      "<meta name=\"citation_online_date\" content=\"2012/12/10\">\n" + 
      "<meta name=\"citation_publication_date\" content=\"2013\">\n" + 
      "<meta name=\"citation_volume\" content=\"26\">\n" + 
      "<meta name=\"citation_issue\" content=\"2\">\n" + 
      "<meta name=\"citation_firstpage\" content=\"295\">\n" + 
      "<meta name=\"citation_lastpage\" content=\"340\">\n" + 
      "<meta name=\"citation_doi\" content=\"10.1090/S0894-0347-2012-00756-5\">\n" + 
      "</head>\n" +
      "</html>";
  
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.ams.org/journals/jams/2013-26-02/S0894-0347-2012-00756-5/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new 
        AmericanMathematicalSocietyHtmlMetadataExtractorFactory.
        AmericanMathematicalSocietyHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
//    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
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
    FileMetadataExtractor me = new 
        AmericanMathematicalSocietyHtmlMetadataExtractorFactory.
        AmericanMathematicalSocietyHtmlMetadataExtractor();
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
  
}
