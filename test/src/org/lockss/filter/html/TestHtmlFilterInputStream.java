/*
 * $Id$
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

package org.lockss.filter.html;

import java.io.*;
import java.util.*;

import org.lockss.filter.html.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.htmlparser.*;
import org.htmlparser.util.*;
import org.htmlparser.filters.*;

public class TestHtmlFilterInputStream extends LockssTestCase {

  /** Check that the filtered string matches expected. */
  private void assertFilterString(String expected, String input,
				  HtmlTransform xform)
      throws IOException {
    assertFilterString(expected, input, null, null, null, xform);
  }

  private void assertFilterString(String expected, String input,
				  String strCharset, String inCharset,
				  String outCharset, HtmlTransform xform)
      throws IOException {

    InputStream in = (strCharset == null)
      ? new StringInputStream(input)
      : new ReaderInputStream(new StringReader(input), strCharset);
    InputStream filt =
      new HtmlFilterInputStream(in, inCharset, outCharset, xform);
    if (strCharset != null) {
      assertInputStreamMatchesString(expected, filt, strCharset);
    } else {
      assertInputStreamMatchesString(expected, filt);
    }
    assertEquals(-1, filt.read());
    filt.close();
    try {
      filt.read();
      fail("closed InputStream should throw");
    } catch (IOException e) {}
  }

  private void assertIdentityXform(String expected, String input)
      throws IOException {
    assertFilterString(expected, input, new IdentityXform());
  }

  private void assertIdentityXform(String expected,
				   String input, String strCharset,
				   String inCharset, String outCharset)
      throws IOException {
    assertFilterString(expected, input, strCharset, inCharset, outCharset,
		       new IdentityXform());
  }

  public void testIll() {
    try {
      new HtmlFilterInputStream(null, new IdentityXform ());
      fail("null InputStream should throw");
    } catch(IllegalArgumentException iae) {
    }
    try {
      new HtmlFilterInputStream(new StringInputStream("blah"), null);
      fail("null xform should throw");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testIdentityXform() throws IOException {
    assertFilterString("<html>foo</html>",
		       "<html>foo</html>",
		       new IdentityXform());
  }

  public void testEmpty() throws IOException {
    assertFilterString("", "", new IdentityXform());

    MockHtmlTransform xform =
      new MockHtmlTransform(ListUtil.list(new NodeList()));
    assertFilterString("", "", xform);
    assertEquals(0, xform.getArg(0).size());
  }

  NodeList parse(String in) throws Exception {
    Parser p = ParserUtils.createParserParsingAnInputString(in);
    return p.parse(null);
  }

  public void testXform() throws Exception {
    String in = "<b>bold</b>";
    NodeList out = parse("<i>it</i>");
    MockHtmlTransform xform = new MockHtmlTransform(ListUtil.list(out));
    assertFilterString("<i>it</i>", in, xform);
    NodeList nl = xform.getArg(0);
    assertEquals(3, nl.size());
    assertEquals("<b>", nl.elementAt(0).toHtml());
    assertEquals("bold", nl.elementAt(1).toHtml());
    assertEquals("</b>", nl.elementAt(2).toHtml());
  }

  public void testUnclosed1() throws IOException {
    String in = "<HTML><BODY>" +
      "<ul><li>l1<li>l2<div>text1</ul>tween" +
      "<ul><li>l3<li>l4<div><script>text2</ul>" +
      "</body></html>";
    String exp = "<HTML><BODY>" +
      "<ul><li>l1</li><li>l2<div>text1</div></li></ul>tween" +
      "<ul><li>l3</li><li>l4<div><script>text2</script></div></li></ul>" +
      "</body></html>";
    assertIdentityXform(in, in);
    ConfigurationUtil.setFromArgs(HtmlFilterInputStream.PARAM_VERBATIM,
				  "false");
    assertIdentityXform(exp, in);
  }

  public void testUnclosed2() throws IOException {
    String in = "<HTML><BODY>" +
      "<dl><dt>t1<dd>d1<div>text1</dl>" +
      "<dl><dt>t2<dd>d2<div><script>text2</dl>" +
      "</body></html>";
    String exp = "<HTML><BODY>" +
      "<dl><dt>t1</dt><dd>d1<div>text1</div></dd></dl>" +
      "<dl><dt>t2</dt><dd>d2<div><script>text2</script></div></dd></dl>" +
      "</body></html>";
    assertIdentityXform(in, in);
    ConfigurationUtil.setFromArgs(HtmlFilterInputStream.PARAM_VERBATIM,
				  "false");
    assertIdentityXform(exp, in);
  }

  public void testCharset() throws Exception {
    String in1 = "<html><body>" +
      "abc\u00e91234" +
      "</body></html>";
    String exp1 = "<html><body>" +
      "abc\u00e91234" +
      "</body></html>";
    String exp1U = "<html><body>" +
      "abc\u00fd1234" +
      "</body></html>";
    String in2 = "<html><body>" +
      "abc\u2260" +
      "</body></html>";
    String exp2 = "<html><body>" +
      "abc\u2260" +
      "</body></html>";
    String exp28 = "<html><body>" +
      "abc\u0060" +
      "</body></html>";
    assertIdentityXform(exp1, in1);
    assertIdentityXform(exp1, in1, "ISO-8859-1", "ISO-8859-1", "ISO-8859-1");
    assertIdentityXform(exp1, in1, "ISO-8859-1", "ISO-8859-1", null);
    assertIdentityXform(exp1U, in1, "ISO-8859-1", "UTF-8", null);
    assertIdentityXform(exp2, in2, "UTF-8", "UTF-8", "UTF-8");
    assertIdentityXform(exp28, in2);
    assertIdentityXform(exp28, in2, "UTF-8", null, null);
    assertIdentityXform(exp28, in2, "UTF-8", "UTF-8", null);
  }

  public void testChangeCharsetFailsIfNoMark() throws Exception {
    ConfigurationUtil.setFromArgs(HtmlFilterInputStream.PARAM_MARK_SIZE, "0");
    log.info("read(): exception following is expected");
    try {
      doParseCharset();
      fail("parser should fail to reset() input stream if not mark()ed");
    } catch (IOException e) {
    }
  }

  // Test default mark size
  public void testChangeCharset() throws Exception {
    doParseCharset();
  }

  void doParseCharset() throws Exception {
    String file = "rewind-test.txt";
    java.net.URL url = getClass().getResource(file);
    assertNotNull(file + " missing.", url);
    InputStream in = null;
    InputStream expin = null;
    try {
      in = UrlUtil.openInputStream(url.toString());
      assertNotNull(in);
      in = new BufferedInputStream(in);
      expin = UrlUtil.openInputStream(url.toString());
      Reader rdr = new InputStreamReader(expin, "iso-8859-1");
      String exp = StringUtil.fromReader(rdr);
      Reader filt = StringUtil.getLineReader(new HtmlFilterInputStream(in, new IdentityXform()));
      assertReaderMatchesString(exp, filt);
    } finally {
      IOUtil.safeClose(in);
      IOUtil.safeClose(expin);
    }
  }


  class IdentityXform implements HtmlTransform {
    public NodeList transform(NodeList nl) {
      return nl;
    }
  }
}
