/*
 * $Id: TestCssParser.java,v 1.1 2006-12-09 01:30:59 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.lockss.crawler.ContentParser.FoundUrlCallback;
import org.lockss.test.LockssTestCase;

public class TestCssParser extends LockssTestCase {

  public static final String URL = "http://www.foo.com/stylesheet.css";
  
  public void testAll() throws Exception {
    // Normal @import syntax
    testOneUrl("@import url(\'", URL, "\');");
    testOneUrl("@import url(\"", URL, "\");");
    // Simplified @import syntax
    testOneUrl("@import \'", URL, "\';");
    testOneUrl("@import \"", URL, "\";");
    // One selector
    testOneUrl("bar { foo: url(\'", URL, "\'); }");
    testOneUrl("bar { foo: url(\"", URL, "\"); }");
    // Multiple selectors
    testOneUrl("bar, .baz, #qux { foo: url(\'", URL, "\'); }");
    testOneUrl("bar, .baz, #qux { foo: url(\"", URL, "\"); }");
  }  

  protected void testOneUrl(String before, String url, String after) throws IOException {

    class UrlSetCallback implements FoundUrlCallback {
    
      protected Set found;
      
      public UrlSetCallback(Set found) {
        this.found = found;
      }
      
      public void foundUrl(String url) {
        found.add(url);
      }
    
    }
    
    Set found = new HashSet();
    new CssParser().parseForUrls(new StringReader(before + url + after),
                                 null,
                                 null,
                                 new UrlSetCallback(found));
    assertEquals(1, found.size());
    assertContains(found, url);
  }
  
}
