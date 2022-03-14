/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.atypon.nas;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestNasHtmlHashFilterFactory extends LockssTestCase {

  private NasHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new NasHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String variousCmsAssetImgs =
    "<div class=\"testSection\">" +
      "<div data-cover-src=\"/cms/asset/099477d1-3f71-4e50-84b5-bbbf4ed3cdfa/pnas.2022.119.issue-9.largecover.png\" class=\"cover-image__popup-moving-cover position-fixed d-block\"></div>" +
      "<div data-cover-src=\"/cms/asset/56597d67-7abd-41f4-a970-ff42ae8e0057/pnas.2022.119.issue-9.largecover.png\" class=\"cover-image__popup-moving-cover position-fixed d-block\"></div>" +
      "<div role=\"presentation\" style=\"background: linear-gradient(180deg, #000000 0%, rgba(0, 0, 0, 0.74) 42.8%, rgba(0, 0, 0, 0) 84%), url(/cms/asset/3a6137bb-cbbb-465d-bd3f-32b6e37903f5/toc-banner.jpg) no-repeat; background-size: cover;\" class=\"banner-widget__background\"></div>" +
      "<div role=\"presentation\" style=\"background: linear-gradient(180deg, #000000 0%, rgba(0, 0, 0, 0.74) 42.8%, rgba(0, 0, 0, 0) 84%), url(/cms/asset/0976e527-f944-425c-8436-d41b5d4e0bcd/toc-banner.jpg) no-repeat; background-size: cover;\" class=\"banner-widget__background\"></div>" +
      "<img src=\"/cms/10.1073/iti0122119/asset/9dc20090-4b47-4c0a-9abe-dab0613ae80e/assets/images/large/iti0122119unfig03.jpg\" />" +
      "<img src=\"/cms/10.1073/iti0122119/asset/0ea25029-06ce-4afb-8c81-3c6527f7e4b3/assets/images/large/iti0122119unfig03.jpg\" />" +
      "<img src=\"/cms/asset/099477d1-3f71-4e50-84b5-bbbf4ed3cdfa/pnas.2022.119.issue-9.largecover.png\" alt=\"Go to Proceedings of the National Academy of Sciences \" loading=\"lazy\" />" +
      "<img src=\"/cms/asset/56597d67-7abd-41f4-a970-ff42ae8e0057/pnas.2022.119.issue-9.largecover.png\" alt=\"Go to Proceedings of the National Academy of Sciences \" loading=\"lazy\" />" +
      "<img alt=\"March 8, 2022 Vol. 119 No. 10\" src=\"/cms/asset/09b6afce-9f42-41d7-a57d-4d56c6cf75f1/pnas.2022.119.issue-10.largecover.png\" loading=\"lazy\" width=\"272\" height=\"354\" class=\"w-100 h-auto\" />" +
      "<img alt=\"March 8, 2022 Vol. 119 No. 10\" src=\"/cms/asset/ec13a2d2-b070-4378-8a1a-a07d1a159c65/pnas.2022.119.issue-10.largecover.png\" loading=\"lazy\" width=\"272\" height=\"354\" class=\"w-100 h-auto\" />" +
      "<a href=\"/cms/asset/535b0729-21f9-405e-8e07-941b49472ab2/pnas.2022.119.issue-9.toc.pdf\" class=\"animation-icon-shift cta text-uppercase font-weight-bold text-reset\"></a>" +
      "<a href=\"/cms/asset/0077a5af-361b-4216-89d3-403980be452b/pnas.2022.119.issue-9.toc.pdf\" class=\"animation-icon-shift cta text-uppercase font-weight-bold text-reset\"></a>" +
    "</div>";

  private static final String filteredVariousCmsAssetImgs = " <div class=\"testSection\"> </div>";

  private static final String pubMed =
    "<div class=\"core-pmid\">" +
      "<span class=\"heading\">PubMed</span>: <a class=\"content\" href=\"https://pubmed.ncbi.nlm.nih.gov/35263229/\" property=\"sameAs\">35263229</a>" +
    "</div>";

  private static final String timeBadgeStamps =
    "<div class=\"testSection\">" +
      "<span class=\"pl-2 ml-2 pl-2 border-left border-darker-gray card__meta__date\">\n" +
        "February 22, 2022" +
      "</span>\n" +
      "<span class=\"pl-2 ml-2 border-left border-darker-gray card__meta__badge\">\n" +
        "From the Cover" +
      "</span>" +
    "</div>";

  private static final String filteredTimeBadgeStamps = " <div class=\"testSection\"> </div>";

  private static final String iframe =
    "<iframe src=\"https://iframe.videodelivery.net/https://videodelivery.net/eyJraWQiOiI3YjgzNTg3NDZlNWJmNDM0MjY5YzEwZTYwMDg0ZjViYiIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiI0MGU2NTZkNjEyMDNiYTVjM2VmYmFjMTc3YTg5Njk3NyIsImtpZCI6IjdiODM1ODc0NmU1YmY0MzQyNjljMTBlNjAwODRmNWJiIiwiZXhwIjoxNjQ3MDY1OTM1fQ.0pXPHunEO5QdlLGnCIsEQr6vmM7WFnrgx7YagV-h1rCoRgSSRZkHk2aT4-fVbJ-Obk0kS7NKw8JtQtfzbJfFjaUp6dI0HGKEzRvQGeJgcc-wh6aNjsszHMCd8mNgh0YlS-WRnch1fqk62E_1W2EkFS5zgN0CysnNk_vnIhtLvWwPQdbkPqaNHheui3LwwykgmsjgQxWy-rVBIaPMs1MoJK8Do-PCk8fjq8_N0CSX35KzGudR1gx05bcm-R1rOKHJ009d2WlIfyZPGZ3XaK_zUy4vxxVPa5A_ChnRpmTnQhcL1_W1yBm9rP2WIXcbUaFDwlxxnbWBCyvxVjgvcwYUOg/thumbnails/thumbnail.jpg?time=10.0s\"  style=\"border: none;\"  height=640\"  width=\"100%\"  allow=\"accelerometer; gyroscope; autoplay; encrypted-media; picture-in-picture;\"  allowfullscreen=\"false\">\n";

  private static final String h3withId1 = "<h3 id=\"h_d1245944e49\" class=\"to-section mb-4\"></h3>";

  private static final String h3withId2 = "<h3 id=\"h_d1049943e49\" class=\"to-section mb-4\"></h3>";

  private static final String blankString = "";

  public void testCmsAssetRemoval() throws Exception {
    InputStream inA;
    inA = fact.createFilteredInputStream(mau, new StringInputStream(variousCmsAssetImgs), Constants.DEFAULT_ENCODING);
    assertEquals(filteredVariousCmsAssetImgs, StringUtil.fromInputStream(inA));
  }

  public void testPubMedRemoval() throws Exception {
    InputStream inA;
    inA = fact.createFilteredInputStream(mau, new StringInputStream(pubMed), Constants.DEFAULT_ENCODING);
    assertEquals(blankString, StringUtil.fromInputStream(inA));
  }

  public void testTimeBadgeRemoval() throws Exception {
    InputStream inA;
    inA = fact.createFilteredInputStream(mau, new StringInputStream(timeBadgeStamps), Constants.DEFAULT_ENCODING);
    assertEquals(filteredTimeBadgeStamps, StringUtil.fromInputStream(inA));
  }

  public void testIframeRemoval() throws Exception {
    InputStream inA;
    inA = fact.createFilteredInputStream(mau, new StringInputStream(iframe), Constants.DEFAULT_ENCODING);
    assertEquals(blankString, StringUtil.fromInputStream(inA));
  }

  public void testIdRemoval() throws Exception {
    InputStream inA, inB;
    inA = fact.createFilteredInputStream(mau, new StringInputStream(h3withId1), Constants.DEFAULT_ENCODING);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(h3withId2), Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(inB), StringUtil.fromInputStream(inA));
  }
}
