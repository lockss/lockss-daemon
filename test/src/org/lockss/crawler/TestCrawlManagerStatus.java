/*
 * $Id: TestCrawlManagerStatus.java,v 1.24 2006-04-11 08:33:33 tlipkis Exp $
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
import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
// import org.lockss.daemon.status.*;
import org.lockss.plugin.*;

public class TestCrawlManagerStatus extends LockssTestCase {
  private CrawlManagerStatus cmStatus;

  public void setUp() throws Exception {
  }

  public void testHist() {
    cmStatus = new CrawlManagerStatus(3);
    assertEmpty(cmStatus.getCrawlStatusList());
    cmStatus.addCrawl(newCStat("one"));
    cmStatus.addCrawl(newCStat("two"));
    cmStatus.addCrawl(newCStat("three"));
    List l = cmStatus.getCrawlStatusList();
    Crawler.Status cs = (Crawler.Status)l.get(0);
    assertEquals(ListUtil.list("one"), cs.getStartUrls());
    assertEquals(3, l.size());
    cmStatus.addCrawl(newCStat("four"));
    l = cmStatus.getCrawlStatusList();
    cs = (Crawler.Status)l.get(0);
    assertEquals(ListUtil.list("two"), cs.getStartUrls());
    assertEquals(3, l.size());
  }

  public void testCounts() {
    cmStatus = new CrawlManagerStatus(3);
    assertEquals(0, cmStatus.getSuccessCount());
    assertEquals(0, cmStatus.getFailedCount());
    cmStatus.incrFinished(true);
    assertEquals(1, cmStatus.getSuccessCount());
    assertEquals(0, cmStatus.getFailedCount());
    cmStatus.incrFinished(false);
    assertEquals(1, cmStatus.getSuccessCount());
    assertEquals(1, cmStatus.getFailedCount());
    cmStatus.incrFinished(false);
    assertEquals(1, cmStatus.getSuccessCount());
    assertEquals(2, cmStatus.getFailedCount());
  }

  Crawler.Status newCStat(String url) {
    return new Crawler.Status(new MockArchivalUnit(),
			      ListUtil.list(url), "new");
  }
}
