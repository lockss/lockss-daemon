/*
 * $Id: TestCanadianYoungScientistHtmlFilterFactory.java,v 1.1 2013-11-07 01:00:16 ldoan Exp $
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

package org.lockss.plugin.atypon.canadianyoungscientist;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestCanadianYoungScientistHtmlFilterFactory
  extends LockssTestCase {
  
  public FilterFactory fact;
  
  private static MockArchivalUnit mau;
  
  // Example instances mostly from pages of strings of HTMl that should be 
  // filtered and the expected post filter HTML strings. These are shared 
  // crawl & hash filters
  private static final String afterFilteringStr = 
      "<div class=\"block\"></div>";
  
  private static final String withSidebarLeft = 
      "<div class=\"block\">"
        + "<div id=\"sidebar-left\">"
        + "<a href=\"/journal/xxx\">"
        + "<img src=\"/blah/covergifs/xxx/cover.jpg\"></a>"
        + "<div class=\"ads\">"
        + "</div><ul style=\"margin-top: -10px;\" id=\"left-menu\"><li>"
        + "<div class=\"header-bar header-gray\">Browse the journal</div><ul>"
        + "<li><a href=\"/action/doi/full/11.11111/xxxx-2013-005\">"
        + "Archives</a></li>"
        + "<li><a href=\"/action/recommendation/doi/full/11.11111"
        + "/xxxx-2013-005\">Recommend to public</a></li>"
        + "</ul></li></ul><div></div></div>"
        + "</div>";
  
  private static final String withNavWrapper =
      "<div class=\"block\">"
          + "<div id=\"nav-wrapper\">"
          + "<div>"
          + "<ul id=\"nav-left\"><li class=\"first\">"
          + "<li><a href=\"/journal/xxxx\">Home</a></li>"
          + "<li><a href=\"/page/aboutxxxx\">About Xxxx</a></li>"
          + "</ul>"
          + "</div>"
          + "<ul id=\"nav-right\">"
          + "<li><a class=\"language\" href=\"/action/blah\">Other language</a>"
          + "</li></ul></div>"
          + "</div>";
  
  private static final String withFullIssues =
      "<div class=\"block\">"
          + "<div class=\"box-pad border-gray margin-bottom clearfix\">"
          + "<class=\"float-right\">"
          + "<h1>Table of Contents</h1>"
          + "<img src=\"/imagehome/cover.jpg\">"
          + "<div class=\"journal-details\">"
          + "<a class=\"btn-article-items \""
          + "href=\"/doi/pdf/11.11111/xxxx2013-2\">"
          + "</a></div></div>"
          + "</div>";
  
  private static final String withSpiderTrap =
      "<div class=\"block\">"
          + "<span id=\"hide\"><a href=\"/doi/pdf/10.xxxx/9999-9999.99999\">"
          + "<!-- Spider trap link --></a></span>"
          + "</div>";
  
  private static final String withIconRecommended =
      "<div class=\"block\">"
          + "<a class=\"icon-recommended\" href=\"/action/blah\">Also read</a>"
          + "</div>";
  
  private static final String withTopBarWrapper =
      "<div class=\"block\">"
          + "<div id=\"top-bar-wrapper\">"
          + "<div id=\"top-bar-1\">"
          + "<div class=\"headerAd\"><img src=\"/yyy/logo.gif\">"
          + "</div></div></div>"
          + "</div>";
  
  private static final String withBanner =
      "<div class=\"block\">"
          + "<div class=\"banner\">"
          + "<h1> Journal Name</h1>"
          + "</div>"
          + "</div>";
  
  private static final String withBreadcrumbs =
      "<div class=\"block\">"
          + "<div id=\"breadcrumbs\">"
          + "<span class=\" page_breadcrumbs\">"
          + "<a href=\"/\">Home</a>&gt;"
          + "<a href=\"/action/blah\">Journals</a>"
          + "</span>"
          + "</div>"
          + "</div>";
  
  private static final String withSidebarRight = 
      "<div class=\"block\">"
          + "<div id=\"sidebar-right\">"
          + "<div class=\"article-tools\">"
          + "<span class=\"article-tools-top header-light-gray no-margin\">"
          + "</span><ul class=\"article-tools-list no-margin\">"
          + "<li><a title=\"Download Citation\" class=\"icon-citation\""
          + "href=\"/action/showCitFormats?blah\">Download Citation</a></li>"
          + "</ul></div>"
          + "<div class=\"socialMedia\">"
          + "<ul id=\"social-media\" class=\"box-gray border-gray\">"
          + "<li><a class=\"icon-citeulike\""
          + "href=\"http://www.citeulike.org/blah\">CiteULike</a></li>"
          + "</ul></div></div>"
          + "</div>";
  
  private static void doFilterTest(FilterFactory fact, String nameToHash) 
      throws PluginException, IOException {
    InputStream actIn; 

    actIn = fact.createFilteredInputStream(mau, 
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);
      assertEquals(afterFilteringStr, StringUtil.fromInputStream(actIn));
  }
          		
  // Variant to test with Crawl Filter
  public static class TestCrawl
    extends TestCanadianYoungScientistHtmlFilterFactory {
    public void setUp() throws Exception {
      super.setUp();
      fact = new CanadianYoungScientistHtmlCrawlFilterFactory();
    }
    public void testFiltering() throws Exception {
      doFilterTest(fact, withSidebarLeft);
      doFilterTest(fact, withNavWrapper);
      doFilterTest(fact, withFullIssues);
      doFilterTest(fact, withSpiderTrap);
      doFilterTest(fact, withIconRecommended);
    }    
  }

  // Variant to test with Hash Filter
   public static class TestHash 
    extends TestCanadianYoungScientistHtmlFilterFactory {   
    public void setUp() throws Exception {
      super.setUp();
      fact = new CanadianYoungScientistHtmlHashFilterFactory();
    }
    public void testFiltering() throws Exception {
      doFilterTest(fact, withSidebarLeft);
      doFilterTest(fact, withNavWrapper);
      doFilterTest(fact, withSpiderTrap);
      doFilterTest(fact, withIconRecommended);
      doFilterTest(fact, withTopBarWrapper);
      doFilterTest(fact, withBanner);
      doFilterTest(fact, withBreadcrumbs);
      doFilterTest(fact, withSidebarRight);
    }
  }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

