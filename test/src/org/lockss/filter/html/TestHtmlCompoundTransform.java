/*
 * $Id$
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
import org.htmlparser.filters.*;

public class TestHtmlCompoundTransform extends LockssTestCase {
  static Logger log = Logger.getLogger("TestHtmlCompoundTransform");

  public void testIll() {
    try {
      HtmlNodeFilterTransform.include(null);
      fail("null filter should throw");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testCompound() throws IOException {
    NodeList in1 = new NodeList();
    NodeList out1 = new NodeList();
    NodeList out2 = new NodeList();
    MockHtmlTransform m1 = new MockHtmlTransform(ListUtil.list(out1));
    MockHtmlTransform m2 = new MockHtmlTransform(ListUtil.list(out2));

    HtmlCompoundTransform xform = new HtmlCompoundTransform(m1, m2);
    NodeList out = xform.transform(in1);
    assertSame(in1, m1.getArg(0));
    assertSame(out1, m2.getArg(0));
    assertSame(out2, out);
  }
}
