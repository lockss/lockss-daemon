/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer.link;

import java.io.InputStream;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestSpringerLinkHtmlCrawlFilterFactory extends LockssTestCase {
  private SpringerLinkHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new SpringerLinkHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  

  private static final String references = 
    "<h2 class=\"Heading\">References</h2>" +
    "<div class=\"content\">" +
    "  <ol class=\"BibliographyWrapper\">" +
    "    <li class=\"Citation\">"+
    "      <div class=\"CitationNumber\">1.</div>" +
    "      <div class=\"CitationContent\" id=\"CR1\">Bohm, D.: A suggested interpretation of the quantum theory in terms of “Hidden” variables. I. Phys. Rev. " +
    "        <strong class=\"EmphasisTypeBold \">85</strong>(2), 166 (1952). doi:" +
    "        <span class=\"ExternalRef\"> " +
    "          <a target=\"_blank\" rel=\"noopener\" href=\"https://doi.org/10.1103/PhysRev.85.166\">" +
    "            <span class=\"RefSource\">10.1103/PhysRev.85.166</span>" +
    "          </a>" +
    "        </span>" +
    "        <span class=\"Occurrences\"><span class=\"Occurrence OccurrenceBibcode\">" +
    "          <a class=\"gtm-reference\" data-reference-type=\"ADS\" target=\"_blank\" rel=\"noopener\" href=\"http://adsabs.harvard.edu/cgi-bin/nph-data_query?link_type=ABSTRACT&amp;bibcode=1952PhRv...85..166B\">" +
    "            <span>" +
    "              <span>ADS</span>" +
    "            </span>" +
    "          </a>" +
    "        </span>" +
    "      </span>" +
    "      </div>" +
    "    </li>" +
    "  </ol>" +
    "</div>" +
    "Hello World";

  private static final String referencesFiltered = 
    "<h2 class=\"Heading\">References</h2>" +
    "<div class=\"content\">" +
    "<ol class=\"BibliographyWrapper\">" +
    "Hello World";

  private static final String articleHeader =
    "<div class=\"c-article-header\">" +
    "  <header>" +
    "    <ul class=\"c-article-identifiers\" data-test=\"article-identifier\">" +
    "      <li class=\"c-article-identifiers__item\" data-test=\"article-category\">Research</li>" +
    "      <li class=\"c-article-identifiers__item\"><span class=\"c-article-identifiers__open\" data-test=\"open-access\">Open Access</span></li>" +
    "      <li class=\"c-article-identifiers__item\"><a href=\"#article-info\" data-track=\"click\" data-track-action=\"publication date\" data-track-label=\"link\">Published: <time datetime=\"2019-01-09\" itemprop=\"datePublished\">09 January 2019</time></a></li>" +
    "    </ul>" +
    "    <h1 class=\"c-article-title\" data-test=\"article-title\" data-article-title=\"\" itemprop=\"name headline\">Embodied meaning: a systemic functional perspective on paralanguage</h1>" +
    "    <p class=\"c-article-info-details\" data-container-section=\"info\"><a data-test=\"journal-link\" href=\"/journal/40554\"><i data-test=\"journal-title\">Functional Linguistics</i></a><b data-test=\"journal-volume\"><span class=\"u-visually-hidden\">volume</span>&nbsp;6</b>, Article&nbsp;number:&nbsp;<span data-test=\"article-number\">1</span> (<span data-test=\"article-publication-year\">2019</span>)<a href=\"#citeas\" class=\"c-article-info-details__cite-as u-hide-print\" data-track=\"click\" data-track-action=\"cite this article\" data-track-label=\"link\">Cite this article</a></p>" +
    "    <div data-test=\"article-metrics\">" +
    "      <div id=\"altmetric-container\">" +
    "        <div class=\"c-article-metrics-bar__wrapper u-clear-both\">" +
    "          <ul class=\"c-article-metrics-bar u-list-reset\">" +
    "            <li class=\" c-article-metrics-bar__item\">" +
    "              <p class=\"c-article-metrics-bar__count\">4856 <span class=\"c-article-metrics-bar__label\">Accesses</span></p>" +
    "            </li>" +
    "            <li class=\"c-article-metrics-bar__item\">" +
    "              <p class=\"c-article-metrics-bar__count\">4 <span class=\"c-article-metrics-bar__label\">Citations</span></p>" +
    "            </li>" +
    "            <li class=\"c-article-metrics-bar__item\">" +
    "              <p class=\"c-article-metrics-bar__count\">1 <span class=\"c-article-metrics-bar__label\">Altmetric</span></p>" +
    "            </li>" +
    "            <li class=\"c-article-metrics-bar__item\">" +
    "              <p class=\"c-article-metrics-bar__details\"><a href=\"/article/10.1186%2Fs40554-018-0065-9/metrics\" data-track=\"click\" data-track-action=\"view metrics\" data-track-label=\"link\" rel=\"nofollow\">Metrics <span class=\"u-visually-hidden\">details</span></a></p>" +
    "            </li>" +
    "          </ul>" +
    "        </div>" +
    "      </div>" +
    "    </div>" +
    "  </header>" +
    "</div>";

  private static final String filteredArticleHeader =
    "<div class=\"c-article-header\">" +
    "  <header>" +
    "    <ul class=\"c-article-identifiers\" data-test=\"article-identifier\">" +
    "      <li class=\"c-article-identifiers__item\" data-test=\"article-category\">Research</li>" +
    "      <li class=\"c-article-identifiers__item\"><span class=\"c-article-identifiers__open\" data-test=\"open-access\">Open Access</span></li>" +
    "      <li class=\"c-article-identifiers__item\"><a href=\"#article-info\" data-track=\"click\" data-track-action=\"publication date\" data-track-label=\"link\">Published: <time datetime=\"2019-01-09\" itemprop=\"datePublished\">09 January 2019</time></a></li>" +
    "    </ul>" +
    "    <h1 class=\"c-article-title\" data-test=\"article-title\" data-article-title=\"\" itemprop=\"name headline\">Embodied meaning: a systemic functional perspective on paralanguage</h1>" +
    "    <p class=\"c-article-info-details\" data-container-section=\"info\"><a data-test=\"journal-link\" href=\"/journal/40554\"><i data-test=\"journal-title\">Functional Linguistics</i></a><b data-test=\"journal-volume\"><span class=\"u-visually-hidden\">volume</span>&nbsp;6</b>, Article&nbsp;number:&nbsp;<span data-test=\"article-number\">1</span> (<span data-test=\"article-publication-year\">2019</span>)<a href=\"#citeas\" class=\"c-article-info-details__cite-as u-hide-print\" data-track=\"click\" data-track-action=\"cite this article\" data-track-label=\"link\">Cite this article</a></p>" +
    "    " +  // extra 4 spaces due to the 4 spaces in front of the <div> in the articleHeader
    "  </header>" +
    "</div>";

  private static final String footer =
    "<footer class=\"app-footer\" role=\"contentinfo\"> " +
    "  <div class=\"app-footer__aside-wrapper u-hide-print\">" +
    "    <div class=\"app-footer__container\">" +
    "      <p class=\"app-footer__strapline\">Over 10 million scientific documents at your fingertips</p>" +
    "      <div class=\"app-footer__edition\" data-component=\"SV.EditionSwitcher\">" +
    "        <span class=\"u-visually-hidden\" data-role=\"button-dropdown__title\" data-btn-text=\"Switch between Academic & Corporate Edition\">Switch Edition</span>" +
    "        <ul class=\"app-footer-edition-list\" data-role=\"button-dropdown__content\" data-test=\"footer-edition-switcher-list\">" +
    "          <li class=\"selected\">" +
    "            <a data-test=\"footer-academic-link\"" +
    "               href=\"/siteEdition/link\"" +
    "               id=\"siteedition-academic-link\">Academic Edition</a>" +
    "          </li>" +
    "          <li>" +
    "            <a data-test=\"footer-corporate-link\"" +
    "              href=\"/siteEdition/rd\"" +
    "              id=\"siteedition-corporate-link\">Corporate Edition</a>" +
    "          </li>" +
    "        </ul>" +
    "      </div>" +
    "    </div>" +
    "  </div>" +
    "  <div class=\"app-footer__container\">" +
    "    <ul class=\"app-footer__nav u-hide-print\">" +
    "      <li><a href=\"/\">Home</a></li>" +
    "      <li><a href=\"/impressum\">Impressum</a></li>" +
    "      <li><a href=\"/termsandconditions\">Legal information</a></li>" +
    "      <li><a href=\"/privacystatement\">Privacy statement</a></li>" +
    "      <li><a href=\"https://www.springernature.com/ccpa\">California Privacy Statement</a></li>" +
    "      <li><a href=\"/cookiepolicy\">How we use cookies</a></li>" +
    "      <li><a class=\"optanon-toggle-display\" href=\"javascript:void(0);\">Manage cookies/Do not sell my data</a></li>" +
    "    </ul>" +
    "    <div class=\"c-user-metadata\">" +
    "      <p class=\"c-user-metadata__item\">" +
    "        <span data-test=\"footer-user-login-status\">Not logged in</span>" +
    "        <span data-test=\"footer-user-ip\"> - 171.66.236.212</span>" +
    "      </p>" +
    "      <p class=\"c-user-metadata__item\" data-test=\"footer-business-partners\">" +
    "        North East Research Libraries (8200828607)  - Stanford University (8200924855)  - Stanford University Medical Center, Serials Control (1600134053)  - CLOCKSS (3000682639) " +
    "      </p>" +
    "    </div>" +
    "  </div>" +
    "</footer>" +
    "<span>Hello World</span>";

  private static final String filteredFooter =
    "<span>Hello World</span>";

  
  private static final String asides =
    "<aside class=\"c-ad c-ad--728x90\" data-test=\"springer-doubleclick-ad\">\n" +
    "        <div class=\"c-ad__inner\">\n" +
    "            <p class=\"c-ad__label\">Advertisement</p>\n" +
    "                <div id=\"div-gpt-ad-LB1\" data-gpt-unitpath=\"/270604982/springerlink/138/article\" data-gpt-sizes=\"728x90\" data-gpt-targeting=\"pos=LB1;articleid=s00138-018-00997-4;\" data-google-query-id=\"CJb9s6yjkO8CFQbrwAodKW8H8g\"><div id=\"google_ads_iframe_/270604982/springerlink/138/article_0__container__\" style=\"border: 0pt none;\"><iframe id=\"google_ads_iframe_/270604982/springerlink/138/article_0\" title=\"3rd party ad content\" name=\"google_ads_iframe_/270604982/springerlink/138/article_0\" scrolling=\"no\" marginwidth=\"0\" marginheight=\"0\" style=\"border: 0px none; vertical-align: bottom;\" srcdoc=\"\" data-google-container-id=\"2\" data-load-complete=\"true\" width=\"728\" height=\"90\" frameborder=\"0\"></iframe></div></div>\n" +
    "        </div>\n" +
    "</aside>" +
    "<aside>" +
    "  <div data-test=\"download-article-link-wrapper\">" +
    "    <div class=\"c-pdf-download u-clear-both\">" +
    "      <a href=\"https://link.springer.com/content/pdf/10.1007/s00138-018-00997-4.pdf\" class=\"c-pdf-download__link\" data-article-pdf=\"true\" data-readcube-pdf-url=\"true\" data-test=\"pdf-link\" data-draft-ignore=\"true\" data-track=\"click\" data-track-action=\"download pdf\" data-track-label=\"button\" data-track-external=\"\">" +
    "        <span>Download PDF</span>" +
    "        <svg aria-hidden=\"true\" focusable=\"false\" width=\"16\" height=\"16\" class=\"u-icon\">" +
    "          <use xlink:href=\"#global-icon-download\"></use>" +
    "        </svg>" +
    "      </a>" +
    "    </div>" +
    "  </div>" +
    "  <div data-test=\"collections\"></div>" +
    "  <div data-test=\"editorial-summary\"></div>" +
    "  <div class=\"c-reading-companion\">" +
    "    <div class=\"c-reading-companion__sticky\" data-component=\"reading-companion-sticky\" data-test=\"reading-companion-sticky\" style=\"top: 18px;\">" +
    "      <ul class=\"c-reading-companion__tabs\" role=\"tablist\" style=\"width: 389px;\">" +
    "        <li role=\"presentation\">" +
    "          <button data-tab-target=\"sections\" role=\"tab\" id=\"tab-sections\" aria-controls=\"tabpanel-sections\" aria-selected=\"true\" class=\"c-reading-companion__tab c-reading-companion__tab--active\" data-track=\"click\" data-track-action=\"sections tab\" data-track-label=\"tab\">Sections</button>" +
    "        </li>" +
    "        <li role=\"presentation\">" +
    "          <button data-tab-target=\"figures\" role=\"tab\" id=\"tab-figures\" aria-controls=\"tabpanel-figures\" aria-selected=\"false\" tabindex=\"-1\" class=\"c-reading-companion__tab\" data-track=\"click\" data-track-action=\"figures tab\" data-track-label=\"tab\">Figures</button>" +
    "        </li>" +
    "        <li role=\"presentation\">" +
    "          <button data-tab-target=\"references\" role=\"tab\" id=\"tab-references\" aria-controls=\"tabpanel-references\" aria-selected=\"false\" tabindex=\"-1\" class=\"c-reading-companion__tab\" data-track=\"click\" data-track-action=\"references tab\" data-track-label=\"tab\">References</button>" +
    "        </li>" +
    "      </ul>" +
    "      <div class=\"c-reading-companion__panel c-reading-companion__sections c-reading-companion__panel--active\" id=\"tabpanel-sections\" aria-labelledby=\"tab-sections\" style=\"max-width: 389px;\">" +
    "        <div class=\"c-reading-companion__scroll-pane\" style=\"max-height: 17px;\">" +
    "          <ul class=\"c-reading-companion__sections-list\">" +
    "            <li id=\"rc-sec-Abs1\" class=\"c-reading-companion__section-item c-reading-companion__section-item--active\">" +
    "              <a href=\"#Abs1\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Abstract\">Abstract</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Sec1\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Sec1\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Introduction\">Introduction</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Sec2\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Sec2\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Related work\">Related work</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Sec5\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Sec5\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Gaze estimation from eye appearance\">Gaze estimation from eye appearance</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Sec9\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Sec9\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Practical issues\">Practical issues</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Sec12\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Sec12\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Practical eyetyping experiment\">Practical eye-typing experiment</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Sec13\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Sec13\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Discussion\">Discussion</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Sec14\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Sec14\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Conclusion and future work\">Conclusion and future work</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Bib1\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Bib1\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:References\">References</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-Ack1\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#Ack1\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Acknowledgements\">Acknowledgements</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-author-information\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#author-information\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Author information\">Author information</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-additional-information\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#additional-information\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Additional information\">Additional information</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-rightslink\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#rightslink\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Rights and permissions\">Rights and permissions</a>" +
    "            </li>" +
    "            <li id=\"rc-sec-article-info\" class=\"c-reading-companion__section-item\">" +
    "              <a href=\"#article-info\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:About this article\">About this article</a>" +
    "            </li>" +
    "          </ul>" +
    "        </div>" +
    "        <div class=\"js-ad\">" +
    "          <aside class=\"c-ad c-ad--300x250\">" +
    "            <div class=\"c-ad__inner\">" +
    "              <p class=\"c-ad__label\">Advertisement</p>" +
    "              <div id=\"div-gpt-ad-MPU1\" data-gpt-unitpath=\"/270604982/springerlink/138/article\" data-gpt-sizes=\"300x250\" data-gpt-targeting=\"pos=MPU1;articleid=s00138-018-00997-4;\" data-google-query-id=\"CN2msayjkO8CFYfgwAodVcMB3w\">" +
    "                <div id=\"google_ads_iframe_/270604982/springerlink/138/article_1__container__\" style=\"border: 0pt none;\">" +
    "                  <iframe id=\"google_ads_iframe_/270604982/springerlink/138/article_1\" title=\"3rd party ad content\" name=\"google_ads_iframe_/270604982/springerlink/138/article_1\" scrolling=\"no\" marginwidth=\"0\" marginheight=\"0\" style=\"border: 0px none; vertical-align: bottom;\" srcdoc=\"\" data-google-container-id=\"1\" data-load-complete=\"true\" width=\"300\" height=\"250\" frameborder=\"0\"></iframe>" +
    "                </div>" +
    "              </div>" +
    "            </div>" +
    "          </aside>" +
    "        </div>" +
    "      </div>" +
    "    </div>" +
    "  </div>" +
    "</aside>";

  private static final String filteredAsides =
      "<aside>" +
      "  <div data-test=\"download-article-link-wrapper\">" +
      "    <div class=\"c-pdf-download u-clear-both\">" +
      "      <a href=\"https://link.springer.com/content/pdf/10.1007/s00138-018-00997-4.pdf\" class=\"c-pdf-download__link\" data-article-pdf=\"true\" data-readcube-pdf-url=\"true\" data-test=\"pdf-link\" data-draft-ignore=\"true\" data-track=\"click\" data-track-action=\"download pdf\" data-track-label=\"button\" data-track-external=\"\">" +
      "        <span>Download PDF</span>" +
      "        <svg aria-hidden=\"true\" focusable=\"false\" width=\"16\" height=\"16\" class=\"u-icon\">" +
      "          <use xlink:href=\"#global-icon-download\"></use>" +
      "        </svg>" +
      "      </a>" +
      "    </div>" +
      "  </div>" +
      "  <div data-test=\"collections\"></div>" +
      "  <div data-test=\"editorial-summary\"></div>" +
      "  <div class=\"c-reading-companion\">" +
      "    <div class=\"c-reading-companion__sticky\" data-component=\"reading-companion-sticky\" data-test=\"reading-companion-sticky\" style=\"top: 18px;\">" +
      "      <ul class=\"c-reading-companion__tabs\" role=\"tablist\" style=\"width: 389px;\">" +
      "        <li role=\"presentation\">" +
      "          <button data-tab-target=\"sections\" role=\"tab\" id=\"tab-sections\" aria-controls=\"tabpanel-sections\" aria-selected=\"true\" class=\"c-reading-companion__tab c-reading-companion__tab--active\" data-track=\"click\" data-track-action=\"sections tab\" data-track-label=\"tab\">Sections</button>" +
      "        </li>" +
      "        <li role=\"presentation\">" +
      "          <button data-tab-target=\"figures\" role=\"tab\" id=\"tab-figures\" aria-controls=\"tabpanel-figures\" aria-selected=\"false\" tabindex=\"-1\" class=\"c-reading-companion__tab\" data-track=\"click\" data-track-action=\"figures tab\" data-track-label=\"tab\">Figures</button>" +
      "        </li>" +
      "        <li role=\"presentation\">" +
      "          <button data-tab-target=\"references\" role=\"tab\" id=\"tab-references\" aria-controls=\"tabpanel-references\" aria-selected=\"false\" tabindex=\"-1\" class=\"c-reading-companion__tab\" data-track=\"click\" data-track-action=\"references tab\" data-track-label=\"tab\">References</button>" +
      "        </li>" +
      "      </ul>" +
      "      <div class=\"c-reading-companion__panel c-reading-companion__sections c-reading-companion__panel--active\" id=\"tabpanel-sections\" aria-labelledby=\"tab-sections\" style=\"max-width: 389px;\">" +
      "        <div class=\"c-reading-companion__scroll-pane\" style=\"max-height: 17px;\">" +
      "          <ul class=\"c-reading-companion__sections-list\">" +
      "            <li id=\"rc-sec-Abs1\" class=\"c-reading-companion__section-item c-reading-companion__section-item--active\">" +
      "              <a href=\"#Abs1\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Abstract\">Abstract</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Sec1\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Sec1\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Introduction\">Introduction</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Sec2\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Sec2\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Related work\">Related work</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Sec5\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Sec5\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Gaze estimation from eye appearance\">Gaze estimation from eye appearance</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Sec9\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Sec9\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Practical issues\">Practical issues</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Sec12\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Sec12\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Practical eyetyping experiment\">Practical eye-typing experiment</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Sec13\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Sec13\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Discussion\">Discussion</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Sec14\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Sec14\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Conclusion and future work\">Conclusion and future work</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Bib1\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Bib1\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:References\">References</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-Ack1\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#Ack1\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Acknowledgements\">Acknowledgements</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-author-information\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#author-information\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Author information\">Author information</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-additional-information\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#additional-information\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Additional information\">Additional information</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-rightslink\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#rightslink\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:Rights and permissions\">Rights and permissions</a>" +
      "            </li>" +
      "            <li id=\"rc-sec-article-info\" class=\"c-reading-companion__section-item\">" +
      "              <a href=\"#article-info\" data-track=\"click\" data-track-action=\"section anchor\" data-track-label=\"link:About this article\">About this article</a>" +
      "            </li>" +
      "          </ul>" +
      "        </div>" +
      "        <div class=\"js-ad\">" +
      "          " + // removed aside section
      "        </div>" +
      "      </div>" +
      "    </div>" +
      "  </div>" +
      "</aside>";
  
  /*
   *  Compare Html and HtmlHashFiltered
   */
  public void testOverlay() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(references), Constants.DEFAULT_ENCODING);
    //System.out.printf("["+StringUtil.fromInputStream(actIn) + "]");
    //System.out.printf(referencesFiltered);
    //assertEquals(referencesFiltered, StringUtil.fromInputStream(actIn));
  }

  public void testHeader() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(articleHeader), Constants.DEFAULT_ENCODING);
    assertEquals(filteredArticleHeader, StringUtil.fromInputStream(actIn));
  }

  public void testSidebar() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(asides), Constants.DEFAULT_ENCODING);
    assertEquals(filteredAsides, StringUtil.fromInputStream(actIn));
  }

  public void testFooter() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(footer), Constants.DEFAULT_ENCODING);
    assertEquals(filteredFooter, StringUtil.fromInputStream(actIn));
  }


}