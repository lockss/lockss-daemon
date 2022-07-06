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

package org.lockss.plugin.atypon.ampsychpub;

import java.io.*;

import junit.framework.Test;

import org.apache.commons.io.FileUtils;
import org.lockss.filter.html.HtmlNodeFilters;
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

public class TestAmPsychPubHtmlFilterFactory extends LockssTestCase {

  static Logger log = Logger.getLogger(TestAmPsychPubHtmlFilterFactory.class);


  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
  
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.ampsychpub.AmPsychPubAtyponPlugin";
  
  
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
  
  private static final String manifestContentFiltered = 
      "<a href=\"/toc/jid/313/6\">December 2017 (Vol. 313 Issue 6 Page F1179-F1280)</a>" + 
      "<a href=\"/toc/jid/313/5\">November 2017 (Vol. 313 Issue 5 Page F1061-F1178)</a>" + 
      "<a href=\"/toc/jid/313/4\">October 2017 (Vol. 313 Issue 4 Page F835-F1060)</a>" + 
      "<a href=\"/toc/jid/313/3\">September 2017 (Vol. 313 Issue 3 Page F561-F728)</a>" + 
      "<a href=\"/toc/jid/313/2\">August 2017 (Vol. 313 Issue 2 Page F135-F560)</a>" + 
      "<a href=\"/toc/jid/313/1\">July 2017 (Vol. 313 Issue 1 Page F1-F61)</a>";
  /*
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
      "  <head data-pb-dropzone=\"head\">...head stuff...</head>\n" + 
      "  <title>Journal Name: Vol 313, No 1</title>\n" + 
      "  <body class=\"pb-ui\">\n" + 
      "    <div class=\"base\" data-db-parent-of=\"sb1\">\n" + 
      "      <header class=\"header \">\n" + 
      "        header stuff \n" + 
      "      </header>\n" + 
      "      <nav class=\"article__breadcrumbs\">\n" +
      " <a href=\"/\" class=\"article__tocHeading\">Physiology.org</a>\n" +
      " <a href=\"/journal/jid\" class=\"article__tocHeading\">Journal Name</a>\n" +
      " <a href=\"/toc/jid/313/1\" class=\"article__tocHeading separator\">Vol. 313, No. 1</a> </nav>\n" + 
      "    </div>\n" + 
      "    <main class=\"content\">\n" + 
      "      main\n" + 
      "      <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"e81613f4-f742-47c6-9a72-83da91c190d3\" class=\"publication_header\">\n" + 
      "        <div class=\"overlay\">\n" + 
      "          <div class=\"container\">\n" + 
      "            <div class=\"row\">\n" + 
      "              <div>\n" + 
      "                <div class=\"container\">\n" + 
      "                  <div class=\"row\">\n" + 
      "                    <div class=\"logo-container\"><img src=\"/pb-assets/images/jid_logo.svg\" alt=\"Journal Name Logo\"></div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      <div class=\"container\">\n" + 
      "        <div class=\"row\">\n" + 
      "          <div class=\"publication_container clearfix\">\n" + 
      "            <div class=\"publication-menu hidden-xs hidden-sm container\">\n" + 
      "              <div class=\"row\"></div>\n" + 
      "            </div>\n" + 
      "            <div class=\"container\">\n" + 
      "              <div class=\"row\">\n" + 
      "                <div class=\"xs-space\">\n" + 
      "                  <div class=\"col-sm-8 toc-left-side\">\n" + 
      "                    <div class=\"tocContent\">\n" + 
      "     <!--totalCount1--><!--modified:1519149651000-->\n" + 
      "     <h2 class=\"tocHeading\" id=\"toc-heading-5\">\n" + 
      "       <div class=\"subject heading-1\">Articles</div>\n" + 
      "     </h2>\n" + 
      "     <table class=\"articleEntry\" width=\"100%\" border=\"0\">\n" + 
      "       <tbody>\n" + 
      "         <tr>\n" + 
      "           <td class=\"accessIconContainer\">\n" + 
      "             <div></div>\n" + 
      "             <div class=\"ccIcon\"></div>\n" + 
      "           </td>\n" + 
      "           <td width=\"10\" valign=\"top\" align=\"right\"></td>\n" + 
      "           <td valign=\"top\">\n" + 
      "             <div class=\"icon-feature-au icon-feature-cm \"></div>\n" + 
      "             <div class=\"art_title linkable title-group level1\"><a class=\"ref nowrap\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\"></a>\n" + 
      "               <a class=\"ref nowrap\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\">Internet Disorder</a>\n" + 
      "             </div>\n" + 
      "             <div class=\"tocAuthors afterTitle\">\n" + 
      "               <div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\">\n" + 
      "                 <span class=\"hlFld-ContribAuthor\"><a class=\"entryAuthor linkable\" href=\"/author/Author\">Author</a></span>, Ph.D., \n" + 
      "                 <span class=\"hlFld-ContribAuthor\"><a class=\"entryAuthor linkable\" href=\"/author/Other\">Other</a></span>, Ph.D.</span>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "             <div class=\"art_meta citation\"><span class=\"issueInfo\">174(3)</span><span class=\"articlePageRange \"><span class=\"issueInfoComma\">, </span>pp. 230<span>-</span>236</span></div>\n" + 
      "             <div class=\"tocArticleDoi\"><a href=\"https://doi.org/10.1176/appi.ajp.2016.16020224\">https://doi.org/10.1176/appi.ajp.2016.16020224</a></div>\n" + 
      "             <a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1176/appi.ajp.2016.16020224" +
      "&amp;url=http%3A%2F%2Fsfx.stanford.edu%2Flocal%3Fsid%3Dapp%26iuid%3D9332%26id%3Ddoi%3A10.1176%2Fappi.ajp.2016.16020224\"\n" +
      " title=\"OpenURL\" onclick=\"newWindow(this.href);return false\"\n" +
      " class=\"sfxLink\"><img src=\"/userimages/9332/sfxbutton\" alt=\"OpenURL\"></a>\n" + 
      "             <div class=\"tocDeliverFormatsLinks\">\n" + 
      "               <a class=\"ref nowrap abs\" href=\"/doi/abs/10.1176/appi.ajp.2016.16020224\">Abstract</a> |\n" + 
      "               <a class=\"ref nowrap full\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\">Full Text</a> |\n" + 
      "               <a class=\"ref nowrap references\" href=\"/doi/ref/10.1176/appi.ajp.2016.16020224\">References</a> |\n" + 
      "               <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1176/appi.ajp.2016.16020224\">PDF (534 KB)</a> |\n" + 
      "               <a class=\"ref nowrap pdfplus\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.1176/appi.ajp.2016.16020224\">PDF Plus (554 KB)</a> |\n" + 
      "               <a class=\"ref nowrap suppl\" href=\"/doi/suppl/10.1176/appi.ajp.2016.16020224\">Supplemental Material</a>&nbsp;\n" + 
      "               <div id=\"Absappiajp201616020224\" class=\"previewViewSection tocPreview\">\n" + 
      "                 <div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1176/appi.ajp.2016.16020224', 'Absappiajp201616020224');\"></div>\n" + 
      "                 <p class=\"previewContent\"></p>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "           </td>\n" + 
      "         </tr>\n" + 
      "       </tbody>\n" + 
      "     </table>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <div class=\"col-sm-4 gray-bg toc-right-side\"></div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      </div></div>\n" + 
      "    </main>\n" + 
      "    <footer>  footer stuff</footer>\n" + 
      "  </body>\n" + 
      "</html>";
  
  private static final String tocContentCrawlFiltered = 
      "<html class=\"pb-page\" data-request-id=\"9b091ab2-2ab5-4d31-bed7-31d90503fe85\" lang=\"en\">\n" + 
      "  <head data-pb-dropzone=\"head\">...head stuff...</head>\n" + 
      "  <title>Journal Name: Vol 313, No 1</title>\n" + 
      "  <body class=\"pb-ui\">\n" + 
      "    <div class=\"base\" data-db-parent-of=\"sb1\">\n" + 
      "      \n" + 
      "      <nav class=\"article__breadcrumbs\">\n" +
      " <a href=\"/\" class=\"article__tocHeading\">Physiology.org</a>\n" +
      " <a href=\"/journal/jid\" class=\"article__tocHeading\">Journal Name</a>\n" +
      " <a href=\"/toc/jid/313/1\" class=\"article__tocHeading separator\">Vol. 313, No. 1</a> </nav>\n" + 
      "    </div>\n" + 
      "    <main class=\"content\">\n" + 
      "      main\n" + 
      "      <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"e81613f4-f742-47c6-9a72-83da91c190d3\" class=\"publication_header\">\n" + 
      "        <div class=\"overlay\">\n" + 
      "          <div class=\"container\">\n" + 
      "            <div class=\"row\">\n" + 
      "              <div>\n" + 
      "                <div class=\"container\">\n" + 
      "                  <div class=\"row\">\n" + 
      "                    <div class=\"logo-container\"><img src=\"/pb-assets/images/jid_logo.svg\" alt=\"Journal Name Logo\"></div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      <div class=\"container\">\n" + 
      "        <div class=\"row\">\n" + 
      "          <div class=\"publication_container clearfix\">\n" + 
      "            <div class=\"publication-menu hidden-xs hidden-sm container\">\n" + 
      "              <div class=\"row\"></div>\n" + 
      "            </div>\n" + 
      "            <div class=\"container\">\n" + 
      "              <div class=\"row\">\n" + 
      "                <div class=\"xs-space\">\n" + 
      "                  <div class=\"col-sm-8 toc-left-side\">\n" + 
      "                    <div class=\"tocContent\">\n" + 
      "     <!--totalCount1--><!--modified:1519149651000-->\n" + 
      "     <h2 class=\"tocHeading\" id=\"toc-heading-5\">\n" + 
      "       <div class=\"subject heading-1\">Articles</div>\n" + 
      "     </h2>\n" + 
      "     <table class=\"articleEntry\" width=\"100%\" border=\"0\">\n" + 
      "       <tbody>\n" + 
      "         <tr>\n" + 
      "           <td class=\"accessIconContainer\">\n" + 
      "             <div></div>\n" + 
      "             <div class=\"ccIcon\"></div>\n" + 
      "           </td>\n" + 
      "           <td width=\"10\" valign=\"top\" align=\"right\"></td>\n" + 
      "           <td valign=\"top\">\n" + 
      "             <div class=\"icon-feature-au icon-feature-cm \"></div>\n" + 
      "             <div class=\"art_title linkable title-group level1\"><a class=\"ref nowrap\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\"></a>\n" + 
      "               <a class=\"ref nowrap\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\">Internet Disorder</a>\n" + 
      "             </div>\n" + 
      "             <div class=\"tocAuthors afterTitle\">\n" + 
      "               <div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\">\n" + 
      "                 <span class=\"hlFld-ContribAuthor\"></span>, Ph.D., \n" + 
      "                 <span class=\"hlFld-ContribAuthor\"></span>, Ph.D.</span>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "             <div class=\"art_meta citation\"><span class=\"issueInfo\">174(3)</span><span class=\"articlePageRange \"><span class=\"issueInfoComma\">, </span>pp. 230<span>-</span>236</span></div>\n" + 
      "             <div class=\"tocArticleDoi\"><a href=\"https://doi.org/10.1176/appi.ajp.2016.16020224\">https://doi.org/10.1176/appi.ajp.2016.16020224</a></div>\n" + 
      "             \n" + 
      "             <div class=\"tocDeliverFormatsLinks\">\n" + 
      "               <a class=\"ref nowrap abs\" href=\"/doi/abs/10.1176/appi.ajp.2016.16020224\">Abstract</a> |\n" + 
      "               <a class=\"ref nowrap full\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\">Full Text</a> |\n" + 
      "               <a class=\"ref nowrap references\" href=\"/doi/ref/10.1176/appi.ajp.2016.16020224\">References</a> |\n" + 
      "               <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1176/appi.ajp.2016.16020224\">PDF (534 KB)</a> |\n" + 
      "               <a class=\"ref nowrap pdfplus\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.1176/appi.ajp.2016.16020224\">PDF Plus (554 KB)</a> |\n" + 
      "               <a class=\"ref nowrap suppl\" href=\"/doi/suppl/10.1176/appi.ajp.2016.16020224\">Supplemental Material</a>&nbsp;\n" + 
      "               <div id=\"Absappiajp201616020224\" class=\"previewViewSection tocPreview\">\n" + 
      "                 <div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1176/appi.ajp.2016.16020224', 'Absappiajp201616020224');\"></div>\n" + 
      "                 <p class=\"previewContent\"></p>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "           </td>\n" + 
      "         </tr>\n" + 
      "       </tbody>\n" + 
      "     </table>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <div class=\"col-sm-4 gray-bg toc-right-side\"></div>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "      </div></div>\n" + 
      "    </main>\n" + 
      "    \n" + 
      "  </body>\n" + 
      "</html>";
  
  private static final String tocContentHashFiltered = 
      "<div class=\"tocContent\">\n" + 
      "     \n" + 
      "     <h2 class=\"tocHeading\" >\n" + 
      "       <div class=\"subject heading-1\">Articles</div>\n" + 
      "     </h2>\n" + 
      "     <table class=\"articleEntry\" width=\"100%\" border=\"0\">\n" + 
      "       <tbody>\n" + 
      "         <tr>\n" + 
      "           \n" + 
      "           <td width=\"10\" valign=\"top\" align=\"right\"></td>\n" + 
      "           <td valign=\"top\">\n" + 
      "             <div class=\"icon-feature-au icon-feature-cm \"></div>\n" + 
      "             <div class=\"art_title linkable title-group level1\"><a class=\"ref nowrap\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\"></a>\n" + 
      "               <a class=\"ref nowrap\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\">Internet Disorder</a>\n" + 
      "             </div>\n" + 
      "             <div class=\"tocAuthors afterTitle\">\n" + 
      "               <div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\">\n" + 
      "                 <span class=\"hlFld-ContribAuthor\"><a class=\"entryAuthor linkable\" href=\"/author/Author\">Author</a></span>, Ph.D., \n" + 
      "                 <span class=\"hlFld-ContribAuthor\"><a class=\"entryAuthor linkable\" href=\"/author/Other\">Other</a></span>, Ph.D.</span>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "             <div class=\"art_meta citation\"><span class=\"issueInfo\">174(3)</span><span class=\"articlePageRange \"><span class=\"issueInfoComma\">, </span>pp. 230<span>-</span>236</span></div>\n" + 
      "             <div class=\"tocArticleDoi\"><a href=\"https://doi.org/10.1176/appi.ajp.2016.16020224\">https://doi.org/10.1176/appi.ajp.2016.16020224</a></div>\n" + 
      "             \n" + 
      "             <div class=\"tocDeliverFormatsLinks\">\n" + 
      "               <a class=\"ref nowrap abs\" href=\"/doi/abs/10.1176/appi.ajp.2016.16020224\">Abstract</a> |\n" + 
      "               <a class=\"ref nowrap full\" href=\"/doi/full/10.1176/appi.ajp.2016.16020224\">Full Text</a> |\n" + 
      "               <a class=\"ref nowrap references\" href=\"/doi/ref/10.1176/appi.ajp.2016.16020224\">References</a> |\n" + 
      "               <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1176/appi.ajp.2016.16020224\"></a> |\n" + 
      "               <a class=\"ref nowrap pdfplus\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdfplus/10.1176/appi.ajp.2016.16020224\"></a> |\n" + 
      "               <a class=\"ref nowrap suppl\" href=\"/doi/suppl/10.1176/appi.ajp.2016.16020224\">Supplemental Material</a>&nbsp;\n" + 
      "               <div  class=\"previewViewSection tocPreview\">\n" + 
      "                 <div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1176/appi.ajp.2016.16020224', 'Absappiajp201616020224');\"></div>\n" + 
      "                 <p class=\"previewContent\"></p>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "           </td>\n" + 
      "         </tr>\n" + 
      "       </tbody>\n" + 
      "     </table>\n" + 
      "                    </div>";
  /*
  private static final String tocContentHashFiltered = 
      " Articles" + 
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
   */
  
