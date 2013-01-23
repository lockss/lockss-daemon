/* $Id: TestAIPJatsMetadataExtractorFactory.java,v 1.1 2013-01-23 23:07:39 ldoan Exp $

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

package org.lockss.plugin.aipjats;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * One of the articles used to get the JATS source for this plugin is:
 * Files used to write this class constructed from AIP JATS Source FTP archive:
 * ./074101_1-testnobodyback.xml from
 * http://clockss-ingest.lockss.org/sourcefiles/aip-dev/2012/
 */

public class TestAIPJatsMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger(TestAIPJatsMetadataExtractorFactory.class);

  private ArchivalUnit aau;		// AIPJats AU
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.aipjats.AIPJatsPlugin";

  private static String BASE_URL = "http://www.example.com";
  private final String GOOD_CONTENT_FILE_NAME = "test_jats_junit.xml"; // xml from aip
  //private final String GOOD_CONTENT_FILE_NAME = "test_jats_junit_reformatted.xml";  //eclipse formated;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    aau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, aipAuConfig());
  }

  public void tearDown() throws Exception {
    super.tearDown();
    theDaemon.stopDaemon();
  }

  // Set AU config for AIP JATS source
  Configuration aipAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2012");
    return conf;
  }
  
  String goodJournalTitle = "The Bogus Journal";
  ArrayList<String> goodPublisherNames = new ArrayList<String>();
  ArrayList<String> goodContributors = new ArrayList<String>(); // authors
  String goodVolume = "1";
  String goodPubDate = "2013-03-33T03:33:33";
  String goodIssn = "0022-0066"; // ppub
  String goodIssue = "1";
  String goodDoi = "10.1000/1.33333";
    
  // test with aip xml
  //String goodArticleTitle = "Unraveling rotation-vibration " +
  //		"mixing in highly fluxional molecules using diffusion " +
  //		"Monte Carlo: Applications to H3+ and H3O+";
      
  String goodArticleTitle = "Always bear in mind that your own resolution " +
      "to succeed is more important than any other. Abraham Lincoln.";

  String goodInputContent = getInputFile(GOOD_CONTENT_FILE_NAME);
  
  // Get test input file test_jats_junit.xml from current directory.
  private String getInputFile(String filename) {
  
    String jatsStr;
    try {
      InputStream jatsIn = getClass().getResourceAsStream(filename);
      jatsStr = StringUtil.fromInputStream(jatsIn);
    }
    catch (IOException e) {
       throw new RuntimeException(e);
    }
    
    return (jatsStr);
        
  } // getInputFile
  

  public void testExtractFromGoodContent() throws Exception {
    
    goodPublisherNames.add("Coocoo Institute");
    goodPublisherNames.add("CI"); // short-name
    
    // Authors
    goodContributors.add("Smith, John T.");
    goodContributors.add("McArthur, Helen M.");
    goodContributors.add("London, James C.");
    
    String url = "http://www.example.com/";
    MockCachedUrl cu = new MockCachedUrl(url, aau);
    cu.setContent(goodInputContent);
    cu.setContentSize(goodInputContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    
    FileMetadataExtractor me =
        new AIPJatsMetadataExtractorFactory.AIPJatsMetadataExtractor();
    assertNotNull(me);
    
    log.debug3("Extractor: " + me.toString());
    
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodContributors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodPubDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    
    // Note: this test for article title passes using the test sample file
    // from AIP.  If this file is reformatted in Eclipse, it does not pass
    // since Eclipse adds newlines to the xml, and node.getTextContent()
    // concatenates all lines together with 1 space delimited.
    assertEquals(goodArticleTitle,
                 md.get(MetadataField.FIELD_ARTICLE_TITLE).replaceAll("\\n","").replaceAll("\\s+", " "));
    
  } // testExtractFromGoodContent
  
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodJournalTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    " </div>\n";


  public void testExtractFromBadContent() throws Exception {
    
    String url = "http://www.example.com/APPLAB/vol96/iss_1/";
    MockCachedUrl cu = new MockCachedUrl(url, aau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    
    FileMetadataExtractor me =
      new AIPJatsMetadataExtractorFactory.AIPJatsMetadataExtractor();
    assertNotNull(me);
    
    log.info("Extractor: " + me.toString());
    
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    
  } // testExtractFromBadContent
  
} // TestAIPSourceJatsMetadataExtractorFactory
