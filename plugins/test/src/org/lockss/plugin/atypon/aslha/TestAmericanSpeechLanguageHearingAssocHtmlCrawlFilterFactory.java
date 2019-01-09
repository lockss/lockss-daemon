/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the \"Software\"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/


package org.lockss.plugin.atypon.aslha;

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
import org.apache.commons.io.FileUtils;


public class TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory extends LockssTestCase {

  FilterFactory variantCrawlFact = new AmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory();
  FilterFactory variantHashFact = new AmericanSpeechLanguageHearingAssocHtmlHashFilterFactory();
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;

  private static final String PLUGIN_ID =
          "org.lockss.plugin.atypon.americanspeechlanguagehearingassoc.AmericanSpeechLanguageHearingAssocAtyponPlugin";

  private static final String manifestContent =
      "<html>\n" +
              "<head>\n" +
              "    <title>American Journal of Audiology 2018 CLOCKSS Manifest Page</title>\n" +
              "    <meta charset=\"UTF-8\" />\n" +
              "</head>\n" +
              "<body>\n" +
              "<h1>American Journal of Audiology 2018 CLOCKSS Manifest Page</h1>\n" +
              "<ul>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/4\">December 2018 (Vol. 27 Issue 4)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/3S\">November 2018 (Vol. 27 Issue 3S)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/3\">September 2018 (Vol. 27 Issue 3)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/2\">June 2018 (Vol. 27 Issue 2)</a></li>\n" +
              "    \n" +
              "    <li><a href=\"/toc/aja/27/1\">March 2018 (Vol. 27 Issue 1)</a></li>\n" +
              "    \n" +
              "</ul>\n" +
              "<p>\n" +
              "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>\n" +
              "    The CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.\n" +
              "</p>\n" +
              "</body>\n" +
              "</html>";

  private static final String manifestHashFiltered = " December 2018 (Vol. 27 Issue 4) November 2018 (Vol. 27 Issue 3S) " +
          "September 2018 (Vol. 27 Issue 3) June 2018 (Vol. 27 Issue 2) March 2018 (Vol. 27 Issue 1) ";

  private static String tocContent =
          "<html lang=\"en\">\n" +
          "<head>\n" +
          "head section content\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <header class=\"header fixed\">\n" +
          "            some content\n" +
          "        </header>\n" +
          "        <main class=\"content toc-page journal-branding\">\n" +
          "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"38797f92-e1ab-41a7-860c-a348de7ce2b5\" class=\"page-top-banner\">\n" +
          "                <div class=\"container\">\n" +
          "                    <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"19bb39c8-91f2-4e6d-a97c-ef3a4939b143\" class=\"row title__row\">\n" +
          "                        <div class=\"col-xs-12\">\n" +
          "                            <h2 class=\"page__title\">Table of contents</h2>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"83f77ad6-f12e-4174-8da6-e631600dbba0\" class=\"row\">\n" +
          "                    <div class=\"col-xs-12\">\n" +
          "                        <div class=\"publication__menu\">\n" +
          "                            <div class=\"publication__menu__journal__logo\">\n" +
          "                                <div data-widget-def=\"ux3-general-image\" data-widget-id=\"edb63f56-ec4c-40c9-9183-5aa9439ae422\" class=\"publication__menu__journal__logo\">\n" +
          "                                    <a href=\"/journal/aja\" title=\"aja Journal\"><img alt=\"aja Journal\" src=\"/pb-assets/images/logos/journal_aja-1541785550173.png\"/></a>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <button data-ctrl-res=\"screen-lg\" data-slide-target=\".publication__menu .publication__menu__list\" data-slide-clone=\"self\" class=\"w-slide__btn publication__nav__toggler\"><span class=\"icon-list\"><span class=\"sr-only open\">Menu</span></span></button>\n" +
          "                            <ul class=\"rlist publication__menu__list\">\n" +
          "                                <li><a href=\"/journal/aja\" title=\"HOME\" class=\"publication__menu__link\">HOME</a></li>\n" +
          "                                <li><a href=\"/loi/aja\" title=\"ISSUES\" class=\"publication__menu__link\">ISSUES</a></li>\n" +
          "                                <li><a href=\"/toc/aja/0/0\" title=\"NEWLY PUBLISHED\" class=\"publication__menu__link\">NEWLY PUBLISHED</a></li>\n" +
          "                                <li><a href=\"/aja/subscribe\" title=\"SUBSCRIBE\" class=\"publication__menu__link\">SUBSCRIBE</a></li>\n" +
          "                                <li><a href=\"/aja/forlibrarians\" title=\"RECOMMEND TO A LIBRARIAN\" class=\"publication__menu__link\">RECOMMEND TO A LIBRARIAN</a></li>\n" +
          "                            </ul>\n" +
          "                            <div data-widget-def=\"UX3QuickSearchWidget\" data-widget-id=\"4fd0df5b-af53-47a2-9997-be3bf24fb17b\" class=\"journal-search\">\n" +
          "                                <div class=\"quick-search quick-search--journal\">\n" +
          "                                    <div class=\"full-width\">\n" +
          "                                        <a href=\"#\" title=\"search\" data-db-target-for=\"thisJournalQuickSearch\" class=\"quick-search__toggler lg-hidden\"><i aria-hidden=\"true\" class=\"block-icon icon-search\"></i></a>\n" +
          "                                        <div data-db-target-of=\"thisJournalQuickSearch\" class=\"dropBlock__holder quick-search__dropBlock lg-opened\">\n" +
          "                                            <form action=\"/action/doSearch\" name=\"thisJournalQuickSearch\" method=\"get\" title=\"Quick Search\">\n" +
          "                                                <div class=\"input-group\"><input type=\"search\" name=\"text1\" placeholder=\"Search\" data-auto-complete-max-words=\"7\" data-auto-complete-max-chars=\"32\" data-contributors-conf=\"3\" data-topics-conf=\"3\" data-publication-titles-conf=\"3\" data-history-items-conf=\"3\" value=\"\" class=\"autocomplete\"/><input type=\"hidden\" name=\"SeriesKey\" value=\"aja\"/></div>\n" +
          "                                                <button type=\"submit\" title=\"Search\" class=\"btn quick-search__button\"><i class=\"icon-search\"></i></button>\n" +
          "                                            </form>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div class=\"page__content padding-wrapper\">\n" +
          "                            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"5b1ca61f-04da-4fc4-ae08-bf4cf8f5d8b2\" class=\"row\">\n" +
          "                                <div class=\"col-lg-8 col-md-8\">\n" +
          "                                    <div class=\"current-issue\">\n" +
          "                                        <div class=\"current-issue__cover\"><a href=\"/journal/aja\"><img src=\"/cms/attachment/1cf9960e-db4b-4171-9f8d-80ea7319a4c8/aja.27.issue-1.cover.gif\" alt=\"American Journal of Audiology cover\"></a></div>\n" +
          "                                        <div class=\"current-issue__info\">\n" +
          "                                            <div class=\"current-issue__details\">\n" +
          "                                                <div class=\"current-issue__specifics\"><span>Volume 27</span><span>Issue 1</span><span>March 2018</span></div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"hidden-lg hidden-md\">\n" +
          "                                        <div class=\"transplant showit\" data-target=\".social-menus\" data-remove=\"false\" data-targetClass=\"hidden-sm hidden-lg hidden-md\" data-toggle=\"transplant\" data-direction=\"from\" data-transplant=\"self\"></div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"actionsbar actionsbar__has__sections\">\n" +
          "                                        <ul class=\"rlist sections-navigation\">\n" +
          "                                            <li class=\"sections-block-container\">\n" +
          "                                                <a data-db-target-for=\"sectionsNavigation\" data-slide-clone=\"self\" data-slide-target=\"#sectionsNavigation_Pop\" href=\"#\">\n" +
          "                                                    <i aria-hidden=\"true\" class=\"icon-list\"></i>\n" +
          "                                                    Sections\n" +
          "                                                </a>\n" +
          "                                                <ul class=\"rlist sections-block\" data-db-target-of=\"sectionsNavigation\" id=\"sectionsNavigation_Pop\">\n" +
          "                                                </ul>\n" +
          "                                            </li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"table-of-content\">\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e37\" class=\"titled_issues__title to-section\">Clinical Focus</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                            <span class=\"issue-item-access\">\n" +
          "                                                            <i title=\"Free Access\" class=\"citation__acess__icon icon-lock_open\"></i>\n" +
          "                                                            </span>\n" +
          "                                                    <span xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:pxje=\"java:com.atypon.frontend.services.impl.PassportXslJavaExtentions\">Clinical Focus</span>\n" +
          "                                                    <span>08 March 2018</span>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    <div class=\"issue-item__title\">\n" +
          "                                                        <a href=\"/doi/part1/part2\" title=\"Audiological Assessment of Word Recognition Skills in Persons With Aphasia\">\n" +
          "                                                            <h5>Audiological Assessment of Word Recognition Skills in Persons With Aphasia</h5>\n" +
          "                                                        </a>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__authors\">\n" +
          "                                                        <ul data-truncate-lines=\"3\" data-truncate-see-more-link=\"true\" data-truncate-type=\"list\" aria-label=\"authors\" class=\"rlist--inline loa js--truncate\">\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=Zhang%2C+Min\" title=\"Author1\">\n" +
          "                                                                    <span>Author1</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=Pratt%2C+Sheila+R\" title=\"Author2\">\n" +
          "                                                                    <span>Author2</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                        </ul>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__doi\">\n" +
          "                                                        <a href=\"https://doi.org/part1/part2\" title=\"https://doi.org/part1/part2\">https://doi.org/part1/part2</a>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\">\n" +
          "                                                            <span>Preview Abstract</span>\n" +
          "                                                            <i aria-hidden=\"true\" class=\"icon-section_arrow_d\"></i>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"accordion__content card--shadow\" style=\"display: none;\">\n" +
          "                                                            Purpose section\n" +
          "                                                        </div>\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part1/part2\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part1/part2\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part1/part2\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part3/part4\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part3/part4\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part3/part4\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                issue item section\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                issue item section\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e1114\" class=\"titled_issues__title to-section\">Research Articles</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    Research Articles  issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    Research Articles  issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        Research Articles accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part5/part6\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part5/part6\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part5/part6\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e3481\" class=\"titled_issues__title to-section\">Review Article</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    Review Article issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    Review Article issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        Review Article  accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part7/part8\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part7/part8\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part7/part8\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div data-widget-def=\"UX3ContentNavigation\" data-widget-id=\"cdcd0b55-9689-4d44-b60a-d5a6b439d622\" class=\"table-of-content__navigation\">\n" +
          "                                        <div class=\"content-navigation clearfix\">\n" +
          "                                            <a href=\"/toc/prev\" title=\"Previous\" class=\"content-navigation__btn--pre\"><span>Previous issue</span></a>\n" +
          "                                            <div class=\"content-navigation__extra\">\n" +
          "                                                <div class=\"pb-dropzone\" data-pb-dropzone=\"navigation-dropzone\"></div>\n" +
          "                                            </div>\n" +
          "                                            <a href=\"/toc/next\" title=\"Next\" class=\"content-navigation__btn--next\"><span>Next issue</span></a>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <div class=\"col-lg-4 col-md-4\">\n" +
          "                                    <div class=\"social-menus\">\n" +
          "                                        social menu section\n" +
          "                                    </div>\n" +
          "                                    <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"dd0314d3-dc07-428c-9266-7011af62d021\" class=\"row\">\n" +
          "                                        <div class=\"col-lg-12 col-md-12 col-sm-6\">\n" +
          "                                            <div data-widget-def=\"literatumAd\" data-widget-id=\"5165f5b3-7fb3-491b-bae9-97dc59c264e2\" class=\"advertisement\">\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"a80395bd-9c28-4645-adcc-7be001fa001a\" class=\"row\">\n" +
          "                            <div class=\"col-md-12\">\n" +
          "                                <div data-widget-def=\"literatumAd\" data-widget-id=\"9086b942-8642-4de7-a62e-8518379023f6\" class=\"advertisement text-center hidden-xs hidden-sm\">\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <footer class=\"footer journal-branding\">\n" +
          "           some content\n" +
          "        </footer>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";

  private static String tocContentCrawlFiltered =
          "<html lang=\"en\">\n" +
          "<head>\n" +
          "head section content\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        \n" +
          "        <main class=\"content toc-page journal-branding\">\n" +
          "            \n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"83f77ad6-f12e-4174-8da6-e631600dbba0\" class=\"row\">\n" +
          "                    <div class=\"col-xs-12\">\n" +
          "                        \n" +
          "                        <div class=\"page__content padding-wrapper\">\n" +
          "                            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"5b1ca61f-04da-4fc4-ae08-bf4cf8f5d8b2\" class=\"row\">\n" +
          "                                <div class=\"col-lg-8 col-md-8\">\n" +
          "                                    \n" +
          "                                    <div class=\"hidden-lg hidden-md\">\n" +
          "                                        <div class=\"transplant showit\" data-target=\".social-menus\" data-remove=\"false\" data-targetClass=\"hidden-sm hidden-lg hidden-md\" data-toggle=\"transplant\" data-direction=\"from\" data-transplant=\"self\"></div>\n" +
          "                                    </div>\n" +
          "                                    \n" +
          "                                    <div class=\"table-of-content\">\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e37\" class=\"titled_issues__title to-section\">Clinical Focus</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                            <span class=\"issue-item-access\">\n" +
          "                                                            <i title=\"Free Access\" class=\"citation__acess__icon icon-lock_open\"></i>\n" +
          "                                                            </span>\n" +
          "                                                    <span xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:pxje=\"java:com.atypon.frontend.services.impl.PassportXslJavaExtentions\">Clinical Focus</span>\n" +
          "                                                    <span>08 March 2018</span>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    <div class=\"issue-item__title\">\n" +
          "                                                        <a href=\"/doi/part1/part2\" title=\"Audiological Assessment of Word Recognition Skills in Persons With Aphasia\">\n" +
          "                                                            <h5>Audiological Assessment of Word Recognition Skills in Persons With Aphasia</h5>\n" +
          "                                                        </a>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__authors\">\n" +
          "                                                        <ul data-truncate-lines=\"3\" data-truncate-see-more-link=\"true\" data-truncate-type=\"list\" aria-label=\"authors\" class=\"rlist--inline loa js--truncate\">\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=Zhang%2C+Min\" title=\"Author1\">\n" +
          "                                                                    <span>Author1</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=Pratt%2C+Sheila+R\" title=\"Author2\">\n" +
          "                                                                    <span>Author2</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                        </ul>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__doi\">\n" +
          "                                                        <a href=\"https://doi.org/part1/part2\" title=\"https://doi.org/part1/part2\">https://doi.org/part1/part2</a>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\">\n" +
          "                                                            <span>Preview Abstract</span>\n" +
          "                                                            <i aria-hidden=\"true\" class=\"icon-section_arrow_d\"></i>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"accordion__content card--shadow\" style=\"display: none;\">\n" +
          "                                                            Purpose section\n" +
          "                                                        </div>\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part1/part2\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part1/part2\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part1/part2\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part3/part4\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part3/part4\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part3/part4\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                issue item section\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                issue item section\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e1114\" class=\"titled_issues__title to-section\">Research Articles</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    Research Articles  issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    Research Articles  issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        Research Articles accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part5/part6\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part5/part6\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part5/part6\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e3481\" class=\"titled_issues__title to-section\">Review Article</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    Review Article issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    Review Article issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        Review Article  accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part7/part8\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part7/part8\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part7/part8\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    \n" +
          "                                </div>\n" +
          "                                <div class=\"col-lg-4 col-md-4\">\n" +
          "                                    \n" +
          "                                    <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"dd0314d3-dc07-428c-9266-7011af62d021\" class=\"row\">\n" +
          "                                        <div class=\"col-lg-12 col-md-12 col-sm-6\">\n" +
          "                                            \n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"a80395bd-9c28-4645-adcc-7be001fa001a\" class=\"row\">\n" +
          "                            <div class=\"col-md-12\">\n" +
          "                                \n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        \n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";

