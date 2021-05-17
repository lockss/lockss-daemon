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

package org.lockss.plugin.atypon.rsp;

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

public class TestRoyalSocietyPublishingHtmlCrawlFilterFactory extends LockssTestCase {

  FilterFactory variantCrawlFact = new RoyalSocietyPublishingHtmlCrawlFilterFactory();
  FilterFactory variantHashFact = new RoyalSocietyPublishingHtmlHashFilterFactory();
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;

  private static Logger log = Logger.getLogger(
          TestRoyalSocietyPublishingHtmlCrawlFilterFactory.class);


  private static final String PLUGIN_ID =
          "org.lockss.plugin.atypon.rsp.RoyalSocietyPublishingAtyponPlugin";

  private static final String manifestContent = "<html>\n" +
          "<head>\n" +
          "    <title>Biographical Memoirs of Fellows of the Royal Society 2018 CLOCKSS Manifest Page</title>\n" +
          "    <meta charset=\"UTF-8\" />\n" +
          "</head>\n" +
          "<body>\n" +
          "<h1>Biographical Memoirs of Fellows of the Royal Society 2018 CLOCKSS Manifest Page</h1>\n" +
          "<ul>\n" +
          "    \n" +
          "    <li><a href=\"/toc/rsbm/64\">April 2018 (Vol. 64 Issue )</a></li>\n" +
          "    \n" +
          "</ul>\n" +
          "<p>\n" +
          "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>\n" +
          "    The CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.\n" +
          "</p>\n" +
          "</body>\n" +
          "</html>";

  private static final String manifestHashFiltered = " April 2018 (Vol. 64 Issue ) ";

