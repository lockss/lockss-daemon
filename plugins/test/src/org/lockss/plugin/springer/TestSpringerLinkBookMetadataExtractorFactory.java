/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer;

import java.util.Arrays;
import java.util.List;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataListExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

/**
 * One of the articles used to get the xml source for this plugin is:
 * http://www.springerlink.com/content/978-3-642-14308-3
 */
public class TestSpringerLinkBookMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestSpringerLinkBookMetadataExtractorFactory");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;		//SpringerLinkBook AU
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = "org.lockss.plugin.springer.ClockssSpringerLinkBookPlugin";

  private static String BASE_URL = "http://www.example.com/";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

  @Override
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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, springerAuConfig());
  }

  @Override
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("year", "2012");
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration springerAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("book_isbn", "000-0-000-00000-0");
    return conf;
  }

  String[] goodAuthors = new String[] {"John A. Author", "John B. Author"};
  String goodVolume = "1234";
  String goodDate = "2010";
  String goodDoi = "10.5555/000-0-000-00000-0_0";
  String goodTitle = "This is a good title";
  String goodIsbn = "000-0-000-00000-0";
  String goodItemNo = "0";

  //break string in to pieces to allow for bad ISBN test
  String goodDOI_ISBN = "<span class=\"label\">DOI:</span> <span class=\"value\">10.5555/000-0-000-00000-0_0</span>";
  String goodContent_top =
		  "</ul>\n"+
		  "</div>\n"+
  		  "<a id=\"ctl00_ContentToolbar_ctl00_SubjectLink\" href=\"../../computer-science/\">Computer Science</a>\n"+
  		  "</div>\n"+
          "<div id=\"ContentHeading\">\n"+
          "<div class=\"heading enumeration\">\n"+
          "<div class=\"primary\">\n"+
          "<a lang=\"en\" href=\"/content/0000-0000/\" title=\"Link to the Book Series of this Book\">Lecture Notes in Computer Science</a>\n"+
          "</div><div class=\"secondary\">\n"+
          "Volume 1234, 2010<span class=\"doi\">, ";
  String goodContent_bottom = "</span>\n"+
          "</div>\n"+
          "</div><div class=\"heading primitive\">\n"+
          "<div class=\"coverImage\" title=\"Cover Image\" style=\"background-image: url(/content/105633/cover-medium.jpg)\">\n"+
          "</div><div class=\"text\">\n"+
          "<h1 lang=\"en\" class=\"title\">\n"+
		  "This is a good title\n"+
          "<span class=\"subtitle\">Subtitle</span>\n"+
          "</h1><p class=\"authors\"><a title=\"View content where unimportant\" href=\"/content/?Author=John+A+Author\">John A. Author</a> and <a title=\"View content where unimportant\" href=\"/content/?Author=John+B+Author\">John B. Author</a></p>\n"+
          "</div><div class=\"clearer\">\n"+
          "<!-- Clear floating elements -->\n"+
          "</div>\n"+
  		  "</div>\n"+
	  	  "<div class=\"heading view linkOutView\">\n"+
		  "<div>\n"+
		  "<span class=\"key\">Link Out to this Book:</span><ul class=\"values\">\n"+
		  "<li><a target=\"_blank\" href=\"/link-out/?id=1234&amp;code=WXW3WX33W\"><img alt=\"Linkout Icon\" src=\"http://www.metapress.com/media/public/Products/Admin/LinkoutIcons/1782/wrong.gif\" />find it</a></li>\n"+
		  "</ul><div class=\"clearer\">\n"+
		  "<!-- Clear floating elements -->\n"+
		  "</div>\n"+
		  "</div>\n"+
		  "<div id=\"ContentSecondary\">\n"+
		  "<div id=\"Cockpit\">\n"+
		  "<ul class=\"sections count3\" data-ajaxSections=\"\">";

  String goodContent = goodContent_top + goodDOI_ISBN + goodContent_bottom;

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    FileMetadataExtractor me =
      new SpringerLinkBookMetadataExtractorFactory.SpringerLinkBookMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodIsbn, md.get(MetadataField.FIELD_EISBN));
    assertEquals(goodItemNo, md.get(MetadataField.FIELD_ITEM_NUMBER));
  }

  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" +
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodAuthors.toString() + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new SpringerLinkBookMetadataExtractorFactory.SpringerLinkBookMetadataExtractor();
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
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_EISBN));
    assertNull(md.get(MetadataField.FIELD_ITEM_NUMBER));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
  }

  String goodDOI_badISBN = "<span class=\"label\">DOI:</span> <span class=\"value\">10.5555/11677437_1</span>";
  String content_BadISBN = goodContent_top + goodDOI_badISBN + goodContent_bottom;

  public void testExtractWithBadISBN() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(content_BadISBN);
    cu.setContentSize(content_BadISBN.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    FileMetadataExtractor me =
      new SpringerLinkBookMetadataExtractorFactory.SpringerLinkBookMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    log.debug3("in extractWithBadISBN, the values pulled are: ");
    log.debug3(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    log.debug3("; ");
    log.debug3(md.get(MetadataField.FIELD_AUTHOR));
    log.debug3("; ");
    log.debug3(md.get(MetadataField.FIELD_VOLUME));
    log.debug3("; ");
    log.debug3(md.get(MetadataField.FIELD_DATE));
    log.debug3("; ");
    log.debug3(md.get(MetadataField.FIELD_DOI));
    log.debug3("; ");
    log.debug3(md.get(MetadataField.FIELD_ISBN));
    log.debug3("!");
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals("10.5555/11677437_1", md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_ISBN));
  }

  String trickierContent =
      "<div class=\"heading enumeration\">\n" +
          "  <div class=\"primary\">\n" +
          "          <a lang=\"en\" href=\"/blah/978-3-540-32547-5/\" title=\"Link to the Book of this Chapter\">Book Title</a>\n" +
          "  </div><div class=\"secondary\">" +
          "          <a lang=\"en\" href=\"/blah/0302-9743/\" title=\"Link to the Book Series of this Chapter\">Book Series Title</a>, 2006, Volume 3755/2006, " +
          "<span class=\"pagination\">1-13</span><span class=\"doi\">, " +
          "<span class=\"label\">DOI:</span> <span class=\"value\">10.1007/11677437_1</span>" +
          "</span>\n" +
          "  </div>\n" +
          "</div><div class=\"heading primitive\">\n" +
          "  <div class=\"coverImage\" title=\"Cover Image\" style=\"background-image: url(/blah/xxxx/cover-medium.jpg); background-size: contain;\">" +
          " </div><div class=\"text\">\n" +
          "          <h1>\n" +
          "                 <a href=\"/blah/12345/\" title=\"Link to Chapter\">Article Title</a>\n" +
          "         </h1><p class=\"authors\"><a title=\"View content where Author is Geoffrey I. Author\" href=\"/content/?Author=Geoffrey+I.+Author\">Geoffrey I. Author</a>" +
          " and <a title=\"View content where Author is Second Brain\" href=\"/content/?Author=Second+Brain\">Second Brain</a></p>\n" +
          "  </div><div class=\"clearer\">\n"+
          "<!-- Clear floating elements -->\n"+
          "</div>\n"+
          "<div class=\"heading view linkOutView\">\n"+
          "<div>\n"+
          "<span class=\"key\">Link Out to this Book:</span><ul class=\"values\">\n"+
          "<li><a target=\"_blank\" href=\"/link-out/?id=1234&amp;code=WXW3WX33W\"><img alt=\"Linkout Icon\" src=\"http://www.metapress.com/media/public/Products/Admin/LinkoutIcons/1782/wrong.gif\" />find it</a></li>\n"+
          "</ul><div class=\"clearer\">\n"+
          "<!-- Clear floating elements -->\n"+
          "</div>\n"+
          "</div>\n"+
          "<div id=\"ContentSecondary\">\n"+
          "<div id=\"Cockpit\">\n";

  String[] moreAuthors = new String[] {"Geoffrey I. Author", "Second Brain"};
  public void testTrickierTitle() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(trickierContent);
    cu.setContentSize(trickierContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    FileMetadataExtractor me =
      new SpringerLinkBookMetadataExtractorFactory.SpringerLinkBookMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    log.debug3("in testTrickierContent, the values pulled are: ");
    log.debug3(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    log.debug3("; ");
    log.debug3(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    log.debug3("; ");
    log.debug3(md.get(MetadataField.FIELD_AUTHOR));
    log.debug3("!");
    assertEquals("Article Title", md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals("Book Title", md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(Arrays.asList(moreAuthors), md.getList(MetadataField.FIELD_AUTHOR));
  }

  String authorContent =
      "<div class=\"heading enumeration\">\n" +
      "<div class=\"text\">\n" +
      "<h1>\n" +
      "        PolyE+CTR: A Swiss-Army-Knife Mode for Block Ciphers\n" +
      "</h1><p class=\"authors\"><a title=\"View content where Author is Liting Zhang\" href=\"/content/?Author=Liting+Zhang\">Liting Zhang</a>" +
      ", <a title=\"View content where Author is Wenling Wu\" href=\"/content/?Author=Wenling+Wu\">Wenling Wu</a>" +
      " and <a title=\"View content where Author is Peng Wang\" href=\"/content/?Author=Peng+Wang\">Peng Wang</a></p>\n" +
      "</div>";
  
  public void testJustAuthors() throws Exception {
    String[] justGoodAuthors = new String[] {"Liting Zhang", "Wenling Wu", "Peng Wang"};
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(authorContent);
    cu.setContentSize(authorContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
        new SpringerLinkBookMetadataExtractorFactory.SpringerLinkBookMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(Arrays.asList(justGoodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    

  }

  /**
   * Inner class that where a number of Archival Units can be created
   *
   */
  public static class MySimulatedPlugin extends SimulatedPlugin {
    @Override
    public ArchivalUnit createAu0(Configuration auConfig)
	throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    @Override
    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }
  }

  /**
   * Inner class to create a html source code simulated content
   */
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    @Override
    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {

      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";

      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";

      file_content += getHtmlContent(fileNum, depth, branchNum,	isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
		    + file_content);

      return file_content;
    }
  }
}