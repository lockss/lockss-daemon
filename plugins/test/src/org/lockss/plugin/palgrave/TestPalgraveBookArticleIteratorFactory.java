/* $Id$
 
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

import java.io.ByteArrayInputStream;
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

import java.util.HashMap;
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
 * new 1/14/15:
 * top-level (landing page) : http://www.palgraveconnect.com/pc/doifinder/10.1057/9781137283351
 *  citation:                 http://www.palgraveconnect.com/pc/browse/citationExport?doi=10.1057/9781137304094
 *  pdf (download pdf):       http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137023803
 *  epub (download epub):     http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137023803.epub
 */

public class TestPalgraveBookArticleIteratorFactory extends ArticleIteratorTestCase {

  //private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  
  private static final String PLUGIN_NAME = "org.lockss.plugin.palgrave.ClockssPalgraveBookPlugin";
  private static final String BASE_URL = "http://www.palgraveconnect.com/";
  private static final String BOOK_ISBN = "9781137024497";
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String BOOK_ISBN_KEY = "book_isbn";
  private static final int DEFAULT_FILESIZE = 3000;
  
  private final String EXPECTED_PDF_LANDING_PAGE = "http://www.palgraveconnect.com/pc/doifinder/10.1057/9781137024497";
  private final String EXPECTED_PDF_URL = "http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf";
  private final String EXPECTED_FULL_TEXT_URL = EXPECTED_PDF_URL;
  private CIProperties pdfHeader = new CIProperties();    
  private CIProperties textHeader = new CIProperties();
  private CIProperties epubHeader = new CIProperties();
  private static final String ContentString = "foo blah";
  InputStream random_content_stream;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    au = createAu();
    // set up headers for creating mock CU's of the appropriate type
    pdfHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    textHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    epubHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/epub");
    // the content in the urls doesn't really matter for the test
    random_content_stream = new ByteArrayInputStream(ContentString.getBytes(Constants.ENCODING_UTF_8));
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  // Set configuration attributes to create plugin AU (archival unit)
  Configuration palgraveBookAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(BOOK_ISBN_KEY, BOOK_ISBN);
    return conf;
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, palgraveBookAuConfig());
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "pc/"), getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
      Pattern pat = getPattern(artIter);
    // PATTERN_TEMPLATE = "\"%spc/.+/browse/inside/(download|epub)?/[0-9]+\\.(html|pdf|epub)$\", base_url";
    // NEW PATTERN_TEMPLATE = "\"%spc/doifinder/download/10.1057/([0-9]+)(\\.epub)?$\", base_url";
    assertNotMatchesRE(pat, "http://www.palgraveconnect.com/pc/busman2013/browsee/inside/download/9781137024497.pdfbad");
    assertNotMatchesRE(pat, "http://www.palgraveconnect.com/pc/doifinder/download-this/10.1057/9781137289520");
    assertNotMatchesRE(pat, "http://www.palgraveconnect.com/pc/busman2013/browse/inside/download/9781137024497.pdf");
    assertNotMatchesRE(pat, "http://www.palgraveconnect.com/pc/busman2013/browse/inside/epub/9781137024497.epub");
    // 
    assertMatchesRE(pat, "http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137024497");
    assertMatchesRE(pat, "http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137024497.epub");
    assertMatchesRE(pat, "http://www.palgraveconnect.com/pc/doifinder/download/10.1057/9781137289520");
  
  }

  public void testCreateArticleFiles() throws Exception {    
    // create urls to store in UrlCacher
    String[] au_urls = { 
        BASE_URL + "pc/doifinder/10.1057/9780123456789",
        BASE_URL + "pc/browse/citationExport?doi=10.1057/9780123456789",
        BASE_URL + "pc/doifinder/download/10.1057/9780123456789",
        BASE_URL + "pc/doifinder/download/10.1057/9780123456789.epub",

        BASE_URL + "pc/doifinder/10.1057/9781234567890",
        BASE_URL + "pc/browse/citationExport?doi=10.1057/9781234567890",
        BASE_URL + "pc/doifinder/download/10.1057/9781234567890",
        BASE_URL + "pc/doifinder/download/10.1057/9781234567890.epub",
        
        BASE_URL + "pc/doifinder/10.1057/9782345678901",
        BASE_URL + "pc/browse/citationExport?doi=10.1057/9782345678901",
        BASE_URL + "pc/doifinder/download/10.1057/9782345678901",
        BASE_URL + "pc/doifinder/download/10.1057/9782345678901.epub"
        };
/*
    // get cached url content type and properties from simulated contents
    // for UrclCacher.storeContent()
    CachedUrl cuPdf = null;
    CachedUrl cuHtml = null;
    CachedUrl cuEpub = null;
    for (CachedUrl cu : AuUtil.getCuIterable(sau)) {
      if (cuPdf == null
          && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF)) {
        log.info("pdf contenttype: " + cu.getContentType());
        cuPdf = cu;
      } else if (cuHtml == null 
          && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML)) {
        log.info("html contenttype: " + cu.getContentType());
        cuHtml = cu;
      } else if (cuEpub == null 
          && cu.getContentType().toLowerCase().startsWith("application/epub")) {
        log.info("epub contenttype: " + cu.getContentType());
        cuEpub = cu;
      }
    	if (cuPdf != null && cuHtml != null && cuEpub != null) {
    	  break;
    	}
    }
    */
    CachedUrl cu;
    // store content using cached url content type and properties
    for (String url : au_urls) {
      if(url.contains("download") && !url.endsWith(".epub")){
        storeContent(random_content_stream, pdfHeader, url);
      } else if (url.contains("download")) {    // epub 
        storeContent(random_content_stream, epubHeader, url);
      } else {
        storeContent(random_content_stream,textHeader, url);
      }
    }

    // book 9780123456789
    ArticleFiles af1 = new ArticleFiles();
    af1.setRoleString(ArticleFiles.ROLE_FULL_TEXT_PDF, BASE_URL + "pc/doifinder/download/10.1057/9780123456789");
    af1.setRoleString(ArticleFiles.ROLE_ARTICLE_METADATA, BASE_URL + "pc/browse/citationExport?doi=10.1057/9780123456789");
    af1.setRoleString(ArticleFiles.ROLE_FULL_TEXT_EPUB, BASE_URL + "pc/doifinder/download/10.1057/9780123456789.epub");
    // book 9780123456789
    ArticleFiles af2 = new ArticleFiles();
    af1.setRoleString(ArticleFiles.ROLE_FULL_TEXT_PDF, BASE_URL + "pc/doifinder/download/10.1057/9781234567890");
    af1.setRoleString(ArticleFiles.ROLE_ARTICLE_METADATA, BASE_URL + "pc/browse/citationExport?doi=10.1057/9781234567890");
    af1.setRoleString(ArticleFiles.ROLE_FULL_TEXT_EPUB, BASE_URL + "pc/doifinder/download/10.1057/9781234567890.epub");
    // book 9780123456789
    ArticleFiles af3 = new ArticleFiles();
    af1.setRoleString(ArticleFiles.ROLE_FULL_TEXT_PDF, BASE_URL + "pc/doifinder/download/10.1057/9782345678901");
    af1.setRoleString(ArticleFiles.ROLE_ARTICLE_METADATA, BASE_URL + "pc/browse/citationExport?doi=10.1057/9782345678901");
    af1.setRoleString(ArticleFiles.ROLE_FULL_TEXT_EPUB, BASE_URL + "pc/doifinder/download/10.1057/9782345678901.epub");
       
    // key the expected content to the fullTextUrl for the ArticleFiles
    HashMap<String, ArticleFiles> fullUrlToAF = new HashMap<String, ArticleFiles>();
    fullUrlToAF.put(BASE_URL + "pc/doifinder/download/10.1057/9780123456789", af1);
    fullUrlToAF.put(BASE_URL + "pc/doifinder/download/10.1057/9781234567890", af2);
    fullUrlToAF.put(BASE_URL + "pc/doifinder/download/10.1057/9782345678901", af3);

    // get article iterator, get article files and the appropriate urls according
    // to their roles.
    String [] expectedUrls = { EXPECTED_FULL_TEXT_URL,
                               EXPECTED_PDF_URL,
                               };
    for (SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      String[] actualUrls = { af.getFullTextUrl(),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF),
                              //af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE) 
                              };
      log.info("actualUrls: " + actualUrls.length);
      for (int i = 0;i< actualUrls.length; i++) {
        log.info("e_url: " + expectedUrls[i]);

        log.info("url: " + actualUrls[i]);
        //assertEquals(expectedUrls[i], actualUrls[i]);
      }   
    }
  }
}
