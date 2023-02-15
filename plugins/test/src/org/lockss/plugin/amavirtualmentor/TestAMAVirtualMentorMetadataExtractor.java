/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.amavirtualmentor;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://virtualmentor.ama-assn.org/2011/06/ccas1-1106.html

 *
 */
public class TestAMAVirtualMentorMetadataExtractor extends LockssTestCase {
  static Logger log = Logger.getLogger(TestAMAVirtualMentorMetadataExtractor.class);

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit amavmau; // AMAVirtualMentor AU
  private MockLockssDaemon theDaemon;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.amavirtualmentor.ClockssAMAVirtualMentorPlugin";

  private static String BASE_URL = "http://virtualmentor.ama-assn.org/";
//  private static String SIM_ROOT = BASE_URL + "xyzjn/";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";


  private static final Map<String, String> tagMap =
    new HashMap<String, String>();
  static {
    tagMap.put("citation_journal_title", "JOURNAL %1 %2 %3");
//    tagMap.put("citation_issn", "%1-%2-%3");
    tagMap.put("citation_authors", "AUTHOR %1 %2 %3");
    tagMap.put("citation_title", "TITLE %1 %2 %3");
    tagMap.put("citation_date", "%1/%2/%3");
    tagMap.put("citation_volume", "%1%2%3");
    tagMap.put("citation_issue", "%3%2%1");
    tagMap.put("citation_firstpage", "%2%1%3");
//    tagMap.put("citation_id", "%1%2%3/%3%2%1/%2%1%3");
//    tagMap.put("citation_mjid", "MJID;%1%2%3/%3%2%1/%2%1%3");
    tagMap.put("citation_doi", "10.1001/virtualmentor.2011.%1.%2.%3");
//    tagMap.put("citation_abstract_html_url", "http://www.example.com/cgi/content/abstract/%1%2%3/%3%2%1/%2%1%3");
    tagMap.put("citation_fulltext_html_url", "http://virtualmentor.ama-assn.org/%1%2%3/%3%2%1/%2%1%3.html");
    tagMap.put("citation_pdf_url", "http://virtualmentor.ama-assn.org/%1%2%3/%3%2%1/pdf/%2%1%3.pdf");
//    tagMap.put("citation_pmid", "%3%2%1");

    tagMap.put("dc.Contributor", "AUTHOR %1 %2 %3");
    tagMap.put("dc.Title", "TITLE %1 %2 %3");
    tagMap.put("dc.Identifier", "10.1001/virtualmentor.2011.%1.%2.%3");
    tagMap.put("dc.Date", "%1/%2/%3");
  };

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
					     simAuConfig(tempDirPath));
    amavmau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, amavirtualmentorAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "4");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
//     conf.put("default_article_mime_type", "application/pdf");
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration amavirtualmentorAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "15");
    conf.put("year", "2006");
//    conf.put("journal_issn", "1234-1234");
    return conf;
  }

  String goodDOI = "10.1001/virtualmentor.2011.13.6.ccas1-1106";
  String goodVolume = "13";
  String goodIssue = "6";
  String goodStartPage = "336";
//  String goodISSN = "1937-7010";
  String goodDate = "06/01/2011";
  String goodAuthor = "Fisch, Michael J.;Lee, Richard T.";
  String[] goodAuthors = new String[] {
      "Fisch, Michael J.", "Lee, Richard T." };
  String goodArticleTitle = "When Patients Choose CAM over EBM—How to Negotiate Treatment";
  String goodJournalTitle = "Virtual Mentor";
  String goodPdfUrl = "http://virtualmentor.ama-assn.org/2011/06/pdf/ccas1-1106.pdf";
  String goodHtmUrl = "http://virtualmentor.ama-assn.org/2011/06/ccas1-1106.html";

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
  "<meta name=\"citation_issn\" content=\"" + "\">\n" +
  "<meta name=\"citation_volume\"" +
    " content=\"" + goodVolume + "\">\n" +
  "<meta name=\"citation_issue\" content=\"" + goodIssue + "\">\n" +
  "<meta name=\"citation_firstpage\"" +
    " content=\"" + goodStartPage + "\">\n" +
  "<meta name=\"citation_date\" content=\"" + goodDate + "\">\n" +
  "<meta name=\"citation_doi\"" + " content=\"" + goodDOI + "\">\n" +
  "<meta name=\"citation_abstract_html_url\"" +
    " content=\"" + "\">\n" +
		"<meta name=\"citation_fulltext_html_url\" content=\"" + goodHtmUrl + "\">\n" +
  "<meta name=\"citation_pdf_url\"" +
    " content=\"" + goodPdfUrl + "\">\n";
