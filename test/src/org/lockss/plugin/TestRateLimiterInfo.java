/*
 * $Id$
 */

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

package org.lockss.plugin;

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.test.*;

public class TestRateLimiterInfo extends LockssTestCase {

  public void testDefault() {
    RateLimiterInfo rli = new RateLimiterInfo("foo", 12000);
    assertEquals("1/12000", rli.getDefaultRate());
    assertEquals("foo", rli.getCrawlPoolKey());
    assertNull(rli.getMimeRates());
    assertNull(rli.getUrlRates());
  }

  public void testMime() {
    RateLimiterInfo rli = new RateLimiterInfo("bar", 13000);
    Map map = MapUtil.map("text/html", "1/2s", "image/*", "1/1");
    rli.setMimeRates(map);
    assertEquals("1/13000", rli.getDefaultRate());
    assertEquals("bar", rli.getCrawlPoolKey());
    assertSame(map, rli.getMimeRates());
    assertNull(rli.getUrlRates());
    assertNull(rli.getCond());
  }

  public void testUrl() {
    RateLimiterInfo rli = new RateLimiterInfo("bar", 13000);
    Map map = MapUtil.map(".*\\.pdf", "1/2s", ".*/images/.*", "1/1");
    rli.setUrlRates(map);
    assertEquals("1/13000", rli.getDefaultRate());
    assertEquals("bar", rli.getCrawlPoolKey());
    assertSame(map, rli.getUrlRates());
    assertNull(rli.getMimeRates());
    assertNull(rli.getCond());
  }

  public void testCond() {
    RateLimiterInfo rli = new RateLimiterInfo("bar", 13000);
    CrawlWindow win1 = new CrawlWindows.Daily("1:00", "3:00", "GMT");
    CrawlWindow win2 = new CrawlWindows.Daily("3:00", "1:00", "GMT");
    RateLimiterInfo rli1 = new RateLimiterInfo("one", "2/300");
    RateLimiterInfo rli2 = new RateLimiterInfo("two", "4/500");
    LinkedHashMap map = new LinkedHashMap();
    map.put(win1, rli1);
    map.put(win2, rli2);
    rli.setCond(map);
    assertEquals("1/13000", rli.getDefaultRate());
    assertEquals("bar", rli.getCrawlPoolKey());
    assertNull(rli.getUrlRates());
    assertNull(rli.getMimeRates());
    Map cond = rli.getCond();
    // the set views returned by LinkedHashMap don't appear to be ordered
    // (as far as CollectionUtil.isOrdered() can tell); only the iterator
    // is ordered.  Make a copy to invoke the iterator.
    assertIsomorphic(ListUtil.list(win1, win2), new ArrayList(cond.keySet()));
    assertIsomorphic(ListUtil.list(rli1, rli2), new ArrayList(cond.values()));
  }

  String condser = 
    "<org.lockss.plugin.RateLimiterInfo>" +
    "  <crawlPoolKey>poolKey</crawlPoolKey>" +
    "  <cond>" +
    "    <entry>" +
    "      <org.lockss.daemon.CrawlWindows-Never/>" +
    "      <org.lockss.plugin.RateLimiterInfo>" +
    "        <rate>1/4s</rate>" +
    "        <mimeRates>" +
    "          <entry>" +
    "            <string>text/html</string>" +
    "            <string>10/1m</string>" +
    "          </entry>" +
    "          <entry>" +
    "            <string>image/*</string>" +
    "            <string>5/1s</string>" +
    "          </entry>" +
    "        </mimeRates>" +
    "      </org.lockss.plugin.RateLimiterInfo>" +
    "    </entry>" +
    "    <entry>" +
    "      <org.lockss.daemon.CrawlWindows-Daily>" +
    "        <from>22:00</from>" +
    "        <to>23:00</to>" +
    "        <timeZoneId>America/Los_Angeles</timeZoneId>" +
    "      </org.lockss.daemon.CrawlWindows-Daily>" +
    "      <org.lockss.plugin.RateLimiterInfo>" +
    "        <rate>1/12s</rate>" +
    "      </org.lockss.plugin.RateLimiterInfo>" +
    "    </entry>" +
    "  </cond>" +
    "</org.lockss.plugin.RateLimiterInfo>";

  public void testCondSer() throws Exception {
    RateLimiterInfo rli = fromString(condser);
    assertNull("1/13000", rli.getDefaultRate());
    assertEquals("poolKey", rli.getCrawlPoolKey());
    assertNull(rli.getUrlRates());
    assertNull(rli.getMimeRates());
    Map cond = rli.getCond();
    List wins = new ArrayList(cond.keySet());
    assertClass(CrawlWindows.Never.class, wins.get(0));
    assertEquals(new CrawlWindows.Daily("22:00", "23:00",
					"America/Los_Angeles"),
		 wins.get(1));
    List<RateLimiterInfo> rlis = new ArrayList(cond.values());
    RateLimiterInfo rli1 = rlis.get(0);
    assertEquals("1/4s", rli1.getDefaultRate());
    assertEquals(MapUtil.map("text/html", "10/1m", "image/*", "5/1s"),
		 rli1.getMimeRates());
    RateLimiterInfo rli2 = rlis.get(1);
    assertEquals("1/12s", rli2.getDefaultRate());
    assertNull(rli2.getMimeRates());
  }

  RateLimiterInfo fromString(String s) throws Exception {
    InputStream in = new StringInputStream(s);
    return (RateLimiterInfo)new XStreamSerializer().deserialize(in);
  }
}
