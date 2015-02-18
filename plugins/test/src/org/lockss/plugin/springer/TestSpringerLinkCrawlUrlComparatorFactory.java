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

package org.lockss.plugin.springer;

import java.util.Comparator;

import org.lockss.crawler.CrawlUrl;
import org.lockss.test.LockssTestCase;

public class TestSpringerLinkCrawlUrlComparatorFactory extends LockssTestCase {

  private static CrawlUrl url(final String url) {
    return new CrawlUrl() {
      @Override public int getDepth() { return 0; }
      @Override public String getUrl() { return url; }
    };
  }
  
  public void testComparator() throws Exception {
    Comparator<CrawlUrl> cuc = new SpringerLinkCrawlUrlComparatorFactory().createCrawlUrlComparator(null);
    assertEquals("http://www.example.com/dynamic-file.axd?id=foo".compareTo("http://www.example.com/dynamic-file.axd?id=bar"),
                 cuc.compare(url("http://www.example.com/dynamic-file.axd?id=foo"),
                             url("http://www.example.com/dynamic-file.axd?id=bar")));
    assertNegative(cuc.compare(url("http://www.example.com/dynamic-file.axd?id=foo"),
                               url("http://www.example.com/content/abcdefghijklmnop/fulltext.pdf")));
    assertNegative(cuc.compare(url("http://www.example.com/dynamic-file.axd?id=foo"),
                               url("http://www.example.com/content/abcdefghijklmnop/fulltext.html")));
    assertNegative(cuc.compare(url("http://www.example.com/dynamic-file.axd?id=foo"),
                               url("http://www.example.com/favicon.ico")));
    
    assertPositive(cuc.compare(url("http://www.example.com/content/abcdefghijklmnop/fulltext.pdf"),
                               url("http://www.example.com/dynamic-file.axd?id=foo")));
    assertEquals("http://www.example.com/content/abcdefghijklmnop/fulltext.pdf".compareTo("http://www.example.com/content/zyxwvutsrqponmlk/fulltext.pdf"),
                 cuc.compare(url("http://www.example.com/content/abcdefghijklmnop/fulltext.pdf"),
                             url("http://www.example.com/content/zyxwvutsrqponmlk/fulltext.pdf")));
    assertEquals("http://www.example.com/content/abcdefghijklmnop/fulltext.pdf".compareTo("http://www.example.com/content/zyxwvutsrqponmlk/fulltext.html"),
                 cuc.compare(url("http://www.example.com/content/abcdefghijklmnop/fulltext.pdf"),
                             url("http://www.example.com/content/zyxwvutsrqponmlk/fulltext.html")));
    assertNegative(cuc.compare(url("http://www.example.com/content/abcdefghijklmnop/fulltext.pdf"),
                               url("http://www.example.com/favicon.ico")));
    
    assertPositive(cuc.compare(url("http://www.example.com/content/abcdefghijklmnop/fulltext.html"),
                               url("http://www.example.com/dynamic-file.axd?id=foo")));
    assertEquals("http://www.example.com/content/abcdefghijklmnop/fulltext.html".compareTo("http://www.example.com/content/zyxwvutsrqponmlk/fulltext.pdf"),
                 cuc.compare(url("http://www.example.com/content/abcdefghijklmnop/fulltext.html"),
                             url("http://www.example.com/content/zyxwvutsrqponmlk/fulltext.pdf")));
    assertEquals("http://www.example.com/content/abcdefghijklmnop/fulltext.html".compareTo("http://www.example.com/content/zyxwvutsrqponmlk/fulltext.html"),
                 cuc.compare(url("http://www.example.com/content/abcdefghijklmnop/fulltext.html"),
                             url("http://www.example.com/content/zyxwvutsrqponmlk/fulltext.html")));
    assertNegative(cuc.compare(url("http://www.example.com/content/abcdefghijklmnop/fulltext.html"),
                               url("http://www.example.com/favicon.ico")));
    
    assertPositive(cuc.compare(url("http://www.example.com/favicon.ico"),
                               url("http://www.example.com/dynamic-file.axd?id=foo")));
    assertPositive(cuc.compare(url("http://www.example.com/favicon.ico"),
                               url("http://www.example.com/content/abcdefghijklmnop/fulltext.pdf")));
    assertPositive(cuc.compare(url("http://www.example.com/favicon.ico"),
                               url("http://www.example.com/content/abcdefghijklmnop/fulltext.html")));
    assertEquals("http://www.example.com/favicon.ico".compareTo("http://www.example.com/logo.gif"),
                 cuc.compare(url("http://www.example.com/favicon.ico"),
                             url("http://www.example.com/logo.gif")));

  }
  
}
