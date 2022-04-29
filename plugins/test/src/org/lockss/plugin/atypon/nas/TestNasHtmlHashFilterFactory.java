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

  private static final String abstractSection =
    "<section id=\"frontmatter\" data-extent=\"frontmatter\">\n" +
    "   <header>\n" +
    "      <div class=\"core-container\">\n" +
    "         <div data-article-access=\"free\" data-article-access-type=\"free\" class=\"meta-panel\">\n" +
    "            <div class=\"meta-panel__left-content\">\n" +
    "               <div class=\"meta-panel__type\"><span>Letter</span></div>\n" +
    "               <div class=\"meta-panel__sub-type\"><a href=\"/topic/eco\">Ecology</a></div>\n" +
    "               <div class=\"meta-panel__access meta-panel__access--free\">FREE ACCESS</div>\n" +
    "            </div>\n" +
    "            <div class=\"meta-panel__right-content\">\n" +
    "               <div class=\"meta-panel__share\">\n" +
    "                  <!-- Go to www.addthis.com/dashboard to customize your tools --><script type=\"text/javascript\" async=\"async\" src=\"//s7.addthis.com/js/300/addthis_widget.js#pubid=xa-4faab26f2cff13a7\"></script>\n" +
    "                  <div class=\"share__block share__inline-links\">\n" +
    "                     <div class=\"pb-dropzone\" data-pb-dropzone=\"shareBlock\" title=\"shareBlock\"></div>\n" +
    "                     <span class=\"sr-only\">Share on</span>\n" +
    "                     <ul class=\"d-flex list-unstyled addthis addthis_toolbox mb-0\">\n" +
    "                        <li><a role=\"link\" class=\"addthis_button_facebook at300b\" title=\"Facebook\" href=\"#\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-facebook\"></i></a></li>\n" +
    "                        <li><a role=\"link\" class=\"addthis_button_twitter at300b\" title=\"Twitter\" href=\"#\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-twitter\"></i></a></li>\n" +
    "                        <li><a role=\"link\" class=\"addthis_button_linkedin at300b\" target=\"_blank\" title=\"LinkedIn\" href=\"#\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-LinkedIn2\"></i></a></li>\n" +
    "                        <li><a role=\"link\" class=\"addthis_button_mailto at300b\" href=\"https://v1.addthis.com/live/redirect/?url=mailto%3A%3Fbody%3Dhttps%253A%252F%252Fwww.pnas.org%252Fdoi%252F10.1073%252Fpnas.2113862119%26subject%3DExtreme%2520uncertainty%2520and%2520unquantifiable%2520bias%2520do%2520not%2520inform%2520population%2520sizes&amp;uid=62321b79242bdf95&amp;pub=xa-4faab26f2cff13a7&amp;rev=v8.28.8-wp&amp;per=undefined&amp;pco=tbx-300\" title=\"Email App\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-mail\"></i></a></li>\n" +
    "                        <div class=\"atclear\"></div>\n" +
    "                     </ul>\n" +
    "                  </div>\n" +
    "               </div>\n" +
    "               <div class=\"meta-panel__crossmark\"><span class=\"crossmark\"><a data-target=\"crossmark\" href=\"#\" data-doi=\"10.1073/pnas.2113862119\" class=\"crossmark__link\"><img alt=\"Check for updates on crossmark\" src=\"/specs/products/pnas/releasedAssets/images/crossmark.png\" class=\"crossmark__logo\"></a></span></div>\n" +
    "            </div>\n" +
    "         </div>\n" +
    "         <h1 property=\"name\">Extreme uncertainty and unquantifiable bias do not inform population sizes</h1>\n" +
    "         <div class=\"contributors\"><span class=\"authors\"><span role=\"list\"><span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con1\"><span property=\"givenName\">Orin J.</span> <span property=\"familyName\">Robinson</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-8935-1242\" property=\"identifier\">https://orcid.org/0000-0001-8935-1242</a> <a href=\"/cdn-cgi/l/email-protection#7817120a4f381b170a161d1414561d1c0d471b1b4517120a4f381b170a161d1414561d1c0d\" property=\"email\"><span class=\"__cf_email__\" data-cfemail=\"89e6e3fbbec9eae6fbe7ece5e5a7ecedfc\">[email&nbsp;protected]</span></a></span>, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con2\"><span property=\"givenName\">Jacob B.</span> <span property=\"familyName\">Socolar</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-9126-9093\" property=\"identifier\">https://orcid.org/0000-0002-9126-9093</a></span>, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con3\"><span property=\"givenName\">Erica F.</span> <span property=\"familyName\">Stuber</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-2687-6874\" property=\"identifier\">https://orcid.org/0000-0002-2687-6874</a></span><span data-control-for=\"ui-revealable\" aria-hidden=\"true\">, <span data-action-for=\"ui-revealable\" data-action=\"show\">+30</span> </span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con4\"><span property=\"givenName\">Tom</span> <span property=\"familyName\">Auer</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con5\"><span property=\"givenName\">Alex J.</span> <span property=\"familyName\">Berryman</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0003-1273-7184\" property=\"identifier\">https://orcid.org/0000-0003-1273-7184</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con6\"><span property=\"givenName\">Philipp H.</span> <span property=\"familyName\">Boersch-Supan</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-6723-6833\" property=\"identifier\">https://orcid.org/0000-0001-6723-6833</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con7\"><span property=\"givenName\">Donald J.</span> <span property=\"familyName\">Brightsmith</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-3306-6490\" property=\"identifier\">https://orcid.org/0000-0002-3306-6490</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con8\"><span property=\"givenName\">Allan H.</span> <span property=\"familyName\">Burbidge</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-2136-3973\" property=\"identifier\">https://orcid.org/0000-0002-2136-3973</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con9\"><span property=\"givenName\">Stuart H. M.</span> <span property=\"familyName\">Butchart</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-1140-4049\" property=\"identifier\">https://orcid.org/0000-0002-1140-4049</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con10\"><span property=\"givenName\">Courtney L.</span> <span property=\"familyName\">Davis</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-6467-4288\" property=\"identifier\">https://orcid.org/0000-0002-6467-4288</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con11\"><span property=\"givenName\">Adriaan M.</span> <span property=\"familyName\">Dokter</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-6573-066X\" property=\"identifier\">https://orcid.org/0000-0001-6573-066X</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con12\"><span property=\"givenName\">Adrian S.</span> <span property=\"familyName\">Di Giacomo</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con13\"><span property=\"givenName\">Andrew</span> <span property=\"familyName\">Farnsworth</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-9854-4449\" property=\"identifier\">https://orcid.org/0000-0002-9854-4449</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con14\"><span property=\"givenName\">Daniel</span> <span property=\"familyName\">Fink</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-8368-1248\" property=\"identifier\">https://orcid.org/0000-0002-8368-1248</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con15\"><span property=\"givenName\">Wesley M.</span> <span property=\"familyName\">Hochachka</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-0595-7827\" property=\"identifier\">https://orcid.org/0000-0002-0595-7827</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con16\"><span property=\"givenName\">Paige E.</span> <span property=\"familyName\">Howell</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-5911-2870\" property=\"identifier\">https://orcid.org/0000-0002-5911-2870</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con17\"><span property=\"givenName\">Frank A.</span> <span property=\"familyName\">La Sorte</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-8521-2501\" property=\"identifier\">https://orcid.org/0000-0001-8521-2501</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con18\"><span property=\"givenName\">Alexander C.</span> <span property=\"familyName\">Lees</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-7603-9081\" property=\"identifier\">https://orcid.org/0000-0001-7603-9081</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con19\"><span property=\"givenName\">Stuart</span> <span property=\"familyName\">Marsden</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con20\"><span property=\"givenName\">Robert</span> <span property=\"familyName\">Martin</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con21\"><span property=\"givenName\">Rowan O.</span> <span property=\"familyName\">Martin</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-0326-0161\" property=\"identifier\">https://orcid.org/0000-0002-0326-0161</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con22\"><span property=\"givenName\">Juan F.</span> <span property=\"familyName\">Masello</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-6826-4016\" property=\"identifier\">https://orcid.org/0000-0002-6826-4016</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con23\"><span property=\"givenName\">Eliot T.</span> <span property=\"familyName\">Miller</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con24\"><span property=\"givenName\">Yoshan</span> <span property=\"familyName\">Moodley</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0003-4216-2924\" property=\"identifier\">https://orcid.org/0000-0003-4216-2924</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con25\"><span property=\"givenName\">Andy</span> <span property=\"familyName\">Musgrove</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con26\"><span property=\"givenName\">David G.</span> <span property=\"familyName\">Noble</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con27\"><span property=\"givenName\">Valeria</span> <span property=\"familyName\">Ojeda</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0003-4158-704X\" property=\"identifier\">https://orcid.org/0000-0003-4158-704X</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con28\"><span property=\"givenName\">Petra</span> <span property=\"familyName\">Quillfeldt</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-4450-8688\" property=\"identifier\">https://orcid.org/0000-0002-4450-8688</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con29\"><span property=\"givenName\">J. Andrew</span> <span property=\"familyName\">Royle</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0003-3135-2167\" property=\"identifier\">https://orcid.org/0000-0003-3135-2167</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con30\"><span property=\"givenName\">Viviana</span> <span property=\"familyName\">Ruiz-Gutierrez</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con31\"><span property=\"givenName\">José L.</span> <span property=\"familyName\">Tella</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-3038-7424\" property=\"identifier\">https://orcid.org/0000-0002-3038-7424</a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con32\"><span property=\"givenName\">Pablo</span> <span property=\"familyName\">Yorio</span></a></span></span><span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con33\"><span property=\"givenName\">Casey</span> <span property=\"familyName\">Youngflesh</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-6343-3311\" property=\"identifier\">https://orcid.org/0000-0001-6343-3311</a></span></span>, and <span property=\"author\" typeof=\"Person\" role=\"listitem\"><a href=\"#con34\"><span property=\"givenName\">Alison</span> <span property=\"familyName\">Johnston</span></a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-8221-013X\" property=\"identifier\">https://orcid.org/0000-0001-8221-013X</a></span><span class=\"ui-hidden\" data-control-for=\"ui-revealable\" aria-hidden=\"true\"> <span data-action-for=\"ui-revealable\" data-action=\"hide\">-30</span></span></span></span></div>\n" +
    "         <div class=\"self-citation\"><span property=\"datePublished\">March 1, 2022</span> | <span class=\"core-enumeration\"><span property=\"isPartOf\" typeof=\"PublicationVolume\"><span property=\"volumeNumber\">119</span></span> (<span property=\"isPartOf\" typeof=\"PublicationIssue\"><span property=\"issueNumber\">10</span></span>) <span property=\"identifier\" typeof=\"Text\">e2113862119</span></span> | <a href=\"https://doi.org/10.1073/pnas.2113862119\" property=\"sameAs\">https://doi.org/10.1073/pnas.2113862119</a></div>\n" +
    "         <div data-core-tabs=\"relations\" class=\"core-relations my-3\"></div>\n" +
    "         <div class=\"info-panel\">\n" +
    "            <div class=\"info-panel__metrics info-panel__item\">\n" +
    "               <div class=\"toolbar-metric-container data-source\" data-source=\"/pb/widgets/toolBarMetric/getResponse?widgetId=e3cebcd6-6709-4826-9957-d822f658d986&amp;pbContext=%3Bjournal%3Ajournal%3Apnas%3Bpage%3Astring%3AArticle%2FChapter+View%3BrequestedJournal%3Ajournal%3Apnas%3Bctype%3Astring%3AJournal+Content%3BsubPage%3Astring%3AAbstract%3Bwebsite%3Awebsite%3Apnas-site%3Bwgroup%3Astring%3APublication+Websites%3Bissue%3Aissue%3Adoi%5C%3A10.1073%2Fpnas.2022.119.issue-10%3BpageGroup%3Astring%3APublication+Pages%3Barticle%3Aarticle%3Adoi%5C%3A10.1073%2Fpnas.2113862119&amp;doi=10.1073%2Fpnas.2113862119\"></div>\n" +
    "            </div>\n" +
    "            <div class=\"info-panel__right-items-wrapper\">\n" +
    "               <div class=\"info-panel__favorite info-panel__item\">\n" +
    "                  <div data-permission=\"\" class=\"article-tools\"><a href=\"/action/addCitationAlert?doi=10.1073%2Fpnas.2113862119\" target=\"_blank\" title=\"Track Citations\" aria-label=\"Track Citations\" data-toggle=\"tooltip\" class=\"article-tools__citation btn\"><i class=\"icon-bell\"></i></a><a href=\"/personalize/addFavoritePublication?doi=10.1073%2Fpnas.2113862119\" target=\"_blank\" title=\"Add to favorites\" aria-label=\"Add to favorites\" data-toggle=\"tooltip\" class=\"article-tools__favorite btn\"><i class=\"icon-bookmark\"></i></a></div>\n" +
    "               </div>\n" +
    "               <div class=\"info-panel__citations info-panel__item\"><a href=\"#tab-citations\" title=\"cite\" aria-label=\"citation\" data-toggle=\"tooltip\" class=\"btn\"><i class=\"icon-citations\"></i></a></div>\n" +
    "               <div class=\"info-panel__formats info-panel__item\"><a href=\"https://www.pnas.org/doi/epdf/10.1073/pnas.2113862119\" aria-label=\"PDF\" class=\"btn-circle btn-square btn-pdf ml-2\"><i class=\"icon-pdf-file-1\"></i></a></div>\n" +
    "            </div>\n" +
    "         </div>\n" +
    "      </div>\n" +
    "   </header>\n" +
    "   <nav data-core-nav=\"article\">\n" +
    "      <header>\n" +
    "         <div class=\"core-self-citation\"><span class=\"core-enumeration\"><span property=\"isPartOf\" typeof=\"PublicationVolume\">Vol. <span property=\"volumeNumber\">119</span></span> | <span property=\"isPartOf\" typeof=\"PublicationIssue\">No. <span property=\"issueNumber\">10</span></span></span></div>\n" +
    "      </header>\n" +
    "      <ul>\n" +
    "         <li><a href=\"#bibliography\">References</a></li>\n" +
    "      </ul>\n" +
    "   </nav>\n" +
    "   <div id=\"abstracts\">\n" +
    "      <div class=\"core-container\">\n" +
    "         <div class=\"core-first-page-image\"><img src=\"/cms/10.1073/pnas.2113862119/asset/8767a70f-fefc-4c96-a1b9-4f3a2ad0b9e3/assets/pnas.2113862119.fp.png\"></div>\n" +
    "      </div>\n" +
    "   </div>\n" +
    "</section>";

  private static final String filteredAbstractKeep =
    " <section data-extent=\"frontmatter\"> <header> <div class=\"core-container\"> <h1 property=\"name\">Extreme uncertainty and unquantifiable bias do not inform population sizes </h1> <div class=\"contributors\"> <span class=\"authors\"> <span role=\"list\"> <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con1\"> <span property=\"givenName\">Orin J. </span> <span property=\"familyName\">Robinson </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-8935-1242\" property=\"identifier\">https://orcid.org/0000-0001-8935-1242 </a> </span>, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con2\"> <span property=\"givenName\">Jacob B. </span> <span property=\"familyName\">Socolar </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-9126-9093\" property=\"identifier\">https://orcid.org/0000-0002-9126-9093 </a> </span>, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con3\"> <span property=\"givenName\">Erica F. </span> <span property=\"familyName\">Stuber </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-2687-6874\" property=\"identifier\">https://orcid.org/0000-0002-2687-6874 </a> </span> <span data-control-for=\"ui-revealable\" aria-hidden=\"true\">, <span data-action-for=\"ui-revealable\" data-action=\"show\">+30 </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con4\"> <span property=\"givenName\">Tom </span> <span property=\"familyName\">Auer </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con5\"> <span property=\"givenName\">Alex J. </span> <span property=\"familyName\">Berryman </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0003-1273-7184\" property=\"identifier\">https://orcid.org/0000-0003-1273-7184 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con6\"> <span property=\"givenName\">Philipp H. </span> <span property=\"familyName\">Boersch-Supan </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-6723-6833\" property=\"identifier\">https://orcid.org/0000-0001-6723-6833 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con7\"> <span property=\"givenName\">Donald J. </span> <span property=\"familyName\">Brightsmith </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-3306-6490\" property=\"identifier\">https://orcid.org/0000-0002-3306-6490 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con8\"> <span property=\"givenName\">Allan H. </span> <span property=\"familyName\">Burbidge </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-2136-3973\" property=\"identifier\">https://orcid.org/0000-0002-2136-3973 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con9\"> <span property=\"givenName\">Stuart H. M. </span> <span property=\"familyName\">Butchart </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-1140-4049\" property=\"identifier\">https://orcid.org/0000-0002-1140-4049 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con10\"> <span property=\"givenName\">Courtney L. </span> <span property=\"familyName\">Davis </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-6467-4288\" property=\"identifier\">https://orcid.org/0000-0002-6467-4288 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con11\"> <span property=\"givenName\">Adriaan M. </span> <span property=\"familyName\">Dokter </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-6573-066X\" property=\"identifier\">https://orcid.org/0000-0001-6573-066X </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con12\"> <span property=\"givenName\">Adrian S. </span> <span property=\"familyName\">Di Giacomo </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con13\"> <span property=\"givenName\">Andrew </span> <span property=\"familyName\">Farnsworth </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-9854-4449\" property=\"identifier\">https://orcid.org/0000-0002-9854-4449 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con14\"> <span property=\"givenName\">Daniel </span> <span property=\"familyName\">Fink </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-8368-1248\" property=\"identifier\">https://orcid.org/0000-0002-8368-1248 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con15\"> <span property=\"givenName\">Wesley M. </span> <span property=\"familyName\">Hochachka </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-0595-7827\" property=\"identifier\">https://orcid.org/0000-0002-0595-7827 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con16\"> <span property=\"givenName\">Paige E. </span> <span property=\"familyName\">Howell </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-5911-2870\" property=\"identifier\">https://orcid.org/0000-0002-5911-2870 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con17\"> <span property=\"givenName\">Frank A. </span> <span property=\"familyName\">La Sorte </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-8521-2501\" property=\"identifier\">https://orcid.org/0000-0001-8521-2501 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con18\"> <span property=\"givenName\">Alexander C. </span> <span property=\"familyName\">Lees </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-7603-9081\" property=\"identifier\">https://orcid.org/0000-0001-7603-9081 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con19\"> <span property=\"givenName\">Stuart </span> <span property=\"familyName\">Marsden </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con20\"> <span property=\"givenName\">Robert </span> <span property=\"familyName\">Martin </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con21\"> <span property=\"givenName\">Rowan O. </span> <span property=\"familyName\">Martin </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-0326-0161\" property=\"identifier\">https://orcid.org/0000-0002-0326-0161 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con22\"> <span property=\"givenName\">Juan F. </span> <span property=\"familyName\">Masello </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-6826-4016\" property=\"identifier\">https://orcid.org/0000-0002-6826-4016 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con23\"> <span property=\"givenName\">Eliot T. </span> <span property=\"familyName\">Miller </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con24\"> <span property=\"givenName\">Yoshan </span> <span property=\"familyName\">Moodley </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0003-4216-2924\" property=\"identifier\">https://orcid.org/0000-0003-4216-2924 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con25\"> <span property=\"givenName\">Andy </span> <span property=\"familyName\">Musgrove </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con26\"> <span property=\"givenName\">David G. </span> <span property=\"familyName\">Noble </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con27\"> <span property=\"givenName\">Valeria </span> <span property=\"familyName\">Ojeda </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0003-4158-704X\" property=\"identifier\">https://orcid.org/0000-0003-4158-704X </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con28\"> <span property=\"givenName\">Petra </span> <span property=\"familyName\">Quillfeldt </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-4450-8688\" property=\"identifier\">https://orcid.org/0000-0002-4450-8688 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con29\"> <span property=\"givenName\">J. Andrew </span> <span property=\"familyName\">Royle </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0003-3135-2167\" property=\"identifier\">https://orcid.org/0000-0003-3135-2167 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con30\"> <span property=\"givenName\">Viviana </span> <span property=\"familyName\">Ruiz-Gutierrez </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con31\"> <span property=\"givenName\">JosÃ© L. </span> <span property=\"familyName\">Tella </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0002-3038-7424\" property=\"identifier\">https://orcid.org/0000-0002-3038-7424 </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con32\"> <span property=\"givenName\">Pablo </span> <span property=\"familyName\">Yorio </span> </a> </span> </span> <span class=\"ui-revealable ui-hidden\">, <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con33\"> <span property=\"givenName\">Casey </span> <span property=\"familyName\">Youngflesh </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-6343-3311\" property=\"identifier\">https://orcid.org/0000-0001-6343-3311 </a> </span> </span>, and <span property=\"author\" typeof=\"Person\" role=\"listitem\"> <a href=\"#con34\"> <span property=\"givenName\">Alison </span> <span property=\"familyName\">Johnston </span> </a> <a class=\"orcid-id\" href=\"https://orcid.org/0000-0001-8221-013X\" property=\"identifier\">https://orcid.org/0000-0001-8221-013X </a> </span> <span class=\"ui-hidden\" data-control-for=\"ui-revealable\" aria-hidden=\"true\"> <span data-action-for=\"ui-revealable\" data-action=\"hide\">-30 </span> </span> </span> </span> </div> <div class=\"self-citation\"> <span property=\"datePublished\">March 1, 2022 </span> | <span class=\"core-enumeration\"> <span property=\"isPartOf\" typeof=\"PublicationVolume\"> <span property=\"volumeNumber\">119 </span> </span> ( <span property=\"isPartOf\" typeof=\"PublicationIssue\"> <span property=\"issueNumber\">10 </span> </span>) <span property=\"identifier\" typeof=\"Text\">e2113862119 </span> </span> | <a href=\"https://doi.org/10.1073/pnas.2113862119\" property=\"sameAs\">https://doi.org/10.1073/pnas.2113862119 </a> </div> <div data-core-tabs=\"relations\" class=\"core-relations my-3\"> </div> </div> </header> <div > <div class=\"core-container\"> <div class=\"core-first-page-image\"> </div> </div> </div> </section>";

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

  public void testAbstractKeep() throws Exception {
    InputStream inA;
    inA = fact.createFilteredInputStream(mau, new StringInputStream(abstractSection), Constants.DEFAULT_ENCODING);
    assertEquals(filteredAbstractKeep, StringUtil.fromInputStream(inA));
  }
}
