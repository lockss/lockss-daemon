/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import org.htmlparser.Node;
import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestHtmlTags extends LockssTestCase {
  
  // Ensure <header>...</header> gets parsed as an HtmlTags.Header
  // composite tag, not as the default sequence of TagNodes
  public void testHeaderTag() throws IOException {
    String in = "<header class=\"special\"><i>iii</i></header>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform)
        .registerTag(new HtmlTags.Header());
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Header);
    assertEquals(1, nl.size());
  }
  
  // Ensure <footer>...</footer> gets parsed as an HtmlTags.Footer
  // composite tag, not as the default sequence of TagNodes
  public void testFooterTag() throws IOException {
    String in = "<footer class=\"special\"><i>iii</i></footer>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform)
        .registerTag(new HtmlTags.Footer());
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Footer);
    assertEquals(1, nl.size());
  }
  
  // Ensure <section>...</section> gets parsed as an HtmlTags.Section
  // composite tag, not as the default sequence of TagNodes
  public void testSectionTag() throws IOException {
    String in = "<section class=\"special\"><i>iii</i></section>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform)
        .registerTag(new HtmlTags.Section());
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Section);
    assertEquals(1, nl.size());
  }
  
  // Ensure <aside>...</aside> gets parsed as an HtmlTags.Aside
  // composite tag, not as the default sequence of TagNodes
  public void testAsideTag() throws IOException {
    String in = "<aside class=\"special\"><i>iii</i></section>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform)
        .registerTag(new HtmlTags.Aside());
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Aside);
    assertEquals(1, nl.size());
  }
  
  // Ensure <datalist>...</datalist> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testDatalistTag() throws IOException {
    String in = "<datalist class=\"special\"><i>iii</i></datalist>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Datalist);
    assertEquals(1, nl.size());
  }
  
  // Ensure <details>...</details> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testDetailsTag() throws IOException {
    String in = "<details class=\"special\"><i>iii</i></details>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Details);
    assertEquals(1, nl.size());
  }
  
  // Ensure <dialog>...</dialog> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testDialogTag() throws IOException {
    String in = "<dialog class=\"special\"><i>iii</i></dialog>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Dialog);
    assertEquals(1, nl.size());
  }
  
  // Ensure <menu>...</menu> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testMenuTag() throws IOException {
    String in = "<menu class=\"special\"><i>iii</i></menu>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Menu);
    assertEquals(1, nl.size());
  }
  
  // Ensure <menuitem>...</menuitem> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testMenuitemTag() throws IOException {
    String in = "<menuitem class=\"special\"><i>iii</i></menuitem>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Menuitem);
    assertEquals(1, nl.size());
  }
  
  // Ensure <meter>...</meter> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testMeterTag() throws IOException {
    String in = "<meter class=\"special\"><i>iii</i></meter>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Meter);
    assertEquals(1, nl.size());
  }
  
  // Ensure <nav>...</nav> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testNavTag() throws IOException {
    String in = "<nav class=\"special\"><i>iii</i></nav>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Nav);
    assertEquals(1, nl.size());
  }
  
  // Ensure <progress>...</progress> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testProgressTag() throws IOException {
    String in = "<progress class=\"special\"><i>iii</i></progress>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Progress);
    assertEquals(1, nl.size());
  }
  
  // Ensure <summary>...</summary> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testSummaryTag() throws IOException {
    String in = "<summary class=\"special\"><i>iii</i></summary>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Summary);
    assertEquals(1, nl.size());
  }
  
  // Ensure <time>...</time> gets parsed as an HtmlTags.
  // composite tag, not as the default sequence of TagNodes
  public void testTimeTag() throws IOException {
    String in = "<time class=\"special\"><i>iii</i></time>";
    MockHtmlTransform xform =
        new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
        new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Time);
    assertEquals(1, nl.size());
  }
  
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

  // Ensure <center>...</center> gets parsed as an HtmlTags.Center
  // composite tag, not as the default sequence of TagNodes
  public void testCenterTag() throws IOException {
    String in = "<center><i>iii</i></center>";
    MockHtmlTransform xform =
      new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins =
      new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(node instanceof HtmlTags.Center);
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
