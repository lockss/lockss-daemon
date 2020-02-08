/*
 * $Id$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.sage;

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
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;
import org.lockss.test.*;

public class TestSageHtmlFilterFactory extends LockssTestCase {
  
  FilterFactory variantFact;
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;
  
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.atypon.sage.ClockssSageAtyponJournalsPlugin";
  
  
  private static final String manifestContent = 
		  "<!DOCTYPE html>" +
				  "<html>" +
				  "<head>" +
				  "    <title>Crime &amp; Delinquency 2017 CLOCKSS Manifest Page</title>" +
				  "    <meta charset=\"UTF-8\" />" +
				  "</head>" +
				  "<body>" +
				  "<h1>Crime &amp; Delinquency 2017 CLOCKSS Manifest Page</h1>" +
				  "<ul>" +
				  "    <li><a href=\"/toc/cadc/63/14\">December 2017 (Vol. 63 Issue 14 Page 1807-1967)</a></li>" +
				  "    " +
				  "    <li><a href=\"/toc/cadc/63/13\">December 2017 (Vol. 63 Issue 13 Page 1655-1803)</a></li>" +
				  "</ul>" +
				  "<p>" +
				  "    <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>" +
				  "    CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit." +
				  "</p>" +
				  "</body>" +
				  "</html>";

  
  // sage removes tags 
  private static final String manifestHashFiltered = 
				  " December 2017 (Vol. 63 Issue 14 Page 1807-1967) " +
				  "December 2017 (Vol. 63 Issue 13 Page 1655-1803) ";
  private static final String tocContent = 
		  "<div> " + 
		  "<span class=\"TocHeading\"><legend class=\"tocListTitle\"><h1>Table of Contents</h1></legend> " + 
		  "</span><span><h2 class=\" currentIssue\"> </h2> </span> " + 
	      "</div> " +
	      "<h3 class=\"tocListHeader\"> Volume 63, Issue 8, July 2017 </h3> " + 
		  "<div class=\"tocListtDropZone2\" data-pb-dropzone=\"tocListtDropZone2\"> </div> " + 
		  "<div class=\"tocContent\"> " + 
		  "<!--totalCount6--><!--modified:1525656417000-->" +
		  "<h2 class=\"tocHeading\">" +
		  "<a class=\"header-aware-toc-section-anchor\" name=\"sage_toc_section_Articles\"></a>" +
		  "<div class=\"subject heading-1\">Articles</div>" +
		  "</h2>" +
		  "<table border=\"0\" width=\"100%\" class=\"articleEntry\">" +
		  "<tr>" +
		  "<td valign=\"top\">" +
		  "<span class=\"ArticleType\">Articles</span>" +
		  "<div class=\"art_title linkable\">" +
		  "<a data-item-name=\"click-article-title\" class=\"ref nowrap\" href=\"/doi/full/10.1177/0011128715574977\">" +
		  "<h3 class=\"heading-title\">" +
		  "<span class=\"hlFld-Title\">Assault of Police</span>" +
		  "</h3>" +
		  "</a>" +
		  "</div>" +
		  "</td>" +
		  "</tr>" +
		  "</table></div>";

    private static final String tocContentFiltered =
		  " Articles Assault of Police ";


    private static final String tocContent2 = "<div class=\"tocContent\">\n" +
            "    <h2 class=\"tocHeading\">\n" +
            "        <a class=\"header-aware-toc-section-anchor\" name=\"sage_toc_section_Article\"></a>\n" +
            "        <div class=\"subject heading-1\">Article</div>\n" +
            "    </h2>\n" +
            "    <table border=\"0\" width=\"100%\" class=\"articleEntry\">\n" +
            "        <tr>\n" +
            "            <td class=\"accessIconContainer\">\n" +
            "                <div><img src=\"/templates/jsp/_style2/_sage/images/access_free.gif\" alt=\"Free Access\" title=\"Free Access\" class=\"accessIcon freeAccess\" /></div>\n" +
            "                <div class=\"ccIcon\"></div>\n" +
            "            </td>\n" +
            "            <td align=\"right\" valign=\"top\" width=\"10\"></td>\n" +
            "            <td valign=\"top\">\n" +
            "                <span class=\"ArticleType\">Article</span>\n" +
            "                <div class=\"art_title linkable\">\n" +
            "                    <a data-item-name=\"click-article-title\" class=\"ref nowrap\" href=\"/doi/pdf/10.1177/0976343020120101\">\n" +
            "                        <h3 class=\"heading-title\"><span class=\"hlFld-Title\">Spatiality and Perspective for Pilgrimage-Tourism</span></span></h3>\n" +
            "                    </a>\n" +
            "                </div>\n" +
            "                <div class=\"tocAuthors afterTitle\">\n" +
            "                    <div class=\"articleEntryAuthor all\">\n" +
            "                                                                                                <span class=\"articleEntryAuthorsLinks\">\n" +
            "                                                                                                   <span class=\"contribDegrees\">\n" +
            "                                                                                                      <a class=\"entryAuthor\" href=\"/action/doSearch?target=default&amp;ContribAuthorStored=Singh%2C+Rana+PB+Prof\" aria-label=\"Open contributor information pop-up for Rana P.B. Singh, Prof.\"> Rana P.B. Singh, Prof.</a>\n" +
            "                                                                                                      <div class=\"authorLayer\">\n" +
            "                                                                                                         <div class=\"header\"><a class=\"entryAuthor\" href=\"/action/doSearch?target=default&amp;ContribAuthorStored=Singh%2C+Rana+PB+Prof\" aria-label=\"Open contributor information pop-up for Rana P.B. Singh, Prof.\"> Rana P.B. Singh, Prof.</a></div>\n" +
            "                                                                                                         <div class=\"author-section-div\">\n" +
            "                                                                                                            <a class=\"entryAuthor\" href=\"/action/doSearch?target=default&amp;ContribAuthorStored=Singh%2C+Rana+PB+Prof\"><span><a class=\"entryAuthor\" href=\"/action/doSearch?target=default&amp;ContribAuthorStored=Singh, Rana PB Prof\">See all articles</a> by this author</span></a>\n" +
            "                                                                                                            <div><a class=\"entryAuthor\" target=\"_blank\" href=\"/action/searchDispatcher?searchService=scholar&amp;author=Singh, Rana PB Prof\">\n" +
            "                                                                                                               Search Google Scholar\n" +
            "                                                                                                               </a> for this author\n" +
            "                                                                                                            </div>\n" +
            "                                                                                                         </div>\n" +
            "                                                                                                      </div>\n" +
            "                                                                                                   </span>\n" +
            "                                                                                                </span>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div class=\"art_meta citation\"></div>\n" +
            "                <span class=\"tocEPubDate\"><span class=\"maintextleft\"><span class=\"ePubDateLabel\">First Published </span>September 5, 2019</span></span><span class=\"articlePageRange\">; pp. 1–37</span>\n" +
            "                <div class=\"showAbstract\">\n" +
            "                    <div class=\"abstract-section\"><a class=\"abstract-link PDF-link\" href=\"/doi/abs/10.1177/0976343020120101\">Abstract</a></div>\n" +
            "                </div>\n" +
            "                <div class=\"tocDeliverFormatsLinks\">\n" +
            "                    <a class=\"ref nowrap abs\" href=\"/doi/abs/10.1177/0976343020120101\">Citation</a><a data-item-name=\"download-PDF\" class=\"ref nowrap pdf\" target=\"_blank\" title=\"PDF Download\" href=\"/doi/pdf/10.1177/0976343020120101\"><img alt=\"PDF Download\" title=\"PDF Download\" src=\"/templates/jsp/_style2/_sage/images/pdf-icon-large.png\" /></a>\n" +
            "                    <div id=\"Abs0976343020120101\" class=\"previewViewSection tocPreview\">\n" +
            "                        <div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1177/0976343020120101', 'Abs0976343020120101');\"></div>\n" +
            "                        <p class=\"previewContent\"></p>\n" +
            "                    </div>\n" +
            "                    <span class=\"ref-SFXLink sfxLink-toc\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26issn%3D0972-558X%26atitle%3DThe%252051%2520Shakti%2520Pithas%2520in%2520South%2520Asia%26pub%3DSAGE%2520Publications%2520India%26aulast%3DSingh%26aufirst%3DRana%2520P.B.%26date%3D2012%26date%3D2019%26volume%3D12%26issue%3D1%26spage%3D1\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span>\n" +
            "                </div>\n" +
            "            </td>\n" +
            "        </tr>\n" +
            "    </table>\n" +
            "    <table border=\"0\" width=\"100%\" class=\"articleEntry\">\n" +
            "        <tr>\n" +
            "            <td class=\"accessIconContainer\">\n" +
            "                <div><img src=\"/templates/jsp/_style2/_sage/images/access_free.gif\" alt=\"Free Access\" title=\"Free Access\" class=\"accessIcon freeAccess\" /></div>\n" +
            "                <div class=\"ccIcon\"></div>\n" +
            "            </td>\n" +
            "            <td align=\"right\" valign=\"top\" width=\"10\"></td>\n" +
            "            <td valign=\"top\">\n" +
            "                <span class=\"ArticleType\">Article</span>\n" +
            "                <div class=\"art_title linkable\">\n" +
            "                    <a data-item-name=\"click-article-title\" class=\"ref nowrap\" href=\"/doi/pdf/10.1177/0976343020120102\">\n" +
            "                        <h3 class=\"heading-title\"><span class=\"hlFld-Title\">Domestic Arrangements and Fictive Kinship among Nigerian Migrants in Central Durban, South Africa</span></h3>\n" +
            "                    </a>\n" +
            "                </div>\n" +
            "                <div class=\"tocAuthors afterTitle\">\n" +
            "                    <div class=\"articleEntryAuthor all\">\n" +
            "                                                                                                <span class=\"articleEntryAuthorsLinks\">\n" +
            "                                                                                                   <span class=\"contribDegrees\">\n" +
            "                                                                                                      <a class=\"entryAuthor\" href=\"/action/doSearch?target=default&amp;ContribAuthorStored=Singh%2C+Anand\" aria-label=\"Open contributor information pop-up for Anand Singh\"> Anand Singh</a>\n" +
            "                                                                                                      <div class=\"authorLayer\">\n" +
            "                                                                                                         <div class=\"header\"><a class=\"entryAuthor\" href=\"/action/doSearch?target=default&amp;ContribAuthorStored=Singh%2C+Anand\" aria-label=\"Open contributor information pop-up for Anand Singh\"> Anand Singh</a></div>\n" +
            "                                                                                                         <div class=\"author-section-div\">\n" +
            "                                                                                                            <a class=\"entryAuthor\" href=\"/action/doSearch?target=default&amp;ContribAuthorStored=Singh%2C+Anand\"><span><a class=\"entryAuthor\" href=\"/action/doSearch?target=default&amp;ContribAuthorStored=Singh, Anand\">See all articles</a> by this author</span></a>\n" +
            "                                                                                                            <div><a class=\"entryAuthor\" target=\"_blank\" href=\"/action/searchDispatcher?searchService=scholar&amp;author=Singh, Anand\">\n" +
            "                                                                                                               Search Google Scholar\n" +
            "                                                                                                               </a> for this author\n" +
            "                                                                                                            </div>\n" +
            "                                                                                                         </div>\n" +
            "                                                                                                      </div>\n" +
            "                                                                                                   </span>\n" +
            "                                                                                                </span>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div class=\"art_meta citation\"></div>\n" +
            "                <span class=\"tocEPubDate\"><span class=\"maintextleft\"><span class=\"ePubDateLabel\">First Published </span>September 5, 2019</span></span><span class=\"articlePageRange\">; pp. 39–55</span>\n" +
            "                <div class=\"showAbstract\">\n" +
            "                    <div class=\"abstract-section\"><a class=\"abstract-link PDF-link\" href=\"/doi/abs/10.1177/0976343020120102\">Abstract</a></div>\n" +
            "                </div>\n" +
            "                <div class=\"tocDeliverFormatsLinks\">\n" +
            "                    <a class=\"ref nowrap abs\" href=\"/doi/abs/10.1177/0976343020120102\">Citation</a><a data-item-name=\"download-PDF\" class=\"ref nowrap pdf\" target=\"_blank\" title=\"PDF Download\" href=\"/doi/pdf/10.1177/0976343020120102\"><img alt=\"PDF Download\" title=\"PDF Download\" src=\"/templates/jsp/_style2/_sage/images/pdf-icon-large.png\" /></a>\n" +
            "                    <div id=\"Abs0976343020120102\" class=\"previewViewSection tocPreview\">\n" +
            "                        <div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1177/0976343020120102', 'Abs0976343020120102');\"></div>\n" +
            "                        <p class=\"previewContent\"></p>\n" +
            "                    </div>\n" +
            "                    <span class=\"ref-SFXLink sfxLink-toc\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26issn%3D0972-558X%26atitle%3DDomestic%2520Arrangements%2520and%2520Fictive%2520Kinship%2520among%2520Nigerian%2520Migrants%2520in%2520Central%2520Durban%252C%2520South%2520Africa%25E2%2588%2597%26pub%3DSAGE%2520Publications%2520India%26aulast%3DSingh%26aufirst%3DAnand%26date%3D2012%26date%3D2019%26volume%3D12%26issue%3D1%26spage%3D39\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span>\n" +
            "                </div>\n" +
            "            </td>\n" +
            "        </tr>\n" +
            "    </table>\n" +
            "</div>";

    private static final String tocContent2Filtered = " Article Spatiality and Perspective for Pilgrimage-Tourism Rana P.B. Singh, Prof. Rana P.B. Singh, Prof. See all articles by this author Search Google Scholar for this author First Published September 5, 2019 ; pp. 1 37 Abstract Citation Domestic Arrangements and Fictive Kinship among Nigerian Migrants in Central Durban, South Africa Anand Singh Anand Singh See all articles by this author Search Google Scholar for this author First Published September 5, 2019 ; pp. 39 55 Abstract Citation ";

    private static final String tocContent3 = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" class=\"pb-page\" data-request-id=\"dc3c01c2-69c1-4a4c-8161-b6219cccbd1a\">\n" +
            "<head data-pb-dropzone=\"head\">\n" +
            "    <script>\n" +
            "         var dataLayer = dataLayer ||[];\n" +
            "         dataLayer.push({\"site\":{\"environment\":\"live\",\"platform\":\"responsive-web\"},\"page\":{\"title\":\"\",\"type\":\"table-of-contents\"},\"user\":{\"action\":\"showToc\",\"type\":[],\"loginStatus\":false,\"authentication\":true,\"authenticationType\":\"[IP]\",\"institutionType\":\"[institution]\",\"institution\":[\"Stanford University\",\"NERL\"]},\"product\":{\"type\":\"issue\",\"format\":\"electronic\",\"journal\":{\"name\":\"The Oriental Anthropologist\",\"tla\":\"OAN\",\"category\":[],\"subCategory\":[],\"open_access\":\"false\",\"e_issn\":\"0976-3430\",\"p_issn\":\"0972-558X\",\"issue\":{\"volume\":\"12\",\"number\":\"1\"}}}});\n" +
            "      </script>\n" +
            "    <script>(function (w, d, s, l, i) {\n" +
            "         w[l] = w[l] || [];\n" +
            "         w[l].push(\n" +
            "                 {'gtm.start': new Date().getTime(), event: 'gtm.js'}\n" +
            "         );\n" +
            "         var f = d.getElementsByTagName(s)[0],\n" +
            "                 j = d.createElement(s), dl = l != 'dataLayer' ? '&l=' + l : '';\n" +
            "         j.async = true;\n" +
            "         j.src =\n" +
            "                 '//www.googletagmanager.com/gtm.js?id=' + i + dl;\n" +
            "         f.parentNode.insertBefore(j, f);\n" +
            "         })(window, document, 'script', 'dataLayer', 'GTM-5M58KS');\n" +
            "      </script>\n" +
            "    <meta name=\"pbContext\" content=\";wgroup:string:SAGE Publication Websites;ctype:string:Journal Content;requestedJournal:journal:oana;page:string:Table of Contents;website:website:sage;csubtype:string:Regular Issue;issue:issue:10.1177/OANA_12_1;pageGroup:string:Publication Pages;journal:journal:oana\" />\n" +
            "    <meta name=\"twitter:card\" content=\"summary_large_image\">\n" +
            "    <meta property=\"og:locale\" content=\"en_US\">\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"robots\" content=\"noarchive\" />\n" +
            "    <meta name=\"pb-robots-disabled\">\n" +
            "    <meta property=\"og:title\" content=\"The Oriental Anthropologist - Volume 12, Number 1, Jun 01, 2012\" />\n" +
            "    <meta property=\"og:type\" content=\"article\">\n" +
            "    <meta property=\"og:url\" content=\"https://journals.sagepub.com/toc/oana/12/1\" />\n" +
            "    <meta property=\"og:image:width\" content=\"600\" />\n" +
            "    <meta property=\"og:image:height\" content=\"900\" />\n" +
            "    <meta name=\"twitter:image\" content=\"https://journals.sagepub.com/action/showCoverImage?journalCode=oana\" />\n" +
            "    <meta property=\"og:image\" content=\"https://journals.sagepub.com/action/showCoverImage?journalCode=oana\" />\n" +
            "    <meta property=\"og:site_name\" content=\"SAGE Journals\" />\n" +
            "    <meta property=\"og:description\" content=\"Table of contents for The Oriental Anthropologist: A Bi-annual International Journal of the Science of Man, 12, 1, Jun 01, 2012\" />\n" +
            "    <link rel=\"canonical\" href=\"/toc/oan/12/1\" />\n" +
            "    <title>The Oriental Anthropologist - Volume 12, Number 1, Jun 01, 2012</title>\n" +
            "    <meta name=\"description\" content=\"Table of contents for The Oriental Anthropologist: A Bi-annual International Journal of the Science of Man, 12, 1, Jun 01, 2012\">\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/wro/i7wh~product.css\">\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb/css/t1580277876000-v1580277876000/head_1_6_7.css\" id=\"pb-css\" data-pb-css-id=\"t1580277876000-v1580277876000/head_1_6_7.css\" />\n" +
            "    <script type=\"text/javascript\">\n" +
            "         if (window.location.hash && window.location.hash == '#_=_') {\n" +
            "             window.location.hash = '';\n" +
            "         }\n" +
            "      </script>\n" +
            "    <script type=\"text/javascript\" src=\"/wro/i7wh~product.js\"></script>\n" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://stats.g.doubleclick.net\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://tpc.googlesyndication.com\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://pagead2.googlesyndication.com\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://securepubads.g.doubleclick.net\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://cdn.ampproject.org\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://api.altmetric.com\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://s7.addthis.com\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://m.addthis.com\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://www.deepdyve.com\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://sage.msgfocus.com\" crossorigin>\n" +
            "    <link rel=\"preconnect\" crossorigin=\"crossorigin\" href=\"https://widgets.sagepub.com\" crossorigin>\n" +
            "    <link rel=\"dns-prefetch\" href=\"https://sadmin.brightcove.com\">\n" +
            "    <link rel=\"dns-prefetch\" href=\"https://w.usabilla.com\">\n" +
            "    <link rel=\"dns-prefetch\" href=\"https://rum-collector-2.pingdom.net\">\n" +
            "    <script>\n" +
            "         if (typeof jQuery==='undefined') {\n" +
            "         \tdocument.write(unescape('\\x3Clink rel=\"stylesheet\" type=\"text/css\" href=\"/wro/h7sc/product.css\"\\x3E'));\n" +
            "         \tdocument.write(unescape('\\x3Cscript src=\"/wro/h7sc/product.js\"\\x3E\\x3C/script\\x3E'));\n" +
            "         }\n" +
            "      </script>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb-assets/CSS/global-1580468678270.css\">\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb-assets/CSS/temp-1553094239500.css\">\n" +
            "    <link rel=\"preload\" type=\"text/css\" href=\"//widgets.sagepub.com/bookshelf/css/all.css\" as=\"style\">\n" +
            "    <link rel=\"apple-touch-icon\" href=\"/pb-assets/Icons/sj-apple-touch-1513073710420.png\">\n" +
            "    <meta property=\"og:image\" content=\"/pb-assets/Images/SJ_Twitter_Card-1557144152440.jpg\">\n" +
            "    <meta name=\"google-site-verification\" content=\"6b_U8gTbwxPAJSSIOBxO9mKsWVht9WAMuijMWNDwNCs\" />\n" +
            "    <script>\n" +
            "         //if ($('meta[property=\"og:image\"]').length===0)\n" +
            "         //\t$('meta[property=\"fallback:image\"]').attr('property','og:image');\n" +
            "         // Remove HTML tag-items from the page tile\n" +
            "         //document.title=document.title.replace(/<\\/?[^>]+(>|$)/g, \"\");\n" +
            "         function CM8ShowAd(ph) {} // fallback function \"masking\" CM8\n" +
            "         var googletag = googletag || {};\n" +
            "         googletag.cmd = googletag.cmd || [];\n" +
            "         var dfpSlotsReady=false;\n" +
            "         var globalAdParams={\n" +
            "         portal: {\n" +
            "         \tname: 'Portal',\n" +
            "         \tslot: 'portal_site.home',\n" +
            "         \tcode: '1530873931903'\n" +
            "         },\n" +
            "         search: {\n" +
            "         \tname: 'Search',\n" +
            "         \tslot: 'portal_site.search',\n" +
            "         \tcode: '1530874010820'\n" +
            "         },\n" +
            "         browse: {\n" +
            "         \tname: 'Browse',\n" +
            "         \tslot: 'portal_site.browse',\n" +
            "         \tcode: '1530874064610'\n" +
            "         }\n" +
            "         };\n" +
            "         var journalAdParams = {\n" +
            "         \tserviceName: '{}',\n" +
            "         \tdfp_slot: '{}',\n" +
            "         \talpha_code: 'OAN'\n" +
            "         };\n" +
            "\n" +
            "         var dmdEnabledJournals=['EAR', 'OTO']; //SAGE-4037\n" +
            "         if (dmdEnabledJournals.indexOf('OAN')>-1) {\n" +
            "         \t(function(w,d,s,m,n,t){\n" +
            "         \t\tw[m]=w[m]||{init:function(){(w[m].q=w[m].q||[]).push(arguments);},ready:function(c){if('function'!=typeof c){return;}(w[m].c=w[m].c||[]).push(c);c=w[m].c;\n" +
            "         \t\tn.onload=n.onreadystatechange=function(){if(!n.readyState||/loaded|complete/.test(n.readyState)){n.onload=n.onreadystatechange=null;\n" +
            "         \t\tif(t.parentNode&&n.parentNode){t.parentNode.removeChild(n);}while(c.length){(c.shift())();}}};}},w[m].d=1*new Date();n=d.createElement(s);t=d.getElementsByTagName(s)[0];\n" +
            "         \t\tn.async=1;n.src='//www.medtargetsystem.com/javascript/beacon.js?'+(Date.now().toString()).substring(0,5);n.setAttribute(\"data-aim\",m);t.parentNode.insertBefore(n,t);\n" +
            "         \t})(window,document,'script','AIM_126');\n" +
            "\n" +
            "         \tAIM_126.init('126-610-A5ECBB04');\n" +
            "         \tconsole.debug('AIM_126 initialized');\n" +
            "         }\n" +
            "      </script>\n" +
            "    <script src=\"/pb-assets/JS/xhrCatch-1564476093570.js\"></script>\n" +
            "    <script src=\"/pb-assets/JS/js.cookie.min-1517227616353.js\"></script>\n" +
            "    <script data-cfasync=\"false\" src=\"/pb-assets/JS/jquery.actual.min-1513079278967.js\"></script>\n" +
            "    <script data-cfasync=\"false\" src=\"/pb-assets/JS/imagesloaded.pkgd.min-1523539547607.js\"></script>\n" +
            "    <script data-cfasync=\"false\" src=\"/pb-assets/JS/listExpander-1580296423817.js\"></script>\n" +
            "    <script data-cfasync=\"false\" src=\"/pb-assets/JS/listExpander-ie-1580296424420.js\"></script>\n" +
            "    <script data-cfasync=\"false\" src=\"/pb-assets/JS/societiesModal-1560270859740.js\"></script>\n" +
            "    <script data-cfasync=\"false\" src=\"/pb-assets/JS/sj-utils-1575909647957.js\"></script>\n" +
            "    <script data-cfasync=\"false\" src=\"/pb-assets/JS/portalAutomation-1580469837247.js\"></script>\n" +
            "    <script src=\"/pb-assets/JS/authorsPatch-1542815689637.js\"></script>\n" +
            "    <script src=\"/pb-assets/microsite/lib/microsite-dfp-1560847671230.js\"></script>\n" +
            "    <script src=\"/pb-assets/JS/dfp-1572436488633.js\"></script>\n" +
            "    <script>\n" +
            "         if (inJournalScope())\n" +
            "         \tinitJournalDfp();\n" +
            "         else if (inMicrositeScope())\n" +
            "         \tinitMicrositeDfp();\n" +
            "         else\n" +
            "         \tinitGlobalDfp();\n" +
            "      </script><script defer src=\"https://static.cloudflareinsights.com/beacon.min.js\" data-cf-beacon='{\"rayId\":\"55d393b6c8ac9e0d\",\"version\":\"2019.10.2\",\"startTime\":158038868353\n" +
            "      <link rel=\"canonical\" href=\"https://journals.sagepub.com/toc/oana/12/1\">\n" +
            "      <script>var _prum=[[' id','5847762c1e20722da47b23c6'],['mark','firstbyte',(new Date()).getTime()]];(function(){var s=document.getElementsByTagName('script')[0],p=document.createElement('script');p.async='async';p.src='//rum-static.pingdom.net/prum.min.js';s.parentNode.insertBefore(p,s);})();</script>\n" +
            "   </head>\n" +
            "   <body class=\"pb-ui\">\n" +
            "      <div class=\"totoplink\">\n" +
            "         <a id=\"skiptocontent\" class=\"skiptocontent\" href=\"#\" tabindex=\"1\" title=\"Skip to Content\" aria-live=\"polite\">Skip to main content</a>\n" +
            "      </div>\n" +
            "      <noscript>\n" +
            "         <iframe src=\"//www.googletagmanager.com/ns.html?id=GTM-5M58KS\"\n" +
            "            height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe>\n" +
            "      </noscript>\n" +
            "      <script type=\"text/javascript\">\n" +
            "         function initSelector(element){\n" +
            "             $('.skiptocontent').removeAttr(\"href\");\n" +
            "             $( element ).parent().before( \"<a id=\\\"top\\\"></a>\" );\n" +
            "             window.location.hash = '#top';\n" +
            "             $(window).scrollTop($(\"#top\").offset().top-100);\n" +
            "             history.pushState(\"\", document.title, window.location.pathname + window.location.search);\n" +
            "         }\n" +
            "\n" +
            "\n" +
            "         $('#skiptocontent').click(function (e) {\n" +
            "\n" +
            "             var code;\n" +
            "             try {\n" +
            "                 code = (window.event) ? window.event.keyCode : event.which;\n" +
            "             }\n" +
            "             catch(err) {\n" +
            "                 code = e.keyCode || e.which;\n" +
            "             }\n" +
            "\n" +
            "             //click Enter\n" +
            "\n" +
            "                     var mainPageId=$(\"#main-page-content\").text();\n" +
            "                 if(mainPageId){\n" +
            "                     initSelector('#main-page-content');\n" +
            "                 }else{\n" +
            "\n" +
            "                     var firstH1=$('h1:first').text();\n" +
            "                     if(firstH1){\n" +
            "                         initSelector('h1:first');\n" +
            "                     }else{\n" +
            "                        $('#skiptocontent').css('display','none');\n" +
            "                     }\n" +
            "                 }\n" +
            "\n" +
            "\n" +
            "         });\n" +
            "\n" +
            "\n" +
            "      </script>" +
            "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
            "        <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
            "            <div class=\"widget page-main-content none  widget-none  widget-compact-all\" id=\"dea397f8-1087-4823-917e-a9dd6b971630\">\n" +
            "                <div class=\"wrapped \">\n" +
            "                    <div class=\"widget-body body body-none  body-compact-all\">\n" +
            "                        <main role=\"main\" data-pb-dropzone=\"contents\"><div class=\"widget pageBody none  widget-none  widget-compact-all\" id=\"b90442ed-bd15-483c-9c3d-448b7c0604f7\">\n" +
            "                                <div class=\"wrapped \">\n" +
            "                                    <div class=\"widget-body body body-none  body-compact-all\">\n" +
            "                                        <div class=\"page-body pagefulltext\">\n" +
            "                                            <div data-pb-dropzone=\"main\">\n" +
            "                                                <div class=\"widget responsive-layout none TOCLeftColumn widget-none  widget-compact-all\" id=\"fbedf2f7-490a-4cc0-bce5-ea003c8f90dd\"><div class=\"wrapped \">\n" +
            "                                                        <div class=\"widget-body body body-none  body-compact-all\">\n" +
            "                                                            <div class=\"container-fluid\">\n" +
            "                                                                <div class=\"row row-md  \">\n" +
            "                                                                    <div class=\"col-md-1-1 \">\n" +
            "                                                                        <div class=\"contents\" data-pb-dropzone=\"contents0\"><div class=\"widget tocListWidget none sageTOCList widget-none  widget-compact-all\" id=\"ff3b1f93-4dc8-4cb1-bbd3-4e7770bfed90\"><div class=\"wrapped \"><div class=\"widget-body body body-none  body-compact-all\">" +
			"<div class=\"tocContent\"> tocContent..............</div>\n" +
			"</div></div></div></div></div></div></div></div></div></div></div></div></div></div></div>\n" +
            "</main></div></div></div></div></div>\n" +
            "    </body>\n" +
            "</html>";

    private static final String tocContent3Filtered = " fail here";

    private static final String tocContent4 = "";


    private static final String articleAccessDenialContent =
    "<div class=\"accessDenialDropZone1\" data-pb-dropzone=\"accessDenialDropZone1\">" +
    "Hello World" +
    "</div>" +
    "<div class=\"accessDenialDropZone2\" data-pb-dropzone=\"accessDenialDropZone2\">" +
    "Hello Kitty" +
    "</div>" +
    "<div id=\"accessDenialWidget\">" +
    "Hello Sailor" +
    "</div>" ;

  private static final String articleAccessDenialContentFiltered = "" ;

  private static final String absContent = "Abstract<div class=\"ecommAbs ja\">\n" +
          "                                                                                                    <article class=\"article\">\n" +
          "                                                                                                        <div class=\"tabs tabs-widget\">\n" +
          "                                                                                                            <ul class=\"tab-nav\" role=\"tablist\">\n" +
          "                                                                                                                <li class=\"active\" role=\"tab\">\n" +
          "                                                                                                                    <a href=\"/doi/abs/10.1606/1044-3894.2016.97.18\" class=\"show-abstract\">\n" +
          "                                                                                                                        Abstract\n" +
          "                                                                                                                    </a>\n" +
          "                                                                                                                </li>\n" +
          "                                                                                                                <li class=\"version-clone\" style=\"border:none;background:transparent;\">\n" +
          "                                                                                                                </li>\n" +
          "                                                                                                            </ul>\n" +
          "                                                                                                            <div class=\"tab-content \">\n" +
          "                                                                                                                <a id=\"top-content-scroll\"></a>\n" +
          "                                                                                                                <div class=\"tab tab-pane active\">\n" +
          "                                                                                                                    <article class=\"article\">\n" +
          "                                                                                                                        <div class=\"hlFld-Abstract\"><a name=\"abstract\"></a><div class=\"sectionInfo abstractSectionHeading\"><h2 class=\"sectionHeading\">Abstract</h2></div><div class=\"abstractSection abstractInFull\"><p>Abstract filtered content detail</p></div></div><a name=\"_i1\"></a><div class=\"sectionInfo\"><h2 class=\"sectionHeading\">References</h2></div><table border=\"0\" class=\"references\"><tr id=\"bibr1-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Altman, J. C. (<span class=\"NLM_year\">2008</span>). <span class=\"NLM_article-title\">Engaging families in child welfare services: Worker versus client perspectives</span>. Child Welfare, 87(3), <span class=\"NLM_fpage\">41</span>–<span class=\"NLM_lpage\">60</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2008&pages=41-60&issue=3&author=J.+C.+Altman&title=Engaging+families+in+child+welfare+services%3A+Worker+versus+client+perspectives\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr1-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DAltman%26aufirst%3DJ.%2520C.%26date%3D2008%26atitle%3DEngaging%2520families%2520in%2520child%2520welfare%2520services%253A%2520Worker%2520versus%2520client%2520perspectives%26title%3DChild%2520Welfare%26volume%3D87%26issue%3D3%26spage%3D41\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr2-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Antle, B. F. , Barbee, A. P. , Christensen, D. N. , &amp; Sullivan, D. (<span class=\"NLM_year\">2010</span>). <span class=\"NLM_article-title\">The prevention of child maltreatment recidivism through the Solution-Based Casework model of child welfare practice</span>. Children and Youth Services Review, 31, <span class=\"NLM_fpage\">1346</span>–<span class=\"NLM_lpage\">1351</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2010&pages=1346-1351&author=B.+F.+Antleauthor=A.+P.+Barbeeauthor=D.+N.+Christensenauthor=D.+Sullivan&title=The+prevention+of+child+maltreatment+recidivism+through+the+Solution-Based+Casework+model+of+child+welfare+practice\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr2-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DAntle%26aufirst%3DB.%2520F.%26aulast%3DBarbee%26aufirst%3DA.%2520P.%26aulast%3DChristensen%26aufirst%3DD.%2520N.%26aulast%3DSullivan%26aufirst%3DD.%26date%3D2010%26atitle%3DThe%2520prevention%2520of%2520child%2520maltreatment%2520recidivism%2520through%2520the%2520Solution-Based%2520Casework%2520model%2520of%2520child%2520welfare%2520practice%26title%3DChildren%2520and%2520Youth%2520Services%2520Review%26volume%3D31%26spage%3D1346\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr3-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Antle, B. , Christensen, D. , van Zyl, M. A. , &amp; Barbee, A. P. (<span class=\"NLM_year\">2012</span>). <span class=\"NLM_article-title\">The impact of the Solution Based Casework (SBC) practice model on federal outcomes in public child welfare</span>. Child Abuse and Neglect, 36, <span class=\"NLM_fpage\">342</span>–<span class=\"NLM_lpage\">353</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2012&pages=342-353&author=B.+Antleauthor=D.+Christensenauthor=M.+A.+van+Zylauthor=A.+P.+Barbee&title=The+impact+of+the+Solution+Based+Casework+%28SBC%29+practice+model+on+federal+outcomes+in+public+child+welfare\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr3-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DAntle%26aufirst%3DB.%26aulast%3DChristensen%26aufirst%3DD.%26aulast%3Dvan%2520Zyl%26aufirst%3DM.%2520A.%26aulast%3DBarbee%26aufirst%3DA.%2520P.%26date%3D2012%26atitle%3DThe%2520impact%2520of%2520the%2520Solution%2520Based%2520Casework%2520%2528SBC%2529%2520practice%2520model%2520on%2520federal%2520outcomes%2520in%2520public%2520child%2520welfare%26title%3DChild%2520Abuse%2520and%2520Neglect%26volume%3D36%26spage%3D342\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr4-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Barbee, A. P. , Christensen, D. , Antle, B. , Wandersman, A. , &amp; Cahn, K. (<span class=\"NLM_year\">2011</span>). <span class=\"NLM_article-title\">Successful adoption and implementation of a comprehensive casework practice model in a public child welfare agency: Application of the getting to outcomes (GTO) model</span>. Children &amp; Youth Services Review, 33, <span class=\"NLM_fpage\">622</span>–<span class=\"NLM_lpage\">633</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2011&pages=622-633&author=A.+P.+Barbeeauthor=D.+Christensenauthor=B.+Antleauthor=A.+Wandersmanauthor=K.+Cahn&title=Successful+adoption+and+implementation+of+a+comprehensive+casework+practice+model+in+a+public+child+welfare+agency%3A+Application+of+the+getting+to+outcomes+%28GTO%29+model\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr4-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DBarbee%26aufirst%3DA.%2520P.%26aulast%3DChristensen%26aufirst%3DD.%26aulast%3DAntle%26aufirst%3DB.%26aulast%3DWandersman%26aufirst%3DA.%26aulast%3DCahn%26aufirst%3DK.%26date%3D2011%26atitle%3DSuccessful%2520adoption%2520and%2520implementation%2520of%2520a%2520comprehensive%2520casework%2520practice%2520model%2520in%2520a%2520public%2520child%2520welfare%2520agency%253A%2520Application%2520of%2520the%2520getting%2520to%2520outcomes%2520%2528GTO%2529%2520model%26title%3DChildren%2520%2526%2520Youth%2520Services%2520Review%26volume%3D33%26spage%3D622\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr5-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Bolen, M. G. , McWey, L. M. , &amp; Schlee, B. M. (<span class=\"NLM_year\">2008</span>). <span class=\"NLM_article-title\">Are at-risk parents getting what they need? Perspectives of parents involved with child protective services</span>. Clinical Social Work Journal, 36, <span class=\"NLM_fpage\">341</span>–<span class=\"NLM_lpage\">354</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2008&pages=341-354&author=M.+G.+Bolenauthor=L.+M.+McWeyauthor=B.+M.+Schlee&title=Are+at-risk+parents+getting+what+they+need%3F+Perspectives+of+parents+involved+with+child+protective+services\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr5-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DBolen%26aufirst%3DM.%2520G.%26aulast%3DMcWey%26aufirst%3DL.%2520M.%26aulast%3DSchlee%26aufirst%3DB.%2520M.%26date%3D2008%26atitle%3DAre%2520at-risk%2520parents%2520getting%2520what%2520they%2520need%253F%2520Perspectives%2520of%2520parents%2520involved%2520with%2520child%2520protective%2520services%26title%3DClinical%2520Social%2520Work%2520Journal%26volume%3D36%26spage%3D341\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr6-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Casper, E. S. , Oursler, J. , Schmidt, L. T. , &amp; Gill, K. J. (<span class=\"NLM_year\">2002</span>). <span class=\"NLM_article-title\">Measuring practitioners' beliefs, goals, and practices in psychiatric rehabilitation</span>. Psychiatric Rehabilitation Journal, 23(3), <span class=\"NLM_fpage\">223</span>–<span class=\"NLM_lpage\">234</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2002&pages=223-234&issue=3&author=E.+S.+Casperauthor=J.+Ourslerauthor=L.+T.+Schmidtauthor=K.+J.+Gill&title=Measuring+practitioners%27+beliefs%2C+goals%2C+and+practices+in+psychiatric+rehabilitation\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr6-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DCasper%26aufirst%3DE.%2520S.%26aulast%3DOursler%26aufirst%3DJ.%26aulast%3DSchmidt%26aufirst%3DL.%2520T.%26aulast%3DGill%26aufirst%3DK.%2520J.%26date%3D2002%26atitle%3DMeasuring%2520practitioners%2527%2520beliefs%252C%2520goals%252C%2520and%2520practices%2520in%2520psychiatric%2520rehabilitation%26title%3DPsychiatric%2520Rehabilitation%2520Journal%26volume%3D23%26issue%3D3%26spage%3D223\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr7-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Children's Bureau . (<span class=\"NLM_year\">2005</span>, January). Child and Family Services Review Technical Bulletin #1. Retrieved October 25, 2015, from <a class=\"ext-link\" href=\"http://www.acf.hhs.gov/sites/default/files/cb/cfsr_technical_bulletin_1.pdf\" target=\"_blank\">http://www.acf.hhs.gov/sites/default/files/cb/cfsr_technical_bulletin_1.pdf</a> <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=Children%27s+Bureau.+%282005%2C+January%29.+Child+and+Family+Services+Review+Technical+Bulletin+%231.+Retrieved+October+25%2C+2015%2C+from+http%3A%2F%2Fwww.acf.hhs.gov%2Fsites%2Fdefault%2Ffiles%2Fcb%2Fcfsr_technical_bulletin_1.pdf\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr7-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3D%26date%3D2005http%3A%2F%2Fwww.acf.hhs.gov%2Fsites%2Fdefault%2Ffiles%2Fcb%2Fcfsr_technical_bulletin_1.pdf\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr8-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Christensen, D. N. , Todahl, J. , &amp; Barrett, W. G. (<span class=\"NLM_year\">1999</span>). Solution-based casework: An introduction to clinical and case management skills in case work practice. <span class=\"NLM_publisher-loc\">New York, NY</span>: <span class=\"NLM_publisher-name\">Aldine DeGruyter.</span> <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=1999&author=D.+N.+Christensenauthor=J.+Todahlauthor=W.+G.+Barrett&title=Solution-based+casework%3A+An+introduction+to+clinical+and+case+management+skills+in+case+work+practice.\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr8-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DChristensen%26aufirst%3DD.%2520N.%26aulast%3DTodahl%26aufirst%3DJ.%26aulast%3DBarrett%26aufirst%3DW.%2520G.%26date%3D1999%26title%3DSolution-based%2520casework%253A%2520An%2520introduction%2520to%2520clinical%2520and%2520case%2520management%2520skills%2520in%2520case%2520work%2520practice.\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr9-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Dawson, K. , &amp; Berry, M. (<span class=\"NLM_year\">2002</span>). <span class=\"NLM_article-title\">Engaging families in child welfare services: An evidence-based approach to best practices</span>. Child Welfare, 81, <span class=\"NLM_fpage\">293</span>–<span class=\"NLM_lpage\">317</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2002&pages=293-317&author=K.+Dawsonauthor=M.+Berry&title=Engaging+families+in+child+welfare+services%3A+An+evidence-based+approach+to+best+practices\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr9-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DDawson%26aufirst%3DK.%26aulast%3DBerry%26aufirst%3DM.%26date%3D2002%26atitle%3DEngaging%2520families%2520in%2520child%2520welfare%2520services%253A%2520An%2520evidence-based%2520approach%2520to%2520best%2520practices%26title%3DChild%2520Welfare%26volume%3D81%26spage%3D293\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr10-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> DeBacker, T. K. , Crowson, H. M. , Beesley, A. D. , Thoma, S. J. , &amp; Hestevold, N. L. (<span class=\"NLM_year\">2008</span>). <span class=\"NLM_article-title\">The challenge of measuring epistemic beliefs: An analysis of three self-report instruments</span>. The Journal of Experimental Education, 76(3), <span class=\"NLM_fpage\">281</span>–<span class=\"NLM_lpage\">312</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2008&pages=281-312&issue=3&author=T.+K.+DeBackerauthor=H.+M.+Crowsonauthor=A.+D.+Beesleyauthor=S.+J.+Thomaauthor=N.+L.+Hestevold&title=The+challenge+of+measuring+epistemic+beliefs%3A+An+analysis+of+three+self-report+instruments\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr10-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DDeBacker%26aufirst%3DT.%2520K.%26aulast%3DCrowson%26aufirst%3DH.%2520M.%26aulast%3DBeesley%26aufirst%3DA.%2520D.%26aulast%3DThoma%26aufirst%3DS.%2520J.%26aulast%3DHestevold%26aufirst%3DN.%2520L.%26date%3D2008%26atitle%3DThe%2520challenge%2520of%2520measuring%2520epistemic%2520beliefs%253A%2520An%2520analysis%2520of%2520three%2520self-report%2520instruments%26title%3DThe%2520Journal%2520of%2520Experimental%2520Education%26volume%3D76%26issue%3D3%26spage%3D281\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr11-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> DeVellis, R. F. (<span class=\"NLM_year\">2003</span>). Scale development theory and application (2nd ed.). <span class=\"NLM_publisher-loc\">Thousand Oaks, CA</span>: <span class=\"NLM_publisher-name\">SAGE</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2003&author=R.+F.+DeVellis&title=Scale+development+theory+and+application\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr11-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DDeVellis%26aufirst%3DR.%2520F.%26date%3D2003%26title%3DScale%2520development%2520theory%2520and%2520application\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr12-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Dillman, D. A. (<span class=\"NLM_year\">2000</span>). Mail and internet surveys: The tailored design method (2nd ed.). <span class=\"NLM_publisher-loc\">New York, NY</span>: <span class=\"NLM_publisher-name\">Wiley</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2000&author=D.+A.+Dillman&title=Mail+and+internet+surveys%3A+The+tailored+design+method\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr12-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DDillman%26aufirst%3DD.%2520A.%26date%3D2000%26title%3DMail%2520and%2520internet%2520surveys%253A%2520The%2520tailored%2520design%2520method\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr13-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Dumbrill, G. C. (<span class=\"NLM_year\">2006</span>). <span class=\"NLM_article-title\">Parental experience of child protection intervention: A qualitative study</span>. Child Abuse and Neglect, 30, <span class=\"NLM_fpage\">27</span>–<span class=\"NLM_lpage\">37</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2006&pages=27-37&author=G.+C.+Dumbrill&title=Parental+experience+of+child+protection+intervention%3A+A+qualitative+study\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr13-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DDumbrill%26aufirst%3DG.%2520C.%26date%3D2006%26atitle%3DParental%2520experience%2520of%2520child%2520protection%2520intervention%253A%2520A%2520qualitative%2520study%26title%3DChild%2520Abuse%2520and%2520Neglect%26volume%3D30%26spage%3D27\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr14-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Ellett, A. , &amp; Steib, S. (<span class=\"NLM_year\">2005</span>). <span class=\"NLM_article-title\">Child welfare and the courts: A statewide study with implications for professional development</span>. Research on Social Work Practice, 15, <span class=\"NLM_fpage\">339</span>–<span class=\"NLM_lpage\">352</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2005&pages=339-352&author=A.+Ellettauthor=S.+Steib&title=Child+welfare+and+the+courts%3A+A+statewide+study+with+implications+for+professional+development\">Google Scholar</a></span><span class=\"ref-xLink\"> | <a href=\"https://journals.sagepub.com/doi/10.1177/1049731505276680\" onclick=\"newWindow(this.href);return false\">SAGE Journals</a></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr14-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DEllett%26aufirst%3DA.%26aulast%3DSteib%26aufirst%3DS.%26date%3D2005%26atitle%3DChild%2520welfare%2520and%2520the%2520courts%253A%2520A%2520statewide%2520study%2520with%2520implications%2520for%2520professional%2520development%26title%3DResearch%2520on%2520Social%2520Work%2520Practice%26volume%3D15%26spage%3D339\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr15-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Estefan, L. F. , Coulter, M. L. , VandeWeerd, C. L. , Armstrong, M. , &amp; Gorski, P. (<span class=\"NLM_year\">2012</span>). <span class=\"NLM_article-title\">Receiving mandated therapeutic services: Experiences of parents involved in the child welfare system</span>. Children and Youth Services Review, 34, <span class=\"NLM_fpage\">2353</span>–<span class=\"NLM_lpage\">2360</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2012&pages=2353-2360&author=L.+F.+Estefanauthor=M.+L.+Coulterauthor=C.+L.+VandeWeerdauthor=M.+Armstrongauthor=P.+Gorski&title=Receiving+mandated+therapeutic+services%3A+Experiences+of+parents+involved+in+the+child+welfare+system\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr15-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DEstefan%26aufirst%3DL.%2520F.%26aulast%3DCoulter%26aufirst%3DM.%2520L.%26aulast%3DVandeWeerd%26aufirst%3DC.%2520L.%26aulast%3DArmstrong%26aufirst%3DM.%26aulast%3DGorski%26aufirst%3DP.%26date%3D2012%26atitle%3DReceiving%2520mandated%2520therapeutic%2520services%253A%2520Experiences%2520of%2520parents%2520involved%2520in%2520the%2520child%2520welfare%2520system%26title%3DChildren%2520and%2520Youth%2520Services%2520Review%26volume%3D34%26spage%3D2353\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr16-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Fox, A. , Berrrick, J. D. , &amp; Frasch, K. (<span class=\"NLM_year\">2008</span>). <span class=\"NLM_article-title\">Safety, family, permanency, and child well-being: What we can learn from children</span>. Child Welfare, 87(1), <span class=\"NLM_fpage\">63</span>–<span class=\"NLM_lpage\">90</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2008&pages=63-90&issue=1&author=A.+Foxauthor=J.+D.+Berrrickauthor=K.+Frasch&title=Safety%2C+family%2C+permanency%2C+and+child+well-being%3A+What+we+can+learn+from+children\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr16-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DFox%26aufirst%3DA.%26aulast%3DBerrrick%26aufirst%3DJ.%2520D.%26aulast%3DFrasch%26aufirst%3DK.%26date%3D2008%26atitle%3DSafety%252C%2520family%252C%2520permanency%252C%2520and%2520child%2520well-being%253A%2520What%2520we%2520can%2520learn%2520from%2520children%26title%3DChild%2520Welfare%26volume%3D87%26issue%3D1%26spage%3D63\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr17-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Gill, M. J. , &amp; Andreychik, M. R. (<span class=\"NLM_year\">2014</span>). <span class=\"NLM_article-title\">The social explanatory styles questionnaire: Assessing moderator of basic social-cognitive phenomena including spontaneous trait inference, the fundamental attribution error, and moral blame</span>. PLoS ONE, 9(7), <span class=\"NLM_fpage\">1</span>–<span class=\"NLM_lpage\">14</span>. doi:10.1371/journal.pone.0100886 <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2014&pages=1-14&issue=7&author=M.+J.+Gillauthor=M.+R.+Andreychik&title=The+social+explanatory+styles+questionnaire%3A+Assessing+moderator+of+basic+social-cognitive+phenomena+including+spontaneous+trait+inference%2C+the+fundamental+attribution+error%2C+and+moral+blame\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr17-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DGill%26aufirst%3DM.%2520J.%26aulast%3DAndreychik%26aufirst%3DM.%2520R.%26date%3D2014%26atitle%3DThe%2520social%2520explanatory%2520styles%2520questionnaire%253A%2520Assessing%2520moderator%2520of%2520basic%2520social-cognitive%2520phenomena%2520including%2520spontaneous%2520trait%2520inference%252C%2520the%2520fundamental%2520attribution%2520error%252C%2520and%2520moral%2520blame%26title%3DPLoS%2520ONE%26volume%3D9%26issue%3D7%26spage%3D1\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr18-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Gladstone, J. , Dumbrill, G. , Leslie, B. , Koster, A. , Young, M. , &amp; Ismaila, A. (<span class=\"NLM_year\">2012</span>). <span class=\"NLM_article-title\">Looking at engagement and outcome from the perspectives of child protection workers and parents</span>. Children and Youth Services Review, 34, <span class=\"NLM_fpage\">112</span>–<span class=\"NLM_lpage\">118</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2012&pages=112-118&author=J.+Gladstoneauthor=G.+Dumbrillauthor=B.+Leslieauthor=A.+Kosterauthor=M.+Youngauthor=A.+Ismaila&title=Looking+at+engagement+and+outcome+from+the+perspectives+of+child+protection+workers+and+parents\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr18-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DGladstone%26aufirst%3DJ.%26aulast%3DDumbrill%26aufirst%3DG.%26aulast%3DLeslie%26aufirst%3DB.%26aulast%3DKoster%26aufirst%3DA.%26aulast%3DYoung%26aufirst%3DM.%26aulast%3DIsmaila%26aufirst%3DA.%26date%3D2012%26atitle%3DLooking%2520at%2520engagement%2520and%2520outcome%2520from%2520the%2520perspectives%2520of%2520child%2520protection%2520workers%2520and%2520parents%26title%3DChildren%2520and%2520Youth%2520Services%2520Review%26volume%3D34%26spage%3D112\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr19-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Glisson, C. , Green, P. , &amp; Williams, N. (<span class=\"NLM_year\">2012</span>). <span class=\"NLM_article-title\">Assessing the organizational social context (OSC) of child welfare systems: Implications for research and practice</span>. Child Abuse and Neglect, 36(9), <span class=\"NLM_fpage\">621</span>–<span class=\"NLM_lpage\">632</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2012&pages=621-632&issue=9&author=C.+Glissonauthor=P.+Greenauthor=N.+Williams&title=Assessing+the+organizational+social+context+%28OSC%29+of+child+welfare+systems%3A+Implications+for+research+and+practice\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr19-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DGlisson%26aufirst%3DC.%26aulast%3DGreen%26aufirst%3DP.%26aulast%3DWilliams%26aufirst%3DN.%26date%3D2012%26atitle%3DAssessing%2520the%2520organizational%2520social%2520context%2520%2528OSC%2529%2520of%2520child%2520welfare%2520systems%253A%2520Implications%2520for%2520research%2520and%2520practice%26title%3DChild%2520Abuse%2520and%2520Neglect%26volume%3D36%26issue%3D9%26spage%3D621\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr20-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Grief, G. L. (<span class=\"NLM_year\">2014</span>). <span class=\"NLM_article-title\">The voices of fathers in prison: Implications for family practice</span>. Journal of Family Social Work, 17, <span class=\"NLM_fpage\">68</span>–<span class=\"NLM_lpage\">80</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2014&pages=68-80&author=G.+L.+Grief&title=The+voices+of+fathers+in+prison%3A+Implications+for+family+practice\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr20-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DGrief%26aufirst%3DG.%2520L.%26date%3D2014%26atitle%3DThe%2520voices%2520of%2520fathers%2520in%2520prison%253A%2520Implications%2520for%2520family%2520practice%26title%3DJournal%2520of%2520Family%2520Social%2520Work%26volume%3D17%26spage%3D68\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr21-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Haight, W. L. , Shim, W. S. , Linn, L. M. , &amp; Swinford, L. (<span class=\"NLM_year\">2007</span>). <span class=\"NLM_article-title\">Mothers' strategies for protecting children from batters: The perspectives of battered women involved in child protective services</span>. Child Welfare, 86, <span class=\"NLM_fpage\">41</span>–<span class=\"NLM_lpage\">62</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2007&pages=41-62&author=W.+L.+Haightauthor=W.+S.+Shimauthor=L.+M.+Linnauthor=L.+Swinford&title=Mothers%27+strategies+for+protecting+children+from+batters%3A+The+perspectives+of+battered+women+involved+in+child+protective+services\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr21-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DHaight%26aufirst%3DW.%2520L.%26aulast%3DShim%26aufirst%3DW.%2520S.%26aulast%3DLinn%26aufirst%3DL.%2520M.%26aulast%3DSwinford%26aufirst%3DL.%26date%3D2007%26atitle%3DMothers%2527%2520strategies%2520for%2520protecting%2520children%2520from%2520batters%253A%2520The%2520perspectives%2520of%2520battered%2520women%2520involved%2520in%2520child%2520protective%2520services%26title%3DChild%2520Welfare%26volume%3D86%26spage%3D41\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr22-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Huebner, R. A. , Durbin, L. , &amp; Webb, T. (<span class=\"NLM_year\">2010</span>). Engagement: Attitudes and Beliefs Scale. Unpublished measure. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2010&author=R.+A.+Huebnerauthor=L.+Durbinauthor=T.+Webb\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr22-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DHuebner%26aufirst%3DR.%2520A.%26aulast%3DDurbin%26aufirst%3DL.%26aulast%3DWebb%26aufirst%3DT.%26date%3D2010%26title%3DEngagement%253A%2520Attitudes%2520and%2520Beliefs%2520Scale.\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr23-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Huebner, R. A. , Jones, B. L. , Miller, V. P. , Custer, M. , &amp; Critchfield, B. (<span class=\"NLM_year\">2006</span>). <span class=\"NLM_article-title\">Comprehensive family services and customer satisfaction outcomes</span>. Child Welfare, 85(4), <span class=\"NLM_fpage\">691</span>–<span class=\"NLM_lpage\">714</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2006&pages=691-714&issue=4&author=R.+A.+Huebnerauthor=B.+L.+Jonesauthor=V.+P.+Millerauthor=M.+Custerauthor=B.+Critchfield&title=Comprehensive+family+services+and+customer+satisfaction+outcomes\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr23-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DHuebner%26aufirst%3DR.%2520A.%26aulast%3DJones%26aufirst%3DB.%2520L.%26aulast%3DMiller%26aufirst%3DV.%2520P.%26aulast%3DCuster%26aufirst%3DM.%26aulast%3DCritchfield%26aufirst%3DB.%26date%3D2006%26atitle%3DComprehensive%2520family%2520services%2520and%2520customer%2520satisfaction%2520outcomes%26title%3DChild%2520Welfare%26volume%3D85%26issue%3D4%26spage%3D691\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr24-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Kemp, S. , Marcenko, M. , Hoagwood, K. , &amp; Vesneski, W. (<span class=\"NLM_year\">2009</span>). <span class=\"NLM_article-title\">Engaging parents in child welfare services: Bridging family needs and child welfare mandates</span>. Child Welfare, 88(1), <span class=\"NLM_fpage\">101</span>–<span class=\"NLM_lpage\">126</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2009&pages=101-126&issue=1&author=S.+Kempauthor=M.+Marcenkoauthor=K.+Hoagwoodauthor=W.+Vesneski&title=Engaging+parents+in+child+welfare+services%3A+Bridging+family+needs+and+child+welfare+mandates\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr24-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DKemp%26aufirst%3DS.%26aulast%3DMarcenko%26aufirst%3DM.%26aulast%3DHoagwood%26aufirst%3DK.%26aulast%3DVesneski%26aufirst%3DW.%26date%3D2009%26atitle%3DEngaging%2520parents%2520in%2520child%2520welfare%2520services%253A%2520Bridging%2520family%2520needs%2520and%2520child%2520welfare%2520mandates%26title%3DChild%2520Welfare%26volume%3D88%26issue%3D1%26spage%3D101\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr25-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Longhofer, J. , &amp; Floersch, J. (<span class=\"NLM_year\">2014</span>). <span class=\"NLM_article-title\">Values in a science of social work: Values-informed research and research-informed values</span>. Research on Social Work Practice, 24(5), <span class=\"NLM_fpage\">527</span>–<span class=\"NLM_lpage\">534</span>. Merriam-Webster Online. (n.d.). Belief. Retrieved July 24, 2014, from <a class=\"ext-link\" href=\"http://www.merriam-webster.com/dictionary/belief\" target=\"_blank\">http://www.merriam-webster.com/dictionary/belief</a> <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar?hl=en&q=LonghoferJ.%2C+%26+FloerschJ.+%282014%29.++Values+in+a+science+of+social+work%3A+Values-informed+research+and+research-informed+values.+Research+on+Social+Work+Practice%2C+24%285%29%2C+527%E2%80%93534.+Merriam-Webster+Online.+%28n.d.%29.+Belief.+Retrieved+July+24%2C+2014%2C+from+http%3A%2F%2Fwww.merriam-webster.com%2Fdictionary%2Fbelief\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr25-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DLonghofer%26aufirst%3DJ.%26aulast%3DFloersch%26aufirst%3DJ.%26date%3D2014%26atitle%3DValues%2520in%2520a%2520science%2520of%2520social%2520work%253A%2520Values-informed%2520research%2520and%2520research-informed%2520values%26volume%3D24%26issue%3D5%26spage%3D527http%3A%2F%2Fwww.merriam-webster.com%2Fdictionary%2Fbelief\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr26-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Palmer, S , Maiter, S. , &amp; Manji, S. (<span class=\"NLM_year\">2006</span>). <span class=\"NLM_article-title\">Effective intervention in child protective services: Learning from parents</span>. Children and Youth Services Review, 28, <span class=\"NLM_fpage\">812</span>–<span class=\"NLM_lpage\">824</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2006&pages=812-824&author=S+Palmerauthor=S.+Maiterauthor=S.+Manji&title=Effective+intervention+in+child+protective+services%3A+Learning+from+parents\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr26-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DPalmer%26aufirst%3DS%26aulast%3DMaiter%26aufirst%3DS.%26aulast%3DManji%26aufirst%3DS.%26date%3D2006%26atitle%3DEffective%2520intervention%2520in%2520child%2520protective%2520services%253A%2520Learning%2520from%2520parents%26title%3DChildren%2520and%2520Youth%2520Services%2520Review%26volume%3D28%26spage%3D812\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr27-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Park, C. L. (<span class=\"NLM_year\">2010</span>). <span class=\"NLM_article-title\">Making sense of the meaning literature: An integrative review of meaning making and its effects on adjustment to stressful life events</span>. Psychological Bulletin, 136(2), <span class=\"NLM_fpage\">257</span>–<span class=\"NLM_lpage\">301</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2010&pages=257-301&issue=2&author=C.+L.+Park&title=Making+sense+of+the+meaning+literature%3A+An+integrative+review+of+meaning+making+and+its+effects+on+adjustment+to+stressful+life+events\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr27-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DPark%26aufirst%3DC.%2520L.%26date%3D2010%26atitle%3DMaking%2520sense%2520of%2520the%2520meaning%2520literature%253A%2520An%2520integrative%2520review%2520of%2520meaning%2520making%2520and%2520its%2520effects%2520on%2520adjustment%2520to%2520stressful%2520life%2520events%26title%3DPsychological%2520Bulletin%26volume%3D136%26issue%3D2%26spage%3D257\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr28-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Peters, J. (<span class=\"NLM_year\">2005</span>). <span class=\"NLM_article-title\">True ambivalence: Child welfare workers' thoughts, feelings, and beliefs about kinship foster care</span>. Children and Youth Services Review, 27, <span class=\"NLM_fpage\">595</span>–<span class=\"NLM_lpage\">614</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2005&pages=595-614&author=J.+Peters&title=True+ambivalence%3A+Child+welfare+workers%27+thoughts%2C+feelings%2C+and+beliefs+about+kinship+foster+care\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr28-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DPeters%26aufirst%3DJ.%26date%3D2005%26atitle%3DTrue%2520ambivalence%253A%2520Child%2520welfare%2520workers%2527%2520thoughts%252C%2520feelings%252C%2520and%2520beliefs%2520about%2520kinship%2520foster%2520care%26title%3DChildren%2520and%2520Youth%2520Services%2520Review%26volume%3D27%26spage%3D595\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr29-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Scott, D. L. , Lee, C. B. , Harrell, S. W. , &amp; Smith-West, M. B. (<span class=\"NLM_year\">2013</span>). <span class=\"NLM_article-title\">Permanency for children in foster care: Issues and barriers for adoption</span>. Child and Youth Services, 34, <span class=\"NLM_fpage\">290</span>–<span class=\"NLM_lpage\">307</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2013&pages=290-307&author=D.+L.+Scottauthor=C.+B.+Leeauthor=S.+W.+Harrellauthor=M.+B.+Smith-West&title=Permanency+for+children+in+foster+care%3A+Issues+and+barriers+for+adoption\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr29-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DScott%26aufirst%3DD.%2520L.%26aulast%3DLee%26aufirst%3DC.%2520B.%26aulast%3DHarrell%26aufirst%3DS.%2520W.%26aulast%3DSmith-West%26aufirst%3DM.%2520B.%26date%3D2013%26atitle%3DPermanency%2520for%2520children%2520in%2520foster%2520care%253A%2520Issues%2520and%2520barriers%2520for%2520adoption%26title%3DChild%2520and%2520Youth%2520Services%26volume%3D34%26spage%3D290\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr30-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> van Zyl, M. A. , Barbee, A. P. , Cunningham, M. R. , Antle, B. F. , Christensen, D. N. , &amp; Boamah, D. (<span class=\"NLM_year\">2014</span>). <span class=\"NLM_article-title\">Components of the solution-based casework child welfare practice model that predict positive child outcomes</span>. Journal of Public Child Welfare, 8(4), <span class=\"NLM_fpage\">433</span>–<span class=\"NLM_lpage\">365</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2014&pages=433-365&issue=4&author=M.+A.+van+Zylauthor=A.+P.+Barbeeauthor=M.+R.+Cunninghamauthor=B.+F.+Antleauthor=D.+N.+Christensenauthor=D.+Boamah&title=Components+of+the+solution-based+casework+child+welfare+practice+model+that+predict+positive+child+outcomes\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr30-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3Dvan%2520Zyl%26aufirst%3DM.%2520A.%26aulast%3DBarbee%26aufirst%3DA.%2520P.%26aulast%3DCunningham%26aufirst%3DM.%2520R.%26aulast%3DAntle%26aufirst%3DB.%2520F.%26aulast%3DChristensen%26aufirst%3DD.%2520N.%26aulast%3DBoamah%26aufirst%3DD.%26date%3D2014%26atitle%3DComponents%2520of%2520the%2520solution-based%2520casework%2520child%2520welfare%2520practice%2520model%2520that%2520predict%2520positive%2520child%2520outcomes%26title%3DJournal%2520of%2520Public%2520Child%2520Welfare%26volume%3D8%26issue%3D4%26spage%3D433\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr31-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Williams, S. E. , Nichols, Q. L. , &amp; Williams, N. L. (<span class=\"NLM_year\">2013</span>). <span class=\"NLM_article-title\">Public child welfare workers' perception of efficacy relative to multicultural awareness, knowledge and skills</span>. Children and Youth Services Review, 35, <span class=\"NLM_fpage\">1789</span>–<span class=\"NLM_lpage\">1793</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2013&pages=1789-1793&author=S.+E.+Williamsauthor=Q.+L.+Nicholsauthor=N.+L.+Williams&title=Public+child+welfare+workers%27+perception+of+efficacy+relative+to+multicultural+awareness%2C+knowledge+and+skills\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr31-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DWilliams%26aufirst%3DS.%2520E.%26aulast%3DNichols%26aufirst%3DQ.%2520L.%26aulast%3DWilliams%26aufirst%3DN.%2520L.%26date%3D2013%26atitle%3DPublic%2520child%2520welfare%2520workers%2527%2520perception%2520of%2520efficacy%2520relative%2520to%2520multicultural%2520awareness%252C%2520knowledge%2520and%2520skills%26title%3DChildren%2520and%2520Youth%2520Services%2520Review%26volume%3D35%26spage%3D1789\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr32-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Williamson, E. , &amp; Gray, A. (<span class=\"NLM_year\">2011</span>). <span class=\"NLM_article-title\">New roles for families in child welfare: Strategies for expanding family involvement beyond the case level</span>. Children and Youth Services Review, 33, <span class=\"NLM_fpage\">1212</span>–<span class=\"NLM_lpage\">1216</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2011&pages=1212-1216&author=E.+Williamsonauthor=A.+Gray&title=New+roles+for+families+in+child+welfare%3A+Strategies+for+expanding+family+involvement+beyond+the+case+level\">Google Scholar</a></span><span class=\"ref-xLink\"></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr32-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DWilliamson%26aufirst%3DE.%26aulast%3DGray%26aufirst%3DA.%26date%3D2011%26atitle%3DNew%2520roles%2520for%2520families%2520in%2520child%2520welfare%253A%2520Strategies%2520for%2520expanding%2520family%2520involvement%2520beyond%2520the%2520case%2520level%26title%3DChildren%2520and%2520Youth%2520Services%2520Review%26volume%3D33%26spage%3D1212\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span><hr /></td></tr><tr id=\"bibr33-1044-3894.2016.97.18\"><td class=\"refnumber\"> </td><td valign=\"top\"> Yatchmenoff, D. K. (<span class=\"NLM_year\">2005</span>). <span class=\"NLM_article-title\">Measuring client engagement from the client's perspective in nonvoluntary child protection services</span>. Research in Social Work Practice, 15, <span class=\"NLM_fpage\">84</span>–<span class=\"NLM_lpage\">96</span>. <br /><span class=\"ref-google\"><a class=\"google-scholar\" href=\"http://scholar.google.com/scholar_lookup?hl=en&publication_year=2005&pages=84-96&author=D.+K.+Yatchmenoff&title=Measuring+client+engagement+from+the+client%27s+perspective+in+nonvoluntary+child+protection+services\">Google Scholar</a></span><span class=\"ref-xLink\"> | <a href=\"https://journals.sagepub.com/doi/10.1177/1049731504271605\" onclick=\"newWindow(this.href);return false\">SAGE Journals</a></span><span class=\"ref-SFXLink\"><a href=\"/servlet/linkout?suffix=bibr33-1044-3894.2016.97.18&amp;dbid=16384&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dsage%26iuid%3D263325%26aulast%3DYatchmenoff%26aufirst%3DD.%2520K.%26date%3D2005%26atitle%3DMeasuring%2520client%2520engagement%2520from%2520the%2520client%2527s%2520perspective%2520in%2520nonvoluntary%2520child%2520protection%2520services%26title%3DResearch%2520in%2520Social%2520Work%2520Practice%26volume%3D15%26spage%3D84\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/263325/sfxbutton\" alt=\"OpenURL Stanford University\" /></a></span></td></tr></table>\n" +
          "                                                                                                                    </article>\n" +
          "                                                                                                                </div>\n" +
          "                                                                                                                <div class=\"tab tab-pane\" id=\"relatedContent\">\n" +
          "                                                                                                                </div>\n" +
          "                                                                                                            </div>\n" +
          "                                                                                                        </div>\n" +
          "                                                                                                    </article>\n" +
          "                                                                                                </div>";

    private static final String absContentFiltered = " Abstract Abstract filtered content detail ";
 
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
    String filterdStr = StringUtil.fromInputStream(actIn);

    log.info("filteredStr = " + filterdStr);

    log.info("expectedStr = " + expectedStr);

    assertTrue(filterdStr.equals(expectedStr));
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
  public static class TestCrawl extends TestSageHtmlFilterFactory {
    public void testFiltering() throws Exception {
      variantFact = new BaseAtyponHtmlCrawlFilterFactory();
      assertEquals(true,true);
   //TODO
    }
  }
  
  // Variant to test with Hash Filter
   public static class TestHash extends TestSageHtmlFilterFactory {
     public void testFiltering() throws Exception {
       variantFact = new SageAtyponHtmlHashFilterFactory();
       doFilterTest(mau, variantFact, manifestContent, manifestHashFiltered);
       doFilterTest(mau, variantFact, tocContent, tocContentFiltered);
       log.info("====test start====");
       doFilterTest(mau, variantFact, tocContent2, tocContent2Filtered);
       //doFilterTest(mau, variantFact, tocContent3, tocContent3Filtered); comment out this one, it is used to debug 0 hash, will trigger failed test
       log.info("====test end====");
       doFilterTest(mau, variantFact, articleAccessDenialContent, articleAccessDenialContentFiltered);
       doFilterTest(mau, variantFact, absContent, absContentFiltered);
     }
   }
   
  public static Test suite() {
    return variantSuites(new Class[] {
        TestCrawl.class,
        TestHash.class
      });
  }
  
}

