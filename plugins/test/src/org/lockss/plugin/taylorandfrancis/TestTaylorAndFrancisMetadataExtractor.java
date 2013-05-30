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

package org.lockss.plugin.taylorandfrancis;

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
 * One of the articles used to get the HTML source for this plugin is:
 * http://www.tandfonline.com/doi/full/10.1080/09639284.2010.501577
 *
 */
public class TestTaylorAndFrancisMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestTaylorAndFrancisMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit tafau; // Taylor and Francis AU
  private static final String issnTemplate = "%1%2%3%1-%3%1%2%3";	

  private static String PLUGIN_NAME = "org.lockss.plugin.taylorandfrancis.TaylorAndFrancisPlugin"; // XML file in org.lockss.plugin.taylorandfrancis package

  private static String BASE_URL = "http://www.tandfonline.com/";
  private static String SIM_ROOT = BASE_URL + "toc/tafjn/"; // Simulated journal ID: "Taylor and Francis Journal"

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,	simAuConfig(tempDirPath));
    tafau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, taylorAndFrancisAuConfig());
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
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes",""	+ (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration taylorAndFrancisAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "19");
    conf.put("journal_id", "tafjn");
    return conf;
  }

  // the metadata that should be extracted
  String goodDOI = "10.1080/09639284.2010.501577";
  String goodVolume = "19";
  String goodIssue = "6";
  String goodStartPage = "555";
  String goodISSN = "1234-5678";
  String goodAuthor = "Melvin Harbison";
  String[] goodAuthors = new String[] {"Melvin Harbison", "Desmond McCallan"};
  String goodSubject = "food and culture; alternative treatment approaches; rhyme therapy"; // Cardinality of dc.subject is single even though content is identical to the keywords field
  String goodDescription = "The description is a lengthier statement, often with mixed punctuation. This should do; let's try it.";
  String goodDate = "2011-09-21";
  String origPublisher = "Gleeson";
  String goodPublisher = "Taylor & Francis Group";  
  String goodType = "content-article";
  String goodFormat = "text/HTML";
  String goodLanguage = "en";
  String goodCoverage = "world";
  String goodSource = "http://dx.doi.org/10.1080/09639284.2010.501577";
  String goodArticleTitle = "Something Terribly Interesting: A Stirring Report";
  String[] goodKeywords = new String[] {"food and culture", "alternative treatment approaches", "rhyme therapy"};
  String goodJournalTitle = "Life, Pain & Death";
  String goodAbsUrl = "http://www.tandfonline.com/doi/abs/10.1080/09639284.2010.501577";
  String goodPdfUrl = "http://www.tandfonline.com/doi/pdf/10.1080/09639284.2010.501577";
  String goodHtmUrl = "http://www.tandfonline.com/doi/full/10.1080/09639284.2010.501577";

  // a chunk of html source code from the publisher's site from where the metadata should be extracted
  String goodContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n"
          + "<title>Taylor &amp; Francis Online  :: Something Terribly Interesting: A Stirring Report - Research and Results - Volume 19,\nIssue 6 </title>\n"
          + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
          + "<meta name=\"robots\" content=\"noarchive,nofollow\" />\n"
          + "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link><meta name=\"dc.Title\" content=\"" + goodArticleTitle + "\"></meta>"
          + "<meta name=\"dc.Creator\" content=\" " + goodAuthors[0] + " \"></meta>" //spaces within content field in dc.Creator appear like this in the HTML
          + "<meta name=\"dc.Creator\" content=\" " + goodAuthors[1] + " \"></meta>" 
          + "<meta name=\"dc.Subject\" content=\"" + goodSubject + "\"></meta>" 
          + "<meta name=\"dc.Description\" content=\"" + goodDescription + "\"></meta>"
          + "<meta name=\"dc.Publisher\" content=\" " + origPublisher + " \"></meta>" //spaces within content field in dc.Publisher appear like this in the HTML
          + "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"" + goodDate + "\"></meta>"
          + "<meta name=\"dc.Type\" content=\"" + goodType + "\"></meta>"
          + "<meta name=\"dc.Format\" content=\"" + goodFormat + "\"></meta>"
          + "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"501577\"></meta>"
          + "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"" + goodDOI + "\"></meta>"
          + "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Life, Pain &amp; Death, Vol. 19, No. 6, December 2010, pp. 555-567\"></meta>"
          + "<meta name=\"dc.Source\" content=\"" + goodSource + "\"></meta>"
          + "<meta name=\"dc.Language\" content=\"" + goodLanguage + "\"></meta>"
          + "<meta name=\"dc.Coverage\" content=\"" + goodCoverage + "\"></meta>"
          + "<meta name=\"keywords\" content=\"" + goodKeywords[0] + "; " + goodKeywords[1] + "; " + goodKeywords[2] + "\"></meta>";
  
  
  /**
   * Method that creates a simulated Cached URL from the source code provided by the goodContent String.
   * It then asserts that the metadata extracted with TaylorAndFrancisHtmlMetadataExtractorFactory
   * match the metadata in the source code. 
   * @throws Exception
   */
    public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.tandfonline.com/toc/tafjn/19/6";
    MockCachedUrl cu = new MockCachedUrl(url, tafau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new TaylorAndFrancisHtmlMetadataExtractorFactory.TaylorAndFrancisHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodDate, md.get(MetadataField.DC_FIELD_DATE));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodSubject, md.get(MetadataField.DC_FIELD_SUBJECT));
    assertEquals(goodDescription, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodPublisher, md.get(MetadataField.DC_FIELD_PUBLISHER));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodCoverage, md.get(MetadataField.DC_FIELD_COVERAGE));
    assertEquals(goodSource, md.get(MetadataField.DC_FIELD_SOURCE));
    assertEquals(Arrays.asList(goodKeywords), md.getList(MetadataField.FIELD_KEYWORDS));
  }

  // a chunk of HTML source code from where the TaylorAndFrancisHtmlMetadataExtractorFactory should NOT be able to extract metadata
  String badContent = "<html><head><title>" 
	+ goodArticleTitle
    + "</title></head><body>\n"
    + "<meta name=\"foo\""
    + " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + goodISSN + " </div>\n";

  /**
   * Method that creates a simulated Cached URL from the source code provided by the badContent Sring. It then asserts that NO metadata is extracted by using 
   * the TaylorAndFrancisHtmlMetadataExtractorFactory as the source code is broken.
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, tafau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new TaylorAndFrancisHtmlMetadataExtractorFactory.TaylorAndFrancisHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));

     //the meta info was outside the head section - parser extractor won't get it 
    //assertEquals(1, md.rawSize());
   // assertEquals("bar", md.getRaw("foo")); 
  }	
  
  String encodedContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n"
      + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
      + "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\""
      + "\u201c" + "BOO" + "\u201d" + ", Vol. 19, No. 6, December 2010, pp. 555" + "\u2013" + "567\"></meta>"
      + "<meta name=\"dc.Title\" content=\""
      + "\u201c" + "BOO" + "\u201d" + "\"></meta>"      
      + "</head>";
  
  public void testEncodedContent() throws Exception {
    String goodTitle = "\u201c" + "BOO" + "\u201d";
    
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, tafau);
    cu.setContent(encodedContent);
    cu.setContentSize(encodedContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new TaylorAndFrancisHtmlMetadataExtractorFactory.TaylorAndFrancisHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals("555", md.get(MetadataField.FIELD_START_PAGE));
  }

  // When a the coden doesn't have spaces before the comma, it can cause problems - note the use of -u2013 for the hyphen - endash - encoding safe
  String noSpaceContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n" +
      "<meta name=\"dc.Title\" content=\"An Integrative, Empowerment Model for Helping Lesbian, Gay, and Bisexual Youth Negotiate the Coming-Out Process\"></meta>" +
      "<meta name=\"dc.Publisher\" content=\" Taylor &amp; Francis Group \"></meta>" +
      "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"678176\"></meta>" +
      "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"10.1080/15538605.2012.678176\"></meta>" +
      "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Journal of LGBT Issues in Counseling,Vol. 6, No. 2, April-June 2012, pp. 96\u2013117\"></meta>";

  public void testNoSpaceContent() throws Exception {
    String goodTitle = "An Integrative, Empowerment Model for Helping Lesbian, Gay, and Bisexual Youth Negotiate the Coming-Out Process";
    
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, tafau);
    cu.setContent(noSpaceContent);
    cu.setContentSize(noSpaceContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new TaylorAndFrancisHtmlMetadataExtractorFactory.TaylorAndFrancisHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals("96", md.get(MetadataField.FIELD_START_PAGE));
    assertEquals("Journal of LGBT Issues in Counseling", md.get(MetadataField.FIELD_JOURNAL_TITLE));
  }
  
  String noJournalTitleContent =
      "<meta name=\"dc.Title\" content=\"Engagement\"></meta>"+
          "<meta name=\"dc.Creator\" content=\" Stephanie   Burley \"></meta>"+
          "<meta name=\"dc.Publisher\" content=\" Taylor &amp; Francis Group \"></meta>"+
          "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"2012-06-19\"></meta>"+
          "<meta name=\"dc.Format\" content=\"text/HTML\"></meta>"+
          "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"596666\"></meta>"+
          "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"10.1080/03323315.2011.596666\"></meta>"+
          "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Irish Educational Studies Vol. 31, No. 2, June 2012, pp. 175\u2013190\"></meta>"+
          "<meta name=\"dc.Source\" content=\"http://dx.doi.org/10.1080/03323315.2011.596666\"></meta>";
  
  public void testNoJournalTitleContent() throws Exception {

    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, tafau);
    cu.setContent(noJournalTitleContent);
    cu.setContentSize(noJournalTitleContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new TaylorAndFrancisHtmlMetadataExtractorFactory.TaylorAndFrancisHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals("10.1080/03323315.2011.596666", md.get(MetadataField.FIELD_DOI));
    assertEquals("Irish Educational Studies", md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals("31", md.get(MetadataField.FIELD_VOLUME));
    assertEquals("2", md.get(MetadataField.FIELD_ISSUE));
    assertEquals("175", md.get(MetadataField.FIELD_START_PAGE));
    assertEquals("190", md.get(MetadataField.FIELD_END_PAGE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }

  String badIdentifierContent =
      "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"567009\"></meta>" +
"<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"10.1080/13567888.2011.567009\"></meta>" + 
"<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Volume 17, Comment 1 Ð January 2011\"></meta>";
  
  public void testbadIdentifierContent() throws Exception {

    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, tafau);
    cu.setContent(badIdentifierContent);
    cu.setContentSize(badIdentifierContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new TaylorAndFrancisHtmlMetadataExtractorFactory.TaylorAndFrancisHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals("10.1080/13567888.2011.567009", md.get(MetadataField.FIELD_DOI));
    assertEquals(null, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(null, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(null, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(null, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }
  
  String noVolumeContent =
      "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"739174\"></meta>" +
          "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"10.1080/09500782.2012.739174\"></meta>" +
          "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Language and Education, preprint, 2012, pp. 1Ð24\"></meta>";

  // Since we never get a Vol, we don't every pick up a title.  This is probably best. Let the correct title come from the TDB
  public void testNoVolumeContent() throws Exception {

    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, tafau);
    cu.setContent(noVolumeContent);
    cu.setContentSize(noVolumeContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new TaylorAndFrancisHtmlMetadataExtractorFactory.TaylorAndFrancisHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals("10.1080/09500782.2012.739174", md.get(MetadataField.FIELD_DOI));
    // it's the best we can do 
    assertEquals(null, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(null, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(null, md.get(MetadataField.FIELD_ISSUE));
    // encoding issue...fix later
    //assertEquals("1", md.get(MetadataField.FIELD_START_PAGE));
    //assertEquals("24", md.get(MetadataField.FIELD_END_PAGE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
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

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create HTML source code simulated content
   *
   */
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
			
      String file_content = "<html><head><title>" + filename + "</title></head><body>\n";
			
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";			

      file_content += getHtmlContent(fileNum, depth, branchNum,	isAbnormal);
      file_content += "\n</body></html>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
		    + file_content);

      return file_content;
    }
  }
  
}

