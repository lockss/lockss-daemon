/*
 * $Id$
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

package org.lockss.plugin.georgthiemeverlag;

import java.util.Stack;
import java.util.regex.Pattern;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;

public class TestGeorgThiemeVerlagArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;  // Simulated AU to generate content
  private final String ARTICLE_FAIL_MSG = "Article files not created properly";
  private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
  private static String PLUGIN_NAME = 
      "org.lockss.plugin.georgthiemeverlag.ClockssGeorgThiemeVerlagPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private static final String BASE_URL = "https://www.thieme-connect.de/";
  private final String JOURNAL_ID = "10.1055/s-00000002";
  private final String VOLUME_NAME = "2010";
  
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      JOURNAL_ID_KEY, JOURNAL_ID,
      VOLUME_NAME_KEY, VOLUME_NAME);
  private static final int DEFAULT_FILESIZE = 3000;
  
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
    conf.put(JOURNAL_ID_KEY, JOURNAL_ID);
    conf.put(VOLUME_NAME_KEY, VOLUME_NAME);
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes","" + 
        (SimulatedContentGenerator.FILE_TYPE_HTML | 
         SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL), getRootUrls(artIter));
  }
  
  //
  // We are set up to match any of "<base_url>(html|pdf|abstract)/<doi_prefix/<doi_uid>(.pdf)?((\\?articleLanguage=.*)?"
  //
  // various aspects of an article
  // https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0029-1214947
  // https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947
  // https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947?issue=10.1055/s-003-25342
  // https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf
  // https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf?issue=10.1055/s-003-25342
  // https://www.thieme-connect.de/ejournals/ris/10.1055/s-0031-1296349/BIB
  // https://www.thieme-connect.de/products/ejournals/html/10.1055/s-0037-1608741?articleLanguage=en
  // https://www.thieme-connect.de/products/ejournals/abstract/10.1055/s-0037-1608741?articleLanguage=en
  // https://www.thieme-connect.de/products/ejournals/pdf/10.1055/s-0037-1608741.pdf?articleLanguage=de
  // https://www.thieme-connect.de/products/ejournals/ris/10.1055/s-0037-1608741/BIB?articleLanguage=de
  
  public void testUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // we match to "%sejournals/(html|pdf|abstract)/%s/<doi_uid>(?:\\?articleLanguage=.*)?", base_url, doi_prefix
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947?issue=10.1055/s-003-25342");

    // abstract is now good enough (meeting abstracts should be counted
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0029-1214947");
    // allow for language argument
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/products/ejournals/html/10.1055/s-0037-1608741?articleLanguage=en");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/products/ejournals/abstract/10.1055/s-0037-1608741?articleLanguage=en");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/products/ejournals/pdf/10.1055/s-0037-1608741.pdf?articleLanguage=de");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/products/ejournals/abstract/10.1055/s-0029-1214947");    

    // start to matching urls with "." inside, it is discovered in real case, like
    // https://www.thieme-connect.de/products/ejournals/pdf/10.4103/2454-6798.165082.pdf

    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/html/10.1055/s-0029-1214947");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf?issue=10.1055/s-003-25342");

    // but not to ...
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat,  "http://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947/foogood");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947.foogood");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/html/10.1055/s-0029-1214947");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/ris/10.1055/s-0029-1214947");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf?issue=10.1055/s-003-25342");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947?issue=10.1055/s-003-25342&foo");
    
    // wrong base url
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://ametsoc.org/bitstream/handle/foobar");
  }
  
  /*
   * PDF Full Text: https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf
   * HTML Abstract: https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947
   * 
   * Abstract: https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0029-1214947
   */
  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String[] urls = {
        BASE_URL + "ejournals/html/10.1055/s-1",
        BASE_URL + "ejournals/pdf/10.1055/s-1.pdf",
        BASE_URL + "ejournals/abstract/10.1055/s-1",
        
        BASE_URL + "ejournals/html/10.1055/s-2",
        BASE_URL + "ejournals/abstract/10.1055/s-2",
        
        BASE_URL + "ejournals/html/10.1055/s-3",
        BASE_URL + "ejournals/pdf/10.1055/s-3.pdf",
        
        BASE_URL + "ejournals/html/10.1055/s-4",
        
        BASE_URL + "ejournals/pdf/10.1055/s-5.pdf",
        BASE_URL + "ejournals/abstract/10.1055/s-5",
        
        BASE_URL + "ejournals/pdf/10.1055/s-6.pdf",
        
        BASE_URL + "ejournals/abstract/10.1055/s-77",
        BASE_URL + "ejournals/ris/10.1055/s-77/BIB",
        BASE_URL,
        BASE_URL + "ejournals",
        
        BASE_URL + "ejournals/html/10.1055/s-99?articleLanguage=en",
        BASE_URL + "ejournals/abstract/10.1055/s-99?articleLanguage=en",
        BASE_URL + "ejournals/pdf/10.1055/s-99.pdf?articleLanguage=en",
        BASE_URL + "ejournals/html/10.1055/s-99?articleLanguage=de",
        BASE_URL + "ejournals/abstract/10.1055/s-99?articleLanguage=de",
        BASE_URL + "ejournals/pdf/10.1055/s-99.pdf?articleLanguage=de",
        
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
      if (url.contains("html")) {
        storeContent(cuHtml.getUnfilteredInputStream(),
            cuHtml.getProperties(), url);
      }
      else if (url.contains("pdf")) {
        storeContent(cuPdf.getUnfilteredInputStream(),
            cuPdf.getProperties(), url);
      }
      else if (url.contains("abstract")) {
        storeContent(cuHtml.getUnfilteredInputStream(),
            cuHtml.getProperties(), url);
      }
    }
    
    Stack<String[]> expStack = new Stack<String[]>();
    String [] af3 = {
        BASE_URL + "ejournals/pdf/10.1055/s-1.pdf",
        BASE_URL + "ejournals/html/10.1055/s-1",
        BASE_URL + "ejournals/pdf/10.1055/s-1.pdf",
        BASE_URL + "ejournals/abstract/10.1055/s-1"};
    
    String [] af1 = {
        BASE_URL + "ejournals/html/10.1055/s-2",
        BASE_URL + "ejournals/html/10.1055/s-2",
        null,
        BASE_URL + "ejournals/abstract/10.1055/s-2"};
    
    String [] af4 = {
        BASE_URL + "ejournals/pdf/10.1055/s-3.pdf",
        BASE_URL + "ejournals/html/10.1055/s-3",
        BASE_URL + "ejournals/pdf/10.1055/s-3.pdf",
        BASE_URL + "ejournals/html/10.1055/s-3"};
    
    String [] af2 = {
        BASE_URL + "ejournals/html/10.1055/s-4",
        BASE_URL + "ejournals/html/10.1055/s-4",
        null,
        BASE_URL + "ejournals/html/10.1055/s-4"};
    
    String [] af5 = {
        BASE_URL + "ejournals/pdf/10.1055/s-5.pdf",
        null,
        BASE_URL + "ejournals/pdf/10.1055/s-5.pdf",
        BASE_URL + "ejournals/abstract/10.1055/s-5"};
    
    String [] af6 = {
        BASE_URL + "ejournals/pdf/10.1055/s-6.pdf",
        null,
        BASE_URL + "ejournals/pdf/10.1055/s-6.pdf",
        BASE_URL + "ejournals/pdf/10.1055/s-6.pdf"};

    
    String [] af7 = {
        null,
        null,
        null,
        null};

    // we now consider abstract only sufficient for an AF
    String [] afabs = {
        BASE_URL + "ejournals/abstract/10.1055/s-77",
        null,
        null,
        BASE_URL + "ejournals/abstract/10.1055/s-77"};
    
    // for now leave languages as separate article files
    // ultimately this will combine in to one aspect
    String [] aflang1 = {
        BASE_URL + "ejournals/pdf/10.1055/s-99.pdf?articleLanguage=en",
        BASE_URL + "ejournals/html/10.1055/s-99?articleLanguage=en",
        BASE_URL + "ejournals/pdf/10.1055/s-99.pdf?articleLanguage=en",
        BASE_URL + "ejournals/abstract/10.1055/s-99?articleLanguage=en"};
    
    String [] aflang2 = {
        BASE_URL + "ejournals/pdf/10.1055/s-99.pdf?articleLanguage=de",
        BASE_URL + "ejournals/html/10.1055/s-99?articleLanguage=de",
        BASE_URL + "ejournals/pdf/10.1055/s-99.pdf?articleLanguage=de",
        BASE_URL + "ejournals/abstract/10.1055/s-99?articleLanguage=de"};    

    // the order here matters - expecting the iterator to return AFs in reverse of this order
    // the af7 has to come last - it is a null marker - 
    // I did not originally write this - I don't know what is determining the order of the actual AFs
    expStack.push(af7);
    expStack.push(aflang1);
    expStack.push(aflang2);
    expStack.push(af6);
    expStack.push(af5);
    expStack.push(af4);
    expStack.push(af3);
    expStack.push(af2);
    expStack.push(af1);
    expStack.push(afabs);
    String[] exp;
    
    for ( SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) 
    {
      // the article iterator return aspects with html first, then pdf, then nothing 
      ArticleFiles af = artIter.next();
      String[] act = {
          af.getFullTextUrl(),
          af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
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
    assertEquals("Did not find null end marker", exp, af7);
  }
  
}
