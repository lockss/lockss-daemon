/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;
import java.util.*;
import java.io.*;

import org.lockss.crawler.PermissionRecord.PermissionStatus;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class TestPermissionMap extends LockssTestCase {
  private PermissionMap pMap;
  private String permissionUrl1 = "http://www.example.com/index.html";
  private String url1 = "http://www.example.com/link1.html";
  private Collection<String> permUrls;
  private MockArchivalUnit mau;
  private MockCrawlerFacade mcf;
  
  public void setUp() throws Exception {
    super.setUp();
    getMockLockssDaemon().getAlertManager(); //populates AlertManager
    
    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    List startUrls = ListUtil.list(permissionPage1);

    MockCrawlRule crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(permissionPage1);
    permUrls = ListUtil.list(permissionPage1);
    mau.addUrl(permissionPage1);
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(permUrls);
    mau.setRefetchDepth(1);
    mau.setCrawlRule(crawlRule);
    mcf = new MockCrawler().new MockCrawlerFacade();
    mcf.setAu(mau);
    mcf.setCrawlerStatus(new MockCrawlStatus());
    mcf.setPermissionUrlFetcher(new MockUrlFetcher(mcf, permissionPage1));
  }

  void putStatus(PermissionMap map, String permissionUrl, PermissionStatus status)
        throws java.net.MalformedURLException {
    PermissionRecord rec = map.createRecord(permissionUrl);
    rec.setStatus(status);
  }

  public void testConstructorNullFacade() {
    try {
      new PermissionMap(null, null, null, null);
      fail("Should have thrown an IllegalArgumentException");
    } catch (IllegalArgumentException ex) {
      //expected
    }
  }

  private static final String permissionPage1 =
    "http://www.example.com/perm.html";

  private static final String nonPermissionPage1 =
    "http://www.example.com/noperm.html";

  public void testCheckPermissionDaemonPermissionOnly() throws Exception {
    PermissionMap map =
      new PermissionMap(mcf, ListUtil.list(new MockPermissionChecker(999)),
                        null, permUrls);
    assertTrue(map.populate());
    assertTrue(map.hasPermission("http://www.example.com/"));
    assertFalse(mcf.getCrawlerStatus().isCrawlError());
  }

  public void testCheckPermissionDaemonAndPluginPermission() throws Exception {
    PermissionMap map =
      new PermissionMap(mcf, ListUtil.list(new MockPermissionChecker(999)),
          ListUtil.list(new MockPermissionChecker(999)), permUrls);
    assertTrue(map.populate());
    assertTrue(map.hasPermission("http://www.example.com/"));
    assertFalse(mcf.getCrawlerStatus().isCrawlError());
  }

  public void testCheckPermissionDaemonPermissionRefuses() throws Exception {
    PermissionMap map =
      new PermissionMap(mcf, ListUtil.list(new MockPermissionChecker(0)),
                ListUtil.list(new MockPermissionChecker(999)), permUrls);
    assertFalse(map.populate());
    assertFalse(map.hasPermission("http://www.example.com/"));
    assertEquals("No permission statement on manifest page.",
                 mcf.getCrawlerStatus().getCrawlStatusMsg());
  }

  public void testCheckPermissionPluginPermissionRefuses() throws Exception {
    PermissionMap map =
      new PermissionMap(mcf, ListUtil.list(new MockPermissionChecker(999)),
          ListUtil.list(new MockPermissionChecker(0)), permUrls);
    assertFalse(map.populate());
    assertFalse(map.hasPermission("http://www.example.com/"));
    assertEquals("No permission statement on manifest page.",
                 mcf.getCrawlerStatus().getCrawlStatusMsg());
  }

  public void testCheckPermissionPluginCrawlWindowClosed() throws Exception {
    mau.setCrawlWindow(new MockCrawlWindow(false));

    PermissionMap map =
      new PermissionMap(mcf, ListUtil.list(new MockPermissionChecker(999)),
          ListUtil.list(new MockPermissionChecker(999)), permUrls);
    assertFalse(map.populate());
    assertFalse(map.hasPermission("http://www.example.com/"));
    assertEquals("Interrupted by crawl window",
                 mcf.getCrawlerStatus().getCrawlStatusMsg());
  }

  public void testSameHostFirstHasPermission() throws Exception {
    MockCrawlRule crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(permissionPage1);
    mau.addUrl(permissionPage1);
    mau.addUrl(nonPermissionPage1);
    mau.setStartUrls(ListUtil.list(permissionPage1));
    permUrls = ListUtil.list(permissionPage1, nonPermissionPage1);
    mau.setPermissionUrls(permUrls);
    mau.setCrawlRule(crawlRule);
    
    PermissionMap map =
      new PermissionMap(mcf, 
          ListUtil.list(new MockPermissionChecker(permissionPage1)),
          ListUtil.list(new MockPermissionChecker(999)), permUrls);
    assertTrue(map.populate());
    assertTrue(map.hasPermission("http://www.example.com/"));
    assertFalse(mcf.getCrawlerStatus().isCrawlError());
  }

  public void testSameHostSecondHasPermission() throws Exception {
    MockCrawlRule crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(permissionPage1);
    mau.addUrl(permissionPage1);
    mau.addUrl(nonPermissionPage1);
    mau.setStartUrls(ListUtil.list(permissionPage1));
    permUrls = ListUtil.list(nonPermissionPage1, permissionPage1 );
    mau.setPermissionUrls(permUrls);
    mau.setCrawlRule(crawlRule);
    mcf.setPermissionUrlFetcher(new MockUrlFetcher(mcf, nonPermissionPage1));

    
    PermissionMap map =
        new PermissionMap(mcf, 
            ListUtil.list(new MockPermissionChecker(permissionPage1)),
            ListUtil.list(new MockPermissionChecker(999)), permUrls);
    assertTrue(map.populate());
    assertTrue(map.hasPermission("http://www.example.com/"));
    assertFalse(mcf.getCrawlerStatus().isCrawlError());
  }

  public void testGetPutPermission() throws Exception {
    pMap = new PermissionMap(mcf, new ArrayList(), new ArrayList(),
        new ArrayList());
    putStatus(pMap, permissionUrl1, PermissionStatus.PERMISSION_OK);

    assertEquals(permissionUrl1, pMap.getPermissionUrl(url1));
    assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));

    String permissionUrl2 = "http://www.foo.com/index.html";
    String url2 = "http://www.foo.com/link2.html";

    putStatus(pMap, permissionUrl2, PermissionStatus.PERMISSION_NOT_OK);
    assertEquals(permissionUrl2, pMap.getPermissionUrl(url2));
    assertEquals(PermissionStatus.PERMISSION_NOT_OK, pMap.getStatus(url2));

    assertEquals(permissionUrl1, pMap.getPermissionUrl(url1));
    assertEquals(PermissionStatus.PERMISSION_OK, pMap.getStatus(url1));
  }

  public void testGloballyPermittedHost() throws Exception {
    mcf.setGloballyPermittedHosts(ListUtil.list("www.css-host.com"));
    PermissionMap map =
      new PermissionMap(mcf, ListUtil.list(new MockPermissionChecker(999)),
                        null, permUrls);
    assertTrue(map.populate());
    assertFalse(map.hasPermission("http://www.not-example.com/"));
    assertTrue(map.hasPermission("http://www.css-host.com/"));
    assertFalse(mcf.getCrawlerStatus().isCrawlError());
  }

  public void testPluginPermittedHostNot() throws Exception {
    mau.setPermittedHostPatterns(RegexpUtil.compileRegexps(ListUtil.list(".*")));
    PermissionMap map =
      new PermissionMap(mcf, ListUtil.list(new MockPermissionChecker(999)),
                        null, permUrls);
    assertTrue(map.populate());
    assertFalse(map.hasPermission("http://anything.net/"));
  }

  public void testPluginPermittedHost() throws Exception {
    mcf.setAllowedPluginPermittedHosts(ListUtil.list("foo.cdn.net",
						     "bar.cdn.net",
						     "xxx.net"));
    mau.setPermittedHostPatterns(RegexpUtil.compileRegexps(ListUtil.list(".*\\.cdn\\.net")));
    PermissionMap map =
      new PermissionMap(mcf, ListUtil.list(new MockPermissionChecker(999)),
                        null, permUrls);
    assertTrue(map.populate());
    assertTrue(map.hasPermission("http://foo.cdn.net/bar"));
    assertTrue(map.hasPermission("http://bar.cdn.net/bar"));
    assertFalse(map.hasPermission("http://baz.cdn.net/bar"));
    assertFalse(map.hasPermission("http://xxx.net/"));
  }
}
