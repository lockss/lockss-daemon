/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.highwire.bmj;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Pattern;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.test.*;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;

public class TestBMJJCoreArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  
  private final String PLUGIN_NAME = "org.lockss.plugin.highwire.bmj.BMJDrupalPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.bmj.org/";
  private final String BASE_URL_NO_WWW = "http://bmj.org/";
  private final String BASE_URL_HTTPS = "https://www.bmj.org/";
  private final String BASE_URL_HTTPS_NO_WWW = "https://bmj.org/";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, "347");
  private String BASE_AU_URL = BASE_URL + "content/";
  private final String BASE_AU_URL_NO_WWW =  BASE_URL_NO_WWW + "content/";
  private final String BASE_AU_URL_HTTPS = BASE_URL_HTTPS + "content/";
  private final String BASE_AU_URL_HTTPS_NO_WWW = BASE_URL_HTTPS_NO_WWW + "content/";
  private static final int DEFAULT_FILESIZE = 3000;
  private final String ARTICLE_FAIL_MSG = "Article files not created properly";
  
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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(VOLUME_NAME_KEY, "2012");
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "6");
    conf.put("fileTypes", "" +
        (SimulatedContentGenerator.FILE_TYPE_PDF |
            SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }
  
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_AU_URL_NO_WWW,  BASE_AU_URL_HTTPS_NO_WWW, BASE_AU_URL, BASE_AU_URL_HTTPS), getRootUrls(artIter));
  }
  
  
  //
  // We are set up to match any of "<base_url>content/347/bmj.e<pg>(.full.pdf)"
  //
  
  public void testUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertMatchesRE(pat, "http://www.bmj.org/content/347/bmj.f3757");
    assertMatchesRE(pat, "http://www.bmj.org/content/347/7373/1164");
    // but not to ...
    assertNotMatchesRE(pat, "http://www.bmj.org/content/bmj/347");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/bmj/347.1");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/bmj/347/");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/bmj/347/bmj.f3757");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/347/bmj.f3757.abstract");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/347/bmj.f3757.full");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/347/bmj.f3757.full.pdf");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/347/bmj.f3757.full.pdf+html");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/347/7938");
    assertNotMatchesRE(pat, "http://www.bmj.org/content/347/7938.toc");
    
    // wrong base url
    assertNotMatchesRE(pat, "http://ametsoc.org/bitstream/handle/foobar");
  }
  
  //
  // We are set up to match any of "<base_url>content/<vol>/.*[.]body"
  //
  
  //
  // simAU was created with only one depth
  // 1 filetype (html) and 2 files of each type
  // So the total number of files of all types is 2
  // simAU file structures looks like this branch01/01file.html
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with BMJDrupal:
     *  <base_url>content/<num>/e<pg>
     */
    
    String pat0 = "(?!branch)00([1-6])file[.]html";
    // turn xxfile.html into body
    String rep0 = "content/347/bmj.f$1";
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat0, rep0);
    
    String pat1 = "branch(\\d+)/\\d+([45])file[.]pdf";
    String rep1 = "content/347/bmj.f$2.full.pdf";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat1, rep1);
    
    String pat2 = "branch(\\d+)/\\d+([23])file[.]pdf";
    String rep2 = "content/347/bmj.f$2.full.pdf";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat2, rep2);
    
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullText= 0;
    int countMetadata = 0;
    int countPdf = 0;
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
      cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF);
      if (cu != null) {
        ++countPdf;
      }
    }
    // potential article count is 6 (1 branch * 6 files each branch)
    int expCount = 6;
    
    log.debug3("Article count is " + count);
    assertEquals(expCount, count);
    
    // you will get full text for ALL articles
    assertEquals(expCount, countFullText);
    
    // you will get metadata for all
    assertEquals(expCount, countMetadata);
    
    // you will get pdf for 4 (2 bmj pdf + 2 pdf)
    assertEquals(4, countPdf);
  }
  
  /*
   * PDF Full Text: http://www.bmj.org/content/347/bmj.e1.full.pdf
   * HTML/Abstract: http://www.bmj.org/content/347/bmj.e1
   * 
   * http://www.bmj.org/content/347/bmj.e1/article-data
   */
  public void testCreateArticleFiles2() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String[] urls = {
        BASE_URL + "content/347/bmj.e1.full.pdf",
        BASE_URL + "content/347/bmj.e1",
        BASE_URL + "content/347/bmj.e1.full",
        
        BASE_URL + "content/347/bmj.e10.full.pdf",
        BASE_URL + "content/347/bmj.e10",
        
        BASE_URL + "content/347/bmj.e3",
        BASE_URL + "content/347/bmj.e3/article-data",
        
        BASE_URL + "content/347/bmj.e4.full.pdf",
        BASE_URL + "content/347/bmj.e4",
        
        
        BASE_URL + "content/347/bmj.e18/article-data",
        BASE_URL + "content/347/bmj.e19.full.pdf",
        BASE_URL,
        BASE_URL + "content"
    };
    CachedUrl cuPdf = null;
    CachedUrl cuHtml = null;
    for (CachedUrl cu : AuUtil.getCuIterable(sau)) {
      if (cuPdf == null && 
          cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF))
      {
        cuPdf = cu;
      }
      else if (cuHtml == null && 
          cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML))
      {
        cuHtml = cu;
      }
      if (cuPdf != null && cuHtml != null) {
        break;
      }
    }
    
    for (String url : urls) {
      InputStream input = null;
      CIProperties props = null;
      if (url.contains("pdf+html")) {
        input = cuHtml.getUnfilteredInputStream();
        props = cuHtml.getProperties();
      }
      else if (url.contains("pdf")) {
        input = cuPdf.getUnfilteredInputStream();
        props = cuPdf.getProperties();
      }
      else {
        input = cuHtml.getUnfilteredInputStream();
        props = cuHtml.getProperties();
      }
      UrlData ud = new UrlData(input, props, url);
      UrlCacher uc = au.makeUrlCacher(ud);
      uc.storeContent();
    }
    
    Stack<String[]> expStack = new Stack<String[]>();
    String [] af1 = {
        BASE_URL + "content/347/bmj.e1",
        BASE_URL + "content/347/bmj.e1.full.pdf",
        BASE_URL + "content/347/bmj.e1"};
    
    String [] af2 = {
        BASE_URL + "content/347/bmj.e10",
        BASE_URL + "content/347/bmj.e10.full.pdf",
        BASE_URL + "content/347/bmj.e10"};
    
    String [] af3 = {
        BASE_URL + "content/347/bmj.e3",
        null,
        BASE_URL + "content/347/bmj.e3"};
    
    String [] af4 = {
        BASE_URL + "content/347/bmj.e4",
        BASE_URL + "content/347/bmj.e4.full.pdf",
        BASE_URL + "content/347/bmj.e4"};
    
    String [] af5 = {
        null,
        null,
        null,
        null};
    
    expStack.push(af5);
    expStack.push(af4);
    expStack.push(af3);
    expStack.push(af2);
    expStack.push(af1);
    String[] exp;
    
    for ( SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) 
    {
      // the article iterator return aspects with html first, then pdf, then nothing 
      ArticleFiles af = artIter.next();
      String[] act = {
          af.getFullTextUrl(),
          af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF),
          af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA)
      };
      System.err.println(" ");
      exp = expStack.pop();
      if(act.length == exp.length){
        for(int i = 0;i< act.length; i++){
          System.err.println(" Expected: " + exp[i] + "\n   Actual: " + act[i]);
          assertEquals(ARTICLE_FAIL_MSG + " Expected: " + exp[i] + "\n   Actual: " + act[i], exp[i],act[i]);
        }
      }
      else fail(ARTICLE_FAIL_MSG + " length of expected and actual ArticleFiles content not the same:" + exp.length + "!=" + act.length);
    }
    exp = expStack.pop();
    assertEquals("Did not find null end marker", exp, af5);
  }
}
