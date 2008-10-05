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
    InputStream in =
      new HtmlFilterInputStream(new StringInputStream(input), xform);
    assertInputStreamMatchesString(expected, in);
    assertEquals(-1, in.read());
    in.close();
    try {
      in.read();
      fail("closed InputStream should throw");
    } catch (IOException e) {}
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

  public void testCharsetFailsIfNoMark() throws Exception {
    ConfigurationUtil.setFromArgs(HtmlFilterInputStream.PARAM_MARK_SIZE, "0"); 
    log.info("read(): exception following is expected");
    try {
      doParseCharset();
      fail("parser should fail to reset() input stream if not mark()ed");
    } catch (IOException e) {
    }
  }

  public void testCharset() throws Exception {
    ConfigurationUtil.setFromArgs(HtmlFilterInputStream.PARAM_MARK_SIZE,
				  "20000"); 
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
      InputStream filt = new HtmlFilterInputStream(in, new IdentityXform());
      assertInputStreamMatchesString(exp, filt);
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