  private static final String art1Content = 
      "<html class=\"pb-page\" data-request-id=\"3b0ad0c2-6486-4c37-b3b1-febfb45e6b98\" lang=\"en\">\n" + 
      "  <script type=\"text/javascript\" async=\"\" src=\"https://www.google-analytics.com/analytics.js\"></script>" +
      "  <head data-pb-dropzone=\"head\">\n" + 
      "    <link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\">\n" + 
      "    <meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"2017-03-01\">\n" + 
      "    <meta charset=\"UTF-8\">\n" + 
      "    <title>EDITOR'S NOTE | American Journal of Psychiatry</title>\n" + 
      "    <meta data-pb-head=\"head-widgets-end\">\n" + 
      "  </head>\n" + 
      "  <body class=\"pb-ui\">\n" + 
      "    <div id=\"pb-page-content\" data-ng-non-bindable=\"\">\n" + 
      "      <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" + 
      "        <div class=\"widget pageHeader none  widget-none  widget-compact-all\" id=\"13c35e80-9b5e-479b-a71e-eec581320abf\">\n" + 
      "          <div class=\"wrapped \">\n" + 
      "            <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "              <header class=\"page-header\">\n" + 
      "                <div data-pb-dropzone=\"main\">\n" + 
      "                  <div class=\"widget layout-inline-content alignRight  widget-none  widget-compact-all\">\n" + 
      "                    <div class=\"wrapped \">\n" + 
      "     <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "       <div class=\"inline-dropzone\" data-pb-dropzone=\"content\">\n" + 
      "         <div class=\"widget literatumInstitutionBanner none inst-welcome-text widget-none  widget-compact-all\">\n" + 
      "           <div class=\"wrapped \">\n" + 
      "             <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "               <div class=\"welcome\">\n" + 
      "                 <span>\n" + 
      "                 <a href=\"http://lane.stanford.edu/index.html\" target=\"_blank\" class=\"institutionLink\"> Access provided </a>\n" + 
      "                 </span>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "           </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"widget literatumNavigationLoginBar none  widget-none  widget-compact-all\" id=\"cb45e9a4-16b2-496d-ab56-03679748b2f9\">\n" + 
      "           <div class=\"wrapped \">\n" + 
      "             <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "               <div class=\"loginBar\">\n" + 
      "                 <a href=\"/action/showLogin?uri=%2Fdoi%2Ffull%2F10.1176%2Fappi.ajp.2017.1743editor\">\n" + 
      "                 Sign In\n" + 
      "                 </a>\n" + 
      "                 &nbsp;|&nbsp;\n" + 
      "                 <a href=\"/action/registration?redirectUri=%2Fdoi%2Ffull%2F10.1176%2Fappi.ajp.2017.1743editor\" class=\"register-link\">\n" + 
      "                 Register\n" + 
      "                 </a>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "           </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"widget general-html none  widget-none  widget-compact-all\" id=\"2e0349a3-fb1b-442d-85fc-96b757b4b122\">\n" + 
      "           <div class=\"wrapped \">\n" + 
      "             <div class=\"widget-body body body-none  body-compact-all\">|&nbsp;&nbsp;<a href=\"http://psychiatryonline.org/store/home\">POL Subscriptions</a></div>\n" + 
      "           </div>\n" + 
      "         </div>\n" + 
      "       </div>\n" + 
      "     </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"widget general-html none menuXml pso-nav widget-none  widget-compact-all\" id=\"4e77a08e-279b-4d2d-ab49-287ccba5f3fa\">\n" + 
      "                    <div class=\"wrapped \">\n" + 
      "     <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "       <ul class=\"primaryNav\">\n" + 
      "         <li><a href=\"http://psychiatryonline.org\">PsychiatryOnline</a></li>\n" + 
      "         <li>\n" + 
      "           <a href=\"http://dsm.psychiatryonline.org\">DSM Library</a>\n" + 
      "           <ul>\n" + 
      "             <li><a href=\"http://dsm.psychiatryonline.org/doi/book/10.1176/appi.books.9780890425596\">DSM-5®</a></li>\n" + 
      "           </ul>\n" + 
      "         </li>\n" + 
      "         <li>\n" + 
      "           <a href=\"http://psychiatryonline.org/books\">Books</a>\n" + 
      "           <ul>\n" + 
      "             <li><a href=\"http://psychiatryonline.org/doi/book/10.1176/appi.books.9781585624201\">Textbook</a></li>\n" + 
      "             <li><a href=\"http://psychiatryonline.org/books\">More Books</a></li>\n" + 
      "           </ul>\n" + 
      "         </li>\n" + 
      "         <li>\n" + 
      "           <a href=\"#\">Collections</a>\n" + 
      "           <ul>\n" + 
      "             <li><a href=\"http://psychiatryonline.org/psychotherapy\">Psychotherapy Library</a></li>\n" + 
      "             <li><a href=\"http://psychiatryonline.org/ebooks\">eBooks</a></li>\n" + 
      "           </ul>\n" + 
      "         </li>\n" + 
      "         <li>\n" + 
      "           <a href=\"http://psychiatryonline.org/journals\">Journals</a>\n" + 
      "           <ul>\n" + 
      "             <li><a href=\"http://ajp.psychiatryonline.org\">The American Journal of Psychiatry</a></li>\n" + 
      "             <li><a href=\"http://focus.psychiatryonline.org\">FOCUS</a></li>\n" + 
      "             <li><a href=\"http://ps.psychiatryonline.org\">Psychiatric Services</a></li>\n" + 
      "           </ul>\n" + 
      "         </li>\n" + 
      "         <li><a href=\"http://psychnews.psychiatryonline.org\">News</a></li>\n" + 
      "         <li><a href=\"http://psychiatryonline.org/guidelines\">APA Guidelines</a></li>\n" + 
      "         <li>\n" + 
      "           <a href=\"http://psychiatryonline.org/patients\">Patient Education</a>\n" + 
      "           <ul>\n" + 
      "             <li><a href=\"http://psychiatryonline.org/doi/book/10.1176/appi.books.9781615371280\">What Your </a></li>\n" + 
      "             <li><a href=\"http://psychiatryonline.org/doi/book/10.1176/appi.books.9781615370283\">Helping </a></li>\n" + 
      "             <li><a href=\"http://psychiatryonline.org/doi/10.1176/appi.books.9781615370740\">Understanding </a></li>\n" + 
      "           </ul>\n" + 
      "         </li>\n" + 
      "         <li><a href=\"http://psychiatryonline.org/international\">International</a></li>\n" + 
      "         <li><a href=\"http://psychiatryonline.org/cme\">CME</a></li>\n" + 
      "         <li><a href=\"http://psychiatryonline.org/action/showPreferences\">My POL</a></li>\n" + 
      "       </ul>\n" + 
      "     </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"widget layout-frame none logo-area widget-none  widget-compact-all\" id=\"bb52c9e8-36a4-4f06-b076-7efc3f26a473\">\n" + 
      "                    <div class=\"wrapped \">\n" + 
      "     <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "       <div data-pb-dropzone=\"contents\">\n" + 
      "         <div class=\"widget quickSearchWidget alignRight  widget-none  widget-compact-all\" id=\"191aebe7-029c-4c98-968c-118e596d5978\">\n" + 
      "           <div class=\"wrapped \">\n" + 
      "             <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "               <div class=\"quickSearchFormContainer\">\n" + 
      "               </div>\n" + 
      "               <div class=\"advancedSearchLinkDropZone\" data-pb-dropzone=\"advancedSearchLinkDropZone\">\n" + 
      "                 <div class=\"widget general-html alignRight  widget-none  widget-compact-all\" id=\"df755376-76dc-4e40-bc93-f917707c7646\">\n" + 
      "                 </div>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "           </div>\n" + 
      "         </div>\n" + 
      "       </div>\n" + 
      "     </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"widget general-html none menuXml ajp-nav widget-none  widget-compact-all\" id=\"444a132a-c177-432d-9851-305a063fd6ed\">\n" + 
      "                    <div class=\"wrapped \">\n" + 
      "     <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "       <ul class=\"primaryNav\">\n" + 
      "         <li><a href=\"/\">Home</a></li>\n" + 
      "         <li><a href=\"/current\">Current Issue</a></li>\n" + 
      "         <li><a href=\"/loi/ajp\">All Issues</a></li>\n" + 
      "       </ul>\n" + 
      "     </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </header>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "        <div class=\"widget pageBody none  widget-none  widget-compact-all\" id=\"a3907b2c-e067-4eb5-9e12-040b6179d9d6\">\n" + 
      "          <div class=\"wrapped \">\n" + 
      "            <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "              <!--begin pagefulltext-->\n" + 
      "              <div class=\"page-body pagefulltext\">\n" + 
      "                <div data-pb-dropzone=\"main\">\n" + 
      "                  <div class=\"widget layout-two-columns none articleColumns widget-none  widget-compact-all\" id=\"037a5805-7259-48af-8825-21291d1e258b\">\n" + 
      "                    <div class=\"wrapped \">\n" + 
      "     <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "       <div class=\"pb-columns row-fluid \">\n" + 
      "         <div class=\"width_2_3\">\n" + 
      "           <div data-pb-dropzone=\"left\" class=\"pb-autoheight\">\n" + 
      "             <div class=\"widget literatumBookIssueNavigation none articleContentNavigation widget-none  widget-compact-all\" id=\"95ff3bc6-de68-4ecb-812b-fe6f505b1aa8\">\n" + 
      "               <div class=\"wrapped \">\n" + 
      "                 <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "                   <div class=\"pager issueBookNavPager\">\n" + 
      "                     <span class=\"journalNavLeftTd\">\n" + 
      "                     <span class=\"prev placedLeft\">\n" + 
      "                     <a href=\"http://ajp.psychiatryonline.org/doi/10.1176/appi.ajp.2017.174301\">\n" + 
      "                     Previous Article\n" + 
      "                     </a>\n" + 
      "                     </span>\n" + 
      "                     </span>\n" + 
      "                     <span class=\"journalNavCenterTd\">\n" + 
      "                       <div class=\"journalNavTitle\">\n" + 
      "        <a href=\"/toc/ajp/174/3\">Volume 174, Issue 3, March 01, 2017</a>, <span class=\"articlePageRange\">pp. 298-298</span>\n" + 
      "                       </div>\n" + 
      "                     </span>\n" + 
      "                     <span class=\"journalNavRightTd\">\n" + 
      "                     </span>\n" + 
      "                   </div>\n" + 
      "                 </div>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "             <div class=\"widget literatumPublicationContentWidget none  widget-none  widget-compact-all\" id=\"75b33ea9-e7d2-43bd-8d65-da6fe6904e73\">\n" + 
      "               <div class=\"wrapped \">\n" + 
      "                 <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "                   <div class=\"articleMeta ja\">\n" + 
      "                     <div class=\"tocHeading\">\n" + 
      "                       <b>Editor's Note</b>\n" + 
      "                     </div>\n" + 
      "                     <div class=\"publicationContentTitle\">\n" + 
      "                       <h1>\n" + 
      "        EDITOR'S NOTE\n" + 
      "                       </h1>\n" + 
      "                     </div>\n" + 
      "                     <div class=\"articleMetaDrop publicationContentDropZone\" data-pb-dropzone=\"articleMetaDropZone\">\n" + 
      "                     </div>\n" + 
      "                     <div class=\"publicationContentAuthors\">\n" + 
      "                       <div class=\"hlFld-ContribAuthor\"></div>\n" + 
      "                     </div>\n" + 
      "                     <div class=\"articleMetaDrop publicationContentDropZone publicationContentDropZone1\" data-pb-dropzone=\"articleMetaDropZone1\">\n" + 
      "                     </div>\n" + 
      "                     <div class=\"authInfo authInfoLink\" style=\"display: none;\"><a href=\"#\" onclick=\"scrollToAuthAndArtInformation();\">View Author and Article Information</a></div>\n" + 
      "                     <div>\n" + 
      "                     </div>\n" + 
      "                     <div class=\"publicationContentDoi publicationContentEpubDate\">\n" + 
      "                       Published online: March 01, 2017\n" + 
      "                       &nbsp;|&nbsp;\n" + 
      "                       <a href=\"https://doi.org/10.1176/appi.ajp.2017.1743editor\">https://doi.org/10.1176/appi.ajp.2017.1743editor</a>\n" + 
      "                     </div>\n" + 
      "                   </div>\n" + 
      "                   <div class=\"articleToolsDropZone\" data-pb-dropzone=\"articleToolsDropZone\">\n" + 
      "                   </div>\n" + 
      "                   <div class=\"publication-tabs ja\">\n" + 
      "                     <div class=\"tabs tabs-widget\">\n" + 
      "                       <ul class=\"tab-nav\" role=\"tablist\">\n" + 
      "        <li role=\"tab\" aria-selected=\"false\">\n" + 
      "    <a href=\"/doi/abs/10.1176/appi.ajp.2017.1743editor\" class=\"show-abstract\">\n" + 
      "    Citation\n" + 
      "    </a>\n" + 
      "        </li>\n" + 
      "        <li class=\"active\" role=\"tab\" aria-selected=\"true\">\n" + 
      "    <a href=\"/doi/full/10.1176/appi.ajp.2017.1743editor\" class=\"show-full\">\n" + 
      "    Full Text\n" + 
      "    </a>\n" + 
      "        </li>\n" + 
      "        <li role=\"tab\" aria-selected=\"false\">\n" + 
      "    <a href=\"http://ajp.psychiatryonline.org/doi/pdf/10.1176/appi.ajp.2017.1743editor\"" +
      " class=\"show-pdf\" target=\"_self\">\n" + 
      "    PDF\n" + 
      "    </a>\n" + 
      "        </li>\n" + 
      "        <li role=\"tab\" aria-selected=\"false\">\n" + 
      "    <a href=\"http://ajp.psychiatryonline.org/doi/pdfplus/10.1176/appi.ajp.2017.1743editor\" target=\"_self\">\n" + 
      "    PDF Plus\n" + 
      "    </a>\n" + 
      "        </li>\n" + 
      "        <li role=\"tab\" aria-selected=\"false\">\n" + 
      "    <a href=\"#relatedContent\">\n" + 
      "    Related\n" + 
      "    </a>\n" + 
      "        </li>\n" + 
      "                       </ul>\n" + 
      "                       <div class=\"tab-content \">\n" + 
      "        <a id=\"top-content-scroll\"></a>\n" + 
      "        <div class=\"tab tab-pane active\">\n" + 
      "    <article class=\"article\">\n" + 
      "      <p class=\"fulltext\"></p>\n" +
      "      <!-- abstract content -->\n" +
      "      <div class=\"hlFld-Abstract\">\n" +
      "    <p class=\"fulltext\"></p>\n" +
      "      </div>\n" +
      "      <!-- /abstract content --><!-- fulltext content -->\n" +
      "      <div class=\"hlFld-Fulltext\">\n" + 
      "    <p>The <i>Journal</i> was notified about a complaint (" +
      "<a class=\"ext-link\" href=\"http://ajp.psychiatryonline.org/doi/full/10.1176/appi.ajp.158.6.906\" target=\"_blank\">" +
      "http://dx.doi.org/10.1176/appi.ajp.158.6.906</a>).</p>\n" + 
      "      </div>\n" + 
      "      <!-- /fulltext content -->\n" + 
      "      <div class=\"response\">\n" + 
      "    <div class=\"sub-article-title\"></div>\n" + 
      "      </div>\n" + 
      "    </article>\n" + 
      "        </div>\n" + 
      "        <div class=\"tab tab-pane\" id=\"relatedContent\">\n" + 
      "    <div class=\"category\">\n" + 
      "      <h3>Original Article</h3>\n" + 
      "      <ul>\n" + 
      "    <li>\n" + 
      "      <a href=\"/doi/full/10.1176/appi.ajp.158.6.906\" class=\"ref nowrap\">\n" + 
      "      Double-Blind\n" + 
      "      </a>\n" + 
      "    </li>\n" + 
      "      </ul>\n" + 
      "    </div>\n" + 
      "        </div>\n" + 
      "        <div class=\"tab tab-pane\" id=\"cme\">\n" + 
      "    <div data-pb-dropzone=\"cme\">\n" + 
      "    </div>\n" + 
      "        </div>\n" + 
      "                       </div>\n" + 
      "                     </div>\n" + 
      "                   </div>\n" + 
      "                   <input id=\"viewLargeImageCaption\" value=\"View Large Image\" type=\"hidden\">\n" + 
      "                 </div>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "           </div>\n" + 
      "         </div>\n" + 
      "         <div class=\"width_1_3\">\n" + 
      "           <div data-pb-dropzone=\"right\" class=\"pb-autoheight\">\n" + 
      "             <div class=\"widget literatumArticleToolsWidget none  widget-none\"" +
      " id=\"2a984b58-2a84-4286-a261-a8108df4d8a8\">\n" + 
      "               <div class=\"wrapped \">\n" + 
      "                 <div class=\"widget-body body body-none \">\n" + 
      "                   <div class=\"articleTools\">\n" + 
      "                     <ul class=\"linkList blockLinks separators centered\">\n" + 
      "                       <li class=\"addToFavs\">\n" + 
      "        <a href=\"/personalize/addFavoritePublication?doi=10.1176%2Fappi.ajp.2017.1743editor\">Add to My POL</a>\n" + 
      "                       </li>\n" + 
      "                       <li class=\"downloadCitations\">\n" + 
      "        <a href=\"/action/showCitFormats?doi=10.1176%2Fappi.ajp.2017.1743editor\">Send to Citation Mgr</a>\n" + 
      "                       </li>\n" + 
      "                     </ul>\n" + 
      "                   </div>\n" + 
      "                 </div>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "             <div class=\"widget layout-two-columns none  widget-none  widget-compact-all\">\n" + 
      "               <div class=\"wrapped \">\n" + 
      "                 <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "                   <div class=\"pb-columns row-fluid gutterless\">\n" + 
      "                     <div class=\"width_5_24\">\n" + 
      "                       <div data-pb-dropzone=\"left\" class=\"pb-autoheight\">\n" + 
      "        <div class=\"widget general-html none  widget-none  widget-compact-all\"" +
      " id=\"1e879678-c84a-44e1-9a30-da2e26c3da18\">\n" + 
      "    <div class=\"wrapped \">\n" + 
      "      <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "    <script type=\"text/javascript\"></script>\n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "        </div>\n" + 
      "                       </div>\n" + 
      "                     </div>\n" + 
      "                     <div class=\"width_19_24\">\n" + 
      "                       <div data-pb-dropzone=\"right\" class=\"pb-autoheight\">\n" + 
      "        <div class=\"widget general-bookmark-share alignCenter widget-compact-horizontal\">\n" + 
      "    <div class=\"wrapped \">\n" + 
      "      <div class=\"widget-body body body-none  body-compact-horizontal\">\n" + 
      "    <!-- AddThis Button BEGIN -->\n" + 
      "    <div class=\"addthis_toolbox addthis_default_style addthis_32x32_style\"" +
      " addthis:url=\"https://ajp.psychiatryonline.org/doi/full/10.1176/appi.ajp.2017.1743editor\">\n" + 
      "      <a class=\"addthis_button_twitter at300b\" title=\"Twitter\" href=\"#\">\n" + 
      "        <span class=\"at-icon-wrapper\" style=\"background-color: rgb(29, 161, 242);" +
      " line-height: 32px; height: 32px; width: 32px;\">\n" + 
      "        </span>\n" + 
      "      </a>\n" + 
      "      <div class=\"atclear\"></div>\n" + 
      "    </div>\n" + 
      "    <!-- AddThis Button END -->\n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "        </div>\n" + 
      "                       </div>\n" + 
      "                     </div>\n" + 
      "                   </div>\n" + 
      "                 </div>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "             <div class=\"widget layout-frame none inner-white widget-emphasis\" id=\"f4410c40-ec02-421f-a474-5b07264d4858\">\n" +
      "               <div class=\"wrapped \">\n" + 
      "                 <h1 class=\"widget-header header-regular \">Related Articles</h1>\n" + 
      "                 <div class=\"widget-body body body-regular \">\n" + 
      "                   <p><b>Text...</b></p>\n" + 
      "                   <a href=\"http://www.psychiatryonline.org/doi/abs/10.1176/appi.ps.201300158\" target=\"_blank\">10.1176/appi.ps.201300158</a>\n" + 
      "                   <hr>\n" + 
      "                 </div>\n" + 
      "               </div>\n" + 
      "               <div class=\"wrapped \" id=\"TrendMD\">\n" + 
      "                 <div class=\"widget-body body body-emphasis \">\n" + 
      "                   <div data-pb-dropzone=\"contents\">\n" + 
      "                     <div class=\"widget general-html none  widget-none  widget-compact-vertical\"" +
      " id=\"6117f7c0-7fe4-4a19-9c90-a141fe2541cf\">\n" + 
      "                       <div class=\"wrapped \" id=\"TrendMD\">\n" + 
      "        <div class=\"widget-body body body-none  body-compact-vertical\">\n" + 
      "    <div id=\"trendmd-suggestions\"></div>\n" + 
      "        </div>\n" + 
      "                       </div>\n" + 
      "                     </div>\n" + 
      "                   </div>\n" + 
      "                 </div>\n" + 
      "               </div>\n" + 
      "             </div>\n" + 
      "           </div>\n" + 
      "         </div>\n" + 
      "       </div>\n" + 
      "     </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </div>\n" + 
      "              <!--end pagefulltext-->\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "        <div class=\"widget pageFooter none  widget-none  widget-compact-all\" id=\"857d8e34-9b91-4e39-a31f-dc42600dc42e\">\n" + 
      "          <div class=\"wrapped \">\n" + 
      "            <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "              <footer class=\"page-footer\">\n" + 
      "                <div data-pb-dropzone=\"main\">\n" + 
      "                  <div class=\"widget layout-two-columns none journal-footer widget-emphasis  widget-compact-all\" id=\"81af7199-c224-44f9-b563-5e3a25b1ba12\">\n" + 
      "                    <div class=\"wrapped \">\n" + 
      "     <div class=\"widget-body body body-emphasis  body-compact-all\"></div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                  <div class=\"widget layout-frame none site-footer widget-none  widget-compact-all\" id=\"e81e9be5-29c7-4a4d-8f66-4f7292f99263\">\n" + 
      "                    <div class=\"wrapped \">\n" + 
      "     <div class=\"widget-body body body-none  body-compact-all\">\n" + 
      "       <div data-pb-dropzone=\"contents\">\n" + 
      "         <div class=\"widget layout-three-columns none  widget-none  widget-compact-all\" id=\"baf7b00f-2c80-479e-a4b9-c4cda6e243c1\">\n" + 
      "         </div>\n" + 
      "         <div class=\"widget general-html none  widget-none\" id=\"5143440b-2a7f-455d-9aef-494cf0659e35\">\n" + 
      "           <div class=\"wrapped \">\n" + 
      "             <div class=\"widget-body body body-none \">\n" + 
      "               <div class=\"site-copyright\">Copyright © American Psychiatric Association</div>\n" + 
      "               <div class=\"lit-site\">Powered by <a href=\"http://www.atypon.com/\" target=\"new\">Atypon® Literatum</a></div>\n" + 
      "             </div>\n" + 
      "           </div>\n" + 
      "         </div>\n" + 
      "       </div>\n" + 
      "     </div>\n" + 
      "                    </div>\n" + 
      "                  </div>\n" + 
      "                </div>\n" + 
      "              </footer>\n" + 
      "            </div>\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "        <div class=\"widget general-html none  widget-none\" id=\"9b0d00cb-3a46-4474-a164-2b3bc16b9005\">\n" + 
      "          <div class=\"wrapped \" id=\"open-athens-js\">\n" + 
      "          </div>\n" + 
      "        </div>\n" + 
      "      </div>\n" + 
      "    </div>\n" + 
      "  </body>\n" + 
      "</html>";
  
