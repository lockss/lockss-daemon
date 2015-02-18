/*
 * $Id$
 */
/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bioone;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

/*
 *  This test rig has two subclasses one of which is for hash filter testing 
 *  and the other of which is for crawl filter testing.
 *  Strings (of html bits) that are common to both can be shared in the parent
 */

public class TestBioOneAtyponHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  public FilterFactory fact;
  public MockArchivalUnit mau;

  /*
   * These html bits are used for both crawl and hash filtering
   */

  public static final String citingArticlesHtml=
      "<div class=\"itemContent itemClosed\" id=\"citingArticles\">" +
          "<p><a  href=\"/servlet/linkout?type=\"> Scholar</a></p>" +
          "</div>";
  public static final String citingArticlesFiltered = "";

  public static final String rightColumnHtml =
      "<div id=\"contentSidebar\">" +
          "<div class=\"relatedContent\">" +
          " <div class=\"columnBlock\">" +
          "   <div class=\"columnBlock\">" +
          "    <h2>Article Views</h2>" +
          "    <div class=\"itemContent\" id=\"articleViews\"></div>" +
          "   </div>" +
          "   <div class=\"columnBlock\">" +
          "    <h2>Article Tools</h2>" +
          "    <div class=\"itemContent\" id=\"articleTools\"></div>" +
          "   </div>" +
          "   <div class=\"columnBlock\">" +
          "    <h2>Related Article Sarch</h2>" +
          "    <div class=\"itemContent\" id=\"relatedArticleSearch\"></div>" +
          "   </div>" +
          "   <div class=\"columnBlock\">" +
          "    <h2>Share</h2>" +
          "    <div class=\"itemContent\" id=\"Share\"></div>" +
          "   </div>" +
          "   <div class=\"columnBlock\">" +
          "     <h2>Citing Articles</h2>" +
          "     <div class=\"itemContent itemClosed\" id=\"citingArticles\">" +
          "      <p><a  href=\"/servlet/linkout?type=google-cite&doi=10.3106%2F041.035.0104\">Google Scholar</a></p>" +
          "     </div>" +
          "   </div>" +
          "</div>" +
          "</div>" +
          "</div>";


  /**
   * Variant to test with Crawl Filter
   */

  /* these string html bits are unique to crawl filtering */
  private static final String rightColumnCrawlFiltered = 
      "<div id=\"contentSidebar\">" +
          "<div class=\"relatedContent\">" +
          " <div class=\"columnBlock\">" +
          "   <div class=\"columnBlock\">" +
          "    <h2>Article Views</h2>" +
          "    " +
          "   </div>" +
          "   <div class=\"columnBlock\">" +
          "    <h2>Article Tools</h2>" +
          "    <div class=\"itemContent\" id=\"articleTools\"></div>" +
          "   </div>" +
          "   <div class=\"columnBlock\">" +
          "    <h2>Related Article Sarch</h2>" +
          "    " +
          "   </div>" +
          "   <div class=\"columnBlock\">" +
          "    <h2>Share</h2>" +
          "    " +
          "   </div>" +
          "   <div class=\"columnBlock\">" +
          "     <h2>Citing Articles</h2>" +
          "     " +
          "   </div>" +
          "</div>" +
          "</div>" +
          "</div>";

  private static final String leftColumnTOC =
      "<div class=\"btnToggle\">" +
          "<a title=\"Toggle Title Tools\" href=\"javascript:toggleLayer('titleTools','titleToolsImg');\">" +
          "<img id=\"titleToolsImg\" src=\"/templates/jsp/blah.gif\" alt=\"click this button to close\" width=\"14\" height=\"13\" />" +
          "</a>" +
          "</div>" +
          "<h2>Title Tools</h2>" +
          "<div id=\"titleTools\" class=\"itemContent\">" +
          "<div class=\"articleComponent titleTools\">" +
          "<div class=\"btnToggleMini\">" +
          "    <h3>Most Read</h3>" +
          "</div>" +
          "<div id=\"compMostRead\">" +
          "<p><a href=\"/doi/abs/10.3106/blah\">Title</a></p>" +
          "</div>" +
          "</div>" +
          "<div class=\"articleComponent titleTools\">" +
          "<div class=\"btnToggleMini\">" +
          "<h3>Most Cited </h3>" +
          "</div>" +
          "<div id=\"compMostCited\">" +
          "<p>" +
          "<a href=\"/doi/abs/10.3106/other-volume\">Other Title</a>" +
          "</p>" +
          "</div>" +
          "</div>" +
          "</div>";

  private static final String leftColumnTOCFiltered =
      "<div class=\"btnToggle\">" +
          "<a title=\"Toggle Title Tools\" href=\"javascript:toggleLayer('titleTools','titleToolsImg');\">" +
          "<img id=\"titleToolsImg\" src=\"/templates/jsp/blah.gif\" alt=\"click this button to close\" width=\"14\" height=\"13\" />" +
          "</a>" +
          "</div>" +
          "<h2>Title Tools</h2>";
  
  
  private static final String articleNav=
      "<div class=\"articleNav\">" +
          "<a class=\"articleToolsNav\" href=\"/doi/full/10.111/x\">� previous article</a>" +
          "<span class=\"navSearchColon\"> : </span>" +
          "<a class=\"articleToolsNav\" href=\"/doi/full/10.1111/x\">next article �</a>" +
          "</div>";
  private static final String articleNavFiltered=
      "<div class=\"articleNav\">" +
          "<span class=\"navSearchColon\"> : </span>" +
          "</div>";
  private static final String issueNav=
      "<div class=\"issueNav\">Jun 2010 : Volume 45 Issue 1 |" +
          "<!--<div id=\"nextprev\">-->" +
          "<a href=\"/toc/xxx/44/2\">" +
          "                            � previous issue" +
          "</a>" +
          "            : " +
          "<a href=\"/toc/xxx/45/2\">" +
          "                            next issue �" +
          "</a>" +
          "<!--</div><br/>-->" +
          "</div>";

  private static final String issueNavFiltered=
      "";
  private static final String breadcrumb=
      "<div id=\"breadcrumbs\">" +
          "<a href=\"/\">Home</a>" +
          "    / " +
          "<a href=\"/action/showPublications?type=byAlphabet\">All Titles</a>" +
          "        / <span class=\"title\">" +
          "<a href=\"/loi/xxx\">" +
          "                    JTitle" +
          "</a>" +
          "</span>" +
          "        / <span class=\"title\">" +
          "<a href=\"/toc/xxx/45/1\">" +
          "                    Jun 2010" +
          "</a>" +
          "</span>" +
          "       / <span class=\"title\">" +
          "               pg(s) 1-26" +
          "</span>" +
          "</div>";

  private static final String breadcrumbFiltered=
