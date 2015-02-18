/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.util;
import org.lockss.test.*;


public class TestHeaderUtil extends LockssTestCase {

  public void testGetMimeTypeFromContentType() {
    assertNull(HeaderUtil.getMimeTypeFromContentType(null));
    assertEquals("text/html",
		 HeaderUtil.getMimeTypeFromContentType("text/html"));
    assertEquals("text/html",
		 HeaderUtil.getMimeTypeFromContentType(" Text/Html "));
    assertEquals("text/html",
		 HeaderUtil.getMimeTypeFromContentType("TEXT/HTML ; charset=foo"));
    assertEquals("text/html",
		 HeaderUtil.getMimeTypeFromContentType(" text/html ; charset=foo"));
    assertEquals("application/binary",
		 HeaderUtil.getMimeTypeFromContentType("Application/Binary; charset=foo"));
    assertSame(HeaderUtil.getMimeTypeFromContentType(" Text/Html "),
	       HeaderUtil.getMimeTypeFromContentType(" Text/Html "));
  }

  public void testGetCharsetFromContentType() {
    assertNull(HeaderUtil.getCharsetFromContentType(null));
    assertNull(HeaderUtil.getCharsetFromContentType("text/html"));
    assertNull(HeaderUtil.getCharsetFromContentType("text/html;"));
    assertNull(HeaderUtil.getCharsetFromContentType("text/html;foobar"));
    assertNull(HeaderUtil.getCharsetFromContentType("text/html; foobar"));
    assertNull(HeaderUtil.getCharsetFromContentType("text/html;charset"));
    assertNull(HeaderUtil.getCharsetFromContentType("text/html; charset"));
    assertEquals("utf-8",
		 HeaderUtil.getCharsetFromContentType("text/html;charset=UTF-8"));
    assertEquals("utf-8",
                 HeaderUtil.getCharsetFromContentType("text/html; charset=UTF-8"));
    assertEquals("utf-8",
		 HeaderUtil.getCharsetFromContentType("text/html;CHARSET=UTF-8"));
    assertEquals("utf-8",
                 HeaderUtil.getCharsetFromContentType("text/html; CHARSET=UTF-8"));
    assertEquals("iso8859-1",
		 HeaderUtil.getCharsetFromContentType("text/html;charset=\"iso8859-1\""));
    assertEquals("iso8859-1",
                 HeaderUtil.getCharsetFromContentType("text/html; charset=\"iso8859-1\""));
    assertEquals("foo-1",
                 HeaderUtil.getCharsetFromContentType("text/html;charset=foo-1;other=stuff"));
    assertEquals("foo-1",
                 HeaderUtil.getCharsetFromContentType("text/html; charset=foo-1; other=stuff"));
    assertEquals("foo-1",
		 HeaderUtil.getCharsetFromContentType("text/html;charset=\"foo-1\";other=stuff"));
    assertEquals("foo-1",
                 HeaderUtil.getCharsetFromContentType("text/html; charset=\"foo-1\"; other=stuff"));
    assertSame(HeaderUtil.getCharsetFromContentType("text/html;charset=\"iso8859-1\""),
	       HeaderUtil.getCharsetFromContentType("text/html;charset=\"iso8859-1\""));
    assertSame(HeaderUtil.getCharsetFromContentType("text/html; charset=\"iso8859-1\""),
               HeaderUtil.getCharsetFromContentType("text/html; charset=\"iso8859-1\""));
  }

  String gcodfct(String ctype) {
    return HeaderUtil.getCharsetOrDefaultFromContentType(ctype);
  }

  public void testGetCharsetOrDefaultFromContentType() {
    String DEF = Constants.DEFAULT_ENCODING;
    assertEquals(DEF, gcodfct(null));
    assertEquals(DEF, gcodfct("text/html"));
    assertEquals(DEF, gcodfct("text/html;"));
    assertEquals(DEF, gcodfct("text/html;foobar"));
    assertEquals(DEF, gcodfct("text/html;charset"));
    assertEquals("utf-8", gcodfct("text/html;charset=UTF-8"));
    assertEquals("utf-8", gcodfct("text/html;CHARSET=UTF-8"));
    assertEquals("iso8859-1", gcodfct("text/html;charset=\"iso8859-1\""));
    assertEquals("foo-1", gcodfct("text/html;charset=\"foo-1\";other=stuff"));
    assertEquals("foo-1", gcodfct("text/html;charset=foo-1;other=stuff"));
    assertSame(gcodfct("text/html;charset=\"iso8859-1\""),
	       gcodfct("text/html;charset=\"iso8859-1\""));
  }

