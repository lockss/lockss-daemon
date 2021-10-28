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

package org.lockss.plugin.illiesia;

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
 * Each article consists of a full text pdf and nothing else. All abstracts
 * are listed in one page. This simply test the pdf article count.
 *   Issue toc containing abstracts - <illiesiabase>/html/2013.html
 *   pdf - <illiesiabase>/papers/Illiesia09-01.pdf
 */
public class TestIlliesiaArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
        
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.illiesia.IlliesiaPlugin";
   
  private static final String BASE_URL = "http://www.example.com/";
  private static final String YEAR = "2013";
  private static final int DEFAULT_FILESIZE = 3000;
  private static final int EXP_DELETED_FILE_COUNT = 6;
  private static final int EXP_FULL_TEXT_COUNT = 9; // full text pdf
  private static final int EXP_PDF_COUNT = 9; // after deleteBlock

  public void setUp() throws Exception {
    super.setUp();
    au = createAu(); 
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));

    ConfigurationUtil.addFromArgs(CachedUrl.PARAM_ALLOW_DELETE, "true");
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() 
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, dcrAuConfig());
  }
  
  // only use one file type pdf here since af contains only pdfs
  // since this the only article aspect we can collect
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "3");
    conf.put("numFiles", "5");
    conf.put("fileTypes", "" + (  SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  private Configuration dcrAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
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
    //          "\"^%spapers/Illiesia[0-9]+-[0-9]+\\.pdf$\", base_url";
    assertNotMatchesRE(pat, BASE_URL + "papers/Illiesia09-01.pdfbad");
    assertMatchesRE(pat, BASE_URL + "papers/Illiesia09-01.pdf");
  }
  
  // simulated content:
  // total number files = 15; // 1 depth, 3 branch, 5 files
  // after deleting pdfs ending with 002.pdf and 004 pdf, the total number
  // of pdfs should be 9. Full text Article count is also 9.
  //
  // au cached urls from simulated crawled content:
  // http://www.example.com/papers/Illiesia1-001.pdf
  // http://www.example.com/papers/Illiesia1-002.pdf
  // http://www.example.com/papers/Illiesia1-003.pdf
  // http://www.example.com/papers/Illiesia1-004.pdf
  // http://www.example.com/papers/Illiesia1-005.pdf
  // http://www.example.com/papers/Illiesia2-001.pdf
  // http://www.example.com/papers/Illiesia2-002.pdf
  // http://www.example.com/papers/Illiesia2-003.pdf
  // http://www.example.com/papers/Illiesia2-004.pdf
  // http://www.example.com/papers/Illiesia2-005.pdf
  // http://www.example.com/papers/Illiesia3-001.pdf
  // http://www.example.com/papers/Illiesia3-002.pdf
  // http://www.example.com/papers/Illiesia3-003.pdf
  // http://www.example.com/papers/Illiesia3-004.pdf
  //
  // deleting pdfs ending with 002.pdf and 004.pdf:
  // http://www.example.com/papers/Illiesia1-002.pdf
  // http://www.example.com/papers/Illiesia1-004.pdf
  // http://www.example.com/papers/Illiesia2-002.pdf
  // http://www.example.com/papers/Illiesia2-004.pdf
  // http://www.example.com/papers/Illiesia3-002.pdf
  // http://www.example.com/papers/Illiesia3-004.pdf
  // 
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pdfPat = "branch(\\d+)/(\\d+)file\\.pdf";
    // $1 is volume name, $2 is article number
    String pdfRep = "papers/Illiesia$1-$2.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, pdfRep);   
    // Remove some urls: pdf urls ending with 002.pdf and 004.pdf
    // deletedFileCount should be 6
    int deletedFileCount = 0; 
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        log.info("au cached url: " + url);
        if (url.endsWith("002.pdf") || url.endsWith("004.pdf")) {
          deleteBlock(cu);
          ++deletedFileCount;
        }
    }
    assertEquals(EXP_DELETED_FILE_COUNT, deletedFileCount);
    
    // au should now match the aspects that the SubTreeArticleIteratorBuilder
    // builds in IlliesiaArticleIteratorFactory
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int countFullText = 0;
    int countPdf = 0;
    // after deleting, there should be 9 full text pdfs, and 9 pdfs 
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
    log.debug3("Full text Article count is " + countFullText);
    assertEquals(EXP_FULL_TEXT_COUNT, countFullText);
    assertEquals(EXP_PDF_COUNT, countPdf);
   }
 
  private void deleteBlock(CachedUrl cu) throws IOException {
    log.debug3("deleting " + cu.getUrl());
    cu.delete();
  }

}
