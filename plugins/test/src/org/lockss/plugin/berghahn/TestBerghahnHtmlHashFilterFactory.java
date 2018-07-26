/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.berghahn;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestBerghahnHtmlHashFilterFactory extends LockssTestCase {
  private BerghahnHtmlHashFilterFactory fact;
  private MockArchivalUnit bau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BerghahnHtmlHashFilterFactory();
  }
  
  private void doFilterTest(ArchivalUnit au, 
      FilterFactory fact, String nameToHash, String expectedStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);
    assertEquals(expectedStr, StringUtil.fromInputStream(actIn));
  }
  
  private static final String manifestHtml =
		  "<div class=\"column\">" + 
				  "<h1>Boyhood Studies Volume 10 LOCKSS Manifest Page</h1>" + 
				  "<div>" + 
				  "<li>" + 
				  "<a href=\"/view/journals/foo/10/2/foo.10.issue-2.xml\">Issue 10/2</a>" + 
				  "</li>" + 
				  "<li>" + 
				  "<a href=\"/view/journals/foo/10/1/foo.10.issue-1.xml\">Issue 10/1</a>" + 
				  "</li>" + 
				  "</div>" + 
				  "<div>LOCKSS system has permission to collect, preserve, and serve this Archival Unit</div>" + 
				  "<br/>" + 
				  "<div>CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit</div>" + 
				  "</div>";
  private static final String manifestFiltered =
				  "<a href=\"/view/journals/foo/10/2/foo.10.issue-2.xml\">Issue 10/2</a>" + 
				  "<a href=\"/view/journals/foo/10/1/foo.10.issue-1.xml\">Issue 10/1</a>"; 
  
  //placeholder - currently article and toc using same include block
  private static final String tocBlockHtml = 
		"";
  private static final String tocBlockFiltered = "";
  
  private static final String artBlockHtml = 
		  "<div class=\"mainBase\" id=\"mainContent\">" + 
				  "  <div id=\"readPanel\">" + 
				  "    <a class=\"summary-toggle ico-summary js-summary-toggle phoneOnly\" href=\"#\"><span>Show Summary Details</span></a>\n" + 
				  "  </div>" +
				  "</div>";
  private static final String artBlockFiltered = 
		  "<div id=\"readPanel\">" + 
				  "    <a class=\"summary-toggle ico-summary js-summary-toggle phoneOnly\" href=\"#\"><span>Show Summary Details</span></a>\n" + 
				  "  </div>";
  
  private static final String citOverlayHtml =
      "<div id=\"previewWrapper\">" +
      "<h2>Preview Citation</h2>" +
                    "<p style=\"\" class=\"citationPreview\" id=\"apa_book\">" +
                        "Borovnik, M. (2017). Nighttime Navigating, <em>Transfers</em>, <em>7</em>(3), 38-55.  Retrieved Jul 26, 2018, from " +
                    "<a target=\"_blank\" href=\"https://www.berghahnjournals.com/view/journals/transfers/7/3/trans070305.xml\">" +
                    "https://www.berghahnjournals.com/view/journals/transfers/7/3/trans070305.xml</a>" +
                    "</p></div>";
  
  private static final String citOverlayFiltered=
      "<div id=\"previewWrapper\">" +
                        "<p style=\"\" class=\"citationPreview\" id=\"apa_book\">" +
                        "<a target=\"_blank\" href=\"https://www.berghahnjournals.com/view/journals/transfers/7/3/trans070305.xml\">" +
                        "https://www.berghahnjournals.com/view/journals/transfers/7/3/trans070305.xml</a>" +
                        "</p></div>";
  public void testFiltering() throws Exception {
	    doFilterTest(bau, fact, manifestHtml, manifestFiltered);
	    doFilterTest(bau, fact, tocBlockHtml, tocBlockFiltered);
            doFilterTest(bau, fact, artBlockHtml, artBlockFiltered);
            doFilterTest(bau, fact, citOverlayHtml, citOverlayFiltered);
  }

}
