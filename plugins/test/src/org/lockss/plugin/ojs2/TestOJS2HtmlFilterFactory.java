/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs2;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestOJS2HtmlFilterFactory extends LockssTestCase {
  private OJS2HtmlFilterFactory fact;
  private MockArchivalUnit mau;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new OJS2HtmlFilterFactory();
  }

  //NodeFilter[] filters = new NodeFilter[] {
  // Some OJS sites have a tag cloud
  //HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarKeywordCloud"),
  // Some OJS sites have a subscription status area
 // HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarSubscription"),
  // Some OJS sites have a language switcher, which can change over time
  // HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarLanguageToggle"),
  // Top-level menu items sometimes change over time
  // HtmlNodeFilters.tagWithAttribute("div", "id", "navbar"),
  // Popular location for sidebar customizations
  //HtmlNodeFilters.tagWithAttribute("div", "id", "custom"),
  
  private static final String sidebarKeywordCloudHtml =
		 "<div class=\"block\" id=\"sidebarUser\"><div id=\"sidebarKeywordCloud\"></div></div>";
  private static final String sidebarKeywordCloudHtmlFiltered =
      "";
  
  private static final String sidebarSubscriptionHtml =
      "<div id=\"sidebarSubscription\"><a class=\"blockTitle\" href=\"http://pkp.sfu.ca/ojs/\" id=\"developedBy\">Open Journal Systems</a></div>";
  private static final String sidebarSubscriptionHtmlFiltered =
      "";
  
  private static final String sidebarLanguageToggleHtml =
      "<div id=\"sidebarLanguageToggle\" class=\"block\">\n.....</div>";
  private static final String sidebarLanguageToggleHtmlFiltered =
        "";
  
  private static final String navbarHtml =
      "<div id=\"navbar\">\n" + 
      "  <ul class=\"menu\">\n........" + 
      "  </ul>\n......." + 
      "</div>";
  private static final String navbarHtmlFiltered =
      "";
  
  private static final String customHtml =
      "<div id=\"keepme\"><div id=\"custom\"></div></div>";
  private static final String customHtmlFiltered =
      "<div id=\"keepme\"></div>";
  
  // new TagNameFilter("script"),
  // Date accessed is a variable
  // HtmlNodeFilters.tagWithTextRegex("div", "Date accessed: "),
  // The version of the OJS software, which can change over time, appears in a tag
  // HtmlNodeFilters.tagWithAttribute("meta", "name", "generator"),
  // Header image with variable dimensions
  // HtmlNodeFilters.tagWithAttribute("div", "id", "headerTitle"),
  // For Ubiquity Press
  // HtmlNodeFilters.tagWithAttribute("div", "id", "rightSidebar"),
  // For JLIS.it: landing pages contain user view count
  // HtmlNodeFilters.tagWithAttribute("span", "class", "ArticleViews")
  
  
  private static final String scriptHtml =
      "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><script type=\"text/javascript\" " +
      "async=\"\" src=\"http://www.google-analytics.com/ga.js\"></script></head";
  private static final String scriptHtmlFiltered =
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">";
  
  private static final String dateAccessedHtml = 
    "<div class=\"separator\">" +
  		"</div><div id=\"citation\">SMITH, J., DOE, J..Article " +
  		"title that goes on for a little ways.<strong>Journal Title</strong>, North America, " +
  		"1,may. 2010. Available at: &lt;<a href=\"http://sampleurl." +
  		"net/index.php/more/path/here\" target=\"_new\">http://sampleurl" +
  		".net/index.php/more/path/here</a>&gt;. Date accessed" +
  		": 13 Jul. 2012.</div></div><div class=\"separator\"></div>";
  private static final String dateAccessedHtmlFiltered = 
    "</div>";
  
  private static final String generatorHtml =
      "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta name=\"generator\"></head>";
  private static final String generatorHtmlFiltered =
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">";
  
  private static final String headerImageHtml =
    "<div id=\"headerTitle\"><h1>" +
    "<img src=\"http://www.tellusa.net/public/journals/27/" +
    "pageHeaderTitleImage_en_US.jpg\" width=\"750\" height=\"90\" " +
    "alt=\"Page Header\" /></h1></div>";
  private static final String headerImageHtmlFiltered =
    "";
  
  private static final String rightSidebarHtml =
      "<div id=\"keepme\"><div id=\"rightSidebar\">\n" + 
      "<div id=\"sidebarRTAuthorBios\" class=\"block\">\n</div>\n" + 
      "<div id=\"sidebarRTRelatedItems\" class=\"block\">\n</div>\n" + 
      "</div></div>";
  private static final String rightSidebarHtmlFiltered =
      "<div id=\"keepme\"></div>";
  
  private static final String viewsHtml =
      "<div id=\"keepme\"><span class=\"ArticleViews\">- Views:<span content=\"UserPageVisits: \n" + 
      "234\n\" itemprop=\"interactionCount\"> \n234\n" + 
      "</span></span></div>";
  private static final String viewsHtmlFiltered =
      "<div id=\"keepme\"></div>";
  
  private static final String pqpHtml =
      "<div style=\"display: block;\" class=\"pQp hideDetails\" id=\"pqp-container\">\n" + 
      "<div class=\"console\" id=\"pQp\">\n" + 
      "some other stuff\n" + 
      "</div>\n" + 
      "</div>\n";
  private static final String pqpHtmlFiltered =
      " ";
  
  private static final String accessHtml =
      "<br>\n" + 
      "<b>Total de acessos: 455</b>\n";
  private static final String accessHtmlFiltered =
      " ";
  
  /*
   */
  private static final String footerHtml =
      "<body>\n" +
      "<div id=\"footer\">\n" + 
      "<div id=\"footerContent\">\n" + 
      "<div class=\"debugStats\">\n" + 
      "Página gerada em: 0,2359s<br />\n" + 
      "Número de requisições ao bancos de dados: 165<br/>\n" + 
      "Uso de Memória:: 6825496<br/>\n" + 
      "</div>\n" + 
      "</div><!-- footerContent -->\n" + 
      "</div><!-- footer -->\n" + 
      "</body>";
  private static final String footerHtmlFiltered =
      "<body> " +
      "</body>";
  
  public void testSidebarKeywordCloudFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(sidebarKeywordCloudHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(sidebarKeywordCloudHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testSidebarSubscriptionFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(sidebarSubscriptionHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(sidebarSubscriptionHtmlFiltered, StringUtil.fromInputStream(actIn));

  }

  public void testCustomFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(customHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(customHtmlFiltered, StringUtil.fromInputStream(actIn));

  }

  public void testDateAccessed() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(dateAccessedHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(dateAccessedHtmlFiltered, StringUtil.fromInputStream(actIn));

  }

  public void testHeaderImage() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(headerImageHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(headerImageHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testRemaining() throws Exception {
    InputStream actIn;
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(sidebarLanguageToggleHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(sidebarLanguageToggleHtmlFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(navbarHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(navbarHtmlFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(scriptHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(scriptHtmlFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(generatorHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(generatorHtmlFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(rightSidebarHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(rightSidebarHtmlFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(viewsHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(viewsHtmlFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(pqpHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(pqpHtmlFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(accessHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(accessHtmlFiltered, StringUtil.fromInputStream(actIn));
    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(footerHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(footerHtmlFiltered, StringUtil.fromInputStream(actIn));

  }

}
