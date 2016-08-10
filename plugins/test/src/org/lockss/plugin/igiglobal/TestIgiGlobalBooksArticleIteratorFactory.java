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

package org.lockss.plugin.igiglobal;

import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.*;

/*
 * Chapter abstract: http://www.igi-global.com/gateway/chapter/20212
 * Chapter PDF with frames: http://www.igi-global.com/gateway/chapter/full-text-pdf/20212
 * Chapter PDF alone: http://www.igi-global.com/pdf.aspx?tid=20212&ptid=464&ctid=3&t=E-Survey+Methodology
 *  where & is encoded as %26 and = is encoded as %3D and tid is the chapter number; ptid is the book number
 */
public class TestIgiGlobalBooksArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private final String ARTICLE_FAIL_MSG = "Article files not created properly";
  private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
  private final String PLUGIN_NAME = "org.lockss.plugin.igiglobal.ClockssIgiGlobalBooksPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String BOOK_ISBN_KEY = "book_isbn";
  static final String VOLUME_NUMBER_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();
  private final String BASE_URL = "http://www.example.com/";
  private final String VOLUME_NUMBER = "464";
  private final String BOOK_ISBN = "9781591407928";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NUMBER_KEY, VOLUME_NUMBER,
      BOOK_ISBN_KEY, BOOK_ISBN);
  private static final int DEFAULT_FILESIZE = 3000;
  
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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "2");
    conf.put("numFiles", "6");
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
            | SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals("Article file root URL pattern changed or incorrect" ,
        ListUtil.list(BASE_URL + "gateway/article/", BASE_URL + "gateway/chapter/"), getRootUrls(artIter));
  }
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/gateway/chapter/20212");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "gateway/book/464"); //Book TOC
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "gateway/full-text-pdf/20212");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "chapter/survey-methodology/20212");
  }
  
  /*
   * Chapter abstract: http://www.igi-global.com/gateway/chapter/20212
   * Chapter PDF with frames: http://www.igi-global.com/gateway/chapter/full-text-pdf/20212
   * Chapter PDF alone: http://www.igi-global.com/pdf.aspx?tid=20212&ptid=464&ctid=3&t=E-Survey+Methodology
   *  where & is encoded as %26 and = is encoded as %3D and tid is the chapter number; ptid is the book number
   */
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    // Order doesn't matter on these. We're just loading a bunch of URLs into a UrlCacher
    // Notice that in some cases we have an abstract and the matchingPDF
    // In other cases we're missing one of the pieces
    // This is to test edge cases in the ArticleIterator
    String[] urls = {
        BASE_URL + "gateway/chapter/full-text-pdf/55656",
        BASE_URL + "gateway/chapter/full-text-pdf/12345",
        BASE_URL + "gateway/chapter/full-text-html/54321",
        BASE_URL + "gateway/chapter/11111",
        BASE_URL + "gateway/chapter/55656",
        BASE_URL + "gateway/chapter/54321",
        BASE_URL + "gateway/book/464",
        BASE_URL,
        BASE_URL + "gateway"
    };
    CachedUrl cuPdf = null;
    CachedUrl cuHtml = null;
    for (CachedUrl cu : AuUtil.getCuIterable(sau)) {
      //
      // The only thing we seem to be doing with the content that was created in the SimulatedAU 
      // Is to pick up one PDF cu and one HTML cu
      // Only the HTML CU is used and then only to put content in to other html URLs in the UrlCacher
      //
      if(cuPdf == null && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF))
	{
	  cuPdf = cu;
	}
      else if (cuHtml == null && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML))
	{
	  cuHtml = cu;
	}
      if (cuPdf != null && cuHtml != null) {
	break;
      }
    }
    // add a URL with content to the "real" au
    // oddly, they'll all be html...the PDF URLs have an thml frameset so this is okay
    for (String url : urls) {
      UrlData ud = new UrlData(cuHtml.getUnfilteredInputStream(), cuHtml.getProperties(),url);
      UrlCacher uc = au.makeUrlCacher(ud);
      uc.storeContent();
    }
    
    Stack<String[]> expStack = new Stack<String[]>();
    // fulltextcu
    // FULL_TEXT_PDF
    // ABSTRACT 
    String [] af1 = {BASE_URL + "gateway/chapter/11111",
        null,
        BASE_URL + "gateway/chapter/11111"};
    
    String [] af2 = {BASE_URL + "gateway/chapter/full-text-html/54321",
        null,
        BASE_URL + "gateway/chapter/54321"};
    
    String [] af3 = {BASE_URL + "gateway/chapter/full-text-pdf/12345",
        BASE_URL + "gateway/chapter/full-text-pdf/12345", 
        null};
    
    String [] af4 = {BASE_URL + "gateway/chapter/full-text-pdf/55656",
        BASE_URL + "gateway/chapter/full-text-pdf/55656",
        BASE_URL + "gateway/chapter/55656"};
    
    expStack.push(af4);
//    expStack.push(af3); // no abstract chapter aspect
    expStack.push(af2);
    expStack.push(af1);
    
    for ( SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) 
    {
      ArticleFiles af = artIter.next();
      String[] act = {
          af.getFullTextUrl(),
          af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE),
          af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT)
      };
      String[] exp = expStack.pop();
      if(act.length == exp.length){
        for(int i = 0;i< act.length; i++){
          assertEquals(ARTICLE_FAIL_MSG + " Expected: " + exp[i] + " Actual: " + act[i], exp[i],act[i]);
        }
      }
      else
        fail(ARTICLE_FAIL_MSG + " length of expected and actual ArticleFiles content not the same:" + exp.length + "!=" + act.length);
    }
    if (!expStack.empty()) {
      fail("Test stack is not empty:");
    }
  }
}
