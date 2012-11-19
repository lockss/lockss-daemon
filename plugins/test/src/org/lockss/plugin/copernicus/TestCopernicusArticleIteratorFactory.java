/*
 * $Id: TestCopernicusArticleIteratorFactory.java,v 1.2 2012-11-19 21:03:19 alexandraohlson Exp $
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

package org.lockss.plugin.copernicus;

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
import org.lockss.extractor.BaseArticleMetadataExtractor;
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

public class TestCopernicusArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
	private final String PLUGIN_NAME = "org.lockss.plugin.copernicus.ClockssCopernicusPublicationsPlugin";
        static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
        static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
        static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
        private final String BASE_URL = "http://www.clim-past.net/";
        private final String HOME_URL = "http://www.climate-of-the-past.net/";
        private final String VOLUME_NAME = "8";
        private final String YEAR = "2012";
        private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
                                                                                                BASE_URL_KEY, BASE_URL,
                                                                                                "home_url", HOME_URL,
                                                                                                VOLUME_NAME_KEY, VOLUME_NAME,
                                                                                                YEAR_KEY, YEAR);
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
    assertEquals(ListUtil.list(BASE_URL + VOLUME_NAME + "/"),
		 getRootUrls(artIter));
  }
  
  
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    //"\"^%s%s/[0-9]+/[0-9]+/[A-Za-z0-9-]+\\.html\", base_url,volume_name"

    assertNotMatchesRE(pat, "http://www.clim-past.net/7/1/2012/cp-7-1-2012.html"); //wrong volume
    assertNotMatchesRE(pat, "http://www.clim-past.net/8/1/2012/cp-8-1-2012.pdf"); //pdf not abstract
    assertMatchesRE(pat, "http://www.clim-past.net/8/1/2012/cp-8-1-2012.html");
    assertNotMatchesRE(pat, "http://www.clim-past.net/8/1/2012");
    assertNotMatchesRE(pat, "http://www.clim-past.net/8/1/2012/cp-8-1-2012-supplement.pdf");
  }
  
  
   
  public void testCreateArticleFiles() throws Exception {
    int expCount = 15; // 1 depth, 4 branches, 5 files, but removing some later in test   
    PluginTestUtil.crawlSimAu(sau);
    
 /*
  *  Go through the simulated content you just crawled and modify the results to emulate
  *  what you would find in a "real" crawl with this plugin:
  *  <base_url>/48/<page#>/2011/art#file.{html, pdf & ris}   
  */
    String pat1 = "branch(\\d+)/(\\d+file)\\.html";
    String rep1 = "8/$1/2012/art$2.html";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+file)\\.pdf";
    String rep2 = "8/$1/2012/art$2.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
    String pat3 = "branch(\\d+)/(\\d+file)\\.txt";
    String rep3 = "8/$1/2012/art$2.ris";
    PluginTestUtil.copyAu(sau, au, ".*\\.txt$", pat3, rep3);
    
    // Remove some of the URLs just created to make test more robust
    // Remove files 1file.html and 2file.pdf in each branch (and there are 3 branches)
    int deleted = 0;
    int pdf_deleted = 0;
    for (Iterator it = au.getAuCachedUrlSet().contentHashIterator() ; it.hasNext() ; ) {
      CachedUrlSetNode cusn = (CachedUrlSetNode)it.next();
      if (cusn instanceof CachedUrl) {
        CachedUrl cu = (CachedUrl)cusn;
        String url = cu.getUrl();
        if (url.contains("/2012/")) {
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
    int countNoRis = 0;
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
        ++countNoRis; 
      }
    }
    // missing HTML file will mean no article is picked up at all
    // missing Ris file will be fine
    // there should always be a pdf whenever an article is found
    log.debug("Article count is " + count);
    assertEquals(expCount, count);
    assertEquals(4, countNoRis);
    assertEquals(1, countNoPDF);
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
