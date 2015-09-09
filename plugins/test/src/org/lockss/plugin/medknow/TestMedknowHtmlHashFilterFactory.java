/* $Id$

 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.medknow;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;


public class TestMedknowHtmlHashFilterFactory extends LockssTestCase {
  private MedknowHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MedknowHtmlHashFilterFactory();
  }


  private static final String citationCounts =
      "<div><table><tr>" +
          "<td class=\"articlepage\" >" +
          "<font class=\"CorrsAdd\">" +
          "<img src=\"http://api.elsevier.com/content/abstract/citation-count?pubmed_id=2352505\">" +
          "<div style=\"float: left; border:1px solid #ddd;padding:4px;\">" +
          "<a href=\"crossrefCitation.asp?doi=10.4103/0022-3859.109492\" target=\"_blank\">" +
          "<table width=\"100%\">" +
          "<tbody>" +
          "<tr>" +
          "<td>" +
          "<img src=\"http://www.medknow.com\" alt=\"Crossref citations\">" +
          "</td>" +
          "<td style=\"font-size:11px;background-color:#ddd;padding:4px\" width=\"5px;\">" +
          "<b>7" +
          "</b>" +
          "</td>" +
          "</tr>" +
          "</tbody>" +
          "</table>" +
          "</a>" +
          "</div>" +
          "<div style=\"float: left; border:1px solid #ddd;padding:4px; \">" +
          "<table width=\"33%\">" +
          "<tbody>" +
          "<tr>" +
          "<td>" +
          "<img src=\"http://www.medknow.com\" alt=\"PMC citations\">" +
          "</td>" +
          "<td style=\"font-size:11px;background-color:#ddd;padding:4px\" width=\"5px\">" +
          "<a target=\"_blank\" href=\"http://www.ncbi.nlm.nih.gov\">" +
          "<b>3" +
          "</b>" +
          "</a>" +
          "</td>" +
          "</tr>" +
          "</tbody>" +
          "</table>" +
          "</div>" +
          "</font>" +
          "</td>" +
          "</tr></table></div>";

  private static final String  tocHtml = 
      "<table>" +
          "<tr>" +
          "    <td width=\"90%\" colspan=\"2\" class=\"tochead\">EDITORIAL</td>" +
          "    <td width=\"10%\"  class=\"tochead\">&nbsp;</td>" +
          "  </tr>" +
          " <tr>" +
          "   <td width=\"100%\" colspan=\"3\" height=\"15px\"></td>" +
          " </tr>" +
          "  <tr>" +
          "    <td class=\"other\" vAlign=\"top\" align=\"center\" width=\"20%\" rowSpan=\"3\">" +
          "    <input type='checkbox' name='sai' value='jpgm_2015_X_Y_Z'><br></td>" +
          "    <td width=\"75%\" class=\"articleTitle\" style='margin:left:5px;'>Article Title for Editorial</td>" +
          "    <td width=\"5%\" rowspan=\"3\" class=\"other\" valign=\"top\" align=\"right\">p. 73</td>" +
          "  </tr>" +
          "  <tr>" +
          "    <td class=\"sAuthor\">P Ran, NJ Goo<br><b>DOI</b>:1X.1111/0022-3859.153101&nbsp;&nbsp;<b>PMID</b>:55555555</td>" +
          "  </tr>" +
          "  <tr>" +
          "    <td class=\"other\" style='text-align:left;'>" +
          "<a class=\"toc\" href=\"article.asp?foo\" title=\"Click to View Full Text of the article.\">[HTML Full text]</a>" +
          "&nbsp;&nbsp;" +
          "<a class=\"toc\" href=\"article.asp?foo;type=2\" title=\"Click to download PDF version of the article.\">[PDF]</a>" +
          "&nbsp;&nbsp;" +
          "<a class=\"toc\" href=\"article.asp?foo;type=3\" title=\"Click to View Full Text in Mobile format.\">[Mobile Full text]</a>" +
          "&nbsp;&nbsp;" +
          "<a class=\"toc\" href=\"article.asp?foo;type=4\" title=\"Click to download as ePub file.\">[EPub]</a>" +
          "&nbsp;&nbsp;" +
          "<a class=\"toc\" target='_blank' href='http://www.ncbi.nlm.nih.gov/pubmed/25766335'>[PubMed]</a>" +
          "&nbsp;&nbsp;" +
          "<a class=\"toc\" href=\"article.asp?foo;type=5\" title=\"Click to download Sword Plugin for Repository file.\">" +
          "[Sword Plugin for Repository]</a><sup>Beta</sup></td>" +
          "  </tr>" +
          "<tr>" +
          "    <td width=\"90%\" colspan=\"2\"  class=\"tochead\">ORIGINAL ARTICLES</td>" +
          "  </tr>" +
          "  <tr>" +
          "    <td width=\"75%\" class=\"articleTitle\" style='margin:left:5px;'>Another TItle for an Original Article</td>" +
          "  </tr>" +
          "  <tr>" +
          "    <td class=\"sAuthor\" style='line-height:18px;'>P Author, P Writer<br>" +
          "     <b>DOI</b>:1X.1111/0022-3859.150442&nbsp;&nbsp;<b>PMID</b>:66666666" +
          "    <div id='a' style='display:none;background-color:#eaeaea;border:1px solid #ddd;padding:5px;'>" +
          "       abstract goes here </div></td>" +
          "  </tr>" +
          " </table>";

  private static final String tocHtmlKept = 
      "<td width=\"90%\" colspan=\"2\" class=\"tochead\">EDITORIAL</td>" +
          "<td width=\"10%\"  class=\"tochead\">&nbsp;</td>" +
          "<td width=\"75%\" class=\"articleTitle\" style='margin:left:5px;'>Article Title for Editorial</td>" +
          "<td class=\"sAuthor\">P Ran, NJ Goo<br><b>DOI</b>:1X.1111/0022-3859.153101&nbsp;&nbsp;<b>PMID</b>:55555555</td>" +
          "<td width=\"90%\" colspan=\"2\"  class=\"tochead\">ORIGINAL ARTICLES</td>" +
          "<td width=\"75%\" class=\"articleTitle\" style='margin:left:5px;'>Another TItle for an Original Article</td>" +
          "<td class=\"sAuthor\" style='line-height:18px;'>P Author, P Writer<br>" +
          "     <b>DOI</b>:1X.1111/0022-3859.150442&nbsp;&nbsp;<b>PMID</b>:66666666" +
          "    <div id='a' style='display:none;background-color:#eaeaea;border:1px solid #ddd;padding:5px;'>" +
          "       abstract goes here </div></td>";


  private static final String articleHtml = 
      "<div><table><tr><td class=\"articlepage\" >" +
          "<div>"+
          "<font class=\"AuthorAff\">Foo, Blah, Blah</font>" +
          "</div>" +
          "</td></tr></table></div>";

  private static final String articleHtmlKept = 
      "<td class=\"articlepage\" >" +
          "<div></div>" +
          "</td>";

  private static final String onlyKept = "<td class=\"articlepage\" ></td>";


  /*
   *  Compare Html and HtmlHashFiltered
   */

  public void testAbstract() throws Exception {

    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(articleHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(articleHtmlKept, StringUtil.fromInputStream(actIn));

  }


  public void testCitationCounts() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(citationCounts),
        Constants.DEFAULT_ENCODING);
    assertEquals(onlyKept, StringUtil.fromInputStream(actIn));
  }

  public void testTOC() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(tocHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(tocHtmlKept, StringUtil.fromInputStream(actIn));
  }



}
