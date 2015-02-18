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

package org.lockss.plugin.clockss.jats;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.onixbooks.Onix3LongSourceXmlMetadataExtractorFactory;


public class TestJatsPublishingXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestJatsPublishingXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.jats.ClockssJatsPublishingSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";

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
    conf.put("year", "2014");
    return conf;
  }
  

 // An XML snippet to test a specific issue as needed.
 private static final String XMLsnippet = 
     "";
 /*
 public void testFromXMLSnippet() throws Exception {
   

     CIProperties xmlHeader = new CIProperties();    
     String xml_url = "http://www.source.com/TestXML.xml";
     xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
     MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
     mau.addUrl("http://www.source.com/TestXML.pdf", true, true, xmlHeader);
     
     mcu.setContent(XMLsnippet);
     mcu.setContentSize(XMLsnippet.length());
     mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

   FileMetadataExtractor me = new JatsPublishingSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
     FileMetadataListExtractor mle =
         new FileMetadataListExtractor(me);
     List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);

     // There should be one record from the snippet
     assertNotEmpty(mdlist);
     assertEquals(1, mdlist.size());

     // Pick up the one record
     Iterator<ArticleMetadata> mdIt = mdlist.iterator();
     ArticleMetadata mdRecord = null;
     mdRecord = (ArticleMetadata) mdIt.next();
     
     //Check snippet item you want to verify
     assertEquals(mdRecord.get(MetadataField.FIELD_JOURNAL_TITLE), "The Publication Title");
 }
 */

 private static final String realXMLFile = "JatsPublishingSourceTest.xml";

  public void testFromJatsPublishingXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();    
      String xml_url = "http://www.source.com/TestXML.xml";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      // Now add all the pdf files in our AU since we check for them before emitting
      mau.addUrl(pdfUrl1, true, true, xmlHeader);
      
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

    FileMetadataExtractor me = new  JatsPublishingSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(1, mdlist.size());

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        compareMetadata(mdRecord);
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }
 
  private static final int ISSN_INDEX = 0;
  private static final int DOI_INDEX = 1;
  private static final int AUTHOR_INDEX = 2;
  private static final int JOURNAL_TITLE = 3;
  private static final int ARTICLE_DATE = 4;
  private static final int ARTICLE_TITLE = 5;
  private static final int VOLUME = 6;
  private static final int ISSUE = 7;
  private static final int SPAGE = 8;
  private static final int EPAGE = 9;


  
  private static final String pdfUrl1 = "http://www.source.com/TestXML.pdf";
  private static final ArrayList md1 = (ArrayList) ListUtil.list(
      "0077-9999",
      "10.1111/xjid/172/test",
      "[Foo, Firstie]",
      "Excellent Journal - Smart: With a Subtitle",
      "1998-01-05",
      "Single Title of Article across Multiple lines",
      "172",
      "2-3",
      "309",
      "323");

  // Find the matching expected data by matching against ISSN
  static private final Map<String, List> expectedMD =
      new HashMap<String,List>();
  static {
    expectedMD.put("0077-9999", md1);
  }
  
  private void compareMetadata(ArticleMetadata AM) {
    String issn = AM.get(MetadataField.FIELD_ISSN);
    ArrayList expected = (ArrayList) expectedMD.get(issn);
    
    assertNotNull(expected);
    assertEquals(expected.get(DOI_INDEX), AM.get(MetadataField.FIELD_DOI));
    //assertEquals(expected.get(AUTHOR_INDEX), AM.getList(MetadataField.FIELD_AUTHOR).toString());
    assertEquals(expected.get(JOURNAL_TITLE), AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
    //assertEquals(expected.get(ARTICLE_DATE), AM.get(MetadataField.FIELD_DATE));
    assertEquals(expected.get(ARTICLE_TITLE), AM.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(expected.get(VOLUME), AM.get(MetadataField.FIELD_VOLUME));
    assertEquals(expected.get(ISSUE), AM.get(MetadataField.FIELD_ISSUE));
    assertEquals(expected.get(SPAGE), AM.get(MetadataField.FIELD_START_PAGE));
    assertEquals(expected.get(EPAGE), AM.get(MetadataField.FIELD_END_PAGE));
    
  }
}
