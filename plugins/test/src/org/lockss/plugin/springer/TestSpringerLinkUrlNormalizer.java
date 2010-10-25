/*
 * $Id: TestSpringerLinkUrlNormalizer.java,v 1.1 2010-10-25 21:09:53 thib_gc Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestSpringerLinkUrlNormalizer extends LockssTestCase {

  public void testSameUrls() throws Exception {
    String[] urls = new String[] {
        "http://www.example.com/",
        "http://www.example.com/favicon.ico",
        "http://www.example.com/content/9999-9999/1",
        "http://www.example.com/content/9999-9999/1/",
        "http://www.example.com/content/9999-9999/1/?target=print",
        "http://www.example.com/content/9999-9999/1/42",
        "http://www.example.com/content/9999-9999/1/42/",
        "http://www.example.com/content/9999-9999/1/42/?target=print",
        "http://www.example.com/0123456789ABCDEF",
        "http://www.example.com/0123456789ABCDEF/",
        "http://www.example.com/0123456789ABCDEF/abstract",
        "http://www.example.com/0123456789ABCDEF/abstract/",
        "http://www.example.com/0123456789ABCDEF/fulltext.html",
        "http://www.example.com/0123456789ABCDEF/fulltext.pdf",
        "http://www.example.com/0123456789ABCDEF/crossref_link.gif",
        "http://www.example.com/images/foo.gif",
        "http://www.example.com/images/foo.jpg",
        "http://www.example.com/images/foo.png",
        "http://www.example.com/images/foo.1.png",
        "http://www.example.com/images/foo.1.2.png",
        "http://www.example.com/images/foo.1.2.3.png",
        "http://www.example.com/scripts/zzz/foo.js",
        "http://www.example.com/scripts/zzz/foo.1.js",
        "http://www.example.com/scripts/zzz/foo.1.2.js",
        "http://www.example.com/scripts/zzz/foo.1.2.3.js",
        "http://www.example.com/styles/foo.css",
        "http://www.example.com/styles/foo.1.css",
        "http://www.example.com/styles/foo.1.2.css",
        "http://www.example.com/styles/foo.1.2.3.css",
    };
    UrlNormalizer norm = new SpringerLinkUrlNormalizer();
    for (String url : urls) {
      assertEquals(url, norm.normalizeUrl(url, null));
    }
  }

  public void testNormalizedUrls() throws Exception {
    String[][] map = new String[][] {
        {"http://www.example.com/images/foo.1.2.3.4.gif", "http://www.example.com/images/foo.gif"},
        {"http://www.example.com/images/foo.1.2.3.4.jpg", "http://www.example.com/images/foo.jpg"},
        {"http://www.example.com/images/foo.1.2.3.4.png", "http://www.example.com/images/foo.png"},
        {"http://www.example.com/scripts/zzz/foo.1.2.3.4.js", "http://www.example.com/scripts/zzz/foo.js"},
        {"http://www.example.com/styles/foo.1.2.3.4.css", "http://www.example.com/styles/foo.css"},
    };
    UrlNormalizer norm = new SpringerLinkUrlNormalizer();
    for (String[] urlin_urlout : map) {
      assertEquals(urlin_urlout[1], norm.normalizeUrl(urlin_urlout[0], null));
    }
  }
    
}
