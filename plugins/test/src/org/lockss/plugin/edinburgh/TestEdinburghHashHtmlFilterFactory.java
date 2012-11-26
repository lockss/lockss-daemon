/*
 * $Id: TestEdinburghHashHtmlFilterFactory.java,v 1.2 2012-11-26 19:02:12 alexandraohlson Exp $
 */

package org.lockss.plugin.edinburgh;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestEdinburghHashHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private EdinburghUniversityPressHashHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new EdinburghUniversityPressHashHtmlFilterFactory();
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

  private static final String institutionBannerHtml ="<div id=\"mainNavContainer\">"
      + "<ul id=\"mainNav\"> "
    + "<li><p> <a href=\"/\">Home</a> </p></li>"
    + "<li><p><a href=\"/page/infoZone/home\">Information Zone</a></p></li>"
    + "<li class=\"institutionBanner institutionBannerText\"><p>\"Serials Department, Green Library\"</p></li>"
    + "</ul> </div>";
 
  private static final String institutionBannerFiltered = "<div id=\"mainNavContainer\">"
      + "<ul id=\"mainNav\"> "
    + "<li><p> <a href=\"/\">Home</a> </p></li>"
    + "<li><p><a href=\"/page/infoZone/home\">Information Zone</a></p></li>"
    + "</ul> </div>";
  
  public void testCitationsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(withCitations),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutCitations, StringUtil.fromInputStream(actIn));
  }
  
  public void testInstitutionBannerFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(institutionBannerHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(institutionBannerFiltered, StringUtil.fromInputStream(actIn));
  } 

}
