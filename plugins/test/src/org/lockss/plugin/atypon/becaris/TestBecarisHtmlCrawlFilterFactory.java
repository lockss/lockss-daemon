package org.lockss.plugin.atypon.becaris;

import org.lockss.daemon.PluginException;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestBecarisHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private BecarisHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BecarisHtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }

  public String filterString(String rawHtml) throws IOException {
    InputStream actIn = null;
    try {
      actIn = fact.createFilteredInputStream(mau,
          new StringInputStream(rawHtml),
          Constants.DEFAULT_ENCODING);
    } catch (PluginException e) {
      throw new RuntimeException(e);
    }
    return StringUtil.fromInputStream(actIn);
  }


  String related_content = "<div id=\"relatedTab3\" data-exp-type=\"\" data-query-id=\"10.2217/cer-2022-0164\" class=\"accordion-tabbed__content\">\n" +
          "   <ul class=\"rlist lot\">\n" +
          "      <li class=\"grid-item\">\n" +
          "         <div class=\"teaser\">\n" +
          "            <a href=\"/doi/full/10.2217/cer-2022-0187\" title=\"Semaglutide treatment for obesity in teenagers: a plain language summary of the STEP TEENS research study\">\n" +
          "               <h5 class=\"teaser__title\">Semaglutide treatment for obesity in teenagers: a plain language summary of the STEP TEENS research study</h5>\n" +
          "            </a>\n" +
          "            <ul class=\"meta__authors rlist--inline loa mobile-authors\" title=\"list of authors\">\n" +
          "               <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorRaw=Weghuber%2C+Daniel\" title=\"Daniel Weghuber\">Daniel Weghuber</a></span>, </li>\n" +
          "               <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorRaw=Boberg%2C+Klaus\" title=\"Klaus Boberg\">Klaus Boberg</a></span>, </li>\n" +
          "               <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorRaw=Hesse%2C+Dan\" title=\"Dan Hesse\">Dan Hesse</a></span>, </li>\n" +
          "               <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorRaw=Jeppesen%2C+Ole+K\" title=\"Ole K Jeppesen\">Ole K Jeppesen</a></span>, </li>\n" +
          "               <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorRaw=S%C3%B8rrig%2C+Rasmus\" title=\"Rasmus Sørrig\">Rasmus Sørrig</a></span> &amp; </li>\n" +
          "               <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorRaw=Kelly%2C+Aaron+S\" title=\"Aaron S Kelly\">Aaron S Kelly</a></span> &amp; </li>\n" +
          "               <li><span class=\"hlFld-ContribAuthor\"><a href=\"/action/doSearch?ContribAuthorRaw=\" title=\"for the STEP TEENS Investigators\">for the STEP TEENS Investigators</a></span></li>\n" +
          "            </ul>\n" +
          "            <div class=\"teaser__meta\"><span class=\"teaser__citation-line\"><a href=/toc/cer/12/2>Vol. 12, No. 2\n" +
          "               </a></span><span class=\"teaser__date\"><time datetime=\"December 2022\">December 2022</time></span>\n" +
          "            </div>\n" +
          "         </div>\n" +
          "      </li>\n" +
          "   </ul>\n" +
          "</div>";

  String related_content_filtered = "";


    public void testRelatedContentFiltering() throws Exception {
        assertEquals(related_content_filtered, filterString(related_content));
    }
}
