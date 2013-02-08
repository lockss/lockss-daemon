/*
 * $Id: TestAMetSocArticleIteratorFactory.java,v 1.1 2013-02-08 00:19:41 alexandraohlson Exp $
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

package org.lockss.plugin.americanmeteorologicalsociety;

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

public class TestAMetSocArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
	private final String PLUGIN_NAME = "org.lockss.plugin.americanmeteorologicalsociety.AMetSocPlugin";
        static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
        static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
        private final String BASE_URL = "http://journals.ametsoc.org/";
        private final String JOURNAL_ID = "wcas";
        private final String VOLUME_NAME = "2";
        private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
                                                                                                BASE_URL_KEY, BASE_URL,
                                                                                                "journal_id", JOURNAL_ID,
                                                                                                VOLUME_NAME_KEY, VOLUME_NAME);
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
	    conf.put("numFiles", "4");
	    conf.put("fileTypes",
	             "" + (SimulatedContentGenerator.FILE_TYPE_HTML |
	                 SimulatedContentGenerator.FILE_TYPE_PDF));
	    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
	    return conf;
	  }
  

  public void testRoots() throws Exception {      
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "doi/abs/"),
		 getRootUrls(artIter));
  }
  
  
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
//     "\"^%sdoi/abs/[.0-9]+/\", base_url";

    assertNotMatchesRE(pat, "http://journals.ametsoc.org/doi/pdf/10.1175/2009WCAS1006.1"); //pdf not abstract
    assertMatchesRE(pat, "http://journals.ametsoc.org/doi/abs/10.1175/2009WCAS1006.1");
    assertNotMatchesRE(pat, "http://journals.ametsoc.org/doi/abs/10.1175");
    assertNotMatchesRE(pat, "http://ametsoc.org/doi/abs/10.1175/2009WCAS1006.1");
  }
  
  
   
  public void testCreateArticleFiles() throws Exception {
    int expCount = 12; // 1 depth, 4 branches, 4 files, but removing 4 later in test
    PluginTestUtil.crawlSimAu(sau);
    
 /*
  *  Go through the simulated content you just crawled and modify the results to emulate
  *  what you would find in a "real" crawl with this plugin: 
  *  <base_url>/doi/{doi,pdf,pdfplus}/10.2514/X.XXXXXX
  */
    String pat1 = "branch(\\d+)/(\\d+)file\\.html";
    String rep1 = "doi/abs/10.1175/b$1.art$2";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+)file\\.pdf";
    String rep2 = "doi/pdf/10.1175/b$1.art$2";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
    
    // Remove some of the URLs just created to make test more robust
    // Remove files b{1-4}.art1 for doi/abs and b{1-4}.art2 for each doi/pdf 
    int deleted = 0;
    int pdf_deleted = 0;
    for (Iterator it = au.getAuCachedUrlSet().contentHashIterator() ; it.hasNext() ; ) {
      CachedUrlSetNode cusn = (CachedUrlSetNode)it.next();
      if (cusn instanceof CachedUrl) {
        CachedUrl cu = (CachedUrl)cusn;
        String url = cu.getUrl();
        if (url.contains("doi/abs/") && (url.endsWith("art001"))) {
          deleteBlock(cu);
          ++deleted;
        } else if (url.contains("doi/pdf") && url.endsWith("art002") && (pdf_deleted == 0)) {
          // delete ONE pdf file to see what happens with no FULL_TEXT_CU
          // it probably won't happen but the publisher might have an error
          pdf_deleted = 1;
          deleteBlock(cu);
        }
      }
    }
    assertEquals(4, deleted); // 4 branches, 1 file removed per branch - don't count PDF file

    Iterator<ArticleFiles> it = au.getArticleIterator();
    int count = 0;
    int countNoPDF = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
         String url = cu.getUrl();
         String contentType = cu.getContentType();
         log.debug("count " + count + " url " + url + " " + contentType);
      } else {
        // only a PDF file is full text
        ++countNoPDF;
      }
    }
    // missing HTML file will mean no article is picked up at all
    // there should always be a pdf whenever an article is found
    log.debug("Article count is " + count);
    assertEquals(expCount, count);
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
