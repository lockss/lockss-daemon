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

package org.lockss.plugin.peerj;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
import org.lockss.state.NodeManager;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.daemon.SingleNodeCachedUrlSetSpec;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;

/*
 * Test with PeerJ Archives site.
 *   article files:
 *   full text pdf      - <base_url>/articles/55.pdf
 *   abstract           - <base_url>/articles/55/    (part of full text)
 *   full text xml      - <base_url>/articles/55.xml
 *   citation bib       - <base_url>/articles/55.bib
 *   citation ris       - <base_url>/articles/55.ris
 *   alternate html     - <base_url>/articles/55.html
 *   alternate rdf      - <base_url>/articles/55.rdf
 *   alternate json     - <base_url>/articles/55.json
 *   alternate unixref  - <base_url>/articles/55.unixref
 *   
 */
public class TestPeerJArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
        
  private static final String PLUGIN_NAME =
      "org.lockss.plugin.peerj.PeerJPlugin";
   
  private static final String BASE_URL = "http://www.example.com/";
  private static final String PEERJ_SITE = "archives";
  private static final String VOLUME_NAME = "2013";
  
  private static final int DEFAULT_FILESIZE = 3000;
  
  private static final int EXP_DELETED_FILE_COUNT = 8;
  private static final int EXP_FULL_TEXT_COUNT = 12;
  private static final int EXP_PDF_COUNT = 8; // after deleteBlock
  private static final int EXP_XML_COUNT = 8; // after deleteBlock
  private static final int EXP_ABS_COUNT = 12;
  private static final int EXP_BIB_COUNT = 12;
  private static final int EXP_RIS_COUNT = 12;
  private static final int EXP_ARTICLE_METADATA_COUNT = 12;
  private static final int EXP_ALTERNATE_FILE_COUNT = 48;
 
  public void setUp() throws Exception {
    super.setUp();
    
    // au is protected archival unit from super class ArticleIteratorTestCase
    au = createAu(); 
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,  peerjAuConfig());
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
            | SimulatedContentGenerator.FILE_TYPE_XML));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  private Configuration peerjAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("peerj_site", PEERJ_SITE);
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "articles/"), getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // PATTERN_TEMPLATE = 
    //  "\"^%s(articles)/([0-9]+)(\\.pdf)?$\", base_url";
    
    assertNotMatchesRE(pat, "http://www.example.com/articles/55.pdfbad");
    assertMatchesRE(pat, "http://www.example.com/articles/55");
    assertMatchesRE(pat, "http://www.example.com/articles/55.pdf");
  }
  
  // simulated cached urls:
  // total number files = 24; // 1 depth, 4 branches, 3 files
  // 1003 means branch #1 and file  #3
  // there are 9 different file formats
  // 
  // full text pdf      - http://www.example.com/articles/1003.pdf
  // abstract           - http://www.example.com/articles/1003
  // full text xml      - http://www.example.com/articles/1003.xml
  // citation bib       - http://www.example.com/articles/1003.bib
  // citation ris       - http://www.example.com/articles/1003.ris
  // alternate html     - http://www.example.com/articles/1003.html
  // alternate rdf      - http://www.example.com/articles/1003.rdf
  // alternate json     - http://www.example.com/articles/1003.json
  // alternate unixref  - http://www.example.com/articles/1003.unixref
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    String pdfPat = "branch(\\d+)/(\\d+)file\\.pdf";
    String pdfRep = "/articles/$1$2.pdf";
    String absRep = "/articles/$1$2";
    String xmlRep = "/articles/$1$2.xml";
    String bibRep = "/articles/$1$2.bib";
    String risRep = "/articles/$1$2.ris";
    String alternateHtmlRep = "/articles/$1$2.html";
    String alternateJsonRep = "/articles/$1$2.json";
    String alternateRdfRep = "/articles/$1$2.rdf";
    String alternateUnixrefRep = "/articles/$1$2.unixref";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, pdfRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, absRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, xmlRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, bibRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, risRep);    
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, alternateHtmlRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, alternateJsonRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, alternateUnixrefRep);
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, alternateRdfRep);
  
    // Remove some URLs
    int deletedFileCount = 0; 
    for (Iterator it = 
        au.getAuCachedUrlSet().contentHashIterator() ; it.hasNext() ; ) {
      CachedUrlSetNode cusn = (CachedUrlSetNode)it.next();
      if (cusn instanceof CachedUrl) {
        CachedUrl cu = (CachedUrl)cusn;
        String url = cu.getUrl();
        log.info("au cached url: " + url);
        if (url.contains("/articles/") 
            && (url.endsWith("1.xml") || url.endsWith("2.pdf"))) {
          deleteBlock(cu);
          ++deletedFileCount;
        }
      }
    }
    assertEquals(EXP_DELETED_FILE_COUNT, deletedFileCount);
    
    // au should now match the aspects that the SubTreeArticleIteratorBuilder
    // builds in PeerJArticleIteratorFactory
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    
    // each branch has 12 alternate files (3 rdf, 3 json, 3 unixref, 3 html)
    int count = 0;
    int countPdfOnly = 0;
    int countXmlOnly = 0;
    int countAbsOnly = 0;
    int countBibOnly = 0;
    int countRisOnly = 0;
    int countArticleMetadataOnly = 0;
    int countAlternateFileOnly = 0;
    
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
      if (!StringUtil.isNullString(url) && url.endsWith(".pdf")) {
        ++countPdfOnly;
      }
      url = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_XML);
      if (!StringUtil.isNullString(url) && url.endsWith(".xml")) {
        ++countXmlOnly;
      }
      url = af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT);
      log.info("abs url: " + url);
      if (!StringUtil.isNullString(url) && url.matches(".*/\\d+$")) {
        log.info("abs url2: " + url);
        ++countAbsOnly;
      }     
      if (!StringUtil.isNullString(
          af.getRoleUrl(ArticleFiles.ROLE_CITATION_BIBTEX))) { 
        ++countBibOnly;
      }
      if (!StringUtil.isNullString(
          af.getRoleUrl(ArticleFiles.ROLE_CITATION_RIS))) { 
        ++countRisOnly;
      }
      if (!StringUtil.isNullString(
          af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA))) { 
        ++countArticleMetadataOnly;
      }
      // count all alternate files together
      if (!StringUtil.isNullString(af.getRoleUrl(
          PeerJArticleIteratorFactory.ROLE_ALTERNATE_FULL_TEXT_HTML))) {
        ++countAlternateFileOnly;
      }
      if (!StringUtil.isNullString(af.getRoleUrl(
          PeerJArticleIteratorFactory.ROLE_ALTERNATE_RDF))) {
        ++countAlternateFileOnly;
      }
      if (!StringUtil.isNullString(af.getRoleUrl(
          PeerJArticleIteratorFactory.ROLE_ALTERNATE_JSON))) {
        ++countAlternateFileOnly;
      }
      if (!StringUtil.isNullString(af.getRoleUrl(
          PeerJArticleIteratorFactory.ROLE_ALTERNATE_UNIXREF))) {
        ++countAlternateFileOnly;
      }
    }
    
    log.info("Full text Article count is " + count);
    assertEquals(EXP_FULL_TEXT_COUNT, count);
    assertEquals(EXP_PDF_COUNT, countPdfOnly);
    assertEquals(EXP_XML_COUNT, countXmlOnly);
    assertEquals(EXP_ABS_COUNT, countAbsOnly);
    assertEquals(EXP_BIB_COUNT, countBibOnly);
    assertEquals(EXP_RIS_COUNT, countRisOnly);
    assertEquals(EXP_ARTICLE_METADATA_COUNT, countArticleMetadataOnly);
    assertEquals(EXP_ALTERNATE_FILE_COUNT, countAlternateFileOnly);    
   }
 
  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    CachedUrlSetSpec cuss = new SingleNodeCachedUrlSetSpec(cu.getUrl());
    ArchivalUnit au = cu.getArchivalUnit();
    CachedUrlSet cus = au.makeCachedUrlSet(cuss);
    NodeManager nm = au.getPlugin().getDaemon().getNodeManager(au);
    nm.deleteNode(cus);
  }

}