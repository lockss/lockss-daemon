/*
 * $Id$
 */
package org.lockss.plugin.nature;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestNatureHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private NaturePublishingGroupHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new NaturePublishingGroupHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String issueNav = 
      "<div class=\"articlenav\"><h2><span class=\"hidden\">ARTICLE NAVIGATION - </span>ISSUE</h2>" +
          "<div class=\"prevnext\"><a title=\"previous issue\" class=\"prev\" href=\"/vital/journal/v7/n1/index.html\">Previous</a>" +
          "<span class=\"divider\"> | </span><a class=\"next\" title=\"next issue\" href=\"/vital/journal/v7/n3/index.html\">Next</a>" +
          "<span class=\"hidden\"> | </span></div></div></div><div class=\"issue-links\"><h2 class=\"issue\">Spring 2010, Volume 7 </h2>" +
          "<ul class=\"toc-anchor\"><li><a href=\"#upfrnt\">Up front</a></li><li><a href=\"#strle\">Star Letter</a></li><li>" +
          "<a href=\"#le\">Letters</a></li><li><a href=\"#rv\">Reviews</a><ul> <li><a href=\"#bks\">Book Reviews</a>" +
          "</li> </ul></li><li><a href=\"#nw\">News</a></li><li><a href=\"#upfrnt\">Up front</a></li><li><a href=\"#fe\">Features</a>" +
          "</li><li><a href=\"#advice\">Advice</a></li><li><a href=\"#getact\">Get Active</a></li><li><a href=\"#lstwrd\">Last Word</a></li>" +
          "<li><a href=\"#puzzle\">Puzzle page</a></li><li><a href=\"#mp\">Marketplace</a></li><li><a href=\"#advice\">Advice</a>" +
          "</li><li><a href=\"#lstwrd\">Last Word</a></li></ul></div><span class=\"cleardiv\"><!-- --></span>" +
          "</div><div class=\"subject\" id=\"upfrnt\"><a href=\"#top\" class=\"backtotop\">Top<span class=\"hidden\"> of page</span></a>" +
          "<h3 class=\"subject\">Up front</h3>";

  private static final String withoutIssueNav = 
      "<div class=\"articlenav\"><h2><span class=\"hidden\">ARTICLE NAVIGATION - </span>ISSUE</h2>" +
          "</div></div><div class=\"issue-links\"><h2 class=\"issue\">Spring 2010, Volume 7 </h2>" +
          "<ul class=\"toc-anchor\"><li><a href=\"#upfrnt\">Up front</a></li><li><a href=\"#strle\">Star Letter</a></li><li>" +
          "<a href=\"#le\">Letters</a></li><li><a href=\"#rv\">Reviews</a><ul> <li><a href=\"#bks\">Book Reviews</a>" +
          "</li> </ul></li><li><a href=\"#nw\">News</a></li><li><a href=\"#upfrnt\">Up front</a></li><li><a href=\"#fe\">Features</a>" +
          "</li><li><a href=\"#advice\">Advice</a></li><li><a href=\"#getact\">Get Active</a></li><li><a href=\"#lstwrd\">Last Word</a></li>" +
          "<li><a href=\"#puzzle\">Puzzle page</a></li><li><a href=\"#mp\">Marketplace</a></li><li><a href=\"#advice\">Advice</a>" +
          "</li><li><a href=\"#lstwrd\">Last Word</a></li></ul></div><span class=\"cleardiv\"><!-- --></span>" +
          "</div><div class=\"subject\" id=\"upfrnt\"><a href=\"#top\" class=\"backtotop\">Top<span class=\"hidden\"> of page</span></a>" +
          "<h3 class=\"subject\">Up front</h3>";
  


  public void testCitationsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(issueNav),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutIssueNav, StringUtil.fromInputStream(actIn));
    
  }


}
