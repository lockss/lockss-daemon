/*
 * $Id: TestMassachusettsMedicalSocietyHtmlHashFilterFactory.java,v 1.4 2015/01/14 23:07:10 aishizaki Exp $
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

import org.htmlparser.filters.TagNameFilter;
import org.lockss.util.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.test.*;

public class TestMassachusettsMedicalSocietyHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private MassachusettsMedicalSocietyHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MassachusettsMedicalSocietyHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  String test1[] = {
                  "<html>     <div id = \"related\">abc</div></html>",
                  "<html>  <div id = \"trendsBox\">abc</div></html>",
              "<html>   <div id = \"topAdBar\">abc</div></html>",
              "<html>  <div class = \"Topbanner CM8\">abc</div></html>",
              "<html>   <div class = \"audioTitle\">abc</div></html>",
              "<html>  <div class = \"topLeftAniv\">abc</div></html>",
              "<html>          <div class = \"ad\">abc</div></html>",
              "<html> <div id = \"rightRailAd\">abc</div></html>",
              "<html>  <div class = \"rightAd\">abc</div></html>",
              "<html><div id = \"rightAd\">abc</div>  </html>",
              "<html><div class = \"toolsAd\">abc</div>                </html>",
              "<html><div id = \"bottomAdBar\">abc</div>       </html>",
              "<html> <div class = \"bottomAd\">abc</div> </html>",
              "<html>  <div class = \"bannerAdTower\">abc</div>        </html>",
              "<html>  <dd id = \"comments\">abc</dd>  </html>",
              "<html> <dd id = \"letters\">abc</dd>    </html>",
              "<html>  <dd id = \"citedby\">abc</dd> </html>",
              "<html> <div class = \"articleCorrection\">abc</div>     </html>",
  };
  String test2[] = {
              "<html><div id = \"galleryContent\">abc</div></html>",
              "<html><div class = \"discussion\">abc</div></html>",
              "<html><dt id = \"citedbyTab\">abc</dt></html>",
              "<html><div class = \"articleActivity\">abc</div></html>",
              "<html><div id = \"institutionBox\">abc</div></html>",
              "<html><div id = \"copyright\">abc</div></html>",
              "<html><a class = \"issueArchive-recentIssue\">abc</a></html>",
              "<html><div id = \"moreIn\">abc</div></html>"               
  };
  
  String test3[] = {
  " <html><div id = \"layerPlayer\">abc</div></html>    ",
  "  <html><li class = \"submitLetter\">abc</div></html>        ",
  "     <html><p class = \"openUntilInfo\">abc</div></html>  ",
  "     <html><div class = \"poll\">abc</div></html>            ",
  "             <html><meta name=\"evt-ageContent\" content=\"Last-6-Months\" /></html> ",
  "  <html><div class=\"emailAlert\"></div></html>  ",
  " <html><div class=\"&#xA; jcarousel-skin-audio carousel-type-audiosummary\"><h3>More Weekly Audio Summaries (8)</h3></html> ",
  "     <html><div id=\"toolsBox\"><h3> Tools </h3></div></html>    ",
  " <html><a href=\"/servlet/linkout?suffix=r001&amp;dbid=16384&amp;url=http%3A%2F%2Fsfx.stanford.edu%2Flocal%3Fsid%3Dmms%26genre%3D%26aulast%3DSampson%26aufirst%3DHA%26aulast%3DMunoz-Furlong%26aufirst%3DA%26aulast%3DCampbell%26aufirst%3DRL%26atitle%3DSecond%2Bsymposium%2Bon%2Bthe%2Bdefinition%2Band%2Bmanagement%2Bof%2Banaphylaxis%253A%2Bsummary%2Breport%2B%252D%252D%2BSecond%2BNational%2BInstitute%2Bof%2BAllergy%2Band%2BInfectious%2BDisease%252FFood%2BAllergy%2Band%2BAnaphylaxis%2BNetwork%2BSymposium.%26stitle%3DJ%2BAllergy%2BClin%2BImmunol%26date%3D2006%26volume%3D117%26spage%3D391%26id%3Ddoi%3A10.1016%252Fj.jaci.2005.12.1303\" title=\"OpenURL STANFORD UNIVERSITY\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL STANFORD UNIVERSITY\"></a></html>  "
  };

  String test4[] = {"<html>"+
                   "<div align=\"center\" style=\"\">"+
                   "<!-- placeholder id=null, description=N-ad-global-top -->"+
                   "<div align=\"center\" style=\"background:#fffcd2;border:1px solid #E6DB55;padding:5"+
                   "px 12px 0;border-radius:3px 3px 3px 3px;width:800px;height:40px;margin:0 auto 15"+
                   "px;\">"+
                   "<p style=\"font:normal 11px arial,sans-serif;line-height:15px;color:#333\">"+
                   "<span style=\"font-weight:bold;color:#f30\">"+
                   "ATTENTION:</span>"+
                   "On <span style=\"font-weight:bold\">"+
                   "Friday, February 20th from 6pm &ndash; 10pm ET,</span>"+
                   "NEJM.org may be temporarily unavailable due to scheduled site maintenance. <br>"+
                   "We apologize for the inconvenience.</p>"+
                   "</div>"+
                   "</div>"+
                   "</html>"};
   
   
  public void testFiltering() throws Exception {
    InputStream in;
    
    for (String t : test1){
        in = fact.createFilteredInputStream(mau, new StringInputStream(t),
                ENC);
        String str = StringUtil.fromInputStream(in);
        assertEquals("<html> </html>", str);
    }
    for (String t : test2){
      in = fact.createFilteredInputStream(mau, new StringInputStream(t),
              ENC);
      String str = StringUtil.fromInputStream(in);
      assertEquals("<html></html>", str);
    }
    for (String t : test3){
      in = fact.createFilteredInputStream(mau, new StringInputStream(t),
              ENC);
      String str = StringUtil.fromInputStream(in);
      assertEquals(" <html></html> ", str);
    }
    for (String t : test4){
      in = fact.createFilteredInputStream(mau, new StringInputStream(t),
              ENC);
      String str = StringUtil.fromInputStream(in);
      assertEquals("<html><div align=\"center\" style=\"\"></div></html>", str);
    }
  }
    
    //Example instances mostly from pages of strings of HTMl that should be filtered
    //and the expected post filter HTML strings.
    private static final String modifiedHtmlHash = 
                    "<!--modified:1327018773000--><div class=\"tocContent\"><!--This is for full browser--><!--totalCount20--></div>";
    private static final String modifiedHtmlHashFiltered = 
                    "<div class=\"tocContent\"></div>";
    
    private static final String citedByHtmlHash = 
                    "<dl class=\"articleTabs tabPanel lastChild\">\n" +
                          "<dt id=\"abstractTab\" class=\"active abstract firstChild sideBySide\">Abstract</dt>\n" +
                          "<dt id=\"citedbyTab\" class=\"citedby sideBySide inactive\">Citing Articles<span>(216) </span></dt>\n" +
                    "</dl>";
    private static final String citedByHtmlHashFiltered = 
                    "<dl class=\"articleTabs tabPanel lastChild\"> " +
                          "<dt id=\"abstractTab\" class=\"active abstract firstChild sideBySide\">Abstract</dt> " +
                    "</dl>";
    
    private static final String articleActivityHtmlHash = 
                    "<div class=\"articleMedia\"><h3 class=\"title\">Media in This Article</h3></div>\n" +
                    "<div class=\"articleActivity\"><h3 class=\"title\">Article Activity</h3>\n" +
                          "<p><a class=\"articleActivity-citedby more\" href=\"#citedby\">216 articles have cited this article</a></p>\n" +
                    "</div>";
    private static final String articleActivityHtmlHashFiltered = 
                    "<div class=\"articleMedia\"><h3 class=\"title\">Media in This Article</h3></div> ";
    
    private static final String institutionHtmlHash = 
                    "<p>Access Provided By:<br><div id=\"institutionBox\">LANE MEDICAL LIBRARY</div></p>";
    private static final String institutionHtmlHashFiltered = 
                    "<p>Access Provided By:<br></p>";
    
    private static final String commentsOpenHtmlHash = 
                    "<p class=\"citationLine\"><a href=\"/toc/nejm/366/10/\">March 8, 2012</a></p>\n" +
                    "<p class=\"openUntilInfo\">\n" +
                          "<img src=\"/templates/jsp/_style2/_mms/_nejm/img/icon_comment.gif\">\n" +
                          "<a class=\"scrollDirectly\" name=\"discussion\" href=\"#discussion\">Comments</a> open through March 14, 2012\n" +
                    "</p>";
    private static final String commentsOpenHtmlHashFiltered = 
                    "<p class=\"citationLine\"><a href=\"/toc/nejm/366/10/\">March 8, 2012</a></p> ";
    
    private static final String copyrightHtmlHash = 
                    "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=ON&amp;amp; </noscript>\n" +
                    "<div id=\"copyright\"><p></p</div>Hello";
    private static final String copyrightHtmlHashFiltered = 
                    "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=ON&amp;amp; </noscript> " +
                    "Hello";
    
    private static final String javascriptHtmlHash = 
                    "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=ON&amp;amp; </noscript>\n" +
                    "<script type=\"text/javascript\" src=\"http://nejm.resultspage.com/autosuggest/searchbox_suggest_v1.js\" language=\"javascript\">Hello</script>";
    private static final String javascriptHtmlHashFiltered = 
                    "<noscript> onversion/1070139620/?label=_s1RCLTo-QEQ5JGk_gM&amp;amp;guid=ON&amp;amp; </noscript> ";
    
    private static final String recentIssueHtmlHash =
                    "<div class=\"issueArchive-recentIssue\"><a class=\"issueArchive-recentIssue\">topics</a></div>\n" +
                    "<a id=\"issueArchive-recentIssue\">Hello</div>";
    private static final String recentIssueHtmlHashFiltered =
                    "<div class=\"issueArchive-recentIssue\"></div> " +
                    "<a id=\"issueArchive-recentIssue\">Hello</div>";
    
    private static final String moreInHtmlHash =
                    "<div id=\"topics\">topics</div>\n" +
                    "<div id=\"moreIn\"><h3>More In</h3><ul><li class=\"firstChild\"></li></ul></div>";
    private static final String moreInHtmlHashFiltered =
                    "<div id=\"topics\">topics</div> ";
    
    private static final String layerPlayerHtmlHash =
                    "<div id=\"\" style=\"width:830px; height:600px\">\n" +
                          "<div id=\"layerPlayer_d3000e2652\"><div class=\"noFlashImgContainer\">image</div></div>\n" +
                    "</div>";
    private static final String layerPlayerHtmlHashFiltered =
                    "<div id=\"\" style=\"width:830px; height:600px\"> " +
                    "</div>";
    
    private static final String pollHtmlHash =
                    "<div class=\"notpoll\"><div class=\"poll\">poll</div></div>\n" +
                    "<div id=\"poll\"></div>";
    private static final String pollHtmlHashFiltered =
                    "<div class=\"notpoll\"></div> " +
                    "<div id=\"poll\"></div>";
    
    private static final String submitLetterHtmlHash =
                    "<div class=\"submitLetter\"><li class=\"submitLetter\">topics</li></div>\n" +
                    "<li id=\"submitLetter\">Hello</li>";
    private static final String submitLetterHtmlHashFiltered =
                    "<div class=\"submitLetter\"></div> " +
                    "<li id=\"submitLetter\">Hello</li>";
  
    public void testModifiedHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(modifiedHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(modifiedHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testCitedByHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(citedByHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(citedByHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testInstitutionHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(institutionHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(institutionHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testCommentsOpenHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(commentsOpenHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(commentsOpenHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testCopyrightHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(copyrightHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(copyrightHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testJavascriptHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau, 
          new StringInputStream(javascriptHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(javascriptHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testMoreInHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau, 
          new StringInputStream(moreInHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(moreInHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testLayerPlayerHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau, 
          new StringInputStream(layerPlayerHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(layerPlayerHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testRecentIssueHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(recentIssueHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(recentIssueHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testArticleActivityHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau, 
          new StringInputStream(articleActivityHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(articleActivityHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testPollHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(pollHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(pollHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

    public void testSubmitLetterHtmlHashFiltering() throws Exception {
      InputStream actIn = fact.createFilteredInputStream(mau, 
          new StringInputStream(submitLetterHtmlHash),
          Constants.DEFAULT_ENCODING);

      assertEquals(submitLetterHtmlHashFiltered, StringUtil.fromInputStream(actIn));
    }

}