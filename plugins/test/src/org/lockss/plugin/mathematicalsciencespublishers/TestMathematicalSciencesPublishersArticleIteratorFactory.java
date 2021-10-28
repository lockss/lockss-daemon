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

package org.lockss.plugin.mathematicalsciencespublishers;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;

public class TestMathematicalSciencesPublishersArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  
  private final String PLUGIN_NAME =
      "org.lockss.plugin.mathematicalsciencespublishers.ClockssMathematicalSciencesPublishersPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private final String BASE_URL = "http://msp.org/";
  private final String JOURNAL_ID = "jid";
  private final String YEAR = "2008";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      JOURNAL_ID_KEY, JOURNAL_ID,
      YEAR_KEY, YEAR);
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

    ConfigurationUtil.addFromArgs(CachedUrl.PARAM_ALLOW_DELETE, "true");
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
    conf.put(JOURNAL_ID_KEY, "jid");
    conf.put(YEAR_KEY, "2008");
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "10");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_XHTML |
            SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "jid/2008/"),
        getRootUrls(artIter));
  }
  
  
  //
  // We are set up to match any of <journal_id>/<year>/p.*
  //
  
  public void testUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // we match to "%s%s/%s/[0-9-]+/p.+\.xhtml", base, jid, year
    assertMatchesRE(pat, "http://msp.org/jid/2008/3-1/p01.xhtml"); 
    
    // but not to pdf or index
    assertNotMatchesRE(pat, "http://msp.org/camcos/2008/3-1/camcos-v3-n1-p01-p.pdf");
    assertNotMatchesRE(pat, "http://msp.org/camcos/2008/3-1/index.xhtml");
    assertNotMatchesRE(pat, "http://msp.org/jid/2008/3-1/p01.jpg");
    assertNotMatchesRE(pat, "http://msp.org/jid/2008/3-1/b01.xhtml");
    
    // wrong base url
    assertNotMatchesRE(pat, "http://ametsoc.org/jid/2008/3-1/p01.xhtml");
  }
  
  //
  // simAU was created with only one depth
  // 3 filetypes (html & pdf & txt) and 4 files of each type
  // So the total number of files of all types is 40 (5 * (4*2)) + 4 RIS (handled independently)
  // simAU file structures looks like this branch01/01file.html or branch04/08file.pdf, etc
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with MathematicalSciencesPublishers:
     *  <base_url>/<journal_id>/<year>/X.XXXXXX
     *  http://msp.org/involve/2013/6-1/p01.xhtml
     *  http://msp.org/ant/2011/5-2/pC1.xhtml
     *  http://msp.org/pjm/2007/229-2/pjm-v229-n2-p10-s.pdf
     */
    
    String pat1 = "(\\d+)file[.]xhtml";
    // turn xxfile.xhtml into abstracts
    String repAbs = JOURNAL_ID + "/" + YEAR + "/31-9/p$1.xhtml";
    PluginTestUtil.copyAu(sau, au, ".*[.]xhtml$", pat1, repAbs);
    // turn xxfile.pdf into fulltext pdfs
    String pat2 = "(\\d+)file[.]pdf";
    String repPdf = JOURNAL_ID + "/" + YEAR + "/31-9/" + JOURNAL_ID + 
        "-v31-n9-p$1.pdf";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat2, repPdf);

    String pat3 = "branch(\\d+)/(\\d+)file[.]xhtml";
    String pat4 = "branch(\\d+)/(\\d+)file[.]pdf";
    String repAbs2 = JOURNAL_ID + "/" + YEAR + "/32-$1/p$2.xhtml";
    String repPdf2 = JOURNAL_ID + "/" + YEAR + "/32-$1/" + JOURNAL_ID + 
        "-v32-n$1-p$2.pdf";
    PluginTestUtil.copyAu(sau, au, ".*[.]xhtml$", pat3, repAbs2);
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat4, repPdf2);
    
    // At this point we have 10 sets of 2 types of articles
    // Remove some of the URLs just created to make test more robust
    //
    // branch1: remove all aspects for 009
    int deleted = 0;
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        // branch 1 - all or none
        if (url.contains("-1/")) {
          if (url.contains("009")) {
            deleteBlock(cu);
            ++deleted;
          }
          if (url.contains("007.xhtml")) {
            deleteBlock(cu);
            ++deleted;
          }
        }
    }
    assertEquals(3, deleted); // trust me

    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullText= 0;
    int countMetadata = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;
      //log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
        ++countFullText;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (cu != null) {
        ++countMetadata; // will be abstract aspect
      }
    }
    // potential article count is 20 (2 branches * 10 files each branch) = 20
    // subtract the one where we removed everything
    int expCount = 18; 

    log.debug3("Article count is " + count);
    assertEquals(expCount, count);

    // you will get a full text for ALL articles
    assertEquals(expCount, countFullText);
    
    // you will get metadata for ALL articles
    assertEquals(expCount, countMetadata); //don't count the one where we removed everything
  }

  private void deleteBlock(CachedUrl cu) throws IOException {
    //log.info("deleting " + cu.getUrl());
    cu.delete();
  }
}
