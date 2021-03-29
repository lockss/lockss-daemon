/*  $Id$
 
 Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,

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
  
  // this line within a script tag section messed up the parser
  // as it couldn't handle the unpaired quote, double-quote ordering
  // jtitle = jtitle.replace(/([\'])/g,"\\'");
  // now we strip out all "<script" to "<script>" before parsing
  private static final String beastScript = 
      "<div class=\"one\">" +
          "<script>" +
          "           jtitle = jtitle.replace(/([\\'])/g,\"\\\\'\");" +
          "</script>" +
          "<div class=\"two\"  >FOO</div>" +
          "</div>";

  private static final String beastScriptFiltered = 
      "<div class=\"one\">" +
          "<div class=\"two\"  >FOO</div>" +
          "</div>";

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
  
  private static final String withMetrics = 
      "<h3>Metrics</h3>\n" + 
      "  <div data-widget-def=\"literatumContentItemDownloadCount\" data-widget-id=\"id\" class=\"article-downloads\">\n" + 
      "                Downloaded 15 times\n" + 
      "  </div>\n" + 
      "  <div id=\"doi_altmetric_drawer_area\">\n" + 
      "    <div data-badge-details=\"right\" data-badge-type=\"donut\" data-doi=\"10.1152/jn.00002.2017\" data-hide-no-mentions=\"true\" class=\"altmetric-embed\">\n" + 
      "    </div>\n" + 
      "  </div>";
  private static final String withoutMetrics =
        "<h3>Metrics</h3>\n" +
        "  \n" +
        "  ";

  private static final String withTableReferences = 
        "Hello<table border=\"0\" class=\"references\"> " +
        "<tr>" +
        "<td>Hello</td>" +
        "<td>Kitty</td>" +
        "<\tr>" +
        "<tr>" +
        "<td>Hello</td>" +
        "<td>World</td>" +
        "<\tr>" +
        "</table>World" ;
  private static final String withoutTableReferences = 
        "HelloWorld" ;

  private static final String withRelatedContent = 
        "Hello<div class=\"tab tab-pane\" id=\"relatedContent\">" +
        "</div>World" ;
  private static final String withoutRelatedContent = 
        "HelloWorld" ;


  protected static final String protectedEmailSpan =
        "<a href=\"dgern@thoracic.org\">" +
        "<span class=\"__cf_email__\" data-cfemail=\"34505351465a74405c5b4655575d571a5b4653\">[email&nbsp;protected] </span>" +
        "</a>";

  protected static final String filteredProtectedEmailSpan =
        "<a href=\"dgern@thoracic.org\">" +
        "</a>";

  protected static final String protectedEmailASpan =
      "<div class=\"NLM_corresp\">" +
      "Correspondence and requests for reprints should be addressed to Peter Classi, M.Sc., M.B.A., " +
      "United Therapeutics Corporation, 55 TW Alexander Drive, Research Triangle Park, NC 27709. E-mail: " +
      "<a class=\"email\" href=\"/cdn-cgi/l/email-protection#1060737c7163637950657e79647875623e737f7d\">" +
      "<span class=\"__cf_email__\" data-cfemail=\"3c4c5f505d4f4f557c4952554854594e125f5351\">[email&nbsp;protected]</span>" +
      "</a>." +
      "</div>";

  protected static final String filteredProtectedEmailASpan =
      "<div class=\"NLM_corresp\">" +
      "Correspondence and requests for reprints should be addressed to Peter Classi, M.Sc., M.B.A., " +
      "United Therapeutics Corporation, 55 TW Alexander Drive, Research Triangle Park, NC 27709. E-mail: " +
      "." +
      "</div>";

  protected static final String pVidSource =
      "<p>" +
      "<span class=\"video-source\" style=\"display:none\">https://thoracic-prod-streaming.literatumonline.com/journals/content/annalsats/2019/annalsats.2019.16.issue-12/annalsats.201905-414cc/20191115/media/annalsats.201905-414cc_vid1.,964,750,300,180,.mp4.m3u8?b92b4ad1b4f274c70877528314abb28bd3f723a7d6082e6507476c036d1b3402e209f95f47cb691aca526557783e82bc64ff0999d3d535157ece591a7960e52d0ad6ff2e906196e220cb93f961768e02064b91a1ad9c7348821c98f7acc9bd5e389723630f66ab576db0f419f0c939f58d827bfa2eac7787d4b56d13de187b3827fc74a9d5fbda90a8b17c06c05d2720b3f7c0d3e1346cc83905b6bb1906c3b9d888e9193497328183834474e8c05f9b2eee691ed114090d8fb9bb9bea87d9b35ba05edca8b3b902 </span>" +
      "</p>";

  protected static final String filteredPVidSource =
      "<p>" +
      "</p>";

  protected static final String loginForm =
      "<table class=\"loginForm\" summary=\"\">" +
        "<tbody>" +
          "<tr>" +
            "<td>" +
              "<form action=\"/action/doLogin\" name=\"frmLogin\" method=\"post\">" +
                "<input type=\"hidden\" name=\"redirectUri\" value=\"/doi/full/10.1513/AnnalsATS.201810-723CC\">" +
                "<input type=\"hidden\" name=\"loginUri\" value=\"/doi/full/10.1513/AnnalsATS.201810-723CC\">" +
                "<p> </p>" +
                "<table class=\"loginForm marginTobVandr\" summary=\"\">" +
                  "<tbody>" +
                    "<tr>" +
                      "<th>" +
                        "<label for=\"login\">Username: </label>" +
                      "</th>" +
                      "<td>" +
                        "<input class=\"textInput\" style=\"width:150px\" type=\"text\" name=\"login\" value=\"\" size=\"15\">" +
                      "</td>" +
                    "</tr>" +
                    "<tr>" +
                      "<th>" +
                        "<label for=\"password\">Password: </label>" +
                      "</th>" +
                      "<td>" +
                        "<input class=\"textInput\" style=\"width:150px\" type=\"password\" name=\"password\" value=\"\" autocomplete=\"off\">" +
                      "</td>" +
                    "</tr>" +
                    "<tr>" +
                      "<td colspan=\"2\">" +
                        "<input type=\"checkbox\" name=\"savePassword\" value=\"1\">" +
                        "<label for=\"savePassword\">Remember me </label>" +
                      "</td>" +
                    "</tr>" +
                  "</tbody>" +
                "</table>" +
                "<input type=\"submit\" name=\"submit\" value=\"Sign In\" class=\"formbutton\">" +
                "<input type=\"reset\" name=\"clear\" value=\"Clear\" class=\"formbutton\">" +
                "<br>" +
                "<br>" +
                "<div>" +
                  "<a href=\"/action/requestResetPassword\">Forgotten your password? </a>" +
                "</div>" +
              "</form>" +
            "</td>" +
          "</tr>" +
          "<tr>" +
            "<td>" +
              "<h1>New User Registration </h1>" +
              "<b>Not Yet Registered? </b>" +
              "<br>" +
              "<i>Benefits of Registration Include: </i>" +
              "<br>" +
              "<table summary=\"\">" +
                "<tbody>" +
                  "<tr valign=\"top\">" +
                    "<td> • </td>" +
                    "<td>A Unique User Profile that will allow you to manage your current subscriptions (including online access) </td>" +
                  "</tr>" +
                  "<tr valign=\"top\">" +
                    "<td> • </td>" +
                    "<td>The ability to create favorites lists down to the article level </td>" +
                  "</tr> " +
                  "<tr valign=\"top\">" +
                    "<td> • </td>" +
                    "<td>The ability to customize email alerts to receive specific notifications about the topics you care most about and special offers </td>" +
                  "</tr>" +
                "</tbody>" +
              "</table>" +
              "<div align=\"right\">" +
                "<form action=\"https://thoracic.secure.force.com/MyAccount?fp=cn&amp;tr=atsjournals&amp;ar=https%3A%2F%2Fwww.atsjournals.org%2Faction%2Fssostart%3Fidp%3Dhttps%253A%252F%252Flogin.thoracic.org%252Fidp%252Fshibboleth%26redirectUri%3Dnull\" method=\"post\">" +
                  "<input class=\"formbutton\" type=\"submit\" value=\"Register\">" +
                  "<input type=\"hidden\" name=\"redirectUri\" value=\"/doi/full/10.1513/AnnalsATS.201810-723CC\">" +
                "</form>" +
              "</div>" +
            "</td>" +
          "</tr>" +
        "</tbody>" +
      "</table>";

  private static final String filteredLoginForm =
      "";

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
  
  public void testMetrics() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withMetrics), Constants.DEFAULT_ENCODING);
    assertEquals(withoutMetrics, StringUtil.fromInputStream(actIn));
  }

  public void testTableRef() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withTableReferences), Constants.DEFAULT_ENCODING);
    assertEquals(withoutTableReferences, StringUtil.fromInputStream(actIn));
  }
  
  public void testRelatedContent() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withRelatedContent), Constants.DEFAULT_ENCODING);
    assertEquals(withoutRelatedContent, StringUtil.fromInputStream(actIn));
  }
  
  public void testAccessIcon() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withAccessIcon), Constants.DEFAULT_ENCODING);
    assertEquals(withoutAccessIcon, StringUtil.fromInputStream(actIn));
  }

  public void testProtectedEmail() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(protectedEmailSpan), Constants.DEFAULT_ENCODING);
    assertEquals(filteredProtectedEmailSpan, StringUtil.fromInputStream(actIn));
  }

  public void testProtectedEmail2() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(protectedEmailASpan), Constants.DEFAULT_ENCODING);
    assertEquals(filteredProtectedEmailASpan, StringUtil.fromInputStream(actIn));
  }

  public void testPVidSource() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(pVidSource), Constants.DEFAULT_ENCODING);
    assertEquals(filteredPVidSource, StringUtil.fromInputStream(actIn));
  }

  public void testLoginForm() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(loginForm), Constants.DEFAULT_ENCODING);
    assertEquals(filteredLoginForm, StringUtil.fromInputStream(actIn));
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
  
  public void testBeastScript() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(beastScript), Constants.DEFAULT_ENCODING);
    assertEquals(beastScriptFiltered, StringUtil.fromInputStream(actIn));
  }
  
  private static final String htmlTagWithId = 
      "<!DOCTYPE html>" +
       "<html lang=\"en\" class=\"pb-page\"  data-request-id=\"6e56e369-793f-4108-8bb2-813336b037c1\">" +
       "<head>boo</head><body>FOO</body></html>";
  
  private static final String htmlTagWithIdFiltered = 
      "<!DOCTYPE html>" +
          "<html lang=\"en\" class=\"pb-page\"  >" +
          "<body>FOO</body></html>";
     
  
  public void testHtmlAttribute() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(htmlTagWithId), Constants.DEFAULT_ENCODING);
    assertEquals(htmlTagWithIdFiltered, StringUtil.fromInputStream(actIn));
  }
  
  /* This section tests variants that the child plugins can turn on or not */
  private static final String wsVariant = 
      "<html>" +
      "<body><h2>  This is a title      lots of spaces   </h2>" +
      "<div>   foo</div>" +
      "</body>  </html>";
  /* WS filtering does three things:
   * 1. turn &nbsp; to ascii space
   * 2. add space before any "<"
   * 3. consolidates spaces down to 1 space
   */
  private static final String wsVariant_withfilter = 
  " <html>" +
  " <body> <h2> This is a title lots of spaces </h2>" +
  " <div> foo </div>" +
  " </body> </html>";

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
  private static final String tagIDVariant_defaultfilter_withWSFilter = 
      " <div id=\"blah\">" +
          " <div id=\"123\" class=\"mainMenu\">" +
          " <ul class=\"xxx menu\">" +
          " <li class=\"\" id=\"hoho\">"+
          " <a href=\"/journals\">Journals </a>"+
          " </li>" +       
          " <li class=\"\">" +
          " <a href=\"/ebooks\">E-Books </a>" +
          " </li> </ul>"+
          " <span >" +
          " inside the span" +
          " </span>" +
          " </div>" +
          " </div>Hello World";

  private static final String tagIDVariant_allfilter = 
      " <div >" +
          " <div class=\"mainMenu\">" +
          " <ul class=\"xxx menu\">" +
          " <li class=\"\" >"+
          " <a href=\"/journals\">Journals </a>"+
          " </li>" +       
          " <li class=\"\">" +
          " <a href=\"/ebooks\">E-Books </a>" +
          " </li> </ul>"+
          " <span >" +
          " inside the span" +
          " </span>" +
          " </div>" +
          " </div>Hello World";
  
  private static final String wsVariant_withWSFilter_withoutTags = 
  " This is a title lots of spaces foo ";
  private static final String wsVariant_noWSFilter_withoutTags = 
      "  This is a title      lots of spaces   " +
      "   foo" +
      "  ";
  
  private static final String tagIDVariant_defaultfilter_withoutTags = 
          "  " +
          "             "+
          "    Journals"+
          "    E-Books" +
          " inside the span" +
          "Hello World";
  
  private static final String tagIDVariant_allFilters = 
      " Journals"+
      " E-Books" +
      " inside the span" +
      " Hello World";
  
  
  private static final String wsNbspVariant = 
      "<div class=\"tocContent\">" +
      "<div class=\"art_title noLink\"> &nbsp;<span class=\"hlFld-Title\">" +
      "<i>In Memoriam,</i> Dr. E. Cheesey, 1942 2014</span></div>" +
      "</div>";
  
  private static final String wsNbspVariant_withFilter = 
      " <div class=\"tocContent\">" +
      " <div class=\"art_title noLink\"> <span class=\"hlFld-Title\">" +
      " <i>In Memoriam, </i> Dr. E. Cheesey, 1942 2014 </span> </div>" +
      " </div>";
  
  public void test_Variations() throws Exception {
    TestRigHashFilterFactory rigFact = new TestRigHashFilterFactory();

    InputStream actIn;
    //1. test WS - first with no ws filtering; default settings
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(wsVariant), Constants.DEFAULT_ENCODING);
    assertEquals(wsVariant, StringUtil.fromInputStream(actIn));
    //1b. Test space and &nbsp; variant; still just default settings
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(wsNbspVariant), Constants.DEFAULT_ENCODING);
    assertEquals(wsNbspVariant, StringUtil.fromInputStream(actIn));
    
    // 2. test WS - with ws filtering (also adds space before "<")
    rigFact.setWSFiltering(true);
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(wsVariant), Constants.DEFAULT_ENCODING);
    assertEquals(wsVariant_withfilter, StringUtil.fromInputStream(actIn));
    //2b. Test space and &nbsp; variant;
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(wsNbspVariant), Constants.DEFAULT_ENCODING);
    assertEquals(wsNbspVariant_withFilter, StringUtil.fromInputStream(actIn));
    
    // 3. leave WS on, now test tag ids - default behavior
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(tagIDVariant), Constants.DEFAULT_ENCODING);
    assertEquals(tagIDVariant_defaultfilter_withWSFilter, StringUtil.fromInputStream(actIn));

    // 4. leave WS on, test tag ids - strips all tag ids
    rigFact.setTagFiltering(true);
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(tagIDVariant), Constants.DEFAULT_ENCODING);
    assertEquals(tagIDVariant_allfilter, StringUtil.fromInputStream(actIn));
    
    // 5. WS off, but remove all tags
    rigFact.setTagFiltering(false); // back to default
    rigFact.setWSFiltering(false); // back to default
    rigFact.setRemoveTagsFiltering(true);
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(wsVariant), Constants.DEFAULT_ENCODING);
    assertEquals(wsVariant_noWSFilter_withoutTags, StringUtil.fromInputStream(actIn));
    
    // 6. WS on, AND remov all all tags
    rigFact.setWSFiltering(true);
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(wsVariant), Constants.DEFAULT_ENCODING);
    assertEquals(wsVariant_withWSFilter_withoutTags, StringUtil.fromInputStream(actIn));
    
    // 7. WS filter off, default tagID, remove all tags
    rigFact.setWSFiltering(false);
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(tagIDVariant), Constants.DEFAULT_ENCODING);
    assertEquals(tagIDVariant_defaultfilter_withoutTags, StringUtil.fromInputStream(actIn));
    
    // 8. WS filter on, tagID filter on (will do nothing), remove all tags
    rigFact.setWSFiltering(true);
    rigFact.setTagFiltering(true);
    actIn = rigFact.createFilteredInputStream(mau,
        new StringInputStream(tagIDVariant), Constants.DEFAULT_ENCODING);
    assertEquals(tagIDVariant_allFilters, StringUtil.fromInputStream(actIn));
    
  }
  
  /*
   * PRIVATE class created just for testing - create a child hash filter that can 
   * test variations in types of filtering
   */
  private class TestRigHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {
    private boolean do_ws_filtering = false;
    private boolean do_all_tag_id_filtering = false;
    private boolean do_remove_tags_filtering = false;
    
    public void setWSFiltering(boolean ws) {
      this.do_ws_filtering = ws;
    }

    public void setTagFiltering(boolean tag) {
      this.do_all_tag_id_filtering = tag;
    }

    public void setRemoveTagsFiltering(boolean rem) {
      this.do_remove_tags_filtering = rem;
    }


    @Override
    public boolean doWSFiltering() {
      return this.do_ws_filtering;
    }

    @Override
    public boolean doTagIDFiltering() {
      return this.do_all_tag_id_filtering;
    }
    
    @Override
    public boolean doTagRemovalFiltering() {
      return this.do_remove_tags_filtering;
    }

  }

}