  private static String doiFullContent =
          "<html lang=\"en\">\n" +
          "<head>\n" +
          "    head section content\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "\n" +
          "        <main class=\"content toc-page journal-branding\">\n" +
          "\n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"83f77ad6-f12e-4174-8da6-e631600dbba0\" class=\"row\">\n" +
          "                    <div class=\"col-xs-12\">\n" +
          "\n" +
          "                        <div class=\"page__content padding-wrapper\">\n" +
          "                            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"5b1ca61f-04da-4fc4-ae08-bf4cf8f5d8b2\" class=\"row\">\n" +
          "                                <div class=\"col-lg-8 col-md-8\">\n" +
          "\n" +
          "                                    <div class=\"hidden-lg hidden-md\">\n" +
          "                                        <div class=\"transplant showit\" data-target=\".social-menus\" data-remove=\"false\" data-targetClass=\"hidden-sm hidden-lg hidden-md\" data-toggle=\"transplant\" data-direction=\"from\" data-transplant=\"self\"></div>\n" +
          "                                    </div>\n" +
          "\n" +
          "                                    <div class=\"table-of-content\">\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e37\" class=\"titled_issues__title to-section\">Clinical Focus</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                            <span class=\"issue-item-access\">\n" +
          "                                                            <i title=\"Free Access\" class=\"citation__acess__icon icon-lock_open\"></i>\n" +
          "                                                            </span>\n" +
          "                                                    <span xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:pxje=\"java:com.atypon.frontend.services.impl.PassportXslJavaExtentions\">Clinical Focus</span>\n" +
          "                                                    <span>08 March 2018</span>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    <div class=\"issue-item__title\">\n" +
          "                                                        <a href=\"/doi/part1/part2\" title=\"Audiological Assessment of Word Recognition Skills in Persons With Aphasia\">\n" +
          "                                                            <h5>Audiological Assessment of Word Recognition Skills in Persons With Aphasia</h5>\n" +
          "                                                        </a>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__authors\">\n" +
          "                                                        <ul data-truncate-lines=\"3\" data-truncate-see-more-link=\"true\" data-truncate-type=\"list\" aria-label=\"authors\" class=\"rlist--inline loa js--truncate\">\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=Zhang%2C+Min\" title=\"Author1\">\n" +
          "                                                                    <span>Author1</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=Pratt%2C+Sheila+R\" title=\"Author2\">\n" +
          "                                                                    <span>Author2</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                        </ul>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__doi\">\n" +
          "                                                        <a href=\"https://doi.org/part1/part2\" title=\"https://doi.org/part1/part2\">https://doi.org/part1/part2</a>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\">\n" +
          "                                                            <span>Preview Abstract</span>\n" +
          "                                                            <i aria-hidden=\"true\" class=\"icon-section_arrow_d\"></i>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"accordion__content card--shadow\" style=\"display: none;\">\n" +
          "                                                            Purpose section\n" +
          "                                                        </div>\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part1/part2\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part1/part2\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part1/part2\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part3/part4\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part3/part4\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part3/part4\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                issue item section\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                issue item section\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e1114\" class=\"titled_issues__title to-section\">Research Articles</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    Research Articles  issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    Research Articles  issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        Research Articles accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part5/part6\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part5/part6\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part5/part6\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e3481\" class=\"titled_issues__title to-section\">Review Article</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    Review Article issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    Review Article issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        Review Article  accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part7/part8\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part7/part8\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part7/part8\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "\n" +
          "                                </div>\n" +
          "                                <div class=\"col-lg-4 col-md-4\">\n" +
          "\n" +
          "                                    <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"dd0314d3-dc07-428c-9266-7011af62d021\" class=\"row\">\n" +
          "                                        <div class=\"col-lg-12 col-md-12 col-sm-6\">\n" +
          "\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"a80395bd-9c28-4645-adcc-7be001fa001a\" class=\"row\">\n" +
          "                            <div class=\"col-md-12\">\n" +
          "\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";

  private static String doiFullContentCrawlFiltered =
          "<html lang=\"en\">\n" +
          "<head>\n" +
          "    head section content\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "\n" +
          "        <main class=\"content toc-page journal-branding\">\n" +
          "\n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"83f77ad6-f12e-4174-8da6-e631600dbba0\" class=\"row\">\n" +
          "                    <div class=\"col-xs-12\">\n" +
          "\n" +
          "                        <div class=\"page__content padding-wrapper\">\n" +
          "                            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"5b1ca61f-04da-4fc4-ae08-bf4cf8f5d8b2\" class=\"row\">\n" +
          "                                <div class=\"col-lg-8 col-md-8\">\n" +
          "\n" +
          "                                    <div class=\"hidden-lg hidden-md\">\n" +
          "                                        <div class=\"transplant showit\" data-target=\".social-menus\" data-remove=\"false\" data-targetClass=\"hidden-sm hidden-lg hidden-md\" data-toggle=\"transplant\" data-direction=\"from\" data-transplant=\"self\"></div>\n" +
          "                                    </div>\n" +
          "\n" +
          "                                    <div class=\"table-of-content\">\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e37\" class=\"titled_issues__title to-section\">Clinical Focus</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                            <span class=\"issue-item-access\">\n" +
          "                                                            <i title=\"Free Access\" class=\"citation__acess__icon icon-lock_open\"></i>\n" +
          "                                                            </span>\n" +
          "                                                    <span xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:pxje=\"java:com.atypon.frontend.services.impl.PassportXslJavaExtentions\">Clinical Focus</span>\n" +
          "                                                    <span>08 March 2018</span>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    <div class=\"issue-item__title\">\n" +
          "                                                        <a href=\"/doi/part1/part2\" title=\"Audiological Assessment of Word Recognition Skills in Persons With Aphasia\">\n" +
          "                                                            <h5>Audiological Assessment of Word Recognition Skills in Persons With Aphasia</h5>\n" +
          "                                                        </a>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__authors\">\n" +
          "                                                        <ul data-truncate-lines=\"3\" data-truncate-see-more-link=\"true\" data-truncate-type=\"list\" aria-label=\"authors\" class=\"rlist--inline loa js--truncate\">\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=Zhang%2C+Min\" title=\"Author1\">\n" +
          "                                                                    <span>Author1</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=Pratt%2C+Sheila+R\" title=\"Author2\">\n" +
          "                                                                    <span>Author2</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                        </ul>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__doi\">\n" +
          "                                                        <a href=\"https://doi.org/part1/part2\" title=\"https://doi.org/part1/part2\">https://doi.org/part1/part2</a>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\">\n" +
          "                                                            <span>Preview Abstract</span>\n" +
          "                                                            <i aria-hidden=\"true\" class=\"icon-section_arrow_d\"></i>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"accordion__content card--shadow\" style=\"display: none;\">\n" +
          "                                                            Purpose section\n" +
          "                                                        </div>\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part1/part2\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part1/part2\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part1/part2\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part3/part4\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part3/part4\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part3/part4\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                issue item section\n" +
          "                                            </div>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                issue item section\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e1114\" class=\"titled_issues__title to-section\">Research Articles</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    Research Articles  issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    Research Articles  issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        Research Articles accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part5/part6\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part5/part6\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part5/part6\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"titled_issues\">\n" +
          "                                            <h4 id=\"h_d221375e3481\" class=\"titled_issues__title to-section\">Review Article</h4>\n" +
          "                                            <div class=\"issue-item\">\n" +
          "                                                <div class=\"issue-item__header\">\n" +
          "                                                    Review Article issue item header section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    Review Article issue item body section\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__footer\">\n" +
          "                                                    <div class=\"accordion\">\n" +
          "                                                        Review Article  accordion section\n" +
          "                                                    </div>\n" +
          "                                                    <ul class=\"rlist--inline separator issue-item__links\">\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Abstract\" href=\"/doi/abs/part7/part8\">\n" +
          "                                                                <span>Abstract</span>\n" +
          "                                                                <i class=\"icon icon-abstract\"></i>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"Full text\" href=\"/doi/full/part7/part8\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon icon-full-text\"></i>\n" +
          "                                                                <span>Full text</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a title=\"PDF\" href=\"/doi/pdf/part7/part8\">\n" +
          "                                                                <i aria-hidden=\"true\" class=\"icon-PDF inline-icon\"></i>\n" +
          "                                                                <span>PDF</span>\n" +
          "                                                            </a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "\n" +
          "                                </div>\n" +
          "                                <div class=\"col-lg-4 col-md-4\">\n" +
          "\n" +
          "                                    <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"dd0314d3-dc07-428c-9266-7011af62d021\" class=\"row\">\n" +
          "                                        <div class=\"col-lg-12 col-md-12 col-sm-6\">\n" +
          "\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"a80395bd-9c28-4645-adcc-7be001fa001a\" class=\"row\">\n" +
          "                            <div class=\"col-md-12\">\n" +
          "\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";


