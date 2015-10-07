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

package org.lockss.plugin.pion;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://i-perception.perceptionweb.com/journal/I/volume/1/article/i0402 
 */
public class TestPionHtmlMetadataExtractor extends LockssTestCase {
  static Logger log = Logger.getLogger("TestPionMetadataExtractor");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;		//Pion AU
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.pion.ClockssPionIPerceptionPlugin";

  private static String BASE_URL = "http://i-perception.perceptionweb.com/";
  private static String BASE_URL2 = "http://www.perceptionweb.com";
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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, pionAuConfig());
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
//     conf.put("default_article_mime_type", "application/pdf");
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration pionAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("base_url2", BASE_URL2);
    conf.put("volume_name", "1");
    conf.put("journal_code", "I");
    return conf;
  }

  String goodDOI = "10.1068/i0386";
  String goodVolume = "1";
  String goodIssue = "1";
  String goodStartPage = "3";
  String goodISSN = "2041-6695";
  String goodDate = "2010";
  String goodAuthor = "Simons, Daniel J; ";
  String[] goodAuthors = new String[] {"Simons, Daniel J" };
  String goodArticleTitle = "Monkeying around with the gorillas in our midst: familiarity with an inattentional-blindness task does not improve the detection of unexpected events";
  String goodJournalTitle = "i-Perception";
 
  String goodContent =

		"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
		"<html>\n" +
		"<head>\n" +
		"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n"+
	    "<meta name=\"citation_journal_title\" content=\"i-Perception\">\n"+
	    "<meta name=\"citation_authors\" content=\"Simons, Daniel J; \">\n"+
	    "<meta name=\"citation_title\" content=\"Monkeying around with the gorillas in our midst: familiarity with an inattentional-blindness task does not improve the detection of unexpected events\">\n"+
	    "<meta name=\"citation_date\" content=\"2010\">\n"+
	    "<meta name=\"citation_volume\" content=\"1\">\n"+
	    "<meta name=\"citation_issue\" content=\"1\">\n"+
	    "<meta name=\"citation_firstpage\" content=\"3\">\n"+
	    "<meta name=\"citation_lastpage\" content=\"6\">\n"+
	    "<meta name=\"citation_doi\" content=\"10.1068/i0386\">\n"+
	    "<meta name=\"citation_pdf_url\""+
	    "content=\"http://i-perception.perceptionweb.com/fulltext/i01/i0386.pdf\">\n"+
	    "<meta name=\"citation_abstract_html_url\""+
	    "content=\"http://i-perception.perceptionweb.com/journal/I/article/i0386\">\n"+
	    "<!--<meta name=\"citation_fulltext_html_url\""+
	    "content=\"\">  -->\n"+
	    
	    "<meta name=\"dc.Contributor\" content=\"Simons, Daniel J\">\n"+
	    "<meta name=\"dc.Title\" content=\"Monkeying around with the gorillas in our midst: familiarity with an inattentional-blindness task does not improve the detection of unexpected events\">\n"+
	    "<meta name=\"dc.Date\" content=\"2010\">\n"+
	    "<meta name=\"citation_publisher\" content=\"Pion Ltd\">";

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/i123";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
      new PionHtmlMetadataExtractorFactory.PionHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(url, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
  }
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodArticleTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodISSN + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/bad987";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new PionHtmlMetadataExtractorFactory.PionHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(url, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals("10.1068/bad987", md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_PUBLICATION_TITLE));
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
  /*
  String realHtmlFile = "i0552.html";

  public void testFromRealHtmlFile() throws Exception {
    CIProperties risHeader = new CIProperties();
    InputStream file_input = null;
    MockArchivalUnit mau; // source au
    mau = new MockArchivalUnit();
    mau.setConfiguration(pionAuConfig());
    log.info("testing Real RIS File");
    try {
      file_input = getResourceAsStream(realHtmlFile);
      //String string_input = StringUtil.fromInputStream(file_input);
      String string_input = StringUtil.fromReader(new InputStreamReader(file_input, Constants.ENCODING_UTF_8));
 log.info("string: "+string_input);     
      IOUtil.safeClose(file_input);

      String ris_url = BASE_URL + realHtmlFile;
      risHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/ris");
      MockCachedUrl ris_cu = mau.addUrl(ris_url, true, true, risHeader);
      // need to check for this file before emitting
      
      ris_cu.setContent(string_input);
      ris_cu.setContentSize(string_input.length());
      ris_cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/ris");
      
      
      file_input = getResourceAsStream(realHtmlFile);
      string_input = StringUtil.fromReader(new InputStreamReader(file_input, Constants.ENCODING_UTF_8));
      String pdf_url = BASE_URL + realHtmlFile;
      MockCachedUrl pdf_cu = mau.addUrl(pdf_url, true, true, risHeader);
      // need to check for this file before emitting
      
      pdf_cu.setContent(string_input);
      pdf_cu.setContentSize(string_input.length());
      pdf_cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new PionHtmlMetadataExtractorFactory.PionHtmlMetadataExtractor();

      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), ris_cu);
      assertNotEmpty(mdlist);

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
log.info("mdRecord: "+mdRecord.toString());  
log.info("pub title: " + mdRecord.get(MetadataField.FIELD_ARTICLE_TITLE));

      }
    }finally {
      IOUtil.safeClose(file_input);
    }
  }
  */
  
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
