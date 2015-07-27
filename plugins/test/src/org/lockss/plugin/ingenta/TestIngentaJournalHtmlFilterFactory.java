/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"}, to deal
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

package org.lockss.plugin.ingenta;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

/**
 * This class tests the IngentaJouranlPluginHtmlFilterFactory.
 * @author phil
 *
 */
public class TestIngentaJournalHtmlFilterFactory extends LockssTestCase {
  private IngentaJournalHtmlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new IngentaJournalHtmlFilterFactory();
  }
  
  // block tags from IngentaJouranlHtmlFilterFactory
  String blockIds[][] = new String[][] {
    // only tests the constructed tag rather than actual example from page
    // Filter out <div id="header">...</div>
    {"div", "id", "header"},
    // Filter out <div id="footerarea">...</div>
    {"div", "id", "footerarea"},
    // Filter out <div id="rightnavbar">...</div>
    {"div", "id", "rightnavbar"},
    // Filter out <div class="article-pager">...</div>
    {"div", "class", "article-pager"},
    // Filter out <div id="purchaseexpand"...>...</div>
    {"div", "id", "purchaseexpand"},
    // Filter out <div id="moredetails">...</div>
    {"div", "id", "moredetails"},
    // Filter out <div id="moreLikeThis">...</div>
    {"div", "id", "moreLikeThis"},
    // filter out <div class="heading"> that encloses a statement with
    // the number of references and the number that can be referenced: 
    // number of reference links won't be the same because not all 
    // the referenced articles are available at a given institution.
    {"div", "class", "heading"},
    // filter out <div class="advertisingbanner[ clear]"> that encloses 
    // GA_googleFillSlot("TopLeaderboard") & GA_googleFillSlot("Horizontal_banner")
    {"div", "class", "advertisingbanner"},
    {"div", "class", "advertisingbanner clear"},
    // filter out <li class="data"> that encloses a reference for the
    // article: reference links won't be the same because not all 
    // the referenced articles are available at a given institution.
    {"li", "class", "data"},
    // institution-specific subscription link section
    {"div", "id", "subscribe-links"},
    // Filter out <div id="links">...</div>
    {"div", "id", "links"},
    // Filter out <div id="footer">...</div>
    {"div", "id", "footer"},
    // Filter out <div id="top-ad-alignment">...</div>
    {"div", "id", "top-ad-alignment"},
    // Filter out <div id="top-ad">...</div>
    {"div", "id", "top-ad"},
    // Filter out <div id="ident">...</div>
    {"div", "id", "ident"},
    // Filter out <div id="ad">...</div>
    {"div", "id", "ad"},
    // Filter out <div id="vertical-ad">...</div>
    {"div", "id", "vertical-ad"},
    // Filter out <div class="right-col-download">...</div>
    {"div", "class", "right-col-download"},
    // Filter out <div id="cart-navbar">...</div>
    {"div", "id", "cart-navbar"},
    // Filter out <div class="heading-macfix">...</div>
    {"div", "class", "heading-macfix"},
    // Filter out <div id="baynote-recommendations">...</div>
    {"div", "id", "baynote-recommendations"},
    // Filter out <div id="bookmarks-container">...</div>
    {"div", "id", "bookmarks-container"},
    // Filter out <div id="llb">...</div>
    {"div", "id", "llb"},
    // Filter out <a href="...">...</a> where the href value includes "exitTargetId" as a parameter
    {"a", "href", "foo?exitTargetId=bar"},
    {"a", "href", "foo?parm=value&exitTargetId=bar"},
    // Icon on article reference page
    {"span", "class", "access-icon"},
    {"span", "class", "acess-icon"},
    {"span", "class", "acess-icon"},
    // extraneous tags
    {"div", "id", "sign-in-container"},
    {"div", "id", "tools"},
    {"div", "class", "shareContent"},
    {"div", "class", "icon-key"},
    {"div", "class", "cornerPolicyTab"},
    {"div", "class", "mainCookiesPopUp"},
  };
  
  // single tags from IngentaJouranlHtmlFilterFactory
  String[][] tagIds = new String[][] {
    // filter out <link rel="stylesheet" href="..."> because Ingenta has
    // bad habit of adding a version number to the CSS file name
    {"link", "rel", "stylesheet"},
    // Filter out <input name="exitTargetId">
    {"input", "name", "exitTargetId"},
  };
  
  public void testTagFiltering() throws Exception {
    // common filtered html results
    String filteredHtml = " ";
//        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"> "
//            + "<html lang=\"en\"> <body> "
//            + "</body> </html> ";
    
    // html for block tags
    String blockHtml =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
          + "<html lang=\"en\">\n<head>\n"
          + "<meta name=\"DC.identifier\" content=\"id/art0001\"/>\n" 
          + "</head> <body>\n"
          + "<%s %s=\"%s\">\n"
          + "chicken chicken chicken...\n"
          + "</%s>\n"
          + "</body>\n</html>\n\n\n";
    
    // test block tag ID filtering
    for (String[] id : blockIds) {
      InputStream htmlIn = fact.createFilteredInputStream(mau,
          new StringInputStream(String.format(blockHtml, id[0],id[1],id[2],id[0])),
          Constants.DEFAULT_ENCODING);
      assertEquals(filteredHtml, StringUtil.fromInputStream(htmlIn));
    }
    
    // html for single tags
    String tagHtml =
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
            + "<html lang=\"en\">\n<head>\n<meta name=\"DC.identifier\" content=\"id/art0001\"/>\n"
            + "</head>\n<body>\n"
            + "<%s %s=\"%s\">\n"
            + "</body>\n</html>\n\n\n";
    
    // test single tag ID filtering
    for (String[] id : tagIds) {
      InputStream htmlIn = fact.createFilteredInputStream(mau,
          new StringInputStream(String.format(tagHtml, id[0],id[1],id[2])),
          Constants.DEFAULT_ENCODING);
      assertEquals(filteredHtml, StringUtil.fromInputStream(htmlIn));
    }
  }
  //test AdvertisingBannerHtml with explicit tests
  private static final String AdvertisingBannerHtml =
      "<p>The chickens were decidedly cold.</p>" +
          "<div class=\"advertisingbanner\"> " +
          "<script type='text/javascript'>" +
          "GA_googleFillSlot(\"TopLeaderboard\"); </script>" +
          "<script type='text/javascript'>" +
          "GA_googleFillSlot(\"Horizontal_banner\"); </script>" +
          "</div>";
  
  private static final String AdvertisingBannerHtmlFiltered =
      "The chickens were decidedly cold.";
  
  public void testFilterAdvertising() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(AdvertisingBannerHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(AdvertisingBannerHtmlFiltered,StringUtil.fromInputStream(inA));
  }
  
  //test div class heading-macfix with explicit tests
  private static final String HeadingMacfixHtml =
      "<p>The chickens were decidedly cold.</p>" +
          "<div class=\" heading-macfix\"> " +
          "<span class=\"rust\"> 2" +
          "</span> references have been identified for this article, " +
          "of which <span class=\"rust\">2</span> have matches and can be" +
          "accessed below </div>";
  
  private static final String HeadingMacfixHtmlFiltered =
      "The chickens were decidedly cold.";
  
  public void testHeadingMacfix() throws Exception {
    InputStream inA = fact.createFilteredInputStream(mau,
        new StringInputStream(HeadingMacfixHtml),
        Constants.DEFAULT_ENCODING);
    assertEquals(HeadingMacfixHtmlFiltered,StringUtil.fromInputStream(inA));
  }
  
  private static final String FreeTrialHtmlFiltered =
      "The chickens were decidedly cold. ";
  
  //test free trial with explicit tests
  private static final String FreeTrialHtml =
      "<p>The chickens were decidedly cold.</p> " +
          "<p class=\"heading-macfix\">" +
          "<a name=\"trial\">" +
          "<a href=\"/content/00000021/00000002/trial\" title=\"sign up for free trial\">" +
          "or click here to sign up for a free trial</a></p>" +
          "\n" +
          "<a href=\"#trial\" title=\"trial available\">Free trial available!</a>" +
          "<br/>";
  
  public void testFreeTrial() throws Exception {
    InputStream inA = fact.createFilteredInputStream(mau,
        new StringInputStream(FreeTrialHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(FreeTrialHtmlFiltered,StringUtil.fromInputStream(inA));
  }
  
  //test some straight html strings with explicit tests
  private static final String expireChecksumHtml =
      "<p>The chickens were decidedly cold.</p>" +
          "<p xmlns:f=\"http://www.example.org/functions\">" +
          "<a name=\"g002\"></a>" +
          "<div class=\"figure\">" +
          "<div class=\"image\">\n" +
          "<a class=\"table-popup\" href=\"javascript:popupImage('s4-ft1401-0065-g002.gif.html" +
          "?expires=1355962274&id=72080579&titleid=6312&accname=Stanford+University" +
          "&checksum=FA59636C74DD0E40E92BA6EFECB866E7')\">" +
          "<img alt=\"Figure 1\" border=\"0\" src=\"s4-ft1401-0065-g002_thmb.gif\">" +
          "<p>Figure 1<br>Click to view</p>\n" +
          "</a>\n" +
          "</div>" +
          "<div class=\"caption\">\n" +
          "<span class=\"captionLabel\"><span class=\"label\">The Chickens.</span></span>\n" +
          "</div>";
  
// NOTE - the two following lines below:
//   "<p>Figure 1<br>Click to view</p>" +
//   "</a>" +  
// need to be included in the filtered result because 
// the <a> tag hashing will stop when it finds an embedded composite tag,
// (in this case, <p>).  Once we update the daemon to allow modifying this in a subclass
// then we'll need to make the change for this plugin & update this test.
// HOWEVER - this still solves the hash problems since the item to remove is
// before the <p> tag
// NOTE: new <br> tag filter was added
  private static final String expireChecksumFiltered =
      "The chickens were decidedly cold. Figure 1Click to view The Chickens. ";
  
  public void testFiltering() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(expireChecksumHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(expireChecksumFiltered,StringUtil.fromInputStream(inA));
  }
  
  private static final String onClickExitTargetID =
      "<div class=\"left-col-download\">View now:</div>" +
          "<div class=\"right-col-download contain\">" +
          "<span class=\"orangebutton\">" +
          "<span class=\"orangeleftside icbutton\">" +
          "<a onclick=\"javascript:popup('/search/download?pub=infobike%3a%2f%2flse%2fjtep%2f2001%2f00000035%2f00000001%2fart00001&mimetype=" +
          "application%2fpdf&exitTargetId=1371686277240','dowloadWindow','900','800')\" title=\"PDF download of Editorial\" class=\"no-underl" +
          "ine contain\" >PDF" +
          "</a></span></span>" +
          "</div>";
  
  private static final String onClickExitTargetIDFiltered =
      "View now:";
  
  public void testonClickFiltering() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(onClickExitTargetID),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(onClickExitTargetIDFiltered,StringUtil.fromInputStream(inA));
  }
  
  //test ScriptHtml with explicit test
  private static final String ScriptHtml =
      "<p>The chickens were decidedly cold.</p>" +
          "<script type='text/javascript'>" +
          "GA_googleFillSlot(\"TopLeaderboard\"); </script>" +
          "<script type='text/javascript'>" +
          "GA_googleFillSlot(\"Horizontal_banner\"); </script>" +
          "<script type=\"text/javascript\" charset=\"utf-8\">" +
          "   $(document).ready(function() {\n" + 
          "      var shortdescription = $(\".originaldescription\").text().replace(/\\&/g, '&amp;').replace(/\\</g, '&lt;').replace(/\\>/g, '&gt;').replace(/\\t/g, '&nbsp;&nbsp;&nbsp;').replace(/\\n/g, '<br />');\n" + 
          "      if (shortdescription.length > 350){\n" + 
          "         shortdescription = \"<span class='shortdescription'>\" + shortdescription.substring(0,250) + \"... <a href='#'>more</a></span>\";\n" + 
          "      }\n" + 
          "      $(\".descriptionitem\").prepend(shortdescription);\n" + 
          "         \n" + 
          "      $(\".shortdescription a\").click(function() {\n" + 
          "         $(\".shortdescription\").hide();\n" + 
          "         $(\".originaldescription\").slideDown();\n" + 
          "         return false;        \n" + 
          "      });\n" + 
          "   });                  \n" + 
          "</script>" +
          "<noscript>      \n" + 
          "<img id=\"siqImg\" height=\"1\" width=\"1\" alt=\"\">\n" + 
          "</noscript>";
  
  private static final String ScriptHtmlFiltered =
      "The chickens were decidedly cold.";
  
  public void testFilterScript() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(ScriptHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(ScriptHtmlFiltered,StringUtil.fromInputStream(inA));
  }
  
  //test nav, footer, style with explicit test
  private static final String NavHtml =
      "<p>The chickens were decidedly cold.</p>" +
          "<style> /* cache issue message */ .cacheIssue { display:none; background: none repeat scroll 0 0 #faf9a9;" +
          " color:#000; padding:9px; position: absolute; top:-10px; width:986px; z-index:108;" +
          " box-shadow: 2px 2px 3px #88898a; } .cacheIssue p { width: 92%; }" +
          " .cacheIssue a { color:#0775cf; text-decoration:underlined !important; }" +
          " .cacheIssue a:hover{ color:#0775cf; text-decoration:underlined; }" +
          " .cacheIssue span { color: #7c7b09; cursor: pointer; display: block;" +
          " font-size: 14px; font-weight: bold; padding: 0 7px; position: absolute;" +
          " right: -1px; top: 8px; border-radius:6px; }" +
          " .cacheIssue span:hover { color: #05477c; } } " +
          "</style>" +
          "<nav role=\"navigation\" class=\"navbar navbar\"> " +
          "<button data-target=\"#about-section-nav\" data-toggle=\"collapse\"" +
          " class=\"navbar-toggle\" type=\"button\"> " +
          "<span><i class=\"fa fa-ellipsis-h\"></i></span> </button>" +
          "<form action=\"/cart?exitTargetId=1404984884181\" class=\"cartButtonRWD\"> " +
          "<input type=\"submit\" value=\"\" class=\"navbar-toggle\"> </form> " +
          "</nav>" +
          "<footer role=\"footernav\" class=\"footer\">\n" + 
          "<div class=\"foo\">\n" + 
          "</div>\n" + 
          "</footer>";
  
  private static final String NavHtmlFiltered =
      "The chickens were decidedly cold.";
  
  public void testNavHtml() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(NavHtml),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(NavHtmlFiltered,StringUtil.fromInputStream(inA));
  }
  
  //test li with rowShade with explicit test
  private static final String RowShade =
      "<ul>\n" +
      "<li class=\" rowShadeOdd\" more>i1</li>\n" +
      "<li class=\"rowShadeEven\" more2>i2</li>\n" +
      "<li class=\"diff\" diff2>i2</li>\n" +
      "</ul>\n";
  
  private static final String RowShadeFiltered =
      " i1 i2 i2 ";
  
  public void testRowShade() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(RowShade),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(RowShadeFiltered,StringUtil.fromInputStream(inA));
  }
  
  //test li with rowShade with explicit test
  private static final String ExtRef =
      /*
       * <a title="link to external reference" onclick="popup('reswin','780','600')" href="#">19, p102</a>
       */
      "<li> Item " +
      "<a title=\"link to external reference\" onclick=\"popup('/path/ref', 'reswin', '40', '30')\" " +
      "href=\"#\">19, p102</a>  </li>";
  
  private static final String ExtRefFiltered =
      " Item 19, p102 ";
  
  public void testExtRef() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(ExtRef),
        Constants.DEFAULT_ENCODING);
    
    assertEquals(ExtRefFiltered,StringUtil.fromInputStream(inA));
  }
  
}
