/*
 * $Id: TestHighWirePressH20CrawlUrlComparatorFactory.java,v 1.1 2009-08-05 23:59:37 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import org.lockss.plugin.highwire.HighWirePressH20CrawlUrlComparatorFactory.HighWirePressH20StringUrlComparator;
import org.lockss.plugin.highwire.HighWirePressH20CrawlUrlComparatorFactory.HighWirePressH20StringUrlComparator.HighWireUrl;
import org.lockss.test.LockssTestCase;

public class TestHighWirePressH20CrawlUrlComparatorFactory extends LockssTestCase {

  protected HighWirePressH20StringUrlComparator comparator;
  
  public void setUp() throws Exception {
    this.comparator = new HighWirePressH20StringUrlComparator("http://www.example.com/", "123");
  }
  
  public void testAssumptions() throws Exception {
    assertNegative(".".compareTo("/"));
    assertNegative("456".compareTo("789"));
  }

  public void testHighWireUrl() throws Exception {
    doTestOneHighWireUrl("http://www.example.com/lockss-manifest/vol_106_manifest.dtl", false, null, null);
    doTestOneHighWireUrl("http://www.example.com/shared/css/hw-global.css", false, null, null);
    doTestOneHighWireUrl("http://www.example.com/content/123/456.author-index", true, "456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/456.cover-expansion", true, "456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/456.toc", true, "456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/456.toc.pdf", true, "456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/456.cover.gif", true, "456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999.abstract", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999.extract", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999.figures-only", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999.full", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999.full.pdf", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999.full.pdf+html", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999.full.pdf+html?frame=header", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999.full.pdf+html?frame=sidebar", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999/suppl/DCSupplemental", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999/F1", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999/F1.expansion.html", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/456/999/F1.large.jpg", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/powerpoint/123/456/999/F1", true, "456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456.author-index", true, "suppl.456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456.cover-expansion", true, "suppl.456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456.toc", true, "suppl.456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456.toc.pdf", true, "suppl.456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456.cover.gif", true, "suppl.456", "");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999.abstract", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999.extract", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999.figures-only", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999.full", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999.full.pdf", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999.full.pdf+html", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999.full.pdf+html?frame=header", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999.full.pdf+html?frame=sidebar", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999/suppl/DCSupplemental", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999/F1", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999/F1.expansion.html", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/content/123/suppl.456/999/F1.large.jpg", true, "suppl.456", "999");
    doTestOneHighWireUrl("http://www.example.com/powerpoint/123/suppl.456/999/F1", true, "suppl.456", "999");
  }
  
  protected void doTestOneHighWireUrl(String url,
                                      boolean expectedTuple,
                                      String expectedIssue,
                                      String expectedPage)
      throws Exception {
    HighWireUrl hwu = comparator.makeHighWireUrl(url);
    assertEquals(expectedTuple, hwu.tuple);
    if (!expectedTuple) {
      return;
    }
    assertEquals("Expected " + expectedIssue + " but was " + hwu.issue,
                 expectedIssue,
                 hwu.issue);
    if (expectedPage == null) {
      assertNull("Expected null page but got " + hwu.page,
                 hwu.page);
      return;
    }
    assertEquals("Expected " + expectedPage + " but was " + hwu.page,
                 expectedPage,
                 hwu.page);
  }
  
  public void testComparator() throws Exception {
    doTestFirstBeforeSecond("http://www.example.com/lockss-manifest/vol_123_manifest.dtl",
                            "http://www.example.com/content/123/456.toc");
    doTestFirstBeforeSecond("http://www.example.com/content/123/456.toc",
                            "http://www.example.com/content/123/789.toc");
    doTestFirstBeforeSecond("http://www.example.com/content/123/456/999.full",
                            "http://www.example.com/content/123/789.toc");
    doTestFirstBeforeSecond("http://www.example.com/content/123/456/999.full",
                            "http://www.example.com/content/123/789/9999.full");
    doTestFirstBeforeSecond("http://www.example.com/content/123/456.toc",
                            "http://www.example.com/content/123/456.toc.pdf");
    doTestFirstBeforeSecond("http://www.example.com/content/123/456.cover.gif",
                            "http://www.example.com/content/123/456.toc");
    doTestFirstBeforeSecond("http://www.example.com/content/123/456/999.full",
                            "http://www.example.com/content/123/789.author-index");
    doTestFirstBeforeSecond("http://www.example.com/content/123/456/999.full",
                            "http://www.example.com/content/123/456/999/F1");
    doTestFirstBeforeSecond("http://www.example.com/content/123/456/999.full",
                            "http://www.example.com/powerpoint/123/456/999/F1");
  }
  
  protected void doTestFirstBeforeSecond(String url1,
                                         String url2)
      throws Exception {
    assertNegative(comparator.compare(url1, url2));
    assertPositive(comparator.compare(url2, url1));
  }
  
}
