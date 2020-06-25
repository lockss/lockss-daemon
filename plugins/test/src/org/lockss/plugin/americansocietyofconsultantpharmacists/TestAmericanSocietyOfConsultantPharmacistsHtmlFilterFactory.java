/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americansocietyofconsultantpharmacists;

import org.apache.commons.io.FileUtils;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TestAmericanSocietyOfConsultantPharmacistsHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AmericanSocietyOfConsultantPharmacistsHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AmericanSocietyOfConsultantPharmacistsHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String articlePage = "" +
          "<!DOCTYPE html>\n" +
          "<html lang=\"en\">\n" +
          "<head>\n" +
          "    head content\n" +
          "</head>\n" +
          "<body class=\"ingenta\">\n" +
          "<div id=\"hiddenContext\"></div>\n" +
          "<div id=\"skiptocontent\"><a href=\"#mainContents\">Skip to main content</a></div>\n" +
          "<div class=\"wrapper\">\n" +
          "    <div id=\"llb\" class=\"container\" style=\"background-color : #FFFFFF;\">\n" +
          "        llb content\n" +
          "    </div>\n" +
          "    <div class=\"container device-tab-mobile-buttons-container\">\n" +
          "        device-tab-mobile-buttons-container content\n" +
          "    </div>\n" +
          "    <div class=\"container\">\n" +
          "        <div class=\"row mainRow\">\n" +
          "            <div id=\"secure\">\n" +
          "                secure content\n" +
          "            </div>\n" +
          "            <div class=\"breadcrumb\">\n" +
          "                breadcrumb content\n" +
          "            </div>\n" +
          "            <div class=\"col-xs-12 col-sm-8 col-md-9\" id=\"mainContents\">\n" +
          "                <div class=\"advertisingbanner\">\n" +
          "                    advertisingbanner content\n" +
          "                </div>\n" +
          "                <div id=\"headingetc\">\n" +
          "                    <div class=\"page-heading\">\n" +
          "                        <!-- thispubshort = -ascp- --><!-- isBLPub = false -->\n" +
          "                        <div id=\"article-journal-logo\">\n" +
          "                            <img src=\"/images/journal-logos/ascp/tscp.png\" alt=\" logo\" />\n" +
          "                        </div>\n" +
          "                        <div class=\"heading-text\">\n" +
          "                            <h1 class=\"abstract-heading\">\n" +
          "                              <span class=\"access-icon\">\n" +
          "                              <img alt=\"Free Content\" src=\"/images/icon_f_square.gif\" title=\"Free content\"/>\n" +
          "                              </span>\n" +
          "                                Rhinorrhea as a Result of Alzheimer's Disease Treatment\n" +
          "                            </h1>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                    <div class=\"heading-macfix article-access-options rs_skip availabilityfree\" id=\"purchaseexpand\">\n" +
          "                        <div class=\"normal\">\n" +
          "                            <h5 class=\"buylabel downloadArt\">Download Article:</h5>\n" +
          "                            <span class=\"rust\">\n" +
          "                           </span>\n" +
          "                            <div class=\"right-col-download contain\">\n" +
          "                                <a class=\"fulltext pdf btn btn-general icbutton no-underline contain\" href=\"#\" data-popup='/search/download?pub=infobike%3a%2f%2fascp%2ftscp%2f2020%2f00000035%2f00000004%2fart00002&mimetype=application%2fpdf' title=\"PDF download of Rhinorrhea as a Result of Alzheimer&#039;s Disease Treatment\" ><i class=\"fa fa-arrow-circle-o-down\"></i></a>&nbsp;<span class=\"rust\"><strong>Download</strong> <br />(PDF 53.4 kb)</span>&nbsp;\n" +
          "                            </div>\n" +
          "                            <div class=\"spinner hiddenDownload hidden\"><i class=\"fa fa-spinner fa-spin\"></i></div>\n" +
          "                            <p class=\"hidden\">&nbsp;</p>\n" +
          "                            <p class=\"hidden\">&nbsp;</p>\n" +
          "                            <p class=\"hidden\">&nbsp;</p>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <div class=\"metaDataArea\">\n" +
          "                    <div id=\"infoArticle\">\n" +
          "                        <span class=\"__dimensions_badge_embed__\" data-doi=\"10.4140/TCP.n.2020.148.\" data-hide-zero-citations=\"true\"\n" +
          "                              data-legend=\"hover-bottom\" data-style=\"small_circle\"></span>\n" +
          "                        <script async src=\"https://badge.dimensions.ai/badge.js\" charset=\"utf-8\"></script>\n" +
          "                        <div class=\"supMetaData\"></div>\n" +
          "                        <div class=\"supMetaData\">\n" +
          "                            <p><strong>Author: </strong><a href=\"/search?option2=author&value2=Vouri,+Scott+Martin\" title=\"Search for articles by this author\">Vouri, Scott Martin</a></p>\n" +
          "                        </div>\n" +
          "                        <div class=\"supMetaData\">\n" +
          "                            <p><strong>Source:</strong> <a href=\"/content/ascp/tscp\" title=\"link to all issues of this title\">The Senior Care Pharmacist</a>, Volume 35,&nbsp;Number 4, April 2020, pp. <span class=\"pagesNum\">148-149(2)</span></p>\n" +
          "                            <p><strong>Publisher: </strong><a href=\"/content/ascp\" title=\"link to all titles by this publisher\">American Society of Consultant Pharmacists</a></p>\n" +
          "                            <p><strong>DOI:</strong> <a href=\"https://doi.org/10.4140/TCP.n.2020.148.\">https://doi.org/10.4140/TCP.n.2020.148.</a></p>\n" +
          "                        </div>\n" +
          "                        <div class=\"heading-text\"></div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <div class=\"includeCompilations rs_skip\"></div>\n" +
          "                <div class=\"article-pager rs_skip\">\n" +
          "                    article-pager content\n" +
          "                </div>\n" +
          "                <p class=\"hidden\">&nbsp;</p>\n" +
          "                <div class=\"heading-macfix noline rs_skip\">\n" +
          "                    <div class=\"right-col\"></div>\n" +
          "                    <p class=\"hidden\">&nbsp;</p>\n" +
          "                </div>\n" +
          "                <div class=\"skyscraperright rs_skip\">\n" +
          "                    <!-- /1008130/Skyscraper (_body) -->\n" +
          "                    <div id=\"div-gpt-ad-Skyscraper_body\" style=\"height:600px; width:120px;\">\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <ul class=\"nav nav-tabs rs_skip\" role=\"tablist\">\n" +
          "                     nav-tabs content" + 
          "                </ul>\n" +
          "                <div class=\"tab-content\">\n" +
          "                    <div id=\"Abst\" class=\"tab-pane active\">\n" +
          "                        Pharmacists serving older individuals should be encouraged to avoid prescribing cascades by recommending medication discontinuation or dose reduction, whenever possible.\n" +
          "                    </div>\n" +
          "                    <div id=\"Refs\" class=\"tab-pane\">\n" +
          "                        No References for this article.\n" +
          "                    </div>\n" +
          "                    <div id=\"Cits\" class=\"tab-pane\">\n" +
          "                        <!-- No Citations for this article ?? -->\n" +
          "                        <div id=\"cit-hook\" class=\"fa fa-spinner fa-spin\" title=\"Please wait, loading citation information...\"></div>\n" +
          "                        <div id=\"cit-hook-content\" class=\"hidden\"></div>\n" +
          "                    </div>\n" +
          "                    <div id=\"Supp\" class=\"tab-pane\">\n" +
          "                        No Supplementary Data.\n" +
          "                    </div>\n" +
          "                    <div id=\"Data\" class=\"tab-pane rs_skip\">No Article Media</div>\n" +
          "                    <div id=\"Metr\" class=\"tab-pane rs_skip\">No Metrics</div>\n" +
          "                </div>\n" +
          "                <div id=\"Info\">\n" +
          "                    Info content\n" +
          "                </div>\n" +
          "                <div id=\"trendmd-suggestions\"></div>\n" +
          "                <div class=\"advertisingbanner clear\">\n" +
          "                    <!-- /1008130/Horizontal_banner_bottom -->\n" +
          "                    <div id=\"div-gpt-ad-Horizontal_banner_bottom\" style=\"height:60px; width:468px;\">\n" +
          "                        <script>\n" +
          "                           try {\n" +
          "                           googletag.cmd.push(function() { googletag.display('div-gpt-ad-Horizontal_banner_bottom'); });\n" +
          "                           } catch (e) {\n" +
          "                           console.warn(\"*** googletag.cmd.push(function() failed: div: div-gpt-ad-Horizontal_banner_bottom Position: Horizontal_banner_bottom Error: '%s'\", e.message);\n" +
          "                           }\n" +
          "                        </script>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "            <div class=\"col-xs-3 col-sm-4 col-md-3 pull-right signin-tools\">\n" +
          "                <div class=\"sign-transparent-bg\"></div>\n" +
          "                <div class=\"collapse navbar-collapse signInContainer\" id=\"sign-in-container\">\n" +
          "                    sign-in-container content\n" +
          "                </div>\n" +
          "                <div id=\"tools\" class=\"collapse navbar-collapse\">\n" +
          "                    tools content\n" +
          "                </div>\n" +
          "            </div>\n" +
          "            <div class=\"col-xs-12 col-sm-4 col-md-3\">\n" +
          "                <div class=\"shareContent\" id=\"google_translate_element\">\n" +
          "                    google_translate_element content\n" +
          "                </div>\n" +
          "                <script type=\"text/javascript\" src=\"//translate.google.com/translate_a/element.js?cb=googleTranslateElementInit\"></script>\n" +
          "                <div class=\"shareContent\">\n" +
          "                    shareContent content\n" +
          "                </div>\n" +
          "                <div class=\"icon-key\">\n" +
          "                    icon-key content\n" +
          "                </div>\n" +
          "                <div class=\"advertisingbanner\">\n" +
          "                    advertisingbanner content\n" +
          "                </div>\n" +
          "            </div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "<footer class=\"footer\">\n" +
          "    footer content\n" +
          "</footer>\n" +
          "<script type=\"text/javascript\">\n" +
          "         $(document).ready(function() {\n" +
          "         $(\".addArticleToVJ\").on(\"click\", function() {\n" +
          "         var $this = $(this),\n" +
          "         formaction = $this.parent(\"span\").parent(\"td\").find(\".formAction\").val();\n" +
          "         $(\"#addArticleToVPub\").attr(\"action\",formaction);\n" +
          "         // $(this).parent(\"td\").find(\".formAction\").remove();\n" +
          "         $(\"#addArticleToVPub\").empty().append($this.parent(\"span\").parent(\"td\").find(\":input\")).submit();\n" +
          "         return false;\n" +
          "         });\n" +
          "         });\n" +
          "      </script>\n" +
          "<!-- cookies tag -->\n" +
          "<div class=\"cornerPolicyTab\">\n" +
          "    cornerPolicyTab content\n" +
          "</div>\n" +
          "<div class=\"mainCookiesPopUp\">\n" +
          "    mainCookiesPopUp content\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";
  
  private static final String getArticlePageFiltered = "" +
          "<!DOCTYPE html>\n" +
          "<html lang=\"en\">\n" +
          "\n" +
          "<body class=\"ingenta\">\n" +
          "<div id=\"hiddenContext\"></div>\n" +
          "<div id=\"skiptocontent\"><a href=\"#mainContents\">Skip to main content</a></div>\n" +
          "<div class=\"wrapper\">\n" +
          "    \n" +
          "    <div class=\"container device-tab-mobile-buttons-container\">\n" +
          "        device-tab-mobile-buttons-container content\n" +
          "    </div>\n" +
          "    <div class=\"container\">\n" +
          "        <div class=\"row mainRow\">\n" +
          "            \n" +
          "            \n" +
          "            <div class=\"col-xs-12 col-sm-8 col-md-9\" id=\"mainContents\">\n" +
          "                \n" +
          "                <div id=\"headingetc\">\n" +
          "                    <div class=\"page-heading\">\n" +
          "                        <!-- thispubshort = -ascp- --><!-- isBLPub = false -->\n" +
          "                        <div id=\"article-journal-logo\">\n" +
          "                            <img src=\"/images/journal-logos/ascp/tscp.png\" alt=\" logo\" />\n" +
          "                        </div>\n" +
          "                        <div class=\"heading-text\">\n" +
          "                            <h1 class=\"abstract-heading\">\n" +
          "                              <span class=\"access-icon\">\n" +
          "                              <img alt=\"Free Content\" src=\"/images/icon_f_square.gif\" title=\"Free content\"/>\n" +
          "                              </span>\n" +
          "                                Rhinorrhea as a Result of Alzheimer's Disease Treatment\n" +
          "                            </h1>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                    <div class=\"heading-macfix article-access-options rs_skip availabilityfree\" id=\"purchaseexpand\">\n" +
          "                        <div class=\"normal\">\n" +
          "                            <h5 class=\"buylabel downloadArt\">Download Article:</h5>\n" +
          "                            <span class=\"rust\">\n" +
          "                           </span>\n" +
          "                            <div class=\"right-col-download contain\">\n" +
          "                                <a class=\"fulltext pdf btn btn-general icbutton no-underline contain\" href=\"#\" data-popup='/search/download?pub=infobike%3a%2f%2fascp%2ftscp%2f2020%2f00000035%2f00000004%2fart00002&mimetype=application%2fpdf' title=\"PDF download of Rhinorrhea as a Result of Alzheimer&#039;s Disease Treatment\" ><i class=\"fa fa-arrow-circle-o-down\"></i></a>&nbsp;<span class=\"rust\"><strong>Download</strong> <br />(PDF 53.4 kb)</span>&nbsp;\n" +
          "                            </div>\n" +
          "                            <div class=\"spinner hiddenDownload hidden\"><i class=\"fa fa-spinner fa-spin\"></i></div>\n" +
          "                            <p class=\"hidden\">&nbsp;</p>\n" +
          "                            <p class=\"hidden\">&nbsp;</p>\n" +
          "                            <p class=\"hidden\">&nbsp;</p>\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <div class=\"metaDataArea\">\n" +
          "                    <div id=\"infoArticle\">\n" +
          "                        <span class=\"__dimensions_badge_embed__\" data-doi=\"10.4140/TCP.n.2020.148.\" data-hide-zero-citations=\"true\"\n" +
          "                              data-legend=\"hover-bottom\" data-style=\"small_circle\"></span>\n" +
          "                        \n" +
          "                        <div class=\"supMetaData\"></div>\n" +
          "                        <div class=\"supMetaData\">\n" +
          "                            <p><strong>Author: </strong><a href=\"/search?option2=author&value2=Vouri,+Scott+Martin\" title=\"Search for articles by this author\">Vouri, Scott Martin</a></p>\n" +
          "                        </div>\n" +
          "                        <div class=\"supMetaData\">\n" +
          "                            <p><strong>Source:</strong> <a href=\"/content/ascp/tscp\" title=\"link to all issues of this title\">The Senior Care Pharmacist</a>, Volume 35,&nbsp;Number 4, April 2020, pp. <span class=\"pagesNum\">148-149(2)</span></p>\n" +
          "                            <p><strong>Publisher: </strong><a href=\"/content/ascp\" title=\"link to all titles by this publisher\">American Society of Consultant Pharmacists</a></p>\n" +
          "                            <p><strong>DOI:</strong> <a href=\"https://doi.org/10.4140/TCP.n.2020.148.\">https://doi.org/10.4140/TCP.n.2020.148.</a></p>\n" +
          "                        </div>\n" +
          "                        <div class=\"heading-text\"></div>\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <div class=\"includeCompilations rs_skip\"></div>\n" +
          "                <div class=\"article-pager rs_skip\">\n" +
          "                    article-pager content\n" +
          "                </div>\n" +
          "                <p class=\"hidden\">&nbsp;</p>\n" +
          "                <div class=\"heading-macfix noline rs_skip\">\n" +
          "                    <div class=\"right-col\"></div>\n" +
          "                    <p class=\"hidden\">&nbsp;</p>\n" +
          "                </div>\n" +
          "                <div class=\"skyscraperright rs_skip\">\n" +
          "                    <!-- /1008130/Skyscraper (_body) -->\n" +
          "                    <div id=\"div-gpt-ad-Skyscraper_body\" style=\"height:600px; width:120px;\">\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "                <ul class=\"nav nav-tabs rs_skip\" role=\"tablist\">\n" +
          "                     nav-tabs content                </ul>\n" +
          "                <div class=\"tab-content\">\n" +
          "                    <div id=\"Abst\" class=\"tab-pane active\">\n" +
          "                        Pharmacists serving older individuals should be encouraged to avoid prescribing cascades by recommending medication discontinuation or dose reduction, whenever possible.\n" +
          "                    </div>\n" +
          "                    <div id=\"Refs\" class=\"tab-pane\">\n" +
          "                        No References for this article.\n" +
          "                    </div>\n" +
          "                    <div id=\"Cits\" class=\"tab-pane\">\n" +
          "                        <!-- No Citations for this article ?? -->\n" +
          "                        <div id=\"cit-hook\" class=\"fa fa-spinner fa-spin\" title=\"Please wait, loading citation information...\"></div>\n" +
          "                        <div id=\"cit-hook-content\" class=\"hidden\"></div>\n" +
          "                    </div>\n" +
          "                    <div id=\"Supp\" class=\"tab-pane\">\n" +
          "                        No Supplementary Data.\n" +
          "                    </div>\n" +
          "                    <div id=\"Data\" class=\"tab-pane rs_skip\">No Article Media</div>\n" +
          "                    <div id=\"Metr\" class=\"tab-pane rs_skip\">No Metrics</div>\n" +
          "                </div>\n" +
          "                \n" +
          "                <div id=\"trendmd-suggestions\"></div>\n" +
          "                <div class=\"advertisingbanner clear\">\n" +
          "                    <!-- /1008130/Horizontal_banner_bottom -->\n" +
          "                    <div id=\"div-gpt-ad-Horizontal_banner_bottom\" style=\"height:60px; width:468px;\">\n" +
          "                        \n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "            <div class=\"col-xs-3 col-sm-4 col-md-3 pull-right signin-tools\">\n" +
          "                <div class=\"sign-transparent-bg\"></div>\n" +
          "                \n" +
          "                \n" +
          "            </div>\n" +
          "            <div class=\"col-xs-12 col-sm-4 col-md-3\">\n" +
          "                \n" +
          "                \n" +
          "                \n" +
          "                \n" +
          "                \n" +
          "            </div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "\n" +
          "\n" +
          "<!-- cookies tag -->\n" +
          "\n" +
          "\n" +
          "</body>\n" +
          "</html>";
  

  
  public void testFiltering() throws Exception {
    assertFilterToSame(articlePage, getArticlePageFiltered);
  }
  
  private void assertFilterToSame(String str1, String Str2) throws Exception {
    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(str1),
        Constants.DEFAULT_ENCODING);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(Str2),
        Constants.DEFAULT_ENCODING);
    
    String a = StringUtil.fromInputStream(inA);
    String b = StringUtil.fromInputStream(inB);
    assertEquals(a, b);
  }
  
}
