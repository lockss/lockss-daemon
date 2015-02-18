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

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.extractor.Sitemap.SitemapType;

/**
 * This is the test class for org.lockss.extrator.SitemapUrl
 */
public class TestSitemap extends LockssTestCase {

  Sitemap sitemap;
  SitemapUrl sUrl1, sUrl2;
  //ArrayList <SitemapUrl> sUrls;
  List sitemapUrls;
   
  // Data for sitemapUrl1
  private static final String url1 = "http://www.example.com/catalog?item=12&amp;desc=vacation_hawaii";
  private static final String lastmod1 = "2011-04-06T10:38:09+00:00";
    
  // Data for sitemapUrl2
  private static final String url2 = "http://www.example.com/catalog?item=12&amp;desc=vacation_vietnam";
  private static final String lastmod2 = "2011-04-06";
  
  public void setUp() throws Exception {
    super.setUp();
    sitemap = new Sitemap(SitemapType.INDEX);
    sUrl1 = new SitemapUrl(url1, lastmod1);
    sUrl2 = new SitemapUrl(url2, lastmod2);
    sitemapUrls = ListUtil.list(sUrl1, sUrl2);
  }
  
  public void testGetType() {
    assertEquals(SitemapType.INDEX, sitemap.getType());
  }
  
  public void testaddSitemapUrl() {
    sitemap.addSitemapUrl(sUrl1);
    sitemap.addSitemapUrl(sUrl2);
    assertSameElements(sitemapUrls, sitemap.getUrls());
  }

}