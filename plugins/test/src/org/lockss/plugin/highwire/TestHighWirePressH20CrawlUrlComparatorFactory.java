/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlUrl;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.highwire.HighWirePressH20CrawlUrlComparatorFactory.*;
import org.lockss.plugin.highwire.HighWirePressH20CrawlUrlComparatorFactory.HighWirePressH20UrlFactory.*;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;

public class TestHighWirePressH20CrawlUrlComparatorFactory extends LockssTestCase {
  
  private final String PLUGIN_NAME = "org.lockss.plugin.highwire.ClockssHighWirePressH20Plugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.example.com/";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, "1");
  private ArchivalUnit au;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    MockLockssDaemon daemon = getMockLockssDaemon();
    PluginManager pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
    
    au = PluginTestUtil.createAndStartAu(PLUGIN_NAME,  AU_CONFIG);
  }

  public void testAssumptions() throws Exception {
    assertNegative(".".compareTo("/"));
    assertNegative("123".compareTo("124"));
    assertNegative("456".compareTo("457"));
    assertNegative("998".compareTo("999"));
    assertNegative("f1".compareTo("f2"));
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
      public MyFactory(String baseUrl, String jcode) { super(baseUrl, jcode); }
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
    
    MyFactory factory = new MyFactory("http://www.example.com/", "jcode");
    factory.testUrl("http://www.example.com/content/123",
                    HighWirePressH20UrlPriority.VOLUME,
                    "123", null, null, null);
    factory.testUrl("http://www.example.com/content/123/",
                    HighWirePressH20UrlPriority.VOLUME,
                    "123", null, null, null);
    factory.testUrl("http://www.example.com/content/123.fake",
                    HighWirePressH20UrlPriority.VOLUME,
                    "123", null, null, null);
    factory.testUrl("http://www.example.com/content/123.0.fake",
                    HighWirePressH20UrlPriority.VOLUME,
                    "123.0", null, null, null);
    factory.testUrl("http://www.example.com/content/123/456",
                    HighWirePressH20UrlPriority.ISSUE,
                    "123", "456", null, null);
    factory.testUrl("http://www.example.com/content/123/456/",
                    HighWirePressH20UrlPriority.ISSUE,
                    "123", "456", null, null);
    factory.testUrl("http://www.example.com/content/123/456.toc",
                    HighWirePressH20UrlPriority.ISSUE,
                    "123", "456", null, null);
    factory.testUrl("http://www.example.com/content/123/suppl.0.toc",
                    HighWirePressH20UrlPriority.ISSUE,
                    "123", "suppl.0", null, null);
    factory.testUrl("http://www.example.com/content/123/456/local/masthead.pdf",
                    HighWirePressH20UrlPriority.ISSUE,
                    "123", "456", null, null);
    factory.testUrl("http://www.example.com/content/123/456/999",
                    HighWirePressH20UrlPriority.PAGE,
                    "123", "456", "999", null);
    factory.testUrl("http://www.example.com/content/123/456/999/",
                    HighWirePressH20UrlPriority.PAGE,
                    "123", "456", "999", null);
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
    factory.testUrl("http://www.example.com/powerpoint/123/456/999/F1",
                    HighWirePressH20UrlPriority.FIGURE,
                    "123", "456", "999", "f1");
    factory.testUrl("http://www.example.com/powerpoint/123/456/999/F1/",
                    HighWirePressH20UrlPriority.FIGURE,
                    "123", "456", "999", "f1");
    factory.testUrl("http://www.example.com/content/123/456/999/F1.expansion.html",
                    HighWirePressH20UrlPriority.FIGURE,
                    "123", "456", "999", "f1");
    factory.testUrl("http://www.example.com/content/123/456/999/F1.0.expansion.html",
                    HighWirePressH20UrlPriority.FIGURE,
                    "123", "456", "999", "f1.0");
    factory.testUrl("http://www.example.com/lockss-manifest/vol_123_manifest.dtl",
                    HighWirePressH20UrlPriority.HIGHEST,
                    null, null, null, null);
    factory.testUrl("http://www.lockss.org/favicon.ico",
                    HighWirePressH20UrlPriority.HIGHEST,
                    null, null, null, null);
    
    // Drupal additional tests
    factory.testUrl("http://www.example.com/lockss-manifest/vol_3_manifest.html",
        HighWirePressH20UrlPriority.HIGHEST, null, null, null, null);
    factory.testUrl("http://www.example.com/content/3/4.toc",
        HighWirePressH20UrlPriority.ISSUE, "3", "4", null, null);
    factory.testUrl("http://www.example.com/content/3/4/5",
        HighWirePressH20UrlPriority.PAGE, "3", "4", "5", null);
    factory.testUrl("http://www.example.com/content/3/4/5.abstract",
        HighWirePressH20UrlPriority.PAGE, "3", "4", "5", null);
    factory.testUrl("http://www.example.com/content/3/4/5.article-info",
        HighWirePressH20UrlPriority.PAGE, "3", "4", "5", null);
    factory.testUrl("http://www.example.com/content/3/4/5.e-letters",
        HighWirePressH20UrlPriority.PAGE, "3", "4", "5", null);
    factory.testUrl("http://www.example.com/content/3/4/5.figures-only",
        HighWirePressH20UrlPriority.PAGE, "3", "4", "5", null);
    factory.testUrl("http://www.example.com/content/3/4/5.full.pdf",
        HighWirePressH20UrlPriority.PAGE, "3", "4", "5", null);
    // these are actually pages may need to revisit later XXX
    factory.testUrl("http://www.example.com/content/jcode/3/4/5.full.pdf",
        HighWirePressH20UrlPriority.PAGE, "3", "4", "5", null);
    factory.testUrl("http://www.example.com/content/jcode/3/4/5/F6.medium.gif",
        HighWirePressH20UrlPriority.FIGURE, "3", "4", "5", "f6");
  }
  
  public void testHighWirePressH20CrawlUrlComparator() throws Exception {
    
    class MyCrawlUrl implements CrawlUrl {
      protected String url;
      public MyCrawlUrl(String url) { this.url = url; }
      @Override
      public String getUrl() { return url; }
      @Override
      public int getDepth() { return 0; }
    }
    
    class MyComparator implements Comparator<CrawlUrl> {
      protected Comparator<CrawlUrl> comparator;
      public MyComparator(Comparator<CrawlUrl> comparator) { this.comparator = comparator; }
      @Override
      public int compare(CrawlUrl o1, CrawlUrl o2) { return comparator.compare(o1, o2); }
      public int compare(String url1, String url2) { return compare(new MyCrawlUrl(url1), new MyCrawlUrl(url2)); }
    }
    
    String[] urls = new String[] {
        "http://www.lockss.org/favicon.ico",
        "http://www.lockss.org/robots.txt",
        "http://www.example.com/content/123/456/998",
        "http://www.example.com/content/123/456/998.abstract",
        "http://www.example.com/content/123/456/999/F1.expansion.html",
        "http://www.example.com/content/123/456/999/F1.large.jpg",
        "http://www.example.com/content/123/456/999/F2.expansion.html",
        "http://www.example.com/content/123/456/999.abstract",
        "http://www.example.com/content/123/456/999.full",
        "http://www.example.com/content/123/456.toc",
        "http://www.example.com/content/123/456.toc.pdf",
        "http://www.example.com/content/123/457.toc",
        "http://www.example.com/content/123.fake",
        "http://www.example.com/content/124.fake",
    };
    
    MyComparator cmp = new MyComparator(new HighWirePressH20CrawlUrlComparatorFactory().createCrawlUrlComparator(au));
    for (int i = 0 ; i < urls.length ; ++i) {
      for (int j = 0 ; j < urls.length ; ++j) {
        int x = cmp.compare(urls[i], urls[j]);
        String msg = "i = " + i + ", j = " + j + ", x = " + x;
        if (i < j) {
          assertNegative(msg, x);
        }
        else if (i > j) {
          assertPositive(msg, x);
        }
        else {
          assertEquals(msg, 0, x);
        }
      }
    }
  }
  
}