  public void testIsTokenChar() {
    assertTrue(HeaderUtil.isTokenChar('a'));
    assertTrue(HeaderUtil.isTokenChar('z'));
    assertTrue(HeaderUtil.isTokenChar('A'));
    assertTrue(HeaderUtil.isTokenChar('Z'));
    assertTrue(HeaderUtil.isTokenChar('0'));
    assertTrue(HeaderUtil.isTokenChar('9'));
    assertTrue(HeaderUtil.isTokenChar('_'));
    assertTrue(HeaderUtil.isTokenChar('.'));
    assertTrue(HeaderUtil.isTokenChar('$'));
    assertTrue(HeaderUtil.isTokenChar('&'));
    assertTrue(HeaderUtil.isTokenChar('*'));
    assertFalse(HeaderUtil.isTokenChar('('));
    assertFalse(HeaderUtil.isTokenChar(')'));
    assertFalse(HeaderUtil.isTokenChar('<'));
    assertFalse(HeaderUtil.isTokenChar('>'));
    assertFalse(HeaderUtil.isTokenChar('@'));
    assertFalse(HeaderUtil.isTokenChar(','));
    assertFalse(HeaderUtil.isTokenChar(';'));
    assertFalse(HeaderUtil.isTokenChar(':'));
    assertFalse(HeaderUtil.isTokenChar('\\'));
    assertFalse(HeaderUtil.isTokenChar('\"'));
    assertFalse(HeaderUtil.isTokenChar('/'));
    assertFalse(HeaderUtil.isTokenChar('['));
    assertFalse(HeaderUtil.isTokenChar(']'));
    assertFalse(HeaderUtil.isTokenChar('?'));
    assertFalse(HeaderUtil.isTokenChar('='));
    assertFalse(HeaderUtil.isTokenChar('{'));
    assertFalse(HeaderUtil.isTokenChar('}'));
    assertFalse(HeaderUtil.isTokenChar(' '));
    assertFalse(HeaderUtil.isTokenChar('\t'));
  }

  public void testIsCachableContentType() {
    assertFalse(HeaderUtil.isCachableContentType(null));
    assertFalse(HeaderUtil.isCachableContentType("foo=bar"));
    assertTrue(HeaderUtil.isCachableContentType("text/html"));
    assertTrue(HeaderUtil.isCachableContentType("text/html;charset"));
    assertTrue(HeaderUtil.isCachableContentType("text/html;charset=utf-8"));
    assertTrue(HeaderUtil.isCachableContentType("text/html;CHARSET=utf-16"));
    assertFalse(HeaderUtil.isCachableContentType("text/html;xcharset=utf-8"));
    assertFalse(HeaderUtil.isCachableContentType("text/html;charsety=utf-8"));
    assertFalse(HeaderUtil.isCachableContentType("text/html;charset=utf-8;name=foo"));
  }

  public void testIsEarlier() throws Exception {
    String t1 = "Wed, 17 Sep 2008 18:24:58 GMT";
    String t2 = "Thu, 18 Sep 2008 18:24:58 GMT";
    assertFalse(HeaderUtil.isEarlier(t1, t1));
    assertFalse(HeaderUtil.isEarlier(t1, new String(t1)));
    assertTrue(HeaderUtil.isEarlier(t1, t2));
    assertFalse(HeaderUtil.isEarlier(t2, t1));
  }

  public void testEarlierLater() throws Exception {
    String t1 = "Wed, 17 Sep 2008 18:24:58 GMT";
    String t2 = "Thu, 18 Sep 2008 18:24:58 GMT";

    assertSame(t1, HeaderUtil.earlier(t1, t1));
    assertSame(t1, HeaderUtil.earlier(t1, t2));
    assertSame(t1, HeaderUtil.earlier(t2, t1));

    assertSame(t1, HeaderUtil.earlier(t1, null));
    assertSame(t2, HeaderUtil.earlier(null, t2));
    assertSame(null, HeaderUtil.earlier(null, null));

    assertSame(t1, HeaderUtil.earlier(t1, ""));
    assertSame(t2, HeaderUtil.earlier("", t2));
    assertSame(null, HeaderUtil.earlier("", ""));

    assertSame(t1, HeaderUtil.later(t1, t1));
    assertSame(t2, HeaderUtil.later(t1, t2));
    assertSame(t2, HeaderUtil.later(t2, t1));

    assertSame(t1, HeaderUtil.later(t1, null));
    assertSame(t2, HeaderUtil.later(null, t2));
    assertSame(null, HeaderUtil.later(null, null));

    assertSame(t1, HeaderUtil.later(t1, ""));
    assertSame(t2, HeaderUtil.later("", t2));
    assertSame(null, HeaderUtil.later("", ""));

  }
}
