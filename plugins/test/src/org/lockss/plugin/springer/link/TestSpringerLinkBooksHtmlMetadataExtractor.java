/*
 * $Id: TestRoyalSocietyOfChemistryMetadataExtractor.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer.link;


import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;

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
import java.util.*;

/**
 * Two of the articles used to get the html source for this plugin is:
 * http://www.rsc.org/publishing/journals/JC/article.asp?doi=a700024c
 * http://www.rsc.org/publishing/journals/FT/article.asp?doi=a706359h
 * Need to proxy content through beta2.lockss.org or another LOCKSS box.
 * The content online is NOT relevant to this plugin.
 *
 */
public class TestSpringerLinkBooksHtmlMetadataExtractor extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestSpringerLinkBooksHtmlMetadataExtractor.class);
  
  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit bau;		// RSC AU
  
  private static String PLUGIN_NAME = "org.lockss.plugin.springer.link.SpringerLinkBooksPlugin";
  
  private static String BASE_URL = "http://www.example.org/";
  private static String DOWNLOAD_URL = "http://www.example.download.org/";
  private static String BOOK_EISBN = "1234-1234";
  
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class, simAuConfig(tempDirPath));
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("download_url", DOWNLOAD_URL);
    conf.put("book_eisbn", BOOK_EISBN);
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }
  
  
  String goodDoi = "10.1007/978-3-658-04916-4";
  String goodISBN = "978-3-628-74912-7";
  String goodEISBN = "978-3-628-74916-4";
  String goodDate = "2714";
  String[] goodAuthors = {"Kelly Kooper", "Foo Bar", "E. Hello World", "Face Face"};
  String goodBookTitle = "Contemporary Turkey at a Chicken";
  String goodPublisher = "Chicken Fachmedien Wiesbaden";
  
  String goodContent = 
