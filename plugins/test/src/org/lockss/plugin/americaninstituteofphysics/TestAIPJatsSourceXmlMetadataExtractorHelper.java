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

package org.lockss.plugin.americaninstituteofphysics;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinablePlugin;

//import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;

/*
 * Test file used to extract metadata:
 * <base_url>2013/JAP/v114/i18/183704_1/Markup/VOR_10.1063_1.4829703.xml
 *      basic examples to test metadata extraction
 */

public class TestAIPJatsSourceXmlMetadataExtractorHelper
  extends LockssTestCase {
  
  static Logger log = 
      Logger.getLogger(TestAIPJatsSourceXmlMetadataExtractorHelper.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau; // aipjats source au

  private static final String PLUGIN_NAME =
      "org.lockss.plugin.americaninstituteofphysics.ClockssAIPJatsSourcePlugin";

  private static final String YEAR = "2013";

  private static final String BASE_URL = 
      "http://clockss-ingest.lockss.org/sourcefiles/aipjats-released/"+ YEAR + "/";
  
  private static final String BASIC_CONTENT_FILE_NAME = "test_jats1.xml";
   
  private static final String TEST_XML_URL = "localhost:~audreyishizaki/aip-aud/Markup/test_jats0.xml";
  private static final String TEST_PDF_URL = "localhost:~audreyishizaki/aip-aud/Page_Renditions/online.pdf";

  // expected metadata
  private static final String GOOD_JOURNAL_TITLE = "Journal of Applied Physics";
  private static final String GOOD_PUB_DATE = "2012-06-01";
  private static final String GOOD_DOI = "10.1063/1.4726137";
  private static final String GOOD_ISSN = "0021-8979";
  private static final String GOOD_EISSN = "1089-7550";
  private static final String GOOD_ISSUE = "11";
  private static final String GOOD_ARTICLE_TITLE = "Preface to Special Topic: Selected Papers ... from the International Conference on the Study of STUFF at Extreme Conditions, SSEC 2011";
  private static final String GOOD_JOURNAL_ID = "JAPIAU";
  private static final String GOOD_VOLUME = "111";
//  private static final String HARDWIRED_PUBLISHER = "American Institute of Physics";
  private static ArrayList goodAuthors = (ArrayList) ListUtil.list(
      "Chelast, Firsthua",
      "Liast, ",
      "Sainlast, Naufirst L.",
      "Sun, B. M.");
   
 
  private static final String BASIC_CONTENT =
    "<?xml version=\"1.0\" ?>" +
    "<!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.0 20120330//EN\" \"JATS-archivearticle1.dtd\">" +
    "<article xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" article-type=\"preface\" dtd-version=\"1.0\" specific-use=\"syndication\">" +
    "<front>" +
    "<journal-meta>" +
    "<journal-id journal-id-type=\"coden\">JAPIAU</journal-id>" +
    "<journal-title-group>" +
    "<journal-title>Journal of Applied Physics</journal-title>" +
    "<abbrev-journal-title>J. Appl. Phys.</abbrev-journal-title>" +
    "</journal-title-group>" +
    "<issn pub-type=\"ppub\">0021-8979</issn>" +
    "<issn pub-type=\"epub\">1089-7550</issn>" +
    "<publisher>" +
    "<publisher-name>American Institute of Physics</publisher-name>" +
    "<publisher-name specific-use=\"short-name\">AIP</publisher-name>" +
    "</publisher>" +
    "</journal-meta>" +
    "<article-meta>" +
    "<article-id pub-id-type=\"publisher-id:aipid\">001297JAP</article-id>" +
    "<article-id pub-id-type=\"publisher-id:aipkey\">1.4726137</article-id>" +
    "<article-id pub-id-type=\"doi\">10.1063/1.4726137</article-id>" +
    "<article-id pub-id-type=\"manuscript\">J-SMEC6-12-4290</article-id>" +
    "<article-categories>" +
    "<subj-group subj-group-type=\"toc-heading\">" +
    "<subject>" +
    "SPECIAL TOPIC: INVITED PAPERS FROM THE 6TH MEETING OF THE STUDY OF STUFF AT EXTREME CONDITIONS (SSEC), MIAMI, FLORIDA, USA, 2011" +
    "</subject>" +
    "<subj-group>" +
    "<subject>Preface</subject>" +
    "</subj-group>" +
    "</subj-group>" +
    "</article-categories>" +
    "<title-group>" +
    "<article-title>" +
    "Preface to Special Topic: Selected Papers" +
    "<inline-formula><mml:math display=\"inline\" overflow=\"scroll\"><mml:mrow><mml:mi>G</mml:mi><mml:mi>a</mml:mi></mml:mrow></mml:math></inline-formula>" + 
    /* testing for inline formulas in article titles - replacing formulae with " ... " */
    "from the International Conference on the Study of STUFF at Extreme Conditions, SSEC 2011" +
    "</article-title>" +
    "</title-group>" +
    "<contrib-group>" +
    "<contrib contrib-type=\"author\">" +
    "<name name-style=\"western\">" +   
    "<surname>Chelast</surname>" +
    "<given-names>Firsthua</given-names>" +
    "</name>" +
    "<xref ref-type=\"aff\" rid=\"a1\">1</xref>" +
    "</contrib>" +
    "<contrib contrib-type=\"author\">" +
    "<name name-style=\"western\">" +
    "<surname>Liast</surname>" +
    "<given-names></given-names>" +
    "</name>" +
    "<xref ref-type=\"aff\" rid=\"a2\">2</xref>" +
    "</contrib>" +
    "<contrib contrib-type=\"author\">" +
    "<name name-style=\"western\">" +
    "<surname>Sainlast</surname>" +
    "<given-names>Naufirst L.</given-names>" +
    "</name>" +
    "<xref ref-type=\"aff\" rid=\"a3\">3</xref>" +
    "</contrib>" +
    "<contrib contrib-type=\"author\">"+
    "<name-alternatives><name name-style=\"western\">"+
    "<surname>Sun</surname>"+
    "<given-names>B. M.</given-names></name>"+
    /* if you add the following, complaints about invalid utf-8 characters follow
    "<string-name name-style=\"eastern\" xml:lang=\"zh\">孙保民</string-name>" +
     */
    "</name-alternatives></contrib>"+
    "<aff id=\"a1\">" +
    "<label>1</label>" +
    "<institution>" +
    "Center for the Study of Stuff at Extreme Conditions, Florida Extranational University" +
    "</institution>" +
    ", Miami, Florida 33144," +
    "<country>USA</country>" +
    "</aff>" +
    "<aff id=\"a2\">" +
    "<label>2</label>" +
    "<institution>" +
    "Natural Science Research Center, Harbin Institute of Technology (HIT)" +
    "</institution>" +
    ", 2 Yikuang Street, Harbin 150080" +
    "<country>China</country>" +
    "</aff>" +
    "<aff id=\"a3\">" +
    "<label>3</label>" +
    "<institution>Department of Physics, Sapienza University of Rome</institution>" +
    ", Piazzale Aldo Moro 2, 00185 Roma," +
    "<country>Italy</country>" +
    "</aff>" +
    "</contrib-group>" +
    "<pub-date pub-type=\"ppub\">" +
    "<day>01</day>" +
    "<month>06</month>" +
    "<year>2012</year>" +
    "</pub-date>" +
    "<volume>111</volume>" +
    "<issue>11</issue>" +
    "<elocation-id seq=\"1\">112501</elocation-id>" +
    "<history>" +
    "<date date-type=\"received\">" +
    "<day>15</day>" +
    "<month>05</month>" +
    "<year>2012</year>" +
    "</date>" +
    "<date date-type=\"accepted\">" +
    "<day>16</day>" +
    "<month>05</month>" +
    "<year>2012</year>" +
    "</date>" +
    "<date date-type=\"published\">" +
    "<day>15</day>" +
    "<month>06</month>" +
    "<year>2012</year>" +
    "</date>" +
    "<date date-type=\"online\" specific-use=\"metadata\">" +
    "<string-date>2012-06-15T08:24:17</string-date>" +
    "</date>" +
    "</history>" +
    "<permissions>" +
    "<copyright-year>2012</copyright-year>" +
    "<copyright-holder>American Institute of Physics</copyright-holder>" +
    "<license license-type=\"ccc\">" +
    "<license-p>" +
    "0021-8979/2012/111(11)/112501/1/" +
    "<price>$30.00</price>" +
    "</license-p>" +
    "</license>" +
    "<license license-type=\"FREE\">" +
    "<license-p/>" +
    "</license>" +
    "</permissions>" +
    "<kwd-group kwd-group-type=\"pacs-codes\">" +
    "<compound-kwd>" +
    "<compound-kwd-part content-type=\"code\">0130Cc</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"value\">Conference proceedings</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd>" +
    "<compound-kwd-part content-type=\"code\">7462-c</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"value\">Transition temperature variations, phase diagrams</compound-kwd-part>" +
    "</compound-kwd>" +
    "</kwd-group>" +
    "<kwd-group kwd-group-type=\"inspec\">" +
    "<kwd>superconducting transitions</kwd>" +
    "</kwd-group>" +
    "<kwd-group kwd-group-type=\"sem:SCitationThes\" specific-use=\"online\">" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.1\">" +
    "<compound-kwd-part content-type=\"value\">Advanced materials</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">151</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">3</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.2\">" +
    "<compound-kwd-part content-type=\"value\">Geological materials</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">2429</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">2429</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.3\">" +
    "<compound-kwd-part content-type=\"value\">Renewable energy</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">5818</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.4\">" +
    "<compound-kwd-part content-type=\"value\">Anatomy</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">231</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.5\">" +
    "<compound-kwd-part content-type=\"value\">Planetary interiors</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">5179</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.6\">" +
    "<compound-kwd-part content-type=\"value\">Materials science</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">3820</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.7\">" +
    "<compound-kwd-part content-type=\"value\">High pressure</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">2701</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.8\">" +
    "<compound-kwd-part content-type=\"value\">Academia</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">14</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.9\">" +
    "<compound-kwd-part content-type=\"value\">Researchers</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">5824</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "<compound-kwd content-type=\"sem:SCitationThes1.2\" id=\"kwd1.10\">" +
    "<compound-kwd-part content-type=\"value\">Geophysical exploration</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"code\">2452</compound-kwd-part>" +
    "<compound-kwd-part content-type=\"weight\">1</compound-kwd-part>" +
    "</compound-kwd>" +
    "</kwd-group>" +
    "<counts>" +
    "<page-count count=\"1\"/>" +
    "</counts>" +
    "</article-meta>" +
    "</front>" +
    "<body>" +
    "<p>" +
    "This special issue of the Journal of Applied Physics contains a collection of papers on all aspects of interdisciplinary themes treated in the SSEC 2011." +
    "</p>" +
    "</body>" +
    "</article>";
  
  private static final String EMPTY_CONTENT =
      "<article>" +
      "</article>";
  
  private static final String BAD_CONTENT =
    "<HTML><HEAD><TITLE>" + GOOD_ARTICLE_TITLE + "</TITLE></HEAD><BODY>\n"
    + "<meta name=\"foo\"" +  " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + " </div>\n";


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
  
  public TestAIPJatsSourceXmlMetadataExtractorHelper() throws Exception {
    super.setUp();

    setUp();
  }
 
    // get test input files from current directory
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
    
    FileMetadataExtractor me = new AIPJatsSourceXmlMetadataExtractorFactory
                                  .AIPJatsSourceXmlMetadataExtractor();
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

    
    FileMetadataExtractor me = new AIPJatsSourceXmlMetadataExtractorFactory
                                  .AIPJatsSourceXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xml_cu);
    assertEmpty(mdlist);
  
  }

  // original xml file from the publisher
  public void testExtractFromBasicContent() throws Exception {
    CIProperties xmlHeader = new CIProperties();
    try {
      String xml_url = BASE_URL + "Markup/basic.xml";
      String pdf_url = BASE_URL + "Page_Renditions/online.pdf";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting
      MockCachedUrl pcu = mau.addUrl(pdf_url, true, true, xmlHeader);

      String string_input = BASIC_CONTENT;

      cu.setContent(string_input);
      cu.setContentSize(string_input.length());
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
      // setting content (non-pdf) just so the check can find content
      pcu.setContent(string_input);
      pcu.setContentSize(string_input.length());
      pcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new AIPJatsSourceXmlMetadataExtractorFactory.AIPJatsSourceXmlMetadataExtractor();
      assertNotNull(me);
      log.debug3("Extractor: " + me.toString());
      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);

      assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
      assertEquals(GOOD_ISSN, md.get(MetadataField.FIELD_ISSN));
      assertEquals(GOOD_EISSN, md.get(MetadataField.FIELD_EISSN));
      assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
      assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
      assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
//      assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
      assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
      //use FIELD_JOURNAL_TITLE for content5/6 until they adopt the latest daemon
      assertEquals(GOOD_JOURNAL_TITLE, md.get(MetadataField.FIELD_JOURNAL_TITLE));
      //assertEquals(GOOD_JOURNAL_TITLE, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GOOD_JOURNAL_ID, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
      assertEquals(goodAuthors.toString(), md.getList(MetadataField.FIELD_AUTHOR).toString());

    } finally {
      //IOUtil.safeClose(file_input);
    }
  }
  
 /*
  // Use when getting test content locally
  private String basicInputContent = getInputFile("Markup/VOR_10.1063_1.4829703.xml");

  // original xml file from the publisher
  public void testExtractFromRealContent() throws Exception {
    String xml_url = "Markup/VOR_10.1063_1.4829703.xml";
    String pdf_url ="Page_Renditions/online.pdf";
    CIProperties xmlHeader = new CIProperties();

    InputStream file_input = null;
    try {
      //MockCachedUrl mcu = new MockCachedUrl(url, mau);
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      //MockCachedUrl cu = new MockCachedUrl(xml_url, mau);
      MockCachedUrl xcu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting
      MockCachedUrl pcu = mau.addUrl(pdf_url, true, true, xmlHeader);

      file_input = getResourceAsStream(xml_url);
      String xml_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);
      //MockCachedUrl pcu = new MockCachedUrl(pdf_url, mau);
      // really not going to be read other than to check .hasContent()
      file_input = getResourceAsStream(xml_url);
      String pdf_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);
      
      xcu.setContent(xml_input);
      xcu.setContentSize(xml_input.length());
      xcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
      pcu.setContent(pdf_input);
      pcu.setContentSize(pdf_input.length());
      pcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new AIPJatsSourceXmlMetadataExtractorFactory.AIPJatsSourceXmlMetadataExtractor();
      assertNotNull(me);
      log.debug3("Extractor: " + me.toString());
      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xcu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);

          assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
          assertEquals(GOOD_ISSN, md.get(MetadataField.FIELD_ISSN));
          assertEquals(GOOD_EISSN, md.get(MetadataField.FIELD_EISSN));
          assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
          assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
          assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
          assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
          assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
          //use FIELD_JOURNAL_TITLE for content5/6 until they adopt the latest daemon
       assertEquals(GOOD_JOURNAL_TITLE, md.get(MetadataField.FIELD_JOURNAL_TITLE));
      //assertEquals(GOOD_JOURNAL_TITLE, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GOOD_JOURNAL_ID, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
      assertEquals(goodAuthors, md.get(MetadataField.FIELD_AUTHOR));
      
    } finally {
      IOUtil.safeClose(file_input);
    }
  }
  */
}