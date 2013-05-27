/*
 * $Id: TestSiamHtmlHashFilterFactory.java,v 1.3 2013-05-27 19:10:07 alexandraohlson Exp $
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
          " <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1137/x\">PDF (1862 KB)</a>" +
          "  </td>" +
          " </tr>";

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
          "http://dx.doi.org/10.1137/x</a> |  Cited <b>40</b> times</div>" +
          "<div class=\"doiCrossRef\"></div><div class=\"pubDate\">Online Publication Date: 2009-2010</div>" +
          " <a class=\"ref nowrap\" href=\"/doi/abs/10.1137/x\">Abstract</a> | <a class=\"ref nowrap\"" +
          "href=\"/doi/ref/10.1137/x\">References</a> | <a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\"" +
          " href=\"/doi/pdf/10.1137/x\">PDF (1862 KB)</a>" +
          " </td>";

  private static final String footer =
      "<div id=\"footer\">" +
          "<div id=\"footer\">" +
          "<div class=\"copyright\">©" +
          "2013 SIAM By using <i>SIAM Publications Online</i> you agree to abide by the" +
          "<a href=\"http://www.siam.org\" style=\"text-decoration: underline;\">" +
          "Terms and Conditions of Use.</a> <span class=\"footerRight\"><a href=\"http://www.siam.org/\" " +
          "target=\"_blank\"><img alt=\"\" src=\"/templates/jsp/fu.png\" border=\"0\" /></a></span>" +
          "<br />" +
          "<i>Banner art .</i>" +
          "<br />" +
          "Powered by <a href=\"http://www.atypon.com\" target=\"_blank\">Atypon¨ Literatum</a>" +
          "<br />" +
          "</div>" +
          "    </div>" +
          "</div>" +
          "</body>";
  private static final String footerFiltered =
      "</body>";

  private static final String sessionHistory =
      "<div class=\"view\">" +
          "<div class=\"view-inner\">" +
          "            <div class=\"panel panel_476\"  id=\"sessionHistory\">" +
          "<div class=\"box\">" +
          "   <div class=\"header \">" +
          "        <h3>Session History</h3>" +
          "    </div>" +
          "        <div class=\"no-top-border\">" +
          "        <div class=\"box-inner\">" +
          "   <div class=\"sessionViewed\">" +
          "       <div class=\"label\">Recently Viewed</div>" +
          "       <ul class=\"sessionHistory\" >" +
          "          <li><a href=\"/doi/abs/10.1137/x\">Split Bregman Methods and Frame Based Image Restoration</a></li>" +
          "  </ul>" +
          "   </div></div></div></div></div></div></div>BOO";
  private static final String sessionHistoryFiltered =
      "<div class=\"view\">" +
          "<div class=\"view-inner\">" +
          "            </div></div>BOO";


  private static final String sideBars =

      "<div class=\"panel\">" +
          "<div class=\"box collapsible open\">" +
          "    <div class=\"header publicationSideBar\">" +
          "        <h3>Notify Me!</h3>" +
          "    </div>" +
          "    <div class=\"box-inner\">" +
          "        <div>" +
          "                    <a href=\"/action/fu?action=addJournal&amp;journalCode=mmsubt\">" +
          "                        E-mail Alerts" +
          "                    </a>" +
          "        </div>" +
          "        <div>" +
          "                    <a href=\"/action/showFeed?type=etoc&amp;feed=rss&amp;jc=mmsubt\">" +
          "                        RSS Feeds" +
          "                    </a>" +
          "        </div>" +
          "    </div>" +
          "    <div class=\"footer\"></div>" +
          "</div>" +
          "</div>";
  private static final String sideBarsFiltered =
      "<div class=\"panel\">" +
          "</div>";

  private static final String browseVolumes =
      "<div class=\"view\">" +
          "<div class=\"view-inner\">" +
          "<div class=\"panel panel_476\" id=\"volumelisting\"> <div class=\"box\">" +
          "<div class=\"header \"> <h3>Browse Volumes</h3> </div>" +
          "<div class=\"no-top-border\">" +
          "<div class=\"box-inner\"> <a href=\"http://epubs.siam.org/\"> Issue in Progress </a>" +
          "<div id=\"selectyearrange\"> Year Range:&nbsp;" +
          "<select id=\"volyears\" onchange=\"return showDecade();\"> 2010 2010" +
          "<option value=\"dec_2010s\" selected>2010-Current</option> 2000 2010" +
          "<option value=\"dec_2000s\">2000-2009</option>" +
          "</select> </div> <div id=\"decadeList\">" +
          "<div class=\"decade\" id=\"dec_2010s\" style=\"\"> <div class=\"volume\" id=\"vol_10_2010s\">" +
          "<div class=\"volInfo\" onclick=\"return showVolume('vol_10_2010s')\">" +
          "<div class=\"volNum\">Volume 10</div> <div class=\"volYear\">2012</div> </div>" +
          "</div> </div> </div> </div> </div> <div class=\"footer\"></div> </div> </div>" +
          "<script type=\"text/javascript\">" +
          "</script>" +
          "</div>" +
          "</div>" +
          "</div>";
  private static final String browseVolumesFiltered =
      "<div class=\"view\">" +
          "<div class=\"view-inner\">" +
          "</div>" +
          "</div>" +
          "</div>";
  
  private static final String issueNavHtml =
      "     <div id=\"issueNav\">" +
          " <div id=\"prevNextNav\">" +
          "     <div id=\"issueSearch\">" +
          "         <form action=\"/action/doSearch\" method=\"get\"><input type=\"text\" name=\"searchText\" value=\"\" size=\"17\" />" +
          "             <input type=\"hidden\" name=\"issue\" value=\"4\" />" +
          "             <input type=\"hidden\" name=\"journalCode\" value=\"mmsubt\" />" +
          "             <input type=\"hidden\" name=\"volume\" value=\"10\" />" +
          "             <input type=\"hidden\" name=\"filter\" value=\"issue\" />" +
          "             <input type=\"submit\" value=\"Search Issue\" /></form>" +
          "     </div>" +
          "     <a href=\"javascript:toggleSlide('issueSearch')\">Search Issue</a> |" +      
          "        <img src=\"/templates/jsp/_style2/_pagebuilder/_c3/images/rss_32.png\" />" +
          "<a href=\"http://epubs.siam.org/action/showFeed?ui=0&mi=3d6gcp&ai=s9&jc=mmsubt&type=etoc&feed=rss\">RSS</a>" +     
          "    <br/>" +      
          "        <a href=\"/toc/mmsubt/10/3\">Previous Issue</a>" +           
          "        <a href=\"/toc/mmsubt/11/1\"> Next Issue</a>" +      
          " </div>" +
          " <div id=\"coverDate\">";
  private static final String issueNavFiltered =
      "     <div id=\"issueNav\"> " +
          " <div id=\"coverDate\">";     
  
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
  }

  public void testInstitution() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(institutionBanner),
        ENC);
    assertEquals(institutionBannerFiltered,StringUtil.fromInputStream(inA));
    inA = fact.createFilteredInputStream(mau, new StringInputStream(scriptFindIt),
        ENC);
    assertEquals(scriptFindItFiltered,StringUtil.fromInputStream(inA));
  }

  public void testSideBars() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(footer),
        ENC);
    assertEquals(footerFiltered,StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(sessionHistory),
        ENC);
    assertEquals(sessionHistoryFiltered,StringUtil.fromInputStream(inA));
    inA = fact.createFilteredInputStream(mau, new StringInputStream(sideBars),
        ENC);
    assertEquals(sideBarsFiltered,StringUtil.fromInputStream(inA));
    inA = fact.createFilteredInputStream(mau, new StringInputStream(browseVolumes),
        ENC);
    assertEquals(browseVolumesFiltered,StringUtil.fromInputStream(inA));
  }
  
  public void testNavSearch() throws Exception {
    InputStream inA;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(issueNavHtml),
        ENC);
    assertEquals(issueNavFiltered,StringUtil.fromInputStream(inA));

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
