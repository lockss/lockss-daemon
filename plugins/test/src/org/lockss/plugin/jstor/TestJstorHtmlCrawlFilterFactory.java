/*  $Id$

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

package org.lockss.plugin.jstor;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestJstorHtmlCrawlFilterFactory extends LockssTestCase {
  private JstorHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new JstorHtmlCrawlFilterFactory();
  }


  private static final String errata =
      "<li>" +
          "<div class=\"cite\">" +
          "<div class=\"subCite\">" +
          "<input type=\"checkbox\" name=\"doi\" class=\"checkBox\" value=\"10.1111/foo\" id=\"10.1111/foo\" />" +
          "</div>" +
          "<div class=\"mainCite\">" +
          "<div class=\"bd langMatch\">" +
          "<div class=\"title\">" +
          "<!-- ty: fla -->" +
          "<a class=\"title\" href=\"/stable/10.1111/foo\">" +
          "Title <i>" +
          "More</i>" +
          "</a>" +
          "(pp. 302-315)&nbsp;&nbsp;</div>" +
          "<div class=\"author\">" +
          "Terry Foo</div>" +
          "<div class=\"stable\">" +
          "<div class=\"doi\">" +
          "DOI: 10.1111/foo</div>" +
          "Stable URL: &#104;&#116;&#116;&#112;&#058;&#047;&#047;www.jstor.org/stable/10.1111/foo</div>" +
          "</div>" +
          "<div class=\"ft articleLinks\">" +
          "<a class=\"pdflink\" data-articledoi=\"10.1111/foo\" target=\"_blank\" href=\"/stable/pdfplus/10.1111/foo.pdf\">" +
          "Article PDF</a>" +
          "<span class=\"articleLinks\">" +
          "<a href=\"/stable/10.1111/erratum_foo\">" +
          "Erratum</a>" +
          "</span>" +
          "<span class=\"articleLinks\">" +
          "<a href=\"/stable/info/10.1111/foo\">" +
          "Article Summary</a>" +
          "</span>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</li>";
  private static final String errataFiltered = 
      "<li>" +
          "<div class=\"cite\">" +
          "<div class=\"subCite\">" +
          "<input type=\"checkbox\" name=\"doi\" class=\"checkBox\" value=\"10.1111/foo\" id=\"10.1111/foo\" />" +
          "</div>" +
          "<div class=\"mainCite\">" +
          "<div class=\"bd langMatch\">" +
          "<div class=\"title\">" +
          "<!-- ty: fla -->" +
          "<a class=\"title\" href=\"/stable/10.1111/foo\">" +
          "Title <i>" +
          "More</i>" +
          "</a>" +
          "(pp. 302-315)&nbsp;&nbsp;</div>" +
          "<div class=\"author\">" +
          "Terry Foo</div>" +
          "<div class=\"stable\">" +
          "<div class=\"doi\">" +
          "DOI: 10.1111/foo</div>" +
          "Stable URL: &#104;&#116;&#116;&#112;&#058;&#047;&#047;www.jstor.org/stable/10.1111/foo</div>" +
          "</div>" +
          "<div class=\"ft articleLinks\">" +
          "<a class=\"pdflink\" data-articledoi=\"10.1111/foo\" target=\"_blank\" href=\"/stable/pdfplus/10.1111/foo.pdf\">" +
          "Article PDF</a>" +
          "<span class=\"articleLinks\">" +
          "</span>" +
          "<span class=\"articleLinks\">" +
          "<a href=\"/stable/info/10.1111/foo\">" +
          "Article Summary</a>" +
          "</span>" +
          "</div>" +
          "</div>" +
          "</div>" +
          "</li>";

  private static final String rightCol=
      "<div class=\"body jstor brandingStripe\">" +
      "  <div class=\"rightCol myYahoo\">" +
      "    <div id=\"rrSkipNav\"><a href=\"#mainContent\" class=\"ufo\">Skip to Main Content</a></div>" +
      "    <div class=\"login\">" +
      "      <div id=\"logo\"><a href=\"/\" title=\"JSTOR: Home\"><img src=\"jstor_logo.jpg\" height=\"60\" width=\"47\" alt=\"JSTOR\" /></a></div>" +
      "      <ul class=\"menu support\">" +
      "      <li>" +
      "      <a href=\"https://www.jstor.org\">Login</a>" +
      "      </li>" +
      "      <li><a href=\"http://about.jstor.org\">Help</a></li>" +
      "      <li><a href=\"http://about.jstor.org\">About</a></li>" +
      "      </ul>" +
      "      <div class=\"welcome\"></div>" +
      "    </div>" +
      "  </div>" +
      "</div>";
  private static final String rightColFiltered=
      "<div class=\"body jstor brandingStripe\">" +
      "  " +
      "</div>";

      private static final String refList =
      "<div class=\"indent refContainer\">" +
      "<ul class=\"citeList clear\">" +
      "<li id=\"atypb1\">" +
      "blah" +
      "<div class=\"citationLinks\">" +
      "</div>" +
      "</li>" +
      "<li id=\"r001\">" +
      "1.reference item" +
      "<div class=\"citationLinks\">" +
      "<a href=\"#rid_r001\">" +
      "First citation in article</a>" +
      "</div>" +
      "</li>" +
      "</ul>" +
      "</div> ";
      private static final String refListFiltered =
      "<div class=\"indent refContainer\">" +
      "</div> ";

      private static final String issueNav =
      "<div id=\"issueNav\" class=\"lastUnit\">" +
      " <span class=\"previous\"> " +
      "   <a href=\"/stable/foo\">&laquo; &nbsp;Previous Item</a></span>" +
      "   &nbsp;|&nbsp;" +
      " <span class=\"next\">" +
      "   <a href=\"/stable/foo\">Next Item&nbsp; &raquo;</a></span>" +
      "</div>";
      private static final String issueNavFiltered =
      "";



  /*
   *  Compare Html and HtmlFiltered
   */

  public void testErrataFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(errata), Constants.DEFAULT_ENCODING);
    assertEquals(errataFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testColumnFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(rightCol), Constants.DEFAULT_ENCODING);
    assertEquals(rightColFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testNavFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(issueNav), Constants.DEFAULT_ENCODING);
    assertEquals(issueNavFiltered, StringUtil.fromInputStream(actIn));
  }
  public void testReferencesFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(refList), Constants.DEFAULT_ENCODING);
    assertEquals(refListFiltered, StringUtil.fromInputStream(actIn));
  }

}
