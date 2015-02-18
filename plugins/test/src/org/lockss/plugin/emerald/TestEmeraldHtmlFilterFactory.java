/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.InputStream;

import org.lockss.test.*;
import org.lockss.util.*;

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
  
  private static final String whiteSpaceHtml =
      "<html>\n\n" +
      "<body>" +
         "\n" +
         "\n" +
         "\n" +
      "<div id=\"pgContainer\" class=\"rounded\">                               <div id=\"pgHead\">" +
      "<img src=\"journalcover.gif\" alt=\"Journal cover: Advances in Medecine\" width=\"90\" border=\"0\" align=\"left\" />" +
                      "</div>\n"                 +
                 "</div>\n" +
      "</body>\n\n" +
      "</html>";
  private static final String whiteSpaceHtmlFiltered =
      "<html> <body> <div id=\"pgContainer\" class=\"rounded\"> <div id=\"pgHead\"></div> </div> ";
      // Note the trailing space

  private static final String articlePrintTableHtml =
    "<div id=\"printJournalHead\"><h1>Article Title</h1> <table summary=\"" +
    "Article Information: \" class=\"articlePrintTable\"> <caption>Article " +
    "Information:</caption> <tbody> <tr><th scope=\"row\" id=\"references\" " +
    "class=\"column1\" valign=\"top\">References:</th><td header=\"references\" " +
    "class=\"column2\">17</td></tr><tr><th scope=\"row\" id=\"tocite\" " +
    "valign=\"top\" class=\"column1\">To cite this article:</th><td headers=" +
    "\"tocite\"> Author Name, (2012) \"Article Title\", Journal Title, " +
    "Vol. X Iss: X, pp.XX - XX</td></tr><tr><th scope=\"row\" id=\"tocopy\" " +
    "valign=\"top\" class=\"column1\">To copy this article:</th></tr><tr><th " +
    "scope=\"row\" id=\"count\" valign=\"top\" class=\"column1\">Downloads:" +
    "</th></tr><tr><th scope=\"row\" id=\"releatedlist\" valign=\"top\" " +
    "colspan=\"2\">Users who downloaded this case study also downloaded:" +
    "</th></tr><tr></tr></td></tr> </tbody> </table> </div>";
  
  private static final String articlePrintTableHtmlFiltered =
    "";
  
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

  public void testWhiteSpaceFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(whiteSpaceHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(whiteSpaceHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testArticlePrintTableFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(articlePrintTableHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(articlePrintTableHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testNormalizeLinks() throws Exception {
    assertEquals(
        "<a href=\"foo\">bar</a>",
        StringUtil.fromInputStream(fact.createFilteredInputStream(null,
                                                                  new StringInputStream("<a href=\"foo\">bar</a>"),
                                                                  Constants.DEFAULT_ENCODING)));
    assertEquals(
        "<a href=\"foo\">bar</a>",
        StringUtil.fromInputStream(fact.createFilteredInputStream(null,
                                                                  new StringInputStream("<a href= \"foo\">bar</a>"),
                                                                  Constants.DEFAULT_ENCODING)));
    assertEquals(
        "<a >bar</a>", // Note the trailing space
        StringUtil.fromInputStream(fact.createFilteredInputStream(null,
                                                                  new StringInputStream("<a href=\"#foo\">bar</a>"),
                                                                  Constants.DEFAULT_ENCODING)));
  }
  
  public void testNormalizeAbbr() throws Exception {
    assertEquals(
        "<abbr title=\"foo\">bar</abbr>",
        StringUtil.fromInputStream(fact.createFilteredInputStream(null,
                                                                  new StringInputStream("<abbr title=\"foo\">bar</abbr>"),
                                                                  Constants.DEFAULT_ENCODING)));
    assertEquals(
        "<abbr title=\"foo\">bar</abbr>",
        StringUtil.fromInputStream(fact.createFilteredInputStream(null,
                                                                  new StringInputStream("<abbr title= \"foo\">bar</abbr>"),
                                                                  Constants.DEFAULT_ENCODING)));
  }

}
