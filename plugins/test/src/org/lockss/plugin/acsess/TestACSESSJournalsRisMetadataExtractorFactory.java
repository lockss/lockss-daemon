/* $Id$

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.acsess;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * Example from https://dl.sciencesocieties.org/publications/aj/articles/106/4/1470:
TY  - JOUR
T2  - Agronomy Journal
TI  - Alfalfa Pasture Bloat Can Be Eliminated by Intermixing with Newly-Developed Sainfoin Population
AU  - Sottie, E. T.
AU  - Acharya, S. N.
AU  - McAllister, T.
AU  - Thomas, J.
AU  - Wang, Y.
AU  - Iwaasa, A.
PB  - The American Society of Agronomy, Inc.
PY  - 2014
CY  - Madison, WI
SN  - 
DO  - 10.2134/agronj13.0378
UR  - http://dx.doi.org/10.2134/agronj13.0378
LA  - English
SP  - 1470
EP  - 1478
AB  - Pasture bloat is a major deterrent to the grazing of alfalfa (Medicago sativa L.) pastures by ruminants, despite the high growth rates that are obtainable. Sainfoin (Onobrychis viciifolia Scop.), a condensed tannin-containing legume, is known to reduce alfalfa pasture bloat in mixed stands. Experiments were conducted in 2010 to 2012 at Lethbridge, AB, using two sainfoin/cultivar AC Blue J alfalfa mixed pastures originally seeded as 50:50 mixes in 2008 and rotationally grazed by steers (Bos taurus). New sainfoin population cultivar LRC-3519 specifically selected for improved performance under a multi-cut system, persisted better (29% of total herbage dry matter [DM]) compared to cultivar Nova (5%) after two cycles of rotational grazing in each year. Bloat incidence and severity in steers were reduced (P < 0.001) by 98% in LRC-3519 mixed stands compared to Nova mixtures when Angus steers grazed sainfoin/alfalfa mixed pastures under conditions for maximizing bloat occurrence. In a separate crop preference study during 2011–2012, eight steers were randomly paired and assigned to four alfalfa and sainfoin strip pastures to determine DM utilization and time spent grazing the two forages. Steers spent more time (55%) in alfalfa strips compared to sainfoin and grazed more (P < 0.05) alfalfa (2048 kg DM) than sainfoin (1164 kg DM). In spite of higher preference for alfalfa, a high proportion of new sainfoin in mixed alfalfa stands reduced risk of bloat substantially in cattle grazing mixed alfalfa/sainfoin pasture.
VL  - 106
M1  - 4
ER  - 
 */
public class TestACSESSJournalsRisMetadataExtractorFactory extends LockssTestCase {

  private MockLockssDaemon theDaemon;
  private ArchivalUnit aau; // ascess au
  
 private static final String PLUGIN_ID = 
      "org.lockss.plugin.acsess.ClockssACSESSJournalsPlugin";
  
  private static final String BASE_URL = "https://www.example.com/";
  private static final String JID = "xxxjid";  
  private static final String VOL = "2014";
    
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace(); // must be set
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    aau = PluginTestUtil.createAndStartAu(PLUGIN_ID, acsessAuConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID,  acsessAuConfig());
  }

  private Configuration acsessAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JID);
    conf.put("volume_name", VOL);
    return conf;
  }

  // the metadata that should be extracted
  String goodPublicationTitle = "XYZ Journal";
  String goodArticleTitle = "Article Title";
  String goodAuthor1 = "A, Author1";
  String goodAuthor2 = "B, Author2";
  List<String> goodAuthors = Arrays.asList("A, Author1", "B, Author2");
  String goodPublisher = "A Publisher";
  String goodDate = "2014";  
  String goodDoi = "10.1111/example.173";  
  String goodLanguage = "English";
  String goodStartPage = "1000";
  String goodEndPage = "1050";
  String goodAbstract = "Article abstract";
  String goodVolume = "1";
  String goodIssue = "4";
  
  private String createGoodContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    sb.append("\nT2  - " + goodPublicationTitle);
    sb.append("\nTI  - " + goodArticleTitle);
    sb.append("\nAU  - " + goodAuthor1);
    sb.append("\nAU  - " + goodAuthor2);
    sb.append("\nPB  - " + goodPublisher);
    sb.append("\nPY  - " + goodDate);    
    sb.append("\nDO  - " + goodDoi);
    sb.append("\nLA  - " + goodLanguage);
    sb.append("\nSP  - " + goodStartPage);
    sb.append("\nEP  - " + goodEndPage);
    sb.append("\nAB  - " + goodAbstract);
    sb.append("\nVL  - " + goodVolume);
    sb.append("\nM1  - " + goodIssue);
    sb.append("\nER  -");
    return sb.toString();
  }
      
  // Creates a simulated Cached URL from the source code 
  // provided by the goodContent string. It then asserts that the metadata 
  // extracted with ACSESSJournalsRisMetadataExtractorFactory
  // match the metadata in the source code. 
  public void testExtractFromGoodContent() throws Exception {
    String goodContent = createGoodContent();
    //log.info(goodContent);
    // ris file url: https://dl.sciencesocieties.org/publications/citation-manager/down/pc/aj/106/4/1470
    String url = BASE_URL + "publications/citation-manager/down/pc/aj/999/9/9999";
    MockCachedUrl cu = new MockCachedUrl(url, aau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_RIS);
    
    // Create FileMetadataExtractor object through ACSESSJournalsRisMetadataExtractor()
    FileMetadataExtractor me = 
        new ACSESSJournalsRisMetadataExtractorFactory()
                .createFileMetadataExtractor(MetadataTarget.Any(), 
                                             Constants.MIME_TYPE_RIS);
    // Create the metadata list containing all articles for this AU.
    // In this test case, the list has only one item.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodPublicationTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));    
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodLanguage, md.get(MetadataField.FIELD_LANGUAGE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEndPage, md.get(MetadataField.FIELD_END_PAGE));
    assertEquals(goodAbstract, md.get(MetadataField.FIELD_ABSTRACT));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
  }
  
}