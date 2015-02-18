/*
 * $Id$
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

package org.lockss.plugin.msue;

import java.io.File;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.*;
import org.lockss.util.*;

/*
 * PDF Full Text: http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e1/e1.pdf
 */
public class TestMichiganStateUniversityExtensionArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;  // Simulated AU to generate content
  private final String ARTICLE_FAIL_MSG = 
      "Article files not created properly";
  private final String PATTERN_FAIL_MSG = 
      "Article file URL pattern changed or incorrect";
  private final String PLUGIN_NAME = 
      "org.lockss.plugin.msue.MichiganStateUniversityExtensionPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private final String BASE_URL = 
      "http://web2.msue.msu.edu/";
  private final String YEAR = "1917";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
                        BASE_URL_KEY, BASE_URL,
                        YEAR_KEY, YEAR);
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
              "" + (SimulatedContentGenerator.FILE_TYPE_PDF));
      conf.put("binFileSize", ""+DEFAULT_FILESIZE);
      return conf;
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals("Article file root URL pattern changed or incorrect", ListUtil.list(BASE_URL),
     getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/Bulletins/Bulletin/PDF/Historical/finished_pubs/e1/e1.pdf");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "wrongdir/Bulletin/PDF/Historical/finished_pubs/e1/e1.pdf");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "Bulletins/wrongdir/PDF/Historical/finished_pubs/e1/e1.pdf");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "Bulletins/Bulletin/wrongdir/Historical/finished_pubs/e1/e1.pdf");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "Bulletins/Bulletin/PDF/wrongdir/finished_pubs/e1/e1.pdf");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "Bulletins/Bulletin/PDF/Historical/wrongdir/e1/e1.pdf");;
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e1/e1.pdf");
  }

  /*
   * PDF Full Text: http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e1/e1.pdf
   */
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    String[] urls = {
      BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e1/e1.pdf",
      BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e3/e3rev2.pdf",
      BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e4/e4.pdf",
      BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e6/e6rev2.pdf"
    };
    
    CachedUrl cuPdf = null;
    for (CachedUrl cu : AuUtil.getCuIterable(sau)) {
      if (cuPdf == null && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF)) {
        cuPdf = cu;
        break;
      }
    }
    for(String url : urls) {
      UrlCacher uc = au.makeUrlCacher(
          new UrlData(cuPdf.getUnfilteredInputStream(),
              cuPdf.getProperties(), url));
      uc.storeContent();
    }
    
    Stack<String[]> expStack = new Stack<String[]>();
    String [] af1 = {
          BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e1/e1.pdf"
            };
    String [] af2 = {
          BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e3/e3rev2.pdf"
            };
    String [] af3 = {
          BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e4/e4.pdf"
            };
    String [] af4 = {
          BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e6/e6rev2.pdf"
            };
    expStack.push(af4);
    expStack.push(af3);
    expStack.push(af2);
    expStack.push(af1);
    
    for (SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext();) {
      ArticleFiles af = artIter.next();
      String[] act = {
        af.getFullTextUrl(),
      };
      String[] exp = expStack.pop();
      if(act.length == exp.length){
        for(int i = 0; i< act.length; i++){
          assertEquals(ARTICLE_FAIL_MSG + " Expected: " + exp[i] + " Actual: " + act[i], exp[i],act[i]);
        }
      }
      else fail(ARTICLE_FAIL_MSG + " length of expected and actual ArticleFiles content not the same:" + exp.length + "!=" + act.length);
    }
  }     
}