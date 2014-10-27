/* $Id: TestWoltersKluwerSourceXmlMetadataExtractorHelper.java,v 1.4 2014-10-27 19:31:09 aishizaki Exp $

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

package org.lockss.plugin.clockss.wolterskluwer;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.apache.commons.io.input.BOMInputStream;

//import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;

/*
 * Test file used to extract metadata:
 * <base_url>2013/JAP/v114/i18/183704_1/Markup/VOR_10.1063_1.4829703.xml
 *      basic examples to test metadata extraction
 */

public class TestWoltersKluwerSourceXmlMetadataExtractorHelper
  extends LockssTestCase {
  
  static Logger log = 
      Logger.getLogger(TestWoltersKluwerSourceXmlMetadataExtractorHelper.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau; 

  private static final String YEAR = "2013";

  private static final String BASE_URL = 
      "http://content5.lockss.org/sourcefiles/wolterskluwer-released/"+ YEAR + "/";
  
  private static final String BASIC_XML_FILENAME = "20140615.0";
  private static final String ACCESS_URL = "0125480-201406150-00002";
  private static final String BASIC_PDF_FILENAME = ACCESS_URL+".pdf";
   
  private static final String TEST_XML_URL = BASE_URL + "ADAPA20140615.0/" + BASIC_XML_FILENAME;
  // need to add a "0" to precede the BASIC_PDF_FILENAME... GRRR
  private static final String TEST_PDF_URL = BASE_URL + "ADAPA20140615.0/" + "0"+BASIC_PDF_FILENAME;

  // expected metadata
  private static final String GOOD_JOURNAL_TITLE = "Hello Oligarchy: With Songs & Dances";
  private static final String GOOD_PUB_YEAR = "2014";
  private static final String GOOD_PUB_MONTH = "June";
  private static final String GOOD_PUB_DAY = "15";
  private static final String GOOD_PUB_DATE = "2014-06";
  //private static final String GOOD_PUB_DATE = "2014-06-15";
  private static final String GOOD_DOI = "10.1097/01.CNE.0000428768.49988.91";
  private static final String GOOD_ISSN = "0163-2108";
  private static final String GOOD_ISSUE = "8";
  private static final String GOOD_VOLUME = "24";
  private static final String GOOD_ARTICLE_TITLE = "Special Topic";
  private static final String GOOD_ARTICLE_SUBTITLE = "An Intro to Plutocrats";
  private static final String GOOD_TITLE = GOOD_ARTICLE_TITLE + ":" + GOOD_ARTICLE_SUBTITLE;
  private static final String A1_FN = "Anna";
  private static final String A1_MN = "B.";
  private static final String A1_SN = "Cross";
  private static final String A2_FN = "Doug";
  private static final String A2_SN = "Evans";

  private static ArrayList goodAuthors = (ArrayList) ListUtil.list(
      A1_SN + ", " + A1_FN + " " + A1_MN,
      A2_SN + ", " + A2_FN);
   
 
  private static final String BASIC_CONTENT =
    "<!DOCTYPE dg SYSTEM \"ovidbase.dtd\">" +
    "<DG><COVER NAME=\"G0256406-201406150-00000\">" +
    "<D V=\"2009.2F\" AN=\""+ACCESS_URL+"\" FILE=\"G0256406-201406150-00001\" CME=\"CME\">" +
    "<BB>" +
    "<TG>" +
    "<TI>" + GOOD_ARTICLE_TITLE + "</TI>" +
    "<STI>" + GOOD_ARTICLE_SUBTITLE + "</STI></TG>" +
    "<BY>" +
    "<PN><FN>"+A1_FN+"</FN><MN>"+A1_MN+"</MN><SN>"+A1_SN+"</SN><DEG>MD</DEG></PN>" +
    "<PN><FN>"+A2_FN+"</FN><SN>"+A2_SN+"</SN><DEG>MD, MSc</DEG></PN>" +
    "</BY>" +
    "<SO>" +
    "<PB>"+GOOD_JOURNAL_TITLE+"</PB>" +
    "<ISN>"+GOOD_ISSN+"</ISN>" +
    "<DA>"+"<MO>"+GOOD_PUB_MONTH+"</MO><YR>"+GOOD_PUB_YEAR+"</YR></DA>" +
//    "<DA><DY>"+GOOD_PUB_DAY+"</DY><MO>"+GOOD_PUB_MONTH+"</MO><YR>"+GOOD_PUB_YEAR+"</YR></DA>" +
    "<V>"+GOOD_VOLUME+"</V>" +
    "<IS><IP>"+GOOD_ISSUE+"</IP></IS>" +
    "<PG>1&ndash;6</PG></SO>" +
    "<CP>&copy; 2014 Lippincott Williams &amp; Wilkins.</CP>" +
    "<DT>Article</DT><XUI XDB=\"pub-doi\" UI=\""+GOOD_DOI+"\"></BB>" +
    "</D></DG>";
  
  private static final String EMPTY_CONTENT =
    "<!DOCTYPE dg SYSTEM \"ovidbase.dtd\">"+
    "<DG>"+
    "</DG>";
  // Try some disallowed content: 
  // Bad date: date without a year
  private static final String BAD_PUB_DAY = "77";
  private static final String BAD_PUB_MONTH = "Juneteenth";
  private static final String BAD_PUB_YEAR = "";
  // Bad authors name combos:
  private static final String BAD1_FN = "Firstname";
  private static final String BAD1_MN = "M";
  private static final String BAD1_SN = "LastName";
  private static final String BAD2_FN = "Cher";
  private static final String BAD2_SN = "";
  // bad DOI (whichshould be corrected)
  private static final String BAD_DOI = "  DOI:  "+GOOD_DOI;
  //private static final String BAD_DOI = "  ";
  // Bad Title: only subtitle
  private static final String BAD_ARTICLE_SUBTITLE = "Just a Subtitle";
  private static final String BAD_ARTICLE_TITLE = ":" +BAD_ARTICLE_SUBTITLE;
  private static ArrayList badAuthors = (ArrayList) ListUtil.list(
      BAD1_SN + ", " + BAD1_FN + " " + BAD1_MN,
      ", " + BAD2_FN);
  
  private static final String BAD_CONTENT =
    "<!DOCTYPE dg SYSTEM \"ovidbase.dtd\">" +
    "<DG><COVER NAME=\"G0256406-201406150-00000\">" +
    "<D V=\"2009.2F\" AN=\""+ACCESS_URL+"\" FILE=\"G0256406-201406150-00001\" CME=\"CME\">" +
    "<BB>" +
    "<TG>" +
    "<TI></TI>" +
    "<STI>" + BAD_ARTICLE_SUBTITLE + "</STI></TG>" +
    "<BY>" +
    "<PN><FN>"+BAD1_FN+"</FN><MN>"+BAD1_MN+"</MN><SN>"+BAD1_SN+"</SN><DEG>MD</DEG></PN>" +
    "<PN><FN>"+BAD2_FN+"</FN><SN>"+BAD2_SN+"</SN><DEG>MD, MSc</DEG></PN>" +
    "</BY>" +
    "<SO>" +
    "<PB>"+GOOD_JOURNAL_TITLE+"</PB>" +
    "<ISN>"+GOOD_ISSN+"</ISN>" +
    "<DA><DY>"+BAD_PUB_DAY+"</DY><MO>"+BAD_PUB_MONTH+"</MO><YR>"+BAD_PUB_YEAR+"</YR></DA>" +
    "<V>"+GOOD_VOLUME+"</V>" +
    "<IS><IP>"+GOOD_ISSUE+"</IP></IS>" +
    "<PG>1&ndash;6</PG></SO>" +
    "<CP>&copy; 2014 Lippincott Williams &amp; Wilkins.</CP>" +
    "<DT>Article</DT><XUI XDB=\"pub-doi\" UI=\""+BAD_DOI+"\"></BB>" +
    "</D>" +
    // 
    "<D V=\"2009.2F\" AN=\""+ACCESS_URL+"\" FILE=\"G0256406-201406150-00002\" CME=\"CME\">" +
    "<BB>" +
    "<TG>" +
    "<TI></TI>" +
    "<STI>" + BAD_ARTICLE_SUBTITLE + "1"+ "</STI></TG>" +
    "<BY>" +
    "<PN><FN>"+BAD1_FN+"</FN><MN>"+BAD1_MN+"</MN><SN>"+BAD1_SN+"</SN><DEG>MD</DEG></PN>" +
    "<PN><FN>"+BAD2_FN+"</FN><SN>"+BAD2_SN+"</SN><DEG>MD, MSc</DEG></PN>" +
    "</BY>" +
    "<SO>" +
    "<PB>"+GOOD_JOURNAL_TITLE+"1"+"</PB>" +
    "<ISN>"+GOOD_ISSN+"1"+"</ISN>" +
    "<DA><DY>"+BAD_PUB_DAY+"</DY><MO>"+BAD_PUB_MONTH+"</MO><YR>"+BAD_PUB_YEAR+"</YR></DA>" +
    "<V>"+GOOD_VOLUME+"</V>" +
    "<IS><IP>"+GOOD_ISSUE+"</IP></IS>" +
    "<PG>1&ndash;6</PG></SO>" +
    "<CP>&copy; 2014 Lippincott Williams &amp; Wilkins.</CP>" +
    "<DT>Article</DT><XUI XDB=\"pub-doi\" UI=\" \"></BB>" +
    "</D>" +
    "</DG>";


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
    conf.put("year", YEAR);
    return conf;
  }
  
  public TestWoltersKluwerSourceXmlMetadataExtractorHelper() throws Exception {
    super.setUp();

    setUp();
  }

  public void testExtractFromEmptyContent() throws Exception {
    String xml_url = TEST_XML_URL;
    String pdf_url = TEST_PDF_URL;

    CIProperties xmlHeader = new CIProperties();

    MockCachedUrl xml_cu = mau.addUrl(xml_url, true, true, xmlHeader);
    xml_cu.setContent(EMPTY_CONTENT);
    xml_cu.setContentSize(EMPTY_CONTENT.length());
    MockCachedUrl pdf_cu = mau.addUrl(pdf_url, true, true, xmlHeader);
    pdf_cu.setContent(EMPTY_CONTENT);
    pdf_cu.setContentSize(EMPTY_CONTENT.length());
    
    FileMetadataExtractor me = new WoltersKluwerSourceXmlMetadataExtractorFactory
                                  .WoltersKluwerSourceXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xml_cu);
    assertEmpty(mdlist);

  }
  
  public void testExtractFromBadContent() throws Exception {
    String xml_url = TEST_XML_URL;
    String pdf_url = TEST_PDF_URL;

    CIProperties xmlHeader = new CIProperties();

    MockCachedUrl xml_cu = mau.addUrl(xml_url, true, true, xmlHeader);
    //MockCachedUrl cu = new MockCachedUrl(url, mau);
    xml_cu.setContent(BAD_CONTENT);
    xml_cu.setContentSize(BAD_CONTENT.length());
    MockCachedUrl pdf_cu = mau.addUrl(pdf_url, true, true, xmlHeader);
    pdf_cu.setContent(BAD_CONTENT);
    pdf_cu.setContentSize(BAD_CONTENT.length());

    
    FileMetadataExtractor me = new WoltersKluwerSourceXmlMetadataExtractorFactory
                                  .WoltersKluwerSourceXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xml_cu);
    assertNotEmpty(mdlist);
        ArticleMetadata md = null;

    //for (int i = 0; i< mdlist.size(); i++) {
      md = mdlist.get(0);
      assertNotNull(md);

      // checking how the "bad" inputs (date, title, authors) are handled
      assertEquals(badAuthors.toString(), md.getList(MetadataField.FIELD_AUTHOR).toString());
      assertEquals(null, md.get(MetadataField.FIELD_DATE));
      assertEquals(BAD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
      assertEquals(GOOD_DOI,md.get(MetadataField.FIELD_DOI));
    //}
  
  }
  
  // original xml file from the publisher
  public void testExtractFromBasicContent() throws Exception {
    CIProperties xmlHeader = new CIProperties();
    try {
      String xml_url = TEST_XML_URL;
      String pdf_url = TEST_PDF_URL;
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting
      MockCachedUrl pcu = mau.addUrl(pdf_url, true, true, xmlHeader);

      String string_input = BASIC_CONTENT;

      cu.setContent(string_input);
      cu.setContentSize(string_input.length());
      // the CU does not recognize the sgml (yet- maybe by 1.67?)
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/sgml");

      // setting content (non-pdf) just so the check can find content
      pcu.setContent(string_input);
      pcu.setContentSize(string_input.length());
      pcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new WoltersKluwerSourceXmlMetadataExtractorFactory.WoltersKluwerSourceXmlMetadataExtractor();
      assertNotNull(me);
      log.debug3("Extractor: " + me.toString());
 
      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = null;

      for (int i = 0; i< mdlist.size(); i++) {
       md = mdlist.get(i);
       assertNotNull(md);

       assertEquals(GOOD_ISSN, md.get(MetadataField.FIELD_ISSN));
       assertEquals(TEST_PDF_URL, md.get(MetadataField.FIELD_ACCESS_URL));
       assertEquals(GOOD_JOURNAL_TITLE, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
       assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
       assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
       assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
       assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
       assertEquals(GOOD_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
       assertEquals(goodAuthors.toString(), md.getList(MetadataField.FIELD_AUTHOR).toString());
      }
    } finally {
      //IOUtil.safeClose(file_input);
    }
  }

/*
  // get test input files from current directory
private String getInputFile(String filename) {
  String fileStr;
  try {
    InputStream jatsIn = getClass().getResourceAsStream(filename);
log.info("filename:"+filename);
    fileStr = StringUtil.fromInputStream(jatsIn);
  }
  catch (IOException e) {
     throw new RuntimeException(e);
  }
  return (fileStr);
}
/* 
  // Use when getting test content locally
  // original xml file from the publisher
  final static String baseDir = "/Users/audreyishizaki/Desktop/LOCKSS/lockss-daemon/plugins/test/src/org/lockss/plugin/clockss/wolterskluwer/";
  final static String testXmlFile = "JHYPE_20121000.0"; // "20120500.0"; //"90000000.0"; //"20120500.0";
  final static String testPdfFile = "00004872-201210000-00001.pdf"; //"00000539-201205000-00033.pdf"; //"00004583-900000000-99789.pdf"; //"00000441-201205000-00001.pdf";
  public void testExtractFromRealContent() throws Exception {
    //String xml_url = getInputFile(testXmlFile);
    //String pdf_url = getInputFile(testPdfFile);

    CIProperties xmlHeader = new CIProperties();
    InputStream xml_istream = null;
    InputStream pdf_istream = null;
    BOMInputStream bom_istream = null;
    try {
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl xcu = mau.addUrl(baseDir+testXmlFile, true, true, xmlHeader);
      // need to check for this file before emitting
      MockCachedUrl pcu = mau.addUrl(baseDir+testPdfFile, true, true, xmlHeader);

      xml_istream = getResourceAsStream(testXmlFile);
      bom_istream = new BOMInputStream(xml_istream, false);
      String xml_input = StringUtil.fromInputStream(bom_istream);
      IOUtil.safeClose(bom_istream);
      IOUtil.safeClose(xml_istream);

      //MockCachedUrl pcu = new MockCachedUrl(pdf_url, mau);
      // really not going to be read other than to check .hasContent()
      pdf_istream = getResourceAsStream(testPdfFile);
      String pdf_input = StringUtil.fromInputStream(pdf_istream);
      IOUtil.safeClose(pdf_istream);
      
      xcu.setContent(xml_input);
      xcu.setContentSize(xml_input.length());
      xcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
      pcu.setContent(pdf_input);
      pcu.setContentSize(pdf_input.length());
      pcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new WoltersKluwerSourceXmlMetadataExtractorFactory.WoltersKluwerSourceXmlMetadataExtractor();
      assertNotNull(me);
      log.debug3("Extractor: " + me.toString());
      log.info("Extractor: " + me.toString());

      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xcu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = null;
      for (int i = 0; i< mdlist.size(); i++) {
       md = mdlist.get(i);
       assertNotNull(md);
      log.info("metadata["+i+"]: ");
      log.info(md.get(MetadataField.FIELD_ISSN));
      log.info(md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      log.info(md.get(MetadataField.FIELD_ARTICLE_TITLE));
      log.info(md.get(MetadataField.FIELD_ACCESS_URL));
      log.info(md.get(MetadataField.FIELD_ISSUE));
      log.info(md.get(MetadataField.FIELD_VOLUME));
      log.info(md.get(MetadataField.FIELD_DATE));
      log.info(md.get(MetadataField.FIELD_ARTICLE_TITLE));
      log.info(md.getList(MetadataField.FIELD_AUTHOR).toString());
      log.info(md.get(MetadataField.FIELD_DOI));
      }
    } finally {
      IOUtil.safeClose(bom_istream);
      IOUtil.safeClose(xml_istream);
      IOUtil.safeClose(pdf_istream);
    }
  }
  */
  
}