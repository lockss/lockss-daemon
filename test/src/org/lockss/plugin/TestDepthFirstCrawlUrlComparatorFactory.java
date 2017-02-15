/*
 * $Id$
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

import org.lockss.crawler.*;
import org.lockss.test.LockssTestCase;

public class TestDepthFirstCrawlUrlComparatorFactory extends LockssTestCase {

  /**
   * <p>
   * A string comparator that uses a crawl URL comparator to perform the
   * comparison.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @since 1.67
   */
  protected static class UrlComparatorAdapter implements Comparator<String> {
    
    protected Comparator<CrawlUrl> cucmp;
    
    public UrlComparatorAdapter(Comparator<CrawlUrl> cucmp) {
      this.cucmp = cucmp;
    }
    
    @Override
    public int compare(String o1, String o2) {
      return cucmp.compare(mkcud(o1), mkcud(o2));
    }
    
  }
  
  protected Comparator<String> scmp;
  
  public void setUp() throws Exception {
    scmp = new UrlComparatorAdapter(new DepthFirstCrawlUrlComparatorFactory().createCrawlUrlComparator(null));
  }
  
  public void testCompareUrls() throws Exception {
     assertNegative(scmp.compare("http://www.example.com", "http://www.lockss.org"));
     assertNegative(scmp.compare("http://www.example.com", "http://www.lockss.org/"));
     assertNegative(scmp.compare("http://www.example.com/", "http://www.lockss.org"));
     assertNegative(scmp.compare("http://www.example.com/", "http://www.lockss.org/"));
     
     assertEquals(0, scmp.compare("http://www.example.com/a", "http://www.example.com/a"));
     assertPositive(scmp.compare("http://www.example.com/a", "http://www.example.com/a/"));
     assertNegative(scmp.compare("http://www.example.com/a/", "http://www.example.com/a"));
     assertEquals(0, scmp.compare("http://www.example.com/a/", "http://www.example.com/a/"));
     
     assertNegative(scmp.compare("http://www.example.com/a", "http://www.example.com/x"));
     assertNegative(scmp.compare("http://www.example.com/a", "http://www.example.com/x/"));
     assertNegative(scmp.compare("http://www.example.com/a/", "http://www.example.com/x"));
     assertNegative(scmp.compare("http://www.example.com/a/", "http://www.example.com/x/"));
     
     assertNegative(scmp.compare("http://www.example.com/a", "http://www.example.com/x/y/z"));
     assertNegative(scmp.compare("http://www.example.com/a", "http://www.example.com/x/y/z/"));
     assertNegative(scmp.compare("http://www.example.com/a/", "http://www.example.com/x/y/z"));
     assertNegative(scmp.compare("http://www.example.com/a/", "http://www.example.com/x/y/z/"));

     assertNegative(scmp.compare("http://www.example.com/a/b/c", "http://www.example.com/x"));
     assertNegative(scmp.compare("http://www.example.com/a/b/c", "http://www.example.com/x/"));
     assertNegative(scmp.compare("http://www.example.com/a/b/c/", "http://www.example.com/x"));
     assertNegative(scmp.compare("http://www.example.com/a/b/c/", "http://www.example.com/x/"));
     
     assertPositive(scmp.compare("http://www.example.com/a", "http://www.example.com/a/b/c"));
     assertPositive(scmp.compare("http://www.example.com/a", "http://www.example.com/a/b/c/"));
     assertPositive(scmp.compare("http://www.example.com/a/", "http://www.example.com/a/b/c"));
     assertPositive(scmp.compare("http://www.example.com/a/", "http://www.example.com/a/b/c/"));
  }
  
  public void testOrderList() throws Exception {
    List<String> expected = Arrays.asList(
        "http://www.example.com/a/b/c/",
        "http://www.example.com/a/b/c",
        "http://www.example.com/a/b/",
        "http://www.example.com/a/b",
        "http://www.example.com/a/",
        "http://www.example.com/a",
        "http://www.example.com/p/q/r/",
        "http://www.example.com/p/q/r",
        "http://www.example.com/p/q/",
        "http://www.example.com/p/q",
        "http://www.example.com/p/",
        "http://www.example.com/p",
        "http://www.example.com/x/y/z/",
        "http://www.example.com/x/y/z",
        "http://www.example.com/x/y/",
        "http://www.example.com/x/y",
        "http://www.example.com/x/",
        "http://www.example.com/x",
        "http://www.example.com/"
    );
    List<String> input = new ArrayList<String>(expected);
    while (input.equals(expected)) {
      Collections.shuffle(input);
    }
    Collections.sort(input, scmp);
    assertEquals(expected, input);
  }
  
  public void testCrawlQueue() throws Exception {
    // Simulate a crawl and its crawl queue for a simplistic site
    CrawlQueue cq = new CrawlQueue(new DepthFirstCrawlUrlComparatorFactory().createCrawlUrlComparator(null));
    cq.add(mkcud("http://www.example.com/"));

    assertEquals("http://www.example.com/", cq.remove().getUrl());
    // ... / has links to /x, /a, /p
    cq.add(mkcud("http://www.example.com/x"));
    cq.add(mkcud("http://www.example.com/a"));
    cq.add(mkcud("http://www.example.com/p"));
    
    assertEquals("http://www.example.com/a", cq.remove().getUrl());
    // ... /a has links to /, /a/b
    cq.add(mkcud("http://www.example.com/a/b"));
    
    assertEquals("http://www.example.com/a/b", cq.remove().getUrl());
    // ... /a/b has links to /a, /a/b/c
    cq.add(mkcud("http://www.example.com/a/b/c"));
    
    assertEquals("http://www.example.com/a/b/c", cq.remove().getUrl());
    // ... /a/b/c has links to /a/b
    
    assertEquals("http://www.example.com/p", cq.remove().getUrl());
    // ... /p has links to /, /p/q
    cq.add(mkcud("http://www.example.com/p/q"));
    
    assertEquals("http://www.example.com/p/q", cq.remove().getUrl());
    // ... /p/q has links to /p, /p/q/r
    cq.add(mkcud("http://www.example.com/p/q/r"));
    
    assertEquals("http://www.example.com/p/q/r", cq.remove().getUrl());
    // ... /p/q/r has links to /p/q
    
    assertEquals("http://www.example.com/x", cq.remove().getUrl());
    // ... /x has links to /, /x/y
    cq.add(mkcud("http://www.example.com/x/y"));
    
    assertEquals("http://www.example.com/x/y", cq.remove().getUrl());
    // ... /x/y has links to /x, /x/y/z
    cq.add(mkcud("http://www.example.com/x/y/z"));
    
    assertEquals("http://www.example.com/x/y/z", cq.remove().getUrl());
    // ... /x/y/z has links to /x/y
    
    assertTrue(cq.isEmpty());
  }
  
  protected static CrawlUrlData mkcud(String url) {
    return new CrawlUrlData(url, 0);
  }
  
}
