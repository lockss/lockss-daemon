/**
 * $Id: TestSitemapIndex.java,v 1.1 2013-03-19 20:59:04 ldoan Exp $
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.lockss.extractor.Sitemap;
import org.lockss.extractor.SitemapParser;
import org.lockss.extractor.SitemapUrl;
import org.lockss.test.LockssTestCase;
import org.lockss.uiapi.util.DateFormatter;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Test sitemap index structure
 * which follows sitemaps.org's sitemap protocol.
 */
public class TestSitemapIndex extends LockssTestCase {
  
  protected static Logger log =
      Logger.getLogger(TestSitemapIndex.class);

  private static final String INPUT_SITEMAP_INDEX_FILE_NAME = "test_sitemap_index.xml";
  
  /** 
   * This empty method needed here since without it, the test continues
   * and get error instead of ending.
   */
  public void testEmptyFileReturnsNoLinks() throws Exception {
  }
  
  /**
   * Read input test_sitemap.xml file. 
   */
  public void testSitemapIndexParser() throws Exception {
    
    InputStream xmlSitemapStream = getTestInputStream("");
    
    SitemapParser parser = new SitemapParser();
    
    String encoding = "UTF-8";
    Sitemap sitemapObj = parser.processXmlSitemap(xmlSitemapStream, encoding);
    
    assertNotNull(sitemapObj);  
        
    Collection<SitemapUrl> sitemapUrlMap = sitemapObj.getUrlMap();
    
    /** Just test the first node
     *    <url>
            <loc>http://www.example.org/sitemap_2011_8_1.xml</loc>
            <lastmod>2011-04-06T10:38:09+00:00</lastmod>
          </url>
     */
    
    Iterator<SitemapUrl> it = sitemapUrlMap.iterator();
    int count = 0;
    while (it.hasNext()) {
      SitemapUrl sitemapUrlObj = it.next();
      assertNotNull(sitemapUrlObj);
      count++;
      
      /** test the text contents of the first node in collection*/
      if (count == 1) {
        testOneNodeContent(sitemapUrlObj);
      }
      
    }
    
    /** log.info("count=" + count); */
    assertEquals(6, count); /** test has 6 nodes */
    
  } /** end testSitemapIndexParser */
  
  private void testOneNodeContent(SitemapUrl sitemapUrlObj) {
    
    assertEquals("http://www.example.org/sitemap_2011_8_3.xml",
                  sitemapUrlObj.getUrl().toString());
      
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+hh:00");
    Date lastmodDate = null;
    
    try {
      lastmodDate = sdf.parse(sitemapUrlObj.getLastModified());
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  
    /** log.info("date: " + DateFormatter.w3cLongFormat(lastmodDate)); */
    assertEquals("2012-01-11T09:27:45+09:00", DateFormatter.w3cLongFormat(lastmodDate));
    
  }
   
  /**
   * Read the test sitemap file test_sitemap.xmlfrom current directory
   * Prepare input stream for extractUrls().
   */
  private InputStream getTestInputStream(String url) throws IOException {
  
    InputStream xmlIn = getClass().getResourceAsStream(INPUT_SITEMAP_INDEX_FILE_NAME);
    String xmlSitemap = StringUtil.fromInputStream(xmlIn);
  
    return IOUtils.toInputStream(xmlSitemap);
        
  } /** end getTestInputStream */
    
} /** end TestSitemapIndex */
