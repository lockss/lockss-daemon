/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americaninstituteofphysics;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.util.ListUtil;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;

/*
 * Full-text and metadata XML:
 *      "<base_url>/<year>/<zip_file_name>.zip!/
 *              <journal_id>/<volume_num>/<issue_num>/
 *              <article_num>/Markup/<xml_file_name>.xml"
 *
 *      "http://clockss-ingest.lockss.org/sourcefiles/aipjats-released/
 *              2013/test_76_clockss_aip_2013-06-07_084326.zip!/
 *              JAP/v111/i11/112601_1/Markup/VOR_10.1063_1.4726155.xml"
 * 
 * PDF:
 *      "<base_url>/<year>/<zip_file_name>.zip!/
 *              <journal_id>/<volume_num>/<issue_num>/
 *              <article_num>/Page_Renditions/online.pdf"
 *
 *      "http://clockss-ingest.lockss.org/sourcefiles/aipjats-released/
 *              2013/test_76_clockss_aip_2013-06-07_084326.zip!/
 *              JAP/v111/i11/112601_1/Page_Renditions/online.pdf"
 */
public class TestAIPJatsSourceArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau; // simulated au to generate content
//  private static final String PLUGIN_NAME =
//    "org.lockss.plugin.americaninstituteofphysics.ClockssAIPJatsSourcePlugin";
	
  private static final String PLUGIN_NAME =
      "org.lockss.plugin.americaninstituteofphysics.ClockssAIPJatsSourcePlugin";
  private final String ARTICLE_FAIL_MSG = "Article files not created properly";
  private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_NAME_KEY = "journal_name";
  private final String BASE_URL = "http://www.example.com/";
  private final String YEAR = "2013";
  private final String JOURNAL_NAME = "PR0";

  private static final int DEFAULT_FILESIZE = 3000;
  private final Configuration AU_CONFIG = 
      ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
      YEAR_KEY, YEAR);
  /*
  public TestAIPJatsSourceArticleIteratorFactory() throws Exception {
    super.setUp();
    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }
  */
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    
    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));

  }
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return 
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("year", YEAR);
    conf.put("depth", "1");
    conf.put("branch", "3");
    conf.put("numFiles", "2");
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_XML
            | SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }
/*
  private ArchivalUnit createAu() 
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, aipJatsSourceAuConfig());
  }

  private Configuration aipJatsSourceAuConfig() {
    return ConfigurationUtil.fromArgs("base_url",
        "http://www.example.com/",
        "year", "2013");
  }
   */ 

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // PATTERN_TEMPLATE = 
    // "\"%s%d/[^/]+\\.zip!/[A-Z]+/v[0-9]+/i[0-9]+/[0-9]+_[0-9]+/
    //       [Markup|Page_Renditions|Components]/(Figures/)?.+[pdf|zip|jpg]$\",
    //            base_url, year";
    assertNotMatchesRE(pat,
        "http://www.example.com/2013/test_76_clockss_aip_2013-06-07_084326.zip"
        + "!/JAP/v111/i11/112601_1/Page_Renditions/online.pdfbad");
    assertNotMatchesRE(pat,
        "http://www.example.com/2013/test_76_clockss_aip_2013-06-07_084326.zip"
        + "!/JAP/v111/i11/112601_1/Page_Renditions/online.pdf");
    assertMatchesRE(pat, 
        "http://www.example.com/2013/test_76_clockss_aip_2013-06-07_084326.zip"
        + "!/JAP/v111/i11/112601_1/Markup/VOR_10.1063_1.4726155.xml");
  }
  
  // There's only one pdf file per simcontent branch because it has 
  // a constant name online.pdf. Each branch has 1 xml files and 1 pdf.
  //  	http://www.example.com/1/Markup/001file.xml
  //  	http://www.example.com/1/Page_Renditions/online.pdf
  //  	http://www.example.com/2/Markup/001file.xml
  //  	http://www.example.com/2/Page_Renditions/online.pdf
  //  	http://www.example.com/3/Markup/001file.xml
  //  	http://www.example.com/3/Page_Renditions/online.pdf
  // expCount = 6; // 1 depth, 3 branches, 2 files

  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    String pat1 = "branch(\\d+)/(\\d+file)\\.xml";
    String rep1 = "/$1/Markup/$2.xml";
    PluginTestUtil.copyAu(sau, au, ".*\\.xml$", pat1, rep1);

    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);

    int expXmlCount = 6;
    int expPdfCount = 0;
    int xmlCount = 0;
    int pdfCount = 0;
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        log.debug3("url: " + url);
        if (url.contains("/Markup/")) {
          if (url.endsWith("1/Markup/001file.xml")) {
            //verifyArticleFile(cu);
            log.debug3(url);
          }
          xmlCount++;
        } else if (url.contains("Page_Renditions")) {
          pdfCount++;
        }
    }
    //log.info("Article count is " + count);
    log.debug3("xml count is " + xmlCount);
    log.debug3("pdf count is " + pdfCount);

    //assertEquals(expXmlCount, xmlCount);
    //assertEquals(expPdfCount, pdfCount);
  }
 
 
}