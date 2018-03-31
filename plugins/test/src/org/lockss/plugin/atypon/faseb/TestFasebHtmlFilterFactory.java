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

package org.lockss.plugin.atypon.faseb;

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

public class TestFasebHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
  
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.faseb.FasebAtyponPlugin";
  
  
  private static final String manifestContent = 
      "<html>\n" +
      "<body>\n" + 
      "<h1>The FASEB Journal 2017 CLOCKSS Manifest Page</h1>\n" + 
      "<ul>\n" + 
      "    <li><a href=\"/toc/fasebj/31/12\">December 2017 (Vol. 31 Issue 12 Page 5135-5625)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/11\">November 2017 (Vol. 31 Issue 11 Page 4661-5134)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/10\">October 2017 (Vol. 31 Issue 10 Page 4205-4660)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/9\">September 2017 (Vol. 31 Issue 9 Page 3711-4204)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/8\">August 2017 (Vol. 31 Issue 8 Page 3209-3710)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/7\">July 2017 (Vol. 31 Issue 7 Page 2721-3206)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/6\">June 2017 (Vol. 31 Issue 6 Page 2221-2719)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/5\">May 2017 (Vol. 31 Issue 5 Page 1769-2220)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/1_supplement\">April 2017 (Vol. 31 Issue 1_supplement Page lb1-1091.2)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/4\">April 2017 (Vol. 31 Issue 4 Page 1251-1767)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/3\">March 2017 (Vol. 31 Issue 3 Page 853-1249)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/2\">February 2017 (Vol. 31 Issue 2 Page 423-852)</a></li>\n" + 
      "    <li><a href=\"/toc/fasebj/31/1\">January 2017 (Vol. 31 Issue 1 Page 1-421)</a></li>\n" + 
      "</ul>\n" + 
      "<p>\n" + 
      "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" alt=\"LOCKSS logo\" width=\"108\" height=\"108\">\n" + 
      "    CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.\n" + 
      "</p>\n" + 
      "</body>" +
      "</html>";
  
  private static final String manifestContentFiltered = 
      "<a href=\"/toc/fasebj/31/12\">December 2017 (Vol. 31 Issue 12 Page 5135-5625)</a>" + 
      "<a href=\"/toc/fasebj/31/11\">November 2017 (Vol. 31 Issue 11 Page 4661-5134)</a>" + 
      "<a href=\"/toc/fasebj/31/10\">October 2017 (Vol. 31 Issue 10 Page 4205-4660)</a>" + 
      "<a href=\"/toc/fasebj/31/9\">September 2017 (Vol. 31 Issue 9 Page 3711-4204)</a>" + 
      "<a href=\"/toc/fasebj/31/8\">August 2017 (Vol. 31 Issue 8 Page 3209-3710)</a>" + 
      "<a href=\"/toc/fasebj/31/7\">July 2017 (Vol. 31 Issue 7 Page 2721-3206)</a>" + 
      "<a href=\"/toc/fasebj/31/6\">June 2017 (Vol. 31 Issue 6 Page 2221-2719)</a>" + 
      "<a href=\"/toc/fasebj/31/5\">May 2017 (Vol. 31 Issue 5 Page 1769-2220)</a>" + 
      "<a href=\"/toc/fasebj/31/1_supplement\">April 2017 (Vol. 31 Issue 1_supplement Page lb1-1091.2)</a>" + 
      "<a href=\"/toc/fasebj/31/4\">April 2017 (Vol. 31 Issue 4 Page 1251-1767)</a>" + 
      "<a href=\"/toc/fasebj/31/3\">March 2017 (Vol. 31 Issue 3 Page 853-1249)</a>" + 
      "<a href=\"/toc/fasebj/31/2\">February 2017 (Vol. 31 Issue 2 Page 423-852)</a>" + 
      "<a href=\"/toc/fasebj/31/1\">January 2017 (Vol. 31 Issue 1 Page 1-421)</a>";
  
  private static final String manifestHashFiltered = 
      " December 2017 (Vol. 31 Issue 12 Page 5135-5625)" +
      " November 2017 (Vol. 31 Issue 11 Page 4661-5134)" +
      " October 2017 (Vol. 31 Issue 10 Page 4205-4660)" +
      " September 2017 (Vol. 31 Issue 9 Page 3711-4204)" +
      " August 2017 (Vol. 31 Issue 8 Page 3209-3710)" +
      " July 2017 (Vol. 31 Issue 7 Page 2721-3206)" +
      " June 2017 (Vol. 31 Issue 6 Page 2221-2719)" +
      " May 2017 (Vol. 31 Issue 5 Page 1769-2220)" +
      " April 2017 (Vol. 31 Issue 1_supplement Page lb1-1091.2)" +
      " April 2017 (Vol. 31 Issue 4 Page 1251-1767)" +
      " March 2017 (Vol. 31 Issue 3 Page 853-1249)" +
      " February 2017 (Vol. 31 Issue 2 Page 423-852)" +
      " January 2017 (Vol. 31 Issue 1 Page 1-421)" +
      " ";
  
  private static final String tocContent = 
      "<html class=\"pb-page\" data-request-id=\"9b091ab2-2ab5-4d31-bed7-31d90503fe85\" lang=\"en\">\n" +
      "  <head data-pb-dropzone=\"head\">...head stuff...</head>\n" + 
      "  <title>Journal Name: Vol 313, No 1</title>\n" + 
      "  <body class=\"pb-ui\">\n" + 
      "    <div class=\"base\" data-db-parent-of=\"sb1\">\n" + 
      "      <header class=\"header \">\n" + 
      "        header stuff \n" + 
      "      </header>\n" + 
      "    </div>\n" + 
      "    <main class=\"content toc\" style=\"min-height: 518px; padding-top: 120.2px;\">\n" + 
      "      <div class=\"top-image\"></div>\n" + 
      "      <div class=\"container\">\n" + 
      "        <div class=\"row\">\n" + 
      "          <div class=\"card card--shadow card--gutter meta__body journal-banner\">\n" + 
      "            <div class=\"meta__left-side left-side-image\">\n" + 
      "              <h1 class=\"meta__title\"><a href=\"/toc/fasebj/32/4\">The FASEB Journal</a></h1>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"ff65c90d-0a0e-49ba-8223-35b246d4c988\" class=\"separator row\">\n" + 
      "        <div class=\"container\">\n" + 
      "          <div class=\"row\">\n" + 
      "            <div class=\"col-sm-7 col-md-8 col-lg-9\">\n" + 
      "              <h3 class=\"section__header border-bottom\">\n" + 
      "                This Issue\n" + 
      "              </h3>\n" + 
      "              <div class=\"toc__latest-issue left-side-image\">\n" + 
      "                <img src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fasebj.2017.31.issue-2/production/fasebj.2017.31.issue-2.cover.gif\" alt=\"The FASEB Journal cover\">\n" + 
      "                <div class=\"meta__body\">\n" + 
      "                  <h5 class=\"meta__title\"> \n" + 
      "                    Volume 31, Issue 2\n" + 
      "                  </h5>\n" + 
      "                  <div class=\"meta__info\">\n" + 
      "                    <p>Pages 423-852</p>\n" + 
      "                    <p>February 2017</p>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <br><br>\n" + 
      "                <hr>\n" + 
      "              </div>\n" + 
      "              <div class=\"table-of-content\">\n" + 
      "                <nav class=\"toc__section\">\n" + 
      "                  <a href=\"#\" data-db-target-for=\"sections\" data-db-switch=\"icon-close_thin\" title=\"Go To Sections\" data-slide-target=\"#sections\" class=\"w-slide__btn\"><i class=\"icon-toc\"></i><span>Sections</span></a>\n" + 
      "                  <div id=\"sections\">\n" + 
      "                    <ul data-db-target-of=\"sections\" class=\"sections__drop rlist separator\">\n" + 
      "                      <li role=\"menuitem\" style=\"\"><a class=\"w-slide__hide\" href=\"#sec_research\"><span>Research</span></a></li>\n" + 
      "                      <li role=\"menuitem\" style=\"\"><a class=\"w-slide__hide\" href=\"#sec_erratum\"><span>Erratum</span></a></li>\n" + 
      "                    </ul>\n" + 
      "                  </div>\n" + 
      "                </nav>\n" + 
      "                <h5 class=\"toc__heading section__header to-section\" id=\"sec_research\">Research</h5>\n" + 
      "                <div class=\"issue-item\">\n" + 
      "                  <div class=\"badges\"><span class=\"badge-type\">Research</span></div>\n" + 
      "                  <h5 class=\"issue-item__title\"><a href=\"/doi/10.1096/fj.201600838R\">Article Title</a></h5>\n" + 
      "                  <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" + 
      "                    <li><a href=\"/author/Author\" title=\"Author\"><span>Author</span></a>, </li>\n" + 
      "                    <li><a href=\"/author/Other\" title=\"Other\"><span>Other</span></a></li>\n" + 
      "                  </ul>\n" + 
      "                  <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                    <li><span>Pages:</span>19-25</li>\n" + 
      "                    <li><span>Published on</span>18 October 2016</li>\n" + 
      "                  </ul>\n" + 
      "                  <p><a href=\"https://doi.org/10.1096/fj.201600838R\">https://doi.org/10.1096/fj.201600838R</a></p>\n" + 
      "                  <div class=\"toc-item__footer\">\n" + 
      "                    <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                      <li><a title=\"Abstract\" href=\"/doi/abs/10.1096/fj.201600838R\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "                      <li><a title=\"Full text\" href=\"/doi/full/10.1096/fj.201600838R\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "                      <li><a title=\"PDF\" href=\"/doi/pdf/10.1096/fj.201600838R\"><span>PDF</span><i class=\"icon icon-file-pdf\"></i></a></li>\n" + 
      "                      <li><a href=\"/doi/references/10.1096/fj.201600838R\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "                      <li><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;\" title=\"OpenURL\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/sfxbutton\" alt=\"OpenURL\"></a></li>\n" + 
      "                    </ul>\n" + 
      "                    <div class=\"accordion\">\n" + 
      "                      <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i>\n" + 
      "                      Preview Abstract\n" + 
      "                      </a>\n" + 
      "                      <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">preview text</div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <hr class=\"section__separator\">\n" + 
      "                <h5 class=\"toc__heading section__header to-section\" id=\"sec_erratum\">Erratum</h5>\n" + 
      "                <div class=\"issue-item\">\n" + 
      "                  <div class=\"badges\"><span class=\"badge-type\">Erratum</span></div>\n" + 
      "                  <h5 class=\"issue-item__title\"><a href=\"/doi/10.1096/fj.201500116ERR\">Erratum</a></h5>\n" + 
      "                  <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                    <li><span>Pages:</span>852-852</li>\n" + 
      "                    <li><span>Published on</span>1 February 2017</li>\n" + 
      "                  </ul>\n" + 
      "                  <p><a href=\"https://doi.org/10.1096/fj.201500116ERR\">https://doi.org/10.1096/fj.201500116ERR</a></p>\n" + 
      "                  <div class=\"toc-item__footer\">\n" + 
      "                    <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                      <li><a title=\"Full text\" href=\"/doi/full/10.1096/fj.201500116ERR\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "                      <li><a title=\"PDF\" href=\"/doi/pdf/10.1096/fj.201500116ERR\"><span>PDF</span><i class=\"icon icon-file-pdf\"></i></a></li>\n" + 
      "                    </ul>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "            <div class=\"col-sm-5 col-md-4 col-lg-3\">\n" + 
      "              <div data-widget-def=\"literatumAd\" data-widget-id=\"befeeeab-d560-4cc4-aa18-9c60a3919ff5\" class=\"top-gutter\">\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      <div class=\"w-slide\">\n" + 
      "        <div class=\"w-slide_head\"><a href=\"#\" class=\"w-slide__back\"><i class=\" icon-arrow_l\" aria-hidden=\"true\"></i>back</a><span class=\"w-slide__title\"></span></div>\n" + 
      "        <div class=\"w-slide__content\"></div>\n" + 
      "      </div>\n" + 
      "    </main>\n" + 
      "    <footer>\n" + 
      "      <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"81a18a14-d709-41fd-9bb6-25a7fc1e13df\" class=\"footer-top clearfix\">\n" + 
      "        <div class=\"container\">\n" + 
      "          <div class=\"row\">\n" + 
      "            <div class=\"col-sm-3\">\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"6e6da9b7-371e-4ea0-827e-8de5915b8cd5\" class=\"footer-bottom text-onDark\">\n" + 
      "        <div class=\"container\">\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "    </footer>\n" + 
      "  </body>\n" + 
      "</html>";
  
  private static final String tocContentCrawlFiltered = 
      "<html class=\"pb-page\" data-request-id=\"9b091ab2-2ab5-4d31-bed7-31d90503fe85\" lang=\"en\">\n" + 
      "  <head data-pb-dropzone=\"head\">...head stuff...</head>\n" + 
      "  <title>Journal Name: Vol 313, No 1</title>\n" + 
      "  <body class=\"pb-ui\">\n" + 
      "    <div class=\"base\" data-db-parent-of=\"sb1\">\n" + 
      "      \n" + 
      "    </div>\n" + 
      "    <main class=\"content toc\" style=\"min-height: 518px; padding-top: 120.2px;\">\n" + 
      "      <div class=\"top-image\"></div>\n" + 
      "      <div class=\"container\">\n" + 
      "        <div class=\"row\">\n" + 
      "          <div class=\"card card--shadow card--gutter meta__body journal-banner\">\n" + 
      "            <div class=\"meta__left-side left-side-image\">\n" + 
      "              <h1 class=\"meta__title\"><a href=\"/toc/fasebj/32/4\">The FASEB Journal</a></h1>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"ff65c90d-0a0e-49ba-8223-35b246d4c988\" class=\"separator row\">\n" + 
      "        <div class=\"container\">\n" + 
      "          <div class=\"row\">\n" + 
      "            <div class=\"col-sm-7 col-md-8 col-lg-9\">\n" + 
      "              <h3 class=\"section__header border-bottom\">\n" + 
      "                This Issue\n" + 
      "              </h3>\n" + 
      "              <div class=\"toc__latest-issue left-side-image\">\n" + 
      "                <img src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fasebj.2017.31.issue-2/production/fasebj.2017.31.issue-2.cover.gif\" alt=\"The FASEB Journal cover\">\n" + 
      "                <div class=\"meta__body\">\n" + 
      "                  <h5 class=\"meta__title\"> \n" + 
      "                    Volume 31, Issue 2\n" + 
      "                  </h5>\n" + 
      "                  <div class=\"meta__info\">\n" + 
      "                    <p>Pages 423-852</p>\n" + 
      "                    <p>February 2017</p>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <br><br>\n" + 
      "                <hr>\n" + 
      "              </div>\n" + 
      "              <div class=\"table-of-content\">\n" + 
      "                \n" + 
      "                <h5 class=\"toc__heading section__header to-section\" id=\"sec_research\">Research</h5>\n" + 
      "                <div class=\"issue-item\">\n" + 
      "                  <div class=\"badges\"><span class=\"badge-type\">Research</span></div>\n" + 
      "                  <h5 class=\"issue-item__title\"><a href=\"/doi/10.1096/fj.201600838R\">Article Title</a></h5>\n" + 
      "                  <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" + 
      "                    <li>, </li>\n" + 
      "                    <li></li>\n" + 
      "                  </ul>\n" + 
      "                  <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                    <li><span>Pages:</span>19-25</li>\n" + 
      "                    <li><span>Published on</span>18 October 2016</li>\n" + 
      "                  </ul>\n" + 
      "                  <p><a href=\"https://doi.org/10.1096/fj.201600838R\">https://doi.org/10.1096/fj.201600838R</a></p>\n" + 
      "                  <div class=\"toc-item__footer\">\n" + 
      "                    <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                      <li><a title=\"Abstract\" href=\"/doi/abs/10.1096/fj.201600838R\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "                      <li><a title=\"Full text\" href=\"/doi/full/10.1096/fj.201600838R\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "                      <li><a title=\"PDF\" href=\"/doi/pdf/10.1096/fj.201600838R\"><span>PDF</span><i class=\"icon icon-file-pdf\"></i></a></li>\n" + 
      "                      <li><a href=\"/doi/references/10.1096/fj.201600838R\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "                      <li></li>\n" + 
      "                    </ul>\n" + 
      "                    <div class=\"accordion\">\n" + 
      "                      <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i>\n" + 
      "                      Preview Abstract\n" + 
      "                      </a>\n" + 
      "                      <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">preview text</div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <hr class=\"section__separator\">\n" + 
      "                <h5 class=\"toc__heading section__header to-section\" id=\"sec_erratum\">Erratum</h5>\n" + 
      "                <div class=\"issue-item\">\n" + 
      "                  <div class=\"badges\"><span class=\"badge-type\">Erratum</span></div>\n" + 
      "                  <h5 class=\"issue-item__title\"></h5>\n" + 
      "                  <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                    <li><span>Pages:</span>852-852</li>\n" + 
      "                    <li><span>Published on</span>1 February 2017</li>\n" + 
      "                  </ul>\n" + 
      "                  <p><a href=\"https://doi.org/10.1096/fj.201500116ERR\">https://doi.org/10.1096/fj.201500116ERR</a></p>\n" + 
      "                  <div class=\"toc-item__footer\">\n" + 
      "                    <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                      <li><a title=\"Full text\" href=\"/doi/full/10.1096/fj.201500116ERR\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "                      <li><a title=\"PDF\" href=\"/doi/pdf/10.1096/fj.201500116ERR\"><span>PDF</span><i class=\"icon icon-file-pdf\"></i></a></li>\n" + 
      "                    </ul>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "            <div class=\"col-sm-5 col-md-4 col-lg-3\">\n" + 
      "              <div data-widget-def=\"literatumAd\" data-widget-id=\"befeeeab-d560-4cc4-aa18-9c60a3919ff5\" class=\"top-gutter\">\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      <div class=\"w-slide\">\n" + 
      "        <div class=\"w-slide_head\"><a href=\"#\" class=\"w-slide__back\"><i class=\" icon-arrow_l\" aria-hidden=\"true\"></i>back</a><span class=\"w-slide__title\"></span></div>\n" + 
      "        <div class=\"w-slide__content\"></div>\n" + 
      "      </div>\n" + 
      "    </main>\n" + 
      "    \n" + 
      "  </body>\n" + 
      "</html>";
  
  private static final String tocContentFiltered = 
      "<div class=\"table-of-content\">\n" + 
      "                \n" + 
      "                <h5 class=\"toc__heading section__header to-section\" >Research</h5>\n" + 
      "                <div class=\"issue-item\">\n" + 
      "                  <div class=\"badges\"><span class=\"badge-type\">Research</span></div>\n" + 
      "                  <h5 class=\"issue-item__title\"><a href=\"/doi/10.1096/fj.201600838R\">Article Title</a></h5>\n" + 
      "                  <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" + 
      "                    <li><a href=\"/author/Author\" title=\"Author\"><span>Author</span></a>, </li>\n" + 
      "                    <li><a href=\"/author/Other\" title=\"Other\"><span>Other</span></a></li>\n" + 
      "                  </ul>\n" + 
      "                  <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                    <li><span>Pages:</span>19-25</li>\n" + 
      "                    <li><span>Published on</span>18 October 2016</li>\n" + 
      "                  </ul>\n" + 
      "                  <p><a href=\"https://doi.org/10.1096/fj.201600838R\">https://doi.org/10.1096/fj.201600838R</a></p>\n" + 
      "                  <div class=\"toc-item__footer\">\n" + 
      "                    <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                      <li><a title=\"Abstract\" href=\"/doi/abs/10.1096/fj.201600838R\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "                      <li><a title=\"Full text\" href=\"/doi/full/10.1096/fj.201600838R\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "                      <li><a title=\"PDF\" href=\"/doi/pdf/10.1096/fj.201600838R\"><span>PDF</span><i class=\"icon icon-file-pdf\"></i></a></li>\n" + 
      "                      <li><a href=\"/doi/references/10.1096/fj.201600838R\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "                      <li></li>\n" + 
      "                    </ul>\n" + 
      "                    <div class=\"accordion\">\n" + 
      "                      <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i>\n" + 
      "                      Preview Abstract\n" + 
      "                      </a>\n" + 
      "                      <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">preview text</div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <hr class=\"section__separator\">\n" + 
      "                <h5 class=\"toc__heading section__header to-section\" >Erratum</h5>\n" + 
      "                <div class=\"issue-item\">\n" + 
      "                  <div class=\"badges\"><span class=\"badge-type\">Erratum</span></div>\n" + 
      "                  <h5 class=\"issue-item__title\"><a href=\"/doi/10.1096/fj.201500116ERR\">Erratum</a></h5>\n" + 
      "                  <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                    <li><span>Pages:</span>852-852</li>\n" + 
      "                    <li><span>Published on</span>1 February 2017</li>\n" + 
      "                  </ul>\n" + 
      "                  <p><a href=\"https://doi.org/10.1096/fj.201500116ERR\">https://doi.org/10.1096/fj.201500116ERR</a></p>\n" + 
      "                  <div class=\"toc-item__footer\">\n" + 
      "                    <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                      <li><a title=\"Full text\" href=\"/doi/full/10.1096/fj.201500116ERR\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "                      <li><a title=\"PDF\" href=\"/doi/pdf/10.1096/fj.201500116ERR\"><span>PDF</span><i class=\"icon icon-file-pdf\"></i></a></li>\n" + 
      "                    </ul>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>";
  
  private static final String tocContentHashFiltered = 
      "" + 
      "" + 
      " Research" +
      "" + 
      " Research" +
      " Article Title" +
      "" + 
      " Author ," +
      " Other" +
      "" + 
      "" + 
      " Pages: 19-25" +
      " Published on 18 October 2016" +
      "" + 
      " https://doi.org/10.1096/fj.201600838R" +
      "" + 
      "" + 
      " Abstract" +
      " Full text" +
      " PDF" +
      " References" +
      "" + 
      "" + 
      "" + 
      "" + 
      " Preview Abstract" + 
      "" + 
      " preview text" +
      "" + 
      "" + 
      "" + 
      "" + 
      " Erratum" +
      "" + 
      " Erratum" +
      " Erratum" +
      "" + 
      " Pages: 852-852" +
      " Published on 1 February 2017" +
      "" + 
      " https://doi.org/10.1096/fj.201500116ERR" +
      "" + 
      "" + 
      " Full text" +
      " PDF" +
      "" + 
      "" + 
      "" + 
      " ";
  
  private static final String art1Content = 
      "<html class=\"pb-page\" data-request-id=\"3b0ad0c2-6486-4c37-b3b1-febfb45e6b98\" lang=\"en\">\n" +
      "<head data-pb-dropzone=\"head\">\n" + 
      "</head>\n" + 
      "<body class=\"pb-ui\">\n" + 
      "  <div id=\"pb-page-content\" data-ng-non-bindable=\"\">\n" + 
      "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "      <div class=\"header base\" data-db-parent-of=\"sb1\">\n" + 
      "        <header class=\"article-page-header hide-menu-dropzone-xs\">\n" + 
      "          <div class=\"popup login-popup hidden\">\n" + 
      "          </div>\n" + 
      "          <div class=\"scrollThenFix\">\n" + 
      "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"f3992bda-f515-41a7-a6c2-85c77bd2ff4b\" class=\"container header--first-row\">\n" + 
      "              <div class=\"pull-left\">\n" + 
      "                <a href=\"/\" class=\"header__logo\">\n" + 
      "                  <picture>\n" + 
      "                    <source srcset=\"/pb-assets/images/logos/logo-xs.png\" media=\"(max-width: 532px)\">\n" + 
      "                    <source srcset=\"/pb-assets/images/logos/logo-sm.png\" media=\"(max-width: 992px)\">\n" + 
      "                    <img src=\"/pb-assets/images/logos/FASEB JournallogoBlue.svg\" alt=\"FASEB Journal logo\">\n" + 
      "                  </picture>\n" + 
      "                </a>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"c76399aa-0643-4b38-875a-73f93c92f22f\" class=\"primary-bg-color text-onDark drawer__nav\">\n" + 
      "              <div class=\"container\">\n" + 
      "                <div class=\"row\">\n" + 
      "                  <div>\n" + 
      "                    <div class=\"main-nav\">\n" + 
      "                      <a href=\"#main-menu\" data-target=\"main-menu\" data-toggle=\"nav\" title=\"menu drawer\"><i aria-hidden=\"true\" class=\"icon-menu2\"></i><i aria-hidden=\"true\" class=\"icon-close_thin\"></i></a>\n" + 
      "                      <nav role=\"navigation\" id=\"main-menu\" data-ctrl-res=\"screen-sm\" class=\"drawer__nav container gutterless hidden-sm hidden-xs\">\n" + 
      "                        <a href=\"#\" class=\"menu-header hidden-md hidden-lg\">Home</a>\n" + 
      "                        <ul id=\"menubar\" role=\"menubar\" class=\"menubar rlist--inline\">\n" + 
      "                          <li role=\"menuitem\" aria-label=\"Home\" id=\"menu-item-main-menu-0\" class=\"menu-item\"><a href=\"/\" title=\"Home\"><span>Home</span></a></li>\n" + 
      "                          <li role=\"menuitem\" aria-label=\"Current Issue\" id=\"menu-item-main-menu-1\" class=\"menu-item\"><a href=\"/toc/fasebj/current\" title=\"Current Issue\"><span>Current Issue</span></a></li>\n" + 
      "                        </ul>\n" + 
      "                      </nav>\n" + 
      "                    </div>\n" + 
      "                    <div class=\"header__dropzone\">\n" + 
      "                      <a href=\"/action/showPreferences?menuTab=Alerts\"><i class=\"icon-Icon_Alerts\"></i></a>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </header>\n" + 
      "      </div>\n" + 
      "      <main class=\"content\" style=\"min-height: 518px; padding-top: 0px;\">\n" + 
      "        <div>\n" + 
      "          <article data-figures=\"http://www.fasebj.org/action/ajaxShowFigures?doi=10.1096%2Ffj.201500132&amp;ajax=true\" data-references=\"http://www.fasebj.org/action/ajaxShowEnhancedAbstract?doi=10.1096%2Ffj.201500132&amp;ajax=true\" class=\"container\">\n" + 
      "            <div class=\"row\">\n" + 
      "              <div class=\"article__content col-lg-9 col-sm-7 col-md-8\">\n" + 
      "                <!--+articleHEeader()--><!--+articleCitation()-->\n" + 
      "                <div class=\"citation\">\n" + 
      "                  <div class=\"citation__top\"><span class=\"article__breadcrumbs\"></span><span class=\"citation__top__item article__tocHeading\">Research</span><span class=\"citation__top__item article__access\"></span></div>\n" + 
      "                  <h1 class=\"citation__title\">Article Title</h1>\n" + 
      "                  <ul class=\"rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" + 
      "                    <li><a href=\"#\" title=\"Author\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author</span></a></li>\n" + 
      "                    , and \n" + 
      "                    <li><a href=\"#\" title=\"Other\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Other</span><i aria-hidden=\"true\" class=\"icon-Email\"></i></a></li>\n" + 
      "                  </ul>\n" + 
      "                  <ul class=\"rlist--inline article__corrections\">\n" + 
      "                    <div class=\"corrections-articles\"></div>\n" + 
      "                  </ul>\n" + 
      "                </div>\n" + 
      "                <div class=\"epub-section\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">Published Online:</span><span class=\"epub-section__date\">13 Sep 2016</span></span><span class=\"epub-section__item\">" +
      "<a href=\"https://doi.org/10.1096/fj.201500132\" class=\"epub-section__doi__text\">https://doi.org/10.1096/fj.201500132</a></span></div>\n" + 
      "                <!--+articleCoolbar()-->\n" + 
      "                <nav class=\"stickybar coolBar trans\">\n" + 
      "                  <div class=\"stickybar__wrapper coolBar__wrapper clearfix\" style=\"width: 1050px; top: 120.2px;\">\n" + 
      "                    <div class=\"rlist coolBar__zone\">\n" + 
      "                    </div>\n" + 
      "                    <ul data-cb-group=\"Article\" data-cb-group-icon=\"icon-toc\" class=\"rlist coolBar__first\">\n" + 
      "                      <li class=\"coolBar__section coolBar--sections\">\n" + 
      "                        <a href=\"#\" data-db-target-for=\"sections\" data-db-switch=\"icon-close_thin\" title=\"Sections\" data-slide-target=\"#sectionMenu\"" +
      " class=\"coolBar__ctrl w-slide__btn\"><i aria-hidden=\"true\" class=\"icon-Icon_Section-menu\"></i><span>Sections</span></a>\n" + 
      "                      </li>\n" + 
      "                      <li class=\"coolBar__section coolBar--pdf\"><a href=\"/doi/pdf/10.1096/fj.201500132\" class=\"coolBar__ctrl coolBar__ctrl__pdf cloned hidden-xs hidden-sm\"><i aria-hidden=\"true\" class=\"icon-pdf\"></i><span>PDF</span></a></li>\n" + 
      "                    </ul>\n" + 
      "                  </div>\n" + 
      "                </nav>\n" + 
      "                <div class=\"article__body\">\n" + 
      "                  <p class=\"fulltext\"></p>\n" + 
      "                  <!--abstract content-->\n" + 
      "                  <div class=\"hlFld-Abstract\">\n" + 
      "                    <p class=\"fulltext\"></p>\n" + 
      "                    <h2 class=\"article-section__title section__title\" id=\"d400253e1\">Abstract</h2>\n" + 
      "                    <div class=\"abstractSection abstractInFull\">\n" + 
      "                      <p>abstract In Full text</p>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <!--/abstract content--><!--fulltext content-->\n" + 
      "                  <div class=\"hlFld-Fulltext\">\n" + 
      "                    <p>fulltext content</p>\n" + 
      "                    <div id=\"_i1\" class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" id=\"_i1\">MATERIALS AND METHODS</h2>\n" + 
      "                    <div id=\"_i2\" class=\"anchor-spacer\"></div>\n" + 
      "                    <h3 class=\"article-section__title\" id=\"_i2\">Cell culture</h3>\n" + 
      "                    <figure id=\"F1\" class=\"article__inlineFigure\">\n" + 
      "                      <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/medium/fasebj201500132f1.jpeg\" data-lg-src=\"/na101/home/literatum/publisher" +
      "/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\" alt=\"Figure 1.\">\n" + 
      "                      <figcaption>\n" + 
      "                        <strong class=\"figure__title\"></strong>\n" + 
      "                        <span class=\"figure__caption\">\n" + 
      "                          <p><span class=\"captionLabel\">Figure 1.</span> figure caption.</p>\n" + 
      "                        </span>\n" + 
      "                      </figcaption>\n" + 
      "                      <div class=\"figure-links\"><a href=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\">Download figure</a>&nbsp;" +
      "<a href=\"/action/downloadFigures?id=F1&amp;doi=10.1096/fj.201500132\">Download PowerPoint</a></div>\n" + 
      "                    </figure>\n" + 
      "                    <div id=\"_i27\" class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" id=\"_i30\">CONCLUSIONS</h2>\n" + 
      "                    <p>lastly</p>\n" + 
      "                    <div id=\"_i31\" class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" id=\"fulltext_glossary\">ABBREVIATIONS</h2>\n" + 
      "                    <div class=\"glossary\">\n" + 
      "                      <table summary=\"\" class=\"NLM_def-list\">\n" + 
      "                        <tbody>\n" + 
      "                          <tr>\n" + 
      "                            <td class=\"NLM_term\">AEA</td>\n" + 
      "                            <td class=\"NLM_def\">\n" + 
      "                              <p>arachidonyl ethanolamide or anandamide</p>\n" + 
      "                            </td>\n" + 
      "                          </tr>\n" + 
      "                        </tbody>\n" + 
      "                      </table>\n" + 
      "                    </div>\n" + 
      "                    <div class=\"ack\">\n" + 
      "                      <h2 class=\"article-section__title section__title\" id=\"_i32\">ACKNOWLEDGMENTS</h2>\n" + 
      "                      <p>The authors thank you.</p>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <!--/fulltext content-->\n" + 
      "                  <p class=\"fulltext\"></p>\n" + 
      "                  <i></i>\n" + 
      "                  <div class=\"response\">\n" + 
      "                    <div class=\"sub-article-title\"></div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <div class=\"col-sm-5 col-md-4 col-lg-3 hidden-xs sticko__parent\">\n" + 
      "                <!--+articleTab()-->\n" + 
      "                <div class=\"tab tab--slide tab--flex sticko__md tab--flex tabs--xs js--sticko\" style=\"width: 330px; top: 130.2px;\">\n" + 
      "                  <ul class=\"rlist tab__content sticko__child\" style=\"height: 539.8px; overflow-y: auto;\">\n" + 
      "                    <li id=\"pane-pcw-figures\" aria-labelledby=\"pane-pcw-figurescon\" role=\"tabpanel\" class=\"tab__pane\">\n" + 
      "                      <figure class=\"article__tabFigure\" data-figure-id=\"F1\">\n" + 
      "                        <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/medium/fasebj201500132f1.jpeg\" data-lg-src=\"/na101/home/literatum/publisher" +
      "/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\" alt=\"Figure 1.\">\n" + 
      "                        <figcaption>\n" + 
      "                          <strong class=\"figure__title\"></strong>\n" + 
      "                          <span class=\"figure__caption\">\n" + 
      "                            <p>Figure 1. caption.</p>\n" + 
      "                          </span>\n" + 
      "                        </figcaption>\n" + 
      "                        <div class=\"figure-links\">" +
      "<a href=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\">" +
      "Download figure</a>&nbsp;<a href=\"/action/downloadFigures?id=F1&amp;doi=10.1096/fj.201500132\">Download PowerPoint</a></div>\n" + 
      "                      </figure>\n" + 
      "                    </li>\n" + 
      "                    <li id=\"pane-pcw-references\" aria-labelledby=\"pane-pcw-referencescon\" role=\"tabpanel\" class=\"tab__pane\">\n" + 
      "                      <div class=\"article__references\">\n" + 
      "                        <p class=\"explanation__text\"></p>\n" + 
      "                        <h2>References</h2>\n" + 
      "                        <ul class=\"rlist separator\">\n" + 
      "                          <li id=\"B1\" class=\"\n" + 
      "                            references__item\n" + 
      "                            \"><span class=\"references__note\">" +
      "<span class=\"label\">1 </span><span class=\"references__authors\">auths</span> (<span class=\"references__year\">2014</span>)" +
      " <span class=\"references__article-title\">Surface.</span>" +
      " <span class=\"references__source\"><strong>J.</strong></span> <b>49</b>, 681–689 <a href=\"/servlet/linkout?suffix=B1\" onclick=\"newWindow(this.href);return false\" target=\"_blank\">Crossref</a>" +
      " <a href=\"/servlet/linkout?suffix=B1&amp;dbid=8&amp;doi=10.1096%2Ffj.201500132&amp;key=24694282\" onclick=\"newWindow(this.href);return false\" target=\"_blank\">Medline</a></span></span></li>\n" + 
      "                        </ul>\n" + 
      "                      </div>\n" + 
      "                    </li>\n" + 
      "                    <li id=\"pane-pcw-related\" aria-labelledby=\"pane-pcw-relatedcon\" role=\"tabpanel\" class=\"accordion-with-arrow tab__pane tab__pane--clear\">\n" + 
      "                      <div class=\"accordion\">\n" + 
      "                        <ul class=\"accordion-tabbed rlist\">\n" + 
      "                          <li class=\"accordion-tabbed__tab js--open\">\n" + 
      "                            <a href=\"#\" title=\"Cited By\" aria-expanded=\"true\" aria-controls=\"relatedTab1\" class=\"accordion-tabbed__control\">Cited By</a>\n" + 
      "                            <div id=\"relatedTab1\" class=\"accordion-tabbed__content\">\n" + 
      "                              <ul class=\"rlist\">\n" + 
      "                                <li>\n" + 
      "                                  <div class=\"related\">\n" + 
      "                                    all kinds of stuff\n" + 
      "                                  </div>\n" + 
      "                                </li>\n" + 
      "                              </ul>\n" + 
      "                            </div>\n" + 
      "                          </li>\n" + 
      "                          <li class=\"accordion-tabbed__tab\">\n" + 
      "                            <a href=\"#\" title=\"Similar\" aria-expanded=\"false\" aria-controls=\"relatedTab3\" class=\"accordion-tabbed__control\">Recommended</a>\n" + 
      "                            <div id=\"relatedTab3\" class=\"accordion-tabbed__content\" style=\"display: none;\">\n" + 
      "                              <!-- 0892-6638 -->\n" + 
      "                              <div id=\"trendmd-suggestions\">\n" + 
      "                                <div class=\"trendmd-widget-container trendmd-widget_title-larger trendmd-widget_body-normal\" data-trendmd-journal-id=\"25105\" data-trendmd-article-id=\"41ba8ce7be94\">\n" + 
      "                                  <div class=\"trendmd-widget \">\n" + 
      "                                    ???\n" + 
      "                                  </div>\n" + 
      "                                </div>\n" + 
      "                              </div>\n" + 
      "                            </div>\n" + 
      "                          </li>\n" + 
      "                        </ul>\n" + 
      "                      </div>\n" + 
      "                    </li>\n" + 
      "                    <li id=\"pane-pcw-details\" aria-labelledby=\"pane-pcw-detailscon\" role=\"tabpanel\" class=\"tab__pane active\">\n" + 
      "                      <div class=\"details-tab\">\n" + 
      "                        <div class=\"cover-meta-wrap\">\n" + 
      "                          <a href=\"/toc/fasebj/31/2\"><img src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fasebj.2017.31.issue-2/production/fasebj.2017.31.issue-2.cover.gif\"></a>\n" + 
      "                          <div class=\"meta\">\n" + 
      "                            <strong><a href=\"/toc/fasebj/31/2\">Vol. 31, No. 2</a></strong>\n" + 
      "                            <div>February 2017</div>\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                        <hr class=\"section__separator\">\n" + 
      "                        <a href=\"/doi/suppl/10.1096/fj.201500132\" class=\"format-link\">Supplemental Materials</a>\n" + 
      "                        <hr class=\"section__separator\">\n" + 
      "                        <section class=\"article__metrics\">\n" + 
      "                          <strong class=\"section__title\">Metrics</strong>\n" + 
      "                          <div class=\"section__body\">\n" + 
      "                            <p>\n" + 
      "                              Downloaded 64 times\n" + 
      "                              <script type=\"text/javascript\" src=\"https://d1bxh8uas1mnw7.cloudfront.net/assets/embed.js\"></script>\n" + 
      "                            </p>\n" + 
      "                            <div data-badge-details=\"right\" data-badge-type=\"donut\" data-doi=\"10.1096/fj.201500132\" data-hide-no-mentions=\"true\" class=\"altmetric-embed\" data-uuid=\"582401e9-04b6-2e22-fe8e-b344e79c8cc0\">\n" + 
      "                              <div style=\"overflow:hidden;\">\n" + 
      "                                <div class=\"altmetric-normal-legend\">\n" + 
      "                                  <a target=\"_self\" href=\"https://www.altmetric.com/details.php?domain=www.fasebj.org&amp;citation_id=12010871\" style=\"display:inline-block;\">\n" + 
      "                                  <img alt=\"Article has an altmetric score of 2\" src=\"https://badges.altmetric.com/?size=128&amp;score=2&amp;types=tttttttt\" style=\"border:0; margin:0; max-width: none;\" width=\"64px\" height=\"64px\">\n" + 
      "                                  </a>\n" + 
      "                                </div>\n" + 
      "                                <div id=\"_altmetric_popover_el\" class=\"altmetric-embed right\" style=\"margin:0; padding:0; display:inline-block; float:left; position:relative;\">\n" + 
      "                                  <div class=\"altmetric_container\" id=\"_altmetric_container\">\n" + 
      "                                    <div class=\"altmetric-embed altmetric-popover-inner right\" id=\"_altmetric_popover_inner\">\n" + 
      "                                      <div style=\"padding:0; margin: 0;\" class=\"altmetric-embed altmetric-popover-content\">\n" + 
      "                                        <div style=\"padding-left: 10px; line-height:18px; border-left: 16px solid #74CFED;\">\n" + 
      "                                          <a class=\"link-to-altmetric-details-tab\" target=\"_self\" href=\"https://www.altmetric.com/details.php?domain=www.fasebj.org&amp;citation_id=12010871&amp;tab=twitter\">\n" + 
      "                                          Tweeted by <b>5</b>\n" + 
      "                                          </a>\n" + 
      "                                        </div>\n" + 
      "                                        <div class=\"altmetric-embed readers\" style=\"margin-top: 10px;\">\n" + 
      "                                          <div class=\"altmetric-embed tip_mendeley\" style=\"padding-left: 10px; line-height:18px; border-left: 16px solid #A60000;\">\n" + 
      "                                            <b>15</b> readers on Mendeley            \n" + 
      "                                          </div>\n" + 
      "                                        </div>\n" + 
      "                                      </div>\n" + 
      "                                    </div>\n" + 
      "                                  </div>\n" + 
      "                                </div>\n" + 
      "                              </div>\n" + 
      "                            </div>\n" + 
      "                            <div data-widget-def=\"UX3HTMLWidget\" data-widget-id=\"39aeaf81-5656-418c-abd6-9574e7224421\" class=\"top-gutter\">\n" + 
      "                              <div>\n" + 
      "                                <!-- no details widget for 10.1096/fj.201500132 -->\n" + 
      "                              </div>\n" + 
      "                            </div>\n" + 
      "                            <p></p>\n" + 
      "                          </div>\n" + 
      "                        </section>\n" + 
      "                      </div>\n" + 
      "                      <section class=\"article__keyword\">\n" + 
      "                        <strong class=\"section__title\">Keywords</strong>\n" + 
      "                        <div class=\"section__body\">\n" + 
      "                          <ul class=\"rlist rlist--inline\">\n" + 
      "                            <li><a href=\"/keyword/Gut\" class=\"badge-type\">gut</a></li>\n" + 
      "                            <li><a href=\"/keyword/Cannabinoid\" class=\"badge-type\">cannabinoid</a></li>\n" + 
      "                            <li><a href=\"/keyword/Nuclear+Receptor\" class=\"badge-type\">nuclear receptor</a></li>\n" + 
      "                          </ul>\n" + 
      "                        </div>\n" + 
      "                      </section>\n" + 
      "                      <hr class=\"section__separator\">\n" + 
      "                      <ul class=\"rlist rlist--inline\">\n" + 
      "                        <section class=\"article__acknowledgements\">\n" + 
      "                          <strong class=\"section__title\">ACKNOWLEDGMENTS</strong>\n" + 
      "                          <div class=\"section__body\">\n" + 
      "                            <p>The authors thank.</p>\n" + 
      "                          </div>\n" + 
      "                        </section>\n" + 
      "                        <hr class=\"section__separator\">\n" + 
      "                      </ul>\n" + 
      "                      <h6 class=\"section__title\">\n" + 
      "                        Publication History\n" + 
      "                      </h6>\n" + 
      "                      <div>Published in print 1 February 2017</div>\n" + 
      "                    </li>\n" + 
      "                    <li class=\"tab__spinner\" style=\"display: none;\"><img src=\"/ux3/widgets/publication-content/images/spinner.gif\" id=\"spinner\" style=\"width: 100%\"></li>\n" + 
      "                  </ul>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </article>\n" + 
      "        </div>\n" + 
      "        <script>$('.article__body:not(.show-references) .article__references').hide();</script>\n" + 
      "        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" + 
      "        </div>\n" + 
      "      </main>\n" + 
      "      <footer>\n" + 
      "        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"81a18a14-d709-41fd-9bb6-25a7fc1e13df\" class=\"footer-top clearfix\">\n" + 
      "          <div class=\"container\">\n" + 
      "            <div class=\"row\">\n" + 
      "              <div class=\"col-sm-3\">\n" +
      "                footer text" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </footer>\n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "</body>\n" + 
      "</html>";
  
  private static final String art1ContentCrawlFiltered = 
      "<html class=\"pb-page\" data-request-id=\"3b0ad0c2-6486-4c37-b3b1-febfb45e6b98\" lang=\"en\">\n" + 
      "<head data-pb-dropzone=\"head\">\n" + 
      "</head>\n" + 
      "<body class=\"pb-ui\">\n" + 
      "  <div id=\"pb-page-content\" data-ng-non-bindable=\"\">\n" + 
      "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "      <div class=\"header base\" data-db-parent-of=\"sb1\">\n" + 
      "        \n" + 
      "      </div>\n" + 
      "      <main class=\"content\" style=\"min-height: 518px; padding-top: 0px;\">\n" + 
      "        <div>\n" + 
      "          <article data-figures=\"http://www.fasebj.org/action/ajaxShowFigures?doi=10.1096%2Ffj.201500132&amp;ajax=true\" data-references=\"http://www.fasebj.org/action/ajaxShowEnhancedAbstract?doi=10.1096%2Ffj.201500132&amp;ajax=true\" class=\"container\">\n" + 
      "            <div class=\"row\">\n" + 
      "              <div class=\"article__content col-lg-9 col-sm-7 col-md-8\">\n" + 
      "                <!--+articleHEeader()--><!--+articleCitation()-->\n" + 
      "                <div class=\"citation\">\n" + 
      "                  <div class=\"citation__top\"><span class=\"article__breadcrumbs\"></span><span class=\"citation__top__item article__tocHeading\">Research</span><span class=\"citation__top__item article__access\"></span></div>\n" + 
      "                  <h1 class=\"citation__title\">Article Title</h1>\n" + 
      "                  <ul class=\"rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" + 
      "                    <li><a href=\"#\" title=\"Author\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author</span></a></li>\n" + 
      "                    , and \n" + 
      "                    <li><a href=\"#\" title=\"Other\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Other</span><i aria-hidden=\"true\" class=\"icon-Email\"></i></a></li>\n" + 
      "                  </ul>\n" + 
      "                  \n" + 
      "                </div>\n" + 
      "                <div class=\"epub-section\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">Published Online:</span><span class=\"epub-section__date\">13 Sep 2016</span></span><span class=\"epub-section__item\"><a href=\"https://doi.org/10.1096/fj.201500132\" class=\"epub-section__doi__text\">https://doi.org/10.1096/fj.201500132</a></span></div>\n" + 
      "                <!--+articleCoolbar()-->\n" + 
      "                \n" + 
      "                <div class=\"article__body\">\n" + 
      "                  <p class=\"fulltext\"></p>\n" + 
      "                  <!--abstract content-->\n" + 
      "                  <div class=\"hlFld-Abstract\">\n" + 
      "                    <p class=\"fulltext\"></p>\n" + 
      "                    <h2 class=\"article-section__title section__title\" id=\"d400253e1\">Abstract</h2>\n" + 
      "                    <div class=\"abstractSection abstractInFull\">\n" + 
      "                      <p>abstract In Full text</p>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <!--/abstract content--><!--fulltext content-->\n" + 
      "                  <div class=\"hlFld-Fulltext\">\n" + 
      "                    <p>fulltext content</p>\n" + 
      "                    <div id=\"_i1\" class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" id=\"_i1\">MATERIALS AND METHODS</h2>\n" + 
      "                    <div id=\"_i2\" class=\"anchor-spacer\"></div>\n" + 
      "                    <h3 class=\"article-section__title\" id=\"_i2\">Cell culture</h3>\n" + 
      "                    <figure id=\"F1\" class=\"article__inlineFigure\">\n" + 
      "                      <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/medium/fasebj201500132f1.jpeg\" data-lg-src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\" alt=\"Figure 1.\">\n" + 
      "                      <figcaption>\n" + 
      "                        <strong class=\"figure__title\"></strong>\n" + 
      "                        <span class=\"figure__caption\">\n" + 
      "                          <p><span class=\"captionLabel\">Figure 1.</span> figure caption.</p>\n" + 
      "                        </span>\n" + 
      "                      </figcaption>\n" + 
      "                      <div class=\"figure-links\"><a href=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\">Download figure</a>&nbsp;<a href=\"/action/downloadFigures?id=F1&amp;doi=10.1096/fj.201500132\">Download PowerPoint</a></div>\n" + 
      "                    </figure>\n" + 
      "                    <div id=\"_i27\" class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" id=\"_i30\">CONCLUSIONS</h2>\n" + 
      "                    <p>lastly</p>\n" + 
      "                    <div id=\"_i31\" class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" id=\"fulltext_glossary\">ABBREVIATIONS</h2>\n" + 
      "                    <div class=\"glossary\">\n" + 
      "                      <table summary=\"\" class=\"NLM_def-list\">\n" + 
      "                        <tbody>\n" + 
      "                          <tr>\n" + 
      "                            <td class=\"NLM_term\">AEA</td>\n" + 
      "                            <td class=\"NLM_def\">\n" + 
      "                              <p>arachidonyl ethanolamide or anandamide</p>\n" + 
      "                            </td>\n" + 
      "                          </tr>\n" + 
      "                        </tbody>\n" + 
      "                      </table>\n" + 
      "                    </div>\n" + 
      "                    <div class=\"ack\">\n" + 
      "                      <h2 class=\"article-section__title section__title\" id=\"_i32\">ACKNOWLEDGMENTS</h2>\n" + 
      "                      <p>The authors thank you.</p>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <!--/fulltext content-->\n" + 
      "                  <p class=\"fulltext\"></p>\n" + 
      "                  <i></i>\n" + 
      "                  <div class=\"response\">\n" + 
      "                    <div class=\"sub-article-title\"></div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <div class=\"col-sm-5 col-md-4 col-lg-3 hidden-xs sticko__parent\">\n" + 
      "                <!--+articleTab()-->\n" + 
      "                <div class=\"tab tab--slide tab--flex sticko__md tab--flex tabs--xs js--sticko\" style=\"width: 330px; top: 130.2px;\">\n" + 
      "                  <ul class=\"rlist tab__content sticko__child\" style=\"height: 539.8px; overflow-y: auto;\">\n" + 
      "                    <li id=\"pane-pcw-figures\" aria-labelledby=\"pane-pcw-figurescon\" role=\"tabpanel\" class=\"tab__pane\">\n" + 
      "                      <figure class=\"article__tabFigure\" data-figure-id=\"F1\">\n" + 
      "                        <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/medium/fasebj201500132f1.jpeg\" data-lg-src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\" alt=\"Figure 1.\">\n" + 
      "                        <figcaption>\n" + 
      "                          <strong class=\"figure__title\"></strong>\n" + 
      "                          <span class=\"figure__caption\">\n" + 
      "                            <p>Figure 1. caption.</p>\n" + 
      "                          </span>\n" + 
      "                        </figcaption>\n" + 
      "                        <div class=\"figure-links\"><a href=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\">Download figure</a>&nbsp;<a href=\"/action/downloadFigures?id=F1&amp;doi=10.1096/fj.201500132\">Download PowerPoint</a></div>\n" + 
      "                      </figure>\n" + 
      "                    </li>\n" + 
      "                    \n" + 
      "                    \n" + 
      "                    <li id=\"pane-pcw-details\" aria-labelledby=\"pane-pcw-detailscon\" role=\"tabpanel\" class=\"tab__pane active\">\n" + 
      "                      <div class=\"details-tab\">\n" + 
      "                        <div class=\"cover-meta-wrap\">\n" + 
      "                          <a href=\"/toc/fasebj/31/2\"><img src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fasebj.2017.31.issue-2/production/fasebj.2017.31.issue-2.cover.gif\"></a>\n" + 
      "                          <div class=\"meta\">\n" + 
      "                            <strong><a href=\"/toc/fasebj/31/2\">Vol. 31, No. 2</a></strong>\n" + 
      "                            <div>February 2017</div>\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                        <hr class=\"section__separator\">\n" + 
      "                        <a href=\"/doi/suppl/10.1096/fj.201500132\" class=\"format-link\">Supplemental Materials</a>\n" + 
      "                        <hr class=\"section__separator\">\n" + 
      "                        \n" + 
      "                      </div>\n" + 
      "                      \n" + 
      "                      <hr class=\"section__separator\">\n" + 
      "                      <ul class=\"rlist rlist--inline\">\n" + 
      "                        <section class=\"article__acknowledgements\">\n" + 
      "                          <strong class=\"section__title\">ACKNOWLEDGMENTS</strong>\n" + 
      "                          <div class=\"section__body\">\n" + 
      "                            <p>The authors thank.</p>\n" + 
      "                          </div>\n" + 
      "                        </section>\n" + 
      "                        <hr class=\"section__separator\">\n" + 
      "                      </ul>\n" + 
      "                      <h6 class=\"section__title\">\n" + 
      "                        Publication History\n" + 
      "                      </h6>\n" + 
      "                      <div>Published in print 1 February 2017</div>\n" + 
      "                    </li>\n" + 
      "                    <li class=\"tab__spinner\" style=\"display: none;\"><img src=\"/ux3/widgets/publication-content/images/spinner.gif\" id=\"spinner\" style=\"width: 100%\"></li>\n" + 
      "                  </ul>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </article>\n" + 
      "        </div>\n" + 
      "        <script>$('.article__body:not(.show-references) .article__references').hide();</script>\n" + 
      "        <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" + 
      "        </div>\n" + 
      "      </main>\n" + 
      "      \n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "</body>\n" +
      "</html>";
  
  private static final String art1ContentFiltered = 
      "<div class=\"article__content col-lg-9 col-sm-7 col-md-8\">\n" + 
      "                \n" + 
      "                <div class=\"citation\">\n" + 
      "                  <div class=\"citation__top\"><span class=\"article__breadcrumbs\"></span><span class=\"citation__top__item article__tocHeading\">Research</span><span class=\"citation__top__item article__access\"></span></div>\n" + 
      "                  <h1 class=\"citation__title\">Article Title</h1>\n" + 
      "                  <ul class=\"rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" + 
      "                    <li><a href=\"#\" title=\"Author\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author</span></a></li>\n" + 
      "                    , and \n" + 
      "                    <li><a href=\"#\" title=\"Other\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Other</span><i aria-hidden=\"true\" class=\"icon-Email\"></i></a></li>\n" + 
      "                  </ul>\n" + 
      "                  \n" + 
      "                </div>\n" + 
      "                <div class=\"epub-section\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">Published Online:</span><span class=\"epub-section__date\">13 Sep 2016</span></span><span class=\"epub-section__item\"><a href=\"https://doi.org/10.1096/fj.201500132\" class=\"epub-section__doi__text\">https://doi.org/10.1096/fj.201500132</a></span></div>\n" + 
      "                \n" + 
      "                \n" + 
      "                <div class=\"article__body\">\n" + 
      "                  <p class=\"fulltext\"></p>\n" + 
      "                  \n" + 
      "                  <div class=\"hlFld-Abstract\">\n" + 
      "                    <p class=\"fulltext\"></p>\n" + 
      "                    <h2 class=\"article-section__title section__title\" >Abstract</h2>\n" + 
      "                    <div class=\"abstractSection abstractInFull\">\n" + 
      "                      <p>abstract In Full text</p>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  \n" + 
      "                  <div class=\"hlFld-Fulltext\">\n" + 
      "                    <p>fulltext content</p>\n" + 
      "                    <div  class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" >MATERIALS AND METHODS</h2>\n" + 
      "                    <div  class=\"anchor-spacer\"></div>\n" + 
      "                    <h3 class=\"article-section__title\" >Cell culture</h3>\n" + 
      "                    <figure  class=\"article__inlineFigure\">\n" + 
      "                      <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/medium/fasebj201500132f1.jpeg\" data-lg-src=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\" alt=\"Figure 1.\">\n" + 
      "                      <figcaption>\n" + 
      "                        <strong class=\"figure__title\"></strong>\n" + 
      "                        <span class=\"figure__caption\">\n" + 
      "                          <p><span class=\"captionLabel\">Figure 1.</span> figure caption.</p>\n" + 
      "                        </span>\n" + 
      "                      </figcaption>\n" + 
      "                      <div class=\"figure-links\"><a href=\"/na101/home/literatum/publisher/faseb/journals/content/fasebj/2017/fasebj.2017.31.issue-2/fj.201500132/production/images/large/fasebj201500132f1.jpeg\">Download figure</a>&nbsp;<a href=\"/action/downloadFigures?id=F1&amp;doi=10.1096/fj.201500132\">Download PowerPoint</a></div>\n" + 
      "                    </figure>\n" + 
      "                    <div  class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" >CONCLUSIONS</h2>\n" + 
      "                    <p>lastly</p>\n" + 
      "                    <div  class=\"anchor-spacer\"></div>\n" + 
      "                    <h2 class=\"article-section__title section__title\" >ABBREVIATIONS</h2>\n" + 
      "                    <div class=\"glossary\">\n" + 
      "                      <table summary=\"\" class=\"NLM_def-list\">\n" + 
      "                        <tbody>\n" + 
      "                          <tr>\n" + 
      "                            <td class=\"NLM_term\">AEA</td>\n" + 
      "                            <td class=\"NLM_def\">\n" + 
      "                              <p>arachidonyl ethanolamide or anandamide</p>\n" + 
      "                            </td>\n" + 
      "                          </tr>\n" + 
      "                        </tbody>\n" + 
      "                      </table>\n" + 
      "                    </div>\n" + 
      "                    <div class=\"ack\">\n" + 
      "                      <h2 class=\"article-section__title section__title\" >ACKNOWLEDGMENTS</h2>\n" + 
      "                      <p>The authors thank you.</p>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  \n" + 
      "                  <p class=\"fulltext\"></p>\n" + 
      "                  <i></i>\n" + 
      "                  \n" + 
      "                </div>\n" + 
      "              </div>";
  
  private static final String art1ContentHashFiltered = 
      "" + 
      "" + 
      "" + 
      " Research" + 
      " Article Title" + 
      "" + 
      " Author" + 
      " , and" + 
      " Other" + 
      "" + 
      "" + 
      "" + 
      " Published Online: 13 Sep 2016 https://doi.org/10.1096/fj.201500132" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " Abstract" + 
      "" + 
      " abstract In Full text" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " fulltext content" + 
      "" + 
      " MATERIALS AND METHODS" + 
      "" + 
      " Cell culture" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " Figure 1. figure caption." + 
      "" + 
      "" + 
      " Download figure Download PowerPoint" + 
      "" + 
      "" + 
      " CONCLUSIONS" + 
      " lastly" + 
      "" + 
      " ABBREVIATIONS" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " AEA" + 
      "" + 
      " arachidonyl ethanolamide or anandamide" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " ACKNOWLEDGMENTS" + 
      " The authors thank you." + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " ";
  
  private static final String citContent = 
      "<html>\n" +
      "<div class=\"citationFormats\">\n" + 
      "  <div class=\"citation-download\">\n" + 
      "    <p class=\"citation-msg\">\n" + 
      "      download article citation data to the citation manager of your choice.<br><br>\n" + 
      "    </p>\n" + 
      "    <!-- download options -->\n" + 
      "    <form action=\"/action/downloadCitation\" name=\"frmCitmgr\" class=\"citation-form\" method=\"post\" target=\"_self\">\n" + 
      "    </form>\n" + 
      "  </div>\n" + 
      "  <!-- list of articles -->\n" + 
      "  <div class=\"articleList\">\n" + 
      "    <span class=\"sectionTitle\">Download article citation data for:</span>\n" + 
      "    <hr>\n" + 
      "    <div class=\"art_title\"><a href=\"/doi/abs/10.1152/jn.00002.2017\">research in progress</a></div>\n" + 
      "    <div class=\"art_authors\"><span class=\"NLM_string-name\">A Author</span>" +
      "    </div>\n" + 
      "    <span class=\"journalName\">journal Name</span>\n" + 
      "    <span class=\"year\">2017</span>\n" + 
      "    <span class=\"volume\">99</span>:<span class=\"issue\">9</span>,\n" + 
      "    <br>\n" + 
      "    <hr>\n" + 
      "  </div>\n" + 
      "</div>\n" +
      "</html>";
  
  private static final String citContentFiltered = 
      "<div class=\"articleList\">\n" + 
      "    <span class=\"sectionTitle\">Download article citation data for:</span>\n" + 
      "    <hr>\n" + 
      "    <div class=\"art_title\"><a href=\"/doi/abs/10.1152/jn.00002.2017\">research in progress</a></div>\n" + 
      "    <div class=\"art_authors\"><span class=\"NLM_string-name\">A Author</span>" +
      "    </div>\n" + 
      "    <span class=\"journalName\">journal Name</span>\n" + 
      "    <span class=\"year\">2017</span>\n" + 
      "    <span class=\"volume\">99</span>:<span class=\"issue\">9</span>,\n" + 
      "    <br>\n" + 
      "    <hr>\n" + 
      "  </div>";
  private static final String citContentHashFiltered = 
      " Download article citation data for:" + 
      " research in progress" + 
      " A Author" +
      " journal Name" + 
      " 2017" + 
      " 99 : 9 ," + 
      " ";
  
  
  protected ArchivalUnit createAu()
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
  }
  
  private Configuration thisAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "abc");
    conf.put("volume_name", "99");
    return conf;
  }
  
  private static void doFilterTest(ArchivalUnit au, FilterFactory fact,
      String nameToHash, String expectedStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.ENCODING_UTF_8);
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
  
  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();
    startMockDaemon();
    mau = createAu();
  }
  
  // Variant to test with Crawl Filter
  public static class TestCrawl extends TestFasebHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new FasebHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, manifestContent, manifestContent);
      doFilterTest(mau, variantFact, tocContent, tocContentCrawlFiltered);
      doFilterTest(mau, variantFact, art1Content, art1ContentCrawlFiltered);
      doFilterTest(mau, variantFact, citContent, citContent);
    }
  }
  
  // Variant to test with Hash Filter
   public static class TestHash extends TestFasebHtmlFilterFactory {
     public void testFiltering() throws Exception {
       variantFact = new FasebHtmlHashFilterFactory();
       doFilterTest(mau, variantFact, manifestContent, manifestHashFiltered);
       doFilterTest(mau, variantFact, tocContent, tocContentHashFiltered);
       doFilterTest(mau, variantFact, art1Content, art1ContentHashFiltered);
       doFilterTest(mau, variantFact, citContent, citContentHashFiltered);
     }
   }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

