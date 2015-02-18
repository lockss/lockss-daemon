/*
 * $Id$
 */
package org.lockss.plugin.atypon.bloomsburyqatar;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestBQHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private BloomsburyQatarHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BloomsburyQatarHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String articleNav=
      "<div class=\"articleNavigation\">" +
          "<!-- Previous and Next Articles -->" +
          "<a href=\"/doi/full/10.1111/test.2013.7\">" +
          "            Previous Article" +
          "</a>" +
          "|" +
          "<a href=\"/doi/full/10.1111/test.2013.1\">" +
          "            Next Article" +
          "</a>" +
          "</div>";
  private static final String articleNavFiltered=
      "";

  private static final String breadcrumb=
      "<div class=\"clearfix\" id=\"breadcrumb-block\">" +
          "<div id=\"breadcrumb\">" +
          "<a href=\"/\">All Publications</a>" +
          "        &gt;" +
          "<a href=\"/loi/test\">" +
          "                    Testcenna" +
          "</a>" +
          "        &gt;" +
          "<a href=\"/toc/test//2013\">" +
          "                    Volume 2013" +
          "</a>" +
          "        &gt;" +
          "                Title  xmlns=\"http://www.w3.org/1999/xhtml\" /..." +
          "</div>" +
          "<div id=\"languageswitch\">" +
          "<p data-notecount=\"0\" data-lastcheck=\"1412275711954\">" +
          "<a class=\"language_ar\" href=\"/action/doLocaleChange?\">test</a>" +
          "</p><div style=\"display: none; \" class=\"note-flag rs-text\">0</div>" +
          "</div>" +
          "</div>";
  private static final String breadcrumbFiltered=
      "<div class=\"clearfix\" id=\"breadcrumb-block\">" +
          "<div id=\"languageswitch\">" +
          "<p data-notecount=\"0\" data-lastcheck=\"1412275711954\">" +
          "<a class=\"language_ar\" href=\"/action/doLocaleChange?\">test</a>" +
          "</p><div style=\"display: none; \" class=\"note-flag rs-text\">0</div>" +
          "</div>" +
          "</div>";


  public void testBQFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(articleNav),
        Constants.DEFAULT_ENCODING);
    assertEquals(articleNavFiltered, StringUtil.fromInputStream(actIn));
    actIn = fact.createFilteredInputStream(mau, new StringInputStream(breadcrumb),
        Constants.DEFAULT_ENCODING);
    assertEquals(breadcrumbFiltered, StringUtil.fromInputStream(actIn));


  }


}
