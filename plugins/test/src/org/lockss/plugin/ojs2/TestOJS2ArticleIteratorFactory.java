/* $Id$

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

package org.lockss.plugin.ojs2;

import java.io.*;
import org.apache.commons.io.IOUtils;

import java.util.*;
import java.util.Iterator;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

/* 
 * Processes the table of content:
 * http://www.ojs2articleiteratortest.com/index.php/lq/issue/view/478
 *
 * Stores sample article URLs with different article files aspects:
 * Abstract - http://www.ojs2articleiteratortest.com/index.php/lq/article
 *                      /view/8110
 * Full Text HTML - http://www.ojs2articleiteratortest.com/lq/article
 *                      /view/8110/8514
 * Full Text PDF - http://www.ojs2articleiteratortest.com/index.php/lq/article
 *                      /view/8110/8601
 */

public class TestOJS2ArticleIteratorFactory extends ArticleIteratorTestCase {
  
  static Logger log = Logger.getLogger(TestOJS2ArticleIteratorFactory.class);

  private final String PLUGIN_NAME = "org.lockss.plugin.ojs2.OJS2Plugin";
  private final String BASE_URL = "http://www.ojs2articleiteratortest.com/";
  private final String JOURNAL_ID = "lq";
  private final String YEAR = "2012";
  
  // expected urls for article 1
  private final String EXPECTED_ABS_URL_1 =
    "http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110";
  private final String EXPECTED_ARTICLE_METADATA_URL_1 =
    "http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110";
  private final String EXPECTED_FULL_TEXT_HTML_URL_1 = 
    "http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110/8514";
  private final String EXPECTED_PDF_URL_1 = 
    "http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110/8601";
  private final String EXPECTED_FULL_TEXT_URL_1 = EXPECTED_FULL_TEXT_HTML_URL_1;

  String [] expectedUrls1 = { EXPECTED_ABS_URL_1,
                              EXPECTED_ARTICLE_METADATA_URL_1,
                              EXPECTED_FULL_TEXT_URL_1,
                              EXPECTED_FULL_TEXT_HTML_URL_1,
                              EXPECTED_PDF_URL_1 };

  // expected urls for article 2
  private final String EXPECTED_ABS_URL_2 =
    "http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8120";
  private final String EXPECTED_ARTICLE_METADATA_URL_2 =
    "http://www.ojs2articleiteratortest.com/index.php/lq/article/viewArticle/8120";
  private final String EXPECTED_FULL_TEXT_HTML_URL_2 = 
    "http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8120/8515";
  private final String EXPECTED_PDF_URL_2 = 
    "http://www.ojs2articleiteratortest.com/index.php/lq/article/download/8120/8602";
  private final String EXPECTED_FULL_TEXT_URL_2 = EXPECTED_FULL_TEXT_HTML_URL_2;
  
