/*
 * $Id: TestMetaPressHtmlHashFilterFactory.java,v 1.1.2.2 2012-06-20 00:03:04 nchondros Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.metapress;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.test.*;

public class TestMetaPressHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private MetaPressHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MetaPressHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String inst1 =
    "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlBody\">" +
    "Stanford University Libraries <nobr>(379-06-324)</nobr></div>";
  
  private static final String inst2 =
    "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlBody\">" +
    "Indiana University, Bloomington <nobr>(641-02-015)</nobr></div>";

  public void testFiltering() throws PluginException, IOException {
    InputStream inA;
    InputStream inB;

    inA = fact.createFilteredInputStream(mau,
           new StringInputStream(inst1), ENC);
    inB = fact.createFilteredInputStream(mau,
           new StringInputStream(inst2), ENC);
    assertEquals(StringUtil.fromInputStream(inA),
                 StringUtil.fromInputStream(inB));
  }
}