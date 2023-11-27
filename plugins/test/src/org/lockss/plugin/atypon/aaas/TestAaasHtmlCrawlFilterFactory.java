package org.lockss.plugin.atypon.aaas;

import org.lockss.daemon.PluginException;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestAaasHtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AaasHtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AaasHtmlCrawlFilterFactory();
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
    //log.info("======result=====");
    //log.info(StringUtil.fromInputStream(actIn));
    return StringUtil.fromInputStream(actIn);
  }

 //From page https://www.science.org/doi/10.1126/scisignal.aav3810 need to filter out
 // overcrawled url: https://www.science.org/doi/10.1126/scisignal.aav3810
  String prev_next_nagivation_links =
          "<nav title=\"Content Navigation\" class=\"content-navigation\">\n" +
          "   <a href=\"/doi/10.1126/sciadv.adf5509\" title=\"An optical aptasensor for real-time quantification of endotoxin: From ensemble to single-molecule resolution\" class=\"content-navigation__prev\">\n" +
          "      <div aria-hidden=\"true\" class=\"content-navigation__hint\">\n" +
          "         <div class=\"content-navigation__hint__content\">\n" +
          "            <h6>PREVIOUS ARTICLE</h6>\n" +
          "            <div>An optical aptasensor for real-time quantification of endotoxin: From ensemble to single-molecule resolution</div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "      <i aria-hidden=\"true\" class=\"icon-arrow-left\"></i><span>contentNavigation.previous.default</span>\n" +
          "   </a>\n" +
          "   <a href=\"/doi/10.1126/sciadv.abo6405\" title=\"A highly specific CRISPR-Cas12j nuclease enables allele-specific genome editing\" class=\"content-navigation__next\">\n" +
          "      <div aria-hidden=\"true\" class=\"content-navigation__hint\">\n" +
          "         <div class=\"content-navigation__hint__content\">\n" +
          "            <h6>NEXT ARTICLE</h6>\n" +
          "            <div>A highly specific CRISPR-Cas12j nuclease enables allele-specific genome editing</div>\n" +
          "         </div>\n" +
          "      </div>\n" +
          "      <span>contentNavigation.next.default</span><i aria-hidden=\"true\" class=\"icon-arrow-right\"></i>\n" +
          "   </a>\n" +
          "</nav>";

  String prev_next_nagivation_links_filtered = "";

  String related_content = "" +
          "<div role=\"listitem\" class=\"related-item p-3\">\n" +
          "   <div role=\"heading\" class=\"related-item__heading\">This article has a correction.</div>\n" +
          "   <div class=\"related-item__content\"><span>Please see:&nbsp;</span><a href=\"/doi/10.1126/scisignal.abb5851\"><span>Erratum for the Research Article: “Biased M<sub>1</sub> receptor–positive allosteric modulators reveal role of phospholipase D in M<sub>1</sub>-dependent rodent cortical plasticity” by S. P. Moran, Z. Xiang, C. A. Doyle, J. Maksymetz, X. Lv, S. Faltin, N. M. Fisher, C. M. Niswender, J. M. Rook, C.W. Lindsley, P. J. Conn -<time datetime=\"Mar 17, 2020, 12:00:00 AM\"> 17 March 2020</time></span></a></div>\n" +
          "</div>";

  String related_content_filtered = "";


  public void testPrevNextFiltering() throws Exception {
    assertEquals(prev_next_nagivation_links_filtered, filterString(prev_next_nagivation_links));
  }

    public void testRelatedContentFiltering() throws Exception {
        assertEquals(related_content_filtered, filterString(related_content));
    }
}
