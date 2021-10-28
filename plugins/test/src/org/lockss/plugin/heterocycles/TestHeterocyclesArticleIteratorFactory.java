/* 
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.heterocycles;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.daemon.SingleNodeCachedUrlSetSpec;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;

/*
 * article content may look like:
 * <heterocyclesbase>.com/clockss/libraries/fulltext/21568/83/1
 * <heterocyclesbase>.com/clockss/libraries/abst/21568/83/1 (hidden url)
 * <heterocyclesbase>.com/clockss/downloads/PDF/23208/83/1
 * <heterocyclesbase>.com/clockss/downloads/PDFwithLinks/23208/83/1
 * <heterocyclesbase>.com/clockss/downloads/PDFsi/23208/83/1
 */
public class TestHeterocyclesArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
        
  private static final String PLUGIN_NAME =
      "org.lockss.plugin.heterocycles.ClockssHeterocyclesPlugin";
   
  private static final String BASE_URL = "http://www.example.com/";
  private static final String VOLUME_NAME = "87";
  
  private static final int DEFAULT_FILESIZE = 3000;
  
  private static final int EXP_DELETED_FILE_COUNT = 8;
  private static final int EXP_FULL_TEXT_COUNT = 8; // full text pdf
  private static final int EXP_PDF_COUNT = 8; // after deleteBlock
  private static final int EXP_PDF_WITH_LINKS_COUNT = 8;
  private static final int EXP_PDF_SI_COUNT = 8;
  // EXP_HTML_COUNT/EXP_ABS_COUNT is 4 because, after deleteBlock, 
  // there are 8 full text cu articles to iterate,
  // including 4 deteled full text htmls
  private static final int EXP_HTML_COUNT = 4; 
  private static final int EXP_ABS_COUNT = 4; 
  private static final int EXP_HIDDEN_ABS_COUNT = 8;

  public void setUp() throws Exception {
    super.setUp();
    
    // au is protected archival unit from super class ArticleIteratorTestCase
    au = createAu(); 
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));

    ConfigurationUtil.addFromArgs(CachedUrl.PARAM_ALLOW_DELETE, "true");
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,  heterocyclesAuConfig());
  }
  
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "3");
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_PDF        
            | SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  private Configuration heterocyclesAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "clockss/"), getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // PATTERN_TEMPLATE = 
    //  "\"^%s(articles)/([0-9]+)(\\.pdf)?$\", base_url";
    
    assertNotMatchesRE(pat, 
        BASE_URL + "clockss/downloads/PDFbad/24689/" + VOLUME_NAME + "/1");
    assertMatchesRE(pat, 
        BASE_URL + "clockss/downloads/PDF/24689/" + VOLUME_NAME + "/1");
    assertMatchesRE(pat, 
        BASE_URL + "clockss/libraries/fulltext/24689/"  + VOLUME_NAME + "/1");
  }
  
  // simulated cached urls:
  // total number files = 24; // 1 depth, 4 branches, 3 files
  // 1003 means branch #1 and file  #3
  // there are 9 different file formats
  // 
  // full text pdf      - http://www.example.com/clockss/1003.pdf
  // abstract           - http://www.example.com/clockss/1003
  // full text xml      - http://www.example.com/clockss/1003.xml
  // citation bib       - http://www.example.com/clockss/1003.bib
  // citation ris       - http://www.example.com/clockss/1003.ris
  // alternate html     - http://www.example.com/clockss/1003.html
  // alternate rdf      - http://www.example.com/clockss/1003.rdf
  // alternate json     - http://www.example.com/clockss/1003.json
  // alternate unixref  - http://www.example.com/clockss/1003.unixref
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    String pdfPat = "branch(\\d+)/(\\d+)file\\.pdf";
    String pdfRep = "clockss/downloads/PDF/11111/87/$1$2";
    String pdfWithLinksRep = "clockss/downloads/PDFwithLinks/11111/87/$1$2";
    String pdfSiRep = "clockss/downloads/PDFsi/11111/87/$1$2";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, pdfRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, pdfWithLinksRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, pdfSiRep);
    
    String htmlPat = "branch(\\d+)/(\\d+)file\\.html";
    String htmlRep = "clockss/libraries/fulltext/11111/87/$1$2";
    String hiddenAbsRep = "clockss/libraries/abst/11111/87/$1$2";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, htmlRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, hiddenAbsRep);    
    
    // Remove some URLs:
    // http://www.example.com/clockss/downloads/PDF/11111/87/1002
    // http://www.example.com/clockss/downloads/PDF/11111/87/2002
    // http://www.example.com/clockss/downloads/PDF/11111/87/3002
    // http://www.example.com/clockss/downloads/PDF/11111/87/4002
    // http://www.example.com/clockss/libraries/fulltext/11111/87/1001
    // http://www.example.com/clockss/libraries/fulltext/11111/87/2001
    // http://www.example.com/clockss/libraries/fulltext/11111/87/3001
    // http://www.example.com/clockss/libraries/fulltext/11111/87/4001
    int deletedFileCount = 0; 
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        log.info("au cached url: " + url);
        if ((url.contains("/fulltext/") && url.endsWith("1")) 
            || (url.contains("/PDF/") && url.endsWith("2"))) {
          deleteBlock(cu);
          ++deletedFileCount;
        }
    }
    assertEquals(EXP_DELETED_FILE_COUNT, deletedFileCount);
    
    // au should now match the aspects that the SubTreeArticleIteratorBuilder
    // builds in HeterocyclesArticleIteratorFactory
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    
    int count = 0;
    int countPdfOnly = 0;
    int countPdfwithLinksOnly = 0;
    int countPdfsiOnly = 0;
    int countHtmlOnly = 0;
    int countAbsOnly = 0;
    int countHiddenAbsOnly = 0;

    // after deleting, there are 8 full text pdfs left,
    // including 4 deleted full text htmls
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      String url = cu.getUrl();
      assertNotNull(cu);
      String contentType = cu.getContentType();
      log.info("count " + count + " url " + url + " " + contentType);
      count++;
      url = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF);
      if (!StringUtil.isNullString(url) && url.contains("PDF")) {
        ++countPdfOnly;
      }
      url = af.getRoleUrl(
          HeterocyclesArticleIteratorFactory.ROLE_PDF_WITH_LINKS);
      if (!StringUtil.isNullString(url) && url.contains("PDFwithLinks")) {
        ++countPdfwithLinksOnly;
      }
      url = af.getRoleUrl(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
      if (!StringUtil.isNullString(url) && url.contains("PDFsi")) {
        ++countPdfsiOnly;
      }
      url = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML);
      if (!StringUtil.isNullString(url) && url.contains("fulltext")) {
        ++countHtmlOnly;
        ++countAbsOnly;
      }
      url = af.getRoleUrl(
          HeterocyclesArticleIteratorFactory.ROLE_HIDDEN_ABSTRACT);
      if (!StringUtil.isNullString(url) && url.contains("abst")) {
        ++countHiddenAbsOnly;
      }
    }
    
    log.info("Full text Article count is " + count);
    assertEquals(EXP_FULL_TEXT_COUNT, count);
    assertEquals(EXP_PDF_COUNT, countPdfOnly);
    assertEquals(EXP_PDF_WITH_LINKS_COUNT, countPdfwithLinksOnly);
    assertEquals(EXP_PDF_SI_COUNT, countPdfsiOnly);
    assertEquals(EXP_HTML_COUNT, countHtmlOnly);
    assertEquals(EXP_ABS_COUNT, countAbsOnly);
    assertEquals(EXP_HIDDEN_ABS_COUNT, countHiddenAbsOnly);
   }
 
  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    cu.delete();
  }

}
