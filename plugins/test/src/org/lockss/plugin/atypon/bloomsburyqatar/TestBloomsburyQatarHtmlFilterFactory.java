/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the \"Software\"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.atypon.bloomsburyqatar;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestBloomsburyQatarHtmlFilterFactory extends LockssTestCase {
  
  public FilterFactory fact;
  
  private static MockArchivalUnit mau;
  
  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings. These are shared crawl & hash filters
  public static final String emptyFiltered = "";
  
  public static final String topArticlesHtml =
  "<div id=\"topArticlesTabs\" class=\"yui-navset \">" +
  "    <ul class=\"yui-nav\">" +
  "        <li class=\"selected\">" +
  "         <a href=\"#mostRead\">Most Read</a>" +
  "        </li>" +
   "       <li >" +
  "         <a href=\"#mostCited\">Most Cited</a>" +
  "        </li>" +    
  "    </ul>" +
  "    <div id=\"mostCited\">" +
  "       <div class=\"more\"></div>" +
  "    </div>" +
  "</div>";
  
  public static final String rightColHtml =
      "<div id=\"rightCol\">" +
      "<div class=\"panelcontent-site-pnl\"  id=\"journalFooPanel\">" +
      "    <div class=\"box\">" +
      "      <div class=\"header\"><h3>Publication Information</h3></div>" +
      "      <div class=\"box-inner\">" +           
      "<p><img alt=\"\" src=\"http://www.websitebullets.com/bullet/207/5.gif\" /> <a href=\"/loi/nmejre\">Previous issues  </a>" +
      "<br />" +
      "</p>" +
      "<hr />" +
      "        </div>" +
      "        <div class=\"footer\"></div>" +
      "    </div>" +
      "</div>" +
      "</div>";
  

  /**
   * Variant to test with Crawl Filter
   */
  public static class TestCrawl extends TestBloomsburyQatarHtmlFilterFactory {
    

    public void setUp() throws Exception {
      super.setUp();
      fact = new BloomsburyQatarHtmlCrawlFilterFactory();
    }
    
    public void testRightColPiecesFiltering() throws Exception {
      InputStream actIn =
          fact.createFilteredInputStream(mau,
              new StringInputStream(topArticlesHtml),
              Constants.DEFAULT_ENCODING);
      assertEquals(StringUtil.fromInputStream(actIn),
          emptyFiltered);     // filtered in CRAWL version of filter
    }
    
    public void testRightColFiltering() throws Exception {
      InputStream actIn =
          fact.createFilteredInputStream(mau,
                                         new StringInputStream(rightColHtml),
                                         Constants.DEFAULT_ENCODING);
        assertEquals(StringUtil.fromInputStream(actIn),
            rightColHtml);
        }
    
  }

  /**
   * Variant to test with Hash Filter
   */
  public static class TestHash extends TestBloomsburyQatarHtmlFilterFactory {
    
    
    public static final String topHtml =
        "<div id=\"top\">" +
        " <div id=\"hdrmenu\" class=\"clearfix\">" +
        "  <!-- placeholder id=null, description=Header - Mobile Button -->" +
        "  <div style=\"float:left;\" class=\"headerAd\"><!-- End Initiate Jscript code ---->" +
        "    <!-- Qualaroo for qscience.com -->" +
        " </div>" +
        "  </div>" +
        "  <div id=\"banner-search\" class=\"clearfix\">" +
        "  <div id=\"top-banner\"></div>" +
        "    </div>" +
        "    <div class=\"color-line\"></div>" +
        "</div>";

        public static final String rightColHtml =
        "<div id=\"rightCol\">" +
        "<!-- placeholder id=null, description=\"Journal Right 1\" -->" +
        "<div class=\"panelcontent-site-pnl\"  id=\"journalInfoPanel\">" +
        "    <div class=\"box\">" +
        "      <div class=\"header\"><h3>Publication Information</h3></div>" +
        "      <div class=\"box-inner\">" +           
        "<p><img alt=\"\" src=\"http://www.websitebullets.com/bullet/207/5.gif\" /> <a href=\"/loi/nmejre\">Previous issues  </a>" +
        "<br />" +
        "</p>" +
        "<hr />" +
        "        </div>" +
        "        <div class=\"footer\"></div>" +
        "    </div>" +
        "</div>" +
        "</div>";
        
        public static final String addthisHtml =
        "<div class=\"addthis_toolbox addthis_default_style \">" +
        "<a title=\"Add this article to your Mendeley library\" href=\"http://www.foo.com/import/?doi=10.xxxxx/yyy\">" +
        "<img hspace=\"20\" height=\"20\" width=\"125\" src=\"http://d36cz9buwru1tt.cloudfront.net/logo_mendeley.png\"></a>" +
        "<a class=\"addthis_counter addthis_pill_style\" style=\"display: inline-block; \" href=\"#\"><a class=\"atc_s addthis_button_compact\">" +
        "<span></span></a>" +
        "<a class=\"addthis_button_expanded\" target=\"_blank\" title=\"View more services\" href=\"#\"></a>" +
        "</a><div class=\"atclear\">" +
        "</div>" +
        "</div>";     

        public static final String fbHtml =
        "<div class=\"fb-comments\" href=\"http%3A%2F%2Fdx.doi.org%2F' + DOI + '\" width=\"730\"></div>";

        public static final String altmetric =
        "<div class=\"altmetric-embed\" popover=\"left\" type=\"donut\" doi=\"\"></div>";

 
    
    public void setUp() throws Exception {
      super.setUp();
      fact = new BloomsburyQatarHtmlHashFilterFactory();
    }
    
    public void testTopFiltering() throws Exception {
      InputStream actIn =
          fact.createFilteredInputStream(mau,
                                         new StringInputStream(topHtml),
                                         Constants.DEFAULT_ENCODING);
        assertEquals(StringUtil.fromInputStream(actIn),
                    emptyFiltered);
    }
    
    public void testRightColFiltering() throws Exception {
      InputStream actIn =
          fact.createFilteredInputStream(mau,
                                         new StringInputStream(rightColHtml),
                                         Constants.DEFAULT_ENCODING);
        assertEquals(StringUtil.fromInputStream(actIn),
            emptyFiltered);
        }
    
    public void testRegExFiltering() throws Exception {
      
      InputStream actIn =
          fact.createFilteredInputStream(mau,
              new StringInputStream(addthisHtml),
              Constants.DEFAULT_ENCODING);
      assertEquals(StringUtil.fromInputStream(actIn),
          emptyFiltered);
      
      actIn =
          fact.createFilteredInputStream(mau,
              new StringInputStream(fbHtml),
              Constants.DEFAULT_ENCODING);
      assertEquals(StringUtil.fromInputStream(actIn),
          emptyFiltered);
      
      actIn =
          fact.createFilteredInputStream(mau,
              new StringInputStream(altmetric),
              Constants.DEFAULT_ENCODING);
      assertEquals(StringUtil.fromInputStream(actIn),
          emptyFiltered);
      
      actIn =
          fact.createFilteredInputStream(mau,
              new StringInputStream(topArticlesHtml),
              Constants.DEFAULT_ENCODING);
      assertEquals(StringUtil.fromInputStream(actIn),
          topArticlesHtml); // unchanged in HASH version of filter
    }
    
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