  private static String tocContent = "<html lang=\"en\" class=\"pb-page\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    header section...\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <div>\n" +
          "            <header data-db-parent-of=\"sb1\" class=\"header fixed base pageHeader\">\n" +
          "               div header section...\n" +
          "            </header>\n" +
          "            <div>div form section...</div>\n" +
          "        </div>\n" +
          "        <main class=\"content rsbm\">\n" +
          "            <div class=\"rsbm \">rsbm logo</div>\n" +
          "            <div class=\"container\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div>\n" +
          "                        <div class=\"sections-navigations-mobile\">\n" +
          "                            <nav class=\"toc__section\">\n" +
          "                                <a class=\"w-slide__btn\" href=\"#\" data-db-target-for=\"sections\" data-db-switch=\"icon-close_thin\" title=\"Go To Sections\" data-slide-target=\"#sections\">\n" +
          "                                    <i class=\"icon-toc\"></i>\n" +
          "                                    <span>Sections</span>\n" +
          "                                </a>\n" +
          "                            </nav>\n" +
          "                        </div>\n" +
          "                        <div class=\"mobile-gutters col-md-9\">\n" +
          "                            <div class=\"table-of-content-navigable clearfix\">\n" +
          "                                <div class=\"col-md-3 navigation-column\">\n" +
          "                                    <div class=\"sticko__parent\">\n" +
          "                                        <div class=\"sticko__md dynamic-sticko\">\n" +
          "                                            <div class=\"sticko__child\">\n" +
          "                                                <div class=\"article-sections\">\n" +
          "                                                    <h4>Sections</h4>\n" +
          "                                                    <nav id=\"sections\" class=\"toc__topics-navigation sections-block scroll-to-target\">\n" +
          "                                                        <ul class=\"rlist\"></ul>\n" +
          "                                                    </nav>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <div class=\"col-md-9 line-separator--vertical toc-content\">\n" +
          "                                    <div class=\"table-of-content__header\">\n" +
          "                                        <h2>Table of Contents</h2>\n" +
          "                                        <div class=\"pb-dropzone\" data-pb-dropzone=\"tableOfContentDropzone\">\n" +
          "                                            <!-- Empty dropzone -->\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"table-of-content\">\n" +
          "                                        <h3 class=\"to-section section__title\" id=\"d6878631e37\">Editorial</h3>\n" +
          "                                        <div class=\"issue-item\">\n" +
          "                                            <div class=\"badges\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<span class=\"badge-type badge-full\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<i title=\"You have access\" class=\"icon-check\"></i>\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</span>\n" +
          "                                                <span class=\"badge-type\">Editorial</span>\n" +
          "                                            </div>\n" +
          "                                            <h5 class=\"issue-item__title\">\n" +
          "                                                <a href=\"/doi/10.1098/rsbm.2018.0002\">Editorial</a>\n" +
          "                                            </h5>\n" +
          "                                            <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"/action/\" title=\"first name\">\n" +
          "                                                        <span>first name</span>\n" +
          "                                                    </a>\n" +
          "                                                    <span>, </span>CBE FRS FRSE\n" +
          "\n" +
          "                                                </li>\n" +
          "                                            </ul>\n" +
          "                                            <div class=\"toc-item__detail\">\n" +
          "                                                <span>Published:</span>\n" +
          "                                                <strong>28 March 2018</strong>\n" +
          "                                                <span class=\"separator\"></span>\n" +
          "                                                <span>Article ID:</span>\n" +
          "                                                <strong>20180002</strong>\n" +
          "                                            </div>\n" +
          "                                            <p>\n" +
          "                                                <a href=\"https://doi.org/10.1098/rsbm.2018.0002\">https://doi.org/10.1098/rsbm.2018.0002</a>\n" +
          "                                            </p>\n" +
          "                                            <div class=\"toc-item__footer\">\n" +
          "                                                <ul class=\"rlist--inline separator toc-item__detail\">\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Abstract\" href=\"/doi/abs/10.1098/rsbm.2018.0002\">First Page</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Full text\" href=\"/doi/full/10.1098/rsbm.2018.0002\">Full text</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"PDF\" href=\"/doi/pdf/10.1098/rsbm.2018.0002\">\n" +
          "                                                            PDF\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/servlet/linkout?suffix=something\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">\n" +
          "                                                            <img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL Stanford University\" />\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                </ul>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <h3 class=\"to-section section__title\" id=\"d6878631e99\">Memoirs</h3>\n" +
          "                                        <div class=\"issue-item\">\n" +
          "                                            <div class=\"badges\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<span class=\"badge-type badge-full\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<i title=\"You have access\" class=\"icon-check\"></i>\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</span>\n" +
          "                                                <span class=\"badge-type\">Obituary</span>\n" +
          "                                            </div>\n" +
          "                                            <h5 class=\"issue-item__title\">\n" +
          "                                                <a href=\"/doi/10.1098/rsbm.2017.0016\">Vladimir Igorevich Arnold</a>\n" +
          "                                            </h5>\n" +
          "                                            <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"/action/doSearch?second\" title=\"second personn\">\n" +
          "                                                        <span>second person</span>\n" +
          "                                                    </a>\n" +
          "                                                    <span> and </span>\n" +
          "                                                </li>\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"/action/doSearch?third\" title=\"third\">\n" +
          "                                                        <span>third</span>\n" +
          "                                                    </a>\n" +
          "                                                </li>\n" +
          "                                            </ul>\n" +
          "                                            <div class=\"toc-item__detail\">\n" +
          "                                                <span>Published:</span>\n" +
          "                                                <strong>30 August 2017</strong>\n" +
          "                                                <span class=\"separator\"></span>\n" +
          "                                                <span>Article ID:</span>\n" +
          "                                                <strong>20170016</strong>\n" +
          "                                            </div>\n" +
          "                                            <p>\n" +
          "                                                <a href=\"https://doi.org/10.1098/rsbm.2017.0016\">https://doi.org/10.1098/rsbm.2017.0016</a>\n" +
          "                                            </p>\n" +
          "                                            <div class=\"toc-item__footer\">\n" +
          "                                                <ul class=\"rlist--inline separator toc-item__detail\">\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Abstract\" href=\"/doi/abs/10.1098/rsbm.2017.0016\">Abstract</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Full text\" href=\"/doi/full/10.1098/rsbm.2017.0016\">Full text</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"PDF\" href=\"/doi/pdf/10.1098/rsbm.2017.0016\">\n" +
          "                                                            PDF\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/doi/references/10.1098/rsbm.2017.0016\">\n" +
          "                                                            References\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/servlet/linkout?suffix=something\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">\n" +
          "                                                            <img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL Stanford University\" />\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                </ul>\n" +
          "                                                <div class=\"accordion\">\n" +
          "                                                    <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\">\n" +
          "                                                        <i class=\"icon-section_arrow_d\"></i>Preview Abstract\n" +
          "                                                    </a>\n" +
          "                                                    <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">\n" +
          "\n" +
          "                                                        Vladimir Arnold was a pre-eminent mathematician of the second half of the twentieth\n" +
          "                                                        and early twenty-first century. (KAM) theory, Arnold diffusion,\n" +
          "                                                        Arnold tongues in bifurcation theory, LA theorem in completely ...\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"issue-item\">\n" +
          "                                            <div class=\"badges\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<span class=\"badge-type badge-full\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<i title=\"You have access\" class=\"icon-check\"></i>\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</span>\n" +
          "                                                <span class=\"badge-type\">Obituary</span>\n" +
          "                                            </div>\n" +
          "                                            <h5 class=\"issue-item__title\">\n" +
          "                                                <a href=\"/doi/10.1098/rsbm.2017.0033\">Ian William Murison Smith</a>\n" +
          "                                            </h5>\n" +
          "                                            <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"/action/doSearch?fourth\" title=\"Gus Hancock\">\n" +
          "                                                        <span>fourth person</span>\n" +
          "                                                    </a>\n" +
          "                                                </li>\n" +
          "                                            </ul>\n" +
          "                                            <div class=\"toc-item__detail\">\n" +
          "                                                <span>Published:</span>\n" +
          "                                                <strong>14 February 2018</strong>\n" +
          "                                                <span class=\"separator\"></span>\n" +
          "                                                <span>Article ID:</span>\n" +
          "                                                <strong>20170033</strong>\n" +
          "                                            </div>\n" +
          "                                            <p>\n" +
          "                                                <a href=\"https://doi.org/10.1098/rsbm.2017.0033\">https://doi.org/10.1098/rsbm.2017.0033</a>\n" +
          "                                            </p>\n" +
          "                                            <div class=\"toc-item__footer\">\n" +
          "                                                <ul class=\"rlist--inline separator toc-item__detail\">\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Abstract\" href=\"/doi/abs/10.1098/rsbm.2017.0033\">Abstract</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Full text\" href=\"/doi/full/10.1098/rsbm.2017.0033\">Full text</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"PDF\" href=\"/doi/pdf/10.1098/rsbm.2017.0033\">\n" +
          "                                                            PDF\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/doi/references/10.1098/rsbm.2017.0033\">\n" +
          "                                                            References\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/servlet/linkout?something\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">\n" +
          "                                                            <img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL Stanford University\" />\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                </ul>\n" +
          "                                                <div class=\"accordion\">\n" +
          "                                                    <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\">\n" +
          "                                                        <i class=\"icon-section_arrow_d\"></i>Preview Abstract\n" +
          "                                                    </a>\n" +
          "                                                    <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">\n" +
          "\n" +
          "                                                        Ian Smith was one of the world-wide leading researchers into reaction kinetics, energy\n" +
          "                                                        transfer and molecular dynamics in gas phase systems. He was able to span all of these\n" +
          "                                                        aspects of collisional behaviour, and to form connections and insights that ...\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"content-navigation clearfix\">\n" +
          "                                        Previous/Next section...\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        <div class=\"toc-right-side col-md-3\">\n" +
          "                            <div class=\"card\">\n" +
          "                               right card section...\n" +
          "                            <div></div>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <div>\n" +
          "            <footer data-accordion-vport=\"screen-sm\" data-accordion-option=\"with-arrow\">\n" +
          "                footer section...\n" +
          "            </footer>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>\n" +
          "\n" +
          "\n";

