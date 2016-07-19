/*
 * $Id$
 */
/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.copernicus;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestCopernicusHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private CopernicusHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new CopernicusHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String withHighlights = 
      "<h2>Highlight articles</h2>" +
      "<div id=\"highlight_articles\"><!--// Highlights start-->" +
      "<!-- ITEM 12-->" +
      "<div class=\"j-news-item\"><span class=\"j-news-item-thumb\">" +
      "<a href=\"http://www.test-journal.net/9/2689/2016/\">" +
      "<img alt=\"\" cofileid=\"27325\" src=\"http://www.test-journal.net/ACME-9-2689-2016-f07_50x50.png\" />" +
      "</a>             </span>" +
      "<div class=\"j-news-item-content\">" +
      "<div class=\"j-news-item-header\">" +
      "<!-- the following link is what we're filtering -->" +
      "<div><a href=\"http://www.test-journal.net/9/2689/2016/\">Test Title</a>" +
      "<a class=\"toggle-summary-link\" onclick=\"MM_showHideLayers('Layer12','','show')\" href=\"#12\"><img alt=\"\" cofileid=\"20138\" src=\"http://www.test-journal.net/ACME_icon_summary.png\" />" +
      "</a></div>" +
      "<span class=\"j-news-item-date\">28 Jun 2016</span></div>" +
      "<div id=\"Layer12\" class=\"expanded-text\">" +
      "<p><a onclick=\"MM_showHideLayers('Layer12','','show')\" href=\"#12\"><img border=\"0\" alt=\"\" style=\"padding-left: 15px; float: right\" src=\"https://contentmanager.hello.org/9726/445/locale/ssl\" /></a>A very short article.</p>" +
      "</div>" +
      "<p class=\"j-news-item-body\">A. D. Abacus, and S. D. Calculator</p>" +
      "</div>" +
      "</div>" +
      "</div>";

  private static final String withoutHighlights = 
      "<h2>Highlight articles</h2>" ;
  
  private static final String withRecentPaper = 
      "<div id=\"recent_paper\" class=\"cmsbox j-article j-article-section\">" +
      "<h2 class=\"title\">Recent articles</h2>" +
      "<div class=\"a-paper\">" +
      "<div class=\"journal-type\"> ACME </div>" +
      "</div>" +
      "</div> <p> Recent Papers </p>";
  private static final String withoutRecentPaper = 
      " <p> Recent Papers </p>";

  private static final String withHeaderCurrentCover = 
      "<div id=\"w-head\">" +
      "<a id=\"j-banner\" href=\"http://www.acme.net/index.html\">" +
      "<span id=\"j-cover\" class=\"t-j-cover-standard\">" +
      "<span class=\"g-hidden g-text\">Journal cover</span>" +
      "</span> </div> <p> Header Current Cover </p>";

  private static final String withoutHeaderCurrentCover = 
      " <p> Header Current Cover </p>"; 
  
  private static final String withLandingPage = 
      "<div id=\"landing_page\" class=\"cmsbox j-intro-section j-section\">" +
      "<div class=\"jo_journal-intro\">" +
      "<div class=\"jo_intro-column\" id=\"jo_lp-cover-container\">" +
      "<img id=\"open_pop_up\" title=\"ACME cover\" alt=\"ACME cover\" cofileid=\"12607\" " +
      "src=\"http://www.acme.net/graphic_ACME_cover_homepage.jpg\">" +
      "</div>" +
      "</div>" +
      "</div> <p> Landing Page </p>";

  private static final String withoutLandingPage = 
      " <p> Landing Page </p>"; 
  
  private static final String withNewsContainer = 
      "<div id=\"news_container\" class=\"cmsbox \"><h2>News</h2>" +
      "<div id=\"news\">" +
      "<div class=\"j-news-item\"><a href=\"http://www.hello.org/news_and_press/2016.html\" target=\"_blank\">" +
      "<span class=\"j-news-item-thumb\"><img width=\"50px\" src=\"http://www.acme.net/logo-tib-icon.png\" /></span></a>" +
      "<div class=\"j-news-item-content\">" +
      "<div class=\"j-news-item-header\">" +
      "<h4>Institutional agreement for ACME authors affiliated with the Leibniz Universit&auml;t Hannover</h4>" +
      "<span class=\"j-news-item-date\">11 Jan 2016</span></div>" +
      "<p class=\"j-news-item-body\">blurb. <a href=\"http://www.hello.org/news_and_press/tib-agreement.html\" target=\"_blank\">" +
      "<img src=\"http://www.acme.net/graphic_grey_right_symbol.png\"/></a></p>" +
      "</div></div></div>" +
      "</div> <p> News Container </p>";

  private static final String withoutNewsContainer = 
      " <p> News Container </p>";
  
  private static final String withCenterLogos = 
      "<div id=\"essentential-logos-carousel\" class=\"cmsbox \">" +
      "<ul class=\"essentential-logos\">" +
      "<li class=\"essentential-logo\"> " +  
      "<a href=\"http://www.acme.net/about/licence_and_copyright.html\" title=\"CC-BY 3.0\"" +
      "class=\"essential-logo__link\"><img src=\"http://www.acme.net/ejl_CC-BY-30.png\"" +
      "class=\"essential-logo__media\"/></a>" +
      "<a href=\"http://creativecommons.org/licenses/by/3.0/\" title=\"license CC-BY 3.0 \" "+
      "class=\"essential-logo__link-hidden\" rel=\"license\">CC-BY 3.0</a>" +
      "</li></div> <p> essentential logos carousel (sic)</p>";

  private static final String withoutCenterLogos = 
      " <p> essentential logos carousel (sic)</p>";
    
  public void testFiltering() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withHighlights),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutHighlights, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withLandingPage),
        Constants.DEFAULT_ENCODING);
   assertEquals(withoutLandingPage, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withHeaderCurrentCover),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutHeaderCurrentCover, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withNewsContainer),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutNewsContainer, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withRecentPaper),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutRecentPaper, StringUtil.fromInputStream(inStream));

    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(withCenterLogos),
        Constants.DEFAULT_ENCODING);
    assertEquals(withoutCenterLogos, StringUtil.fromInputStream(inStream));
  
  }

}
