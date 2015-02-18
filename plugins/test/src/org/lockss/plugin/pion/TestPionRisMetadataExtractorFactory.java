/* $Id$

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pion;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * Tests PeerJ ris metadata. ClockssPion ris file looks like:
 *  TY  - JOUR
 *  A1  - A., Author1
 *  A1  - B., Author2
 *  Y1  - 2013
 *  T1  - article title
 *  JO  - journal name
 *  SP  - e173
 *  EP  - e177
 *  VL  - 1
 *  IS  - 3
 *  UR  - http://www.envplan.com/abstract.cgi?id=a12345
 *  PB  - Pion Ltd
 *  SN  - 1111-2222
 *  N2  - article abstract
 *  DO  - 10.5194/cp-8-1-2012
 *  ER  - 
 */
public class TestPionRisMetadataExtractorFactory extends LockssTestCase {

  static Logger log = 
      Logger.getLogger(TestPionRisMetadataExtractorFactory.class);

  private MockLockssDaemon theDaemon;
  private ArchivalUnit pjau; // peerj au
  
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.pion.ClockssPionPlugin";
  
  private static final String BASE_URL = "http://i-perception.perceptionweb.com/";
  private static final String JOURNAL_CODE = "epa";
  private static final String SHORT_JOURNAL_CODE = "A";
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
    
    pjau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, pionAuConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,  pionAuConfig());
  }

  private Configuration pionAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_code", JOURNAL_CODE);
    conf.put("short_journal_code", SHORT_JOURNAL_CODE);
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }

  // the metadata that should be extracted
  String goodDoi = "10.1068/a12345";
  String goodAccessUrl = "http://www.envplan.com/abstract.cgi?id=a12345";
  String goodAuthor1 = "A, Author1";
  String goodAuthor2 = "B, Author2";
  List<String> goodAuthors = Arrays.asList("A, Author1", "B, Author2");
  String goodArticleTitle = "Article Title";
  String goodYear = "2013";
  String goodAbstract = "Article abstract";
  String goodVolume = "Volume 1";
  String goodStartPage = "Article start page";
  String goodJournalTitle = "Journal Title";
  String goodIssn = "1111-2222";
  
  private String createGoodContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    sb.append("\nUR  - ");
    sb.append(goodAccessUrl);
    sb.append("\nT1  - ");
    sb.append(goodArticleTitle);
    sb.append("\nA1  - ");
    sb.append(goodAuthor1);
    sb.append("\nA1  - ");
    sb.append(goodAuthor2);
    sb.append("\nY1  - ");
    sb.append(goodYear);
    sb.append("\nN2  - ");
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
    
    // Create FileMetadataExtractor object through PionRisMetadataExtractor()
    FileMetadataExtractor me = 
        new PionRisMetadataExtractorFactory()
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
    assertEquals(goodYear, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodAbstract, md.get(MetadataField.FIELD_ABSTRACT));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodAccessUrl, md.get(MetadataField.FIELD_ACCESS_URL));
  }
  /*
  String realRISFile = "pion.ris";
  String realPDFFile = "pion.pdf";

  public void testFromRealRISFile() throws Exception {
    CIProperties risHeader = new CIProperties();
    InputStream file_input = null;
    MockArchivalUnit mau; // source au
    mau = new MockArchivalUnit();
    mau.setConfiguration(pionAuConfig());
    log.info("testing Real RIS File");
    try {
      file_input = getResourceAsStream(realRISFile);
      //String string_input = StringUtil.fromInputStream(file_input);
      String string_input = StringUtil.fromReader(new InputStreamReader(file_input, Constants.ENCODING_UTF_8));
 log.info("string: "+string_input);     
      IOUtil.safeClose(file_input);

      String ris_url = BASE_URL + realRISFile;
      risHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/ris");
      MockCachedUrl ris_cu = mau.addUrl(ris_url, true, true, risHeader);
      // need to check for this file before emitting
      
      ris_cu.setContent(string_input);
      ris_cu.setContentSize(string_input.length());
      ris_cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/ris");
      
      
      file_input = getResourceAsStream(realRISFile);
      string_input = StringUtil.fromReader(new InputStreamReader(file_input, Constants.ENCODING_UTF_8));
      String pdf_url = BASE_URL + realRISFile;
      MockCachedUrl pdf_cu = mau.addUrl(pdf_url, true, true, risHeader);
      // need to check for this file before emitting
      
      pdf_cu.setContent(string_input);
      pdf_cu.setContentSize(string_input.length());
      pdf_cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

      FileMetadataExtractor me = new PionRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "application/ris");

      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), ris_cu);
      assertNotEmpty(mdlist);

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
log.info("mdRecord: "+mdRecord.toString());  
log.info("pub title: " + mdRecord.get(MetadataField.FIELD_ARTICLE_TITLE));

      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }
  */
}