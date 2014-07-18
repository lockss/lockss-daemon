/*
 * $Id: TestBIRHtmlFilterFactory.java,v 1.1.2.2 2014-07-18 15:49:51 wkwilson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

/*
 * This test file tests both the hash and crawl filters  - two subclasses within
 * the larger test so they can share some html strings.
 */

package org.lockss.plugin.atypon.bir;

import junit.framework.Test;

import org.apache.commons.lang.RandomStringUtils;
import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestBIRHtmlFilterFactory extends LockssTestCase {

  public FilterFactory fact;

  //private static MockArchivalUnit mau;

  /*
   * SHARED HTML TEST SNIPPETS
   */
  private static final String topHtml=
      "<html lang=\"en\" class=\"pb-page\">" +
          "<head data-pb-dropzone=\"head\">" +
          "<script type=\"text/javascript\" src=\"/wro/product.js\"></script>" +
          "FOO" +
          "</head>" +
          "<body>" +
          "<div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">" +
          "<section class=\"widget literatumAd alignCenter  widget-none  widget-compact-all\" id=\"xx\">" +
          "<div class=\"pb-ad\">" +
          "<a href=\"/action/clickThrough?id=15\"><img src=\"foo.jpg\" alt=\"NewTom What's Next\"/></a>" +
          "</div>" +
          "</section></div>";
  private static final String topHtmlFiltered=
      "<html lang=\"en\" class=\"pb-page\">" +
          "<body>" +
          "<div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">" +
          "</div>";

  private static final String pageHeader=
      "<div>" +    
          "<section class=\"widget pageHeader none  widget-none  widget-compact-all\" id=\"pageHeader\">" +
          "<div class=\"page-header\">" +
          "<div data-pb-dropzone=\"main\">" +
          "</div></div>" +
          "</section>" +
          "</div>";
  private static final String pageHeaderFiltered=
      "<div>" +
          "</div>";


  private static final String banner=
      "<div data-pb-dropzone=\"center\">" +
          "<section class=\"widget general-image alignRight  widget-none  widget-compact-all\" id=\"xx\">" +
          "<div class=\"wrapped 1_12\" >" +
          "<div class=\"widget-body body body-none  body-compact-all\"><img src=\"foo.png\"/></div>" +
          "</div>" +
          "</section>" +
          "</div>";
  private static final String bannerFiltered=
      "<div data-pb-dropzone=\"center\">" +
          "</div>";

  private static final String tocNav=
      "<div>" +
          "<section class=\"widget menuXml none  widget-none  widget-compact-all\" id=\"xx\">" +
          "<ul class=\"shadow primaryNav\">" +
          "<li><a class=\"expander\" href=\"#\">Journals</a><ul>" +
          "<li class=\"\"><a href=\"/toc/bjr/current\">BJR</a></li>" +
          "<li class=\"\"><a href=\"/toc/dmfr/current\">DMFR</a></li>" +
          "<li class=\"\"><a href=\"/toc/img/current\">Imaging</a></li>" +
          "<li class=\"\"><a href=\"/page/archivedcollection\">Archived Collection</a></li>" +
          "</ul>" +
          "</li>" +
          "</ul>" +
          "</section>" +
          "</div>";
  private static final String tocNavFiltered=
      "<div>" +
          "</div>";


  private static final String toolPulldown=
      "<div class=\"publicationTooldropdownContainer\">" +
          "<select name=\"articleTool\" class=\"items-choice publicationToolSelect\" title=\"Article Tools\">" +
          "<option value=\"\">Please select</option>" +
          "<option value=\"/action/addFavoritePublication\">Add to Favourites</option>" +
          "<option value=\"/action/addCitationAlert\">Track Citation</option>" +
          "<option value=\"/action/showMultipleAbstracts\">View Abstracts</option>" +
          "<option value=\"/action/showCitFormats\">Download Citation</option>" +
          "<option value=\"/action/showMailPage\">Email</option>" +
          "</select>" +
          "</div>";
  private static final String toolPulldownFiltered=
      "";


  private static final String navHtml=
      "<div data-pb-dropzone=\"right\" >" +
          "<section class=\"widget literatumBookIssueNavigation none  widget-none\" id=\"xx\">" +
          "<div class=\"pager issueBookNavPager\">" +
          "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +
          "<tr>" +
          "<td class=\"journalNavLeftTd\">" +
          "<div class=\"prev placedLeft\">" +
          "</div>" +
          "</td>" +
          "<td class=\"journalNavRightTd\">" +
          "<div class=\"next\">" +
          "</div>" +
          "</td>" +
          "</tr>" +
          "</table>" +
          "</div>" +
          "</section>" +
          "</div>";
  private static final String navHtmlFiltered=
      "<div data-pb-dropzone=\"right\" >" +
          "</div>";


  private static final String social=
      "<section class=\"widget general-bookmark-share none  widget-none\" id=\"xx\">" +
          "<div class=\"addthis_toolbox addthis_default_style\">" +
          "<a class=\"addthis_button_email\"></a>" +
          "<a class=\"addthis_button_facebook\"></a>" +
          "<a class=\"addthis_button_twitter\"></a>" +
          "<a class=\"addthis_button_linkedin\"></a>" +
          "<a class=\"addthis_button_google_plusone_share\"></a>" +
          "<a class=\"addthis_button_compact\"></a>" +
          "</div>" +
          "</section>";
  private static final String socialFiltered=
      "";


  private static final String accessIcon=
      "<div class=\"tocContent\">" +
          "<!--totalCount10--><!--modified:1403021921000-->" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr><td class=\"accessIconContainer\"><div></div></td>" +
          "<td align=\"right\" valign=\"top\" width=\"10\"><input class=\"tocToolCheckBox\" type=\"checkbox\" name=\"doi\" value=\"10.1259/xxx\" />" +
          "</td>" +
          "</tr>" +
          "</table>" +
          "</div>";
  private static final String accessIconFiltered=
      "<div class=\"tocContent\">" +
          "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr>" +
          "<td align=\"right\" valign=\"top\" width=\"10\"><input class=\"tocToolCheckBox\" type=\"checkbox\" name=\"doi\" value=\"10.1259/xxx\" />" +
          "</td>" +
          "</tr>" +
          "</table>" +
          "</div>";

  private static final String artTab=
      "<div class=\"tabs tabs-widget\">" +
          "<ul class=\"tab-nav\">" +
          "<li class=\"active\">" +
          "<a href=\"foo\">Abstract</a>" +
          "</li>" +
          "<li>" +
          "<a href=\"foo\">Full Text</a>" +
          "</li>" +
          "</ul>" +
          "</div>";
  private static final String artTabFiltered=
      "<div class=\"tabs tabs-widget\">" +
          "</div>";


  private static final String rightCol=
      "<div>" +
          "<section class=\"widget literatumArticleToolsWidget none  widget-regular  widget-compact-all widget-border-toggle\" id=\"xx\">" +
          "<div class=\"articleTools\">" +
          "<ul class=\"blockLinks\">" +
          "<li class=\"addToFavs\">" +
          "<a href=\"/action/addFavoritePublication?doi=10.1259%2Fbjr.20130727\">Add to Favourites</a>" +
          "</li>" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?doi=10.1259%2Fbjr.20130727\">Download Citation</a>" +
          "</li>" +
          "<li class=\"trackCitations\">" +
          "<a href=\"/action/addCitationAlert?doi=10.1259%2Fbjr.20130727\">Track Citations</a>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</section>" +
          "</div>";
  private static final String rightColFiltered=
      "<div>" +
          "</div>";
// remove the OTHER <li> items, but not the trimmed path to the one we want
  private static final String rightColCrawlFiltered=
      "<div>" +
          "<section class=\"widget literatumArticleToolsWidget none  widget-regular  widget-compact-all widget-border-toggle\" id=\"xx\">" +
          "<div class=\"articleTools\">" +
          "<ul class=\"blockLinks\">" +
          "<li class=\"downloadCitations\">" +
          "<a href=\"/action/showCitFormats?doi=10.1259%2Fbjr.20130727\">Download Citation</a>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</section>" +
          "</div>";

  private static final String jumpTo=
      "<div class=\"tab-content\">" +
          "<div class=\"tab tab-pane active\">" +
          "<article class=\"article\">" +
          "<!-- abstract content -->" +
          "<a name=\"abstract\"></a>" +
          "<div class=\"sectionInfo\">" +
          "<div class=\"sectionHeading\">Abstract</div>" +
          "<div class=\"sectionJumpTo\">" +
          "<div class=\"content\">" +
          "<div class=\"sectionLabel\">Section:</div>" +
          "<form style=\"margin-bottom:0\">" +
          "<select name=\"select23\" class=\"fulltextdd\" onChange=\"GoTo(this, 'self')\">" +
          "<option value=\"#\" selected=\"#\">Choose</option>" +
          "<option value=\"#\">Top of page</option>" +
          "<option value=\"\">Abstract &lt;&lt;</option>" +
          "<option value=\"#_i6\">METHODS AND MATERIALS</option>" +
          "<option value=\"#_i10\">RESULTS</option>" +
          "<option value=\"#_i15\">DISCUSSION</option>" +
          "<option value=\"#_i16\">REFERENCES</option>" +
          "</select>" +
          "</form>" +
          "<div class=\"nextPrevSec\">" +
          "<a href=\"#_i6\" class=\"down\"></a>" +
          "</div>" +
          "</div>" +
          "</div>";
  private static final String jumpToFiltered=
      "<div class=\"tab-content\">" +
          "<div class=\"tab tab-pane active\">" +
          "<article class=\"article\">" +
          "<a name=\"abstract\"></a>" +
          "<div class=\"sectionInfo\">" +
          "<div class=\"sectionHeading\">Abstract</div>";


  private static final String relatedHtml=
      "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr><td class=\"accessIconContainer\">" +
          "</td><td align=\"right\" valign=\"top\" width=\"10\">" +
          "<input class=\"tocToolCheckBox\" type=\"checkbox\" name=\"doi\" value=\"10.1259/dmfr.20139012\" />" +
          "</td>" +
          "<td valign=\"top\">" +
          "<div class=\"art_title noLink\">" +
          "Notice of redundant publication</div>" +
          "<a href=\"#\" class=\"relatedLink\">" +
          "Related Content</a>" +
          "<div class=\"relatedLayer\">" +
          "<div class=\"category\">" +
          "Original Article" +
          "<ul><li><a class=\"ref nowrap\" href=\"/foo\">foo title</a>" +
          "</li></ul>" +
          "</div></div>" +
          "</td></tr>" +
          "</table>";
  private static final String relatedHtmlFiltered=
      "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr>" +
      "<td align=\"right\" valign=\"top\" width=\"10\">" +
          "<input class=\"tocToolCheckBox\" type=\"checkbox\" name=\"doi\" value=\"10.1259/dmfr.20139012\" />" +
          "</td>" +
          "<td valign=\"top\">" +
          "<div class=\"art_title noLink\">" +
          "Notice of redundant publication</div>" +
          "</td></tr>" +
          "</table>";
  private static final String relatedHtmlCrawlFiltered=
      "<table border=\"0\" width=\"100%\" class=\"articleEntry\"><tr><td class=\"accessIconContainer\">" +
          "</td><td align=\"right\" valign=\"top\" width=\"10\">" +
          "<input class=\"tocToolCheckBox\" type=\"checkbox\" name=\"doi\" value=\"10.1259/dmfr.20139012\" />" +
          "</td>" +
          "<td valign=\"top\">" +
          "<div class=\"art_title noLink\">" +
          "Notice of redundant publication</div>" +
          "</td></tr>" +
          "</table>";  

  // crawl filter only
  private static final String refsHtml=
      "<span>" +
          "<div class=\"references\">" +
          "<tr><td class=\"refnumber\" id=\"b1\">1 .</td>" +
          "reference info goes here" +
          "</td>" +
          "</tr>" +
          "<br /></div>" +
          "</span>";
  private static final String refsHtmlFiltered=
      "<span>" +
          "</span>";

  /**
   * CRAWL FILTER VARIANT
   */
  public static class TestCrawl extends TestBIRHtmlFilterFactory {


    public void setUp() throws Exception {
      super.setUp();
      fact = new BIRAtyponHtmlCrawlFilterFactory();
    }

    public void testCorrections() throws Exception {

      assertEquals(refsHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(refsHtml),
              Constants.DEFAULT_ENCODING)));
      assertEquals(relatedHtmlCrawlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(relatedHtml),
              Constants.DEFAULT_ENCODING)));


      /* and make sure we *don't* crawl filter out citation link */
      /* test that the filter keeps the entire chunk intact */
      assertEquals(rightColCrawlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(rightCol),
              Constants.DEFAULT_ENCODING)));

    }


  }


  /**
   * HASH FILTER VARIANT
   */
  public static class TestHash extends TestBIRHtmlFilterFactory {

    public void setUp() throws Exception {
      super.setUp();
      fact = new BIRAtyponHtmlHashFilterFactory();
    }

    public void testArticlePageHash() throws Exception {
      assertEquals(artTabFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(artTab),
              Constants.DEFAULT_ENCODING)));
      assertEquals(rightColFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(rightCol),
              Constants.DEFAULT_ENCODING)));
      assertEquals(jumpToFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(jumpTo),
              Constants.DEFAULT_ENCODING)));

    }


    public void testTOCPageHash() throws Exception {

      assertEquals(topHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream( topHtml),
              Constants.DEFAULT_ENCODING)));       
      assertEquals(pageHeaderFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(pageHeader),
              Constants.DEFAULT_ENCODING)));
      assertEquals(bannerFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(banner),
              Constants.DEFAULT_ENCODING)));
      assertEquals(tocNavFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(tocNav),
              Constants.DEFAULT_ENCODING)));
      assertEquals(toolPulldownFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(toolPulldown),
              Constants.DEFAULT_ENCODING)));
      assertEquals(navHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(navHtml),
              Constants.DEFAULT_ENCODING)));
      assertEquals( socialFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(social),
              Constants.DEFAULT_ENCODING)));
      assertEquals(accessIconFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(accessIcon),
              Constants.DEFAULT_ENCODING)));

      assertEquals(relatedHtmlFiltered,
          StringUtil.fromInputStream(fact.createFilteredInputStream(null,
              new StringInputStream(relatedHtml),
              Constants.DEFAULT_ENCODING)));

    }


  }

  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
    });
  }

  protected static String rand() {
    return RandomStringUtils.randomAlphabetic(30);
  }

}
