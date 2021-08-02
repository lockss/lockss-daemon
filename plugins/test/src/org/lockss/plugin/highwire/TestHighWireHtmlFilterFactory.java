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

package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestHighWireHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private HighWireHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new HighWireHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String inst1 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n	" +
      "<NOBR><STRONG>Institution: Periodicals Department/Lane Library</STRONG></NOBR>\n	" +
      "<NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">" +
      "Sign In as Personal Subscriber</A></NOBR>";

  private static final String inst2 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n	" +
      "<NOBR><STRONG>Institution: Stanford University Libraries</STRONG></NOBR>\n	" +
      "<NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">" +
      "Sign In as Personal Subscriber</A></NOBR>";

  private static final String inst3 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n    " +
      "<NOBR><STRONG>Institution: Stanford University Libraries</STRONG></NOBR>\n      " +
      "<NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">" +
      "Sign In as SOMETHING SOMETHING</A></NOBR>";

  String test[] = {

      // Contains variable ad-generating code
      "<script type=\"text/javascript\" src=\"http://nejm.resultspage.com/" +
      "autosuggest/searchbox_suggest_v1.js\" language=\"javascript\">Hello</script>xxyyzz",
      // Contains variable ad-generating code
      "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=" +
      "ON&amp;amp; </noscript>\nxxyyzz",
      
      // Typically contains ads (e.g. American Academy of Pediatrics)
      "<object width=\"100%\" height=\"100%\" type=\"video/x-ms-asf\" " +
      "url=\"3d.wmv\" data=\"3d.wmv\" classid=\"CLSID:6BF52A52-394A-11d3-B153-00C04F79FAA6\">" +
      "</object>xxyyzz",
      // Typically contains ads
      "<iframe src=\"demo_iframe.htm\" width=\"20\" height=\"200\"></iframe>xxyyzz",
      // Contains ads (e.g. American Medical Association)
      "<div id = \"advertisement\"></div>xxyyzz",
      "<div id = \"authenticationstring\"></div>xxyyzz",
      // Contains institution name (e.g. SAGE Publications)
      "<div id = \"universityarea\"></div>xxyyzz",
      // Contains institution name (e.g. Oxford University Press)
      "<div id = \"inst_logo\"></div>xxyyzz",
      // Contains institution name (e.g. American Medical Association)
      "<p id = \"UserToolbar\"></p>xxyyzz",
      "<div id = \"user_nav\"></div>xxyyzz",
      "<table class = \"content_box_inner_table\"></table>xxyyzz",
      "<a class = \"contentbox\"></a>xxyyzz",
      "<div id = \"ArchivesNav\"></div>xxyyzz",
      // lowestLevelMatchFilter(HtmlNodeFilters.tagWithText("table", "Related Content", false)),
      "<table class=\"content_box_outer_table\" align=\"right\">" +
      "  <tr><td>xx" +
      "<!-- beginning of inner table -->"+
      "<table class=\"content_box_inner\">" +
      "   <tr><td width=\"4\" class=\"content_box_arrow\" valign=\"top\"><img alt=\"Right arrow\" width=\"4\" height=\"11\" border=\"0\" src=\"/icons/shared/misc/arrowTtrim.gif\" /></td><td class=\"content_box_item\">" +
      "             <strong><a target=\"_blank\" href=\"http://scholar.google.com/scholar?q=%22author%3AM. L.+author%3AWahl%22\">" +
      "             Articles by Wahl, M. L.</a></strong></td></tr>" +
      "   <tr><td width=\"4\" class=\"content_box_arrow\" valign=\"top\"><img alt=\"Right arrow\" width=\"4\" height=\"11\" border=\"0\" src=\"/icons/shared/misc/arrowTtrim.gif\" /></td><td class=\"content_box_item\">" +
      "             <strong><a target=\"_blank\" href=\"http://scholar.google.com/scholar?q=%22author%3AS. V.+author%3APizzo%22\">" +
      "             Articles by Pizzo, S. V.</a></strong></td></tr>" +
      "   <tr><td width=\"4\" class=\"content_box_arrow\" valign=\"top\"><img alt=\"Right arrow\" width=\"4\" height=\"11\" border=\"0\" src=\"/icons/shared/misc/arrowTtrim.gif\" /></td><td class=\"content_box_item\">" +
      "             <strong><a target=\"_blank\" href=\"/cgi/external_ref?access_num=" +
      "           http://rphr.endojournals.org" +
      "           /cgi/content/abstract/59/1/73" +
      "         &link_type=GOOGLESCHOLARRELATED\">Search for Related Content</a></strong>" +
      "           </td></tr>    " +
      "not found</td></tr></table>yy</td></tr></table>zz", 
      // lowestLevelMatchFilter(HtmlNodeFilters.tagWithText("table", "Citing Articles", false)),
      "<table class=\"content_box_outer_table\" align=\"right\">" +
      "  <tr><td>xx" +
      "<!-- beginning of inner table -->" +
      "<table cellspacing=\"0\" cellpadding=\"0\" width=\"140\" border=\"0\" marginwidth=\"0\" marginheight=\"0\" leftmargin=\"0\" topmargin=\"0\">" +
      "  <tbody>" +
      "<tr><td colspan=\"2\" height=\"20\" valign=\"middle\"><strong>&nbsp;Citing Articles</strong></td></tr>\n" +
      "<tr><td valign=\"TOP\" width=\"5\"></td><td valign=\"top\"><a href=\"/foo\" class=\"contentbox\">Citing articles on Web of Science (5)</a></td></tr>\n" +
      "<tr><td valign=\"TOP\" width=\"5\"></td><td valign=\"top\"><a href=\"/cgi/alerts/ctalert\" class=\"contentbox\">Contact me when this article is cited</a></td></tr>\n" +
      "  </tbody>" +
      "not found</td></tr></table>yy</td></tr></table>zz", 
      // Contains the current year (e.g. Oxford University Press)
      "<div id = \"copyright\"></div>xxyyzz",
      // Contains the current year (e.g. SAGE Publications)
      "<div id = \"footer\"></div>xxyyzz",
      // Contains the current date and time (e.g. American Medical Association)
      "<a target = \"help\"></a>xxyyzz",
      // Contains the name and date of the current issue (e.g. Oxford University Press)
      "<li id = \"nav_current_issue\"></li>xxyyzz",
      // Contains ads or variable banners (e.g. Oxford University Press)
      "<div id = \"oas_top\"></div>xxyyzz",
      // Contains ads or variable banners (e.g. Oxford University Press)
      "<div id = \"oas_bottom\"></div>xxyyzz",
      // Optional institution-specific citation resolver (e.g. SAGE Publications)
      "<a href = \"^/cgi/openurl\"></a>xxyyzz",
      // Contains ad-dependent URLs (e.g. American Academy of Pediatrics)
      "<a href = \"^http://ads.adhostingsolutions.com/\"></a>xxyyzz",
      // alt for less/greater than confuses WhiteSpace filter
      "<img alt = \"[<>]\"></img>xxyyzz",
      //CMAJ (c)year tag
      "<div class = \"slugline-copyright\"></div>xxyyzz",
      // Red Book tag tests
      "<head>head stuff</head>xxyyzz",
      "<div class=\"leaderboard-ad\">leaderboard-ad stuff</div>xxyyzz",
      "<div id=\"header\">header stuff</div>xxyyzz",

  };
  
  public void testFiltering() throws Exception {
    InputStream in;
    InputStream inA;
    InputStream inB;

    for (String t : test){
      in = fact.createFilteredInputStream(mau, new StringInputStream(t), ENC);
      String test_in = StringUtil.fromInputStream(in);
      // trim leading spaces
      test_in = test_in.trim();
      assertEquals("xxyyzz", test_in);
    }

    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(inst1), ENC);
    inB = fact.createFilteredInputStream(mau,
        new StringInputStream(inst2), ENC);
    assertEquals(StringUtil.fromInputStream(inA),
        StringUtil.fromInputStream(inB));

    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(inst1), ENC);
    inB = fact.createFilteredInputStream(mau,
        new StringInputStream(inst3), ENC);
    assertEquals(StringUtil.fromInputStream(inA),
        StringUtil.fromInputStream(inB));
  }

}