  private static String doiAbsContent = "<html lang=\"en\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    head section\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<!-- Google Tag Manager -->\n" +
          "<noscript>\n" +
          "    noscript\n" +
          "</noscript>\n" +
          "<!-- End Google Tag Manager -->\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <header class=\"header fixed\">\n" +
          "            header section\n" +
          "        </header>\n" +
          "        <main class=\"content article-page journal-branding\">\n" +
          "            <div class=\"page-top-panel\"></div>\n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div data-widget-def=\"menuWidget\" data-widget-id=\"99adadc5-2ec1-4d9a-81ae-617865c8a260\" class=\"col-xs-12\">\n" +
          "                        <div class=\"publication__menu\">\n" +
          "                            <div class=\"publication__menu__journal__logo\">\n" +
          "                                <div data-widget-def=\"ux3-general-image\" data-widget-id=\"edb63f56-ec4c-40c9-9183-5aa9439ae422\" class=\"publication__menu__journal__logo\">\n" +
          "                                    <a href=\"/journal/aja\" title=\"aja Journal\"><img alt=\"aja Journal\" src=\"/pb-assets/images/logos/journal_aja-1541785550173.png\"/></a>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <button data-ctrl-res=\"screen-lg\" data-slide-target=\".publication__menu .publication__menu__list\" data-slide-clone=\"self\" class=\"w-slide__btn publication__nav__toggler\"><span class=\"icon-list\"><span class=\"sr-only open\">Menu</span></span></button>\n" +
          "                            <ul class=\"rlist publication__menu__list\">\n" +
          "                                <li><a href=\"/journal/aja\" title=\"HOME\" class=\"publication__menu__link\">HOME</a></li>\n" +
          "                                <li><a href=\"/loi/aja\" title=\"ISSUES\" class=\"publication__menu__link\">ISSUES</a></li>\n" +
          "                                <li><a href=\"/toc/aja/0/0\" title=\"NEWLY PUBLISHED\" class=\"publication__menu__link\">NEWLY PUBLISHED</a></li>\n" +
          "                                <li><a href=\"/aja/subscribe\" title=\"SUBSCRIBE\" class=\"publication__menu__link\">SUBSCRIBE</a></li>\n" +
          "                                <li><a href=\"/aja/forlibrarians\" title=\"RECOMMEND TO A LIBRARIAN\" class=\"publication__menu__link\">RECOMMEND TO A LIBRARIAN</a></li>\n" +
          "                            </ul>\n" +
          "                            <div data-widget-def=\"UX3QuickSearchWidget\" data-widget-id=\"4fd0df5b-af53-47a2-9997-be3bf24fb17b\" class=\"journal-search\">\n" +
          "                                <div class=\"quick-search quick-search--journal\">\n" +
          "                                    <div class=\"full-width\">\n" +
          "                                        <a href=\"#\" title=\"search\" data-db-target-for=\"thisJournalQuickSearch\" class=\"quick-search__toggler lg-hidden\"><i aria-hidden=\"true\" class=\"block-icon icon-search\"></i></a>\n" +
          "                                        <div data-db-target-of=\"thisJournalQuickSearch\" class=\"dropBlock__holder quick-search__dropBlock lg-opened\">\n" +
          "                                            <form action=\"/action/doSearch\" name=\"thisJournalQuickSearch\" method=\"get\" title=\"Quick Search\">\n" +
          "                                                <div class=\"input-group\"><input type=\"search\" name=\"text1\" placeholder=\"Search\" data-auto-complete-max-words=\"7\" data-auto-complete-max-chars=\"32\" data-contributors-conf=\"3\" data-topics-conf=\"3\" data-publication-titles-conf=\"3\" data-history-items-conf=\"3\" value=\"\" class=\"autocomplete\"/><input type=\"hidden\" name=\"SeriesKey\" value=\"aja\"/></div>\n" +
          "                                                <button type=\"submit\" title=\"Search\" class=\"btn quick-search__button\"><i class=\"icon-search\"></i></button>\n" +
          "                                            </form>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <article>\n" +
          "                    <div class=\"row\">\n" +
          "                        <div class=\"col-sm-8 col-md-8 article__content\">\n" +
          "                            <div class=\"citation\">\n" +
          "                                <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><span title=\"Restricted access\" class=\"article__access__type\"><i aria-hidden=\"true\" class=\"citation__acess__icon icon-lock\"></i><span class=\"citation__access__type no-access\">No Access</span></span></span><span class=\"citation__top__item\">American Journal of Audiology</span><span class=\"citation__top__item\">Research Article</span><span class=\"citation__top__item\">8 Mar 2018</span></div>\n" +
          "                                <h1 class=\"citation__title\"><a href=\"/doi/full/part1/part2\" title=\"Investigating the Knowledge, Skills, and Tasks Required for Hearing Aid Management: Perspectives of Clinicians and Hearing Aid Owners\">Investigating the Knowledge, Skills, and Tasks Required for Hearing Aid Management: Perspectives of Clinicians and Hearing Aid Owners</a></h1>\n" +
          "                                <ul xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"meta__authors rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" +
          "                                    <li><a href=\"#\" title=\"Author1\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author1</span></a></li>\n" +
          "                                    ,\n" +
          "                                    <li><a href=\"#\" title=\"Author2\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author2</span></a></li>\n" +
          "                                    ,\n" +
          "                                    <li><a href=\"#\" title=\"Robert H. Eikelboom\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Robert H. Eikelboom</span></a></li>\n" +
          "                                    ,&nbsp;and\n" +
          "                                    <li><a href=\"#\" title=\"Marcus D. Atlas\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Marcus D. Atlas</span></a></li>\n" +
          "                                </ul>\n" +
          "                                <div xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"loa-wrapper hidden-xs\">\n" +
          "                                    <div id=\"sb-1\" class=\"accordion\">\n" +
          "                                        <div class=\"accordion-tabbed loa-accordion\">\n" +
          "                                            <div class=\"accordion-tabbed__tab accordion__closed \">\n" +
          "                                                <a href=\"#\" data-id=\"a1\" data-db-target-for=\"a1\" title=\"Author1\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author1</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
          "                                                <div data-db-target-of=\"a1\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
          "                                                    <p>some content</p>\n" +
          "                                                    <div class=\"bottom-info\">\n" +
          "                                                        <a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Author1\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                                        <p><a href=\"/author/Author1\">\n" +
          "                                                            More articles by this author\n" +
          "                                                        </a>\n" +
          "                                                        </p>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            ,\n" +
          "                                            <div class=\"accordion-tabbed__tab \">\n" +
          "                                                <a href=\"#\" data-id=\"a2\" data-db-target-for=\"a2\" title=\"Author2\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author2</span></a>\n" +
          "                                                <div data-db-target-of=\"a2\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
          "                                                    <p></p>\n" +
          "                                                    <p>School of Health and Rehabilitation Sciences, The University of Queensland, Brisbane,\n" +
          "                                                        Australia\n" +
          "                                                    </p>\n" +
          "                                                    <div class=\"bottom-info\">\n" +
          "                                                        <a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Author2\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                                        <p><a href=\"/author/Author2\">\n" +
          "                                                            More articles by this author\n" +
          "                                                        </a>\n" +
          "                                                        </p>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <p><a href=\"https://doi.org/part1/part2\" class=\"citation__doi__link\">https://doi.org/part1/part2</a></p>\n" +
          "                            <div class=\"actionsbar actionsbar__standalone\">\n" +
          "                                <div class=\"actions-block-container\">\n" +
          "                                    <div><a href=\"/doi/full/part1/part2\" class=\"main-link\"><i class=\"icon-subject\"></i><span>View Full Text</span></a></div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" data-db-target-for=\"article-downloads-list\" data-slide-target=\"#article-downloads-list_Pop\" data-slide-clone=\"self\" class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" data-db-target-for=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6\" data-slide-target=\"#c5ae41be-2f85-497a-b15c-8f2f4110cef6_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul data-db-target-of=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6\" id=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6_Pop\" class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1044%2F2017_AJA-17-0059\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1044%2F2017_AJA-17-0059\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1044%2F2017_AJA-17-0059\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <!-- Go to www.addthis.com/dashboard to customize your tools --><script type=\"text/javascript\" src=\"//s7.addthis.com/js/300/addthis_widget.js#pubid=xa-4faab26f2cff13a7\"></script><a href=\"#\" data-db-target-for=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c\" data-slide-target=\"#693579be-cdc9-4d0f-9ad7-50c9aa60651c_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul data-db-target-of=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c\" id=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c_Pop\" class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
          "                                            <div class=\"pb-dropzone\" data-pb-dropzone=\"shareBlock\"></div>\n" +
          "                                            <li><a class=\"addthis_button_facebook\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-facebook\"></i><span>Facebook</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_twitter\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-twitter\"></i><span>Twitter</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_linkedin\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-linkedin\"></i><span>Linked In</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_email\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-mail\"></i><span>Email</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div class=\"actionsbar actionsbar__has__sections\">\n" +
          "                                <ul class=\"rlist sections-navigation\">\n" +
          "                                    <li class=\"sections-block-container not-visible\">\n" +
          "                                        <a href=\"#\" data-db-target-for=\"sectionsNavigation\" data-slide-target=\"#sectionsNavigation_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-list\"></i>Sections</a>\n" +
          "                                        <ul data-db-target-of=\"sectionsNavigation\" id=\"sectionsNavigation_Pop\" class=\"rlist sections-block\"></ul>\n" +
          "                                    </li>\n" +
          "                                    <li class=\"article-navigation-items-separator not-visible\"></li>\n" +
          "                                    <li class=\"article-tabs-block-container hidden-md hidden-lg\"><a href=\"#\" data-slide-target=\"article .tab--slide\" data-remove=\"false\" class=\"w-slide__btn\"><i aria-hidden=\"true\" class=\"icon-sort\"></i><span>About</span></a></li>\n" +
          "                                </ul>\n" +
          "                                <div class=\"actions-block-container\">\n" +
          "                                    <div><a href=\"/doi/full/part1/part2\" class=\"main-link\"><i class=\"icon-subject\"></i><span>View Full Text</span></a></div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" data-db-target-for=\"article-downloads-list\" data-slide-target=\"#article-downloads-list_Pop\" data-slide-clone=\"self\" class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" data-db-target-for=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6\" data-slide-target=\"#c5ae41be-2f85-497a-b15c-8f2f4110cef6_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul data-db-target-of=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6\" id=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6_Pop\" class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1044%2F2017_AJA-17-0059\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1044%2F2017_AJA-17-0059\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1044%2F2017_AJA-17-0059\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <!-- Go to www.addthis.com/dashboard to customize your tools --><script type=\"text/javascript\" src=\"//s7.addthis.com/js/300/addthis_widget.js#pubid=xa-4faab26f2cff13a7\"></script><a href=\"#\" data-db-target-for=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c\" data-slide-target=\"#693579be-cdc9-4d0f-9ad7-50c9aa60651c_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul data-db-target-of=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c\" id=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c_Pop\" class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
          "                                            <div class=\"pb-dropzone\" data-pb-dropzone=\"shareBlock\"></div>\n" +
          "                                            <li><a class=\"addthis_button_facebook\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-facebook\"></i><span>Facebook</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_twitter\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-twitter\"></i><span>Twitter</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_linkedin\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-linkedin\"></i><span>Linked In</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_email\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-mail\"></i><span>Email</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div class=\"article__body \">\n" +
          "                                <!-- abstract content -->\n" +
          "                                <div class=\"hlFld-Abstract\">\n" +
          "                                    <a name=\"abstract\"></a>\n" +
          "                                    <div class=\"sectionInfo abstractSectionHeading\">\n" +
          "                                        <h2 class=\"article-section__title section__title to-section \" id=\"d1350727e1\">Abstract</h2>\n" +
          "                                    </div>\n" +
          "                                    <div xmlns:exsl=\"http://exslt.org/common\" xmlns:contentType=\"java:com.atypon.literatum.acs.content.ContentType\" xmlns:urlutil=\"java:com.atypon.literatum.customization.UrlUtil\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"abstractSection abstractInFull\">\n" +
          "                                        <div id=\"acd3e286\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e286\">Purpose</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e295\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e295\">Method</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e304\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e304\">Result</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e313\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e313\">Conclusion</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <!-- /abstract content -->\n" +
          "                                <div class=\"article__references\">\n" +
          "                                    <p class=\"explanation__text\"></p>\n" +
          "                                    <h2>References</h2>\n" +
          "                                    <ul class=\"rlist separator\">\n" +
          "                                        <li id=\"bib77\" class=\" references__item \">\n" +
          "                                                    li content\n" +
          "                                        </li>\n" +
          "                                        <li id=\"bib1\" class=\" references__item \">\n" +
          "                                                    li content\n" +
          "                                        </li>\n" +
          "                                    </ul>\n" +
          "                                </div>\n" +
          "                                <div xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:urlutil=\"java:com.atypon.literatum.customization.UrlUtil\" xmlns:exsl=\"http://exslt.org/common\" class=\"response\">\n" +
          "                                    <div class=\"sub-article-title\"></div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div class=\"col-sm-4 sticko__parent article-row-right hidden-xs hidden-sm\">\n" +
          "                            <div class=\"tab tab--slide tab--flex sticko__md dynamic-sticko  tab--flex tabs--xs\">\n" +
          "                                <ul data-mobile-toggle=\"slide\" role=\"tablist\" class=\"rlist tab__nav w-slide--list\">\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-figures\" aria-controls=\"pane-pcw-figures\" role=\"tab\" data-toggle=\"tab\" title=\"figures\" id=\"pane-pcw-figurescon\" data-slide-target=\"#pane-pcw-figures\" class=\"figures-tab\"><i aria-hidden=\"true\" class=\"icon-photo\"></i><span>Figures</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-references\" aria-controls=\"pane-pcw-references\" role=\"tab\" data-toggle=\"tab\" title=\"references\" id=\"pane-pcw-referencescon\" data-slide-target=\"#pane-pcw-references\" class=\"references-tab\"><i aria-hidden=\"true\" class=\"icon-references\"></i><span>References</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-related\" aria-controls=\"pane-pcw-related\" role=\"tab\" data-toggle=\"tab\" title=\"related\" id=\"pane-pcw-relatedcon\" data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><i aria-hidden=\"true\" class=\"icon-related\"></i><span>Related</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-details\" aria-controls=\"pane-pcw-details\" role=\"tab\" data-toggle=\"tab\" title=\"details\" id=\"pane-pcw-detailscon\" data-slide-target=\"#pane-pcw-details\" class=\"details-tab\"><i aria-hidden=\"true\" class=\"icon-info\"></i><span>Details</span></a></li>\n" +
          "                                </ul>\n" +
          "                                <ul class=\"rlist tab__content sticko__child\">\n" +
          "                                    <li id=\"pane-pcw-figures\" aria-labelledby=\"pane-pcw-figurescon\" role=\"tabpanel\" class=\"tab__pane\"></li>\n" +
          "                                    <li id=\"pane-pcw-references\" aria-labelledby=\"pane-pcw-referencescon\" role=\"tabpanel\" class=\"tab__pane\"></li>\n" +
          "                                    <li id=\"pane-pcw-related\" aria-labelledby=\"pane-pcw-relatedcon\" role=\"tabpanel\" class=\"accordion-with-arrow tab__pane tab__pane--clear\">\n" +
          "                                        <div class=\"accordion\">\n" +
          "                                            <ul class=\"accordion-tabbed rlist\">\n" +
          "                                                <li class=\"accordion-tabbed__tab\">\n" +
          "                                                    <a href=\"#\" title=\"Cited By\" aria-expanded=\"false\" aria-controls=\"relatedTab1\" class=\"accordion-tabbed__control\">Cited By</a>\n" +
          "                                                    <div id=\"relatedTab1\" class=\"accordion-tabbed__content\">\n" +
          "                                                        <div class=\"citedBySection\">\n" +
          "                                                            <div class=\"citedByEntry\"><span class=\"entryAuthor\"><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Bennett%2C+Rebecca+J\">Bennett R</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Meyer%2C+Carly+J\">Meyer C</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Eikelboom%2C+Robert+H\">Eikelboom R</a> and </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Atlas%2C+Marcus+D\">Atlas M</a></span></span> <span class=\"pub-year\">(2018)</span> <a href=\"https://doi.org/10.1044/2018_AJA-18-0050\" target=\"_blank\" class=\"cited-link\">Evaluating Hearing Aid Management: Development of the Hearing Aid Skills and Knowledge Inventory (HASKI)</a><span class=\"seperator\">, </span><span class=\"seriesTitle\">American Journal of Audiology</span><span class=\"seperator\">, </span><span class=\"volume\"><b>27</b></span><span class=\"issue\">:3</span><span class=\"seperator\">, </span><span class=\"page-range\"> (333-348)</span><span class=\"seperator\">, </span><span class=\"pub-date\">Online publication date: 12-Sep-2018</span>.</div>\n" +
          "                                                            <div class=\"citedByEntry\"><span class=\"entryAuthor\"><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Bennett%2C+Rebecca+J\">Bennett R</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Meyer%2C+Carly+J\">Meyer C</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Eikelboom%2C+Robert+H\">Eikelboom R</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Atlas%2C+Julian+D\">Atlas J</a> and </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Atlas%2C+Marcus+D\">Atlas M</a></span></span> <span class=\"pub-year\">(2018)</span> <a href=\"https://doi.org/10.1044/2018_AJA-18-0053\" target=\"_blank\" class=\"cited-link\">Factors Associated With Self-Reported Hearing Aid Management Skills and Knowledge</a><span class=\"seperator\">, </span><span class=\"seriesTitle\">American Journal of Audiology</span><span class=\"seperator\">, </span><span class=\"volume\"><b>27</b></span><span class=\"issue\">:4</span><span class=\"seperator\">, </span><span class=\"page-range\"> (604-613)</span><span class=\"seperator\">, </span><span class=\"pub-date\">Online publication date: 6-Dec-2018</span>.</div>\n" +
          "                                                        </div>\n" +
          "                                                    </div>\n" +
          "                                                </li>\n" +
          "                                                <li class=\"accordion-tabbed__tab\">\n" +
          "                                                    <a href=\"#\" aria-expanded=\"false\" aria-controls=\"relatedTab3\" class=\"accordion-tabbed__control\">Recommended</a>\n" +
          "                                                    <div id=\"relatedTab3\" class=\"accordion-tabbed__content\">\n" +
          "                                                        <ul class=\"rlist lot\">\n" +
          "                                                            <li class=\"grid-item\">\n" +
          "                                                                <div class=\"creative-work\">\n" +
          "                                                                    <div class=\"delayLoad\">\n" +
          "                                                                        <a href=\"/doi/full/10.1044/arri21.2.56\" title=\"Exploring the Relationship Between Hearing Aid Self-Efficacy and Hearing Aid Management\">\n" +
          "                                                                            <h5 class=\"creative-work__title\">Exploring the Relationship Between Hearing Aid Self-Efficacy and Hearing Aid Management</h5>\n" +
          "                                                                        </a>\n" +
          "                                                                        <ul xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"meta__authors rlist--inline loa mobile-authors\" title=\"list of authors\">\n" +
          "                                                                            <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorStored=Dullard%2C+Brittney+A\" title=\"Brittney A. Dullard\">Brittney A. Dullard</a></span> and\n" +
          "                                                                            </li>\n" +
          "                                                                            <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorStored=Cienkowski%2C+Kathleen+M\" title=\"Kathleen M. Cienkowski\">Kathleen M. Cienkowski</a></span></li>\n" +
          "                                                                        </ul>\n" +
          "                                                                        <div class=\"meta\"><a href=/toc/ashaarii/21/2>Vol. 21, No. 2\n" +
          "                                                                        </a><time datetime=\"November 2018\">November 2018</time>\n" +
          "                                                                        </div>\n" +
          "                                                                    </div>\n" +
          "                                                                    <div class=\"lazy-load\">\n" +
          "                                                                        <div class=\"lazy-load__text\">\n" +
          "                                                                            <div class=\"lazy-load__line\"></div>\n" +
          "                                                                            <div class=\"lazy-load__line--80\"></div>\n" +
          "                                                                            <div class=\"lazy-load__line\"></div>\n" +
          "                                                                            <div class=\"lazy-load__line--70\"></div>\n" +
          "                                                                            <div class=\"lazy-load__line--50\"></div>\n" +
          "                                                                        </div>\n" +
          "                                                                    </div>\n" +
          "                                                                </div>\n" +
          "                                                            </li>\n" +
          "                                                        </ul>\n" +
          "                                                    </div>\n" +
          "                                                </li>\n" +
          "                                            </ul>\n" +
          "                                        </div>\n" +
          "                                    </li>\n" +
          "                                    <li id=\"pane-pcw-details\" aria-labelledby=\"pane-pcw-detailscon\" role=\"tabpanel\" class=\"tab__pane\">\n" +
          "                                        <div class=\"cover-details\">\n" +
          "                                            <div class=\"cover-details-image\"><a href=\"/toc/aja/27/1\"><img src=\"/cms/attachment/1cf9960e-db4b-4171-9f8d-80ea7319a4c8/aja.27.issue-1.cover.gif\"></a></div>\n" +
          "                                            <div class=\"cover-details-info\">\n" +
          "                                                <div class=\"cover-info-head\">\n" +
          "                                                    <a href=\"/toc/aja/27/1\">\n" +
          "                                                        <h4>Volume 27</h4>\n" +
          "                                                        <h4>Issue 1</h4>\n" +
          "                                                        <h4>March 2018</h4>\n" +
          "                                                    </a>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"cover-info-foot\">Page: 67-84</div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <section class=\"section copywrites\">\n" +
          "                                            <strong class=\"section__title\">Copyright & Permissions</strong>\n" +
          "                                            <div class=\"section__body my-3\">Copyright © 2018 American Speech-Language-Hearing Association</div>\n" +
          "                                        </section>\n" +
          "                                        <section class=\"article__history\">\n" +
          "                                            <ul class=\"rlist rlist--inline\">\n" +
          "                                                <li><a href=\"http://www.copyright.com/openurl.do?issn=1059-0889&amp;contentid=10.1044/2017_AJA-17-0059\" id=\"PermissionsLink\" class=\"badge-type\" target=\"_blank\">Get Permissions</a></li>\n" +
          "                                            </ul>\n" +
          "                                            <p class=\"history__section__title\">History</p>\n" +
          "                                            <ul class=\"rlist article-chapter-history-list\">\n" +
          "                                                <li><span class=\"item_label\">Received:  6/21/17 12:00 AM</span></li>\n" +
          "                                                <li><span class=\"item_label\">Accepted: 8/9/17 12:00 AM</span></li>\n" +
          "                                                <li><span class=\"item_label\">Revised: 8/9/17 12:00 AM</span></li>\n" +
          "                                                <li><span class=\"item_label\">Published in print: 3/8/18 12:00 AM</span></li>\n" +
          "                                            </ul>\n" +
          "                                        </section>\n" +
          "                                        <section id=\"doi_altmetric_drawer_area\" class=\"section\">\n" +
          "                                            <strong class=\"section__title\">Metrics</strong>\n" +
          "                                            <div class=\"section__body my-3 altmetric-container\">\n" +
          "                                                <div data-badge-type=\"donut\" data-condensed=\"true\" data-hide-no-mentions=\"true\" data-badge-details=\"right\" data-doi=\"10.1044/2017_AJA-17-0059\" class=\"altmetric-embed\"></div>\n" +
          "                                            </div>\n" +
          "                                        </section>\n" +
          "                                    </li>\n" +
          "                                    <li role=\"alert\" aria-busy=\"true\" class=\"tab__spinner\">\n" +
          "                                        <div class=\"loading-spinner\"></div>\n" +
          "                                        <p>Loading ...</p>\n" +
          "                                    </li>\n" +
          "                                </ul>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </article>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <script>var articleRef = document.querySelector('.article__body:not(.show-references) .article__references');\n" +
          "                    if (articleRef) { articleRef.style.display = \"none\"; }\n" +
          "\n" +
          "                </script>\n" +
          "        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "            <div class=\"figure-viewer__reg__top clearfix\">\n" +
          "                <div class=\"figure-viewer__top__right\"><a href=\"#\" data-ux3-role=\"controller\" role=\"button\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"><span class=\"sr-only\">Close Figure Viewer</span></span></a></div>\n" +
          "                <div class=\"figure-viewer__top__left\"><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"><span class=\"sr-only\">Browse All Figures</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"><span class=\"sr-only\">Return to Figure</span></span></a><span class=\"zoomSlider js__zoom-slider ui-slider\"><label for=\"figure-viewer__zoom-range\" class=\"sr-only\">Change zoom level</label><input type=\"range\" id=\"figure-viewer__zoom-range\" class=\"zoom-range\"/></span><button class=\"figure-viewer__label__zoom icon-zoom zoom-in\"><span class=\"sr-only\">Zoom in</span></button><button class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"><span class=\"sr-only\">Zoom out</span></button></div>\n" +
          "            </div>\n" +
          "            <div class=\"figure-viewer__reg__center\">\n" +
          "                <div class=\"figure-viewer__cent__left\">\n" +
          "                    <a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__prev\"><span class=\"icon-arrow_l\"><span class=\"sr-only\">Previous Figure</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__next\"><span class=\"icon-arrow_r\"><span class=\"sr-only\">Next Figure</span></span></a>\n" +
          "                    <div class=\"figure-viewer__hold__fig\">\n" +
          "                        <figure class=\"holder\"></figure>\n" +
          "                    </div>\n" +
          "                    <div class=\"figure-viewer__hold__list clearfix container\"></div>\n" +
          "                </div>\n" +
          "                <div class=\"figure-viewer__cent__right\">\n" +
          "                    <div class=\"figure-viewer__title\"><a title=\"Open/Close Caption\" href=\"#\" class=\"figure-viewer__ctrl__caption\"><span class=\"icon-doublearrow\"></span><span class=\"figure-viewer__caption__label\">Caption</span></a><span class=\"figure-viewer__title__text\"></span></div>\n" +
          "                    <div class=\"figure-viewer__hold__figcap\"></div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </div>\n" +
          "        <footer class=\"footer journal-branding\">\n" +
          "            footer content\n" +
          "        </footer>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";

