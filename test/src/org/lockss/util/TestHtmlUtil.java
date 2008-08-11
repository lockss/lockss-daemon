/*
 * $Id: TestHtmlUtil.java,v 1.3 2008-08-11 23:32:18 tlipkis Exp $
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

import junit.framework.TestCase;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.HtmlUtil
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class TestHtmlUtil extends LockssTestCase {

  /** Encode for open html text */
  public static final int ENCODE_TEXT = 1;
  /** Encode for a textarea form field.  */
  public static final int ENCODE_TEXTAREA = 2;
  /** Encode for attr value in "" */
  public static final int ENCODE_QUOTED_ATTR = 3;
  /** Encode for attr value in '' */
  public static final int ENCODE_QUOTED1_ATTR = 4;
  /** Encode for unquoted attr value */
  public static final int ENCODE_ATTR = 5;
  /** Encode for javascript string */
  public static final int ENCODE_JS_STRING = 6;

  private void tstHtmlEncode(int encode) {
    assertEquals("foo", HtmlUtil.encode("foo", encode));
    assertEquals("&lt;", HtmlUtil.encode("<", encode));
    assertEquals("&gt;", HtmlUtil.encode(">", encode));
    assertEquals("&amp;", HtmlUtil.encode("&", encode));
    assertEquals("&quot;", HtmlUtil.encode("\"", encode));
    assertEquals("&lt;bar&amp;&quot;fly&gt;",
		 HtmlUtil.encode("<bar&\"fly>", encode));
    assertEquals("&quot;", HtmlUtil.encode("\"", encode));
    assertEquals("&#1;", HtmlUtil.encode("\001", encode));
    assertEquals("&#31;", HtmlUtil.encode("\037", encode));
    assertEquals("ab&#31;\40\177&#128;&#254;bc&#255;",
		 HtmlUtil.encode("ab\037\040\177\200\376bc\377", encode));
    assertEquals("foo<br>bar", HtmlUtil.encode("foo\nbar", encode));
  }

  public void testEncodeText() {
    tstHtmlEncode(ENCODE_TEXT);
  }

  public void testEncodeTextArea() {
    tstHtmlEncode(ENCODE_TEXTAREA);
  }

  public void testEncodeAttr() {
    tstHtmlEncode(ENCODE_ATTR);
  }

  public void testEncodeJsString() {
    assertEquals("foo\\n", HtmlUtil.encode("foo\r\n", ENCODE_JS_STRING));
  }

  public void testExtractMetaRefreshUrl() {
    assertEquals("http://foo.com/xxx",
		 HtmlUtil.extractMetaRefreshUrl("0; url=http://foo.com/xxx"));
    assertEquals("http://bar.com/xxy",
		 HtmlUtil.extractMetaRefreshUrl("0;url=http://bar.com/xxy"));
    assertEquals("http://bar.com/xxz",
		 HtmlUtil.extractMetaRefreshUrl("5;url=http://bar.com/xxz"));
    assertEquals("http://bar.com/xxz",
		 HtmlUtil.extractMetaRefreshUrl("5 ; url = http://bar.com/xxz"));
  }
}
