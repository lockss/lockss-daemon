/*
 * $Id$
 */
package org.lockss.plugin.edinburgh;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestEdinburghCrawlHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private EdinburghUniversityPressCrawlHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new EdinburghUniversityPressCrawlHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String withCitations = "<div id=\"firstPage\">"
      + "<img src=\"/na101/home/literatum/jbctv.2006.3.1.128.fp.png_v03\""
      + "alt=\"Free first page\" class=\"firstPageImage\"/>"
      + "</div>"
      + "<div class=\"citedBySection\">"
      + "<a name=\"citedBySection\"></a> <h2>Cited by</h2>"
      + "<div class=\"citedByEntry\"> <i>"
      + "<span class=\"NLM_source\">Comparative Critical Studies</span></i>"
      + "<span class=\"CbLinks\">"
      + "<a class=\"articleListing_links\" href=\"/doi/abs/10.3366/ccs.2011.0017\">"
      + "Citation</a>"
      + "<a class=\"articleListing_links\" href=\"/doi/pdfplus/10.3366/ccs.2011.0017\">"
      + "<input type=\"image\" alt=\"Pdf With References\""
      + "src=\"/templates/jsp/_style2/_eup/images/icon_toolBar_pdf.gif\"/>"
      + "</a></span> </div> </div>";

  private static final String withoutCitations = "<div id=\"firstPage\">"
      + "<img src=\"/na101/home/literatum/jbctv.2006.3.1.128.fp.png_v03\""
      + "alt=\"Free first page\" class=\"firstPageImage\"/>"
      + "</div>";
  
  private static final String breadcrumb=
      "<div id=\"mainBreadCrumb\">" +
          "<a href=\"/action/showPublications?display=bySubject&amp;pubType=journal\">Journals</a> &gt;" +
          "<a href=\"/journal/xxx\">DXXX</a> &gt;" +
          "<a href=\"/loi/xxx\">All Issues</a> &gt;" +
          "<a href=\"/toc/xxx/26/2\">" +
          " Oct 2008</a> &gt;" +
          "<span class=\"currentBreadCrumb\">TitleofArticle</span>" +
          "</div>";
  private static final String breadcrumbFiltered=
"";
  private static final String articleNav=
      "<div class=\"moduleToolBar\">" +
          "<div class=\"moduleToolBarPaging\">" +
          "<p>" +
          "<a class=\"link\" href=\"/toc/xxx/26/1\">&lt; Previous</a>" +
          "</p>" +
          "<div class=\"separator\">" +
          "</div>" +
          "<p>" +
          "<a class=\"link\" href=\"/toc/xxx/27/1\">Next &gt;</a>" +
          "</p>" +
          "</div>" +
          "</div>";
  private static final String articleNavFiltered=
      "<div class=\"moduleToolBar\">" +
          "</div>";
      
  

  public void testCitationsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(withCitations),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutCitations, StringUtil.fromInputStream(actIn));
  }

  public void testNavFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(breadcrumb),
        Constants.DEFAULT_ENCODING);
    assertEquals(breadcrumbFiltered, StringUtil.fromInputStream(actIn));

    actIn = fact.createFilteredInputStream(mau, new StringInputStream(articleNav),
        Constants.DEFAULT_ENCODING);
    assertEquals(articleNavFiltered, StringUtil.fromInputStream(actIn));

  }
  
  

}
