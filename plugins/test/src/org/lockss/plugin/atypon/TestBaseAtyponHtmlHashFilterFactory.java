/*  $Id$
 
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
          + "Copyright � 1996-2013, American So</span>"
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
  
  // the listing of articles on a TOC
  private static final String pdfPlusSize =
      "<div>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\">PDF Plus (584 KB)</a>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\">Pdf (584 KB)</a>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\">PDFplus(584 KB)</a>" +
      "</div>";
  private static final String pdfPlusSizeFiltered =
      "<div>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\"></a>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\"></a>" +
          "<a class=\"ref nowrap\" target=\"_blank\" title=\"Opens new window\" " +
          "href=\"/doi/pdfplus/10.2217/foo\"></a>" +
      "</div>";

  // the filesize with the links on an article page
  private static final String fileSize =
      "<div>" +
      "<a href=\"/doi/pdfplus/10.2217/foo\" target=\"_blank\">View PDF Plus " +
  "<span class=\"fileSize\">(584 KB)</span></a>" +
          "</div>";
  private static final String fileSizeFiltered =
      "<div>" +
      "<a href=\"/doi/pdfplus/10.2217/foo\" target=\"_blank\">View PDF Plus " +
  "</a>" +
          "</div>";
  
  private static final String cys_fileSize =
    //CYS pdf with sizing
      "<ul class=\"icon-list-vertical box-gray-border box-pad clear\">" +
      "<li>" +
      "<a href=\"/doi/abs/10.13034/cysj-2013-005\" title=\"View the Abstract\" class=\"icon-abstract\">" +
      "<span>Abstract</span>" +
      "</a>" +
      "</li>" +
      "<li>" +
      "<a href=\"/doi/pdf/10.13034/cysj-2013-005\" class=\"icon-pdf\">" +
      "               PDF (318 K)" +
      "            " +
      "</a>" +
      "</li>" +
      "<li>" +
      "<a href=\"/doi/pdfplus/10.13034/cysj-2013-005\" class=\"icon-pdf-plus\">" +
      "               PDF-Plus (319 K)" +
      "            " +
      "</a>" +
      "</li>" +
      "<li>" +
      "<a href=\"javascript:void(0);\" id=\"figures\" class=\"icon-figures\">Figures</a></li>" +
      "<li>" +
      "<a class=\"icon-tables\" href=\"javascript:void(0);\" id=\"tables\">Tables</a>" +
      "</li>" +
      "</ul>";  
  private static final String cys_fileSize_filtered =
      //CYS pdf with sizing
        "<ul class=\"icon-list-vertical box-gray-border box-pad clear\">" +
        "<li>" +
        "<a href=\"/doi/abs/10.13034/cysj-2013-005\" title=\"View the Abstract\" class=\"icon-abstract\">" +
        "<span>Abstract</span>" +
        "</a>" +
        "</li>" +
        "<li>" +
        "<a href=\"/doi/pdf/10.13034/cysj-2013-005\" class=\"icon-pdf\">" +
        "</a>" +
        "</li>" +
        "<li>" +
        "<a href=\"/doi/pdfplus/10.13034/cysj-2013-005\" class=\"icon-pdf-plus\">" +
        "</a>" +
        "</li>" +
        "<li>" +
        "<a href=\"javascript:void(0);\" id=\"figures\" class=\"icon-figures\">Figures</a></li>" +
        "<li>" +
        "<a class=\"icon-tables\" href=\"javascript:void(0);\" id=\"tables\">Tables</a>" +
        "</li>" +
        "</ul>";        


  
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
  
  public void test_pdfPlusSize() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(pdfPlusSize),
        Constants.DEFAULT_ENCODING);
    assertEquals(pdfPlusSizeFiltered, StringUtil.fromInputStream(actIn));
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(fileSize),
        Constants.DEFAULT_ENCODING);
    assertEquals(fileSizeFiltered, StringUtil.fromInputStream(actIn));    
    actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(cys_fileSize),
        Constants.DEFAULT_ENCODING);
    assertEquals(cys_fileSize_filtered, StringUtil.fromInputStream(actIn));    

  }
  
  /* This section tests variants that the child plugins can turn on or not */
  private static final String wsVariant = 
      "<html>" +
      "<body><h2>  This is a title      lots of spaces   </h2>" +
      "<div>   foo</div>" +
      "</body>  </html>";
  private static final String wsVariant_withfilter = 
  "<html>" +
  "<body><h2> This is a title lots of spaces </h2>" +
  "<div> foo</div>" +
  "</body> </html>";

  private static final String tagIDVariant = 
      "<div id=\"blah\">" +
          "<div id=\"123\" class=\"mainMenu\">" +
          "  <ul class=\"xxx menu\">" +
          "             <li class=\"\" id=\"hoho\">"+
          "    <a href=\"/journals\">Journals</a>"+
          "</li>" +       
          "<li class=\"\">" +
          "    <a href=\"/ebooks\">E-Books</a>" +
          "</li></ul>"+
          "<span id=\"gensym_here\">" +
          " inside the span" +
          "</span>" +
          "</div>" +
          "</div>Hello World";
  private static final String tagIDVariant_defaultfilter = 
      "<div id=\"blah\">" +
          "<div id=\"123\" class=\"mainMenu\">" +
          " <ul class=\"xxx menu\">" +
          " <li class=\"\" id=\"hoho\">"+
          " <a href=\"/journals\">Journals</a>"+
          "</li>" +       
          "<li class=\"\">" +
          " <a href=\"/ebooks\">E-Books</a>" +
          "</li></ul>"+
          "<span >" +
          " inside the span" +
          "</span>" +
          "</div>" +
          "</div>Hello World";

  private static final String tagIDVariant_allfilter = 
      "<div >" +
          "<div class=\"mainMenu\">" +
          " <ul class=\"xxx menu\">" +
          " <li class=\"\" >"+
          " <a href=\"/journals\">Journals</a>"+
          "</li>" +       
          "<li class=\"\">" +
          " <a href=\"/ebooks\">E-Books</a>" +
          "</li></ul>"+
          "<span >" +
          " inside the span" +
          "</span>" +
          "</div>" +
          "</div>Hello World";
      
  
  
  public void test_WSVariations() throws Exception {
    TestRigHashFilterFactory rigFact = new TestRigHashFilterFactory();

    InputStream actIn;
    //1. test WS - first with no ws filtering; default settings
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(wsVariant), Constants.DEFAULT_ENCODING);
    assertEquals(wsVariant, StringUtil.fromInputStream(actIn));
    // 2. test WS - with ws filtering
    rigFact.setWSFiltering(true);
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(wsVariant), Constants.DEFAULT_ENCODING);
    assertEquals(wsVariant_withfilter, StringUtil.fromInputStream(actIn));
    
    // 3. leave WS on, now test tag ids - default behavior
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(tagIDVariant), Constants.DEFAULT_ENCODING);
    assertEquals(tagIDVariant_defaultfilter, StringUtil.fromInputStream(actIn));

    // 4. leave WS on, test tag ids - strips all tag ids
    rigFact.setTagFiltering(true);
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(tagIDVariant), Constants.DEFAULT_ENCODING);
    assertEquals(tagIDVariant_allfilter, StringUtil.fromInputStream(actIn));
  }
  
  /*
   * PRIVATE class created just for testing - create a child hash filter that can 
   * test variations in types of filtering
   */
  private class TestRigHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
    private boolean do_ws_filtering = false;
    private boolean do_all_tag_id_filtering = false;
    
    public void setWSFiltering(boolean ws) {
      this.do_ws_filtering = ws;
    }

    public void setTagFiltering(boolean tag) {
      this.do_all_tag_id_filtering = tag;
    }


    @Override
    public boolean doWSFiltering() {
      return this.do_ws_filtering;
    }

    @Override
    public boolean doTagIDFiltering() {
      return this.do_all_tag_id_filtering;
    }

  }

}
