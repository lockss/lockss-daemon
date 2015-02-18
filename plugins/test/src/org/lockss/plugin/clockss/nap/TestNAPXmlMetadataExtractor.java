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

package org.lockss.plugin.clockss.nap;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestNAPXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestNAPXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  // PLUGIN_NAME = "org.lockss.plugin.clockss.nap.ClockssNAPBooksSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  
  /*
   * Set up the metadata expected for each of the above tests
   */
  private static final String pdfUrl1 = "http://www.source.com/10001.stamped.pdf";

  private static CIProperties xmlHeader = new CIProperties();
  private static String xml_url = "http://www.source.com/10001.xml";
  private MockCachedUrl mcu;
  private FileMetadataExtractor me;
  private FileMetadataListExtractor mle;

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

    // the following is consistent across all tests; only content changes
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    mau.addUrl(pdfUrl1, true, true, xmlHeader);

    me = new NAPSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
    mle = new FileMetadataListExtractor(me);
    
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
    conf.put("year", "2013");
    return conf;
  }
 
  private static final String realXMLFile = "NAPSourceTest.xml";

  private static final String GOOD_TITLE = "Advanced Stuff for Future Wizards, Gremlins, and Physicists: Seventh Lecture International Dark Arts Series";
  private static final String GOOD_DATE =  "2000-11-28";
  private static final String COPY_YEAR =  "2000";
  // a corporate author
  private static final String GOOD_AUTHOR = "Arthur P. Somebody, University of California at Hogwarts, Organized by the National Research Council and the Office of the Dark Arts";
  private static final String GOOD_PUBLISHER = "National Academies Press"; 
  private static final String FLAT_ISBN =       "0111111110";
  private static final String ISBN13 =       "111-0-309-51287-9";
  private static final String BAD_ISBN = "NI000909"; //used with old content sometimes
 
  //TODO - 
  // add a test to check the splitting of an author
  // add a test to check title: subtitle permutations
  
  public void testFromNAPXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      // set up the content for this test
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());

      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(1, mdlist.size());
      ArticleMetadata mdRecord = mdlist.get(0);
      assertNotNull(mdRecord);

      assertEquals(ISBN13, mdRecord.get(MetadataField.FIELD_ISBN));
      assertEquals(GOOD_TITLE, mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GOOD_DATE, mdRecord.get(MetadataField.FIELD_DATE));
      assertEquals(GOOD_AUTHOR,mdRecord.get(MetadataField.FIELD_AUTHOR));
    }finally {
      IOUtil.safeClose(file_input);
    }

  }
  
  /*
   * XML snippets which can be built up for specific cases
   */
  private static final String nap_xml_start = 
      "<book>\n" +
          "<record_id>10001</record_id>\n" +
          "<title>Advanced Stuff for Future Wizards, Gremlins, and Physicists:</title>\n" +
          "<author>Arthur P. Somebody, University of California at Hogwarts, Organized by the National Research Council and the Office of the Dark Arts</author>\n" +
          "<page_count>20</page_count>\n" +
          "<copyright>2000</copyright>\n" +
          "<alt_title></alt_title>\n" +
          "<subtitle>Seventh Lecture International Dark Arts Series</subtitle>\n";

  private static final String nap_valid_flat_isbn =
      "<flat_isbn>0111111110</flat_isbn>\n";

  private static final String nap_invalid_flat_isbn =
      "<flat_isbn>NI000909</flat_isbn>\n";

  private static final String nap_display_date =
      "<display_date>\n" + 
          "<year>2000</year>\n" +
          "<month>11</month>\n" +
          "<day>28</day>\n" +
          "<epoch>975387600</epoch>\n" +
          "</display_date>\n";

  private static final String nap_pdf_book_item = 
      "<product><item>\n" +
          "<type>pdf_book</type>\n";

  private static final String nap_book_item = 
      "<product><item>\n" +
          "<type>book</type>\n";

  private static final String nap_item_contents =
      "<isbn>0-111-51287-5</isbn>\n" +
          "<isbn13>111-0-309-51287-9</isbn13>\n" +
          "<price>11.50</price>\n" +
          "<binding>pdfb</binding>\n" +
          "<productID>10001-0-309-XXXXX-5</productID>\n" +
          "<for_sale>on</for_sale>\n" +
          "<free>on</free>\n" +
          "<free_start_date>2005-02-24</free_start_date>\n" +
          "<free_end_date></free_end_date>\n" +
          "<word>PDF Full Book</word>\n" +
          "<catalog_word>PDF BOOK</catalog_word>\n" +
          "<bind_type>electronic</bind_type>\n" +

  "</item>\n";

  private static final String nap_xml_end =
      "</product></book>";

  public void testNoPdfBookItem() throws Exception {

    String xml_string = nap_xml_start + nap_valid_flat_isbn + nap_display_date + nap_book_item + nap_item_contents + nap_xml_end;
    mcu.setContent(xml_string);
    mcu.setContentSize(xml_string.length());
    
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    assertEquals(1, mdlist.size());
    ArticleMetadata mdRecord = mdlist.get(0);
    assertNotNull(mdRecord);
    assertEquals(FLAT_ISBN, mdRecord.get(MetadataField.FIELD_ISBN));
  }
  
  public void testNoValidISBN() throws Exception {

    String xml_string = nap_xml_start + nap_invalid_flat_isbn + nap_display_date + nap_book_item + nap_item_contents + nap_xml_end;
    mcu.setContent(xml_string);
    mcu.setContentSize(xml_string.length());
    
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    assertEquals(1, mdlist.size());
    ArticleMetadata mdRecord = mdlist.get(0);
      assertNotNull(mdRecord);
      assertEquals(null, mdRecord.get(MetadataField.FIELD_ISBN));
  }
  
  public void testNoDisplayDate() throws Exception {

    String xml_string = nap_xml_start + nap_invalid_flat_isbn + nap_book_item + nap_item_contents + nap_xml_end;
    mcu.setContent(xml_string);
    mcu.setContentSize(xml_string.length());
    
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    assertEquals(1, mdlist.size());
    ArticleMetadata mdRecord = mdlist.get(0);
      assertNotNull(mdRecord);
      assertEquals(COPY_YEAR, mdRecord.get(MetadataField.FIELD_DATE));
  }
  
}