//  String goodContent = "<HTML><HEAD><TITLE>"+ goodArticleTitle+ "</TITLE></HEAD><BODY>\n"
//  + "<meta name=\"citation_journal_title\" content=\""+ goodJournalTitle+ "\">\n"
//  + "<meta name=\"citation_authors\" content=\""+ goodAuthor+ "\">\n"
//  + "<meta name=\"citation_title\" content=\""+ goodArticleTitle+ "\">\n"
//  + "<meta name=\"citation_journal_title\" content=\""+ goodJournalTitle+ "\">\n"
//  + "<meta name=\"citation_date\" content=\""+ goodDate+ "\">\n"
//  + "<meta name=\"citation_volume\""+ " content=\""+ goodVolume+ "\">\n"
//  + "<meta name=\"citation_issue\" content=\""+ goodIssue	+ "\">\n"
//  + "<meta name=\"citation_firstpage\""+ " content=\""+ goodStartPage	+ "\">\n"
//  + "<meta name=\"citation_doi\"" + " content=\""     + goodDOI + "\">\n";
//  + "<meta name=\"prism.issn\"" + " content=\""     + goodISSN + "\">\n"
//  + "<meta name=\"prism.eIssn\"" + " content=\""     + goodEISSN + "\">\n";	
		
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://virtualmentor.ama-assn.org/year/month/toc-0612.html";
    MockCachedUrl cu = new MockCachedUrl(url, amavmau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
      new AMAVirtualMentorHtmlMetadataExtractorFactory.AMAVirtualMentorHtmlMetadataExtractor();
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
//    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
  }
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodArticleTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
   " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://virtualmentor.ama-assn.org/year/month/toc-0612.html";
    MockCachedUrl cu = new MockCachedUrl(url, amavmau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new AMAVirtualMentorHtmlMetadataExtractorFactory.AMAVirtualMentorHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
//    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertEquals(1, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
  }

  private static String getFieldContent(String content, int fileNum, int depth,
				 int branchNum) {
    content = StringUtil.replaceString(content, "%1", ""+fileNum);
    content = StringUtil.replaceString(content, "%2", ""+depth);
    content = StringUtil.replaceString(content, "%3", ""+branchNum);
    return content;
  }

  public void checkMetadata(ArticleMetadata md) {
    String temp = null;
    temp = (String) md.getRaw("lockss.filenum");
    int fileNum = -1;
    try {
      fileNum = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      fail(temp + " caused " + ex);
    }
    temp = (String) md.getRaw("lockss.depth");
    int depth = -1;
    try {
      depth = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      log.error(temp + " caused " + ex);
      fail();
    }
    temp = (String) md.getRaw("lockss.branchnum");
    int branchNum = -1;
    try {
      branchNum = Integer.parseInt(temp);
    } catch (NumberFormatException ex) {
      log.error(temp + " caused " + ex);
      fail();
    }
    // Does md have all the fields in the meta tags with the right content?
    for (Iterator it = tagMap.keySet().iterator(); it.hasNext(); ) {
      String expected_name = (String)it.next();
      if (expected_name.startsWith("lockss")) {
	continue;
      }
      String expected_content = getFieldContent(tagMap.get(expected_name),
						fileNum, depth, branchNum);
      assertNotNull(expected_content);
      log.debug("key: " + expected_name + " value: " + expected_content);
      String actual_content = (String)md.getRaw(expected_name.toLowerCase());
      assertNotNull(actual_content);
      log.debug("expected: " + expected_content + " actual: " + actual_content);
      assertEquals(expected_content, actual_content);
    }
    // Do the accessors return the expected values?
    assertEquals(getFieldContent(tagMap.get("citation_doi"), fileNum,
			 depth, branchNum),
		 md.get(MetadataField.FIELD_DOI));
//    assertEquals(getFieldContent(tagMap.get("citation_issn"), fileNum,
//				 depth, branchNum),
//		 md.get(MetadataField.FIELD_ISSN));
    assertEquals(getFieldContent(tagMap.get("citation_volume"), fileNum,
				 depth, branchNum),
		 md.get(MetadataField.FIELD_VOLUME));
    assertEquals(getFieldContent(tagMap.get("citation_issue"), fileNum,
				 depth, branchNum),
		 md.get(MetadataField.FIELD_ISSUE));
    assertEquals(getFieldContent(tagMap.get("citation_firstpage"), fileNum,
				 depth, branchNum),
		 md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(getFieldContent(tagMap.get("citation_authors"), fileNum,
			 depth, branchNum),
		 md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(getFieldContent(tagMap.get("citation_title"), fileNum,
			 depth, branchNum),
		 md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(getFieldContent(tagMap.get("citation_journal_title"), fileNum,
			 depth, branchNum),
		 md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(getFieldContent(tagMap.get("citation_date"), fileNum,
			 depth, branchNum),
		 md.get(MetadataField.FIELD_DATE));
  }
  
  public static class MySimulatedPlugin extends SimulatedPlugin {

    public SimulatedContentGenerator getContentGenerator(Configuration cf,
							 String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }
  }

  public static class MySimulatedContentGenerator
    extends SimulatedContentGenerator {

    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum,
				     int depth, int branchNum,
				     boolean isAbnormal) {
      String file_content =
	"<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
      for (Iterator it = tagMap.keySet().iterator(); it.hasNext(); ) {
	String name = (String)it.next();
	String content = tagMap.get(name);
	file_content += "  <meta name=\"" + name + "\" content=\"" +
	  getFieldContent(content, fileNum, depth, branchNum) + "\">\n";
      }
      file_content += "  <meta name=\"lockss.filenum\" content=\"" + fileNum +
	"\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth +
	"\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" +
	branchNum + "\">\n";
      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug("MySimulatedContentGenerator.getHtmlFileContent: " +
		   file_content);
      return file_content;
    }
  }
}
