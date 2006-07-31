/*
 * $Id: TestHtmlNodeSequenceTransform.java,v 1.1 2006-07-31 06:47:25 tlipkis Exp $
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

public class TestHtmlNodeSequenceTransform extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHtmlNodeSequenceTransform");

  /** Check that nodes that match the filter are removed. */
  private void assertFiltersOut(String expected, String input,
				NodeFilter beginFilter, NodeFilter endFilter)
      throws IOException {
    HtmlTransform xform =
      HtmlNodeSequenceTransform.excludeSequence(beginFilter, endFilter);
    Reader reader = new HtmlFilterReader(new StringReader(input), xform);
    assertReaderMatchesString(expected, reader);
  }

  public void testIll() {
    try {
      HtmlNodeSequenceTransform.excludeSequence(null, new OrFilter());
      fail("null filter should throw");
    } catch(IllegalArgumentException iae) {
    }
    try {
      HtmlNodeSequenceTransform.excludeSequence(new OrFilter(), null);
      fail("null filter should throw");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testNotFound() throws IOException {
    String str = "<html><b>foo<i></html>";
    NodeFilter notag = new TagNameFilter("nosuchtag");
    assertFiltersOut(str, str, notag, notag);
    assertFiltersOut(str, str, new TagNameFilter("b"), notag);
    assertFiltersOut(str, str, notag, new TagNameFilter("i"));
    assertFiltersOut("<html></html>", str,
		     new TagNameFilter("b"), new TagNameFilter("i"));
  }

  public void testNotFoundThrows() throws IOException {
    String str = "<html><b>foo<i></html>";
    HtmlNodeSequenceTransform xform =
      HtmlNodeSequenceTransform.excludeSequence(new TagNameFilter("b"),
						new TagNameFilter("notag"));
    xform.setErrorIfNoEndNode(true);
    Reader reader = new HtmlFilterReader(new StringReader(str), xform);
    try {
      StringUtil.fromReader(reader);
      fail("Should throw if end string not found");
    } catch (HtmlNodeSequenceTransform.MissingEndNodeException e) {}
  }

  public void testSimple() throws IOException {
    String str =
      "<html>bo<!-- start ad 15 --><img>ad text<!-- end ad 15 -->dy</html>";
    NodeFilter sf = HtmlNodeFilters.commentWithString("start ad");
    NodeFilter ef = HtmlNodeFilters.commentWithString("end ad");
    assertFiltersOut("<html>body</html>", str, sf, ef);
  }

  public void testComplex() throws IOException {
    String str =
      "<html>bo<!-- start ad 15 --><img>ad text<!-- end ad 15 -->dy" +
      "<table>" +
      "<tr><td>da<!-- start ad -->foo<!-- end ad -->ta</td></tr>" +
      "</table></html>";
    NodeFilter sf = HtmlNodeFilters.commentWithString("start ad");
    NodeFilter ef = HtmlNodeFilters.commentWithString("end ad");
    assertFiltersOut("<html>body<table><tr><td>data</td></tr></table></html>",
		     str, sf, ef);
  }

}
