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

package org.lockss.plugin.ojs2;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestOJS2HtmlFilterFactory extends LockssTestCase {
  private OJS2HtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new OJS2HtmlFilterFactory();
  }

  //NodeFilter[] filters = new NodeFilter[] {
  // Some OJS sites have a tag cloud
  //HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarKeywordCloud"),
  // Some OJS sites have a subscription status area
 // HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarSubscription"),
  // Popular location for sidebar customizations
  //HtmlNodeFilters.tagWithAttribute("div", "id", "custom"),
  
  private static final String sidebarKeywordCloudHtml =
		 "<div class=\"block\" id=\"sidebarUser\"><div id=\"sidebarKeywordCloud\"></div></div>";
  private static final String sidebarKeywordCloudHtmlFiltered =
  		 "<div class=\"block\" id=\"sidebarUser\"></div>";
  
  private static final String sidebarSubscriptionHtml =
		 "<div id=\"sidebarSubscription\"><a class=\"blockTitle\" href=\"http://pkp.sfu.ca/ojs/\" id=\"developedBy\">Open Journal Systems</a></div>";
  private static final String sidebarSubscriptionHtmlFiltered =
	  	 "";
  
  private static final String customHtml =
			 "<div id=\"header\"><div id=\"custom\"></div></div>";
  private static final String customHtmlFiltered =
	  		 "<div id=\"header\"></div>";

  
  public void testSidebarKeywordCloudFiltering() throws Exception {
	  InputStream actIn = fact.createFilteredInputStream(mau,
			  										     new StringInputStream(sidebarKeywordCloudHtml),
			  										     Constants.DEFAULT_ENCODING);
	  
	  assertEquals(sidebarKeywordCloudHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testSidebarSubscriptionFiltering() throws Exception {
	  InputStream actIn = fact.createFilteredInputStream(mau,
				  										 new StringInputStream(sidebarSubscriptionHtml),
				  										 Constants.DEFAULT_ENCODING);
		  
	  assertEquals(sidebarSubscriptionHtmlFiltered, StringUtil.fromInputStream(actIn));
    
  }
  
  public void testCustomFiltering() throws Exception {
	  InputStream actIn = fact.createFilteredInputStream(mau,
				  										 new StringInputStream(customHtml),
				  										 Constants.DEFAULT_ENCODING);
		  
      assertEquals(customHtmlFiltered, StringUtil.fromInputStream(actIn));
    
  }
  
}
