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

package org.lockss.plugin.wiley;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * Wiley stores metadata in xmls.
 */
public class TestWileyMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestWileyMetadataExtractorFactory");

  private MockArchivalUnit hau;		
  private MockLockssDaemon theDaemon;

  private static String BASE_URL =
      "http://clockss-ingest.lockss.org/sourcefiles/wiley-dev/";

  @SuppressWarnings("unused")
  public void setUp() throws Exception {
    super.setUp();
    // even though you don't use path, you need to call method setUpDiskSpace
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    hau = new MockArchivalUnit();
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
  
  ArrayList<String> goodKeywords = new ArrayList<String>();
  {
    goodKeywords.add("organic organic");
    goodKeywords.add("solar solar");
    goodKeywords.add("hybrid hybrid");
    goodKeywords.add("titanium titanium");
  }
  String goodDescription = "Summary";
  String goodRights = "Rights";
  String goodCoverage = "abstract";

  String goodContent =
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
            "</numberingGroup>" +
            "<doi>" + goodDoi + "</doi>" +
          "</publicationMeta>" +

        "</header>" +
        "<body>" +
          "<section xml:id=\"sec1-1\">" +
            "<title type=\"main\">1. Introduction</title>" +
          "</section>" +
        "</body>" +
      "</component>";
  
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
            "<doi>" + goodDoi + "</doi>" +
          "</publicationMeta>" +

        "</header>" +
      "</component>";

  // content missing journal title, journal id, volume and issue
  String missingMetadataContent =
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
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, goodContent);
    CachedUrl xmlCu = hau.makeCachedUrl(xmlUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xmlCu);
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
    assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodJTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodPropId, 
                 md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    // hard-wire publisher name because Wiley has different imprints,
    // to be consistent for board report
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    // full-text content (with <body> tag)
    assertEquals(null, md.get(MetadataField.FIELD_COVERAGE));
  }
  
  // missing journal title, journal id, volume, and issue metadata
  // use <path to source file>/A/XXXX27.14.zip
  public void testExtractFromBaseAMissingMetaContent() throws Exception {
    String pdfUrl = realUrlBaseA + realPdfUrlBaseA;
    String xmlUrl = realUrlBaseA + realXmlUrlBaseA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, missingMetadataContent);
    CachedUrl xmlCu = hau.makeCachedUrl(xmlUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xmlCu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodPrintIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodEissn, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    // get journal id, volume, and issue from xml url path
    assertEquals(urlVolumeBaseA, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(urlIssueBaseA, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(urlJournalIdBaseA,
                 md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    // hard-wire publisher name because Wiley has different imprints,
    // to be consistent for board report
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    // full-text content (with <body> tag)
    assertEquals(null, md.get(MetadataField.FIELD_COVERAGE));
  }

  // missing journal title, journal id, volume, and issue metadata
  // use <path to source file>/2/11111.1.zip  
  public void testExtractFromBase2MissingMetaContent() throws Exception {
    String pdfUrl = realUrlBase2 + realPdfUrlBase2;
    String xmlUrl = realUrlBase2 + realXmlUrlBase2;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, missingMetadataContent);
    CachedUrl xmlCu = hau.makeCachedUrl(xmlUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xmlCu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodPrintIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodEissn, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    // journal id, volume and issue can be extracted from url .../11111.1.zip
    // can not get these values from the zip file name.
    assertNull(md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    // full-text content (with <body> tag)
    assertEquals(null, md.get(MetadataField.FIELD_COVERAGE));
  }
  
  // empty content
  String emptyContent =
      "<component>" +
      "</component>";

  // even if the content is empty, we still get the 
  // synthesized metadata such as publisher, journal id, volume, and issue
  public void testExtractFromEmptyContent() throws Exception {
    String pdfUrl = realUrlBaseA + realPdfUrlBaseA;
    String xmlUrl = realUrlBaseA + realXmlUrlBaseA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, emptyContent);
    CachedUrl xmlCu = hau.makeCachedUrl(xmlUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xmlCu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    // get journal id, volume, and issue from xml url path
    assertEquals(urlVolumeBaseA, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(urlIssueBaseA, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(urlJournalIdBaseA,
                 md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    // hard-wire publisher name because Wiley has different imprints,
    // to be consistent for board report
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(null, md.get(MetadataField.FIELD_COVERAGE));
  }

  // bad content -- not XML
  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodDescription + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String pdfUrl = realUrlBaseA + realPdfUrlBaseA;
    String xmlUrl = realUrlBaseA + realXmlUrlBaseA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, badContent);
    CachedUrl xmlCu = hau.makeCachedUrl(xmlUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xmlCu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    // get journal id, volume, and issue from xml url path
    assertEquals(urlVolumeBaseA, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(urlIssueBaseA, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(urlJournalIdBaseA, 
                 md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    // hard-wire publisher name because Wiley has different imprints,
    // to be consistent for board report
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(null, md.get(MetadataField.FIELD_COVERAGE));
  }
  
  // test xml with <header> but no <body> tags - likely an abstract
  public void testNoBodyTagContent() throws Exception{
    String pdfUrl = realUrlBaseA + realCoverImagePdfUrlBaseA;
    String xmlUrl = realUrlBaseA + realNoBobyTagXmlUrlBaseA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, goodNoBodyTagContent);
    CachedUrl xmlCu = hau.makeCachedUrl(xmlUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xmlCu);
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
    assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodJTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodPropId, 
                 md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    // hard-wire publisher name because Wiley has different imprints,
    // to be consistent for board report
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodCoverage, md.get(MetadataField.FIELD_COVERAGE));    
 }
  
}