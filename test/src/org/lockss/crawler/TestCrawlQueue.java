/*
 * $Id: TestCrawlQueue.java,v 1.3 2009-08-04 02:19:56 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.test.*;
import org.lockss.util.*;

public class TestCrawlQueue extends LockssTestCase {
  public void testIll() {
    CrawlQueue cq = new CrawlQueue(null);
    CrawlUrlData c1 = new CrawlUrlData("u1", 0);
    assertTrue(cq.isEmpty());
    cq.add(c1);
    assertFalse(cq.isEmpty());
    try {
      cq.add(c1);
      fail("Should not be able to re-add element");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testDefaultSort() {
    CrawlQueue cq = new CrawlQueue(null);
    CrawlUrlData c1 = new CrawlUrlData("x1", 0);
    CrawlUrlData c2 = new CrawlUrlData("u3", 1);
    CrawlUrlData c3 = new CrawlUrlData("u2", 1);
    CrawlUrlData c4 = new CrawlUrlData("u4", 2);
    CrawlUrlData c5 = new CrawlUrlData("a5", 3);
    
    assertTrue(cq.isEmpty());
    assertNull(cq.get(c4.getUrl()));
    cq.add(c4);
    assertFalse(cq.isEmpty());
    assertSame(c4, cq.get(c4.getUrl()));
    assertSame(c4, cq.first());
    cq.add(c3);
    assertSame(c3, cq.first());
    assertSame(c3, cq.get(c3.getUrl()));
    assertSame(c4, cq.get(c4.getUrl()));
    cq.add(c2);
    assertSame(c3, cq.first());
    cq.add(c1);
    assertSame(c1, cq.first());
    cq.add(c5);
    assertSame(c1, cq.first());

    List lst = new ArrayList();
    while (!cq.isEmpty()) {
      lst.add(cq.remove());
    }
    assertEquals(ListUtil.list(c1, c3, c2, c4, c5), lst);
  }

  public void testCustomSort() {
    CrawlQueue cq = new CrawlQueue(new DeepestFirstUrlOrderComparator());
    CrawlUrlData c1 = new CrawlUrlData("u1", 0);
    CrawlUrlData c2 = new CrawlUrlData("u3", 1);
    CrawlUrlData c3 = new CrawlUrlData("u2", 1);
    CrawlUrlData c4 = new CrawlUrlData("u4", 2);
    CrawlUrlData c5 = new CrawlUrlData("u5", 3);
    
    assertTrue(cq.isEmpty());
    assertNull(cq.get(c4.getUrl()));
    cq.add(c4);
    assertFalse(cq.isEmpty());
    assertSame(c4, cq.get(c4.getUrl()));
    assertSame(c4, cq.first());
    cq.add(c3);
    assertSame(c4, cq.first());
    assertSame(c3, cq.get(c3.getUrl()));
    assertSame(c4, cq.get(c4.getUrl()));
    cq.add(c2);
    assertSame(c4, cq.first());
    cq.add(c1);
    assertSame(c4, cq.first());
    cq.add(c5);
    assertSame(c5, cq.first());

    List lst = new ArrayList();
    while (!cq.isEmpty()) {
      lst.add(cq.remove());
    }
    assertEquals(ListUtil.list(c5, c4, c3, c2, c1), lst);
  }

  class DeepestFirstUrlOrderComparator implements Comparator<CrawlUrl> {
    public int compare(CrawlUrl curl1, CrawlUrl curl2) {
      int res = curl2.getDepth() - curl1.getDepth();
      if (res == 0) {
	res = curl1.getUrl().compareTo(curl2.getUrl());
      }
      return res;
    }
  }
}