  private static final String art1ContentCrawlFiltered =
          "<html class=\"pb-page\" data-request-id=\"3b0ad0c2-6486-4c37-b3b1-febfb45e6b98\" lang=\"en\">\n" +
                  "  <script type=\"text/javascript\" async=\"\" src=\"https://www.google-analytics.com/analytics.js\"></script>  <head data-pb-dropzone=\"head\">\n" +
                  "    <link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\">\n" +
                  "    <meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"2017-03-01\">\n" +
                  "    <meta charset=\"UTF-8\">\n" +
                  "    <title>EDITOR'S NOTE | American Journal of Psychiatry</title>\n" +
                  "    <meta data-pb-head=\"head-widgets-end\">\n" +
                  "  </head>\n" +
                  "  <body class=\"pb-ui\">\n" +
                  "    <div id=\"pb-page-content\" data-ng-non-bindable=\"\">\n" +
                  "      <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
                  "        <div class=\"widget pageHeader none  widget-none  widget-compact-all\" id=\"13c35e80-9b5e-479b-a71e-eec581320abf\">\n" +
                  "          <div class=\"wrapped \">\n" +
                  "            <div class=\"widget-body body body-none  body-compact-all\">\n" +
                  "              \n" +
                  "            </div>\n" +
                  "          </div>\n" +
                  "        </div>\n" +
                  "        <div class=\"widget pageBody none  widget-none  widget-compact-all\" id=\"a3907b2c-e067-4eb5-9e12-040b6179d9d6\">\n" +
                  "          <div class=\"wrapped \">\n" +
                  "            <div class=\"widget-body body body-none  body-compact-all\">\n" +
                  "              <!--begin pagefulltext-->\n" +
                  "              <div class=\"page-body pagefulltext\">\n" +
                  "                <div data-pb-dropzone=\"main\">\n" +
                  "                  <div class=\"widget layout-two-columns none articleColumns widget-none  widget-compact-all\" id=\"037a5805-7259-48af-8825-21291d1e258b\">\n" +
                  "                    <div class=\"wrapped \">\n" +
                  "     <div class=\"widget-body body body-none  body-compact-all\">\n" +
                  "       <div class=\"pb-columns row-fluid \">\n" +
                  "         <div class=\"width_2_3\">\n" +
                  "           <div data-pb-dropzone=\"left\" class=\"pb-autoheight\">\n" +
                  "             \n" +
                  "             <div class=\"widget literatumPublicationContentWidget none  widget-none  widget-compact-all\" id=\"75b33ea9-e7d2-43bd-8d65-da6fe6904e73\">\n" +
                  "               <div class=\"wrapped \">\n" +
                  "                 <div class=\"widget-body body body-none  body-compact-all\">\n" +
                  "                   <div class=\"articleMeta ja\">\n" +
                  "                     <div class=\"tocHeading\">\n" +
                  "                       <b>Editor's Note</b>\n" +
                  "                     </div>\n" +
                  "                     <div class=\"publicationContentTitle\">\n" +
                  "                       <h1>\n" +
                  "        EDITOR'S NOTE\n" +
                  "                       </h1>\n" +
                  "                     </div>\n" +
                  "                     <div class=\"articleMetaDrop publicationContentDropZone\" data-pb-dropzone=\"articleMetaDropZone\">\n" +
                  "                     </div>\n" +
                  "                     <div class=\"publicationContentAuthors\">\n" +
                  "                       <div class=\"hlFld-ContribAuthor\"></div>\n" +
                  "                     </div>\n" +
                  "                     <div class=\"articleMetaDrop publicationContentDropZone publicationContentDropZone1\" data-pb-dropzone=\"articleMetaDropZone1\">\n" +
                  "                     </div>\n" +
                  "                     <div class=\"authInfo authInfoLink\" style=\"display: none;\"><a href=\"#\" onclick=\"scrollToAuthAndArtInformation();\">View Author and Article Information</a></div>\n" +
                  "                     <div>\n" +
                  "                     </div>\n" +
                  "                     <div class=\"publicationContentDoi publicationContentEpubDate\">\n" +
                  "                       Published online: March 01, 2017\n" +
                  "                       &nbsp;|&nbsp;\n" +
                  "                       <a href=\"https://doi.org/10.1176/appi.ajp.2017.1743editor\">https://doi.org/10.1176/appi.ajp.2017.1743editor</a>\n" +
                  "                     </div>\n" +
                  "                   </div>\n" +
                  "                   \n" +
                  "                   <div class=\"publication-tabs ja\">\n" +
                  "                     <div class=\"tabs tabs-widget\">\n" +
                  "                       <ul class=\"tab-nav\" role=\"tablist\">\n" +
                  "        <li role=\"tab\" aria-selected=\"false\">\n" +
                  "    <a href=\"/doi/abs/10.1176/appi.ajp.2017.1743editor\" class=\"show-abstract\">\n" +
                  "    Citation\n" +
                  "    </a>\n" +
                  "        </li>\n" +
                  "        <li class=\"active\" role=\"tab\" aria-selected=\"true\">\n" +
                  "    <a href=\"/doi/full/10.1176/appi.ajp.2017.1743editor\" class=\"show-full\">\n" +
                  "    Full Text\n" +
                  "    </a>\n" +
                  "        </li>\n" +
                  "        <li role=\"tab\" aria-selected=\"false\">\n" +
                  "    <a href=\"http://ajp.psychiatryonline.org/doi/pdf/10.1176/appi.ajp.2017.1743editor\" class=\"show-pdf\" target=\"_self\">\n" +
                  "    PDF\n" +
                  "    </a>\n" +
                  "        </li>\n" +
                  "        <li role=\"tab\" aria-selected=\"false\">\n" +
                  "    <a href=\"http://ajp.psychiatryonline.org/doi/pdfplus/10.1176/appi.ajp.2017.1743editor\" target=\"_self\">\n" +
                  "    PDF Plus\n" +
                  "    </a>\n" +
                  "        </li>\n" +
                  "        <li role=\"tab\" aria-selected=\"false\">\n" +
                  "    <a href=\"#relatedContent\">\n" +
                  "    Related\n" +
                  "    </a>\n" +
                  "        </li>\n" +
                  "                       </ul>\n" +
                  "                       <div class=\"tab-content \">\n" +
                  "        <a id=\"top-content-scroll\"></a>\n" +
                  "        <div class=\"tab tab-pane active\">\n" +
                  "    <article class=\"article\">\n" +
                  "      <p class=\"fulltext\"></p>\n" +
                  "      <!-- abstract content -->\n" +
                  "      <div class=\"hlFld-Abstract\">\n" +
                  "    <p class=\"fulltext\"></p>\n" +
                  "      </div>\n" +
                  "      <!-- /abstract content --><!-- fulltext content -->\n" +
                  "      <div class=\"hlFld-Fulltext\">\n" +
                  "    <p>The <i>Journal</i> was notified about a complaint (<a class=\"ext-link\" href=\"http://ajp.psychiatryonline.org/doi/full/10.1176/appi.ajp.158.6.906\" target=\"_blank\">http://dx.doi.org/10.1176/appi.ajp.158.6.906</a>).</p>\n" +
                  "      </div>\n" +
                  "      <!-- /fulltext content -->\n" +
                  "      <div class=\"response\">\n" +
                  "    <div class=\"sub-article-title\"></div>\n" +
                  "      </div>\n" +
                  "    </article>\n" +
                  "        </div>\n" +
                  "        \n" +
                  "        \n" +
                  "                       </div>\n" +
                  "                     </div>\n" +
                  "                   </div>\n" +
                  "                   <input id=\"viewLargeImageCaption\" value=\"View Large Image\" type=\"hidden\">\n" +
                  "                 </div>\n" +
                  "               </div>\n" +
                  "             </div>\n" +
                  "           </div>\n" +
                  "         </div>\n" +
                  "         <div class=\"width_1_3\">\n" +
                  "           <div data-pb-dropzone=\"right\" class=\"pb-autoheight\">\n" +
                  "             <div class=\"widget literatumArticleToolsWidget none  widget-none\" id=\"2a984b58-2a84-4286-a261-a8108df4d8a8\"><div class=\"wrapped \"><div class=\"widget-body body body-none \"><div class=\"articleTools\"><ul class=\"linkList blockLinks separators centered\"><li class=\"downloadCitations\"><a href=\"/action/showCitFormats?doi=10.1176%2Fappi.ajp.2017.1743editor\">Send to Citation Mgr</a></li></ul></div></div></div></div>\n" +
                  "             <div class=\"widget layout-two-columns none  widget-none  widget-compact-all\">\n" +
                  "               <div class=\"wrapped \">\n" +
                  "                 <div class=\"widget-body body body-none  body-compact-all\">\n" +
                  "                   <div class=\"pb-columns row-fluid gutterless\">\n" +
                  "                     <div class=\"width_5_24\">\n" +
                  "                       <div data-pb-dropzone=\"left\" class=\"pb-autoheight\">\n" +
                  "        <div class=\"widget general-html none  widget-none  widget-compact-all\" id=\"1e879678-c84a-44e1-9a30-da2e26c3da18\">\n" +
                  "    <div class=\"wrapped \">\n" +
                  "      <div class=\"widget-body body body-none  body-compact-all\">\n" +
                  "    <script type=\"text/javascript\"></script>\n" +
                  "      </div>\n" +
                  "    </div>\n" +
                  "        </div>\n" +
                  "                       </div>\n" +
                  "                     </div>\n" +
                  "                     <div class=\"width_19_24\">\n" +
                  "                       <div data-pb-dropzone=\"right\" class=\"pb-autoheight\">\n" +
                  "        \n" +
                  "                       </div>\n" +
                  "                     </div>\n" +
                  "                   </div>\n" +
                  "                 </div>\n" +
                  "               </div>\n" +
                  "             </div>\n" +
                  "             <div class=\"widget layout-frame none inner-white widget-emphasis\" id=\"f4410c40-ec02-421f-a474-5b07264d4858\">\n" +
                  "               <div class=\"wrapped \">\n" +
                  "                 <h1 class=\"widget-header header-regular \">Related Articles</h1>\n" +
                  "                 \n" +
                  "               </div>\n" +
                  "               \n" +
                  "             </div>\n" +
                  "           </div>\n" +
                  "         </div>\n" +
                  "       </div>\n" +
                  "     </div>\n" +
                  "                    </div>\n" +
                  "                  </div>\n" +
                  "                </div>\n" +
                  "              </div>\n" +
                  "              <!--end pagefulltext-->\n" +
                  "            </div>\n" +
                  "          </div>\n" +
                  "        </div>\n" +
                  "        <div class=\"widget pageFooter none  widget-none  widget-compact-all\" id=\"857d8e34-9b91-4e39-a31f-dc42600dc42e\">\n" +
                  "          <div class=\"wrapped \">\n" +
                  "            <div class=\"widget-body body body-none  body-compact-all\">\n" +
                  "              \n" +
                  "            </div>\n" +
                  "          </div>\n" +
                  "        </div>\n" +
                  "        <div class=\"widget general-html none  widget-none\" id=\"9b0d00cb-3a46-4474-a164-2b3bc16b9005\">\n" +
                  "          <div class=\"wrapped \" id=\"open-athens-js\">\n" +
                  "          </div>\n" +
                  "        </div>\n" +
                  "      </div>\n" +
                  "    </div>\n" +
                  "  </body>\n" +
                  "</html>";
  
