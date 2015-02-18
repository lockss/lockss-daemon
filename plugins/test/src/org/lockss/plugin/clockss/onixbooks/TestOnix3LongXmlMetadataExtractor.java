/*
 * $Id$
 */
/*

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.onixbooks;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.onixbooks.Onix3LongSourceXmlMetadataExtractorFactory;


public class TestOnix3LongXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestOnix3LongXmlMetadataExtractor.class);

 // private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.onixbooks.ClockssOnix3BooksSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";

  public void setUp() throws Exception {
    super.setUp();

    mau = new MockArchivalUnit();
    mau.setConfiguration(auConfig());

  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2012");
    return conf;
  }
  
  //This is probably invalid ONIX for Books. It has header info, but no products (articles)
 private static final String noArticleXML = 
     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
  "<ONIXMessage release=\"3.0\">" +
  "<Header>" +
  "<Sender>" +
  "<SenderName>Test Content Group</SenderName>" +
  "<ContactName>CoreSource</ContactName>" +
  "<EmailAddress>partnersupport@test.com</EmailAddress>" +
  "</Sender>" +
  "<Addressee>" +
  "<AddresseeName>CLOCKSS</AddresseeName>" +
  "</Addressee>" +
  "<SentDateTime>20130701T080307Z</SentDateTime>" +
  "</Header>" +
  "</ONIXMessage>";
 
 public void testFromNoArticleXMLFile() throws Exception {
   

     CIProperties xmlHeader = new CIProperties();    
     String xml_url = "http://www.source.com/TestXML.xml";
     xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
     MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
     
     mcu.setContent(noArticleXML);
     mcu.setContentSize(noArticleXML.length());
     mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

   FileMetadataExtractor me = new Onix3LongSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
     FileMetadataListExtractor mle =
         new FileMetadataListExtractor(me);
     List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
     assertEmpty(mdlist);
 }

 // An XML snippet to test a specific issue as needed.
 private static final String XMLsnippet = 
     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
         "<ONIXMessage release=\"3.0\">" +
         "<Header>" +
         "<Sender>" +
         "<SenderName>Test Content Group</SenderName>" +
         "<ContactName>CoreSource</ContactName>" +
         "<EmailAddress>partnersupport@test.com</EmailAddress>" +
         "</Sender>" +
         "<Addressee>" +
         "<AddresseeName>CLOCKSS</AddresseeName>" +
         "</Addressee>" +
         "<SentDateTime>20130701T080307Z</SentDateTime>" +
         "</Header>" +
         "<Product>" + 
         " <RecordReference>7780857450449</RecordReference>" +
         "<ProductIdentifier>  " +
         "<ProductIDType>15</ProductIDType> " + 
         "<IDValue>7780857450449</IDValue>  " +
         "</ProductIdentifier>  " +
         "<DescriptiveDetail> " +
         "<NoCollection/> " +
         "<TitleDetail>   " +
         "<TitleType>01</TitleType> " +
         "<TitleElement>  " +
         "<TitleElementLevel>01</TitleElementLevel> " +
         "<TitlePrefix>The</TitlePrefix>  " +
         "<TitleWithoutPrefix>1911/12 Book Title</TitleWithoutPrefix>  " +
         "</TitleElement> " +
         "</TitleDetail>" +
         "</DescriptiveDetail>" +
         "<PublishingDetail>" +
         "<Imprint> <ImprintName>Berghahn Books</ImprintName></Imprint>" +
         "<PublishingDate><PublishingDateRole>01</PublishingDateRole>" +
         "<Date dateformat=\"00\">20110415</Date></PublishingDate>" +
         "</PublishingDetail>" +
         "</Product>" +
         "</ONIXMessage>";
 
 public void testFromXMLSnippet() throws Exception {
   

     CIProperties xmlHeader = new CIProperties();    
     String xml_url = "http://www.source.com/TestXML.xml";
     xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
     MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
     mau.addUrl("http://www.source.com/7780857450449.pdf", true, true, xmlHeader);
     
     mcu.setContent(XMLsnippet);
     mcu.setContentSize(XMLsnippet.length());
     mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

   FileMetadataExtractor me = new Onix3LongSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
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
     assertEquals(mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE), "The 1911/12 Book Title");
 }

 private static final String realXMLFile = "Onix3BooksSourceTest.xml";

  public void testFromOnixBooksXMLFile() throws Exception {
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
      mau.addUrl(pdfUrl2, true, true, xmlHeader);
      mau.addUrl(pdfUrl3, true, true, xmlHeader);
      mau.addUrl(epubUrl5, true, true, xmlHeader);
      mau.addUrl(pdfUrl6, true, true, xmlHeader);
      
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

    FileMetadataExtractor me = new  Onix3LongSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      // 1,2,3,5,6 (4 has no associated content file)
      assertEquals(5, mdlist.size());

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
 
  private static final int ISBN_INDEX = 0;
  private static final int DOI_INDEX = 1;
  private static final int AUTHOR_INDEX = 2;
  private static final int JOURNAL_TITLE = 3;
  private static final int ARTICLE_DATE = 4;
  
  private static final String pdfUrl1 = "http://www.source.com/9781606501260.pdf";
  private static final ArrayList md1 = (ArrayList) ListUtil.list(
      "9781606501260",
      "10.5643/9781606501260",
      "[Author, David W.]",
      "Book Title One: A Test for JUnit",
      "2009-10-26");

  private static final String pdfUrl2 = "http://www.source.com/9780857458510.pdf";
  private static final String epubUrl2 = "http://www.source.com/9780857458510.epub";
  private static final ArrayList md2 = (ArrayList) ListUtil.list(
      "9780857458510",
      null,// no doi
      "[Author-Name, Sally von, Editor, Fernanda]",
      "A Second Book Title: Perspectives",
      "2011-06-15");

  private static final String pdfUrl3 = "http://www.source.com/9781847699110.pdf";
  private static final ArrayList md3 = (ArrayList) ListUtil.list(
      "9781847699110",
      null,
      "[BWriter, Mary]",
      "English Title of Book: From A to Z",
      "2013-02-05");
  
  private static final String noContentisbn = "1111111111111"; //shouldn't have emitted
  
  private static final String epubUrl5 = "http://www.source.com/2222222222222.epub";
  private static final ArrayList md5 = (ArrayList) ListUtil.list(
      "2222222222222",
      null,
      "[Smith, Lurlene]",
      "Book With EPUB Content",
      "2012-01-01");  
  
  private static final String pdfUrl6 = "http://www.source.com/9998887776665.pdf";
  private static final ArrayList md6 = (ArrayList) ListUtil.list(
      "9998887776665",
      null,
      "[Scholar, Simone]",
      "The Big Journey: Captivity and the Holocaust",
      "2009-07-15");  

  static private final Map<String, List> expectedMD =
      new HashMap<String,List>();
  static {
    expectedMD.put("9781606501260", md1);
    expectedMD.put("9780857458510", md2);
    expectedMD.put("9781847699110", md3);
    expectedMD.put("2222222222222", md5);
    expectedMD.put("9998887776665", md6);
  }
  

  // Not yet able to test which facet formats are available
  // Not yet able to test which KIND of thing it iss (book, book series, etc)
  
  private void compareMetadata(ArticleMetadata AM) {
    String isbn = AM.get(MetadataField.FIELD_ISBN);
    assertNotEquals(noContentisbn, isbn); //no associated file; shouldn't have emitted
    ArrayList expected = (ArrayList) expectedMD.get(isbn);
    
    assertNotNull(expected);
    assertEquals(AM.get(MetadataField.FIELD_DOI), expected.get(DOI_INDEX));
    assertEquals(AM.getList(MetadataField.FIELD_AUTHOR).toString(), expected.get(AUTHOR_INDEX));
    assertEquals(AM.get(MetadataField.FIELD_PUBLICATION_TITLE), expected.get(JOURNAL_TITLE));
    assertEquals(AM.get(MetadataField.FIELD_DATE), expected.get(ARTICLE_DATE));
    
  }
}
