/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