  private static String tocContentCrawlFiltered = "<html lang=\"en\" class=\"pb-page\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    header section...\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <div>\n" +
          "            \n" +
          "            <div>div form section...</div>\n" +
          "        </div>\n" +
          "        <main class=\"content rsbm\">\n" +
          "            <div class=\"rsbm \">rsbm logo</div>\n" +
          "            <div class=\"container\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div>\n" +
          "                        <div class=\"sections-navigations-mobile\">\n" +
          "                            <nav class=\"toc__section\">\n" +
          "                                <a class=\"w-slide__btn\" href=\"#\" data-db-target-for=\"sections\" data-db-switch=\"icon-close_thin\" title=\"Go To Sections\" data-slide-target=\"#sections\">\n" +
          "                                    <i class=\"icon-toc\"></i>\n" +
          "                                    <span>Sections</span>\n" +
          "                                </a>\n" +
          "                            </nav>\n" +
          "                        </div>\n" +
          "                        <div class=\"mobile-gutters col-md-9\">\n" +
          "                            <div class=\"table-of-content-navigable clearfix\">\n" +
          "                                \n" +
          "                                <div class=\"col-md-9 line-separator--vertical toc-content\">\n" +
          "                                    <div class=\"table-of-content__header\">\n" +
          "                                        <h2>Table of Contents</h2>\n" +
          "                                        <div class=\"pb-dropzone\" data-pb-dropzone=\"tableOfContentDropzone\">\n" +
          "                                            <!-- Empty dropzone -->\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    <div class=\"table-of-content\">\n" +
          "                                        <h3 class=\"to-section section__title\" id=\"d6878631e37\">Editorial</h3>\n" +
          "                                        <div class=\"issue-item\">\n" +
          "                                            <div class=\"badges\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<span class=\"badge-type badge-full\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<i title=\"You have access\" class=\"icon-check\"></i>\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</span>\n" +
          "                                                <span class=\"badge-type\">Editorial</span>\n" +
          "                                            </div>\n" +
          "                                            <h5 class=\"issue-item__title\">\n" +
          "                                                <a href=\"/doi/10.1098/rsbm.2018.0002\">Editorial</a>\n" +
          "                                            </h5>\n" +
          "                                            <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"/action/\" title=\"first name\">\n" +
          "                                                        <span>first name</span>\n" +
          "                                                    </a>\n" +
          "                                                    <span>, </span>CBE FRS FRSE\n" +
          "\n" +
          "                                                </li>\n" +
          "                                            </ul>\n" +
          "                                            <div class=\"toc-item__detail\">\n" +
          "                                                <span>Published:</span>\n" +
          "                                                <strong>28 March 2018</strong>\n" +
          "                                                <span class=\"separator\"></span>\n" +
          "                                                <span>Article ID:</span>\n" +
          "                                                <strong>20180002</strong>\n" +
          "                                            </div>\n" +
          "                                            <p>\n" +
          "                                                <a href=\"https://doi.org/10.1098/rsbm.2018.0002\">https://doi.org/10.1098/rsbm.2018.0002</a>\n" +
          "                                            </p>\n" +
          "                                            <div class=\"toc-item__footer\">\n" +
          "                                                <ul class=\"rlist--inline separator toc-item__detail\">\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Abstract\" href=\"/doi/abs/10.1098/rsbm.2018.0002\">First Page</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Full text\" href=\"/doi/full/10.1098/rsbm.2018.0002\">Full text</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"PDF\" href=\"/doi/pdf/10.1098/rsbm.2018.0002\">\n" +
          "                                                            PDF\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/servlet/linkout?suffix=something\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">\n" +
          "                                                            <img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL Stanford University\" />\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                </ul>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <h3 class=\"to-section section__title\" id=\"d6878631e99\">Memoirs</h3>\n" +
          "                                        <div class=\"issue-item\">\n" +
          "                                            <div class=\"badges\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<span class=\"badge-type badge-full\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<i title=\"You have access\" class=\"icon-check\"></i>\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</span>\n" +
          "                                                <span class=\"badge-type\">Obituary</span>\n" +
          "                                            </div>\n" +
          "                                            <h5 class=\"issue-item__title\">\n" +
          "                                                <a href=\"/doi/10.1098/rsbm.2017.0016\">Vladimir Igorevich Arnold</a>\n" +
          "                                            </h5>\n" +
          "                                            <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"/action/doSearch?second\" title=\"second personn\">\n" +
          "                                                        <span>second person</span>\n" +
          "                                                    </a>\n" +
          "                                                    <span> and </span>\n" +
          "                                                </li>\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"/action/doSearch?third\" title=\"third\">\n" +
          "                                                        <span>third</span>\n" +
          "                                                    </a>\n" +
          "                                                </li>\n" +
          "                                            </ul>\n" +
          "                                            <div class=\"toc-item__detail\">\n" +
          "                                                <span>Published:</span>\n" +
          "                                                <strong>30 August 2017</strong>\n" +
          "                                                <span class=\"separator\"></span>\n" +
          "                                                <span>Article ID:</span>\n" +
          "                                                <strong>20170016</strong>\n" +
          "                                            </div>\n" +
          "                                            <p>\n" +
          "                                                <a href=\"https://doi.org/10.1098/rsbm.2017.0016\">https://doi.org/10.1098/rsbm.2017.0016</a>\n" +
          "                                            </p>\n" +
          "                                            <div class=\"toc-item__footer\">\n" +
          "                                                <ul class=\"rlist--inline separator toc-item__detail\">\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Abstract\" href=\"/doi/abs/10.1098/rsbm.2017.0016\">Abstract</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Full text\" href=\"/doi/full/10.1098/rsbm.2017.0016\">Full text</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"PDF\" href=\"/doi/pdf/10.1098/rsbm.2017.0016\">\n" +
          "                                                            PDF\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/doi/references/10.1098/rsbm.2017.0016\">\n" +
          "                                                            References\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/servlet/linkout?suffix=something\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">\n" +
          "                                                            <img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL Stanford University\" />\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                </ul>\n" +
          "                                                <div class=\"accordion\">\n" +
          "                                                    <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\">\n" +
          "                                                        <i class=\"icon-section_arrow_d\"></i>Preview Abstract\n" +
          "                                                    </a>\n" +
          "                                                    <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">\n" +
          "\n" +
          "                                                        Vladimir Arnold was a pre-eminent mathematician of the second half of the twentieth\n" +
          "                                                        and early twenty-first century. (KAM) theory, Arnold diffusion,\n" +
          "                                                        Arnold tongues in bifurcation theory, LA theorem in completely ...\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"issue-item\">\n" +
          "                                            <div class=\"badges\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<span class=\"badge-type badge-full\">\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t<i title=\"You have access\" class=\"icon-check\"></i>\n" +
          "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t</span>\n" +
          "                                                <span class=\"badge-type\">Obituary</span>\n" +
          "                                            </div>\n" +
          "                                            <h5 class=\"issue-item__title\">\n" +
          "                                                <a href=\"/doi/10.1098/rsbm.2017.0033\">Ian William Murison Smith</a>\n" +
          "                                            </h5>\n" +
          "                                            <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" +
          "                                                <li>\n" +
          "                                                    <a href=\"/action/doSearch?fourth\" title=\"Gus Hancock\">\n" +
          "                                                        <span>fourth person</span>\n" +
          "                                                    </a>\n" +
          "                                                </li>\n" +
          "                                            </ul>\n" +
          "                                            <div class=\"toc-item__detail\">\n" +
          "                                                <span>Published:</span>\n" +
          "                                                <strong>14 February 2018</strong>\n" +
          "                                                <span class=\"separator\"></span>\n" +
          "                                                <span>Article ID:</span>\n" +
          "                                                <strong>20170033</strong>\n" +
          "                                            </div>\n" +
          "                                            <p>\n" +
          "                                                <a href=\"https://doi.org/10.1098/rsbm.2017.0033\">https://doi.org/10.1098/rsbm.2017.0033</a>\n" +
          "                                            </p>\n" +
          "                                            <div class=\"toc-item__footer\">\n" +
          "                                                <ul class=\"rlist--inline separator toc-item__detail\">\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Abstract\" href=\"/doi/abs/10.1098/rsbm.2017.0033\">Abstract</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"Full text\" href=\"/doi/full/10.1098/rsbm.2017.0033\">Full text</a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a title=\"PDF\" href=\"/doi/pdf/10.1098/rsbm.2017.0033\">\n" +
          "                                                            PDF\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/doi/references/10.1098/rsbm.2017.0033\">\n" +
          "                                                            References\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                    <li>\n" +
          "                                                        <a href=\"/servlet/linkout?something\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\">\n" +
          "                                                            <img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL Stanford University\" />\n" +
          "                                                        </a>\n" +
          "                                                    </li>\n" +
          "                                                </ul>\n" +
          "                                                <div class=\"accordion\">\n" +
          "                                                    <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\">\n" +
          "                                                        <i class=\"icon-section_arrow_d\"></i>Preview Abstract\n" +
          "                                                    </a>\n" +
          "                                                    <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">\n" +
          "\n" +
          "                                                        Ian Smith was one of the world-wide leading researchers into reaction kinetics, energy\n" +
          "                                                        transfer and molecular dynamics in gas phase systems. He was able to span all of these\n" +
          "                                                        aspects of collisional behaviour, and to form connections and insights that ...\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                    \n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </div>\n" +
          "                        \n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <div>\n" +
          "            \n" +
          "        </div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>\n" +
          "\n" +
          "\n";


