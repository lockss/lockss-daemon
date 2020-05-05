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

  // absContent updated 5/2020 because of skin change.
  private static final String absContent = "" +
          "<div class=\"hlFld-Title\">\n" +
          "<div class=\"publicationContentTitle\">\n" +
          "<h1>\n" +
          "Fertility and Family Planning among the Rural Women of Phaknung Village in Manipur, India.\n" +
          "</h1>\n" +
          "</div>\n" +
          "</div>" +
          "<span class=\"publicationContentEpubDate dates\">\n" +
          "<b>First Published </b> September 5, 2019\n" +
          "</span>" +
          "<div class=\"tab-content \">\n" +
          "<a id=\"top-content-scroll\" onfocus=\"hideAuthor();\"></a>\n" +
          "<div class=\"tab tab-pane active\">\n" +
          "<article class=\"article\">\n" +
          "<div class=\"hlFld-Abstract\"><div id=\"firstPage\" class=\"firstPage\"><img src=\"/na101/home/literatum/publisher/sage/journals/content/oana/2013/oana_13_1/097634302013010i/20190828/097634302013010i.fp.png_v03\" alt=\"Free first page\" class=\"firstPageImage\"></div></div>\n" +
          "</article>\n" +
          "</div>\n" +
          "<div class=\"tab tab-pane\" id=\"relatedContent\">\n" +
          "</div>\n" +
          "</div>";

    private static final String absContentFiltered = " Fertility and Family Planning among the Rural Women of Phaknung Village in Manipur, India. First Published September 5, 2019 ";
 
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

    //log.info("filteredStr = " + filterdStr);
    //log.info("expectedStr = " + expectedStr);

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
       doFilterTest(mau, variantFact, tocContent2, tocContent2Filtered);
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

