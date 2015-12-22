/*
 * $Id: TestSciELOHtmlMetadataExtractor.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.scielo;

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
 * http://ajpendo.physiology.org/content/301/5/E767.full
 *
 */
public class TestSciELOHtmlMetadataExtractor extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestSciELOHtmlMetadataExtractor.class);
  
  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit tau; // test AU
  
  private static String PLUGIN_NAME = "org.lockss.plugin.scielo.SciELOPlugin";
  private static String BASE_URL = "http://www.scielo.br/";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      JOURNAL_ISSN_KEY, "X090-999X",
      YEAR_KEY, "2013");
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class, simAuConfig(tempDirPath));
    tau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  @Override
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes",""	+ (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }
  
  // the metadata that should be extracted
  String goodJournalTitle = "Doctor Friendly's Fake Journal of Laughs";
  String goodPublisher = "CBCD";
  String goodArticleTitle = "surgery in patients";
  String goodDate = "06/2014";
  String goodVolume = "27";
  String goodIssue = "5";
  String goodISSN = "1234-5678";
  String goodDOI = "10.1590/S0102-67202014000200199";
  String goodURLabs = "http://www.scielo.br/scielo.php?script=sci_abstract&amp;pid=S0102-67202014000200199&amp;lng=pt&amp;nrm=iso&amp;tlng=en";
  String goodURL = "http://www.scielo.br/scielo.php?script=sci_arttext&amp;pid=S0102-67202014000200199&amp;lng=pt&amp;nrm=iso&amp;tlng=en";
  String[] goodAuthors = new String[] {"Baker, Alfred", "Elfman, Charles D.", "Gale, Fiona"};
  String goodStartPage = "199";
  String goodLastPage = "205";
  String goodCitationID = "10.1590/S0102-67202014000200199";
  String goodURLpdf = "http://www.scielo.br/pdf/abcd/v27n2/0102-6720-abcd-27-02-00199.pdf";
  
  // a chunk of html source code from the publisher's site from where the metadata should be extracted
  String goodContent = "" +
      "<head>" +
      "<title>surgery in patients</title>" +
      "<meta content=\"no-cache\" http-equiv=\"Pragma\">" +
      "<meta content=\"Mon, 06 Jan 1990 00:00:01 GMT\" http-equiv=\"Expires\">" +
      "<meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\">" +
      "<meta content=\"" + goodJournalTitle + "\" name=\"citation_journal_title\">" +
      "<meta content=\"" + goodPublisher + "\" name=\"citation_publisher\">" +
      "<meta content=\"" + goodArticleTitle + "\" name=\"citation_title\">" +
      "<meta content=\"" + goodDate + "\" name=\"citation_date\">" +
      "<meta content=\"" + goodVolume + "\" name=\"citation_volume\">" +
      "<meta content=\"" + goodIssue  + "\" name=\"citation_issue\">" +
      "<meta content=\"" + goodISSN + "\" name=\"citation_issn\">" +
      "<meta content=\"" + goodDOI + "\" name=\"citation_doi\">" +
      "<meta content=\"" + goodURLabs + "\" name=\"citation_abstract_html_url\">" +
      "<meta content=\"" + goodURL + "\" name=\"citation_fulltext_html_url\">" +
      "<meta content=\"" + goodAuthors[0] + "\" name=\"citation_author\" xmlns=\"\">" +
      "<meta content=\"" + goodAuthors[1] + "\" name=\"citation_author\" xmlns=\"\">" +
      "<meta content=\"" + goodAuthors[2] + "\" name=\"citation_author\" xmlns=\"\">" +
      "<meta content=\"199\" name=\"citation_firstpage\">" +
      "<meta content=\"205\" name=\"citation_lastpage\">" +
      "<meta content=\"" + goodCitationID + "\" name=\"citation_id\">" +
      "<meta content=\"" + goodURLpdf + "\" default=\"true\" language=\"en\" name=\"citation_pdf_url\" xmlns=\"\">" +
      "</head>";
  
  /**
   * Method that creates a simulated Cached URL from the source code provided by the goodContent String.
   * It then asserts that the metadata extracted with TaylorAndFrancisHtmlMetadataExtractorFactory
   * match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = goodURLabs;
    MockCachedUrl cu = new MockCachedUrl(url, tau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new SciELOHtmlMetadataExtractorFactory.SciELOHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    // will use metadata url if extracted goodURL does not exist and contain content
    // XXX assertEquals(url, md.get(MetadataField.FIELD_ACCESS_URL));
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
    MockCachedUrl cu = new MockCachedUrl(url, tau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new SciELOHtmlMetadataExtractorFactory.
        SciELOHtmlMetadataExtractor();
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
    @Override
    public ArchivalUnit createAu0(Configuration auConfig)
        throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }
    
    @Override
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
    
    @Override
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