  private static String doiFullContent = "" +
          "<html lang=\"en\" class=\"pb-page\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "   header section...\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <div>\n" +
          "            <div>\n" +
          "               popup section...\n" +
          "            </div>\n" +
          "            <header data-db-parent-of=\"sb1\" class=\"header fixed base pageHeader\">\n" +
          "                div header section...\n" +
          "            </header>\n" +
          "        </div>\n" +
          "        <main class=\"content rsbm\">\n" +
          "            <div class=\"rsbm \">\n" +
          "                rsbm section...\n" +
          "            </div>\n" +
          "            <div class=\"container\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div>\n" +
          "                        <article data-figures=\"/action/ajaxShowFigures?\" data-enable-mathjax=\"true\" class=\"container\">\n" +
          "                            <div class=\"row\">\n" +
          "                                <div class=\"col-xs-12 col-sm-12 col-md-8 col-lg-9 article__cont mobile-gutters\">\n" +
          "                                    <div class=\"article_body clearfix gutterless\">\n" +
          "                                        <div class=\"col-xs-12 col-sm-12 col-md-4 col-lg-3 left-side sticko__parent gutterless--md gutterless--sm gutterless--xs\">\n" +
          "                                            <nav class=\"article-coolBar trans\">\n" +
          "                                                article coolBar section...\n" +
          "                                            </nav>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"col-xs-12 col-md-8 col-lg-9 article__content\">\n" +
          "                                            <div class=\"citation\">\n" +
          "                                                citation section...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"epub-section\">\n" +
          "                                                epub section...\n" +
          "                                            <div>\n" +
          "                                                <ul class=\"rlist article__header__accordions\">\n" +
          "                                                    <li class=\"article__versions accordion rlist\">\n" +
          "                                                        <a href=\"#\" title=\"Other version(s) of this article\" aria-controls=\"articleVersion\" data-slide-target=\"#articleVesrions\" class=\"accordion__control w-slide__btn article__header__accordions__ctrl\">\n" +
          "                                                            <i class=\"icon-version\"></i>\n" +
          "                                                            <span>This is the latest version of the article, see previous versions.</span>\n" +
          "                                                        </a>\n" +
          "                                                        <div id=\"articleVesrions\" class=\"rlist accordion__content\">\n" +
          "                                                            <ul class=\"rlist\">\n" +
          "                                                                <li>\n" +
          "                                                                    <a href=\"/cms/attachment/something.pdf\">March 28, 2018: Previous Version 1</a>\n" +
          "                                                                </li>\n" +
          "                                                            </ul>\n" +
          "                                                        </div>\n" +
          "                                                    </li>\n" +
          "                                                </ul>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"article__body \">\n" +
          "                                                <p class=\"fulltext\">\n" +
          "                                                </p>\n" +
          "                                                <!--abstract content-->\n" +
          "                                                <div class=\"hlFld-Abstract\">\n" +
          "                                                   Abstract section...\n" +
          "                                                </div>\n" +
          "                                                <!--/abstract content-->\n" +
          "                                                <!--fulltext content-->\n" +
          "                                                    Full text section...It contains a lot of paragraphs...blah, blah, blah....\n" +
          "                                            <!--/fulltext content-->\n" +
          "                                            <div class=\"response\">\n" +
          "                                                <div class=\"sub-article-title\"></div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            <div class=\"col-sm-4 col-md-4 col-lg-3 sticko__parent article-row-right hidden-xs hidden-sm hidden-md\">\n" +
          "                                <div data-ctrl-res=\"screen-md\" class=\"tab tab--slide tab--flex sticko__md dynamic-sticko  tab--flex tabs--xs\">\n" +
          "                                    <ul role=\"tablist\" class=\"rlist tab__nav w-slide--list tab--slide\">\n" +
          "                                        li elements of presentations section...\n" +
          "                                    </ul>\n" +
          "                                    <ul class=\"rlist tab__content sticko__child\">\n" +
          "                                        right list of the article, including year, isbn, eisbn....\n" +
          "                                    </ul>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                    </div>\n" +
          "                    </article>\n" +
          "                    <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "                        figure viewer section...\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "    </div>\n" +
          "        </main>\n" +
          "    <div>\n" +
          "        <footer data-accordion-vport=\"screen-sm\" data-accordion-option=\"with-arrow\">\n" +
          "            footer section...\n" +
          "        </footer>\n" +
          "    </div>\n" +
          "    <div>\n" +
          "        <div class=\"pb-dropzone\" data-pb-dropzone=\"col-0\"></div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>\n";

