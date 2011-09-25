/*
 * $Id: TestRateLimiterInfo.java,v 1.1 2011-09-25 04:20:39 tlipkis Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestRateLimiterInfo extends LockssTestCase {

  public void testDefault() {
    RateLimiterInfo rli = new RateLimiterInfo("foo", 1, 12000);
    assertEquals("1/12000", rli.getDefaultRate());
    assertEquals("foo", rli.getCrawlPoolKey());
    assertNull(rli.getMimeRates());
    assertNull(rli.getUrlRates());
  }

  public void testMime() {
    RateLimiterInfo rli = new RateLimiterInfo("bar", 1, 13000);
    Map map = MapUtil.map("text/html", "1/2s", "image/*", "1/1");
    rli.setMimeRates(map);
    assertEquals("1/13000", rli.getDefaultRate());
    assertEquals("bar", rli.getCrawlPoolKey());
    assertSame(map, rli.getMimeRates());
    assertNull(rli.getUrlRates());
  }

  public void testUrl() {
    RateLimiterInfo rli = new RateLimiterInfo("bar", 1, 13000);
    Map map = MapUtil.map(".*\\.pdf", "1/2s", ".*/images/.*", "1/1");
    rli.setUrlRates(map);
    assertEquals("1/13000", rli.getDefaultRate());
    assertEquals("bar", rli.getCrawlPoolKey());
    assertSame(map, rli.getUrlRates());
    assertNull(rli.getMimeRates());
  }
}