  private static String doiAbsContentCrawlFiltered = "<html lang=\"en\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    head section\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<!-- Google Tag Manager -->\n" +
          "<noscript>\n" +
          "    noscript\n" +
          "</noscript>\n" +
          "<!-- End Google Tag Manager -->\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        \n" +
          "        <main class=\"content article-page journal-branding\">\n" +
          "            \n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div data-widget-def=\"menuWidget\" data-widget-id=\"99adadc5-2ec1-4d9a-81ae-617865c8a260\" class=\"col-xs-12\">\n" +
          "                        \n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <article>\n" +
          "                    <div class=\"row\">\n" +
          "                        <div class=\"col-sm-8 col-md-8 article__content\">\n" +
          "                            <div class=\"citation\">\n" +
          "                                <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><span title=\"Restricted access\" class=\"article__access__type\"><i aria-hidden=\"true\" class=\"citation__acess__icon icon-lock\"></i><span class=\"citation__access__type no-access\">No Access</span></span></span><span class=\"citation__top__item\">American Journal of Audiology</span><span class=\"citation__top__item\">Research Article</span><span class=\"citation__top__item\">8 Mar 2018</span></div>\n" +
          "                                <h1 class=\"citation__title\"><a href=\"/doi/full/part1/part2\" title=\"Investigating the Knowledge, Skills, and Tasks Required for Hearing Aid Management: Perspectives of Clinicians and Hearing Aid Owners\">Investigating the Knowledge, Skills, and Tasks Required for Hearing Aid Management: Perspectives of Clinicians and Hearing Aid Owners</a></h1>\n" +
          "                                <ul xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"meta__authors rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" +
          "                                    <li><a href=\"#\" title=\"Author1\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author1</span></a></li>\n" +
          "                                    ,\n" +
          "                                    <li><a href=\"#\" title=\"Author2\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author2</span></a></li>\n" +
          "                                    ,\n" +
          "                                    <li><a href=\"#\" title=\"Robert H. Eikelboom\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Robert H. Eikelboom</span></a></li>\n" +
          "                                    ,&nbsp;and\n" +
          "                                    <li><a href=\"#\" title=\"Marcus D. Atlas\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Marcus D. Atlas</span></a></li>\n" +
          "                                </ul>\n" +
          "                                <div xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"loa-wrapper hidden-xs\">\n" +
          "                                    <div id=\"sb-1\" class=\"accordion\">\n" +
          "                                        <div class=\"accordion-tabbed loa-accordion\">\n" +
          "                                            <div class=\"accordion-tabbed__tab accordion__closed \">\n" +
          "                                                <a href=\"#\" data-id=\"a1\" data-db-target-for=\"a1\" title=\"Author1\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author1</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
          "                                                <div data-db-target-of=\"a1\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
          "                                                    <p>some content</p>\n" +
          "                                                    <div class=\"bottom-info\">\n" +
          "                                                        <a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Author1\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                                        <p><a href=\"/author/Author1\">\n" +
          "                                                            More articles by this author\n" +
          "                                                        </a>\n" +
          "                                                        </p>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            ,\n" +
          "                                            <div class=\"accordion-tabbed__tab \">\n" +
          "                                                <a href=\"#\" data-id=\"a2\" data-db-target-for=\"a2\" title=\"Author2\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author2</span></a>\n" +
          "                                                <div data-db-target-of=\"a2\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
          "                                                    <p></p>\n" +
          "                                                    <p>School of Health and Rehabilitation Sciences, The University of Queensland, Brisbane,\n" +
          "                                                        Australia\n" +
          "                                                    </p>\n" +
          "                                                    <div class=\"bottom-info\">\n" +
          "                                                        <a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Author2\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                                        <p><a href=\"/author/Author2\">\n" +
          "                                                            More articles by this author\n" +
          "                                                        </a>\n" +
          "                                                        </p>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <p><a href=\"https://doi.org/part1/part2\" class=\"citation__doi__link\">https://doi.org/part1/part2</a></p>\n" +
          "                            \n" +
          "                            \n" +
          "                            <div class=\"article__body \">\n" +
          "                                <!-- abstract content -->\n" +
          "                                <div class=\"hlFld-Abstract\">\n" +
          "                                    <a name=\"abstract\"></a>\n" +
          "                                    <div class=\"sectionInfo abstractSectionHeading\">\n" +
          "                                        <h2 class=\"article-section__title section__title to-section \" id=\"d1350727e1\">Abstract</h2>\n" +
          "                                    </div>\n" +
          "                                    <div xmlns:exsl=\"http://exslt.org/common\" xmlns:contentType=\"java:com.atypon.literatum.acs.content.ContentType\" xmlns:urlutil=\"java:com.atypon.literatum.customization.UrlUtil\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"abstractSection abstractInFull\">\n" +
          "                                        <div id=\"acd3e286\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e286\">Purpose</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e295\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e295\">Method</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e304\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e304\">Result</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e313\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e313\">Conclusion</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <!-- /abstract content -->\n" +
          "                                <div class=\"article__references\">\n" +
          "                                    <p class=\"explanation__text\"></p>\n" +
          "                                    <h2>References</h2>\n" +
          "                                    <ul class=\"rlist separator\">\n" +
          "                                        <li id=\"bib77\" class=\" references__item \">\n" +
          "                                                    li content\n" +
          "                                        </li>\n" +
          "                                        <li id=\"bib1\" class=\" references__item \">\n" +
          "                                                    li content\n" +
          "                                        </li>\n" +
          "                                    </ul>\n" +
          "                                </div>\n" +
          "                                <div xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:urlutil=\"java:com.atypon.literatum.customization.UrlUtil\" xmlns:exsl=\"http://exslt.org/common\" class=\"response\">\n" +
          "                                    <div class=\"sub-article-title\"></div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div class=\"col-sm-4 sticko__parent article-row-right hidden-xs hidden-sm\">\n" +
          "                            <div class=\"tab tab--slide tab--flex sticko__md dynamic-sticko  tab--flex tabs--xs\">\n" +
          "                                \n" +
          "                                \n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </article>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <script>var articleRef = document.querySelector('.article__body:not(.show-references) .article__references');\n" +
          "                    if (articleRef) { articleRef.style.display = \"none\"; }\n" +
          "\n" +
          "                </script>\n" +
          "        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "            <div class=\"figure-viewer__reg__top clearfix\">\n" +
          "                <div class=\"figure-viewer__top__right\"><a href=\"#\" data-ux3-role=\"controller\" role=\"button\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"><span class=\"sr-only\">Close Figure Viewer</span></span></a></div>\n" +
          "                <div class=\"figure-viewer__top__left\"><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"><span class=\"sr-only\">Browse All Figures</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"><span class=\"sr-only\">Return to Figure</span></span></a><span class=\"zoomSlider js__zoom-slider ui-slider\"><label for=\"figure-viewer__zoom-range\" class=\"sr-only\">Change zoom level</label><input type=\"range\" id=\"figure-viewer__zoom-range\" class=\"zoom-range\"/></span><button class=\"figure-viewer__label__zoom icon-zoom zoom-in\"><span class=\"sr-only\">Zoom in</span></button><button class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"><span class=\"sr-only\">Zoom out</span></button></div>\n" +
          "            </div>\n" +
          "            <div class=\"figure-viewer__reg__center\">\n" +
          "                <div class=\"figure-viewer__cent__left\">\n" +
          "                    <a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__prev\"><span class=\"icon-arrow_l\"><span class=\"sr-only\">Previous Figure</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__next\"><span class=\"icon-arrow_r\"><span class=\"sr-only\">Next Figure</span></span></a>\n" +
          "                    <div class=\"figure-viewer__hold__fig\">\n" +
          "                        <figure class=\"holder\"></figure>\n" +
          "                    </div>\n" +
          "                    <div class=\"figure-viewer__hold__list clearfix container\"></div>\n" +
          "                </div>\n" +
          "                <div class=\"figure-viewer__cent__right\">\n" +
          "                    <div class=\"figure-viewer__title\"><a title=\"Open/Close Caption\" href=\"#\" class=\"figure-viewer__ctrl__caption\"><span class=\"icon-doublearrow\"></span><span class=\"figure-viewer__caption__label\">Caption</span></a><span class=\"figure-viewer__title__text\"></span></div>\n" +
          "                    <div class=\"figure-viewer__hold__figcap\"></div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </div>\n" +
          "        \n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";

