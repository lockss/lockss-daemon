/*
 * $Id$
 */
/*

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

package org.lockss.plugin.clockss.discreteanalysis;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestDiscreteAnalysisSourceXmlMetadataExtractor extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestDiscreteAnalysisSourceXmlMetadataExtractor.class);
  
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;
  
  // PLUGIN_NAME = "org.lockss.plugin.clockss.discreteanalysis.ClockssDiscreteAnalysisSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  
  /*
   * Set up the metadata expected for each of the above tests
   */
  private static final String pdfUrl1 = "http://www.source.com/10001.pdf";
  
  private static CIProperties xmlHeader = new CIProperties();
  private static String xml_url = "http://www.source.com/10001.xml";
  private MockCachedUrl mcu;
  private FileMetadataExtractor me;
  private FileMetadataListExtractor mle;
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...
    
    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();
    
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau.setConfiguration(auConfig());
    
    // the following is consistent across all tests; only content changes
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    mau.addUrl(pdfUrl1, true, true, xmlHeader);
    
    me = new DiscreteAnalysisSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
    mle = new FileMetadataListExtractor(me);
    
  }
  
  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2016");
    return conf;
  }
  
  private static final String realXMLFile = "DASourceTest.xml";
  
  private static final String GOOD_TITLE =  "Advanced Stuff for Future Wizards, Gremlins, and Mathists";
  private static final String GOOD_JTITLE = "Discrete Analysis";
  private static final String GOOD_DATE =   "2016";
  private static final String GOOD_AUTHOR = "Arthur Somebody"; //, Ryan Nobody, Li Ping, Yuan Lau";
//  private static final String GOOD_PUBLISHER = "National Academies Press"; 
  private static final String GOOD_EISSN =  "1239-3219";
  
  //TODO - 
  // add a test to check the splitting of an author
  // add a test to check title: subtitle permutations
  
  public void testFromDAXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);
      
      // set up the content for this test
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(1, mdlist.size());
      ArticleMetadata mdRecord = mdlist.get(0);
      assertNotNull(mdRecord);
      
      assertEquals(GOOD_JTITLE, mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GOOD_TITLE, mdRecord.get(MetadataField.FIELD_ARTICLE_TITLE));
      
      assertEquals(GOOD_DATE, mdRecord.get(MetadataField.FIELD_DATE));
      assertEquals(GOOD_AUTHOR,mdRecord.get(MetadataField.FIELD_AUTHOR));
      assertEquals(GOOD_EISSN,mdRecord.get(MetadataField.FIELD_EISSN));
    }finally {
      IOUtil.safeClose(file_input);
    }
    
  }
  
  /*
   * XML snippets which can be built up for specific cases
   */
  private static final String da_xml_start = 
      "<journal_article>\n" +
          "<source>Diamond Open Access Science</source>\n" +
          "<journal_title>Discrete Analysis</journal_title>\n";
  
  private static final String da_invalid_eissn =
      "<eissn>NI000909</eissn>\n";
  
  private static final String da_item_contents =
      "<year>2016</year>\n" +
          "<title>Advanced Stuff for Future Wizards, Gremlins, and Mathists</title>\n" +
          "<language>en</language>\n" +
          "<author>\n" +
          "  <given_name>Arthur</given_name>\n" +
          "  <surname>Somebody</surname>\n" +
          "  <institution>University of California at Hogwarts, Organized by the National Research Council and the Office of the Dark Arts</institution>\n" +
          "</author>\n" +
          "<first_page>1</first_page>\n" +
          "<last_page>20</last_page>\n" +
          "<doi>10.19086/da.nodoi</doi>\n" +
          "<abstract>Dark art for art's sake</abstract>\n" +
          "<msc>03D15</msc>";
  
  private static final String da_xml_end =
      "</journal_article>";
  
  public void testNoValidEISSN() throws Exception {
    
    String xml_string = da_xml_start + da_invalid_eissn + da_item_contents + da_xml_end;
    mcu.setContent(xml_string);
    mcu.setContentSize(xml_string.length());
    
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    assertEquals(1, mdlist.size());
    ArticleMetadata mdRecord = mdlist.get(0);
    assertNotNull(mdRecord);
    assertEquals(null, mdRecord.get(MetadataField.FIELD_EISSN));
  }
  
}
