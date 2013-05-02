/* $Id: TestPalgraveBookArticleIteratorFactory.java,v 1.2 2013-05-02 20:14:47 ldoan Exp $
 
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

package org.lockss.plugin.palgrave;

import java.util.regex.Pattern;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.util.ListUtil;
import java.util.Iterator;
import org.lockss.util.Constants;

/*
 * Book content page:
 * http://www.palgraveconnect.com/pc/doifinder/10.1057/9781137024497
 * 
 * Book formats:
 * PDF Landing html - http://www.palgraveconnect.com/pc/busman2013/browse/inside/9781137024497.html
 * PDF              - http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf
 * EPUB             - http://www.palgraveconnect.com/pc/busman2013/browse/inside/epub/9781137024497.epub
 * citation export  - http://www.palgraveconnect.com/pc/browse/citationExport?isbn=9781137024497&WT.cg_n=eBooks&WT.cg_s=Citation%20Export
 */

public class TestPalgraveBookArticleIteratorFactory extends ArticleIteratorTestCase {

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  
  private static final String PLUGIN_NAME = "org.lockss.plugin.palgrave.ClockssPalgraveBookPlugin";
  private static final String BASE_URL = "http://www.palgraveconnect.com/";
  private static final String BOOK_ISBN = "9781137024497";
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String BOOK_ISBN_KEY = "book_isbn";
  private static final int DEFAULT_FILESIZE = 3000;
  
  private final String EXPECTED_PDF_LANDING_PAGE = "http://www.palgraveconnect.com/pc/busman2013/browse/inside/9781137024497.html";
  private final String EXPECTED_PDF_URL = "http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf";
  private final String EXPECTED_FULL_TEXT_URL = EXPECTED_PDF_URL;

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
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, palgraveBookAuConfig());
  }

  // Set configuration attributes to create plugin AU (archival unit)
  Configuration palgraveBookAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(BOOK_ISBN_KEY, BOOK_ISBN);
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
    assertEquals(ListUtil.list(BASE_URL + "pc/"), getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    // PATTERN_TEMPLATE = "\"%spc/.+/browse/inside/(download|epub)?/[0-9]+\\.(html|pdf|epub)$\", base_url";
    assertNotMatchesRE(pat, "http://www.palgraveconnect.com/pc/busman2013/browsee/inside/download/9781137024497.pdfbad");
    assertMatchesRE(pat, "http://www.palgraveconnect.com/pc/busman2013/browse/inside/9781137024497.html");
    assertMatchesRE(pat, "http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf");
    assertMatchesRE(pat, "http://www.palgraveconnect.com/pc/busman2013/browse/inside/epub/9781137024497.epub");
  }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    // create urls to store in UrlCacher
    String[] urls = { BASE_URL + "pc/busman2013/browse/inside/9781137024497.html",
                      BASE_URL + "pc/busman2013/browse/inside/download/9781137024497.pdf" };

    // get cached url content type and properties from simulated contents
    // for UrclCacher.storeContent()
    Iterator<CachedUrlSetNode> cuIter = sau.getAuCachedUrlSet().contentHashIterator();
    CachedUrl cuPdf = null;
    CachedUrl cuHtml = null;
    CachedUrlSetNode cusn;
    while (cuIter.hasNext()) {
      cusn = cuIter.next();
      if (!cusn.hasContent()) {
        continue;
      }
      if (cusn.getType() == CachedUrlSetNode.TYPE_CACHED_URL) {
        CachedUrl cu = (CachedUrl)cusn;
        if (cuPdf == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF)) {
          //log.info("pdf contenttype: " + cu.getContentType());
          cuPdf = cu;
        } else if (cuHtml == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML)) {
          //log.info("html contenttype: " + cu.getContentType());
          cuHtml = cu;
        }
      }
    }
    // store content using cached url content type and properties
    UrlCacher uc;
    for (String url : urls) {
      uc = au.makeUrlCacher(url);
      if(url.contains("pdf")){
        uc.storeContent(cuPdf.getUnfilteredInputStream(), cuPdf.getProperties());
      } else if (url.contains("html")) {
        uc.storeContent(cuHtml.getUnfilteredInputStream(), cuHtml.getProperties());
      }
    }
        
    // get article iterator, get article files and the appropriate urls according
    // to their roles.
    String [] expectedUrls = { EXPECTED_FULL_TEXT_URL,
                               EXPECTED_PDF_URL,
                               EXPECTED_PDF_LANDING_PAGE };
    for (SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      String[] actualUrls = { af.getFullTextUrl(),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE) };
      //log.info("actualUrls: " + actualUrls.length);
      for (int i = 0;i< actualUrls.length; i++) {
        //log.info("url: " + actualUrls[i]);
        assertEquals(expectedUrls[i], actualUrls[i]);
      }   
    }
  }

}
