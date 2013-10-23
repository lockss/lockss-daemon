/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs2;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ojs2.OJS2HtmlMetadataExtractorFactory.OJS2HtmlMetadataExtractor;
import org.lockss.plugin.simulated.*;

/*
 * TestOJS2MetadataExtractor invokes
 * OJS2HtmlMetadataExtractorFactory.java through 
 * OJS2ArticleIteratorFactory.java.  OJS2 AU is created in setUp() method.
 * 
 * One of the articles used to get the HTML source for this plugin is:
 * http://liber.library.uu.nl/index.php/lq/article/view/8039
 */
public class TestOJS2MetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestOJS2MetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit ojs2au; // OJS2 AU
  
  // XML file in org.lockss.plugin.ojs2 package
  private static String PLUGIN_NAME = "org.lockss.plugin.ojs2.OJS2Plugin";
  
  private static String BASE_URL = "http://liber.library.uu.nl/";
  private final String JOURNAL_ID = "lq";
  private final String YEAR = "2012";
  
  // Simulated journal ID: "OJS2 Journal"
  private static String SIM_ROOT = BASE_URL + "ojs2jn/";

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
    ojs2au = PluginTestUtil.createAndStartAu(PLUGIN_NAME, OJS2AuConfig());
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
    conf.put("fileTypes","" + (SimulatedContentGenerator.FILE_TYPE_PDF 
                               + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  /*
   * Configuration method. 
   * @return
   */
  Configuration OJS2AuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JOURNAL_ID);
    conf.put("year", YEAR);
    return conf;
  }

  /*
   * the metadata that should be extracted
   */
  String goodFormat = "text/html";
  String goodLanguage = "en";
  String goodArticleTitle = 
        "Twenty Years After: Armenian Research Libraries Today";
  String goodDCDate = "2012-05-18";
  String goodDate = "2012-05-23";
  String goodPublisher = "Igitur, Utrecht Publishing and Archiving Services";
  String[] goodAuthors = new String[] {"Donabedian, D. Aram",
                                       "Carey, John", "Balayan, Arshak"};
  String goodJournalTitle = "Doctor Friendly's Fake Journal of Laughs";
  String goodISSN = "2213-056X";
  String goodEISSN = "2213-056X";
  String goodDoi = "10.11566/cmoshmcj.v12i1.20";
  String goodVolume = "22";
  String goodIssue = "1";
  String goodStartPage = "3";
  String goodURL = "http://liber.library.uu.nl/index.php/lq/article/view/8039";

  /*
   * a chunk of html source code from the publisher's site from where the 
   * metadata should be extracted
   */
  String goodContent = 
      "<meta name=\"DC.Format\" content=\"" + goodFormat + "\" />" +
      "<meta name=\"DC.Language\" content=\"" + goodLanguage + "\" />" +
      "<meta content=\"" + goodArticleTitle + "\" name=\"DC.Title\" />" +
      "<meta content=\"" + goodDCDate + "\" name=\"DC.Date\" />" +
      "<meta content=\"" + goodPublisher + "\" name=\"DC.Publisher\" />" +
      "<meta content=\"" + goodJournalTitle + "\" name=\"citation_journal_title\" />" +
      "<meta content=\"" + goodISSN + "\" name=\"citation_issn\" />" +
      "<meta content=\"" + goodEISSN + "\" name=\"citation_issn\" />" +
      "<meta content=\"" + goodAuthors[0] + ";" + goodAuthors[1] + ";"
                         + goodAuthors[2] + "\" name=\"citation_authors\" />" +
      "<meta content=\"" + goodArticleTitle + "\" name=\"citation_title\" />" +
      "<meta content=\"" + goodDate + "\" name=\"citation_date\" />" +
      "<meta content=\"" + goodVolume + "\" name=\"citation_volume\" />" +
      "<meta content=\"" + goodIssue + "\" name=\"citation_issue\" />" +
      "<meta content=\"" + goodStartPage + "\" name=\"citation_firstpage\" />" +
      "<meta content=\"" + goodDoi + "\" name=\"citation_doi\" />" +
      "<meta content=\"" + goodURL + "\" name=\"citation_public_url\" />";
		
  /*
   * Method that creates a simulated Cached URL from the source code 
   * provided by the goodContent String. It then asserts that the metadata 
   * extracted with TaylorAndFrancisHtmlMetadataExtractorFactory
   * match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    
    // HTML file which the metadata extracted from.
    String url = "http://liber.library.uu.nl/index.php/lq/article/view/8039";
    
    MockCachedUrl cu = new MockCachedUrl(url, ojs2au);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    
    // Create FileMetadataExtractor object through OJS2HtmlMetadataExtractor().
    FileMetadataExtractor me = 
          new OJS2HtmlMetadataExtractorFactory.OJS2HtmlMetadataExtractor();
    
    // Create the metadata list containing all articles for this AU.
    // In this test case, the list has only one item.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodArticleTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals(goodDCDate, md.get(MetadataField.DC_FIELD_DATE));
    assertEquals(goodPublisher, md.get(MetadataField.DC_FIELD_PUBLISHER));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodVolume, md.get(MetadataField.DC_FIELD_CITATION_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodIssue, md.get(MetadataField.DC_FIELD_CITATION_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodURL, md.get(MetadataField.FIELD_ACCESS_URL));
        
  } // testExtractFromGoodContent

  /*
   * a chunk of HTML source code from where the
   * OJS2HtmlMetadataExtractorFactory should NOT be able to extract metadata
   */
  String badContent = "<html><head><title>" 
	+ goodArticleTitle
    + "</title></head><body>\n"
    + "<meta name=\"foo\""
    + " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + goodISSN + " </div>\n";

  /*
   * Method that creates a simulated Cached URL from the source code
   * provided by the badContent String. It then asserts that NO metadata 
   * is extracted by using the OJS2HtmlMetadataExtractorFactory as the 
   * source code is broken.
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, ojs2au);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    
    FileMetadataExtractor me =
        new OJS2HtmlMetadataExtractorFactory.OJS2HtmlMetadataExtractor();
    
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
    
  } // testExtractFromBadContent

  /*
   * Inner class that where a number of Archival Units can be created
   * for simulated content.
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

  } // MySimulatedPlugin

  /*
   * Inner class to create HTML source code simulated content.
   */
  public static class MySimulatedContentGenerator extends SimulatedContentGenerator {
    
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, 
                                     int depth, int branchNum, 
                                     boolean isAbnormal) {
			
      String file_content = "<html><head><title>" + filename + "</title></head><body>\n";
			
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";			

      file_content += getHtmlContent(fileNum, depth, branchNum,	isAbnormal);
      file_content += "\n</body></html>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
		    + file_content);

      return file_content;
      
    } // getHtmlFileContent
    
  } // MySimulatedContentGenerator
  
} // TestOJS2MetadataExtractor
