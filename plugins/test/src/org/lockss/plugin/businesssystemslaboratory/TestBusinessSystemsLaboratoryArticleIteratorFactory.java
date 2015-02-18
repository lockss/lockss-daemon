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

package org.lockss.plugin.businesssystemslaboratory;

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
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;

/*
 * This publisher provides inconsistent url structures. Not possible for 
 * the article iterator builder to guess an abstract from the full text pdf
 * url. This simply test the pdf article count.
 * 
 * article content may look like:
 * Volume 1 (2012):
 *   <bslbase>.org/Aiello.et.al.(2012).Complex.Products.1.1.htm
 *   <bslbase>.org/BSR.Vol.1-Iss.1-Aiello.Esposito.Ferri.Complex.Products.pdf
 * Volume 2 (2013):
 *   <bslbase>.org/Bardy.&.Massaro.(2013).Sustainability.Value.Index.2.1.htm
 *   <bslbase>.org/BSR.Vol.2-Iss.1-Massaro.et.al.Organising.Innovation.pdf
 */
public class TestBusinessSystemsLaboratoryArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
        
  private static final String PLUGIN_NAME = "org.lockss.plugin."
      + "businesssystemslaboratory.BusinessSystemsLaboratoryPlugin";
   
  private static final String BASE_URL = "http://www.example.com/";
  private static final String VOLUME_NAME = "2";
  private static final String YEAR = "2013";
  
  private static final int DEFAULT_FILESIZE = 3000;
  
  private static final int EXP_FULL_TEXT_COUNT = 3; // full text pdf
  private static final int EXP_PDF_COUNT = 3; // after deleteBlock

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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,  bslAuConfig());
  }
  
  // only use one file type pdf here since af contains only pdfs
  // this publisher provides inconsistent url structure, impossible to guess
  // other file type from the pdfs
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "2");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (  SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  private Configuration bslAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", VOLUME_NAME);
    conf.put("year", YEAR);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL), getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // PATTERN_TEMPLATE =
    //  "\"^%sBSR[.-]Vol[.-]%s[.-](?![^/]+[.-]Complete([.-]Issue)?\\.pdf)
    //                                 [^/]+\\.pdf$\", base_url, volume_name";

    assertNotMatchesRE(pat, BASE_URL + "BSR.Vol." + VOLUME_NAME
                       + "-Iss.1-Shaari.et.al..Energy.Economic.Growth.pdfbad");
    assertMatchesRE(pat, BASE_URL + "BSR.Vol." + VOLUME_NAME
                       + "-Iss.1-Shaari.et.al..Energy.Economic.Growth.pdf");
  }
  
  // simulated cached urls:
  // total number files = 6; // 1 depth, 2 branches, 3 files
  // sim au cached urls:
  // http://www.example.com/BSR.Vol.1.author.title.001file.pdf
  // http://www.example.com/BSR.Vol.1.author.title.002file.pdf
  // http://www.example.com/BSR.Vol.1.author.title.003file.pdf
  // http://www.example.com/BSR.Vol.2.author.title.001file.pdf
  // http://www.example.com/BSR.Vol.2.author.title.002file.pdf
  // http://www.example.com/BSR.Vol.2.author.title.003file.pdf
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    String pdfPat = "branch(\\d+)/(\\d+)file\\.pdf";
    String pdfRep = "/BSR\\.Vol\\.$1\\.author\\.title\\.$2file.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, pdfRep);   
    
    // au should now match the aspects that the SubTreeArticleIteratorBuilder
    // builds in BusinessSystemsLaboratoryArticleIteratorFactory
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    
    int countFullText = 0;
    int countPdf = 0;
    
    // after deleting, there are 2 full text pdfs, and 2 pdfs
    // left for volume 2
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      String url = cu.getUrl();
      assertNotNull(cu);
      String contentType = cu.getContentType();
      log.info("count full text " + countFullText + " url " 
               + url + " " + contentType);
      countFullText++;
      url = af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF);
      if (!StringUtil.isNullString(url)) {
        ++countPdf;
      }
    }
    
    log.info("Full text Article count is " + countFullText);
    assertEquals(EXP_FULL_TEXT_COUNT, countFullText);
    assertEquals(EXP_PDF_COUNT, countPdf);
   }

}