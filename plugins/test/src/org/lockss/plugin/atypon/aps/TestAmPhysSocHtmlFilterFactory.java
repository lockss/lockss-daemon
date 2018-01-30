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

package org.lockss.plugin.atypon.aps;

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

public class TestAmPhysSocHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
  
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.aps.AmPhysSocAtyponPlugin";
  
  
  private static final String manifestContent = 
      "<html><body>\n" +
      "<h1>Journal Name 2017 CLOCKSS Manifest Page</h1>\n" +
      "<ul>\n" +
      "    <li><a href=\"/toc/jid/313/6\">December 2017 (Vol. 313 Issue 6 Page F1179-F1280)</a></li>\n" +
      "    <li><a href=\"/toc/jid/313/5\">November 2017 (Vol. 313 Issue 5 Page F1061-F1178)</a></li>\n" +
      "    <li><a href=\"/toc/jid/313/4\">October 2017 (Vol. 313 Issue 4 Page F835-F1060)</a></li>\n" +
      "    <li><a href=\"/toc/jid/313/3\">September 2017 (Vol. 313 Issue 3 Page F561-F728)</a></li>\n" +
      "    <li><a href=\"/toc/jid/313/2\">August 2017 (Vol. 313 Issue 2 Page F135-F560)</a></li>\n" +
      "    <li><a href=\"/toc/jid/313/1\">July 2017 (Vol. 313 Issue 1 Page F1-F61)</a></li>\n" +
      "</ul>\n" +
      "<p>\n" +
      "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" alt=\"LOCKSS logo\" width=\"108\" height=\"108\">\n" +
      "    CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.\n" +
      "</p>\n" +
      "</body></html>";
  
