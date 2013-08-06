/*  $Id: TestBaseAtyponHtmlHashFilterFactory.java,v 1.1 2013-08-06 21:24:24 aishizaki Exp $
 
 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.atypon;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestBaseAtyponHtmlHashFilterFactory extends LockssTestCase {
  private BaseAtyponHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BaseAtyponHtmlHashFilterFactory();
  }
  
  private static final String withHeader =
      "<div class=\"block\">"
          + "<div id=\"header\">"
          + "<div class=\"welcome stackContents\">"
          + "<div class=\"stackedReverse\">"
          + "<div class=\"loginIdentity\">"
          + "Welcome"
          + "<a href=\"https://boguspublisherlibrary.org/action/showLogin?"
          + "uri=%2Ftoc%2Fijgnai%2F12%2F1\">"
          + "     Log in"
          + "</a>"
          + "<a href=\"https://boguspublisherlibrary.org/action/registration\">"
          + "    Register"
          + "</a>"
          + "<a href=\"https://secure.boguspublisher.org/BOGUSPUBLISHERWebSite/Secure/"
          + "CheckOut/AddToCart.aspx\">View Cart</a>"
          + "</div>"
          + "<ul class=\"linkList topLinks menu\">"
          + ""
          + "<li class=\"\">"
          + "         <a href=\"http://www.boguspublisher.org\" target=\"new\">BOGUSPUBLISHER</a>"
          + "     </li>"
          + "<li class=\"\">"
          + "         <a href=\"http://www.boguspublisher.org/About-Civil-Engineering/\""
          + " target=\"new\">About Civil Engineering</a>"
          + "     </li>"
          + "     <li class=\"\">"
          + "         <a href=\"/page/contactus\">Contact Us</a>"
          + "     </li>"
          + "     <li class=\"\">"
          + "         <a href=\"https://go.boguspublisher.org/forms/foundation/"
          + "boguspublisher-foundation\" target=\"new\">Donate Now</a>"
          + "     </li>"
          + "     <li class=\"\">"
          + "        <a href=\"/page/about/mobile\">Mobile</a>"
          + "     </li>"
          + "     <li class=\"\">"
          + "        <a href=\"http://www.boguspublisher.org/members/\""
          + " target=\"new\">MyBOGUSPUBLISHER</a>"
          + "     </li>"
          + "     <li class=\"\">"
          + "        <a href=\"http://www.boguspublisher.org/Content.aspx?id=2147487201\""
          + " target=\"new\">Shop BOGUSPUBLISHER</a>"
          + "     </li>"
          + "</ul>"
          + "</div>"
          + "</div>"
          + "</div>"
          + "</div>";
  
  private static final String withoutHeader =
      "<div class=\"block\"></div>";
      
  private static final String withJScript =
      "<script type=\"text/javascript\">"
      +"var curvyCornersVerbose = false;"
      +"</script>Hello World";
  private static final String withoutJScript =
      "Hello World";
  
  private static final String withStylesheet =
      "<link rel=\"stylesheet\" type=\"text/css\""
      +"\"href=\"/templates/jsp/grids-min.css\">Hello World";
 
  private static final String withoutStylesheet =
    "Hello World";
  
  private static final String withAccessIcon =
      "<div class=\"block\"><img src=\"/templates/jsp/images/access_full.gif\""
      +"\" class=\"accessIcon\">Hello World</div>";
  private static final String withoutAccessIcon =
    "<div class=\"block\">Hello World</div>";

  private static final String withFooter =
      "<div class=\"block\">"
          + "<div id=\"footer\">"
          + "<div class=\"footer\">"
          + "<img src=\"pubpartners1.jpg\" alt=\"pubpartner1\" "
          + "usemap=\"#pubpartner1.jpg\">"
          + "<map name=\"pubpartners1.jpg\">"
          + "<area shape=\"rect\" coords=\"184,50,277,92\" "
          + "href=\"http://www.blah.org/\" title=\"blah.org\" "
          + "alt=\"blah.org\">"
          + "</map>"
          + "<div id=\"copyright\">"
          + "<div id=\"footer_links_grey\"><span class=\"fontSize2\">"
          + "<span style=\"font-family: arial,helvetica,sans-serif;\">"
          + "<span style=\"color: rgb(0, 0, 0);\">"
          + "<a href=\"http://www.blahboguspublisher.org/\" target=\"_blank\">"
          + "Blahboguspublisher Home</a>&nbsp;&nbsp; | &nbsp; "
          + "<a href=\"http://blah.blah.org/\" target=\"_blank\">"
          + "Civil Engineering Database</a>&nbsp;&nbsp; |&nbsp;&nbsp; "
          + "<a href=\"http://www.boguspublisher.org/PPLContent.aspx?id=2147486529\" "
          + "target=\"_blank\">Bookstore</a>&nbsp;&nbsp; | &nbsp; "
          + "<a href=\"/page/journalsubscriptioninformation\" "
          + "target=\"_blank\">Subscribe</a> &nbsp; |&nbsp;&nbsp; "
          + "<a href=\"/page/termsofuse\" target=\"_blank\">"
          + "BOGUSPUBLISHER Library Terms of Use</a>&nbsp;&nbsp; | &nbsp; "
          + "<a href=\"/page/contactus\" target=\"_blank\">Contact Us</a>"
          + "<br>"
          + "</span></span></span></div>"
          + "</div>"
          + "<div id=\"footer_message\"><span style=\"color: rgb(0, 0, 0);\">"
          + "<span class=\"fontSize2\"><span style=\"color: rgb(0, 0, 0);\">"
          + "Copyright © 1996-2013, American So</span>"
          + "ciety of Civil Engineers</span>"
          + "<br>"
          + "</span></div>"
          + "<span style=\"color: rgb(0, 0, 0);\">"
          + "<span style=\"font-family: arial,helvetica,sans-serif;\">"
          + "<span class=\"fontSize2\"><a title=\"Copyright\" "
          + "href=\"http://www.boguspublisher.org/copyright\" target=\"_blank\">"
          + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; "
          + "BOGUSPUBLISHER Copyright</a> &nbsp; <a title=\"FAQs\" "
          + "href=\"http://www.boguspublisher.org/faqs\" target=\"_blank\">"
          + "BOGUSPUBLISHER FAQs</a>&nbsp;&nbsp; <a title=\"Privacy\" "
          + "href=\"http://www.boguspublisher.org/privacy\" target=\"_blank\">"
          + "BOGUSPUBLISHER Privacy</a>&nbsp;&nbsp; <a title=\"Questions\" "
          + "href=\"http://www.boguspublisher.org/questions\" target=\"_blank\">"
          + "BOGUSPUBLISHER Questions</a>&nbsp;&nbsp; "
          + "<a title=\"Terms &amp; Conditions\""
          + "href=\"http://www.boguspublisher.org/terms-conditions\""
          + "target=\"_blank\">BOGUSPUBLISHER Terms &amp; Conditions</a></span></span>"
          + "<br>"
          + "</span>"
          + "</div>"        
          + "</div>"
          + "</div>";

  private static final String withoutFooter =
      "<div class=\"block\"></div>";

  private static final String withHeaderMainMenu = 
      "<div id=\"header\">" +
      "<div class=\"mainMenu\">  <ul class=\"nav menu\"             <li class=\"\">"+
      "    <a href=\"/journals\">Journals</a>"+
      "</li>" +       
      "<li class=\"\">" +
      "    <a href=\"/ebooks\">E-Books</a>" +
      "</li>>"+
      "</div>" +
      "</div>Hello World";
  
  private static final String withoutHeaderMainMenu = "Hello World";
  
  private static final String withStylesheets = 
    "<link rel=\"stylesheet\" type=\"text/css\" href=\"/templates/jsp/_style2/_pagebuilder/reset-fonts-grids.css\">Hello World";
  private static final String withoutStylesheets ="Hello World";
  
  private static final String withComments =
    "<!--totalCount15--><!--modified:1374684679000-->Hello World";
  private static final String withoutComments ="Hello World";

  
  /*
   *  Compare Html and HtmlHashFiltered
   */
  
  public void testStackContentsHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withHeader), Constants.DEFAULT_ENCODING);
    assertEquals(withoutHeader, StringUtil.fromInputStream(actIn));
  }
        
  public void testFooterHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withFooter), Constants.DEFAULT_ENCODING);
    assertEquals(withoutFooter, StringUtil.fromInputStream(actIn));
  }
  
  public void testWithStyleSheets() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withStylesheets), Constants.DEFAULT_ENCODING);
    assertEquals(withoutStylesheets, StringUtil.fromInputStream(actIn));
  }
  
  public void testMenu() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withHeaderMainMenu), Constants.DEFAULT_ENCODING);
    assertEquals(withoutHeaderMainMenu, StringUtil.fromInputStream(actIn));
  }
  
  public void testComments() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withComments), Constants.DEFAULT_ENCODING);
    assertEquals(withoutComments, StringUtil.fromInputStream(actIn));
  }
  
  public void testScript() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withJScript), Constants.DEFAULT_ENCODING);
    assertEquals(withoutJScript, StringUtil.fromInputStream(actIn));
  }
  
  public void testStyleSheets() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withStylesheet), Constants.DEFAULT_ENCODING);
    assertEquals(withoutStylesheet, StringUtil.fromInputStream(actIn));
  }
  
  public void testAccessIcon() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withAccessIcon), Constants.DEFAULT_ENCODING);
    assertEquals(withoutAccessIcon, StringUtil.fromInputStream(actIn));
  }
}
