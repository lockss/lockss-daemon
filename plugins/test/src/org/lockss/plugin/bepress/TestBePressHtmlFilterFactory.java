/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bepress;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.MetadataField;
import org.lockss.test.*;

public class TestBePressHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private BePressHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BePressHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }
  //
  private static final String HtmlHashA =
   "<div id='custom-fields'>" +
   "<p>The above text, published by Hello University Press &copy;Hello" +
   "University, has been downloaded 2370 times as of 04/07/14. "+
   "Note: the download counts of the journal's material are since Issue 9.1 "+
   "(March 2007), since the journal's format in pdf (instead of in html 1999-2007).</p>"+
   "</div>Hello World";

  private static final String HtmlHashAFiltered =
    "Hello World";

  //
  private static final String HtmlHashB =
    "<div id='doi' class='element'>"+
    "<div class='altmetric-embed' data-doi=\"10.7771/1481-4374.1211\"></div>"+
    "<script type=\"text/javascript\" src=\"http://cdn.altmetric.com/embed.min.js\"></script>"+
    "</div> Hello World";

  private static final String HtmlHashBFiltered =
    " Hello World";

  public void testFilterA() throws Exception {
    InputStream inA;

    /* viewed-by test  */
    inA = fact.createFilteredInputStream(mau,
          new StringInputStream(HtmlHashA), ENC);
    String filtStrA = StringUtil.fromInputStream(inA);

    assertEquals(HtmlHashAFiltered, filtStrA);

  }

  public void testFilterB() throws Exception {
    InputStream inB;

    inB = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlHashB), ENC);
    String filtStrB = StringUtil.fromInputStream(inB);
    assertEquals(HtmlHashBFiltered, filtStrB);

  }

}
