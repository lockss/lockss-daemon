/*
 * $Id:$
 */
package org.lockss.plugin.pub2web.hbku;

import org.apache.commons.io.FileUtils;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.File;
import java.io.InputStream;

public class TestHBKUHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private HBKUHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new HBKUHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  private static final String aritlePage = "<div id=\"referenceContainer\">referenceContainer content</div>\n" +
          "<section class=\"trendmd-widget\">trendmd-widget content</section>\n" +
          "<section class=\"header-container\">header-container content</section>\n" +
          "<section class=\"navbar\">navbar content</section>\n" +
          "<div id=\"tools-nav\">tools-nav content</div>\n" +
          "<div class=\"sidebar-pub2web-element\">sidebar-pub2web-element content</div>\n" +
          "<div class=\"article\">main content</div>";

  private static final String aritlePageFiltered =
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "\n" +
          "<div class=\"article\">main content</div>";


  public void testArticlePage() throws Exception {
    InputStream inStream;
    inStream = fact.createFilteredInputStream(mau,
        new StringInputStream(aritlePage),
        Constants.DEFAULT_ENCODING);

    String filteredStr = StringUtil.fromInputStream(inStream);
    assertEquals(aritlePageFiltered, filteredStr);
  }

}
