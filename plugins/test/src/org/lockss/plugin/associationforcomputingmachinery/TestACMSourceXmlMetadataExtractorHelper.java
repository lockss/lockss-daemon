/* $Id: TestACMSourceXmlMetadataExtractorHelper.java,v 1.1 2014-01-15 17:35:22 aishizaki Exp $

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

package org.lockss.plugin.associationforcomputingmachinery;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import org.lockss.repository.LockssRepository;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;

/*
 * Test file used to extract metadata:
 *      basic examples to test metadata extraction
 */

public class TestACMSourceXmlMetadataExtractorHelper
  extends LockssTestCase {
  
  static Logger log = 
      Logger.getLogger(TestACMSourceXmlMetadataExtractorHelper.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau; // source au
  private DefinablePlugin ap;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.sourcecontent.com/"; //this is not a real url


  private static final String PLUGIN_NAME =
      "org.lockss.plugin.associationforcomputingmachinery.ClockssAssociationForComputingMachinerySourcePlugin";

  private static final String YEAR = "2010";

  private static final String BASE_URL = 
      "http://clockss-ingest.lockss.org/sourcefiles/acm/dev/"+ YEAR + "/";
  
  private static final String BASIC_CONTENT_FILE_NAME = "test_jats1.xml";
  final static String ARTICLE_BASE = BASE_URL + "8aug2010/TEST-TEST00/";
  final static String pdfUrl = "12-authorname.pdf";
  final static String badpdfUrl = "8aug2010/TEST-BADTEST00/DummyMetadataTest.pdf";
  final static String GOOD_JOURNAL_NAME = "Journal Name";
  final static String GOOD_JOURNAL_ID = "J123";
  final static String GOOD_ARTICLE_ID = "2222222";
  final static String GOOD_ISSN = "0000-0000";
  final static String GOOD_ISBN = "978-1-4503-0995-0";
  final static String GOOD_EISSN = "1111-1111";
  final static String GOOD_VOLUME = "22";
  final static String GOOD_ISSUE = "3";
  final static String GOOD_DATE = "07-26-2010";
  final static String GOOD_TITLE = "Title";
  final static String GOOD_START = "Start";
  final static String GOOD_DOI = "10.1111/1231231.2342342";
  final static String GOOD_LANGUAGE = "Language";
  //final static String goodAuthor = "AuthorName, John A.; LongAuthorName, John B.; EvenLongerAuthorName, John C.; AnotherVeryLongAuthorName, John D.; ProbablyTheLongestAuthorNameInThisBunch, John E.";      
  final static String GOOD_AUTHOR = "AuthorName, John A.";      
  final static String GOOD_JOURNAL_CODE = "Journal Code";
  final static String HARDWIRED_PUBLISHER = "ACM";
  final static ArrayList goodAuthors = (ArrayList) ListUtil.list(
      "AuthorName, John A.",
      "LongAuthorName, John B.",
      "EvenLongerAuthorName, John C.",
      "AnotherVeryLongAuthorName, John D.",
      "ProbablyTheLongestAuthorNameInThisBunch, John E."
      );
   
  private static final String BASIC_PERIODICAL_CONTENT =
    "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"+
    "<periodical ver=\"4.0\" ts=\"12/22/2008\">"+
    "<journal_rec>"+
    "<journal_id>"+GOOD_JOURNAL_ID+"</journal_id>"+
    "<journal_code>"+GOOD_JOURNAL_CODE+"</journal_code>"+
    "<journal_name>"+GOOD_JOURNAL_NAME+"</journal_name>"+
    "<journal_abbr>Journal Abbr</journal_abbr>"+
    "<issn>"+GOOD_ISSN+"</issn>"+
    "<eissn>"+GOOD_EISSN+"</eissn>"+
    "<language>"+GOOD_LANGUAGE+"</language>"+
    "<periodical_type>Periodical</periodical_type>"+
    "<publisher>"+
    "<publisher_id>PUB555</publisher_id>"+
    "<publisher_code>ORLFLA</publisher_code>"+
    "<publisher_name>Publisher</publisher_name>"+
    "<publisher_address>123 Street Rd.</publisher_address>"+
    "<publisher_city>New York</publisher_city>"+
    "<publisher_state>NY</publisher_state>"+
    "<publisher_country>USA</publisher_country>"+
    "<publisher_zip_code>10121-0701</publisher_zip_code>"+
    "<publisher_contact>Fake Name</publisher_contact>"+
    "<publisher_phone>333 444-5555</publisher_phone>"+
    "<publisher_isbn_prefix>ISBN</publisher_isbn_prefix>"+
    "<publisher_url>www.lockss.org</publisher_url>"+
    "</publisher>"+
    "</journal_rec>"+
    "<issue_rec>"+
    "<issue_id>1111111</issue_id>"+
    "<volume>"+GOOD_VOLUME+"</volume>"+
    "<issue>"+GOOD_ISSUE+"</issue>"+
    "<issue_date>July 2010</issue_date>"+
    "<publication_date>"+GOOD_DATE+"</publication_date>"+
    "<special_issue_title><![CDATA[]]></special_issue_title>"+
    "<front_matter>"+
    "<fm_file>frontmatter.pdf</fm_file>"+
    "<fm_text>Front Matter Metadata</fm_text>"+
    "</front_matter>"+
    "</issue_rec>"+
    "<section>"+
    "<article_rec>"+
    "<article_id>"+GOOD_ARTICLE_ID+"</article_id>"+
    "<sort_key>44</sort_key>"+
    "<display_label>a</display_label>"+
    "<display_no>55</display_no>"+
    "<article_publication_date>"+GOOD_DATE+"</article_publication_date>"+
    "<seq_no>1</seq_no>"+
    "<title><![CDATA[Title]]></title>"+
    "<subtitle></subtitle>"+
    "<page_from>Start</page_from>"+
    "<page_to>1</page_to>"+
    "<doi_number>"+GOOD_DOI+"</doi_number>"+
    "<url></url>"+
    "<foreign_title></foreign_title>"+
    "<foreign_subtitle></foreign_subtitle>"+
    "<language>Language</language>"+
    "<categories>"+
    "<primary_category>"+
    "<cat_node/>"+
    "<descriptor/>"+
    "<type/>"+
    "</primary_category>"+
    "</categories>"+
    "<authors>"+
    "<au>"+
    "<person_id>PERSONID</person_id>"+
    "<seq_no>1</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[A.]]></middle_name>"+
    "<last_name><![CDATA[AuthorName]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Writer]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+
    "<au>"+
    "<person_id>PERSONID2</person_id>"+
    "<seq_no>2</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[B.]]></middle_name>"+
    "<last_name><![CDATA[LongAuthorName]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Editor]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+
    "<au>"+
    "<person_id>PERSONID3</person_id>"+
    "<seq_no>3</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[C.]]></middle_name>"+
    "<last_name><![CDATA[EvenLongerAuthorName]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Writer]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+
    "<au>"+
    "<person_id>PERSONID4</person_id>"+
    "<seq_no>4</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[D.]]></middle_name>"+
    "<last_name><![CDATA[AnotherVeryLongAuthorName]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Writer]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+
    "<au>"+
    "<person_id>PERSONID5</person_id>"+
    "<seq_no>5</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[E.]]></middle_name>"+
    "<last_name><![CDATA[ProbablyTheLongestAuthorNameInThisBunch]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Writer]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+  
    "</authors>"+
    "<fulltext>"+
    "<file>"+
    "<seq_no>1</seq_no>"+
    "<fname>"+pdfUrl+"</fname>"+
    "</file>"+
    "</fulltext>"+
    "<ccc>"+
    "<copyright_holder>"+
    "<copyright_holder_name>XXX</copyright_holder_name>"+
    "<copyright_holder_year>2012</copyright_holder_year>"+
    "</copyright_holder>"+
    "</ccc>"+
    "</article_rec>"+
    "</section>"+
    "</periodical>";
  
  // the following content does not have the "section" level -
  // should work with and without
  private static final String BASIC_PROCEEDING_CONTENT =
    "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"+
    "<proceeding ver=\"4.0\" ts=\"12/22/2008\">"+
    "<proceeding_rec>"+
    "<proc_id>"+GOOD_JOURNAL_ID+"</proc_id>"+
    "<acronym>"+GOOD_JOURNAL_CODE+"</acronym>"+
    "<proc_desc>"+GOOD_JOURNAL_NAME+"</proc_desc>"+
    "<journal_abbr>Journal Abbr</journal_abbr>"+
    "<isbn>"+GOOD_ISBN+"</isbn>"+
    "<language>"+GOOD_LANGUAGE+"</language>"+
    "<publisher>"+
    "<publisher_id>PUB555</publisher_id>"+
    "<publisher_code>ORLFLA</publisher_code>"+
    "<publisher_name>Publisher</publisher_name>"+
    "<publisher_address>123 Street Rd.</publisher_address>"+
    "<publisher_city>New York</publisher_city>"+
    "<publisher_state>NY</publisher_state>"+
    "<publisher_country>USA</publisher_country>"+
    "<publisher_zip_code>10121-0701</publisher_zip_code>"+
    "<publisher_contact>Fake Name</publisher_contact>"+
    "<publisher_phone>333 444-5555</publisher_phone>"+
    "<publisher_isbn_prefix>ISBN</publisher_isbn_prefix>"+
    "<publisher_url>www.lockss.org</publisher_url>"+
    "</publisher>"+
    "</proceeding_rec>"+
    "<issue_rec>"+
    "<issue_id>1111111</issue_id>"+
    "<volume>"+GOOD_VOLUME+"</volume>"+
    "<issue>"+GOOD_ISSUE+"</issue>"+
    "<issue_date>July 2010</issue_date>"+
    "<publication_date>"+GOOD_DATE+"</publication_date>"+
    "<special_issue_title><![CDATA[]]></special_issue_title>"+
    "<front_matter>"+
    "<fm_file>frontmatter.pdf</fm_file>"+
    "<fm_text>Front Matter - Dummy Metadata</fm_text>"+
    "</front_matter>"+
    "</issue_rec>"+
   // "<section>"+
    "<article_rec>"+
    "<article_id>"+GOOD_ARTICLE_ID+"</article_id>"+
    "<sort_key>44</sort_key>"+
    "<display_label>a</display_label>"+
    "<display_no>55</display_no>"+
    "<article_publication_date>"+GOOD_DATE+"</article_publication_date>"+
    "<seq_no>1</seq_no>"+
    "<title><![CDATA[Title]]></title>"+
    "<subtitle></subtitle>"+
    "<page_from>Start</page_from>"+
    "<page_to>1</page_to>"+
    "<doi_number>"+GOOD_DOI+"</doi_number>"+
    "<url></url>"+
    "<foreign_title></foreign_title>"+
    "<foreign_subtitle></foreign_subtitle>"+
    "<language>Language</language>"+
    "<categories>"+
    "<primary_category>"+
    "<cat_node/>"+
    "<descriptor/>"+
    "<type/>"+
    "</primary_category>"+
    "</categories>"+
    "<authors>"+
    "<au>"+
    "<person_id>PERSONID</person_id>"+
    "<seq_no>1</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[A.]]></middle_name>"+
    "<last_name><![CDATA[AuthorName]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Writer]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+
    "<au>"+
    "<person_id>PERSONID2</person_id>"+
    "<seq_no>2</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[B.]]></middle_name>"+
    "<last_name><![CDATA[LongAuthorName]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Editor]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+
    "<au>"+
    "<person_id>PERSONID3</person_id>"+
    "<seq_no>3</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[C.]]></middle_name>"+
    "<last_name><![CDATA[EvenLongerAuthorName]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Writer]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+
    "<au>"+
    "<person_id>PERSONID4</person_id>"+
    "<seq_no>4</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[D.]]></middle_name>"+
    "<last_name><![CDATA[AnotherVeryLongAuthorName]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Writer]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+
    "<au>"+
    "<person_id>PERSONID5</person_id>"+
    "<seq_no>5</seq_no>"+
    "<first_name><![CDATA[John]]></first_name>"+
    "<middle_name><![CDATA[E.]]></middle_name>"+
    "<last_name><![CDATA[ProbablyTheLongestAuthorNameInThisBunch]]></last_name>"+
    "<suffix><![CDATA[]]></suffix>"+
    "<affiliation><![CDATA[Writer]]></affiliation>"+
    "<role><![CDATA[Author]]></role>"+
    "</au>"+  
    "</authors>"+
    "<fulltext>"+
    "<file>"+
    "<seq_no>1</seq_no>"+
    "<fname>"+pdfUrl+"</fname>"+
    "</file>"+
    "</fulltext>"+
    "<ccc>"+
    "<copyright_holder>"+
    "<copyright_holder_name>XXX</copyright_holder_name>"+
    "<copyright_holder_year>2012</copyright_holder_year>"+
    "</copyright_holder>"+
    "</ccc>"+
    "</article_rec>"+
  //  "</section>"+
    "</proceeding>";
  
  /* the following string has &/unicode content of different types (eg)
   * Wiley & Son, Jim&#233;nez, option=com_content&task=view&id=67,
   * 
  */
  static final String NAME_WITH_UNICODE = "Jim&#233;nez";
  static final String NAME_WITH_AMPER = "A & B";
  static final String AMPER_NEEDS_DTD = 
  "In <i>Proceedings of the 2009 Teragrid Conference</i>. Teragrid, 2009. http://archive.teragrid.org/tg09/index.php?option=com_content\\&task=view\\&id=69.";

  private static final String AMPER_CONTENT =
      "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"+
      "<proceeding ver=\"4.0\" ts=\"12/22/2008\">"+
      "<proceeding_rec>"+
      "<proc_id>"+GOOD_JOURNAL_ID+"</proc_id>"+
      "<acronym>"+GOOD_JOURNAL_CODE+"</acronym>"+
      "<proc_desc>"+NAME_WITH_AMPER+"</proc_desc>"+
      "<journal_abbr>Journal Abbr</journal_abbr>"+
      "<isbn>"+GOOD_ISBN+"</isbn>"+
      "<language>"+GOOD_LANGUAGE+"</language>"+
      "<publisher>"+
      "<publisher_id>PUB555</publisher_id>"+
      "<publisher_code>ORLFLA</publisher_code>"+
      "<publisher_name>Publisher</publisher_name>"+
      "<publisher_address>123 Street Rd.</publisher_address>"+
      "<publisher_city>New York</publisher_city>"+
      "<publisher_state>NY</publisher_state>"+
      "<publisher_country>USA</publisher_country>"+
      "<publisher_zip_code>10121-0701</publisher_zip_code>"+
      "<publisher_contact>"+NAME_WITH_AMPER+"</publisher_contact>"+
      "<publisher_phone>333 444-5555</publisher_phone>"+
      "<publisher_isbn_prefix>ISBN</publisher_isbn_prefix>"+
      "<publisher_url>www.lockss.org</publisher_url>"+
      "</publisher>"+
      "</proceeding_rec>"+
      "<issue_rec>"+
      "<issue_id>1111111</issue_id>"+
      "<volume>"+GOOD_VOLUME+"</volume>"+
      "<issue>"+GOOD_ISSUE+"</issue>"+
      "<issue_date>July 2010</issue_date>"+
      "<publication_date>"+GOOD_DATE+"</publication_date>"+
      "<special_issue_title><![CDATA[]]></special_issue_title>"+
      "<front_matter>"+
      "<fm_file>frontmatter.pdf</fm_file>"+
      "<fm_text>Front Matter - Dummy Metadata</fm_text>"+
      "</front_matter>"+
      "</issue_rec>"+
     // "<section>"+
      "<article_rec>"+
      "<article_id>"+GOOD_ARTICLE_ID+"</article_id>"+
      "<sort_key>44</sort_key>"+
      "<display_label>a</display_label>"+
      "<display_no>55</display_no>"+
      "<article_publication_date>"+GOOD_DATE+"</article_publication_date>"+
      "<seq_no>1</seq_no>"+
      "<title><![CDATA[Title]]></title>"+
      "<subtitle></subtitle>"+
      "<page_from>Start</page_from>"+
      "<page_to>1</page_to>"+
      "<doi_number>"+GOOD_DOI+"</doi_number>"+
      "<url></url>"+
      "<foreign_title></foreign_title>"+
      "<foreign_subtitle></foreign_subtitle>"+
      "<language>Language</language>"+
      "<categories>"+
      "<primary_category>"+
      "<cat_node/>"+
      "<descriptor/>"+
      "<type/>"+
      "</primary_category>"+
      "</categories>"+
      "<authors>"+
      "<au>"+
      "<person_id>PERSONID</person_id>"+
      "<seq_no>1</seq_no>"+
      "<first_name><![CDATA[John]]></first_name>"+
      "<middle_name><![CDATA[A.]]></middle_name>"+
      "<last_name><![CDATA["+ NAME_WITH_UNICODE +"]]></last_name>"+
      "<suffix><![CDATA[]]></suffix>"+
      "<affiliation><![CDATA[Writer]]></affiliation>"+
      "<role><![CDATA[Author]]></role>"+
      "</au>"+
      "<au>"+
      "<person_id>PERSONID2</person_id>"+
      "<seq_no>2</seq_no>"+
      "<first_name><![CDATA[John]]></first_name>"+
      "<middle_name><![CDATA[B.]]></middle_name>"+
      "<last_name><![CDATA[LongAuthorName]]></last_name>"+
      "<suffix><![CDATA[]]></suffix>"+
      "<affiliation><![CDATA[Editor]]></affiliation>"+
      "<role><![CDATA[Author]]></role>"+
      "</au>"+
      "<au>"+
      "<person_id>PERSONID3</person_id>"+
      "<seq_no>3</seq_no>"+
      "<first_name><![CDATA[John]]></first_name>"+
      "<middle_name><![CDATA[C.]]></middle_name>"+
      "<last_name><![CDATA[EvenLongerAuthorName]]></last_name>"+
      "<suffix><![CDATA[]]></suffix>"+
      "<affiliation><![CDATA[Writer]]></affiliation>"+
      "<role><![CDATA[Author]]></role>"+
      "</au>"+
      "<au>"+
      "<person_id>PERSONID4</person_id>"+
      "<seq_no>4</seq_no>"+
      "<first_name><![CDATA[John]]></first_name>"+
      "<middle_name><![CDATA[D.]]></middle_name>"+
      "<last_name><![CDATA[AnotherVeryLongAuthorName]]></last_name>"+
      "<suffix><![CDATA[]]></suffix>"+
      "<affiliation><![CDATA[Writer]]></affiliation>"+
      "<role><![CDATA[Author]]></role>"+
      "</au>"+
      "<au>"+
      "<person_id>PERSONID5</person_id>"+
      "<seq_no>5</seq_no>"+
      "<first_name><![CDATA[John]]></first_name>"+
      "<middle_name><![CDATA[E.]]></middle_name>"+
      "<last_name><![CDATA[ProbablyTheLongestAuthorNameInThisBunch]]></last_name>"+
      "<suffix><![CDATA[]]></suffix>"+
      "<affiliation><![CDATA[Writer]]></affiliation>"+
      "<role><![CDATA[Author]]></role>"+
      "</au>"+  
      "</authors>"+
      "<references>"+
      "<ref>"+
      "<ref_obj_id/>"+
      "<ref_seq_no>10</ref_seq_no>"+
      "<ref_text>"+
      "<![CDATA["+
      AMPER_NEEDS_DTD +
      "]]>"+
      "</ref_text>"+
      "<ref_id/>"+
      "</ref>"+
      "</references>"+
      "<fulltext>"+
      "<file>"+
      "<seq_no>1</seq_no>"+
      "<fname>"+pdfUrl+"</fname>"+
      "</file>"+
      "</fulltext>"+
      "<ccc>"+
      "<copyright_holder>"+
      "<copyright_holder_name>XXX</copyright_holder_name>"+
      "<copyright_holder_year>2012</copyright_holder_year>"+
      "</copyright_holder>"+
      "</ccc>"+
      "</article_rec>"+
    //  "</section>"+
      "</proceeding>";
  private static final String EMPTY_CONTENT =
    //"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"+
    //"<periodical ver=\"4.0\" ts=\"12/22/2008\">"+
      "<article>" +
      "</article>";
     // +"</periodical>";
  
  private static final String BAD_CONTENT =
    "<HTML><HEAD><TITLE>"
    + GOOD_TITLE
    + "</TITLE></HEAD><BODY>\n"
    + "<article_rec>\n<meta name=\"foo\""
    + " content=\"bar\">\n"+ "</meta>"
    + "  <div id=\"issn\">\n"
    + "_t3 0XFAKE0X 1231231 3453453 6786786\n"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + " </div>\n" + "<fname>fake</fname>\n" 
    + "</article_rec>\n" +"</BODY></HTML>";


  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();

    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    mau = new MockArchivalUnit();
    mau.setConfiguration(auConfig());
    
    /* must set up plugin to get helper name */
    ap = new DefinablePlugin();
    ap.initPlugin(theDaemon, PLUGIN_NAME);
    mau.setPlugin(ap);
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
  
  public TestACMSourceXmlMetadataExtractorHelper() throws Exception {
    super.setUp();
    setUp();
  }
 /*
    // get test input files from current directory
  private String getInputFile(String filename) {
    String acmStr;
    try {
      InputStream acmIn = getClass().getResourceAsStream(filename);
      acmStr = StringUtil.fromInputStream(acmIn);
    }
    catch (IOException e) {
       throw new RuntimeException(e);
    }
    return (acmStr);
  }
  */
  
  final static String TEST_XML_URL = "http://clockss-ingest.lockss.org/sourcefiles/acm-dev/2010/7sept2010/NEW-MAG-QUEUE-V8I8-1839572/NEW-MAG-QUEUE-V8I8-1839572.xml";
  final static String TEST_PDF_URL = "http://clockss-ingest.lockss.org/sourcefiles/acm-dev/2010/7sept2010/NEW-MAG-QUEUE-V8I8-1839572/12-authorname.pdf";
  //final static String TEST_XML_URL = "acmtest.xml";
  //final static String TEST_PDF_URL = "localhost:~audreyishizaki/acm-dev/2010/7sept2010/NEW-MAG-QUEUE-V8I8-1839572/p10-chessin.pdf";

  public void testExtractFromEmptyContent() throws Exception {
    String xml_url = TEST_XML_URL;
    String pdf_url = TEST_PDF_URL;

    CIProperties xmlHeader = new CIProperties();
    URL base = new URL(BASE_URL);
    theDaemon.getLockssRepository(mau);
    theDaemon.getNodeManager(mau);
    
    MockCachedUrl xml_cu = mau.addUrl(xml_url, EMPTY_CONTENT);
    // doesn't matter what content the fake pdf_cu has
    MockCachedUrl pdf_cu = mau.addUrl(pdf_url, EMPTY_CONTENT);
    
    FileMetadataExtractor me = new ACMSourceXmlMetadataExtractorFactory
                                  .ACMSourceXmlMetadataExtractor();
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
    xml_cu.setContent(BAD_CONTENT);
    xml_cu.setContentSize(BAD_CONTENT.length());
    MockCachedUrl pdf_cu = mau.addUrl(pdf_url, true, true, xmlHeader);
    // doesn't matter what content the fake pdf_cu has
    pdf_cu.setContent(BAD_CONTENT);
    pdf_cu.setContentSize(BAD_CONTENT.length());

    
    FileMetadataExtractor me = new ACMSourceXmlMetadataExtractorFactory
                                  .ACMSourceXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), xml_cu);
    assertEmpty(mdlist);
  
  }

  // original xml file from the publisher
  public void testExtractFromBasicPeriodicalContent() throws Exception {
    CIProperties xmlHeader = new CIProperties();
    try {
      String xml_url = BASE_URL + "basic.xml";
      String pdf_url = BASE_URL + pdfUrl;
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting
      //MockCachedUrl pcu = mau.addUrl(pdf_url, true, true, xmlHeader);
      MockCachedUrl pcu = mau.addUrl(BASE_URL+pdfUrl, EMPTY_CONTENT);
      
      String string_input = BASIC_PERIODICAL_CONTENT;

      cu.setContent(string_input);
      cu.setContentSize(string_input.length());
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
      // setting content (non-pdf) just so the check can find content
      //pcu.setContent(string_input);
      //pcu.setContentSize(string_input.length());
      //pcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new ACMSourceXmlMetadataExtractorFactory.ACMSourceXmlMetadataExtractor();
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
      assertEquals(GOOD_DATE, md.get(MetadataField.FIELD_DATE));
      //assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
      assertEquals(GOOD_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
      //use FIELD_JOURNAL_TITLE for content5/6 until they adopt the latest daemon
      assertEquals(GOOD_JOURNAL_NAME, md.get(MetadataField.FIELD_JOURNAL_TITLE));
      //assertEquals(GOOD_JOURNAL_NAME, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GOOD_ARTICLE_ID, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
      assertEquals(goodAuthors.toString(), md.getList(MetadataField.FIELD_AUTHOR).toString());

    } finally {
      //IOUtil.safeClose(file_input);
    }
  }

  public void testExtractFromBasicProceedingContent() throws Exception {
    CIProperties xmlHeader = new CIProperties();
    try {
      String xml_url = BASE_URL + "basic.xml";
      String pdf_url = BASE_URL + "12-authorname.pdf";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting
      //MockCachedUrl pcu = mau.addUrl(pdf_url, true, true, xmlHeader);
      MockCachedUrl pcu = mau.addUrl(BASE_URL+pdfUrl, EMPTY_CONTENT);
      
      String string_input = BASIC_PROCEEDING_CONTENT;

      cu.setContent(string_input);
      cu.setContentSize(string_input.length());
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
      // setting content (non-pdf) just so the check can find content
      //pcu.setContent(string_input);
      //pcu.setContentSize(string_input.length());
      pcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new ACMSourceXmlMetadataExtractorFactory.ACMSourceXmlMetadataExtractor();
      assertNotNull(me);
      log.debug3("Extractor: " + me.toString());
      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);

      assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
      assertEquals(GOOD_ISBN, md.get(MetadataField.FIELD_ISBN));
      assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
      assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
      assertEquals(GOOD_DATE, md.get(MetadataField.FIELD_DATE));
      //assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
      assertEquals(GOOD_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
      //use FIELD_JOURNAL_TITLE for content5/6 until they adopt the latest daemon
      assertEquals(GOOD_JOURNAL_NAME, md.get(MetadataField.FIELD_JOURNAL_TITLE));
      //assertEquals(GOOD_JOURNAL_NAME, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
      assertEquals(GOOD_ARTICLE_ID, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
      assertEquals(goodAuthors.toString(), md.getList(MetadataField.FIELD_AUTHOR).toString());

    } finally {
      //IOUtil.safeClose(file_input);
    }
  }
  public void testExtractFromAmperContent() throws Exception {
    CIProperties xmlHeader = new CIProperties();
    try {
      String xml_url = BASE_URL + "basic.xml";
      String pdf_url = BASE_URL + "12-authorname.pdf";
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      MockCachedUrl cu = mau.addUrl(xml_url, true, true, xmlHeader);
      // need to check for this file before emitting
      //MockCachedUrl pcu = mau.addUrl(pdf_url, true, true, xmlHeader);
      MockCachedUrl pcu = mau.addUrl(BASE_URL+pdfUrl, EMPTY_CONTENT);
      
      String string_input = AMPER_CONTENT;

      cu.setContent(string_input);
      cu.setContentSize(string_input.length());
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
      // setting content (non-pdf) just so the check can find content
      //pcu.setContent(string_input);
      //pcu.setContentSize(string_input.length());
      pcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new ACMSourceXmlMetadataExtractorFactory.ACMSourceXmlMetadataExtractor();
      assertNotNull(me);
      log.debug3("Extractor: " + me.toString());
      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);

      log.info(md.getList(MetadataField.FIELD_AUTHOR).toString());
      log.info(md.get(MetadataField.FIELD_JOURNAL_TITLE));

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

 /* 
  // xml file missing journal title
  public void testExtractFromMissingJournalMetaContent1() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(missingJournalMetaInputContent1);
    cu.setContentSize(missingJournalMetaInputContent1.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");

    FileMetadataExtractor me = new AIPJatsSourceXmlMetadataExtractorFactory
                                   .AIPJatsSourceXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
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
    assertEquals(GOOD_COPYRIGHT_YEAR, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(GOOD_JOURNAL_ID,
        md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertSameElements(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
  }

  // xml file missing journal title, journal id and issn
  public void testExtractFromMissingJournalMetaContent2() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(missingJournalMetaInputContent2);
    cu.setContentSize(missingJournalMetaInputContent2.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");

    FileMetadataExtractor me = new AIPJatsSourceMetadataExtractorFactory
                                   .AIPJatsSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(GOOD_EISSN, md.get(MetadataField.FIELD_EISSN));
    assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
    assertEquals(GOOD_COPYRIGHT_YEAR, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertSameElements(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
  }

  // xml file missing journal title, journal id, issn and eissn
  public void testExtractFromMissingJournalMetaContent3() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(missingJournalMetaInputContent3);
    cu.setContentSize(missingJournalMetaInputContent3.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");

    FileMetadataExtractor me = new SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
    assertEquals(GOOD_COPYRIGHT_YEAR, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    //assertEquals(GEN_JOURNAL_TITLE_WITH_JOURNAL_ID,
    //    md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertSameElements(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
  }
  */
  
}