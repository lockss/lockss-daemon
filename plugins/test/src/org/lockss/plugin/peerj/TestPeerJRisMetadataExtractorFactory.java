/* $Id$

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

package org.lockss.plugin.peerj;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * Tests PeerJ ris metadata. PeerJ ris file looks like:
 *  TY  - JOUR
 *  UR  - http://dx.doi.org/10.7717/xxxxx.173
 *  DO  - 10.7717/xxxxx.173
 *  TI  - article title
 *  AU  - A., Author1
 *  AU  - B., Author2
 *  A2  - C., Secondary Author
 *  DA  - 2013/09/26
 *  PY  - 2013
 *  KW  - kwd1
 *  KW  - kwd2
 *  KW  - kwd3
 *  KW  - kwd4
 *  KW  - kwd5
 *  KW  - kwd6
 *  AB  - article abstract
 *  VL  - 1
 *  SP  - e173
 *  T2  - xxxxx
 *  JO  - xxxxx
 *  J2  - xxxxx
 *  SN  - 1111-2222
 *  ER  - 
 */
public class TestPeerJRisMetadataExtractorFactory extends LockssTestCase {

  static Logger log = 
      Logger.getLogger(TestPeerJRisMetadataExtractorFactory.class);

  private MockLockssDaemon theDaemon;
  private ArchivalUnit pjau; // peerj au
  
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.peerj.PeerJPlugin";
  
  private static final String BASE_URL = "http://www.example.com/";
  private static final String VOLUME_NAME = "2013";
    
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace(); // must be set
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    pjau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, peerjAuConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME,  peerjAuConfig());
  }

  private Configuration peerjAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }

  // the metadata that should be extracted
  String goodDoi = "10.1111/example.173";
  String goodAuthor1 = "A, Author1";
  String goodAuthor2 = "B, Author2";
  String goodSecondaryAuthor = "C, SecondaryAuthor";
  List<String> goodAuthors = Arrays.asList("A, Author1", "B, Author2",
                                           "C, SecondaryAuthor");
  String goodArticleTitle = "Article Title";
  String goodDate = "2013/09/26";
  List<String> goodKeywords = Arrays.asList("kwd1", "kwd2", "kwd3");
  String goodAbstract = "Article abstract";
  String goodVolume = "Volume 1";
  String goodStartPage = "Article start page";
  String goodJournalTitle = "Journal Title";
  String goodIssn = "1111-2222";
  
  private String createGoodContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    sb.append("\nDO  - ");
    sb.append(goodDoi);
    sb.append("\nTI  - ");
    sb.append(goodArticleTitle);
    sb.append("\nAU  - ");
    sb.append(goodAuthor1);
    sb.append("\nAU  - ");
    sb.append(goodAuthor2);
    sb.append("\nA2  - ");
    sb.append(goodSecondaryAuthor);
    sb.append("\nDA  - ");
    sb.append(goodDate);
    for (String kwd : goodKeywords) {
      sb.append("\nKW  - ");
      sb.append(kwd);
    }
    sb.append("\nAB  - ");
    sb.append(goodAbstract);
    sb.append("\nVL  - ");
    sb.append(goodVolume);
    sb.append("\nSP  - ");
    sb.append(goodStartPage);
    sb.append("\nJO  - ");
    sb.append(goodJournalTitle);
    sb.append("\nSN  - ");
    sb.append(goodIssn);
    sb.append("\nER  -");
    return sb.toString();
  }
      
  // Method that creates a simulated Cached URL from the source code 
  // provided by the goodContent string. It then asserts that the metadata 
  // extracted with PalgraveBookRisMetadataExtractorFactory
  // match the metadata in the source code. 
  public void testExtractFromGoodContent() throws Exception {
    String goodContent = createGoodContent();
    //log.info(goodContent);
    // ris file url 
    String url = BASE_URL + "articles/55.ris";
    MockCachedUrl cu = new MockCachedUrl(url, pjau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_RIS);
    
    // Create FileMetadataExtractor object through PeerJRisMetadataExtractor()
    FileMetadataExtractor me = 
        new PeerJRisMetadataExtractorFactory()
                .createFileMetadataExtractor(MetadataTarget.Any(), 
                                             Constants.MIME_TYPE_RIS);
    // Create the metadata list containing all articles for this AU.
    // In this test case, the list has only one item.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertSameElements(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodAbstract, md.get(MetadataField.FIELD_ABSTRACT));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));

  }
  
}