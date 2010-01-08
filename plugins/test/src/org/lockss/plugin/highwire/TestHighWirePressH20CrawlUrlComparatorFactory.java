/*
 * $Id: TestHighWirePressH20CrawlUrlComparatorFactory.java,v 1.2 2010-01-08 01:49:33 thib_gc Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.util.Comparator;

import org.lockss.crawler.CrawlUrl;
import org.lockss.plugin.highwire.HighWirePressH20CrawlUrlComparatorFactory.*;
import org.lockss.plugin.highwire.HighWirePressH20CrawlUrlComparatorFactory.HighWirePressH20UrlFactory.*;
import org.lockss.test.LockssTestCase;

public class TestHighWirePressH20CrawlUrlComparatorFactory extends LockssTestCase {

  public void testAssumptions() throws Exception {
    assertNegative(".".compareTo("/"));
    assertNegative("456".compareTo("789"));
  }
  
  /**
   * <p>Constructs typical H20 URLs and verifies that the components
   * were broken into and extracted correctly.</p>
   */
  public void testHighWirePressH20Url() throws Exception {
    
    /*
     * This factory adds testUrl() which simply calls makeUrl() then
     * performs assertions on the constructed HighWirePressH20Url.
     */
    class MyFactory extends HighWirePressH20UrlFactory {
      public MyFactory(String baseUrl) { super(baseUrl); }
      public void testUrl(String url,
                          HighWirePressH20UrlPriority expectedPriority,
                          String expectedVolume,
                          String expectedIssue,
                          String expectedPage,
                          String expectedFigure) {
        HighWirePressH20Url hu = makeUrl(url);
        assertEquals(expectedPriority, hu.priority);
        assertEquals(expectedVolume, hu.volume);
        assertEquals(expectedIssue, hu.issue);
        assertEquals(expectedPage, hu.page);
        assertEquals(expectedFigure, hu.figure);
      }
    }
    
    MyFactory factory = new MyFactory("http://www.example.com/");
    factory.testUrl("http://www.example.com/content/123.fake",
                    HighWirePressH20UrlPriority.VOLUME,
                    "123", null, null, null);
    factory.testUrl("http://www.example.com/content/123.0.fake",
                    HighWirePressH20UrlPriority.VOLUME,
                    "123.0", null, null, null);
    factory.testUrl("http://www.example.com/content/123/456.toc",
                    HighWirePressH20UrlPriority.ISSUE,
                    "123", "456", null, null);
    factory.testUrl("http://www.example.com/content/123/suppl.0.toc",
                    HighWirePressH20UrlPriority.ISSUE,
                    "123", "suppl.0", null, null);
    factory.testUrl("http://www.example.com/content/123/456/local/masthead.pdf",
                    HighWirePressH20UrlPriority.ISSUE,
                    "123", "456", null, null);
    factory.testUrl("http://www.example.com/content/123/456/999.abstract",
                    HighWirePressH20UrlPriority.PAGE,
                    "123", "456", "999", null);
    factory.testUrl("http://www.example.com/content/123/456/999.0.abstract",
                    HighWirePressH20UrlPriority.PAGE,
                    "123", "456", "999.0", null);
    factory.testUrl("http://www.example.com/content/123/456/999/embed/graphic-0.gif",
                    HighWirePressH20UrlPriority.PAGE,
                    "123", "456", "999", null);
    factory.testUrl("http://www.example.com/content/123/456/999/suppl/DC0",
                    HighWirePressH20UrlPriority.PAGE,
                    "123", "456", "999", null);
    factory.testUrl("http://www.example.com/content/123/456/999/F1.expansion.html",
                    HighWirePressH20UrlPriority.FIGURE,
                    "123", "456", "999", "f1");
    factory.testUrl("http://www.example.com/content/123/456/999/F1.0.expansion.html",
                    HighWirePressH20UrlPriority.FIGURE,
                    "123", "456", "999", "f1.0");
    factory.testUrl("http://www.example.com/powerpoint/123/456/999/F1",
                    HighWirePressH20UrlPriority.FIGURE,
                    "123", "456", "999", "f1");
    factory.testUrl("http://www.example.com/lockss-manifest/vol_123_manifest.dtl",
                    HighWirePressH20UrlPriority.HIGHEST,
                    null, null, null, null);
    factory.testUrl("http://www.lockss.org/favicon.ico",
                    HighWirePressH20UrlPriority.HIGHEST,
                    null, null, null, null);
  }
  
  public void testHighWirePressH20CrawlUrlComparator() throws Exception {
    
    class MyCrawlUrl implements CrawlUrl {
      protected String url;
      public MyCrawlUrl(String url) { this.url = url; }
      public String getUrl() { return url; }
      public int getDepth() { return 0; }
    }
    
    class MyComparator implements Comparator<CrawlUrl> {
      protected Comparator<CrawlUrl> comparator;
      public MyComparator(Comparator<CrawlUrl> comparator) { this.comparator = comparator; }
      public int compare(CrawlUrl o1, CrawlUrl o2) { return comparator.compare(o1, o2); }
      public int compare(String url1, String url2) { return compare(new MyCrawlUrl(url1), new MyCrawlUrl(url2)); }
      public void doTestPair(String url1, String url2) {
        assertNegative(compare(url1, url2));
        assertPositive(compare(url2, url1));
      }
    }
    
    MyComparator cmp = new MyComparator(new HighWirePressH20CrawlUrlComparatorFactory().createCrawlUrlComparator("http://www.example.com/"));
    
    cmp.doTestPair("http://www.lockss.org/favicon.ico",
                   "http://www.lockss.org/robots.txt");
    cmp.doTestPair("http://www.lockss.org/favicon.ico",
                   "http://www.example.com/content/123.fake");
    cmp.doTestPair("http://www.lockss.org/favicon.ico",
                   "http://www.example.com/content/123/456.toc");
    cmp.doTestPair("http://www.lockss.org/favicon.ico",
                   "http://www.example.com/content/123/456/999.abstract");
    cmp.doTestPair("http://www.lockss.org/favicon.ico",
                   "http://www.example.com/content/123/456/999/F1.expansion.html");
    cmp.doTestPair("http://www.example.com/content/123.xxx",
                   "http://www.example.com/content/123.yyy");
    cmp.doTestPair("http://www.example.com/content/123.fake",
                   "http://www.example.com/content/124.fake");
    cmp.doTestPair("http://www.example.com/content/123/456.toc",
                   "http://www.example.com/content/124.fake");
    cmp.doTestPair("http://www.example.com/content/123/456/999.abstract",
                   "http://www.example.com/content/124.fake");
    cmp.doTestPair("http://www.example.com/content/123/456/999/F1.expansion.html",
                   "http://www.example.com/content/124.fake");
    cmp.doTestPair("http://www.example.com/content/123/456.toc",
                   "http://www.example.com/content/123/456.toc.pdf");
    cmp.doTestPair("http://www.example.com/content/123/456.toc",
                   "http://www.example.com/content/123/457.toc");
    cmp.doTestPair("http://www.example.com/content/123/456/999.abstract",
                   "http://www.example.com/content/123/457.toc");
    cmp.doTestPair("http://www.example.com/content/123/456/999/F1.expansion.html",
                   "http://www.example.com/content/123/457.toc");
    cmp.doTestPair("http://www.example.com/content/123/456/999.abstract",
                   "http://www.example.com/content/123/456/999.full");
    cmp.doTestPair("http://www.example.com/content/123/456/998.abstract",
                   "http://www.example.com/content/123/456/999.abstract");
    cmp.doTestPair("http://www.example.com/content/123/456/998/F1.expansion.html",
                   "http://www.example.com/content/123/456/999.abstract");
    cmp.doTestPair("http://www.example.com/content/123/456/999/F1.expansion.html",
                   "http://www.example.com/content/123/456/999/F1.large.jpg");
    cmp.doTestPair("http://www.example.com/content/123/456/999/F1.expansion.html",
                   "http://www.example.com/content/123/456/999/F2.expansion.html");
  }
  
}
