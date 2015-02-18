/*
 * $Id$
 */

/* Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University, all rights reserved.

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
package org.lockss.plugin.atypon.nrcresearchpress;


import java.io.InputStream;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestNRCResearchPressHtmlHashFilterFactory extends LockssTestCase{
  private ClockssNRCResearchPressHtmlHashFilterFactory filt;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    filt = new ClockssNRCResearchPressHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }


  String testContent2 =
    "<script type=\"text/javascript\">"+
    "<!-- // hide it from old browsers" +
    "var prefix3 = \"http%3A%2F%2Flibrary.stanford.edu%2Fsfx\";" +
    "function genSfxLink3(id, url, doi) {" +
    "   var href = \"javascript:popSfxLink(prefix3,'\"+id+\"','\"+url+\"','\"+doi+\"')\""+
    "   var name =  \"null\";"+
    "    if( name == null || name == \"\" || name == \"null\") name = \"OpenURL STANFORD UNIVERSITY\";"+
    "   var height = 14;"+
    "   var width = 72;"+
    "   document.write('<a href=\"'+href+'\" title=\"'+name+'\">');"+
    "       document.write('<img src=\"/userimages/57853/sfxbutton\" alt=\"'+name+'\" border=\"0\" valign=\"bottom\" height=\"' + height + '\" width=\"' + width + '\" />');"+
    "       document.write('</a>');"+
    "}"+
    "// stop hiding -->"+
    "</script>Hello";
  String resultingContent2 =
    "Hello";
  

  public void testFilter() throws Exception {
    InputStream in2;


    in2 = filt.createFilteredInputStream(mau,
        new StringInputStream(testContent2), Constants.DEFAULT_ENCODING);
 
    assertEquals( resultingContent2, StringUtil.fromInputStream(in2));

  }     
  
  public void testSanitizedFile() throws Exception {
    InputStream inA;
    inA = filt.createFilteredInputStream(mau, new StringInputStream(sanitizedFileHtml), Constants.DEFAULT_ENCODING);
    assertEquals( resultingSanitizedFileHtml, StringUtil.fromInputStream(inA));
    
  }
  
  /* In order to catch a variety of filtered tags, sanitize an entire article file
   * and then run the filters across the whole thing
   */
  private static final String sanitizedFileHtml =
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
          "<head>" +
          "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" +
          "    <title>Title </title>" +
          "<meta name=\"robots\" content=\"noarchive,nofollow\" />" +
          "<meta name=\"MSSmartTagsPreventParsing\" content=\"true\"/>" +
          "</head>" +
          "<body >    <!-- placeholder id=null, description=leaderboard --><div class=\"leaderboard\"><!-- BEGIN JS TAG - BlahBlahy 728x90 < - DO NOT MODIFY -->" +
          "<!-- END TAG --></div>" +
          "    <!-- placeholder id=null, description=Header - Maintenance Message --> " +
          "    <!-- placeholder id=null, description=Header - Mobile Button -->" +
          "<div id=\"wrapper\">" +
          "" +
          "<div id=\"top-bar-wrapper\">" +
          "<div id=\"top-bar-1\">" +
          "  <div class=\"headerAd\"><!-- placeholder id=null, description=Header - Logo --><a href=\"/action/\"><img src=\"/blah.png\" alt=\"Logo\"></a></div>" +
          "  <div class=\"institutionBanner\" >Subscriber access provided by Stanford University - Green Library </div>" +
          "  <div id=\"identity-bar\">" +
          "    <span id=\"individual-menu\"></span>" +
          "  </div>" +
          "</div> <!-- top-bar-1 -->" +
          "<div id=\"top-bar-2\">" +
          "  <p id=\"tag-line\"><!-- placeholder id=null, description=Header - Slogan --><br style=\"line-height:19px\"/>" +
          "  <span>a not-for-profit publisher</span>" +
          "  </p>" +
          "</div> <!-- top-bar-2 -->" +
          "</div> <!-- top-bar-wrapper -->" +
          "<div class=\"banner\" style='background-image:url(/url/title.gif);'>" +
          "    <h1>Journale Title</h1>" +
          "</div>" +
          "<div id=\"nav-wrapper\">" +
          "nav-wrapper area" +
          "</div>" +
          "<div id=\"breadcrumbs\">" +
          "breadcrumbs" +
          "</div>" +
          "<div id=\"main\">" +
          "<div id=\"sidebar-left\">" +
          "   <a href=\"/journal/foo\">" +
          "    <img class=\"pubCoverImg\" src=\"/url/cover.jpg\" alt=\"Journal\" />" +
          "   </a>" +
          "<ul>" +
          "<li>" +
          "  <div class=\"header-bar header-gray\">Browse the journal</div>" +
          "  <ul>" +
          "  <li class=\"\">" +
          "    <a href=\"/loi/xxx\">List of issues</a>" +
          "  </li>" +
          "  </ul>" +
          "</li>" +
          "</ul>" +
          "<div class=\"ads\">" +
          "  <!-- placeholder id=null, description=Left Sidebar - Publications Ad 1 -->" +
          "<a href=\"/action/\"><img src=\"blah.jpg\"></a>" +
          "  <!-- placeholder id=null, description=Left Sidebar - Publications Ad 2 -->" +
          "</div>" +
          "<div class=\"ads\">" +
          " <!-- placeholder id=null, description=Left Sidebar - Ad 1 --><a href=\"/action/\"><img src=\"/blah.jpg\"></a>" +
          " <!-- placeholder id=null, description=Left Sidebar - Ad 2 --><div id=\"mission\" class=\"box-gray\" style=\"display: none; width:158px\">" +
          "   <h3 class=\"header-bar header-orange\">Mission</h3>" +
          "   <div class=\"box-pad\">" +
          "      <p>To support knowledge.</p>" +
          "        </div>" +
          "</div>" +
          "<!-- placeholder id=null, description=Left Sidebar - Ad 3 -->" +
          "</div>" +
          "</div> <!-- sidebar-left -->" +
          "<div id=\"content\">" +
          "<script>" +
          "script foo" +
          "</script>" +
          "        <div id='background' style=\"display:none\"></div>" +
          "<div class=\"border-gray\">" +
          "    <div class=\"box-pad\">" +
          "       <!-- articleType_en: Article--><!-- articleType_fr: Article-->" +
          "<ul class=\"icon-list-vertical box-gray-border box-pad clear\"><li><a class=\"icon-abstract\" title=\"View the Abstract\" href=\"/doi/abs/10.1139/blah\">" +
          "<span>Abstract</span></a></li><li><a class=\"icon-pdf\" href=\"/doi/pdf/10.1139/blah\">" +
          "PDF (207 K)" +
          "</a></li><li><a class=\"icon-pdf-plus\" href=\"/doi/pdfplus/10.1139/blah\">" +
          "PDF-Plus (208 K)" +
          "</a></li><li><a class=\"icon-figures\" id=\"figures\" href=\"javascript:void(0);\">Figures</a></li>" +
          "<li><a class=\"icon-references\" href=\"#ttl5\">References</a></li><li><a href=\"/action/z\" class=\"icon-recommended\">Also read</a></li>" +
          "<li><a class=\"icon-citing\" href=\"#citart1\">Citing articles</a></li>" +
          "<li><a href=\"/action/\" class=\"icon-recommended\">Also read</a></li>" +
          "<li><a href=\"/doi/abs/10.1139/blah\" class=\"icon-related\">Corrected article</a></li>" +
          "</ul><!-- abstract content -->" +
          "<h2 id=\"ttl12\">References</h2><ul class=\"no-bullet no-padding\">" +
          "<li id=\"ref1\"><span class=\"numbering\"></span>" +
          "</li>" +
          "</ul>" +
          "<div class=\"citedBySection\"><a name=\"citart1\"></a><h2>Cited by</h2><p><a href=\"/doi/citedby/10.1139/foo\">View all 3 citing articles</a></p></div>" +
          "</div>" +
          "</div>" +
          "</div> <!-- content -->" +
          "<div id=\"sidebar-right\">" +
          "    <!-- Right Sidebar Section: Institutional Logo Component -->" +
          "<div class=\"article-tools\">" +
          "    <h3 class=\"header-bar header-light-gray no-margin\"><span class=\"color-gray\">Article Tools</span></h3>" +
          "    <ul class=\"article-tools-list no-margin\">" +
          "        <li>" +
          "            <a href=\"/url\" class=\"icon-fav\" title=\"Add to Favorites\">" +
          "                Add to Favorites" +
          "            </a>" +
          "        </li>" +
          "        <li>" +
          "            <a href=\"/url\" class=\"icon-citation\" title=\"Download Citation\">" +
          "                Download Citation" +
          "            </a>" +
          "        </li>" +
          "    </ul>" +
          "</div>" +
          "<div class=\"socialMedia\">" +
          "    <!-- placeholder id=null, description=Right Sidebar Ad -->" +
          "</div>" +
          "</div><!-- sidebar-right" +
          "</div><!-- main -->" +
          "<div>" +
          "<table class=\"mceItemTable\" height=\"30px\" bgcolor=\"#BE1E2E\" border=\"0\" width=\"100%\">" +
          "<tbody>" +
          "<tr>" +
          "<td><span style=\"color: white;\"></span></td>" +
          "</tr>" +
          "</tbody>" +
          "</table>" +
          "<table style=\"width: 100%;\" class=\"mceItemTable\" border=\"0\">" +
          "<tbody>" +
          "<tr>" +
          "<td style=\"text-align: center;\"><a href=\"http://www.research4life.org/\" target=\"_blank\"><img alt=\"\" src=\"/userimages.gif\" style=\"vertical-align: middle;\" /></a>" +
          "<br />" +
          "</td>" +
          "</tr>" +
          "</tbody>" +
          "</table>" +
          "</div>" +
          "<div id=\"footer\">" +
          "    <!-- placeholder id=null, description=Footer --><div style=\"position: relative; text-align: center; color: white; padding: 23px; margin: 0px;\">" +
          "<a href=\"/action/\">" +
          "<img src=\"/blah.png\" alt=\"Logo\"></a>" +
          "&copy; Copyright 2013 &ndash; Publishing" +
          "</div>" +
          "</div>" +
          "</div><!-- wrapper -->" +
          "</body>" +
          "</html>";
  
  private static final String resultingSanitizedFileHtml =
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
          "<head>" +
          "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" +
          "    <title>Title </title>" +
          "<meta name=\"robots\" content=\"noarchive,nofollow\" />" +
          "<meta name=\"MSSmartTagsPreventParsing\" content=\"true\"/>" +
          "</head>" +
          "<body >    <div class=\"leaderboard\">" +
          "</div>" +
          "     " +
          "    " +
          "<div id=\"wrapper\"> " +
          "" +
          "<div id=\"main\"> " +
          "<div id=\"content\">" +
          "        <div id='background' style=\"display:none\"></div>" +
          "<div class=\"border-gray\">" +
          "    <div class=\"box-pad\">" +
          "       " +
          "" +
          "<ul class=\"icon-list-vertical box-gray-border box-pad clear\"><li><a class=\"icon-abstract\" title=\"View the Abstract\" href=\"/doi/abs/10.1139/blah\">" +
          "<span>Abstract</span></a></li><li><a class=\"icon-pdf\" href=\"/doi/pdf/10.1139/blah\">" +
          "</a></li><li><a class=\"icon-pdf-plus\" href=\"/doi/pdfplus/10.1139/blah\">" +
          "</a></li><li><a class=\"icon-figures\" id=\"figures\" href=\"javascript:void(0);\">Figures</a></li>" +
          "<li><a class=\"icon-references\" href=\"#ttl5\">References</a></li><li></li>" +
          "<li></li>" +
          "<li></li>" +
          "<li><a href=\"/doi/abs/10.1139/blah\" class=\"icon-related\">Corrected article</a></li>" +
          "</ul>" +
          "<h2 id=\"ttl12\">References</h2><ul class=\"no-bullet no-padding\">" +
          "<li id=\"ref1\"><span class=\"numbering\"></span>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</div>" +
          "</div> " +
          "<div>" +
          "</div>" +
          "</div>" +
          "</body>" +
          "</html>";


}