  String [] expectedUrls2 = { EXPECTED_ABS_URL_2,
                              EXPECTED_ARTICLE_METADATA_URL_2,
                              EXPECTED_FULL_TEXT_URL_2,
                              EXPECTED_FULL_TEXT_HTML_URL_2,
                              EXPECTED_PDF_URL_2 };
  
  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
  }
    
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return 
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, OJS2AuConfig());
  }     

  Configuration OJS2AuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JOURNAL_ID);
    conf.put("year", YEAR);
    return conf;
  }
  
  // Simulated content URLs stored in the UrlCacher object.
  public void storeTestContent(String url) throws Exception {
    log.info("storeTestContent() url: " + url);
    InputStream input = null;
    CIProperties props = null;
    // issue table of content
    if (url.contains("478")) { 
      input = getTestTocInputStream();
      props = getHtmlProperties();
    } else if (url.endsWith("8110") || url.endsWith("8120")
        || url.endsWith("8514") || url.endsWith("8515")) {
      // abs/full-text html
      input = new StringInputStream("<html></html>");
      props = getHtmlProperties();
    } else if (url.endsWith("8601") || url.endsWith("8602")) {
      // pdf
      input = new StringInputStream("");
      props = getPdfProperties();
    }
    UrlData ud = new UrlData(input, props, url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
  }  
  
  // Create simulated article files for testing. Store them in UrlCacher object
  // Access OJS2ArticleItrerator to process the table of content page (TOC).
  // The simulated TOC file being read in is test_toc_junit.html
  // which can be found in the same directory as this TestOJS2ArticleIterator.
  public void testCreateArticleFiles() throws Exception {
     
    ArrayList<String> articleUrls = new ArrayList<String>();
    articleUrls.add(String.format("%sindex.php/%s/issue/view/478",
                                  BASE_URL, JOURNAL_ID)); // table of contents
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110",
                                  BASE_URL, JOURNAL_ID)); // abstract
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110/8514",
                                  BASE_URL, JOURNAL_ID)); // full-text html
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110/8601",
                                  BASE_URL, JOURNAL_ID)); // pdf
    articleUrls.add(String.format("%sindex.php/%s/article/view/8120",
                                  BASE_URL, JOURNAL_ID)); // abstract
    articleUrls.add(String.format("%sindex.php/%s/article/viewArticle/8120",
                                  BASE_URL, JOURNAL_ID)); // abstract
    articleUrls.add(String.format("%sindex.php/%s/article/view/8120/8515",
                                  BASE_URL, JOURNAL_ID)); // full-text html
    articleUrls.add(String.format("%sindex.php/%s/article/download/8120/8602",
                                  BASE_URL, JOURNAL_ID)); // pdf
    
    // Store test cases - articleUrls
    Iterator<String> itr = articleUrls.iterator();
    while (itr.hasNext()) {
      String url = itr.next();
      log.info("testCreateArticleFiles() url: " + url);
      storeTestContent(url);
    }
    
    // access OJS2ArticleItrerator
    Iterator<ArticleFiles> it = au.getArticleIterator();
    while (it.hasNext()) {
      ArticleFiles af1 = it.next();
      log.info("article file af1: " + af1.toString());
      
      // assert article 1
      String[] actualUrls1 = { af1.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
                               af1.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
                               af1.getFullTextUrl(),                                                                      
                               af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
                               af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };

      for (int i = 0;i< actualUrls1.length; i++) {
        log.info("expected url1: " + expectedUrls1[i]);
        log.info("  actual url1: " + actualUrls1[i]);
        assertEquals(expectedUrls1[i], actualUrls1[i]);
      }
      
      // assert article 2 - with frame src
      if (it.hasNext()) {
        ArticleFiles af2 = it.next();
        log.info("article file af2: " + af2.toString());

        String[] actualUrls2 = { af2.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
                                 af2.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
                                 af2.getFullTextUrl(),                                                                      
                                 af2.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
                                 af2.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };

        for (int i = 0;i< actualUrls2.length; i++) {
          log.info("expected url2: " + expectedUrls2[i]);
          log.info("  actual url2: " + actualUrls2[i]);
          assertEquals(expectedUrls2[i], actualUrls2[i]);
        }
      }
    }

  }
    
  // Response header to be stored with the simulated HTML content URLs
  // Note the X-Locks-content-type is necessary (not Content-type)
  // in order to be found by the LOCKSS Daemon. 
  private CIProperties getHtmlProperties() {
    CIProperties htmlProps = new CIProperties();
    htmlProps.put("RESPONSE","HTTP/1.0 200 OK");
    htmlProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
    htmlProps.put("Server", "Apache/2.2.3 (CentOS)");
    htmlProps.put("X-Powered-By", "PHP/5.2.17");
    htmlProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    htmlProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
    htmlProps.put("Pragma", "no-cache");
    htmlProps.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    htmlProps.put("X-Cache", "MISS from lockss.org");
    htmlProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    htmlProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    htmlProps.put("Connection", "close");
    return htmlProps;
  }
  
  // Response header to be stored with the simulated PDF content URLs
  // Note the X-Locks-content-type is necessary (not Content-type)
  // in order to be found by the LOCKSS Daemon. 
  private CIProperties getPdfProperties() {
    CIProperties pdfProps = new CIProperties();
    pdfProps.put("RESPONSE","HTTP/1.0 200 OK");
    pdfProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
    pdfProps.put("Server", "Apache/2.2.3 (CentOS)");
    pdfProps.put("X-Powered-By", "PHP/5.2.17");
    pdfProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    pdfProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
    pdfProps.put("Pragma", "no-cache");
    pdfProps.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    pdfProps.put("X-Cache", "MISS from lockss.org");
    pdfProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    pdfProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    pdfProps.put("Connection", "close");
    return pdfProps;
  }

  // Read the test TOC file test_toc_junit.html from current directory
  // Prepare input stream for UrlCacher storeContent() method
  private InputStream getTestTocInputStream() throws IOException {
    InputStream htmlIn = getResourceAsStream("test_toc_junit.html");
    String absHtml = StringUtil.fromInputStream(htmlIn);
    return IOUtils.toInputStream(absHtml);
  }
   
}
