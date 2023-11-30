package org.lockss.plugin.atypon.aaas;

import org.lockss.daemon.PluginException;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestAaasHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AaasHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AaasHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }

  public String filterString(String rawHtml) throws IOException {
    InputStream actIn = null;
    try {
      actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(rawHtml),
          Constants.DEFAULT_ENCODING);
    } catch (PluginException e) {
      throw new RuntimeException(e);
    }
    log.info("======result=====");
    log.info(StringUtil.fromInputStream(actIn));
    return StringUtil.fromInputStream(actIn);
  }

 //From page https://www.science.org/doi/10.1126/scisignal.aav3810 need to filter out
 // overcrawled url: https://www.science.org/doi/10.1126/scisignal.aav3810
  String prev_next_nagivation_links =
          "<nav title=\"Content Navigation\" class=\"content-navigation\">\n" +
          "   <a href=\"/doi/10.1126/sciadv.adf5509\" title=\"An optical aptasensor for real-time quantification of endotoxin: From ensemble to single-molecule resolution\" class=\"content-navigation__prev\">\n" +
          "      <div aria-hidden=\"true\" class=\"content-navigation__hint\">\n" +
          "         <div class=\"content-navigation__hint__content\">\n" +
          "            <h6>PREVIOUS ARTICLE</h6>\n" +
          "            <div>An optical aptasensor for real-time quantification of endotoxin: From ensemble to single-molecule resolution</div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "      <i aria-hidden=\"true\" class=\"icon-arrow-left\"></i><span>contentNavigation.previous.default</span>\n" +
          "   </a>\n" +
          "   <a href=\"/doi/10.1126/sciadv.abo6405\" title=\"A highly specific CRISPR-Cas12j nuclease enables allele-specific genome editing\" class=\"content-navigation__next\">\n" +
          "      <div aria-hidden=\"true\" class=\"content-navigation__hint\">\n" +
          "         <div class=\"content-navigation__hint__content\">\n" +
          "            <h6>NEXT ARTICLE</h6>\n" +
          "            <div>A highly specific CRISPR-Cas12j nuclease enables allele-specific genome editing</div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "      <span>contentNavigation.next.default</span><i aria-hidden=\"true\" class=\"icon-arrow-right\"></i>\n" +
          "   </a>\n" +
          "</nav>";

  String prev_next_nagivation_links_filtered = "";

  String related_content = "" +
          "<div role=\"listitem\" class=\"related-item p-3\">\n" +
          "   <div role=\"heading\" class=\"related-item__heading\">This article has a correction.</div>\n" +
          "   <div class=\"related-item__content\"><span>Please see:&nbsp;</span><a href=\"/doi/10.1126/scisignal.abb5851\"><span>Erratum for the Research Article: “Biased M<sub>1</sub> receptor–positive allosteric modulators reveal role of phospholipase D in M<sub>1</sub>-dependent rodent cortical plasticity” by S. P. Moran, Z. Xiang, C. A. Doyle, J. Maksymetz, X. Lv, S. Faltin, N. M. Fisher, C. M. Niswender, J. M. Rook, C.W. Lindsley, P. J. Conn -<time datetime=\"Mar 17, 2020, 12:00:00 AM\"> 17 March 2020</time></span></a></div>\n" +
          "</div>";

  String related_content_filtered = "";

  String aside_content = "" +
            "<aside data-core-aside=\"right-rail\">\n" +
            "\t<section>\n" +
            "\t   <h4 class=\"main-title-2--decorated h4\">LATEST NEWS</h4>\n" +
            "\t   <div class=\"mt-4\">\n" +
            "\t\t  <div class=\"multi-search\">\n" +
            "\t\t\t <div class=\"multi-search--news-article-aside\">\n" +
            "\t\t\t\t<article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
            "\t\t\t\t   <div class=\"card-content\">\n" +
            "\t\t\t\t\t  <div class=\"card-meta text-uppercase\">\n" +
            "\t\t\t\t\t\t <span class=\"card-meta__category\"><a href=\"/news/all-news\" class=\"text-decoration-none\"><span>News</span></a></span><time class=\"border-left\">8 Nov 2023</time>\n" +
            "\t\t\t\t\t  </div>\n" +
            "\t\t\t\t\t  <div class=\"card-header mb-2\">\n" +
            "\t\t\t\t\t\t <a href=\"/content/article/lice-dna-records-moment-europeans-colonized-americas\" title=\"Lice DNA records the moment Europeans colonized the Americas\" class=\"text-reset animation-underline\">\n" +
            "\t\t\t\t\t\t Lice DNA records the moment Europeans colonized the Americas\n" +
            "\t\t\t\t\t\t </a>\n" +
            "\t\t\t\t\t  </div>\n" +
            "\t\t\t\t   </div>\n" +
            "\t\t\t\t</article>\n" +
            "\t\t\t </div>\n" +
            "\t\t  </div>\n" +
            "\t   </div>\n" +
            "\t</section>\n" +
            " </aside>";

  String aside_content_filtered = "";

  String aside_content_long = "" +
          "<aside data-core-aside=\"right-rail\">\n" +
          "   <section>\n" +
          "      <div class=\"current-issue-aside\">\n" +
          "         <h4 class=\"h4 main-title-2--decorated\">\n" +
          "            Current Issue\n" +
          "         </h4>\n" +
          "         <div class=\"current-issue-aside__content\">\n" +
          "            <div class=\"current-issue-aside__cover\">\n" +
          "               <div class=\"cover-image flat d-flex justify-content-center flex-column\">\n" +
          "                  <div data-is-viewable=\"true\" class=\"cover-image__image\"><a href=\"/toc/science/382/6670\" title=\"View Science current issue\" class=\"d-block\"><img src=\"/cdn-cgi/image/width=400/cms/asset/a294780c-5e0a-4503-a718-7878d32cfd8b/science.2023.382.issue-6670.largecover.jpg\" alt=\"Science cover image\"></a></div>\n" +
          "               </div>\n" +
          "            </div>\n" +
          "            <div class=\"current-issue-aside__multisearch pt-0 pt-md-3 pt-xl-0 pl-0 pl-md-3 pl-xl-0\">\n" +
          "               <div class=\"multi-search\">\n" +
          "                  <div>\n" +
          "                     <article class=\"card-do\">\n" +
          "                        <div class=\"card-content\">\n" +
          "                           <div class=\"card-header\">\n" +
          "                              <h3 class=\"card__title\"><a href=\"/doi/10.1126/science.abp9201\" title=\"Formaldehyde regulates <i>S</i>-adenosylmethionine biosynthesis and one-carbon metabolism\" class=\"text-reset animation-underline\">Formaldehyde regulates <i>S</i>-adenosylmethionine biosynthesis and one-carbon metabolism</a></h3>\n" +
          "                              <ul class=\"card-meta align-middle pl-0\">\n" +
          "                                 <li class=\"card-contribs text-uppercase comma-separated mb-0\" data-visible-items-sm=\"2\" data-visible-items-md=\"4\" data-visible-items=\"9\" data-truncate-less=\"less\" data-truncate-more=\"authors\" data-truncate-dots=\"true\">\n" +
          "                                    <span>By</span>\n" +
          "                                    <ul class=\"list-inline comma-separated d-inline\" title=\"list of authors\">\n" +
          "                                       <li class=\"list-inline-item\"><span class=\"hlFld-ContribAuthor\">Vanha N. Pham</span></li>\n" +
          "                                       <li class=\"list-inline-item\"><span class=\"hlFld-ContribAuthor\">Kevin J. Bruemmer</span></li>\n" +
          "                                       <li class=\"list-inline-item\"><span><em>et al.</em></span></li>\n" +
          "                                    </ul>\n" +
          "                                 </li>\n" +
          "                              </ul>\n" +
          "                           </div>\n" +
          "                        </div>\n" +
          "                     </article>\n" +
          "                     <article class=\"card-do\">\n" +
          "                        <div class=\"card-content\">\n" +
          "                           <div class=\"card-header\">\n" +
          "                              <h3 class=\"card__title\"><a href=\"/doi/10.1126/science.adf1046\" title=\"Sex-biased gene expression across mammalian organ development and evolution\" class=\"text-reset animation-underline\">Sex-biased gene expression across mammalian organ development and evolution</a></h3>\n" +
          "                              <ul class=\"card-meta align-middle pl-0\">\n" +
          "                                 <li class=\"card-contribs text-uppercase comma-separated mb-0\" data-visible-items-sm=\"2\" data-visible-items-md=\"4\" data-visible-items=\"9\" data-truncate-less=\"less\" data-truncate-more=\"authors\" data-truncate-dots=\"true\">\n" +
          "                                    <span>By</span>\n" +
          "                                    <ul class=\"list-inline comma-separated d-inline\" title=\"list of authors\">\n" +
          "                                       <li class=\"list-inline-item\"><span class=\"hlFld-ContribAuthor\">Leticia Rodríguez-Montes</span></li>\n" +
          "                                       <li class=\"list-inline-item\"><span class=\"hlFld-ContribAuthor\">Svetlana Ovchinnikova</span></li>\n" +
          "                                       <li class=\"list-inline-item\"><span><em>et al.</em></span></li>\n" +
          "                                    </ul>\n" +
          "                                 </li>\n" +
          "                              </ul>\n" +
          "                           </div>\n" +
          "                        </div>\n" +
          "                     </article>\n" +
          "                     <article class=\"card-do\">\n" +
          "                        <div class=\"card-content\">\n" +
          "                           <div class=\"card-header\">\n" +
          "                              <h3 class=\"card__title\"><a href=\"/doi/10.1126/science.adl1522\" title=\"Make the upcoming IPCC Cities Special Report count\" class=\"text-reset animation-underline\">Make the upcoming IPCC Cities Special Report count</a></h3>\n" +
          "                              <ul class=\"card-meta align-middle pl-0\">\n" +
          "                                 <li class=\"card-contribs text-uppercase comma-separated mb-0\" data-visible-items-sm=\"2\" data-visible-items-md=\"4\" data-visible-items=\"9\" data-truncate-less=\"less\" data-truncate-more=\"authors\" data-truncate-dots=\"true\">\n" +
          "                                    <span>By</span>\n" +
          "                                    <ul class=\"list-inline comma-separated d-inline\" title=\"list of authors\">\n" +
          "                                       <li class=\"list-inline-item\"><span class=\"hlFld-ContribAuthor\">Xuemei Bai</span></li>\n" +
          "                                    </ul>\n" +
          "                                 </li>\n" +
          "                              </ul>\n" +
          "                           </div>\n" +
          "                        </div>\n" +
          "                     </article>\n" +
          "                  </div>\n" +
          "                  <div class=\"d-flex justify-content-end btn--more__wrapper\"><a href=\"/toc/science/current\" class=\"btn btn--more animation-icon-shift text-uppercase font-weight-bold\"><span>Table of Contents</span><span aria-hidden=\"true\" class=\"icon-arrow-right\"></span></a></div>\n" +
          "               </div>\n" +
          "            </div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "   </section>\n" +
          "   <div class=\"aside-ads\">\n" +
          "      <div data-widget-def=\"literatumAd\" data-widget-id=\"d9c37dd2-7a6d-4012-aeb1-014af2d32163\" class=\"text-center\">\n" +
          "         <div class=\"pb-ad\">\n" +
          "            <div class=\"adplaceholder exists\">\n" +
          "               <h4 class=\"adplaceholder__title overline text-uppercase\">Advertisement</h4>\n" +
          "               <div id=\"div-gpt-ad-rectangle\" class=\"weby-gam\" network_code=\"21824331052\" data-google-query-id=\"COaJo_rRtYIDFcI0RAgdXD8DDA\">\n" +
          "                  <div id=\"google_ads_iframe_/21824331052/science/journal_1__container__\" style=\"border: 0pt; display: inline-block; width: 300px; height: 250px;\"><iframe frameborder=\"0\" style=\"border: 0px; vertical-align: bottom;\" src=\"https://d8a4576dddd1572fbdef27f630e2a173.safeframe.googlesyndication.com/safeframe/1-0-40/html/container.html\" id=\"google_ads_iframe_/21824331052/science/journal_1\" title=\"3rd party ad content\" name=\"\" scrolling=\"no\" marginwidth=\"0\" marginheight=\"0\" width=\"300\" height=\"250\" data-is-safeframe=\"true\" sandbox=\"allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts allow-top-navigation-by-user-activation\" role=\"region\" aria-label=\"Advertisement\" tabindex=\"0\" data-google-container-id=\"2\" data-load-complete=\"true\"></iframe></div>\n" +
          "               </div>\n" +
          "            </div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "      <div class=\"mt-2x\">\n" +
          "         <div class=\"mb-2x mb-xl-3x\">\n" +
          "            <div class=\"d-flex flex-column align-items-center align-items-xl-start\">\n" +
          "               <h3 class=\"h6 mb-2 text-primary\">Sign up for ScienceAdviser</h3>\n" +
          "               <p class=\"text-sm letter-spacing-default text-center text-xl-left mb-1x\">Subscribe to <cite>Science</cite>Adviser to get the latest news, commentary, and research, free to your inbox daily.</p>\n" +
          "               <a href=\"/content/page/scienceadviser?intcmp=rrail-adviser&amp;utm_id=recFUzjFNRznSEEDd\" class=\"btn btn-outline-primary btn--connect pl-1x\">\n" +
          "               <span class=\"text-xxs\">Subscribe</span>\n" +
          "               <i aria-hidden=\"true\" class=\"h4 icon-arrow-right ml-1\"></i>\n" +
          "               </a>\n" +
          "            </div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "   </div>\n" +
          "   <section>\n" +
          "      <h4 class=\"main-title-2--decorated h4\">LATEST NEWS</h4>\n" +
          "      <div class=\"mt-4\">\n" +
          "         <div class=\"multi-search\">\n" +
          "            <div class=\"multi-search--news-article-aside\">\n" +
          "               <article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta text-uppercase\">\n" +
          "                        <span class=\"card-meta__category\"><a href=\"/news/all-news\" class=\"text-decoration-none\"><span>News</span></a></span><time class=\"border-left\">8 Nov 2023</time>\n" +
          "                     </div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <a href=\"/content/article/lice-dna-records-moment-europeans-colonized-americas\" title=\"Lice DNA records the moment Europeans colonized the Americas\" class=\"text-reset animation-underline\">\n" +
          "                        Lice DNA records the moment Europeans colonized the Americas\n" +
          "                        </a>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta text-uppercase\">\n" +
          "                        <span class=\"card-meta__category\"><a href=\"/news/all-news\" class=\"text-decoration-none\"><span>News</span></a></span><time class=\"border-left\">8 Nov 2023</time>\n" +
          "                     </div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <a href=\"/content/article/social-media-addictive-digital-detox-study-suggests-not\" title=\"Is social media addictive? ‘Digital detox’ study suggests not\" class=\"text-reset animation-underline\">\n" +
          "                        Is social media addictive? ‘Digital detox’ study suggests not\n" +
          "                        </a>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta text-uppercase\">\n" +
          "                        <span class=\"card-meta__category\"><a href=\"/news/scienceinsider\" class=\"text-decoration-none\"><span>ScienceInsider</span></a></span><time class=\"border-left\">8 Nov 2023</time>\n" +
          "                     </div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <a href=\"/content/article/how-many-americans-are-disabled-proposed-census-changes-would-greatly-decrease-count\" title=\"How many in the U.S. are disabled? Proposed census changes would greatly decrease count\" class=\"text-reset animation-underline\">\n" +
          "                        How many in the U.S. are disabled? Proposed census changes would greatly decrease count\n" +
          "                        </a>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta text-uppercase\">\n" +
          "                        <span class=\"card-meta__category\"><a href=\"/news/all-news\" class=\"text-decoration-none\"><span>News</span></a></span><time class=\"border-left\">8 Nov 2023</time>\n" +
          "                     </div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <a href=\"/content/article/new-antifungal-kills-without-toxic-side-effects\" title=\"New antifungal kills without toxic side effects\" class=\"text-reset animation-underline\">\n" +
          "                        New antifungal kills without toxic side effects\n" +
          "                        </a>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta text-uppercase\">\n" +
          "                        <span class=\"card-meta__category\"><a href=\"/news/all-news\" class=\"text-decoration-none\"><span>News</span></a></span><time class=\"border-left\">8 Nov 2023</time>\n" +
          "                     </div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <a href=\"/content/article/synthetic-yeast-project-unveils-cells-50-artificial-dna\" title=\"Synthetic yeast project unveils cells with 50% artificial DNA\" class=\"text-reset animation-underline\">\n" +
          "                        Synthetic yeast project unveils cells with 50% artificial DNA\n" +
          "                        </a>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta text-uppercase\">\n" +
          "                        <span class=\"card-meta__category\"><a href=\"/news/scienceinsider\" class=\"text-decoration-none\"><span>ScienceInsider</span></a></span><time class=\"border-left\">7 Nov 2023</time>\n" +
          "                     </div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <a href=\"/content/article/u-s-senate-confirms-monica-bertagnolli-nih-director\" title=\"U.S. Senate confirms Monica Bertagnolli as NIH director\" class=\"text-reset animation-underline\">\n" +
          "                        U.S. Senate confirms Monica Bertagnolli as NIH director\n" +
          "                        </a>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "            </div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "   </section>\n" +
          "   <div class=\"aside-ads\">\n" +
          "      <div data-widget-def=\"literatumAd\" data-widget-id=\"66ba5b6d-db27-46b8-845b-17b7688f490c\" class=\"text-center\">\n" +
          "         <div class=\"pb-ad\">\n" +
          "            <div class=\"adplaceholder exists\">\n" +
          "               <h4 class=\"adplaceholder__title overline text-uppercase\">Advertisement</h4>\n" +
          "               <div id=\"div-gpt-ad-tower\" class=\"weby-gam\" network_code=\"21824331052\" data-google-query-id=\"CLLRhvzRtYIDFSUxRAgd_UgDEg\">\n" +
          "                  <div id=\"google_ads_iframe_/21824331052/science/journal_2__container__\" style=\"border: 0pt;\"><iframe id=\"google_ads_iframe_/21824331052/science/journal_2\" name=\"google_ads_iframe_/21824331052/science/journal_2\" title=\"3rd party ad content\" width=\"300\" height=\"600\" scrolling=\"no\" marginwidth=\"0\" marginheight=\"0\" frameborder=\"0\" style=\"border: 0px; vertical-align: bottom;\" role=\"region\" aria-label=\"Advertisement\" tabindex=\"0\" data-google-container-id=\"3\" data-load-complete=\"true\"></iframe></div>\n" +
          "               </div>\n" +
          "            </div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "   </div>\n" +
          "   <section class=\"mt-0\">\n" +
          "      <section class=\"mt-0\">\n" +
          "         <div class=\"hawkeye-side-position-node\"></div>\n" +
          "      </section>\n" +
          "      <section><iframe src=\"//hawkeye4.semetricmedia.com/widgets-v4/25/80.html\" style=\"width: 100%; border: none; height:350px; background: 0; overflow: hidden\" frameborder=\"0\" id=\"mdgxWidgetiFrameLg\"></iframe></section>\n" +
          "   </section>\n" +
          "   <section>\n" +
          "      <div class=\"js-intersection-placeholder\" id=\"pop-after-passed-placeholder-8681\"></div>\n" +
          "      <div class=\"show-recommended related-content pop-notification pop-after-passed\" data-visible-after=\"40%\" aria-hidden=\"true\">\n" +
          "         <div class=\"show-recommended related-content pop-notification pop-after-passed\" data-visible-after=\"40%\" aria-hidden=\"true\">\n" +
          "            <h2 class=\"main-title-2--decorated h4 mb-3 d-flex align-items-center\"><span class=\"flex-fill\">Recommended</span><a href=\"#\" aria-controls=\".pop-notification.related-content\" class=\"text-gray pop-notification__close\"><i aria-hidden=\"true\" class=\"icon-close\"></i><span class=\"sr-only\">Close</span></a></h2>\n" +
          "            <div data-exp-type=\"\" data-query-id=\"\" id=\"id4544\">\n" +
          "               <div class=\"pb-dropzone\" data-pb-dropzone=\"Extra content\" title=\"Extra content\"></div>\n" +
          "               <article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Editorial</span></span><time class=\"text-uppercase border-left\">December 2018</time></div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <div class=\" \"><a href=\"/doi/full/10.1126/science.aaw2116\" title=\"Choices in the climate commons\" class=\"text-reset animation-underline\">Choices in the climate commons</a></div>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Policy Forum</span></span><time class=\"text-uppercase border-left\">September 2009</time></div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <div class=\" \"><a href=\"/doi/full/10.1126/science.1175325\" title=\"Looming Global-Scale Failures and Missing Institutions\" class=\"text-reset animation-underline\">Looming Global-Scale Failures and Missing Institutions</a></div>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Special Reviews</span></span><time class=\"text-uppercase border-left\">December 2003</time></div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <div class=\" \"><a href=\"/doi/full/10.1126/science.1091015\" title=\"The Struggle to Govern the Commons\" class=\"text-reset animation-underline\">The Struggle to Govern the Commons</a></div>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Policy Forum</span></span><time class=\"text-uppercase border-left\">December 2018</time></div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <div class=\" \"><a href=\"/doi/full/10.1126/science.aaw0911\" title=\"Tragedy revisited\" class=\"text-reset animation-underline\">Tragedy revisited</a></div>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \">\n" +
          "                  <div class=\"card-content\">\n" +
          "                     <div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Letter</span></span><time class=\"text-uppercase border-left\">October 2017</time></div>\n" +
          "                     <div class=\"card-header mb-2\">\n" +
          "                        <div class=\" \"><a href=\"/doi/full/10.1126/science.aap9964\" title=\"Sand in demand: Trapped behind dams\" class=\"text-reset animation-underline\">Sand in demand: Trapped behind dams</a></div>\n" +
          "                     </div>\n" +
          "                  </div>\n" +
          "               </article>\n" +
          "               <div class=\"pb-dropzone\" data-pb-dropzone=\"Show More link\" title=\"Show More link\"></div>\n" +
          "            </div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "   </section>\n" +
          "   <div class=\"aside-ads sticky-ads\">\n" +
          "      <div data-widget-def=\"literatumAd\" data-widget-id=\"5c8e486e-29f1-446a-a3ca-103184a6ac90\" class=\"text-center\">\n" +
          "         <div class=\"pb-ad\">\n" +
          "            <div class=\"adplaceholder exists\">\n" +
          "               <h4 class=\"adplaceholder__title overline text-uppercase\">Advertisement</h4>\n" +
          "               <div id=\"div-gpt-ad-rectangle-sticky\" class=\"weby-gam\" network_code=\"21824331052\">\n" +
          "                  <script type=\"text/javascript\">googletag.cmd.push(function() { googletag.display('div-gpt-ad-rectangle-sticky'); });</script>\n" +
          "               </div>\n" +
          "            </div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "   </div>\n" +
          "</aside>";

  String aside_content_long_filtered = "";


    String related_content2 = "" +
            "<div class=\"core-container\">\n" +
            "                              <!-- Group by presentation type--><!-- Render important items first-->\n" +
            "                              <div role=\"listitem\" class=\"related-item p-3\">\n" +
            "                                 <div role=\"heading\" class=\"related-item__heading\"></div>\n" +
            "                                 <div class=\"related-item__content\"><span>Please see:&nbsp;</span><a href=\"/doi/10.1126/science.aap9964\"><span>Sand in demand: Trapped behind dams -<time datetime=\"Oct 13, 2017, 12:00:00 AM\"> 13 October 2017</time></span></a></div>\n" +
            "                              </div>\n" +
            "                              <div role=\"listitem\" class=\"related-item p-3\">\n" +
            "                                 <div role=\"heading\" class=\"related-item__heading\"></div>\n" +
            "                                 <div class=\"related-item__content\"><span>Please see:&nbsp;</span><a href=\"/doi/10.1126/science.aar3388\"><span>Greenland: Build an economy on sand -<time datetime=\"Nov 17, 2017, 12:00:00 AM\"> 17 November 2017</time></span></a></div>\n" +
            "                              </div>\n" +
            "                              <!-- Group remaining items by label-->\n" +
            "                           </div>\n" +
            "                        </div>";

    String related_content2_filtered = "";

  public void testPrevNextFiltering() throws Exception {
        //assertEquals(prev_next_nagivation_links_filtered, filterString(prev_next_nagivation_links));
  }

    public void testRelatedContentFiltering() throws Exception {
        //assertEquals(related_content_filtered, filterString(related_content));
    }

    public void testRelatedContent2Filtering() throws Exception {
        assertEquals(related_content2_filtered, filterString(related_content2));
    }

    public void testAsideContentFiltering() throws Exception {
        //assertEquals(aside_content_filtered, filterString(aside_content));
    }

    public void testAsideContentLongFiltering() throws Exception {
        //assertEquals(aside_content_long_filtered, filterString(aside_content_long));
    }
}
