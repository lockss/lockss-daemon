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

import org.lockss.test.*;

public class TestBMCPluginHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  private static Logger log = Logger.getLogger(TestBMCPluginHtmlFilterFactory.class);

  private BMCPluginHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BMCPluginHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String inst1 = "<dl class=google-ad </dl>"
      + "<ul>Fill in SOMETHING SOMETHING</ul>";
  private static final String inst2 = "<dl class=google-ad wide</dl>"
      + "<ul>Fill in SOMETHING SOMETHING</ul>";

  private static final String inst1result = "<ul>Fill in SOMETHING SOMETHING</ul>";
  private static final char cNewL = '\n';
  private static final char cFormf = '\f';
  private static final char cEnter = '\r';
  private static final char[] cArr = {cNewL, cFormf, cEnter};
  private static final String whiteSp = new String(cArr);
  // add random whitespaces to test whitespace filter 
  //    must add at least one whitespace char to match commonResult
  private static final String inst3 = "  <ul id=\"social-networking-links\"> LALALA </ul>  Hello World";
  private static final String inst4 = " <div id=\"impact-factor\" class=\"official\"></div>     Hello World";
  private static final String inst5 = "   <a href=\"/sfx_links?ui=1471-2105-13-230&amp;bibl=B1\" onclick=\"popup('/sfx_links?ui=1471-2105-13-230&amp;bibl=B1','SFXMenu','460','420'); return false;\">          <img src=\"/sfx_links?getImage\" alt=\"OpenURL\" align=\"absmiddle\"></a>Hello World";
  private static final String inst6 = "          <a href=\"http://www.helloworld.com/about/mostviewed/\">  <img alt=\"Highly Accessed\" src=\"/images/articles/highlyaccessed-large.png\" class=\"access mr15\"/>       </a>Hello World";
  private static final String inst7 = " <link rel=\"stylesheet\" type=\"text/css\" href=\"/bmcbioinformatics/css/themes/1002.css?1330085853225\" media=\"screen, print\"/>       Hello World";
  // new extreme hashing - should remove headers, footers and both right/left navigation and ad areas
  private static final String inst8 ="<div id=\"branding\" role=\"banner\"> <dl class=\"google-ad wide \">"+
      " </dl></div>       Hello World";
  private static final String inst9 ="<div id=\"left-article-box\" class=\"left-article-box\"> <dl class=\"google-ad wide \">"+
      whiteSp + " </dl>  </div>       Hello World";
  private static final String inst10 = whiteSp + "<div id=\"article-navigation-bar\" role=\"banner\"> <dl class=\"google-ad wide \">"+
      " </dl>         </div>       Hello World";
  // added a space before 'Hello World' to match consolidated white space
  private static final String commonResult = " Hello World";

  private static final String inst11 = 
      "<html><p style=\"line-height:160%\" class=\"inlinenumber\">" +
          "<m:math xmlns:m=\"http://www.w3.org/1998/Math/MathML\" >" +
          "<m:mrow>" +
          "</m:mrow>" +
          "</p></html>";
  private static final String inst12 = 
      "<html><div style=\"display:table;width:100%;*display:inline\">" +
          "<m:math xmlns:m=\"http://www.w3.org/1998/Math/MathML\" >" +
          "<m:mrow>" +
          "</m:mrow>" +
          "</div></html>";
  private static final String inst13 = 
      "<html><span class=\"mathjax\">" +
          "</span></html>";
  private static final String inst1123Filtered = "<html></html>";

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



  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    String sA, sB;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst1),
        ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(inst1result),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst2), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(inst1result),
        ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();

    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst3), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(commonResult), ENC);
    //assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    sA=StringUtil.fromInputStream(inA);
    sB=StringUtil.fromInputStream(inB);
    assertEquals(sA, sB);
    inA.close();
    inB.close();    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst4), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(commonResult), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst5), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(commonResult), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst6), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(commonResult), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst7), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(commonResult), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst8), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(commonResult), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst9), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(commonResult), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();
    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst10), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(commonResult), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();
  }
  public void testInlineNumber() throws Exception {
    InputStream inA;
    InputStream inB;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst11), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(inst1123Filtered), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();

    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst12), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(inst1123Filtered), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();

    inA = fact.createFilteredInputStream(mau, new StringInputStream(inst13), ENC);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(inst1123Filtered), ENC);
    assertEquals(StringUtil.fromInputStream(inA), StringUtil.fromInputStream(inB));
    inA.close();
    inB.close();
  }
  
  public void testfloatingMsg() throws Exception {
    InputStream inA;
    inA = fact.createFilteredInputStream(mau, new StringInputStream(floatingMsg), ENC);
    assertEquals(floatingMsgFiltered,StringUtil.fromInputStream(inA));
  }
}