  private static String doiFullContentCrawlFiltered = "<html lang=\"en\" class=\"pb-page\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "   header section...\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <div>\n" +
          "            <div>\n" +
          "               popup section...\n" +
          "            </div>\n" +
          "            \n" +
          "        </div>\n" +
          "        <main class=\"content rsbm\">\n" +
          "            <div class=\"rsbm \">\n" +
          "                rsbm section...\n" +
          "            </div>\n" +
          "            <div class=\"container\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div>\n" +
          "                        <article data-figures=\"/action/ajaxShowFigures?\" data-enable-mathjax=\"true\" class=\"container\">\n" +
          "                            <div class=\"row\">\n" +
          "                                <div class=\"col-xs-12 col-sm-12 col-md-8 col-lg-9 article__cont mobile-gutters\">\n" +
          "                                    <div class=\"article_body clearfix gutterless\">\n" +
          "                                        <div class=\"col-xs-12 col-sm-12 col-md-4 col-lg-3 left-side sticko__parent gutterless--md gutterless--sm gutterless--xs\">\n" +
          "                                            <nav class=\"article-coolBar trans\">\n" +
          "                                                article coolBar section...\n" +
          "                                            </nav>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"col-xs-12 col-md-8 col-lg-9 article__content\">\n" +
          "                                            <div class=\"citation\">\n" +
          "                                                citation section...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"epub-section\">\n" +
          "                                                epub section...\n" +
          "                                            <div>\n" +
          "                                                <ul class=\"rlist article__header__accordions\">\n" +
          "                                                    <li class=\"article__versions accordion rlist\">\n" +
          "                                                        <a href=\"#\" title=\"Other version(s) of this article\" aria-controls=\"articleVersion\" data-slide-target=\"#articleVesrions\" class=\"accordion__control w-slide__btn article__header__accordions__ctrl\">\n" +
          "                                                            <i class=\"icon-version\"></i>\n" +
          "                                                            <span>This is the latest version of the article,  see previous versions.</span>\n" +
          "                                                        </a>\n" +
          "                                                        <div id=\"articleVesrions\" class=\"rlist accordion__content\">\n" +
          "                                                            <ul class=\"rlist\">\n" +
          "                                                                <li>\n" +
          "                                                                    <a href=\"/cms/attachment/something.pdf\">March 28, 2018: Previous Version 1</a>\n" +
          "                                                                </li>\n" +
          "                                                            </ul>\n" +
          "                                                        </div>\n" +
          "                                                    </li>\n" +
          "                                                </ul>\n" +
          "                                            </div>\n" +
          "                                            <div class=\"article__body \">\n" +
          "                                                <p class=\"fulltext\">\n" +
          "                                                </p>\n" +
          "                                                <!--abstract content-->\n" +
          "                                                <div class=\"hlFld-Abstract\">\n" +
          "                                                   Abstract section...\n" +
          "                                                </div>\n" +
          "                                                <!--/abstract content-->\n" +
          "                                                <!--fulltext content-->\n" +
          "                                                    Full text section...It contains a lot of paragraphs...blah, blah, blah....\n" +
          "                                            <!--/fulltext content-->\n" +
          "                                            <div class=\"response\">\n" +
          "                                                <div class=\"sub-article-title\"></div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                            \n" +
          "                    </div>\n" +
          "                    </article>\n" +
          "                    <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "                        figure viewer section...\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "    </div>\n" +
          "        </main>\n" +
          "    <div>\n" +
          "        \n" +
          "    </div>\n" +
          "    <div>\n" +
          "        <div class=\"pb-dropzone\" data-pb-dropzone=\"col-0\"></div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>\n";

  private static String doiAbsContent = "<html lang=\"en\" class=\"pb-page\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "   header section...\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <div>\n" +
          "            <div>\n" +
          "                popup section...\n" +
          "            </div>\n" +
          "            <header data-db-parent-of=\"sb1\" class=\"header fixed base pageHeader\">\n" +
          "                div header section...\n" +
          "            </header>\n" +
          "        </div>\n" +
          "        <main class=\"content rsbm\">\n" +
          "            <div class=\"rsbm \">\n" +
          "                rsbm section...\n" +
          "            </div>\n" +
          "            <div class=\"container\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div>\n" +
          "                        <article data-figures=\"/action/ajaxShowFigures?widgetId=f7ffb5e4-603e-4dc2-b204-abd7a5dfc91f&amp;ajax=true&amp;doi=10.1098%2Frsbm.2017.0046&amp;pbContext=%3BrequestedJournal%3Ajournal%3Arsbm%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3BsubPage%3Astring%3AAbstract%3Barticle%3Aarticle%3Adoi%5C%3A10.1098%2Frsbm.2017.0046%3Bwebsite%3Awebsite%3Arsj-site%3Bjournal%3Ajournal%3Arsbm1932%3Bissue%3Aissue%3Adoi%5C%3A10.1098%2Frsbm.issue-64%3Bwgroup%3Astring%3APublication+Websites%3BpageGroup%3Astring%3APublication+Pages\" data-references=\"/action/ajaxShowEnhancedAbstract?widgetId=f7ffb5e4-603e-4dc2-b204-abd7a5dfc91f&amp;ajax=true&amp;doi=10.1098%2Frsbm.2017.0046&amp;pbContext=%3BrequestedJournal%3Ajournal%3Arsbm%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3BsubPage%3Astring%3AAbstract%3Barticle%3Aarticle%3Adoi%5C%3A10.1098%2Frsbm.2017.0046%3Bwebsite%3Awebsite%3Arsj-site%3Bjournal%3Ajournal%3Arsbm1932%3Bissue%3Aissue%3Adoi%5C%3A10.1098%2Frsbm.issue-64%3Bwgroup%3Astring%3APublication+Websites%3BpageGroup%3Astring%3APublication+Pages\" data-enable-mathjax=\"true\" class=\"container\">\n" +
          "                            <div class=\"row\">\n" +
          "                                <div class=\"col-xs-12 col-sm-12 col-md-8 col-lg-9 article__cont mobile-gutters\">\n" +
          "                                    <div class=\"article_body clearfix gutterless\">\n" +
          "                                        <div class=\"col-xs-12 col-sm-12 col-md-4 col-lg-3 left-side sticko__parent gutterless--md gutterless--sm gutterless--xs\">\n" +
          "                                            <nav class=\"article-coolBar trans\">\n" +
          "                                                <div class=\"coolBar stickybar__wrapper article-coolBar__wrapper clearfix\">\n" +
          "                                                    <div class=\"coolBar__inner\">\n" +
          "                                                        coolBar section, including share, favoriate, citation and supplement materials...\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </nav>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"col-xs-12 col-md-8 col-lg-9 article__content\">\n" +
          "                                            <div class=\"citation\">\n" +
          "                                                Citation section...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"epub-section\">\n" +
          "                                                Epub section...\n" +
          "                                            </div>\n" +
          "                                            <div>\n" +
          "                                                Latest version of the article section...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"article__body \">\n" +
          "                                                <!-- abstract content -->\n" +
          "                                                <div class=\"hlFld-Abstract\">\n" +
          "                                                    Abstract of the article section, blah, blah, blah....\n" +
          "                                                </div>\n" +
          "                                                <!-- /abstract content -->\n" +
          "                                                <h2 class=\"article-section__title section__title to-section\" id=\"d2146779e1\">Footnotes</h2>\n" +
          "                                                <div class=\"author-notes\">\n" +
          "                                                    Author notes section...\n" +
          "                                                </div>\n" +
          "                                                <div class=\"article__references\">\n" +
          "                                                    Some article related references...\n" +
          "                                                </div>\n" +
          "                                                <div class=\"article__references\">\n" +
          "                                                   More article related references...\n" +
          "                                                </div>\n" +
          "                                                <div  class=\"response\">\n" +
          "                                                    <div class=\"sub-article-title\"></div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <div class=\"col-sm-4 col-md-4 col-lg-3 sticko__parent article-row-right hidden-xs hidden-sm hidden-md\">\n" +
          "                                    <div data-ctrl-res=\"screen-md\" class=\"tab tab--slide tab--flex sticko__md dynamic-sticko  tab--flex tabs--xs\">\n" +
          "                                        <ul role=\"tablist\" class=\"rlist tab__nav w-slide--list tab--slide\">\n" +
          "                                            li elements for presentions...\n" +
          "                                        </ul>\n" +
          "                                        <ul class=\"rlist tab__content sticko__child\">\n" +
          "                                            rlist section...\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </article>\n" +
          "                        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "                            figure-view section...\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <div>\n" +
          "            <footer data-accordion-vport=\"screen-sm\" data-accordion-option=\"with-arrow\">\n" +
          "               footer section...\n" +
          "            </footer>\n" +
          "        </div>\n" +
          "        <div>\n" +
          "            <div class=\"pb-dropzone\" data-pb-dropzone=\"col-0\"></div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>\n";

