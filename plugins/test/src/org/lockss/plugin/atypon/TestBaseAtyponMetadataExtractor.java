/*
 * $Id: TestBaseAtyponMetadataExtractor.java,v 1.3 2013-07-31 21:43:56 alexandraohlson Exp $
 */
/*

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.plugin.copernicus.CopernicusRisMetadataExtractorFactory;


public class TestBaseAtyponMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestBaseAtyponMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.atypon.BaseAtyponPlugin";
  private static String BASE_URL = "http://www.baseatypon.org/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, auConfig());
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
    conf.put("journal_id", "xxxx");
    conf.put("volume_name","123");
    return conf;
  }
  
  // the metadata that should be extracted
  String goodDate = "2012-07-05";
  String[] goodAuthors = new String[] {"D. Author", "S. Author2"};
  String goodFormat = "text/HTML";
  String goodTitle = "Title of Article";
  String goodType = "research-article";
  String goodPublisher = "Base Atypon";
  String goodPublishingPlatform = "Atypon";
  String goodDOI = "10.1137/10081839X";
  
  String goodJournal = "Journal Name"; 
  String goodStartPage = "22";
  String goodEndPage = "44";
  String goodVolume = "13";
  String goodIssue = "3";
  String goodIssn = "1540-3459";
  String goodURL = "http://dx.doi.org/" + goodDOI;

  /*
   "<meta name="dc.Title" content="Title of Article"></meta>
   "<meta name="dc.Creator" content="D. Author"></meta>
   "<meta name="dc.Creator" content="S. Author2"></meta>
   "<meta name="dc.Subject" content="weighted regularity; elliptic problem; oscillatory diffusion; $hp$ finite elements; 65N30; 35B65; 35J57"></meta>
   "<meta name="dc.Description" content="Long test summary of article, probably taken directly from the adstract..."></meta>
   "<meta name="dc.Publisher" content="Name of Publisher"></meta>
   "<meta name="dc.Date" scheme="WTN8601" content="2012-07-05"></meta>
   "<meta name="dc.Type" content="research-article"></meta>
   "<meta name="dc.Format" content="text/HTML"></meta>
   "<meta name="dc.Identifier" scheme="publisher" content="81839"></meta>
   "<meta name="dc.Identifier" scheme="doi" content="10.1137/10081839X"></meta>
   "<meta name="dc.Source" content="http://dx.doi.org/10.1137/10081839X"></meta>
   "<meta name="dc.Language" content="en"></meta>
   "<meta name="dc.Coverage" content="world"></meta>
   "<meta name="keywords" content="weighted regularity, elliptic problem, oscillatory diffusion, $hp$ finite elements, 65N30, 35B65, 35J57"></meta>
   */

  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted

  String goodHtmlContent = 
      "<meta name=\"dc.Title\" content=\"Title of Article\"></meta>" +
   "<meta name=\"dc.Creator\" content=\"D. Author\"></meta>" +
   "<meta name=\"dc.Creator\" content=\"S. Author2\"></meta>" +
   "<meta name=\"dc.Subject\" content=\"weighted regularity; elliptic problem; oscillatory diffusion; $hp$ finite elements; 65N30; 35B65; 35J57\"></meta>" +
   "<meta name=\"dc.Description\" content=\"Long test summary of article, probably taken directly from the adstract...\"></meta>" +
   "<meta name=\"dc.Publisher\" content=\"Base Atypon\"></meta>" +
   "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"2012-07-05\"></meta>" +
   "<meta name=\"dc.Type\" content=\"research-article\"></meta>" +
   "<meta name=\"dc.Format\" content=\"text/HTML\"></meta>" +
   "<meta name=\"dc.Identifier\" scheme=\"publisher\" content=\"81839\"></meta>" +
   "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"10.1137/10081839X\"></meta>" +
   "<meta name=\"dc.Source\" content=\"http://dx.doi.org/10.1137/10081839X\"></meta>" +
   "<meta name=\"dc.Language\" content=\"en\"></meta>" +
   "<meta name=\"dc.Coverage\" content=\"world\"></meta>" +
   "<meta name=\"keywords\" content=\"weighted regularity, elliptic problem, oscillatory diffusion, $hp$ finite elements, 65N30, 35B65, 35J57\"></meta>";	
   

  public void testExtractGoodHtmlContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new BaseAtyponHtmlMetadataExtractorFactory.BaseAtyponHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodType, md.get(MetadataField.DC_FIELD_TYPE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors[0], md.get(MetadataField.DC_FIELD_CREATOR));

  }
  
  String goodHtmlContentNoDOIorPublisher = 
      "<meta name=\"dc.Title\" content=\"Title of Article\"></meta>" +
   "<meta name=\"dc.Creator\" content=\"D. Author\"></meta>" +
   "<meta name=\"dc.Creator\" content=\"S. Author2\"></meta>" +
   "<meta name=\"dc.Subject\" content=\"weighted regularity; elliptic problem; oscillatory diffusion; $hp$ finite elements; 65N30; 35B65; 35J57\"></meta>" +
   "<meta name=\"dc.Description\" content=\"Long test summary of article, probably taken directly from the adstract...\"></meta>" +
   "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"2012-07-05\"></meta>" +
   "<meta name=\"dc.Type\" content=\"research-article\"></meta>" +
   "<meta name=\"dc.Format\" content=\"text/HTML\"></meta>" +
   "<meta name=\"dc.Identifier\" scheme=\"publisher\" content=\"81839\"></meta>" +
   "<meta name=\"dc.Language\" content=\"en\"></meta>" +
   "<meta name=\"dc.Coverage\" content=\"world\"></meta>" +
   "<meta name=\"keywords\" content=\"weighted regularity, elliptic problem, oscillatory diffusion, $hp$ finite elements, 65N30, 35B65, 35J57\"></meta>";       
   

  public void testDOIExtraction() throws Exception {
    String url = BASE_URL + "doi/abs/10.1175/2010WCAS1063.1";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodHtmlContentNoDOIorPublisher);
    cu.setContentSize(goodHtmlContentNoDOIorPublisher.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new BaseAtyponHtmlMetadataExtractorFactory.BaseAtyponHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    // gets pulled from the URL if not set in the metadata
    assertEquals("10.1175/2010WCAS1063.1", md.get(MetadataField.FIELD_DOI));
    // gets set manually if not in the metadata
    // first it would try the TDB (which won't exist for the Base Atypon plugin)
    // next it would pull the publishing platform out of the plugin, which is better than nothing...
    // Need to figure this out if based on Atypon Base
   assertEquals(goodPublishingPlatform, md.get(MetadataField.FIELD_PUBLISHER));
    
  }
  
  private String createGoodRisContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    for(String auth : goodAuthors) {
      sb.append("\nA1  - ");
      sb.append(auth);
    }
    sb.append("\nDA  - ");
    sb.append(goodDate);
    sb.append("\nJF  - ");
    sb.append(goodJournal);
    sb.append("\nSP  - ");
    sb.append(goodStartPage);
    sb.append("\nEP  - ");
    sb.append(goodEndPage);
    sb.append("\nVL  - ");
    sb.append(goodVolume);
    sb.append("\nIS  - ");
    sb.append(goodIssue);
    sb.append("\nSN  - ");
    sb.append(goodIssn);
    sb.append("\nT1  - ");
    sb.append(goodTitle);
    sb.append("\nPB  - ");
    sb.append(goodPublisher);
    sb.append("\nDO  - ");
    sb.append(goodDOI);
    sb.append("\nUR  - ");
    sb.append(goodURL);
    sb.append("\nER  -");
    return sb.toString();
  }
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the MetaPressRisMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractGoodRisContent() throws Exception {
    String goodContent = createGoodRisContent();
    log.debug3(goodContent);
    String url = BASE_URL + "action/downloadCitation?doi=" + goodDOI + "&format=ris&include=cit";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
    FileMetadataExtractor me = new BaseAtyponRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/plain");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEndPage, md.get(MetadataField.FIELD_END_PAGE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    Iterator<String> actAuthIter = md.getList(MetadataField.FIELD_AUTHOR).iterator();
    for(String expAuth : goodAuthors) {
      assertEquals(expAuth, actAuthIter.next());
    }
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));

    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodURL, md.get(MetadataField.FIELD_ACCESS_URL));

  }
  
  /* the extractor checks if data is missing it uses possible alternate RIS tags */
  private String createAlternateRisContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    for(String auth : goodAuthors) {
      sb.append("\nAU  - ");
      sb.append(auth);
    }
    sb.append("\nY1  - ");
    sb.append(goodDate);
    sb.append("\nT2  - ");
    sb.append(goodJournal);
    sb.append("\nT1  - ");
    sb.append(goodTitle);
    sb.append("\nPB  - ");
    sb.append(goodPublisher);
    sb.append("\nER  -");
    return sb.toString();
  }
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the MetaPressRisMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractAlternateRisContent() throws Exception {
    String goodContent = createAlternateRisContent();
    log.debug3(goodContent);
    String url = BASE_URL + "action/downloadCitation?doi=" + goodDOI + "&format=ris&include=cit";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
    FileMetadataExtractor me = new BaseAtyponRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/plain");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    Iterator<String> actAuthIter = md.getList(MetadataField.FIELD_AUTHOR).iterator();
    for(String expAuth : goodAuthors) {
      assertEquals(expAuth, actAuthIter.next());
    }
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }


}
