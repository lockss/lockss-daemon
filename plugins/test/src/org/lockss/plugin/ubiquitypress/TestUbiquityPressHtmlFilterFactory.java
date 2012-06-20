/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ubiquitypress;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestUbiquityPressHtmlFilterFactory extends LockssTestCase {
  private UbiquityPressHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new UbiquityPressHtmlFilterFactory();
  }

  // NodeFilter[] filters = new NodeFilter[] {
  //HtmlNodeFilters.tagWithAttribute("div", "id", "rightSidebar"),
  
  private static final String sidebarHtml =
		 "<div id='rightSidebar'></div>";
  private static final String sidebarHtmlFiltered =
  		 "";
  
  private static final String sideStuffBarHtml =
		 "<div id='rightSidebar'><p align='center' class='ads'><a href='www.ads.org/'><img src='ads.gif'/></a></p></div>";
  private static final String sideStuffBarHtmlFiltered =
	  	 "";

  
  public void testSidebarFiltering() throws Exception {
	  InputStream actIn = fact.createFilteredInputStream(mau,
			  										     new StringInputStream(sidebarHtml),
			  										     Constants.DEFAULT_ENCODING);
	  
	  assertEquals(sidebarHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testsideStuffBarFiltering() throws Exception {
	  InputStream actIn = fact.createFilteredInputStream(mau,
				  										 new StringInputStream(sideStuffBarHtml),
				  										 Constants.DEFAULT_ENCODING);
		  
	  assertEquals(sideStuffBarHtmlFiltered, StringUtil.fromInputStream(actIn));
    
  }
  
}
