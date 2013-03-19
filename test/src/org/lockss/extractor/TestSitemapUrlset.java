/**
 * $Id: TestSitemapUrlset.java,v 1.1 2013-03-19 18:54:34 ldoan Exp $
 */

/**

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

package org.lockss.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.lockss.extractor.Sitemap;
import org.lockss.extractor.SitemapParser;
import org.lockss.extractor.SitemapUrl;
import org.lockss.extractor.SitemapUrl.ChangeFrequency;
import org.lockss.test.LockssTestCase;
import org.lockss.uiapi.util.DateFormatter;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Test sitemap urlset structure
 * which follows sitemaps.org's sitemap protocol.
 */
public class TestSitemapUrlset extends LockssTestCase {
  
  protected static Logger log =
      Logger.getLogger(TestSitemapUrlset.class);

  private static final String INPUT_SITEMAP_URLSET_FILE_NAME = "test_sitemap_urlset.xml";
  
  /** 
   * This empty method needed here since without it, the test continues
   * and get error instead of ending.
   */
  public void testEmptyFileReturnsNoLinks() throws Exception {
  }
  
  /**
   * Read input test_sitemap.xml file. 
   */
  public void testSitemapUrlsetParser() throws Exception {
    
    InputStream xmlSitemapStream = getTestInputStream("");
    
    SitemapParser parser = new SitemapParser();
    
    String encoding = "UTF-8";
    Sitemap sitemapObj = parser.processXmlSitemap(xmlSitemapStream, encoding);
    
    assertNotNull(sitemapObj);  
        
    Collection<SitemapUrl> sitemapUrlMap = sitemapObj.getUrlMap();
    
    /** Just test the first node
     *    <url>
            <loc>http://www.example.com/catalog?item=12&amp;desc=vacation_hawaii</loc>
            <lastmod>2005-02-02</lastmod>
            <changefreq>weekly</changefreq>
            <priority>0.5</priority>
         </url>
     */

    Iterator<SitemapUrl> it = sitemapUrlMap.iterator();
    SitemapUrl sitemapUrlObj = it.next();
    
    assertNotNull(sitemapUrlObj);
    assertEquals("http://www.example.com/catalog?item=12&desc=vacation_hawaii",
                  sitemapUrlObj.getUrl().toString());
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date lastmodDate = sdf.parse(sitemapUrlObj.getLastModified());
    assertEquals("2005-02-02", DateFormatter.w3cShortFormat(lastmodDate));
    
    ChangeFrequency weekly = sitemapUrlObj.getChangeFrequency();
    assertEquals("weekly", weekly.name().toLowerCase());
    
    assertEquals(0.5, sitemapUrlObj.getPriority());
  
  } /** end testSitemapUrlsetParser */
   
  /**
   * Read the test sitemap file test_sitemap.xmlfrom current directory
   * Prepare input stream for extractUrls().
   */
  private InputStream getTestInputStream(String url) throws IOException {
  
    InputStream xmlIn = getClass().getResourceAsStream(INPUT_SITEMAP_URLSET_FILE_NAME);
    String xmlSitemap = StringUtil.fromInputStream(xmlIn);
  
    return IOUtils.toInputStream(xmlSitemap);
        
  } /** end getTestInputStream */
    
} /** end TestSitemapUrlSet */
