/*
 * $Id: TestHtmlTags.java,v 1.3 2010-02-04 06:53:00 tlipkis Exp $
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

package org.lockss.filter.html;

import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.filter.html.*;
import org.lockss.test.*;
import org.htmlparser.*;
import org.htmlparser.util.*;
import org.htmlparser.tags.*;
import org.htmlparser.filters.*;

public class TestHtmlTags extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHtmlTags");

  // Ensure <iframe>...</iframe> gets parsed as an HtmlTags.Iframe
  // composite tag, not as the default sequence of TagNodes
  public void testIframeTag() throws IOException {
    String in = "<iframe src=\"http://foo.bar\"><i>iii</i></iframe>";
    MockHtmlTransform xform =
      new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
      new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Iframe);
    assertEquals(1, nl.size());
  }

  // Ensure <noscript>...</noscript> gets parsed as an HtmlTags.Noscript
  // composite tag, not as the default sequence of TagNodes
  public void testNoscriptTag() throws IOException {
    String in = "<noscript><i>iii</i></noscript>";
    MockHtmlTransform xform =
      new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
      new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Noscript);
    assertEquals(1, nl.size());
  }

  // Ensure <tr> closes a previous unclosed <tr>
  public void testTdEnder() throws IOException {
    String in = "<body><table><tr><td>1<td>2<tr><td>3<td>4</table></body>";
    MockHtmlTransform xform =
      new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
      new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node body = nl.elementAt(0);
    assertTrue(body instanceof BodyTag);
    assertEquals(1, nl.size());
    Node table = ((BodyTag)body).getChild(0);
    assertTrue(table instanceof TableTag);
    NodeList elems = ((TableTag)table).getChildren();
    assertEquals(2, elems.size());
    assertTrue(elems.elementAt(0) instanceof TableRow);
    assertTrue(elems.elementAt(1) instanceof TableRow);
  }

}
