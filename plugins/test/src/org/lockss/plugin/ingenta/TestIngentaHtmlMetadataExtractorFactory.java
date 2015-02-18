/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www.ingentaconnect.com/content/maney/bjdd/2011/00000057/00000113/art00004
 */
public class TestIngentaHtmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(TestIngentaHtmlMetadataExtractorFactory.class);
  
  //Simulated AU to generate content
  private SimulatedArchivalUnit sau; 
  //Ingenta AU
  private ArchivalUnit hau; 
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = "org.lockss.plugin.ingenta.ClockssIngentaJournalPlugin";

  private static String BASE_URL = "http://www.ingentaconnect.com/";
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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, ingentaAuConfig());
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
    conf.put(
        "fileTypes",
        ""
            + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    // conf.put("default_article_mime_type", "application/pdf");
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration ingentaAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();

    conf.put("base_url", BASE_URL);
    conf.put("api_url", "http://api.ingentaconnect.com/");
    conf.put("graphics_url", "http://graphics.ingentaconnect.com/");
    conf.put("journal_issn", "1468-2737");
    conf.put("volume_name", "1");
    conf.put("publisher_id", "maney");
    conf.put("journal_id", "hrj");
    return conf;
  }

  String goodDate = "Date";
  String goodTitle = "British Corrosion Journal";
  String goodPublisher = "Publisher";
  String goodSubject = "Subject";
  String goodDescription = "Description";
  String goodType = "Type";
  String goodArticle = "Title";
  String goodFormat = "Format";
  String goodAuthor = " Name1";
  String goodDoi = "10.1179/096979511798967106";
  String goodLanguage = "Language";
  String goodCoverage = "Coverage";
  String goodSource = "Source";
  String goodISSN = "0007-0599";
  String goodVolume = "37";
  String goodIssue = "4";
  String goodStartPage = "267";
  String goodEndPage = "275";

  // Unfortunately, it has to be on one line for an accurate representation (and
  // to work)
  String goodContent =

  "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
      + "<html>\n"
      + "<head>\n"
      + "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link>"
      + "<meta name=\"dc.Title\" content=\"Title\"></meta>"
      + "<meta name=\"dc.Creator\" content=\" Name1\"></meta>"
      + "<meta name=\"dc.Creator\" content=\" Name2\">"
      + "</meta><meta name=\"dc.Creator\" content=\" Name3\"></meta>"
      + "<meta name=\"dc.Creator\" content=\" Name4\"></meta>"
      + "<meta name=\"dc.Subject\" content=\"Subject\"></meta>"
      + "<meta name=\"dc.Description\" content=\"Description\"></meta>"
      + "<meta name=\"dc.Publisher\" content=\"Publisher\"></meta>"
      + "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"Date\"></meta>"
      + "<meta name=\"dc.Type\" content=\"Type\"></meta>"
      + "<meta name=\"DCTERMS.isPartOf\" content=\"urn:ISSN:0007-0599\"></meta>"
      + "<meta name=\"DCTERMS.bibliographicCitation\" content=\"British Corrosion Journal,37,4,267-275(9)\"></meta>"
      + "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"info:doi/10.1179/096979511798967106\">"
      + "<meta name=\"dc.Source\" content=\"Source\"></meta>"
      + "<meta name=\"dc.Language\" content=\"Language\"></meta>"
      + "<meta name=\"dc.Coverage\" content=\"Coverage\"></meta>"
      + "<meta name=\"keywords\" content=\"keywords\"></meta>"
      + "\n</head>\n"
      + "</html>";

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.ingentaconnect.com/content/maney/bjdd/2011/00000057/00000113/art00004";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new IngentaHtmlMetadataExtractorFactory.IngentaHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodType, md.get(MetadataField.DC_FIELD_TYPE));
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEndPage, md.get(MetadataField.FIELD_END_PAGE));
  }

  String badContent = "<HTML><HEAD><TITLE>" + goodTitle
      + "</TITLE></HEAD><BODY>\n" + "<meta name=\"foo\""
      + " content=\"bar\">\n" + "  <div id=\"issn\">"
      + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
      + goodDescription + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me = new IngentaHtmlMetadataExtractorFactory.IngentaHtmlMetadataExtractor();
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
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertEquals(1, md.rawSize());
  }
  
  // bibliographicCitation that won't match extraction pattern
  // and make sure that it will handle comma's in the title....
  String oddBiblioContent = 
      "<meta name=\"DC.title\" content=\"Editorial\"/>" +
          "<meta name=\"DC.type\" scheme=\"DCMIType\" content=\"Text\"/>" +
          "<meta name=\"DC.creator\" content=\"Pearson, Sara L\"/>" +
          "<meta name=\"DC.publisher\" content=\"Maney Publishing\"/>" +
          "<meta name=\"DC.identifier\" scheme=\"URI\" content=\"info:doi/10.1179/1474893212Z.00000000046\"/>" +
          "<meta name=\"DCTERMS.issued\" content=\"November 2012\"/>" +
          "<meta name=\"DCTERMS.isPartOf\" scheme=\"URI\" content=\"urn:ISSN:1474-8932\"/>" +
          "<meta name=\"DCTERMS.bibliographicCitation\" content=\"Bronte, Wolf, and Blah Studies, 37, 4, iii\"/>";  
  String shortBiblioContent =
      "<meta name=\"DC.title\" content=\"Editorial\"/>" +
          "<meta name=\"DCTERMS.bibliographicCitation\" content=\"Just a Title so Move Along\"/>";  

  public void testOddBiblioContent() throws Exception {
    
    String goodTitle = "Editorial";
    
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(oddBiblioContent);
    cu.setContentSize(oddBiblioContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new IngentaHtmlMetadataExtractorFactory.IngentaHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals("Bronte, Wolf, and Blah Studies", md.get(MetadataField.FIELD_JOURNAL_TITLE));
    
    // try other content as well
    cu.setContent(shortBiblioContent);
    cu.setContentSize(shortBiblioContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    List<ArticleMetadata> mdlist2 = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist2);
    md = mdlist2.get(0);
    assertNotNull(md);
    assertEquals("Just a Title so Move Along", md.get(MetadataField.FIELD_JOURNAL_TITLE));
    
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