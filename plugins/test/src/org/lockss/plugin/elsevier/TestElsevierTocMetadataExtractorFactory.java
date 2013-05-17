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

package org.lockss.plugin.elsevier;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.elsevier.ElsevierTocMetadataExtractorFactory.ElsevierTocMetadataExtractor;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://i-perception.perceptionweb.com/journal/I/volume/1/article/i0402 
 */
public class TestElsevierTocMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestElsevierTocMetadataExtractorFactory");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;		//Elsevier AU
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.elsevier.ClockssElsevierSourcePlugin";

  private static String BASE_URL = "http://test.com/";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, elsevierFtpAuConfig());
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
    conf.put("year", "2012");
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_XML));
    conf.put("default_article_mime_type", "application/pdf");
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration elsevierFtpAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2012");
    return conf;
  }
  
	String goodVolume = "volume";
	String goodDate = "2011-07-23";
	String goodUrl = "http://test.com/2012/0XFAKE0X/1231231.tar!/3453453/5675675/main.pdf";
	String goodJournal = "Journal";
	String goodDoi = "10.1016/d.rhodes.2011.05.001";
	String goodLanguage = "EN";
	String goodIssue = "Issue";
	String goodRights = "Rights";
	String goodTitle = "This tests a title which spans two lines";
	List<?> goodAuthors = 
	    ListUtil.list("AuthorLongName, A.", "AuthorLongerName, B.",
	  "AuthorEvenLongerName, C.","AuthorWantsALongName, D.",
	  "AuthorHasALongerName, E.", "AuthorHasAnotherLongerName, F.",
	  "AuthorHasATerriblyLongerName, G.");
	String goodSummary = "This summary also spans several lines";
	List<?> goodKeywords = ListUtil.list("testing1", "testing2",  "testing3");
	String goodStart = "10";
	String goodEnd = "20";
 
  //Represents a metadata section for two dummy articles structured as in dataset.toc
  String goodContent =
		  
		  "_t1 Issn\n"+
  "_t3 0XFAKE0X 1231231 3453453 5675675\n"+
  "_ps [PS000]\n"+
  "_ii S0000-0000(00)00000-0\n"+
  "_ii [DOI] 55.5555/d.rhodes.2011.05.010\n"+
  "_ty FLA\n"+
  "_t1 Issn\n"+
  "_pd Date\n"+
  "_jn Journal\n"+
  "_cr Rights\n"+ 
  "_is Issue\n"+
  "_la EN\n"+
  "_ti This tests a title which\n"+
  " spans two lines\n"+
  "_au AuthorLongName, A.\n"+
  "_au AuthorLongerName, B.\n"+
  "_au AuthorEvenLongerName, C.\n"+
  "_au AuthorWantsALongName, D.\n"+
  "_au AuthorHasALongerName, E.\n"+
  "_au AuthorHasAnotherLongerName, F.\n"+
  "_au AuthorHasATerriblyLongerName, G.\n"+
  "_ca AuthorLongName, A.\n"+
  "_ab This summary\n"+
  "	also spans\n"+
  "	several lines\n"+ 
  "_dt 20110723\n"+
  "_la EN\n"+
  "_ii S0000-0000(00)00000-0\n"+
  "_vl volume\n"+
  "_ii [DOI] 10.1016/d.rhodes.2011.05.001\n"+
  "_ty FLA\n"+
  "_li EN\n"+
  "_kw testing1\n"+
  "_kw testing2\n"+
  "_kw testing3\n"+
  "_pg 10-20\n"+
  "_mf [XML JA 5.1.0 ARTICLE] main\n"+
  "_mf [PDF 1.7 6.2 DISTILLED OPTIMIZED BOOKMARKED] main\n"+
  "_mf [Raw ASCII] main\n"+
  "_t3 0XFAKE0X 1231231 3453453 6786786\n"+
  "_ps [PS000]\n"+
  "_dt 201201\n"+
  "_ti This tests a title which\n"+
  " spans two lines\n"+
  "_au Author, A.\n"+
  "_au Author, B.\n"+
  "_au Author, C.\n"+
  "_au Author, D.\n"+
  "_au Author, E.\n"+
  "_ca Author, A.\n"+
  "_ab This summary\n"+
  " also spans\n"+
  " several lines\n"+
  "_la EN\n"+
  "_kw test1\n"+
  "_kw test2\n"+
  "_kw test3\n"+
  "_jn Journal\n"+
  "_pg 20-30\n"+
  "_mf [XML JA 5.1.0 ARTICLE] main\n"+
  "_mf [PDF 1.7 6.2 DISTILLED OPTIMIZED BOOKMARKED] main\n"+
  "_mf [Raw ASCII] main";

  public void testExtractFromGoodContent() throws Exception {
    String url = "0XFAKE0X 1231231 3453453 5675675";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    ElsevierTocMetadataExtractor me =
      new ElsevierTocMetadataExtractorFactory.ElsevierTocMetadataExtractor();
    assertNotNull(me);
    
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodUrl, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodSummary, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodKeywords,  md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodStart, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodRights, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodEnd, md.get(MetadataField.FIELD_END_PAGE));
  }
  
  
  String testContent = 
      "_t1 OXH26350 02726386\n" + 
      "_jn American Journal of Kidney Diseases\n" +
      "_cr Copyright (c) 2011 The National Kidney Foundation, Inc.\n" +
      "_t2 OXH26350 02726386 005901S1\n" +
      "_ps [S300]\n" +
      "_vl 59\n" +
      "_is 1\n" +
      "_pr A1-A8,e1-e420\n" +
      "_cf [name] United States Renal Data System 2011 Annual Data Report: Atlas\n" +
      "    of Chronic Kidney Disease & End-Stage Renal Disease in the United States\n" +
      "_xt Supplement 1\n" +
      "_dt 201201\n" +
      "_t3 OXH26350 02726386 005901S1 11016325\n" +
      "_ps [S300]\n" +
      "_ii S0272-6386(11)01632-5\n" +
      "_ii [DOI] 10.1053/S0272-6386(11)01632-5\n" +
      "_ty MIS\n" +
      "_li EN\n" +
      "_ti Masthead\n" +
      "_pg 6A,8A,10A,12A,14A,16A,18A-19A\n" +
      "_mf [PDF 1.7 6.2 DISTILLED OPTIMIZED] main\n" +
      "_mf [XML JA 5.1.0 SIMPLE-ARTICLE] main\n" +
      "_mf [Raw ASCII] main\n";

  String testVolume = "59";
  String testDate = "2012-01";
  String testUrl = "http://test.com/2012/OXH26350/02726386.tar!/005901S1/11016325/main.pdf";
  String testJournal = "American Journal of Kidney Diseases";
  String testIssn = "0272-6386";
  String testDoi = "10.1053/S0272-6386(11)01632-5";
  String testLanguage = null;
  String testIssue = "1";
  String testRights = "Copyright (c) 2011 The National Kidney Foundation, Inc.";
  String testTitle = "Masthead";
  String testAuthors = null;
  String testSummary = null;
  String testKeywords = null;
  String testStart = "6A";
  String testEnd = "19A";
  String testPublisher = "Elsevier";
  
  public void testExtractFromTestContent() throws Exception {
    String url = "OXH26350 02726386 005901S1 11016325";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(testContent);
    cu.setContentSize(testContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    ElsevierTocMetadataExtractor me =
      new ElsevierTocMetadataExtractorFactory.ElsevierTocMetadataExtractor();
    assertNotNull(me);
    
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(testVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(testDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(testUrl, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(testJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(testIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(testDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(testLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(testAuthors, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(testSummary, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(testKeywords, md.get(MetadataField.FIELD_KEYWORDS));
    assertEquals(testStart, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(testRights, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(testIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(testTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(testEnd, md.get(MetadataField.FIELD_END_PAGE));
    assertEquals(testPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }

  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">\n" +
    "_t2 0XFAKE0X 1231231 3453453 6786786\n"+
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodSummary + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "0XFAKEX0 234234 456456 567567";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new ElsevierTocMetadataExtractorFactory.ElsevierTocMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertEmpty(mdlist);
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
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
			
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
			
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";			

      file_content += getHtmlContent(fileNum, depth, branchNum,	isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
		    + file_content);

      return file_content;
    }
  }
}