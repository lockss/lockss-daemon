/* 
 * $Id$
 */
/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bepress;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.config.*;
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
 * Article content may look like:
 * abstract      - <dcrbase>/statistics/122/
 * full text pdf - <dcrbase>/cgi/viewcontent.cgi?article=1108
 *                                                        &context=statistics
 */
public class TestDigitalCommonsRepositoryArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
        
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.bepress.DigitalCommonsRepositoryPlugin";
   
  private static final String BASE_URL = "http://www.example.com/";
  private static final String COLLECTION_TYPE = "xxxtype";  
  private static final String COLLECTION = "xxxdept";  
  private static final String COLLECTION_HEADING = "xxxheading";
  private static final int DEFAULT_FILESIZE = 3000;
  private static final int EXP_DELETED_FILE_COUNT = 2;
  private static final int EXP_FULL_TEXT_COUNT = 3; // full text pdf
  private static final int EXP_PDF_COUNT = 3; // after deleteBlock

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
  // this publisher provides inconsistent url structure, impossible to guess
  // other file type from the pdfs
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "5");
    conf.put("fileTypes", "" + (  SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  private Configuration dcrAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("collection_type", COLLECTION_TYPE);
    conf.put("collection", COLLECTION);
    conf.put("collection_heading", COLLECTION_HEADING);
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
    //  "\"^%scgi/viewcontent\\.cgi[^/]+context=%s$\", base_url, collection";
    assertNotMatchesRE(pat, BASE_URL 
        + "cgibad/viewcontent.cgi?article=1108&context=" + COLLECTION);
    assertMatchesRE(pat, BASE_URL
        + "cgi/viewcontent.cgi?article=1108&context=" + COLLECTION);
  }
  
  // simulated cached urls:
  // total number files = 5; // 1 depth, 1 branch, 5 files
  // sim au cached urls:
  // http://www.example.com/cgi/viewcontent.cgi?article=001&&context=xxxdept
  // http://www.example.com/cgi/viewcontent.cgi?article=002&&context=xxxdept
  // http://www.example.com/cgi/viewcontent.cgi?article=003&&context=xxxdept
  // http://www.example.com/cgi/viewcontent.cgi?article=004&&context=xxxdept
  // http://www.example.com/cgi/viewcontent.cgi?article=005&&context=xxxdept
  // delete:
  // http://www.example.com/cgi/viewcontent.cgi?article=002&&context=xxxdept
  // http://www.example.com/cgi/viewcontent.cgi?article=002&&context=xxxdept
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pdfPat = "branch(\\d+)/(\\d+)file\\.pdf";
    // $2 is article number, and collection is xxxdept
    String pdfRep = "cgi/viewcontent.cgi?article=$2&&context=xxxdept";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pdfPat, pdfRep);   
    // Remove some urls: pdf urls containing string 
    // "article=002" and "article=004"
    // deletedFileCount should be 2
    int deletedFileCount = 0; 
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        log.info("au cached url: " + url);
        if (url.contains("article=002") || url.contains("article=004")) {
          deleteBlock(cu);
          ++deletedFileCount;
        }
    }
    assertEquals(EXP_DELETED_FILE_COUNT, deletedFileCount);
    
    // au should now match the aspects that the SubTreeArticleIteratorBuilder
    // builds in DigitalCommonsRepositoryArticleIteratorFactory
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int countFullText = 0;
    int countPdf = 0;
    // after deleting, there should be 3 full text pdfs, and 3 pdfs
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
    log.debug3("Full text Article count is " + countFullText);
    assertEquals(EXP_FULL_TEXT_COUNT, countFullText);
    assertEquals(EXP_PDF_COUNT, countPdf);
   }
 
  private void deleteBlock(CachedUrl cu) throws IOException {
    log.debug3("deleting " + cu.getUrl());
    cu.delete();
  }

}
