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

package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.util.CIProperties;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;

import java.util.Iterator;

import org.lockss.util.Constants;

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

  private SimulatedArchivalUnit sau;    // Simulated AU to generate content

  private final String PLUGIN_NAME = "org.lockss.plugin.atypon.americansocietyofcivilengineers.ClockssASCEPlugin";
  private final String BASE_URL = "http://ascelibrary.org/";
  private final String JOURNAL_ID = "jaeied";
  private final String VOLUME_NAME = "15";
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  private static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static final int DEFAULT_FILESIZE = 3000;

  private final String EXPECTED_ABS_URL = "http://ascelibrary.org/doi/abs/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29";
  private final String EXPECTED_FULL_URL = "http://ascelibrary.org/doi/full/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29";
  private final String EXPECTED_REF_URL = "http://ascelibrary.org/doi/ref/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29";
  private final String EXPECTED_PDF_URL = "http://ascelibrary.org/doi/pdf/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29";
  private final String EXPECTED_FULL_TEXT_URL = EXPECTED_FULL_URL;

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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, ASCEAuConfig());
  }     

  // Set configuration attributes to create plugin AU (archival unit)
  Configuration ASCEAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(JOURNAL_ID_KEY, JOURNAL_ID);
    conf.put(VOLUME_NAME_KEY, VOLUME_NAME);
    return conf;
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
            | SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "doi/"), getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    // PATTERN_TEMPLATE = "\"^%sdoi/(full|pdf|pdfplus)/[.0-9]+/\", base_url";

    // we match to doi/(full|pdf|pdfplus)
    assertNotMatchesRE(pat, "http://ascelibrary.org/doi/abs/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29"); 
    assertMatchesRE(pat, "http://ascelibrary.org/doi/full/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29");
    assertMatchesRE(pat, "http://ascelibrary.org/doi/pdf/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29");
    // but not to doi/(ref|suppl| which are supporting only
    assertNotMatchesRE(pat, "http://ascelibrary.org/doi/ref/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29");
    // must have both parts of DOI
    assertNotMatchesRE(pat, "http://ascelibrary.org/doi/abs/10.1137");
    // prefix of DOI doesn't support letters though that is technically legal
    assertNotMatchesRE(pat, "http://ascelibrary.org/doi/abs/10.1ABCD/12345");
    // wrong base url
    assertNotMatchesRE(pat, "http://ametsoc.org/doi/abs/10.1175/2009WCAS1006.1");
  }
  
  // Create simulated article files for testing. Store them in UrlCacher object.
  // Access ASCEArticleItrerator to process the issue table of contents
  // page. The simulated issue toc file being read in is test_ASCE_issue_toc.html
  // which can be found in the same directory as this TestASCEArticleIterator.
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    // create urls to store in UrlCacher
    String[] urls = { BASE_URL + "doi/abs/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29",
                      BASE_URL + "doi/full/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29",
                      BASE_URL + "doi/ref/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29",
                      BASE_URL + "doi/pdf/10.1061/%28ASCE%291076-0431%282009%2915%3A1%2814%29" };

    // get cached url content type and properties from simulated contents
    // for UrclCacher.storeContent()
    CachedUrl cuPdf = null;
    CachedUrl cuHtml = null;
    for (CachedUrl cu : AuUtil.getCuIterable(sau)) {
        if (cuPdf == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF)) {
          //log.info("pdf contenttype: " + cu.getContentType());
          cuPdf = cu;
        } else if (cuHtml == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML)) {
          //log.info("html contenttype: " + cu.getContentType());
          cuHtml = cu;
        }
	if (cuPdf != null && cuHtml != null) {
	  break;
	}
    }
    // store content using cached url content type and properties
    UrlCacher uc;
    for (String url : urls) {
      //log.info("url: " + url);
      InputStream input = null;
      CIProperties props = null;
      if(url.contains("abs")){
        input = cuHtml.getUnfilteredInputStream();
        props = cuHtml.getProperties();
      } else if (url.contains("full")) {
        input = cuHtml.getUnfilteredInputStream();
        props = cuHtml.getProperties();
      } else if (url.contains("ref")) {
        input = cuHtml.getUnfilteredInputStream();
        props = cuHtml.getProperties();
      } else if (url.contains("pdf")) {
        input = cuPdf.getUnfilteredInputStream();
        props = cuPdf.getProperties();
      }
      UrlData ud = new UrlData(input, props, url);
      uc = au.makeUrlCacher(ud);
      uc.storeContent();
    }
        
    // get article iterator, get article files and the appropriate urls according
    // to their roles.
    String [] expectedUrls = { EXPECTED_FULL_TEXT_URL,
                               EXPECTED_ABS_URL,
                               EXPECTED_ABS_URL,
                               EXPECTED_FULL_URL,
                               EXPECTED_REF_URL,
                               EXPECTED_PDF_URL };
    for (SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      String[] actualUrls = { af.getFullTextUrl(),
                              af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
                              af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
                              af.getRoleUrl(ArticleFiles.ROLE_REFERENCES),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };
      //log.info("actualUrls: " + actualUrls.length);
      for (int i = 0;i< actualUrls.length; i++) {
        //log.info("expected url: " + expectedUrls[i]);
        //log.info("  actual url: " + actualUrls[i]);
        assertEquals(expectedUrls[i], actualUrls[i]);
      }   
    }
  }
  
}
