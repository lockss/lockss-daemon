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

/**
 * This is the test class for org.lockss.extrator.SitemapUrl
 */
public class TestSitemapUrl extends LockssTestCase {

  private static final String url = "http://www.example.com/catalog?item=12&amp;desc=vacation_hawaii";
  private static final String lastmod = "2011-04-06T10:38:09+00:00";
  private static final String changefreq = "weekly";
  private static final String priority = "0.5";
  
  private static final String lastmodToSet = "2012-12-14T15:28:51+00:00";
  
  public void setUp() throws Exception {
    super.setUp();
  }
  
  public void testConstructors() {
    try {
      new SitemapUrl(null);
      fail("Expected illegal argument exception");
    } catch (IllegalArgumentException e) {
      // fall through
    }
    assertNotNull(new SitemapUrl(url));
    assertNotNull(new SitemapUrl(url, lastmod));
    assertNotNull(new SitemapUrl(url, lastmod, changefreq, priority));
    assertNotNull(new SitemapUrl(url, lastmod, SitemapUrl.ChangeFrequency.MONTHLY, 0.7));
  }
  
  public void testGetUrl() {
    SitemapUrl sitemapUrl = new SitemapUrl(url, lastmod, changefreq, priority);
    String url = sitemapUrl.getUrl();
    assertNotNull(url);
    assertEquals("http://www.example.com/catalog?item=12&amp;desc=vacation_hawaii", url);
  }

  public void testHashCode() throws Exception {
    SitemapUrl sUrl1 = new SitemapUrl(url, lastmod, changefreq, priority);
    SitemapUrl sUrl2 = new SitemapUrl(url, lastmod, changefreq, priority);
    assertTrue(sUrl1.hashCode() == sUrl2.hashCode());
  }

  public void testEquals() {
    SitemapUrl sUrl1 = new SitemapUrl(url, lastmod, changefreq, priority);
    SitemapUrl sUrl2 = new SitemapUrl(url, lastmod, changefreq, priority);
    assertEquals(sUrl1, sUrl2);
    SitemapUrl sUrl3 = new SitemapUrl(url, lastmod, changefreq, "1.1");
    assertNotEquals(sUrl1, sUrl3);
  }
  
}