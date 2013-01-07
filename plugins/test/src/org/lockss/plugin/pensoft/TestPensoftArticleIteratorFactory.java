/*
 * $Id: 
 */

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

package org.lockss.plugin.pensoft;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.TdbAu;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.SingleNodeCachedUrlSetSpec;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
//import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.ArticleMetadataExtractor.Emitter;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.NodeManager;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestPensoftArticleIteratorFactory extends ArticleIteratorTestCase {
        
  private SimulatedArchivalUnit sau;      // Simulated AU to generate content
        
  private final String PLUGIN_NAME = "org.lockss.plugin.pensoft.PensoftPlugin";
  private static final ConfigParamDescr J_NAME =
    new ConfigParamDescr()
    .setKey("journal_name")
    .setDisplayName("Journal Name")
    .setType(ConfigParamDescr.TYPE_STRING)
    .setSize(20);
  private static final ConfigParamDescr ISSUE_SET =
    new ConfigParamDescr()
    .setKey("issue_set")
    .setDisplayName("Issue Set")
    .setType(ConfigParamDescr.TYPE_SET)
    .setSize(10);
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String JOURNAL_NAME_KEY = J_NAME.getKey();
  static final String ISSUE_SET_KEY = ISSUE_SET.getKey();
  private final String BASE_URL = "http://www.pensoft-ex-base.net/";
  private final String JOURNAL_NAME = "abc";
  private final String YEAR = "2011";
  private final String ISSUE_SET_STR = "5-7";
  private final Configuration AU_CONFIG = 
          ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
                                      JOURNAL_NAME_KEY, JOURNAL_NAME,
                                      ISSUE_SET_KEY, ISSUE_SET_STR);
  private static final int DEFAULT_FILESIZE = 3000;

  protected String cuRole = null;
  ArticleMetadataExtractor.Emitter emitter;
  protected boolean emitDefaultIfNone = false;
  FileMetadataExtractor me = null; 
  MetadataTarget target;

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
      PluginTestUtil.createAndStartAu(PLUGIN_NAME,  AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "5");
    conf.put("fileTypes",
             "" + (SimulatedContentGenerator.FILE_TYPE_HTML |
             SimulatedContentGenerator.FILE_TYPE_PDF | 
             SimulatedContentGenerator.FILE_TYPE_TXT));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
    }
  

  public void testRoots() throws Exception {      
    SubTreeArticleIterator artIter = createSubTreeIter();
    
    assertEquals(ListUtil.list(BASE_URL),
                 getRootUrls(artIter));
  }
  
  
  //http://www.example.net/journals/abc/article/1142/abstract/a-really-long-title-longer-than-this
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "http://www.pensoft-ex-base.net/7/1/2012/cp-7-1-2012.html"); //wrong volume
    assertMatchesRE(pat, "http://www.pensoft.net/journals/abc/article/606/abstract/a-very-descriptive-title");
  }
  
  
   
  public void testCreateArticleFiles() throws Exception {
    int expCount = 15; // 1 depth, 4 branches, 5 files, but removing some later in test   
    PluginTestUtil.crawlSimAu(sau);
    
 /*
  *  Go through the simulated content you just crawled and modify the results to emulate
  *  what you would find in a "real" crawl with this plugin:
  *  <base_url>/48/<page#>/2011/art#file.{html, pdf & ris}   
    http://www.pensoft.net/J_FILES/2/articles/617/52-G-1-layout.pdf
    http://www.pensoft.net/journals/biorisk/article/623/mites-and-ticks-acari-chapter-74   
    http://www.pensoft.net/inc/journals/download.php?fileId=1481&fileTable=J_GALLEYS
    http://www.pensoft.net/journals/biorisk/article/608/abstract/introduction-chapter-1
    http://www.pensoft.net/inc/journals/ris.php?articleId=632&type=ris&journalId=2
    http://www.pensoft.net/inc/journals/ris.php?articleId=632&type=txt&journalId=2
  */
    String pat1 = "journals/[\\w]+/article/[\\d]+/[\\w-]+";
    String rep1 = "journals/biorisk/article/623/mites-and-ticks-acari-chapter-$1";
    PluginTestUtil.copyAu(sau, au, ".*$", pat1, rep1);
    String pat2 = "J_FILES/[\\d]+/articles/[\\d]+/[^/]+";
    String rep2 = "J_FILES/2/articles/$1/52-G-1-layout.pdf";
    PluginTestUtil.copyAu(sau, au, "/\\d+/.*\\.pdf$", pat2, rep2);
    String pat3 = "inc/journals/ris.php\\?articleId=[\\d]+&type=(ris|txt)&journalId=[\\d]+";
    String rep3 = "inc/journals/ris.php?articleId=632&type=txt&journalId=$1";
    PluginTestUtil.copyAu(sau, au, ".*\\.txt$", pat3, rep3);
    
    // Remove some of the URLs just created to make test more robust
    // Remove files 1file.html and 2file.pdf in each branch (and there are 3 branches)
/*
    int deleted = 0;
    int pdf_deleted = 0;
    for (Iterator it = au.getAuCachedUrlSet().contentHashIterator() ; it.hasNext() ; ) {
      CachedUrlSetNode cusn = (CachedUrlSetNode)it.next();
      if (cusn instanceof CachedUrl) {
        CachedUrl cu = (CachedUrl)cusn;
        String url = cu.getUrl();
        if (url.contains("/2011/")) {
          if (url.endsWith("1file.html") || url.endsWith("2file.ris")) {
            deleteBlock(cu);
            ++deleted;
          } else if (url.endsWith("3file.pdf") && (pdf_deleted == 0)) {
            // delete ONE pdf file to see what happens with no FULL_TEXT_CU
            // it probably won't happen but the publisher might have an error
            pdf_deleted = 1;
            deleteBlock(cu);
          }
        }
      }
    }
 

    assertEquals(8, deleted); // 3 branches, 2 files removed per branch - don't count PDF file

    Iterator<ArticleFiles> it = au.getArticleIterator();
    int count = 0;
    int countNoMeta = 0;
    int countNoPDF = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
         String url = cu.getUrl();
         String contentType = cu.getContentType();
         log.debug("count " + count + " url " + url + " " + contentType);
         count++;
      } else {
        // only a PDF file is full text
        ++countNoPDF;
      }
      if ( af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA) == null) {
        ++countNoMeta; 
      }
    }
    // missing HTML file will mean no article is picked up at all
    // missing Ris file will be fine
    // there should always be a pdf whenever an article is found
    log.debug("Article count is " + count);
    assertEquals(expCount, count);
    assertEquals(4, countNoMeta);
    assertEquals(1, countNoPDF);
       */
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
