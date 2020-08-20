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

package org.lockss.plugin.spandidos;

import junit.framework.Test;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestSpandidosHtmlHashFilterFactory extends LockssTestCase {

  FilterFactory variantHashFact = new SpandidosHtmlFilterFactory();
  ArchivalUnit mau;
  String tempDirPath;
  MockLockssDaemon daemon;
  PluginManager pluginMgr;

  private static Logger log = Logger.getLogger(
          TestSpandidosHtmlHashFilterFactory.class);


  private static final String PLUGIN_ID =
          "org.lockss.plugin.spandidos.SpandidosPlugin";

  private static final String manifestContent = "<html>\n" +
          "<!--\n" +
          "<![endif]-->\n" +
          "<head>\n" +
          "   head section...\n" +
          "</head>\n" +
          "<body class=\"br\" >\n" +
          "<div class=\"wrapper\">\n" +
          "    <div>\n" +
          "        <!-- Journal and Article pages show Journal banner -->\n" +
          "    </div>\n" +
          "    <div class=\"content wide\">\n" +
          "        <div class=\"content fixed_width\">\n" +
          "            <div class=\"content_main_home\">\n" +
          "                <div class=\"content_heading\">\n" +
          "                    <h1>Archive Issues</h1>\n" +
          "                </div>\n" +
          "                <!-- Output list by Volumes in descending order -->\n" +
          "                <p class=\"pushdown2\"> Articles can also be requested from the\n" +
          "                    <a href=\"https://www.contentscm.com/vlib/logon.aspx\">Infotrieve</a>\n" +
          "                    document delivery service or the\n" +
          "                    <a href=\"http://www.bl.uk/\">British Library</a>.\n" +
          "                </p>\n" +
          "            </div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "    <div class=\"wide\">\n" +
          "        <div class=\"event fixed_width\"></div>\n" +
          "        <div class=\"event fixed_width\"></div>\n" +
          "        <div class=\"event fixed_width\"></div>\n" +
          "    </div>\n" +
          "</div>\n" +
          "</body>\n" +
          "</html>";

  private static final String manifestHashFiltered = "<div class=\"content_main_home\"> </div>";


  private static String FullContent = "<html>\n" +
          "<head>\n" +
          "    header section...\n" +
          "</head>\n" +
          "<body class=\"br\">\n" +
          "<!--[if lt IE 7]>\n" +
          "<p class=\"chromeframe\">You are using an outdated browser.\n" +
          "    <a href=\"http://browsehappy.com/\">Upgrade your browser today</a> or\n" +
          "    <a href=\"http://www.google.com/chromeframe/?redirect=true\">install Google Chrome Frame</a> to better experience this site.\n" +
          "</p>\n" +
          "<![endif]-->\n" +
          "<div class=\"wrapper\">\n" +
          "    <div>\n" +
          "        <div class=\"super wide\">\n" +
          "           super section...\n" +
          "        </div>\n" +
          "        <div class=\"header wide\">\n" +
          "            header section...\n" +
          "        </div>\n" +
          "        <!-- Journal and Article pages show Journal banner -->\n" +
          "        <div class=\"banner wide\">\n" +
          "            <div class=\"banner fixed_width\">\n" +
          "                <h1>Biomedical\n" +
          "                    <br />Reports\n" +
          "                </h1>\n" +
          "                <img src=\"/resources/images/bannerbr.jpg\" alt=\"Journal Banner\" />\n" +
          "            </div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "    <div class=\"content wide\">\n" +
          "        <div class=\"content fixed_width\">\n" +
          "            <div class=\"content_sidebar\">\n" +
          "                content_sidebar section...\n" +
          "            </div>\n" +
          "            <div class=\"content_main\">\n" +
          "                <div class=\"content_article\">\n" +
          "                    <h1 id=\"titleId\">Ecto‑protein kinase CK2, the neglected form of CK2 (Review)</h1>\n" +
          "                    <ul class=\"article_details\">\n" +
          "                        article_details...\n" +
          "                    </ul>\n" +
          "                    <div>\n" +
          "                        <div id=\"metrics\">\n" +
          "                            metrics section...\n" +
          "                        </div>\n" +
          "                        <a class=\"moreLikeThis\" style=\"display:none\" href=\"/morelikethis/br_8_4_307\"></a>\n" +
          "                        <div id=\"citationDiv\" style=\"display:none\">\n" +
          "                            <h4>This article is mentioned in:</h4>\n" +
          "                            <div id=\"citations\"></div>\n" +
          "                        </div>\n" +
          "                        <span class=\"__dimensions_badge_embed__\" data-hide-zero-citations=\"true\" data-legend=\"always\" data-style=\"small_circle\" data-doi=\"10.3892/br.2018.1069\"></span>\n" +
          "                        <script async=\"async\" src=\"https://badge.dimensions.ai/badge.js\" charset=\"utf-8\"></script>\n" +
          "                        <br />\n" +
          "                        <span data-hide-no-mentions=\"true\" data-badge-details=\"right\" class=\"altmetric-embed\" data-badge-type=\"donut\" data-link-target=\"_blank\" data-doi=\"10.3892/br.2018.1069\"></span>\n" +
          "                    </div>\n" +
          "                    <h4>Abstract</h4>\n" +
          "                    <div>article abstract section...</div>\n" +
          "                    <div>\n" +
          "                        <div>\n" +
          "                            article main content and other supported information....\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                    <div></div>\n" +
          "                    <div id=\"moreLikeThisDiv\">\n" +
          "                        <h4>Related Articles</h4>\n" +
          "                        <ul style=\"list-style-type:disc\" id=\"moreLikeThisUl\" class=\"moreLikeThisClassFragment\"></ul>\n" +
          "                    </div>\n" +
          "                    <div class=\"article_interactive\">\n" +
          "                        article_interative section...\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "            <div class=\"journal_information\">\n" +
          "                journal_information section...\n" +
          "            </div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "    <div class=\"wide\">\n" +
          "        wide section...\n" +
          "    </div>\n" +
          "    <div class=\"footer wide\">\n" +
          "        <div id=\"navbar\" align=\"center\" class=\"fixed_width footer\">\n" +
          "            navbar section...\n" +
          "        </div>\n" +
          "        <div class=\"footer fixed_width\">\n" +
          "            footer section...\n" +
          "        </div>\n" +
          "        <div id=\"growl\" class=\"bottom-left\"></div>\n" +
          "    <!-- modal content -->\n" +
          "    <div id=\"osx-modal-content\">\n" +
          "        osx-modal-content section...\n" +
          "    </div>\n" +
          "</div>\n" +
          "<!-- End Wrapper -->\n" +
          "</body>\n" +
          "</html>" ;

  private static String AbsContent = "<html>\n" +
          "<head>\n" +
          "    head section...\n" +
          "</head>\n" +
          "<body class=\"br\">\n" +
          "<!--[if lt IE 7]>\n" +
          "<p class=\"chromeframe\">You are using an outdated browser.\n" +
          "    <a href=\"http://browsehappy.com/\">Upgrade your browser today</a> or\n" +
          "    <a href=\"http://www.google.com/chromeframe/?redirect=true\">install Google Chrome Frame</a> to better experience this site.\n" +
          "</p>\n" +
          "<![endif]-->\n" +
          "<div class=\"wrapper\">\n" +
          "    <div>\n" +
          "        <div class=\"super wide\">\n" +
          "            <div class=\"super fixed_width\">\n" +
          "                super section...\n" +
          "            </div>\n" +
          "        </div>\n" +
          "        <div class=\"header wide\">\n" +
          "            <div class=\"header fixed_width\">header section...</div>\n" +
          "        </div>\n" +
          "        <!-- Journal and Article pages show Journal banner -->\n" +
          "        <div class=\"banner wide\">\n" +
          "            <div class=\"banner fixed_width\">\n" +
          "                <h1>Biomedical\n" +
          "                    <br />Reports\n" +
          "                </h1>\n" +
          "                <img src=\"/resources/images/bannerbr.jpg\" alt=\"Journal Banner\" />\n" +
          "            </div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "    <div class=\"content wide\">\n" +
          "        <div class=\"content fixed_width\">\n" +
          "            <div class=\"content_sidebar\">\n" +
          "               content sidebar section...\n" +
          "            </div>\n" +
          "            <div class=\"content_main\">\n" +
          "                <div class=\"content_article\">\n" +
          "                    <h1 id=\"titleId\">Ecto‑protein kinase CK2, the neglected form of CK2 (Review)</h1>\n" +
          "                    <ul class=\"article_details\">\n" +
          "                        li article details...\n" +
          "                    </ul>\n" +
          "                    <div>\n" +
          "                        <div id=\"metrics\">\n" +
          "                            metrics section...\n" +
          "                        </div>\n" +
          "                        <a class=\"moreLikeThis\" style=\"display:none\" href=\"/morelikethis/br_8_4_307\"></a>\n" +
          "                        <div id=\"citationDiv\" style=\"display:none\">\n" +
          "                            <h4>This article is mentioned in:</h4>\n" +
          "                            <div id=\"citations\"></div>\n" +
          "                        </div>\n" +
          "                        <span class=\"__dimensions_badge_embed__\" data-hide-zero-citations=\"true\" data-legend=\"always\" data-style=\"small_circle\" data-doi=\"10.3892/br.2018.1069\"></span>\n" +
          "                        <script async=\"async\" src=\"https://badge.dimensions.ai/badge.js\" charset=\"utf-8\"></script>\n" +
          "                        <br />\n" +
          "                        <span data-hide-no-mentions=\"true\" data-badge-details=\"right\" class=\"altmetric-embed\" data-badge-type=\"donut\" data-link-target=\"_blank\" data-doi=\"10.3892/br.2018.1069\"></span>\n" +
          "                    </div>\n" +
          "                    <h4>Abstract</h4>\n" +
          "                    <div>Abstract content of the article...</div>\n" +
          "                    <div></div>\n" +
          "                    <div>\n" +
          "                        <div>\n" +
          "                            <h4>References</h4>\n" +
          "                            References table section...\n" +
          "                        </div>\n" +
          "                    </div>\n" +
          "                    <div id=\"moreLikeThisDiv\">\n" +
          "                        <h4>Related Articles</h4>\n" +
          "                        <ul style=\"list-style-type:disc\" id=\"moreLikeThisUl\" class=\"moreLikeThisClassFragment\"></ul>\n" +
          "                    </div>\n" +
          "                    <div class=\"article_interactive\">\n" +
          "                       article interactive section...\n" +
          "                    </div>\n" +
          "                </div>\n" +
          "            </div>\n" +
          "            <div class=\"journal_information\">\n" +
          "                journal information section...\n" +
          "            </div>\n" +
          "        </div>\n" +
          "    </div>\n" +
          "    <div class=\"wide\">\n" +
          "       wide section...\n" +
          "    </div>\n" +
          "    <div class=\"footer wide\">\n" +
          "        footer section...\n" +
          "    </div>\n" +
          "\n" +
          "    <!-- modal content -->\n" +
          "    <div id=\"osx-modal-content\">\n" +
          "        osx-modal-content section...\n" +
          "    </div>\n" +
          "</div>\n" +
          "<!-- End Wrapper -->\n" +
          "</body>\n" +
          "</html>";

  private static final String FullContentHashFiltered = "<div class=\"content_main\"> <div class=\"content_article\"> <div> <br /> </div> <h4>Abstract</h4> <div>article abstract section...</div> <div> <div> article main content and other supported information.... </div> </div> <div></div> </div> </div>";
  private static final String AbsContentHashFiltered = "<div class=\"content_main\"> <div class=\"content_article\"> <div> <br /> </div> <h4>Abstract</h4> <div>Abstract content of the article...</div> <div></div> <div> <div> <h4>References</h4> References table section... </div> </div> </div> </div>";

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
  }

  private Configuration thisAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "abc");
    conf.put("volume_name", "99");
    return conf;
  }


  private static String getFilteredContent(ArchivalUnit au, FilterFactory fact, String nameToHash) {

    InputStream actIn;
    String filteredStr = "";

    try {
      actIn = fact.createFilteredInputStream(au,
              new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);

      try {

        filteredStr = StringUtil.fromInputStream(actIn);

      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (PluginException e) {
      e.printStackTrace();
    }

    return filteredStr;
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


  private static void compareContentLineByLine(String before, String after) {
    String[] beforeArr = before.split("\n");
    String[] afterArr = after.split("\n");

    int len = beforeArr.length;

    for (int i = 0, sb1_i = 0, sb2_i = 0;  i < len; i++, sb1_i++, sb2_i++) {
      StringBuilder sb1 = new StringBuilder();
      StringBuilder sb2 = new StringBuilder();

      sb1.append(beforeArr[i].replaceAll("\\s+", ""));
      sb2.append(afterArr[i].replaceAll("\\s+", ""));

      assertEquals(sb2.toString(), sb2.toString());

      sb1.setLength(0);
      sb2.setLength(0);
    }

  }

  public static class TestHash extends TestSpandidosHtmlHashFilterFactory {

    public void testManifestHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, manifestContent);
      String unicodeExpectedStr = manifestHashFiltered;
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }


    public void testFullContentHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, FullContent);
      String unicodeExpectedStr = FullContentHashFiltered;

      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }


    public void testAbsContentHash() throws Exception {
      String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, AbsContent);
      String unicodeExpectedStr = AbsContentHashFiltered;
      assertEquals(unicodeFilteredStr, unicodeExpectedStr);
    }
  }

  public static Test suite() {
    return variantSuites(new Class[] {
            TestHash.class
    });
  }

}
