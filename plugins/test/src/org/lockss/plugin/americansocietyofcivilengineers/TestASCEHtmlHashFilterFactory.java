/*
  $Id:
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

package org.lockss.plugin.americansocietyofcivilengineers;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestASCEHtmlHashFilterFactory extends LockssTestCase {
  private ASCEHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new ASCEHtmlHashFilterFactory();
  }
  
  // <div id="widget-10646" class="widget type-ad-placeholder ui-helper-clearfix">
  //   <div class="view">
  //     <div class="view-inner">
  //       <!-- placeholder id=null, description=Books Left 1 -->
  //            <a href="/action/clickThrough?id=1238&amp;url=%2Fpage%2Fjitse4
  //            %2Finfrastructureassessmentandpublicpolicy&amp;loc=%2Fdoi%2Fabs
  //            %2F10.1061%2F%2528ASCE%25291076-0431%25282007%252913%253A2
  //            %252872%2529&amp;pubId=40084448"><center>
  //            <img src="/sda/1238/infrastructure2.jpg"></center></a><br>
  //     </div>
  //   </div>
  // </div>
  private static final String withInfrastructureAssessmentAd =
      "<div class=\"block\">"
          + "<div id=\"widget-10646\""
          + " class=\"widget type-ad-placeholder ui-helper-clearfix\">"
          + "<div class=\"view\">"
          + "<div class=\"view-inner\">"
          + "<!-- placeholder id=null, description=Books Left 1 -->"
          + "<a href=\"/action/clickThrough?id=1238&amp;url=%2Fpage%2Fjitse4"
          + "%2Finfrastructureassessmentandpublicpolicy&amp;loc=%2Fdoi%2Fabs"
          + "%2F10.1061%2F%2528ASCE%25291076-0431%25282007%252913%253A2"
          + "%252872%2529&amp;pubId=40084448\"><center>"
          + "<img src=\"/sda/1238/infrastructure2.jpg\"></center></a><br>"
          + "</div>"
          + "</div>"
          + "</div>"
          + "</div>";

  private static final String withoutInfrastructureAssessmentAd =
      "<div class=\"block\"></div>";
  
  private static final String withSessionViewed =
      "<div class=\"block\">"
        + "<div class=\"sessionViewed\">"
        + "<div class=\"label\">Recently Viewed</div>"
        + "<ul class=\"sessionHistory\">"
        + "<li><a href=\"/doi/abs/10.1061/%28ASCE%290887-3801%282009%2923%3A1"
        + "%283%29\">Methodology for Automating the Identification and "
        + "Localization of Construction Components on Industrial Projects</a></li>"
        + "<li><a href=\"/doi/abs/10.1061/%28ASCE%290887-3801%282009%2923%3A1"
        + "%2814%29\">Methodology to Assess Building Designs for Protection "
        + "against Internal Chemical and Biological Threats</a></li>"
        + "<li><a href=\"/doi/abs/10.1061/%28ASCE%291076-0431%282007%2913%3A2"
        + "%2884%29\">Effects of HVAC System and Building Characteristics on "
        + "Exposure of Occupants to Short-Duration Point Source Aerosol "
        + "Releases</a></li>"
        + "</ul>"
        + "</div>"
        + "</div>";

  private static final String withoutSessionViewed =
      "<div class=\"block\"></div>";
  
  private static final String withCopyright =
      "<div class=\"block\">"
      + "<div id=\"footer_message\"><span style=\"color: rgb(0, 0, 0);\">"
      + "<span class=\"fontSize2\"><span style=\"color: rgb(0, 0, 0);\">"
      + "Copyright © 1996-2013, American So</span>ciety of Civil Engineers</span>"
      + "<br>"
      + "</span></div>"
      + "</div>";

  private static final String withoutCopyright =
      "<div class=\"block\"></div>";

  /*
   *  Compare Html and HtmlHashFiltered
   */

  public void testInfrastructureAssessmentAdHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withInfrastructureAssessmentAd), Constants.DEFAULT_ENCODING);
    assertEquals(withoutInfrastructureAssessmentAd,
        StringUtil.fromInputStream(actIn));
  }
  
  public void testSessionViewedHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withSessionViewed), Constants.DEFAULT_ENCODING);
    assertEquals(withoutSessionViewed,
        StringUtil.fromInputStream(actIn));
  }

  public void testCopyrightHtmlFiltering() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withCopyright), Constants.DEFAULT_ENCODING);
    assertEquals(withoutCopyright, StringUtil.fromInputStream(actIn));
  }

}
