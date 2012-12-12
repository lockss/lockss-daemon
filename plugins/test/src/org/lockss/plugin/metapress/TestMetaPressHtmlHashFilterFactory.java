/*
 * $Id: TestMetaPressHtmlHashFilterFactory.java,v 1.3 2012-12-12 23:15:56 alexandraohlson Exp $
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
import org.lockss.daemon.PluginException;
import org.lockss.test.*;

public class TestMetaPressHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private MetaPressHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MetaPressHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String inst1 =
    "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlBody\">" +
    "Stanford University Libraries <nobr>(379-06-324)</nobr></div>";
  
  private static final String inst2 =
    "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_RecognizedAsControlBody\">" +
    "Indiana University, Bloomington <nobr>(641-02-015)</nobr></div>";
 
  private static final String footerInfoHtml =
      "      <div class=\"pageFooterMP\">" +
          "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControl\">" + 
          "<div id=\"SitePrivacyPolicy\" align=\"center\" class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControlUserDetails\">" +
          "<a style=\"padding:0.6em;\" href=\"http://public.metapress.com/download/common/MetaPress_Privacy.pdf\" target=\"_blank\">Metapress Privacy Policy</a>" +
          "</div><div class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControlUserDetails\">" +
          "Remote Address:&nbsp;171.66.236.239&nbsp;¥&nbsp;Server:&nbsp;MPSHQWBRDR02P<br>HTTP User Agent:&nbsp;Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/536.26.17 (KHTML, like Gecko) Version/6.0.2 Safari/536.26.17<br><br>" +
          "</div><div class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControlUserDetails\" align=\"center\">" +
          "</div>" +
          "</div>" +
          "</div>";
  private static final String footerInfoFiltered =
      "      <div class=\"pageFooterMP\">" +
          "<div class=\"MetaPress_Products_Reader_Web_UI_Controls_FooterControl\">" + 
          "</div>" +
          "</div>";
  
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
      "<td class=\"pageLeft\"> " +    
          "<br /><br /><img src=\"../images/common/spacer.gif\" style=\"height:1px;width:176;\" />" +           
          "</td>";

  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;

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
    
    
  }
}