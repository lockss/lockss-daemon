/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.ListUtil;

public class TestHtmlTags extends LockssTestCase {
  
  public void doTestCompositeTag(Class<?> clazz) throws IOException {
    String tagName = clazz.getSimpleName().toLowerCase();
    String in = String.format("<%s class=\"foo\"><i>iii</i><div id=\"bar\"><b>bbb</b></div></%s>", tagName, tagName);
    MockHtmlTransform xform = new MockHtmlTransform(ListUtil.list(new NodeList()));
    InputStream ins = new HtmlFilterInputStream(new StringInputStream(in), xform);
    assertInputStreamMatchesString("", ins);
    NodeList nl = xform.getArg(0);
    Node node = nl.elementAt(0);
    assertTrue(clazz.isInstance(node));
    assertEquals(1, nl.size());
  }
  
  public void testArticle() throws Exception {
    doTestCompositeTag(HtmlTags.Article.class);
  }
  
  public void testAside() throws Exception {
    doTestCompositeTag(HtmlTags.Aside.class);
  }
  
  public void testAudio() throws Exception {
    doTestCompositeTag(HtmlTags.Audio.class);
  }
  
  public void testButton() throws Exception {
    doTestCompositeTag(HtmlTags.Button.class);
  }
  
  public void testCanvas() throws Exception {
    doTestCompositeTag(HtmlTags.Canvas.class);
  }
  
  public void testCenter() throws Exception {
    doTestCompositeTag(HtmlTags.Center.class);
  }
  
  public void testDataList() throws Exception {
    doTestCompositeTag(HtmlTags.DataList.class);
  }
  
  public void testDetails() throws Exception {
    doTestCompositeTag(HtmlTags.Details.class);
  }
  
  public void testDialog() throws Exception {
    doTestCompositeTag(HtmlTags.Dialog.class);
  }
  
  public void testFigCaption() throws Exception {
    doTestCompositeTag(HtmlTags.FigCaption.class);
  }
  
  public void testFigure() throws Exception {
    doTestCompositeTag(HtmlTags.Figure.class);
  }
  
  public void testFont() throws Exception {
    doTestCompositeTag(HtmlTags.Font.class);
  }
  
  public void testFooter() throws Exception {
    doTestCompositeTag(HtmlTags.Footer.class);
  }
  
  public void testHeader() throws Exception {
    doTestCompositeTag(HtmlTags.Header.class);
  }
  
  public void testIframe() throws Exception {
    doTestCompositeTag(HtmlTags.Iframe.class);
  }

  public void testMain() throws Exception {
    doTestCompositeTag(HtmlTags.Main.class);
  }

  public void testMark() throws Exception {
    doTestCompositeTag(HtmlTags.Mark.class);
  }

  public void testMenu() throws Exception {
    doTestCompositeTag(HtmlTags.Menu.class);
  }

  public void testMenuItem() throws Exception {
    doTestCompositeTag(HtmlTags.MenuItem.class);
  }

  public void testMeter() throws Exception {
    doTestCompositeTag(HtmlTags.Meter.class);
  }

  public void testNav() throws Exception {
    doTestCompositeTag(HtmlTags.Nav.class);
  }

  public void testNoScript() throws Exception {
    doTestCompositeTag(HtmlTags.NoScript.class);
  }

  public void testProgress() throws Exception {
    doTestCompositeTag(HtmlTags.Progress.class);
  }

  public void testSection() throws Exception {
    doTestCompositeTag(HtmlTags.Section.class);
  }

  public void testSummary() throws Exception {
    doTestCompositeTag(HtmlTags.Summary.class);
  }

  public void testTime() throws Exception {
    doTestCompositeTag(HtmlTags.Time.class);
  }

  public void testVideo() throws Exception {
    doTestCompositeTag(HtmlTags.Video.class);
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
