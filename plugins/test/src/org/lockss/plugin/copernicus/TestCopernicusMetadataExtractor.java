/*
 * $Id: TestCopernicusMetadataExtractor.java,v 1.1 2012-11-15 21:36:52 alexandraohlson Exp $
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
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit au;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static String PLUGIN_NAME = "org.lockss.plugin.copernicus.ClockssCopernicusPublicationsPlugin";
  private final String BASE_URL = "http://www.clim-past.net/";
  private final String HOME_URL = "http://www.climate-of-the-past.net/";
  private final String VOLUME_NAME = "8";
  private final String YEAR = "2012";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
                                                                                          BASE_URL_KEY, BASE_URL,
                                                                                          "home_url", HOME_URL,
                                                                                          VOLUME_NAME_KEY, VOLUME_NAME,
                                                                                          YEAR_KEY, YEAR);

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
    au = PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
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
    conf.put("depth", "1");
    conf.put("branch", "5");
    conf.put("numFiles", "4");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_HTML |
            SimulatedContentGenerator.FILE_TYPE_PDF | 
            SimulatedContentGenerator.FILE_TYPE_TXT));
    return conf;
  }

  // the metadata that should be extracted
  String goodVolume = "8";
  String goodIssue = "1";
  String goodStartPage = "1";
  String goodEndPage = "16";
  String goodIssn = "1814-9332";
  String goodDate = "2012/01/03";
  String goodAuthors[] = {"Winkler, R.", "Landais, A.", "Sodemann, H.", "Damgen, L.", "Priac, F.", "Masson-Delmotte, V.", "Stenni, B.", "Jouzel, J."};
  String goodArticleTitle = "Degalciation records of 170-excess in East Antarctica: relaiable construction of oceanic normalized relative humidity from coastal sites";
  String goodJournalTitle = "Clim. Past";
  String goodPublication = "Copernicus Publications";
  String goodDOI = "10.5194/cp-8-1-2012";
  String goodURL = "http://www.clim-past.net/8/1/2012/";

  private String createGoodContent() {
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
  public void testExtractFromGoodContent() throws Exception {
	  String goodContent = createGoodContent();
	  log.info(goodContent);
    String url = "http://www.clim-past.net/8/1/2012/cp-8-1-2012.ris";
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
    FileMetadataExtractor me = new CopernicusRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any, "text/plain");
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
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
 
    assertEquals(goodPublication, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodURL, md.get(MetadataField.FIELD_ACCESS_URL));
    
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
   * Inner class to create a html source code simulated content
   *
   */
  public static class MySimulatedContentGenerator extends 
    SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

  }
}