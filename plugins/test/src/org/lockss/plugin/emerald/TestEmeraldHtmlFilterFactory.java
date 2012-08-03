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

package org.lockss.plugin.emerald;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.test.*;

public class TestEmeraldHtmlFilterFactory extends LockssTestCase {
  private EmeraldHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new EmeraldHtmlFilterFactory();
  }
  
  //Has number of article download in row
  //HtmlNodeFilters.tagWithAttribute("td", "headers", "tocopy"),
  //Has users can also download list
  //HtmlNodeFilters.tagWithAttribute("td", "headers", "releatedlist")
  
  private static final String toCopyHtml =
      "<td headers=\"tocopy\">The fulltext of this document has been download a whole bunch of times</td>";
  private static final String toCopyHtmlFiltered =
      "";
  
  private static final String releatedHtml =
      "<td valign=\"top\" colspan=\"2\" headers=\"releatedlist\" scope=\"row\">" +
      "<li>Journal Title" +
      "<br>" +
      "http://journal.org" +
      "<br>" +
      "</li>" +
      "</td>";
  private static final String releatedHtmlFiltered =
      "";
  
  private static final String printedHtml =
      "<p>Printed from: http://www.printers.com on Friday the 13th, 2012 copyright Emerald Group Publishing Limited</p>";
  private static final String printedHtmlFiltered =
      "";
  
  private static final String whiteSpaceHtml =
      "<body>" +
         "\n" +
         "\n" +
         "\n" +
      "<div id=\"pgContainer\" class=\"rounded\">                               <div id=\"pgHead\">" +
      "<img src=\"journalcover.gif\" alt=\"Journal cover: Advances in Medecine\" width=\"90\" border=\"0\" align=\"left\" />" +
                      "</div>\n"                 +
                 "</div>\n" +
      "</body>";
  private static final String whiteSpaceHtmlFiltered =
      "<body> <div id=\"pgContainer\" class=\"rounded\"> <div id=\"pgHead\"><img src=\"journalcover.gif\" alt=\"Journal cover: Advances in Medecine\" width=\"90\" border=\"0\" align=\"left\" /></div> </div> </body>";

  
  public void testToCopyFiltering() throws Exception {
	  InputStream actIn = fact.createFilteredInputStream(mau,
			  										     new StringInputStream(toCopyHtml),
			  										     Constants.DEFAULT_ENCODING);
	  
	  assertEquals(toCopyHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testReleatedFiltering() throws Exception {
	  InputStream actIn = fact.createFilteredInputStream(mau,
			  										     new StringInputStream(releatedHtml),
			  										     Constants.DEFAULT_ENCODING);
	  
	  assertEquals(releatedHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testPrintedFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
                                                                                                       new StringInputStream(printedHtml),
                                                                                                       Constants.DEFAULT_ENCODING);
    
    assertEquals(printedHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testWhiteSpaceFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
                                                                                                       new StringInputStream(whiteSpaceHtml),
                                                                                                       Constants.DEFAULT_ENCODING);
    
    assertEquals(whiteSpaceHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
}
