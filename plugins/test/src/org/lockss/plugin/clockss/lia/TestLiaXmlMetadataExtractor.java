/*
 * $Id:$
 */
/*

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

package org.lockss.plugin.clockss.lia;
import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestLiaXmlMetadataExtractor extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestLiaXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.lia.ClockssLIAJatsSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static final String xml_url = BASE_URL + "JLA/v26/i4/041501_1/Markup/VOR_10.2351_1.4893749.xml";
  private static final String pdf_url = BASE_URL + "JLA/v26/i4/041501_1/Page_Renditions/online.pdf";

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

  private static final String PUB_DATE =  "<pub-date pub-type=\"ppub\"><month>11</month><year>2014</year></pub-date>";
  private static final String PERMISSIONS_GROUP =
      "<permissions>" +
          "<copyright-year>2014</copyright-year>" +
          "<copyright-holder>Laser Institute of America</copyright-holder>" +
          "<license license-type=\"ccc\">" +
          "<license-p>1938-xxxx/2014/26(4)/041501/6/<price>$28.00</price></license-p>" +
          "</license>" +
          "</permissions>";

  // An XML snippet to test a specific issue as needed.
  private static final String XMLsnippet_noCopyDate = 
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
          "<!DOCTYPE article" +
          "  PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.0 20120330//EN\" \"JATS-archivearticle1.dtd\">" +
          "<article><front><journal-meta>" +
          "<journal-id journal-id-type=\"coden\">JLAPEN</journal-id>" +
          "<journal-title-group>" +
          "<journal-title>Journal of Laser Applications</journal-title>" +
          "<abbrev-journal-title>J. Laser Appl.</abbrev-journal-title></journal-title-group>" +
          "<issn pub-type=\"epub\">1111-0000</issn>" +
          "<publisher>" +
          "<publisher-name>Laser Institute of America</publisher-name>" +
          "<publisher-name specific-use=\"short-name\">LIA</publisher-name>" +
          "</publisher>" +
          "</journal-meta>" +
          "<article-meta>" +
          "<article-id pub-id-type=\"doi\">10.1111/1.23456</article-id>" +
          "<title-group>" +
          "<article-title>Laser article title</article-title>" +
          "</title-group>" +
          PUB_DATE +
          "<volume>26</volume><issue>4</issue><elocation-id seq=\"1\">041501</elocation-id>" +
          "<counts>" +
          "<page-count count=\"6\"/>" +
          "</counts>" +
          "<custom-meta-group><custom-meta><meta-name>crossmark</meta-name><meta-value/></custom-meta></custom-meta-group></article-meta>" +
          "</front>" +
          "<body>" +
          "</body>" +
          "</article>";

  public void testFromXMLSnippet_noCopyDate() throws Exception {


    CIProperties xmlHeader = new CIProperties();    
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mau.addUrl(pdf_url, true, true, xmlHeader);

    mcu.setContent(XMLsnippet_noCopyDate);
    mcu.setContentSize(XMLsnippet_noCopyDate.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

    FileMetadataExtractor me = new LiaJatsXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
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
    assertEquals("2014", mdRecord.get(MetadataField.FIELD_DATE));
  }
  
  private static final String realXMLFile = "LiaJatsSourceTest.xml";

  public void testFromJatsPublishingXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();    
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      // Now add all the pdf files in our AU since we check for them before emitting
      mau.addUrl(pdf_url, true, true, xmlHeader);

      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      FileMetadataExtractor me = new  LiaJatsXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
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
        log.debug3(mdRecord.ppString(2));
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



  private static final ArrayList md1 = (ArrayList) ListUtil.list(
      "1111-0000",
      "10.1111/1.23456",
      "[Foo, B., Barr, D. J., Pixel, M., Widget, J. A.]",
      "Journal of Laser Applications",
      "2014",
      "Laser article title",
      "26",
      "4",
      null,
      null);

  // Find the matching expected data by matching against ISSN
  static private final Map<String, List> expectedMD =
      new HashMap<String,List>();
  static {
    expectedMD.put("1111-0000", md1);
  }

  private void compareMetadata(ArticleMetadata AM) {
    String issn = AM.get(MetadataField.FIELD_ISSN);
    ArrayList expected = (ArrayList) expectedMD.get(issn);

    assertNotNull(expected);
    assertEquals(expected.get(DOI_INDEX), AM.get(MetadataField.FIELD_DOI));
    assertEquals(expected.get(AUTHOR_INDEX), AM.getList(MetadataField.FIELD_AUTHOR).toString());
    assertEquals(expected.get(JOURNAL_TITLE), AM.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(expected.get(ARTICLE_DATE), AM.get(MetadataField.FIELD_DATE));
    assertEquals(expected.get(ARTICLE_TITLE), AM.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(expected.get(VOLUME), AM.get(MetadataField.FIELD_VOLUME));
    assertEquals(expected.get(ISSUE), AM.get(MetadataField.FIELD_ISSUE));
    assertEquals(expected.get(SPAGE), AM.get(MetadataField.FIELD_START_PAGE));
    assertEquals(expected.get(EPAGE), AM.get(MetadataField.FIELD_END_PAGE));

  }
}
