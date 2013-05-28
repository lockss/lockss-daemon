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

/**
 * One of the articles used to get the xml source for this plugin is:
 * http://clockss-ingest.lockss.org/sourcefiles/aip-dev/2010/AIP_xml_9.tar.gz/AIP_xml_9.tar/./APPLAB/vol_96/iss_1/01212_1.xml 
 */
public class TestWileyMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestAmericanInstituteOfPhysicsMetadataExtractorFactory");

  private MockArchivalUnit hau;		
  private MockLockssDaemon theDaemon;

  private static String BASE_URL = "http://clockss-ingest.lockss.org/sourcefiles/wiley-dev/";

  @SuppressWarnings("unused")
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();//even though you don't use path, you need to call method
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


  /* The journal title is pulled from the journal code in the URL */
  String realUrlBaseA = BASE_URL + "2011/A/AEN49.1.zip!/";
  String realUrlBase0 = BASE_URL + "2011/0/049.1.zip!/";
  String realPdfUrlA = "j.1440-6055.2009.00720.x.pdf"; //Australian Journal of Entomology
  String realXmlUrlA = "j.1440-6055.2009.00720.x.wml.xml"; //Australian Journal of Entomology
  String realPdfUrlB = "j.1440-6055.2009.00725.x.pdf"; // Australian Journal of Entomology
  String realXmlUrlB = "j.1440-6055.2009.00725.x.wml.xml"; // Australian Journal of Entomology
  
  String goodTitle = "Article Title";
  String goodJTitle = "JournalTitle";
  String goodPublisher = "John Wiley and Sons";
  ArrayList<String> goodAuthors = new ArrayList<String>();
  {
    goodAuthors.add("Doe, John");
  }
  String goodPrintIssn = "1234-5678";
  String goodEissn = "8765-4321";
  String goodVolume = "49";
  String goodDate = "2010-02";
  String goodIssue = "1";
  String goodPageFirst = "34";
  String goodPageLast = "43";
  String goodDoi = "10.1111/j.1440-6055.2009.00732.x";
  String goodPropId = "AEN";
  String hardwiredPublisher = "John Wiley & Sons, Inc.";
  ArrayList<String> goodKeywords = new ArrayList<String>();
  {
    goodKeywords.add("commercialisation");
    goodKeywords.add("Helicoverpa");
  }
  String goodDescription = "Summary";
  String goodRights = "Rights";
