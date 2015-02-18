/*
 * $Id$ 
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.autobiography;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the xml source for this plugin is:
 * http://www.springerlink.com/content/978-3-642-14308-3 
 */
public class TestAutoBiographyMetadataExtractorFactory extends LockssTestCase {
  
  private static Logger log = Logger.getLogger(TestAutoBiographyMetadataExtractorFactory.class);

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
      "org.lockss.plugin.autobiography.ClockssAutoBiographyPlugin";

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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, autobiographyAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", "http://www.example.com/");
    conf.put("base_url2", "http://www.example2.com/");
    conf.put("journal_id", "jrnl");
    conf.put("volume_name", "15");
    conf.put("year", "2000");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (
    			SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML +
				SimulatedContentGenerator.FILE_TYPE_XML +
				SimulatedContentGenerator.FILE_TYPE_TXT));
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration autobiographyAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("base_url2", "http://www.example2.com/");
    conf.put("volume_name", "12");
    conf.put("journal_id", "jrnl");
    conf.put("year", "2000");
    return conf;
  }
  
  String goodAuthors = "[Author, John A., Author, John B., Author, John C.]";
  String goodVolume = "Volume";
  String goodJournal = "Journal Title";
  String goodISSN = "5555-5555";
  String goodDate = "07/2006";
  String goodIssue = "Issue";
  String goodPublisher = "Publisher";
  String goodDoi = "10.5555/00000";
  String goodTitle = "Article Title";
  String goodStart = "Start";
  String goodEnd = "End";
  
  String goodContent = 
		  "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"+
		  "<!DOCTYPE Fake SYSTEM \"Fake.dtd\">"+
		  "<SAGEmeta type=\"article\" doi=\"10.5555/00000\">"+
		  "<header>"+
		  "<jrn_info>"+
		  "<jrn_title>Journal Title</jrn_title>"+
		  "<ISSN>5555-5555</ISSN>"+
		  "<vol>Volume</vol>"+
		  "<iss>Issue</iss>"+
		  "<date><yy>2006</yy><mm>07</mm></date>"+
		  "<pub_info>"+
		  "<pub_name>Publisher</pub_name>"+
		  "<pub_location>Publisher Location</pub_location>"+
		  "</pub_info>"+
		  "</jrn_info>"+
		  "<art_info>"+
		  "<art_title>Article\nTitle"+
		  "</art_title>"+
		  "<art_stitle>Article Subtitle</art_stitle>"+
		  "<art_author>"+
		  "<per_aut><fn>John</fn><mn>A.</mn><ln>Author</ln><affil>University</affil></per_aut>"+
		  "<per_aut><fn>John</fn><mn>B.</mn><ln>Author</ln><affil>University</affil></per_aut>"+
		  "<per_aut><fn>John</fn><mn>C.</mn><ln>Author</ln><affil>University</affil></per_aut>"+
		  "</art_author>"+
		  "<spn>Start</spn>"+
		  "<epn>End</epn>"+
		  "<descriptors></descriptors>"+
		  "</art_info>"+
		  "</header>"+
		  "</SAGEmeta>";


  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    FileMetadataExtractor me =
      new AutoBiographyMetadataExtractorFactory.AutoBiographySageXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR).toString());
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodStart, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEnd, md.get(MetadataField.FIELD_END_PAGE));
  }
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodAuthors + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new AutoBiographyMetadataExtractorFactory.AutoBiographySageXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
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