/*
 * $Id: TestCrawlUrlData.java,v 1.2 2009-08-04 02:19:56 tlipkis Exp $
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

public class TestCrawlUrlData extends LockssTestCase {
  public void testIll() {
    try {
      new CrawlUrlData(null, 3);
      fail("null URL should throw");
    } catch (NullPointerException e) {
    }
    try {
      new CrawlUrlData("bar", -1);
      fail("negative depth should throw");
    } catch (IllegalArgumentException e) {
    }
    CrawlUrlData cu1 = new CrawlUrlData("foo", 3);
    try {
      cu1.encounteredAtDepth(-1);
      fail("negative depth should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testFlags() {
    CrawlUrlData curl = new CrawlUrlData("foo", 7);
    assertFalse(curl.isFetched());
    assertFalse(curl.isFailedFetch());
    assertFalse(curl.isFailedParse());
    curl.setFetched(true);
    assertTrue(curl.isFetched());
    assertFalse(curl.isFailedFetch());
    assertFalse(curl.isFailedParse());
    curl.setFetched(false);
    assertFalse(curl.isFetched());
    assertFalse(curl.isFailedFetch());
    assertFalse(curl.isFailedParse());
    curl.setFailedFetch(true);
    assertFalse(curl.isFetched());
    assertTrue(curl.isFailedFetch());
    assertFalse(curl.isFailedParse());
    curl.setFailedFetch(false);
    assertFalse(curl.isFetched());
    assertFalse(curl.isFailedFetch());
    assertFalse(curl.isFailedParse());
    curl.setFailedParse(true);
    assertFalse(curl.isFetched());
    assertFalse(curl.isFailedFetch());
    assertTrue(curl.isFailedParse());
    curl.setFailedParse(false);
    assertFalse(curl.isFetched());
    assertFalse(curl.isFailedFetch());
    assertFalse(curl.isFailedParse());
  }

  class Event {
    CrawlUrlData curl;
    int from;
    int to;
    Event(CrawlUrlData curl, int from, int to) {
      this.curl = curl;
      this.from = from;
      this.to = to;
    }

    public boolean equals(Object o) {
      if (o instanceof Event) {
	Event e = (Event)o;
	return curl == e.curl && from == e.from && to == e.to;
      }
      return false;
    }
    public String toString() {
      return "[rde: " + from + " => " + to + ": " + curl + "]";
    }
  }

  class EventRecorder implements CrawlUrlData.ReducedDepthHandler {
    List<Event> events = new ArrayList<Event>();

    public void depthReduced(CrawlUrlData curl, int from, int to) {
      events.add(new Event(curl, from, to));
    }
    Event getEvent(int n) {
      return events.get(n);
    }
  }

  public void testNoChildren() {
    CrawlUrlData cu1 = new CrawlUrlData("foo", 0);
    assertEquals("foo", cu1.getUrl());
    assertEquals(0, cu1.getDepth());
    cu1.encounteredAtDepth(3);
    assertEquals(0, cu1.getDepth());

    CrawlUrlData cu2 = new CrawlUrlData("http://foo.com/bar", 8);
    assertEquals("http://foo.com/bar", cu2.getUrl());
    assertEquals(8, cu2.getDepth());
    cu2.encounteredAtDepth(9);
    assertEquals(8, cu2.getDepth());
    cu2.encounteredAtDepth(6);
    assertEquals(6, cu2.getDepth());
    cu2.encounteredAtDepth(7);
    assertEquals(6, cu2.getDepth());
    cu2.encounteredAtDepth(3);
    assertEquals(3, cu2.getDepth());
  }

//     assertEquals(ListUtil.list(new Event(cu2, 8, 6), new Event(cu2, 6, 3)),
// 		 er.events);

  public void testChildren() {
    EventRecorder er = new EventRecorder();

    CrawlUrlData cu1 = new CrawlUrlData("c1", 0);
    CrawlUrlData cu2 = new CrawlUrlData("c2", 2);
    CrawlUrlData cu3 = new CrawlUrlData("c3", 4);
    CrawlUrlData cu4 = new CrawlUrlData("c4", 5);
    CrawlUrlData cu5 = new CrawlUrlData("c5", 6);
    CrawlUrlData cu6 = new CrawlUrlData("c6", 8);
    CrawlUrlData cu7 = new CrawlUrlData("c6", 2);
    assertEquals("c1", cu1.getUrl());
    assertEquals(0, cu1.getDepth());
    assertEquals(2, cu2.getDepth());
    assertEquals(4, cu3.getDepth());
    assertEquals(5, cu4.getDepth());
    assertEquals(6, cu5.getDepth());
    assertEquals(8, cu6.getDepth());
    assertEquals(2, cu7.getDepth());
    assertEmpty(er.events);

    cu2.addChild(cu3, er);
    assertEquals(ListUtil.list(new Event(cu3, 4, 3)), er.events);
    cu3.addChild(cu4, er);
    assertEquals(ListUtil.list(new Event(cu3, 4, 3),
			       new Event(cu4, 5, 4)),
		 er.events);
    cu3.addChild(cu5, er);
    assertEquals(ListUtil.list(new Event(cu3, 4, 3),
			       new Event(cu4, 5, 4),
			       new Event(cu5, 6, 4)),
		 er.events);
    cu3.addChild(cu6, er);
    assertEquals(ListUtil.list(new Event(cu3, 4, 3),
			       new Event(cu4, 5, 4),
			       new Event(cu5, 6, 4),
			       new Event(cu6, 8, 4)),
		 er.events);
    cu3.addChild(cu7, er);
    assertEquals(ListUtil.list(new Event(cu3, 4, 3),
			       new Event(cu4, 5, 4),
			       new Event(cu5, 6, 4),
			       new Event(cu6, 8, 4)),
		 er.events);

    cu1.addChild(cu2, er);
    assertEquals(ListUtil.list(new Event(cu3, 4, 3),
			       new Event(cu4, 5, 4),
			       new Event(cu5, 6, 4),
			       new Event(cu6, 8, 4),
			       new Event(cu4, 4, 3),
			       new Event(cu5, 4, 3),
			       new Event(cu6, 4, 3),
			       new Event(cu3, 3, 2),
			       new Event(cu2, 2, 1)),
		 er.events);

    assertEquals(0, cu1.getDepth());
    assertEquals(1, cu2.getDepth());
    assertEquals(2, cu3.getDepth());
    assertEquals(3, cu4.getDepth());
    assertEquals(3, cu5.getDepth());
    assertEquals(3, cu6.getDepth());
    assertEquals(2, cu7.getDepth());
  }
}
