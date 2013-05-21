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

package org.lockss.plugin.associationforcomputingmachinery;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the files used to get the xml source for this plugin is:
 * http://clockss-ingest.lockss.org/sourcefiles/acm-dev/2010/1jun2010/UPD-PROC-POPL10-1706299/
 */
public class TestAssociationForComputingMachineryXmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestACMXmlMetadataExtractorFactory");

  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  //private ArchivalUnit hau; // ACM AU
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.associationforcomputingmachinery.ClockssAssociationForComputingMachinerySourcePlugin";

  private static String BASE_URL = "http://www.test.com";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    //sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
    //    simAuConfig(tempDirPath));
    //hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, acmAuConfig());
    mau = new MockArchivalUnit();
    mau.setConfiguration(acmAuConfig());
  }

  public void tearDown() throws Exception {
    //sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("year", "2012");
    //conf.put("depth", "2");
    //conf.put("branch", "3");
    //conf.put("numFiles", "7");
    //conf.put( "fileTypes",""
    //    + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_XML));
    //conf.put("default_article_mime_type", "application/pdf");
    //conf.put("binFileSize", "7");
    return conf;
  }

  Configuration acmAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2012");
    return conf;
  }

  String goodJournal = "Journal Name";
  String goodIssn = "0000-0000";
  String goodEissn = "1111-1111";
  String goodVolume = "22";
  String goodIssue = "3";
  String goodDate = "07-26-2010";
  String goodTitle = "Title";
  String goodStart = "Start";
  String goodDoi = "10.1111/1231231.2342342";
  String goodLanguage = "Language";
  //String goodAuthor = "AuthorName, John A.; LongAuthorName, John B.; EvenLongerAuthorName, John C.; AnotherVeryLongAuthorName, John D.; ProbablyTheLongestAuthorNameInThisBunch, John E.";      
  String goodAuthor = "AuthorName, John A.";      
  String goodJournalCode = "Journal Code";
  String goodContent = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"+
  "<periodical ver=\"4.0\" ts=\"12/22/2008\">"+
  "<journal_rec>"+
  "<journal_id>Journal</journal_id>"+
  "<journal_code>Journal Code</journal_code>"+
  "<journal_name>Journal Name</journal_name>"+
  "<journal_abbr>Journal Abbr</journal_abbr>"+
  "<issn>0000-0000</issn>"+
  "<eissn>1111-1111</eissn>"+
  "<language>EN</language>"+
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
  "<volume>22</volume>"+
  "<issue>3</issue>"+
  "<issue_date>July 2010</issue_date>"+
  "<publication_date>08/01/2010</publication_date>"+
  "<special_issue_title><![CDATA[]]></special_issue_title>"+
  "<front_matter>"+
  "<fm_file>DummyMetadata.pdf</fm_file>"+
  "<fm_text>Dummy Metadata</fm_text>"+
  "</front_matter>"+
  "</issue_rec>"+
  "<section>"+
  "<article_rec>"+
  "<article_id>2222222</article_id>"+
  "<sort_key>44</sort_key>"+
  "<display_label>a</display_label>"+
  "<display_no>55</display_no>"+
  "<article_publication_date>07-26-2010</article_publication_date>"+
  "<seq_no>1</seq_no>"+
  "<title><![CDATA[Title]]></title>"+
  "<subtitle></subtitle>"+
  "<page_from>Start</page_from>"+
  "<page_to>1</page_to>"+
  "<doi_number>10.1111/1231231.2342342</doi_number>"+
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
  "<seq_no>5</seq_no>"+
  "<fname>DummyMetadata</fname>"+
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

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.clockss-ingest.lockss.org/sourcefiles/acm-dev/2010/8aug2010/TEST-TEST00/TEST-TEST00.xml";
    CIProperties xmlHeader = new CIProperties();
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    MockCachedUrl mcu = mau.addUrl(url, true, true, xmlHeader);
    
    mcu.setContent(goodContent);
    mcu.setContentSize(goodContent.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    
    FileMetadataExtractor me = new AssociationForComputingMachineryXmlMetadataExtractorFactory.ACMXmlMetadataExtractor();
    assertNotNull(me);
    
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodEissn, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodStart, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodLanguage, md.get(MetadataField.FIELD_LANGUAGE));
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodJournalCode, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(goodLanguage, md.get(MetadataField.FIELD_LANGUAGE));
  }

  String badContent = "<HTML><HEAD><TITLE>"
    + goodTitle
    + "</TITLE></HEAD><BODY>\n"
    + "<article_rec>\n<meta name=\"foo\""
    + " content=\"bar\">\n"+ "</meta>"
    + "  <div id=\"issn\">\n"
    + "_t3 0XFAKE0X 1231231 3453453 6786786\n"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + " </div>\n" + "<fname>fake</fname>\n" 
    + "</article_rec>\n" +"</BODY></HTML>";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.clockss-ingest.lockss.org/sourcefiles/acm-dev/2010/8aug2010/TEST-TEST00/TEST-TEST00.xml";
    CIProperties xmlHeader = new CIProperties();
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    MockCachedUrl mcu = mau.addUrl(url, true, true, xmlHeader);
     
    mcu.setContent(badContent);
    mcu.setContentSize(badContent.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    
    FileMetadataExtractor me = new AssociationForComputingMachineryXmlMetadataExtractorFactory.ACMXmlMetadataExtractor();
    assertNotNull(me);
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotNull(mdlist);
  }
 
  // Read input from one of two XML files (which have been sanitized for anonymity
  // If you wish to add to the test for a specific issues, you can modify or add to one of both of the XML files
  public void testExtractFromProceedingsFile() throws Exception {
    // read in the xml for testing from a file. It's simpler
    InputStream input = null;
    String filename = "org/lockss/plugin/associationforcomputingmachinery/proceedingsA.xml";
    //String filename = "org/lockss/plugin/associationforcomputingmachinery/proceedingsB.xml";
    String proceedingsA_title = "Principles of Stuff";
    String proceedingsB_title = "Proceedings of the 2010 Stuff Conference";
    String proceedingsTitle=proceedingsA_title;

    try {
      input = getClass().getClassLoader().getResourceAsStream(filename);
      String fileContent = StringUtil.fromInputStream(input);

      String url = "http://www.clockss-ingest.lockss.org/sourcefiles/acm-dev/2010/8aug2010/TEST-TEST00/TEST-TEST00.xml";
      CIProperties xmlHeader = new CIProperties();
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
      MockCachedUrl mcu = mau.addUrl(url, true, true, xmlHeader);
         mcu.setContent(fileContent);
      mcu.setContentSize(fileContent.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
      FileMetadataExtractor me = new AssociationForComputingMachineryXmlMetadataExtractorFactory.ACMXmlMetadataExtractor();
      assertNotNull(me);
      FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      ArticleMetadata md = mdlist.get(0);
      assertNotNull(md);

      assertEquals(proceedingsTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    } finally {
      IOUtil.safeClose(input);
    }

  }

  /**
   * Inner class that where a number of Archival Units can be created
   * 
   */
  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
    throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf,
        String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create a html source code simulated content
   */
  public static class MySimulatedContentGenerator extends
  SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum,
        int depth, int branchNum, boolean isAbnormal) {

      String file_content = "<HTML><HEAD><TITLE>" + filename
      + "</TITLE></HEAD><BODY>\n";

      file_content += "  <meta name=\"lockss.filenum\" content=\""
        + fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth
      + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\""
        + branchNum + "\">\n";

      file_content += getHtmlContent(fileNum, depth, branchNum,
          isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
          + file_content);

      return file_content;
    }
  }
}