//  xpathMap.put("/component/header/contentMeta/titleGroup/title", MetadataField.FIELD_ARTICLE_TITLE);
//  xpathMap.put("/component/header/publicationMeta[@level='product']/titleGroup/title", MetadataField.FIELD_JOURNAL_TITLE);
//  xpathMap.put("/component/header/publicationMeta[@level='product']/issn[@type='print']", MetadataField.FIELD_ISSN);
//  xpathMap.put("/component/header/publicationMeta[@level='product']/issn[@type='electronic']", MetadataField.FIELD_EISSN);
//  xpathMap.put("/component/header/publicationMeta[@level='product']/idGroup/id[@type='product']/@value", MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
//  xpathMap.put("/component/header/publicationMeta[@level='part']/numberingGroup/numbering[@type='journalVolume']", MetadataField.FIELD_VOLUME);
//  xpathMap.put("/component/header/publicationMeta[@level='part']/numberingGroup/numbering[@type='journalIssue']", MetadataField.FIELD_ISSUE);
//  xpathMap.put("/component/header/publicationMeta[@level='part']/coverDate/@startDate", MetadataField.FIELD_DATE);
//  xpathMap.put("/component/header/publicationMeta[@level='unit']/numberingGroup/numbering[@type='pageFirst']", MetadataField.FIELD_START_PAGE);
//  xpathMap.put("/component/header/publicationMeta[@level='unit']/numberingGroup/numbering[@type='pageLast']", MetadataField.FIELD_END_PAGE);
//  xpathMap.put("/component/header/publicationMeta[@level='unit']/doi", MetadataField.FIELD_DOI);
//  xpathMap.put("/component/header/contentMeta/keywordGroup/keyword", MetadataField.FIELD_KEYWORDS);
//  xpathMap.put("/component/header/publicationMeta[@level='product']/publisherInfo/publisherName", MetadataField.FIELD_PUBLISHER);
//  xpathMap.put("/component/header/contentMeta/creators/creator[@creatorRole='author']/personName", MetadataField.FIELD_AUTHOR);

  // content with title specified
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
            "</keywordGroup>" +
            "<creators>" +
              "<creator creatorRole=\"author\">" +
                "<personName>" +
                  "<givenNames>John</givenNames>" +
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
            "<coverDate startDate=\"" + goodDate + "\">February 2010</coverDate>" +
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
      "</component>";

  // metadata with no title specified
  String noTitleContent =
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
            "</keywordGroup>" +
            "<creators>" +
              "<creator creatorRole=\"author\">" +
                "<personName>" +
                  "<givenNames>John</givenNames>" +
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
            "<idGroup>" +
              "<id type=\"product\" value=\"" + goodPropId + "\" />" +
            "</idGroup>" +
          "</publicationMeta>" +
            
          "<publicationMeta level=\"part\">" +
            "<coverDate startDate=\"" + goodDate + "\">February 2010</coverDate>" +
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
      "</component>";


  public void testExtractFromGoodContent() throws Exception {
    // test with content with title
    String pdfUrl = realUrlBaseA + realPdfUrlA;
    String xmlUrl = realUrlBaseA + realXmlUrlA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, goodContent);
    CachedUrl pdfCu = hau.makeCachedUrl(pdfUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), pdfCu);
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
    assertEquals(goodPropId, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }
  
  public void testExtractFromNoTitleContent() throws Exception {
    // test with content with no title -- uses journalID instead
    String pdfUrl = realUrlBaseA + realPdfUrlB;
    String xmlUrl = realUrlBaseA + realXmlUrlB;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, noTitleContent);
    CachedUrl pdfCu = hau.makeCachedUrl(pdfUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), pdfCu);
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
    assertEquals("UNKNOWN_TITLE/issn=" + goodPrintIssn, 
                 md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodPropId, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }
  
  public void testExtractFromGoodContentAndNoTitle() throws Exception {
    // test with content with title
    String pdfUrl = realUrlBaseA + realPdfUrlA;
    String xmlUrl = realUrlBaseA + realXmlUrlA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, goodContent);
    CachedUrl pdfCu = hau.makeCachedUrl(pdfUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), pdfCu);
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
    assertEquals(goodPropId, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));

    // test with no title content following content with title
    pdfUrl = realUrlBaseA + realPdfUrlB;
    xmlUrl = realUrlBaseA + realXmlUrlB;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, noTitleContent);
    pdfCu = hau.makeCachedUrl(pdfUrl);
    mle = new FileMetadataListExtractor(me);
    mdlist = mle.extract(MetadataTarget.Any(), pdfCu);
    assertNotEmpty(mdlist);
    md = mdlist.get(0);
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
    assertEquals(goodPropId, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }

  // empty content
  String emptyContent =
      "<component>" +
      "</component>";


  public void testExtractFromEmptyContent() throws Exception {
    // test empty content in "A" directory with no journal ID prefix
    String pdfUrl = realUrlBaseA + realPdfUrlA;
    String xmlUrl = realUrlBaseA + realXmlUrlA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, emptyContent);
    CachedUrl pdfCu = hau.makeCachedUrl(pdfUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), pdfCu);
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
    assertEquals("UNKNOWN_TITLE/journalId=" + goodPropId, 
        md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));

    // test empty content in "0" directory with no journal ID prefix
    pdfUrl = realUrlBase0 + realPdfUrlA;
    xmlUrl = realUrlBase0 + realXmlUrlA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, emptyContent);
    pdfCu = hau.makeCachedUrl(pdfUrl);
    mle = new FileMetadataListExtractor(me);
    mdlist = mle.extract(MetadataTarget.Any(), pdfCu);
    assertEmpty(mdlist);
}

  // bad content -- not XML
  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodDescription + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    // test bad content in "A" directory with journal ID prefix
    String pdfUrl = realUrlBaseA + realPdfUrlA;
    String xmlUrl = realUrlBaseA + realXmlUrlA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, badContent);
    CachedUrl pdfCu = hau.makeCachedUrl(pdfUrl);
    FileMetadataExtractor me =
      new WileyMetadataExtractorFactory.WileyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), pdfCu);
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
    assertEquals("UNKNOWN_TITLE/journalId=" + goodPropId, 
        md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));

    // test bad content in "0" directory with no journal ID prefix
    pdfUrl = realUrlBase0 + realPdfUrlA;
    xmlUrl = realUrlBase0 + realXmlUrlA;
    hau.addUrl(pdfUrl);
    hau.addUrl(xmlUrl, badContent);
    pdfCu = hau.makeCachedUrl(pdfUrl);
    mle = new FileMetadataListExtractor(me);
    mdlist = mle.extract(MetadataTarget.Any(), pdfCu);
    assertEmpty(mdlist);
 }
  
}