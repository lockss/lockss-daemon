/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.extractor;

import java.io.*;
import java.net.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;

public class TestRegexpCssLinkExtractor extends LinkExtractorTestCase {

  protected RegexpCssLinkExtractor cssExtractor;

  public void setUp() throws Exception {
    super.setUp();
    cssExtractor = (RegexpCssLinkExtractor)extractor;
  }

  public String getMimeType() {
    return "text/css";
  }

  public LinkExtractorFactory getFactory() {
    return new RegexpCssLinkExtractor.Factory();
  }

  public void testBackslashEscapePattern() throws Exception {
    assertEquals("foo", cssExtractor.processUrlEscapes("foo"));
    assertEquals("f,oo", cssExtractor.processUrlEscapes("f,oo"));
    assertEquals("f,oo", cssExtractor.processUrlEscapes("f\\,oo"));
    assertEquals("f,o'o\"b(a r)",
		 cssExtractor.processUrlEscapes("f\\,o\\'o\\\"b\\(a\\ r\\)"));
  }

  public void testAbsoluteUrl() throws Exception {
    String url = "http://not.example.com/stylesheet.css";
    
    // Normal @import syntax
    doTestOneUrl("@import url(\'", url, "\');", url);
    doTestOneUrl("@import url(\"", url, "\");", url);
    doTestOneUrl("@import url( \'", url, "\');", url);
    doTestOneUrl("@import  url(\"", url, "\");", url);
    doTestOneUrl("@import\turl(\"", url, "\");", url);
    doTestOneUrl("@import \turl(\"", url, "\");", url);
    doTestOneUrl("@import  url(\'", url, "\');", url);
    // Simplified @import syntax
    doTestOneUrl("@import \'", url, "\';", url);
    doTestOneUrl("@import \"", url, "\";", url);
    // Property
    doTestOneUrl("bar { foo: url(\'", url, "\'); }", url);
    doTestOneUrl("bar { foo: url(\"", url, "\"); }", url);

    // newlines
    doTestOneUrl("@import\nurl(\'", url, "\');", url);
    doTestOneUrl("@import url(\n\'", url, "\');", url);
    doTestOneUrl("@import \n url(\n\'", url, "\'\n);", url);
  }
  
  public void testHandlesInputWithNoUrls() throws Exception {
    String source =
      "/* Comment */" +
      "foo {" +
      "  bar: baz;" +
      "}";
    assertEmpty(extractUrls(source));
  }
  
  public void testIgnoresDataUrls() throws Exception {
    String source =
      "foo { background: url(data:image/gif;base64,ABC);" +
      "bar { background: url('data:image/gif;base64,DEF');" +
      "baz { background: url(\"data:image/gif;base64,GHI\");";
    assertEmpty(extractUrls(source));
  }
  
  public void testHandlesInputWithBadHexConstant() throws Exception {
    extractUrls("foo { background-color: #1; }");
    extractUrls("foo { background-color: #12; }");
    extractUrls("foo { background-color: #1234; }");
    extractUrls("foo { background-color: #12345; }");
    extractUrls("foo { background-color: #1234567; }");
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
  
  public void testEmptyUrl() throws Exception {
    assertEmpty(extractUrls("@import url(\'\');"));
  }

  public void testCssFragment() throws Exception {
    String givenPrefix = "http://www.foo.com/";
    String expectedPrefix = SOURCE_URL.substring(0, SOURCE_URL.lastIndexOf('/') + 1);
    String url0 = "foo0.css";
    String url1 = "foo1.css";
    String url2 = "foo2.css";
    String url3 = "foo3.css";
    String url3a = "foo3.c\\(ss";
    String url3b = "foo3.c(ss";
    String url4 = "foo4.css";
    String url5 = "img5.gif";
    String url6 = "img6.gif";
    String url7 = "img7.gif";
    String url8 = "img8.gif";

    String source =
      "@import url(" + url0 + ");\n" +
      "@import url(\'" + url1 + "\'); " + // no newline
      "@import url(  \"" + url2 + "\"  );\n" +
      "@import \'" + url3 + "\';\n" +
      "@import \'" + url3a + "\';\n" +
      "@import \"" + givenPrefix + url4 + "\";\n" +
      "foo {\n" +
      " bar: url(\n\'" + givenPrefix + url5 + "\');\n" + // embedded newline
      " baz: url(\"" + givenPrefix + "\\\n" + url6 + "\");\n" +
      " baq: url(  " + givenPrefix + url7 + "  );\n" +
      "}\n" +
      "/* Comment */ url(" + url8 + ");\n";
    
    assertEquals(SetUtil.set(expectedPrefix + url0,
                             expectedPrefix + url1,
                             expectedPrefix + url2,
                             expectedPrefix + url3,
                             expectedPrefix + url3b,
                             givenPrefix + url4,
                             givenPrefix + url5,
                             givenPrefix + url6,
                             givenPrefix + url7,
                             expectedPrefix + url8),
                 extractUrls(source));
  }

  public void testCssFragmentSmallBuf() throws Exception {
    extractor = new RegexpCssLinkExtractor(1000, 200);
    testCssFragment();
    extractor = new RegexpCssLinkExtractor(1000, 900);
    testCssFragment();
    extractor = new RegexpCssLinkExtractor(100, 50);
    testCssFragment();
  }

  public void testMalformedUrl() throws Exception {
    String badUrl = "unknown://www.bad.url/foo.gif";
    
    try {
      // Sanity check
      URL url = new URL(badUrl);
      fail("This test assumes that a bad URL of some kind throws a MalformedURLException");
    }
    catch (MalformedURLException expected) {
      // all is well
    }
    
    assertEquals(SetUtil.set("http://www.example.com/foo"),
		 extractUrls("@import url(\'" + badUrl + "\');\n" +
			     "@import url(\'foo\');"));
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
