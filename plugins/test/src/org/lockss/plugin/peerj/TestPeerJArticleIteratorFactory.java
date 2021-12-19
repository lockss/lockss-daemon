/* $Id$

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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import junit.framework.Test;
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
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;

public class TestPeerJArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  SimulatedArchivalUnit sau;
  String variantPeerjSite;
  String variantBaseConstant;
  String variantPluginName;
  String variantArticleName;
  String variantPeerjXmlRole;
  List<String> variantAlternateRoles;
    
  private static final String BASE_URL = "http://www.example.com/";
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
        PluginTestUtil.createAndStartAu(variantPluginName,  peerjAuConfig());
  }
    
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", Integer.toString(1));
    conf.put("branch", Integer.toString(4));
    conf.put("numFiles", Integer.toString(3));
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_PDF        
            | SimulatedContentGenerator.FILE_TYPE_XML));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  private Configuration peerjAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + variantBaseConstant + "/"), 
                 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // PATTERN_TEMPLATE = 
    //  "\"^%s(articles)/([0-9]+)(\\.pdf)?$\", base_url";
    
    assertNotMatchesRE(pat, BASE_URL + variantBaseConstant + "/" 
                            + variantArticleName + ".pdfbad");
    assertMatchesRE(pat, BASE_URL + variantBaseConstant + "/" 
                            + variantArticleName + "");
    assertMatchesRE(pat, BASE_URL + variantBaseConstant + "/" 
                            + variantArticleName + ".pdf");
  }
  
  /*
   * simulated cached urls:
   * total number files = 24;  // 1 depth, 4 branches, 3 files
   * 1003 means branch #1 and file  #3
   * there are 9 different file formats
   * base_constant is either 'articles' or 'preprints'
   * 
   * full text pdf      - <base_url>/<base_constant>/1003.pdf
   * abstract           - <base_url>/<base_constant>/1003
   * full text xml      - <base_url>/<base_constant>/1003.xml
   * citation bib       - <base_url>/<base_constant>/1003.bib
   * citation ris       - <base_url>/<base_constant>/1003.ris
   * altertnate html    - <base_url>/<base_constant>/1003.html
   * alternate rdf      - <base_url>/<base_constant>/1003.rdf
   * alternate json     - <base_url>/<base_constant>/1003.json
   * alternate unixref  - <base_url>/<base_constant>/1003.unixref
   */
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    // 48 for Archives (main) site
    // 36 for Preprints since it does not have unixref files
    int expAlternateFileCount = 12 * variantAlternateRoles.size();
    
    String pdfPat = "branch(\\d+)/(\\d+)file\\.pdf";
    String pdfRep = variantBaseConstant + "/$1$2.pdf";
    String absRep = variantBaseConstant + "/$1$2";
    String xmlRep = variantBaseConstant + "/$1$2.xml";
    String bibRep = variantBaseConstant + "/$1$2.bib";
    String risRep = variantBaseConstant + "/$1$2.ris";
    String alternateHtmlRep = variantBaseConstant + "/$1$2.html";
    String alternateJsonRep = variantBaseConstant + "/$1$2.json";
    String alternateRdfRep = variantBaseConstant + "/$1$2.rdf";
    String alternateUnixrefRep = variantBaseConstant + "/$1$2.unixref";
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
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        log.info("au cached url: " + url);
        if (url.contains(variantBaseConstant) 
            && (url.endsWith("1.xml") || url.endsWith("2.pdf"))) {
          deleteBlock(cu);
          ++deletedFileCount;
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
      url = af.getRoleUrl(variantPeerjXmlRole);
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
      for (int i = 0; i < variantAlternateRoles.size(); i++) {
        if (!StringUtil.isNullString(
            af.getRoleUrl(variantAlternateRoles.get(i).toString()))) {
          ++countAlternateFileOnly;
        }        
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
    assertEquals(expAlternateFileCount, countAlternateFileOnly);    
   }
 
  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    cu.delete();
  }
  
  /*
   * PeerJ Archives (main) site.
   *   article files:
   *   full text pdf      - <baser_url>/articles/55.pdf
   *   abstract           - <baser_url>/articles/55/     (including full text)
   *   full text xml      - <baser_url>/articles/55.xml
   *   citation bib       - <baser_url>/articles/55.bib
   *   citation ris       - <baser_url>/articles/55.ris
   *   alternate html     - <baser_url>/articles/55.html
   *   alternate rdf      - <baser_url>/articles/55.rdf
   *   alternate json     - <baser_url>/articles/55.json
   *   alternate unixref  - <baser_url>/articles/55.unixref
   */
 public static class TestArchives extends TestPeerJArticleIteratorFactory {
    
    public TestArchives() {
      variantPluginName = "org.lockss.plugin.peerj.PeerJPlugin";
      variantPeerjSite = "archives";
      variantBaseConstant = "articles";
      variantArticleName = "55";
      variantPeerjXmlRole = ArticleFiles.ROLE_FULL_TEXT_XML;
      variantAlternateRoles = Arrays.asList(
          PeerJArticleIteratorFactory.ROLE_ALTERNATE_FULL_TEXT_HTML,
          PeerJArticleIteratorFactory.ROLE_ALTERNATE_RDF,
          PeerJArticleIteratorFactory.ROLE_ALTERNATE_JSON,
          PeerJArticleIteratorFactory.ROLE_ALTERNATE_UNIXREF);
    }
  }



  public static Test suite() {
    return variantSuites(new Class[] {
        TestArchives.class,
        // Variant to test PeerJ Preprints site no longer exists
      });
  }
}
