/*
 * $Id$
 */
package org.lockss.plugin.ojs2;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestOJS2HtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private OJS2HtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new OJS2HtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withNavbar = "<p align=\"center\" class=\"ads\">"
    + "<a href=\"http://okfestival.org/\"><img src=\"/up/ads/ad2.gif\" /></a></p>"
    + "<div id=\"navbar\">"
    + "<ul class=\"menu\">"
    + "<li id=\"home\"><a href=\"http://www.ancient-asia-journal.com/index\">Home</a></li>"
    + "<li id=\"about\"><a href=\"http://www.ancient-asia-journal.com/about\">About</a></li>"
    + "<li id=\"navItem\"><a href=\"/about/editorialTeam\">Editorial Board</a></li>"
    +	"<li id=\"current\"><a href=\"http://www.ancient-asia-journal.com/issue/current\">Current Issue</a></li>"
    + "<li id=\"archives\"><a href=\"http://www.ancient-asia-journal.com/issue/archive\">All Issues</a></li>"
    + "</ul>"
    + "</div>"
    + "<p align=\"center\" class=\"ads\"><a href=\"http://okfestival.org/\">"
    + "<img src=\"/up/ads/ad2.gif\" /></a></p>";
  
  private static final String withoutNavbar = "<p align=\"center\" class=\"ads\">"
    + "<a href=\"http://okfestival.org/\"><img src=\"/up/ads/ad2.gif\" /></a></p>"
    + "<p align=\"center\" class=\"ads\"><a href=\"http://okfestival.org/\">"
    + "<img src=\"/up/ads/ad2.gif\" /></a></p>";
  
  private static final String withBreadcrumb = "<div>"
    + "<div id=\"breadcrumb\">"
    + "<a href=\"http://www.ancient-asia-journal.com/index\">Home</a> &gt;"
    + "<a href=\"http://www.ancient-asia-journal.com/issue/archive\" class=\"hierarchyLink\">Archives</a> &gt;"
    + "Vol 1 (2006)</div>"
    + "</div>";

  private static final String withoutBreadcrumb = "<div>"
    + "</div>";

  
  public void testFiltering() throws Exception {
    InputStream inA, inB;
    
    // navbar
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withNavbar),
					 Constants.DEFAULT_ENCODING);
    assertEquals(withoutNavbar, StringUtil.fromInputStream(inA));
    
    // breadcrumb
    inB = fact.createFilteredInputStream(mau, new StringInputStream(withBreadcrumb),
					 Constants.DEFAULT_ENCODING);
    assertEquals(withoutBreadcrumb, StringUtil.fromInputStream(inB));

  }

}
