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
import org.lockss.plugin.UrlData;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.util.CIProperties;
import org.lockss.util.ListUtil;

/*
 * Full-text PDF:
 *      <base_url>/<year>/<zip_file_name>.zip!/<[A-Z0-9]>/<file_name>.pdf
 *      ex: <base_url>/<year>/A/XXXX27.14.zip!/1810test_ftp.pdf
 *              
 * Full-text and metadata XML:
 *      <base_url>/<year>/<zip_file_name>.zip!/<[A-Z0-9]>/<file_name>.xml
 *      ex: <base_url>/<year>/A/XXXX27.14.zip!/1810test_ftp.wml.xml
 *              
 * Abstract:  <base_url>/<year>/A/1803_hdp.wml.xml
 * Pdf coupled with abstract is cover image.    
 *                      
 * Test zip file used is XXXX27.14.zip, containing 3 test artiles:
 * 1803test_ftp.pdf	1810test_ftp.pdf      j.0000-0000.2013.00000.xtest.pdf
 * 1803test_ftp.wml.xml	1810test_ftp.wml.xml  j.0000-0000.2013.00000.xtest.xml
 */
public class TestWileyArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private static final String PLUGIN_NAME =
      "org.lockss.plugin.wiley.ClockssWileySourcePlugin";
  
  // 1803test - cover image pdf, abstract xml (no <body> tag)
  private final String EXPECTED_1803test_XML_URL = 
      "http://www.example.com/2011/A/XXXX27.14.zip!/1803test_hdp.wml.xml";
  
  private final String EXPECTED_1810test_XML_URL = 
      "http://www.example.com/2011/A/XXXX27.14.zip!/1810test_ftp.wml.xml";
  
  private final String EXPECTED_J_0000_0000_2013_00000_xtest_XML_URL = 
      "http://www.example.com/2011" +
      "/A/XXXX27.14.zip!/j.0000-0000.2013.00000.xtest.wml.xml";
  
  List expected1803testUrls = ListUtil.list(EXPECTED_1803test_XML_URL);

  List expected1810testUrls = ListUtil.list(EXPECTED_1810test_XML_URL);

  List expectedj_0000_0000_2013_00000_xtestUrls = 
                ListUtil.list(EXPECTED_J_0000_0000_2013_00000_xtest_XML_URL);

  List[] expectecUrls = { expected1803testUrls,
                          expected1810testUrls,
                          expectedj_0000_0000_2013_00000_xtestUrls };
                          
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
    // "\"%s[^/]+/([A-Z0-9]/)?[^/]+\\.zip!/.*\\.xml$\",base_url";
    assertNotMatchesRE(pat,
        "http://www.example.com/2011/A/XXXX27.14.zip!/1810test_ftp..wml.xmlbad");
    assertMatchesRE(pat,
        "http://www.example.com/2011/A/XXXX27.14.zip!/1810test_ftp..wml.xml");
    assertMatchesRE(pat,
        "http://www.example.com/2011/1/XXXX27.14.zip!/1810test_ftp..wml.xml");
    assertMatchesRE(pat,
        "http://www.example.com/2018_2/XXXX27.14.zip!/1810test_ftp..wml.xml");
    assertMatchesRE(pat,
        "http://www.example.com/2018_q/XXXX27.14.zip!/1810test_ftp..wml.xml");
  }
  
  public void testCreateArticleFiles() throws Exception {
    String zipUrl = "http://www.example.com/2011/A/XXXX27.14.zip";
    CachedUrl zipCu = au.makeCachedUrl(zipUrl);
    assertNotNull(zipCu);
    
    // make url cacher, then store content of the zip file as inputstream
    
    UrlData ud = new UrlData(
        getResourceAsStream("XXXX27.14.zip"), getZipHeader(), zipUrl);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
    int i = 0;
    for (SubTreeArticleIterator artIter = 
                          createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      List actualUrls = ListUtil.list(
                            af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA));
      // the order of expected articles: 1803test, 1810test, 
      // and j_0000_0000_2013_00000_xtest
      assertSameElements(expectecUrls[i++], actualUrls);
    }
    
  }
  
  private CIProperties getZipHeader() {
    CIProperties zipProps = new CIProperties();
    zipProps.put("RESPONSE","HTTP/1.0 200 OK");
    zipProps.put("Date", "Thu, 29 Aug 2013 09:22:49 GMT");
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
