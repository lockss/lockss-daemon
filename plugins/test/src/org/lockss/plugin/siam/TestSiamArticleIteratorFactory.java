/*
 * $Id: TestSiamArticleIteratorFactory.java,v 1.1 2013-03-20 17:56:48 alexandraohlson Exp $
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

package org.lockss.plugin.siam;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.SingleNodeCachedUrlSetSpec;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
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
import org.lockss.state.NodeManager;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.ListUtil;

public class TestSiamArticleIteratorFactory extends ArticleIteratorTestCase {

	private SimulatedArchivalUnit sau;	// Simulated AU to generate content

	private final String PLUGIN_NAME = "org.lockss.plugin.siam.SiamPlugin";
        static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
        static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
        private final String BASE_URL = "http://epubs.siam.org/";
        private final String JOURNAL_ID = "mmsubt";
        private final String VOLUME_NAME = "9";
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

  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }

  @Override
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
    assertEquals(ListUtil.list(BASE_URL + "doi/"),
		 getRootUrls(artIter));
  }



  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
//     "\"^%sdoi/abs/[.0-9]+/\", base_url";

    assertMatchesRE(pat, "http://epubs.siam.org/doi/pdf/10.1137/100818522"); //pdf not abstract
    assertMatchesRE(pat, "http://epubs.siam.org/doi/abs/10.1137/100818522");
    assertNotMatchesRE(pat, "http://epubs.siam.org/doi/abs/10.1137");
    assertNotMatchesRE(pat, "http://ametsoc.org/doi/abs/10.1175/2009WCAS1006.1");
  }



  public void testCreateArticleFiles() throws Exception {
    int expCount = 15; // 1 depth, 4 branches, 4 files, remove 4 abstracts, but pdf will compensate except for 1 without both
    PluginTestUtil.crawlSimAu(sau);

 /*
  *  Go through the simulated content you just crawled and modify the results to emulate
  *  what you would find in a "real" crawl with this plugin:
  *  <base_url>/doi/{doi,pdf,ref}/10.1137/X.XXXXXX
  *  Currently this test doesn't create any "ref" html files
  */
    String pat1 = "branch(\\d+)/(\\d+)file\\.html";
    String rep1 = "doi/abs/10.1137/b$1.art$2";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+)file\\.pdf";
    String rep2 = "doi/pdf/10.1137/b$1.art$2";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);

    // Remove some of the URLs just created to make test more robust
    // Remove files b{1-4}.art1 for doi/abs and one doi/pdf (which has abstract) and one doi/pdf so neither 
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
        } else if (url.contains("doi/pdf") && url.endsWith("b1.art002")) {
          // delete ONE pdf file to see what happens
          // it probably won't happen but the publisher might have an error
          ++pdf_deleted;
          deleteBlock(cu);
        } else if (url.contains("doi/pdf") && url.endsWith("b2.art001")) {
          /* this one will have neither abstract nor pdf */
          ++pdf_deleted;
          deleteBlock(cu);
        }
      }
    }
    assertEquals(4, deleted); // 4 branches, 1 file removed per branch - don't count PDF file
    assertEquals(2, pdf_deleted);

    Iterator<ArticleFiles> it = au.getArticleIterator();
    int count = 0;
    int countFullText= 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
          ++countFullText;
        }
    }

    log.debug("Article count is " + count);
    assertEquals(expCount, count);
    assertEquals(14, countFullText);
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
