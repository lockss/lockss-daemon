package org.lockss.plugin.atypon.aaas;

import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestAaasHtmlLinkRewriterFactory  extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  static final String FOO_DOI = "10.1126/journal.123.456";
  static final String BASE_URL = "http://www.science.org/";
  AaasHtmlLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AaasHtmlLinkRewriterFactory();
  }

  static final String testCitationLink =
    "<div class=\"info-panel__citations info-panel__item\">" +
      "<a href=\"#pill-citations\" title=\"\" data-toggle=\"tooltip\" class=\"btn\" data-original-title=\"cite\">" +
        "<i class=\"icon-citations\"></i>" + // gets represented by an double-quote icon
      "</a>" +
    "</div>";

  // the link rewriter replaces #pill-citations with a link to the ris citation file for the url of the page
  // additionally the href gets prepended with the lockss daemon ServeContent action
  // note that the url gets url encoded as well.
  static final String testCitationLinkRewritten =
    "<div class=\"info-panel__citations info-panel__item\">" +
      "<a href=\"http://www.lockssdaemon.org/ServeContent?url=" +
        "http%3A%2F%2Fwww.science.org%2F" +
        "action%2FdownloadCitation%3Fdoi%3D10.1126%2Fjournal.123.456%26format%3Dris%26include%3Dcit\"" +
        " title=\"\" data-toggle=\"tooltip\" " +
        "class=\"btn\" " +
        "data-original-title=\"cite\" " +
        "target=_blank>" +
        "<i class=\"icon-citations\"></i>" +
      "</a>" +
    "</div>";

  /**
   * Make a basic test AU to which URLs can be added.
   *
   * @return a basic test AU
   * @throws ArchivalUnit.ConfigurationException if can't set configuration
   */
  MockArchivalUnit makeAu() throws ArchivalUnit.ConfigurationException {
    MockArchivalUnit mau = new MockArchivalUnit();
    Configuration config = ConfigurationUtil.fromArgs(
        "base_url", BASE_URL);
    mau.setConfiguration(config);
    mau.setUrlStems(ListUtil.list(
        BASE_URL
    ));
    return mau;
  }

  public void testCitationAnchorRewriting() throws Exception {
    MockArchivalUnit mockAu = makeAu();

    InputStream in = new ByteArrayInputStream(testCitationLink.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.lockssdaemon.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn =
      fact.createLinkRewriter(
        "text/html",
        mockAu,
        in,
        "UTF-8",
        BASE_URL + "/doi/" + FOO_DOI,
        xfm
      );
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testCitationLinkRewritten, fout);
  }

}
