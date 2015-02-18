/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"}, to deal
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

package org.lockss.plugin.psychiatryonline;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestPsychiatryOnlineHtmlFilterFactory extends LockssTestCase {
  private PsychiatryOnlineHtmlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new PsychiatryOnlineHtmlFilterFactory();
  }
  
  // block tags from PsychiatryOnlineHtmlFilterFactory
  String blockIds[][] = new String[][] {
      // only tests the constructed tag rather than actual example from page
      {"span", "id", "lblSeeAlso"},
  };
  
  // single tags from PsychiatryOnlineHtmlFilterFactory
  String[][] tagIds = new String[][] {
      {"input", "type", "hidden"},
  };
  
  public void testTagFiltering() throws Exception {
    // common filtered html results
    String filteredHtml = "" +
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"> "
        + "<html lang=\"en\"> <body>\n"
        + "\n</body>\n</html>\n\n";
    
    // html for block tags
    String blockHtml = "" +
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"> "
        + "<html lang=\"en\"> <body>\n"
        + "<%s %s=\"%s\">\n"
        + "chicken chicken chicken...\n"
        + "</%s>"
        + "\n</body>\n</html>\n\n";
    
    // test block tag ID filtering
    for (String[] id : blockIds) {
      InputStream htmlIn = fact.createFilteredInputStream(mau,
          new StringInputStream(String.format(blockHtml, id[0],id[1],id[2],id[0])),
          Constants.DEFAULT_ENCODING);
      assertEquals(filteredHtml, StringUtil.fromInputStream(htmlIn));
    }
  }
  //test meta tag with explicit tests
  private static final String MetaTagHtml = "" +
      "<p>The chickens were decidedly cold." +
      "<META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-16\">" +
      "</p>";
  
  private static final String MetaTagHtmlFiltered =
      "<p>The chickens were decidedly cold.</p>";
  
  public void testMetaTag() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(MetaTagHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(MetaTagHtmlFiltered,StringUtil.fromInputStream(inA));
  }
  
}