  private static final String art1ContentHashFiltered = "<div class=\"widget literatumPublicationContentWidget none  widget-none  widget-compact-all\" >\n" +
          "               <div class=\"wrapped \">\n" +
          "                 <div class=\"widget-body body body-none  body-compact-all\">\n" +
          "                   <div class=\"articleMeta ja\">\n" +
          "                     <div class=\"tocHeading\">\n" +
          "                       <b>Editor's Note</b>\n" +
          "                     </div>\n" +
          "                     <div class=\"publicationContentTitle\">\n" +
          "                       <h1>\n" +
          "        EDITOR'S NOTE\n" +
          "                       </h1>\n" +
          "                     </div>\n" +
          "                     \n" +
          "                     <div class=\"publicationContentAuthors\">\n" +
          "                       <div class=\"hlFld-ContribAuthor\"></div>\n" +
          "                     </div>\n" +
          "                     \n" +
          "                     <div class=\"authInfo authInfoLink\" style=\"display: none;\"><a href=\"#\" onclick=\"scrollToAuthAndArtInformation();\">View Author and Article Information</a></div>\n" +
          "                     <div>\n" +
          "                     </div>\n" +
          "                     <div class=\"publicationContentDoi publicationContentEpubDate\">\n" +
          "                       Published online: March 01, 2017\n" +
          "                       &nbsp;|&nbsp;\n" +
          "                       <a href=\"https://doi.org/10.1176/appi.ajp.2017.1743editor\">https://doi.org/10.1176/appi.ajp.2017.1743editor</a>\n" +
          "                     </div>\n" +
          "                   </div>\n" +
          "                   <div class=\"articleToolsDropZone\" data-pb-dropzone=\"articleToolsDropZone\">\n" +
          "                   </div>\n" +
          "                   <div class=\"publication-tabs ja\">\n" +
          "                     <div class=\"tabs tabs-widget\">\n" +
          "                       \n" +
          "                       <div class=\"tab-content \">\n" +
          "        <a ></a>\n" +
          "        <div class=\"tab tab-pane active\">\n" +
          "    <article class=\"article\">\n" +
          "      <p class=\"fulltext\"></p>\n" +
          "      \n" +
          "      <div class=\"hlFld-Abstract\">\n" +
          "    <p class=\"fulltext\"></p>\n" +
          "      </div>\n" +
          "      \n" +
          "      <div class=\"hlFld-Fulltext\">\n" +
          "    <p>The <i>Journal</i> was notified about a complaint (<a class=\"ext-link\" href=\"http://ajp.psychiatryonline.org/doi/full/10.1176/appi.ajp.158.6.906\" target=\"_blank\">http://dx.doi.org/10.1176/appi.ajp.158.6.906</a>).</p>\n" +
          "      </div>\n" +
          "      \n" +
          "      \n" +
          "    </article>\n" +
          "        </div>\n" +
          "        \n" +
          "        \n" +
          "                       </div>\n" +
          "                     </div>\n" +
          "                   </div>\n" +
          "                   <input  value=\"View Large Image\" type=\"hidden\">\n" +
          "                 </div>\n" +
          "               </div>\n" +
          "             </div><div class=\"hlFld-Abstract\">\n" +
          "    <p class=\"fulltext\"></p>\n" +
          "      </div><div class=\"hlFld-Fulltext\">\n" +
          "    <p>The <i>Journal</i> was notified about a complaint (<a class=\"ext-link\" href=\"http://ajp.psychiatryonline.org/doi/full/10.1176/appi.ajp.158.6.906\" target=\"_blank\">http://dx.doi.org/10.1176/appi.ajp.158.6.906</a>).</p>\n" +
          "      </div>";

