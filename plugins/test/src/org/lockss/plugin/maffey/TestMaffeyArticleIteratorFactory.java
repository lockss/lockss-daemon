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

package org.lockss.plugin.maffey;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.CIProperties;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/*
 * PDF Full Text: http://www.la-press.com/redirect_file.php?fileType=pdf&fileId=4199&filename=3103-ACI-Holistic-Control-of-Herbal-Teas-and-Tinctures-Based-on-Sage-(Salvia-of.pdf&nocount=1
 * HTML Abstract: http://www.la-press.com/holistic-control-of-herbal-teas-and-tinctures-based-on-sage-salvia-off-article-a3103
 * <meta content="http://la-press.com/redirect_file.php?fileId=4199&filename=3103-ACI-Holistic-Control-of-Herbal-Teas-and-Tinctures-Based-on-Sage-(Salvia-of.pdf&fileType=pdf" name="citation_pdf_url">
 */
public class TestMaffeyArticleIteratorFactory extends ArticleIteratorTestCase {
	
  private static Logger log = Logger.getLogger("TestMaffeyArticleIteratorFactor");
  private final String ARTICLE_FAIL_MSG = "Article files not created properly";
  private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
  private final String PLUGIN_NAME = "org.lockss.plugin.maffey.MaffeyPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private final String BASE_URL = "http://www.la-press.com/";
  private final String YEAR = "2010";
  private final String JOURNAL_ID = "11";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(BASE_URL_KEY,
	    							     BASE_URL,
	    							     YEAR_KEY,
	    							     YEAR,
	    							     JOURNAL_ID_KEY,
	    							     JOURNAL_ID);
  private static final int DEFAULT_FILESIZE = 3000;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    
    au = createAu();
  }
  
  public void tearDown() throws Exception {
	    super.tearDown();
	  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals("Article file root URL pattern changed or incorrect" ,ListUtil.list( BASE_URL),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/hello-world-article-a123");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "hello-world-article-");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "/hello-world-article-a123");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "hello-world-articl-a123");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "hello-world-article-a123");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "hello-world-foo-bar-article-a4");
   }

  public void testCreateArticleFiles() throws Exception {
    String[] urls = {BASE_URL + "hello-world-foo-bar-article-a12",
		     BASE_URL + "foo-bar-hello-world-article-a1234",
		     BASE_URL + "redirect_file.php?fileType=pdf&fileId=1111&filename=Hello-World-(Foo.pdf&nocount=1",
		     BASE_URL + "redirect_file.php?fileType=pdf&fileId=2222&filename=1234-Foo-Bar-(Hello.pdf&nocount=1",
		     BASE_URL + "bad-pdf-article-a22",
		     BASE_URL + "lockss.php?t=lockss&pa=issue&j_id=1&year=2010"};
    
    for (String url : urls) {
      UrlNormalizer norm = new MaffeyUrlNormalizer();
      UrlCacher uc = au.makeUrlCacher(norm.normalizeUrl(url, null));
      
      if(url.contains("-article-a1234")) {
	uc.storeContent(getAbsAlteredInputStream("http://la-press.com/redirect_file.php?fileId=2222&filename=1234-Foo-Bar-(Hello.pdf&fileType=pdf"), getAbsProperties());
      } else if(url.contains("-article-a12")){
	uc.storeContent(getAbsAlteredInputStream("http://la-press.com/redirect_file.php?fileId=1111&filename=Hello-World-(Foo.pdf&fileType=pdf"), getAbsProperties());
      } else if(url.contains("-article-a22")) {
	uc.storeContent(getAbsAlteredInputStream("http://la-press.com/redirect_file.php?fileId=3333&filename=Bad-PDF.pdf&fileType=pdf"), getAbsProperties());
      } else {
	uc.storeContent(getAbsAlteredInputStream(null), getAbsProperties());
      }
    }
    
    Stack<String[]> expStack = new Stack<String[]>();
    String [] af1 = {null,
		     BASE_URL + "bad-pdf-article-a22"};
    
    String [] af2 = {BASE_URL + "redirect_file.php?fileType=pdf&fileId=1111&filename=Hello-World-(Foo.pdf",
    		     BASE_URL + "hello-world-foo-bar-article-a12"};
    
    String [] af3 = {BASE_URL + "redirect_file.php?fileType=pdf&fileId=2222&filename=1234-Foo-Bar-(Hello.pdf",
    		     BASE_URL + "foo-bar-hello-world-article-a1234"};
								
    expStack.push(af2);
    expStack.push(af3);
    expStack.push(af1);
    
    for ( SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      String[] act = {af.getFullTextUrl(),
		      af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT)};
      String[] exp = expStack.pop();
      if (act.length == exp.length) {
 	for (int i = 0;i< act.length; i++) {
	  assertEquals(ARTICLE_FAIL_MSG, exp[i],act[i]);
        }
      } else {
	fail(ARTICLE_FAIL_MSG + " length of expected and actual ArticleFiles content not the same:" + exp.length + "!=" + act.length);
      }
    }
    
  }
  
  private CIProperties getAbsProperties() {
    CIProperties absProps = new CIProperties();
    absProps.put("RESPONSE","HTTP/1.0 200 OK");
    absProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
    absProps.put("Server", "Apache/2.2.3 (CentOS)");
    absProps.put("X-Powered-By", "PHP/5.2.17");
    absProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    absProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
    absProps.put("Pragma", "no-cache");
    absProps.put("Content-Type", "text/html; charset=UTF-8");
    absProps.put("X-Cache", "MISS from lockss.org");
    absProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    absProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    absProps.put("Connection", "close");
    return absProps;
  }
	
  private InputStream getAbsAlteredInputStream(String url) throws IOException {
    InputStream htmlIn = getClass().getResourceAsStream("LibertasAbstract.html");
    String absHtml = StringUtil.fromInputStream(htmlIn);
    return IOUtils.toInputStream(absHtml.replace("<Hello World/>","<meta name=\"citation_pdf_url\" content=\"" + url +"\">"));
  }
	
}
