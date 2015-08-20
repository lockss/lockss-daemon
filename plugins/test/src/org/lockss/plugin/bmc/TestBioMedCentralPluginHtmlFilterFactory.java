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
  
  private static final String HeadHtmlHash = 
    "<html><head>" +
    "<script>" +
    "window.onmessage = function(e) {" +
    "if(e.data == \"biome-failed\") {" +
    "console.log(e);" +
    "document.getElementById(\"biome-badge\").style.display = \"none\";" +
    "}" +
    "};" +
    "</script>" +
    "</head></html>";
  private static final String HeadHtmlHashFiltered =
    "<html></html>";
  
  private static final String BiomeBadgeHash = 
    "<div id=\"mobile-sidebar\">" +
    "<div id=\"biome-badge\" style=\"width: 100%\">" +
    "<script>" +
    "window.onmessage = function(e) {" +
    "if(e.data == \"biome-failed\") {" +
    "console.log(e);" +
    "document.getElementById(\"biome-badge\").style.display = \"none\";" +
    "}" +
    "};" +
    "</script>" +
    "<iframe src=\"http://www.biomedcentral.com/sites/9001/biome-widget.html?doi=10.1186/1757-1146-1-9&size=large\" width=\"100%\" height=\"75px\">Your browser does not support iframes</iframe>" +
    "</div>" + "</div>" +
    "Hello World";
  private static final String BiomeBadgeHashFiltered = "Hello World";
  
  private static final String InlineNumberHash = 
    "<html><p style=\"line-height:160%\" class=\"inlinenumber\">" +
    "<m:math xmlns:m=\"http://www.w3.org/1998/Math/MathML\" >" +
    "<m:mrow>" +
    "</m:mrow>" +
    "</p></html>";
  private static final String DisplayInlineHash = 
    "<html><div style=\"display:table;width:100%;*display:inline\">" +
    "<m:math xmlns:m=\"http://www.w3.org/1998/Math/MathML\" >" +
    "<m:mrow>" +
    "</m:mrow>" +
    "</div></html>";
  private static final String MathJaxHash = 
    "<html><span class=\"mathjax\">" +
    "</span></html>";
  private static final String InlineNumberHashFiltered =
    "<html></html>";
  
  private static final String floatingMsg=
      "<html><style>" +
          ".banner-footer {  text-align: initial !important;" +
          "  font-size: initial; width: 100%; height: 5.2%;;" +
          "-webkit-transition: height 500ms ease-in 1s;" +
          "    -moz-transition: height 500ms ease-in 1s;" +
          "    -o-transition: height 500ms ease-in 1s;" +
          "    transition: height 500ms ease-in 1s; cursor: hand; 0.8" +
          "}" +
          "</style>" +
          "<noscript>" +
          "&lt;style&gt;" +
          ".banner-footer--instart {display: block !important}" +
          "&lt;/style&gt;" +
          "</noscript>" +
          "<div class=\"banner-footer\">" +
          "<i class=\"banner-footer--handle\">&nbsp;</i>" +
          "<div class=\"banner-footer--panel\"><div>" +
          "<span class=\"banner-footer--text\">Try out the new beta version of our site</span> " +
          "<a type=\"button\" class=\"banner-footer--button\" target=\"oscar-site\" " +
          "href=\"http://beta.bmcpalliatcare.com/article/10.1186/s12904-015-0029-8\" " +
          "onclick=\"_gaq.push(['_trackEvent', 'REFERRAL FROM BMC-JOURNAL PLATFORM', " +                                                                                  
          "'BMC BETA BANNER', '/1472-684X/14/31', 1, true]);\">Take me there</a>" +
          "<i class=\"banner-footer--close\">&nbsp;</i></div></div></div>" +
          "</html>";
      
      private static final String floatingMsgFiltered=
          "<html></html>";


  private void checkHashFilter2(String testStr1, String testStr2) throws Exception {
    InputStream inA;
    InputStream inB;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(testStr1),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(testStr2),
        ENC);  
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));

  }
  private void checkHashFilter1(String testStr, String filtStr) throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(testStr),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA), filtStr);

  }
  
  public void testFiltering() throws Exception {
    
    /* inst1 test */
    checkHashFilter2(inst1, inst2);

    /* articlesTab test */
    checkHashFilter2(articlesTab1, articlesTab2);

    /* whiteSpace test */
    checkHashFilter2(whiteSpace1, whiteSpace2);

    /* impactFactor test */
    checkHashFilter1(impactFactorHtmlHash, impactFactorHtmlHashFiltered);

    /* access test */     
    checkHashFilter1(accessHtmlHash, accessHtmlHashFiltered);

    /* access block */
    checkHashFilter1(accessesBlockHtml, accessesBlockHtmlFiltered);

    /* citations block */
    checkHashFilter1(citationsBlockHtml, citationsBlockHtmlFiltered);

    /* bannerAd */     
    checkHashFilter1(bannerAdHtmlHash, bannerAdHtmlHashFiltered);

    /* SkyscraperAd */     
    checkHashFilter1(SkyscraperAdHtmlHash, SkyscraperAdHtmlHashFiltered);

    /* head */     
    checkHashFilter1(HeadHtmlHash, HeadHtmlHashFiltered);

  }
  private static final String SocialNetworkingHash = "<ul id=\"social-networking-links\">"+
        "<li>"+
        "<div class=\"fb-like\" data-href=\"http://genomebiology.com/2011/12/10/R101\" data-send=\"false\" data-layout=\"button_count\" data-width=\"100\" data-show-faces=\"false\" data-action=\"recommend\"></div>"+
        "</li></ul>Hello World";
  private static final String SocialNetworkingHashFiltered = "Hello World";

  public void testFilterSocialNetworking() throws Exception {
    
    checkHashFilter1(SocialNetworkingHash, SocialNetworkingHashFiltered);
  }
  private static final String GoogleAdHash = "<dl class=\"google-ad wide \">"+
        "<dt class=\"hide\">"+
        "</dt></dl>Hello World";
  private static final String GoogleAdHashFiltered = "Hello World";

  public void testFilterGoogleAd() throws Exception {
    
    checkHashFilter1(GoogleAdHash, GoogleAdHashFiltered);

  }
  public void testFilterBiomeBadge() throws Exception {
    
    checkHashFilter1(BiomeBadgeHash, BiomeBadgeHashFiltered);

  }
  
  public void testFilterInlineNumber() throws Exception {
    
    checkHashFilter1(InlineNumberHash, InlineNumberHashFiltered);
    checkHashFilter1(DisplayInlineHash, InlineNumberHashFiltered);
    checkHashFilter1(MathJaxHash, InlineNumberHashFiltered);


  }
  public void testFilterFloatingMsg() throws Exception {
    
    checkHashFilter1(floatingMsg, floatingMsgFiltered);

  }
}