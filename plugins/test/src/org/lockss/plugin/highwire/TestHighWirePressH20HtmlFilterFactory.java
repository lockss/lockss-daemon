/*
 * $Id: TestHighWirePressH20HtmlFilterFactory.java,v 1.4 2012-06-06 02:30:23 kendrayee Exp $
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
import org.lockss.daemon.PluginException;
import org.lockss.test.*;

public class TestHighWirePressH20HtmlFilterFactory extends LockssTestCase {
	static String ENC = Constants.DEFAULT_ENCODING;

	private HighWirePressH20HtmlFilterFactory fact;
	private MockArchivalUnit mau;

	public void setUp() throws Exception {
		super.setUp();
		fact = new HighWirePressH20HtmlFilterFactory();
		mau = new MockArchivalUnit();
	}

	private static final String inst1 = "<div class=\"leaderboard-ads leaderboard-ads-two\"</div>"
			+ "<ul>Fill in SOMETHING SOMETHING</ul>";

	private static final String inst2 = "<ul>Fill in SOMETHING SOMETHING</ul>";

	private static final String withAds = "<div id=\"footer\">"
			+ "<div class=\"block-1\">"
			+ "<div class=\"leaderboard-ads-ft\">"
			+ "<ul>"
			+ "<li><a href=\"com%2FAbout.html\"><img title=\"Advertiser\""
			+ "src=\"http:/adview=true\""
			+ "alt=\"Advertiser\" /></a></li>"
			+ "</ul>"
			+ "</div>"
			+ "<p class=\"disclaimer\">The content of this site is intended for health care professionals</p>"
			+ "<p class=\"copyright\">Copyright © 2012 by "
			+ "The Journal of Rheumatology" + "</p>" + "<ul class=\"issns\">"
			+ "<li><span>Print ISSN: </span>"
			+ "<span class=\"issn\">0315-162X</span></li>"
			+ "<li><span>Online ISSN: </span>"
			+ "<span class=\"issn\">1499-2752</span></li>" + "</ul>" + "</div>"
			+ "<div class=\"block-2 sb-div\"></div>" + "</div>\"";

	private static final String withoutAds = "<div id=\"footer\">"
			+ "<div class=\"block-1\">"
			+ "<p class=\"disclaimer\">The content of this site is intended for health care professionals</p>"
			+ "<p class=\"copyright\">Copyright © 2012 by "
			+ "The Journal of Rheumatology" + "</p>" + "<ul class=\"issns\">"
			+ "<li><span>Print ISSN: </span>"
			+ "<span class=\"issn\">0315-162X</span></li>"
			+ "<li><span>Online ISSN: </span>"
			+ "<span class=\"issn\">1499-2752</span></li>" + "</ul>" + "</div>"
			+ "<div class=\"block-2 sb-div\"></div>" + "</div>\"";

	private static final String withCopyright = "<div id=\"footer\">"
			+ "<div class=\"block-1\">"
			+ "<p class=\"disclaimer\">The content of this site is intended for health care professionals</p>"
			+ "<p class=\"copyright\">Copyright © 2012 by "
			+ "The Journal of Rheumatology" + "</p>" + "<ul class=\"issns\">"
			+ "<li><span>Print ISSN: </span>"
			+ "<span class=\"issn\">0315-162X</span></li>"
			+ "<li><span>Online ISSN: </span>"
			+ "<span class=\"issn\">1499-2752</span></li>" + "</ul>" + "</div>"
			+ "<div class=\"block-2 sb-div\"></div>" + "</div>\"";

	private static final String withoutCopyright = "<div id=\"footer\">"
			+ "<div class=\"block-1\">"
			+ "<p class=\"disclaimer\">The content of this site is intended for health care professionals</p>"
			+ "<ul class=\"issns\">" + "<li><span>Print ISSN: </span>"
			+ "<span class=\"issn\">0315-162X</span></li>"
			+ "<li><span>Online ISSN: </span>"
			+ "<span class=\"issn\">1499-2752</span></li>" + "</ul>" + "</div>"
			+ "<div class=\"block-2 sb-div\"></div>" + "</div>\"";

	private static final String withCurrentIssue = "<div class=\"col-3-top sb-div\"></div>"
			+ "<div class=\"content-box\" id=\"sidebar-current-issue\">"
			+ "<div class=\"cb-contents\">"
			+ "<h3 class=\"cb-contents-header\"><span>Current Issue</span></h3>"
			+ "<div class=\"cb-section\">"
			+ "<ol>"
			+ "<li><span><a href=\"/content/current\" rel=\"current-issue\">May 2012, 39 (5)</a></span></li>"
			+ "</ol>"
			+ "</div>"
			+ "<div class=\"cb-section\">"
			+ "<ol>"
			+ "<div class=\"current-issue\"><a href=\"/content/current\" rel=\"current-issue\"><img src=\"/local/img/sample_cover.gif\" width=\"67\" height=\"89\" alt=\"Current Issue\" /></a></div>"
			+ "</ol>"
			+ "</div>"
			+ "<div class=\"cb-section sidebar-etoc-link\">"
			+ "<ol>"
			+ "<li><a href=\"/cgi/alerts/etoc\">Alert me to new issues of The Journal"
			+ "</a></li>" + "</ol>" + "</div>" + "</div>" + "</div>";

	  private static final String headHtml =
				"<html><head>Title</head></HTML>";
	  
	  private static final String headHtmlFiltered =
				  "<html></HTML>";
	
	private static final String withoutCurrentIssue = "<div class=\"col-3-top sb-div\"></div>";

	public void testFiltering() throws IOException, PluginException {
		assertFilterToSame(inst1, inst2);
		assertFilterToSame(withAds, withoutAds);
	    assertFilterToSame(withCopyright, withoutCopyright);
	    assertFilterToSame(withCurrentIssue, withoutCurrentIssue);
	}
	
	private void assertFilterToSame(String str1, String Str2) throws IOException, PluginException {
		try {
		    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(str1),
							 Constants.DEFAULT_ENCODING);
		    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(Str2),
							 Constants.DEFAULT_ENCODING);
		    assertEquals(StringUtil.fromInputStream(inA),
		                 StringUtil.fromInputStream(inB));
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	  public void testHeadFiltering() throws Exception {
		    InputStream actIn = fact.createFilteredInputStream(mau,
		    												   new StringInputStream(headHtml),
		    												   Constants.DEFAULT_ENCODING);
		    
		    assertEquals(headHtmlFiltered, StringUtil.fromInputStream(actIn));
	  }

}