  private static String doiAbsContentCrawlFiltered = "<html lang=\"en\" class=\"pb-page\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "   header section...\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <div>\n" +
          "            <div>\n" +
          "                popup section...\n" +
          "            </div>\n" +
          "            \n" +
          "        </div>\n" +
          "        <main class=\"content rsbm\">\n" +
          "            <div class=\"rsbm \">\n" +
          "                rsbm section...\n" +
          "            </div>\n" +
          "            <div class=\"container\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div>\n" +
          "                        <article data-figures=\"/action/ajaxShowFigures?widgetId=f7ffb5e4-603e-4dc2-b204-abd7a5dfc91f&amp;ajax=true&amp;doi=10.1098%2Frsbm.2017.0046&amp;pbContext=%3BrequestedJournal%3Ajournal%3Arsbm%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3BsubPage%3Astring%3AAbstract%3Barticle%3Aarticle%3Adoi%5C%3A10.1098%2Frsbm.2017.0046%3Bwebsite%3Awebsite%3Arsj-site%3Bjournal%3Ajournal%3Arsbm1932%3Bissue%3Aissue%3Adoi%5C%3A10.1098%2Frsbm.issue-64%3Bwgroup%3Astring%3APublication+Websites%3BpageGroup%3Astring%3APublication+Pages\" data-references=\"/action/ajaxShowEnhancedAbstract?widgetId=f7ffb5e4-603e-4dc2-b204-abd7a5dfc91f&amp;ajax=true&amp;doi=10.1098%2Frsbm.2017.0046&amp;pbContext=%3BrequestedJournal%3Ajournal%3Arsbm%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3BsubPage%3Astring%3AAbstract%3Barticle%3Aarticle%3Adoi%5C%3A10.1098%2Frsbm.2017.0046%3Bwebsite%3Awebsite%3Arsj-site%3Bjournal%3Ajournal%3Arsbm1932%3Bissue%3Aissue%3Adoi%5C%3A10.1098%2Frsbm.issue-64%3Bwgroup%3Astring%3APublication+Websites%3BpageGroup%3Astring%3APublication+Pages\" data-enable-mathjax=\"true\" class=\"container\">\n" +
          "                            <div class=\"row\">\n" +
          "                                <div class=\"col-xs-12 col-sm-12 col-md-8 col-lg-9 article__cont mobile-gutters\">\n" +
          "                                    <div class=\"article_body clearfix gutterless\">\n" +
          "                                        <div class=\"col-xs-12 col-sm-12 col-md-4 col-lg-3 left-side sticko__parent gutterless--md gutterless--sm gutterless--xs\">\n" +
          "                                            <nav class=\"article-coolBar trans\">\n" +
          "                                                <div class=\"coolBar stickybar__wrapper article-coolBar__wrapper clearfix\">\n" +
          "                                                    <div class=\"coolBar__inner\">\n" +
          "                                                        coolBar section, including share, favoriate, citation and supplement materials...\n" +
          "                                                    </div>\n" +
          "                                                </div>\n" +
          "                                            </nav>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"col-xs-12 col-md-8 col-lg-9 article__content\">\n" +
          "                                            <div class=\"citation\">\n" +
          "                                                Citation section...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"epub-section\">\n" +
          "                                                Epub section...\n" +
          "                                            </div>\n" +
          "                                            <div>\n" +
          "                                                Latest version of the article section...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"article__body \">\n" +
          "                                                <!-- abstract content -->\n" +
          "                                                <div class=\"hlFld-Abstract\">\n" +
          "                                                    Abstract of the article section, blah, blah, blah....\n" +
          "                                                </div>\n" +
          "                                                <!-- /abstract content -->\n" +
          "                                                <h2 class=\"article-section__title section__title to-section\" id=\"d2146779e1\">Footnotes</h2>\n" +
          "                                                <div class=\"author-notes\">\n" +
          "                                                    Author notes section...\n" +
          "                                                </div>\n" +
          "                                                <div class=\"article__references\">\n" +
          "                                                    Some article related references...\n" +
          "                                                </div>\n" +
          "                                                <div class=\"article__references\">\n" +
          "                                                   More article related references...\n" +
          "                                                </div>\n" +
          "                                                <div class=\"response\">\n" +
          "                                                    <div class=\"sub-article-title\"></div>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                \n" +
          "                            </div>\n" +
          "                        </article>\n" +
          "                        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "                            figure-view section...\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <div>\n" +
          "            \n" +
          "        </div>\n" +
          "        <div>\n" +
          "            <div class=\"pb-dropzone\" data-pb-dropzone=\"col-0\"></div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>\n";

