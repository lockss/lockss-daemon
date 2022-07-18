/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.highwire.bmj;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestBMJJCoreHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private BMJJCoreHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new BMJJCoreHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
  private static final String withHead = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n" + 
      "<html lang=\"en\" xml:lang=\"en\" style=\"\">" +
      "<head profile=\"http://www.w3.org/1999/xhtml/vocab\">\n" + 
      "  <meta charset=\"utf-8\">\n" + 
      "<link type=\"image/vnd.microsoft.icon\" href=\"http://static.www.bmj.com/sites/default/themes/bmj/the_bmj/favicon.ico\" rel=\"shortcut icon\">\n" + 
      "<!--[if (gte IE 6)&(lte IE 9)]><script src=\"/sites/default/themes/bmj/the_bmj/js/respond.min.js\" />\n" + 
      "</script><![endif]--><link type=\"text/css\" media=\"all\" rel=\"stylesheet\" href=\"//netdna.bootstrapcdn.com/font-awesome/4.0.3/css/font-awesome.css\">\n" + 
      "<meta content=\"/bmj/347/7915.atom\" name=\"HW.identifier\">\n" + 
      "<meta content=\"bmj;347/7915\" name=\"HW.pisa\">\n" + 
      "<meta content=\"BMJ\" name=\"citation_journal_title\">\n" + 
      "<meta content=\"BMJ Publishing Group Ltd\" name=\"citation_publisher\">\n" + 
      "<meta content=\"BMJ : British Medical Journal:\" name=\"citation_title\">\n" + 
      "<meta content=\"2013/07/06\" name=\"citation_publication_date\">\n" + 
      "<meta content=\"bmj;347/7915\" name=\"citation_mjid\">\n" + 
      "<meta content=\"347/7915\" name=\"citation_id\">\n" + 
      "<meta content=\"1756-1833\" name=\"citation_issn\">\n" + 
      "<meta content=\"347\" name=\"citation_volume\">\n" + 
      "<meta content=\"7915\" name=\"citation_issue\">\n" + 
      "<meta content=\"http://static.www.bmj.com/sites/default/files/highwire/bmj/347/7915.cover-source.jpg\" name=\"citation_image\">\n" + 
      "<meta content=\"Drupal 7 (http://drupal.org)\" name=\"generator\">\n" + 
      "<link href=\"http://www.bmj.com/content/347/7915\" rel=\"canonical\">\n" + 
      "<link href=\"http://www.bmj.com/node/724572\" rel=\"shortlink\">\n" + 
      "  <title>BMJ : British Medical Journal: | The BMJ</title>\n" + 
      "  <link media=\"all\" href=\"http://www.bmj.com/sites/default/files/cdn/css/http/css_nZpqy9LysFSwGKJ7v4z11U9YB9kpVSWL_JhlIW3O5FI.css\" rel=\"stylesheet\" type=\"text/css\">\n" + 
      "<link media=\"all\" href=\"http://www.bmj.com/sites/default/files/cdn/css/http/css_LCvCQQenOZftP9DGmJQ1AC-c57eFSh7UXDLv8OVdeZg.css\" rel=\"stylesheet\" type=\"text/css\">\n" + 
      "<link media=\"print\" href=\"http://www.bmj.com/sites/default/files/cdn/css/http/css_VXn5JR586SULWVDL67jN01tt5YpX724gC2Q9rYfH9wg.css\" rel=\"stylesheet\" type=\"text/css\">\n" + 
      "<link media=\"screen\" href=\"http://www.bmj.com/sites/default/files/cdn/css/http/css_GmB3cCWgpK2q5ihUSSCapsAyFPHt-j4AyUGHyqXhLe0.css\" rel=\"stylesheet\" type=\"text/css\">\n" + 
      "  <link type=\"text/css\" media=\"all\" rel=\"stylesheet\" href=\"/sites/default/themes/bmj/the_bmj/css/fonts.css\">\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_-z-2lAhufzBeVjYseT6cTzSICUy9vnoLBpu1sF_zZrs.js\"></script>\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_8cesDs51kIWO42f3ZOLrsAfi9EEUt1q0P0uHVRja86Y.js\"></script>\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_Sh2UgvOvuSU9-K8qwk3-2k4jaFfLVCvAG5OTqQOumXM.js\"></script>\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_kTXHXdUjbRT6JXrj3C5k6w5lLUdroufD6DGU1pLvKl4.js\"></script>\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_d8nvZsNcH9M1AdbeKkecL0ZPaRgMfWLhCmktg3wHDu4.js\"></script>" +
      "<script src=\"http://oas.services.bmj.com/RealMedia/ads/adstream_mjx.ads/www.bmj.com/content/347/7915.toc/1716469929@Middle1,Middle2,Top,Top2?\" language=\"JavaScript1.1\"></script>\n" + 
      "</head>" +
      "<body><header>stuff</header>text<footer>stuff</footer></body>" +
      "</html>";
  private static final String filteredHead = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n" + 
      "<html lang=\"en\" xml:lang=\"en\" style=\"\">" +
      "<head profile=\"http://www.w3.org/1999/xhtml/vocab\">\n" + 
      "  <meta charset=\"utf-8\">\n" + 
      "<link type=\"image/vnd.microsoft.icon\" href=\"http://static.www.bmj.com/sites/default/themes/bmj/the_bmj/favicon.ico\" rel=\"shortcut icon\">\n" + 
      "<!--[if (gte IE 6)&(lte IE 9)]><script src=\"/sites/default/themes/bmj/the_bmj/js/respond.min.js\" />\n" + 
      "</script><![endif]--><link type=\"text/css\" media=\"all\" rel=\"stylesheet\" href=\"//netdna.bootstrapcdn.com/font-awesome/4.0.3/css/font-awesome.css\">\n" + 
      "<meta content=\"/bmj/347/7915.atom\" name=\"HW.identifier\">\n" + 
      "<meta content=\"bmj;347/7915\" name=\"HW.pisa\">\n" + 
      "<meta content=\"BMJ\" name=\"citation_journal_title\">\n" + 
      "<meta content=\"BMJ Publishing Group Ltd\" name=\"citation_publisher\">\n" + 
      "<meta content=\"BMJ : British Medical Journal:\" name=\"citation_title\">\n" + 
      "<meta content=\"2013/07/06\" name=\"citation_publication_date\">\n" + 
      "<meta content=\"bmj;347/7915\" name=\"citation_mjid\">\n" + 
      "<meta content=\"347/7915\" name=\"citation_id\">\n" + 
      "<meta content=\"1756-1833\" name=\"citation_issn\">\n" + 
      "<meta content=\"347\" name=\"citation_volume\">\n" + 
      "<meta content=\"7915\" name=\"citation_issue\">\n" + 
      "<meta content=\"http://static.www.bmj.com/sites/default/files/highwire/bmj/347/7915.cover-source.jpg\" name=\"citation_image\">\n" + 
      "<meta content=\"Drupal 7 (http://drupal.org)\" name=\"generator\">\n" + 
      "<link href=\"http://www.bmj.com/content/347/7915\" rel=\"canonical\">\n" + 
      "<link href=\"http://www.bmj.com/node/724572\" rel=\"shortlink\">\n" + 
      "  <title>BMJ : British Medical Journal: | The BMJ</title>\n" + 
      "  <link media=\"all\" href=\"http://www.bmj.com/sites/default/files/cdn/css/http/css_nZpqy9LysFSwGKJ7v4z11U9YB9kpVSWL_JhlIW3O5FI.css\" rel=\"stylesheet\" type=\"text/css\">\n" + 
      "<link media=\"all\" href=\"http://www.bmj.com/sites/default/files/cdn/css/http/css_LCvCQQenOZftP9DGmJQ1AC-c57eFSh7UXDLv8OVdeZg.css\" rel=\"stylesheet\" type=\"text/css\">\n" + 
      "<link media=\"print\" href=\"http://www.bmj.com/sites/default/files/cdn/css/http/css_VXn5JR586SULWVDL67jN01tt5YpX724gC2Q9rYfH9wg.css\" rel=\"stylesheet\" type=\"text/css\">\n" + 
      "<link media=\"screen\" href=\"http://www.bmj.com/sites/default/files/cdn/css/http/css_GmB3cCWgpK2q5ihUSSCapsAyFPHt-j4AyUGHyqXhLe0.css\" rel=\"stylesheet\" type=\"text/css\">\n" + 
      "  <link type=\"text/css\" media=\"all\" rel=\"stylesheet\" href=\"/sites/default/themes/bmj/the_bmj/css/fonts.css\">\n\n\n\n\n\n" + 
      "</head>" +
      "<body>text</body>" +
      "</html>";
  
  private static final String withArticle = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n" + 
      "<html lang=\"en\" xml:lang=\"en\" style=\"\">" +
      "<head profile=\"http://www.w3.org/1999/xhtml/vocab\">\n" + 
      "  <meta charset=\"utf-8\">\n" + 
      "</head>\n" +
      "<body>\n" +
      "    <header> \n" + 
      "     </header>\n" + 
      "        <article>\n" + 
      "<div class=\"issue-toc\">\n" +
      "<a href=\"http://www.bmj.com/content/347/16.toc\">PDF</a>\n" +
      "<h2 id=\"editors-choice\" class=\"toc-heading\">Editor's Choice</h2>\n" +
      "<ul class=\"toc-section\">\n" +
      "<li class=\"toc-item first last odd\">\n" +
      " <div class=\"toc-citation\">\n" +
      " </div>\n" +
      "</li>\n" +
      "<li class=\"first\">\n" +
      "<a target=\"_blank\" href=\"http://www.bmj.com/content/347/bmj.f4314.full.pdf+html\">PDF</a>\n" +
      "</li>\n" + 
      "</ul>\n" + 
      "  </div>\n" + 
      "        </article>\n" +
      "<div class=\"panel-pane pane-bmj-issue-pager\">\n" + 
      "  \n" + 
      "      \n" + 
      "  \n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"pager highwire-pager pager-mini clearfix highwire-node-pager highwire-issue-pager\"><span class=\"pager-prev\">" +
      "<a class=\"btn\" rel=\"prev\" data-font-icon=\"fa fa-arrow-circle-left\" title=\"BMJ: \" href=\"/content/341/7786\">Previous</a></span></div>  </div>\n" + 
      "\n" + 
      "  \n" + 
      "  </div>\n" +
      "<footer>stuff</footer>\n" + 
      "</body>\n" +
      "</html>";
  private static final String filteredArticle = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n" + 
      "<html lang=\"en\" xml:lang=\"en\" style=\"\">" +
      "<head profile=\"http://www.w3.org/1999/xhtml/vocab\">\n" + 
      "  <meta charset=\"utf-8\">\n" + 
      "</head>\n" +
      "<body>\n" +
      "    \n" +
      "        <article>\n" + 
      "<div class=\"issue-toc\">\n" +
      "<a href=\"http://www.bmj.com/content/347/16.toc\">PDF</a>\n" +
      "<h2 id=\"editors-choice\" class=\"toc-heading\">Editor's Choice</h2>\n" +
      "<ul class=\"toc-section\">\n" +
      "<li class=\"toc-item first last odd\">\n" +
      " <div class=\"toc-citation\">\n" +
      " </div>\n" +
      "</li>\n" +
      "<li class=\"first\">\n" +
      "<a target=\"_blank\" href=\"http://www.bmj.com/content/347/bmj.f4314.full.pdf+html\">PDF</a>\n" +
      "</li>\n" + 
      "</ul>\n" + 
      "  </div>\n" + 
      "        </article>\n" +
      "\n" + 
      "\n" + 
      "</body>\n" +
      "</html>";
      
  private static final String withRelated = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n" + 
      "<html lang=\"en\" xml:lang=\"en\" style=\"\">\n" + 
      "    <div id=\"page\" class=\"main-container container\">\n" + 
      "      <div class=\"row\">\n" + 
      "        <section class=\"col-sm-12\">\n" + 
      "                    <ol class=\"breadcrumb\"><li class=\"first\"><a href=\"/us/research\">Research</a></li>\n" + 
      "<li>Explaining trends in...</li>\n" + 
      "<li class=\"active last\">Explaining</li>\n" + 
      "</ol>          <a id=\"main-content\"></a>\n" + 
      "<div class=\"region region-content\">\n" + 
      "    <section class=\"block block-system clearfix\" id=\"block-system-main\">\n" + 
      "  <div class=\"row\">\n" + 
      "        <article>\n" + 
      "          <div class=\"panel-pane pane-highwire-article-citation\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "<cite class=\"highwire-cite highwire-citation-bmj-article-top\">\n" + 
      "            <span class=\"highwire-cite-article-type\">Research</span>\n" + 
      "            <h1 id=\"page-title\" class=\"highwire-cite-title\">" +
      "<div xmlns=\"http://www.w3.org/1999/xhtml\">Explaining </div></h1>    \n" + 
      "    <span class=\"highwire-cite-journal\">BMJ</span>\n" + 
      "    <span class=\"highwire-cite-published-year\">2014</span>;\n" + 
      "    <span class=\"highwire-cite-volume-issue\">348</span>\n" + 
      "</cite>\n" + 
      "</div>  </div>\n" + 
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div>" +
      "<div class=\"panel-pane pane-highwire-panel-tabs pane-panels-ajax-tab-tabs\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <ul class=\"tabs inline panels-ajax-tab\">" +
      "<li class=\"first\"><span>Article</span></li>\n" + 
      "<li class=\"active\"><span>Related content</span></li>\n" + 
      "<li><span>Metrics</span></li>\n" + 
      "<li><span>Responses</span></li>\n" + 
      "<li><span>Peer review</span></li>\n" + 
      "</ul>  </div>\n" + 
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div>" +
      "<div class=\"panel-pane pane-highwire-panel-tabs-container\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "   <div class=\"panels-ajax-tab-container\" id=\"panels-ajax-tab-container-highwire_article_tabs\">\n" +
      "<div class=\"panels-ajax-tab-wrap-jnl_bmj_tab_related_art\">1" +
      "<div class=\"panel-display panel-1col clearfix\">\n" + 
      "  <div class=\"panel-panel panel-col\">\n" + 
      "    <div><div class=\"panel-pane pane-bmj-article-fig-data\">\n" + 
      "        <h2 class=\"pane-title\"><a name=\"datasupp\"></a>Data supplement</h2>\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"group frag-additional-files\" id=\"fig-data-additional-files\">\n" +
      "<ul><li class=\"first\"><div class=\"highwire-markup\">\n" +
      "<div>\n" +
      "<div><span class=\"highwire-journal-article-marker-start\"></span><div>\n" + 
      "            <h2>Web Extra</h2>\n" + 
      "            <p>Extra material supplied by the author</p>\n" + 
      "            <p><strong>Files in this Data Supplement:</strong></p>\n" + 
      "            <ul><li><a href=\"/highwire/filestream/756546/field_highwire_adjunct_files/0/hotj014952.ww1_default.pdf\">Data Supplement</a> -\n" + 
      "                                Summary appendix\n" + 
      "               </li><li><a href=\"/highwire/filestream/756546/field_highwire_adjunct_files/1/hotj014952.ww2_default.pdf\">Data Supplement</a> -\n" + 
      "                                Technical appendix\n" + 
      "               </li></ul>\n" + 
      "         </span>\n" + 
      "   </div><span class=\"highwire-journal-article-marker-end\"></span>\n" +
      "</div><span id=\"related-urls\"></span></div></div>\n" +
      "</li></ul></div>  </div>\n" + 
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div>" +
      "<div class=\"panel-pane pane-bmj-related-articles\">\n" + 
      "        <h2 class=\"pane-title\">Related articles</h2>\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"highwire-list bmj-related-articles highwire-article-citation-list\">" +
      "<ul class=\"bmj-related-articles-list\">" +
      "<li class=\"first odd\">" +
      "<cite class=\"highwire-cite highwire-citation-bmj-article-list\">\n" + 
      "      <span class=\"highwire-cite-article-type\">Research Article</span>\n" + 
      "    <div class=\"highwire-cite-title\">" +
      "<span class=\"highwire-cite-title\">\"Heartstart </span></div>\n" + 
      "  <span class=\"highwire-cite-year\">Published: 22 June 1991;</span>\n" + 
      "  <span class=\"highwire-cite-journal\">BMJ</span>\n" + 
      "  <span class=\"highwire-cite-volume-issue\">302</span>\n" + 
      "  <span class=\"highwire-cite-doi\"> doi:10.1136/bmj.302.6791.1517</span>\n" + 
      "  </cite>\n" + 
      "</li><li class=\"odd\">" +
      "<cite class=\"highwire-cite highwire-citation-bmj-article-list\">\n" + 
      "   <span class=\"highwire-cite-article-type\">Editorial</span>\n" + 
      "   <div class=\"highwire-cite-title\">United</div>\n" + 
      "  <span class=\"highwire-cite-year\">Published: 14 September 2011;</span>\n" + 
      "  <span class=\"highwire-cite-journal\">BMJ</span>\n" + 
      "  <span class=\"highwire-cite-volume-issue\">343</span>\n" + 
      "  <span class=\"highwire-cite-doi\"> doi:10.1136/bmj.d5747</span>\n" + 
      "  </cite>\n" + 
      "</div></li></ul></div>" +
      "  </div>\n" + 
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div>" +
      "<div class=\"panel-pane pane-views-panes pane-bmj-topic-related-articles-panel-pane-1\">\n" + 
      "        <h2 class=\"pane-title\">See more</h2>\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"view view-bmj-topic-related-articles\">\n" + 
      "      <div class=\"view-content\">\n" + 
      "      <div class=\"highwire-list-wrapper highwire-article-citation-list\"><div class=\"highwire-list\">" +
      "<ul><li class=\"first odd\">" +
      "<cite class=\"highwire-cite highwire-citation-bmj-article-list\">\n" + 
      "      <span class=\"highwire-cite-article-type\">Research News</span>\n" + 
      "  <span class=\"highwire-cite-year\">Published: 18 September 2015;</span>\n" + 
      "  <span class=\"highwire-cite-journal\"></span>\n" + 
      "  <span class=\"highwire-cite-volume-issue\">351</span>\n" + 
      "  <span class=\"highwire-cite-doi\"> doi:10.1136/bmj.h4977</span>\n" + 
      "  </cite>\n" + 
      "</li></ul>\n" +
      "</div></div>    </div>\n" + 
      "</div>  </div>" + 
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div>" +
      "<div class=\"panel-pane pane-bmj-cited-by\">\n" + 
      "        <h2 class=\"pane-title\">Cited by...</h2>\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"highwire-list-wrapper\">" +
      "<div class=\"highwire-list\">" +
      "<ul class=\"links list-inline\">" +
      "<li class=\"variant-abstract first\"><a href=\"http://eurheartj.oxfordjournals.org/cgi/content/abstract/36/32/2142\">Abstract</a></li>\n" + 
      "<li class=\"variant-fulltext\"><a href=\"http://www.jwatch.org/cgi/content/full/2014/mar07_7/NA33889\">Fulltext</a></li>\n" + 
      "<li class=\"variant-pdf last\"><a href=\"http://heart.bmjjournals.com/content/heartjnl/vol100/Suppl_2/pdf/ii1.pdf\">PDF</a></li>\n" + 
      "</ul>" +
      "</div>" +
      "</li>" +
      "</ul></div></div>" +
      "  </div>\n" + 
      "  </div>\n" + 
      "</div>\n" + 
      "  </div>\n" + 
      "</div>\n" + 
      "</div></div>  </div>\n" + 
      "  </div>\n" + 
      "        </article>\n" + 
      "  </div>\n" + 
      "</div>\n" + 
      "</section>  \n" + 
      "  </div>\n" + 
      "        </section>\n" + 
      "              </div> \n" + 
      "    </div> \n" + 
      "  </div>\n" + 
      "</div>\n" + 
      "</html>";
  private static final String filteredRelated = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n" + 
      "<html lang=\"en\" xml:lang=\"en\" style=\"\">\n" + 
      "    <div id=\"page\" class=\"main-container container\">\n" + 
      "      <div class=\"row\">\n" + 
      "        <section class=\"col-sm-12\">\n" + 
      "                    <ol class=\"breadcrumb\"><li class=\"first\"><a href=\"/us/research\">Research</a></li>\n" + 
      "<li>Explaining trends in...</li>\n" + 
      "<li class=\"active last\">Explaining</li>\n" + 
      "</ol>          <a id=\"main-content\"></a>\n" + 
      "<div class=\"region region-content\">\n" + 
      "    <section class=\"block block-system clearfix\" id=\"block-system-main\">\n" + 
      "  <div class=\"row\">\n" + 
      "        <article>\n" + 
      "          <div class=\"panel-pane pane-highwire-article-citation\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "<cite class=\"highwire-cite highwire-citation-bmj-article-top\">\n" + 
      "            <span class=\"highwire-cite-article-type\">Research</span>\n" + 
      "            <h1 id=\"page-title\" class=\"highwire-cite-title\">" +
      "<div xmlns=\"http://www.w3.org/1999/xhtml\">Explaining </div></h1>    \n" + 
      "    <span class=\"highwire-cite-journal\">BMJ</span>\n" + 
      "    <span class=\"highwire-cite-published-year\">2014</span>;\n" + 
      "    <span class=\"highwire-cite-volume-issue\">348</span>\n" + 
      "</cite>\n" + 
      "</div>  </div>\n" + 
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div>" +
      "<div class=\"panel-pane pane-highwire-panel-tabs pane-panels-ajax-tab-tabs\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <ul class=\"tabs inline panels-ajax-tab\">" +
      "<li class=\"first\"><span>Article</span></li>\n" + 
      "<li class=\"active\"><span>Related content</span></li>\n" + 
      "<li><span>Metrics</span></li>\n" + 
      "<li><span>Responses</span></li>\n" + 
      "<li><span>Peer review</span></li>\n" + 
      "</ul>  </div>\n" + 
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div><div class=\"panel-pane pane-highwire-panel-tabs-container\">\n" + 
      "  <div class=\"pane-content\">\n" + 
      "   <div class=\"panels-ajax-tab-container\" id=\"panels-ajax-tab-container-highwire_article_tabs\">\n" +
      "<div class=\"panels-ajax-tab-wrap-jnl_bmj_tab_related_art\">1" +
      "<div class=\"panel-display panel-1col clearfix\">\n" + 
      "  <div class=\"panel-panel panel-col\">\n" + 
      "    <div><div class=\"panel-pane pane-bmj-article-fig-data\">\n" + 
      "        <h2 class=\"pane-title\"><a name=\"datasupp\"></a>Data supplement</h2>\n" + 
      "  <div class=\"pane-content\">\n" + 
      "    <div class=\"group frag-additional-files\" id=\"fig-data-additional-files\">\n" +
      "<ul>" +
      "<li class=\"first\"><div class=\"highwire-markup\">\n" +
      "<div>\n" +
      "<div><span class=\"highwire-journal-article-marker-start\"></span><div>\n" + 
      "            <h2>Web Extra</h2>\n" + 
      "            <p>Extra material supplied by the author</p>\n" + 
      "            <p><strong>Files in this Data Supplement:</strong></p>\n" + 
      "            <ul><li><a href=\"/highwire/filestream/756546/field_highwire_adjunct_files/0/hotj014952.ww1_default.pdf\">Data Supplement</a> -\n" + 
      "                                Summary appendix\n" + 
      "               </li><li><a href=\"/highwire/filestream/756546/field_highwire_adjunct_files/1/hotj014952.ww2_default.pdf\">Data Supplement</a> -\n" + 
      "                                Technical appendix\n" + 
      "               </li></ul>\n" + 
      "         </span>\n" + 
      "   </div><span class=\"highwire-journal-article-marker-end\"></span>\n" +
      "</div><span id=\"related-urls\"></span></div></div>\n" +
      "</li></ul></div>  </div>\n" + 
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div>" +
      "\n" +
      "  </div>\n" + 
      "<div class=\"panel-separator\"></div>" +
      "\n" +
      "<div class=\"panel-separator\"></div>" +
      "\n" +
      "  </div>\n" + 
      "</div>\n" + 
      "  </div>\n" + 
      "</div>\n" + 
      "</div></div>  </div>\n" + 
      "  </div>\n" + 
      "        </article>\n" + 
      "  </div>\n" + 
      "</div>\n" + 
      "</section>  \n" + 
      "  </div>\n" + 
      "        </section>\n" + 
      "              </div> \n" + 
      "    </div> \n" + 
      "  </div>\n" + 
      "</div>\n" + 
      "</html>";
  
  
  public void testFiltering() throws Exception {
    InputStream inA;
    String a;
    
    // head
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withHead),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(filteredHead, a);
    
    // article
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withArticle),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(filteredArticle, a);
    
    // related - data supplements
    inA = fact.createFilteredInputStream(mau, new StringInputStream(withRelated),
        Constants.DEFAULT_ENCODING);
    a = StringUtil.fromInputStream(inA);
    assertEquals(filteredRelated, a);
    
  }
  
}
