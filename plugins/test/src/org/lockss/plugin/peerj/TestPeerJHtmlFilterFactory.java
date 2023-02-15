/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the \"Software\"), to deal
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

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.*;

/*
 * Tests hash filtering for both PeerJ sites: Archives (main)
 * no longer testing preprints - not supported
 */
public class TestPeerJHtmlFilterFactory
  extends LockssTestCase {
  
  String variantPluginId;
  FilterFactory variantFact;
  ArchivalUnit variantArchivesAu;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
        
  private static final String filteredStr = 
      "A";
  
  // for hash filtering
  private static final String withHead = 
      "<html>"
          + "<head>X"
          + "<meta charset=\"utf-8\">"
          + "<title>The Article Title</title>"
          + "<meta name=\"citation_title\" content=\"Cloning\">"
          + "X</head>"
          + "A</html>";
  private static final String filteredHead = "A";
  
  private static final String withScript =
     "<div class=\"block\">"
         + "<script>"
         + "$(function() {"
         + "$(\"[rel=tooltip]\").tooltip({ placement: \"bottom\"});"
         + "$(\"[rel=popover]\").popover({ trigger: \"hover\"});"
         + "});"
         + "X</script>"
         + "A</div>";
  
  private static final String withNoscript = 
      "<div class=\"block\">"
        + "<noscript>"
        + "<div class=\"alert alert-warning\">X</div>"
        + "</noscript>"
        + "A</div>";
  
  private static final String withComments =
      "<div class=\"block\">"
        + "<!-- empty element at the foot of the page -->"
        + "A</div>";
     
  private static final String withInstitutionAlert =
      "<div class=\"block\">"
        + "<div id=\"instit-alert\" "
        + "class=\"well paper-well announcement-fixed\" "
        + "STYLE=\"display: block;\">"
        + "<i class=\"icon-remove announce-close\"></i>"
        + "<h2>Stanford University</h2>"
        + "Your institution has a plan for publish Open Access."
        + "<a id=\"instit-href\" class=\"open\" "
        + "href=\"/institutions/26/stanford-university/\">"
        + "See if you qualify <i class=\"icon-share-alt\"></i></a>"
        + "X</div>"
        + "A</div>";
  
  private static final String withReadAnnounceAlert =
      "<div class=\"block\">"
        + "<div id=\"read-announce-alert\" "
        + "class=\"well paper-well announcement-fixed\">"
        + "<i class=\"icon-remove announce-close\"></i>"
        + "<h2 class=\"slim\"><strong>Read</strong></h2>"
        + "choosing to publish in XXX.<br> <strong><a class=\"open\" "
        + "href=\"/about/interviews/\">Interviews</a></strong> / <strong>"
        + "<a class=\"open\" href=\"/edu/\">case studies</a></strong>"
        + "X</div>"
        + "A</div>";
    
  private static final String withQaAnnounceAlert =
      "<div class=\"block\">"
        + "<div id=\"qa-announce-alert\" "
        + "class=\"well paper-well announcement-fixed\">"
        + "<i class=\"icon-remove announce-close\"></i>"
        + "<h2 class=\"slim\">Publish for <strong>free</strong></h2>"
        + "waive your author fee<br> <strong><a class=\"open\" "
        + "href=\"http://blog.com/yourownforfree\">Learn more</a></strong>"
        + "X</div>"
        + "A</div>";
  
  private static final String withSubmitAnnounceAlert =
      "<div class=\"block\">"
        + "<div id=\"submit-announce-alert\" "
        + "class=\"well paper-well announcement-fixed\">"
        + "<i class=\"icon-remove announce-close\"></i>"
        + "<h2 class=\"slim\"><strong>Submit</strong> your next article</h2>"
     	+ "<strong><a class=\"open\""
        + "href=\"/about/interviews/\">Interviews</a></strong> / <strong>"
     	+ "<a class=\"open\" href=\"/edu/\">case studies</a></strong>"
     	+ "X</div>"
    	+ "A</div>";
  
  private static final String withTopNavbarFixedTop =
      "<div class=\"block\">"
        + "<div class=\"navbar navbar-fixed-top navbar-inverse\">"
	+ "<div class=\"navbar-inner\">"
        + "<div class=\"container\">"
        + "<li class=\"divider\"></li>"
        + "</div>"
        + "</div>"
        + "X</div>"
        + "A</div>";
  
  private static final String withItemTopNavbar =
      "<div class=\"block\">"
        + "<div class=\"item-top-navbar headroom headroom--top\">"
        + "<div class=\"item-top-navbar-inner\">"
        + "<div class=\"container\">"
        + "<a href=\"/\" class=\"brand item-top-navbar-brand\""
        + "<span class=\"item-top-navbar-social\">"
        + "<a class=\"pj-socialism tw-soc\""
        + "href=\"http://twitter.com/share?url=https://xxxxx.com/9999/"
        + "&amp;via=thexxxx&amp;text=yyyyyyy\"</a>"
        + "</span>"
        + "X</div></div></div>"
        + "A</div>";
    
  private static final String withAllLeftColumnExceptDownloadAs =
      "<div class=\"block\">"
        + "<div class=\"article-item-leftbar\">" 
        + "<div class=\"btn-group notification-actions-btn\">" 
        + "<span>Subscribe</span>" 
        + "<span>Follow</span></span>" 
        + "<div class=\"btn-group\">" 
        + "<a href=\"#\">Download as</a>" 
        + "<ul class=\"dropdown-menu\">" 
        + "<li><a href=\"/9999.pdf\">PDF</a></li>" 
        + "<li><a href=\"/9999.xml\">XML</a></li>" 
        + "<li><a href=\"/9999.ris\">RIS</a></li>" 
        + "<li><a href=\"/9999.bib\">BibTeX</a></li>" 
        + "<li><a href=\"http://www.mendeley.com/import/xxxx.9999\">"
        + "Save to Mendeley</a></li>" 
        + "<li><a href=\"http://www.readcube.com/xxxx.9999\">"
        + "Read in ReadCube</a></li>" 
        + "</ul>" 
        + "</div>" 
        + "<nav class=\"article-sidebar-block\">" 
        + "<div class=\"article-navigation\">" 
        + "<ul class=\"nav nav-list\">" 
        + "<li class=\"article-anchor\"><a href=\"#intro\">"
        + "Introduction</a></li>" 
        + "</ul>" 
        + "</div>" 
        + "</nav>" 
        + "<nav class=\"article-sidebar-block\">" 
        + "<ul class=\"nav nav-list\">" 
        + "<li><a href=\"/9999/citations/\">Articles citing this paper" 
        + "<span>4</span></a></li>" 
        + "</ul>" 
        + "</nav>" 
        + "</div>" 
        + "<nav class=\"article-sidebar-block\">" 
        + "<div class=\"subjects-navigation\">" 
        + "<ul class=\"nav nav-list\">" 
        + "<li class=\"article-anchor\">" 
        + "<a href=\"/search/?subject=Biochemistry\">Biochemistry</a></li>" 
        + "</ul>" 
        + "</div>" 
        + "</nav>" 
        + "<div id=\"article-item-metrics-container\" " 
        + "<div data-count=\"visitors\">995</div>" 
        + "<div class=\"article-item-metrics-label\">Visitors</div>" 
        + "</div>" 
        + "</div>" 
        + "</div>" 
        + "</div>" 
        + "</div>" 
        + "A</div>";

  private static final String withoutAllLeftColumnExceptDownloadAs = 
        ""
          + "PDF"
          + "XML"
          + "RIS"
          + "BibTeX"
          + "Save to Mendeley"
          + "Read in ReadCube"
          + "A";
  
  private static final String withAllRightColumnn =
      "<div class=\"block\">"
        + "<div class=\"span2 article-item-rightbar-wrap article-sidebar\""
        + "style=\"height: 21136px;\">"
        + "<div style=\"margin-bottom:12px;\">"
        + "<form action=\"/follow/publication/9999/0/\" method=\"post\">"
        + "<button class=\"btn btn-success\">Sign up for content alerts"
        + "<i class=\"icon-large icon-chevron-right icon-white\"></i>"
        + "</button></form>"   
        + "</div>"
        + "<h3 class=\"slim\">Similar Papers</h3>"
        + "<div class=\"row-fluid\" style=\"margin-top: 10px;\"></div>"
        + "<h3 class=\"slim\">Similar Academic Editors</h3>"
        + "X</div>"
        + "A</div>";
  
private static final String withFlagModal =
      "<div class=\"block\">"
        + "<div id=\"flagModal\" class=\"modal hide\""
        + "<div class=\"modal-header\">"
        + "<button type=\"button\" class=\"close\" data-dismiss=\"modal\""
        + "aria-hidden=\"true\">yyyy</button>"
        + "<h3>Flag an issue</h3>"
        + "</div>"
        + "X</div>"
        + "A</div>";

  private static final String withFollowModal =
      "<div class=\"block\">"
        + "<div id=\"flagModal\" class=\"modal hide\""
        + "<div class=\"modal-header\">"
        + "<button type=\"button\" class=\"close\" data-dismiss=\"modal\""
        + "aria-hidden=\"true\">aaaa</button>"
        + "<h3>Follow this issue</h3>"
        + "</div>"
        + "X</div>"
        + "A</div>";

  private static final String withUnfollowModal =
      "<div class=\"block\">"
        + "<div id=\"flagModal\" class=\"modal hide\""
        + "<div class=\"modal-header\">"
        + "<button type=\"button\" class=\"close\" data-dismiss=\"modal\""
        + "aria-hidden=\"true\">aaaa</button>"
        + "<h3>Unfollow this issue</h3>"
        + "</div>"
        + "X</div>"
        + "A</div>";

  private static final String withMetricsModal =
      "<div class=\"block\">"
         + "<div id=\"metricsModal\" class=\"modal hide\">"
         + "<div class=\"modal-body\""
         + "style=\"max-height:330px;overflow-y:auto\">"
         + "<div class=\"row-fluid\">"
         + "<div class=\"span12\"><p>Usage update</p>"
         + "X</div></div></div></div>"
         + "A</div>";
  
  private static final String withShareModal =
      "<div class=\"block\">"
        + "<div id=\"shareModal\" class=\"modal hide\">"
        + "<div class=\"modal-body\""
        + "<div id=\"article-item-share-container\">"
        + "<h3 style=\"margin-bottom:10px;margin-top:0;\">Social networks</h3>"
        + "<div class=\"article-share-item\">"
        + "<a href=\"https://twitter.com/share\""
        + "class=\"twitter-share-button\""
        + "data-url=\"https://xxxx.com/9999/\">Tweet</a>"
        + "X</div></div></div></div>"
        + "A</div>";
  
  private static final String withCitingModal =
      "<div class=\"block\">"
        + "<div id=\"citing-modal\" class=\"modal hide\">"
        + "<div class=\"modal-header\">"
        + "<button type=\"button\" class=\"close\" data-dismiss=\"modal\""
        + "aria-hidden=\"true\">zz</button>"
        + "<h2 class=\"slim\"><i class=\"icon-copy\"></i>"
        + "Citing information</h2>"
        + "</div>"
        + "<div class=\"modal-body\">Loading citing information ... "
        + "<i class=\"icon icon-spinner icon-spin\"></i></div>"
        + "X</div>"
        + "A</div>";
  
  private static final String withArticleLinksModal =
      "<div class=\"block\">"
        + "<div class=\"modal hide fade\" id=\"article-links-modal\">"
        + "<div class=\"modal-header\">"
        + "<a data-dismiss=\"modal\" aria-hidden=\"true\" "
        + "class=\"close\">yy</a>"
        + "<h3 class=\"modal-title\">Link titles</h3>"
        + "</div>"
        + "<div class=\"modal-body\"></div>"
        + "<div class=\"modal-footer\">"
        + "<a href=\"/yyy\" "
        + "class=\"btn btn-primary\">adding link</a>"
        + "<button class=\"btn follow-close-btn\" data-dismiss=\"modal\""
        + "aria-hidden=\"true\">Close</button>"
        + "</div>"
        + "X</div>"
        + "A</div>";
  
  private static final String withFoot =
      "<div class=\"block\">"
        + "<div class=\"foot\">"
        + "<div class=\"container\">"
        + "<div class=\"row\">"
	+ "<div class=\"span7\">"
	+ "<b>About us -</b> <a href=\"/about/\" class=\"aboutLink\""
	+ "data-target=\"theAteam\">the A team</a>"
	+ "X</div></div></div></div>"
	+ "A</div>";

  private static final String withAnnotationTabsNav =
      "<div class=\"block\">"
        + "<ul class=\"nav nav-tabs annotation-tabs-nav\">"
        + "<li class=\"active\"><a href=\"#feedback\" data-toggle=\"tab\">"
        + "<i class=\"icon-thumbs-up-alt\"></i> Feedback</a></li>"
        + "<li><a href=\"#questions\" data-toggle=\"tab\">"
        + "<i class=\"icon-comments\"></i> Questions</a></li>"
        + "X</ul>"
        + "A</div>";
    
  private static final String withAnnotationTabsContent =
      "<div class=\"block\">"
        + "<div class=\"tab-content annotation-tab-content\">"
        + "<div class=\"tab-pane active\" id=\"feedback\">"
        + "<div class=\"row-fluid annotation-feedback\">"
        + "<h2>Add your feedback</h2>"
        + "<div id=\"article-item-main-text\""
        + "class=\"article-item-section-toggle\">"
        + "X</div></div></div></div>"
        + "A</div>";

  private static final String withAnnotationsOuterHeapmap =
      "<div class=\"block\">"
        + "<div class=\"annotations-outer-heatmap\">"
        + "<div class=\"current-scroll\" "
        + "style=\"height: 6.011233540001%; top: 0%;\">"
        + "X</div></div>"
        + "A</div>";

  private static final String withNav =
      "<div class=\"block\">"
        + "<nav class=\"navbar navbar-fixed-top navbar-inverse navbar-alpha\" role=\"navigation\">"
        + "X</nav>"
        + "A</div>";

  
  protected ArchivalUnit createAu(String pluginId)
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(pluginId,
        peerjAuConfig());
  }
  
  private Configuration peerjAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "abc");
    conf.put("volume_name", "99");
    return conf;
  }
  
  private static void doFilterTest(ArchivalUnit au, 
      FilterFactory fact, String nameToHash, String expectedStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);
    assertEquals(expectedStr, StringUtil.fromInputStream(actIn));
  }
  
  public void startMockDaemon() {
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }
          		
  // Variant to test with Hash Filter
  public static class TestHash extends TestPeerJHtmlFilterFactory {   
    public void setUp() throws Exception {
      super.setUp();
      tempDirPath = setUpDiskSpace();
      startMockDaemon();
      variantFact = new PeerJHtmlHashFilterFactory();
      variantArchivesAu = createAu("org.lockss.plugin.peerj.ClockssPeerJPlugin");
    }
    public void testFilteringArchives() throws Exception {
      doFilterTest(variantArchivesAu, variantFact, withHead, filteredHead);
      doFilterTest(variantArchivesAu, variantFact, withScript, filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withNoscript, filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withComments, filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withInstitutionAlert,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withReadAnnounceAlert,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withQaAnnounceAlert,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withSubmitAnnounceAlert, 
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withTopNavbarFixedTop,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withItemTopNavbar,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, 
          withAllLeftColumnExceptDownloadAs, 
          withoutAllLeftColumnExceptDownloadAs);
      doFilterTest(variantArchivesAu, variantFact, withAllRightColumnn, 
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withFlagModal, filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withFollowModal,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withUnfollowModal, 
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withMetricsModal, 
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withShareModal, 
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withCitingModal,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withArticleLinksModal,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withFoot, filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withAnnotationTabsNav,
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withAnnotationTabsContent, 
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withAnnotationsOuterHeapmap, 
          filteredStr);
      doFilterTest(variantArchivesAu, variantFact, withNav, filteredStr);
    }
  }
   
  public static Test suite() {
    return variantSuites(new Class[] {
        TestHash.class
    });
  }
  
}

