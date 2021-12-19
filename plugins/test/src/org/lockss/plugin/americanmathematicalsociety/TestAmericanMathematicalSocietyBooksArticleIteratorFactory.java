/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americanmathematicalsociety;

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

public class TestAmericanMathematicalSocietyBooksArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  
  private final String PLUGIN_NAME =
      "org.lockss.plugin.americanmathematicalsociety.ClockssAmericanMathematicalSocietyBooksPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String COLLECTION_ID_KEY = "collection_id";
  static final String YEAR_KEY = "year_string";
  private final String BASE_URL = "http://www.ams.org/";
  private final String COLLECTION_ID = "colid";
  private final String YEAR = "2008";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      COLLECTION_ID_KEY, COLLECTION_ID,
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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(COLLECTION_ID_KEY, "colid");
    conf.put(YEAR_KEY, "2008");
    conf.put("depth", "1");
    conf.put("branch", "0");
    conf.put("numFiles", "4");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_HTML |
            SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "books/colid/"),
        getRootUrls(artIter));
  }
  
  //
  // We are set up to match any of <COLLECTION_id>/<year>/p.*
  //
  
  public void testUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // we match to "^%sbooks/(%s)/([0-9.]+)(/\\1\\2\\.pdf)?$", base_url, collection_i
    assertMatchesRE(pat, "http://www.ams.org/books/colid/200");
    assertMatchesRE(pat, "http://www.ams.org/books/colid/200/colid200.pdf");
    assertMatchesRE(pat, "http://www.ams.org/books/colid/010.1");
    assertMatchesRE(pat, "http://www.ams.org/books/colid/010.1/colid010.1.pdf");
    
    // but not to pdf or image http://www.ams.org/books/conm/629/conm629-endmatter.pdf
    assertNotMatchesRE(pat, "http://www.ams.org/colid/200/colid200-endmatter.pdf");
    assertNotMatchesRE(pat, "http://www.ams.org/images/remote-access-icon.png");
    assertNotMatchesRE(pat, "http://www.ams.org/books/colid/200/12300");
    assertNotMatchesRE(pat, "http://www.ams.org/books/colid/200/12300/colid200-12300.pdf");
    
    // wrong base url
    assertNotMatchesRE(pat, "http://ametsoc.org/colid/2008/3-1/p01.xhtml");
  }
  
  //
  // simAU was created with only one depth
  // 2 filetypes (html & pdf) and 4 files of each type
  // So the total number of files of all types is 8 (4*2)
  // simAU file structures looks like this 01file.html or 08file.pdf, etc
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with AmericanMathematicalSociety:
     *  <base_url>/books/<collection_id>/<year>-.*
     */
    
    String pat1 = "(\\d+)file[.]html";
    // turn xxfile.xhtml into abstracts
    String repHtml = "books/" + COLLECTION_ID + "/" + "$1";
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat1, repHtml);
    // turn xxfile.pdf into fulltext pdfs
    String pat2 = "(\\d+)file[.]pdf";
    String repPdf = "books/" + COLLECTION_ID + "/" +
        "$1/" + COLLECTION_ID + "$1.pdf";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat2, repPdf);
    
    // At this point we have 10 sets of 2 types of articles
    // Remove some of the URLs just created to make test more robust
    
    int deleted = 0;
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        if (url.contains("003.pdf")) {
          deleteBlock(cu);
          ++deleted;
        }
    }
    assertEquals(1, deleted);
    
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullTextPdf= 0;
    int countMetadata = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;
      //log.info(af.toString());
      CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF);
      if ( cu != null) {
        ++countFullTextPdf;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (cu != null) {
        ++countMetadata;
      }
    }
    // potential article count is 4
    // subtract the one where we removed pdf
    int expCount = 4;
    
    log.debug3("Article count is " + count);
    assertEquals(expCount, count);
    
    // you will get a full text for ALL articles
    assertEquals(expCount-1, countFullTextPdf);
    
    // you will get metadata for ALL articles
    assertEquals(expCount, countMetadata);
  }
  
  private void deleteBlock(CachedUrl cu) throws IOException {
    //log.info("deleting " + cu.getUrl());
    cu.delete();
  }
}
