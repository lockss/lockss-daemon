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

package org.lockss.plugin.pubfactory;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestPubFactoryHtmlHashFilterFactory extends LockssTestCase {
  private PubFactoryHtmlHashFilterFactory fact;
  private MockArchivalUnit bau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new PubFactoryHtmlHashFilterFactory();
  }
  
  private void doFilterTest(ArchivalUnit au, 
      FilterFactory fact, String nameToHash, String expectedStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);

      assertEquals(expectedStr, StringUtil.fromInputStream(actIn));

  }
  
  private static final String blockHtml =
		  "<div class=\"content-box\"><div class=\"mainBase\" id=\"mainContent\">" +
				  "  <div id=\"readPanel\">" + 
				  "    <a class=\"summary-toggle ico-summary js-summary-toggle phoneOnly\" href=\"#\"><span>Show Summary Details</span></a>\n" + 
				  "  </div>" +
				  "</div></div>" +
          "<div id=\"headerWrap\">headerWrap content</div>";

  private static final String blockFiltered =
    "<div class=\"content-box\"><div class=\"mainBase\" id=\"mainContent\">" +
        "  <div id=\"readPanel\">" +
        "    <a class=\"summary-toggle ico-summary js-summary-toggle phoneOnly\" href=\"#\"><span>Show Summary Details</span></a>\n" +
        "  </div>" +
        "</div></div>";

  private static final String hTags =
    "<h2 class=\"abstractTitle text-title my-1\" id=\"d3038e2\">Abstract</h2>" +
    "<h3 id=\"d4951423e445\">a. Satellite data</h3>" +
    "<h4 id=\"d4951423e1002\">On what scale does lightning enhancement occur?</h4>" +
    "<h5 id=\"h79834098f6464\">Does this even happen?</h5>" +
    "<h8 id=\"j6984981a43585\">This one can't</h8>";

  private static final String filteredHTags =
    "<h2 class=\"abstractTitle text-title my-1\" >Abstract</h2>" +
    "<h3 >a. Satellite data</h3>" +
    "<h4 >On what scale does lightning enhancement occur?</h4>" +
    "<h5 >Does this even happen?</h5>" +
    "<h8 >This one can't</h8>";

  private static final String dataPopoverAttr1 =
    "<div data-popover-fullscreen=\"false\" data-popover-placement=\"\" data-popover-breakpoints=\"\" data-popover=\"607a919f-a0fd-41c2-9100-deaaff9a0862\" class=\"position-absolute display-none\">" +
    "    <p>Some text</p>" +
    "</div>";

  private static final String filteredDataPopoverAttr1 =
    "<div data-popover-fullscreen=\"false\" data-popover-placement=\"\" data-popover-breakpoints=\"\"  class=\"position-absolute display-none\">" +
    "    <p>Some text</p>" +
    "</div>";

  private static final String dataPopoverAttr2 =
    "<button data-popover-anchor=\"0979a884-7df8-4d05-a54\">Button Content</button>";

  private static final String filteredDataPopoverAttr2 =
    "<button >Button Content</button>";



  public void testFiltering() throws Exception {
	    doFilterTest(bau, fact, blockHtml, blockFiltered);
  }

  public void testHTagsFiltering() throws Exception {
    doFilterTest(bau, fact, hTags, filteredHTags);
  }
  public void testDivPopOverFiltering() throws Exception {
    doFilterTest(bau, fact, dataPopoverAttr1, filteredDataPopoverAttr1);
  }
  public void testButtonPopOverFiltering() throws Exception {
    doFilterTest(bau, fact, dataPopoverAttr2, filteredDataPopoverAttr2);
  }
}
