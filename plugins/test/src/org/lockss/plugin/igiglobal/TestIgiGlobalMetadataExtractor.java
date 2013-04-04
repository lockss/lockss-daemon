
/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.igiglobal;

import java.io.*;
import java.util.*;

import junit.framework.Test;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.plugin.taylorandfrancis.TestTaylorAndFrancisArchivalUnit;
import org.lockss.plugin.taylorandfrancis.TestTaylorAndFrancisArchivalUnit.TestCLOCKSSPlugin;
import org.lockss.plugin.taylorandfrancis.TestTaylorAndFrancisArchivalUnit.TestGLNPlugin;
import org.lockss.plugin.taylorandfrancis.TestTaylorAndFrancisArchivalUnit.TestOverrideCLOCKSSPlugin;

// super class for this plugin - variants defined within it
public class TestIgiGlobalMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestIgiGlobalMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private ArchivalUnit bau;
  
  // variables that will set up for each variant of the test (eg. LOCKSS v. CLOCKSS or JOURNAL v. BOOK
  private static Configuration AU_CONFIG;
  private static String PLUGIN_NAME; 
  private static String BASE_URL = "http://www.igiglobal.com/";
  private static String PUBLISHER = "IGI Global"; //This value should 
  private static String URL_STRING = "http://www.example.com/vol1/issue2/art3/"; //isn't really used except to init CU
  
  // per-variant content used to test extraction
  private static String goodContent;
  private static String badContent;
  // per-variant expected results of extraction cooked and/or raw values
  private static Map<MetadataField, String> expectedFieldMap; // used to set up expected field map key-value combinations
  private static Map<String, String> expectedRawMap; // used to set up arbitrary string raw map key-value combinations

  // variant of the plugin metadata extraction test for the JOURNAL version of the plugin
  public static class TestJournalPlugin extends TestIgiGlobalMetadataExtractor {
   
    public void setUp() throws Exception {

      PLUGIN_NAME="org.lockss.plugin.igiglobal.IgiGlobalPlugin";  
      AU_CONFIG = ConfigurationUtil.fromArgs(
          "base_url", BASE_URL,
          "volume", "2",
          "journal_issn","1546-2234" );
      super.setUp();

   }
    
    // JOURNAL plugin example of good content from which to extract
    public void setUpGoodContent() {
      // Set up the metadata that SHOULD be extracted and load it in to a map
      String goodDoi = "10.4018/joeuc.1990010101";
      String goodVolume = "2";
      String goodIssue = "1";
      String goodStartPage = "2";
      String goodEndPage = "14";
      String goodISSN = "1546-2234";
      String goodDate = "1990";
      String goodArticleTitle = "The Use of an Alternative Source of Expertise for the Development of Microcomputer Expert Systems";
      String goodJournalTitle = "Journal of Organizational and End User Computing (JOEUC)";
      String theAuthors = "Grudnitski, Gary; Black, Robert L.";

      goodContent = 
          "<link rel=\"shortcut icon\" type=\"image/x-icon\" href=\"../../App_Master/favicon.ico\">\n"
              + "<meta content=\"Sitefinity 3.6.1936.2:1\" name=\"Generator\">\n"
              + "<meta content=\"" + goodJournalTitle + "\" name=\"citation_journal_title\">\n"
              + "<meta content=\"" + PUBLISHER + "\" name=\"citation_publisher\">\n"
              + "<meta content=\"" + theAuthors + "\" name=\"citation_authors\">\n"
              + "<meta content=\"" + goodArticleTitle + "\" name=\"citation_title\">\n"
              + "<meta content=\"" + goodVolume + "\" name=\"citation_volume\">\n"
              + "<meta content=\"" + goodIssue + "\" name=\"citation_issue\">\n"
              + "<meta content=\"" + goodStartPage + "\" name=\"citation_firstpage\">\n"
              + "<meta content=\"" + goodEndPage + "\" name=\"citation_lastpage\">\n"
              + "<meta content=\"" + goodDoi + "\" name=\"citation_doi\">\n"
              + "<meta content=\"http://www.igi-global.com/Bookstore/Article.aspx?TitleId=55656\" name=\"citation_abstract_html_url\">\n"
              + "<meta content=\"http://www.igi-global.com/ViewTitle.aspx?TitleId=55656\" name=\"citation_pdf_url\">\n"
              + "<meta content=\"" + goodISSN + "\" name=\"citation_issn\">\n"
              + "<meta content=\"en\" name=\"citation_language\">\n"
              + "<meta name=\"citation_keywords\">\n"
              + "<meta content=\"" + goodDate + "\" name=\"citation_date\">\n";   

      String goodAuthors = "[" + theAuthors.replace(";",",") + "]";
      
      // Set up a hashmap of the fields and their values that you expect to have extracted
      // Set it to null if you are only testing raw fields
      expectedFieldMap  =
          new HashMap<MetadataField, String>();
      {
        expectedFieldMap.put(MetadataField.FIELD_DOI,goodDoi);
        expectedFieldMap.put(MetadataField.FIELD_VOLUME, goodVolume);
        expectedFieldMap.put(MetadataField.FIELD_ISSUE, goodIssue);
        expectedFieldMap.put(MetadataField.FIELD_START_PAGE, goodStartPage);
        expectedFieldMap.put(MetadataField.FIELD_END_PAGE, goodEndPage);
        expectedFieldMap.put(MetadataField.FIELD_PUBLISHER, PUBLISHER);
        expectedFieldMap.put(MetadataField.FIELD_ISSN, goodISSN);
        expectedFieldMap.put(MetadataField.FIELD_ARTICLE_TITLE, goodArticleTitle);
        expectedFieldMap.put(MetadataField.FIELD_JOURNAL_TITLE, goodJournalTitle);
        expectedFieldMap.put(MetadataField.FIELD_DATE, goodDate);
        expectedFieldMap.put(MetadataField.FIELD_AUTHOR, goodAuthors);
      };
      
      // Set up a hashmap of any raw fields with the values you expect
      // Set it to null if you are only testing for MetadataField values
      expectedRawMap = null;
    }

    // JOURNAL plugin example of bad content
    public void setUpBadContent() {
 
      String goodISSN = "1546-2234";
      String goodArticleTitle = "The Use of an Alternative Source of Expertise for the Development of Microcomputer Expert Systems";

      badContent = "<HTML><HEAD><TITLE>"
          + goodArticleTitle
          + "</TITLE></HEAD><BODY>\n"
          + "<meta name=\"foo\""
          + " content=\"bar\">\n"
          + "  <div id=\"issn\">"
          + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
          + goodISSN + " </div>\n";

      // Set up a hashmap of the fields and their values that you expect to have extracted
      // Set it to null if you are only testing raw fields
      expectedFieldMap  =
          new HashMap<MetadataField, String>();
      {
        expectedFieldMap.put(MetadataField.FIELD_DOI,null);
        expectedFieldMap.put(MetadataField.FIELD_VOLUME, null);
        expectedFieldMap.put(MetadataField.FIELD_ISSUE, null);
        expectedFieldMap.put(MetadataField.FIELD_START_PAGE, null);
        expectedFieldMap.put(MetadataField.FIELD_PUBLISHER, PUBLISHER); // This should always be set, even if not in content
        expectedFieldMap.put(MetadataField.FIELD_ISSN, null);
      };

      // Set up a hashmap of any raw fields with the values you expect
      // Set it to null if you are only testing for MetadataField values
      expectedRawMap  =
          new HashMap<String, String>();
      {
        expectedRawMap.put("foo","bar");
      }

    }
    
  }
  
  // variant of the plugin metadata extraction test for the BOOK version of the plugin
  public static class TestBooksPlugin extends TestIgiGlobalMetadataExtractor {
    
    public void setUp() throws Exception {
      // set up stuff explicit to this variant and call parent
      PLUGIN_NAME="org.lockss.plugin.igiglobal.ClockssIgiGlobalBooksPlugin";  
      AU_CONFIG = ConfigurationUtil.fromArgs(
          "base_url", BASE_URL,
          "volume", "464",
          "book_isbn", "9781591407928" );
      super.setUp();
   }
    
    // BOOK plugin example of good content from which to extract
    public void setUpGoodContent() {    
      String oneAuthor = "Roberts, L.";
      String twoAuthor = "Author, John Q.";
      String goodIdentifier = "9781591407928 DOI: 10.4018/978-1-59140-792-8.ch003";
      String goodTitle = "Opportunities and Constraints of Electronic Research";
      String goodDescription = "In the past decade, blah blah blah.";
      String goodType ="chapter";
      String goodFormat = "electronic";
      String goodSource = "http://services.igi-global.com/resolvedoi/resolve.aspx?doi=10.4018/978-1-59140-792-8.ch003";
      String goodLanguage = "English";
      String goodCoverage = "opportunities-constraints-electronic-research";
      String goodRights = "Access limited to members";
      
      goodContent = 
          "<link href=\"http://purl.org/dc/elements/1.1/\" rel=\"schema.DC\" />" +
          "<meta name=\"DC.creator\" content=\"" + oneAuthor + "\" />" +
          "<meta name=\"DC.creator\" content=\"" + twoAuthor + "\" />" +
      "<meta name=\"DC.identifier\" content=\"" + goodIdentifier + "\" />" +
     " <meta name=\"DC.title\" content=\"" + goodTitle + "\" />" +
      "<meta name=\"DC.description\" content=\"" + goodDescription + "\" />" +
      "<meta name=\"DC.publisher\" content=\"IGI Global\" />" +
      "<meta name=\"DC.type\" content=\"" + goodType + "\" />" +
      "<meta name=\"DC.format\" content=\"" + goodFormat + "\" />" +
      "<meta name=\"DC.source\" content=\"" + goodSource + "\" />" +
      "<meta name=\"DC.language\" content=\"" + goodLanguage + "\" />" +
      "<meta name=\"DC.coverage\" content=\"" + goodCoverage + "\" />" +
      "<meta name=\"DC.rights\" content=\"" + goodRights + "\" />" +
      "</head>" +
      "<body class=\"MainBody\"><form name=\"aspnetForm\" method=\"post\" action=\"/gateway/chapter/20214\" id=\"aspnetForm\">" +
      "<script src=\"/WebResource.axd\" type=\"text/javascript\"></script>" +
      "<script src=\"/ScriptResource.axd\" type=\"text/javascript\"></script>" +
      "<script type=\"text/javascript\">";
      
      String goodAuthors = "[" + oneAuthor + ", " + twoAuthor + "]";
      
      // Set up a hashmap of the fields and their values that you expect to have extracted
      // Set it to null if you are only testing raw fields
      expectedFieldMap  =
          new HashMap<MetadataField, String>();
         {
          expectedFieldMap.put(MetadataField.DC_FIELD_CREATOR,goodAuthors);
          expectedFieldMap.put(MetadataField.DC_FIELD_IDENTIFIER, goodIdentifier);
          expectedFieldMap.put(MetadataField.DC_FIELD_TITLE, goodTitle);
          expectedFieldMap.put(MetadataField.DC_FIELD_DESCRIPTION, goodDescription);
          expectedFieldMap.put(MetadataField.DC_FIELD_PUBLISHER, PUBLISHER);
          expectedFieldMap.put(MetadataField.DC_FIELD_TYPE, goodType);
          expectedFieldMap.put(MetadataField.DC_FIELD_FORMAT, goodFormat);
          expectedFieldMap.put(MetadataField.DC_FIELD_SOURCE, goodSource);
          expectedFieldMap.put(MetadataField.DC_FIELD_LANGUAGE, goodLanguage);
          expectedFieldMap.put(MetadataField.DC_FIELD_COVERAGE, goodCoverage);
          expectedFieldMap.put(MetadataField.DC_FIELD_RIGHTS, goodRights);
          
          // and some DC fields also map to standard fields
          expectedFieldMap.put(MetadataField.FIELD_PUBLISHER, PUBLISHER);
          expectedFieldMap.put(MetadataField.FIELD_ARTICLE_TITLE, goodTitle);         
          expectedFieldMap.put(MetadataField.FIELD_AUTHOR, goodAuthors);
        };
        
        // Set up a hashmap of any raw fields with the values you expect
        // Set it to null if you are only testing for MetadataField values
        expectedRawMap = null;
    }
    
    // BOOK variant bad content
    public void setUpBadContent() {
      // Set up the metadata that SHOULD be extracted and load it in to a map
      //WEAK - this is basically the same as for Journal - not sure how to specialize this for books
      String goodISBN = "9781591407928";
      String goodChapterTitle = "Opportunities and Constraints of Electronic Research";
      
      //even though the good content is in the html this me won't pick it out
      badContent = "<HTML><HEAD><TITLE>"
        + goodChapterTitle
        + "</TITLE></HEAD><BODY>\n"
        + "<meta name=\"foo\""
        + " content=\"bar\">\n"
        + "  <div id=\"issn\">"
        + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
        + goodISBN + " </div>\n";
      
      // Set up a hashmap of the fields and their values that you expect to have extracted
      // Set it to null if you are only testing raw fields
      expectedFieldMap  =
          new HashMap<MetadataField, String>();
         {
          expectedFieldMap.put(MetadataField.FIELD_DOI,null);
          expectedFieldMap.put(MetadataField.FIELD_VOLUME, null);
          expectedFieldMap.put(MetadataField.FIELD_ISSUE, null);
          expectedFieldMap.put(MetadataField.FIELD_START_PAGE, null);
          expectedFieldMap.put(MetadataField.FIELD_PUBLISHER, PUBLISHER); // This should always be set, even if not in content
          expectedFieldMap.put(MetadataField.FIELD_ISSN, null);
        };
        
        // Set up a hashmap of any raw fields with the values you expect
        // Set it to null if you are only testing for MetadataField values        
        expectedRawMap  =
            new HashMap<String, String>();
           {
            expectedRawMap.put("foo","bar");
           }

    }
    
  }

