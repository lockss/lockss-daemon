/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.peerj;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestPeerJHtmlHashFilterFactory extends LockssTestCase {
  private PeerJHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new PeerJHtmlHashFilterFactory();
  }

  private static final String withHead = 
      "<html>"
          + "<head>"
          + "<meta charset=\"utf-8\">"
          + "<title>The Article Title</title>"
          + "<meta name=\"citation_title\" content=\"Cloning\">"
          + "</head>"
          + "</html>";
  private static final String withoutHead = "<html></html>";
  
 private static final String withScript =
     "<div class=\"block\">"
         + "<script>"
         + "$(function() {"
         + "$(\"[rel=tooltip]\").tooltip({ placement: \"bottom\"});"
         + "$(\"[rel=popover]\").popover({ trigger: \"hover\"});"
         + "});"
         + "</script>"
         + "</div>";
  private static final String withoutScript = "<div class=\"block\"></div>";
  
  private static final String withNoscript = 
      "<div class=\"block\">"
        + "<noscript>"
        + "<div class=\"alert alert-warning\"></div>"
        + "</noscript>"
        + "</div>";
  private static final String withoutNoscript = "<div class=\"block\"></div>";
  
  private static final String withComments =
      "<div class=\"block\">"
        + "<!-- empty element at the foot of the page -->"
        + "</div>";
  private static final String withoutComments = "<div class=\"block\"></div>";

  private static final String withTopNavbarInverse =
      "<div class=\"block\">"
        + "<div class=\"navbar navbar-fixed-top navbar-inverse\">"
	+ "<div class=\"navbar-inner\">"
        + "<div class=\"container\">"
        + "<li class=\"divider\"></li>"
        + "</div>"
        + "</div>"
        + "</div>"
        + "</div>";
  private static final String withoutTopNavbarInverse = 
      "<div class=\"block\"></div>";
  
  private static final String withTopNavbarInner =
      "<div class=\"block\">"
        + "<div class=\"item-top-navbar-inner\">"
        + "<div class=\"container\">"
        + "<a href=\"/\" class=\"brand item-top-navbar-brand\""
        + "<span class=\"item-top-navbar-social\">"
        + "<a class=\"pj-socialism tw-soc\""
        + "href=\"http://twitter.com/share?url=https://xxxxx.com/articles/46/"
        + "&amp;via=thexxxx&amp;text=yyyyyyy\"</a>"
        + "</span>"
        + "</div></div>"
        + "</div>";
  private static final String withoutTopNavbarInner = 
      "<div class=\"block\"></div>";
  
  private static final String withAlertWarning =
      "<div class=\"block\">"
        + "<div class=\"alert alert-warning\""
        + "style=\"font-size:12px;background: #FAC362;color: #333;\">"
        + "<i class=\"icon-large icon-pencil\"></i> <strong>PREPRINT</strong>" 
        + "</div>"
        + "</div>";
  private static final String withoutAlertWarning = 
      "<div class=\"block\"></div>";
  
  private static final String withRightbarWrap =
      "<div class=\"block\">"
        + "<div class=\"span2 article-item-rightbar-wrap article-sidebar\""
        + "style=\"height: 21136px;\">"
        + "<div style=\"margin-bottom:12px;\">"
        + "<form action=\"/follow/publication/46/0/\" method=\"post\">"
        + "<button class=\"btn btn-success\">Sign up for content alerts"
        + "<i class=\"icon-large icon-chevron-right icon-white\"></i>"
        + "</button></form>"   
        + "</div>"
        + "<h3 class=\"slim\">Similar Papers</h3>"
        + "<div class=\"row-fluid\" style=\"margin-top: 10px;\"></div>"
        + "<h3 class=\"slim\">Similar Academic Editors</h3>"
        + "</div>"
        + "</div>";
  private static final String withoutRightbarWrap = 
      "<div class=\"block\"></div>";
  