  private static String doiReferenceContent = "<html lang=\"en\" class=\"pb-page\"  data-request-id=\"ead6b80b-beca-4e12-a7c8-b46580aad73a\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    header section...\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <div>\n" +
          "            <div>\n" +
          "                popup section...\n" +
          "            </div>\n" +
          "            <header data-db-parent-of=\"sb1\" class=\"header fixed base pageHeader\">\n" +
          "                div header section...\n" +
          "            </header>\n" +
          "        </div>\n" +
          "        <main class=\"content rsbm\">\n" +
          "            <div class=\"rsbm \">\n" +
          "                rsbm section\n" +
          "            </div>\n" +
          "            <div class=\"container\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div>\n" +
          "                        <article data-figures=\"/action/ajaxShowFigures?widgetId=f7ffb5e4-603e-4dc2-b204-abd7a5dfc91f&amp;ajax=true&amp;doi=10.1098%2Frsbm.2017.0016&amp;pbContext=%3BrequestedJournal%3Ajournal%3Arsbm%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3Barticle%3Aarticle%3Adoi%5C%3A10.1098%2Frsbm.2017.0016%3BsubPage%3Astring%3AReferences%3Bwebsite%3Awebsite%3Arsj-site%3Bjournal%3Ajournal%3Arsbm1932%3Bissue%3Aissue%3Adoi%5C%3A10.1098%2Frsbm.issue-64%3Bwgroup%3Astring%3APublication+Websites%3BpageGroup%3Astring%3APublication+Pages\" data-references=\"/action/ajaxShowEnhancedAbstract?widgetId=f7ffb5e4-603e-4dc2-b204-abd7a5dfc91f&amp;ajax=true&amp;doi=10.1098%2Frsbm.2017.0016&amp;pbContext=%3BrequestedJournal%3Ajournal%3Arsbm%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3Barticle%3Aarticle%3Adoi%5C%3A10.1098%2Frsbm.2017.0016%3BsubPage%3Astring%3AReferences%3Bwebsite%3Awebsite%3Arsj-site%3Bjournal%3Ajournal%3Arsbm1932%3Bissue%3Aissue%3Adoi%5C%3A10.1098%2Frsbm.issue-64%3Bwgroup%3Astring%3APublication+Websites%3BpageGroup%3Astring%3APublication+Pages\" data-enable-mathjax=\"true\" class=\"container\">\n" +
          "                            <div class=\"row\">\n" +
          "                                <div class=\"col-xs-12 col-sm-12 col-md-8 col-lg-9 article__cont mobile-gutters\">\n" +
          "                                    <div class=\"article_body clearfix gutterless\">\n" +
          "                                        <div class=\"col-xs-12 col-sm-12 col-md-4 col-lg-3 left-side sticko__parent gutterless--md gutterless--sm gutterless--xs\">\n" +
          "                                            <nav class=\"article-coolBar trans\">\n" +
          "                                                article-coolBar section...\n" +
          "                                            </nav>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"col-xs-12 col-md-8 col-lg-9 article__content\">\n" +
          "                                            <div class=\"citation\">\n" +
          "                                                citation section...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"epub-section\">\n" +
          "                                                epub section...\n" +
          "                                            </div>\n" +
          "                                            <div>\n" +
          "                                                Lastest version of the article...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"article__body show-references\">\n" +
          "                                                <div class=\"article__references\">\n" +
          "                                                    <p class=\"explanation__text\"></p>\n" +
          "                                                    <h3 id=\"d6606055e1239\">References</h3>\n" +
          "                                                    <ul class=\"rlist separator\">\n" +
          "                                                        li elements of rlist references section 1...\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"article__references\">\n" +
          "                                                    <p class=\"explanation__text\"></p>\n" +
          "                                                    <h3 id=\"d6606055e1535\">References</h3>\n" +
          "                                                    <ul class=\"rlist separator\">\n" +
          "                                                        li elements of rlist references again section 2...\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                <div class=\"col-sm-4 col-md-4 col-lg-3 sticko__parent article-row-right hidden-xs hidden-sm hidden-md\">\n" +
          "                                    <div data-ctrl-res=\"screen-md\" class=\"tab tab--slide tab--flex sticko__md dynamic-sticko  tab--flex tabs--xs\">\n" +
          "                                        <ul role=\"tablist\" class=\"rlist tab__nav w-slide--list tab--slide\">\n" +
          "                                            li elements for rlist section...\n" +
          "                                        </ul>\n" +
          "                                        <ul class=\"rlist tab__content sticko__child\">\n" +
          "                                           li elements for rlist section again...\n" +
          "                                        </ul>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                            </div>\n" +
          "                        </article>\n" +
          "                        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "                            figure-view section...\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <div>\n" +
          "            <footer data-accordion-vport=\"screen-sm\" data-accordion-option=\"with-arrow\">\n" +
          "                footer section...\n" +
          "            </footer>\n" +
          "        </div>\n" +
          "        <div>\n" +
          "            <div class=\"pb-dropzone\" data-pb-dropzone=\"col-0\"></div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>\n";

