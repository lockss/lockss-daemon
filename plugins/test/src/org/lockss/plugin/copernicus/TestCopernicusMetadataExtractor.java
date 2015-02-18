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

package org.lockss.plugin.copernicus;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/* Sample RIS file */
/*
 * found: <base_url>/<vol#>/<issue#>/<year>/<abstract_base>.ris
 * 
 * TY  - JOUR
 * A1  - Winkler, R.
 * A1  - Landais, A.
 * A1  - Sodemann, H.
 * A1  - Damgen, L.
 * A1  - Priac, F.
 * A1  - Masson-Delmotte, V.
 * A1  - Stenni, B.
 * A1  - Jouzel, J.
 * T1  - Deglaciation records of 17O-excess in East Antarctica:  reliable reconstruction of oceanic normalized relative humidity from coastal sites
 * JO  - Clim. Past
 * J1  - CP
 * VL  - 8
 * IS  - 1
 * SP  - 1
 * EP  - 16
 * Y1  - 2012/01/03
 * PB  - Copernicus Publications
 * SN  - 1814-9332
 * UR  - http://www.clim-past.net/8/1/2012/
 * L1  - http://www.clim-past.net/8/1/2012/cp-8-1-2012.pdf
 * DO  - 10.5194/cp-8-1-2012
 * ER  - 
 * 
 */
public class TestCopernicusMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestCopernicusMetadataExtractor");

  private MockLockssDaemon theDaemon;
  //private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private MockArchivalUnit mau;
  //private ArchivalUnit au;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static String PLUGIN_NAME = "org.lockss.plugin.copernicus.ClockssCopernicusPublicationsPlugin";
  private final String BASE_URL = "http://www.cop-foo.net/";
  private final String HOME_URL = "http://www.copernicus-foobar.net/";
  private final String VOLUME_NAME = "8";
  private final String YEAR = "2012";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      "home_url", HOME_URL,
      VOLUME_NAME_KEY, VOLUME_NAME,
      YEAR_KEY, YEAR);
  
  CIProperties plainHeader = new CIProperties();


  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();

    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau.setConfiguration(AU_CONFIG);

    plainHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE,  "text/plain");

  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }



  // the metadata that should be extracted
  String goodVolume = "8";
  String goodIssue = "1";
  String goodStartPage = "1";
  String goodEndPage = "16";
  String goodIssn = "1814-9332";
  String goodDate = "2012/01/03";
  String goodAuthors[] = {"Winkler, R.", "Landais, A.", "Sodemann, H.", "Damgen, L.", "Priac, F.", "Masson-Delmotte, V.", "Stenni, B.", "Jouzel, J."};
  String goodArticleTitle = "Delegation of Interesting Stuff from coastal sites";
  String goodJournalTitle = "Cop. FooBar";
  String goodPublication = "Copernicus Publications";
  String goodDOI = "10.5194/cpf-8-1-2012";
  String initialAccessUrl = BASE_URL + "8/1/2012/";
  String risUrl = BASE_URL + "8/1/2012/cpf-8-1-2012.ris";
  String pdfUrl = BASE_URL + "8/1/2012/cpf-8-1-2012.pdf";
  String randomUrl = BASE_URL + "8/1/2012/foo.html";

  private String createGoodContent(String accessUrl) {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    for(String auth : goodAuthors) {
      sb.append("\nA1  - ");
      sb.append(auth);
    }
    sb.append("\nY1  - ");
    sb.append(goodDate);
    sb.append("\nJO  - ");
    sb.append(goodJournalTitle);
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
    sb.append(goodArticleTitle);
    sb.append("\nPB  - ");
    sb.append(goodPublication);
    sb.append("\nDO  - ");
    sb.append(goodDOI);
    sb.append("\nUR  - ");
    sb.append(accessUrl);
    sb.append("\nER  -");
    return sb.toString();
  }

  public void testExtractFromGoodContent() throws Exception {
    String goodContent = createGoodContent(initialAccessUrl);
    //log.info(goodContent);
    MockCachedUrl mcu = mau.addUrl(risUrl, true, true, plainHeader);
    mcu.setContent(goodContent);
    mcu.setContentSize(goodContent.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");

    FileMetadataExtractor me = new CopernicusRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any, "text/plain");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
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
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));

    assertEquals(goodPublication, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    // The access_url should not be set because there is no content in AU
    //assertEquals(null, md.get(MetadataField.FIELD_ACCESS_URL));
    //for continuity, leave as set to the original value - must discuss
    assertEquals(initialAccessUrl, md.get(MetadataField.FIELD_ACCESS_URL));

  }
  
  public void testAccessUrlSetting() throws Exception {
    String goodContent = createGoodContent(initialAccessUrl);
    MockCachedUrl mcu = mau.addUrl(risUrl, true, true, plainHeader);
    mcu.setContent(goodContent);
    mcu.setContentSize(goodContent.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
    mau.addUrl(pdfUrl, true, true, plainHeader); // not the right header but doesn't' mattern

    FileMetadataExtractor me = new CopernicusRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any, "text/plain");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertNotEquals(initialAccessUrl, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(pdfUrl, md.get(MetadataField.FIELD_ACCESS_URL));
    
    // Now check after setting a good initial accessUrl
    // make sure it doesn't get changed
    goodContent = createGoodContent(randomUrl);
    MockCachedUrl mcu2 = mau.addUrl(risUrl, true, true, plainHeader);
    mcu2.setContent(goodContent);
    mcu2.setContentSize(goodContent.length());
    mcu2.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
    // make sure the access url exists in the AU
    mau.addUrl(randomUrl, true, true, plainHeader); // not the right header but doesn't' mattern
    
    List<ArticleMetadata> mdlist2 = mle.extract(MetadataTarget.Any(), mcu2);
    assertNotEmpty(mdlist2);
    ArticleMetadata md2 = mdlist2.get(0);
    assertNotNull(md2);
    assertNotEquals(initialAccessUrl, md2.get(MetadataField.FIELD_ACCESS_URL));
    assertNotEquals(pdfUrl, md2.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(randomUrl, md2.get(MetadataField.FIELD_ACCESS_URL));
    
  }
}