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

package org.lockss.plugin.wiley;

import java.util.List;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.UrlCacher;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.CIProperties;
import org.lockss.util.ListUtil;

/*
 * Full-text and metadata XML:
 *      "<base_url>/<year>/<zip_file_name>.zip!/<[A-Z0-9]>/<file_name>.xml"
 *
 *      "http://clockss-ingest.lockss.org/sourcefiles
 *                      /wiley-dev/2011/A/XXXX27.14.zip!/1810test_ftp.wml.xml"
 * 
 * PDF:
 *      "<base_url>/<year>/<zip_file_name>.zip!/<[A-Z0-9]>/<file_name>.pdf
 *
 *      "http://clockss-ingest.lockss.org/sourcefiles
 *                      /wiley-dev/2011/A/XXXX27.14.Zip!/1810test_ftp.pdf"
 *                      
 * Test zip file used is XXXX27.14.zip, containing 3 test artiles:
 * 1803test_ftp.pdf	1810test_ftp.pdf      j.1467-6435.2009.00438.xtest.pdf
 * 1803test_ftp.wml.xml	1810test_ftp.wml.xml  j.1467-6435.2009.00438.xtest.xml
 */
public class TestWileyArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private static final String PLUGIN_NAME =
      "org.lockss.plugin.wiley.ClockssWileySourcePlugin";
  
  //private static final int DEFAULT_FILESIZE = 3000;
  
  // xml file name with 'hdp' - a xml with no <body>
  private final String EXPECTED_1803test_PDF_URL = 
      "http://www.example.com/2011/A/XXXX27.14.zip!/1803test_ftp.pdf";
  private final String EXPECTED_1803test_XML_URL = 
      "http://www.example.com/2011/A/XXXX27.14.zip!/1803test_hdp.wml.xml";
  private final String EXPECTED_1803test_ABSTRACT_URL = 
                                            EXPECTED_1803test_XML_URL;
  
  private final String EXPECTED_1810test_PDF_URL = 
      "http://www.example.com/2011/A/XXXX27.14.zip!/1810test_ftp.pdf";
  private final String EXPECTED_1810test_XML_URL = 
      "http://www.example.com/2011/A/XXXX27.14.zip!/1810test_ftp.wml.xml";
  
  private final String EXPECTED_J_1467_6435_2009_00438_xtest_PDF_URL = 
      "http://www.example.com/2011" +
      "/A/XXXX27.14.zip!/j.1467-6435.2009.00438.xtest.pdf";
  private final String EXPECTED_J_1467_6435_2009_00438_xtest_XML_URL = 
      "http://www.example.com/2011" +
      "/A/XXXX27.14.zip!/j.1467-6435.2009.00438.xtest.wml.xml";
  
  // 1803 - cover image pdf, abstract xml (no <body> tag)
  List expected1803testUrls = ListUtil.list(null,
                                            EXPECTED_1803test_PDF_URL,
                                            EXPECTED_1803test_ABSTRACT_URL,
                                            EXPECTED_1803test_XML_URL);

  List expected1810testUrls = ListUtil.list(EXPECTED_1810test_PDF_URL,
                                            null,
                                            null,
                                            EXPECTED_1810test_XML_URL);

  List expectedj_1467_6435_2009_00438_xtestUrls = 
                ListUtil.list(EXPECTED_J_1467_6435_2009_00438_xtest_PDF_URL,
                              null,
                              null,
                              EXPECTED_J_1467_6435_2009_00438_xtest_XML_URL);

  List[] expectecUrls = { expected1803testUrls,
                          expected1810testUrls,
                          expectedj_1467_6435_2009_00438_xtestUrls };
                          
  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
  }
  
  private ArchivalUnit createAu() 
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, wileySourceAuConfig());
  }

  private Configuration wileySourceAuConfig() {
    return ConfigurationUtil.fromArgs("base_url",
                                      "http://www.example.com/",
                                      "year", "2011");
  }
  
  // no need to test roots since all content falls under <base_url>/<year>
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // PATTERN_TEMPLATE = 
    // "\"%s%d/[A-Z0-9]/[^/]+\\.zip!/.*\\.pdf$\",base_url,year";
       
    assertNotMatchesRE(pat,
        "http://www.example.com/2011/A/XXXX27.14.zip!/1810test_ftp.pdfbad");
    assertMatchesRE(pat,
        "http://www.example.com/2011/A/XXXX27.14.zip!/1810test_ftp.pdf");
  }
  
  public void testCreateArticleFiles() throws Exception {
    String zipUrl = "http://www.example.com/2011/A/XXXX27.14.zip";
    CachedUrl zipCu = au.makeCachedUrl(zipUrl);
    assertNotNull(zipCu);
    
    // make url cacher, then store content of the zip file as inputstream
    UrlCacher uc = au.makeUrlCacher(zipUrl);
    uc.storeContent(getResourceAsStream("XXXX27.14.zip"), getZipHeader());
    
    int i = 0;
    for (SubTreeArticleIterator artIter = 
                          createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      List actualUrls = ListUtil.list(
                            //af.getFullTextUrl(),
                            af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF),
                            af.getRoleUrl("Cover image"),
                            af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
                            af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA));
      // the order of expected articles: 1803test, 1810test, 
      // and j_1467_6435_2009_00438_xtest
      assertSameElements(expectecUrls[i++], actualUrls);
    }
    
  }
  
  private CIProperties getZipHeader() {
    CIProperties zipProps = new CIProperties();
    zipProps.put("RESPONSE","HTTP/1.0 200 OK");
    zipProps.put("Date", "Aug, 29 2013 09:22:49 GMT");
    zipProps.put("Server", "Apache/2.2.3 (CentOS)");
    zipProps.put("X-Powered-By", "PHP/5.2.17");
    zipProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    zipProps.put("Cache-Control", "no-store, no-cache, must-revalidate, " +
    		 "post-check=0, pre-check=0");
    zipProps.put("Pragma", "no-cache");
    zipProps.put("X-Lockss-content-type", "application/octet-stream");
    zipProps.put("X-Cache", "MISS from lockss.org");
    zipProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    zipProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    zipProps.put("Connection", "close");
    return zipProps;
  }
  
}