// The super (of the variants) methods
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    //igiConf is only set up in variants - check before using?
    assertNotEquals(AU_CONFIG, null);

    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void setUpGoodContent() {
    //  Do I need to implement anything here? It should all be done by the variants
    
    goodContent = "";
    expectedFieldMap  = null;
    expectedRawMap  = null;
  }
  
  public void setUpBadContent() {
 
    badContent = "";
    expectedFieldMap  = null;
      expectedRawMap  = null;
  }

		
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the IgiHtmlMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = URL_STRING;
    MockCachedUrl cu = new MockCachedUrl(url, bau);

    setUpGoodContent();
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");

    FileMetadataExtractor me = new IgiGlobalHtmlMetadataExtractorFactory.IgiGlobalHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    // Go through the expectedFieldMap if set up and compare against actual
    if (expectedFieldMap != null) {
      for (Iterator it = expectedFieldMap.keySet().iterator(); it.hasNext(); ) {
        MetadataField expected_key = (MetadataField)it.next();

        String expected_content = expectedFieldMap.get(expected_key); 
        assertNotNull(expected_content); // in goodContent, expeted_content is not NULL 

        log.debug("key: " + expected_key.toString() + " value: " + expected_content);
        
        // special case fields that return a list
        String actual_content;
        if ( (expected_key == MetadataField.FIELD_AUTHOR) ||
            (expected_key == MetadataField.DC_FIELD_CREATOR) ) {       // turn entire list in to string to check against
          actual_content = (String)md.getList(expected_key).toString();
        } else {
          actual_content = (String)md.get(expected_key);
        }

        assertNotNull(actual_content); 
        log.debug("expected: " + expected_content + " actual: " + actual_content);
        assertEquals(expected_content, actual_content);
      }
    }

    //Go through the expectedRawMap if it was set up and compare against actual
    if (expectedRawMap != null) {
      for (Iterator it = expectedRawMap.keySet().iterator(); it.hasNext(); ) {
        String expected_key = (String)it.next();
        String expected_content = expectedRawMap.get(expected_key); 
        assertNotNull(expected_content); // in goodContent, expeted_content is not NULL 

        log.debug("raw key: " + expected_key + " value: " + expected_content);
        String actual_content = (String)md.getRaw(expected_key.toLowerCase());

        assertNotNull(actual_content); // in gooContent, actual_content is not NULL
        log.debug("expected: " + expected_content + " actual: " + actual_content);
        assertEquals(expected_content, actual_content);
      }
    }
  }


  /**
   * Method that creates a simulated Cached URL from the source code provided by the badContent Sring. It then asserts that NO metadata is extracted by using 
   * the NatureHtmlMetadataExtractorFactory as the source code is broken.
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    String url = URL_STRING;
    MockCachedUrl cu = new MockCachedUrl(url, bau);

    setUpBadContent();
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");

    FileMetadataExtractor me = new IgiGlobalHtmlMetadataExtractorFactory.IgiGlobalHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    if (expectedFieldMap != null) {
      // Go through the expectedFieldMap if set up and compare against actual
      for (Iterator it = expectedFieldMap.keySet().iterator(); it.hasNext(); ) {
        MetadataField expected_key = (MetadataField)it.next();
        String expected_content = expectedFieldMap.get(expected_key); 
        // expected_content could be null

        log.debug("key: " + expected_key.toString() + " value: " + expected_content);

        // special case fields that return a list
        String actual_content;
        if ( (expected_key == MetadataField.FIELD_AUTHOR) ||
            (expected_key == MetadataField.DC_FIELD_CREATOR) ) {       // turn entire list in to string to check against
          actual_content = (String)md.getList(expected_key).toString();
        } else {
          actual_content = (String)md.get(expected_key);
        }

        // actual_content could be null
        log.debug("expected: " + expected_content + " actual: " + actual_content);
        assertEquals(expected_content, actual_content);
      }
    }

    assertEquals(1, md.rawSize()); // modify this depending on whaty ou put in map
    if (expectedRawMap != null) {
      //Go through the expectedRawMap if it was set up and compare against actual
      for (Iterator it = expectedRawMap.keySet().iterator(); it.hasNext(); ) {
        String expected_key = (String)it.next();
        String expected_content = expectedRawMap.get(expected_key); 

        log.debug("raw key: " + expected_key + " value: " + expected_content);
        String actual_content = (String)md.getRaw(expected_key.toLowerCase());

        log.debug("expected: " + expected_content + " actual: " + actual_content);
        assertEquals(expected_content, actual_content);
      }
    }

  }	

  public static Test suite() {
    return variantSuites(new Class[] {
        TestJournalPlugin.class,
        TestBooksPlugin.class,
      });
  }
}
