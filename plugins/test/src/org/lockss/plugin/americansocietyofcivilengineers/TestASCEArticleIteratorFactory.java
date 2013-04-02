/*
 * $Id: TestASCEArticleIteratorFactory.java,v 1.1 2013-04-02 21:18:45 ldoan Exp $
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

package org.lockss.plugin.americansocietyofcivilengineers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.UrlCacher;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.StringInputStream;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/*
 * Stores sample article urls with different
 * format: abstract, full text html, references html and full text pdf.  The input sample toc file
 * is called test_asce_issue_toc.html. It then invokes ACSEArticleIterator which
 * will process the toc file.
 * 
 * Issue table of content:
 * http://ascelibrary.org/toc/jaeied/15/1
 * 
 * Articles:
 * Abstract -        http://ascelibrary.org/doi/abs/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29
 * Full-text HTML -  http://ascelibrary.org/doi/full/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29
 * References HTML - http://ascelibrary.org/doi/ref/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29
 * Full-text PDF -   http://ascelibrary.org/doi/pdf/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29
 */

public class TestASCEArticleIteratorFactory extends ArticleIteratorTestCase {
  
  static Logger log = Logger.getLogger(TestASCEArticleIteratorFactory.class);

  private final String PLUGIN_NAME = "org.lockss.plugin.americansocietyofcivilengineers.ClockssASCEPlugin";
  private final String INPUT_TOC_FILE_NAME = "test_asce_issue_toc.html";
  private final String EXPECTED_URL_INITIAL = "http://ascelibrary.org/doi/fromString/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29";
  private final String BASE_URL = "http://ascelibrary.org/";
  private final String JOURNAL_ID = "jaeied";
  private final String JOURNAL_ISSN = "1076-0431";
  private final String VOLUME_NAME = "15";
  private final String YEAR = "2009";
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  protected String cuRole = null;
  ArticleMetadataExtractor.Emitter emitter;
  protected boolean emitDefaultIfNone = false;
  FileMetadataExtractor me = null; 
  MetadataTarget target;

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
  }
    
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return 
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, ASCEAuConfig());
  }     

  // Set configuration attributes to create plugin AU (archival unit)
  Configuration ASCEAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(JOURNAL_ID_KEY, JOURNAL_ID);
    conf.put(JOURNAL_ISSN_KEY, JOURNAL_ISSN);
    conf.put(VOLUME_NAME_KEY, VOLUME_NAME);
    conf.put(YEAR_KEY, YEAR);
    return conf;
  }
  
  // These urls are the simulated content URLs stored in the UrlCacher object.
  public void storeTestContent(String url, UrlCacher urlca) throws Exception {
    if (url.contains("toc")) { // issue table of contents
      urlca.storeContent(getTestInputStream(INPUT_TOC_FILE_NAME), getAbsProperties());
    } else {
    // only needs nodes without contents since we don't need to
    // look in the article content.
      urlca.storeContent(new StringInputStream("<html>This is it!</html>"), getAbsProperties());
    }
  }
  
  // Use Assert statements in class ArticleTester 
  public void passOrFailArticle(ArticleFiles af) {
    ArticleTester testArticleObj = new ArticleTester(af, EXPECTED_URL_INITIAL);
    // http://ascelibrary.org/doi/abs/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29
    if (testArticleObj.testAbstract("abs") != null) {
      log.info("testAbstract(\"abs\") passed");
    }
    // http://ascelibrary.org/doi/full/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29
    if (testArticleObj.testFullTextHtml("full") != null) {
      log.info("testFullTextHtml(\"full\") passed");
    } 
    // http://ascelibrary.org/doi/ref/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29
    if (testArticleObj.testRef("ref") != null) {
      log.info("testRef(\"ref\") passed");
    }
    // http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=3
    if (testArticleObj.testFullTextPdf("pdf") != null) {
      log.info("testFullTextPdf(\"pdf\") passed");
    }
  }
  
  // Access ASCEArticleIterator
  public void accessASCEArticleIterator(ArchivalUnit au)
      throws PluginException {
    Iterator<ArticleFiles> it = au.getArticleIterator();
    assertNotNull("it is null");
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info("article file af: " + af.toString());
      // Pass or fail articles
      passOrFailArticle(af);
    }
  }
 
  // Create simulated article files for testing. Store them in UrlCacher object.
  // Access ASCEArticleItrerator to process the issue table of contents
  // page. The simulated issue toc file being read in is test_ASCE_issue_toc.html
  // which can be found in the same directory as this TestASCEArticleIterator.
  public void testCreateArticleFiles() throws Exception {
    ArrayList<String> articleUrls = new ArrayList<String>();
    // issue table of contents
    articleUrls.add("http://ascelibrary.org/toc/jaeied/15/1");
    // abstract page
    articleUrls.add("http://ascelibrary.org/doi/abs/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29");
    // full-text html
    articleUrls.add("http://ascelibrary.org/doi/full/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29");
    // references
    articleUrls.add("http://ascelibrary.org/doi/ref/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29");
    // pdf
    articleUrls.add("http://ascelibrary.org/doi/pdf/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29");
    // Store test cases - articleUrls
    Iterator<String> simAfIterator = articleUrls.iterator();
    while (simAfIterator.hasNext()) {
      String url = simAfIterator.next();
      UrlCacher urlca = au.makeUrlCacher(url);
      storeTestContent(url, urlca);
    }
    accessASCEArticleIterator(au);
  }
    
  // Response header to be stored with the simulated content URLs
  // Note the X-Locks-content-type is necessary (not Content-type)
  // in order to be found by the LOCKSS Daemon. 
  private CIProperties getAbsProperties() {
    CIProperties absProps = new CIProperties();
    absProps.put("RESPONSE","HTTP/1.0 200 OK");
    absProps.put("Date", "Mon Feb 25 09:54:17 PST 2013"); // change 
    absProps.put("Server", "Apache/2.2.3 (CentOS)");
    absProps.put("X-Powered-By", "PHP/5.2.17");
    absProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    absProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
    absProps.put("Pragma", "no-cache");
    absProps.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    absProps.put("X-Cache", "MISS from lockss.org");
    absProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    absProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    absProps.put("Connection", "close");
    return absProps;
  }

  // Read the test issue toc file test_ASCE_issue_toc.html from current directory
  // Prepare input stream for UrlCacher storeContent() method.
  private InputStream getTestInputStream(String fileName) throws IOException {
    InputStream htmlIn = getClass().getResourceAsStream(fileName);
    String htmlInStr = StringUtil.fromInputStream(htmlIn);
    return IOUtils.toInputStream(htmlInStr);
  }
   
}
