/*
 * $Id: TestBaseAtyponArticleIteratorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.springer.link;

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

public class TestSpringerLinkArticleIteratorFactory extends ArticleIteratorTestCase {

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content

  private final String PLUGIN_NAME = "org.lockss.plugin.springer.link.SpringerLinkJournalsPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String DOWNLOAD_URL_KEY = "download_url";
  static final String JOURNAL_EISSN_KEY = "journal_eissn";
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String DOWNLOAD_URL = "http://www.example.download.com/";
  private final String BASE_URL = "http://www.link.example.com/";
  private final String JOURNAL_EISSN = "1234-1234";
  private final String VOLUME_NAME = "3";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      DOWNLOAD_URL_KEY, DOWNLOAD_URL,
      JOURNAL_EISSN_KEY, JOURNAL_EISSN,
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
    conf.put("depth", "1");
    conf.put("branch", "5");
    conf.put("numFiles", "4");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_HTML |
            SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }


  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL),
        getRootUrls(artIter));
  }



  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // we match to doi/(full|pdf|pdfplus)
    assertMatchesRE(pat, "http://www.link.example.com/book/10.1137/100818522");
    assertMatchesRE(pat, "http://www.link.example.com/article/10.1137/100818522");
    assertMatchesRE(pat, "http://www.link.example.com/article/10.1137/10081.8522");
    //not allow "/" in 2nd part of DOI
    assertNotMatchesRE(pat, "http://www.link.example.com/book/10.1137/ABC1234-3/fff");
    //no chapters
    assertNotMatchesRE(pat, "http://www.link.example.com/chapter/10.1137/100818522");
    // must have both parts of DOI
    assertNotMatchesRE(pat, "http://www.link.example.com/book/10.1137");
  }


  public void testCreateArticleFilesJournals() throws Exception {
        PluginTestUtil.crawlSimAu(sau);


    // turn xxfile.html in to both abstracts and fulls
    String pat1 = "branch(\\d+)/(\\d+)file\\.html";
    String repAbs = "article/10.1137/b$1.art$2";
    String repFull = "article/10.1137/b$1.art$2.fulltext.html";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, repAbs);
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, repFull);

    // turn xxfile.pdf in to both pdf 
    String pat2 = "branch(\\d+)/(\\d+)file\\.pdf";
    String reppdf = "content/pdf/10.1137/b$1.art$2.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, reppdf);
    

    int deleted = 0;
    int deletedRIS = 0;
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        // branch 1 - all or none
        if (url.contains("b1.art00")) {
          if (url.contains("art001")) {
            deleteBlock(cu);
            ++deleted;
          }
        } else if (url.contains("b2.art00")) {
        // branch 2 - singletons left
          if (url.contains("article") && !(url.endsWith("art001")) ){
            deleteBlock(cu);
            ++deleted;
          }
          if (url.contains("content/pdf/") && !(url.contains("art003")) ) {
            deleteBlock(cu);
            ++deleted;
          }
        } 
    }
    assertEquals(13, deleted); // trust me

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
        ++countMetadata;
      }
    }

    int expCount = 31; 

    log.debug3("Article count is " + count);
    assertEquals(expCount, count);

    assertEquals(31, countFullText);
    
    assertEquals(expCount, countMetadata);
  }

  private void deleteBlock(CachedUrl cu) throws IOException {
    //log.info("deleting " + cu.getUrl());
    cu.delete();
  }
}
