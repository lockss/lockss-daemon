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

package org.lockss.plugin.royalsocietyofchemistry;

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

public class TestRSC2014ArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  
  private final String PLUGIN_NAME =
      "org.lockss.plugin.royalsocietyofchemistry.ClockssRSC2014Plugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String RESOLVER_URL_KEY = "resolver_url";
  static final String GRAPHICS_URL_KEY = "graphics_url";
  static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
  static final String JOURNAL_CODE_KEY = "journal_code";
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  private final String BASE_URL = "http://pubs.rsc.org/";
  private final String RESOLVER_URL = "http://xlink.rsc.org/";
  private final String GRAPHICS_URL = "http://img.rsc.org/";
  private final String BASE_URL2 = "http://www.rsc.org/";
  private final String JOURNAL_CODE = "an";
  private final String VOLUME = "123";
  private final String YEAR = "2013";
  private final Configuration AU_CONFIG1 = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      RESOLVER_URL_KEY, RESOLVER_URL,
      GRAPHICS_URL_KEY, GRAPHICS_URL,
      BASE_URL2_KEY, BASE_URL2);
  private final Configuration AU_CONFIG2 = ConfigurationUtil.fromArgs(
      JOURNAL_CODE_KEY, JOURNAL_CODE,
      VOLUME_NAME_KEY, VOLUME,
      YEAR_KEY, YEAR);
  private final Configuration AU_CONFIG = ConfigurationUtil.merge(AU_CONFIG1, AU_CONFIG2);
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
    conf.put(RESOLVER_URL_KEY, RESOLVER_URL);
    conf.put(GRAPHICS_URL_KEY, GRAPHICS_URL);
    conf.put(BASE_URL2_KEY, BASE_URL2);
    conf.put(JOURNAL_CODE_KEY, "an");
    conf.put(VOLUME_NAME_KEY, "123");
    conf.put(YEAR_KEY, "2013");
    conf.put("depth", "1");
    conf.put("branch", "0");
    conf.put("numFiles", "4");
    conf.put("fileTypes", "" + (
        SimulatedContentGenerator.FILE_TYPE_HTML |
        SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "en/content/articlelanding/2013/an/",
        BASE_URL + "en/content/articlepdf/2013/an/"),
        getRootUrls(artIter));
  }
  
  //
  // We are set up to match any of <journal_id>/<year>/p.*
  //
  // Removing this test as there is no Pattern now
  public void deletetestUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    /*
     * http://pubs.rsc.org/en/content/articlelanding/2009/gc/b906831g
     * http://pubs.rsc.org/en/content/articlepdf/2009/gc/b906831g
     */
    // we match to 
    // "%sen/content/article(?:landing|html|pdf)/%d/%s/", base_url, year, journal_code
    assertMatchesRE(pat, "http://pubs.rsc.org/en/content/articlelanding/2013/an/b906831g");
    assertMatchesRE(pat, "http://pubs.rsc.org/en/content/articlepdf/2013/an/b906831g");
    
    // but not to html and not to link nor image
    assertNotMatchesRE(pat, "http://pubs.rsc.org/en/content/articlehtml/2013/an/b906831g");
    assertNotMatchesRE(pat, "http://xlink.rsc.org/?doi=b906831g");
    assertNotMatchesRE(pat, "http://pubs.rsc.org/services/images/RSCpubs.ePlatform.Service.FreeContent.ImageService.svc/ImageService/image/GA?id=B90683G");
    
    // wrong base url
    assertNotMatchesRE(pat, "http://foo.rsc.org/an/2013/3-1/p01.xhtml");
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
     *  what you would find in a "real" crawl with RSC:
     *  <base_url>/en/content/article[landing|html|pdf]/<year>/<journal_id>/<id>.*
     */
    
    String pat1 = "(\\d+)file[.]html";
    // copy xxfile.xhtml into landing & html
    String repLand = "en/content/articlelanding/" + YEAR + "/" + JOURNAL_CODE + "/" + "S-$1";
    String repHtml = "en/content/articlehtml/" + YEAR + "/" + JOURNAL_CODE + "/" + "S-$1";
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat1, repLand);
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat1, repHtml);
    // copy xxfile.pdf
    String pat2 = "(\\d+)file[.]pdf";
    String repPdf = "en/content/articlepdf/" + YEAR + "/" + JOURNAL_CODE + "/" + "S-$1";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat2, repPdf);
    
    // At this point we have 10 sets of 2 types of articles
    // Remove some of the URLs just created to make test more robust
    
    int deleted = 0;
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
        String url = cu.getUrl();
        if (url.contains("en/content/articlelanding/2013/an/S-002")) {
          deleteBlock(cu);
          ++deleted;
        }
        if (url.contains("en/content/articlepdf/2013/an/S-004")) {
          deleteBlock(cu);
          ++deleted;
        }
    }
    assertEquals(2, deleted);
    
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullTextHtml= 0;
    int countFullTextPdf= 0;
    int countMetadata = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;
      //log.info(af.toString());
      CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML);
      if ( cu != null) {
        ++countFullTextHtml;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF);
      if ( cu != null) {
        ++countFullTextPdf;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (cu != null) {
        ++countMetadata;
      }
    }
    // potential article count is 4
    int expCount = 4;
    
    log.debug3("Article count is " + count);
    assertEquals(expCount, count);
    
    // you will get a full text html for no articles
    assertEquals(0, countFullTextHtml);
    
    // you will get a full text pdf for all but 1 article
    assertEquals(expCount-1, countFullTextPdf);
    
    // you will get metadata for all but 1 article
    assertEquals(expCount-1, countMetadata);
  }
  
  private void deleteBlock(CachedUrl cu) throws IOException {
    //log.info("deleting " + cu.getUrl());
    cu.delete();
  }
}
