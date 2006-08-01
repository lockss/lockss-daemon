/*
 * $Id: TestBlackwellHtmlFilterRule.java,v 1.1 2006-08-01 05:21:51 tlipkis Exp $
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

package org.lockss.plugin.blackwell;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.LockssTestCase;

public class TestBlackwellHtmlFilterRule extends LockssTestCase {

  private BlackwellHtmlFilterRule rule;

  public void setUp() throws Exception {
    super.setUp();
    rule = new BlackwellHtmlFilterRule();
  }

  private static final String in1 = 
    "<center><span class=\"bannerstyle\">\n" +
    "<!-- Institution/Society Banners -->Licensed by Stanford\n" +
    "<!-- Ad Placeholder Id 1017 --><img src=\"/sda/8432/JOPY_synergy.jpg\" width=\"445\" height=\"60\" border=\"0\">\n" +
    "<!-- Ad Placeholder Id 1016 -->\n" +
    "</span></center>\n" +
    "<!-- END REGION 2 -->\n";

  private static final String in2 = 
    "<center><span class=\"bannerstyle\">\n" +
    "<!-- Institution/Society Banners -->Licensed by Generic U.\n" +
    "<!-- Ad Placeholder Id 1017 --><img src=\"/sda/9999/JOPY_synergy.jpg\" width=\"445\" height=\"60\" border=\"0\">\n" +
    "<!-- Ad Placeholder Id 1016 -->\n" +
    "</span></center>\n" +
    "<!-- END REGION 2 -->\n";

  private static final String out = 
    "<center><span class=\"bannerstyle\">\n" +
    "</span></center>\n" +
    "<!-- END REGION 2 -->\n";

  public void testFilter() throws IOException {
    Reader readerA;
    Reader readerB;

    readerA = rule.createFilteredReader(new StringReader(in1));
    assertReaderMatchesString(out, readerA);
    readerB = rule.createFilteredReader(new StringReader(in2));
    assertReaderMatchesString(out, readerB);

    readerA = rule.createFilteredReader(new StringReader(in1));
    readerB = rule.createFilteredReader(new StringReader(in2));
    assertEquals(StringUtil.fromReader(readerA),
                 StringUtil.fromReader(readerB));
  }

}
