/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.metapress;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestMetapressHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.ENCODING_UTF_8;

  private MetapressHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MetapressHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String inst1 =
    "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlBody\">" +
    "Stanford University Libraries <nobr>(379-06-324)</nobr></div>";
  
  private static final String inst2 =
    "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlBody\">" +
    "Indiana University, Bloomington <nobr>(641-02-015)</nobr></div>";
 
  private static final String footerInfoHtml =
      "    <div class=\"pageFooterMP\">" +
          "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControl\">" + 
          "<div id=\"SitePrivacyPolicy\" align=\"center\" class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControlUserDetails\">" +
          "<a style=\"padding:0.6em;\" href=\"http://public.metapress.com/download/common/MetaPress_Privacy.pdf\" target=\"_blank\">Metapress Privacy Policy</a>" +
          "</div><div class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControlUserDetails\">" +
          "Remote Address:&nbsp;171.66.236.239&nbsp;�&nbsp;Server:&nbsp;MPSHQWBRDR02P<br>HTTP User Agent:&nbsp;Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/536.26.17 (KHTML, like Gecko) Version/6.0.2 Safari/536.26.17<br><br>" +
          "</div><div class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControlUserDetails\" align=\"center\">" +
          "</div>" +
          "</div>" +
          "</div>";
  private static final String footerInfoFiltered =
      "    ";
  
  private static final String personalMenuHtml =
      "<td class=\"pageLeft\"> " +    
          "<div id=\"ctl00_PersonalizationPanel\" Class=\"PersonalizationPanel\">" +
          "<div id=\"institutionalLoginRenderAsControl\" class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlBody\">" +
          " <b><a href=\"http://multi-science.metapress.com/institutional-login.aspx?returnUrl=http%3a%2f%2fmulti-science.metapress.com%2fcontent%2f84w427t02j655q17\">Institutional Login</a></b>" +
          "</div><div class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlHeading\">" +
          "  Recognized as:" +
          "</div><div class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlBody\">" +
          "       Stanford University Libraries <nobr>(379-06-324)</nobr>" +
          "</div>" +
          "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_LoggedInAsControlHeading\">" +
          "   Welcome!" +
          " </div><div class=\"MetaPress_Products_Reader_Web_UI_Controls_LoggedInAsControlBody\">" +
          "  To use the personalized features of this site, please <b><a href=\"https://multiscience.metapress.com/identities/me/?sid=aaw4jsnjq1dj05zm3p3at3xh&amp;sh=multi-science.metapress.com\">log in</a></b> or <b><a href=\"/identities/registration/\">register</a></b>." +
          "</div><div class=\"MetaPress_Products_Reader_Web_UI_Controls_LoggedInAsControlBody\">" +
          "If you have forgotten your username or password, we can <b><a href=\"https://multiscience.metapress.com/identities/help/?sid=aaw4jsnjq1dj05zm3p3at3xh&amp;sh=multi-science.metapress.com\">help</a></b>." +
          "</div>" +    
          "<div class=\"PersonalizationMenuHeader\">" +
          "Home" +
          "</div>" +
          "<div class=\"PersonalizationMenuItem\">" +
          "<a href=\"../home/main.mpx\">" +
          "Home" +
          "</a>" +
          "</div>" +
          "<div class=\"PersonalizationMenuItem\">" +
          "<a href=\"../publications\">" +
          "Browse Publications" +
          "</a>" +
          "</div>" +
          "<div class=\"PersonalizationMenuHeader\">" +
          "Saved Items" +
          "</div>" +
          "<div class=\"defaultPadding\">" +
          "<div class=\"PersonalizationMenuItem\">" +
          "<a href=\"../personalization/saved-items.mpx\" label=\"All\" count=\"0\">" +
          "All" +
          "</a>" +
          "</div>" +  
          "</div>" +           
          "</div>" +            
          "<br /><br /><img src=\"../images/common/spacer.gif\" style=\"height:1px;width:176;\" />" +           
          "</td>";
  private static final String personalMenuFiltered =
      "";

  // and this string meets the criteria for FooterUserDetailContainer
  static final String footerInfo2Html =
      "<div class=\"footer-copyright-privacy\">" +
          "<div class=\"DynamicContent\">" +
          "Copyright&nbsp;\u00a9" + //unicode 00A9 is the copyright symbol
          "</div>" +
          "</div>" +
          "<div class=\"FooterUserDetailContainer\">" +
          "<div id=\"toggle\">" +
          "<a id=\"open\" class=\"open\">Support Information</a>" +
          "<a id=\"close\" class=\"close\">Support Information (click to close)</a>" +           
          "</div>" +
          "<div class=\"FooterUserDetailContainerPanel\">" +
          "<B>Remote Address:</B>&nbsp;171.66.236.252&nbsp;<B>•&nbsp;Server:" +
          "</B>&nbsp;MPSHQWBRDR01P<br><B>HTTP User Agent:</B>&nbsp;Mozilla/5.0 " +
          "(X11; Ubuntu; Linux x86_64; rv:22.0) Gecko/20100101 Firefox/22.0<br><br>" +
          "</div>" +
          "</div>";
  static final String footerInfo2Filtered =
      "<div class=\"footer-copyright-privacy\">" +
          "<div class=\"DynamicContent\">" +
          "Copyright&nbsp;\u00a9" +
          "</div>" +
          "</div>";


    public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    InputStreamReader inT;

    inA = fact.createFilteredInputStream(mau,
           new StringInputStream(inst1), ENC);
    inB = fact.createFilteredInputStream(mau,
           new StringInputStream(inst2), ENC);
    assertEquals(StringUtil.fromInputStream(inA),
                 StringUtil.fromInputStream(inB));
    
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(footerInfoHtml), ENC);

    assertEquals(footerInfoFiltered,StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(personalMenuHtml), ENC);

    assertEquals(personalMenuFiltered,StringUtil.fromInputStream(inA));
    
    /* Use encoding to generate the InputStream to handle int'l char */
    inA = fact.createFilteredInputStream(mau,
        new ByteArrayInputStream(footerInfo2Html.getBytes(ENC)), ENC);
    inT = new InputStreamReader(new ByteArrayInputStream(footerInfo2Filtered.getBytes(ENC)), 
        ENC); 
 
    assertEquals(StringUtil.fromReader(inT),StringUtil.fromInputStream(inA));
    
  }

  // This string doesn't meet the criteria
  static final String testAcceptHtml =
      "</h2><table cellpadding=\"0\" cellspacing=\"0\">" +
          "<tr>" +
          "<td class=\"labelName\">Journal</td><td class=\"labelValue\"><a href=\"/content/122707/?pi=0\">Journal Studies</a></td>" +
          "</tr><tr>" +
          "<td class=\"labelName\">Publisher</td><td class=\"labelValue\">Manchester University Press</td>" +
          "</tr><tr>" +
          "<td class=\"labelName\">ISSN</td><td class=\"labelValue\">1362-xxxx (Print)<br/>2050-xxxx (Online)</td>" +
          "</tr>" +
          "</table></td><td valign=\"top\" class=\"MPReader_Content_PrimitiveHeadingControlSecondaryLinks\"><div>" +
          "<a href=\"/personalization/save-item.mpx?code=Q433X32487\">Add to saved items</a>" +
          "</div><div>" +
          "<a href=\"/personalization/email-item.mpx?code=Q433X32487&amp;p=16c51d2495494dc68c5e7c07d1bd3296&amp;pi=0\">Recommend this volume</a>" +
          "</div></td>" +
          "</tr><tr>" +
          "<td></td>" +
          "</tr>" +
          "</table>" +
          "</div><br /><br />  "; 
  
  static final String testAcceptFiltered =
      "</h2><table cellpadding=\"0\" cellspacing=\"0\">" +
          "<tr>" +
          "<td class=\"labelName\">Journal</td><td class=\"labelValue\"><a href=\"/content/122707/?pi=0\">Journal Studies</a></td>" +
          "</tr><tr>" +
          "<td class=\"labelName\">Publisher</td><td class=\"labelValue\">Manchester University Press</td>" +
          "</tr><tr>" +
          "<td class=\"labelName\">ISSN</td><td class=\"labelValue\">1362-xxxx (Print)<br/>2050-xxxx (Online)</td>" +
          "</tr>" +
          "</table></td>" +
          "</tr><tr>" +
          "<td></td>" +
          "</tr>" +
          "</table>" +
          "</div><br /><br />  "; 
  
  // This string caused a nullpointerexception because it *almost* met the criteria
  static final String testAccept2 =
      "<div class=\"primitiveControl\">" +
          "<div class=\"listItemName\">" +
          "<a href=\"/content/n44k27l62k66/?p=277e24c14a064b77b8268ad0b80ebfc3&amp;pi=0\">Number 1 / May 2012</a>" +
          "</div><table cellpadding=\"0\" cellspacing=\"0\">" +
          "<div class=\"labelValue\">" +
          "Journal Materials of the Century" +
          "</div><tr>" +
          "<th scope=\"row\" class=\"labelName\">Editor</th><td class=\"labelValue\"> Angela Editor</td>" +
          "</tr>" +
          "</table>" +
          "</div>";

    static final String testAccept2Filtered =
        "<div class=\"primitiveControl\">" +
            "<div class=\"listItemName\">" +
            "</div><table cellpadding=\"0\" cellspacing=\"0\">" +
            "<div class=\"labelValue\">" +
            "Journal Materials of the Century" +
            "</div><tr>" +
            "<th scope=\"row\" class=\"labelName\">Editor</th><td class=\"labelValue\"> Angela Editor</td>" +
            "</tr>" +
            "</table>" +
            "</div>";
    
    // and this string meets the criteria and will get filteredn
    static final String testAccept3 =
        "<div class=\"primitiveControl\">" +
            "<div class=\"listItemName\">" +
            "</div><table cellpadding=\"0\" cellspacing=\"0\">" +
            "<div class=\"labelValue\">" +
            "Journal Materials of the Century" +
            "</div><tr>" +
            "<th scope=\"row\" class=\"labelName\">Editor</th><td class=\"labelValue\">" +
            "<div><a href=\"/home/linkout.mpx?action\">REMOVE ME</a></div></td>" +
            "</tr>" +
            "</table>" +
            "</div>";
    static final String testAccept3Filtered =
        "<div class=\"primitiveControl\">" +
            "<div class=\"listItemName\">" +
            "</div><table cellpadding=\"0\" cellspacing=\"0\">" +
            "<div class=\"labelValue\">" +
            "Journal Materials of the Century" +
            "</div>" +
            "</table>" +
            "</div>";

  
     public void testAcceptNode() throws Exception 
       {
       InputStream inA;
       
       inA = fact.createFilteredInputStream(mau,
           new StringInputStream(testAcceptHtml), ENC);

       assertEquals(testAcceptFiltered,StringUtil.fromInputStream(inA));
     
       inA = fact.createFilteredInputStream(mau,
           new StringInputStream(testAccept2), ENC);

       assertEquals(testAccept2Filtered,StringUtil.fromInputStream(inA));
       
       inA = fact.createFilteredInputStream(mau,
           new StringInputStream(testAccept3), ENC);

       assertEquals(testAccept3Filtered,StringUtil.fromInputStream(inA));
            
       }

}