  private static String doiPdfContent =
          "<html lang=\"en\" >\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    header section\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<!-- Google Tag Manager -->\n" +
          "<noscript>\n" +
          "    <iframe src=\"https://www.googletagmanager.com/ns.html?id=GTM-5MTTF8\" height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe>\n" +
          "</noscript>\n" +
          "<!-- End Google Tag Manager -->\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <header class=\"header fixed\">\n" +
          "            header section\n" +
          "        </header>\n" +
          "        <main class=\"content article-page journal-branding\">\n" +
          "            <div class=\"page-top-panel\"></div>\n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div data-widget-def=\"menuWidget\" data-widget-id=\"99adadc5-2ec1-4d9a-81ae-617865c8a260\" class=\"col-xs-12\">\n" +
          "                        <div class=\"publication__menu\">\n" +
          "                            <div class=\"publication__menu__journal__logo\">\n" +
          "                                <div data-widget-def=\"ux3-general-image\" data-widget-id=\"edb63f56-ec4c-40c9-9183-5aa9439ae422\" class=\"publication__menu__journal__logo\">\n" +
          "                                    <a href=\"/journal/ajslp\" title=\"ajslp Journal\"><img alt=\"ajslp Journal\" src=\"/pb-assets/images/logos/journal_ajslp-1544488691423.png\"/></a>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <button data-ctrl-res=\"screen-lg\" data-slide-target=\".publication__menu .publication__menu__list\" data-slide-clone=\"self\" class=\"w-slide__btn publication__nav__toggler\"><span class=\"icon-list\"><span class=\"sr-only open\">Menu</span></span></button>\n" +
          "                            <ul class=\"rlist publication__menu__list\">\n" +
          "                                <li><a href=\"/journal/ajslp\" title=\"HOME\" class=\"publication__menu__link\">HOME</a></li>\n" +
          "                                <li><a href=\"/loi/ajslp\" title=\"ISSUES\" class=\"publication__menu__link\">ISSUES</a></li>\n" +
          "                                <li><a href=\"/toc/ajslp/0/0\" title=\"NEWLY PUBLISHED\" class=\"publication__menu__link\">NEWLY PUBLISHED</a></li>\n" +
          "                                <li><a href=\"/ajslp/subscribe\" title=\"SUBSCRIBE\" class=\"publication__menu__link\">SUBSCRIBE</a></li>\n" +
          "                                <li><a href=\"/ajslp/forlibrarians\" title=\"RECOMMEND TO A LIBRARIAN\" class=\"publication__menu__link\">RECOMMEND TO A LIBRARIAN</a></li>\n" +
          "                            </ul>\n" +
          "                            <div data-widget-def=\"UX3QuickSearchWidget\" data-widget-id=\"4fd0df5b-af53-47a2-9997-be3bf24fb17b\" class=\"journal-search\">\n" +
          "                                <div class=\"quick-search quick-search--journal\">\n" +
          "                                    <div class=\"full-width\">\n" +
          "                                        <a href=\"#\" title=\"search\" data-db-target-for=\"thisJournalQuickSearch\" class=\"quick-search__toggler lg-hidden\"><i aria-hidden=\"true\" class=\"block-icon icon-search\"></i></a>\n" +
          "                                        <div data-db-target-of=\"thisJournalQuickSearch\" class=\"dropBlock__holder quick-search__dropBlock lg-opened\">\n" +
          "                                            <form action=\"/action/doSearch\" name=\"thisJournalQuickSearch\" method=\"get\" title=\"Quick Search\">\n" +
          "                                                <div class=\"input-group\"><input type=\"search\" name=\"text1\" placeholder=\"Search\" data-auto-complete-max-words=\"7\" data-auto-complete-max-chars=\"32\" data-contributors-conf=\"3\" data-topics-conf=\"3\" data-publication-titles-conf=\"3\" data-history-items-conf=\"3\" value=\"\" class=\"autocomplete\"/><input type=\"hidden\" name=\"SeriesKey\" value=\"ajslp\"/></div>\n" +
          "                                                <button type=\"submit\" title=\"Search\" class=\"btn quick-search__button\"><i class=\"icon-search\"></i></button>\n" +
          "                                            </form>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <article data-figures=\"/action/ajaxShowFigures?widgetId=9fdf61d5-03e3-49af-9858-423c2ba42830&amp;ajax=true&amp;doi=10.1044%2F2018_AJSLP-17-0013&amp;pbContext=%3Bwebsite%3Awebsite%3Apubs-site%3Bjournal%3Ajournal%3Aajslp%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3Bissue%3Aissue%3Adoi%5C%3A10.1044%2Fajslp.27.issue-4%3Bwgroup%3Astring%3AASHA+Publication+Websites%3Barticle%3Aarticle%3Adoi%5C%3A10.1044%2F2018_AJSLP-17-0013%3BsubPage%3Astring%3AAccess+Denial%3BpageGroup%3Astring%3APublication+Pages\" data-references=\"/action/ajaxShowEnhancedAbstract?widgetId=9fdf61d5-03e3-49af-9858-423c2ba42830&amp;ajax=true&amp;doi=10.1044%2F2018_AJSLP-17-0013&amp;pbContext=%3Bwebsite%3Awebsite%3Apubs-site%3Bjournal%3Ajournal%3Aajslp%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3Bissue%3Aissue%3Adoi%5C%3A10.1044%2Fajslp.27.issue-4%3Bwgroup%3Astring%3AASHA+Publication+Websites%3Barticle%3Aarticle%3Adoi%5C%3A10.1044%2F2018_AJSLP-17-0013%3BsubPage%3Astring%3AAccess+Denial%3BpageGroup%3Astring%3APublication+Pages\" data-enable-mathjax=\"true\">\n" +
          "                    <div class=\"row\">\n" +
          "                        <div class=\"col-sm-8 col-md-8 article__content\">\n" +
          "                            <div class=\"citation\">\n" +
          "                                <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><span title=\"Restricted access\" class=\"article__access__type\"><i aria-hidden=\"true\" class=\"citation__acess__icon icon-lock\"></i><span class=\"citation__access__type no-access\">No Access</span></span></span><span class=\"citation__top__item\">American Journal of Speech-Language Pathology</span><span class=\"citation__top__item\">Research Article</span><span class=\"citation__top__item\">21 Nov 2018</span></div>\n" +
          "                                <h1 class=\"citation__title\"><a href=\"/doi/full/part1/part2\" title=\"Speech Impairment in Boys With Fetal Alcohol Spectrum Disorders\">Speech Impairment in Boys With Fetal Alcohol Spectrum Disorders</a></h1>\n" +
          "                                <ul xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"meta__authors rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" +
          "                                   ul content\n" +
          "                                </ul>\n" +
          "                                <div xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"loa-wrapper hidden-xs\">\n" +
          "                                    <div id=\"sb-1\" class=\"accordion\">\n" +
          "                                        <div class=\"accordion-tabbed loa-accordion\">\n" +
          "                                            <div class=\"accordion-tabbed__tab accordion__closed \">\n" +
          "                                                <a href=\"#\" data-id=\"a1\" data-db-target-for=\"a1\" title=\"Author1\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author1</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
          "                                                <div data-db-target-of=\"a1\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
          "                                                    <p>some content</p>\n" +
          "                                                    <div class=\"bottom-info\">\n" +
          "                                                        <a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Author1\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                                        <p><a href=\"/author/Author1\">\n" +
          "                                                            More articles by this author\n" +
          "                                                        </a>\n" +
          "                                                        </p>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            ,\n" +
          "                                            <div class=\"accordion-tabbed__tab \">\n" +
          "                                                <a href=\"#\" data-id=\"a2\" data-db-target-for=\"a2\" title=\"Author2\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author2</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
          "                                                <div data-db-target-of=\"a2\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
          "                                                    <p>some content</p>\n" +
          "                                                    <div class=\"bottom-info\">\n" +
          "                                                        <a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Author2\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                                        <p><a href=\"/author/Author2\">\n" +
          "                                                            More articles by this author\n" +
          "                                                        </a>\n" +
          "                                                        </p>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <p><a href=\"https://doi.org/part1/part2\" class=\"citation__doi__link\">https://doi.org/part1/part2</a></p>\n" +
          "                            <div class=\"actionsbar actionsbar__standalone\">\n" +
          "                                <div class=\"actions-block-container\">\n" +
          "                                    <div><a href=\"/doi/full/part1/part2\" class=\"main-link\"><i class=\"icon-subject\"></i><span>View Full Text</span></a></div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" data-db-target-for=\"article-downloads-list\" data-slide-target=\"#article-downloads-list_Pop\" data-slide-clone=\"self\" class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" data-db-target-for=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6\" data-slide-target=\"#c5ae41be-2f85-497a-b15c-8f2f4110cef6_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul data-db-target-of=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6\" id=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6_Pop\" class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <!-- Go to www.addthis.com/dashboard to customize your tools --><script type=\"text/javascript\" src=\"//s7.addthis.com/js/300/addthis_widget.js#pubid=xa-4faab26f2cff13a7\"></script><a href=\"#\" data-db-target-for=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c\" data-slide-target=\"#693579be-cdc9-4d0f-9ad7-50c9aa60651c_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul data-db-target-of=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c\" id=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c_Pop\" class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
          "                                            <div class=\"pb-dropzone\" data-pb-dropzone=\"shareBlock\"></div>\n" +
          "                                            <li><a class=\"addthis_button_facebook\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-facebook\"></i><span>Facebook</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_twitter\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-twitter\"></i><span>Twitter</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_linkedin\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-linkedin\"></i><span>Linked In</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_email\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-mail\"></i><span>Email</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div class=\"actionsbar actionsbar__has__sections\">\n" +
          "                                <ul class=\"rlist sections-navigation\">\n" +
          "                                    <li class=\"sections-block-container not-visible\">\n" +
          "                                        <a href=\"#\" data-db-target-for=\"sectionsNavigation\" data-slide-target=\"#sectionsNavigation_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-list\"></i>Sections</a>\n" +
          "                                        <ul data-db-target-of=\"sectionsNavigation\" id=\"sectionsNavigation_Pop\" class=\"rlist sections-block\"></ul>\n" +
          "                                    </li>\n" +
          "                                    <li class=\"article-navigation-items-separator not-visible\"></li>\n" +
          "                                    <li class=\"article-tabs-block-container hidden-md hidden-lg\"><a href=\"#\" data-slide-target=\"article .tab--slide\" data-remove=\"false\" class=\"w-slide__btn\"><i aria-hidden=\"true\" class=\"icon-sort\"></i><span>About</span></a></li>\n" +
          "                                </ul>\n" +
          "                                <div class=\"actions-block-container\">\n" +
          "                                    <div><a href=\"/doi/full/part1/part2\" class=\"main-link\"><i class=\"icon-subject\"></i><span>View Full Text</span></a></div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" data-db-target-for=\"article-downloads-list\" data-slide-target=\"#article-downloads-list_Pop\" data-slide-clone=\"self\" class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" data-db-target-for=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6\" data-slide-target=\"#c5ae41be-2f85-497a-b15c-8f2f4110cef6_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul data-db-target-of=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6\" id=\"c5ae41be-2f85-497a-b15c-8f2f4110cef6_Pop\" class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <!-- Go to www.addthis.com/dashboard to customize your tools --><script type=\"text/javascript\" src=\"//s7.addthis.com/js/300/addthis_widget.js#pubid=xa-4faab26f2cff13a7\"></script><a href=\"#\" data-db-target-for=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c\" data-slide-target=\"#693579be-cdc9-4d0f-9ad7-50c9aa60651c_Pop\" data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul data-db-target-of=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c\" id=\"693579be-cdc9-4d0f-9ad7-50c9aa60651c_Pop\" class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
          "                                            <div class=\"pb-dropzone\" data-pb-dropzone=\"shareBlock\"></div>\n" +
          "                                            <li><a class=\"addthis_button_facebook\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-facebook\"></i><span>Facebook</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_twitter\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-twitter\"></i><span>Twitter</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_linkedin\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-linkedin\"></i><span>Linked In</span></a></li>\n" +
          "                                            <li><a class=\"addthis_button_email\"><i aria-hidden=\"true\" class=\"at-icon-wrapper icon-mail\"></i><span>Email</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div class=\"article__body \">\n" +
          "                                <!-- abstract content -->\n" +
          "                                <div class=\"hlFld-Abstract\">\n" +
          "                                    <a name=\"abstract\"></a>\n" +
          "                                    <div class=\"sectionInfo abstractSectionHeading\">\n" +
          "                                        <h2 class=\"&#xA;                        article-section__title section__title to-section&#xA;                    \" id=\"d7109e1\">PDF Content</h2>\n" +
          "                                    </div>\n" +
          "                                    <div xmlns:exsl=\"http://exslt.org/common\" xmlns:contentType=\"java:com.atypon.literatum.acs.content.ContentType\" xmlns:urlutil=\"java:com.atypon.literatum.customization.UrlUtil\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"abstractSection abstractInFull\">\n" +
          "                                        <div id=\"acd3e249\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e249\">Background</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e258\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e258\">Purpose</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e267\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e267\">Method</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e276\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e276\">Results</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e285\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e285\">Conclusions</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">some content\n" +
          "                                        </p>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <!-- /abstract content -->\n" +
          "                                <div class=\"article__references\">\n" +
          "                                    <p class=\"explanation__text\"></p>\n" +
          "                                    <h2>References</h2>\n" +
          "                                    <ul class=\"rlist separator\">\n" +
          "                                        <li id=\"bib1\" class=\"&#xA;                references__item&#xA;            \">\n" +
          "                                       <span xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" class=\"references__note\">\n" +
          "                                          <person-group xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" person-group-type=\"author\">Abel E. L., &amp; Hannigan J. H.</person-group>\n" +
          "                                          (\n" +
          "                                          <span class=\"references__year\">1995</span>).\n" +
          "                                          <span class=\"references__article-title\">Maternal risk factors in fetal alcohol syndrome: Provocative and permissive influences</span>.\n" +
          "                                          <span class=\"references__source\">\n" +
          "                                          <strong>Neurotoxicology and Teratology</strong>\n" +
          "                                          </span>,\n" +
          "                                          <b>17</b>(4), 445–462.\n" +
          "                                          <a class=\"references__doi\" href=\"https://doi.org/\">\n" +
          "                                          <span class=\"references__doi__label\">\n" +
          "                                          DOI:\n" +
          "                                          </span>https://doi.org/part1/part2\n" +
          "                                          </a>\n" +
          "                                          <span class=\"references__suffix\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=1995&pages=445-462&issue=4&title=Maternal+risk+factors+in+fetal+alcohol+syndrome%3A+Provocative+and+permissive+influences\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                          </span>\n" +
          "                                       </span>\n" +
          "                                        </li>\n" +
          "                                    </ul>\n" +
          "                                </div>\n" +
          "                                <div xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:urlutil=\"java:com.atypon.literatum.customization.UrlUtil\" xmlns:exsl=\"http://exslt.org/common\" class=\"response\">\n" +
          "                                    <div class=\"sub-article-title\"></div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div data-widget-id=\"9fdf61d5-03e3-49af-9858-423c2ba42830\" class=\"eCommercePurchaseAccessWidget\">\n" +
          "                                <div id=\"purchaseArea\" class=\"purchaseArea\">\n" +
          "                                    <h2>Access content</h2>\n" +
          "                                    <h5>Login Options:</h5>\n" +
          "                                    <ul class=\"rlist loginOptions\">\n" +
          "                                        <li class=\"accordion\">\n" +
          "                                            <a href=\"#\" class=\"accordion__control expand-link\">Member login</a>\n" +
          "                                            <div class=\"content accordion__content\">\n" +
          "                                                <div class=\"login-form\">\n" +
          "                                                    <form action=\"/action/doLogin\" method=\"post\">\n" +
          "                                                        <input type=\"hidden\" name=\"id\" value=\"9fdf61d5-03e3-49af-9858-423c2ba42830\"/><input type=\"hidden\" name=\"redirectUri\" value=\"/doi/pdf/part1/part2\"/><input type=\"hidden\" name=\"loginUri\" value=\"/doi/pdf/part1/part2\"/><input type=\"hidden\" name=\"popup\" value=\"true\"/>\n" +
          "                                                        <div class=\"input-group\">\n" +
          "                                                            <div class=\"label\"><label for=\"login\">Email*</label></div>\n" +
          "                                                            <input id=\"login\" type=\"text\" name=\"login\" value=\"\" size=\"15\" class=\"login\"/>\n" +
          "                                                        </div>\n" +
          "                                                        <div class=\"input-group\">\n" +
          "                                                            <div class=\"label\"><label for=\"password\">Password*</label></div>\n" +
          "                                                            <input id=\"password\" type=\"password\" name=\"password\" value=\"\" autocomplete=\"off\" class=\"password\"/><span class=\"password-eye-icon icon-eye hidden\"></span>\n" +
          "                                                            <div class=\"actions reset-password\"><span>Forgot password? Reset it</span><a href=\"/action/requestResetPassword\" class=\"show-request-reset-password uppercase styled-link\">here</a></div>\n" +
          "                                                        </div>\n" +
          "                                                        <div class=\"remember\"><label for=\"9fdf61d5-03e3-49af-9858-423c2ba42830-remember\" tabindex=\"0\" class=\"checkbox--primary remember-me\"><input id=\"9fdf61d5-03e3-49af-9858-423c2ba42830-remember\" type=\"checkbox\" name=\"remember\" value=\"true\" class=\"cmn-toggle cmn-toggle-round-flat\"/><span class=\"label-txt\">Keep me logged in</span></label></div>\n" +
          "                                                        <div class=\"submit\"><input type=\"submit\" name=\"submit\" value=\"Login\" class=\"button submit primary\"/></div>\n" +
          "                                                    </form>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </li>\n" +
          "                                        <li><a href=\"/action/ssostart?redirectUri=%2Fdoi%2Fpdf%2F10.1044%2F2018_AJSLP-17-0013\">Institutional login</a></li>\n" +
          "                                    </ul>\n" +
          "                                    <hr/>\n" +
          "                                    <div class=\"eCommercePurchaseAccessWidgetContainer \">\n" +
          "                                        <a data-bind=\"expandSection\" class=\"purchaseAreaList_expand active\" href=\"#purchasePanel\" id=\"purchaseLink\">Purchase</a>\n" +
          "                                        <a data-bind=\"saveItem\" data-doi=\"10.1044/2018_AJSLP-17-0013\" class=\"save-for-later-link\" href=\"/action/saveItem?doi=10.1044/2018_AJSLP-17-0013\">\n" +
          "                                            Save for later\n" +
          "                                        </a>\n" +
          "                                        <a class=\"saved-go-cart hidden\"  href=\"/action/showCart?FlowID=2\"><span class=\"icon-cart-icon-material\"></span> Item saved, go to cart </a>\n" +
          "                                        <div class=\"purchaseAreaList_expanded\" id=\"#purchasePanel\">\n" +
          "                                            <div class=\"purchase-options-container\">\n" +
          "                                                <div class=\"savedItem-info hidden\">\n" +
          "                                                    <p></p>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"purchaseMessage hidden\" data-item=\"AJSLP-12M-PPV-AJSLP-ALL-12M-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500094\">\n" +
          "                                                    <p class=\"errorMsgBox\"></p>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"add-journal-to-cart\">\n" +
          "                                                    <header>\n" +
          "                                                        <a data-bind=\"addItem\" data-key=\"AJSLP-12M-PPV-AJSLP-ALL-12M-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500094\" data-entity=\"AJSLP-12M-PPV-AJSLP-ALL-12M-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500094\"\n" +
          "                                                           class=\"add-article-to-cart\"\n" +
          "                                                           href=\"/action/addToCart?id=AJSLP-12M-PPV-AJSLP-ALL-12M-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500094\">\n" +
          "                                                <span class=\"add-article-to-cart__title title\">\n" +
          "                                                Entire American Journal of Speech-Language Pathology content & archive 1-Year, All Journal Articles for 12 months\n" +
          "                                                </span>\n" +
          "                                                            <span class=\"add-article-to-cart__price price\">$60.00</span>\n" +
          "                                                            <span class=\"add-to-cart-msg\">\n" +
          "                                                <span class=\"icon-shopping_cart\"></span>\n" +
          "                                                Add to cart\n" +
          "                                                </span>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"purchaseMessage hidden info\" data-item=\"AJSLP-12M-PPV-AJSLP-ALL-12M-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500094\">\n" +
          "                                                            <p></p>\n" +
          "                                                        </div>\n" +
          "                                                    </header>\n" +
          "                                                    <div class=\"addedMessage hidden\" data-item=\"AJSLP-12M-PPV-AJSLP-ALL-12M-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500094\">\n" +
          "                                                <span class=\"article-title\">\n" +
          "                                                <span class=\"article-title-content\">\n" +
          "                                                <i class=\"icon-check_circle\" aria-hidden=\"true\"></i>\n" +
          "                                                <span class=\"text\">\n" +
          "                                                Entire American Journal of Speech-Language Pathology content & archive 1-Year, All Journal Articles for 12 months\n" +
          "                                                </span>\n" +
          "                                                </span>\n" +
          "                                                <a href=\"/action/showCart?FlowID=2\" class=\"show-cart-link\">\n" +
          "                                                <span class=\"text\">\n" +
          "                                                Checkout\n" +
          "                                                </span>\n" +
          "                                                </a>\n" +
          "                                                </span>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"purchaseMessage hidden\" data-item=\"AJSLP-JOURNAL-24H-PPV-AJSLP-ALL-24H-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500093\">\n" +
          "                                                    <p class=\"errorMsgBox\"></p>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"add-journal-to-cart\">\n" +
          "                                                    <header>\n" +
          "                                                        <a data-bind=\"addItem\" data-key=\"AJSLP-JOURNAL-24H-PPV-AJSLP-ALL-24H-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500093\" data-entity=\"AJSLP-JOURNAL-24H-PPV-AJSLP-ALL-24H-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500093\"\n" +
          "                                                           class=\"add-article-to-cart\"\n" +
          "                                                           href=\"/action/addToCart?id=AJSLP-JOURNAL-24H-PPV-AJSLP-ALL-24H-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500093\">\n" +
          "                                                <span class=\"add-article-to-cart__title title\">\n" +
          "                                                Entire American Journal of Speech-Language Pathology content & archive 24-hour, All Journal Articles for 24 hours\n" +
          "                                                </span>\n" +
          "                                                            <span class=\"add-article-to-cart__price price\">$30.00</span>\n" +
          "                                                            <span class=\"add-to-cart-msg\">\n" +
          "                                                <span class=\"icon-shopping_cart\"></span>\n" +
          "                                                Add to cart\n" +
          "                                                </span>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"purchaseMessage hidden info\" data-item=\"AJSLP-JOURNAL-24H-PPV-AJSLP-ALL-24H-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500093\">\n" +
          "                                                            <p></p>\n" +
          "                                                        </div>\n" +
          "                                                    </header>\n" +
          "                                                    <div class=\"addedMessage hidden\" data-item=\"AJSLP-JOURNAL-24H-PPV-AJSLP-ALL-24H-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500093\">\n" +
          "                                                <span class=\"article-title\">\n" +
          "                                                <span class=\"article-title-content\">\n" +
          "                                                <i class=\"icon-check_circle\" aria-hidden=\"true\"></i>\n" +
          "                                                <span class=\"text\">\n" +
          "                                                Entire American Journal of Speech-Language Pathology content & archive 24-hour, All Journal Articles for 24 hours\n" +
          "                                                </span>\n" +
          "                                                </span>\n" +
          "                                                <a href=\"/action/showCart?FlowID=2\" class=\"show-cart-link\">\n" +
          "                                                <span class=\"text\">\n" +
          "                                                Checkout\n" +
          "                                                </span>\n" +
          "                                                </a>\n" +
          "                                                </span>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"purchaseMessage hidden\" data-item=\"AJSLP-ARTICLE-24H-PPV-AJSLP-ARTICLE-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500088\">\n" +
          "                                                    <p class=\"errorMsgBox\"></p>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"add-journal-to-cart\">\n" +
          "                                                    <header>\n" +
          "                                                        <a data-bind=\"addItem\" data-key=\"AJSLP-ARTICLE-24H-PPV-AJSLP-ARTICLE-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500088\" data-entity=\"AJSLP-ARTICLE-24H-PPV-AJSLP-ARTICLE-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500088\"\n" +
          "                                                           class=\"add-article-to-cart\"\n" +
          "                                                           href=\"/action/addToCart?id=AJSLP-ARTICLE-24H-PPV-AJSLP-ARTICLE-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500088\">\n" +
          "                                                <span class=\"add-article-to-cart__title title\">\n" +
          "                                                This Article 24-hour, One Article for 24 hours\n" +
          "                                                </span>\n" +
          "                                                            <span class=\"add-article-to-cart__price price\">$15.00</span>\n" +
          "                                                            <span class=\"add-to-cart-msg\">\n" +
          "                                                <span class=\"icon-shopping_cart\"></span>\n" +
          "                                                Add to cart\n" +
          "                                                </span>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"purchaseMessage hidden info\" data-item=\"AJSLP-ARTICLE-24H-PPV-AJSLP-ARTICLE-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500088\">\n" +
          "                                                            <p></p>\n" +
          "                                                        </div>\n" +
          "                                                    </header>\n" +
          "                                                    <div class=\"addedMessage hidden\" data-item=\"AJSLP-ARTICLE-24H-PPV-AJSLP-ARTICLE-ELECTRONIC-USD-10.1044/2018_AJSLP-17-0013-500088\">\n" +
          "                                                <span class=\"article-title\">\n" +
          "                                                <span class=\"article-title-content\">\n" +
          "                                                <i class=\"icon-check_circle\" aria-hidden=\"true\"></i>\n" +
          "                                                <span class=\"text\">\n" +
          "                                                This Article 24-hour, One Article for 24 hours\n" +
          "                                                </span>\n" +
          "                                                </span>\n" +
          "                                                <a href=\"/action/showCart?FlowID=2\" class=\"show-cart-link\">\n" +
          "                                                <span class=\"text\">\n" +
          "                                                Checkout\n" +
          "                                                </span>\n" +
          "                                                </a>\n" +
          "                                                </span>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div class=\"col-sm-4 sticko__parent article-row-right hidden-xs hidden-sm\">\n" +
          "                            <div class=\"tab tab--slide tab--flex sticko__md dynamic-sticko  tab--flex tabs--xs\">\n" +
          "                                <ul data-mobile-toggle=\"slide\" role=\"tablist\" class=\"rlist tab__nav w-slide--list\">\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-figures\" aria-controls=\"pane-pcw-figures\" role=\"tab\" data-toggle=\"tab\" title=\"figures\" id=\"pane-pcw-figurescon\" data-slide-target=\"#pane-pcw-figures\" class=\"figures-tab\"><i aria-hidden=\"true\" class=\"icon-photo\"></i><span>Figures</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-references\" aria-controls=\"pane-pcw-references\" role=\"tab\" data-toggle=\"tab\" title=\"references\" id=\"pane-pcw-referencescon\" data-slide-target=\"#pane-pcw-references\" class=\"references-tab\"><i aria-hidden=\"true\" class=\"icon-references\"></i><span>References</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-related\" aria-controls=\"pane-pcw-related\" role=\"tab\" data-toggle=\"tab\" title=\"related\" id=\"pane-pcw-relatedcon\" data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><i aria-hidden=\"true\" class=\"icon-related\"></i><span>Related</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-details\" aria-controls=\"pane-pcw-details\" role=\"tab\" data-toggle=\"tab\" title=\"details\" id=\"pane-pcw-detailscon\" data-slide-target=\"#pane-pcw-details\" class=\"details-tab\"><i aria-hidden=\"true\" class=\"icon-info\"></i><span>Details</span></a></li>\n" +
          "                                </ul>\n" +
          "                                <ul class=\"rlist tab__content sticko__child\">\n" +
          "                                    <li id=\"pane-pcw-figures\" aria-labelledby=\"pane-pcw-figurescon\" role=\"tabpanel\" class=\"tab__pane\"></li>\n" +
          "                                    <li id=\"pane-pcw-references\" aria-labelledby=\"pane-pcw-referencescon\" role=\"tabpanel\" class=\"tab__pane\"></li>\n" +
          "                                    <li id=\"pane-pcw-related\" aria-labelledby=\"pane-pcw-relatedcon\" role=\"tabpanel\" class=\"accordion-with-arrow tab__pane tab__pane--clear\">\n" +
          "                                        <div class=\"accordion\">\n" +
          "                                            <ul class=\"accordion-tabbed rlist\">\n" +
          "                                                <li class=\"accordion-tabbed__tab\"></li>\n" +
          "                                            </ul>\n" +
          "                                        </div>\n" +
          "                                    </li>\n" +
          "                                    <li id=\"pane-pcw-details\" aria-labelledby=\"pane-pcw-detailscon\" role=\"tabpanel\" class=\"tab__pane\">\n" +
          "                                        <div class=\"cover-details\">\n" +
          "                                            <div class=\"cover-details-image\"><a href=\"/toc/ajslp/27/4\"><img src=\"/cms/attachment/5138404c-10fa-4b63-abf8-8dd1fd16f0ad/ajslp.27.issue-4.cover.gif\"></a></div>\n" +
          "                                            <div class=\"cover-details-info\">\n" +
          "                                                <div class=\"cover-info-head\">\n" +
          "                                                    <a href=\"/toc/ajslp/27/4\">\n" +
          "                                                        <h4>Volume 27</h4>\n" +
          "                                                        <h4>Issue 4</h4>\n" +
          "                                                        <h4>November 2018</h4>\n" +
          "                                                    </a>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"cover-info-foot\">Page: 1405-1425</div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <section class=\"section copywrites\">\n" +
          "                                            <strong class=\"section__title\">Copyright & Permissions</strong>\n" +
          "                                            <div class=\"section__body my-3\">Copyright © 2018 American Speech-Language-Hearing Association</div>\n" +
          "                                        </section>\n" +
          "                                        <section class=\"article__history\">\n" +
          "                                            <ul class=\"rlist rlist--inline\">\n" +
          "                                                <li><a href=\"http://www.copyright.com/openurl.do?issn=1058-0360&amp;contentid=10.1044/2018_AJSLP-17-0013\" id=\"PermissionsLink\" class=\"badge-type\" target=\"_blank\">Get Permissions</a></li>\n" +
          "                                            </ul>\n" +
          "                                            <p class=\"history__section__title\">History</p>\n" +
          "                                            <ul class=\"rlist article-chapter-history-list\">\n" +
          "                                                <li><span class=\"item_label\">Received:  1/26/17 12:00 AM</span></li>\n" +
          "                                                <li><span class=\"item_label\">Accepted: 5/11/18 12:00 AM</span></li>\n" +
          "                                                <li><span class=\"item_label\">Revised: 8/1/17 12:00 AM</span></li>\n" +
          "                                                <li><span class=\"item_label\">Published in print: 11/21/18 12:00 AM</span></li>\n" +
          "                                            </ul>\n" +
          "                                        </section>\n" +
          "                                        <section id=\"doi_altmetric_drawer_area\" class=\"section\">\n" +
          "                                            <strong class=\"section__title\">Metrics</strong>\n" +
          "                                            <div class=\"section__body my-3 altmetric-container\">\n" +
          "                                                <div data-badge-type=\"donut\" data-condensed=\"true\" data-hide-no-mentions=\"true\" data-badge-details=\"right\" data-doi=\"10.1044/2018_AJSLP-17-0013\" class=\"altmetric-embed\"></div>\n" +
          "                                            </div>\n" +
          "                                        </section>\n" +
          "                                    </li>\n" +
          "                                    <li role=\"alert\" aria-busy=\"true\" class=\"tab__spinner\">\n" +
          "                                        <div class=\"loading-spinner\"></div>\n" +
          "                                        <p>Loading ...</p>\n" +
          "                                    </li>\n" +
          "                                </ul>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </article>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <script>var articleRef = document.querySelector('.article__body:not(.show-references) .article__references');\n" +
          "               if (articleRef) { articleRef.style.display = \"none\"; }\n" +
          "\n" +
          "            </script>\n" +
          "        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "            <div class=\"figure-viewer__reg__top clearfix\">\n" +
          "                <div class=\"figure-viewer__top__right\"><a href=\"#\" data-ux3-role=\"controller\" role=\"button\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"><span class=\"sr-only\">Close Figure Viewer</span></span></a></div>\n" +
          "                <div class=\"figure-viewer__top__left\"><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"><span class=\"sr-only\">Browse All Figures</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"><span class=\"sr-only\">Return to Figure</span></span></a><span class=\"zoomSlider js__zoom-slider ui-slider\"><label for=\"figure-viewer__zoom-range\" class=\"sr-only\">Change zoom level</label><input type=\"range\" id=\"figure-viewer__zoom-range\" class=\"zoom-range\"/></span><button class=\"figure-viewer__label__zoom icon-zoom zoom-in\"><span class=\"sr-only\">Zoom in</span></button><button class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"><span class=\"sr-only\">Zoom out</span></button></div>\n" +
          "            </div>\n" +
          "            <div class=\"figure-viewer__reg__center\">\n" +
          "                <div class=\"figure-viewer__cent__left\">\n" +
          "                    <a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__prev\"><span class=\"icon-arrow_l\"><span class=\"sr-only\">Previous Figure</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__next\"><span class=\"icon-arrow_r\"><span class=\"sr-only\">Next Figure</span></span></a>\n" +
          "                    <div class=\"figure-viewer__hold__fig\">\n" +
          "                        <figure class=\"holder\"></figure>\n" +
          "                    </div>\n" +
          "                    <div class=\"figure-viewer__hold__list clearfix container\"></div>\n" +
          "                </div>\n" +
          "                <div class=\"figure-viewer__cent__right\">\n" +
          "                    <div class=\"figure-viewer__title\"><a title=\"Open/Close Caption\" href=\"#\" class=\"figure-viewer__ctrl__caption\"><span class=\"icon-doublearrow\"></span><span class=\"figure-viewer__caption__label\">Caption</span></a><span class=\"figure-viewer__title__text\"></span></div>\n" +
          "                    <div class=\"figure-viewer__hold__figcap\"></div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </div>\n" +
          "        <footer class=\"footer journal-branding\">\n" +
          "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"099142a7-5ea4-43a7-91ed-1529029cf19a\" class=\"footer__wrapper\">\n" +
          "                <div class=\"footer__top\">\n" +
          "                    <div class=\"x-larg__container\">\n" +
          "                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"0ca57340-aaf8-46d0-8252-8afa3b956b87\" class=\"row footer__top__contnet\">\n" +
          "                            <div class=\"col-md-6\">\n" +
          "                                <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"129713a8-09fb-46c5-9262-fc4b8dc6a4ab\" class=\"row\">\n" +
          "                                    <div class=\"col-md-5 clearfix\">\n" +
          "                                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"4fe5dc6d-ae13-4493-9ce5-495cab51b818\" class=\"footer__logo\">\n" +
          "                                            <div>\n" +
          "                                                <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"cc61a005-0e67-4dfa-8df3-37c77916dcbe\" class=\"footer_logos\">\n" +
          "                                                    <ul class=\"rlist--inline\">\n" +
          "                                                        <li>\n" +
          "                                                            <a href=\"/journal/ajslp\" title=\"ajslp Journal\"><img alt=\"ajslp Journal\" src=\"/pb-assets/images/logos/journal_ajslp-1544488691423.png\"/></a>\n" +
          "                                                        </li>\n" +
          "                                                        <li>\n" +
          "                                                            <a href=\"http://www.asha.org\" title=\"American Speech-language-Hearing Association\"><img alt=\"American Speech-language-Hearing Association\" src=\"/pb-assets/images/logos/asha_logo-1541972283270.svg\"/></a>\n" +
          "                                                        </li>\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"footer__social-media\">\n" +
          "                                            <ul class=\"rlist--inline\">\n" +
          "                                                <li><a href=\"http://www.facebook.com/asha.org\" title=\"facebook\"><i aria-hidden=\"true\" class=\"icon-facebook\"></i></a></li>\n" +
          "                                                <li><a href=\"http://twitter.com/ASHAWeb\" title=\"twitter\"><i aria-hidden=\"true\" class=\"icon-twitter\"></i></a></li>\n" +
          "                                                <li><a href=\"https://www.linkedin.com/company/the-american-speech-language-hearing-association-asha-\" title=\"linkedin\"><i aria-hidden=\"true\" class=\"icon-linkedin\"></i></a></li>\n" +
          "                                                <li><a href=\"http://www.youtube.com/ashaweb\" title=\"youtube\"><i aria-hidden=\"true\" class=\"icon-triangle-right\"></i></a></li>\n" +
          "                                            </ul>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"col-md-7 journals\">\n" +
          "                                        <h6>JOURNALS</h6>\n" +
          "                                        <ul class=\"rlist\">\n" +
          "                                            <li><a href=\"/journal/aja\">American Journal of Audiology (AJA)</a></li>\n" +
          "                                            <li><a href=\"/journal/ajslp\">American Journal of Speech-Language Pathology (AJSLP)</a></li>\n" +
          "                                            <li><a href=\"/journal/jslhr\">Journal of Speech, Language, and Hearing Research (JSLHR)</a></li>\n" +
          "                                            <li><a href=\"/journal/lshss\">Language, Speech, and Hearing Services in Schools (LSHSS)</a></li>\n" +
          "                                            <li><a href=\"https://perspectives.pubs.asha.org\">Perspectives of the ASHA Special Interest Groups</a></li>\n" +
          "                                            <li>\n" +
          "                                                <hr>\n" +
          "                                            </li>\n" +
          "                                            <li><a href=\"https://leader.pubs.asha.org\">The ASHA Leader</a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div class=\"col-md-6\">\n" +
          "                                <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"75aca6ad-d0f9-4db6-adff-4e50d2d9fd25\" class=\"row sitemap clearfix\">\n" +
          "                                    <div class=\"sitemap__column first col-md-4 col-sm-3 col-xs-12\">\n" +
          "                                        <div class=\"sitemap__data\">\n" +
          "                                            <h6>RESOURCES</h6>\n" +
          "                                            <ul class=\"rlist\">\n" +
          "                                                <li><a href=\"/ajslp/forauthors\">For Authors</a></li>\n" +
          "                                                <li><a href=\"/ajslp/forreviewers\">For Reviewers</a></li>\n" +
          "                                                <li><a href=\"/ajslp/forlibrarians\">For Librarians</a></li>\n" +
          "                                            </ul>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"sitemap__column col-md-4 col-sm-3 col-xs-12\">\n" +
          "                                        <div class=\"sitemap__data\">\n" +
          "                                            <h6>STAY CONNECTED</h6>\n" +
          "                                            <ul class=\"rlist\">\n" +
          "                                                <li><a href=\"/ajslp/subscribe\">Subscribe</a></li>\n" +
          "                                                <li><a href=\"https://pubs.asha.org/pubs/contact\">Contact Us</a></li>\n" +
          "                                                <li><a href=\"/action/showPreferences?menuTab=licenses\">My Account</a></li>\n" +
          "                                            </ul>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"sitemap__column col-md-4 col-sm-3 col-xs-12\">\n" +
          "                                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"36cc7d78-b636-4c45-ac4c-e1425d661e36\" class=\"sitemap__data\">\n" +
          "                                            <ul class=\"rlist--inline\">\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"http://extranet.who.int/hinari/en/journals.php\" title=\"Hinari\"><img alt=\"Hinari\" src=\"/pb-assets/images/logos/hinari-image-1541971600793.jpg\"/></a>\n" +
          "                                                </li>\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"https://www.clockss.org/clockss/Home\" title=\"CLOCKSS\"><img alt=\"CLOCKSS\" src=\"/pb-assets/images/logos/clockss-image-1541971600840.jpg\"/></a>\n" +
          "                                                </li>\n" +
          "                                            </ul>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <div class=\"footer__bottom\">\n" +
          "                    <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"63bbb0a4-8224-4c0f-b4e6-50e1b0688636\" class=\"x-larg__container\">\n" +
          "                        <div class=\"row\">\n" +
          "                            <div data-widget-def=\"UX3HTMLWidget\" data-widget-id=\"8132593b-249d-4e20-9058-8b7add190758\" class=\"col-xs-12 my-3\">\n" +
          "                                <ul class=\"rlist--inline legal_links\">\n" +
          "                                    <li class=\"pr-3\">\n" +
          "                                        <a href=\"https://www.asha.org/sitehelp/privacy-statement/\">Privacy policy</a>\n" +
          "                                    </li>\n" +
          "                                    <li class=\"pl-3\">\n" +
          "                                        <a href=\"https://www.asha.org/sitehelp/terms_of_use/\">Terms &amp; Conditions</a>\n" +
          "                                    </li>\n" +
          "                                </ul>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div class=\"row\">\n" +
          "                            <div data-widget-def=\"UX3HTMLWidget\" data-widget-id=\"dbf2ce48-2a0d-4803-a62b-b07146f8797e\" class=\"col-xs-12 copywrites\">\n" +
          "                                <span>Copyright © 2018 American Speech-Language-Hearing Association</span>\n" +
          "                                <p>\n" +
          "                                    <span>Powered by Atypon Literatum</span>\n" +
          "                                </p>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </footer>\n" +
          "        <!-- Bot analytics test -->\n" +
          "        <script src=\"https://api.b2c.com/api/init-260u9k3l7hxtcferh76.js\" data-cfasync=\"false\" async></script>\n" +
          "        <noscript><img src=\"https://api.b2c.com/api/noscript-260u9k3l7hxtcferh76.gif\"></noscript>\n" +
          "        <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb-assets/styles/branding_ajslp-1541975204007.css\">\n" +
          "    </div>\n" +
          "</div>\n" +
          "<script type=\"text/javascript\" src=\"/products/photo-theme/asha/releasedAssets/js/main.bundle.js\"></script>\n" +
          "<script type=\"text/javascript\" src=\"/wro/product.js\"></script>\n" +
          "</body>\n" +
          "</html>";

