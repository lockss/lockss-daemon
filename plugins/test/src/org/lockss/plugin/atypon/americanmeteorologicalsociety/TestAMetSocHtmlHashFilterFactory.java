/*
 * $Id$
 */
/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.atypon.americanmeteorologicalsociety;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestAMetSocHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AMetSocHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AMetSocHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  /* cited by section */
  private static final String citedByHtml = 
      "<div class=\"citedBySection\">" +
          "<a name=\"citedBySection\"></a>" +
          "<h2>Cited by</h2>" +
          "<div class=\"citedByEntry\"><span class=\"author\">Jr-Chuan Huang</span>, " +
          "<span class=\"author\">Cheng-Ku Yu</span>.  (2012) Linking typhoon tracks and spatial rainfall. <i>" +
          "<span class=\"NLM_source\">Water Resources Research</span></i> " +
          "<b>48</b>:9, <br />Online publication date: 1-Jan-2012.<br />" +
          "<span class=\"CbLinks\">" +
          "<a class=\"ref\" href=\"http://dx.doi.org/10.1029/2011WR011508\" target=\"_blank\" title=\"Opens new window\"> CrossRef </a>" +
          "</span>" +
          "</div>" +
          "</div><!-- /fulltext content -->" +
          "</div>";

  private static final String citedByFiltered = 
      "</div>";

  /* copyright in the footer of pages */
  private static final String copyrightHtml = 
      "<div id=\"footer\">" +
          "<script type=\"text/javascript\" async=\"true\" src=\"http://www.google-analytics.com/ga.js\"></script>" +
          "<ul>" +
          "<li><img src=\"/templates/jsp/_style2/_AP/_ams/images/amsseal-blue3.jpg\" /></li>" +
          "<li>" +
          "<b><a target=\"_blank\" href=\"http://www.ametsoc.org/pubs/cr_2005.html\">© 2012</a> American Meteorological Society <a target=\"_blank\" href=\"http://www.ametsoc.org/disclaim.html\">Privacy Policy and Disclaimer</a>" +
          "<br />" +
          "Headquarters: 45 Beacon Street Boston, MA 02108-3693" +
          "<br />" +
          " DC Office: 1120 G Street, NW, Suite 800 Washington DC, 20005-3826" +
          "<br />" +
          "<a target=\"_blank\" href=\"mailto:amsinfo@ametsoc.org\">amsinfo@ametsoc.org</a> Phone: 617-227-2425 Fax: 617-742-8718" +
          "<br />" +
          "<a target=\"_blank\" href=\"http://allenpress.com/\">Allen Press, Inc</a>. assists in the online publication of AMS journals</b>" +
          "</li>" +
          "</ul>" +
          "<div id=\"atypon\">" +
          "<img src=\"/templates/jsp/_style2/_AP/_ams/images/pwrd_by_atypon.gif\" />" +
          "</div>" +
          "<div class=\"clearfix\">&nbsp;</div>" +
          "</div>\n" +
          "</div>";

  private static final String copyrightFiltered = 
      " </div>";

  /* institutionBanner section */
  private static final String headerHtml =
      "        <div id=\"header\">" +
          "<!-- placeholder id=null, description=Header - Mobile Button -->" +
          "<div id=\"identity-bar\">" +
          "    <span id=\"institution-banner-text\">" +
          "    </span>" +
          "</div>" +
          "<img src=\"/templates/jsp/_style2/_AP/_ams/images/ams_banner.gif\" alt=\"AMS Journals\" width=\"980\" height=\"59\" />" +
          "<div class=\"headerContainer\" id=\"headerBottomContainer\">" +
          "    <div id=\"quickSearchBox\">" +
          "    </div>" +
          "    <div class=\"headerSubContainer\" id=\"headerBottomSubContainer\">" +
          "        <ul class=\"headerNavBar\" id=\"headerBottomNavBar\">" +
          "            <li><a href=\"/\">Journals</a></li>" +
          "            <li><a href=\"http://www.ametsoc.org/pubs/subscribe/index.html\">Subscribe</a></li>" +
          "            <li><a href=\"http://www.ametsoc.org/pubs/arcindex.html\">For Authors</a></li>" +
          "            <li><a href=\"/page/information\">Information</a></li>" +
          "            <li><a href=\"/help\">Online Help</a></li>" +
          "        </ul>" +
          "    </div>" +
          "</div>" +
          "    <div id=\"breadcrumbs\">" +
          "    </div>" +
          "        </div>BOOM";


  private static final String headerFiltered =
      " BOOM";


  /* script call that generates institution specific thingie */
  private static final String scriptHtml = 
      " <script type=\"text/javascript\">" +
          " <!-- // hide it from old browsers" +
          " var prefix0 = \"http%3A%2F%2Flibrary.stanford.edu%2Fsfx\";" +
          " function genSfxLink0(id, url, doi) {" +
          " var href = \"javascript:popSfxLink(prefix0,'\"+id+\"','\"+url+\"','\"+doi+\"')\";" +
          " var name =  \"null\";" +
          " if( name == null || name == \"\" || name == \"null\") name = \"OpenURL STANFORD UNIV. GREEN LIBRARY\";" +
          " var height = 20;" +
          " var width = 85;" +
          " document.write('<a href=\"'+href+'\" title=\"'+name+'\">');" +
          " document.write('<img src=\"/userimages/2097/sfxbutton\" alt=\"'+name+'\" border=\"0\" valign=\"bottom\" height=\"' + height + '\" width=\"' + width + '\" />');" +
          " }" +
          " // stop hiding -->" +
          " </script>" +
          "<div id=\"leftColumn\">" +
          "<div class=\"panel panel_228\"  id=\"journalNavPanel\">";

  private static final String scriptFiltered = " <div id=\"leftColumn\">";

  /* current issue will change over time */
  private static final String journalNavHtml =
      "        <div id=\"content\">" +
          "            <div class=\"panelContent\">" +
          "                <div id=\"leftColumn\">" +
          "<div class=\"panel panel_228\"  id=\"journalNavPanel\">" +
          "    <div class=\"panelTopLeft\"></div>" +
          "    <div class=\"panelTopMiddle panel_228_width\">" +
          "        <h3>Volume 2 Issue 4 <br /> (October 2010)</h3>" +
          "    </div>" +
          "    <div class=\"panelBody panel_228_pad\">" +
          "            <div id=\"nextprev\">" +
          "            </div><br/>" +
          "        <!-- placeholder id=null, description=Issue Navigator Ad -->" +
          "    </div>" +
          "    <div class=\"panelBottomLeft\"></div>" +
          "    <div class=\"panelBottomMiddle panel_228_width\"></div>" +
          "    <div class=\"panelBottomRight\"></div>" +
          "</div>" +
          "<div class=\"panel panel_228\"  id=\"journalInfoPanel\">" +
          "    <div class=\"panelTopLeft\"></div>" +
          "    <div class=\"panelTopMiddle panel_228_width\">" +
          "        <h3>Journal Information</h3>" +
          "    </div>" +
          "</div>" +
          "<div class=\"panel panel_228\" id=\"journalInfoPanel\">" +
          "    <!-- placeholder id=null, description=Journal Info Ad -->" +
          "</div>" +
          "<div class=\"panel panel_228\"  id=\"sitetoolsPanel\">" +
          "    <div class=\"panelTopLeft\"></div>" +
          "    <div class=\"panelTopMiddle panel_228_width\">" +
          "        <h3>&nbsp;</h3>" +
          "    </div>" +
          "    <div class=\"panelTopRight\"></div>" +
          "    <div class=\"panelBody panel_228_pad\">" +
          "<h2 style=\"text-align: center;\"> </h2>" +
          "    </div>" +
          "    <div class=\"panelBottomLeft\"></div>" +
          "    <div class=\"panelBottomMiddle panel_228_width\"></div>" +
          "    <div class=\"panelBottomRight\"></div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</div>";


  private static final String journalNavFiltered = 
      " <div id=\"content\">" +
          "<div class=\"panelContent\">" +
          "<div id=\"leftColumn\">" +
          "</div>" +
          "</div>" +
          "</div>";


  private static final String referenceInstitutionHtml=
      "<td valign=\"top\">Benedict" +
          "<span class=\"NLM_x\">, </span>J. J." +
          "<span class=\"NLM_year\">2004</span>: " +
          "<span class=\"NLM_article-title\">Synoptic view of the North Atlantic Oscillation</span>. " +
          "<span class=\"citation_source-journal\"><i>J. Atmos. Sci.</i></span>, " +
          "<b>61</b>, " +
          "<span class=\"NLM_fpage\">121</span>" +
          "<span class=\"NLM_lpage\">144</span>" +
          "<a class=\"ref\" href=\"javascript:newWindow('http://dx.doi.org/10.1175%2F1520-0469%282004%29061%3C0121%3ASVOTNA%3E2.0.CO%3B2')\">[Abstract]</a>" +
          "<a href=\"/servlet/linkout?suffix=bib1&amp;dbid=16384&amp;doi=10.1175/JAS-D-11-0289.1&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dams%26aulast%3DBenedict%26aufirst%3DJ.%2BJ.%26date%3D2004%26atitle%3DSynoptic%2Bview%2Bof%2Bthe%2BNorth%2BAtlantic%2BOscillation%26stitle%3DJ.%2BAtmos.%2BSci.%26volume%3D61%26spage%3D121%26id%3Ddoi%3A10.1175%252F1520-0469%25282004%2529061%253C0121%253ASVOTNA%253E2.0.CO%253B2\" title=\"OpenURL STANFORD UNIV. GREEN LIBRARY\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">" +
          "<img src=\"/userimages/2097/sfxbutton\" alt=\"OpenURL STANFORD UNIV. GREEN LIBRARY\" />" +
          "</a>" +
          "</td>";
  private static final String referenceInstitutionFiltered= 
      "<td valign=\"top\">Benedict" +
          "<span class=\"NLM_x\">, </span>J. J." +
          "<span class=\"NLM_year\">2004</span>: " +
          "<span class=\"NLM_article-title\">Synoptic view of the North Atlantic Oscillation</span>. " +
          "<span class=\"citation_source-journal\"><i>J. Atmos. Sci.</i></span>, " +
          "<b>61</b>, " +
          "<span class=\"NLM_fpage\">121</span>" +
          "<span class=\"NLM_lpage\">144</span>" +
          "<a class=\"ref\" href=\"javascript:newWindow('http://dx.doi.org/10.1175%2F1520-0469%282004%29061%3C0121%3ASVOTNA%3E2.0.CO%3B2')\">[Abstract]</a>" +
          "</td>";

  private static final String scanTableHtml1=
      "<div id=\"s3a\" class=\"NLM_sec NLM_sec_level_2\">  <span class=\"title2\" id=\"d2160184e521\">a. QA1: blah</span>" +
          "  <p>blah <i>G</i><sub>0</sub> blah (<a class=\"ref NLM_xref-bibr\" " +
          "href=\"javascript:popRef2('i1558-8432-47-4-1006-Iqbal1')\">Iqbal 1983</a>):                                           <table " +
          "class=\"formula\" style=\"width:100%;vertical-align:middle\" align=\"center\" border=\"0\" " +
          "cellpadding=\"0\" cellspacing=\"0\" id=\"i1558-8432-47-4-1006-eq1\"><tr><td width=\"90%\" align=\"center\">" +
          "<img src=\"/na101/blah.gif\" alt=\"\" id=\"_e1\"/></td></tr></table>";

  private static final String scanTableHtml2=
      "<div id=\"s3a\" class=\"NLM_sec NLM_sec_level_2\">  <span class=\"title2\" id=\"d801221e521\">a. QA1: blah</span>" +
          "  <p>blah <i>G</i><sub>0</sub> blah (<a class=\"ref NLM_xref-bibr\" " +
          "href=\"javascript:popRef2('i1558-8432-47-4-1006-Iqbal1')\">Iqbal 1983</a>):      <table " +
          "class=\"formula\" style=\"width:100%;vertical-align:middle\" align=\"center\" border=\"0\" " +
          "cellpadding=\"0\" cellspacing=\"0\" id=\"i1558-8432-47-4-1006-eq1\"><tr><td width=\"90%\" align=\"center\">" +
          "<img src=\"/na101/blah.gif\" alt=\"\" id=\"_e1\"/></td></tr></table>";

  private static final String changeableCommentsHtml =
      "<div id=\"tocContent\"><!--totalCount11--><!--modified:1368793519000--><h2 class=\"tocHeading\">" +
          "<span class=\"subj-group\">ARTICLES </span></h2><table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">1501</td>" +
          "<td align=\"right\" valign=\"top\" width=\"18\" class=\"tocCheck\">" +
          "<input type=\"checkbox\" name=\"doi\" value=\"10.1175/XXX1234\"/>" +
          "<img src=\"/templates/jsp/_style2/_AP/images/access_free.gif\" alt=\"open access\" title=\"open access\" class=\"accessIcon\" /></td>" +
          "<td valign=\"top\" width=\"85%\"><div class=\"art_title\">A Big Long Title Here" +
          "</div>" +
          "<span class=\"author\">Author P Writer</span>, <span class=\"author\">Samuel K. Writer</span><br />" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.1175/XXX1234\">Abstract</a>" +
          "<a class=\"ref nowrap\" href=\"/doi/full/10.1175/XXX1234\">Full Text</a>";
  private static final String changeableCommentsFiltered =
      "<div id=\"tocContent\"><h2 class=\"tocHeading\">" +
          "<span class=\"subj-group\">ARTICLES </span></h2><table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
          "<tr>" +
          "<td align=\"right\" valign=\"top\" width=\"18\" class=\"nowrap\">1501</td>" +
          "<td align=\"right\" valign=\"top\" width=\"18\" class=\"tocCheck\">" +
          "<input type=\"checkbox\" name=\"doi\" value=\"10.1175/XXX1234\"/>" +
          "</td>" +
          "<td valign=\"top\" width=\"85%\"><div class=\"art_title\">A Big Long Title Here" +
          "</div>" +
          "<span class=\"author\">Author P Writer</span>, <span class=\"author\">Samuel K. Writer</span><br />" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.1175/XXX1234\">Abstract</a>" +
          "<a class=\"ref nowrap\" href=\"/doi/full/10.1175/XXX1234\">Full Text</a>";

  private static final String emptyHTagOne =
      "<div class=\"panel panel_228\"  id=\"sitetoolsPanel\">" +
          "    <div class=\"panelTopLeft\"></div>" +
          "    <div class=\"panelTopMiddle panel_228_width\">" +
          "    <h3></h3>" +
          "    </div>" +
          "    <div class=\"panelTopRight\"></div>" +
          "    <div class=\"panelBody panel_228_pad\">" +
          "" +
          "&nbsp;" +
          "" +
          "" +
          "    </div>" +
          "    <div class=\"panelBottomLeft\"></div>" +
          "    <div class=\"panelBottomMiddle panel_228_width\"></div>" +
          "    <div class=\"panelBottomRight\"></div>" +
          "</div>";

  private static final String emptyHTagTwo =
      "<div class=\"panel panel_228\"  id=\"sitetoolsPanel\">" +
          "    <div class=\"panelTopLeft\"></div>" +
          "    <div class=\"panelTopMiddle panel_228_width\">" +
          "       <h3>&nbsp;</h3>" +
          "    </div>" +
          "    <div class=\"panelTopRight\"></div>" +
          "    <div class=\"panelBody panel_228_pad\">" +
          "        " +
          "<h2 style=\"text-align: center;\"> </h2>" +
          "" +
          "        " +
          "    </div>" +
          "    <div class=\"panelBottomLeft\"></div>" +
          "    <div class=\"panelBottomMiddle panel_228_width\"></div>" +
          "    <div class=\"panelBottomRight\"></div>" +
          "</div>";
  
  private static final String spaceInClassName = 
      "<p>" + 
          "<span class=\"author\">Fred Q. Writer</span><br />" +
          "<a class=\"ref nowrap  \" href=\"/doi/abs/10.1175/12345\">Abstract</a>" +
          "</p>";
  private static final String spaceInClassNameFiltered = 
      "<p>" + 
          "<span class=\"author\">Fred Q. Writer</span><br />" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.1175/12345\">Abstract</a>" +
          "</p>";

  
          public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(headerHtml),
        ENC);
    assertEquals(headerFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(referenceInstitutionHtml),
        ENC);
    assertEquals(referenceInstitutionFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(scriptHtml),
        ENC);
    assertEquals(scriptFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(journalNavHtml),
        ENC);
    assertEquals(journalNavFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(copyrightHtml),
        ENC);
    assertEquals(copyrightFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(citedByHtml),
        ENC);
    assertEquals(citedByFiltered,StringUtil.fromInputStream(inA));

    /* in this case, check against two versions that really existed - filter both and compare result */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(scanTableHtml1),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(scanTableHtml2),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(changeableCommentsHtml),
        ENC);
    assertEquals(changeableCommentsFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(emptyHTagOne),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(emptyHTagTwo),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(spaceInClassName),
        ENC);
    assertEquals(spaceInClassNameFiltered,StringUtil.fromInputStream(inA));


  }
}