"";



  public static class TestCrawl extends TestBioOneAtyponHtmlFilterFactory {

    public void setUp() throws Exception {
      super.setUp();
      fact = new BioOneAtyponHtmlCrawlFilterFactory();
    }

    public void testCrawlFiltering() throws Exception {
      InputStream inA;
      InputStream inB;


      inA = fact.createFilteredInputStream(mau, new StringInputStream(rightColumnHtml),
          ENC);
      assertEquals(rightColumnCrawlFiltered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(leftColumnTOC),
          ENC);
      assertEquals(leftColumnTOCFiltered,StringUtil.fromInputStream(inA));      

      inA = fact.createFilteredInputStream(mau, new StringInputStream(articleNav),
          ENC);
      assertEquals(articleNavFiltered,StringUtil.fromInputStream(inA));      

      inA = fact.createFilteredInputStream(mau, new StringInputStream(issueNav),
          ENC);
      assertEquals(issueNavFiltered,StringUtil.fromInputStream(inA));      

      inA = fact.createFilteredInputStream(mau, new StringInputStream(breadcrumb),
          ENC);
      assertEquals(breadcrumbFiltered,StringUtil.fromInputStream(inA));      
      
    }
  }

  /**
   * Variant to test with Hash Filter
   */

  /*
   * These html bits are unique to hashing
   */
  private static final String headerHtml =
      "<div id=\"header\">" +                                                                                                                                                             
          "<div id=\"bannerLayout\">" +                                                                                                                                                       
          "<div id=\"search\">" +                                                                                                                                                             
          "  <form action=\"/action/doSearch\" name=\"simpleQuickSearchForm1\" method=\"get\"><span class=\"qSearchLabel\">search</span>" +                                                   
          "    <input id=\"searchQuery\" />" +                                                                                                                                                
          "    <input id=\"foo\" />" +                                                                                                                                                        
          "  </form>" +                                                                                                                                                                       
          "  <span class=\"searchLink\"><a href=\"/search/advanced\">Advanced Search</a></span>" +                                                                                            
          "</div>" +                                                                                                                                                                          
          "<!-- placeholder id=null, description=Header-Mobile Button -->" +                                                                                                                  
          "<div id=\"bannerLogo\">" +                                                                                                                                                         
          "   <a href=\"/\"><img src=\"/templates/logo\" alt=\"BioOne\" width=\"165\" height=\"54\" /></a>" +                                                                                 
          "</div>" +                                                                                                                                                                          
          "</div>" +                                                                                                                                                                          
          "<div id=\"pageHeaderLayout\">" +                                                                                                                                                   
          "<div id=\"headerLogo\" class=\"swirlHeader\" title=\"Stanford University\">" +                                                                                                     
          "Brought to you by: Stanford University" +                                                                                                                                          
          "</div>" +                                                                                                                                                                          
          "</div>" +                                                                                                                                                                          
          "</div>";
  private static final String headerHtmlFiltered = "";

  /* script and google translate element */
  private static final String scriptHtml =                                                                                                                                                              
      "<div style=\"float:none\"><div style=\"float:right\"><div class=\"gWidget\">" +                                                                    
          "<!-- placeholder id=null, description=Google Translator Widget -->" +                                                                                                              
          "<div style=\"margin-left:10px;\"><div id=\"google_translate_element\"></div><script>" +                                                                                            
          "function googleTranslateElementInit() {" +                                                                                                                                         
          "  new google.translate.TranslateElement({" +                                                                                                                                       
          "  }, 'google_translate_element');" +                                                                                                                                               
          "}" +                                                                                                                                                                               
          "</script><script src=\"//blah\"></script>";
  private static final String scriptHtmlFiltered =
      "<div style=\"float:none\"><div style=\"float:right\"><div class=\"gWidget\">" +                                                                    
          "<div style=\"margin-left:10px;\">";                                                                                            

  private static final String accessIconHtml =                                                                                                                                                         
      "<h4 class=\"searchTitle\">The Title" +                                                                                                                                             
          "<img src=\"/templates/jsp/_style2/_AP/_bioone/images/access_full.gif\" alt=\"full access\" title=\"full access\" class=\"accessIcon\" />" +                                        
          "</h4><p class=\"searchAuthor\">blah</p>";
  private static final String accessIconFiltered =
      "<h4 class=\"searchTitle\">The Title" +                                                                                                                                             
          "</h4><p class=\"searchAuthor\">blah</p>";

  private static final String leftColumnHtml = 
      "<div id=\"contentNav\">" +                                                                                                                                                         
          "       <div id=\"articleInfoBox\">" +                                                                                                                                              
          "        <div class=\"articleInfoCover\">" +                                                                                                                                        
          "                <img src=\"/na101/home/literatum/publisher/bioone/journals/covergifs/jmam/2010/041.035.0100/cover.jpg\" />" +                                                      
          "        </div>" +                                                                                                                                                                  
          "        <div class=\"articleInfoNav\">" +                                                                                                                                          
          "        <ul>" +                                                                                                                                                                    
          "          <li><a href=\"/loi/jmam\">List of Issues</a></li>" +                                                                                                                     
          "          <li><a href=\"/toc/jmam/38/2\">" +                                                                                                                                       
          "          Current Issue" +                                                                                                                                                         
          "          </a></li>" +                                                                                                                                                             
          "        </ul>" +                                                                                                                                                                   
          "        </div>" +                                                                                                                                                                  
          "        <br/><br/>" +                                                                                                                                                              
          "        </div>" +                                                                                                                                                                  
          "       <div class=\"columnBlock\"><!-- placeholder id=null, description=Journal Sidebar Bottom --></div>" +                                                                        
          "</div>" ;       
  private static final String leftColumnFiltered = "";

  private static final String footerHtml = 
      "<div id=\"mainFooter\">" +                                                                                                                                                         
          "    <div id=\"articleFooter\">BioOne is the blah.</div>" +                                                                                                                         
          "    <div class=\"clearFloats\">&nbsp;</div>" +                                                                                                                                     
          "        <div id=\"footerLayout\">" +                                                                                                                                               
          "            <div id=\"footerAddress\">" +                                                                                                                                          
          "                    21 Dupont Circle NW, Suite 800, Washington, DC 20036 &bull; Phone 202.296.1605 &bull; Fax 202.872.0884" +                                                      
          "                    </div>" +                                                                                                                                                      
          "        </div>" +                                                                                                                                                                  
          "        <div id=\"footerCopyright\">" +                                                                                                                                            
          "            Copyright &copy; 2013 BioOne All rights reserved" +                                                                                                                    
          "        </div>" +                                                                                                                                                                  
          "</div>";
  private static final String footerHtmlFiltered = "";

  private static final String linkRelHtml =
      "<link href=\"/templates/jsp/style.css\" rel=\"stylesheet\" type=\"text/css\" />";
  private static final String linkRelFiltered = "";

  private static final String rightColumnHashFiltered = "";  

  private static final String googleWidgetHtml = 
      "<div class=\"gWidgetContainer\"><div style=\"float:none\"><div style=\"float:right\">" +
          "<img src=\"/templates/jsp/_style2/_AP/_bioone/images/accessLarge.gif\" alt=\"Denotes Open Access Content\" />" +
          "</div></div></div>";

  private static final String googleWidgetFiltered = 
      "";

  // Yup. They really do have orphan <td> group around inner content
  private static final String refSectionWithTollfreelink=
      "<div valign=\"top\" class=\"refnumber\" id=\"bibr27\">" +
      "<td valign=\"top\">Foo (<span class=\"NLM_year\">2000</span>) " +
      "<span class=\"NLM_article-title\"> Foo Title.</span> " +
      "<span class=\"citation_source-journal\">Zool Sci</span> 17: " +
      "<span class=\"NLM_fpage\">1129</span>- <span class=\"NLM_lpage\">1136</span> " +
      "<a href=\"/servlet/linkout?suffix=bibr27&amp;dbid=4&amp;doi=10.2108%2Fzs140053&amp;key=10.2108%2Fzsj.17.1129&amp;tollfreelink=2011_106728_8dcaec470370ec38b274e723e8495ed975b7ee94917ff16a517fb1b7ae6a4a84\">BioOne</a>" +
      ", <a href=\"/servlet/linkout?suffix=bibr27&amp;dbid=8&amp;doi=10.2108%2Fzs140053&amp;key=18522469\" target=\"new\">PubMed</a>" +
      "</td>" +
      "</div>" +
      "<div valign=\"top\" class=\"refnumber\" id=\"bibr28\">";
  
  private static final String refSectionWithTollfreelink_filtered=
      "<div valign=\"top\" class=\"refnumber\" id=\"bibr27\">" +
      "<td valign=\"top\">Foo (<span class=\"NLM_year\">2000</span>) " +
      "<span class=\"NLM_article-title\"> Foo Title.</span> " +
      "<span class=\"citation_source-journal\">Zool Sci</span> 17: " +
      "<span class=\"NLM_fpage\">1129</span>- <span class=\"NLM_lpage\">1136</span> " +
      ", " +
      "</td>" +
      "</div>" +
      "<div valign=\"top\" class=\"refnumber\" id=\"bibr28\">";

  
  public static class TestHash extends TestBioOneAtyponHtmlFilterFactory {

    public void setUp() throws Exception {
      super.setUp();
      fact = new BioOneAtyponHtmlHashFilterFactory();
    }


    public void testHashFiltering() throws Exception {
      InputStream inA;
      InputStream inB;

      inA = fact.createFilteredInputStream(mau, new StringInputStream(headerHtml),
          ENC);
      assertEquals(headerHtmlFiltered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(scriptHtml),
          ENC);
      assertEquals(scriptHtmlFiltered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(accessIconHtml),
          ENC);
      assertEquals(accessIconFiltered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(leftColumnHtml),
          ENC);
      assertEquals(leftColumnFiltered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(rightColumnHtml),
          ENC);
      assertEquals(rightColumnHashFiltered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(footerHtml),
          ENC);
      assertEquals(footerHtmlFiltered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(linkRelHtml),
          ENC);
      assertEquals(linkRelFiltered,StringUtil.fromInputStream(inA));

      inA = fact.createFilteredInputStream(mau, new StringInputStream(googleWidgetHtml),
          ENC);
      assertEquals(googleWidgetFiltered,StringUtil.fromInputStream(inA));
      
      inA = fact.createFilteredInputStream(mau, new StringInputStream(refSectionWithTollfreelink),
          ENC);
      assertEquals(refSectionWithTollfreelink_filtered,StringUtil.fromInputStream(inA));
    }
  }

  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
    });
  }

  public void testCommonFiltering() throws Exception {
    InputStream inA;
    InputStream inB;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(citingArticlesHtml),
        ENC);
    assertEquals(citingArticlesFiltered,StringUtil.fromInputStream(inA));
  }
}
