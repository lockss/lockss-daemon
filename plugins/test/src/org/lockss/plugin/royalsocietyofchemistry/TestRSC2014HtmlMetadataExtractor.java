/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the HTML source for this plugin is:
 * http://pubs.rsc.org/en/Content/ArticleLanding/2008/GC/B712109A
 * 
 */
public class TestRSC2014HtmlMetadataExtractor extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestRSC2014HtmlMetadataExtractor.class);
  
  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit rscau; // RSC AU
  
  private static String PLUGIN_NAME = "org.lockss.plugin.royalsocietyofchemistry.RSC2014Plugin";
  
  private static String BASE_URL = "http://pubs.rsc.org/";
  
  static final String RESOLVER_URL_KEY = "resolver_url";
  static final String GRAPHICS_URL_KEY = "graphics_url";
  static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
  static final String JOURNAL_CODE_KEY = "journal_code";
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,	simAuConfig(tempDirPath));
    rscau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, rsc2014AuConfig());
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put(RESOLVER_URL_KEY, BASE_URL);
    conf.put(GRAPHICS_URL_KEY, BASE_URL);
    conf.put(BASE_URL2_KEY, BASE_URL);
    conf.put(JOURNAL_CODE_KEY, "an");
    conf.put(VOLUME_NAME_KEY, "123");
    conf.put(YEAR_KEY, "2013");
    conf.put("depth", "1");
    conf.put("branch", "0");
    conf.put("numFiles", "4");
    conf.put("fileTypes",""	+ (
        SimulatedContentGenerator.FILE_TYPE_PDF + 
        SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }
  
  /**
   * Configuration method. 
   * @return
   */
  Configuration rsc2014AuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put(RESOLVER_URL_KEY, BASE_URL);
    conf.put(GRAPHICS_URL_KEY, BASE_URL);
    conf.put(BASE_URL2_KEY, BASE_URL);
    conf.put(JOURNAL_CODE_KEY, "an");
    conf.put("volume_name", "19");
    conf.put(YEAR_KEY, "2013");
    return conf;
  }
  
  // the metadata that should be extracted
  String goodLanguage = "en";
  String goodDOI = "10.1234/testdoi.00123.2011";
  String goodArticleTitle = "Qualitative and quantitative ...";
  String goodDate = "11/01/2011";
  String goodPublisher = "ABC Publicorp Inc., &c.";
  String[] goodAuthors = new String[] {"Baker, Alfred", "Elfman, Charles D.", "Gale, Fiona"};
  String goodJournalTitle = "Doctor Friendly's Fake Journal of Laughs";
  String goodISSN = "1234-5678";
  String goodVolume = "415";
  String goodIssue = "5";
  String goodStartPage = "R543";
  String goodEndPage = "R556";
  String goodURL = "http://fakejournaloflaughs.drfriendly.org/415/5/R543";
  
  // a chunk of html source code from the publisher's site from where the metadata should be extracted
  String goodContent = "" +
      "<meta content=\"" + goodLanguage + "\" name=\"DC.Language\" />" +
      "<meta content=\"" + goodPublisher + "\" name=\"DC.publisher\" />" +
      "<meta content=\"" + goodJournalTitle + "\" name=\"citation_journal_title\" />" +
      "<meta content=\"" + goodISSN + "\" name=\"citation_issn\" />" +
      "<meta content=\"" + goodDOI + "\" name=\"citation_doi\" />" +
      "<meta content=\"" + goodAuthors[0] + "\" name=\"citation_author\" />" +
      "<meta content=\"" + goodAuthors[1] + "\" name=\"citation_author\" />" +
      "<meta content=\"" + goodAuthors[2] + "\" name=\"citation_author\" />" +
      "<meta content=\"" + goodArticleTitle + "\" name=\"citation_title\" />" +
      "<meta content=\"" + goodDate + "\" name=\"citation_publication_date\" />" +
      "<meta content=\"" + goodVolume + "\" name=\"citation_volume\" />" +
      "<meta content=\"" + goodIssue + "\" name=\"citation_issue\" />" +
      "<meta content=\"" + goodStartPage + "\" name=\"citation_firstpage\" />" +
      "<meta content=\"" + goodEndPage + "\" name=\"citation_lastpage\" />" +
      "<meta content=\"" + goodURL + "\" name=\"citation_fulltext_html_url\" />";
  
  /**
   * Method that creates a simulated Cached URL from the source code provided by goodContent
   * It then asserts that the metadata extracted with HtmlMetadataExtractorFactory
   * match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://pubs.rsc.org/en/content/articlelanding/2008/gc/xx";
    MockCachedUrl cu = new MockCachedUrl(url, rscau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new RSC2014HtmlMetadataExtractorFactory.RSC2014HtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodPublisher, md.get(MetadataField.DC_FIELD_PUBLISHER));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(url, md.get(MetadataField.FIELD_ACCESS_URL));
  }
  
  // a chunk of HTML source code from where the HtmlMetadataExtractorFactory 
  // should NOT be able to extract metadata
  String badContent = "<html><head><title>" 
      + goodArticleTitle
      + "</title></head><body>\n"
      + "<meta name=\"foo\""
      + " content=\"bar\">\n"
      + "  <div id=\"issn\">"
      + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
      + goodISSN + " </div>\n";
  
  /**
   * Method that creates a simulated Cached URL from the source code provided by the 
   * badContent String. It then asserts that NO metadata is extracted by using 
   * the HighWirePressH20HtmlMetadataExtractorFactory as the source code is broken.
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, rscau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new RSC2014HtmlMetadataExtractorFactory.
        RSC2014HtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    
    assertEquals(1, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
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
    
    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }
    
  }
  
  /**
   * Inner class to create HTML source code simulated content
   *
   */
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }
    
    public String getHtmlFileContent(String filename, int fileNum, int depth, 
        int branchNum, boolean isAbnormal) {
      
      String file_content = "<html><head><title>" + filename + "</title></head><body>\n";
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";			
      
      file_content += getHtmlContent(fileNum, depth, branchNum,	isAbnormal);
      file_content += "\n</body></html>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "+ file_content);
      
      return file_content;
    }
  }
}
