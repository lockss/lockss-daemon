/*
 * $Id: TestBasicBloggerFilterRule.java,v 1.1 2007-07-18 20:58:01 thib_gc Exp $
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

package org.lockss.plugin.blogger;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.LockssTestCase;

public class TestBasicBloggerFilterRule extends LockssTestCase {
  private BasicBloggerFilterRule rule;

  private static final String filterStart = "<!--LOCKSS ignore start-->";
  private static final String filterEnd = "<!--LOCKSS ignore end-->";


  public void setUp() throws Exception {
    super.setUp();
    rule = new BasicBloggerFilterRule();
  }

  public void testFiltering() throws IOException {
    String content =
      "_WidgetManager._Init('http://www.blogger.com/rearrange?blogID=4503292949532760618', 'http://blog.dshr.org/2007/05/format-obsolescence-prostate-cancer-of.html','4503292949532760618');" +
      "\n" +
      "_WidgetManager._SetPageActionUrl('http://www.blogger.com/display?blogID=4503292949532760618', 'p61jykdrAi7fZES-n9XNj7aGLR8=:1180307387972');" +
      "\n" +
      "_WidgetManager._SetDataContext(...)";
    String expectedContent =
      "_WidgetManager._Init('http://www.blogger.com/rearrange?blogID=4503292949532760618', 'http://blog.dshr.org/2007/05/format-obsolescence-prostate-cancer-of.html','4503292949532760618');" +
      " " +
      "_WidgetManager._SetDataContext(...)";

    Reader reader = rule.createFilteredReader(new StringReader(content));
    assertEquals(expectedContent, StringUtil.fromReader(reader));
  }
}
