/*
 * $Id: TestDepthFirstCrawlUrlComparatorFactory.java,v 1.1 2014-09-22 20:09:00 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.crawler.CrawlUrl;
import org.lockss.daemon.PluginException.LinkageError;
import org.lockss.test.LockssTestCase;

public class TestDepthFirstCrawlUrlComparatorFactory extends LockssTestCase {

  protected static class MyDepthFirstUrlComparator implements Comparator<String> {
    
    protected Comparator<CrawlUrl> cmp;
    
    public MyDepthFirstUrlComparator() throws LinkageError {
      this.cmp = new DepthFirstCrawlUrlComparatorFactory().createCrawlUrlComparator(null);
    }
    
    @Override
    public int compare(String o1, String o2) {
      class MyCrawlUrl implements CrawlUrl {
        protected String url;
        public MyCrawlUrl(String url) { this.url = url; }
        @Override public String getUrl() { return url; }
        @Override public int getDepth() { throw new UnsupportedOperationException(); }
      }
      return cmp.compare(new MyCrawlUrl(o1), new MyCrawlUrl(o2));
    }
    
  }
  
  protected Comparator<String> cmp;
  
  public void setUp() throws Exception {
    cmp = new MyDepthFirstUrlComparator();
  }
  
  public void testCompareUrls() throws Exception {
     assertNegative(cmp.compare("http://www.example.com", "http://www.lockss.org"));
     assertNegative(cmp.compare("http://www.example.com", "http://www.lockss.org/"));
     assertNegative(cmp.compare("http://www.example.com/", "http://www.lockss.org"));
     assertNegative(cmp.compare("http://www.example.com/", "http://www.lockss.org/"));
     assertNegative(cmp.compare("http://www.example.com/", "http://www.lockss.org////"));
     assertNegative(cmp.compare("http://www.example.com////", "http://www.lockss.org/t"));
     assertNegative(cmp.compare("http://www.example.com////", "http://www.lockss.org////"));
     
     assertEquals(0, cmp.compare("http://www.example.com/a", "http://www.example.com/a"));
     assertEquals(0, cmp.compare("http://www.example.com/a", "http://www.example.com/a/"));
     assertEquals(0, cmp.compare("http://www.example.com/a/", "http://www.example.com/a"));
     assertEquals(0, cmp.compare("http://www.example.com/a/", "http://www.example.com/a/"));
     assertEquals(0, cmp.compare("http://www.example.com/a/", "http:///www.example.com///a////"));
     assertEquals(0, cmp.compare("http:///www.example.com///a////", "http://www.example.com/a/"));
     
     assertNegative(cmp.compare("http://www.example.com/a", "http://www.example.com/x"));
     assertNegative(cmp.compare("http://www.example.com/a", "http://www.example.com/x/"));
     assertNegative(cmp.compare("http://www.example.com/a/", "http://www.example.com/x"));
     assertNegative(cmp.compare("http://www.example.com/a/", "http://www.example.com/x/"));
     
     assertNegative(cmp.compare("http://www.example.com/a", "http://www.example.com/x/y/z"));
     assertNegative(cmp.compare("http://www.example.com/a", "http://www.example.com/x/y/z/"));
     assertNegative(cmp.compare("http://www.example.com/a/", "http://www.example.com/x/y/z"));
     assertNegative(cmp.compare("http://www.example.com/a/", "http://www.example.com/x/y/z/"));
     assertNegative(cmp.compare("http://www.example.com/a/", "http://www.example.com//x///y////z/////"));

     assertNegative(cmp.compare("http://www.example.com/a/b/c", "http://www.example.com/x"));
     assertNegative(cmp.compare("http://www.example.com/a/b/c", "http://www.example.com/x/"));
     assertNegative(cmp.compare("http://www.example.com/a/b/c/", "http://www.example.com/x"));
     assertNegative(cmp.compare("http://www.example.com/a/b/c/", "http://www.example.com/x/"));
     assertNegative(cmp.compare("http://www.example.com//a///b////c/////", "http://www.example.com/x/"));
     
     assertPositive(cmp.compare("http://www.example.com/a", "http://www.example.com/a/b/c"));
     assertPositive(cmp.compare("http://www.example.com/a", "http://www.example.com/a/b/c/"));
     assertPositive(cmp.compare("http://www.example.com/a/", "http://www.example.com/a/b/c"));
     assertPositive(cmp.compare("http://www.example.com/a/", "http://www.example.com/a/b/c/"));
  }
  
  public void testOrderList() throws Exception {
    List<String> expected = Arrays.asList(
        "http://www.example.com/a/b/c",
        "http://www.example.com/a/b",
        "http://www.example.com/a",
        "http://www.example.com/p/q/r",
        "http://www.example.com/p/q",
        "http://www.example.com/p",
        "http://www.example.com/x/y/z",
        "http://www.example.com/x/y",
        "http://www.example.com/x",
        "http://www.example.com/"
    );
    List<String> input = new ArrayList<String>(expected);
    while (input.equals(expected)) {
      Collections.shuffle(input);
    }
    Collections.sort(input, cmp);
    assertEquals(expected, input);
  }
  
  public void testSimulateCrawl() throws Exception {
    // Simulate a crawl and its crawl queue for a simplistic site
    List<String> cq = new ArrayList<String>();
    cq.add("http://www.example.com/");

    assertEquals("http://www.example.com/", cq.remove(0));
    // ... / has links to /x, /a, /p
    cq.add("http://www.example.com/x");
    cq.add("http://www.example.com/a");
    cq.add("http://www.example.com/p");
    Collections.sort(cq, cmp);
    
    assertEquals("http://www.example.com/a", cq.remove(0));
    // ... /a has links to /, /a/b
    cq.add("http://www.example.com/a/b");
    Collections.sort(cq, cmp);
    
    assertEquals("http://www.example.com/a/b", cq.remove(0));
    // ... /a/b has links to /a, /a/b/c
    cq.add("http://www.example.com/a/b/c");
    Collections.sort(cq, cmp);
    
    assertEquals("http://www.example.com/a/b/c", cq.remove(0));
    // ... /a/b/c has links to /a/b
    
    assertEquals("http://www.example.com/p", cq.remove(0));
    // ... /p has links to /, /p/q
    cq.add("http://www.example.com/p/q");
    Collections.sort(cq, cmp);
    
    assertEquals("http://www.example.com/p/q", cq.remove(0));
    // ... /p/q has links to /p, /p/q/r
    cq.add("http://www.example.com/p/q/r");
    Collections.sort(cq, cmp);
    
    assertEquals("http://www.example.com/p/q/r", cq.remove(0));
    // ... /p/q/r has links to /p/q
    
    assertEquals("http://www.example.com/x", cq.remove(0));
    // ... /x has links to /, /x/y
    cq.add("http://www.example.com/x/y");
    Collections.sort(cq, cmp);
    
    assertEquals("http://www.example.com/x/y", cq.remove(0));
    // ... /x/y has links to /x, /x/y/z
    cq.add("http://www.example.com/x/y/z");
    Collections.sort(cq, cmp);
    
    assertEquals("http://www.example.com/x/y/z", cq.remove(0));
    // ... /x/y/z has links to /x/y
    
    assertEmpty(cq);
  }
  
}
