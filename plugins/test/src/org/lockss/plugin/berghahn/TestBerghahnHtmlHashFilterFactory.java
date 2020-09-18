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

import org.apache.commons.io.FileUtils;
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
  
  private static final String tocBlockHtml = 
		"";
  private static final String tocBlockFiltered = "";
  
  private static final String blockHtml =
		  "<div class=\"mainBase\" id=\"mainContent\">" + 
				  "  <div id=\"readPanel\">" + 
				  "    <a class=\"summary-toggle ico-summary js-summary-toggle phoneOnly\" href=\"#\"><span>Show Summary Details</span></a>\n" + 
				  "  </div>" +
				  "</div>" +
                  "<div id=\"headerWrap\">headerWrap content</div>";
  private static final String blockFiltered =
		  "<div class=\"mainBase\" id=\"mainContent\">  <div id=\"readPanel\">    <a class=\"summary-toggle ico-summary js-summary-toggle phoneOnly\" href=\"#\"><span>Show Summary Details</span></a>\n" +
                  "  </div></div>";

  public void testFiltering() throws Exception {
	    doFilterTest(bau, fact, blockHtml, blockFiltered);
  }

}
