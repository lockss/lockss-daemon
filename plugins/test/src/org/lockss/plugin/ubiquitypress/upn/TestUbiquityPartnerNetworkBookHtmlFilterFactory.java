/*
 * $Id:$
 */
package org.lockss.plugin.ubiquitypress.upn;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;
import java.io.File;
import java.io.InputStream;

public class TestUbiquityPartnerNetworkBookHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private UbiquityPartnerNetworkBookHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new UbiquityPartnerNetworkBookHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String aritlePage = "<div class=\"table-of-contents\">table-of-contents</div>\n" +
          "<div class=\"disciplines\">disciplines</div>\n" +
          "<div class=\"keywords\">keywords</div>\n" +
          "<div class=\"metrics-block\">metrics-block</div>\n" +
          "<div class=\"metrics\">metrics</div>\n" +
          "<div class=\"article\">main content</div>\n" +
          "<div id=\"thread__wrapper\">thread__wrapper</div>\n" +
          "<div class=\"read-list\">read-list</div>\n" +
          "<section class=\"brand-header\">brand-header</section>\n" +
          "<section class=\"book-cover\">book-cover</section>\n" +
          "<section class=\"how-tos\">how-tos</section>\n" +
          "<section class=\"comments\">comments </section>";

  private static final String aritlePageFiltered =
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "<div class=\"article\">main content</div>" +
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "\n";


  public void testArticlePage() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(aritlePage),
        Constants.DEFAULT_ENCODING);

    String filteredStr = StringUtil.fromInputStream(inStream);
    
    assertEquals(aritlePageFiltered, filteredStr);
  }

}