   /*
  private static final String art1ContentHashFiltered = 
      "" + 
      " ";
   */
  
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
  
  private static final String citContentHashFiltered = 
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
   /*
  private static final String citContentHashFiltered = 
      " Download article citation data for:" + 
      " research in progress" + 
      " A Author" +
      " journal Name" + 
      " 2017" + 
      " 99 : 9 ," + 
      " ";
   */

  // Publisher updated their html source on 05/2020
  private static final String updateHtmlSource = "" +
          "<h1 class=\"citation__title\">h1 title</h1>\n" +
          "<div class=\"epub-section\">Article title</div>\n" +
          "<div class=\"hlFld-Abstract\">Abstracted content</div>\n" +
          "<div class=\"hlFld-Fulltext\">Full text content</div>\n" +
          "<div class=\"table-of-content\">Toc page content</div>";
  
  private static final String filteredUpdateHtmlSource = "<h1 class=\"citation__title\">h1 title</h1><div class=\"epub-section\">Article title</div><div class=\"hlFld-Abstract\">Abstracted content</div><div class=\"hlFld-Fulltext\">Full text content</div><div class=\"table-of-content\">Toc page content</div>";


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
    String hashed = StringUtil.fromInputStream(actIn);
    
    assertEquals(expectedStr, hashed);
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
  public static class TestCrawl extends TestAmPsychPubHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new AmPsychPubHtmlCrawlFilterFactory();
      doFilterTest(mau, variantFact, manifestContent, manifestContent);
      doFilterTest(mau, variantFact, tocContent, tocContentCrawlFiltered);
      doFilterTest(mau, variantFact, art1Content, art1ContentCrawlFiltered);
      doFilterTest(mau, variantFact, citContent, citContent);
    }
  }
  
  // Variant to test with Hash Filter
   public static class TestHash extends TestAmPsychPubHtmlFilterFactory {
     public void testFiltering() throws Exception {
      variantFact = new AmPsychPubHtmlHashFilterFactory();
      doFilterTest(mau, variantFact, manifestContent, manifestContentFiltered);
      doFilterTest(mau, variantFact, tocContent, tocContentHashFiltered);
      doFilterTest(mau, variantFact, art1Content, art1ContentHashFiltered);
      doFilterTest(mau, variantFact, citContent, citContentHashFiltered);
      doFilterTest(mau, variantFact, updateHtmlSource, filteredUpdateHtmlSource);
     }
   }
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

