/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon.hlthaff;

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

public class TestProjectHopeHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
  
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.hlthaff.HealthAffairsPlugin";
  
  
  private static final String manifestContent = 
      "<html>\n" + 
      "<head>\n" + 
      "    <title>Health Affairs 2017 LOCKSS Manifest Page</title>\n" + 
      "    <meta charset=\"UTF-8\" />\n" + 
      "</head>\n" + 
      "<body>\n" + 
      "<h1>Health Affairs 2017 LOCKSS Manifest Page</h1>\n" + 
      "<ul>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/12\">December 2017 (Vol. 36 Issue 12 Page 2039-2216)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/11\">November 2017 (Vol. 36 Issue 11 Page 1863-2031)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/10\">October 2017 (Vol. 36 Issue 10 Page 1695-1854)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/9\">September 2017 (Vol. 36 Issue 9 Page 1527-1688)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/8\">August 2017 (Vol. 36 Issue 8 Page 1359-1520)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/7\">July 2017 (Vol. 36 Issue 7 Page 1167-1349)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/6\">June 2017 (Vol. 36 Issue 6 Page 975-1160)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/5\">May 2017 (Vol. 36 Issue 5 Page 783-962)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/4\">April 2017 (Vol. 36 Issue 4 Page 591-776)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/3\">March 2017 (Vol. 36 Issue 3 Page 391-584)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/2\">February 2017 (Vol. 36 Issue 2 Page 199-384)</a></li>\n" + 
      "    <li><a href=\"/toc/hlthaff/36/1\">January 2017 (Vol. 36 Issue 1 Page 7-191)</a></li>\n" + 
      "</ul>\n" + 
      "<p>\n" + 
      "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>\n" + 
      "    LOCKSS system has permission to collect, preserve, and serve this Archival Unit.\n" + 
      "</p>\n" + 
      "</body>\n" + 
      "</html>";
  
  private static final String manifestHashFiltered = 
      " December 2017 (Vol. 36 Issue 12 Page 2039-2216)" + 
      " November 2017 (Vol. 36 Issue 11 Page 1863-2031)" + 
      " October 2017 (Vol. 36 Issue 10 Page 1695-1854)" + 
      " September 2017 (Vol. 36 Issue 9 Page 1527-1688)" + 
      " August 2017 (Vol. 36 Issue 8 Page 1359-1520)" + 
      " July 2017 (Vol. 36 Issue 7 Page 1167-1349)" + 
      " June 2017 (Vol. 36 Issue 6 Page 975-1160)" + 
      " May 2017 (Vol. 36 Issue 5 Page 783-962)" + 
      " April 2017 (Vol. 36 Issue 4 Page 591-776)" + 
      " March 2017 (Vol. 36 Issue 3 Page 391-584)" + 
      " February 2017 (Vol. 36 Issue 2 Page 199-384)" + 
      " January 2017 (Vol. 36 Issue 1 Page 7-191)" + 
      " ";
  
  private static final String tocContent = 
      "<html>\n" +
      "<head data-pb-dropzone=\"head\">\n" + 
      "  <meta charset=\"UTF-8\">\n" + 
      "  <title>Health Affairs | Vol 36, No 8</title>\n" + 
      "</head>\n" + 
      "<body class=\"pb-ui\">\n" + 
      "  <div id=\"pb-page-content\" data-ng-non-bindable=\"\">\n" + 
      "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "      <div class=\"header base fixed\" data-db-parent-of=\"sb1\">\n" + 
      "        <header>\n" + 
      "          <div class=\"popup login-popup hidden\">\n" + 
      "            <a href=\"#\" class=\"close\"><i class=\"icon-close_thin\"></i></a>\n" + 
      "            <div class=\"content\">\n" + 
      "              <h2>Login to your account</h2>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "          <div class=\"popup registration-popup hidden\">\n" + 
      "            <a href=\"#\" class=\"close\"><i class=\"icon-close_thin\"></i></a>\n" + 
      "            <div class=\"content\">\n" + 
      "              <h2>Create a new account</h2>\n" + 
      "              <form action=\"/action/registration\" class=\"registration-form\" method=\"post\">\n" + 
      "              </form>\n" + 
      "              <div class=\"center\">\n" + 
      "                <a href=\"/action/showLogin\" class=\"show-login\">\n" + 
      "                Returning user\n" + 
      "                </a>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "          <div data-widget-def=\"layout-widget\" data-widget-id=\"9876543-21\" class=\"container header--first-row\">\n" + 
      "            <div class=\"pull-left\">\n" + 
      "              <div class=\"header__logo\">\n" + 
      "                <a href=\"/\" title=\"logo\">\n" + 
      "                <img src=\"/pb-assets/images/logos/HA_logo_header_470x157px.png\" alt=\"Health Affairs\">\n" + 
      "                </a>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "            <div class=\"pull-right\">\n" + 
      "              <div class=\"header__quick-menu\">\n" + 
      "                <div class=\" col-sm-6\">\n" + 
      "                  <div class=\"institution\">\n" + 
      "                    <div class=\"institution-info-wrapper\"><span class=\"institution__intro\">brought to you by</span><span class=\"institution__name\">STANFORD UNIVERISTY MED CTR</span></div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <div class=\" col-sm-6\">\n" + 
      "                  <a title=\"Donate\" href=\"/support\" class=\"header__donate\">Donate</a>\n" + 
      "                </div>\n" + 
      "                <ul class=\"rlist--inline\">\n" + 
      "                  <li class=\"res--left\">\n" + 
      "                    <a title=\"Subscribe\" href=\"https://fulfillment.healthaffairs.org/\" class=\"quick-menu__item\"><span>Subscribe</span></a>\n" + 
      "                  </li>\n" + 
      "                  <li class=\"res--left\">\n" + 
      "                    <a title=\"For Authors\" href=\"/help-for-authors\" class=\"quick-menu__item\"><span>For Authors</span></a>\n" + 
      "                  </li>\n" + 
      "                </ul>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "          <div class=\"main-nav\">\n" + 
      "            <a href=\"#main-menu\" data-target=\"main-menu\" data-toggle=\"nav\" title=\"menu drawer\"><i aria-hidden=\"true\" class=\"icon-menu2\"></i><i aria-hidden=\"true\" class=\"icon-close_thin\"></i></a>\n" + 
      "            <nav role=\"navigation\" id=\"main-menu\" data-ctrl-res=\"screen-sm\" data-db-parent-of=\"extraMenu\" class=\"drawer__nav container hidden-sm hidden-xs\">\n" + 
      "              <a href=\"/\" class=\"menu-header hidden-md hidden-lg\">Home</a>\n" + 
      "            </nav>\n" + 
      "          </div>\n" + 
      "        </header>\n" + 
      "      </div>\n" + 
      "      <main class=\"content toc\" style=\"min-height: 355.117px;\">\n" + 
      "        <div class=\"container\">\n" + 
      "          <div class=\"row\">\n" + 
      "            <div class=\"toc-header clearfix col-xs-12\">\n" + 
      "              <div>\n" + 
      "                <div class=\" col-xs-10\">\n" + 
      "                  <div class=\"toc-header__top\">\n" + 
      "                    <h3 class=\"filled--journal no-top-margin\"> <a href=\"/journal/hlthaff\">Journal</a></h3>\n" + 
      "                    <div class=\"current-issue\"><span>Vol. 36, No. 8</span><span><a href=\"https://www.pubservice.com/backissue/subbi.aspx?CO=PJ&amp;PC=HR&amp;BI=HRI\">&nbsp;|&nbsp;Purchase Issue</a></span></div>\n" + 
      "                    <h1 class=\"toc-header__title\">August 2017 | Drug Approval &amp; More</h1>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <div class=\" col-xs-2\">\n" + 
      "                  <ul class=\"social-links rlist rlist--inline\">\n" + 
      "                    <li> <a href=\"https://twitter.com/Health_Affairs\" title=\"twitter\" class=\"social-links__item\"> <i class=\"icon-twitter\"></i> </a> </li>\n" + 
      "                  </ul>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <div class=\" col-sm-8\">\n" + 
      "                <!--totalCount3--><!--modified:1524396861000-->\n" + 
      "                <div class=\"table-of-content\">\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">From the Editor-In-Chief</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Drug Approval, And More</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"><a href=\"/author/Author%2C+A\" title=\"A Author\"><span>A Author</span></a></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1377/hlthaff.2017.9999&amp;url=http%3A%2F%2Fsfx.stanford.edu%2Flocal%3Fsid%3Dhope%26iuid%3D6220%26id%3Ddoi%3A10.1377%2Fhlthaff.2017.9999\" title=\"OpenURL STANFORD UNIVERISTY MED CTR\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/templates/jsp/images/sfxbutton.gif\" alt=\"OpenURL STANFORD UNIVERISTY MED CTR\"></a></li>\n" + 
      "                        <li>1359-1359</li>\n" + 
      "                      </ul>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">EntryPoint</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Propping Up Health Care Through Medicaid</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"><a href=\"/author/Author%2C+A\" title=\"A Author\"><span>A Author</span></a></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Abstract\" href=\"/doi/abs/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Abstract\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a href=\"/doi/references/10.1377/hlthaff.2017.9999\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "                        <li><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl\" title=\"OpenURL STANFORD UNIVERISTY MED CTR\" class=\"sfxLink\"></a></li>\n" + 
      "                        <li>1360-1364</li>\n" + 
      "                      </ul>\n" + 
      "                      <div class=\"accordion\">\n" + 
      "                        <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i>Preview Abstract</a>\n" + 
      "                        <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">Medicaid expansion in Arizona.</div>\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">Letters</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Innovations And Precise Terms</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"><a href=\"/author/Author%2C+A\" title=\"A Author\"><span>A Author</span></a></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl\" title=\"OpenURL STANFORD UNIVERISTY MED CTR\" class=\"sfxLink\"></a></li>\n" + 
      "                        <li>1520-1520</li>\n" + 
      "                      </ul>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <div class=\" col-sm-4\">\n" + 
      "                <div class=\"sidebar-region\">\n" + 
      "                  <h2 class=\"sidebar-region__title border\">Related Content</h2>\n" + 
      "                  <div class=\"sidebar-region__wrapper\">\n" + 
      "                    <a href=\"#\" title=\"Content Type\" aria-controls=\"tocContnetType\" class=\"sidebar-region__ctrl accordion__control js--open\">\n" + 
      "                      <h4 class=\"sidebar-region__group-title\">Content Type</h4>\n" + 
      "                      <i aria-hidden=\"true\" class=\"icon-section_arrow_d\"></i>\n" + 
      "                    </a>\n" + 
      "                    <div id=\"tocContentType\" class=\"sidebar-region__content accordion__content js--open\"></div>\n" + 
      "                    <ul class=\"rlist sidebar-region__list\">\n" + 
      "                      <li><a href=\"/blog\" title=\"Blog\"><span class=\"sidebar-region__label\">Blog</span></a></li>\n" + 
      "                    </ul>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </main>\n" + 
      "      <footer>\n" + 
      "        <div class=\"container\">\n" + 
      "          <div class=\"row\">\n" + 
      "            <div>\n" + 
      "              list--inline\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </footer>\n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "  <script type=\"text/javascript\" src=\"/products/hope/releasedAssets/js/main.bundle.js\"></script>\n" + 
      "  <script type=\"text/javascript\" src=\"/wro/product.js\"></script>\n" + 
      "  <div id=\"crossmark-widget\" style=\"display: none;\">\n" + 
      "    <div class=\"crossmark-reset crossmark-overlay\"></div>\n" + 
      "    <div class=\"crossmark-reset crossmark-popup\">\n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "  <ul class=\"ui-autocomplete ui-front ui-menu ui-widget ui-widget-content ui-corner-all quickSearchAutocomplete\" id=\"ui-id-1\" tabindex=\"0\" style=\"display: none;\"></ul>\n" + 
      "</body>\n" +
      "</html>";
  
  private static final String tocContentCrawlFiltered = 
      "<html>\n" +
      "<head data-pb-dropzone=\"head\">\n" + 
      "  <meta charset=\"UTF-8\">\n" + 
      "  <title>Health Affairs | Vol 36, No 8</title>\n" + 
      "</head>\n" + 
      "<body class=\"pb-ui\">\n" + 
      "  <div id=\"pb-page-content\" data-ng-non-bindable=\"\">\n" + 
      "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "      <div class=\"header base fixed\" data-db-parent-of=\"sb1\">\n" + 
      "        \n" + 
      "      </div>\n" + 
      "      <main class=\"content toc\" style=\"min-height: 355.117px;\">\n" + 
      "        <div class=\"container\">\n" + 
      "          <div class=\"row\">\n" + 
      "            <div class=\"toc-header clearfix col-xs-12\">\n" + 
      "              <div>\n" + 
      "                <div class=\" col-xs-10\">\n" + 
      "                  \n" + 
      "                </div>\n" + 
      "                <div class=\" col-xs-2\">\n" + 
      "                  <ul class=\"social-links rlist rlist--inline\">\n" + 
      "                    <li> <a href=\"https://twitter.com/Health_Affairs\" title=\"twitter\" class=\"social-links__item\"> <i class=\"icon-twitter\"></i> </a> </li>\n" + 
      "                  </ul>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <div class=\" col-sm-8\">\n" + 
      "                <!--totalCount3--><!--modified:1524396861000-->\n" + 
      "                <div class=\"table-of-content\">\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">From the Editor-In-Chief</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Drug Approval, And More</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li></li>\n" + 
      "                        <li>1359-1359</li>\n" + 
      "                      </ul>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">EntryPoint</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Propping Up Health Care Through Medicaid</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Abstract\" href=\"/doi/abs/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Abstract\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a href=\"/doi/references/10.1377/hlthaff.2017.9999\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "                        <li></li>\n" + 
      "                        <li>1360-1364</li>\n" + 
      "                      </ul>\n" + 
      "                      <div class=\"accordion\">\n" + 
      "                        <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i>Preview Abstract</a>\n" + 
      "                        \n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">Letters</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Innovations And Precise Terms</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li></li>\n" + 
      "                        <li>1520-1520</li>\n" + 
      "                      </ul>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <div class=\" col-sm-4\">\n" + 
      "                \n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </main>\n" + 
      "      \n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "  <script type=\"text/javascript\" src=\"/products/hope/releasedAssets/js/main.bundle.js\"></script>\n" + 
      "  <script type=\"text/javascript\" src=\"/wro/product.js\"></script>\n" + 
      "  <div id=\"crossmark-widget\" style=\"display: none;\">\n" + 
      "    <div class=\"crossmark-reset crossmark-overlay\"></div>\n" + 
      "    <div class=\"crossmark-reset crossmark-popup\">\n" + 
      "    </div>\n" + 
      "  </div>\n" + 
      "  <ul class=\"ui-autocomplete ui-front ui-menu ui-widget ui-widget-content ui-corner-all quickSearchAutocomplete\" id=\"ui-id-1\" tabindex=\"0\" style=\"display: none;\"></ul>\n" + 
      "</body>\n" + 
      "</html>";
  
  private static final String tocContentFiltered = 
      "<div class=\"table-of-content\">\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">From the Editor-In-Chief</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Drug Approval, And More</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"><a href=\"/author/Author%2C+A\" title=\"A Author\"><span>A Author</span></a></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li></li>\n" + 
      "                        <li>1359-1359</li>\n" + 
      "                      </ul>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">EntryPoint</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Propping Up Health Care Through Medicaid</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"><a href=\"/author/Author%2C+A\" title=\"A Author\"><span>A Author</span></a></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Abstract\" href=\"/doi/abs/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Abstract\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a href=\"/doi/references/10.1377/hlthaff.2017.9999\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "                        <li></li>\n" + 
      "                        <li>1360-1364</li>\n" + 
      "                      </ul>\n" + 
      "                      <div class=\"accordion\">\n" + 
      "                        <a href=\"#\" title=\"Preview Abstract\" class=\"accordion__control\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i>Preview Abstract</a>\n" + 
      "                        <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">Medicaid expansion in Arizona.</div>\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"issue-item\">\n" + 
      "                    <span class=\"filled--journal default\">Letters</span><span class=\"toc-label text-uppercase\"></span>\n" + 
      "                    <h2 class=\"issue-item__title\"><a href=\"/doi/full/10.1377/hlthaff.2017.9999\">Innovations And Precise Terms</a></h2>\n" + 
      "                    <div class=\"rlist--inline loa\"> <span area-label=\"author\"><a href=\"/author/Author%2C+A\" title=\"A Author\"><span>A Author</span></a></span>  </div>\n" + 
      "                    <div class=\"toc-item__detail\"></div>\n" + 
      "                    <span class=\"toc-label\" style=\"margin:0;\">Free Access</span>\n" + 
      "                    <div class=\"toc-item__footer\">\n" + 
      "                      <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "                        <li><a title=\"Full text\" href=\"/doi/full/10.1377/hlthaff.2017.9999\">\n" + 
      "                          Full text\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li><a title=\"PDF\" href=\"/doi/pdf/10.1377/hlthaff.2017.9999\">\n" + 
      "                          PDF\n" + 
      "                          </a>\n" + 
      "                        </li>\n" + 
      "                        <li></li>\n" + 
      "                        <li>1520-1520</li>\n" + 
      "                      </ul>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>";
  
  private static final String tocContentHashFiltered =
      " " + 
      "" + 
      "From the Editor-In-Chief " + 
      "Drug Approval, And More " + 
      "A Author " + 
      "" + 
      "Free Access " + 
      "" + 
      "" + 
      "" + 
      "Full text " + 
      "" + 
      "" + 
      "" + 
      "PDF " + 
      "" + 
      "" + 
      "" + 
      "1359-1359 " + 
      "" + 
      "" + 
      "" + 
      "" + 
      "EntryPoint " + 
      "Propping Up Health Care Through Medicaid " + 
      "A Author " + 
      "" + 
      "Free Access " + 
      "" + 
      "" + 
      "" + 
      "Abstract " + 
      "" + 
      "" + 
      "" + 
      "Full text " + 
      "" + 
      "" + 
      "" + 
      "PDF " + 
      "" + 
      "" + 
      "References " + 
      "" + 
      "1360-1364 " + 
      "" + 
      "" + 
      "Preview Abstract " + 
      "Medicaid expansion in Arizona. " + 
      "" + 
      "" + 
      "" + 
      "" + 
      "Letters " + 
      "Innovations And Precise Terms " + 
      "A Author " + 
      "" + 
      "Free Access " + 
      "" + 
      "" + 
      "" + 
      "Full text " + 
      "" + 
      "" + 
      "" + 
      "PDF " + 
      "" + 
      "" + 
      "" + 
      "1520-1520 " + 
      "" + 
      "" + 
      "" + 
      "";
  
  private static final String art1Content = 
      "<html class=\"pb-page\" data-request-id=\"9876543-21\" lang=\"en\">\n" + 
      "  <head data-pb-dropzone=\"head\">\n" + 
      "    <meta name=\"pbContext\" content=\";website:website:hope-site;page:string:Article/Chapter View;journal:journal:hlthaff;article:article:10.1377/hlthaff.2018.test1;issue:issue:10.1377/hlthaff.2017.36.issue-11;wgroup:string:Publication Websites;pageGroup:string:Publication Pages;subPage:string:Full Text\">\n" + 
      "    <meta data-pb-head=\"head-widgets-start\">\n" + 
      "    <link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\">\n" + 
      "    <meta name=\"dc.Language\" content=\"en\">\n" + 
      "    <meta name=\"dc.Coverage\" content=\"world\">\n" + 
      "    <link rel=\"meta\" type=\"application/atom+xml\" href=\"https://doi.org/10.1377%2Fhlthaff.2018.test1\">\n" + 
      "    <link rel=\"meta\" type=\"application/rdf+json\" href=\"https://doi.org/10.1377%2Fhlthaff.2018.test1\">\n" + 
      "    <link rel=\"meta\" type=\"application/unixref+xml\" href=\"https://doi.org/10.1377%2Fhlthaff.2018.test1\">\n" + 
      "    <meta charset=\"UTF-8\">\n" + 
      "    <meta name=\"robots\" content=\"noarchive\">\n" + 
      "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1, user-scalable=0\">\n" + 
      "    <title>Making The Most Of Microfinance Networks | Health Affairs</title>\n" + 
      "    <meta data-pb-head=\"head-widgets-end\">\n" + 
      "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" + 
      "  </head>\n" + 
      "  <body class=\"pb-ui\">\n" + 
      "    <div id=\"pb-page-content\" data-ng-non-bindable=\"\">\n" + 
      "      <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "        <div class=\"header base fixed\" data-db-parent-of=\"sb1\">\n" + 
      "          <header>\n" + 
      "            <div class=\"popup login-popup hidden\">\n" + 
      "            </div>\n" + 
      "            <div class=\"main-nav\">\n" + 
      "              <a href=\"#main-menu\" data-target=\"main-menu\" data-toggle=\"nav\" title=\"menu drawer\"><i aria-hidden=\"true\" class=\"icon-menu2\"></i><i aria-hidden=\"true\" class=\"icon-close_thin\"></i></a>\n" + 
      "              <nav role=\"navigation\" id=\"main-menu\" data-ctrl-res=\"screen-sm\" data-db-parent-of=\"extraMenu\" class=\"drawer__nav container hidden-sm hidden-xs\">\n" + 
      "                <a href=\"/\" class=\"menu-header hidden-md hidden-lg\">Home</a>\n" + 
      "                <div class=\"main-nav--desktop main-nav--main\">\n" + 
      "                  <ul id=\"menubar\" role=\"menubar\" class=\"menubar rlist--inline\">\n" + 
      "                    <li role=\"menuitem\" aria-label=\"Topics\" id=\"menu-item-main-menu-0\" class=\"menu-item\"><a href=\"/topics\" title=\"Topics\"><span>Topics</span></a></li>\n" + 
      "                    <li role=\"menuitem\" aria-haspopup=\"true\" aria-label=\"Journal\" id=\"menu-item-main-menu-1\" class=\"dropdown menu-parent\">\n" + 
      "                      <a href=\"/journal/hlthaff\" title=\"Journal\" id=\"main-menu-1\" class=\"dropdown__toggle\"><span>Journal</span></a>\n" + 
      "                      <ul aria-labelledby=\"main-menu-1\" aria-hidden=\"true\" role=\"menu\" class=\"rlist dropdown__menu\">\n" + 
      "                        <li role=\"menuitem\" tabindex=\"-1\" aria-label=\"Ahead of Print\" class=\"menu-item\"><a href=\"/toc/hlthaff/0/0\" title=\"Ahead of Print\"><span>Ahead of Print</span></a></li>\n" + 
      "                        <li role=\"menuitem\" tabindex=\"-1\" aria-label=\"Current Issue\" class=\"menu-item\"><a href=\"/toc/hlthaff/current\" title=\"Current Issue\"><span>Current Issue</span></a></li>\n" + 
      "                        <li role=\"menuitem\" tabindex=\"-1\" aria-label=\"Archive\" class=\"menu-item\"><a href=\"/journal/hlthaff?year=2018\" title=\"Archive\"><span>Archive</span></a></li>\n" + 
      "                        <li role=\"menuitem\" aria-haspopup=\"true\" tabindex=\"-1\" aria-label=\"Article Series\" class=\"dropdown menu-parent\">\n" + 
      "                          <a href=\"/article-series\" title=\"Article Series\" id=\"main-menu-2\" class=\"dropdown__toggle\"><span>Article Series</span></a>\n" + 
      "                          <ul aria-labelledby=\"main-menu-2\" aria-hidden=\"true\" role=\"menu\" class=\"rlist dropdown__menu level-2\">\n" + 
      "                            <li role=\"menuitem\" tabindex=\"-1\" aria-label=\"Aging &amp; Health\" class=\"menu-item\"><a href=\"/topic/pt_aginghealth\" title=\"Aging &amp; Health\"><span>Aging &amp; Health</span></a></li>\n" + 
      "                            <li role=\"menuitem\" tabindex=\"-1\" aria-label=\"Considering Health Spending\" class=\"menu-item\"><a href=\"/topic/pt_bms120\" title=\"Considering Health Spending\"><span>Considering Health Spending</span></a></li>\n" + 
      "                          </ul>\n" + 
      "                        </li>\n" + 
      "                      </ul>\n" + 
      "                    </li>\n" + 
      "                  </ul>\n" + 
      "                </div>\n" + 
      "                <div class=\"main-nav--extra\">\n" + 
      "                  <ul class=\"rlist--inline\">\n" + 
      "                    <li class=\"quickSearchFormContainer\">\n" + 
      "                      <div class=\"quick-search\">\n" + 
      "                        <form action=\"/action/doSearch\" name=\"defaultQuickSearch\" method=\"get\" title=\"Quick Search\">\n" + 
      "                          <div class=\"input-group option-0\"><input name=\"AllField\" placeholder=\"\" data-auto-complete-max-words=\"7\" data-auto-complete-max-chars=\"32\" data-contributors-conf=\"3\" data-topics-conf=\"3\" data-publication-titles-conf=\"3\" data-history-items-conf=\"3\" value=\"\" class=\"autocomplete ui-autocomplete-input\" autocomplete=\"off\" type=\"search\"><span role=\"status\" aria-live=\"polite\" class=\"ui-helper-hidden-accessible\"></span></div>\n" + 
      "                          <button type=\"submit\" title=\"Search\" class=\"btn quick-search__button icon-search\"><span>Search</span></button><a href=\"/search/advanced\" title=\"Advanced Search\" class=\"advanced-search-link\">ADVANCED SEARCH</a>\n" + 
      "                        </form>\n" + 
      "                      </div>\n" + 
      "                    </li>\n" + 
      "                  </ul>\n" + 
      "                </div>\n" + 
      "              </nav>\n" + 
      "            </div>\n" + 
      "          </header>\n" + 
      "        </div>\n" + 
      "        <main class=\"content main-article\" style=\"min-height: 355.117px; padding-top: 157.7px;\">\n" + 
      "          <div class=\"container\">\n" + 
      "            <div class=\"row\">\n" + 
      "              <div>\n" + 
      "                <div class=\"container\"></div>\n" + 
      "                <div>\n" + 
      "                  <article data-figures=\"https://www.healthaffairs.org/action/ajaxShowFigures?doi=10.1377%2Fhlthaff.2018.test1&amp;ajax=true\" data-references=\"https://www.healthaffairs.org/action/ajaxShowEnhancedAbstract?doi=10.1377%2Fhlthaff.2018.test1&amp;ajax=true\" class=\"container\">\n" + 
      "                    <div class=\"row\">\n" + 
      "                      <div class=\"col-sm-8 col-md-8 article__content\">\n" + 
      "                        <!--+articleHEeader()--><!--+articleCitation()-->\n" + 
      "                        <div class=\"citation\">\n" + 
      "                          <div class=\"citation__top\">\n" + 
      "                            <h3 class=\"filled--journal secondary-font no-top-margin\">People &amp; Places</h3>\n" + 
      "                            <span class=\"article__breadcrumbs\">\n" + 
      "                              <div class=\"article__breadcrumbs\">\n" + 
      "                                <nav class=\"article__tocHeading\"><a href=\"/journal/hlthaff\">Health Affairs</a><a href=\"/toc/hlthaff/36/11\">Vol. 36, No. 11</a>: Global Health Policy</nav>\n" + 
      "                              </div>\n" + 
      "                            </span>\n" + 
      "                            <span class=\"article__seriesTitle\">PEOPLE &amp; PLACES</span>\n" + 
      "                          </div>\n" + 
      "                          <h1 class=\"citation__title\">Making The Most Of Microfinance Networks</h1>\n" + 
      "                          <div class=\"issue-item\">\n" + 
      "                            <ul class=\"rlist--inline loa mobile-authors\" title=\"list of authors\">\n" + 
      "                              <li><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Author\" title=\"Author\">Author</a><sup>1</sup></span></li>\n" + 
      "                            </ul>\n" + 
      "                          </div>\n" + 
      "                          <div class=\"article__affiliations\">\n" + 
      "                            <div class=\"affiliations accordion\">\n" + 
      "                              <a href=\"#\" title=\"Open affiliations\" aria-controls=\"articleAffiliations\" data-slide-target=\"#articleAffiliations\" class=\"affiliations__ctrl accordion__control w-slide__btn\" aria-expanded=\"false\"><span class=\"affiliations__label\"> Affiliations</span><i aria-hidden=\"true\" class=\"icon-section_arrow_d\"></i></a>\n" + 
      "                              <div id=\"articleAffiliations\" class=\"div affiliations__content accordion__content\" style=\"display: none;\">\n" + 
      "                                <!--?xml version=\"1.0\" encoding=\"UTF-8\"?-->\n" + 
      "                                <section class=\"section\">\n" + 
      "                                  <div class=\"section__body\">\n" + 
      "                                    <ol class=\"rlist spaced\">\n" + 
      "                                      <li id=\"BIO1\">1. Author is an author at Health Affairs, in Bethesda, Maryland.</li>\n" + 
      "                                    </ol>\n" + 
      "                                  </div>\n" + 
      "                                </section>\n" + 
      "                              </div>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                        <div class=\"epub-section clearfix\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">PUBLISHED:</span><span class=\"epub-section__date\">November 2017</span></span><span class=\"epub-section__item epub-section__access\"><i aria-hidden=\"true\" class=\"icon-lock_open\"></i><span class=\"epub-section__text\">Full Access</span></span><span class=\"epub-section__item pull-right\"><a href=\"https://doi.org/10.1377/hlthaff.2018.test1\" class=\"epub-section__doi__text\">https://doi.org/10.1377/hlthaff.2018.test1</a></span></div>\n" + 
      "                        <!--+articleCoolbar()-->\n" + 
      "                        <div class=\"scroll-to-target\">\n" + 
      "                          <nav class=\"stickybar coolBar trans\">\n" + 
      "                            <div class=\"stickybar__wrapper coolBar__wrapper clearfix\" style=\"width: 756.667px; top: 157.7px;\">\n" + 
      "                              <div class=\"rlist coolBar__zone\">\n" + 
      "                                <!--div.coolBar__section//| pb.renderDropzone(thisWidget, 'coolbarDropZone1')--><a href=\"#\" data-db-target-for=\"article\" data-db-switch=\"icon-close_thin\" data-slide-target=\"#articleMenu\" class=\"coolBar__ctrl hidden-md hidden-lg w-slide__btn\"><i aria-hidden=\"true\" class=\"icon-Icon_About-Article\"></i><span>About</span></a>\n" + 
      "                                <div data-db-target-of=\"article\" id=\"articleMenu\" class=\"coolBar__drop fixed rlist\">\n" + 
      "                                  <div data-target=\"article .tab .tab__nav, .coolBar--download .coolBar__drop\" data-remove=\"false\" data-target-class=\"hidden-xs hidden-sm\" data-toggle=\"transplant\" data-direction=\"from\" data-transplant=\"self\" class=\"transplant showit\">\n" + 
      "                                    <div class=\"transplanted-clone\">\n" + 
      "                                      <ul data-mobile-toggle=\"slide\" class=\"rlist tab__nav w-slide--list\">\n" + 
      "                                        <li role=\"presentation\" class=\"active\"><a href=\"#pane-pcw-details\" aria-controls=\"#pane-pcw-details\" role=\"tab\" data-toggle=\"tab\" title=\"details\" id=\"pane-pcw-detailscon\" data-slide-target=\"#pane-pcw-details\" class=\"details-tab\" aria-expanded=\"true\"><span>Details</span></a></li>\n" + 
      "                                        <li role=\"presentation\"><a href=\"#pane-pcw-figures\" aria-controls=\"#pane-pcw-figures\" role=\"tab\" data-toggle=\"tab\" title=\"figures\" id=\"pane-pcw-figurescon\" data-slide-target=\"#pane-pcw-figures\" class=\"figures-tab empty\"><span>Exhibits</span></a></li>\n" + 
      "                                        <li role=\"presentation\"><a href=\"#pane-pcw-references\" aria-controls=\"#pane-pcw-references\" role=\"tab\" data-toggle=\"tab\" title=\"references\" id=\"pane-pcw-referencescon\" data-slide-target=\"#pane-pcw-references\" class=\"references-tab empty\"><span>References</span></a></li>\n" + 
      "                                        <li role=\"presentation\"><a href=\"#pane-pcw-related\" aria-controls=\"#pane-pcw-related\" role=\"tab\" data-toggle=\"tab\" title=\"related\" id=\"pane-pcw-relatedcon\" data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><span>Related</span></a></li>\n" + 
      "                                      </ul>\n" + 
      "                                      <ul data-db-target-of=\"downloads\" class=\"coolBar__drop rlist w-slide--list\">\n" + 
      "                                        <li><a href=\"/doi/pdf/10.1377/hlthaff.2018.test1\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View PDF</span></a></li>\n" + 
      "                                      </ul>\n" + 
      "                                    </div>\n" + 
      "                                  </div>\n" + 
      "                                </div>\n" + 
      "                              </div>\n" + 
      "                              <ul data-cb-group=\"Article\" data-cb-group-icon=\"icon-toc\" class=\"rlist coolBar__first\">\n" + 
      "                                <li class=\"coolBar__section coolBar--download hidden-xs hidden-sm\">\n" + 
      "                                  <a href=\"#\" data-db-target-for=\"downloads\" data-db-switch=\"icon-close_thin\" class=\"coolBar__ctrl\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View Article</span></a>\n" + 
      "                                  <ul data-db-target-of=\"downloads\" class=\"coolBar__drop rlist w-slide--list cloned hidden-xs hidden-sm\">\n" + 
      "                                    <li><a href=\"/doi/pdf/10.1377/hlthaff.2018.test1\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View PDF</span></a></li>\n" + 
      "                                  </ul>\n" + 
      "                                </li>\n" + 
      "                                <li class=\"coolBar__section coolBar--permissions\"><a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D1937%26pageCount%3D1\" title=\"Permissions\" class=\"coolBar__ctrl\"><i aria-hidden=\"true\" class=\"icon-lock_open\"></i><span>Permissions</span></a></li>\n" + 
      "                              </ul>\n" + 
      "                              <ul class=\"coolBar__second rlist\">\n" + 
      "                                <div class=\" col-xs-6 gutterless \">\n" + 
      "                                  <!-- Go to www.addthis.com/dashboard to customize your tools --><script type=\"text/javascript\" src=\"//s7.addthis.com/js/300/addthis_widget.js#pubid=xa-4faab26f2cff13a7\"></script><a href=\"#\" data-db-target-for=\"9876543-21\" data-db-switch=\"icon-close_thin\" data-slide-target=\"#3884840d-fcc8-4e71-a609-7ede38eea79d_Pop\" class=\"share__ctrl w-slide__btn\"><i aria-hidden=\"true\" class=\"icon-Icon_Share\"></i><span>Share</span></a>\n" + 
      "                                  <div data-db-target-of=\"9876543-21\" id=\"9876543-21_Pop\" class=\"share__block dropBlock__holder fixed\">\n" + 
      "                                    <div class=\"pb-dropzone\" data-pb-dropzone=\"shareBlock\"></div>\n" + 
      "                                    <ul class=\"rlist w-slide--list addthis addthis_toolbox addthis_default_style addthis_32x32_style\">\n" + 
      "                                      <li>\n" + 
      "                                        <a class=\"addthis_button_facebook at300b\" title=\"Facebook\" href=\"#\">\n" + 
      "                                          <span class=\"at-icon-wrapper\" style=\"background-color: rgb(59, 89, 152); line-height: 32px; height: 32px; width: 32px;\">\n" + 
      "                                            <svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 32 32\" version=\"1.1\" role=\"img\" aria-labelledby=\"at-svg-facebook-1\" class=\"at-icon at-icon-facebook\" style=\"width: 32px; height: 32px;\" title=\"Facebook\" alt=\"Facebook\">\n" + 
      "                                              <title id=\"at-svg-facebook-1\">Facebook</title>\n" + 
      "                                              <g>\n" + 
      "                                                <path d=\"M22 5.16c-.406-.054-1.806-.16-3.43-.16-3.4 0-5.733 1.825-5.733 5.17v2.882H9v3.913h3.837V27h4.604V16.965h3.823l.587-3.913h-4.41v-2.5c0-1.123.347-1.903 2.198-1.903H22V5.16z\" fill-rule=\"evenodd\"></path>\n" + 
      "                                              </g>\n" + 
      "                                            </svg>\n" + 
      "                                          </span>\n" + 
      "                                          Facebook\n" + 
      "                                        </a>\n" + 
      "                                      </li>\n" + 
      "                                      <div class=\"atclear\"></div>\n" + 
      "                                    </ul>\n" + 
      "                                  </div>\n" + 
      "                                </div>\n" + 
      "                                <div class=\" col-xs-6 gutterless \">\n" + 
      "                                  <a href=\"#\" data-db-target-for=\"9876543-21\" data-db-switch=\"icon-close_thin\" aria-haspopup=\"true\" aria-controls=\"9876543-21_Pop\" role=\"button\" id=\"9876543-21_Ctrl\" class=\"article-tools__ctrl w-slide__btn\"><i aria-hidden=\"true\" class=\"icon-Icon_Tools\"></i><span>Tools</span></a>\n" + 
      "                                  <div data-db-target-of=\"9876543-21\" aria-labelledby=\"9876543-21_Ctrl\" role=\"menu\" id=\"9876543-21_Pop\" class=\"article-tools__block fixed dropBlock__holder\">\n" + 
      "                                    <ul class=\"rlist w-slide--list\">\n" + 
      "                                      <li role=\"none\" class=\"article-tool\"><a href=\"/personalize/addFavoritePublication?doi=10.1377%2Fhlthaff.2018.test1\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-Icon_Star-26\"></i><span>Add to favorites</span></a></li>\n" + 
      "                                      <li role=\"none\" class=\"article-tool\"><a href=\"/action/showCitFormats?doi=10.1377%2Fhlthaff.2018.test1\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-Icon_Download\"></i><span>Download Citations</span></a></li>\n" + 
      "                                      <li role=\"none\" class=\"article-tool\"><a href=\"/action/addCitationAlert?doi=10.1377%2Fhlthaff.2018.test1\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-Icon_Track-citations\"></i><span>Track Citations</span></a></li>\n" + 
      "                                      <li role=\"none\" class=\"article-tool\"><a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D1937\" role=\"menuitem\" target=\"blank\"><i aria-hidden=\"true\" class=\"icon-lock_open\"></i><span>Permissions</span></a></li>\n" + 
      "                                    </ul>\n" + 
      "                                  </div>\n" + 
      "                                </div>\n" + 
      "                              </ul>\n" + 
      "                            </div>\n" + 
      "                          </nav>\n" + 
      "                        </div>\n" + 
      "                        <div class=\"article__body \">\n" + 
      "                          <p class=\"fulltext\"></p>\n" + 
      "                          <!--abstract content-->\n" + 
      "                          <div class=\"hlFld-Abstract\">\n" + 
      "                            <p class=\"fulltext\"></p>\n" + 
      "                            <h2 class=\"article-section__title section__title\" id=\"9876543-21\">Abstract</h2>\n" + 
      "                            <div class=\"abstractSection abstractInFull\">\n" + 
      "                              <p xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\">Together, data scientists and microfinance institutions are bringing.</p>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                          <!--/abstract content-->\n" + 
      "                          <div class=\"article__topic\">\n" + 
      "                            <strong class=\"article__topic-title\">TOPICS</strong>\n" + 
      "                            <div class=\"article__topic-body\">\n" + 
      "                              <ul class=\"rlist rlist--inline\">\n" + 
      "                                <li><a href=\"/topic/69\" class=\"badge-type\">Global health</a></li>\n" + 
      "                                <li><a href=\"/topic/1602\" class=\"badge-type\">Community health</a></li>\n" + 
      "                                <li><a href=\"/topic/154\" class=\"badge-type\">Childrens health</a></li>\n" + 
      "                              </ul>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                          <!--fulltext content-->\n" + 
      "                          <div class=\"hlFld-Fulltext\">\n" + 
      "                            <p xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\">Microfinance institutions (MFIs) are known.</p>\n" + 
      "                          </div>\n" + 
      "                          <!--/fulltext content--><script type=\"text/javascript\">\n" + 
      "                            window.figureViewer={doi:'10.1377/hlthaff.2018.test1',path:'/na101/home/literatum/publisher/hope/journals/content/hlthaff/2017/hlthaff.2017.36.issue-11/hlthaff.2018.test1/20171026',figures:[{i:'l1',type:'fig',g:[{m:'figure1.gif',l:'figure1.jpeg',size:'540 KB'}]}\n" + 
      "                            ]}\n" + 
      "                          </script>\n" + 
      "                          <div class=\"response\">\n" + 
      "                            <div class=\"sub-article-title\"></div>\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                      </div>\n" + 
      "                      <div class=\"col-sm-4 hidden-xs hidden-sm sticko__parent\">\n" + 
      "                        <ul class=\"social-links rlist rlist--inline\">\n" + 
      "                          <li> <a href=\"https://www.youtube.com/user/HealthAffairsJournal\" title=\"youtube\" class=\"social-links__item\"> <i class=\"icon-youtube\"></i> </a> </li>\n" + 
      "                          <li> <a href=\"https://twitter.com/Health_Affairs\" title=\"twitter\" class=\"social-links__item\"> <i class=\"icon-twitter\"></i> </a> </li>\n" + 
      "                        </ul>\n" + 
      "                        <!--+articleTab()-->\n" + 
      "                        <div class=\"tab tab--slide tab--flex sticko__md tabs--xs dynamic-sticko tab--flex tabs--xs js--sticko\" style=\"width: 363.317px; top: 167.7px; position: static; height: 534.1px;\">\n" + 
      "                          <ul data-mobile-toggle=\"slide\" class=\"rlist tab__nav w-slide--list cloned hidden-xs hidden-sm\">\n" + 
      "                            <li role=\"presentation\" class=\"active\"><a href=\"#pane-pcw-details\" aria-controls=\"#pane-pcw-details\" role=\"tab\" data-toggle=\"tab\" title=\"details\" id=\"pane-pcw-detailscon\" data-slide-target=\"#pane-pcw-details\" class=\"details-tab\" aria-expanded=\"true\"><span>Details</span></a></li>\n" + 
      "                            <li role=\"presentation\"><a href=\"#pane-pcw-figures\" aria-controls=\"#pane-pcw-figures\" role=\"tab\" data-toggle=\"tab\" title=\"figures\" id=\"pane-pcw-figurescon\" data-slide-target=\"#pane-pcw-figures\" class=\"figures-tab empty\"><span>Exhibits</span></a></li>\n" + 
      "                            <li role=\"presentation\"><a href=\"#pane-pcw-references\" aria-controls=\"#pane-pcw-references\" role=\"tab\" data-toggle=\"tab\" title=\"references\" id=\"pane-pcw-referencescon\" data-slide-target=\"#pane-pcw-references\" class=\"references-tab empty\"><span>References</span></a></li>\n" + 
      "                            <li role=\"presentation\"><a href=\"#pane-pcw-related\" aria-controls=\"#pane-pcw-related\" role=\"tab\" data-toggle=\"tab\" title=\"related\" id=\"pane-pcw-relatedcon\" data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><span>Related</span></a></li>\n" + 
      "                          </ul>\n" + 
      "                          <ul class=\"rlist tab__content sticko__child\" style=\"height: 534.1px; overflow-y: auto;\">\n" + 
      "                            <li id=\"pane-pcw-details\" aria-labelledby=\"pane-pcw-detailscon\" role=\"tabpanel\" class=\"tab__pane active\">\n" + 
      "                              <div class=\"details-tab\">\n" + 
      "                                <section class=\"article__metrics\">\n" + 
      "                                  <div class=\"section__body\">\n" + 
      "                                    <p>\n" + 
      "                                    </p>\n" + 
      "                                    <div>\n" + 
      "                                      <h2>Article Metrics</h2>\n" + 
      "                                    </div>\n" + 
      "                                    <!-- script to add on the page to get the Altmetrics html call to work -->\n" + 
      "                                    <script type=\"text/javascript\" src=\"https://d1bxh8uas1mnw7.cloudfront.net/assets/embed.js\"></script>\n" + 
      "                                    <div data-badge-popover=\"right\" data-badge-type=\"donut\" data-doi=\"10.1377/hlthaff.2018.test1\" class=\"altmetric-embed\" data-uuid=\"9876543-21\"><a target=\"_self\" href=\"https://www.altmetric.com/details.php?domain=www.healthaffairs.org&amp;citation_id=29876543\" rel=\"popover\" data-content=\"<div>    <div style='padding-left: 10px; line-height:18px; border-left: 16px solid #74CFED;'>\n" + 
      "                                      <a class='link-to-altmetric-details-tab' target='_self' href='https://www.altmetric.com/details.php?domain=www.healthaffairs.org&amp;citation_id=29876543&amp;tab=twitter'>\n" + 
      "                                      Tweeted by <b>3</b>\n" + 
      "                                      </a>\n" + 
      "                                      </div>\n" + 
      "                                      <div class='altmetric-embed readers' style='margin-top: 10px;'>\n" + 
      "                                      <div class='altmetric-embed tip_mendeley'\n" + 
      "                                      style='padding-left: 10px; line-height:18px; border-left: 16px solid #A60000;'>\n" + 
      "                                      <b>1</b> readers on Mendeley            \n" + 
      "                                      </div>\n" + 
      "                                      </div>\n" + 
      "                                      <div style='margin-top: 10px; text-align: center;'>\n" + 
      "                                      <a class='altmetric_details' target='_self' href='https://www.altmetric.com/details.php?domain=www.healthaffairs.org&amp;citation_id=29876543'>\n" + 
      "                                      See more details\n" + 
      "                                      </a>\n" + 
      "                                      |\n" + 
      "                                      <a href='javascript:void(0)' class='close-popover'>\n" + 
      "                                      Close this\n" + 
      "                                      </a>\n" + 
      "                                      </div>\n" + 
      "                                      <a class='altmetric_embed close-popover'\n" + 
      "                                      style='display: block; position: absolute; top: 10px; right: 15px; font-size: 1.2em; font-weight: bold; text-decoration: none; color: black; padding-bottom: 2em; padding-left: 2em;'\n" + 
      "                                      href='javascript:void(0)'>\n" + 
      "                                      ×\n" + 
      "                                      </a>\n" + 
      "                                      </div>\" style=\"display:inline-block;\" data-badge-popover=\"right\">\n" + 
      "                                      <img alt=\"Article has an altmetric score of 2\" src=\"https://badges.altmetric.com/?size=128&amp;score=2&amp;types=tttttttt\" style=\"border:0; margin:0; max-width: none;\" width=\"64px\" height=\"64px\">\n" + 
      "                                      </a>\n" + 
      "                                    </div>\n" + 
      "                                    <p></p>\n" + 
      "                                  </div>\n" + 
      "                                </section>\n" + 
      "                                <hr class=\"section__separator\">\n" +
      "                                <hr class=\"section__separator\">\n" + 
      "                              </div>\n" + 
      "                              <section class=\"section\">\n" + 
      "                                <strong class=\"section__title\">Information</strong>\n" + 
      "                                <div class=\"section__body\">\n" + 
      "                                  <p>(c) 2017 Project HOPE-The People-to-People Health Foundation, Inc.</p>\n" + 
      "                                </div>\n" + 
      "                              </section>\n" + 
      "                              <hr class=\"section__separator\">\n" + 
      "                              <ul class=\"rlist rlist--inline\"></ul>\n" + 
      "                            </li>\n" + 
      "                            <li id=\"pane-pcw-figures\" aria-labelledby=\"pane-pcw-figurescon\" role=\"tabpanel\" class=\"tab__pane empty\">\n" + 
      "                              <div class=\"NoContentMessage\">None</div>\n" + 
      "                            </li>\n" + 
      "                            <li id=\"pane-pcw-references\" aria-labelledby=\"pane-pcw-referencescon\" role=\"tabpanel\" class=\"tab__pane\">\n" + 
      "                              <div class=\"NoContentMessage\">None</div>\n" + 
      "                            </li>\n" + 
      "                            <li id=\"pane-pcw-related\" aria-labelledby=\"pane-pcw-relatedcon\" role=\"tabpanel\" class=\"accordion-with-arrow tab__pane tab__pane--clear\">\n" + 
      "                              <div class=\"accordion\">\n" + 
      "                                <ul class=\"accordion-tabbed rlist\">\n" + 
      "                                  <li class=\"accordion-tabbed__tab related-articles js--open\">\n" + 
      "                                    <a href=\"#\" title=\"Cited By\" aria-expanded=\"true\" aria-controls=\"relatedTab1\" class=\"accordion-tabbed__control\">Related articles</a>\n" + 
      "                                    <ul id=\"relatedTab1\" class=\"rlist accordion-tabbed__content creative-work\">\n" + 
      "                                      <li>\n" + 
      "                                        <a href=\"/doi/10.1377/hlthaff.2017.9999\">\n" + 
      "                                          Microfinance Institutions’ Successful Delivery\n" + 
      "                                          <div class=\"meta\"><span>06 Nov 2017</span><span class=\"journal-title\">Health Affairs</span></div>\n" + 
      "                                        </a>\n" + 
      "                                      </li>\n" + 
      "                                    </ul>\n" + 
      "                                  </li>\n" + 
      "                                  <li class=\"accordion-tabbed__tab\">\n" + 
      "                                    <div class=\"accordion\">\n" + 
      "                                      <ul class=\"accordion-tabbed rlist\">\n" + 
      "                                        <li class=\"accordion-tabbed__tab js--open\">\n" + 
      "                                          <a href=\"#\" title=\"Recommended\" aria-expanded=\"true\" aria-controls=\"9876543-21\" class=\"accordion-tabbed__control\">Recommended</a>\n" + 
      "                                          <div id=\"9876543-21\" class=\"accordion-tabbed__content\" style=\"\">\n" + 
      "                                          </div>\n" + 
      "                                        </li>\n" + 
      "                                      </ul>\n" + 
      "                                    </div>\n" + 
      "                                  </li>\n" + 
      "                                </ul>\n" + 
      "                              </div>\n" + 
      "                            </li>\n" + 
      "                            <li class=\"tab__spinner\" style=\"display: none;\"><img src=\"/widgets/publication-content/images/spinner.gif\" id=\"spinner\" style=\"width: 100%\"></li>\n" + 
      "                          </ul>\n" + 
      "                        </div>\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                  </article>\n" + 
      "                  <script>var articleRef = document.querySelector('.article__body:not(.show-references) .article__references');\n" + 
      "                    if (articleRef) { articleRef.style.display = \"none\"; }\n" + 
      "                    \n" + 
      "                  </script>\n" + 
      "                  <div id=\"figure-viewer\" data-wrapper=\"figure-viewer\" data-transformed-by=\"figureInit\" data-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" + 
      "                    <div class=\"figure-viewer__reg__top clearfix\">\n" + 
      "                      <div class=\"figure-viewer__top__right\"><a title=\"Close Figure Viewer\" href=\"#\" data-role=\"controller\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"></span></a></div>\n" + 
      "                      <div class=\"figure-viewer__top__left\"><a title=\"Browse All Figures\" href=\"#\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"></span></a><a title=\"Return to Figure\" href=\"#\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"></span></a><span tabindex=\"1\" class=\"zoomSlider js__zoom-slider ui-slider\"><input class=\"zoom-range\" type=\"range\"></span><button title=\"zoom in\" class=\"figure-viewer__label__zoom icon-zoom zoom-in\"></button><button title=\"zoom out\" class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"></button></div>\n" + 
      "                    </div>\n" + 
      "                    <div class=\"figure-viewer__reg__center\" style=\"height: 100%;\">\n" + 
      "                      <div class=\"figure-viewer__cent__left\">\n" + 
      "                        <a title=\"Previous Figure\" href=\"#\" class=\"figure-viewer__ctrl__prev\"><span class=\"icon-arrow_l\"></span></a><a title=\"Next Figure\" href=\"#\" class=\"figure-viewer__ctrl__next\"><span class=\"icon-arrow_r\"></span></a>\n" + 
      "                        <div class=\"figure-viewer__hold__fig\">\n" + 
      "                          <figure class=\"holder\"></figure>\n" + 
      "                        </div>\n" + 
      "                        <div class=\"figure-viewer__hold__list clearfix container\"></div>\n" + 
      "                      </div>\n" + 
      "                      <div class=\"figure-viewer__cent__right\">\n" + 
      "                        <div class=\"figure-viewer__title\"><a title=\"Open/Close Caption\" href=\"#\" class=\"figure-viewer__ctrl__caption\"><span class=\"icon-doublearrow\"></span><span class=\"figure-viewer__caption__label\">Caption</span></a><span class=\"figure-viewer__title__text\"></span></div>\n" + 
      "                        <div class=\"figure-viewer__hold__figcap\"></div>\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"container\">\n" + 
      "                    <div class=\"row\">\n" + 
      "                      <div class=\" col-xs-8\">\n" + 
      "                        <div id=\"disqus_thread\"><iframe></iframe></div>\n" + 
      "                        <noscript>Please enable JavaScript</noscript>\n" + 
      "                      </div>\n" + 
      "                      <div class=\" col-xs-4\"></div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <div class=\"journal\"></div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "          <div class=\"w-slide\">\n" + 
      "            <div class=\"w-slide_head\"><a href=\"#\" class=\"w-slide__back\"><i class=\" icon-arrow_l\" aria-hidden=\"true\"></i>back</a><span class=\"w-slide__title\"></span></div>\n" + 
      "            <div class=\"w-slide__content\"></div>\n" + 
      "          </div>\n" + 
      "        </main>\n" + 
      "        <footer>\n" + 
      "          <div class=\"container\">\n" + 
      "            <div class=\"row\">\n" + 
      "              <div>\n" + 
      "                <ul class=\"rlist--inline\">\n" + 
      "                  <li class=\"footer-top col-xs-12 gutterless \">\n" + 
      "                    <div class=\"footer-top__header\">\n" + 
      "                      <div data-widget-def=\"general-image\" data-widget-id=\"9876543-21\" class=\"footer__logo\">\n" + 
      "                        <img alt=\"\" src=\"/pb-assets/images/logos/HA_logo_footer_320x74px.png\">\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                    <div class=\" col-sm-4\">\n" + 
      "                      <address class=\"address\">\n" + 
      "                        <div class=\"address__street\">7500 George Road, Suite 600</div>\n" + 
      "                      </address>\n" + 
      "                    </div>\n" + 
      "                    <div class=\" col-sm-8\">\n" + 
      "                      <div class=\"footer-top__body clearfix cloned hidden-xs hidden-sm\">\n" + 
      "                        <div class=\"footer-top__list\">\n" + 
      "                          <h5 class=\"footer-top__title\">Topics</h5>\n" + 
      "                          <ul class=\"rlist\">\n" + 
      "                            <li><a href=\"/topic/3\">Access &amp; Use</a></li>\n" + 
      "                            <li><a href=\"/topic/83\">Costs &amp; Spending</a></li>\n" + 
      "                            <li><a href=\"/topic/24\">Health Reform</a></li>\n" + 
      "                            <li><a href=\"/topic/57\">Quality Of Care</a></li>\n" + 
      "                            <li><a href=\"/topics\">More Topics</a></li>\n" + 
      "                          </ul>\n" + 
      "                        </div>\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                    <div class=\"footer-bottom\">\n" + 
      "                      <div data-widget-def=\"TMLWidget\" data-widget-id=\"9876543-21\" class=\"container\">\n" + 
      "                        <div class=\"footer-bottom__nav\">\n" + 
      "                          <ul class=\"rlist--inline separator\">\n" + 
      "                            <li><a href=\"/terms-of-use\">Terms and conditions</a></li>\n" + 
      "                            <li><a href=\"/privacy-policy\">Privacy</a></li>\n" + 
      "                            <li><a href=\"http://www.projecthope.org\">Project HOPE</a></li>\n" + 
      "                          </ul>\n" + 
      "                        </div>\n" + 
      "                        <div class=\"copyright\">\n" + 
      "                          <div>\n" + 
      "                            Copyright 1995 -\n" + 
      "                            <script language=\"javascript\" type=\"text/javascript\">\n" + 
      "                              var today = new Date()\n" + 
      "                              var year = today.getFullYear()\n" + 
      "                              document.write(year)\n" + 
      "                            </script>2018\n" + 
      "                            by Project HOPE: The People-to-People Health Foundation, Inc., eISSN 1544-5208.\n" + 
      "                          </div>\n" + 
      "                          <div>Health Affairs is pleased to offer <a href=\"/about\">Free Access for low-income countries,</a> and is a signatory to the <a href=\"http://www.dcprinciples.org/\">DC principles for Free Access to Science</a>. Health Affairs gratefully acknowledges the support of many <a href=\"/funders\">funders</a>.\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                  </li>\n" + 
      "                </ul>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </footer>\n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "    <div style=\"visibility: hidden; height: 1px; width: 1px; position: absolute; top: -9999px; z-index: 100000;\" id=\"_atssh\"><iframe>stuff</iframe></div>\n" + 
      "    <style id=\"service-icons-0\"></style>\n" + 
      "    <script type=\"text/javascript\">\n" + 
      "      if (window.location.hash && window.location.hash == '#_=_') {\n" + 
      "          window.location.hash = '';\n" + 
      "      }\n" + 
      "    </script>\n" + 
      "    <div class=\"altmetric-embed altmetric-popover altmetric-right\" data-uuid=\"9876543-21\" id=\"_altmetric_popover_el\" style=\"margin: 0px; display: none;\">\n" + 
      "    </div>\n" + 
      "    <ul class=\"ui-autocomplete ui-front ui-menu ui-widget ui-widget-content ui-corner-all quickSearchAutocomplete\" id=\"ui-id-1\" tabindex=\"0\" style=\"display: none;\"></ul>\n" + 
      "  </body>\n" + 
      "</html>";
  
  private static final String art1ContentCrawlFiltered =
      "<html class=\"pb-page\" data-request-id=\"9876543-21\" lang=\"en\">\n" +
      "  <head data-pb-dropzone=\"head\">\n" + 
      "    <meta name=\"pbContext\" content=\";website:website:hope-site;page:string:Article/Chapter View;journal:journal:hlthaff;article:article:10.1377/hlthaff.2018.test1;issue:issue:10.1377/hlthaff.2017.36.issue-11;wgroup:string:Publication Websites;pageGroup:string:Publication Pages;subPage:string:Full Text\">\n" + 
      "    <meta data-pb-head=\"head-widgets-start\">\n" + 
      "    <link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\">\n" + 
      "    <meta name=\"dc.Language\" content=\"en\">\n" + 
      "    <meta name=\"dc.Coverage\" content=\"world\">\n" + 
      "    <link rel=\"meta\" type=\"application/atom+xml\" href=\"https://doi.org/10.1377%2Fhlthaff.2018.test1\">\n" + 
      "    <link rel=\"meta\" type=\"application/rdf+json\" href=\"https://doi.org/10.1377%2Fhlthaff.2018.test1\">\n" + 
      "    <link rel=\"meta\" type=\"application/unixref+xml\" href=\"https://doi.org/10.1377%2Fhlthaff.2018.test1\">\n" + 
      "    <meta charset=\"UTF-8\">\n" + 
      "    <meta name=\"robots\" content=\"noarchive\">\n" + 
      "    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1, user-scalable=0\">\n" + 
      "    <title>Making The Most Of Microfinance Networks | Health Affairs</title>\n" + 
      "    <meta data-pb-head=\"head-widgets-end\">\n" + 
      "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" + 
      "  </head>\n" + 
      "  <body class=\"pb-ui\">\n" + 
      "    <div id=\"pb-page-content\" data-ng-non-bindable=\"\">\n" + 
      "      <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "        <div class=\"header base fixed\" data-db-parent-of=\"sb1\">\n" + 
      "          \n" + 
      "        </div>\n" + 
      "        <main class=\"content main-article\" style=\"min-height: 355.117px; padding-top: 157.7px;\">\n" + 
      "          <div class=\"container\">\n" + 
      "            <div class=\"row\">\n" + 
      "              <div>\n" + 
      "                <div class=\"container\"></div>\n" + 
      "                <div>\n" + 
      "                  <article data-figures=\"https://www.healthaffairs.org/action/ajaxShowFigures?doi=10.1377%2Fhlthaff.2018.test1&amp;ajax=true\" data-references=\"https://www.healthaffairs.org/action/ajaxShowEnhancedAbstract?doi=10.1377%2Fhlthaff.2018.test1&amp;ajax=true\" class=\"container\">\n" + 
      "                    <div class=\"row\">\n" + 
      "                      <div class=\"col-sm-8 col-md-8 article__content\">\n" + 
      "                        <!--+articleHEeader()--><!--+articleCitation()-->\n" + 
      "                        <div class=\"citation\">\n" + 
      "                          <div class=\"citation__top\">\n" + 
      "                            <h3 class=\"filled--journal secondary-font no-top-margin\">People &amp; Places</h3>\n" + 
      "                            <span class=\"article__breadcrumbs\">\n" + 
      "                              <div class=\"article__breadcrumbs\">\n" + 
      "                                \n" + 
      "                              </div>\n" + 
      "                            </span>\n" + 
      "                            <span class=\"article__seriesTitle\">PEOPLE &amp; PLACES</span>\n" + 
      "                          </div>\n" + 
      "                          <h1 class=\"citation__title\">Making The Most Of Microfinance Networks</h1>\n" + 
      "                          <div class=\"issue-item\">\n" + 
      "                            <ul class=\"rlist--inline loa mobile-authors\" title=\"list of authors\">\n" + 
      "                              <li><span class=\"hlFld-ContribAuthor\"><sup>1</sup></span></li>\n" + 
      "                            </ul>\n" + 
      "                          </div>\n" + 
      "                          <div class=\"article__affiliations\">\n" + 
      "                            <div class=\"affiliations accordion\">\n" + 
      "                              <a href=\"#\" title=\"Open affiliations\" aria-controls=\"articleAffiliations\" data-slide-target=\"#articleAffiliations\" class=\"affiliations__ctrl accordion__control w-slide__btn\" aria-expanded=\"false\"><span class=\"affiliations__label\"> Affiliations</span><i aria-hidden=\"true\" class=\"icon-section_arrow_d\"></i></a>\n" + 
      "                              <div id=\"articleAffiliations\" class=\"div affiliations__content accordion__content\" style=\"display: none;\">\n" + 
      "                                <!--?xml version=\"1.0\" encoding=\"UTF-8\"?-->\n" + 
      "                                <section class=\"section\">\n" + 
      "                                  <div class=\"section__body\">\n" + 
      "                                    <ol class=\"rlist spaced\">\n" + 
      "                                      <li id=\"BIO1\">1. Author is an author at Health Affairs, in Bethesda, Maryland.</li>\n" + 
      "                                    </ol>\n" + 
      "                                  </div>\n" + 
      "                                </section>\n" + 
      "                              </div>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                        <div class=\"epub-section clearfix\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">PUBLISHED:</span><span class=\"epub-section__date\">November 2017</span></span><span class=\"epub-section__item epub-section__access\"><i aria-hidden=\"true\" class=\"icon-lock_open\"></i><span class=\"epub-section__text\">Full Access</span></span><span class=\"epub-section__item pull-right\"><a href=\"https://doi.org/10.1377/hlthaff.2018.test1\" class=\"epub-section__doi__text\">https://doi.org/10.1377/hlthaff.2018.test1</a></span></div>\n" + 
      "                        <!--+articleCoolbar()-->\n" + 
      "                        <div class=\"scroll-to-target\">\n" + 
      "                          <nav class=\"stickybar coolBar trans\">" +
      "<div class=\"stickybar__wrapper coolBar__wrapper clearfix\" style=\"width: 756.667px; top: 157.7px;\">" +
      "<div class=\"rlist coolBar__zone\">" +
      "<div data-db-target-of=\"article\" id=\"articleMenu\" class=\"coolBar__drop fixed rlist\">" +
      "<div data-target=\"article .tab .tab__nav, .coolBar--download .coolBar__drop\" data-remove=\"false\" data-target-class=\"hidden-xs hidden-sm\" data-toggle=\"transplant\" data-direction=\"from\" data-transplant=\"self\" class=\"transplant showit\">" +
      "<div class=\"transplanted-clone\">" +
      "<ul data-db-target-of=\"downloads\" class=\"coolBar__drop rlist w-slide--list\">" +
      "<li><a href=\"/doi/pdf/10.1377/hlthaff.2018.test1\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View PDF</span></a></li>" +
      "</ul>" +
      "</div></div></div></div>" +
      "<ul data-cb-group=\"Article\" data-cb-group-icon=\"icon-toc\" class=\"rlist coolBar__first\">" +
      "<li class=\"coolBar__section coolBar--download hidden-xs hidden-sm\">" +
      "<ul data-db-target-of=\"downloads\" class=\"coolBar__drop rlist w-slide--list cloned hidden-xs hidden-sm\">" +
      "<li><a href=\"/doi/pdf/10.1377/hlthaff.2018.test1\"><i aria-hidden=\"true\" class=\"icon-download\"></i><span>View PDF</span></a></li>" +
      "</ul>" +
      "</li>" +
      "</ul>" +
      "<ul class=\"coolBar__second rlist\">" +
      "<div class=\" col-xs-6 gutterless \">" +
      //"<div data-db-target-of=\"9876543-21\" aria-labelledby=\"9876543-21_Ctrl\" role=\"menu\" id=\"9876543-21_Pop\" class=\"article-tools__block fixed dropBlock__holder\">" +
      //"<ul class=\"rlist w-slide--list\">" +
      //"<li role=\"none\" class=\"article-tool\">" +
      //"<a href=\"/action/showCitFormats?doi=10.1377%2Fhlthaff.2018.test1\" role=\"menuitem\"><i aria-hidden=\"true\" class=\"icon-Icon_Download\"></i><span>Download Citations</span></a>" +
      //"</li>" +
      //"</ul>" +
      "</div>" + //"</div></div>" +
      "</ul>" +
      "</div>" +
      "</nav>\n" + 
      "                        </div>\n" + 
      "                        <div class=\"article__body \">\n" + 
      "                          <p class=\"fulltext\"></p>\n" + 
      "                          <!--abstract content-->\n" + 
      "                          <div class=\"hlFld-Abstract\">\n" + 
      "                            <p class=\"fulltext\"></p>\n" + 
      "                            <h2 class=\"article-section__title section__title\" id=\"9876543-21\">Abstract</h2>\n" + 
      "                            <div class=\"abstractSection abstractInFull\">\n" + 
      "                              <p xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\">Together, data scientists and microfinance institutions are bringing.</p>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                          <!--/abstract content-->\n" + 
      "                          \n" + 
      "                          <!--fulltext content-->\n" + 
      "                          <div class=\"hlFld-Fulltext\">\n" + 
      "                            <p xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\">Microfinance institutions (MFIs) are known.</p>\n" + 
      "                          </div>\n" + 
      "                          <!--/fulltext content--><script type=\"text/javascript\">\n" + 
      "                            window.figureViewer={doi:'10.1377/hlthaff.2018.test1',path:'/na101/home/literatum/publisher/hope/journals/content/hlthaff/2017/hlthaff.2017.36.issue-11/hlthaff.2018.test1/20171026',figures:[{i:'l1',type:'fig',g:[{m:'figure1.gif',l:'figure1.jpeg',size:'540 KB'}]}\n" + 
      "                            ]}\n" + 
      "                          </script>\n" + 
      "                          <div class=\"response\">\n" + 
      "                            <div class=\"sub-article-title\"></div>\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                      </div>\n" + 
      "                      <div class=\"col-sm-4 hidden-xs hidden-sm sticko__parent\">\n" + 
      "                        <ul class=\"social-links rlist rlist--inline\">\n" + 
      "                          <li> <a href=\"https://www.youtube.com/user/HealthAffairsJournal\" title=\"youtube\" class=\"social-links__item\"> <i class=\"icon-youtube\"></i> </a> </li>\n" + 
      "                          <li> <a href=\"https://twitter.com/Health_Affairs\" title=\"twitter\" class=\"social-links__item\"> <i class=\"icon-twitter\"></i> </a> </li>\n" + 
      "                        </ul>\n" + 
      "                        <!--+articleTab()-->\n" + 
      "                        <div class=\"tab tab--slide tab--flex sticko__md tabs--xs dynamic-sticko tab--flex tabs--xs js--sticko\" style=\"width: 363.317px; top: 167.7px; position: static; height: 534.1px;\">\n" + 
      "                          <ul data-mobile-toggle=\"slide\" class=\"rlist tab__nav w-slide--list cloned hidden-xs hidden-sm\">\n" + 
      "                            <li role=\"presentation\" class=\"active\"><a href=\"#pane-pcw-details\" aria-controls=\"#pane-pcw-details\" role=\"tab\" data-toggle=\"tab\" title=\"details\" id=\"pane-pcw-detailscon\" data-slide-target=\"#pane-pcw-details\" class=\"details-tab\" aria-expanded=\"true\"><span>Details</span></a></li>\n" + 
      "                            <li role=\"presentation\"><a href=\"#pane-pcw-figures\" aria-controls=\"#pane-pcw-figures\" role=\"tab\" data-toggle=\"tab\" title=\"figures\" id=\"pane-pcw-figurescon\" data-slide-target=\"#pane-pcw-figures\" class=\"figures-tab empty\"><span>Exhibits</span></a></li>\n" + 
      "                            <li role=\"presentation\"><a href=\"#pane-pcw-references\" aria-controls=\"#pane-pcw-references\" role=\"tab\" data-toggle=\"tab\" title=\"references\" id=\"pane-pcw-referencescon\" data-slide-target=\"#pane-pcw-references\" class=\"references-tab empty\"><span>References</span></a></li>\n" + 
      "                            <li role=\"presentation\"><a href=\"#pane-pcw-related\" aria-controls=\"#pane-pcw-related\" role=\"tab\" data-toggle=\"tab\" title=\"related\" id=\"pane-pcw-relatedcon\" data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><span>Related</span></a></li>\n" + 
      "                          </ul>\n" + 
      "                          <ul class=\"rlist tab__content sticko__child\" style=\"height: 534.1px; overflow-y: auto;\">\n" + 
      "                            <li id=\"pane-pcw-details\" aria-labelledby=\"pane-pcw-detailscon\" role=\"tabpanel\" class=\"tab__pane active\">\n" + 
      "                              <div class=\"details-tab\">\n" + 
      "                                \n" + 
      "                                <hr class=\"section__separator\">\n" +
      "                                <hr class=\"section__separator\">\n" + 
      "                              </div>\n" + 
      "                              <section class=\"section\">\n" + 
      "                                <strong class=\"section__title\">Information</strong>\n" + 
      "                                <div class=\"section__body\">\n" + 
      "                                  <p>(c) 2017 Project HOPE-The People-to-People Health Foundation, Inc.</p>\n" + 
      "                                </div>\n" + 
      "                              </section>\n" + 
      "                              <hr class=\"section__separator\">\n" + 
      "                              <ul class=\"rlist rlist--inline\"></ul>\n" + 
      "                            </li>\n" + 
      "                            <li id=\"pane-pcw-figures\" aria-labelledby=\"pane-pcw-figurescon\" role=\"tabpanel\" class=\"tab__pane empty\">\n" + 
      "                              <div class=\"NoContentMessage\">None</div>\n" + 
      "                            </li>\n" + 
      "                            \n" + 
      "                            \n" + 
      "                            <li class=\"tab__spinner\" style=\"display: none;\"><img src=\"/widgets/publication-content/images/spinner.gif\" id=\"spinner\" style=\"width: 100%\"></li>\n" + 
      "                          </ul>\n" + 
      "                        </div>\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                  </article>\n" + 
      "                  <script>var articleRef = document.querySelector('.article__body:not(.show-references) .article__references');\n" + 
      "                    if (articleRef) { articleRef.style.display = \"none\"; }\n" + 
      "                    \n" + 
      "                  </script>\n" + 
      "                  <div id=\"figure-viewer\" data-wrapper=\"figure-viewer\" data-transformed-by=\"figureInit\" data-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" + 
      "                    <div class=\"figure-viewer__reg__top clearfix\">\n" + 
      "                      <div class=\"figure-viewer__top__right\"><a title=\"Close Figure Viewer\" href=\"#\" data-role=\"controller\" class=\"figure-viewer__ctrl__close\"><span class=\"icon-close_thin\"></span></a></div>\n" + 
      "                      <div class=\"figure-viewer__top__left\"><a title=\"Browse All Figures\" href=\"#\" class=\"figure-viewer__ctrl__browse\"><span class=\"icon-allfigures\"></span></a><a title=\"Return to Figure\" href=\"#\" class=\"figure-viewer__ctrl__return is-hidden\"><span class=\"icon-arrow-left\"></span></a><span tabindex=\"1\" class=\"zoomSlider js__zoom-slider ui-slider\"><input class=\"zoom-range\" type=\"range\"></span><button title=\"zoom in\" class=\"figure-viewer__label__zoom icon-zoom zoom-in\"></button><button title=\"zoom out\" class=\"figure-viewer__label__zoom icon-zoom-out zoom-out hidden\"></button></div>\n" + 
      "                    </div>\n" + 
      "                    <div class=\"figure-viewer__reg__center\" style=\"height: 100%;\">\n" + 
      "                      <div class=\"figure-viewer__cent__left\">\n" + 
      "                        <a title=\"Previous Figure\" href=\"#\" class=\"figure-viewer__ctrl__prev\"><span class=\"icon-arrow_l\"></span></a><a title=\"Next Figure\" href=\"#\" class=\"figure-viewer__ctrl__next\"><span class=\"icon-arrow_r\"></span></a>\n" + 
      "                        <div class=\"figure-viewer__hold__fig\">\n" + 
      "                          <figure class=\"holder\"></figure>\n" + 
      "                        </div>\n" + 
      "                        <div class=\"figure-viewer__hold__list clearfix container\"></div>\n" + 
      "                      </div>\n" + 
      "                      <div class=\"figure-viewer__cent__right\">\n" + 
      "                        <div class=\"figure-viewer__title\"><a title=\"Open/Close Caption\" href=\"#\" class=\"figure-viewer__ctrl__caption\"><span class=\"icon-doublearrow\"></span><span class=\"figure-viewer__caption__label\">Caption</span></a><span class=\"figure-viewer__title__text\"></span></div>\n" + 
      "                        <div class=\"figure-viewer__hold__figcap\"></div>\n" + 
      "                      </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"container\">\n" + 
      "                    <div class=\"row\">\n" + 
      "                      <div class=\" col-xs-8\">\n" + 
      "                        \n" + 
      "                        <noscript>Please enable JavaScript</noscript>\n" + 
      "                      </div>\n" + 
      "                      <div class=\" col-xs-4\"></div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "                <div class=\"journal\"></div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "          <div class=\"w-slide\">\n" + 
      "            <div class=\"w-slide_head\"><a href=\"#\" class=\"w-slide__back\"><i class=\" icon-arrow_l\" aria-hidden=\"true\"></i>back</a><span class=\"w-slide__title\"></span></div>\n" + 
      "            <div class=\"w-slide__content\"></div>\n" + 
      "          </div>\n" + 
      "        </main>\n" + 
      "        \n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "    <div style=\"visibility: hidden; height: 1px; width: 1px; position: absolute; top: -9999px; z-index: 100000;\" id=\"_atssh\"><iframe>stuff</iframe></div>\n" + 
      "    <style id=\"service-icons-0\"></style>\n" + 
      "    <script type=\"text/javascript\">\n" + 
      "      if (window.location.hash && window.location.hash == '#_=_') {\n" + 
      "          window.location.hash = '';\n" + 
      "      }\n" + 
      "    </script>\n" + 
      "    \n" + 
      "    <ul class=\"ui-autocomplete ui-front ui-menu ui-widget ui-widget-content ui-corner-all quickSearchAutocomplete\" id=\"ui-id-1\" tabindex=\"0\" style=\"display: none;\"></ul>\n" + 
      "  </body>\n" + 
      "</html>";
  
  private static final String art1ContentFiltered = 
      "<div class=\"col-sm-8 col-md-8 article__content\">\n" + 
      "                        \n" + 
      "                        <div class=\"citation\">\n" + 
      "                          <div class=\"citation__top\">\n" + 
      "                            <h3 class=\"filled--journal secondary-font no-top-margin\">People &amp; Places</h3>\n" + 
      "                            <span class=\"article__breadcrumbs\">\n" + 
      "                              \n" + 
      "                            </span>\n" + 
      "                            <span class=\"article__seriesTitle\">PEOPLE &amp; PLACES</span>\n" + 
      "                          </div>\n" + 
      "                          <h1 class=\"citation__title\">Making The Most Of Microfinance Networks</h1>\n" + 
      "                          <div class=\"issue-item\">\n" + 
      "                            <ul class=\"rlist--inline loa mobile-authors\" title=\"list of authors\">\n" + 
      "                              <li><span class=\"hlFld-ContribAuthor\"><a href=\"/author/Author\" title=\"Author\">Author</a><sup>1</sup></span></li>\n" + 
      "                            </ul>\n" + 
      "                          </div>\n" + 
      "                          <div class=\"article__affiliations\">\n" + 
      "                            <div class=\"affiliations accordion\">\n" + 
      "                              <a href=\"#\" title=\"Open affiliations\" aria-controls=\"articleAffiliations\" data-slide-target=\"#articleAffiliations\" class=\"affiliations__ctrl accordion__control w-slide__btn\" aria-expanded=\"false\"><span class=\"affiliations__label\"> Affiliations</span><i aria-hidden=\"true\" class=\"icon-section_arrow_d\"></i></a>\n" + 
      "                              <div id=\"articleAffiliations\" class=\"div affiliations__content accordion__content\" style=\"display: none;\">\n" + 
      "                                \n" + 
      "                                <section class=\"section\">\n" + 
      "                                  <div class=\"section__body\">\n" + 
      "                                    <ol class=\"rlist spaced\">\n" + 
      "                                      <li id=\"BIO1\">1. Author is an author at Health Affairs, in Bethesda, Maryland.</li>\n" + 
      "                                    </ol>\n" + 
      "                                  </div>\n" + 
      "                                </section>\n" + 
      "                              </div>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                        <div class=\"epub-section clearfix\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">PUBLISHED:</span><span class=\"epub-section__date\">November 2017</span></span><span class=\"epub-section__item epub-section__access\"><i aria-hidden=\"true\" class=\"icon-lock_open\"></i><span class=\"epub-section__text\">Full Access</span></span><span class=\"epub-section__item pull-right\"><a href=\"https://doi.org/10.1377/hlthaff.2018.test1\" class=\"epub-section__doi__text\">https://doi.org/10.1377/hlthaff.2018.test1</a></span></div>\n" + 
      "                        \n" + 
      "                        \n" + 
      "                        <div class=\"article__body \">\n" + 
      "                          <p class=\"fulltext\"></p>\n" + 
      "                          \n" + 
      "                          <div class=\"hlFld-Abstract\">\n" + 
      "                            <p class=\"fulltext\"></p>\n" + 
      "                            <h2 class=\"article-section__title section__title\" id=\"9876543-21\">Abstract</h2>\n" + 
      "                            <div class=\"abstractSection abstractInFull\">\n" + 
      "                              <p xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\">Together, data scientists and microfinance institutions are bringing.</p>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                          \n" + 
      "                          <div class=\"article__topic\">\n" + 
      "                            <strong class=\"article__topic-title\">TOPICS</strong>\n" + 
      "                            <div class=\"article__topic-body\">\n" + 
      "                              <ul class=\"rlist rlist--inline\">\n" + 
      "                                <li><a href=\"/topic/69\" class=\"badge-type\">Global health</a></li>\n" + 
      "                                <li><a href=\"/topic/1602\" class=\"badge-type\">Community health</a></li>\n" + 
      "                                <li><a href=\"/topic/154\" class=\"badge-type\">Childrens health</a></li>\n" + 
      "                              </ul>\n" + 
      "                            </div>\n" + 
      "                          </div>\n" + 
      "                          \n" + 
      "                          <div class=\"hlFld-Fulltext\">\n" + 
      "                            <p xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:oasis=\"http://www.niso.org/standards/z39-96/ns/oasis-exchange/table\">Microfinance institutions (MFIs) are known.</p>\n" + 
      "                          </div>\n" + 
      "                          \n" + 
      "                          <div class=\"response\">\n" + 
      "                            <div class=\"sub-article-title\"></div>\n" + 
      "                          </div>\n" + 
      "                        </div>\n" + 
      "                      </div>";
  
  private static final String art1ContentHashFiltered = 
      "" + 
      "" + 
      "" + 
      "" + 
      " People &amp; Places" + 
      "" + 
      "" + 
      "" + 
      " PEOPLE &amp; PLACES" + 
      "" + 
      " Making The Most Of Microfinance Networks" + 
      "" + 
      "" + 
      " Author 1" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " Affiliations" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " 1. Author is an author at Health Affairs, in Bethesda, Maryland." + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " PUBLISHED: November 2017 Full Access https://doi.org/10.1377/hlthaff.2018.test1" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " Abstract" + 
      "" + 
      " Together, data scientists and microfinance institutions are bringing." + 
      "" + 
      "" + 
      "" + 
      "" + 
      " TOPICS" + 
      "" + 
      "" + 
      " Global health" + 
      " Community health" + 
      " Childrens health" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " Microfinance institutions (MFIs) are known." + 
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
      "    <div class=\"art_title\"><a href=\"/doi/abs/10.1377/hlthaff.2018.test1\">research in progress</a></div>\n" + 
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
      "    <div class=\"art_title\"><a href=\"/doi/abs/10.1377/hlthaff.2018.test1\">research in progress</a></div>\n" + 
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
    String inStr = StringUtil.fromInputStream(actIn);
    log.info(inStr);
    log.info(expectedStr);
    assertEquals(expectedStr, inStr);
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
  public static class TestCrawl extends TestProjectHopeHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new ProjectHopeHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, manifestContent, manifestContent);
      doFilterTest(mau, variantFact, tocContent, tocContentCrawlFiltered);
      doFilterTest(mau, variantFact, art1Content, art1ContentCrawlFiltered);
      doFilterTest(mau, variantFact, citContent, citContent);
    }
  }
  
  // Variant to test with Hash Filter
   public static class TestHash extends TestProjectHopeHtmlFilterFactory {
     public void testFiltering() throws Exception {
       variantFact = new ProjectHopeHtmlHashFilterFactory();
       doFilterTest(mau, variantFact, manifestContent, manifestHashFiltered);
//       doFilterTest(mau, variantFact, tocContent, tocContentFiltered); // with doTagRemovalFiltering & doWSFiltering = false
       doFilterTest(mau, variantFact, tocContent, tocContentHashFiltered);
//       doFilterTest(mau, variantFact, art1Content, art1ContentFiltered); // with doTagRemovalFiltering & doWSFiltering = false
       doFilterTest(mau, variantFact, art1Content, art1ContentHashFiltered);
//       doFilterTest(mau, variantFact, citContent, citContentFiltered); // with doTagRemovalFiltering & doWSFiltering = false
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