  private static String doiReferenceContentCrawlFiltered = "<html lang=\"en\" class=\"pb-page\"  data-request-id=\"ead6b80b-beca-4e12-a7c8-b46580aad73a\">\n" +
          "<head data-pb-dropzone=\"head\">\n" +
          "    header section...\n" +
          "</head>\n" +
          "<body class=\"pb-ui\">\n" +
          "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
          "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
          "        <div>\n" +
          "            <div>\n" +
          "                popup section...\n" +
          "            </div>\n" +
          "            \n" +
          "        </div>\n" +
          "        <main class=\"content rsbm\">\n" +
          "            <div class=\"rsbm \">\n" +
          "                rsbm section\n" +
          "            </div>\n" +
          "            <div class=\"container\">\n" +
          "                <div class=\"row\">\n" +
          "                    <div>\n" +
          "                        <article data-figures=\"/action/ajaxShowFigures?widgetId=f7ffb5e4-603e-4dc2-b204-abd7a5dfc91f&amp;ajax=true&amp;doi=10.1098%2Frsbm.2017.0016&amp;pbContext=%3BrequestedJournal%3Ajournal%3Arsbm%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3Barticle%3Aarticle%3Adoi%5C%3A10.1098%2Frsbm.2017.0016%3BsubPage%3Astring%3AReferences%3Bwebsite%3Awebsite%3Arsj-site%3Bjournal%3Ajournal%3Arsbm1932%3Bissue%3Aissue%3Adoi%5C%3A10.1098%2Frsbm.issue-64%3Bwgroup%3Astring%3APublication+Websites%3BpageGroup%3Astring%3APublication+Pages\" data-references=\"/action/ajaxShowEnhancedAbstract?widgetId=f7ffb5e4-603e-4dc2-b204-abd7a5dfc91f&amp;ajax=true&amp;doi=10.1098%2Frsbm.2017.0016&amp;pbContext=%3BrequestedJournal%3Ajournal%3Arsbm%3Bpage%3Astring%3AArticle%2FChapter+View%3Bctype%3Astring%3AJournal+Content%3Barticle%3Aarticle%3Adoi%5C%3A10.1098%2Frsbm.2017.0016%3BsubPage%3Astring%3AReferences%3Bwebsite%3Awebsite%3Arsj-site%3Bjournal%3Ajournal%3Arsbm1932%3Bissue%3Aissue%3Adoi%5C%3A10.1098%2Frsbm.issue-64%3Bwgroup%3Astring%3APublication+Websites%3BpageGroup%3Astring%3APublication+Pages\" data-enable-mathjax=\"true\" class=\"container\">\n" +
          "                            <div class=\"row\">\n" +
          "                                <div class=\"col-xs-12 col-sm-12 col-md-8 col-lg-9 article__cont mobile-gutters\">\n" +
          "                                    <div class=\"article_body clearfix gutterless\">\n" +
          "                                        <div class=\"col-xs-12 col-sm-12 col-md-4 col-lg-3 left-side sticko__parent gutterless--md gutterless--sm gutterless--xs\">\n" +
          "                                            <nav class=\"article-coolBar trans\">\n" +
          "                                                article-coolBar section...\n" +
          "                                            </nav>\n" +
          "                                        </div>\n" +
          "                                        <div class=\"col-xs-12 col-md-8 col-lg-9 article__content\">\n" +
          "                                            <div class=\"citation\">\n" +
          "                                                citation section...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"epub-section\">\n" +
          "                                                epub section...\n" +
          "                                            </div>\n" +
          "                                            <div>\n" +
          "                                                Lastest version of the article...\n" +
          "                                            </div>\n" +
          "                                            <div class=\"article__body show-references\">\n" +
          "                                                <div class=\"article__references\">\n" +
          "                                                    <p class=\"explanation__text\"></p>\n" +
          "                                                    <h3 id=\"d6606055e1239\">References</h3>\n" +
          "                                                    <ul class=\"rlist separator\">\n" +
          "                                                        li elements of rlist references section 1...\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                                <div class=\"article__references\">\n" +
          "                                                    <p class=\"explanation__text\"></p>\n" +
          "                                                    <h3 id=\"d6606055e1535\">References</h3>\n" +
          "                                                    <ul class=\"rlist separator\">\n" +
          "                                                        li elements of rlist references again section 2...\n" +
          "                                                    </ul>\n" +
          "                                                </div>\n" +
          "                                            </div>\n" +
          "                                        </div>\n" +
          "                                    </div>\n" +
          "                                </div>\n" +
          "                                \n" +
          "                            </div>\n" +
          "                        </article>\n" +
          "                        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
          "                            figure-view section...\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </main>\n" +
          "        <div>\n" +
          "            \n" +
          "        </div>\n" +
          "        <div>\n" +
          "            <div class=\"pb-dropzone\" data-pb-dropzone=\"col-0\"></div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>\n";

  private static final String tocContentHashFiltered = " Table of Contents Editorial Editorial Editorial first name , CBE FRS FRSE Published: 28 March 2018 Article ID: 20180002 https://doi.org/10.1098/rsbm.2018.0002 First Page Full text PDF Memoirs Obituary Vladimir Igorevich Arnold second person and third Published: 30 August 2017 Article ID: 20170016 https://doi.org/10.1098/rsbm.2017.0016 Abstract Full text PDF References Preview Abstract Vladimir Arnold was a pre-eminent mathematician of the second half of the twentieth and early twenty-first century. (KAM) theory, Arnold diffusion, Arnold tongues in bifurcation theory, LA theorem in completely ... Obituary Ian William Murison Smith fourth person Published: 14 February 2018 Article ID: 20170033 https://doi.org/10.1098/rsbm.2017.0033 Abstract Full text PDF References Preview Abstract Ian Smith was one of the world-wide leading researchers into reaction kinetics, energy transfer and molecular dynamics in gas phase systems. He was able to span all of these aspects of collisional behaviour, and to form connections and insights that ... Table of Contents Editorial Editorial Editorial first name , CBE FRS FRSE Published: 28 March 2018 Article ID: 20180002 https://doi.org/10.1098/rsbm.2018.0002 First Page Full text PDF Memoirs Obituary Vladimir Igorevich Arnold second person and third Published: 30 August 2017 Article ID: 20170016 https://doi.org/10.1098/rsbm.2017.0016 Abstract Full text PDF References Preview Abstract Vladimir Arnold was a pre-eminent mathematician of the second half of the twentieth and early twenty-first century. (KAM) theory, Arnold diffusion, Arnold tongues in bifurcation theory, LA theorem in completely ... Obituary Ian William Murison Smith fourth person Published: 14 February 2018 Article ID: 20170033 https://doi.org/10.1098/rsbm.2017.0033 Abstract Full text PDF References Preview Abstract Ian Smith was one of the world-wide leading researchers into reaction kinetics, energy transfer and molecular dynamics in gas phase systems. He was able to span all of these aspects of collisional behaviour, and to form connections and insights that ... ";
  private static final String doiFullContentHashFiltered = " Abstract section... Full text section...It contains a lot of paragraphs...blah, blah, blah.... ";
  private static final String doiAbsContentHashFiltered = " Abstract of the article section, blah, blah, blah.... Footnotes Author notes section... ";
  private static final String doiReferenceContentHashFiltered = ""; // Latest Atypon code filtered out reference

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

    int len = beforeArr.length;

    for (int i = 0, sb1_i = 0, sb2_i = 0;  i < len; i++, sb1_i++, sb2_i++) {
      StringBuilder sb1 = new StringBuilder();
      StringBuilder sb2 = new StringBuilder();

      sb1.append(beforeArr[i].replaceAll("\\s+", ""));
      sb2.append(afterArr[i].replaceAll("\\s+", ""));

      assertEquals(sb2.toString(), sb2.toString());

      sb1.setLength(0);
      sb2.setLength(0);
    }

  }

  //https://royalsocietypublishing.org/doi/abs/10.1098/rsbm.2017.0046
  public static class TestCrawl extends TestRoyalSocietyPublishingHtmlCrawlFilterFactory {

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

    public void testReferenceContentFiltering() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantCrawlFact, doiReferenceContent);
      String unicodeExpectedStr = doiReferenceContentCrawlFiltered;
      compareContentLineByLine(unicodeFilteredStr, unicodeExpectedStr);
    }

  }

  public static class TestHash extends TestRoyalSocietyPublishingHtmlCrawlFilterFactory {


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

    public void testReferenceContentHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, doiReferenceContent);
      String unicodeExpectedStr = doiReferenceContentHashFiltered;
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
