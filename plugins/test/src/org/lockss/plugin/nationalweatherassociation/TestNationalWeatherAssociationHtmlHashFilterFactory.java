/* $Id: TestNationalWeatherAssociationHtmlHashFilterFactory.java,v 1.1 2014-01-28 17:56:46 ldoan Exp $ */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
        + "<div align=\"right\">Copyright © 2014, All rights are reserved"
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

  private static final String withHome =
      "<div class=\"block\">"
        + "<a href=\"http://www.xxx.org/xjid/index.php\">Home</a>"
        + "</div>";
  private static final String withoutHome = 
      "<div class=\"block\"></div>";

  private static final String withPublications =
      "<div class=\"block\">"
        + "<a href=\"http://www.xxx.org/xjid/publications.php\">"
          + "publications</a>"
        + "</div>";
  private static final String withoutPublications = 
      "<div class=\"block\"></div>";

  private static final String withCallForPapers =
      "<div class=\"block\">"
        + "<a href=\"http://www.xxx.org/xjid/call_for_papers.php\">"
          + "call for papers</a>"
        + "</div>";
  private static final String withoutCallForPapers = 
      "<div class=\"block\"></div>";

  private static final String withAuthorInfo =
      "<div class=\"block\">"
        + "<a href=\"http://www.xxx.org/xjid/author_information.php\">"
          + "author info</a>"
        + "</div>";
  private static final String withoutAuthorInfo = 
      "<div class=\"block\"></div>";

  private static final String withReviewers =
      "<div class=\"block\">"
        + "<a href=\"http://www.xxx.org/xjid/reviewer_information.php\">"
          + "reviewers</a>"
        + "</div>";
  private static final String withoutReviewers = 
      "<div class=\"block\"></div>";

  private static final String withSearch =
      "<div class=\"block\">"
        + "<a href=\"http://www.xxx.org/xjid/search.php\">search</a>"
        + "</div>";
  private static final String withoutSearch = 
      "<div class=\"block\"></div>";

  private static final String withAbout =
      "<div class=\"block\">"
        + "<a href=\"http://www.xxx.org/xjid/about.php\">about</a>"
        + "</div>";
  private static final String withoutAbout = 
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

  public void testHomeFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withHome), Constants.DEFAULT_ENCODING);
    assertEquals(withoutHome, StringUtil.fromInputStream(actIn));
  }

  public void testPublicationsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withPublications), Constants.DEFAULT_ENCODING);
    assertEquals(withoutPublications, StringUtil.fromInputStream(actIn));
  }

  public void testCallForPapersFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withCallForPapers), Constants.DEFAULT_ENCODING);
    assertEquals(withoutCallForPapers, StringUtil.fromInputStream(actIn));
  }

  public void testAuthorInfoFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withAuthorInfo), Constants.DEFAULT_ENCODING);
    assertEquals(withoutAuthorInfo, StringUtil.fromInputStream(actIn));
  }

  public void testReviewsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withReviewers), Constants.DEFAULT_ENCODING);
    assertEquals(withoutReviewers, StringUtil.fromInputStream(actIn));
  }

  public void testSearchFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withSearch), Constants.DEFAULT_ENCODING);
    assertEquals(withoutSearch, StringUtil.fromInputStream(actIn));
  }

  public void testAboutFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withAbout), Constants.DEFAULT_ENCODING);
    assertEquals(withoutAbout, StringUtil.fromInputStream(actIn));
  }

  public void testLastUpdatedFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withLastUpdated), Constants.DEFAULT_ENCODING);
    assertEquals(withoutLastUpdated, StringUtil.fromInputStream(actIn));
  }

}
