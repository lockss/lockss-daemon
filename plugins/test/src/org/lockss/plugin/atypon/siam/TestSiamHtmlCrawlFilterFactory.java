/*
 * $Id$
 */

/*

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

package org.lockss.plugin.atypon.siam;

import java.io.*;

import junit.framework.Test;

import org.lockss.util.*;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.*;

public class TestSiamHtmlCrawlFilterFactory extends LockssTestCase {
  private static FilterFactory fact;
  private static MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new SiamHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  //Example instances mostly from pages of strings of HTMl that should be filtered
  //and the expected post filter HTML strings.

  private static final String sessionHistoryHtml =  
      "<div class=\"view\">" +
          "   <div class=\"view-inner\">" +
          "              <div class=\"panel panel_476\"  id=\"sessionHistory\">" +
          "<div class=\"box\">" +
          "       <div class=\"header \">" +
          "          <h3>Session History</h3>" +
          "     </div>" +
          "        <div class=\"no-top-border\"> " +
          "       <div class=\"box-inner\">" +
          " <div class=\"sessionViewed\">" +
          "     <div class=\"label\">Recently Viewed</div>" +
          "     <ul class=\"sessionHistory\" >" +
          "            <li><a href=\"/doi/abs/10.1137/110842545\">Importan Title</a></li>" +
          "   </ul>" +
          " </div>" +
          "      </div>" +
          "  </div></div></div></div></div>";
  private static final String sessionHistoryHtmlFiltered =
      "<div class=\"view\">" +
          "   <div class=\"view-inner\">" +
          "              </div></div>";

  private static final String citedByHtml =
      " <!-- fulltext content --><div class=\"citedBySection\">" +
          "<a name=\"citedBySection\"></a><h2>Cited by</h2>" +
          "<div class=\"citedByEntry\"> (2013) Large Deviations and Importance Sampling for Systems of Slow-Fast Motion. <i>" +
          "<span class=\"NLM_source\">Applied Mathematics &amp; Optimization</span></i> " +
          "<b>67</b>:1, 123-161" +
          "<span class=\"CbLinks\"><!--noindex-->" +
          "<a class=\"ref\" href=\"http://dx.doi.org/10.1007/s00245-012-9183-z\" target=\"_blank\" title=\"Opens new window\"> CrossRef </a>" +
          "<!--/noindex--></span></div></div><!-- /fulltext content --></div>";
  private static final String citedByHtmlFiltered =
      " <!-- fulltext content --><!-- /fulltext content --></div>";

  private static final String referencesHtml =
      "<!-- fulltext content --><div class=\"abstractReferences\">" +
          "<a name=\"\"><!-- title --></a><div class=\"sectionHeadingContainer\">" +
          "<div class=\"sectionHeadingContainer2\">" +
          "<table class=\"sectionHeading\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
          "<tr><th align=\"left\" valign=\"middle\" width=\"95%\"></th></tr></table>" +
          "</div></div>" +
          "<ol>" +
          "<li class=\"reference\">[1] O.  Author Name, Title, <i>A Siam Journal Title. </i>, <b>40 </b> (2001), pp. 1159–1188.  " +
          "<a class=\"ref\" href=\"javascript:newWindow('http://dx.doi.org/10.1137%2FS0363012900366741')\">[Abstract]</a>" +
          "<script type=\"text/javascript\">genRefLink(128, 'R1', '000173252500008');</script>" +
          "<a href=\"/servlet/linkout?suffix=R1\" title=\"OpenURL Stanford University\" onclick=\"stuff\" class=\"sfxLink\">" +
          "<img src=\"/userimages/2939/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></li>" +
          "<li class=\"reference\">[5] Author Other, Title Other,  Birkhäuser, Boston, 1997. </li>" +
          "</ol></div>";
  private static final String referencesHtmlFiltered =
      "<!-- fulltext content -->";

 //Variant to test with Crawl Filter
 public static class TestCrawl extends TestSiamHtmlCrawlFilterFactory {
          
          public void setUp() throws Exception {
                  super.setUp();
                  fact = new SiamHtmlCrawlFilterFactory();
          }

  }
  
  public void testHtmlSessionHistory() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(sessionHistoryHtml), Constants.DEFAULT_ENCODING);

    assertEquals(sessionHistoryHtmlFiltered, StringUtil.fromInputStream(actIn1));
  }
  
  public void testHtmlCitedBy() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(citedByHtml), Constants.DEFAULT_ENCODING);

    assertEquals(citedByHtmlFiltered, StringUtil.fromInputStream(actIn1));
  }
  
  public void testHtmlReferences() throws Exception {
    InputStream actIn1 = fact.createFilteredInputStream(mau,
        new StringInputStream(referencesHtml), Constants.DEFAULT_ENCODING);

    assertEquals(referencesHtmlFiltered, StringUtil.fromInputStream(actIn1));
  }
  
}
