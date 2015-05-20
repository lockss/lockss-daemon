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

package org.lockss.plugin.wiley;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinablePlugin;

/*
 * Wiley stores metadata in xmls.
 */
public class TestWileySourceXmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(TestWileySourceXmlMetadataExtractorFactory.class);

  private MockArchivalUnit hau;		
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = "org.lockss.plugin.wiley.ClockssWileySourcePlugin";
  private static String BASE_URL =
      "http://clockss-ingest.lockss.org/sourcefiles/wiley-dev/";
  
  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2011");
    return conf;
  }

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    hau = new MockArchivalUnit();
    hau.setConfiguration(auConfig());

  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  // use journal XXXX27.14 for testing
  String realUrlBaseA = BASE_URL + "2011/A/XXXX27.14.zip!/";
  String realUrlBase2 = BASE_URL + "2011/2/11111.1.zip!/";
  String realPdfUrlBaseA = "1810test_ftp.pdf"; 
  String realXmlUrlBaseA = "1810test_ftp.wml.xml"; 
  String realPdfUrlBase2 = "15test_ftp.pdf"; 
  String realXmlUrlBase2 = "15test_ftp.wml.xml";
  // test xml with no <body>, field_coverage=abstract
  String realCoverImagePdfUrlBaseA = "1803test_ftp.pdf"; 
  String realNoBobyTagXmlUrlBaseA = "1803test_hdp.wml.xml"; 
  
  String goodTitle = "Article Title";
  String goodJTitle = "JournalTitle";
  String goodPublisher = "John Wiley and Sons";
  ArrayList<String> goodAuthors = new ArrayList<String>();
  {
    goodAuthors.add("Doe, Deer");
  }
  String goodPrintIssn = "0101-0101";
  String goodEissn = "1010-1010";
  String goodVolume = "23";
  String goodDate = "2011-04-26";
  String goodIssue = "16";
  String goodPageFirst = "1810";
  String goodPageLast = "1828";
  String goodDoi = "10.1111/xxxx.222222222";
  String goodPropId = "ADMA";
  String hardwiredPublisher = "John Wiley & Sons, Inc.";
  // test for XXXX.27.14
  String urlJournalIdBaseA = "XXXX"; // extract using plugin JOURNAL_PATTERN
  String urlVolumeBaseA = "27";      
  String urlIssueBaseA = "14"; 
  

  // wiley puts in long keyword phrases which cause a warning from the 
  // database as it truncates them. We don't need them, so just cease to 
  // pick them up (and therefore, don't test any longer)
  ArrayList<String> goodKeywords = new ArrayList<String>();
  {
    goodKeywords.add("organic organic");
    goodKeywords.add("solar solar");
    goodKeywords.add("hybrid hybrid");
    goodKeywords.add("titanium titanium");
  }
  String goodDescription = "Summary";
  String goodRights = "Rights";

  String firstGoodPortion =
      "<component>" +
        "<header>" +
          "<contentMeta>" +
            "<titleGroup>" +
              "<title>" +
                goodTitle +
              "</title>" +
            "</titleGroup>" +
            "<keywordGroup>" +
              "<keyword>" + goodKeywords.get(0) + "</keyword>" +
              "<keyword>" + goodKeywords.get(1) + "</keyword>" +
              "<keyword>" + goodKeywords.get(2) + "</keyword>" +
              "<keyword>" + goodKeywords.get(3) + "</keyword>" +
            "</keywordGroup>" +
            "<creators>" +
              "<creator creatorRole=\"author\">" +
                "<personName>" +
                  "<givenNames>Deer</givenNames>" +
                  "<familyName>Doe</familyName>" +
                "</personName>" +
              "</creator>" +
            "</creators>" +
          "</contentMeta>" +
       
          "<publicationMeta level=\"product\">" +
            "<publisherInfo>" +
              "<publisherName>" + goodPublisher + "</publisherName>" +
            "</publisherInfo>" +
            "<titleGroup>" +
              "<title>" + goodJTitle + "</title>" +
            "</titleGroup>" +
            "<issn type=\"print\">" + goodPrintIssn + "</issn>" +
            "<issn type=\"electronic\">" + goodEissn + "</issn>" +
            "<idGroup>" +
              "<id type=\"product\" value=\"" + goodPropId + "\" />" +
            "</idGroup>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"part\">" +
            "<coverDate startDate=\"" + goodDate + "\">April 2011</coverDate>" +
            "<numberingGroup>" +
              "<numbering type=\"journalVolume\">" + goodVolume + "</numbering>" +
              "<numbering type=\"journalIssue\">" + goodIssue + "</numbering>" +
            "</numberingGroup>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"unit\">" +
            "<numberingGroup>" +
              "<numbering type=\"pageFirst\">" + goodPageFirst + "</numbering>" +
              "<numbering type=\"pageLast\">" + goodPageLast + "</numbering>" +
            "</numberingGroup>"; 
  String linkGroupPortion =
            "<linkGroup>" +
              "<link href=\"" + realPdfUrlBaseA + "\" type=\"toTypesetVersion\"/>" +
            "</linkGroup>";
  String endGoodPortion =
            "<doi>" + goodDoi + "</doi>" +
          "</publicationMeta>" +

        "</header>" +
        "<body>" +
          "<section xml:id=\"sec1-1\">" +
            "<title type=\"main\">1. Introduction</title>" +
          "</section>" +
        "</body>" +
      "</component>";

  String goodContent = firstGoodPortion + linkGroupPortion + endGoodPortion;

  
    // good abstract content with no <body> tag, 
    // its corresponding pdf is not full-text, 
    // containing only cover image and/or abstract image
    String goodNoBodyTagContent =
      "<component>" +
        "<header>" +
          "<contentMeta>" +
            "<titleGroup>" +
              "<title>" +
                goodTitle +
              "</title>" +
            "</titleGroup>" +
            "<keywordGroup>" +
              "<keyword>" + goodKeywords.get(0) + "</keyword>" +
              "<keyword>" + goodKeywords.get(1) + "</keyword>" +
              "<keyword>" + goodKeywords.get(2) + "</keyword>" +
              "<keyword>" + goodKeywords.get(3) + "</keyword>" +
            "</keywordGroup>" +
            "<creators>" +
              "<creator creatorRole=\"author\">" +
                "<personName>" +
                  "<givenNames>Deer</givenNames>" +
                  "<familyName>Doe</familyName>" +
                "</personName>" +
              "</creator>" +
            "</creators>" +
          "</contentMeta>" +
       
          "<publicationMeta level=\"product\">" +
            "<publisherInfo>" +
              "<publisherName>" + goodPublisher + "</publisherName>" +
            "</publisherInfo>" +
            "<titleGroup>" +
              "<title>" + goodJTitle + "</title>" +
            "</titleGroup>" +
            "<issn type=\"print\">" + goodPrintIssn + "</issn>" +
            "<issn type=\"electronic\">" + goodEissn + "</issn>" +
            "<idGroup>" +
              "<id type=\"product\" value=\"" + goodPropId + "\" />" +
            "</idGroup>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"part\">" +
            "<coverDate startDate=\"" + goodDate + 
                                                  "\">April 2011</coverDate>" +
            "<numberingGroup>" +
              "<numbering type=\"journalVolume\">" + goodVolume + 
                                                               "</numbering>" +
              "<numbering type=\"journalIssue\">" + goodIssue + 
                                                               "</numbering>" +
            "</numberingGroup>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"unit\">" +
            "<numberingGroup>" +
              "<numbering type=\"pageFirst\">" + goodPageFirst + 
                                                               "</numbering>" +
              "<numbering type=\"pageLast\">" + goodPageLast + "</numbering>" +
            "</numberingGroup>" +
            "<linkGroup>" +
              "<link href=\"" + realCoverImagePdfUrlBaseA + "\" type=\"toTypesetVersion\"/>" +
            "</linkGroup>" +
            "<doi>" + goodDoi + "</doi>" +
          "</publicationMeta>" +

        "</header>" +
      "</component>";

  // content missing journal title, journal id, volume and issue
  String missingMetadataContentA =
      "<component>" +
        "<header>" +
          "<contentMeta>" +
            "<titleGroup>" +
              "<title>" +
                goodTitle +
              "</title>" +
            "</titleGroup>" +
            "<keywordGroup>" +
              "<keyword>" + goodKeywords.get(0) + "</keyword>" +
              "<keyword>" + goodKeywords.get(1) + "</keyword>" +
              "<keyword>" + goodKeywords.get(2) + "</keyword>" +
              "<keyword>" + goodKeywords.get(3) + "</keyword>" +
            "</keywordGroup>" +
            "<creators>" +
              "<creator creatorRole=\"author\">" +
                "<personName>" +
                  "<givenNames>Deer</givenNames>" +
                  "<familyName>Doe</familyName>" +
                "</personName>" +
              "</creator>" +
            "</creators>" +
          "</contentMeta>" +
       
          "<publicationMeta level=\"product\">" +
            "<publisherInfo>" +
              "<publisherName>" + goodPublisher + "</publisherName>" +
            "</publisherInfo>" +
            "<titleGroup>" +
              "<title></title>" +
            "</titleGroup>" +
            "<issn type=\"print\">" + goodPrintIssn + "</issn>" +
            "<issn type=\"electronic\">" + goodEissn + "</issn>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"part\">" +
            "<coverDate startDate=\"" + goodDate + 
                                                  "\">April 2011</coverDate>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"unit\">" +
            "<numberingGroup>" +
              "<numbering type=\"pageFirst\">" + goodPageFirst + 
                                                               "</numbering>" +
              "<numbering type=\"pageLast\">" + goodPageLast + "</numbering>" +
            "</numberingGroup>" +
            "<linkGroup>" +
              "<link href=\"" + realPdfUrlBaseA + "\" type=\"toTypesetVersion\"/>" +
            "</linkGroup>" +
            "<doi>" + goodDoi + "</doi>" +
          "</publicationMeta>" +

        "</header>" +
        "<body>" +
          "<section xml:id=\"sec1-1\">" +
            "<title type=\"main\">1. Introduction</title>" +
          "</section>" +
        "</body>" +
      "</component>";

  // content missing journal title, journal id, volume and issue
  String missingMetadataContent2 =
      "<component>" +
        "<header>" +
          "<contentMeta>" +
            "<titleGroup>" +
              "<title>" +
                goodTitle +
              "</title>" +
            "</titleGroup>" +
            "<keywordGroup>" +
              "<keyword>" + goodKeywords.get(0) + "</keyword>" +
              "<keyword>" + goodKeywords.get(1) + "</keyword>" +
              "<keyword>" + goodKeywords.get(2) + "</keyword>" +
              "<keyword>" + goodKeywords.get(3) + "</keyword>" +
            "</keywordGroup>" +
            "<creators>" +
              "<creator creatorRole=\"author\">" +
                "<personName>" +
                  "<givenNames>Deer</givenNames>" +
                  "<familyName>Doe</familyName>" +
                "</personName>" +
              "</creator>" +
            "</creators>" +
          "</contentMeta>" +
       
          "<publicationMeta level=\"product\">" +
            "<publisherInfo>" +
              "<publisherName>" + goodPublisher + "</publisherName>" +
            "</publisherInfo>" +
            "<titleGroup>" +
              "<title></title>" +
            "</titleGroup>" +
            "<issn type=\"print\">" + goodPrintIssn + "</issn>" +
            "<issn type=\"electronic\">" + goodEissn + "</issn>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"part\">" +
            "<coverDate startDate=\"" + goodDate + 
                                                  "\">April 2011</coverDate>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"unit\">" +
            "<numberingGroup>" +
              "<numbering type=\"pageFirst\">" + goodPageFirst + 
                                                               "</numbering>" +
              "<numbering type=\"pageLast\">" + goodPageLast + "</numbering>" +
            "</numberingGroup>" +
            "<linkGroup>" +
              "<link href=\"" + realPdfUrlBase2 + "\" type=\"toTypesetVersion\"/>" +
            "</linkGroup>" +
            "<doi>" + goodDoi + "</doi>" +
          "</publicationMeta>" +

        "</header>" +
        "<body>" +
          "<section xml:id=\"sec1-1\">" +
            "<title type=\"main\">1. Introduction</title>" +
          "</section>" +
        "</body>" +
      "</component>";

  public void testExtractFromGoodContent() throws Exception {
    String pdfUrl = realUrlBaseA + realPdfUrlBaseA;
    String xmlUrl = realUrlBaseA + realXmlUrlBaseA;

    MockCachedUrl mcu = hau.addUrlContype(xmlUrl, true, true, "text/xml");
    mcu.setContent(goodContent);
    mcu.setContentSize(goodContent.length());
    hau.addUrl(pdfUrl, true, true); //doesn't matter what content-type
    
    FileMetadataExtractor me = getNewExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = 
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodPrintIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodEissn, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    // we no longer pick up keywords
    //assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodJTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodPropId, 
                 md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    // hard-wire publisher name because Wiley has different imprints,
    // to be consistent for board report
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(pdfUrl, md.get(MetadataField.FIELD_ACCESS_URL));
  }
  
  // missing journal title, journal id, volume, and issue metadata
  // use <path to source file>/A/XXXX27.14.zip
  public void testExtractFromBaseAMissingMetaContent() throws Exception {
    String pdfUrl = realUrlBaseA + realPdfUrlBaseA;
    String xmlUrl = realUrlBaseA + realXmlUrlBaseA;
    
    MockCachedUrl mcu = hau.addUrlContype(xmlUrl, true, true, "text/xml");
    mcu.setContent(missingMetadataContentA);
    mcu.setContentSize(missingMetadataContentA.length());
    hau.addUrl(pdfUrl, true, true); //doesn't matter what content-type

    FileMetadataExtractor me = getNewExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodPrintIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodEissn, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    // we no longer pick up keywords
    //assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    // get journal id, volume, and issue from xml url path
    assertEquals(urlVolumeBaseA, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(urlIssueBaseA, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(urlJournalIdBaseA,
                 md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    // hard-wire publisher name because Wiley has different imprints,
    // to be consistent for board report
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(pdfUrl, md.get(MetadataField.FIELD_ACCESS_URL));
  }

  // missing journal title, journal id, volume, and issue metadata
  // use <path to source file>/2/11111.1.zip  
  public void testExtractFromBase2MissingMetaContent() throws Exception {
    String pdfUrl = realUrlBase2 + realPdfUrlBase2;
    String xmlUrl = realUrlBase2 + realXmlUrlBase2;

    MockCachedUrl mcu = hau.addUrlContype(xmlUrl, true, true, "text/xml");
    mcu.setContent(missingMetadataContent2);
    mcu.setContentSize(missingMetadataContent2.length());
    hau.addUrl(pdfUrl, true, true); //doesn't matter what content-type

    FileMetadataExtractor me = getNewExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodPrintIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodEissn, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    // we no longer pick up keywords
    //assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    // journal id, volume and issue can be extracted from url .../11111.1.zip
    // can not get these values from the zip file name.
    assertNull(md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    // full-text content (with <body> tag)
    assertEquals(pdfUrl, md.get(MetadataField.FIELD_ACCESS_URL));
  }
  
  // empty content
  String emptyContent =
      "<component>" +
      "</component>";
  
  String noPDFContent = firstGoodPortion + endGoodPortion;  // no linkGroupPortion 

 
  public void testExtractFromEmptyContent() throws Exception {
    String xmlUrl = realUrlBaseA + realXmlUrlBaseA;
    
    MockCachedUrl mcu = hau.addUrlContype(xmlUrl, true, true, "text/xml");
    mcu.setContent(emptyContent);
    mcu.setContentSize(emptyContent.length());
    FileMetadataExtractor me = getNewExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);

    // We no longer synthesize a partial AM for empty or incomplete XML.
    // the information available (issue, volume, jid) wasn't helpful
    // and in the real life cases, there was no preserved corresponding pdf
    // to access or count
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertEmpty(mdlist);

    // this XML has valid content, but doesn't indicate a pdf filename so 
    // we can't validate. In the real-life cases of this, there was no
    // corresponding PDF file and so we shouldn't emit
    mcu.setContent(noPDFContent);
    mcu.setContentSize(noPDFContent.length());
    mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertEmpty(mdlist);
  }

  // bad content -- not XML
  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodDescription + " </div>\n";

  // now - without XML access, we don't emit because we can't be sure we have
  // a corresponding PDF and in the real-life cases we found, except for one
  // corrupt XML file, there was no PDF
  public void testExtractFromBadContent() throws Exception {
    String pdfUrl = realUrlBaseA + realPdfUrlBaseA;
    String xmlUrl = realUrlBaseA + realXmlUrlBaseA;
    
    MockCachedUrl mcu = hau.addUrlContype(xmlUrl, true, true, "text/xml");
    mcu.setContent(badContent);
    mcu.setContentSize(badContent.length());
    hau.addUrl(pdfUrl, true, true); //doesn't matter what content-type

    FileMetadataExtractor me = getNewExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertEmpty(mdlist);
  }
  
  // test xml with <header> but no <body> tags - likely an abstract
  public void testNoBodyTagContent() throws Exception{
    String pdfUrl = realUrlBaseA + realCoverImagePdfUrlBaseA;
    String xmlUrl = realUrlBaseA + realNoBobyTagXmlUrlBaseA;
    
    MockCachedUrl mcu = hau.addUrlContype(xmlUrl, true, true, "text/xml");
    mcu.setContent(goodNoBodyTagContent);
    mcu.setContentSize(goodNoBodyTagContent.length());
    hau.addUrl(pdfUrl, true, true); //doesn't matter what content-type

    FileMetadataExtractor me = getNewExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodPrintIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodEissn, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    // we no longer pick up keywords
    //assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodJTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodPropId, 
                 md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    // hard-wire publisher name because Wiley has different imprints,
    // to be consistent for board report
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(pdfUrl, md.get(MetadataField.FIELD_ACCESS_URL));
 }
  
  String oddPDFName1 = firstGoodPortion + "<linkGroup><link href=\"" +  // leading file:
      "file:123foo.pdf" +
  "\" type=\"toTypesetVersion\"/></linkGroup>" + endGoodPortion;   
  String oddPDFName2 = firstGoodPortion + "<linkGroup><link href=\"" + // leading file://
      "file://123foo.pdf" +
  "\" type=\"toTypesetVersion\"/></linkGroup>" + endGoodPortion;  
  String oddPDFName3 = firstGoodPortion + "<linkGroup><link href=\"" + // trailing space - never seen 
      "123foo.pdf   " +
  "\" type=\"toTypesetVersion\"/></linkGroup>" + endGoodPortion;  
  String oddPDFName4 = firstGoodPortion + "<linkGroup><link href=\"" + // trailing fust - never seen 
      "123foo.pdfss" +
  "\" type=\"toTypesetVersion\"/></linkGroup>" + endGoodPortion;  
  
  public void testFileNameParsing() throws Exception {
    String pdfUrl = realUrlBase2 + "123foo.pdf";
    String xmlUrl = realUrlBase2 + realXmlUrlBase2;

    MockCachedUrl mcu = hau.addUrlContype(xmlUrl, true, true, "text/xml");
    mcu.setContent(oddPDFName1);
    mcu.setContentSize(oddPDFName1.length());
    hau.addUrl(pdfUrl, true, true); //doesn't matter what content-type
    FileMetadataExtractor me = getNewExtractor();
    assertNotNull(me);
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    
    //first test
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals("123foo.pdf", md.getRaw("header/publicationMeta/linkGroup/link[@type='toTypesetVersion']/@href"));
    assertEquals(realUrlBase2 + "123foo.pdf", md.get(MetadataField.FIELD_ACCESS_URL));
    
    //2nd test
    mcu.setContent(oddPDFName2);
    mcu.setContentSize(oddPDFName2.length());
    
    mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals("123foo.pdf", md.getRaw("header/publicationMeta/linkGroup/link[@type='toTypesetVersion']/@href"));
    assertEquals(realUrlBase2 + "123foo.pdf", md.get(MetadataField.FIELD_ACCESS_URL));
    
    //3rd test
    mcu.setContent(oddPDFName3);
    mcu.setContentSize(oddPDFName3.length());
    
    mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals("123foo.pdf", md.getRaw("header/publicationMeta/linkGroup/link[@type='toTypesetVersion']/@href"));
    assertEquals(realUrlBase2 + "123foo.pdf", md.get(MetadataField.FIELD_ACCESS_URL));
    
    //4th test
    mcu.setContent(oddPDFName4);
    mcu.setContentSize(oddPDFName4.length());
    
    mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals("123foo.pdf", md.getRaw("header/publicationMeta/linkGroup/link[@type='toTypesetVersion']/@href"));
    assertEquals(realUrlBase2 + "123foo.pdf", md.get(MetadataField.FIELD_ACCESS_URL));    
  }

  // single point of change for new extractor type  
  private FileMetadataExtractor getNewExtractor() {
    return new WileySourceXmlMetadataExtractorFactory.WileySourceXmlMetadataExtractor();
  }
  
}