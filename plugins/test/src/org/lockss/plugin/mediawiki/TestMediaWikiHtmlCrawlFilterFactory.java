/*
 * $Id$
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

package org.lockss.plugin.mediawiki;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestMediaWikiHtmlCrawlFilterFactory extends LockssTestCase {
	private static final String ENC = Constants.DEFAULT_ENCODING;
	private MediaWikiHtmlCrawlFilterFactory fact;

	public void setUp() throws Exception {
		super.setUp();
		fact = new MediaWikiHtmlCrawlFilterFactory();
	}
 
	private static final String footerInfoHtml =
		"<!-- footer -->" +
		"<div id=\"footer\" role=\"contentinfo\">" +
		" 	<ul id=\"footer-info\">" +
		" 		<li id=\"footer-info-lastmod\"> This page was last modified on 9 December 2013, at 08:53.</li>" +
		"	</ul>" +
		"	<div style=\"clear:both\"></div>" +
		"</div>" +
		"<!-- /footer -->";
	private static final String footerInfoFiltered =
		"<!-- footer -->" +
		"<!-- /footer -->";
	
	private static final String navHtml =
		"<!-- header -->" +
		"<div id=\"mw-navigation\">" +
		"	<h2>Navigation menu</h2>" +
		"	<div id=\"mw-head\">" +
		"	<!-- 0 -->" +
		"	<div id=\"p-personal\" role=\"navigation\" class=\"\">" +
		"		<h3>Personal tools</h3>" +
		"	</div>" +
		"	<!-- /0 -->";

	private static final String navFiltered =
		"<!-- header -->";

	public void testFiltering() throws Exception {
		InputStream inA;
	    
	    inA = fact.createFilteredInputStream(null, new StringInputStream(footerInfoHtml),
	        ENC);
	    assertEquals(footerInfoFiltered, StringUtil.fromInputStream(inA));
	    
	    inA = fact.createFilteredInputStream(null, new StringInputStream(navHtml),
		        ENC);
		assertEquals(navFiltered, StringUtil.fromInputStream(inA)); 
    }
}