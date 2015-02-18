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

package org.lockss.plugin.clockss.knowledge;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestKnowlegeXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestKnowlegeXmlMetadataExtractor.class);

  // private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.clockss.knowledge.ClockssKnowledgeUnlatchedBooksSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static final CIProperties xmlHeader = new CIProperties();
  private static final String TEST_FILENAME = "1234567899999";
  private static final String xml_url = "http://www.source.com/" + TEST_FILENAME + ".xml";
  private static final String pdfUrl1 = "http://www.source.com/" + TEST_FILENAME + ".pdf";
  
  private MockCachedUrl mcu;

  public void setUp() throws Exception {
    super.setUp();

    mau = new MockArchivalUnit();
    mau.setConfiguration(auConfig());
    // consistent across all tests
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    // add a pdf, since we check for it before emitting
    mau.addUrl(pdfUrl1, true, true, xmlHeader);
    
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




  private static final String ISBN_1 = "9876543212345";
  private static final String ISBN_OTHER = "9876543215555 (cloth : alk. paper)";
  private static final String AUTHOR_1 = "Public, John Q.";
  private static final String AUTHOR_2 = "Writer, Susan";
  private static final String AUTHOR_3 = "Emerson, Ralph Waldo";
  private static final String TITLE_1 = "A Big Journey";  
  private static final String SUBTITLE_1 = "Testing the result";
  private static final String DATE_1 = "2013";
  private static final String WONKY_DATE = "Jun-14";
  private static final String PUB_1 = "Publishing, Inc.";
  private static final String PROVIDER = "Knowledge Unlatched";

  private static final String XML_FRAME_START =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
          "<collection>" +
          "<record>";
  private static final String XML_FRAME_END =
      "</record>" +
          "</collection>";
  private static final String END_FIELD_BLOCK=
      "</subfield> </datafield>";

  public void testBasicXml1() throws Exception
  {
    String xml_string =  XML_FRAME_START + 
        isbnFB() + ISBN_1 + END_FIELD_BLOCK +
        author100FB() + AUTHOR_1 + END_FIELD_BLOCK +
        titleFB() + TITLE_1 + addSubFieldBlock("b") + SUBTITLE_1 + END_FIELD_BLOCK +
        pubFB() + PUB_1 + addSubFieldBlock("c") + DATE_1 + END_FIELD_BLOCK +
        XML_FRAME_END;
    ArticleMetadata mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(ISBN_1,mdRecord.get(MetadataField.FIELD_ISBN));
    assertEquals(TITLE_1 + " : " + SUBTITLE_1,mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(DATE_1,mdRecord.get(MetadataField.FIELD_DATE));
    assertEquals(PROVIDER, mdRecord.get(MetadataField.FIELD_PROVIDER));
  }
  
  public void testISBN_issues() throws Exception
  {
    //1. two isbns, one is for some other format
    String xml_string =  XML_FRAME_START + 
        isbnFB() + ISBN_1 + END_FIELD_BLOCK +
        isbnFB() + ISBN_OTHER + END_FIELD_BLOCK +
        XML_FRAME_END;
    ArticleMetadata mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(ISBN_1,mdRecord.get(MetadataField.FIELD_ISBN));
    
    //2.isbn has e-book parenthetic; just strip and use anyway
    xml_string = XML_FRAME_START + 
        isbnFB() + ISBN_1 + " (e-book)" + END_FIELD_BLOCK +
        XML_FRAME_END;
    mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(ISBN_1,mdRecord.get(MetadataField.FIELD_ISBN));
    
    //3. no valid isbn13 at tag=a, so should post-cook to filename
    xml_string = XML_FRAME_START + 
        isbnFB() + "0123456789" + END_FIELD_BLOCK +
        XML_FRAME_END;
    mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(TEST_FILENAME,mdRecord.get(MetadataField.FIELD_ISBN));
    assertEquals(PROVIDER, mdRecord.get(MetadataField.FIELD_PROVIDER));
    
  }
  
  public void testTitle_issues() throws Exception
  {
    //1. Title & Subtitle with separation punctuation included
    String xml_string =  XML_FRAME_START + 
        titleFB() + TITLE_1 + " : " + addSubFieldBlock("b") + SUBTITLE_1 + END_FIELD_BLOCK +
        XML_FRAME_END;
    ArticleMetadata mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(TITLE_1 + " : " + SUBTITLE_1,mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE));
    
    //2.Title & Subtitle with trailing /
    xml_string = XML_FRAME_START + 
        titleFB() + TITLE_1 + addSubFieldBlock("b") + SUBTITLE_1 + "    /" + END_FIELD_BLOCK +
        XML_FRAME_END;
    mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(TITLE_1 + " : " + SUBTITLE_1,mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE));
    
    //3.Title, no subtitle with trailing /
    xml_string = XML_FRAME_START + 
        titleFB() + TITLE_1 + " / " + END_FIELD_BLOCK +
        XML_FRAME_END;
    mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(TITLE_1,mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(PROVIDER, mdRecord.get(MetadataField.FIELD_PROVIDER));
  }
  
  public void testDate_issues() throws Exception
  {
    //1. Date with trailing "."
    String xml_string =  XML_FRAME_START +
        pubFB() + PUB_1 + addSubFieldBlock("c") + DATE_1 + "." + END_FIELD_BLOCK +
        XML_FRAME_END;
    ArticleMetadata mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(DATE_1,mdRecord.get(MetadataField.FIELD_DATE));
    
    //2.Date enclosed in []
    xml_string = XML_FRAME_START + 
        pubFB() + PUB_1 + addSubFieldBlock("c") + "[" + DATE_1 + "]" + END_FIELD_BLOCK +
        XML_FRAME_END;
    mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(DATE_1,mdRecord.get(MetadataField.FIELD_DATE));
    
    //3.Both
    xml_string = XML_FRAME_START + 
        pubFB() + PUB_1 + addSubFieldBlock("c") + "[" + DATE_1 + "] ." + END_FIELD_BLOCK +
        XML_FRAME_END;
    mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(DATE_1,mdRecord.get(MetadataField.FIELD_DATE));
    
    //4. wonky date - keep as is
    xml_string = XML_FRAME_START + 
        pubFB() + PUB_1 + addSubFieldBlock("c") + WONKY_DATE + END_FIELD_BLOCK +
        XML_FRAME_END;
    mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(WONKY_DATE,mdRecord.get(MetadataField.FIELD_DATE));   
    assertEquals(PROVIDER, mdRecord.get(MetadataField.FIELD_PROVIDER));
  }
  

  /* 
   * Authors can show up in several different types of datablocks
   * There are also lots of issues with trailing  ","
   */
  public void testAuthor_issues() throws Exception
  {
    //1. Single author, 100 field, trailing ","
    String xml_string =  XML_FRAME_START + 
        author100FB() + AUTHOR_1 + ", " + END_FIELD_BLOCK +
        XML_FRAME_END;
    ArticleMetadata mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    assertEquals(AUTHOR_1,mdRecord.get(MetadataField.FIELD_AUTHOR));
    
    //2.Multiple authors, 700 field, some trailing ","
    xml_string = XML_FRAME_START + 
        author700FB() + AUTHOR_1 + ", " + END_FIELD_BLOCK +
        author700FB() + AUTHOR_2 + " ," + ", " + END_FIELD_BLOCK +
        author700FB() + AUTHOR_3 + ", " + END_FIELD_BLOCK +
        XML_FRAME_END;
    mdRecord = getMDFromXMLString(xml_string);
    printMetadata(mdRecord);
    List<String> authors = mdRecord.getList(MetadataField.FIELD_AUTHOR);
    assertEquals(AUTHOR_1, authors.get(0));
    assertEquals(AUTHOR_2, authors.get(1));
    assertEquals(AUTHOR_3, authors.get(2));
    assertEquals(PROVIDER, mdRecord.get(MetadataField.FIELD_PROVIDER));
  }
  
  

  /*
   * The private routines used to support the testing
   */

  private void printMetadata(ArticleMetadata AM) {

    assertNotNull(AM);
    log.debug3(AM.ppString(2));
  }
  
  /*
   * Testing XML format that will only ever create ONE md record
   */
  public ArticleMetadata getMDFromXMLString(String xml) throws Exception  {
      mcu.setContent(xml);
      mcu.setContentSize(xml.length());

      FileMetadataExtractor me = new  
          KnowledgeUnlatchedSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(1, mdlist.size());
      return mdlist.get(0);
    }


  /*
   * datafield@tag=20; subfield@tag=a
   * might have more than one
   */
  private String isbnFB() {
    return setUpFieldBlock("020", " ", " ", "a");
  }
  private String author700FB() {
    return setUpFieldBlock("700", "1", " ", "a");
  }
  private String author100FB() {
    return setUpFieldBlock("100", "1", " ", "a");
  }
  private String pubFB() {
    return setUpFieldBlock("264", " ", "1", "b");
  }
  private String titleFB() {
    return setUpFieldBlock("245", " ", " ", "a");
  }

  /*
   * Set up the beginning portion of a datafield block, using the parameters given
   */
  private String setUpFieldBlock(String df_tag, String df_ind1, String df_ind2, String sf_code) {
    return "<datafield tag=\"" + df_tag + "\" ind1=\"" + df_ind1 + "\" ind2=\"" + df_ind2 + "\">" +
        "<subfield code=\"" + sf_code + "\">";
  }
  /*
   * Add additional subfields to an existing datafield block
   */
  private String addSubFieldBlock(String sf_code) {
    return "</subfield><subfield code=\"" + sf_code + "\">";
  }
}