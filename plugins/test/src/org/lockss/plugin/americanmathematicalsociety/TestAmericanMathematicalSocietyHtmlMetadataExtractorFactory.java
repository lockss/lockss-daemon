/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.americanmathematicalsociety;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://msp.org/camcos/2012/7-2/p04.xhtml
 */
public class TestAmericanMathematicalSocietyHtmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(
      TestAmericanMathematicalSocietyHtmlMetadataExtractorFactory.class);
  
  //Simulated AU to generate content
  private SimulatedArchivalUnit sau; 
  //AmericanMathematicalSociety AU
  private ArchivalUnit hau; 
  private MockLockssDaemon theDaemon;
  
  private static String PLUGIN_NAME = 
      "org.lockss.plugin.americanmathematicalsociety.ClockssAmericanMathematicalSocietyPlugin";
  
  private static String BASE_URL = "http://www.ams.org/";
  private static String SIM_ROOT = BASE_URL;
  
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, mspAuConfig());
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
    conf.put("depth", "0");
    conf.put("branch", "0");
    conf.put("numFiles", "4");
    conf.put(
        "fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_PDF 
            + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }
  
  Configuration mspAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "jams");
    conf.put("year", "2013");
    return conf;
  }
  
  String goodJournalTitle = "Journal of the American Mathematical Society";
  String goodArticle = "Title";
  String goodAuthor = "Name1";
  String goodDoi = "10.1090/S0894-0347-2012-00756-5";
  String goodVolume = "26";
  
  String goodContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
      "<head>\n" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" + 
      "<title>Journal of the American Mathematical Society</title>\n" + 
      "<header class=\"journalHomeHeader\">"+
      "<h2 class=\"headerInfo\">Journal of the American Mathematical Society</h2>"+
      "</header>"+
      "<meta name=\"citation_journal_title\" content=\"Journal of the American Mathematical Society\">\n" + 
      "<meta name=\"citation_journal_abbrev\" content=\"J. Amer. Math. Soc.\">\n" + 
      "<meta name=\"citation_abstract_html_url\" content=\"http://www.ams.org/jams/2013-26-02/S0894-0347-2012-00756-5/\">\n" + 
      "<meta name=\"citation_pdf_url\" content=\"http://www.ams.org/jams/2013-26-02/S0894-0347-2012-00756-5/S0894-0347-2012-00756-5.pdf\">\n" + 
      "<meta name=\"citation_issn\" content=\"0894-0347\">\n" + 
      "<meta name=\"citation_issn\" content=\"1088-6834\">\n" + 
      "<meta name=\"citation_author\" content=\"Name1, A\">\n" + 
      "<meta name=\"citation_title\" content=\"Title\">\n" + 
      "<meta name=\"citation_volume\" content=\"26\">\n" + 
      "</head>\n" +
      "<div id=\"articleBibliographyContent\" class=\"accordion-collapse collapse\" aria-labelledby=\"articleBibliographyHeader\">" +
      "<div class=\"accordion-body dottedList\"><ul><li>DOI: https://doi.org/10.1090/S0894-0347-2012-00756-5</li></ul></div>"+
      "<div class=\"productDetailSubscriptionTabInfo\"><div>\"by \"<a href=\"https://mathscinet.ams.org/mathscinet/author?authorId=641675\""+
      " target=\"_blank\">Name1</a></div></div>"+
      "<div id=\"productDetailSubscriptionSelectedArticleContainer\">" +
      "<div class=\"productDetailSubscriptionTabHeader\">Title</div>" +
      "<div class=\"productDetailArticleInfoContent\"><div>J. Amer. Math. Soc. <strong>26</strong> (2016), 1-59</div></div>" +
      "</html>";
  
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.ams.org/journals/jams/2013-26-02/S0894-0347-2012-00756-5/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new 
        AmericanMathematicalSocietyHtmlMetadataExtractorFactory.
        AmericanMathematicalSocietyHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
  }
  
  String badContent = 
      "<HTML><HEAD><TITLE>" + goodJournalTitle + 
      "</TITLE>\n" + "<meta name=\"foo\" content=\"bar\">\n</HEAD><BODY>" + 
      "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " + 
      " </div>\n";
  
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me = new 
        AmericanMathematicalSocietyHtmlMetadataExtractorFactory.
        AmericanMathematicalSocietyHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertEquals("",md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    
    assertEquals(1, md.rawSize());
  }
  
}
