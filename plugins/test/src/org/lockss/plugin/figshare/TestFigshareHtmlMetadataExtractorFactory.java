/*
 * $Id$
 */

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

package org.lockss.plugin.figshare;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.figshare.FigshareHtmlMetadataExtractorFactory;
import org.lockss.plugin.figshare.FigshareHtmlMetadataExtractorFactory.FigshareHtmlMetadataExtractor;
import org.lockss.plugin.simulated.*;

/*


*/

public class TestFigshareHtmlMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger("TestFigshareHtmlMetadataExtractorFactory" +
                "");

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.figshare.ClockssFigsharePlugin";
  private static String BASE_URL = "http://www.examplePress.com/";
  private static String API_URL = "http://api.examplePress.com/";
  private static String SIM_ROOT = BASE_URL + "simroot/";

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
    mau.setConfiguration(figshareAuConfig());  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration figshareAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("api_url", API_URL);
    conf.put("year", "2008");
    return conf;
  }

  // the metadata that should be extracted
  //String goodMultiDOI = "doi:http://dx.doi.org/10.1371/journal.pntd.0000938.g001";
  //String goodDOI = "doi:http://dx.doi.org/10.1371/journal.pntd.0000938.g001";
  String goodDOI = "10.1371/journal.pntd.0000938.g002";
  String goodOtherDOI = "doi:10.6084/m9.figshare.944591";
  // will be normalized without the "doi:"
  String normalizedOtherDOI = "10.6084/m9.figshare.944591";
  String anotherDOIType = "doi:http://dx.doi.org/10.6084/m9.figshare.105663";
  String anotherNormalizedOtherDOI = "10.6084/m9.figshare.944591";
  
  String goodPLOSDOI1 = "doi:http://dx.doi.org/10.1371/journal.pone.0029374.s001";
  String goodPLOSDOI2 = "doi:http://dx.doi.org/10.1371/journal.pone.0029374.s002";
  String goodPLOSDOI3 = "doi:http://dx.doi.org/10.1371/journal.pone.0029374.s003";
  String goodNormPLOSDOI1 = "10.1371/journal.pone.0029374.s001";
  String goodNormPLOSDOI2 = "10.1371/journal.pone.0029374.s002";
  String goodNormPLOSDOI3 = "10.1371/journal.pone.0029374.s003";
  
  String goodDate = "02:16, Jan 04, 2011";
  String goodAuthorA = "Mar’a Betson";
  String goodAuthorB = "Julia Arinaitwe";
  String goodAuthorC = "Stothard, Alba";
  String goodArticleTitle = "meiosis, ketosis and symbiosis";

  
  // a chunk of metadata html source code from where the 
  // metadata should be extracted

  String goodContent = 
    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n"
    + "<meta name=\"citation_author\" content=\""+goodAuthorA+"\">\n"
    + "<meta name=\"citation_author\" content=\""+goodAuthorB+"\">\n"
    + "<meta name=\"citation_author\" content=\""+goodAuthorC+"\">\n"
    + "<meta name=\"citation_title\" content=\""+ goodArticleTitle + "\">\n"
    + "<meta name=\"citation_publication_date\" content=\"" + goodDate+"\">\n"    
    + "<meta name=\"citation_doi\" content=\"" + goodDOI + "\"/>"
    ;
  String goodMultiContent =
    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n"
    + "<meta name=\"citation_author\" content=\""+goodAuthorA+"\">\n"
    + "<meta name=\"citation_author\" content=\""+goodAuthorB+"\">\n"
    + "<meta name=\"citation_author\" content=\""+goodAuthorC+"\">\n"
    + "<meta name=\"citation_title\" content=\""+ goodArticleTitle + "\">\n"
    + "<meta name=\"citation_doi\" content=\"" + goodOtherDOI + "\"/>"
    + "<meta name=\"citation_publication_date\" content=\"" + goodDate+"\">\n" 
    + "<meta name=\"PLOS_citation_doi\" content=\""+goodPLOSDOI1+"\" />\n"
    + "<meta name=\"PLOS_citation_doi\" content=\""+goodPLOSDOI2+"\" />\n"    
    + "<meta name=\"PLOS_citation_doi\" content=\""+goodPLOSDOI3+"\" />\n"
    ;       
  String moreMetadata = 
    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />" +
    "<meta name=\"citation_title\" content=\""+goodArticleTitle+"\">" +
    "<meta name=\"citation_author\" content=\""+goodAuthorA+"\">" +
    "<meta name=\"citation_doi\" content=\"doi:http://dx.doi.org/10.6084/m9.figshare.105663\" />" +
    "<meta name=\"citation_publication_date\" content=\""+goodDate+"\">" ;
 
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the PensoftHtmlMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new FigshareHtmlMetadataExtractorFactory.FigshareHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(Arrays.asList(goodAuthorA, goodAuthorB, goodAuthorC), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    log.info("testExtractFromGoodContent -");
  }
  public void testExtractFromMultiContentContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art4/";
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    cu.setContent(goodMultiContent);
    cu.setContentSize(goodMultiContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new FigshareHtmlMetadataExtractorFactory.FigshareHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(Arrays.asList(goodAuthorA, goodAuthorB, goodAuthorC), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(Arrays.asList(goodNormPLOSDOI1,goodNormPLOSDOI2,goodNormPLOSDOI3), md.getList(FigshareHtmlMetadataExtractor.FIGSHARE_PLOS_DOI));
    assertEquals(normalizedOtherDOI, md.get(MetadataField.FIELD_DOI));
    log.info("testExtractFromMoreGoodContent -");
  } 
 
  // a chunk of html source code from where the PensoftHtmlMetadataExtractorFactory should NOT be able to extract metadata
  String badContent = "<HTML><HEAD><TITLE>"
    + goodArticleTitle
    + "</TITLE></HEAD><BODY>\n"
    + "<meta name=\"foo\""
    + " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + " </div>\n";

  /**
   * Method that creates a simulated Cached URL from the source code provided by the badContent Sring. It then asserts that NO metadata is extracted by using 
   * the NatureHtmlMetadataExtractorFactory as the source code is broken.
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new FigshareHtmlMetadataExtractorFactory.FigshareHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertEquals(1, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
    log.info("testExtractFromBadContent -");

  }     

}
