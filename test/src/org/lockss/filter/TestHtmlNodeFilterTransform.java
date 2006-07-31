/*
 * $Id: TestHtmlNodeFilterTransform.java,v 1.1 2006-07-31 06:47:25 tlipkis Exp $
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

package org.lockss.filter;

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.htmlparser.*;
import org.htmlparser.filters.*;

public class TestHtmlNodeFilterTransform extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHtmlNodeFilterTransform");

  /** Check that nodes that match the filter are removed. */
  private void assertFiltersOut(String expected, String input,
				NodeFilter filter)
      throws IOException {
    HtmlTransform xform = HtmlNodeFilterTransform.exclude(filter);
    Reader reader = new HtmlFilterReader(new StringReader(input), xform);
    assertReaderMatchesString(expected, reader);
  }

  /** Check that only nodes that match the filter are included. */
  private void assertFiltersIn(String expected, String input,
				  NodeFilter filter)
      throws IOException {
    HtmlTransform xform = HtmlNodeFilterTransform.include(filter);
    Reader reader = new HtmlFilterReader(new StringReader(input), xform);
    assertReaderMatchesString(expected, reader);
  }

  public void testIll() {
    try {
      HtmlNodeFilterTransform.include(null);
      fail("null filter should throw");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testSimple() throws IOException {
//     RegexFilter f0 = new RegexFilter(".*");
    assertFiltersOut("<html>foo</html>",
		       "<html>foo</html>",
		       new FalseFilter());
    assertFiltersOut("<h1>foo</h1><h2>bar</h2>",
		       "<h1>foo</h1><h2>bar</h2>",
		       new FalseFilter());
    assertFiltersOut("<html>gor\np</html\n  >",
		       "<html><p class=\"foo\">bar</p>gor\np</html\n  >",
		       new HasAttributeFilter("class", "foo"));
  }

  public void testTagAndAttr() throws IOException {
    NodeFilter f0 = new HasAttributeFilter("class", "foo");
    NodeFilter f1 = new AndFilter(new TagNameFilter("div"), f0);
    String in =
      "<html>" +
      "foo" +
      "<a>b</a>" +
      "<div class=\"foo1\"><font><i>here</i></font></div>" +
      "<div class=\"foo\"><font><i>gone</i></font></div>" +
      "<img class=\"foo\">" +
      "<div class=\"foo\">also gone</div>" +
      "<font><i>here2</i></font>" +
      "</html>";
    assertFiltersOut("<html>" +
		     "foo" +
		     "<a>b</a>" +
		     "<div class=\"foo1\"><font><i>here</i></font></div>" +
		     "<img class=\"foo\">" +
		     "<font><i>here2</i></font>" +
		     "</html>",
		     in ,
		     f1);
    assertFiltersIn("<div class=\"foo\"><font><i>gone</i></font></div>" +
		    "<div class=\"foo\">also gone</div>",
		    in ,
		    f1);
  }

  class FalseFilter implements NodeFilter {
    public boolean accept (Node node) {
      return false;
    }
  }
}
