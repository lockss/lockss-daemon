/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
