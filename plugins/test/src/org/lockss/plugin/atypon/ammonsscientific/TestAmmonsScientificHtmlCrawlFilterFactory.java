/*
 * $Id$
 */
package org.lockss.plugin.atypon.ammonsscientific;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestAmmonsScientificHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AmmonsScientificHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AmmonsScientificHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }


  //TOC - see example at:http://www.amsciepub.com/toc/pr0/108/3
  // this one has both a link to an "Erratum" article in a future volume
  // and an "Original Article" from a previous volume
  private static final String tocWithLinks = 
      "<div id=\"tocContent\"><!--totalCount34--><!--modified:1396966992000-->" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td valign=\"top\"><div class=\"art_title\">ONE TITLE</div>" +
          "<a class=\"entryAuthor\" href=\"/action/doSearch\">AN AUTHOR</a>" +
          "<div class=\"art_meta\">Journal:  699-710.</div>" +
          "<a class=\"ref nowrap \" href=\"/doi/abs/foo\">Abstract</a>" +
          " | <a class=\"ref nowrap\" target=\"_blank\" href=\"/doi/pdf/foo\">PDF (342 KB)</a> " +
          "<span class=\"linkDemarcator\"> | </span>" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/NO\">Erratum</a>" +
          "</td></tr></table>" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr>" +
          "<td valign=\"top\"><div class=\"art_title\">ERRATUM</div>" +
          "<div class=\"art_meta\">Journal:  1011-1011.</div>" +
          "<a class=\"ref nowrap \" href=\"/doi/abs/FOO\">Citation</a> " +
          "| <a class=\"ref nowrap\" target=\"_blank\" href=\"/doi/pdf/FOO\">PDF (189 KB)</a> " +
          "| </span><a class=\"ref\" href=\"/doi/abs/NO\">Original Article</a>" +
          "</td></tr></table>" +
          "</div>";
  private static final String tocWithoutLinks = 
      "<div id=\"tocContent\"><!--totalCount34--><!--modified:1396966992000-->" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td valign=\"top\"><div class=\"art_title\">ONE TITLE</div>" +
          "<a class=\"entryAuthor\" href=\"/action/doSearch\">AN AUTHOR</a>" +
          "<div class=\"art_meta\">Journal:  699-710.</div>" +
          "<a class=\"ref nowrap \" href=\"/doi/abs/foo\">Abstract</a>" +
          " | <a class=\"ref nowrap\" target=\"_blank\" href=\"/doi/pdf/foo\">PDF (342 KB)</a> " +
          "<span class=\"linkDemarcator\"> | </span>" +
          "" +
          "</td></tr></table>" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr>" +
          "<td valign=\"top\"><div class=\"art_title\">ERRATUM</div>" +
          "<div class=\"art_meta\">Journal:  1011-1011.</div>" +
          "<a class=\"ref nowrap \" href=\"/doi/abs/FOO\">Citation</a> " +
          "| <a class=\"ref nowrap\" target=\"_blank\" href=\"/doi/pdf/FOO\">PDF (189 KB)</a> " +
          "| </span>" +
          "</td></tr></table>" +
          "</div>";

  //Article page - see example at:
  private static final String withErrata =
      "<div>" +
          "<hr id=\"articleToolsHr\"/>" +
          "<ul id=\"articleToolsFormats\">" +
          "n<li>" +
          "<a href=\"/doi/pdf/FOO\" target=\"_blank\">PDF</a>" +
          "</li>" +
          "<li>" +
          "<a href=\"/doi/pdfplus/FOO\" target=\"_blank\">PDF Plus</a>" +
          "</li>" +
          "<li>" +
          "<a href=\"/doi/full/NO\">" +
          "Errata" +
          "</a>" +
          "</li>" +
          "</ul>" +
          "</div>";

  private static final String withoutErrata =
      "<div>" +
          "<hr id=\"articleToolsHr\"/>" +
          "<ul id=\"articleToolsFormats\">" +
          "n<li>" +
          "<a href=\"/doi/pdf/FOO\" target=\"_blank\">PDF</a>" +
          "</li>" +
          "<li>" +
          "<a href=\"/doi/pdfplus/FOO\" target=\"_blank\">PDF Plus</a>" +
          "</li>" +
          "<li>" +
          "" +
          "</li>" +
          "</ul>" +
          "</div>";
  
  //Article page - see example at:
  private static final String withOriginal =
      "<div>" +
          "<hr id=\"articleToolsHr\"/>" +
          "<ul id=\"articleToolsFormats\">" +
          "n<li>" +
          "<a href=\"/doi/pdf/FOO\" target=\"_blank\">PDF</a>" +
          "</li>" +
          "<li>" +
          "<a href=\"/doi/pdfplus/FOO\" target=\"_blank\">PDF Plus</a>" +
          "</li>" +
          "<li>" +
          " <a href=\"/doi/full/NO\">" +
          "  Original" +
          "</a>" +
          "</li>" +
          "</ul>" +
          "</div>";

  private static final String withoutOriginal =
      "<div>" +
          "<hr id=\"articleToolsHr\"/>" +
          "<ul id=\"articleToolsFormats\">" +
          "n<li>" +
          "<a href=\"/doi/pdf/FOO\" target=\"_blank\">PDF</a>" +
          "</li>" +
          "<li>" +
          "<a href=\"/doi/pdfplus/FOO\" target=\"_blank\">PDF Plus</a>" +
          "</li>" +
          "<li>" +
          " " +
          "</li>" +
          "</ul>" +
          "</div>";



  public void testCitationsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(tocWithLinks),
        Constants.DEFAULT_ENCODING);
    assertEquals(tocWithoutLinks, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau, new StringInputStream(withErrata),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutErrata, StringUtil.fromInputStream(actIn));
    
     actIn = fact.createFilteredInputStream(mau, new StringInputStream(withOriginal),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutOriginal, StringUtil.fromInputStream(actIn));
  }


}