/*
  private static final String manifestContentFiltered = 
      "<a href=\"/toc/jid/313/6\">December 2017 (Vol. 313 Issue 6 Page F1179-F1280)</a>" + 
      "<a href=\"/toc/jid/313/5\">November 2017 (Vol. 313 Issue 5 Page F1061-F1178)</a>" + 
      "<a href=\"/toc/jid/313/4\">October 2017 (Vol. 313 Issue 4 Page F835-F1060)</a>" + 
      "<a href=\"/toc/jid/313/3\">September 2017 (Vol. 313 Issue 3 Page F561-F728)</a>" + 
      "<a href=\"/toc/jid/313/2\">August 2017 (Vol. 313 Issue 2 Page F135-F560)</a>" + 
      "<a href=\"/toc/jid/313/1\">July 2017 (Vol. 313 Issue 1 Page F1-F61)</a>";
*/
  
  private static final String manifestHashFiltered = 
      " December 2017 (Vol. 313 Issue 6 Page F1179-F1280)" + 
      " November 2017 (Vol. 313 Issue 5 Page F1061-F1178)" + 
      " October 2017 (Vol. 313 Issue 4 Page F835-F1060)" + 
      " September 2017 (Vol. 313 Issue 3 Page F561-F728)" + 
      " August 2017 (Vol. 313 Issue 2 Page F135-F560)" + 
      " July 2017 (Vol. 313 Issue 1 Page F1-F61)" +
      " ";
  
  private static final String tocContent = 
      "<html class=\"pb-page\" data-request-id=\"9b091ab2-2ab5-4d31-bed7-31d90503fe85\" lang=\"en\">\n" +
      "<head data-pb-dropzone=\"head\">...head stuff...</head>" +
      "<title>Journal Name: Vol 313, No 1</title>\n" +
      "<body class=\"pb-ui\">\n" +
      "<div class=\"base\" data-db-parent-of=\"sb1\">\n" + 
      "<header class=\"header \">\n header stuff \n</header>\n" + 
      " <nav class=\"article__breadcrumbs\"><a href=\"/\" class=\"article__tocHeading\">Physiology.org</a>" +
      " <a href=\"/journal/jid\" class=\"article__tocHeading\">Journal Name</a>" +
      " <a href=\"/toc/jid/313/1\" class=\"article__tocHeading separator\">Vol. 313, No. 1</a>" +
      " </nav>\n" + 
      "</div>\n" +
      "<main class=\"content\">\n" +
      "main\n" +
      "<div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"e81613f4-f742-47c6-9a72-83da91c190d3\" class=\"publication_header\">\n" + 
      "        <div class=\"overlay\">\n" + 
      "        <div class=\"container\"><div class=\"row\"><div>\n" + 
      "        <div class=\"container\"><div class=\"row\"><div class=\"logo-container\">" +
      "<img src=\"/pb-assets/images/jid_logo.svg\" alt=\"Journal Name Logo\">" +
      "</div></div></div>\n" + 
      "</div></div></div>\n" + 
      "</div>\n" + 
      "</div>" +
      "<div class=\"container\"><div class=\"row\"><div class=\"publication_container clearfix\">\n" +
      "<div class=\"publication-menu hidden-xs hidden-sm container\">" +
      "<div class=\"row\">" +
      "\n" + 
      "</div></div>" +
      "<div class=\"container\">" +
      "<div class=\"row\">" +
      "<div class=\"xs-space\">\n" + 
      "<div class=\"col-sm-8 toc-left-side\">\n" + 
      "<div class=\"toc_content\">\n" +
      "" +
      "<div class=\"col-md-2\">\n" +
      "<nav class=\"toc__section\">\n" +
      "<div id=\"sections\" class=\"article-sections makeSticky\" style=\"bottom: initial; top: 132.6px;\">" +
      "<ul class=\"sections__drop rlist separator\">\n" +
      "<li role=\"menuitem\" class=\"\"><a class=\"w-slide__hide\" href=\"#d240875e75\"><span>Research Article</span></a></li>\n" +
      "<li role=\"menuitem\"><a class=\"w-slide__hide\" href=\"#d240875e3145\"><span>Review</span></a></li>\n" +
      "</ul></div>\n" +
      "</nav>\n" +
      "</div>\n" +
      "<div class=\"col-md-10 toc-separator\">\n" + 
      "   <div class=\"table-of-content\">\n" + 
      "      <h2 class=\"toc__heading section__header to-section\" id=\"d240875e75\">Research Article</h2>\n" + 
      "      <div class=\"issue-item\">\n" + 
      "         <div class=\"badges\"><span class=\"access__icon icon-lock_open\"></span><span class=\"access__type\">Full Access</span></div>\n" + 
      "         <h6 class=\"toc__heading sub-heading\"></h6>\n" + 
      "         <div class=\"toc-item__main\">\n" + 
      "            <div class=\"article-meta\">\n" + 
      "               <h4 class=\"issue-item__title\"><a href=\"/doi/full/10.9999/jid.0001.2016\">Intrarenal signaling</a></h4>\n" + 
      "               <ul class=\"rlist--inline loa\" aria-label=\"authors\" style=\"float: none; position: static;\" title=\"<li><span class=&quot;author-name&quot;>A Author</span></li>\">\n" + 
      "                  <li><span class=\"author-name\">A Author</span></li>\n" + 
      "               </ul>\n" + 
      "               <ul class=\"toc-item__detail\">\n" + 
      "                  <li class=\"toc-pubdate\"><span tabindex=\"0\"></span><span tabindex=\"0\" class=\"date\">2017 Jul 01</span></li>\n" + 
      "                  <li class=\"toc-pagerange\"><span tabindex=\"0\">: </span><span tabindex=\"0\">F20-F29</span></li>\n" + 
      "               </ul>\n" + 
      "               <p class=\"epub-section__item\"><a href=\"https://doi.org/10.9999/jid.0001.2016\">https://doi.org/10.9999/jid.0001.2016</a></p>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"toc-item__footer\">\n" + 
      "            <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "               <li><a title=\"Abstract\" href=\"/doi/abs/10.9999/jid.0001.2016\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "               <li><a title=\"Full text\" href=\"/doi/full/10.9999/jid.0001.2016\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "               <li><a title=\"PDF\" href=\"/doi/pdf/10.9999/jid.0001.2016\"><span>PDF</span><i class=\"icon icon-PDF\"></i></a></li>\n" + 
      "               <li><a title=\"References\" href=\"/doi/references/10.9999/jid.0001.2016\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "               <li><a class=\"rightslink\" href=\"/servlet/linkout?type=rightslink&amp;url=startPage\"><span>Permissions</span><i class=\"icon icon-permission\"></i></a></li>\n" + 
      "            </ul>\n" + 
      "            <div class=\"accordion accordion__box\">\n" + 
      "               <a class=\"accordion__control\" href=\"#\" title=\"Preview Abstract\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i><span>Preview Abstract</span></a>\n" + 
      "               <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">\n" + 
      "                  <p>Text...</p>\n" + 
      "               </div>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "      </div>\n" + 
      "      <h2 class=\"toc__heading section__header to-section\" id=\"d240875e3145\">Review</h2>\n" + 
      "      <div class=\"issue-item\">\n" + 
      "         <div class=\"badges\"><span class=\"access__icon icon-lock_open\"></span><span class=\"access__type\">Full Access</span></div>\n" + 
      "         <h6 class=\"toc__heading sub-heading\"></h6>\n" + 
      "         <div class=\"toc-item__main\">\n" + 
      "            <div class=\"article-meta\">\n" + 
      "               <h4 class=\"issue-item__title\"><a href=\"/doi/full/10.9999/jid.0001.2016\">Saving the sweetness</a></h4>\n" + 
      "               <ul class=\"rlist--inline loa\" aria-label=\"authors\" style=\"float: none; position: static;\">\n" + 
      "                  <li><span class=\"author-name\">I Author</span></li>\n" + 
      "               </ul>\n" + 
      "               <ul class=\"toc-item__detail\">\n" + 
      "                  <li class=\"toc-pubdate\"><span tabindex=\"0\"></span><span tabindex=\"0\" class=\"date\">2017 Jul 01</span></li>\n" + 
      "                  <li class=\"toc-pagerange\"><span tabindex=\"0\">: </span><span tabindex=\"0\">F55-F61</span></li>\n" + 
      "               </ul>\n" + 
      "               <p class=\"epub-section__item\"><a href=\"https://doi.org/10.9999/jid.0001.2016\">https://doi.org/10.9999/jid.0001.2016</a></p>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"toc-item__footer\">\n" + 
      "            <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "               <li><a title=\"Abstract\" href=\"/doi/abs/10.9999/jid.0001.2016\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "               <li><a title=\"Full text\" href=\"/doi/full/10.9999/jid.0001.2016\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "               <li><a title=\"PDF\" href=\"/doi/pdf/10.9999/jid.0001.2016\"><span>PDF</span><i class=\"icon icon-PDF\"></i></a></li>\n" + 
      "               <li><a title=\"References\" href=\"/doi/references/10.9999/jid.0001.2016\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "               <li><a class=\"rightslink\" href=\"/servlet/linkout?type=rightslink&amp;url=startPage\"><span>Permissions</span><i class=\"icon icon-permission\"></i></a></li>\n" + 
      "            </ul>\n" + 
      "            <div class=\"accordion accordion__box\">\n" + 
      "               <a class=\"accordion__control\" href=\"#\" title=\"Preview Abstract\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i><span>Preview Abstract</span></a>\n" + 
      "               <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">\n" + 
      "                  <p>Text...</p>\n" + 
      "               </div>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "      </div>\n" + 
      "   </div>\n" + 
      "</div>\n" +
      "</div>" +
      "</div>" +
      "</div>" +
      "</div>\n" +
      "<div class=\"col-sm-4 gray-bg toc-right-side\">\n" + 
      "</div>" +
      "</div></div></div>\n" +
      "</div></div></div>\n" +
      "</main>\n" +
      "<footer>  footer stuff</footer>" +
      "</body>\n" +
      "</html>";
  
  private static final String tocContentCrawlFiltered = 
      "<html class=\"pb-page\" data-request-id=\"9b091ab2-2ab5-4d31-bed7-31d90503fe85\" lang=\"en\">\n" +
      "<head data-pb-dropzone=\"head\">...head stuff...</head>" +
      "<title>Journal Name: Vol 313, No 1</title>\n" +
      "<body class=\"pb-ui\">\n" +
      "<div class=\"base\" data-db-parent-of=\"sb1\">\n" + 
      "\n" +
      " <nav class=\"article__breadcrumbs\"><a href=\"/\" class=\"article__tocHeading\">Physiology.org</a>" +
      " <a href=\"/journal/jid\" class=\"article__tocHeading\">Journal Name</a>" +
      " <a href=\"/toc/jid/313/1\" class=\"article__tocHeading separator\">Vol. 313, No. 1</a>" +
      " </nav>\n" + 
      "</div>\n" +
      "<main class=\"content\">\n" +
      "main\n" +
      "<div class=\"container\"><div class=\"row\"><div class=\"publication_container clearfix\">\n" + 
      "<div class=\"container\">" +
      "<div class=\"row\">" +
      "<div class=\"xs-space\">\n" + 
      "<div class=\"col-sm-8 toc-left-side\">\n" + 
      "<div class=\"toc_content\">\n" +
      "<div class=\"col-md-2\">\n" +
      "<nav class=\"toc__section\">\n" +
      "<div id=\"sections\" class=\"article-sections makeSticky\" style=\"bottom: initial; top: 132.6px;\">" +
      "<ul class=\"sections__drop rlist separator\">\n" +
      "<li role=\"menuitem\" class=\"\"><a class=\"w-slide__hide\" href=\"#d240875e75\"><span>Research Article</span></a></li>\n" +
      "<li role=\"menuitem\"><a class=\"w-slide__hide\" href=\"#d240875e3145\"><span>Review</span></a></li>\n" +
      "</ul></div>\n" +
      "</nav>\n" +
      "</div>\n" +
      "<div class=\"col-md-10 toc-separator\">\n" + 
      "   <div class=\"table-of-content\">\n" + 
      "      <h2 class=\"toc__heading section__header to-section\" id=\"d240875e75\">Research Article</h2>\n" + 
      "      <div class=\"issue-item\">\n" + 
      "         <div class=\"badges\"><span class=\"access__icon icon-lock_open\"></span><span class=\"access__type\">Full Access</span></div>\n" + 
      "         <h6 class=\"toc__heading sub-heading\"></h6>\n" + 
      "         <div class=\"toc-item__main\">\n" + 
      "            <div class=\"article-meta\">\n" + 
      "               <h4 class=\"issue-item__title\"><a href=\"/doi/full/10.9999/jid.0001.2016\">Intrarenal signaling</a></h4>\n" + 
      "               <ul class=\"rlist--inline loa\" aria-label=\"authors\" style=\"float: none; position: static;\" title=\"<li><span class=&quot;author-name&quot;>A Author</span></li>\">\n" + 
      "                  <li><span class=\"author-name\">A Author</span></li>\n" + 
      "               </ul>\n" + 
      "               <ul class=\"toc-item__detail\">\n" + 
      "                  <li class=\"toc-pubdate\"><span tabindex=\"0\"></span><span tabindex=\"0\" class=\"date\">2017 Jul 01</span></li>\n" + 
      "                  <li class=\"toc-pagerange\"><span tabindex=\"0\">: </span><span tabindex=\"0\">F20-F29</span></li>\n" + 
      "               </ul>\n" + 
      "               <p class=\"epub-section__item\"><a href=\"https://doi.org/10.9999/jid.0001.2016\">https://doi.org/10.9999/jid.0001.2016</a></p>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"toc-item__footer\">\n" + 
      "            <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "               <li><a title=\"Abstract\" href=\"/doi/abs/10.9999/jid.0001.2016\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "               <li><a title=\"Full text\" href=\"/doi/full/10.9999/jid.0001.2016\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "               <li><a title=\"PDF\" href=\"/doi/pdf/10.9999/jid.0001.2016\"><span>PDF</span><i class=\"icon icon-PDF\"></i></a></li>\n" + 
      "               <li><a title=\"References\" href=\"/doi/references/10.9999/jid.0001.2016\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "               <li></li>\n" + 
      "            </ul>\n" + 
      "            <div class=\"accordion accordion__box\">\n" + 
      "               <a class=\"accordion__control\" href=\"#\" title=\"Preview Abstract\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i><span>Preview Abstract</span></a>\n" + 
      "               \n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "      </div>\n" + 
      "      <h2 class=\"toc__heading section__header to-section\" id=\"d240875e3145\">Review</h2>\n" + 
      "      <div class=\"issue-item\">\n" + 
      "         <div class=\"badges\"><span class=\"access__icon icon-lock_open\"></span><span class=\"access__type\">Full Access</span></div>\n" + 
      "         <h6 class=\"toc__heading sub-heading\"></h6>\n" + 
      "         <div class=\"toc-item__main\">\n" + 
      "            <div class=\"article-meta\">\n" + 
      "               <h4 class=\"issue-item__title\"><a href=\"/doi/full/10.9999/jid.0001.2016\">Saving the sweetness</a></h4>\n" + 
      "               <ul class=\"rlist--inline loa\" aria-label=\"authors\" style=\"float: none; position: static;\">\n" + 
      "                  <li><span class=\"author-name\">I Author</span></li>\n" + 
      "               </ul>\n" + 
      "               <ul class=\"toc-item__detail\">\n" + 
      "                  <li class=\"toc-pubdate\"><span tabindex=\"0\"></span><span tabindex=\"0\" class=\"date\">2017 Jul 01</span></li>\n" + 
      "                  <li class=\"toc-pagerange\"><span tabindex=\"0\">: </span><span tabindex=\"0\">F55-F61</span></li>\n" + 
      "               </ul>\n" + 
      "               <p class=\"epub-section__item\"><a href=\"https://doi.org/10.9999/jid.0001.2016\">https://doi.org/10.9999/jid.0001.2016</a></p>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"toc-item__footer\">\n" + 
      "            <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "               <li><a title=\"Abstract\" href=\"/doi/abs/10.9999/jid.0001.2016\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "               <li><a title=\"Full text\" href=\"/doi/full/10.9999/jid.0001.2016\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "               <li><a title=\"PDF\" href=\"/doi/pdf/10.9999/jid.0001.2016\"><span>PDF</span><i class=\"icon icon-PDF\"></i></a></li>\n" + 
      "               <li><a title=\"References\" href=\"/doi/references/10.9999/jid.0001.2016\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "               <li></li>\n" + 
      "            </ul>\n" + 
      "            <div class=\"accordion accordion__box\">\n" + 
      "               <a class=\"accordion__control\" href=\"#\" title=\"Preview Abstract\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i><span>Preview Abstract</span></a>\n" + 
      "               \n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "      </div>\n" + 
      "   </div>\n" + 
      "</div>\n" +
      "</div></div></div></div>\n" +
      "<div class=\"col-sm-4 gray-bg toc-right-side\">\n" + 
      "</div>" +
      "</div></div></div>\n" +
      "</div></div></div>\n" + 
      "</main>\n" +
      "</body>\n" +
      "</html>";
  
  /*
  private static final String tocContentHashFiltered = 
      "<div class=\"toc_content\">\n" +
      "<div class=\"col-md-2\">\n" +
      "\n" +
      "</div>\n" +
      "<div class=\"col-md-10 toc-separator\">\n" + 
      "   <div class=\"table-of-content\">\n" + 
      "      <h2 class=\"toc__heading section__header to-section\" >Research Article</h2>\n" + 
      "      <div class=\"issue-item\">\n" + 
      "         \n" + 
      "         <h6 class=\"toc__heading sub-heading\"></h6>\n" + 
      "         <div class=\"toc-item__main\">\n" + 
      "            <div class=\"article-meta\">\n" + 
      "               <h4 class=\"issue-item__title\"><a href=\"/doi/full/10.9999/jid.0001.2016\">Intrarenal signaling</a></h4>\n" + 
      "               <ul class=\"rlist--inline loa\" aria-label=\"authors\" style=\"float: none; position: static;\" title=\"<li><span class=&quot;author-name&quot;>A Author</span></li>\">\n" + 
      "                  <li><span class=\"author-name\">A Author</span></li>\n" + 
      "               </ul>\n" + 
      "               <ul class=\"toc-item__detail\">\n" + 
      "                  <li class=\"toc-pubdate\"><span tabindex=\"0\"></span><span tabindex=\"0\" class=\"date\">2017 Jul 01</span></li>\n" + 
      "                  <li class=\"toc-pagerange\"><span tabindex=\"0\">: </span><span tabindex=\"0\">F20-F29</span></li>\n" + 
      "               </ul>\n" + 
      "               <p class=\"epub-section__item\"><a href=\"https://doi.org/10.9999/jid.0001.2016\">https://doi.org/10.9999/jid.0001.2016</a></p>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"toc-item__footer\">\n" + 
      "            <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "               <li><a title=\"Abstract\" href=\"/doi/abs/10.9999/jid.0001.2016\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "               <li><a title=\"Full text\" href=\"/doi/full/10.9999/jid.0001.2016\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "               <li><a title=\"PDF\" href=\"/doi/pdf/10.9999/jid.0001.2016\"><span>PDF</span><i class=\"icon icon-PDF\"></i></a></li>\n" + 
      "               <li><a title=\"References\" href=\"/doi/references/10.9999/jid.0001.2016\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "               <li></li>\n" + 
      "            </ul>\n" + 
      "            <div class=\"accordion accordion__box\">\n" + 
      "               <a class=\"accordion__control\" href=\"#\" title=\"Preview Abstract\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i><span>Preview Abstract</span></a>\n" + 
      "               <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">\n" + 
      "                  <p>Text...</p>\n" + 
      "               </div>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "      </div>\n" + 
      "      <h2 class=\"toc__heading section__header to-section\" >Review</h2>\n" + 
      "      <div class=\"issue-item\">\n" + 
      "         \n" + 
      "         <h6 class=\"toc__heading sub-heading\"></h6>\n" + 
      "         <div class=\"toc-item__main\">\n" + 
      "            <div class=\"article-meta\">\n" + 
      "               <h4 class=\"issue-item__title\"><a href=\"/doi/full/10.9999/jid.0001.2016\">Saving the sweetness</a></h4>\n" + 
      "               <ul class=\"rlist--inline loa\" aria-label=\"authors\" style=\"float: none; position: static;\">\n" + 
      "                  <li><span class=\"author-name\">I Author</span></li>\n" + 
      "               </ul>\n" + 
      "               <ul class=\"toc-item__detail\">\n" + 
      "                  <li class=\"toc-pubdate\"><span tabindex=\"0\"></span><span tabindex=\"0\" class=\"date\">2017 Jul 01</span></li>\n" + 
      "                  <li class=\"toc-pagerange\"><span tabindex=\"0\">: </span><span tabindex=\"0\">F55-F61</span></li>\n" + 
      "               </ul>\n" + 
      "               <p class=\"epub-section__item\"><a href=\"https://doi.org/10.9999/jid.0001.2016\">https://doi.org/10.9999/jid.0001.2016</a></p>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"toc-item__footer\">\n" + 
      "            <ul class=\"rlist--inline separator toc-item__detail\">\n" + 
      "               <li><a title=\"Abstract\" href=\"/doi/abs/10.9999/jid.0001.2016\"><span>Abstract</span><i class=\"icon icon-abstract\"></i></a></li>\n" + 
      "               <li><a title=\"Full text\" href=\"/doi/full/10.9999/jid.0001.2016\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" + 
      "               <li><a title=\"PDF\" href=\"/doi/pdf/10.9999/jid.0001.2016\"><span>PDF</span><i class=\"icon icon-PDF\"></i></a></li>\n" + 
      "               <li><a title=\"References\" href=\"/doi/references/10.9999/jid.0001.2016\"><span>References</span><i class=\"icon icon-Icon_Links-References\"></i></a></li>\n" + 
      "               <li></li>\n" + 
      "            </ul>\n" + 
      "            <div class=\"accordion accordion__box\">\n" + 
      "               <a class=\"accordion__control\" href=\"#\" title=\"Preview Abstract\" aria-expanded=\"false\"><i class=\"icon-section_arrow_d\"></i><span>Preview Abstract</span></a>\n" + 
      "               <div class=\"accordion__content toc-item__abstract\" style=\"display: none;\">\n" + 
      "                  <p>Text...</p>\n" + 
      "               </div>\n" + 
      "            </div>\n" + 
      "         </div>\n" + 
      "      </div>\n" + 
      "   </div>\n" + 
      "</div>\n" +
      "</div>";
   */
  
  private static final String tocContentHashFiltered = 
      " Research Article" + 
      " Intrarenal signaling" + 
      " A Author" + 
      " 2017 Jul 01" + 
      " : F20-F29" + 
      " https://doi.org/10.9999/jid.0001.2016" + 
      " Abstract" + 
      " Full text" + 
      " PDF" + 
      " References" + 
      " Preview Abstract" + 
      " Text..." + 
      " Review" + 
      " Saving the sweetness" + 
      " I Author" + 
      " 2017 Jul 01" + 
      " : F55-F61" + 
      " https://doi.org/10.9999/jid.0001.2016" + 
      " Abstract" + 
      " Full text" + 
      " PDF" + 
      " References" + 
      " Preview Abstract" + 
      " Text..." + 
      " ";
  
  private static final String art1Content = 
      "<html lang=\"en\" class=\"pb-page\" >\n" + 
      "   <head data-pb-dropzone=\"head\">\n" + 
      "      <meta name=\"citation_journal_title\" content=\"Journal of Neurophysiology\" />\n" + 
      "   </head>\n" + 
      "   <body class=\"pb-ui\">\n" + 
      "   <div id=\"pb-page-content\" data-ng-non-bindable>\n" + 
      "   <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "   <div class=\"base\" data-db-parent-of=\"sb1\">\n" + 
      "   <header class=\"header fixed base pageHeader\">\n" + 
      "   <div class=\"popup login-popup hidden\">\n" + 
      "   <a href=\"#\" class=\"close\"><i class=\"icon-close_thin\"></i></a>\n" + 
      "   </div>\n" + 
      "   </header>\n" + 
      "   </div>\n" + 
      "   <main class=\"content jn pageBody\">\n" + 
      "   <div>\n" + 
      "   <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"id\" class=\"publication_header\">\n" + 
      "   <div class=\"overlay\">\n" + 
      "   </div>\n" + 
      "   </div>\n" + 
      "   <div class=\"container\">\n" + 
      "   <div class=\"row\">\n" + 
      "   <div class=\"publication_container clearfix\">\n" + 
      "  <div class=\"publication-menu hidden-xs hidden-sm container\">\n" + 
      "     <div class=\"row\">\n" + 
      "     </div>\n" + 
      "  </div>\n" + 
      "  <article data-figures=\"http://www.physiology.org/action/ajaxShowFigures?doi=10.1152%2Fjn.00002.2017&amp;ajax=true\" data-references=\"http://www.physiology.org/action/ajaxShowEnhancedAbstract?doi=10.1152%2Fjn.00002.2017&amp;ajax=true\" data-enable-mathjax=\"true\" class=\"container\">\n" + 
      "     <div class=\"row\">\n" + 
      "     <div class=\"col-sm-8 col-md-8 article__content\">\n" + 
      "     <div class=\"citation\">\n" + 
      "     <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><i class=\"citation__access__icon icon-lock_open\"></i></span><span class=\"article__breadcrumbs\"><span class=\"citation__top__item article__tocHeading\">Research Article</span><span class=\"citation__top__item article__tocHeading\">Control of Homeostasis</span></span></div>\n" + 
      "     <h1 class=\"citation__title\">Cit Title</h1>\n" + 
      "     <ul class=\"rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" + 
      "       <li><a href=\"#\" title=\"Author\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author</span></a></li>\n" + 
      "     </ul>\n" + 
      "     <div class=\"loa-wrapper hidden-xs\">\n" + 
      "     <div id=\"sb-1\" class=\"accordion\">\n" + 
      "     <div class=\"accordion-tabbed loa-accordion\">\n" + 
      "     <div class=\"accordion-tabbed__tab-mobile \">\n" + 
      " <a href=\"#\" data-id=\"a4\" data-db-target-for=\"a4\" title=\"Author\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author</span><i aria-hidden=\"true\" class=\"icon-Email\"></i><i aria-hidden=\"true\" class=\"icon-arrow_d_n\"></i></a>\n" + 
      " <div data-db-target-of=\"a4\" class=\"author-info accordion-tabbed__content\">\n" + 
      "    <p class=\"author-type\"></p>\n" + 
      "    <p></p>\n" + 
      "    <p>Department of Biology</p>\n" + 
      "    <div class=\"bottom-info\">\n" + 
      "       <p><a href=\"/author/Author\">\n" + 
      "       Search for more papers by this author\n" + 
      "       </a>\n" + 
      "       </p>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " <div class=\"epub-section\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">Published Online:</span><span class=\"epub-section__date\">1 Jun 2017</span></span><span class=\"epub-section__item\"><a href=\"https://doi.org/10.1152/jn.00002.2017\" class=\"epub-section__doi__text\">https://doi.org/10.1152/jn.00002.2017</a></span></div>\n" + 
      " <div>\n" + 
      "              <ul class=\"rlist--inline\"><li class=\"correction\"></li><li class=\"corrections single\"></li><li class=\"latest-version no-border\"><div class=\"versions-ctrl\"><a href=\"http://www.physiology.org/doi/prev/20170728-aop/abs/10.1152/physiolgenomics.00060.2017\">This is the final version - click for previous version</a></div></li></ul>\n" + 
      " </div>\n" + 
      " <!--+articleCoolbar()-->\n" + 
      " <nav class=\"stickybar coolBar trans\">\n" + 
      " <div class=\"stickybar__wrapper coolBar__wrapper clearfix\">\n" + 
      " <div class=\"rlist coolBar__zone\">\n" + 
      "    <!--div.coolBar__section//| pb.renderDropzone(thisWidget, 'coolbarDropZone1')--><a href=\"#\" data-db-target-for=\"article\" data-db-switch=\"icon-close_thin\" data-slide-target=\"#articleMenu\" class=\"coolBar__ctrl hidden-md hidden-lg w-slide__btn\"><i aria-hidden=\"true\" class=\"icon-Icon_About-Article\"></i><span>More</span></a>\n" + 
      "    <div data-db-target-of=\"article\" id=\"articleMenu\" class=\"coolBar__drop fixed rlist\">\n" + 
      "       <div data-target=\"article .tab .tab__nav, .coolBar--download .coolBar__drop\" data-remove=\"false\" data-target-class=\"hidden-xs hidden-sm\" data-toggle=\"transplant\" data-direction=\"from\" data-transplant=\"self\" class=\"transplant showit\"></div>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " <ul data-cb-group=\"Article\" data-cb-group-icon=\"icon-toc\" class=\"rlist coolBar__first\">\n" + 
      "    <li class=\"coolBar__section coolBar--sections article-top-section\">\n" + 
      "       <a href=\"#\" data-db-target-for=\"sections\" data-db-switch=\"icon-close_thin\" title=\"Sections\" data-slide-target=\"#sectionMenu\" class=\"coolBar__ctrl\"><i aria-hidden=\"true\" class=\"icon-Icon_Section-menu\"></i><span>Sections</span></a>\n" + 
      "       <div data-db-target-of=\"sections\" id=\"sectionMenu\" class=\"coolBar__drop rlist\"></div>\n" + 
      "    </li>\n" + 
      " </ul>\n" + 
      " <ul class=\"coolBar__second rlist\">\n" + 
      "    <li class=\"coolBar__section coolBar--download hidden-xs hidden-sm\">\n" + 
      "       <a data-db-target-for=\"pdfLinks\" title=\"PDF\" href=\"#\" aria-haspopup=\"true\" aria-controls=\"download_Pop\" role=\"button\" id=\"download_Ctrl\" class=\"coolBar__ctrl\"><i class=\"icon icon-PDF red_icon\"></i><span>PDF (472 KB)</span></a>\n" + 
      "       <ul data-db-target-of=\"pdfLinks\" aria-labelledby=\"articleToolsCtrl\" role=\"menu\" class=\"coolBar__drop rlist w-slide--list hidden-xs hidden-sm\">\n" + 
      " <li><a href=\"http://www.physiology.org/doi/pdf/10.1152/jn.00002.2017\" role=\"menuitem\" target=\"_blank\">Download PDF</a></li>\n" + 
      "       </ul>\n" + 
      "    </li>\n" + 
      " </ul>\n" + 
      "    </div>\n" + 
      " </nav>\n" + 
      " <div>\n" + 
      " <div class=\"article__body row\">\n" + 
      " <div class=\"col-md-12\">\n" + 
      "    <p class=\"fulltext\"></p>\n" + 
      "    <!--abstract content-->\n" + 
      "    <div class=\"hlFld-Abstract\">\n" + 
      "       <p class=\"fulltext\"></p>\n" + 
      "       <h2 class=\"article-section__title section__title\" id=\"d2276776e1\">Abstract</h2>\n" + 
      "       <div class=\"abstractSection abstractInFull\">\n" + 
      " <p>Text...</p>\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      "    <!--/abstract content--><!--fulltext content-->\n" + 
      "    <div class=\"hlFld-Fulltext\">\n" + 
      "       <p>Text...</p>\n" + 
      "       <h1 class=\"article-section__title section__title\" id=\"_i1\">MATERIALS AND METHODS</h1>\n" + 
      "       <p>Text...</p>\n" + 
      "       <figure data-figure-id=\"F0001\" id=\"F0001\" class=\"figure\">\n" + 
      " <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/medium/z9k0061741260001.gif\" data-lg-src=\"/na101/home/literatum/publisher/images/large/z9k0061741260001.jpeg\" alt=\"Fig. 1.\" />\n" + 
      " <figcaption>\n" + 
      "    <strong class=\"figure__title\"></strong>\n" + 
      "    <span class=\"figure__caption\">\n" + 
      "       <p>figure caption...</p>\n" + 
      "    </span>\n" + 
      " </figcaption>\n" + 
      "       </figure>\n" + 
      "       <div class=\"figure-extra\"><a alt=\"figure\" href=\"/na101/home/literatum/publisher/images/large/z9k0061741260001.jpeg\" download=\"\">Download\n" + 
      "   figure\n" + 
      " </a><a href=\"/action/downloadFigures?id=F0001&amp;doi=10.1152/jn.00002.2017\" class=\"ppt-figure-link\">Download PowerPoint</a>\n" + 
      "       </div>\n" + 
      "       <br /> \n" + 
      "       <div id=\"T1\" class=\"anchor-spacer\"></div>\n" + 
      "       <div class=\"article-table-content\" id=\"T1\">\n" + 
      " <table class=\"table article-section__table\">\n" + 
      "    <caption>\n" + 
      "       <strong>\n" + 
      "          <p><span class=\"captionLabel\">Table 1.</span> Caption...</p>\n" + 
      "       </strong>\n" + 
      "    </caption>\n" + 
      " </table>\n" + 
      " <div class=\"tableFooter\">\n" + 
      "    <div class=\"footnote\">\n" + 
      "       <p>Text...</p>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      "       </div>\n" + 
      "       <p>Text...</p>\n" + 
      "       <div id=\"_i7\" class=\"anchor-spacer\"></div>\n" + 
      "       <h3 class=\"article-section__title\" id=\"_i7\">Data analysis.</h3>\n" + 
      "       <p>Text...</p>\n" + 
      "       <figure data-figure-id=\"F0002\" id=\"F0002\" class=\"figure\">\n" + 
      " <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/images/medium/z9k0061741260002.gif\" data-lg-src=\"/na101/home/literatum/publisher/images/large/z9k0061741260002.jpeg\" alt=\"Fig. 2.\" />\n" + 
      " <figcaption>\n" + 
      "    <strong class=\"figure__title\"></strong>\n" + 
      "    <span class=\"figure__caption\">\n" + 
      "       <p>Text...</p>\n" + 
      "    </span>\n" + 
      " </figcaption>\n" + 
      "       </figure>\n" + 
      "       <div class=\"figure-extra\"><a alt=\"figure\" href=\"/na101/home/literatum/publisher/images/large/z9k0061741260002.jpeg\" download=\"\">Download\n" + 
      "   figure\n" + 
      " </a><a href=\"/action/downloadFigures?id=F0002&amp;doi=10.1152/jn.00002.2017\" class=\"ppt-figure-link\">Download PowerPoint</a>\n" + 
      "       </div>\n" + 
      "       <br /> \n" + 
      "       <p>Text...</p>\n" + 
      "       <div class=\"ack\">\n" + 
      " <h2 class=\"article-section__title section__title\" id=\"_i36\">ACKNOWLEDGMENTS</h2>\n" + 
      " <p>Text...</p>\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      "    <!--/fulltext content-->\n" + 
      "    <p class=\"fulltext\"></p>\n" + 
      "    <h2 class=\"article-section__title section__title\" id=\"d2276776e177\">AUTHOR NOTES</h2>\n" + 
      "    <h2 class=\"article-section__title section__title\" id=\"d2276776e1s\">Supplemental data</h2>\n" + 
      "    <ul>\n" + 
      "       <li><a href=\"/doi/suppl/10.1152/jn.00002.2017/suppl_file/supplemental+table+1.xls\">supplemental table 1.xls (890.5 kb)</a></li>\n" + 
      "    </ul>\n" + 
      "    <div class=\"response\">\n" + 
      "       <div class=\"sub-article-title\"></div>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " <div class=\"content-navigation clearfix\">\n" + 
      " <a href=\"/doi/10.1152/jn.00860.2016\" title=\"Previous\" class=\"content-navigation__btn--pre\"><i class=\"icon-arrow_l\"></i><span>Previous</span></a>\n" + 
      " <div class=\"content-navigation__extra\">\n" + 
      " <a href=\"#\" title=\"Back to Top\" class=\"content-navigation__btn-back\">Back to Top</a>\n" + 
      " </div>\n" + 
      " <a href=\"/doi/10.1152/jn.00756.2016\" title=\"Next\" class=\"content-navigation__btn--next\"><span>Next</span><i class=\"icon-arrow_r\"></i></a>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " <div class=\"col-sm-4 hidden-xs hidden-sm sticko__parent article-row-right gutterless\">\n" + 
      " <!--+articleTab()-->\n" + 
      " <div class=\"tab tab--slide tab--flex sticko__md tabs--xs dynamic-sticko  tab--flex tabs--xs\">\n" + 
      " <ul data-mobile-toggle=\"slide\" class=\"rlist tab__nav w-slide--list\">\n" + 
      " <li role=\"presentation\"><a href=\"#pane-pcw-figures\" aria-controls=\"#pane-pcw-figures\" role=\"tab\" data-toggle=\"tab\" title=\"figures\" id=\"pane-pcw-figurescon\" data-slide-target=\"#pane-pcw-figures\" class=\"figures-tab\"><i aria-hidden=\"true\" class=\"icon-Icon_Images\"></i><span>Figures</span></a></li>\n" + 
      " <li role=\"presentation\"><a href=\"#pane-pcw-references\" aria-controls=\"#pane-pcw-references\" role=\"tab\" data-toggle=\"tab\" title=\"references\" id=\"pane-pcw-referencescon\" data-slide-target=\"#pane-pcw-references\" class=\"references-tab\"><i aria-hidden=\"true\" class=\"icon-Icon_Links-References\"></i><span>References</span></a></li>\n" + 
      " <li role=\"presentation\"><a href=\"#pane-pcw-related\" aria-controls=\"#pane-pcw-related\" role=\"tab\" data-toggle=\"tab\" title=\"related\" id=\"pane-pcw-relatedcon\" data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><i aria-hidden=\"true\" class=\"icon-related2\"></i><span>Related</span></a></li>\n" + 
      " <li role=\"presentation\"><a href=\"#pane-pcw-details\" aria-controls=\"#pane-pcw-details\" role=\"tab\" data-toggle=\"tab\" title=\"details\" id=\"pane-pcw-detailscon\" data-slide-target=\"#pane-pcw-details\" class=\"details-tab\"><i aria-hidden=\"true\" class=\"icon-Icon_Information\"></i><span>Information</span></a></li>\n" + 
      " </ul>\n" + 
      " <ul class=\"rlist tab__content sticko__child scroll-to-target\">\n" + 
      " <li id=\"pane-pcw-figures\" aria-labelledby=\"pane-pcw-figurescon\" role=\"tabpanel\" class=\"tab__pane\"></li>\n" + 
      " <li id=\"pane-pcw-references\" aria-labelledby=\"pane-pcw-referencescon\" role=\"tabpanel\" class=\"tab__pane\"></li>\n" + 
      " <li id=\"pane-pcw-related\" aria-labelledby=\"pane-pcw-relatedcon\" role=\"tabpanel\" class=\"tab__pane tab__pane--clear\">\n" + 
      " <div class=\"accordion\">\n" + 
      " <ul class=\"accordion-tabbed rlist\">\n" + 
      " <li class=\"accordion-tabbed__tab js--open\">\n" + 
      "    <a href=\"#\" title=\"Similar\" aria-expanded=\"false\" aria-controls=\"relatedTab3\" class=\"accordion-tabbed__control\">Recommended</a>\n" + 
      "    <div id=\"relatedTab3\" class=\"accordion-tabbed__content\">\n" + 
      "    <ul class=\"rlist lot\">\n" + 
      "     <li class=\"grid-item\">\n" + 
      "     <div class=\"creative-work\">\n" + 
      "     <div class=\"delayLoad\">\n" + 
      "     <a href=\"/doi/full/10.1152/jn.00499.2006\" title=\"Genetic Modifications\">\n" + 
      "     <h5 class=\"creative-work__title\">Genetic Modifications</h5>\n" + 
      "     </a>\n" + 
      "     <div class=\"meta\"><i class=\"icon-date\"></i><span class=\"publication-time\"><time datetime=\"November 2006\">November 2006</time></span><span class=\"publication-journal-title\"><a href=\"/journal/jn\">Journal of Neurophysiology</a></span></div>\n" + 
      "     </div>\n" + 
      "     </div>\n" + 
      "     </li>\n" + 
      "     </ul>\n" + 
      "     <div class=\"card\">\n" + 
      "     <h3><a href=\"/toc/jn/117/6\">More from this issue ></a></h3>\n" + 
      "     </div>\n" + 
      "    </div>\n" + 
      " </li>\n" + 
      " </ul>\n" + 
      " </div>\n" + 
      " </li>\n" + 
      " <li id=\"pane-pcw-details\" aria-labelledby=\"pane-pcw-detailscon\" role=\"tabpanel\" class=\"tab__pane\">\n" + 
      "    <div data-widget-def=\"graphQueryWidget\" data-widget-id=\"id\" class=\"cover-image\">\n" + 
      "       <!-- - var onePageArticle = pageRange.includes(\"-\")--><!-- Determine whether this article is not part of a regular issue-->\n" + 
      "       <div class=\"article-cover-image\">\n" + 
      " <div class=\"cover-image\">\n" + 
      "    <div class=\"cover-image__image\"><img src=\"/na101/home/literatum/publisher/physio/.issue-6.cover.gif\" width=\"100\" height=\"130\" alt=\"Journal of Neurophysiology 117 6 cover image\"></div>\n" + 
      "    <div class=\"cover-image__details\">\n" + 
      "       <div class=\"parent-item\"> <a href=\"/toc/jn/117/6\" class=\"volume\"><span class=\"volume\">Volume 117</span><span class=\"issue\">Issue 6</span></a><span class=\"coverDate\">June 2017</span><span class=\"pages\">Pages 2125-2136</span></div>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " <hr>\n" + 
      " <a href=\"/doi/suppl/10.1152/jn.00002.2017\" class=\"suppl-info-link\">Supplemental Information</a><br>\n" + 
      " <hr>\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      "    <section class=\"section\">\n" + 
      "       <div class=\"section__body\">\n" + 
      " <strong class=\"section__title\"> Copyright & Permissions</strong>\n" + 
      " <p>Copyright 2017 the American Physiological Society</p>\n" + 
      "       </div>\n" + 
      "    </section>\n" + 
      "    <section class=\"article__keyword\">\n" + 
      "       <strong class=\"section__title\">Keywords</strong>\n" + 
      "       <div class=\"section__body\">\n" + 
      "       </div>\n" + 
      "    </section>\n" + 
      "    <h3>Metrics</h3>\n" + 
      "    <div data-widget-def=\"literatumContentItemDownloadCount\" data-widget-id=\"id\" class=\"article-downloads\">\n" + 
      "                Downloaded 15 times\n" + 
      "    </div>\n" + 
      "    <div id=\"doi_altmetric_drawer_area\">\n" + 
      "       <div data-badge-details=\"right\" data-badge-type=\"donut\" data-doi=\"10.1152/jn.00002.2017\" data-hide-no-mentions=\"true\" class=\"altmetric-embed\">\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      " </li>\n" + 
      " <li class=\"tab__spinner\"><img src=\"/pb-assets/images/spinner.gif\" id=\"spinner\" style=\"width: 100%\"/></li>\n" + 
      " </ul>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      "  </article>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </main>\n" + 
      "  <footer>\n" + 
      "  <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"id\" class=\"footer-top\">\n" + 
      "  <div class=\"container\">\n" + 
      "  <div class=\"row\">\n" + 
      "  <div class=\"col-sm-3\">\n" + 
      "  <div data-widget-def=\"ux3-general-image\" data-widget-id=\"id\" class=\"aps-logo\">\n" + 
      "     <a href=\"http://www.the-aps.org\" title=\"American Physiological Society\"><img alt=\"American Physiological Society\" src=\"/pb-assets/images/aps-logo-footer.svg\"/></a>\n" + 
      "  </div>\n" + 
      "  <div>\n" + 
      "  <div class=\"contact\">\n" + 
      "  <div class=\"contact\">\n" + 
      "  <nav class=\"contact__nav\">\n" + 
      "  <ul class=\"rlist--inline separator separated-list footer--contact__list hidden-lg hidden-md\">\n" + 
      " <li><a href=\"/contact\">Contact Us</a></li>\n" + 
      " <li><a href=\"/about\">About Us</a></li>\n" + 
      "  </ul>\n" + 
      "  </nav>\n" + 
      "  </div>\n" + 
      "     </div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  <div class=\"col-lg-9 sitemap hidden-xs hidden-sm col-sm-9\">\n" + 
      "  <div class=\" col-sm-6\">\n" + 
      "     <h6>Journals</h6>\n" + 
      "  </div>\n" + 
      "  <div class=\" col-sm-3\">\n" + 
      "     <h6>Information For</h6>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </footer>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "   </body>\n" + 
      "</html>";
  
  private static final String art1ContentCrawlFiltered = 
      "<html lang=\"en\" class=\"pb-page\" >\n" + 
      "   <head data-pb-dropzone=\"head\">\n" + 
      "      <meta name=\"citation_journal_title\" content=\"Journal of Neurophysiology\" />\n" + 
      "   </head>\n" + 
      "   <body class=\"pb-ui\">\n" + 
      "   <div id=\"pb-page-content\" data-ng-non-bindable>\n" + 
      "   <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "   <div class=\"base\" data-db-parent-of=\"sb1\">\n" + 
      "   \n" + 
      "   </div>\n" + 
      "   <main class=\"content jn pageBody\">\n" + 
      "   <div>\n" + 
      "   \n" + 
      "   <div class=\"container\">\n" + 
      "   <div class=\"row\">\n" + 
      "   <div class=\"publication_container clearfix\">\n" +
      "  \n" + 
      "  <article data-figures=\"http://www.physiology.org/action/ajaxShowFigures?doi=10.1152%2Fjn.00002.2017&amp;ajax=true\" data-references=\"http://www.physiology.org/action/ajaxShowEnhancedAbstract?doi=10.1152%2Fjn.00002.2017&amp;ajax=true\" data-enable-mathjax=\"true\" class=\"container\">\n" + 
      "     <div class=\"row\">\n" + 
      "     <div class=\"col-sm-8 col-md-8 article__content\">\n" + 
      "     <div class=\"citation\">\n" + 
      "     <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><i class=\"citation__access__icon icon-lock_open\"></i></span><span class=\"article__breadcrumbs\"><span class=\"citation__top__item article__tocHeading\">Research Article</span><span class=\"citation__top__item article__tocHeading\">Control of Homeostasis</span></span></div>\n" + 
      "     <h1 class=\"citation__title\">Cit Title</h1>\n" + 
      "     <ul class=\"rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" + 
      "       <li><a href=\"#\" title=\"Author\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author</span></a></li>\n" + 
      "     </ul>\n" + 
      "     <div class=\"loa-wrapper hidden-xs\">\n" + 
      "     <div id=\"sb-1\" class=\"accordion\">\n" + 
      "     <div class=\"accordion-tabbed loa-accordion\">\n" + 
      "     <div class=\"accordion-tabbed__tab-mobile \">\n" + 
      " <a href=\"#\" data-id=\"a4\" data-db-target-for=\"a4\" title=\"Author\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author</span><i aria-hidden=\"true\" class=\"icon-Email\"></i><i aria-hidden=\"true\" class=\"icon-arrow_d_n\"></i></a>\n" + 
      " <div data-db-target-of=\"a4\" class=\"author-info accordion-tabbed__content\">\n" + 
      "    <p class=\"author-type\"></p>\n" + 
      "    <p></p>\n" + 
      "    <p>Department of Biology</p>\n" + 
      "    <div class=\"bottom-info\">\n" + 
      "       <p>\n" + 
      "       </p>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " <div class=\"epub-section\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">Published Online:</span><span class=\"epub-section__date\">1 Jun 2017</span></span><span class=\"epub-section__item\"><a href=\"https://doi.org/10.1152/jn.00002.2017\" class=\"epub-section__doi__text\">https://doi.org/10.1152/jn.00002.2017</a></span></div>\n" + 
      " <div>\n" + 
      "              <ul class=\"rlist--inline\"></ul>\n" + 
      " </div>\n" + 
      " <!--+articleCoolbar()-->\n" + 
      " <nav class=\"stickybar coolBar trans\">\n" + 
      " <div class=\"stickybar__wrapper coolBar__wrapper clearfix\">\n" + 
      " <div class=\"rlist coolBar__zone\">\n" + 
      "    <!--div.coolBar__section//| pb.renderDropzone(thisWidget, 'coolbarDropZone1')--><a href=\"#\" data-db-target-for=\"article\" data-db-switch=\"icon-close_thin\" data-slide-target=\"#articleMenu\" class=\"coolBar__ctrl hidden-md hidden-lg w-slide__btn\"><i aria-hidden=\"true\" class=\"icon-Icon_About-Article\"></i><span>More</span></a>\n" + 
      "    <div data-db-target-of=\"article\" id=\"articleMenu\" class=\"coolBar__drop fixed rlist\">\n" + 
      "       <div data-target=\"article .tab .tab__nav, .coolBar--download .coolBar__drop\" data-remove=\"false\" data-target-class=\"hidden-xs hidden-sm\" data-toggle=\"transplant\" data-direction=\"from\" data-transplant=\"self\" class=\"transplant showit\"></div>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " <ul data-cb-group=\"Article\" data-cb-group-icon=\"icon-toc\" class=\"rlist coolBar__first\">\n" + 
      "    <li class=\"coolBar__section coolBar--sections article-top-section\">\n" + 
      "       <a href=\"#\" data-db-target-for=\"sections\" data-db-switch=\"icon-close_thin\" title=\"Sections\" data-slide-target=\"#sectionMenu\" class=\"coolBar__ctrl\"><i aria-hidden=\"true\" class=\"icon-Icon_Section-menu\"></i><span>Sections</span></a>\n" + 
      "       <div data-db-target-of=\"sections\" id=\"sectionMenu\" class=\"coolBar__drop rlist\"></div>\n" + 
      "    </li>\n" + 
      " </ul>\n" + 
      " <ul class=\"coolBar__second rlist\">\n" + 
      "    <li class=\"coolBar__section coolBar--download hidden-xs hidden-sm\">\n" + 
      "       <a data-db-target-for=\"pdfLinks\" title=\"PDF\" href=\"#\" aria-haspopup=\"true\" aria-controls=\"download_Pop\" role=\"button\" id=\"download_Ctrl\" class=\"coolBar__ctrl\"><i class=\"icon icon-PDF red_icon\"></i><span>PDF (472 KB)</span></a>\n" + 
      "       <ul data-db-target-of=\"pdfLinks\" aria-labelledby=\"articleToolsCtrl\" role=\"menu\" class=\"coolBar__drop rlist w-slide--list hidden-xs hidden-sm\">\n" + 
      " <li><a href=\"http://www.physiology.org/doi/pdf/10.1152/jn.00002.2017\" role=\"menuitem\" target=\"_blank\">Download PDF</a></li>\n" + 
      "       </ul>\n" + 
      "    </li>\n" + 
      " </ul>\n" + 
      "    </div>\n" + 
      " </nav>\n" + 
      " <div>\n" + 
      " <div class=\"article__body row\">\n" + 
      " <div class=\"col-md-12\">\n" + 
      "    <p class=\"fulltext\"></p>\n" + 
      "    <!--abstract content-->\n" + 
      "    <div class=\"hlFld-Abstract\">\n" + 
      "       <p class=\"fulltext\"></p>\n" + 
      "       <h2 class=\"article-section__title section__title\" id=\"d2276776e1\">Abstract</h2>\n" + 
      "       <div class=\"abstractSection abstractInFull\">\n" + 
      " <p>Text...</p>\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      "    <!--/abstract content--><!--fulltext content-->\n" + 
      "    <div class=\"hlFld-Fulltext\">\n" + 
      "       <p>Text...</p>\n" + 
      "       <h1 class=\"article-section__title section__title\" id=\"_i1\">MATERIALS AND METHODS</h1>\n" + 
      "       <p>Text...</p>\n" + 
      "       <figure data-figure-id=\"F0001\" id=\"F0001\" class=\"figure\">\n" + 
      " <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/medium/z9k0061741260001.gif\" data-lg-src=\"/na101/home/literatum/publisher/images/large/z9k0061741260001.jpeg\" alt=\"Fig. 1.\" />\n" + 
      " <figcaption>\n" + 
      "    <strong class=\"figure__title\"></strong>\n" + 
      "    <span class=\"figure__caption\">\n" + 
      "       <p>figure caption...</p>\n" + 
      "    </span>\n" + 
      " </figcaption>\n" + 
      "       </figure>\n" + 
      "       <div class=\"figure-extra\"><a alt=\"figure\" href=\"/na101/home/literatum/publisher/images/large/z9k0061741260001.jpeg\" download=\"\">Download\n" + 
      "   figure\n" + 
      " </a><a href=\"/action/downloadFigures?id=F0001&amp;doi=10.1152/jn.00002.2017\" class=\"ppt-figure-link\">Download PowerPoint</a>\n" + 
      "       </div>\n" + 
      "       <br /> \n" + 
      "       <div id=\"T1\" class=\"anchor-spacer\"></div>\n" + 
      "       <div class=\"article-table-content\" id=\"T1\">\n" + 
      " <table class=\"table article-section__table\">\n" + 
      "    <caption>\n" + 
      "       <strong>\n" + 
      "          <p><span class=\"captionLabel\">Table 1.</span> Caption...</p>\n" + 
      "       </strong>\n" + 
      "    </caption>\n" + 
      " </table>\n" + 
      " <div class=\"tableFooter\">\n" + 
      "    <div class=\"footnote\">\n" + 
      "       <p>Text...</p>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      "       </div>\n" + 
      "       <p>Text...</p>\n" + 
      "       <div id=\"_i7\" class=\"anchor-spacer\"></div>\n" + 
      "       <h3 class=\"article-section__title\" id=\"_i7\">Data analysis.</h3>\n" + 
      "       <p>Text...</p>\n" + 
      "       <figure data-figure-id=\"F0002\" id=\"F0002\" class=\"figure\">\n" + 
      " <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/images/medium/z9k0061741260002.gif\" data-lg-src=\"/na101/home/literatum/publisher/images/large/z9k0061741260002.jpeg\" alt=\"Fig. 2.\" />\n" + 
      " <figcaption>\n" + 
      "    <strong class=\"figure__title\"></strong>\n" + 
      "    <span class=\"figure__caption\">\n" + 
      "       <p>Text...</p>\n" + 
      "    </span>\n" + 
      " </figcaption>\n" + 
      "       </figure>\n" + 
      "       <div class=\"figure-extra\"><a alt=\"figure\" href=\"/na101/home/literatum/publisher/images/large/z9k0061741260002.jpeg\" download=\"\">Download\n" + 
      "   figure\n" + 
      " </a><a href=\"/action/downloadFigures?id=F0002&amp;doi=10.1152/jn.00002.2017\" class=\"ppt-figure-link\">Download PowerPoint</a>\n" + 
      "       </div>\n" + 
      "       <br /> \n" + 
      "       <p>Text...</p>\n" + 
      "       <div class=\"ack\">\n" + 
      " <h2 class=\"article-section__title section__title\" id=\"_i36\">ACKNOWLEDGMENTS</h2>\n" + 
      " <p>Text...</p>\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      "    <!--/fulltext content-->\n" + 
      "    <p class=\"fulltext\"></p>\n" + 
      "    <h2 class=\"article-section__title section__title\" id=\"d2276776e177\">AUTHOR NOTES</h2>\n" + 
      "    <h2 class=\"article-section__title section__title\" id=\"d2276776e1s\">Supplemental data</h2>\n" + 
      "    <ul>\n" + 
      "       <li><a href=\"/doi/suppl/10.1152/jn.00002.2017/suppl_file/supplemental+table+1.xls\">supplemental table 1.xls (890.5 kb)</a></li>\n" + 
      "    </ul>\n" + 
      "    <div class=\"response\">\n" + 
      "       <div class=\"sub-article-title\"></div>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " \n" + 
      " </div>\n" + 
      " <div class=\"col-sm-4 hidden-xs hidden-sm sticko__parent article-row-right gutterless\">\n" + 
      " <!--+articleTab()-->\n" + 
      " <div class=\"tab tab--slide tab--flex sticko__md tabs--xs dynamic-sticko  tab--flex tabs--xs\">\n" + 
      " <ul data-mobile-toggle=\"slide\" class=\"rlist tab__nav w-slide--list\">\n" + 
      " <li role=\"presentation\"><a href=\"#pane-pcw-figures\" aria-controls=\"#pane-pcw-figures\" role=\"tab\" data-toggle=\"tab\" title=\"figures\" id=\"pane-pcw-figurescon\" data-slide-target=\"#pane-pcw-figures\" class=\"figures-tab\"><i aria-hidden=\"true\" class=\"icon-Icon_Images\"></i><span>Figures</span></a></li>\n" + 
      " <li role=\"presentation\"><a href=\"#pane-pcw-references\" aria-controls=\"#pane-pcw-references\" role=\"tab\" data-toggle=\"tab\" title=\"references\" id=\"pane-pcw-referencescon\" data-slide-target=\"#pane-pcw-references\" class=\"references-tab\"><i aria-hidden=\"true\" class=\"icon-Icon_Links-References\"></i><span>References</span></a></li>\n" + 
      " <li role=\"presentation\"><a href=\"#pane-pcw-related\" aria-controls=\"#pane-pcw-related\" role=\"tab\" data-toggle=\"tab\" title=\"related\" id=\"pane-pcw-relatedcon\" data-slide-target=\"#pane-pcw-related\" class=\"related-tab\"><i aria-hidden=\"true\" class=\"icon-related2\"></i><span>Related</span></a></li>\n" + 
      " <li role=\"presentation\"><a href=\"#pane-pcw-details\" aria-controls=\"#pane-pcw-details\" role=\"tab\" data-toggle=\"tab\" title=\"details\" id=\"pane-pcw-detailscon\" data-slide-target=\"#pane-pcw-details\" class=\"details-tab\"><i aria-hidden=\"true\" class=\"icon-Icon_Information\"></i><span>Information</span></a></li>\n" + 
      " </ul>\n" + 
      " <ul class=\"rlist tab__content sticko__child scroll-to-target\">\n" + 
      " <li id=\"pane-pcw-figures\" aria-labelledby=\"pane-pcw-figurescon\" role=\"tabpanel\" class=\"tab__pane\"></li>\n" + 
      " \n" + 
      " \n" + 
      " <li id=\"pane-pcw-details\" aria-labelledby=\"pane-pcw-detailscon\" role=\"tabpanel\" class=\"tab__pane\">\n" + 
      "    <div data-widget-def=\"graphQueryWidget\" data-widget-id=\"id\" class=\"cover-image\">\n" + 
      "       <!-- - var onePageArticle = pageRange.includes(\"-\")--><!-- Determine whether this article is not part of a regular issue-->\n" + 
      "       <div class=\"article-cover-image\">\n" + 
      " <div class=\"cover-image\">\n" + 
      "    <div class=\"cover-image__image\"><img src=\"/na101/home/literatum/publisher/physio/.issue-6.cover.gif\" width=\"100\" height=\"130\" alt=\"Journal of Neurophysiology 117 6 cover image\"></div>\n" + 
      "    <div class=\"cover-image__details\">\n" + 
      "       <div class=\"parent-item\"> <a href=\"/toc/jn/117/6\" class=\"volume\"><span class=\"volume\">Volume 117</span><span class=\"issue\">Issue 6</span></a><span class=\"coverDate\">June 2017</span><span class=\"pages\">Pages 2125-2136</span></div>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " <hr>\n" + 
      " <a href=\"/doi/suppl/10.1152/jn.00002.2017\" class=\"suppl-info-link\">Supplemental Information</a><br>\n" + 
      " <hr>\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      "    <section class=\"section\">\n" + 
      "       <div class=\"section__body\">\n" + 
      " <strong class=\"section__title\"> Copyright & Permissions</strong>\n" + 
      " <p>Copyright 2017 the American Physiological Society</p>\n" + 
      "       </div>\n" + 
      "    </section>\n" + 
      "    <section class=\"article__keyword\">\n" + 
      "       <strong class=\"section__title\">Keywords</strong>\n" + 
      "       <div class=\"section__body\">\n" + 
      "       </div>\n" + 
      "    </section>\n" + 
      "    <h3>Metrics</h3>\n" + 
      "    <div data-widget-def=\"literatumContentItemDownloadCount\" data-widget-id=\"id\" class=\"article-downloads\">\n" + 
      "                Downloaded 15 times\n" + 
      "    </div>\n" + 
      "    <div id=\"doi_altmetric_drawer_area\">\n" + 
      "       <div data-badge-details=\"right\" data-badge-type=\"donut\" data-doi=\"10.1152/jn.00002.2017\" data-hide-no-mentions=\"true\" class=\"altmetric-embed\">\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      " </li>\n" + 
      " <li class=\"tab__spinner\"><img src=\"/pb-assets/images/spinner.gif\" id=\"spinner\" style=\"width: 100%\"/></li>\n" + 
      " </ul>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      "  </article>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "  </main>\n" + 
      "  \n" + 
      "  </div>\n" + 
      "  </div>\n" + 
      "   </body>\n" + 
      "</html>";
  
  /*
  private static final String art1ContentHashFiltered = 
      "<div class=\"col-sm-8 col-md-8 article__content\">\n" + 
      "     <div class=\"citation\">\n" + 
      "     <div class=\"citation__top\"><span class=\"citation__top__item article__access\"><i class=\"citation__access__icon icon-lock_open\"></i></span><span class=\"article__breadcrumbs\"><span class=\"citation__top__item article__tocHeading\">Research Article</span><span class=\"citation__top__item article__tocHeading\">Control of Homeostasis</span></span></div>\n" + 
      "     <h1 class=\"citation__title\">Cit Title</h1>\n" + 
      "     <ul class=\"rlist--inline loa mobile-authors visible-xs\" title=\"list of authors\">\n" + 
      "       <li><a href=\"#\" title=\"Author\" data-slide-target=\"#sb-1\" class=\"w-slide__btn\"><span>Author</span></a></li>\n" + 
      "     </ul>\n" + 
      "     <div class=\"loa-wrapper hidden-xs\">\n" + 
      "     <div  class=\"accordion\">\n" + 
      "     <div class=\"accordion-tabbed loa-accordion\">\n" + 
      "     <div class=\"accordion-tabbed__tab-mobile \">\n" + 
      " <a href=\"#\" data-id=\"a4\" data-db-target-for=\"a4\" title=\"Author\" class=\"author-name accordion-tabbed__control visible-x\"><span>Author</span><i aria-hidden=\"true\" class=\"icon-Email\"></i><i aria-hidden=\"true\" class=\"icon-arrow_d_n\"></i></a>\n" + 
      " \n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " <div class=\"epub-section\"><span class=\"epub-section__item\"><span class=\"epub-section__state\">Published Online:</span><span class=\"epub-section__date\">1 Jun 2017</span></span><span class=\"epub-section__item\"><a href=\"https://doi.org/10.1152/jn.00002.2017\" class=\"epub-section__doi__text\">https://doi.org/10.1152/jn.00002.2017</a></span></div>\n" + 
      " <div>\n" + 
      "              <ul class=\"rlist--inline\"></ul>\n" + 
      " </div>\n" + 
      " \n" + 
      " \n" + 
      " <div>\n" + 
      " <div class=\"article__body row\">\n" + 
      " <div class=\"col-md-12\">\n" + 
      "    <p class=\"fulltext\"></p>\n" + 
      "    \n" + 
      "    <div class=\"hlFld-Abstract\">\n" + 
      "       <p class=\"fulltext\"></p>\n" + 
      "       <h2 class=\"article-section__title section__title\" >Abstract</h2>\n" + 
      "       <div class=\"abstractSection abstractInFull\">\n" + 
      " <p>Text...</p>\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      "    \n" + 
      "    <div class=\"hlFld-Fulltext\">\n" + 
      "       <p>Text...</p>\n" + 
      "       <h1 class=\"article-section__title section__title\" >MATERIALS AND METHODS</h1>\n" + 
      "       <p>Text...</p>\n" + 
      "       <figure data-figure-id=\"F0001\"  class=\"figure\">\n" + 
      " <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/medium/z9k0061741260001.gif\" data-lg-src=\"/na101/home/literatum/publisher/images/large/z9k0061741260001.jpeg\" alt=\"Fig. 1.\" />\n" + 
      " <figcaption>\n" + 
      "    <strong class=\"figure__title\"></strong>\n" + 
      "    <span class=\"figure__caption\">\n" + 
      "       <p>figure caption...</p>\n" + 
      "    </span>\n" + 
      " </figcaption>\n" + 
      "       </figure>\n" + 
      "       <div class=\"figure-extra\"><a alt=\"figure\" href=\"/na101/home/literatum/publisher/images/large/z9k0061741260001.jpeg\" download=\"\">Download\n" + 
      "   figure\n" + 
      " </a><a href=\"/action/downloadFigures?id=F0001&amp;doi=10.1152/jn.00002.2017\" class=\"ppt-figure-link\">Download PowerPoint</a>\n" + 
      "       </div>\n" + 
      "       <br /> \n" + 
      "       <div  class=\"anchor-spacer\"></div>\n" + 
      "       <div class=\"article-table-content\" >\n" + 
      " <table class=\"table article-section__table\">\n" + 
      "    <caption>\n" + 
      "       <strong>\n" + 
      "          <p><span class=\"captionLabel\">Table 1.</span> Caption...</p>\n" + 
      "       </strong>\n" + 
      "    </caption>\n" + 
      " </table>\n" + 
      " <div class=\"tableFooter\">\n" + 
      "    <div class=\"footnote\">\n" + 
      "       <p>Text...</p>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      "       </div>\n" + 
      "       <p>Text...</p>\n" + 
      "       <div  class=\"anchor-spacer\"></div>\n" + 
      "       <h3 class=\"article-section__title\" >Data analysis.</h3>\n" + 
      "       <p>Text...</p>\n" + 
      "       <figure data-figure-id=\"F0002\"  class=\"figure\">\n" + 
      " <img class=\"figure__image\" src=\"/na101/home/literatum/publisher/images/medium/z9k0061741260002.gif\" data-lg-src=\"/na101/home/literatum/publisher/images/large/z9k0061741260002.jpeg\" alt=\"Fig. 2.\" />\n" + 
      " <figcaption>\n" + 
      "    <strong class=\"figure__title\"></strong>\n" + 
      "    <span class=\"figure__caption\">\n" + 
      "       <p>Text...</p>\n" + 
      "    </span>\n" + 
      " </figcaption>\n" + 
      "       </figure>\n" + 
      "       <div class=\"figure-extra\"><a alt=\"figure\" href=\"/na101/home/literatum/publisher/images/large/z9k0061741260002.jpeg\" download=\"\">Download\n" + 
      "   figure\n" + 
      " </a><a href=\"/action/downloadFigures?id=F0002&amp;doi=10.1152/jn.00002.2017\" class=\"ppt-figure-link\">Download PowerPoint</a>\n" + 
      "       </div>\n" + 
      "       <br /> \n" + 
      "       <p>Text...</p>\n" + 
      "       <div class=\"ack\">\n" + 
      " <h2 class=\"article-section__title section__title\" >ACKNOWLEDGMENTS</h2>\n" + 
      " <p>Text...</p>\n" + 
      "       </div>\n" + 
      "    </div>\n" + 
      "    \n" + 
      "    <p class=\"fulltext\"></p>\n" + 
      "    <h2 class=\"article-section__title section__title\" >AUTHOR NOTES</h2>\n" + 
      "    <h2 class=\"article-section__title section__title\" >Supplemental data</h2>\n" + 
      "    <ul>\n" + 
      "       <li><a href=\"/doi/suppl/10.1152/jn.00002.2017/suppl_file/supplemental+table+1.xls\">supplemental table 1.xls (890.5 kb)</a></li>\n" + 
      "    </ul>\n" + 
      "    <div class=\"response\">\n" + 
      "       <div class=\"sub-article-title\"></div>\n" + 
      "    </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " </div>\n" + 
      " \n" + 
      " </div>";
   */
  private static final String art1ContentHashFiltered = 
      " Research Article Control of Homeostasis" + 
      " Cit Title" + 
      " Author" + 
      " Author" + 
      " Published Online: 1 Jun 2017 https://doi.org/10.1152/jn.00002.2017" + 
      " Abstract" + 
      " Text..." + 
      " Text..." + 
      " MATERIALS AND METHODS" + 
      " Text..." + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " figure caption..." + 
      "" + 
      "" + 
      "" + 
      " Download" + 
      " figure" + 
      " Download PowerPoint" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " Table 1. Caption..." + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " Text..." + 
      "" + 
      "" + 
      "" + 
      " Text..." + 
      "" + 
      " Data analysis." + 
      " Text..." + 
      "" + 
      "" + 
      "" + 
      "" + 
      "" + 
      " Text..." + 
      "" + 
      "" + 
      "" + 
      " Download" + 
      " figure" + 
      " Download PowerPoint" + 
      "" + 
      "" + 
      " Text..." + 
      "" + 
      " ACKNOWLEDGMENTS" + 
      " Text..." + 
      "" + 
      "" + 
      "" + 
      "" + 
      " AUTHOR NOTES" + 
      " Supplemental data" + 
      "" + 
      " supplemental table 1.xls (890.5 kb)" + 
      "" + 
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
  
  /*
  private static final String citContentHashFiltered = 
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
   */
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
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);
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
  public static class TestCrawl extends TestAmPhysSocHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AmPhysSocHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, manifestContent, manifestContent); 
      doFilterTest(mau, variantFact, tocContent, tocContentCrawlFiltered); 
      doFilterTest(mau, variantFact, art1Content, art1ContentCrawlFiltered); 
      doFilterTest(mau, variantFact, citContent, citContent); 
    }
  }
  
  // Variant to test with Hash Filter
   public static class TestHash extends TestAmPhysSocHtmlFilterFactory {
     public void testFiltering() throws Exception {
      variantFact = new AmPhysSocHtmlHashFilterFactory();
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

