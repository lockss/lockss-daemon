/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.nationalweatherassociation;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestNationalWeatherAssociationHtmlHashFilterFactory
  extends LockssTestCase {

  private NationalWeatherAssociationHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new NationalWeatherAssociationHtmlHashFilterFactory();
  }

 private static final String withScript =
     "<div class=\"block\">"
       + "<script type=\"text/javascript\" "
       + "src=\"http://www.google-analytics.com/ga.js\"></script>"
       + "<script type=\"text/javascript\">var xtrac=_xxx._xtrac('aa11');"
       + "idata();iview();"
       + "</script>"
       + "</div>";
  private static final String withoutScript = "<div class=\"block\"></div>";

  private static final String withComments =
      "<div class=\"block\">"
        + "<!-- comment comment comment -->"
        + "</div>";
  private static final String withoutComments = "<div class=\"block\"></div>";

  private static final String withStylesheets =
      "<div class=\"block\">"
        + "<link href=\"/css/xxx.css\" type=\"text/css\" rel=\"stylesheet\">"
        + "</div>";
  private static final String withoutStylesheets =
      "<div class=\"block\"></div>";

  private static final String withHeader =
      "<div class=\"block\">"
        + "<div id=\"header\">"
        + "<div id=\"sub1\">"
        + "<div id=\"subtitle1\"><a href=\"/index1.php\"></div>"
        + "</div>"
        + "<div id=\"sub2\">"
        + "<div id=\"subtitle2\"><a href=\"/index2.php\"></div>"
        + "</div>"
        + "<div id=\"sub3\">"
        + "<div id=\"subtitle3\"><a href=\"/index3.php\"></div>"
        + "</div></div></div>";
  private static final String withoutHeader =
      "<div class=\"block\"></div>";

  private static final String withFooter =
      "<div class=\"block\">"
        + "<div id=\"footer\">"
        + "<div id=\"sub1\">"
        + "<table width=\"100%\">"
        + "<tbody><tr>"
        + "<td valign=\"middle\">"
        + "<a href=\"/about.php\">about</a></td>"
        + "<td valign=\"middle\">"
        + "<div align=\"right\">Copyright &#169; 2014, All rights are reserved"
        + "</div></td></tr></tbody></table>"
        + "</div></div>"
	+ "</div>";
  private static final String withoutFooter =
      "<div class=\"block\"></div>";

  private static final String withLeftSidebar =
      "<div class=\"block\">"
        + "<div id=\"left\">"
        + "<div id=\"moreleft\">"
        + "<div><a href=\"/membership.php\">become member</a></div>"
        + "</div></div></div>";
  private static final String withoutLeftSidebar =
      "<div class=\"block\"></div>";

  private static final String withDeadlines =
      "<div class=\"block\">"
        + "<div id=\"deadlines\">"
        + "blah blah blah"
        + "<p>JID <a href=\"/email_notify.php\">email</a></p>"
        + "</div></div>";
  private static final String withoutDeadlines =
      "<div class=\"block\"></div>";

  private static final String withBanner =
      "<div class=\"block\">"
        + "<img src=\"http://www.xxx.org/xjid/xjid_banner.png\">"
        + "</div>";
  private static final String withoutBanner =
      "<div class=\"block\"></div>";

  private static final String withLastUpdated =
      "<div class=\"block\">"
        + "<p>"
        + "<em>Last updated 1/11/2011 by XXX.</em>"
        + "</p>"
        + "</div>";
  private static final String withoutLastUpdated =
      "<div class=\"block\"></div>";


  public void testScriptFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withScript), Constants.DEFAULT_ENCODING);
    assertEquals(withoutScript, StringUtil.fromInputStream(actIn));
  }

  public void testCommentsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withComments), Constants.DEFAULT_ENCODING);
    assertEquals(withoutComments, StringUtil.fromInputStream(actIn));
  }

  public void testStyleSheetsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withStylesheets), Constants.DEFAULT_ENCODING);
    assertEquals(withoutStylesheets, StringUtil.fromInputStream(actIn));
  }

  public void testHeaderFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withHeader), Constants.DEFAULT_ENCODING);
    assertEquals(withoutHeader, StringUtil.fromInputStream(actIn));
  }

  public void testFooterFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withFooter), Constants.DEFAULT_ENCODING);
    assertEquals(withoutFooter, StringUtil.fromInputStream(actIn));
  }

  public void testLeftSidebarFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withLeftSidebar), Constants.DEFAULT_ENCODING);
    assertEquals(withoutLeftSidebar, StringUtil.fromInputStream(actIn));
  }

  public void testDeadLinesFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withDeadlines), Constants.DEFAULT_ENCODING);
    assertEquals(withoutDeadlines, StringUtil.fromInputStream(actIn));
  }

  public void testBannerFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withBanner), Constants.DEFAULT_ENCODING);
    assertEquals(withoutBanner, StringUtil.fromInputStream(actIn));
  }

  public void testLastUpdatedFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withLastUpdated), Constants.DEFAULT_ENCODING);
    assertEquals(withoutLastUpdated, StringUtil.fromInputStream(actIn));
  }

}
