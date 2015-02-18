/*
 * $Id$
 */

/*

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

package org.lockss.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;

/**
 * This is the test class for org.lockss.extrator.SitemapParser
 */
public class TestSitemapParser extends LockssTestCase {
  
  InputStream xmlInStream;
  SitemapParser parser;
  SitemapUrl sUrl1, sUrl2, sUrl3, sUrl4, sUrl5; 
  SitemapUrl sUrl6, sUrl7, sUrl8, sUrl9, sUrl10;
  List<SitemapUrl> expectedSitemapIndexUrls, expectedSitemapUrlSetUrls;
  
  private static final String encoding = "UTF-8";
  
  // For sitemapindex testing
  private static final String INPUT_SITEMAP_INDEX_FILE_NAME = "test_sitemap_index.xml";
  private static final String url1 = "http://www.example.org/sitemap_2011_8_1.xml";
  private static final String url2 = "http://www.example.org/sitemap_2011_8_2.xml";
  private static final String url3 = "http://www.example.org/sitemap_2011_8_3.xml";
  private static final String url4 = "http://www.example.org/sitemap_2012_9_1.xml";
  private static final String url5 = "http://www.example.org/sitemap_2012_9_2.xml";
  private static final String url6 = "http://www.example.org/sitemap_2012_9_3.xml";
  private static final String lastmod1 = "2011-04-06T10:38:09+00:00";
  private static final String lastmod2 = "2011-10-14T13:06:04+00:00";
  private static final String lastmod3 = "2012-01-11T09:27:45+09:00";
  private static final String lastmod4 = "2012-02-28T11:25:32+00:00";
  private static final String lastmod5 = "2012-08-06T10:29:51+00:00";
  private static final String lastmod6 = "2012-12-14T15:28:51+00:00";

  // For urlset testing
  private static final String INPUT_SITEMAP_URLSET_FILE_NAME = "test_sitemap_urlset.xml";
  private static final String url7 = "http://www.example.com/";
  private static final String lastmod7 = "2005-01-01";
  private static final String changefreq7 = "monthly";
  private static final String priority7 = "0.8";
  private static final String url8 = "http://www.example.com/catalog?item=83&desc=vacation_usa";
  private static final String lastmod8 = "2004-11-23";
  private static final String url9 = "http://www.example.com/catalog?item=73&desc=vacation_new_zealand";
  private static final String lastmod9 = "2004-12-23";
  private static final String changefreq9 = "weekly";
  private static final String url10 = "http://www.example.com/catalog?item=74&desc=vacation_newfoundland";
  private static final String lastmod10 = "2004-12-23T18:00:15+00:00";
  private static final double priority10 = 0.3;

public void setUp() throws Exception {
   super.setUp();
   // Expected data for sitemapindex testing
   sUrl1 = new SitemapUrl(url1, lastmod1);
   sUrl2 = new SitemapUrl(url2, lastmod2);
   sUrl3 = new SitemapUrl(url3, lastmod3);
   sUrl4 = new SitemapUrl(url4, lastmod4);     
   sUrl5 = new SitemapUrl(url5, lastmod5);
   sUrl6 = new SitemapUrl(url6, lastmod6);
   expectedSitemapIndexUrls = ListUtil.list(sUrl1, sUrl2, sUrl3, sUrl4, sUrl5, sUrl6);
   // Expected data for urlset testing
   sUrl7 = new SitemapUrl(url7, lastmod7, changefreq7, priority7);
   sUrl8 = new SitemapUrl(url8, lastmod8);
   sUrl9 = new SitemapUrl(url9, lastmod9, changefreq9);
   sUrl10 = new SitemapUrl(url10, lastmod10, priority10);
   expectedSitemapUrlSetUrls = ListUtil.list(sUrl7, sUrl8, sUrl9, sUrl10);
}
  
  // Read input test_sitemap_index.xml file. 
  public void testSitemapIndexParser() throws Exception {
    xmlInStream = getTestInputStream(INPUT_SITEMAP_INDEX_FILE_NAME);
    SitemapParser parser = new SitemapParser();
    Sitemap sitemapObj = parser.processXmlSitemap(xmlInStream, encoding);
    assertNotNull(sitemapObj);
    log.info("expected: class=" + expectedSitemapIndexUrls.getClass() + ": "+ expectedSitemapIndexUrls.toString());
    log.info("actual: class=" + sitemapObj.getUrls().getClass() + ": " + sitemapObj.getUrls().toString());
    assertSameElements(expectedSitemapIndexUrls, sitemapObj.getUrls());
  }
  
  // Read input test_sitemap_urlset.xml file. 
  public void testSitemapUrlSetParser() throws Exception {
    xmlInStream = getTestInputStream(INPUT_SITEMAP_URLSET_FILE_NAME);
    SitemapParser parser = new SitemapParser();
    Sitemap sitemapObj = parser.processXmlSitemap(xmlInStream, encoding);
    assertNotNull(sitemapObj);
    assertSameElements(expectedSitemapUrlSetUrls, sitemapObj.getUrls());
  }
  
  // Read the test sitemap file test_sitemap.xmlfrom current directory
  private InputStream getTestInputStream(String fileName) throws IOException {
    InputStream xmlIn = getResourceAsStream(fileName);
    String xmlSitemap = StringUtil.fromInputStream(xmlIn);
    return IOUtils.toInputStream(xmlSitemap);
  }

}