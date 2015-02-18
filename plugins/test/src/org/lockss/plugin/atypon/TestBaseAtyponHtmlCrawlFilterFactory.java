/*
 * $Id$
 */
package org.lockss.plugin.atypon;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestBaseAtyponHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private BaseAtyponHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BaseAtyponHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String withCitations = "</div><div class=\"citedBySection\">" +
      "<a name=\"citedBySection\"></a><h2>Cited by</h2>" +
      "<div class=\"citedByEntry\"><span class=\"author\">Robert X</span>, <span class=\"author\">Kenneth X</span>.BIG TITLE HERE. <i>" +
      "<span class=\"NLM_source\">Risk Analysis</span></i>no" +
      "-no<br />Online publication date: 1-Dec-2012.<br /><span class=\"CbLinks\"></span></div>"+
      "</div><!-- /fulltext content --></div>"+
      "       </div></div><div class=\"clearfix\">&nbsp;</div></div>";

  private static final String withoutCitations = "</div><!-- /fulltext content --></div>"+
      "       </div></div><div class=\"clearfix\">&nbsp;</div></div>";  


  private static final String articlePrevNext1=
      "<div class=\"widget type-publication-tools ui-helper-clearfix\" id=\"widget-3168\">" +
          "<div class=\"header thin\">" +
          "<div class=\"head-left\">" +
          "<a class=\"articleToolsNav\" href=\"/doi/full/10.1111/1.xx\">" +
          "<button>" +
          "  Previous Article" +
          "</button>" +
          "</a>" +
          "</div>" +
          "<div class=\"head-right\">" +
          "<a class=\"articleToolsNav\" href=\"/doi/full/10.1111/1.xx\">" +
          "<button>" +
          "  Next Article" +
          "</button>" +
          "</a>" +
          "</div>" +
          "<div class=\"head-middle\">" +
          "<h3>" +
          "<a href=\"http://test/toc/testj/51/7\">" +
          "            Volume 51, Issue 7 (July)" +
          "</a>" +
          "</h3>" +
          "</div>" +
          "</div>" +
          "<div class=\"body\">" +
          "  links to other aspects of article" +
          "</div>" +
          "</div>";
  private static final String articlePrevNext1Filtered=
      "<div class=\"widget type-publication-tools ui-helper-clearfix\" id=\"widget-3168\">" +
          "<div class=\"header thin\">" +
          "<div class=\"head-left\">" +
          "</div>" +
          "<div class=\"head-right\">" +
          "</div>" +
          "<div class=\"head-middle\">" +
          "<h3>" +
          "<a href=\"http://test/toc/testj/51/7\">" +
          "            Volume 51, Issue 7 (July)" +
          "</a>" +
          "</h3>" +
          "</div>" +
          "</div>" +
          "<div class=\"body\">" +
          "  links to other aspects of article" +
          "</div>" +
          "</div>";

  private static final String articlePrevNext2=
      "<div class=\"pager issueBookNavPager\">" +
          "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">" +
          "<tbody><tr>" +
          "<td class=\"journalNavLeftTd\">" +
          "<div class=\"prev placedLeft\">" +
          "<a href=\"/doi/abs/10.1111/1.xx\">" +
          "</a>" +
          "</div>" +
          "</td>" +
          "<td class=\"journalNavRightTd\">" +
          "<div class=\"next\">" +
          "<a href=\"/doi/abs/10.1111/1.xx\">" +
          "</a>" +
          "</div>" +
          "</td>" +
          "</tr>" +
          "</tbody></table>" +
          "</div>";
  private static final String articlePrevNext2Filtered=
      "<div class=\"pager issueBookNavPager\">" +
          "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">" +
          "<tbody><tr>" +
          "</tr>" +
          "</tbody></table>" +
          "</div>";

  private static final String breadcrumb1=
      "<div id=\"breadcrumbs\">" +
          "<span id=\"allJournals\">" +
          "<a href=\"/\">All Publications</a>" +
          "  &gt;" +
          "<a href=\"/loi/test\">" +
          " Journal of Something" +
          "</a>" +
          "  &gt;" +
          "<a href=\"/toc/test/9/3\">" +
          "  June 2008" +
          "</a>" +
          "  &gt;" +
          "  Investigating ..." +
          "</span>" +
          "<span id=\"advancedSearch\">" +
          "<a href=\"/search/advanced\">Advanced Search</a></span>" +
          "</div>";
  private static final String breadcrumb1Filtered=
      "";
  private static final String breadcrumb2=
      "<nav>" +
          "<ul class=\"breadcrumbs\">" +
          "<li class=\"\">" +
          "<a href=\"/\">Home</a><span class=\"divider\">&gt;</span>" +
          "</li>" +
          "<li class=\"\">" +
          "<a href=\"/toc/xx/current\">Journal Title</a><span class=\"divider\">&gt;</span>" +
          "</li>" +
          "<li class=\"\">" +
          "<a href=\"/loi/xx\">List of Issues</a><span class=\"divider\">&gt;</span>" +
          "</li>" +
          "<li class=\"\">" +
          "<a href=\"/toc/xx/42/8\">Volume 42, Issue 8</a><span class=\"divider\">&gt;</span>" +
          "</li>" +
          "<li class=\"truncate\">" +
          "                Prevalence of Things" +
          "</li>" +
          "</ul>" +
          "</nav>";
  private static final String breadcrumb2Filtered=
      "<nav>" +
          "</nav>";
  private static final String breadcrumb3=
      "<div>" +
          "<ul class=\"linkList breadcrumbs\">" +
          "<li>" +
          "<a href=\"/\">Digital Library Home</a>" +
          "<span class=\"breadcrumbsAfter\"> &gt; </span> " +
          "</li>" +
          "<li>" +
          "<a href=\"/loi/xx\">Big Journal</a>" +
          "<span class=\"breadcrumbsAfter\"> &gt; </span> " +
          "</li>" +
          "<li>" +
          "<a href=\"/toc/xx/78/1\">Volume 78, Issue 1 (January-February 2013)</a>" +
          "<span class=\"breadcrumbsAfter\"> &gt; </span> " +
          "</li>" +
          "<li class=\"lastbread\">" +
          "  10.1111/1.xx" +
          "</li>" +
          "</ul>" +
          "</div>";
  private static final String breadcrumb3Filtered=
      "<div>" +
          "</div>";
  private static final String issueNav1=
      "<div class=\"issueNavigator\">" +
          "<div class=\"box\">" +
          "<div class=\"header\">" +
          "<h3></h3>" +
          "<div id=\"smallIssueCover\">" +
          "<img src=\"/na101//test/journals/covergifs/testj/cover.jpg\"><br>" +
          "</div>" +
          "</div>" +
          "<br>" +
          "<div id=\"nextprev\">" +
          "<a class=\"prev\" href=\"http://test/toc/testj/51/6\">" +
          "<img alt=\"Previous\" src=\"/templates/prev.png\" class=\"prev\">" +
          "</a>" +
          "<div class=\"links\">" +
          "<div class=\"afterCover\">" +
          "<!-- File is left blank intentionally. -->" +
          "</div>" +
          "<a href=\"http://test/toc/testj/52/10\">" +
          "            Current Issue" +
          "</a><br>" +
          "<a href=\"/loi/testj\">" +
          "    Available Issues" +
          "</a><br>" +
          "<a href=\"http://test/toc/testj/0/0\">" +
          "        Articles in Advance" +
          "</a>" +
          "</div>" +
          "<a class=\"next\" href=\"http://test/toc/testj/51/8\">" +
          "<img alt=\"Next\" src=\"/templates/jsp/next.png\" class=\"next\">" +
          "</a>" +
          "</div>" +
          "</div>" +
          "</div>";
  private static final String issueNav1Filtered=
      "<div class=\"issueNavigator\">" +
          "<div class=\"box\">" +
          "<div class=\"header\">" +
          "<h3></h3>" +
          "<div id=\"smallIssueCover\">" +
          "<img src=\"/na101//test/journals/covergifs/testj/cover.jpg\"><br>" +
          "</div>" +
          "</div>" +
          "<br>" +
          "</div>" +
          "</div>";
  private static final String issueNav2=
      "<div id=\"issueNav\">" +
          "<div id=\"prevNextNav\">" +
          "<div id=\"issueSearch\">" +
          "<form method=\"get\" action=\"/action/doSearch\">" +
          "<input type=\"text\" size=\"17\" value=\"\" name=\"AllField\">" +
          "<input type=\"hidden\" value=\"6\" name=\"issue\">" +
          "<input type=\"hidden\" value=\"test2\" name=\"journalCode\">" +
          "<input type=\"hidden\" value=\"17\" name=\"volume\">" +
          "<input type=\"hidden\" value=\"issue\" name=\"filter\">" +
          "<input type=\"submit\" value=\"Search Issue\"></form>" +
          "</div>" +
          "<a href=\"javascript:toggleSlide('issueSearch')\">Search Issue</a> |" +
          "<img src=\"/templates/jsp/_style2/_pagebuilder/_c3/images/rss_32.png\">" +
          "<a href=\"http://test.org/action/showFeed\">RSS</a>" +
          "<br>" +
          "<a href=\"/toc/test2/17/5\">Previous Issue</a>" +
          "<a href=\"/toc/test2/18/1\"> Next Issue</a>" +
          "</div>" +
          "<div id=\"coverDate\">" +
          "                    November 2012" +
          "</div>" +
          "<div id=\"tocInfo\">" +
          "                    Volume 17, Issue 6" +
          "<span style=\"margin-left: -5px;\">,</span>" +
          "                        pp. 827-994" +
          "</div>" +
          "<div style=\"clear: both;\"></div>" +
          "</div>";
  private static final String issueNav2Filtered=
      "<div id=\"issueNav\">" +
          "<div id=\"coverDate\">" +
          "                    November 2012" +
          "</div>" +
          "<div id=\"tocInfo\">" +
          "                    Volume 17, Issue 6" +
          "<span style=\"margin-left: -5px;\">,</span>" +
          "                        pp. 827-994" +
          "</div>" +
          "<div style=\"clear: both;\"></div>" +
          "</div>";
  private static final String issueNav3=
      "<ul style=\"display: none\" class=\"volumeIssues\">" +
          "<li class=\" \">" +
          "<a href=\"http://test.org/toc/test2/19/10\">" +
          " Issue 10 | October 2014 </a>" +
          "</li>" +
          "<li class=\" \">" +
          "<a href=\"http://test.org/toc/test2/19/9\">" +
          " Issue 9 | September 2014 </a>" +
          "</li>" +
          "<li class=\" \">" +
          "<a href=\"http://test.org/toc/test2/19/1\">" +
          " Issue 1 | January 2014 | pp. 1-148 </a>" +
          "</li>" +
          "</ul>";
  private static final String issueNav3Filtered=
      "";

  public void testFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withCitations),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutCitations, StringUtil.fromInputStream(inStream));
  }

  public void testArticleNavFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(articlePrevNext1),
        Constants.DEFAULT_ENCODING);
    assertEquals(articlePrevNext1Filtered, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(articlePrevNext2),
        Constants.DEFAULT_ENCODING);
    assertEquals(articlePrevNext2Filtered, StringUtil.fromInputStream(inStream));
  }

  public void testTOCNavFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(breadcrumb1),
        Constants.DEFAULT_ENCODING);
    assertEquals(breadcrumb1Filtered, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(breadcrumb2),
        Constants.DEFAULT_ENCODING);
    assertEquals(breadcrumb2Filtered, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(breadcrumb3),
        Constants.DEFAULT_ENCODING);
    assertEquals(breadcrumb3Filtered, StringUtil.fromInputStream(inStream));
  }

  public void testIssueNavFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(issueNav1),
        Constants.DEFAULT_ENCODING);
    assertEquals(issueNav1Filtered, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(issueNav2),
        Constants.DEFAULT_ENCODING);
    assertEquals(issueNav2Filtered, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(issueNav3),
        Constants.DEFAULT_ENCODING);
    assertEquals(issueNav3Filtered, StringUtil.fromInputStream(inStream));
  }


}
