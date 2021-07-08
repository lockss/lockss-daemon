package org.lockss.plugin.emerald;

import junit.framework.Test;
import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TestEmerald2020HtmlFilterFactory extends LockssTestCase {

    FilterFactory variantHashFact = new Emerald2020HtmlFilterFactory();
    ArchivalUnit mau;
    String tempDirPath;
    MockLockssDaemon daemon;
    PluginManager pluginMgr;

    private static Logger log = Logger.getLogger(
            TestEmerald2020HtmlFilterFactory.class);


    private static final String PLUGIN_ID =
            "org.lockss.plugin.emerald.Emerald2020Plugin";

    private static String FullContent = "" +
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n" +
            "<head>\n" +
            "    head section\n" +
            "</head>\n" +
            "<body data-spy=\"scroll\" data-target=\".table-of-contents\" data-offset=\"220\" class=\"\">\n" +
            "<div id=\"app\" class=\" nav-padding-login-search\">\n" +
            "    <header class=\"mb-0 mb-lg-0 fixed-top\">\n" +
            "        header section\n" +
            "    </header>\n" +
            "    <main role=\"main\" class=\"main-content \" id=\"mainContent\">\n" +
            "        <div class=\"container Chapter__header content_block\">\n" +
            "            <div class=\"row\">\n" +
            "                <div class=\"col-12 col-md-2\">\n" +
            "                    <div class=\"pt-3 pb-3 \">\n" +
            "                        <img class=\"border img-fluid intent_book_img\"  src=\"/insight/content/book/img/10.1108/9781787145016\"/>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div class=\"col-12 col-md-10\">\n" +
            "                    <header class=\"py-3\">\n" +
            "                        <h1 class=\"intent_article_title mt-0 mb-3\">“Internet of Things” Firms and New Patterns of Internationalization</h1>\n" +
            "                        <section id=\"intent_contributors\" class=\"mt-4 intent_contributors\">\n" +
            "                            <contributor-block block=\"&lt;span class=&quot;contrib-group&quot;&gt;\n" +
            "                              &lt;div xmlns:xlink=&quot;http://www.w3.org/1999/xlink&quot; class=&quot;contrib_block__contrib intent_contributor&quot; contrib-type=&quot;author&quot; xlink:type=&quot;simple&quot;&gt;&lt;a class=&quot;contrib-search-book-part-meta&quot; href=&quot;/insight/search?q=Valerio Veglio&quot;&gt; &lt;span class=&quot;given-names&quot;&gt;Valerio&lt;/span&gt;  &lt;span class=&quot;surname&quot;&gt;Veglio&lt;/span&gt;&lt;/a&gt;&lt;/div&gt;\n" +
            "                              &lt;/span&gt;\" contrib-count=\"1\" affil-count=\"0\">\n" +
            "                              <span class=\"contrib-group\">\n" +
            "                                 <div xmlns:xlink=\"http://www.w3.org/1999/xlink\" class=\"contrib_block__contrib intent_contributor\" contrib-type=\"author\" xlink:type=\"simple\"><a class=\"contrib-search-book-part-meta\" href=\"/insight/search?q=Valerio Veglio\"> <span class=\"given-names\">Valerio</span>  <span class=\"surname\">Veglio</span></a></div>\n" +
            "                              </span>\n" +
            "                            </contributor-block>\n" +
            "                        </section>\n" +
            "                        <div class=\"mb-2 row\">\n" +
            "                            <div class=\"col-12 col-md-6\">\n" +
            "                                <p>\n" +
            "                                 <span class=\"intent_book_title\">\n" +
            "                                 <a href=\"/insight/publication/doi/10.1108/9781787145016\">Global Opportunities for Entrepreneurial Growth: Coopetition and Knowledge Dynamics within and across Firms</a>\n" +
            "                                 </span>\n" +
            "                                </p>\n" +
            "                                <p class=\"mt-0\">\n" +
            "                                    <abbr title=\"International Standard Book Number.\" class=\"font-weight-normal\">ISBN</abbr>:\n" +
            "                                    <span class=\"intent_book_p_isbn\">978-1-78714-502-3</span>,\n" +
            "                                    <abbr title=\"International Standard Book Number. Electronic version of Book\" class=\"font-weight-normal\">eISBN</abbr>:\n" +
            "                                    <span class=\"intent_book_e_isbn\">978-1-78714-501-6</span>\n" +
            "                                </p>\n" +
            "                                <p class=\"mt-0\">\n" +
            "                                    <span class=\"intent_journal_publication_date\">Publication date: 14 December 2017</span>\n" +
            "                                    <span class=\"ml-2\" id=\"rights-link\">\n" +
            "                                    <rights-link-button\n" +
            "                                            root=\"/insight/\"\n" +
            "                                            img-src=\"/insight/static/img/rightsLink.png\"\n" +
            "                                            doi=\"10.1108/978-1-78714-501-620171009\"></rights-link-button>\n" +
            "                                 </span>\n" +
            "                                </p>\n" +
            "                            </div>\n" +
            "                            <div class=\"col-12 col-md-6\">\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </header>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <!--/Chapter__header-->\n" +
            "        <div class=\"bg-light border-top border-bottom py-3 mb-3 Chapter__stats content_block\">\n" +
            "            <div class=\"container \">\n" +
            "                <div class=\"row\">\n" +
            "                    <div class=\"col-12 col-md-2\"></div>\n" +
            "                    <div class=\"col-12 col-md-8\">\n" +
            "                        <div class=\"text-center text-sm-left\">\n" +
            "                            header nav section\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                    <div class=\"col-12 col-md-2\"></div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <!--/ Chapter__stats -->\n" +
            "        <div class=\"container Chapter__abstract\">\n" +
            "            <div class=\"row\">\n" +
            "                <div class=\"col-12 col-md-2 col-sm-12 p-0\">\n" +
            "                    <!-- Table of Contents -->\n" +
            "                    <div id=\"tocscroll\" >\n" +
            "                        tocscroll  content\n" +
            "                    </div>\n" +
            "                    <!--/ Table of Contents -->\n" +
            "                </div>\n" +
            "                <div class=\"col-12 col-md-7\">\n" +
            "                    <section id=\"abstract\" class=\"intent_abstract pt-2 Abstract\">\n" +
            "                        absctract content\n" +
            "                        <!--/ Abstract__block -->\n" +
            "                    </section>\n" +
            "                    <!--/ Abstract -->\n" +
            "                    <section id=\"keywords_list\" class=\"intent_keywords\">\n" +
            "                        keywords content\n" +
            "                    </section>\n" +
            "                    <section class=\"Citation mb-2\">\n" +
            "                        Citation content\n" +
            "                    </section>\n" +
            "                    <!--/ Citation -->\n" +
            "                    <section class=\"mt-1 Body\" v-pre>\n" +
            "                        <h3 class=\"d-inline h4\" id=\"page__publisher-label\">Publisher</h3>\n" +
            "                        :\n" +
            "                        <p class=\"publisher d-inline\" aria-labelledby=\"page__publisher-label\">\n" +
            "                            Emerald Publishing Limited\n" +
            "                        </p>\n" +
            "                    </section>\n" +
            "                    <!--/ Body -->\n" +
            "                    <p class=\"Citation__identifier\">\n" +
            "                        Copyright <span\n" +
            "                            class=\"intent_copyright_text\">&copy; 2018 Emerald Publishing Limited</span>\n" +
            "                    </p>\n" +
            "                    <!--/ Citation -->\n" +
            "                    <hr>\n" +
            "                    <section class=\"mb-5 Body \" v-pre>\n" +
            "                        main body content\n" +
            "                    </section>\n" +
            "                    <section class=\"References Chapter__references mt-5 \">\n" +
            "                            References section\n" +
            "                    </section>\n" +
            "                </div>\n" +
            "                <div class=\"col-12 col-md-3 \">\n" +
            "                    <div class=\"card my-4\">\n" +
            "                        <div class=\"card-header\" role=\"tab\" id=\"bookChapters\">\n" +
            "                            <h4 class=\"my-0\"><a data-toggle=\"collapse\" href=\"#collapse-book-chapters\" role=\"button\" aria-expanded=\"true\" aria-controls=\"collapse-book-chapters\">Book Chapters</a></h4>\n" +
            "                        </div>\n" +
            "                        <div id=\"collapse-book-chapters\" class=\"collapse show\" role=\"tabpanel\" aria-labelledby=\"collapse-book-chapters\" >\n" +
            "                            <div class=\"card-body text-small\">\n" +
            "                                card-body content\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </main>\n" +
            "    <flash message=\"\"></flash>\n" +
            "    <footer class=\"bg-dark pt-4 pb-5 text-center text-white\">\n" +
            "        footer section\n" +
            "    </footer>\n" +
            "    <div id=\"topscroll\">\n" +
            "        <top-scroll text=\"Back to top\" visibleoffset=\"500\"></top-scroll>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<div id=\"cookies-consent\">\n" +
            "    <cookies-consent\n" +
            "            root=\"/insight/\"\n" +
            "            class=\"\"></cookies-consent>\n" +
            "</div>\n" +
            "<div id=\"shivPlaceholder\" tabindex=\"-1\" class=\"shivContents position-relative text-center mt-3 mb-3 px-5 py-3 pt-4 border table-responsive\" style=\"display: none\">\n" +
            "    <div style=\"z-index: 999; right: 1rem; top: 0;\" class=\"position-absolute shiv-header row text-right\">\n" +
            "        <button style=\"font-size: 2.1rem; width:2.1rem; height:2.1rem; overflow: hidden;\" type=\"button\" class=\"close\" title=\"Close\">\n" +
            "            <span aria-hidden=\"true\">&times;</span>\n" +
            "        </button>\n" +
            "    </div>\n" +
            "    <div class=\"free dragscroll\">\n" +
            "        <div class=\"shivContent d-inline-block m-auto text-left position-relative\"></div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "\n" +
            "<div id=\"feedback-strip\" class=\"header-feedback dropdown  \">\n" +
            "    feedback-strip section\n" +
            "</div>\n" +
            "<div id=\"feedback-underlay\">feedback-underlay section</div>\n" +
            "</body>\n" +
            "</html>" ;

    private static final String FullContentHashFiltered = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n" +
            "\n" +
            "<body data-spy=\"scroll\" data-target=\".table-of-contents\" data-offset=\"220\" class=\"\">\n" +
            "<div id=\"app\" class=\" nav-padding-login-search\">\n" +
            "    \n" +
            "    <main role=\"main\" class=\"main-content \" id=\"mainContent\">\n" +
            "        <div class=\"container Chapter__header content_block\">\n" +
            "            <div class=\"row\">\n" +
            "                <div class=\"col-12 col-md-2\">\n" +
            "                    <div class=\"pt-3 pb-3 \">\n" +
            "                        <img class=\"border img-fluid intent_book_img\"  src=\"/insight/content/book/img/10.1108/9781787145016\"/>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div class=\"col-12 col-md-10\">\n" +
            "                    \n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <!--/Chapter__header-->\n" +
            "        <div class=\"bg-light border-top border-bottom py-3 mb-3 Chapter__stats content_block\">\n" +
            "            <div class=\"container \">\n" +
            "                <div class=\"row\">\n" +
            "                    <div class=\"col-12 col-md-2\"></div>\n" +
            "                    <div class=\"col-12 col-md-8\">\n" +
            "                        <div class=\"text-center text-sm-left\">\n" +
            "                            header nav section\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                    <div class=\"col-12 col-md-2\"></div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <!--/ Chapter__stats -->\n" +
            "        <div class=\"container Chapter__abstract\">\n" +
            "            <div class=\"row\">\n" +
            "                <div class=\"col-12 col-md-2 col-sm-12 p-0\">\n" +
            "                    <!-- Table of Contents -->\n" +
            "                    \n" +
            "                    <!--/ Table of Contents -->\n" +
            "                </div>\n" +
            "                <div class=\"col-12 col-md-7\">\n" +
            "                    \n" +
            "                    <!--/ Abstract -->\n" +
            "                    \n" +
            "                    \n" +
            "                    <!--/ Citation -->\n" +
            "                    <section class=\"mt-1 Body\" v-pre>\n" +
            "                        <h3 class=\"d-inline h4\" id=\"page__publisher-label\">Publisher</h3>\n" +
            "                        :\n" +
            "                        <p class=\"publisher d-inline\" aria-labelledby=\"page__publisher-label\">\n" +
            "                            Emerald Publishing Limited\n" +
            "                        </p>\n" +
            "                    </section>\n" +
            "                    <!--/ Body -->\n" +
            "                    <p class=\"Citation__identifier\">\n" +
            "                        Copyright <span\n" +
            "                            class=\"intent_copyright_text\">&copy; 2018 Emerald Publishing Limited</span>\n" +
            "                    </p>\n" +
            "                    <!--/ Citation -->\n" +
            "                    <hr>\n" +
            "                    <section class=\"mb-5 Body \" v-pre>\n" +
            "                        main body content\n" +
            "                    </section>\n" +
            "                    \n" +
            "                </div>\n" +
            "                <div class=\"col-12 col-md-3 \">\n" +
            "                    <div class=\"card my-4\">\n" +
            "                        \n" +
            "                        \n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </main>\n" +
            "    <flash message=\"\"></flash>\n" +
            "    \n" +
            "    <div id=\"topscroll\">\n" +
            "        <top-scroll text=\"Back to top\" visibleoffset=\"500\"></top-scroll>\n" +
            "    </div>\n" +
            "</div>\n" +
            "\n" +
            "<div id=\"shivPlaceholder\" tabindex=\"-1\" class=\"shivContents position-relative text-center mt-3 mb-3 px-5 py-3 pt-4 border table-responsive\" style=\"display: none\">\n" +
            "    <div style=\"z-index: 999; right: 1rem; top: 0;\" class=\"position-absolute shiv-header row text-right\">\n" +
            "        <button style=\"font-size: 2.1rem; width:2.1rem; height:2.1rem; overflow: hidden;\" type=\"button\" class=\"close\" title=\"Close\">\n" +
            "            <span aria-hidden=\"true\">&times;</span>\n" +
            "        </button>\n" +
            "    </div>\n" +
            "    <div class=\"free dragscroll\">\n" +
            "        <div class=\"shivContent d-inline-block m-auto text-left position-relative\"></div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "\n" +
            "\n" +
            "\n" +
            "</body>\n" +
            "</html>";

    protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
        return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
    }

    private Configuration thisAuConfig() {
        Configuration conf = ConfigManager.newConfiguration();
        conf.put("base_url", "http://www.example.com/");
        conf.put("journal_issn", "abc");
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
                log.error(e.getMessage(), e);
            }
        } catch (PluginException e) {
            log.error(e.getMessage(), e);
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

    public static class TestHash extends TestEmerald2020HtmlFilterFactory {

        public void testFullContentHash() throws Exception {
            String unicodeFilteredStr = getFilteredContent(mau, variantHashFact, FullContent);
            String unicodeExpectedStr = FullContentHashFiltered;

            assertEquals(unicodeFilteredStr, unicodeExpectedStr);
        }
    }

    public static Test suite() {
        return variantSuites(new Class[] {
                TestEmerald2020HtmlFilterFactory.TestHash.class
        });
    }

}

