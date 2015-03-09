/*
 * $Id: $
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

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestMassachusettsMedicalSocietyHtmlCrawlFilterFactory extends LockssTestCase {
  public FilterFactory fact;
  private static MockArchivalUnit mau;
  
  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings. These are shared crawl & hash filters
  
  private static final String relatedTrendsHtml =
                  "<div id=\"related\">related</div>\n" +
                  "<div id=\"trendsBox\">\n" +
                        "<div class=\"bottomAd\" style=\"display: none;\">\n" +
                                "<div class=\"ad\"></div>\n" +
                        "</div>\n" +
                  "</div>\n" +
                  "<!-- placeholder id=null, description=N-siteWide-email -->\n" +
                  "<div class=\"emailAlert\"></div>\n" +
                  "<!-- placeholder id=null, description=N-ad-article-rightRail-1 -->\n";
  private static final String relatedTrendsHtmlFiltered =
                  "\n" +
                  "\n" +
                  "<!-- placeholder id=null, description=N-siteWide-email -->\n" +
                  "<div class=\"emailAlert\"></div>\n" +
                  "<!-- placeholder id=null, description=N-ad-article-rightRail-1 -->\n";

  private static final String clickTroughHtml =
                  "<test>\n" +
                  "<a href=\"/action/clickThrough?id=3052&amp;url=%2Fclinical-practice-center%3Fquery%3Dcm&amp;loc=%2Fdoi%2Ffull%2F10.1056%2FNEJMicm040442&amp;pubId=40058838\"><img src=\"/sda/3052/Marketing_CPCenter_NewResource_300x150.jpg\"></a>\n" +
                  "<a href=\"/action/clickNotThrough?id=3052&amp;url\"></a>\n" +
                  "</test>";
  private static final String clickTroughHtmlFiltered =
                  "<test>\n" +
                  "\n" +
                  "<a href=\"/action/clickNotThrough?id=3052&amp;url\"></a>\n" +
                  "</test>";

  private static final String tabsHtml =
                  "<dl class=\"articleTabs tabPanel lastChild\">\n" +
                          "<dd id=\"article\" style=\"display: block;\">\n" +
                                "<div class=\"section\"></div>\n" +
                                "I have some text in me yay!\n" +
                          "</dd>\n" +
                          "<dd id=\"references\" style=\"display: none;\">\n" +
                                "<div class=\"section\"></div>\n" +
                          "</dd>\n" +
                          "<dd id=\"citedby\" style=\"display: none;\">\n" +
                                "<div class=\"section\"></div>\n" +
                          "</dd>\n" +
                          "<dd id=\"comments\" style=\"display: none;\">\n" +
                                "<div class=\"section\"></div>\n" +
                          "</dd>\n" +
                          "<dd id=\"letters\" class=\"lastChild\" style=\"display: none;\">\n" +
                                "<div class=\"letterContent\" rel=\"10.1056/NEJMoa1004409\"></div>\n" +
                          "</dd>\n" +
                  "</dl>";
  private static final String tabsHtmlFiltered =
                  "<dl class=\"articleTabs tabPanel lastChild\"> " +
                          "<dd id=\"article\" style=\"display: block;\"> " +
                                "<div class=\"section\"></div> " +
                                "I have some text in me yay! " +
                          "</dd> " +
                          "<dd id=\"references\" style=\"display: none;\"> " +
                                "<div class=\"section\"></div> " +
                          "</dd> " +
                          //"\n" +
                         // "\n" +
                          //"\n" +
                  "</dl>";
  
  private static final String correctionHtml =
                  "<a rel=\"10.1056/NEJMoa1002617\" name=\"articleTop\"></a>\n" +
                  "<div class=\"articleCorrection\">\n" +
                          "<span class=\"moreLink\">\n" +
                                "<a class=\"tab-article-correctionHasBeenPublished\" href=\"/doi/full/10.1056/NEJMx100104\">A Correction Has Been Published</a>\n" +
                          "</span>\n" +
                  "</div>\n" +
                  "<p class=\"articleType\">Original Article</p>";
  
  private static final String correctionHtmlFiltered =
                  "<a rel=\"10.1056/NEJMoa1002617\" name=\"articleTop\"></a>\n" +
                  "\n" +
                  "<p class=\"articleType\">Original Article</p>";
  
  private static final String galleryHtml =
                  "<div class=\" jcarousel-skin-vcmicm\">" +
                          "<div id=\"galleryContent\" class=\"carousel-type-icm jcarousel-container jcarousel-container-horizontal\" style=\"display: block;\">" +
                                  "<div class=\"jcarousel-prev jcarousel-prev-horizontal\" style=\"display: block;\" disabled=\"false\"></div>" +
                                  "<div class=\"jcarousel-next jcarousel-next-horizontal\" style=\"display: block;\" disabled=\"false\"></div>" +
                                  "<div id=\"galleryNav\" class=\"prev_next_bt\"></div>" +
                          "</div>" +
                  "</div>";
  private static final String galleryHtmlFiltered =
                  "<div class=\" jcarousel-skin-vcmicm\">" +
                  "</div>";
  
  private static final String discussionHtml =
                  "<div class=\" test\">" +
                          "<div class=\"discussion\" style=\"display: block;\">" +
                                  "Content" +
                          "</div>" +
                          "<div class=\"notdiscussion\" style=\"display: block;\">" +
                                  "Content" +
                          "</div>" +
                  "</div>";
  private static final String discussionHtmlFiltered =
                  "<div class=\" test\">" +
                          "<div class=\"notdiscussion\" style=\"display: block;\">" +
                                  "Content" +
                          "</div>" +
                  "</div>";


  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestMassachusettsMedicalSocietyHtmlCrawlFilterFactory {
          
          //Example instances mostly from pages of strings of HTMl that should be filtered
          //and the expected post filter HTML strings.
          private static final String tabsHtmlCrawlFiltered =
                          "<dl class=\"articleTabs tabPanel lastChild\">\n" +
                                  "\n" +
                                  "<dd id=\"references\" style=\"display: none;\">\n" +
                                        "<div class=\"section\"></div>\n" +
                                  "</dd>\n" +
                                  "\n" +
                                  "\n" +
                                  "\n" +
                          "</dl>";
          
          public void setUp() throws Exception {
                  super.setUp();
                  fact = new MassachusettsMedicalSocietyHtmlCrawlFilterFactory();
          }
          @Override
          public void testTabsHtmlFiltering() throws Exception {
            InputStream actIn = fact.createFilteredInputStream(mau,
                                                                                                           new StringInputStream(tabsHtml),
                                                                                                           Constants.DEFAULT_ENCODING);
            
            assertEquals(tabsHtmlCrawlFiltered, StringUtil.fromInputStream(actIn));
          }
  }

  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
      });
  }

  public void testAdsIdFiltering() throws Exception {
        String[] adTags = {"rightRailAd", "topAdBar", "rightAd"};
        String adIdHtml;
        String adIdHtmlFiltered;
        for(String adTag : adTags) {
                adIdHtml = 
                        "<test>\n" +
                                "<div id=\"" + adTag + "\">\n" +
                                        "This is an add." +
                                "</div>\n" +
                                "<div id=\"not" + adTag + "\"></div>\n" +
                        "</test>";
                adIdHtmlFiltered = 
                        "<test>\n" +"\n" +
                                "<div id=\"not" + adTag + "\"></div>\n" +
                        "</test>";
                
                InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(adIdHtml), Constants.DEFAULT_ENCODING);
                    String str = StringUtil.fromInputStream(actIn);
                    assertEquals(str, adIdHtmlFiltered);
                //assertEquals(StringUtil.fromInputStream(actIn), adIdHtmlFiltered);
                        System.out.println("FINISHED");
        }
    
  }
  
  public void testAdsClassFiltering() throws Exception {
        String[] adTags = {"toolsAd", "bottomAd", "bannerAdTower", "topLeftAniv", "ad", "rightAd"};
        String adClassHtml;
        String adClassHtmlFiltered;
        for(String adTag : adTags) {
                adClassHtml = 
                        "<test>\n" +
                                "<div class=\"" + adTag + "\">\n" +
                                        "This is an add." +
                                "</div>\n" +
                                "<div class=\"not" + adTag + "\"></div>\n" +
                        "</test>";
                adClassHtmlFiltered = 
                        "<test>\n" +"\n" +
                                "<div class=\"not" + adTag + "\"></div>\n" +
                        "</test>";
                
                InputStream actIn = fact.createFilteredInputStream(mau, new StringInputStream(adClassHtml), Constants.DEFAULT_ENCODING);
                String str = StringUtil.fromInputStream(actIn);
                assertEquals(str, adClassHtmlFiltered);
                //assertEquals(StringUtil.fromInputStream(actIn), adClassHtmlFiltered);
        }
    
  }
  
  public void testRelatedTrendsHtmlFiltering() throws Exception {
    InputStream actIn =
      fact.createFilteredInputStream(mau, new StringInputStream(relatedTrendsHtml),
                                     Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(actIn),
                relatedTrendsHtmlFiltered);
  }
  
  public void testClickTroughHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(clickTroughHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(clickTroughHtmlFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testTabsHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, 
        new StringInputStream(tabsHtml),
        Constants.DEFAULT_ENCODING);
    String str = StringUtil.fromInputStream(actIn);
    assertEquals(str, tabsHtmlFiltered);
    //assertEquals(tabsHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testGalleryHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(galleryHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(galleryHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testCorrectionHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau, 
        new StringInputStream(correctionHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(correctionHtmlFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testDiscussionHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(discussionHtml),
        Constants.DEFAULT_ENCODING);

    assertEquals(discussionHtmlFiltered, StringUtil.fromInputStream(actIn));
  }  
  
}
