/*
 * $Id: TestBMJDrupalHtmlCrawlFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire.bmj;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestBMJDrupalHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;
  
  private BMJDrupalHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new BMJDrupalHtmlCrawlFilterFactory();
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
      "  <link type=\"text/css\" media=\"all\" rel=\"stylesheet\" href=\"/sites/default/themes/bmj/the_bmj/css/fonts.css\">\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_-z-2lAhufzBeVjYseT6cTzSICUy9vnoLBpu1sF_zZrs.js\"></script>\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_8cesDs51kIWO42f3ZOLrsAfi9EEUt1q0P0uHVRja86Y.js\"></script>\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_Sh2UgvOvuSU9-K8qwk3-2k4jaFfLVCvAG5OTqQOumXM.js\"></script>\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_kTXHXdUjbRT6JXrj3C5k6w5lLUdroufD6DGU1pLvKl4.js\"></script>\n" + 
      "<script src=\"http://static.www.bmj.com/sites/default/files/js/js_d8nvZsNcH9M1AdbeKkecL0ZPaRgMfWLhCmktg3wHDu4.js\"></script><script src=\"http://oas.services.bmj.com/RealMedia/ads/adstream_mjx.ads/www.bmj.com/content/347/7915.toc/1716469929@Middle1,Middle2,Top,Top2?\" language=\"JavaScript1.1\"></script>\n" + 
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
      "    <header><!-- /header -->\n" + 
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
      "    <div class=\"pager highwire-pager pager-mini clearfix highwire-node-pager highwire-issue-pager\"><span class=\"pager-prev\"><a class=\"btn\" rel=\"prev\" data-font-icon=\"fa fa-arrow-circle-left\" title=\"BMJ: \" href=\"/content/341/7786\">Previous</a></span></div>  </div>\n" + 
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
    
  }
  
}
