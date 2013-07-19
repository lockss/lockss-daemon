/*
 * $Id: TestBioMedCentralPluginHtmlFilterFactory.java,v 1.7 2013-07-19 21:23:08 aishizaki Exp $
 */

/*

 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bmc;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.test.*;

public class TestBioMedCentralPluginHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private BioMedCentralHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BioMedCentralHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String inst1 = "<li class=\"greeting\"><strong>Welcome <span id=\"username\">Indiana University at Bloomington</span></strong></li>";

  private static final String inst2 = "<li class=\"greeting\"><strong>Welcome <span id=\"username\">Stanford University</span></strong></li>";
  
  private static final String articlesTab1 = "<li><a href=\"/content\"><span id=\"articles-tab\"\">Articles</span></a></li>";
  
  private static final String articlesTab2 = "<li><a href=\"/content\"><span id=\"articles-tab\">Articles</span></a></li>";
  
  private static final String whiteSpace1 = "\n  <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n (543KB)\n </li>";
  
  private static final String whiteSpace2 = "\n\n      <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n       (543KB)\n      </li>";
 
  private static final String impactFactorHtmlHash =
    "<div id=\"\" style=\"width:830px; height:600px\">\n" +
    "<div id=\"impact-factor\" class=\"official\">\n" +
    "<img src=\"/images/branding/official.gif\" alt=\"official impact factor\" title=\"3.42\"/>\n" +
    "<span id=\"impact-factor-value\">3.42</span></div>\n" +
    "</div>";
  
  private static final String impactFactorHtmlHashFiltered =
    "<div id=\"\" style=\"width:830px; height:600px\"> </div>";

  private static final String accessHtmlHash =
    "<div class=\"article-type\">Software <a href=\"/about/access\">\n" +
    "<img alt=\"Open Access\" src=\"images/open-access.gif\"/></a>\n" +
    "<a href=\"http://www.biomedcentral.com/about/mostviewed\">" +
    "<img alt=\"Highly Accessed\" src=\"/images/highly-accessed.gif\"/></a>\n" +
    "</div>";

  private static final String accessHtmlHashFiltered =
    "<div class=\"article-type\">Software </div>";

  private static final String accessesBlockHtml =
    "<div class=\"wrap\">\n" +
    "<h2 class=\"active\" id=\"accesses\">Accesses</h2>\n" +
    "<div class=\"wrap-in\"> <ul> <li> 30 </li> </ul> </div>\n" +
    "</div>";

  private static final String accessesBlockHtmlFiltered = 
    "<div class=\"wrap\"> " +
    "<h2 class=\"active\" id=\"accesses\">Accesses</h2> " +
    "</div>";
  
  private static final String citationsBlockHtml =
    "<div class=\"wrap\">\n" +
    "<h2 class=\"active\" id=\"citations\">Cited by</h2>\n" +
    "<div class=\"wrap-in\"> <ul> <li> 30 </li> </ul> </div>\n" +
    "</div>";
  
  private static final String citationsBlockHtmlFiltered =
    "<div class=\"wrap\"> " +
    "<h2 class=\"active\" id=\"citations\">Cited by</h2> " +
    "</div>";

  private static final String bannerAdHtmlHash =
    "<a class=\"banner-ad\"" +
    "href=\"http://www.biomedcentral.com/advertisers/digital_advertising\"></a>" +
    "<div id=\"\" style=\"width:830px; height:600px\"> </div>";
  
  private static final String bannerAdHtmlHashFiltered =
    "<div id=\"\" style=\"width:830px; height:600px\"> </div>";

  private static final String SkyscraperAdHtmlHash =
    "<a class=\"skyscraper-ad\"" +
    "href=\"http://www.biomedcentral.com/advertisers/digital_advertising\">" +
    "Advertisement</a>" +
    "<div id=\"\" style=\"width:830px; height:600px\"> </div>";
  
  private static final String SkyscraperAdHtmlHashFiltered =
    "<div id=\"\" style=\"width:830px; height:600px\"> </div>";

  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;

    /* inst1 test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst1),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(inst2),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),
        StringUtil.fromInputStream(inB));


    /* articlesTab test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(articlesTab1),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(articlesTab2),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),
        StringUtil.fromInputStream(inB));

    /* whiteSpace test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(whiteSpace1),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(whiteSpace2),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),
        StringUtil.fromInputStream(inB));

    /* impactFactor test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(impactFactorHtmlHash),
        ENC);

    assertEquals(impactFactorHtmlHashFiltered,StringUtil.fromInputStream(inA));

    /* access test */     
    inA = fact.createFilteredInputStream(mau, new StringInputStream(accessHtmlHash),
        ENC);

    assertEquals(accessHtmlHashFiltered,StringUtil.fromInputStream(inA));

    /* access block */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(accessesBlockHtml),
        ENC);

    assertEquals(accessesBlockHtmlFiltered,StringUtil.fromInputStream(inA));


    /* citations block */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(citationsBlockHtml),
        ENC);

    assertEquals(citationsBlockHtmlFiltered,StringUtil.fromInputStream(inA));

    /* bannerAd */     
    inA = fact.createFilteredInputStream(mau, new StringInputStream(bannerAdHtmlHash),
        ENC);

    assertEquals(bannerAdHtmlHashFiltered,StringUtil.fromInputStream(inA));

    /* SkyscraperAd */     
    inA = fact.createFilteredInputStream(mau, new StringInputStream(SkyscraperAdHtmlHash),
        ENC);

    assertEquals(SkyscraperAdHtmlHashFiltered,StringUtil.fromInputStream(inA));

  }
  private static final String SocialNetworkingHash = "<ul id=\"social-networking-links\">"+
        "<li>"+
        "<div class=\"fb-like\" data-href=\"http://genomebiology.com/2011/12/10/R101\" data-send=\"false\" data-layout=\"button_count\" data-width=\"100\" data-show-faces=\"false\" data-action=\"recommend\"></div>"+
        "</li></ul>Hello World";
  private static final String SocialNetworkingHashFiltered = "Hello World";

  public void testFilterSocialNetworking() throws Exception {
    InputStream inA;
    /* SkyscraperAd */     
    inA = fact.createFilteredInputStream(mau, new StringInputStream(SocialNetworkingHash),
        ENC);

    assertEquals(SocialNetworkingHashFiltered,StringUtil.fromInputStream(inA));

  }
  private static final String GoogleAdHash = "<dl class=\"google-ad wide \">"+
        "<dt class=\"hide\">"+
        "</dt></dl>Hello World";
  private static final String GoogleAdHashFiltered = "Hello World";

  public void testFilterGoogleAd() throws Exception {
    InputStream inA;
    /* SkyscraperAd */     
    inA = fact.createFilteredInputStream(mau, new StringInputStream(GoogleAdHash),
        ENC);

    assertEquals(GoogleAdHashFiltered,StringUtil.fromInputStream(inA));

  }
}