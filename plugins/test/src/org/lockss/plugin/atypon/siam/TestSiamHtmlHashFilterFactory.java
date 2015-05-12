/*
 * $Id$
 */
/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.siam;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.IOUtil;
import org.lockss.util.StringUtil;

public class TestSiamHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private SiamHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new SiamHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }


  private static final String citedBySection =
      "<div class=\"citedBySection\"><a name=\"citedBySection\"></a><h2>Cited by</h2>" +
          "<div class=\"citedByEntry\"> (2012) Convergence TITLE <i><span class=\"NLM_source\">More Title</span></i> <b>65</b>:6, 759-789" +
          "<span class=\"CbLinks\"><!--noindex--><a class=\"ref\" href=\"http://dx.doi.org/10.1002/cpa.21384\"" +
          " target=\"_blank\" title=\"Opens new window\"> CrossRef </a><!--/noindex--></span></div></div></div>";
  private static final String citedBySectionFiltered =
      "</div>";

  private static final String citationEntry =
      "<div>" +
          "<div id=\"art1\" class=\"notSelectedRow\">" +
          "<table class=\"articleEntry\">" +
          "<tr>" +
          "<td valign=\"top\">" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.1137/x\"><div class=\"art_title\">Split</div></a>" +
          "<strong><a class=\"entryAuthor\" href=\"/fu\">Name Cai</a></strong>" +
          "<div class=\"citation tocCitation\">Multiscale Model. Simul. 8-2 (2010)," +
          "<span class=\"ciationPageRange\">pp. 337-369</span>" +
          "<a href=\"http://dx.doi.org/10.1137/x\" class=\"ref doi\">http://dx.doi.org/10.1137/x</a>" +
          " |  Cited <b>40</b> times" +
          " </div>" +
          " <div class=\"doiCrossRef\"></div>" +
          " <div class=\"pubDate\">Online Publication Date: 2009-2010</div>" +
          " <a class=\"ref nowrap\" href=\"/doi/abs/10.1137/x\">Abstract</a> |" +
          " <a class=\"ref nowrap\" href=\"/doi/ref/10.1137/x\">References</a> |" +
          " <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1137/x\">PDF (1862 KB)</a>" +
          " <script type=\"text/javascript\">genSfxLinks('s0', '', '10.1137/x');</script>" +
          " </td>" +
          " </tr>";
  private static final String citationEntryFiltered =
      "<div>" +
          "<div id=\"art1\" class=\"notSelectedRow\">" +
          "<table class=\"articleEntry\">" +
          "<tr>" +
          "<td valign=\"top\">" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.1137/x\"><div class=\"art_title\">Split</div></a>" +
          "<strong><a class=\"entryAuthor\" href=\"/fu\">Name Cai</a></strong>" +
          " <div class=\"doiCrossRef\"></div>" +
          " <div class=\"pubDate\">Online Publication Date: 2009-2010</div>" +
          " <a class=\"ref nowrap\" href=\"/doi/abs/10.1137/x\">Abstract</a> |" +
          " <a class=\"ref nowrap\" href=\"/doi/ref/10.1137/x\">References</a> |" +
          " <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1137/x\"></a>" +
          " </td>" +
          " </tr>";

  private static final String citedAnchorHtml =
      "<ul class=\"linkList blockLinks separators centered\" id=\"articleToolList\">" +
          "<li class=\"articleToolLi showAbstract current\">" +
          "    <a href=\"/doi/abs/10.1137/110840546\">Abstract</a>" +
          "</li>" +
          "<li class=\"articleToolLi showEnhancedAbstract\">" +
          "    <a href=\"/doi/ref/10.1137/110840546\">References</a>" +
          "</li>" +
          "<li class=\"articleToolLi showPDF\">" +
          "    <a href=\"/doi/pdf/10.1137/110840546\" target=\"\">" +
          "        PDF" +
          "    </a>" +
          "</li>" +
          " <li>" +
          "   <a href=\"#citedBySection\">Cited By</a>" +
          "</li>" +
          "</ul>DONE";
  private static final String citedAnchorHtmlFiltered =
      "DONE";

  private static final String institutionBanner =
      "<div class=\"view-inner\">" +
          "<div class=\"institutionBanner\"><span class=\"bannerText\">Your access</span><img id=\"accessLogo\" " +
          "src=\"/userimages/2939/banner\" alt=\"Stanford University\"  /></div>" +
          "</div>";
  private static final String institutionBannerFiltered =
      "<div class=\"view-inner\">" +
          "</div>";


  private static final String scriptFindIt =
      "<span class=\"ciationPageRange\">pp. 337-369</span><a href=\"http://dx.doi.org/10.1137/x\" class=\"ref doi\">" +
          "http://dx.doi.org/10.1137/x</a> |  Cited <b>40</b> times</div>" +
          "<div class=\"doiCrossRef\"></div><div class=\"pubDate\">Online Publication Date: 2009-2010</div>" +
          " <a class=\"ref nowrap\" href=\"/doi/abs/10.1137/x\">Abstract</a> | <a class=\"ref nowrap\"" +
          "href=\"/doi/ref/10.1137/x\">References</a> | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\"" +
          " href=\"/doi/pdf/10.1137/x\">PDF (1862 KB)</a>" +
          " <script type=\"text/javascript\">" +
          "           genSfxLinks('s0', '', '10.1137/x');" +
          "       </script></td>";
  private static final String scriptFindItFiltered =
      "<span class=\"ciationPageRange\">pp. 337-369</span><a href=\"http://dx.doi.org/10.1137/x\" class=\"ref doi\">" +
          "http://dx.doi.org/10.1137/x</a> | Cited <b>40</b> times</div>" +
          "<div class=\"doiCrossRef\"></div><div class=\"pubDate\">Online Publication Date: 2009-2010</div>" +
          " <a class=\"ref nowrap\" href=\"/doi/abs/10.1137/x\">Abstract</a> | <a class=\"ref nowrap\"" +
          "href=\"/doi/ref/10.1137/x\">References</a> | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\"" +
          " href=\"/doi/pdf/10.1137/x\"></a>" +
          " </td>";

  private static final String footer =
      "<div id=\"footer\">" +
          "<div id=\"footer\">" +
          "<div class=\"copyright\">© " +
          "2013 SIAM By using <i>SIAM Publications Online</i> you agree to abide by the" +
          "<a href=\"http://www.siam.org\" style=\"text-decoration: underline;\">" +
          "Terms and Conditions of Use.</a> <span class=\"footerRight\"><a href=\"http://www.siam.org/\" " +
          "target=\"_blank\"><img alt=\"\" src=\"/templates/jsp/fu.png\" border=\"0\" /></a></span>" +
          "<br />" +
          "<i>Banner art .</i>" +
          "<br />" +
          "Powered by <a href=\"http://www.atypon.com\" target=\"_blank\">Atypon&reg; Literatum</a>" +
          "<br />" +
          "</div>" +
          "    </div>" +
          "</div>" +
          "</body>";
  private static final String footerFiltered =
      "</body>";


  private static final String leftColumn =
      "<div class=\"yui3-g \">" +
          "  <div class=\"yui3-u yui3-u-1-4 \" >" +
          "    <div class=\"inner\">" +
          "      <div class=\"dropzone ui-corner-all \" id=\"dropzone-Left-Sidebar\">" +
          "" +
          "<div  id=\"widget-4796\" class=\"widget type-browseVolume ui-helper-clearfix\">" +
          "  <div class=\"view\">" +
          "    <div class=\"view-inner\">" +
          "      <div class=\"panel panel_476\" id=\"volumelisting\"> " +
          "        <div class=\"box\"> " +
          "          <div class=\"header \"> <h3>Browse Volumes</h3> </div> " +
          "          <div class=\"no-top-border\"> " +
          "            <div class=\"box-inner\"> <a href=\"http://epubs.siam.org/toc/mmsubt/11/3\"> Issue in Progress </a> </div> " +
          "          </div> " +
          "        </div> " +
          "      </div> " +
          "    </div>" +
          "  </div>" +
          "</div>" +
          "" +
          "<div id=\"widget-4801\" class=\"widget type-authorSearch ui-helper-clearfix\">" +
          "  <div class=\"view\">" +
          "    <div class=\"view-inner\">" +
          "      <div class=\"authorSearchBox\"></div>" +
          "    </div>" +
          "  </div>" +
          "</div>" +
          "" +
          "      </div>" +
          "    </div>" +
          "  </div>" +
          "</div>";
  private static final String leftColumnFiltered =
      "<div class=\"yui3-g \">" +
          " <div class=\"yui3-u yui3-u-1-4 \" >" +
          " <div class=\"inner\">" +
          " </div>" +
          " </div>" +
          "</div>";

  private static final String rightColumn =
      "<div id=\"articleContent\"><p class=\"fulltext\"></p><!-- abstract content -->" +
          "<div class=\"landSubjectHeading\"></div>" +
          "  <h1 class=\"arttitle\">A Title</h1>" +
          "<div id=\"pubHisDataDiv\">" +
          "<div id=\"publicationDataPanel\" class=\"panel\">" +
          "<div class=\"box\">" +
          "<div class=\"header\"><h3>Related Databases</h3></div>" +
          "<div class=\"box-inner\"><h3 class=\"keywords\">Web of Science</h3> You must be logged in with an active subscription to view this.</div></div></div>" +
          "<div id=\"historyPanel\" class=\"panel\">" +
          "<div class=\"box\">" +
          "<div class=\"header\"><h3>Article Data</h3></div>" +
          "<div class=\"box-inner\">" +
          "<div><h3 class=\"keywords\">History</h3>" +
          "<div>Submitted: 19  May  2011</div>" +
          "<div>Accepted: 19 October 2011</div>" +
          "<div>Published online: 06 March 2012</div></div>" +
          "<div><h3 class=\"keywords\">Keywords</h3></div>" +
          "<div><h3 class=\"keywords\">AMS Subject Headings</h3>" +
          "</div>" +
          "</div></div></div>" +
          "<div id=\"publicationDataPanel\" class=\"panel\">" +
          "<div class=\"box\">" +
          "<div class=\"header\"><h3>Publication Data</h3></div>" +
          "</div>" +
          "</div>" +
          "</div></div>";

  private static final String rightColumnFiltered =
      "<div id=\"articleContent\"><p class=\"fulltext\"></p>" +
          "<div class=\"landSubjectHeading\"></div>" +
          " <h1 class=\"arttitle\">A Title</h1>" +
          "</div>";

  private static final String header =
      "<div id=\"header\">" +
          "<!-- placeholder id=null, description=Maintenance Message -->" +
          "  <div class=\"welcome stackContents\">" +
          "    <div id=\"topHeaderBar\">" +
          "      <div class=\"searchBar\">" +
          "        <div class=\"quicksearch\">" +
          "          <div id=\"quickSearchTabs\" class=\"yui-navset quickSearch\">" +
          "            <ul class=\"yui-nav\">" +
          "              <li class=\"selected\">" +
          "                <a href=\"#search\">Keyword</a>" +
          "              </li>" +
          "              <li >" +
          "                <a href=\"#citation\">Citation</a>" +
          "              </li>" +
          "            </ul>" +
          "          </div>" +
          "        </div>" +
          "      </div>" +
          "      <div id=\"bannerImage\"></div>" +
          "      <div class=\"loginIdentity\"></div>" +
          "      <div class=\"stacked\"></div>" +
          "    </div>" +
          "  </div>" +
          "</div>BOOM";

  private static final String headerFiltered =
      "BOOM";

  private static final String free =
      "<!--totalCount12--><!--modified:1374685645000-->" +
          "<div><div><div id=\"art110842545\" class=\"notSelectedRow\">" +
          "<table class=\"articleEntry\">" +
          "<tr><td align=\"left\" valign=\"top\" width=\"18\">" +
          "<br>" +
          "</br>" +
          "<img src=\"/templates/jsp/_style2/_pagebuilder/_c3/_siam/images/access_full.gif\" alt=\"full access\" title=\"full access\" class=\"accessIcon\" />" +
          "</td>" +
          "<td valign=\"top\"><a class=\"ref nowrap\" href=\"/doi/abs/10.1137/110842545\">" +
          "<div class=\"art_title\">TITLE</div>" +
          "</a>" +
          "<div class=\"citation tocCitation\">Multiscale Model. Simul. 10-1 (2012)," +
          "<span class=\"ciationPageRange\">pp. 1-27" +
          "</span>" +
          "<a href=\"http://dx.doi.org/10.1137/110842545\" class=\"ref doi\">http://dx.doi.org/10.1137/110842545" +
          "</a> |  Cited " +
          "<b>1" +
          "</b> time" +
          "</div>" +
          "<div class=\"doiCrossRef\">" +
          "</div>" +
          "<div class=\"pubDate\">Online Publication Date: January 2012" +
          "</div>" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.1137/110842545\">Abstract" +
          "</a> | " +
          "<a class=\"ref nowrap\" href=\"/doi/ref/10.1137/110842545\">References" +
          "</a> | " +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1137/110842545\">PDF (328 KB)" +
          "</a> " +
          "</td>" +
          "</tr>" +
          "</table>" +
          "</div>" +
          "</div>";

  private static final String freeFiltered =
      "<div><div><div id=\"art110842545\" class=\"notSelectedRow\">" +
          "<table class=\"articleEntry\">" +
          "<tr><td align=\"left\" valign=\"top\" width=\"18\">" +
          "<br>" +
          "</br>" +
          "</td>" +
          "<td valign=\"top\"><a class=\"ref nowrap\" href=\"/doi/abs/10.1137/110842545\">" +
          "<div class=\"art_title\">TITLE</div>" +
          "</a>" +
          "<div class=\"doiCrossRef\">" +
          "</div>" +
          "<div class=\"pubDate\">Online Publication Date: January 2012" +
          "</div>" +
          "<a class=\"ref nowrap\" href=\"/doi/abs/10.1137/110842545\">Abstract" +
          "</a> | " +
          "<a class=\"ref nowrap\" href=\"/doi/ref/10.1137/110842545\">References" +
          "</a> | " +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1137/110842545\">" +
          "</a> " +
          "</td>" +
          "</tr>" +
          "</table>" +
          "</div>" +
          "</div>";

  private static final String changeableCommentsHtml =
      "<label for=\"markall\">Select All</label>" +
          "<hr/>" +
          "<!--totalCount14--><!--modified:1368461028000--><div>" +
          "<div><div id=\"art120871894\" class=\"notSelectedRow\">";
  private static final String changeableCommentsFiltered=
      "<label for=\"markall\">Select All</label>" +
          "<hr/>" +
          "<div>" +
          "<div><div id=\"art120871894\" class=\"notSelectedRow\">";

  private static final String adChunkHtml=
      "</div>" +
          "<div class=\"mainAd\">" +
          "<div><!-- placeholder id=null, description=Site Ad 1 --></div>" +
          "<div><!-- placeholder id=null, description=Site Ad 2 --></div>" +
          "<div><!-- placeholder id=null, description=Site Ad 3 --></div>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "<div id=\"footer\"></div>";
  private static final String adChunkFiltered=
      "</div>" +
          "</div>" +
          "</div>";

  public void testCitations() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(citedBySection),
        ENC);
    assertEquals(citedBySectionFiltered,StringUtil.fromInputStream(inA));
    inA = fact.createFilteredInputStream(mau, new StringInputStream(citationEntry),
        ENC);
    assertEquals(citationEntryFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(citedAnchorHtml),
        ENC);
    assertEquals(citedAnchorHtmlFiltered, StringUtil.fromInputStream(inA));
  }

  public void testTopBottom() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(header),
        ENC);
    assertEquals(headerFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(institutionBanner),
        ENC);
    assertEquals(institutionBannerFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(scriptFindIt),
        ENC);
    assertEquals(scriptFindItFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(footer),
        ENC);
    assertEquals(footerFiltered,StringUtil.fromInputStream(inA));
  }

  public void testSideBars() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(leftColumn),
        ENC);
    assertEquals(leftColumnFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(rightColumn),
        ENC);
    assertEquals(rightColumnFiltered,StringUtil.fromInputStream(inA));


  }


  public void testFreeGlyph() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(free),
        ENC);
    assertEquals(freeFiltered,StringUtil.fromInputStream(inA));

  }


  public void testComments() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(changeableCommentsHtml),
        ENC);
    assertEquals(changeableCommentsFiltered,StringUtil.fromInputStream(inA));

  }

  public void testAds() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(adChunkHtml),
        ENC);
    assertEquals(adChunkFiltered,StringUtil.fromInputStream(inA));

  }

}
