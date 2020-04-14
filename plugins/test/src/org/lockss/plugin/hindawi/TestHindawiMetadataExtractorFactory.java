/* $Id$

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

package org.lockss.plugin.hindawi;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www.amsciepub.com/doi/abs/10.2466/07.17.21.PMS.113.6.703-714 
 */
public class TestHindawiMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHindawiMetadataExtractorFactory");

  private SimulatedArchivalUnit sau;    // Simulated AU to generate content
  private ArchivalUnit hau;
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.hindawi.HindawiPublishingCorporationPlugin";

  private static String BASE_URL = "http://www.humanbean.com";
  private static String DOWNLOAD_URL = "http://www.download.com";

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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, hindawiAuConfig());
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
    conf.put("download_url", DOWNLOAD_URL);
    conf.put("journal_id", "JJJ");
    conf.put("volume_name", "2008");
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
                                SimulatedContentGenerator.FILE_TYPE_HTML));
//     conf.put("default_article_mime_type", "application/pdf");
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration hindawiAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("download_url", DOWNLOAD_URL);
    conf.put("journal_id", "JJJ");
    conf.put("volume_name", "2008");
    return conf;
  }
  
  String goodDate = "2008/09/24";
  //String dateScheme = "WTN8601";
  String goodTitle = "Advances in Human-Cauliflower Interaction";
  String goodPublisher = "Hippie Publishing Corporation";
  String goodVolume = "2008";
  String goodIssn = "1687-5893";
  String goodDoi = "10.1155/2015/708915";
  String goodAbstract = "This paper presents vegetably enhanced sports entertainment" +
                "applications: AR VPic Presentation System and InterVegetable AR" +
                "Troweling System. We utilize vegetable-based augmented reality for" +
                "getting that immersive feeling.";
  ArrayList<String> goodAuthors = new ArrayList<String>();
  String goodAuthorA = "Author, A.";
  String goodAuthorB = "Author, B.";
  //Unfortunately, it has to be on one line for an accurate representation (and to work)
  String goodContent =

                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +        
                "<meta name=\"citation_author\" content=\""+goodAuthorA+"\"/>" +
                "<meta name=\"citation_author\" content=\""+goodAuthorB+"\"/>" +
                "<meta name=\"citation_volume\" content=\"2008\"/>" +
                "<meta name=\"citation_date\" content=\"2008/09/24\"/>" +
                "<meta name=\"citation_title\" content=\"Advances in Human-Cauliflower Interaction\"/>" +
                "<meta name=\"citation_publisher\" content=\"Hippie Publishing Corporation\"/>" +
                "<meta name=\"citation_title\" content=\"Vegetable Enhancement for Sports Entertainment by Veggie-Based Aubergine Reality\"/>" +
                "<meta name=\"citation_year\" content=\"2008\"/>" +
                "<meta name=\"citation_volume\" content=\"2008\"/>" +
                "<meta name=\"citation_issn\" content=\"1687-5893\"/>" +
                "<meta name=\"citation_doi\" content=\"10.1155/2015/708915\"/>" +
                "<meta name=\"citation_abstract\" content=\"This paper presents vegetably enhanced sports entertainment" +
                "applications: AR VPic Presentation System and InterVegetable AR" +
                "Troweling System. We utilize vegetable-based augmented reality for" +
                "getting that immersive feeling.\"/>" +
                "<meta name=\"citation_fulltext_html_url\" content=\"http://www.humanbean.com/journals/ahci/2008/145363/\"/>" +
                "<meta name=\"citation_pdf_url\" content=\"http://downloads.humanbean.com/journals/ahci/2008/145363.pdf\"/>" +
                "<meta name=\"citation_abstract_html_url\" content=\"http://www.humanbean.com/journals/ahci/2008/145363/abs/\"/>" +
                "<meta name=\"dcterms.issued\" content=\"2008/09/24\"/>" +
                "<meta name=\"dc.Contributor\" content=\""+goodAuthorA+"\"/>" +
                "<meta name=\"dc.Contributor\" content=\""+goodAuthorB+"\"/>" +
               "\n</head>\n" +
                "</html>";


  public void testExtractFromGoodContent() throws Exception {     
    String url = "http://www.example.com/vol1/issue2/art3/";
    
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xhtml+xml");
    goodAuthors.add(goodAuthorA);
    goodAuthors.add(goodAuthorB);
    FileMetadataExtractor me =
      new Hindawi2020HtmlMetadataExtractorFactory.HindawiHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    // for some reason, the following assert did not work, but...
    //assertEquals(md.getList(MetadataField.FIELD_AUTHOR), goodAuthors);
    // changing the compare to .toString()  did work...
    assertEquals(md.getList(MetadataField.FIELD_AUTHOR).toString(), goodAuthors.toString());
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN)); 
  }
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new Hindawi2020HtmlMetadataExtractorFactory.HindawiHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
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
   * Inner class to create a html source code simulated content
   */
  public static class MySimulatedContentGenerator extends       SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
                        
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
                        
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";                     

      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
                    + file_content);

      return file_content;
    }
  }
}