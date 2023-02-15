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

package org.lockss.plugin.clockss.stanforduniversitypress;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class TestSUPressOnix2XmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestSUPressOnix2XmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.stanforduniversitypress.ClockssSUPressOnix2BooksZipSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static String ZIP_BASE = BASE_URL + "SUP.zip!/testdir/"; 


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
    
    /* must set up plugin to get helper name */
    /*
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        PLUGIN_NAME);
    mau.setPlugin(ap);
    */

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
    conf.put("year", "2012");
    return conf;
  }
  
  //This is probably invalid ONIX for Books. It has header info, but no products (articles)
 private static final String noArticleXML = 
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
 "<!DOCTYPE ONIXMessage SYSTEM \"http://www.editeur.org/onix/2.1/reference/onix-international.dtd\">" +
 "<ONIXMessage>" +
  "<Header>" +
   "<FromCompany>BiblioVault</FromCompany>" +
   "<FromPerson>BiblioVault Staff</FromPerson>" +
   "<FromEmail>bv-help@uchicago.edu</FromEmail>" +
   "<SentDate>20131022</SentDate>" +
   "<DefaultLanguageOfText>eng</DefaultLanguageOfText>" +
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

   FileMetadataExtractor me = new SUPressOnixXmlMetadataExtractorFactory().createFileMetadataExtractor(
       MetadataTarget.Any(), "text/xml");
     FileMetadataListExtractor mle =
         new FileMetadataListExtractor(me);
     List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
     assertEmpty(mdlist);
 }

 
  private static final String realXMLFile = "Onix2BooksSourceTest.xml";

  public void testFromOnix2BooksXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();   
      // mock up coming from a zip arhive
      String xml_url = ZIP_BASE + "TestXML.xml";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");

      // Now add all the pdf files in our AU since we check for them before emitting
      mau.addUrl(ZIP_BASE + "9780804738555.pdf", true, true, xmlHeader);
      mau.addUrl(ZIP_BASE + "9780804741666.pdf", true, true, xmlHeader);
      mau.addUrl(ZIP_BASE + "9780804744888.pdf", true, true, xmlHeader);
      // do not add the PDF with this id so it doesn't get emitted: 9780804741777 n

    FileMetadataExtractor me = new SUPressOnixXmlMetadataExtractorFactory().createFileMetadataExtractor(
        MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      // 1,2,4 (3rd record has no associated content file)
      assertEquals(3, mdlist.size());

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
  private static final int ARTICLE_TITLE = 3;
  private static final int ARTICLE_DATE = 4;
  
  private static final String zipPdf1 = ZIP_BASE + "9780804738555.pdf";
  private static final ArrayList md1 = (ArrayList) ListUtil.list(
      "9780804738555",
      null, //no doi
      "[Writer, David]",
      "On Demand, A Subtitle to go with On Demand",
      "2009-12-03");

  private static final String zipPdf2 = ZIP_BASE + "9780804741666.pdf";
  private static final ArrayList md2 = (ArrayList) ListUtil.list(
      "9780804741666",
      null,// no doi
      "[Author, Good, Editor, Mark, Editor, Another, Translator, Lingua]",
      "The People, 1881 to 1941",
      "2013-01");    // sends another valid address type

  private static final String zipPdf3 = ZIP_BASE + "9780804744888.pdf";
  private static final ArrayList md3 = (ArrayList) ListUtil.list(
      "9780804744888",
      null,
      "[van Writer, Martin]",
      "Philip Whosit, Ideals in his World",
      "2012");
  
  private static final String noContentisbn = "9780804741777"; //shouldn't have emitted
  
  static private final Map<String, List> expectedMD =
      new HashMap<String,List>();
  static {
    expectedMD.put("9780804738555", md1);
    expectedMD.put("9780804741666", md2);
    expectedMD.put("9780804744888", md3);
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
    assertEquals(AM.get(MetadataField.FIELD_DATE),expected.get(ARTICLE_DATE));
  }
  
}
