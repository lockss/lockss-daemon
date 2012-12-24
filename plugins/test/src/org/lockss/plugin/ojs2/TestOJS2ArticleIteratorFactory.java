/*
 * $Id: TestOJS2ArticleIteratorFactory.java,v 1.2 2012-12-24 00:31:56 ldoan Exp $
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

package org.lockss.plugin.ojs2;

import java.io.*;
import org.apache.commons.io.IOUtils;

import java.util.*;
import java.util.Iterator;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

/* 
 * TestOJS2ArticleIteratorFactory stores sample article URLs with different
 * format: abstract, full text HTML and full text PDF.  The input sample TOC file
 * is called text_toc_junit.html. It then invokes OJS2ArticleIterator which
 * will process the TOC file.
 * 
 * Table of content:
 * http://www.ojs2articleiteratortest.com/index.php/lq/issue/view/478
 * 
 * Articles:
 * Abstract - http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110
 * Full Text HTML - http://www.ojs2articleiteratortest.com/lq/article/view/8110/8514
 * Full Text PDF - http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110/8601
 * 
 * The label for the full text HTML is typically 'HTML', but sometimes 'Full Text'.
 * An example is http://www.ancient-asia-journal.com/issue/view/2 (Ubiquity Press).
 */

public class TestOJS2ArticleIteratorFactory extends ArticleIteratorTestCase {
  
  static Logger log = Logger.getLogger(TestOJS2ArticleIteratorFactory.class);

  private final String PLUGIN_NAME = "org.lockss.plugin.ojs2.OJS2Plugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private final String BASE_URL = "http://www.ojs2articleiteratortest.com/";
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  private final String JOURNAL_ID = "lq";
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private final String YEAR = "2012";
  
  private final String TOC_URL =
        "http://www.ojs2articleiteratortest.com/index.php/lq/issue/view/478";

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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, OJS2AuConfig());
  }     

  // Set configuration attributes to create plugin AU (archival unit)
  Configuration OJS2AuConfig() {
    
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JOURNAL_ID);
    conf.put("year", YEAR);
    return conf;
    
  } // OJS2AuConfig
  
  // These URLs are the simulated content URLs stored in the UrlCacher object.
  public void storeTestContent(String url, UrlCacher urlca) throws Exception {
    
    if (url.contains("478")) { // issue table of contents
      urlca.storeContent(getTestTocInputStream(TOC_URL), getAbsProperties());
    } else {
      // only needs nodes without contents since we don't need to
      // look in the article content.
      //urlca.storeContent(getTestTocInputStream(null), getAbsProperties());
      urlca.storeContent(new StringInputStream("<html></html>"), getAbsProperties());
    }
 
    return;
    
  } // storeTestContent
  
  
  // Use Assert statements in class ArticleTester 
  public void passOrFailArticle(ArticleFiles af) {
    
    ArticleTester testArticleObj = new ArticleTester(af);
    
    // example: http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110
    if (testArticleObj.testAbstract("8110") != null) {
      log.info("testAbstract(\"8110\") passed");
    } else {
        if (testArticleObj.testAbstract("8220") != null) {
          log.info("testAbstract(\"8220\") passed");
        }
    }
    
    // example: http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8110/8514
    if (testArticleObj.testFullTextHtml("8110/8514") != null) {
      log.info("testFullTextHtml(\"8110/8514\") passed");
    } else {
        // http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8220/8390
        if (testArticleObj.testFullTextHtml("8220/8390") != null) {
          log.info("testFullTextHtml(\"8220/8390\") passed");
        }
    }
    
    // The article iterator lists full text HTML, but not PDFs
    // so the following tests will have no results.
    if (testArticleObj.testFullTextPdf("8110/8601") != null) {
      log.info("testFullTextPdf(\"8110/8601\") passed");
    } else {
        // example: http://www.ojs2articleiteratortest.com/index.php/lq/article/view/8220/8391
        if (testArticleObj.testFullTextPdf("8220/8391") != null) {
          log.info("testFullTextPdf(\"8220/8391\") passed");
        }
    }
    
    return;
    
  } // passOrFailArticle
  
  // Access OJS2ArticleIterator
  public void accessOJS2ArticleIterator(ArchivalUnit au) {

    Iterator<ArticleFiles> it = au.getArticleIterator();
    while (it.hasNext()) {
      
      ArticleFiles af = it.next();
      log.info("article file af: " + af.toString());

      // Pass or fail articles
      passOrFailArticle(af);

    } // while

  } // accessOJS2ArticleIterator  
 
  // Create simulated article files for testing
  // Store them in UrlCacher object
  // Access OJS2ArticleItrerator to process the table of contents
  // page (TOC). The simulated TOC file being read in is test_toc_junit.html
  // which can be found in the same directory as this TestOJS2ArticleIterator.
  public void testCreateArticleFiles() throws Exception {
     
    ArrayList<String> articleUrls = new ArrayList<String>();
    articleUrls.add(String.format("%sindex.php/%s/issue/view/478", BASE_URL, JOURNAL_ID)); // table of contents
    
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110", BASE_URL, JOURNAL_ID));
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110/8514", BASE_URL, JOURNAL_ID));
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110/8601", BASE_URL, JOURNAL_ID));
    
    articleUrls.add(String.format("%sindex.php/%s/article/view/8220", BASE_URL, JOURNAL_ID));
    articleUrls.add(String.format("%sindex.php/%s/article/view/8220/8390", BASE_URL, JOURNAL_ID));
    articleUrls.add(String.format("%sindex.php/%s/article/view/8220/8391", BASE_URL, JOURNAL_ID));
    
     // Store test cases - articleUrls
    Iterator<String> itr = articleUrls.iterator();
    while (itr.hasNext()) {
      String url = itr.next();
      UrlCacher urlca = au.makeUrlCacher(url);
      storeTestContent(url, urlca);
    } // while
    
    // Access OJS2ArticleItrerator
    accessOJS2ArticleIterator(au);
       
  } // testCreateArticleFiles
    
  // Response header to be stored with the simulated content URLs
  // Note the X-Locks-content-type is necessary (not Content-type)
  // in order to be found by the LOCKSS Daemon. 
  private CIProperties getAbsProperties() {
    
    CIProperties absProps = new CIProperties();
    absProps.put("RESPONSE","HTTP/1.0 200 OK");
    absProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
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
    
  } // getAbsProperties

  // Read the test TOC file test_toc_junit.html from current directory
  // Prepare input stream for UrlCacher storeContent() method
    private InputStream getTestTocInputStream(String url) throws IOException {
  
    InputStream htmlIn = getClass().getResourceAsStream("test_toc_junit.html");
    String absHtml = StringUtil.fromInputStream(htmlIn);
        
    return IOUtils.toInputStream(absHtml);
        
  } // getTestTocInputStream
   
} // TestOJS2ArticleIteratorFactory
