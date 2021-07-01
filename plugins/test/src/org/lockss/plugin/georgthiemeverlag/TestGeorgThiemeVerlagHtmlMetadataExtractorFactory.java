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

package org.lockss.plugin.georgthiemeverlag;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.georgthiemeverlag.GeorgThiemeVerlagHtmlMetadataExtractorFactory.GeorgThiemeVerlagHtmlMetadataExtractor;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947
 */
public class TestGeorgThiemeVerlagHtmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(TestGeorgThiemeVerlagHtmlMetadataExtractorFactory.class);
  
  private SimulatedArchivalUnit sau;  // Simulated AU to generate content
  private static String PLUGIN_NAME = 
      "org.lockss.plugin.georgthiemeverlag.ClockssGeorgThiemeVerlagPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private static final String BASE_URL = "http://www.example.com/";
  private final String JOURNAL_ID = "10.1055/s-00000002";
  private final String VOLUME_NAME = "2010";
  
  //GeorgThiemeVerlag AU
  private ArchivalUnit hau; 
  private MockLockssDaemon theDaemon;

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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, georgthiemeverlagAuConfig());
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
    conf.put(JOURNAL_ID_KEY, JOURNAL_ID);
    conf.put(VOLUME_NAME_KEY, VOLUME_NAME);
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_HTML);
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration georgthiemeverlagAuConfig() {
     Configuration conf = ConfigManager.newConfiguration();

    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(JOURNAL_ID_KEY, JOURNAL_ID);
    conf.put(VOLUME_NAME_KEY, VOLUME_NAME);
    return conf;
  }

  /*
   * Test utility functions
   */
  public void testNormalizeIdValue() throws Exception {

	  	//issn
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("1111-2222"),"11112222"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("  1111-2222"),"11112222"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("1111-2222  "),"11112222"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("11112222"),"11112222"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("eissn: 1111-2222"),"11112222"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("eissN:    1111-2222"),"11112222"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("issN:    1111-2222"),"11112222"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id(" issN:    1111-2222"),"11112222"); 
	    //isbn       
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("978-0-89118-196-5"),"9780891181965"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("  978-0-89118-196-5"),"9780891181965"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("9-7-8-0-89118-196-5"),"9780891181965"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("978-0-89118-196-5   "),"9780891181965"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("9780891181965"),"9780891181965"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("isbn: 978-0-89118-196-5 "),"9780891181965"); 
	    assertEquals(GeorgThiemeVerlagHtmlMetadataExtractor.normalize_id("EISBn:   978-0-89118-196-5  "),"9780891181965"); 

}  
  
  
  String goodDate = "2010/08/25";
  String goodSubject = "Subj";
  String goodArticle = "Some Article Title";
  String goodAuthor = "O. A. Auth";
  String goodDoi = "10.1055/s-0029-1215009";
  String goodVol = "6";
  String journalTitle = "Aktuelle Dermatologie";
  String goodPublisher = "Georg Thieme Verlag KG";

  String goodContent =
      " <html xmlns:i18n=\"http://apache.org/cocoon/i18n/2.1\" class=\"no-js\" lang=\"en\">\n" + 
      "    <head><META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" + 
      "<meta node=\"phvmthieme04\">\n" + 
      "<meta charset=\"utf-8\">\n" + 
      "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">\n" + 
      "<title>Thieme E-Journals - Arb Title</title>\n" + 
      "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=8\">\n" + 
      "<meta name=\"viewport\" content=\"\">\n" + 
      "<meta name=\"format-detection\" content=\"telephone=no\">\n" + 
      "  <meta name=\"prism.publicationName\" content=\"Aktuelle Dermatologie\"/>\n" + 
      "  <meta name=\"prism.issn\" content=\"0340-2541\"/>\n" + 
      "  <meta name=\"prism.eissn\" content=\"1438-938X\"/>\n" + 
      "  <meta name=\"prism.copyright\" content=\"Georg Thieme Verlag\"/>\n" + 
      "  <meta name=\"prism.doi\" content=\"10.1055/s-0029-1215009\"/>\n" + 
      "  <meta name=\"prism.publicationDate\" content=\"2010/08/25\"/>\n" + 
      "  <meta name=\"prism.volume\" content=\"6\"/>\n" + 
      "  <meta name=\"prism.issueIdentifier\" content=\"3\"/>\n" + 
      "  <meta name=\"prism.startingPage\" content=\"1\"/>\n" + 
      "  <meta name=\"prism.endingPage\" content=\"3\"/>\n" + 
      "  <meta name=\"citation_language\" content=\"de\"/>\n" + 
      "  <meta name=\"citation_journal_title\" content=\"" + journalTitle + "\"/>\n" + 
      "  <meta name=\"citation_issn\" content=\"0340-2541\"/>\n" + 
      "  <meta name=\"citation_publisher\" content=\"Georg Thieme Verlag\"/>\n" + 
      "  <meta name=\"citation_doi\" content=\"10.1055/s-0029-1215009\"/>\n" + 
      "  <meta name=\"citation_title\" content=\"Some Article Title\"/>\n" + 
      "  <meta name=\"citation_author\" content=\"O. A. Auth\"/>\n" + 
      "  <meta name=\"citation_online_date\" content=\"2009/08/25\"/>\n" + 
      "  <meta name=\"citation_publication_date\" content=\"2010/08/25\"/>\n" + 
      "  <meta name=\"citation_volume\" content=\"6\"/>\n" + 
      "  <meta name=\"citation_issue\" content=\"3\"/>\n" + 
      "  <meta name=\"citation_firstpage\" content=\"1\"/>\n" + 
      "  <meta name=\"citation_lastpage\" content=\"3\"/>\n" + 
      "  <meta name=\"DC.language\" content=\"de\"/>\n" + 
      "  <meta name=\"DC.rights\" content=\"Georg Thieme Verlag\"/>\n" + 
      "  <meta name=\"DC.publisher\" content=\"Georg Thieme Verlag\"/>\n" + 
      "  <meta name=\"DC.identifier\" content=\"10.1055/s-0029-1215009\"/>\n" + 
      "  <meta name=\"DC.subject\" content=\"Subj\"/>\n" + 
      "  <meta name=\"DC.title\" content=\"Some Arb Title\"/>\n" + 
      "  <meta name=\"DC.creator\" content=\"Auth, One A.\"/>\n" + 
      "  <meta name=\"DC.date\" content=\"2009/08/25\"/>\n" + 
      "  <meta name=\"DC.format\" content=\"XML\"/>\n" + 
      "  <meta name=\"DC.relation\" content=\"https://www.thieme-connect.com/ejournals/html/10.1055/s-0029-1215009\"/>\n" + 
      "  <meta name=\"DC.format\" content=\"PDF\"/>\n" + 
      "  <meta name=\"DC.relation\" content=\"https://www.thieme-connect.com/ejournals/pdf/10.1055/s-0029-1215009.pdf\"/>\n" + 
      "  <link rel=\"stylesheet\" href=\"/css/style.css\">\n" + 
      "</head>\n" + 
      "" +
      "</html>";

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/ejournals/abstract/10.1055/s-0029-1214947";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = 
        new GeorgThiemeVerlagHtmlMetadataExtractorFactory.
            GeorgThiemeVerlagHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodVol, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(journalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    // the following value now hard-coded
    // assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    
    // the following does not exist in test data
    assertNull(md.get(MetadataField.FIELD_END_PAGE));
  }

  String badContent = 
      "<HTML><HEAD><TITLE>" + journalTitle + 
      "</TITLE>\n" + "<meta name=\"foo\" content=\"bar\">\n</HEAD><BODY>" + 
      "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " + 
      " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me = 
        new GeorgThiemeVerlagHtmlMetadataExtractorFactory.
            GeorgThiemeVerlagHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    
    assertEquals(1, md.rawSize());
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