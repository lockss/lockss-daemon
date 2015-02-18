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

package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.LockssTestCase;

public class TestHighWireFilterRule extends LockssTestCase {

  private HighWireFilterRule rule;

  public void setUp() throws Exception {
    super.setUp();
    rule = new HighWireFilterRule();
  }

  private static final String inst1 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n	<NOBR><STRONG>Institution: Periodicals Department/Lane Library</STRONG></NOBR>\n	<NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">Sign In as Personal Subscriber</A></NOBR>";

  private static final String inst2 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n	<NOBR><STRONG>Institution: Stanford University Libraries</STRONG></NOBR>\n	<NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">Sign In as Personal Subscriber</A></NOBR>";

  private static final String inst3 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n    <NOBR><STRONG>Institution: Stanford University Libraries</STRONG></NOBR>\n      <NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">Sign In as SOMETHING SOMETHING</A></NOBR>";

  public void testFiltering() throws IOException {
    Reader readerA;
    Reader readerB;

    readerA = rule.createFilteredReader(new StringReader(inst1));
    readerB = rule.createFilteredReader(new StringReader(inst2));
    assertEquals(StringUtil.fromReader(readerA),
                 StringUtil.fromReader(readerB));

    readerA = rule.createFilteredReader(new StringReader(inst1));
    readerB = rule.createFilteredReader(new StringReader(inst3));
    assertEquals(StringUtil.fromReader(readerA),
                 StringUtil.fromReader(readerB));
  }

}
