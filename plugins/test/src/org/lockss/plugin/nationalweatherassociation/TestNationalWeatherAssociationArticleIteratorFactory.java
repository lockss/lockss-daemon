/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.nationalweatherassociation;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;


/*
 * article content may look like:
 *      <nwasbase>.org/xjid/abstracts/2013/2013-JID22/abstract.php
 *      <nwasbase>.org/xjid/articles/2013/2013-JID22/2013-JID22.pdf
 */
public class TestNationalWeatherAssociationArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
        
  private static final String PLUGIN_NAME = "org.lockss.plugin."
      + "nationalweatherassociation.NationalWeatherAssociationPlugin";
   
  private static final String BASE_URL = "http://www.example.com/";
  private static final String JID = "xjid";
  private static final String YEAR = "2013";
  private static final int DEFAULT_FILESIZE = 3000;
  
  private static final int EXP_DELETED_FILE_COUNT = 5;
  private static final int EXP_FULL_TEXT_COUNT = 11; // after deleteBlock
  private static final int EXP_PDF_COUNT = 11; // after deleteBlock
  private static final int EXP_ABS_COUNT = 7; // after deleteBlock 
  private static final int EXP_AM_COUNT = 7; // after deleteBlock 

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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, nwaAuConfig());
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

  private Configuration nwaAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JID);
    conf.put("year", YEAR);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + JID), getRootUrls(artIter));
  }
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // Pattern PDF_PATTERN = Pattern.compile(
    //  "/(articles)/(([^/]+/)+)([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
   
    // <nwabase>.org/xjid/articles/2013/2013-XJID12/2013-XJID12.pdf
    assertMatchesRE(pat, BASE_URL + JID + "/articles/" + YEAR + "/"
        + YEAR + "-" + JID + "12/"+ YEAR + "-" + JID + "12.pdf");
    
    // <nwabase>.org/xjid/articles/2013/2013-XJID12/2013-XJID12.pdfbad
    assertNotMatchesRE(pat, BASE_URL + JID + "/articles/" + YEAR + "/"
        + YEAR + "-" + JID + "12/"+ YEAR + "-" + JID + "12.pdfbad");
  }
  
  // simulated cached urls:
  // total number files = 24; // 1 depth, 4 branches, 3 files
  // 1003 means branch #1 and file  #3
  // there are 2 different file formats: .pdf and .php
  // .php is actually an html file, so 12 .pdfs and 12 .phps
  // 
  // full text pdf: 
  //  http://www.example.com/xjid/articles/2013/2013-xjid1001/2013-xjid1001.pdf
  // abstract: 
  //  http://www.example.com/xjid/abstracts/2013/2013-xjid1001/abstract.php
  //
  // http://www.example.com/xjid/abstracts/2013/2013-xjid1001/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid1002/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid1003/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid2001/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid2002/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid2003/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid3001/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid3002/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid3003/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid4001/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid4002/abstract.php
  // http://www.example.com/xjid/abstracts/2013/2013-xjid4003/abstract.php
  // http://www.example.com/xjid/articles/2013/2013-xjid1001/2013-xjid1001.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid1002/2013-xjid1002.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid1003/2013-xjid1003.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid2001/2013-xjid2001.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid2002/2013-xjid2002.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid2003/2013-xjid2003.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid3001/2013-xjid3001.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid3002/2013-xjid3002.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid3003/2013-xjid3003.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid4001/2013-xjid4001.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid4002/2013-xjid4002.pdf
  // http://www.example.com/xjid/articles/2013/2013-xjid4003/2013-xjid4003.pdf  
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    String pdfPat = "branch(\\d+)/(\\d+)file\\.pdf";
    // <nwabase>.org/xjid/articles/2013/2013-XJID12/2013-XJID12.pdf
    String pdfRep = JID + "/articles/2013/2013-xjid$1$2/2013-xjid$1$2.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, pdfRep);

    String htmlPat = "branch(\\d+)/(\\d+)file\\.html";
    // <nwabase>.org/xjid/abstracts/2013/2013-XJID22/abstract.php
    String absRep = JID +"/abstracts/2013/2013-xjid$1$2/abstract.php";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", htmlPat, absRep);
        
    // Remove some URLs:
    // http://www.example.com/xjid/abstracts/2013/2013-xjid1002/abstract.php
    // http://www.example.com/xjid/abstracts/2013/2013-xjid2002/abstract.php
    // http://www.example.com/xjid/abstracts/2013/2013-xjid3002/abstract.php
    // http://www.example.com/xjid/abstracts/2013/2013-xjid4002/abstract.php
    // http://www.example.com/xjid/articles/2013/2013-xjid1001/2013-xjid1001.pdf    
    int deletedFileCount = 0; 
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        log.info("au cached url: " + url);
        if ((url.contains("/articles/") && url.endsWith("1001.pdf")) 
            || (url.contains("/abstracts/") && url.contains("002"))) {
          deleteBlock(cu);
          ++deletedFileCount;
        }
    }
    assertEquals(EXP_DELETED_FILE_COUNT, deletedFileCount);
    
    // au should now match the aspects that the SubTreeArticleIteratorBuilder
    // builds in NWAArticleIteratorFactory
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    
    int countFullText = 0;
    int countPdf = 0;
    int countAbs = 0;
    int countAm = 0;
    
    // after deleting, there are 8 full text pdfs left.
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      String url = cu.getUrl();
      assertNotNull(cu);
      String contentType = cu.getContentType();
      log.info("count full text " + countFullText + " url " + url 
          + " " + contentType);
      countFullText++;
      url = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF);
      if (!StringUtil.isNullString(url) && url.contains("pdf")) {
        ++countPdf;
      }
      url = af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT);
      if (!StringUtil.isNullString(url) && url.contains("abstract")) {
        ++countAbs;
      }
      url = af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (!StringUtil.isNullString(url) && url.contains("abstract")) {
        ++countAm;
      }

    }
    
    log.info("Full text article count is " + countFullText);
    log.info("Pdf count is " + countPdf);
    log.info("Abstract count is " + countAbs);
    log.info("Article Metadata count is " + countAm);
    assertEquals(EXP_FULL_TEXT_COUNT, countFullText);
    assertEquals(EXP_PDF_COUNT, countPdf);
    assertEquals(EXP_ABS_COUNT, countAbs);
    assertEquals(EXP_AM_COUNT, countAm);
  }
 
  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    cu.delete();
  }

}
