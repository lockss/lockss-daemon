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

package org.lockss.plugin.medknow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.LinkExtractorTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/*
 * Medknow sitemap structure is Sitemap Index, following Google Sitemap
 * which follows sitemaps.org's sitemap protocol.
 */
public class TestMedknowSitemapLinkExtractorFactory
  extends LinkExtractorTestCase {
  
  private static Logger log =
      Logger.getLogger(TestMedknowSitemapLinkExtractorFactory.class);

  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.medknow.MedknowPlugin";

  private static final String INPUT_SITEMAP_FILE_NAME = "test_sitemap_index.xml";
  
  private ArchivalUnit mkau;
  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    mkau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, medknowAuConfig());
  }
  
  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  // Set AU config for Medknow
  Configuration medknowAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.org/");
    conf.put("journal_issn", "0189-6725");
    conf.put("year", "2012");
    conf.put("volume_name", "9");
    return conf;
  }

  // Definition for the abstract method from parent class LinkExtractorTestCase
  public String getMimeType() {
    return Constants.MIME_TYPE_XML;
  }

  // Definition for the abstract method from parent class LinkExtractorTestCase
  public LinkExtractorFactory getFactory() {
    return new MedknowSitemapLinkExtractorFactory();
  }
  
  // Read input test_sitemap.xml file. Create callback function,
  // then call MedknowSitemapLinkExtractor.extractUrls().
  public void testSitemap() throws Exception {
    // Create MedknowSitemapLinkExtractor object
    LinkExtractor mkExtractor = 
        getFactory().createLinkExtractor(getMimeType());
    assertNotNull(mkExtractor);    
    log.debug3("Medknow Link Extractor: " + mkExtractor.toString());
    // callback function add found url to array
    final ArrayList<String> urls = new ArrayList<String>();

    LinkExtractor.Callback cb = new LinkExtractor.Callback() {
		                  public void foundLink(String url) {
		                    urls.add(url);
		                  }
		                };

		                assertNotNull(cb);  
    String srcUrl = ""; // field not used, but required
    InputStream xmlStream = getTestInputStream(""); // argument not used but required
    mkExtractor.extractUrls(mkau, xmlStream,
                            Constants.ENCODING_UTF_8, srcUrl, cb);
    // Expected 6 urls for 3 journal issues, 3 for before transformed urls (xml)
    // 3 for after (showBackIssue)
    assertEquals(urls.size(), 6);
    assertEquals("http://www.example.org/sitemap_2012_9_1.xml", urls.get(4));
    assertEquals("http://www.example.org/sitemap_2012_9_2.xml", urls.get(2));
    assertEquals("http://www.example.org/sitemap_2012_9_3.xml", urls.get(0));
    assertEquals("http://www.example.org/showBackIssue.asp?issn=0189-6725;year=2012;volume=9;issue=1", urls.get(5));
    assertEquals("http://www.example.org/showBackIssue.asp?issn=0189-6725;year=2012;volume=9;issue=2", urls.get(3));
    assertEquals("http://www.example.org/showBackIssue.asp?issn=0189-6725;year=2012;volume=9;issue=3", urls.get(1));
  }
    
  // Read the test sitemap file test_sitemap.xmlfrom current directory
  // Prepare input stream for extractUrls()
  private InputStream getTestInputStream(String url) throws IOException {
    InputStream xmlIn = getResourceAsStream(INPUT_SITEMAP_FILE_NAME);
    String xmlSitemap = StringUtil.fromInputStream(xmlIn);
    return IOUtils.toInputStream(xmlSitemap);
  }
    
}