  private static String doiPdfContentCrawlFiltered =
          "<html lang=\"en\" >\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    header section\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<!-- Google Tag Manager -->\n" +
          "<noscript>\n" +
          "    <iframe src=\"https://www.googletagmanager.com/ns.html?id=GTM-5MTTF8\" height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe>\n" +
          "</noscript>\n" +
          "<!-- End Google Tag Manager -->\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        \n" +
          "        <main class=\"content article-page journal-branding\">\n" +
          "            \n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div data-widget-def=\"menuWidget\" data-widget-id=\"99adadc5-2ec1-4d9a-81ae-617865c8a260\" class=\"col-xs-12\">\n" +
          "                        \n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <article data-figures=\"/action/ajaxShowFigures?widgetId=9fdf61d5-03e3-49af-9858-423c2ba42830&amp;ajax=true&amp;doi=10.1044%2F2018_AJSLP-17-0013&amp;pbContext=%3Bwebsite%3Awebsite%3Apubs-site%3Bjournal%3Ajournal%3Aajslp%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3Bissue%3Aissue%3Adoi%5C%3A10.1044%2Fajslp.27.issue-4%3Bwgroup%3Astring%3AASHA+Publication+Websites%3Barticle%3Aarticle%3Adoi%5C%3A10.1044%2F2018_AJSLP-17-0013%3BsubPage%3Astring%3AAccess+Denial%3BpageGroup%3Astring%3APublication+Pages\" data-references=\"/action/ajaxShowEnhancedAbstract?widgetId=9fdf61d5-03e3-49af-9858-423c2ba42830&amp;ajax=true&amp;doi=10.1044%2F2018_AJSLP-17-0013&amp;pbContext=%3Bwebsite%3Awebsite%3Apubs-site%3Bjournal%3Ajournal%3Aajslp%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3Bissue%3Aissue%3Adoi%5C%3A10.1044%2Fajslp.27.issue-4%3Bwgroup%3Astring%3AASHA+Publication+Websites%3Barticle%3Aarticle%3Adoi%5C%3A10.1044%2F2018_AJSLP-17-0013%3BsubPage%3Astring%3AAccess+Denial%3BpageGroup%3Astring%3APublication+Pages\" data-enable-mathjax=\"true\">\n" +
          "                    <div class=\"row\">\n" +
          "                        <div class=\"col-sm-8 col-md-8 article__content\">\n" +
          "                            <div class=\"citation\">\n" +
          "                                <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><span title=\"Restricted access\" class=\"article__access__type\"><i aria-hidden=\"true\" class=\"citation__acess__icon icon-lock\"></i><span class=\"citation__access__type no-access\">No Access</span></span></span><span class=\"citation__top__item\">American Journal of Speech-Language Pathology</span><span class=\"citation__top__item\">Research Article</span><span class=\"citation__top__item\">21 Nov 2018</span></div>\n" +
          "                                <h1 class=\"citation__title\"><a href=\"/doi/full/part1/part2\" title=\"Speech Impairment in Boys With Fetal Alcohol Spectrum Disorders\">Speech Impairment in Boys With Fetal Alcohol Spectrum Disorders</a></h1>\n" +
          "                                <ul xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"meta__authors rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" +
          "                                   ul content\n" +
          "                                </ul>\n" +
          "                                <div xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"loa-wrapper hidden-xs\">\n" +
          "                                    <div id=\"sb-1\" class=\"accordion\">\n" +
          "                                        <div class=\"accordion-tabbed loa-accordion\">\n" +
          "                                            <div class=\"accordion-tabbed__tab accordion__closed \">\n" +
          "                                                <a href=\"#\" data-id=\"a1\" data-db-target-for=\"a1\" title=\"Author1\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author1</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
          "                                                <div data-db-target-of=\"a1\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
          "                                                    <p>some content</p>\n" +
          "                                                    <div class=\"bottom-info\">\n" +
          "                                                        <a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Author1\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                                        <p><a href=\"/author/Author1\">\n" +
          "                                                            More articles by this author\n" +
          "                                                        </a>\n" +
          "                                                        </p>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                            ,\n" +
          "                                            <div class=\"accordion-tabbed__tab \">\n" +
          "                                                <a href=\"#\" data-id=\"a2\" data-db-target-for=\"a2\" title=\"Author2\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author2</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
          "                                                <div data-db-target-of=\"a2\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
          "                                                    <p>some content</p>\n" +
          "                                                    <div class=\"bottom-info\">\n" +
          "                                                        <a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Author2\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                                        <p><a href=\"/author/Author2\">\n" +
          "                                                            More articles by this author\n" +
          "                                                        </a>\n" +
          "                                                        </p>\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <p><a href=\"https://doi.org/part1/part2\" class=\"citation__doi__link\">https://doi.org/part1/part2</a></p>\n" +
          "                            \n" +
          "                            \n" +
          "                            <div class=\"article__body \">\n" +
          "                                <!-- abstract content -->\n" +
          "                                <div class=\"hlFld-Abstract\">\n" +
          "                                    <a name=\"abstract\"></a>\n" +
          "                                    <div class=\"sectionInfo abstractSectionHeading\">\n" +
          "                                        <h2 class=\"&#xA;                        article-section__title section__title to-section&#xA;                    \" id=\"d7109e1\">PDF Content</h2>\n" +
          "                                    </div>\n" +
          "                                    <div xmlns:exsl=\"http://exslt.org/common\" xmlns:contentType=\"java:com.atypon.literatum.acs.content.ContentType\" xmlns:urlutil=\"java:com.atypon.literatum.customization.UrlUtil\" xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" class=\"abstractSection abstractInFull\">\n" +
          "                                        <div id=\"acd3e249\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e249\">Background</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e258\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e258\">Purpose</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e267\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e267\">Method</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e276\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e276\">Results</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e285\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e285\">Conclusions</h3>\n" +
          "                                        <p xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\">some content\n" +
          "                                        </p>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <!-- /abstract content -->\n" +
          "                                <div class=\"article__references\">\n" +
          "                                    <p class=\"explanation__text\"></p>\n" +
          "                                    <h2>References</h2>\n" +
          "                                    <ul class=\"rlist separator\">\n" +
          "                                        <li id=\"bib1\" class=\"&#xA;                references__item&#xA;            \">\n" +
          "                                       <span xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" class=\"references__note\">\n" +
          "                                          <person-group xmlns:ali=\"http://www.niso.org/schemas/ali/1.0/\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bkstg=\"http://www.atypon.com/backstage-ns\" person-group-type=\"author\">Abel E. L., &amp; Hannigan J. H.</person-group>\n" +
          "                                          (\n" +
          "                                          <span class=\"references__year\">1995</span>).\n" +
          "                                          <span class=\"references__article-title\">Maternal risk factors in fetal alcohol syndrome: Provocative and permissive influences</span>.\n" +
          "                                          <span class=\"references__source\">\n" +
          "                                          <strong>Neurotoxicology and Teratology</strong>\n" +
          "                                          </span>,\n" +
          "                                          <b>17</b>(4), 445â\u0080\u0093462.\n" +
          "                                          <a class=\"references__doi\" href=\"https://doi.org/\">\n" +
          "                                          <span class=\"references__doi__label\">\n" +
          "                                          DOI:\n" +
          "                                          </span>https://doi.org/part1/part2\n" +
          "                                          </a>\n" +
          "                                          <span class=\"references__suffix\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=1995&pages=445-462&issue=4&title=Maternal+risk+factors+in+fetal+alcohol+syndrome%3A+Provocative+and+permissive+influences\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                          </span>\n" +
          "                                       </span>\n" +
          "                                        </li>\n" +
          "                                    </ul>\n" +
          "                                </div>\n" +
          "                                <div xmlns:xje=\"java:com.atypon.publish.util.xml.XslJavaExtension\" xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:urlutil=\"java:com.atypon.literatum.customization.UrlUtil\" xmlns:exsl=\"http://exslt.org/common\" class=\"response\">\n" +
          "                                    <div class=\"sub-article-title\"></div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            \n" +
          "                        </div>\n" +
          "                        <div class=\"col-sm-4 sticko__parent article-row-right hidden-xs hidden-sm\">\n" +
          "                            <div class=\"tab tab--slide tab--flex sticko__md dynamic-sticko  tab--flex tabs--xs\">\n" +
          "                                \n" +
          "                                \n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </article>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <script>var articleRef = document.querySelector('.article__body:not(.show-references) .article__references');\n" +
          "               if (articleRef) { articleRef.style.display = \"none\"; }\n" +
          "\n" +
          "            </script>\n" +
          "        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "            <div class=\"figure-viewer__reg__top clearfix\">\n" +
          "                <div class=\"figure-viewer__top__right\"><a href=\"#\" data-ux3-role=\"controller\" role=\"button\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"><span class=\"sr-only\">Close Figure Viewer</span></span></a></div>\n" +
          "                <div class=\"figure-viewer__top__left\"><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"><span class=\"sr-only\">Browse All Figures</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"><span class=\"sr-only\">Return to Figure</span></span></a><span class=\"zoomSlider js__zoom-slider ui-slider\"><label for=\"figure-viewer__zoom-range\" class=\"sr-only\">Change zoom level</label><input type=\"range\" id=\"figure-viewer__zoom-range\" class=\"zoom-range\"/></span><button class=\"figure-viewer__label__zoom icon-zoom zoom-in\"><span class=\"sr-only\">Zoom in</span></button><button class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"><span class=\"sr-only\">Zoom out</span></button></div>\n" +
          "            </div>\n" +
          "            <div class=\"figure-viewer__reg__center\">\n" +
          "                <div class=\"figure-viewer__cent__left\">\n" +
          "                    <a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__prev\"><span class=\"icon-arrow_l\"><span class=\"sr-only\">Previous Figure</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__next\"><span class=\"icon-arrow_r\"><span class=\"sr-only\">Next Figure</span></span></a>\n" +
          "                    <div class=\"figure-viewer__hold__fig\">\n" +
          "                        <figure class=\"holder\"></figure>\n" +
          "                    </div>\n" +
          "                    <div class=\"figure-viewer__hold__list clearfix container\"></div>\n" +
          "                </div>\n" +
          "                <div class=\"figure-viewer__cent__right\">\n" +
          "                    <div class=\"figure-viewer__title\"><a title=\"Open/Close Caption\" href=\"#\" class=\"figure-viewer__ctrl__caption\"><span class=\"icon-doublearrow\"></span><span class=\"figure-viewer__caption__label\">Caption</span></a><span class=\"figure-viewer__title__text\"></span></div>\n" +
          "                    <div class=\"figure-viewer__hold__figcap\"></div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </div>\n" +
          "        \n" +
          "        <!-- Bot analytics test -->\n" +
          "        <script src=\"https://api.b2c.com/api/init-260u9k3l7hxtcferh76.js\" data-cfasync=\"false\" async></script>\n" +
          "        <noscript><img src=\"https://api.b2c.com/api/noscript-260u9k3l7hxtcferh76.gif\"></noscript>\n" +
          "        <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb-assets/styles/branding_ajslp-1541975204007.css\">\n" +
          "    </div>\n" +
          "</div>\n" +
          "<script type=\"text/javascript\" src=\"/products/photo-theme/asha/releasedAssets/js/main.bundle.js\"></script>\n" +
          "<script type=\"text/javascript\" src=\"/wro/product.js\"></script>\n" +
          "</body>\n" +
          "</html>";