private static final String withFlagModal =
      "<div class=\"block\">"
        + "<div id=\"flagModal\" class=\"modal hide\""
        + "<div class=\"modal-header\">"
        + "<button type=\"button\" class=\"close\" data-dismiss=\"modal\""
        + "aria-hidden=\"true\">yyyy</button>"
        + "<h3>Flag an issue</h3>"
        + "</div>"
        + "</div>"
        + "</div>";
  private static final String withoutFlagModal = 
      "<div class=\"block\"></div>";

  private static final String withFollowModal =
      "<div class=\"block\">"
        + "<div id=\"flagModal\" class=\"modal hide\""
        + "<div class=\"modal-header\">"
        + "<button type=\"button\" class=\"close\" data-dismiss=\"modal\""
        + "aria-hidden=\"true\">aaaa</button>"
        + "<h3>Follow this issue</h3>"
        + "</div>"
        + "</div>"
        + "</div>";
  private static final String withoutFollowModal = 
      "<div class=\"block\"></div>";

  private static final String withUnfollowModal =
      "<div class=\"block\">"
        + "<div id=\"flagModal\" class=\"modal hide\""
        + "<div class=\"modal-header\">"
        + "<button type=\"button\" class=\"close\" data-dismiss=\"modal\""
        + "aria-hidden=\"true\">aaaa</button>"
        + "<h3>Unfollow this issue</h3>"
        + "</div>"
        + "</div>"
        + "</div>";
  private static final String withoutUnfollowModal = 
      "<div class=\"block\"></div>";

  private static final String withMetricsModal =
      "<div class=\"block\">"
         + "<div id=\"metricsModal\" class=\"modal hide\">"
         + "<div class=\"modal-body\""
         + "style=\"max-height:330px;overflow-y:auto\">"
         + "<div class=\"row-fluid\">"
         + "<div class=\"span12\"><p>Usage update</p>"
         + "</div></div></div></div>"
         + "</div>";
  private static final String withoutMetricsModal = 
      "<div class=\"block\"></div>";
  
  private static final String withShareModal =
      "<div class=\"block\">"
        + "<div id=\"shareModal\" class=\"modal hide\">"
        + "<div class=\"modal-body\""
        + "<div id=\"article-item-share-container\">"
        + "<h3 style=\"margin-bottom:10px;margin-top:0;\">Social networks</h3>"
        + "<div class=\"article-share-item\">"
        + "<a href=\"https://twitter.com/share\""
        + "class=\"twitter-share-button\""
        + "data-url=\"https://xxxx.com/articles/46/\">Tweet</a>"
        + "</div></div></div></div>"
        + "</div>";
  private static final String withoutShareModal = 
      "<div class=\"block\"></div>";
  
  private static final String withFoot =
      "<div class=\"block\">"
        + "<div class=\"foot\">"
        + "<div class=\"container\">"
        + "<div class=\"row\">"
	+ "<div class=\"span7\">"
	+ "<b>About us -</b> <a href=\"/about/\" class=\"aboutLink\""
	+ "data-target=\"theAteam\">the A team</a>"
	+ "</div></div></div></div>"
	+ "</div>";
  private static final String withoutFoot =
      "<div class=\"block\"></div>";

  private static final String withAnnotationTabsContent =
      "<div class=\"block\">"
        + "<div class=\"tab-content annotation-tab-content\">"
        + "<div class=\"tab-pane active\" id=\"feedback\">"
        + "<div class=\"row-fluid annotation-feedback\">"
        + "<h2>Add your feedback</h2>"
        + "<div id=\"article-item-main-text\""
        + "class=\"article-item-section-toggle\">"
        + "</div></div></div></div>"
        + "</div>";
  private static final String withoutAnnotationTabsContent = 
      "<div class=\"block\"></div>";

  private static final String withAnnotationTabsNav =
      "<div class=\"block\">"
        + "<ul class=\"nav nav-tabs annotation-tabs-nav\">"
        + "<li class=\"active\"><a href=\"#feedback\" data-toggle=\"tab\">"
        + "<i class=\"icon-thumbs-up-alt\"></i> Feedback</a></li>"
        + "<li><a href=\"#questions\" data-toggle=\"tab\">"
        + "<i class=\"icon-comments\"></i> Questions</a></li>"
        + "</ul>"
        + "</div>";
  private static final String withoutAnnotationTabsNav = 
      "<div class=\"block\"></div>";
  
  public void testHeadFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withHead), Constants.DEFAULT_ENCODING);
    assertEquals(withoutHead, StringUtil.fromInputStream(actIn));
  }

  public void testScriptFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withScript), Constants.DEFAULT_ENCODING);
    assertEquals(withoutScript, StringUtil.fromInputStream(actIn));
  }

  public void testNoscriptFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withNoscript), Constants.DEFAULT_ENCODING);
    assertEquals(withoutNoscript, StringUtil.fromInputStream(actIn));
  }

  public void testCommentsFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withComments), Constants.DEFAULT_ENCODING);
    assertEquals(withoutComments, StringUtil.fromInputStream(actIn));
  }

  public void testTopNavbarInverseFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withTopNavbarInverse), Constants.DEFAULT_ENCODING);
    assertEquals(withoutTopNavbarInverse, StringUtil.fromInputStream(actIn));
  }
  
  public void testTopNavbarInnerFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withTopNavbarInner), Constants.DEFAULT_ENCODING);
    assertEquals(withoutTopNavbarInner, StringUtil.fromInputStream(actIn));
  }
  
  public void testRightbarWrapFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withRightbarWrap), Constants.DEFAULT_ENCODING);
    assertEquals(withoutRightbarWrap, StringUtil.fromInputStream(actIn));
  }
      
  public void testwithFlagModalFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withHead), Constants.DEFAULT_ENCODING);
    assertEquals(withoutHead, StringUtil.fromInputStream(actIn));
  }
  
  public void testFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withFlagModal), Constants.DEFAULT_ENCODING);
    assertEquals(withoutFlagModal, StringUtil.fromInputStream(actIn));
  }
  
  public void testFollowModalFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withFollowModal), Constants.DEFAULT_ENCODING);
    assertEquals(withoutFollowModal, StringUtil.fromInputStream(actIn));
  }
  
  public void testUnfollowModalFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withUnfollowModal), Constants.DEFAULT_ENCODING);
    assertEquals(withoutUnfollowModal, StringUtil.fromInputStream(actIn));
  }
  
  public void testShareModalFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withShareModal), Constants.DEFAULT_ENCODING);
    assertEquals(withoutShareModal, StringUtil.fromInputStream(actIn));
  }

  public void testMetricsModalFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withMetricsModal), Constants.DEFAULT_ENCODING);
    assertEquals(withoutMetricsModal, StringUtil.fromInputStream(actIn));
  }
  
  public void testAlertWarningFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withAlertWarning), Constants.DEFAULT_ENCODING);
    assertEquals(withoutAlertWarning, StringUtil.fromInputStream(actIn));
  }
  
  public void testFootFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withFoot), Constants.DEFAULT_ENCODING);
    assertEquals(withoutFoot, StringUtil.fromInputStream(actIn));
  }
  
  public void testAnnotationTabsContentFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withAnnotationTabsContent), 
                                                 Constants.DEFAULT_ENCODING);
    assertEquals(withoutAnnotationTabsContent, 
                 StringUtil.fromInputStream(actIn));
  }
  
  public void testAnnotationTabsNavFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withAnnotationTabsNav), 
                                                 Constants.DEFAULT_ENCODING);
    assertEquals(withoutAnnotationTabsNav, StringUtil.fromInputStream(actIn));
  }

}
