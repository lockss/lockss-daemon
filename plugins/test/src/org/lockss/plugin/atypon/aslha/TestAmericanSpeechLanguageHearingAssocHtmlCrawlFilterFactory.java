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

import org.lockss.plugin.atypon.seg.TestSEGArchivalUnit;
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

  private static Logger log = Logger.getLogger(
          TestAmericanSpeechLanguageHearingAssocHtmlCrawlFilterFactory.class);


  private static final String PLUGIN_ID =
          "org.lockss.plugin.atypon.aslha.AmericanSpeechLanguageHearingAssocAtyponPlugin";

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
          "            <div  class=\"page-top-banner\">\n" +
          "                <div class=\"container\">\n" +
          "                    <div  class=\"row title__row\">\n" +
          "                        <div class=\"col-xs-12\">\n" +
          "                            <h2 class=\"page__title\">Table of contents</h2>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div  class=\"row\">\n" +
          "                    <div class=\"col-xs-12\">\n" +
          "                        <div class=\"publication__menu\">\n" +
          "                            <div class=\"publication__menu__journal__logo\">\n" +
          "                                <div  class=\"publication__menu__journal__logo\">\n" +
          "                                    <a href=\"/journal/aja\" ><img  src=\"/pb-assets/images/logos/journal_aja-1541785550173.png\"/></a>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <ul class=\"rlist publication__menu__list\">\n" +
          "                                <li><a href=\"/journal/aja\"  class=\"publication__menu__link\">HOME</a></li>\n" +
          "                                <li><a href=\"/loi/aja\"  class=\"publication__menu__link\">ISSUES</a></li>\n" +
          "                                <li><a href=\"/toc/aja/0/0\"  class=\"publication__menu__link\">NEWLY PUBLISHED</a></li>\n" +
          "                                <li><a href=\"/aja/subscribe\"  class=\"publication__menu__link\">SUBSCRIBE</a></li>\n" +
          "                                <li><a href=\"/aja/forlibrarians\"  class=\"publication__menu__link\">RECOMMEND TO A LIBRARIAN</a></li>\n" +
          "                            </ul>\n" +
          "                            <div  class=\"journal-search\">\n" +
          "                                <div class=\"quick-search quick-search--journal\">\n" +
          "                                    <div class=\"full-width\">\n" +
          "                                        <a href=\"#\" title=\"search\" data-db-target-for=\"thisJournalQuickSearch\" class=\"quick-search__toggler lg-hidden\"><i aria-hidden=\"true\" class=\"block-icon icon-search\"></i></a>\n" +
          "                                        <div  class=\"dropBlock__holder quick-search__dropBlock lg-opened\">\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div class=\"page__content padding-wrapper\">\n" +
          "                            <div  class=\"row\">\n" +
          "                                <div class=\"col-lg-8 col-md-8\">\n" +
          "                                    <div class=\"current-issue\">\n" +
          "                                        <div class=\"current-issue__cover\"><a href=\"/journal/aja\"><img src=\"/cms/attachment/test/aja.27.issue-1.cover.gif\" alt=\"American Journal of Audiology cover\"></a></div>\n" +
          "                                        <div class=\"current-issue__info\">\n" +
          "                                            <div class=\"current-issue__details\">\n" +
          "                                                <div class=\"current-issue__specifics\"><span>Volume 27</span><span>Issue 1</span><span>March 2018</span></div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"hidden-lg hidden-md\">\n" +
          "                                        <div class=\"transplant showit\" ></div>\n" +
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
          "                                                    <span  >Clinical Focus</span>\n" +
          "                                                    <span>08 March 2018</span>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    <div class=\"issue-item__title\">\n" +
          "                                                        <a href=\"/doi/part1/part2\" title=\"test data\">\n" +
          "                                                            <h5>test data</h5>\n" +
          "                                                        </a>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__authors\">\n" +
          "                                                        <ul   class=\"rlist--inline loa js--truncate\">\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=testdata\" title=\"Author1\">\n" +
          "                                                                    <span>Author1</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=testdata\" title=\"Author2\">\n" +
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
          "                                    <div  class=\"table-of-content__navigation\">\n" +
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
          "                                    <div  class=\"row\">\n" +
          "                                        <div class=\"col-lg-12 col-md-12 col-sm-6\">\n" +
          "                                            <div  class=\"advertisement\">\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div  class=\"row\">\n" +
          "                            <div class=\"col-md-12\">\n" +
          "                                <div  class=\"advertisement text-center hidden-xs hidden-sm\">\n" +
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

  private static String tocContentCrawlFiltered = "<html lang=\"en\">\n" +
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
          "                <div  class=\"row\">\n" +
          "                    <div class=\"col-xs-12\">\n" +
          "                        \n" +
          "                        <div class=\"page__content padding-wrapper\">\n" +
          "                            <div  class=\"row\">\n" +
          "                                <div class=\"col-lg-8 col-md-8\">\n" +
          "                                    \n" +
          "                                    <div class=\"hidden-lg hidden-md\">\n" +
          "                                        <div class=\"transplant showit\" ></div>\n" +
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
          "                                                    <span  >Clinical Focus</span>\n" +
          "                                                    <span>08 March 2018</span>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    <div class=\"issue-item__title\">\n" +
          "                                                        <a href=\"/doi/part1/part2\" title=\"test data\">\n" +
          "                                                            <h5>test data</h5>\n" +
          "                                                        </a>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__authors\">\n" +
          "                                                        <ul   class=\"rlist--inline loa js--truncate\">\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=testdata\" title=\"Author1\">\n" +
          "                                                                    <span>Author1</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=testdata\" title=\"Author2\">\n" +
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
          "                                    <div  class=\"row\">\n" +
          "                                        <div class=\"col-lg-12 col-md-12 col-sm-6\">\n" +
          "                                            \n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div  class=\"row\">\n" +
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
          "                <div  class=\"row\">\n" +
          "                    <div class=\"col-xs-12\">\n" +
          "\n" +
          "                        <div class=\"page__content padding-wrapper\">\n" +
          "                            <div  class=\"row\">\n" +
          "                                <div class=\"col-lg-8 col-md-8\">\n" +
          "\n" +
          "                                    <div class=\"hidden-lg hidden-md\">\n" +
          "                                        <div class=\"transplant showit\" ></div>\n" +
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
          "                                                    <span  >Clinical Focus</span>\n" +
          "                                                    <span>08 March 2018</span>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    <div class=\"issue-item__title\">\n" +
          "                                                        <a href=\"/doi/part1/part2\" title=\"test data\">\n" +
          "                                                            <h5>test data</h5>\n" +
          "                                                        </a>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__authors\">\n" +
          "                                                        <ul   class=\"rlist--inline loa js--truncate\">\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=testdata\" title=\"Author1\">\n" +
          "                                                                    <span>Author1</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=testdata\" title=\"Author2\">\n" +
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
          "                                    <div  class=\"row\">\n" +
          "                                        <div class=\"col-lg-12 col-md-12 col-sm-6\">\n" +
          "\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div  class=\"row\">\n" +
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
          "                <div  class=\"row\">\n" +
          "                    <div class=\"col-xs-12\">\n" +
          "\n" +
          "                        <div class=\"page__content padding-wrapper\">\n" +
          "                            <div  class=\"row\">\n" +
          "                                <div class=\"col-lg-8 col-md-8\">\n" +
          "\n" +
          "                                    <div class=\"hidden-lg hidden-md\">\n" +
          "                                        <div class=\"transplant showit\" ></div>\n" +
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
          "                                                    <span  >Clinical Focus</span>\n" +
          "                                                    <span>08 March 2018</span>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"issue-item__body\">\n" +
          "                                                    <div class=\"issue-item__title\">\n" +
          "                                                        <a href=\"/doi/part1/part2\" title=\"test data\">\n" +
          "                                                            <h5>test data</h5>\n" +
          "                                                        </a>\n" +
          "                                                    </div>\n" +
          "                                                    <div class=\"issue-item__authors\">\n" +
          "                                                        <ul   class=\"rlist--inline loa js--truncate\">\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=testdata\" title=\"Author1\">\n" +
          "                                                                    <span>Author1</span>\n" +
          "                                                                </a>,\n" +
          "                                                            </li>\n" +
          "                                                            <li>\n" +
          "                                                                <a href=\"/action/doSearch?ContribAuthorStored=testdata\" title=\"Author2\">\n" +
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
          "                                    <div  class=\"row\">\n" +
          "                                        <div class=\"col-lg-12 col-md-12 col-sm-6\">\n" +
          "\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div  class=\"row\">\n" +
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
          "                    <div  class=\"col-xs-12\">\n" +
          "                        <div class=\"publication__menu\">\n" +
          "                            <div class=\"publication__menu__journal__logo\">\n" +
          "                                <div  class=\"publication__menu__journal__logo\">\n" +
          "                                    <a href=\"/journal/aja\" ><img  src=\"/pb-assets/images/logos/journal_aja-1541785550173.png\"/></a>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <ul class=\"rlist publication__menu__list\">\n" +
          "                                <li><a href=\"/journal/aja\"  class=\"publication__menu__link\">HOME</a></li>\n" +
          "                                <li><a href=\"/loi/aja\"  class=\"publication__menu__link\">ISSUES</a></li>\n" +
          "                                <li><a href=\"/toc/aja/0/0\"  class=\"publication__menu__link\">NEWLY PUBLISHED</a></li>\n" +
          "                                <li><a href=\"/aja/subscribe\"  class=\"publication__menu__link\">SUBSCRIBE</a></li>\n" +
          "                                <li><a href=\"/aja/forlibrarians\"  class=\"publication__menu__link\">RECOMMEND TO A LIBRARIAN</a></li>\n" +
          "                            </ul>\n" +
          "                            <div  class=\"journal-search\">\n" +
          "                                <div class=\"quick-search quick-search--journal\">\n" +
          "                                    <div class=\"full-width\">\n" +
          "                                        <a href=\"#\" title=\"search\" data-db-target-for=\"thisJournalQuickSearch\" class=\"quick-search__toggler lg-hidden\"><i aria-hidden=\"true\" class=\"block-icon icon-search\"></i></a>\n" +
          "                                        <div  class=\"dropBlock__holder quick-search__dropBlock lg-opened\">\n" +
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
          "                                <h1 class=\"citation__title\"><a href=\"/doi/full/part1/part2\" >test</a></h1>\n" +
          "                                <ul  class=\"meta__authors rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" +
          "                                    <li><a href=\"#\" title=\"Author1\"  class=\"w-slide__btn\"><span>Author1</span></a></li>\n" +
          "                                    ,\n" +
          "                                    <li><a href=\"#\" title=\"Author2\"  class=\"w-slide__btn\"><span>Author2</span></a></li>\n" +
          "                                    ,\n" +
          "                                </ul>\n" +
          "                                <div  class=\"loa-wrapper hidden-xs\">\n" +
          "                                    <div id=\"sb-1\" class=\"accordion\">\n" +
          "                                        <div class=\"accordion-tabbed loa-accordion\">\n" +
          "                                            <div class=\"accordion-tabbed__tab accordion__closed \">\n" +
          "                                                <a href=\"#\" data-id=\"a1\"  title=\"Author1\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author1</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
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
          "                                                <a href=\"#\" data-id=\"a2\" title=\"Author2\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author2</span></a>\n" +
          "                                                <div data-db-target-of=\"a2\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
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
          "                                        <a href=\"#\"  class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul  class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                       <a href=\"#\"  data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul  class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
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
          "                                        <a href=\"#\"  class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul  class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                       <a href=\"#\"  data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul  class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
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
          "                                    <div  class=\"abstractSection abstractInFull\">\n" +
          "                                        <div id=\"acd3e286\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e286\">Purpose</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e295\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e295\">Method</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e304\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e304\">Result</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e313\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e313\">Conclusion</h3>\n" +
          "                                        <p  >\n" +
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
          "                                <div   class=\"response\">\n" +
          "                                    <div class=\"sub-article-title\"></div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div class=\"col-sm-4 sticko__parent article-row-right hidden-xs hidden-sm\">\n" +
          "                            <div class=\"tab tab--slide tab--flex sticko__md dynamic-sticko  tab--flex tabs--xs\">\n" +
          "                                <ul data-mobile-toggle=\"slide\" role=\"tablist\" class=\"rlist tab__nav w-slide--list\">\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-figures\"   class=\"figures-tab\"><i aria-hidden=\"true\" class=\"icon-photo\"></i><span>Figures</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-references\"  role=\"tab\"   class=\"references-tab\"><i aria-hidden=\"true\" class=\"icon-references\"></i><span>References</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-related\"  role=\"tab\"  data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><i aria-hidden=\"true\" class=\"icon-related\"></i><span>Related</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-details\" role=\"tab\"  data-slide-target=\"#pane-pcw-details\" class=\"details-tab\"><i aria-hidden=\"true\" class=\"icon-info\"></i><span>Details</span></a></li>\n" +
          "                                </ul>\n" +
          "                                <ul class=\"rlist tab__content sticko__child\">\n" +
          "                                    <li id=\"pane-pcw-figures\"  role=\"tabpanel\" class=\"tab__pane\"></li>\n" +
          "                                    <li id=\"pane-pcw-references\"  role=\"tabpanel\" class=\"tab__pane\"></li>\n" +
          "                                    <li id=\"pane-pcw-related\"  role=\"tabpanel\" class=\"accordion-with-arrow tab__pane tab__pane--clear\">\n" +
          "                                        <div class=\"accordion\">\n" +
          "                                            <ul class=\"accordion-tabbed rlist\">\n" +
          "                                                <li class=\"accordion-tabbed__tab\">\n" +
          "                                                    <a href=\"#\" title=\"Cited By\" aria-expanded=\"false\" aria-controls=\"relatedTab1\" class=\"accordion-tabbed__control\">Cited By</a>\n" +
          "                                                    <div id=\"relatedTab1\" class=\"accordion-tabbed__content\">\n" +
          "                                                        <div class=\"citedBySection\">\n" +
          "                                                            <div class=\"citedByEntry\"><span class=\"entryAuthor\"><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a> and </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a></span></span> <span class=\"pub-year\">(2018)</span> <a href=\"https://doi.org/10.1044/2018_AJA-18-0050\" target=\"_blank\" class=\"cited-link\">Evaluating Hearing Aid Management: Development of the Hearing Aid Skills and Knowledge Inventory (HASKI)</a><span class=\"seperator\">, </span><span class=\"seriesTitle\">American Journal of Audiology</span><span class=\"seperator\">, </span><span class=\"volume\"><b>27</b></span><span class=\"issue\">:3</span><span class=\"seperator\">, </span><span class=\"page-range\"> (333-348)</span><span class=\"seperator\">, </span><span class=\"pub-date\">Online publication date: 12-Sep-2018</span>.</div>\n" +
          "                                                            <div class=\"citedByEntry\"><span class=\"entryAuthor\"><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a>, </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a> and </span><span class=\"hlFld-ContribAuthor\"><a href=\"/author/test\">test</a></span></span> <span class=\"pub-year\">(2018)</span> <a href=\"https://doi.org/10.1044/2018_AJA-18-0053\" target=\"_blank\" class=\"cited-link\">Factors Associated With Self-Reported Hearing Aid Management Skills and Knowledge</a><span class=\"seperator\">, </span><span class=\"seriesTitle\">American Journal of Audiology</span><span class=\"seperator\">, </span><span class=\"volume\"><b>27</b></span><span class=\"issue\">:4</span><span class=\"seperator\">, </span><span class=\"page-range\"> (604-613)</span><span class=\"seperator\">, </span><span class=\"pub-date\">Online publication date: 6-Dec-2018</span>.</div>\n" +
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
          "                                                                        <ul  class=\"meta__authors rlist--inline loa mobile-authors\" title=\"list of authors\">\n" +
          "                                                                            <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/test\" title=\"test\">test</a></span> and\n" +
          "                                                                            </li>\n" +
          "                                                                            <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/test\" title=\"test\">test</a></span></li>\n" +
          "                                                                        </ul>\n" +
          "                                                                        <div class=\"meta\"><a href=/toc/ashaarii/21/2>Vol. 21, No. 2\n" +
          "                                                                        </a><time datetime=\"November 2018\">November 2018</time>\n" +
          "                                                                        </div>\n" +
          "                                                                    </div>\n" +
          "                                                                    <div class=\"lazy-load\">\n" +
          "                                                                        <div class=\"lazy-load__text\">\n" +
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
          "                                            <div class=\"cover-details-image\"><a href=\"/toc/aja/27/1\"><img src=\"/cms/attachment/test/aja.27.issue-1.cover.gif\"></a></div>\n" +
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
          "                                                <li><a href=\"http://www.copyright.com/openurl.do?issn=testtest\" id=\"PermissionsLink\" class=\"badge-type\" target=\"_blank\">Get Permissions</a></li>\n" +
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
          "                                                <div  data-doi=\"10.1044/2017_AJA-17-0059\" class=\"altmetric-embed\"></div>\n" +
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
          "        <div id=\"figure-viewer\"  class=\"figure-viewer\">\n" +
          "            <div class=\"figure-viewer__reg__top clearfix\">\n" +
          "                <div class=\"figure-viewer__top__right\"><a href=\"#\" data-ux3-role=\"controller\" role=\"button\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"><span class=\"sr-only\">Close Figure Viewer</span></span></a></div>\n" +
          "                <div class=\"figure-viewer__top__left\"><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"><span class=\"sr-only\">Browse All Figures</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"><span class=\"sr-only\">Return to Figure</span></span></a><span class=\"zoomSlider js__zoom-slider ui-slider\">some data<input type=\"range\" id=\"figure-viewer__zoom-range\" class=\"zoom-range\"/></span><button class=\"figure-viewer__label__zoom icon-zoom zoom-in\"><span class=\"sr-only\">Zoom in</span></button><button class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"><span class=\"sr-only\">Zoom out</span></button></div>\n" +
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
          "                    <div  class=\"col-xs-12\">\n" +
          "                        \n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <article>\n" +
          "                    <div class=\"row\">\n" +
          "                        <div class=\"col-sm-8 col-md-8 article__content\">\n" +
          "                            <div class=\"citation\">\n" +
          "                                <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><span title=\"Restricted access\" class=\"article__access__type\"><i aria-hidden=\"true\" class=\"citation__acess__icon icon-lock\"></i><span class=\"citation__access__type no-access\">No Access</span></span></span><span class=\"citation__top__item\">American Journal of Audiology</span><span class=\"citation__top__item\">Research Article</span><span class=\"citation__top__item\">8 Mar 2018</span></div>\n" +
          "                                <h1 class=\"citation__title\"><a href=\"/doi/full/part1/part2\" >test</a></h1>\n" +
          "                                <ul  class=\"meta__authors rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" +
          "                                    <li><a href=\"#\" title=\"Author1\"  class=\"w-slide__btn\"><span>Author1</span></a></li>\n" +
          "                                    ,\n" +
          "                                    <li><a href=\"#\" title=\"Author2\"  class=\"w-slide__btn\"><span>Author2</span></a></li>\n" +
          "                                    ,\n" +
          "                                </ul>\n" +
          "                                <div  class=\"loa-wrapper hidden-xs\">\n" +
          "                                    <div id=\"sb-1\" class=\"accordion\">\n" +
          "                                        <div class=\"accordion-tabbed loa-accordion\">\n" +
          "                                            <div class=\"accordion-tabbed__tab accordion__closed \">\n" +
          "                                                <a href=\"#\" data-id=\"a1\"  title=\"Author1\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author1</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
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
          "                                                <a href=\"#\" data-id=\"a2\" title=\"Author2\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author2</span></a>\n" +
          "                                                <div data-db-target-of=\"a2\" class=\"author-info accordion-tabbed__content\">\n" +
          "                                                    <p class=\"author-type\"></p>\n" +
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
          "                                        <a href=\"#\"  class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul  class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                       <a href=\"#\"  data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul  class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
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
          "                                        <a href=\"#\"  class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul  class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/test\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                       <a href=\"#\"  data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul  class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
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
          "                                    <div  class=\"abstractSection abstractInFull\">\n" +
          "                                        <div id=\"acd3e286\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e286\">Purpose</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e295\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e295\">Method</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e304\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e304\">Result</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e313\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e313\">Conclusion</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <!-- /abstract content -->\n" +
          "                                <div class=\"article__references\">\n" +
          "                                    <p class=\"explanation__text\"></p>\n" +
          "                                    <h2>References</h2>\n" +
          "                                    <ul class=\"rlist separator\">\n" +
          "                                        \n" +
          "                                        \n" +
          "                                    </ul>\n" +
          "                                </div>\n" +
          "                                <div   class=\"response\">\n" +
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
          "        <div id=\"figure-viewer\"  class=\"figure-viewer\">\n" +
          "            <div class=\"figure-viewer__reg__top clearfix\">\n" +
          "                <div class=\"figure-viewer__top__right\"><a href=\"#\" data-ux3-role=\"controller\" role=\"button\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"><span class=\"sr-only\">Close Figure Viewer</span></span></a></div>\n" +
          "                <div class=\"figure-viewer__top__left\"><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"><span class=\"sr-only\">Browse All Figures</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"><span class=\"sr-only\">Return to Figure</span></span></a><span class=\"zoomSlider js__zoom-slider ui-slider\">some data<input type=\"range\" id=\"figure-viewer__zoom-range\" class=\"zoom-range\"/></span><button class=\"figure-viewer__label__zoom icon-zoom zoom-in\"><span class=\"sr-only\">Zoom in</span></button><button class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"><span class=\"sr-only\">Zoom out</span></button></div>\n" +
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
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <header class=\"header fixed\">\n" +
          "            header section\n" +
          "        </header>\n" +
          "        <main class=\"content article-page journal-branding\">\n" +
          "            <div class=\"page-top-panel\"></div>\n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div  class=\"col-xs-12\">\n" +
          "                        <div class=\"publication__menu\">\n" +
          "                            <div class=\"publication__menu__journal__logo\">\n" +
          "                                <div  class=\"publication__menu__journal__logo\">\n" +
          "                                    <a href=\"/journal/ajslp\" title=\"ajslp Journal\"><img alt=\"ajslp Journal\" src=\"/pb-assets/images/logos/journal_ajslp-1544488691423.png\"/></a>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <ul class=\"rlist publication__menu__list\">\n" +
          "                                <li><a href=\"/journal/ajslp\"  class=\"publication__menu__link\">HOME</a></li>\n" +
          "                                <li><a href=\"/loi/ajslp\"  class=\"publication__menu__link\">ISSUES</a></li>\n" +
          "                                <li><a href=\"/toc/ajslp/0/0\"  class=\"publication__menu__link\">NEWLY PUBLISHED</a></li>\n" +
          "                                <li><a href=\"/ajslp/subscribe\"  class=\"publication__menu__link\">SUBSCRIBE</a></li>\n" +
          "                                <li><a href=\"/ajslp/forlibrarians\"  class=\"publication__menu__link\">RECOMMEND TO A LIBRARIAN</a></li>\n" +
          "                            </ul>\n" +
          "                            <div  class=\"journal-search\">\n" +
          "                                <div class=\"quick-search quick-search--journal\">\n" +
          "                                    <div class=\"full-width\">\n" +
          "                                        <a href=\"#\" title=\"search\" data-db-target-for=\"thisJournalQuickSearch\" class=\"quick-search__toggler lg-hidden\"><i aria-hidden=\"true\" class=\"block-icon icon-search\"></i></a>\n" +
          "                                        <div  class=\"dropBlock__holder quick-search__dropBlock lg-opened\">\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <article >\n" +
          "                    <div class=\"row\">\n" +
          "                        <div class=\"col-sm-8 col-md-8 article__content\">\n" +
          "                            <div class=\"citation\">\n" +
          "                                <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><span title=\"Restricted access\" class=\"article__access__type\"><i aria-hidden=\"true\" class=\"citation__acess__icon icon-lock\"></i><span class=\"citation__access__type no-access\">No Access</span></span></span><span class=\"citation__top__item\">random test content</span><span class=\"citation__top__item\">Research Article</span><span class=\"citation__top__item\">21 Nov 2018</span></div>\n" +
          "                                <h1 class=\"citation__title\"><a href=\"/doi/full/part1/part2\" title=\"some data\">some data</a></h1>\n" +
          "                                <ul  class=\"meta__authors rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" +
          "                                   ul content\n" +
          "                                </ul>\n" +
          "                                <div  class=\"loa-wrapper hidden-xs\">\n" +
          "                                    <div id=\"sb-1\" class=\"accordion\">\n" +
          "                                        <div class=\"accordion-tabbed loa-accordion\">\n" +
          "                                            <div class=\"accordion-tabbed__tab accordion__closed \">\n" +
          "                                                <a href=\"#\" data-id=\"a1\"  title=\"Author1\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author1</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
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
          "                                                <a href=\"#\" data-id=\"a2\" title=\"Author2\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author2</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
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
          "                                        <a href=\"#\"  class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" ><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul  class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                       <a href=\"#\"  data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul  class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
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
          "                                        <a href=\"#\"  class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul  class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                       <a href=\"#\"  data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul  class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
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
          "                                        <h2 class=\" article-section__title section__title to-section    \" id=\"d7109e1\">PDF Content</h2>\n" +
          "                                    </div>\n" +
          "                                    <div  class=\"abstractSection abstractInFull\">\n" +
          "                                        <div id=\"acd3e249\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e249\">Background</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e258\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e258\">Purpose</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e267\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e267\">Method</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e276\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e276\">Results</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e285\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e285\">Conclusions</h3>\n" +
          "                                        <p  >some content\n" +
          "                                        </p>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <!-- /abstract content -->\n" +
          "                                <div class=\"article__references\">\n" +
          "                                    <p class=\"explanation__text\"></p>\n" +
          "                                    <h2>References</h2>\n" +
          "                                    <ul class=\"rlist separator\">\n" +
          "                                        <li id=\"bib1\" class=\"references__item \">\n" +
          "                                       <span xmlns:xje=\"link\" xmlns:fn=\"link\" class=\"references__note\">\n" +
          "                                          (\n" +
          "                                          <span class=\"references__year\">1995</span>).\n" +
          "                                          <span class=\"references__article-title\">some content</span>.\n" +
          "                                          <span class=\"references__source\">\n" +
          "                                          <strong>Neurotoxicology and Teratology</strong>\n" +
          "                                          </span>,\n" +
          "                                          <b>17</b>(4), 445–462.\n" +
          "                                          <a class=\"references__doi\" href=\"https://doi.org/\">\n" +
          "                                          <span class=\"references__doi__label\">\n" +
          "                                          DOI:\n" +
          "                                          </span>https://doi.org/part1/part2\n" +
          "                                          </a>\n" +
          "                                          <span class=\"references__suffix\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=someyear\"target=\"_blank\">Find this author on Google Scholar</a>\n" +
          "                                          </span>\n" +
          "                                       </span>\n" +
          "                                        </li>\n" +
          "                                    </ul>\n" +
          "                                </div>\n" +
          "                                <div   class=\"response\">\n" +
          "                                    <div class=\"sub-article-title\"></div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div  class=\"eCommercePurchaseAccessWidget\">\n" +
          "                                <div id=\"purchaseArea\" class=\"purchaseArea\">\n" +
          "                                    <h2>Access content</h2>\n" +
          "                                    <h5>Login Options:</h5>\n" +
          "                                    <ul class=\"rlist loginOptions\">\n" +
          "                                        <li class=\"accordion\">\n" +
          "                                            <a href=\"#\" class=\"accordion__control expand-link\">Member login</a>\n" +
          "                                            <div class=\"content accordion__content\">\n" +
          "                                                <div class=\"login-form\">\n" +
          "                                                    <form action=\"/action/doLogin\" method=\"post\">\n" +
          "                                                       login form\n" +
          "                                                    </form>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </li>\n" +
          "                                        <li><a href=\"/action/ssostart?redirectUri=%2Fdoi%2Fpdf%2F10.1044%2F2018_AJSLP-17-0013\">Institutional login</a></li>\n" +
          "                                    </ul>\n" +
          "                                    <hr/>\n" +
          "                                    <div class=\"eCommercePurchaseAccessWidgetContainer \">\n" +
          "                                        <a data-bind=\"expandSection\" class=\"purchaseAreaList_expand active\" href=\"#purchasePanel\" id=\"purchaseLink\">Purchase</a>\n" +
          "                                        <a data-bind=\"saveItem\" class=\"save-for-later-link\" href=\"/action/saveItem?doi=10.1044/2018_AJSLP-17-0013\">\n" +
          "                                            Save for later\n" +
          "                                        </a>\n" +
          "                                        <a class=\"saved-go-cart hidden\"  href=\"/action/showCart?FlowID=2\"><span class=\"icon-cart-icon-material\"></span> Item saved, go to cart </a>\n" +
          "                                        <div class=\"purchaseAreaList_expanded\" id=\"#purchasePanel\">\n" +
          "                                            <div class=\"purchase-options-container\">\n" +
          "                                                <div class=\"savedItem-info hidden\">\n" +
          "                                                    <p></p>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"purchaseMessage hidden\" >\n" +
          "                                                    <p class=\"errorMsgBox\"></p>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"add-journal-to-cart\">\n" +
          "                                                    <header>\n" +
          "                                                        <a data-bind=\"addItem\""+
          "                                                           class=\"add-article-to-cart\"\n" +
          "                                                           href=\"/action/addToCart?id=someid\">\n" +
          "                                                <span class=\"add-article-to-cart__title title\">\n" +
          "                                                test data\n" +
          "                                                </span>\n" +
          "                                                            <span class=\"add-article-to-cart__price price\">$60.00</span>\n" +
          "                                                            <span class=\"add-to-cart-msg\">\n" +
          "                                                <span class=\"icon-shopping_cart\"></span>\n" +
          "                                                Add to cart\n" +
          "                                                </span>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"purchaseMessage hidden info\" >\n" +
          "                                                            <p></p>\n" +
          "                                                        </div>\n" +
          "                                                    </header>\n" +
          "                                                    <div class=\"addedMessage hidden\" >\n" +
          "                                                <span class=\"article-title\">\n" +
          "                                                <span class=\"article-title-content\">\n" +
          "                                                <i class=\"icon-check_circle\" aria-hidden=\"true\"></i>\n" +
          "                                                <span class=\"text\">\n" +
          "                                                test data\n" +
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
          "                                                <div class=\"purchaseMessage hidden\">\n" +
          "                                                    <p class=\"errorMsgBox\"></p>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"add-journal-to-cart\">\n" +
          "                                                    <header>\n" +
          "                                                        <a data-bind=\"addItem\" \n" +
          "                                                           class=\"add-article-to-cart\"\n" +
          "                                                           href=\"/action/addToCart?id=someid\">\n" +
          "                                                <span class=\"add-article-to-cart__title title\">\n" +
          "                                                some data\n" +
          "                                                </span>\n" +
          "                                                            <span class=\"add-article-to-cart__price price\">$30.00</span>\n" +
          "                                                            <span class=\"add-to-cart-msg\">\n" +
          "                                                <span class=\"icon-shopping_cart\"></span>\n" +
          "                                                Add to cart\n" +
          "                                                </span>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"purchaseMessage hidden info\">\n" +
          "                                                            <p></p>\n" +
          "                                                        </div>\n" +
          "                                                    </header>\n" +
          "                                                    <div class=\"addedMessage hidden\">\n" +
          "                                                <span class=\"article-title\">\n" +
          "                                                <span class=\"article-title-content\">\n" +
          "                                                <i class=\"icon-check_circle\" aria-hidden=\"true\"></i>\n" +
          "                                                <span class=\"text\">\n" +
          "                                                some data\n" +
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
          "                                                <div class=\"purchaseMessage hidden\" >\n" +
          "                                                    <p class=\"errorMsgBox\"></p>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"add-journal-to-cart\">\n" +
          "                                                    <header>\n" +
          "                                                        <a data-bind=\"addItem\"  \n" +
          "                                                           class=\"add-article-to-cart\"\n" +
          "                                                           href=\"/action/addToCart?id=someid\">\n" +
          "                                                <span class=\"add-article-to-cart__title title\">\n" +
          "                                                This Article 24-hour, One Article for 24 hours\n" +
          "                                                </span>\n" +
          "                                                            <span class=\"add-article-to-cart__price price\">$15.00</span>\n" +
          "                                                            <span class=\"add-to-cart-msg\">\n" +
          "                                                <span class=\"icon-shopping_cart\"></span>\n" +
          "                                                Add to cart\n" +
          "                                                </span>\n" +
          "                                                        </a>\n" +
          "                                                        <div class=\"purchaseMessage hidden info\" >\n" +
          "                                                            <p></p>\n" +
          "                                                        </div>\n" +
          "                                                    </header>\n" +
          "                                                    <div class=\"addedMessage hidden\" >\n" +
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
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-figures\"   class=\"figures-tab\"><i aria-hidden=\"true\" class=\"icon-photo\"></i><span>Figures</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-references\"  role=\"tab\"   class=\"references-tab\"><i aria-hidden=\"true\" class=\"icon-references\"></i><span>References</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-related\"  role=\"tab\"  data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><i aria-hidden=\"true\" class=\"icon-related\"></i><span>Related</span></a></li>\n" +
          "                                    <li role=\"presentation\"><a href=\"#pane-pcw-details\" role=\"tab\"  data-slide-target=\"#pane-pcw-details\" class=\"details-tab\"><i aria-hidden=\"true\" class=\"icon-info\"></i><span>Details</span></a></li>\n" +
          "                                </ul>\n" +
          "                                <ul class=\"rlist tab__content sticko__child\">\n" +
          "                                    <li id=\"pane-pcw-figures\"  role=\"tabpanel\" class=\"tab__pane\"></li>\n" +
          "                                    <li id=\"pane-pcw-references\"  role=\"tabpanel\" class=\"tab__pane\"></li>\n" +
          "                                    <li id=\"pane-pcw-related\"  role=\"tabpanel\" class=\"accordion-with-arrow tab__pane tab__pane--clear\">\n" +
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
          "                                                <li><a href=\"http://www.copyright.com/openurl.do?issn=test;contentid=test\" id=\"PermissionsLink\" class=\"badge-type\" target=\"_blank\">Get Permissions</a></li>\n" +
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
          "                                                <div  class=\"altmetric-embed\"></div>\n" +
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
          "        <div id=\"figure-viewer\"  class=\"figure-viewer\">\n" +
          "            <div class=\"figure-viewer__reg__top clearfix\">\n" +
          "                <div class=\"figure-viewer__top__right\"><a href=\"#\" data-ux3-role=\"controller\" role=\"button\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"><span class=\"sr-only\">Close Figure Viewer</span></span></a></div>\n" +
          "                <div class=\"figure-viewer__top__left\"><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"><span class=\"sr-only\">Browse All Figures</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"><span class=\"sr-only\">Return to Figure</span></span></a><span class=\"zoomSlider js__zoom-slider ui-slider\">some data<input type=\"range\" id=\"figure-viewer__zoom-range\" class=\"zoom-range\"/></span><button class=\"figure-viewer__label__zoom icon-zoom zoom-in\"><span class=\"sr-only\">Zoom in</span></button><button class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"><span class=\"sr-only\">Zoom out</span></button></div>\n" +
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
          "            <div  class=\"footer__wrapper\">\n" +
          "                <div class=\"footer__top\">\n" +
          "                    <div class=\"x-larg__container\">\n" +
          "                        <div  class=\"row footer__top__contnet\">\n" +
          "                            <div class=\"col-md-6\">\n" +
          "                                <div  class=\"row\">\n" +
          "                                    <div class=\"col-md-5 clearfix\">\n" +
          "                                        <div  class=\"footer__logo\">\n" +
          "                                            <div>\n" +
          "                                                <div  class=\"footer_logos\">\n" +
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
          "                                            <li><a href=\"/journal/first\">first</a></li>\n" +
          "                                            <li><a href=\"/journal/second\">second</a></li>\n" +
          "                                            <li><a href=\"/journal/jslhr\">third</a></li>\n" +
          "                                            <li><a href=\"/journal/lshss\">fourth</a></li>\n" +
          "                                            <li><a href=\"https://perspectives.pubs.asha.org\">fifth</a></li>\n" +
          "                                            <li>\n" +
          "                                                <hr>\n" +
          "                                            </li>\n" +
          "                                            <li><a href=\"https://leader.pubs.asha.org\">The ASHA Leader</a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div class=\"col-md-6\">\n" +
          "                                <div  class=\"row sitemap clearfix\">\n" +
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
          "                                        <div  class=\"sitemap__data\">\n" +
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
          "                    <div  class=\"x-larg__container\">\n" +
          "                        <div class=\"row\">\n" +
          "                            <div  class=\"col-xs-12 my-3\">\n" +
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
          "                            <div  class=\"col-xs-12 copywrites\">\n" +
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

          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";

  private static String doiPdfContentCrawlFiltered = "<html lang=\"en\" >\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    header section\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        \n" +
          "        <main class=\"content article-page journal-branding\">\n" +
          "            \n" +
          "            <div class=\"container shift-up-content\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div  class=\"col-xs-12\">\n" +
          "                        \n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <article >\n" +
          "                    <div class=\"row\">\n" +
          "                        <div class=\"col-sm-8 col-md-8 article__content\">\n" +
          "                            <div class=\"citation\">\n" +
          "                                <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><span title=\"Restricted access\" class=\"article__access__type\"><i aria-hidden=\"true\" class=\"citation__acess__icon icon-lock\"></i><span class=\"citation__access__type no-access\">No Access</span></span></span><span class=\"citation__top__item\">random test content</span><span class=\"citation__top__item\">Research Article</span><span class=\"citation__top__item\">21 Nov 2018</span></div>\n" +
          "                                <h1 class=\"citation__title\"><a href=\"/doi/full/part1/part2\" title=\"some data\">some data</a></h1>\n" +
          "                                <ul  class=\"meta__authors rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" +
          "                                   ul content\n" +
          "                                </ul>\n" +
          "                                <div  class=\"loa-wrapper hidden-xs\">\n" +
          "                                    <div id=\"sb-1\" class=\"accordion\">\n" +
          "                                        <div class=\"accordion-tabbed loa-accordion\">\n" +
          "                                            <div class=\"accordion-tabbed__tab accordion__closed \">\n" +
          "                                                <a href=\"#\" data-id=\"a1\"  title=\"Author1\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author1</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
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
          "                                                <a href=\"#\" data-id=\"a2\" title=\"Author2\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author2</span><i aria-hidden=\"true\" class=\"icon-mail\"></i></a>\n" +
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
          "                                        <a href=\"#\"  class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\" ><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul  class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                       <a href=\"#\"  data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul  class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
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
          "                                        <a href=\"#\"  class=\"main-link\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>PDF</span></a>\n" +
          "                                        <ul data-db-target-of=\"article-downloads-list\" id=\"article-downloads-list_Pop\" class=\"rlist\">\n" +
          "                                            <li><a href=\"/doi/abs/part1/part2\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Abstract</span></a></li>\n" +
          "                                            <li><a href=\"/doi/pdf/part1/part2\"><i aria-hidden=\"true\" class=\"icon-PDF\"></i><span>View PDF</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                        <a href=\"#\"><i aria-hidden=\"true\" class=\"icon-build\"></i><span>Tools</span></a>\n" +
          "                                        <ul  class=\"rlist\">\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-star\"></i><span>Add to favorites</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>Download Citations</span></a></li>\n" +
          "                                            <li role=\"presentation\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1044%2F2018_AJSLP-17-0013\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-my_location\"></i><span>Track Citations</span></a></li>\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                    <div>\n" +
          "                                       <a href=\"#\"  data-slide-clone=\"self\"><i aria-hidden=\"true\" class=\"icon-share\"></i><span>Share</span></a>\n" +
          "                                        <ul  class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style share__block \">\n" +
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
          "                                        <h2 class=\" article-section__title section__title to-section    \" id=\"d7109e1\">PDF Content</h2>\n" +
          "                                    </div>\n" +
          "                                    <div  class=\"abstractSection abstractInFull\">\n" +
          "                                        <div id=\"acd3e249\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e249\">Background</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e258\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e258\">Purpose</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e267\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e267\">Method</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e276\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e276\">Results</h3>\n" +
          "                                        <p  >\n" +
          "                                            some content\n" +
          "                                        </p>\n" +
          "                                        <div id=\"acd3e285\" class=\"anchor-spacer\"></div>\n" +
          "                                        <h3 class=\"article-section__title to-section\" id=\"d3e285\">Conclusions</h3>\n" +
          "                                        <p  >some content\n" +
          "                                        </p>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <!-- /abstract content -->\n" +
          "                                <div class=\"article__references\">\n" +
          "                                    <p class=\"explanation__text\"></p>\n" +
          "                                    <h2>References</h2>\n" +
          "                                    <ul class=\"rlist separator\">\n" +
          "                                        \n" +
          "                                    </ul>\n" +
          "                                </div>\n" +
          "                                <div   class=\"response\">\n" +
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
          "        <div id=\"figure-viewer\"  class=\"figure-viewer\">\n" +
          "            <div class=\"figure-viewer__reg__top clearfix\">\n" +
          "                <div class=\"figure-viewer__top__right\"><a href=\"#\" data-ux3-role=\"controller\" role=\"button\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"><span class=\"sr-only\">Close Figure Viewer</span></span></a></div>\n" +
          "                <div class=\"figure-viewer__top__left\"><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"><span class=\"sr-only\">Browse All Figures</span></span></a><a href=\"#\" role=\"button\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"><span class=\"sr-only\">Return to Figure</span></span></a><span class=\"zoomSlider js__zoom-slider ui-slider\">some data<input type=\"range\" id=\"figure-viewer__zoom-range\" class=\"zoom-range\"/></span><button class=\"figure-viewer__label__zoom icon-zoom zoom-in\"><span class=\"sr-only\">Zoom in</span></button><button class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"><span class=\"sr-only\">Zoom out</span></button></div>\n" +
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

  private static final String tocContentHashFiltered = " Clinical Focus Clinical Focus 08 March 2018 test data Author1 , Author2 , https://doi.org/part1/part2 Preview Abstract Purpose section Abstract Full text PDF issue item header section issue item body section accordion section Abstract Full text PDF issue item section issue item section Research Articles Research Articles issue item header section Research Articles issue item body section Research Articles accordion section Abstract Full text PDF Review Article Review Article issue item header section Review Article issue item body section Review Article accordion section Abstract Full text PDF ";
  private static final String doiFullContentHashFiltered = " Clinical Focus Clinical Focus 08 March 2018 test data Author1 , Author2 , https://doi.org/part1/part2 Preview Abstract Purpose section Abstract Full text PDF issue item header section issue item body section accordion section Abstract Full text PDF issue item section issue item section Research Articles Research Articles issue item header section Research Articles issue item body section Research Articles accordion section Abstract Full text PDF Review Article Review Article issue item header section Review Article issue item body section Review Article accordion section Abstract Full text PDF ";
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

  private static String getFilteredContent(ArchivalUnit au, FilterFactory fact, String nameToHash) {

    InputStream actIn;
    String filteredStr = "";

    try {
      actIn = fact.createFilteredInputStream(au,
              new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);

      try {
        filteredStr = StringUtil.fromInputStream(actIn);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      }
    } catch (PluginException e) {
      log.error(e.getMessage(), e);
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
      String unicodeFilteredStr = getFilteredContent(mau, variantCrawlFact, doiAbsContent);
      String unicodeExpectedStr = doiAbsContentCrawlFiltered;
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