  private static final String tocContentHashFiltered = " Clinical Focus Clinical Focus 08 March 2018 Audiological Assessment of Word Recognition Skills in Persons With Aphasia Author1 , Author2 , https://doi.org/part1/part2 Preview Abstract Purpose section Abstract Full text PDF issue item header section issue item body section accordion section Abstract Full text PDF issue item section issue item section Research Articles Research Articles issue item header section Research Articles issue item body section Research Articles accordion section Abstract Full text PDF Review Article Review Article issue item header section Review Article issue item body section Review Article accordion section Abstract Full text PDF ";
  private static final String doiFullContentHashFiltered = " Clinical Focus Clinical Focus 08 March 2018 Audiological Assessment of Word Recognition Skills in Persons With Aphasia Author1 , Author2 , https://doi.org/part1/part2 Preview Abstract Purpose section Abstract Full text PDF issue item header section issue item body section accordion section Abstract Full text PDF issue item section issue item section Research Articles Research Articles issue item header section Research Articles issue item body section Research Articles accordion section Abstract Full text PDF Review Article Review Article issue item header section Review Article issue item body section Review Article accordion section Abstract Full text PDF ";
  private static final String doiAbsContentHashFiltered = " Abstract Purpose some content Method some content Result some content Conclusion some content ";
  private static final String doiPdfContentHashFiltered = " PDF Content Background some content Purpose some content Method some content Results some content Conclusions some content ";

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
  }

  private Configuration thisAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "abc");
    conf.put("volume_name", "99");
    return conf;
  }

  private static String getUTF8String(String str) {
    return java.nio.charset.StandardCharsets.UTF_8.encode(str).toString();
  }

  private static String getFilteredContent(ArchivalUnit au, FilterFactory fact, String nameToHash) {

    InputStream actIn;
    String filteredStr = "";

    try {
      actIn = fact.createFilteredInputStream(au,
              new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);

      try {
        filteredStr = StringUtil.fromInputStream(actIn);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (PluginException e) {
      e.printStackTrace();
    }

    return filteredStr;
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

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();
    startMockDaemon();
    mau = createAu();
  }

  private static void compareContentLineByLine(String before, String after) {
    String[] beforeArr = before.split("\n");
    String[] afterArr = after.split("\n");

    assertEquals(beforeArr.length, afterArr.length);

    int len = beforeArr.length;

    for (int i = 0, sb1_i = 0, sb2_i = 0;  i < len; i++, sb1_i++, sb2_i++) {
      StringBuilder sb1 = new StringBuilder();
      StringBuilder sb2 = new StringBuilder();

      sb1.append(beforeArr[i].replaceAll("\\s+", ""));
      sb2.append(afterArr[i].replaceAll("\\s+", ""));

      assertEquals(sb1.toString(), sb2.toString());

      sb1.setLength(0);
      sb2.setLength(0);
    }
  }

  public void testTocContentLengthComparision() {

    int tocContentLen = tocContent.length();
    int tocContentCrawlFilteredLen = tocContentCrawlFiltered.length();
    int tocContentHashFilteredLen = tocContentHashFiltered.length();

    assertTrue(tocContentLen > 0);
    assertTrue(tocContentCrawlFilteredLen > 0);
    assertTrue(tocContentHashFilteredLen > 0);
  }

  public void testDoiFullContentLengthComparision() {

    int doiFullContentLen = doiFullContent.length();
    int doiFullContentCrawlFilteredLen = doiFullContentCrawlFiltered.length();
    int doiFullContentHashFilteredLen = doiFullContentHashFiltered.length();

    assertTrue(doiFullContentLen > 0);
    assertTrue(doiFullContentCrawlFilteredLen > 0);
    assertTrue(doiFullContentHashFilteredLen > 0);
  }

  public void testDoiAbsContentLengthComparision() {

    int doiAbsContentLen = doiAbsContent.length();
    int doiAbsContentCrawlFilteredLen = doiAbsContentCrawlFiltered.length();
    int doiAbsContentHashFilteredLen = doiAbsContentHashFiltered.length();

    assertTrue(doiAbsContentLen > 0);
    assertTrue(doiAbsContentCrawlFilteredLen > 0);
    assertTrue(doiAbsContentHashFilteredLen > 0);
    assertTrue(doiAbsContentLen > doiAbsContentCrawlFilteredLen);
    assertTrue(doiAbsContentLen > doiAbsContentHashFilteredLen);
  }

  public void testPdfContentLengthComparision() {

    int doiPdfContentLen = doiPdfContent.length();
    int doiPdfContentCrawlFilteredLen = doiPdfContentCrawlFiltered.length();
    int doiPdfContentHashFilteredLen = doiPdfContentHashFiltered.length();

    assertTrue(doiPdfContentLen > 0);
    assertTrue(doiPdfContentCrawlFilteredLen > 0);
    assertTrue(doiPdfContentHashFilteredLen > 0);
    assertTrue(doiPdfContentLen > doiPdfContentCrawlFilteredLen);
    assertTrue(doiPdfContentLen > doiPdfContentHashFilteredLen);
  }

  public static class TestCrawl extends TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory {

    public void testTocContentFiltering() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantCrawlFact, tocContent);
      String unicodeExpectedStr = tocContentCrawlFiltered;
      compareContentLineByLine(unicodeFilteredStr, unicodeExpectedStr);
    }


    public void testFullContentFiltering() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantCrawlFact, doiFullContent);
      String unicodeExpectedStr = doiFullContentCrawlFiltered;
      compareContentLineByLine(unicodeFilteredStr, unicodeExpectedStr);
    }


    public void testAbsContentFiltering() throws Exception {
      String unicodeFilteredStr = getUTF8String(getFilteredContent(mau, variantCrawlFact, doiAbsContent));
      String unicodeExpectedStr = getUTF8String(doiAbsContentCrawlFiltered);
      compareContentLineByLine(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testPdfContentFiltering() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantCrawlFact, doiPdfContent);
      String unicodeExpectedStr = doiPdfContentCrawlFiltered;
      compareContentLineByLine(unicodeFilteredStr, unicodeExpectedStr);
    }

  }

  public static class TestHash extends TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory {

    public void testManifestHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, manifestContent);
      String unicodeExpectedStr = manifestHashFiltered;
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testTocContentHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, tocContent);
      String unicodeExpectedStr = tocContentHashFiltered;

      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }


    public void testFullContentHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, doiFullContent);
      String unicodeExpectedStr = doiFullContentHashFiltered;

      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }


    public void testAbsContentHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, doiAbsContent);
      String unicodeExpectedStr = doiAbsContentHashFiltered;
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

    public void testPdfContentHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, doiPdfContent);
      String unicodeExpectedStr = doiPdfContentHashFiltered;
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }

  }

  public static Test suite() {
    return variantSuites(new Class[] {
            TestCrawl.class,
            TestHash.class
    });
  }

}
