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

package org.lockss.plugin.emerald;

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
 * One of the articles used to get the html source for this plugin is:
 * http://www.emeraldinsight.com/journals.htm?issn=0961-5539&volume=14&issue=5&articleid=1455115&show=html&view=printarticle
 */
public class TestEmeraldMetadataExtractor extends LockssTestCase {
  static Logger log = Logger.getLogger("TestEmeraldMetadataExtractor");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  //private ArchivalUnit hau;		// Highwire AU
  private MockArchivalUnit mau;
  private MockLockssDaemon theDaemon;
  private TitleConfig tc;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.emerald.EmeraldPlugin";

  private static String BASE_URL = "http://www.emeraldinsight.com/";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

  private static final Map<String, String> tagMap =
    new HashMap<String, String>();
  static {
    tagMap.put("citation_journal_title", "JOURNAL %1 %2 %3");
    tagMap.put("citation_issn", "%1-%2-%3");

    tagMap.put("citation_authors", "AUTHOR %1 %2 %3");
    tagMap.put("citation_title", "TITLE %1 %2 %3");
    tagMap.put("citation_date", "%1/%2/%3");
    tagMap.put("citation_volume", "%1%2%3");
    tagMap.put("citation_issue", "%3%2%1");
    tagMap.put("citation_firstpage", "%2%1%3");
    tagMap.put("citation_id", "%1%2%3/%3%2%1/%2%1%3");
    tagMap.put("citation_mjid", "MJID;%1%2%3/%3%2%1/%2%1%3");
    tagMap.put("citation_doi", "10.1152/ajprenal.%1%2%3.2004");
    tagMap.put("citation_abstract_html_url", "http://www.example.com/cgi/content/abstract/%1%2%3/%3%2%1/%2%1%3");
    tagMap.put("citation_fulltext_html_url", "http://www.example.com/cgi/content/full/%1%2%3/%3%2%1/%2%1%3");
    tagMap.put("citation_pdf_url", "http://www.example.com/cgi/reprint/%1%2%3/%3%2%1/%2%1%3.pdf");
    tagMap.put("citation_pmid", "%3%2%1");

    tagMap.put("dc.Contributor", "AUTHOR %1 %2 %3");
    tagMap.put("dc.Title", "TITLE %1 %2 %3");
    tagMap.put("dc.Identifier", "10.1152/ajprenal.%1%2%3.2004");
    tagMap.put("dc.Date", "%1/%2/%3");
  };
  String goodDOI = "10.1108/09685220710759522";
  static String goodVolume = "15";
  String goodIssue = "3";
  String goodStartPage = "168";
  static String goodISSN = "0968-5227";
  String goodDate = "12/06/2007";
  String goodAuthor = "Mohamad Noorman Masrek; Nor Shahriza Abdul Karim; Ramlah Hussein";
  String[] goodAuthors = new String[] {
      "Mohamad Noorman Masrek", "Nor Shahriza Abdul Karim", "Ramlah Hussein" };
  String goodArticleTitle = "Investigating corporate intranet effectiveness: a conceptual framework";
 static String goodJournalTitle = "Information Management & Computer Security";
  String goodAbsUrl = "http://www.emeraldinsight.com/journals.htm?issn=0968-5227&volume=15&issue=3&articleid=1610921";
  String goodHtmUrl = "http://www.emeraldinsight.com/journals.htm?issn=0968-5227&volume=15&issue=3&articleid=1610921&show=html";
  String goodPdfUrl = "http://www.emeraldinsight.com/journals.htm?issn=0968-5227&volume=15&issue=3&articleid=1610921&show=pdf";

  private static final Map<String, String> jProps = new HashMap<String, String>();
  static {
    jProps.put("type", "JOurnal");      // testing ignoreCase
    jProps.put("issn", goodISSN);
    jProps.put("journalTitle", goodJournalTitle);
    jProps.put("volume", goodVolume);
  }
  private static final Map<String, String> bProps = new HashMap<String, String>();
  static {
    bProps.put("issn", goodISSN);
    bProps.put("type", "BOok");         // testing ignoreCase
    bProps.put("journalTitle", goodJournalTitle);
    bProps.put("volume", goodVolume);
  }

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau = new MockArchivalUnit();
    mau.setConfiguration(emeraldAuConfig());
   
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }


  Configuration emeraldAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", goodVolume);
    conf.put("journal_issn", goodISSN);
    tc = new TitleConfig("EmeraldTest" ,PLUGIN_NAME);
    tc.setJournalTitle(goodJournalTitle);
    return conf;
  }


  String goodContent =
		"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
		"<html>\n" +
		"<head>\n" +
		"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" +
		"            <title>Emerald | " + goodArticleTitle + "</title>\n" +
    "<meta name=\"citation_title\" content=\"" + goodArticleTitle + "\">\n" +
    "<meta name=\"citation_authors\"" + 
      " content=\"" + goodAuthor + "\">\n" +
    "<meta name=\"citation_journal_title\"" + " content=\""+goodJournalTitle+"\">\n" +	
    "<meta name=\"citation_issn\" content=\"" + goodISSN + "\">\n" +
    "<meta name=\"citation_volume\"" +
      " content=\"" + goodVolume + "\">\n" +
    "<meta name=\"citation_issue\" content=\"" + goodIssue + "\">\n" +
    "<meta name=\"citation_firstpage\"" +
      " content=\"" + goodStartPage + "\">\n" +
    "<meta name=\"citation_date\" content=\"" + goodDate + "\">\n" +
    "<meta name=\"citation_doi\"" + " content=\"" + goodDOI + "\">\n" +
    "<meta name=\"citation_abstract_html_url\"" +
      " content=\"" + goodAbsUrl + "\">\n" +
		"<meta name=\"citation_fulltext_html_url\" content=\"" + goodHtmUrl + "\">\n" +
    "<meta name=\"citation_pdf_url\"" +
      " content=\"" + goodPdfUrl + "\">\n";
  
  public void testExtractBookContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    tc.setProperties(bProps);   // set up TitleConfig as a book
    mau.setTitleConfig(tc);     // set titleconfig on au
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
      new EmeraldHtmlMetadataExtractorFactory.EmeraldHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    // for a book, the journal_title is the volume and the volume is the issue
    assertEquals(goodVolume, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
  
  }
  String badContent =
    "<HTML><HEAD><TITLE>" + goodArticleTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    "<meta name=\"citation_journal_title\"" + " content=\""+goodJournalTitle+"\">\n" +      
    "<meta name=\"citation_issn\" content=\"" + goodISSN + "\">\n" +
    "<meta name=\"citation_volume\"" + " content=\"" + goodVolume + "\">\n" +
    goodISSN + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    tc.setProperties(bProps);   
    //using the book properties as it will befuddle the journal_title and volume
    mau.setTitleConfig(tc);
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new EmeraldHtmlMetadataExtractorFactory.EmeraldHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    assertNotNull(mle);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNotEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
     //assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNotEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    //assertEquals(1, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
  }
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    tc.setProperties(jProps);   // set up TitleConfig as a journal
    mau.setTitleConfig(tc);     // set titleconfig on au
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
      new EmeraldHtmlMetadataExtractorFactory.EmeraldHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
  
  }

}
