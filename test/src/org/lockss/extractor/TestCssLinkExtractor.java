/*
 * $Id: TestCssLinkExtractor.java,v 1.3 2008-02-20 19:11:55 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

import java.io.*;
import java.net.*;
import java.util.*;

import junit.framework.Assert;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

public class TestCssLinkExtractor extends LinkExtractorTestCase {

  public String getMimeType() {
    return "text/css";
  }

  public LinkExtractorFactory getFactory() {
    return new CssLinkExtractor.Factory();
  }

  public void testAbsoluteUrl() throws Exception {
    String url = "http://not.example.com/stylesheet.css";
    
    // Normal @import syntax
    doTestOneUrl("@import url(\'", url, "\');", url);
    doTestOneUrl("@import url(\"", url, "\");", url);
    // Simplified @import syntax
    doTestOneUrl("@import \'", url, "\';", url);
    doTestOneUrl("@import \"", url, "\";", url);
    // Property
    doTestOneUrl("bar { foo: url(\'", url, "\'); }", url);
    doTestOneUrl("bar { foo: url(\"", url, "\"); }", url);
  }
  
  public void testHandlesInputWithNoUrls() throws Exception {
    String source =
      "/* Comment */" +
      "foo {" +
      "  bar: baz;" +
      "}";
    assertEmpty(extractUrls(source));
  }
  
  public void testRelativeUrl() throws Exception {
    String url = "stylesheet.css";
    String fullUrl = SOURCE_URL.substring(0, SOURCE_URL.lastIndexOf('/') + 1) + url;
    
    // Normal @import syntax
    doTestOneUrl("@import url(\'", url, "\');", fullUrl);
    doTestOneUrl("@import url(\"", url, "\");", fullUrl);
    // Simplified @import syntax
    doTestOneUrl("@import \'", url, "\';", fullUrl);
    doTestOneUrl("@import \"", url, "\";", fullUrl);
    // Property
    doTestOneUrl("bar { foo: url(\'", url, "\'); }", fullUrl);
    doTestOneUrl("bar { foo: url(\"", url, "\"); }", fullUrl);
  }
  
  public void testThrowsOnEmptyUrl() throws Exception {
    try {
      extractUrls("@import url(\'\');");
      fail("Parser did not throw a MalformedURLException on an empty URL");
    }
    catch (MalformedURLException expected) {
      // all is well
    }
  }

  public void testCssFragment() throws Exception {
    String givenPrefix = "http://www.foo.com/";
    String expectedPrefix = SOURCE_URL.substring(0, SOURCE_URL.lastIndexOf('/') + 1);
    String url1 = "foo1.css";
    String url2 = "foo2.css";
    String url3 = "foo3.css";
    String url4 = "foo4.css";
    String url5 = "img5.gif";
    String url6 = "img6.gif";

    String source =
      "@import url(\'" + url1 + "\');\n" +
      "@import url(\"" + url2 + "\");\n" +
      "@import \'" + url3 + "\';\n" +
      "@import \"" + givenPrefix + url4 + "\";\n" +
      "foo {\n" +
      " bar: url(\'" + givenPrefix + url5 + "\');\n" +
      " baz: url(\"" + givenPrefix + url6 + "\");\n" +
      "}\n" +
      "/* Comment */\n";
    
    assertEquals(SetUtil.set(expectedPrefix + url1,
                             expectedPrefix + url2,
                             expectedPrefix + url3,
                             givenPrefix + url4,
                             givenPrefix + url5,
                             givenPrefix + url6),
                 extractUrls(source));
  }
  
  public void testThrowsOnMalformedUrl() throws Exception {
    String badUrl = "unknown://www.bad.url/foo.gif";
    
    try {
      // Sanity check
      URL url = new URL(badUrl);
      fail("This test assumes that a bad URL of some kind throws a MalformedURLException");
    }
    catch (MalformedURLException expected) {
      // all is well
    }
    
    try {
      extractUrls("@import url(\'" + badUrl + "\');");
      fail("Parser did not throw a MalformedURLException on " + badUrl);
    }
    catch (MalformedURLException expected) {
      // all is well
    }
  }  

  /**
   * <p>Parses a fragment, expecting one URL.</p>
   * <p>The fragment parsed is made of the concatenation of the
   * three arguments <code>beginning</code>, <code>middle</code>
   * and <code>end</code>.</p>
   * <p>The starting URL for the parse is {@link #SOURCE_URL}.</p>
   * @param beginning   The beginning of the CSS fragment.
   * @param middle      The middle of the CSS fragment.
   * @param end         The end of the CSS fragment.
   * @param expectedUrl The single URL expected to be found.
   * @throws IOException if any processing error occurs.
   * @see #SOURCE_URL
   */
  protected void doTestOneUrl(String beginning,
                              String middle,
                              String end,
                              String expectedUrl)
      throws IOException, PluginException {
    assertEquals(SetUtil.set(expectedUrl),
		 extractUrls(beginning + middle + end));
  }  

  protected static final String SOURCE_URL =
    "http://www.example.com/source.css";
  
}