"  <div class=\"summary\">\n"+
"  <dl>\n"+
"  <dt>Book Title</dt>\n"+
"  <dd id=\"abstract-about-title\">Contemporary Turkey at a Chicken</dd>\n"+
"  <dt id=\"dt-abstract-about-book-subtitle\">Book Subtitle</dt>\n"+
"<dd id=\"abstract-about-book-subtitle\">Chicken Chicken Chicken Turkey</dd>\n"+
"\n"+
"  <dt>Open Access</dt>\n"+
"<dd id=\"abstract-about-openaccess\">\n"+
"Available under\n"+
"<span class=\"help\">\n"+
"Open Access\n"+
"<span class=\"tooltip\">This content is freely available online to anyone, anywhere at any time.</span>\n"+
"</span>\n"+
"</dd>\n"+
"\n"+
"  \n"+
"  <dt id=\"dt-abstract-about-book-chapter-copyright-year\">Copyright</dt>\n"+
"<dd id=\"abstract-about-book-chapter-copyright-year\">2714</dd>\n"+
"\n"+
"  <dt>DOI</dt>\n"+
"  <dd id=\"abstract-about-book-chapter-doi\">10.1007/978-3-658-04916-4</dd>\n"+
"  <dt id=\"dt-abstract-about-book-print-isbn\">Print ISBN</dt>\n"+
"<dd id=\"abstract-about-book-print-isbn\">978-3-628-74912-7</dd>\n"+
"\n"+
"  <dt id=\"dt-abstract-about-book-online-isbn\">Online ISBN</dt>\n"+
"<dd id=\"abstract-about-book-online-isbn\">978-3-628-74916-4</dd>\n"+
"\n"+
"  \n"+
"  \n"+
"  \n"+
"  \n"+
"  <dt id=\"dt-abstract-about-publisher\">Publisher</dt>\n"+
"<dd id=\"abstract-about-publisher\">Chicken Fachmedien Wiesbaden</dd>\n"+
"\n"+
"  <dt id=\"dt-abstract-about-book-copyright-holder\">Copyright Holder</dt>\n"+
"<dd id=\"abstract-about-book-copyright-holder\">The Editor(s) (if applicable) and the Author(s) 2714. The book is published with open access at chickenLink.com</dd>\n"+
"\n"+
"  <dt>Additional Links</dt>\n"+
"  <dd id=\"abstract-about-additional-links\">\n"+
"    <ul>\n"+
"      <li>\n"+
"        <a class=\"external\" href=\"http://www.chicken.com/978-3-628-74912-7\" target=\"_blank\" title=\"It opens in new window\">About this Book</a>\n"+
"      </li>\n"+
"    </ul>\n"+
"  </dd>\n"+
"</dl>\n"+
"<dl>\n"+
"<dt>Topics</dt>\n"+
"<dd itemprop=\"genre\">\n"+
"<ul class=\"abstract-about-subject\">\n"+
"<li>\n"+
"<a href=\"/search?facet-subject\">Cultural Studies</a>\n"+
"</li>\n"+
"<li>\n"+
"<a href=\"/search?facet-subject\">Social Structure, Social Inequality</a>\n"+
"</li>\n"+
"<li>\n"+
"<a href=\"/search?facet-subject=\">Sociology, general</a>\n"+
"</li>\n"+
"</ul>\n"+
"</dd>\n"+
"\n"+
"\n"+
"\n"+
"<dt>eBook Packages</dt>\n"+
"<dd itemprop=\"genre\">\n"+
"<ul class=\"abstract-about-ebook-packages\">\n"+
"<li>\n"+
"<a href=\"/search?package\">eBook Package english Humanities, Social Sciences &amp; Law</a>\n"+
"</li>\n"+
"<li>\n"+
"<a href=\"/search?package\">eBook Package english full Collection</a>\n"+
"</li>\n"+
"</ul>\n"+
"</dd>\n"+
"\n"+
"</dl>\n"+
"<dl>\n"+
"<dt>Editors</dt>\n"+
"<dd>\n"+
"<ul class=\"editors\">\n"+
"<li itemprop=\"editor\" itemscope=\"itemscope\" itemtype=\"http://schema.org/Person\">\n"+
"<a class=\"person\" href=\"/search\" itemprop=\"name\">Kelly Kooper</a>\n"+
"<a class=\"envelope\" href=\"mailto:sv@email.com\" title=\"sv@email.com\"><img src=\"/envelope.png\" alt=\"sv@email.com\"/></a>\n"+
"<sup title=\"H-U B\">(1)</sup>\n"+
"</li>\n"+
"<li itemprop=\"editor\" itemscope=\"itemscope\" itemtype=\"http://schema.org/Person\">\n"+
"<a class=\"person\" href=\"/search?facet\" itemprop=\"name\">Foo Bar</a>\n"+
"<a class=\"envelope\" href=\"mailto:ay@edu.tr\" title=\"ay@edu.tr\"><img src=\"/envelope.png\" alt=\"ay@edu.tr\"/></a>\n"+
"<sup title=\"I B U\">(2)</sup>\n"+
"</li>\n"+
"<li itemprop=\"editor\" itemscope=\"itemscope\" itemtype=\"http://schema.org/Person\">\n"+
"<a class=\"person\" href=\"/search?facet\" itemprop=\"name\">E. Hello World</a>\n"+
"<a class=\"envelope\" href=\"mailto:fu@univ.edu\" title=\"fu@univ.edu\"><img src=\"/envelope.png\" alt=\"fu@univ.edu\"/></a>\n"+
"<sup title=\"S U\">(3)</sup>\n"+
"</li>\n"+
"<li itemprop=\"editor\" itemscope=\"itemscope\" itemtype=\"http://schema.org/Person\">\n"+
"<a class=\"person\" href=\"/search?facet\" itemprop=\"name\">Face Face</a>\n"+
"<a class=\"envelope\" href=\"mailto:oonursal@bilgi.edu.tr\" title=\"oo@bedu.tr\"><img src=\"envelope.png\" alt=\"oo@edu.tr\"/></a>\n"+
"<sup title=\"I B U\">(4)</sup>\n"+
"</li>\n"+
"</ul>\n"+
"</dd>\n"+
"<dt>Editor Affiliations</dt>\n"+
"<dd>\n"+
"<ul class=\"editor-affiliations\">\n"+
"<li>\n"+
"<span class=\"position\">1.</span>\n"+
"<span class=\"affiliation\">\n"+
"H-U B\n"+
"</span>\n"+
"</li>\n"+
"<li>\n"+
"<span class=\"position\">2.</span>\n"+
"<span class=\"affiliation\">\n"+
"I B U\n"+
"</span>\n"+
"</li>\n"+
"<li>\n"+
"<span class=\"position\">3.</span>\n"+
"<span class=\"affiliation\">\n"+
"S U\n"+
"</span>\n"+
"</li>\n"+
"<li>\n"+
"<span class=\"position\">4.</span>\n"+
"<span class=\"affiliation\">\n"+
"I B U\n"+
"</span>\n"+
"</li>\n"+
"</ul>\n"+
"</dd>\n"+
"\n"+
"\n"+
"\n"+
"</dl>\n"+
"\n"+
"</div>";

      
  
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/book/" + BOOK_EISBN;
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new SpringerLinkBooksHtmlMetadataExtractorFactory().
        new SpringerLinkBooksHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodDoi,
        md.get(MetadataField.FIELD_DOI));
    assertEquals(goodISBN,
                 md.get(MetadataField.FIELD_ISBN));
    assertEquals(goodEISBN,
                 md.get(MetadataField.FIELD_EISBN));
    List<String> actualAuthors = md.getList(MetadataField.FIELD_AUTHOR);
    for (int i = 0; i < 4; i++) {
      assertEquals(goodAuthors[i], actualAuthors.get(i));
    }
    assertEquals(goodBookTitle,
        md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodPublisher,
                 md.get(MetadataField.FIELD_PUBLISHER));
  }
  
  String badContent = "<HTML><HEAD><TITLE>" + goodBookTitle + "</TITLE></HEAD><BODY>\n" +
      "<meta name=\"foo\"" +  " content=\"bar\">\n" +
      "  <div id=\"issn\">" +
      "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
      goodISBN + " </div>\n";
  
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/publishing/journals/AC/article.asp";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new SpringerLinkBooksHtmlMetadataExtractorFactory().
        new SpringerLinkBooksHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertEquals(0, md.rawSize());
  }
  
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
  
  public static class MySimulatedContentGenerator extends SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }
    
    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
      
      file_content += "  <meta name=\"lockss.filenum\" content=\"" + fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";
      
      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
          + file_content);
      
      return file_content;
    }
  }
}
