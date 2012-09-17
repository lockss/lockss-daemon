/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

/**
 * This class tests the IngentaJouranlPluginHtmlFilterFactory.
 * @author phil
 *
 */
public class TestIngentaJournalHtmlFilterFactory extends LockssTestCase {
  private IngentaJournalHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new IngentaJournalHtmlFilterFactory();
  }

  // block tags from IngentaJouranlHtmlFilterFactory
  String blockIds[][] = new String[][] {
    // institution-specific subscription link section
    {"div", "id", "subscribe-links"},
    // Filter out <div id="links">...</div>
    {"div", "id", "links"},
    // Filter out <div id="footer">...</div>
    {"div", "id", "footer"},
    // Filter out <div id="top-ad-alignment">...</div>
    {"div", "id", "top-ad-alignment"},
    // Filter out <div id="top-ad">...</div>
    {"div", "id", "top-ad"},
    // Filter out <div id="ident">...</div>
    {"div", "id", "ident"},         
    // Filter out <div id="ad">...</div>
    {"div", "id", "ad"},
    // Filter out <div id="vertical-ad">...</div>
    {"div", "id", "vertical-ad"},      
    // institution-specific subscription link section
    {"div", "id", "subscribe-links"},
    // Filter out <div id="links">...</div>
    {"div", "id", "links"},
    // Filter out <div id="footer">...</div>
    {"div", "id", "footer"},
    // Filter out <div id="top-ad-alignment">...</div>
    {"div", "id", "top-ad-alignment"},
    // Filter out <div id="top-ad">...</div>
    {"div", "id", "top-ad"},
    // Filter out <div id="ident">...</div>
    {"div", "id", "ident"},         
    // Filter out <div id="ad">...</div>
    {"div", "id", "ad"},
    // Filter out <div id="vertical-ad">...</div>
    {"div", "id", "vertical-ad"},
    // Filter out <div class="right-col-download">...</div>
    {"div", "class", "right-col-download"},                                                               
    // Filter out <div id="cart-navbar">...</div>
    {"div", "id", "cart-navbar"},   
    //   // Filter out <div class="heading-macfix article-access-options">...</div>
    //  {"div", "class", "heading-macfix article-access-options"},                                                                           
    // Filter out <div id="baynote-recommendations">...</div>
    {"div", "id", "baynote-recommendations"},
    // Filter out <div id="bookmarks-container">...</div>
    {"div", "id", "bookmarks-container"},   
    // Filter out <div id="llb">...</div>
    {"div", "id", "llb"},   
    // Filter out <a href="...">...</a> where the href value includes "exitTargetId" as a parameter
    {"a", "href", "foo?exitTargetId=bar"},
    {"a", "href", "foo?parm=value&exitTargetId=bar"},
    // Icon on article reference page
    {"span", "class", "access-icon"},
  };
  
  // single tags from IngentaJouranlHtmlFilterFactory
  String[][] tagIds = new String[][] {
    // Filter out <input name="exitTargetId">
    {"input", "name", "exitTargetId"},
  };
  
  public void testTagFiltering() throws Exception {
    // common filtered html results
    String filteredHtml =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"> "
            + "<html lang=\"en\"> <head> <body> "
            + "</body> </html> ";
    
    // html for block tags
    String blockHtml =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
          + "<html lang=\"en\">\n<head>\n<body>\n"
          + "<%s %s=\"%s\">\n"
          + "chicken chicken chicken...\n"
          + "</%s>\n"
          + "</body>\n</html>\n\n\n";

    // test block tag ID filtering
    for (String[] id : blockIds) {
      InputStream htmlIn = fact.createFilteredInputStream(mau,
          new StringInputStream(String.format(blockHtml, id[0],id[1],id[2],id[0])),
          Constants.DEFAULT_ENCODING);
      assertEquals(filteredHtml, StringUtil.fromInputStream(htmlIn));
    }

    // html for single tags
    String tagHtml =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
            + "<html lang=\"en\">\n<head>\n<body>\n"
            + "<%s %s=\"%s\">\n"
            + "</body>\n</html>\n\n\n";

    // test single tag ID filtering
    for (String[] id : tagIds) {
      InputStream htmlIn = fact.createFilteredInputStream(mau,
          new StringInputStream(String.format(tagHtml, id[0],id[1],id[2])),
          Constants.DEFAULT_ENCODING);
      assertEquals(filteredHtml, StringUtil.fromInputStream(htmlIn));
    }
  }
}
