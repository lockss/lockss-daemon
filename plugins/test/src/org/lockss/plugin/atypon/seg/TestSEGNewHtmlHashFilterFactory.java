package org.lockss.plugin.atypon.seg;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;
import java.util.Properties;

public class TestSEGNewHtmlHashFilterFactory extends LockssTestCase {
    private SEGNewHtmlHashFilterFactory fact;

    private ArchivalUnit mau;

    public void setUp() throws Exception {
        super.setUp();
        fact = new SEGNewHtmlHashFilterFactory();
        //the hash filter (for backIssues.asp) requires au information
        Properties props = new Properties();
        Configuration conf = ConfigManager.newConfiguration();
        props.setProperty(ConfigParamDescr.BASE_URL.getKey(), "http://www.foo.com/");
        props.setProperty(ConfigParamDescr.JOURNAL_ID.getKey(), "aag");
        props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "99");
        Configuration config = ConfigurationUtil.fromProps(props);

        DefinablePlugin ap = new DefinablePlugin();
        ap.initPlugin(getMockLockssDaemon(),
                "org.lockss.plugin.atypon.seg.ClockssSEGPlugin");
        mau = (DefinableArchivalUnit) ap.createAu(config);
    }


    private static final String tocHtml = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" class=\"pb-page\"  data-request-id=\"2800f01d-6b66-4e80-a7bf-6c8035639381\">\n" +
            "<head data-pb-dropzone=\"head\">\n" +
            "    head content\n" +
            "</head>\n" +
            "<body class=\"pb-ui\">\n" +
            "<script type=\"text/javascript\">\n" +
            "         if(false) {\n" +
            "             document.getElementById(\"skipNavigationLink\").onclick =function skipElement () {\n" +
            "                 var element = document.getElementById('');\n" +
            "                 if(element == null || element == undefined) {\n" +
            "                     element = document.getElementsByClassName('').item(0);\n" +
            "                 }\n" +
            "                 element.setAttribute('tabindex','0');\n" +
            "                 element.focus();\n" +
            "             }\n" +
            "         }\n" +
            "      </script>\n" +
            "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
            "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
            "        <div>\n" +
            "            <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb-assets/styles/journal-branding/journal-brnading-inteio-1548001506880.css\">\n" +
            "            <div class=\"popup login-popup hidden\">\n" +
            "                popup content 1\n" +
            "            </div>\n" +
            "            <div class=\"popup change-password-drawer hidden\">\n" +
            "                popup content 2\n" +
            "            </div>\n" +
            "            <div class=\"popup registration-popup hidden\">\n" +
            "                popup content 3\n" +
            "            </div>\n" +
            "            <div class=\"popup top-drawer request-reset-password-drawer hidden\">\n" +
            "                popup content 4\n" +
            "            </div>\n" +
            "            <div class=\"popup top-drawer request-username-drawer username-popup hidden\">\n" +
            "                popup content 5\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <header class=\"header fixed\">\n" +
            "            header section\n" +
            "        </header>\n" +
            "        <main class=\"content toc-page journal-branding\">\n" +
            "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"65aec47b-0e4b-49ba-92fe-95f538c4f4ea\" class=\"page-top-banner\">\n" +
            "                <div class=\"container\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div>\n" +
            "                            <div data-widget-def=\"UX3HTMLWidget\" data-widget-id=\"ac6bb9e6-10e0-4cd3-a9b8-a5d6366a25d5\" class=\"title__row\">\n" +
            "                                <h2 class=\"page__title\">Table of Contents</h2>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div class=\"container\">\n" +
            "                <div class=\"row\">\n" +
            "                    <div class=\"shift-up-content\">\n" +
            "                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"743a72fe-92d3-4b57-9a2d-9e26c0936b08\" class=\"title-block seg-basic-background\">\n" +
            "                            <div>\n" +
            "                                <div class=\"publication__menu\">\n" +
            "                                    <div class=\"publication__menu__journal__logo\">\n" +
            "                                        <a href=\"/journal/inteio\">\n" +
            "                                            <img src=\"/pb-assets/Logos/journals/inteio-1547138235203.svg\" alt=\"journal logo\">\n" +
            "                                        </a>\n" +
            "                                    </div>\n" +
            "                                    <button data-ctrl-res=\"screen-lg\" data-slide-target=\".publication__menu .publication__menu__list\" data-slide-clone=\"self\" class=\"w-slide__btn publication__nav__toggler\">\n" +
            "                              <span class=\"icon-list\">\n" +
            "                              <span class=\"sr-only open\">Menu</span>\n" +
            "                              </span>\n" +
            "                                    </button>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"32e77aae-6ffb-4805-a758-28943a51646b\" class=\"page__content\">\n" +
            "                <div class=\"container\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div>\n" +
            "                            <div class=\"container\">\n" +
            "                                <div class=\"row\">\n" +
            "                                    <div class=\"col-lg-8 col-md-8\">\n" +
            "                                        <div class=\"current-issue\">\n" +
            "                                            current issue content\n" +
            "                                        </div>\n" +
            "                                        <div data-view-fewer-author=\"View fewer authors\" data-view-all-author=\"View all authors\" class=\"truncate-captions hidden\">\n" +
            "                                        </div>\n" +
            "                                        <div class=\"toc-container\">\n" +
            "                                            <div class=\"actionsbar actionsbar__has__sections fixed-element\">\n" +
            "                                            </div>\n" +
            "                                            <!--totalCount38--><!--modified:1585829677000-->\n" +
            "                                            <div class=\"table-of-content\">       table-of-content </div>\n" +
            "                                            <div data-widget-def=\"UX3ContentNavigation\" data-widget-id=\"b1fa22d4-dbf3-49ce-b5e8-7e9af81a7830\" class=\"table-of-content__navigation\">\n" +
            "                                                table-of-content__navigation content\n" +
            "                                            </div>\n" +
            "                                        </div>\n" +
            "                                        <div class=\"col-lg-4 col-md-4 col-sm-12\">\n" +
            "                                            <div class=\"social-menus\">\n" +
            "                                                <div class=\"page__useful-links card card--shadow social-links\">\n" +
            "                                                </div>\n" +
            "                                                <div class=\"page__social-links card card--shadow social-links\">\n" +
            "                                                    <h5 class=\"uppercase card__title\">stay connected</h5>\n" +
            "                                                </div>\n" +
            "                                            </div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "        </main>\n" +
            "        <div>\n" +
            "            <footer class=\"footer\">\n" +
            "                footer section\n" +
            "            </footer>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<script type=\"text/javascript\" src=\"/wro/product.js\"></script>\n" +
            "</body>\n" +
            "</html>";


    private static final String tocHtmlKept = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" class=\"pb-page\"  data-request-id=\"2800f01d-6b66-4e80-a7bf-6c8035639381\">\n" +
            "\n" +
            "<body class=\"pb-ui\">\n" +
            "\n" +
            "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
            "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
            "        <div>\n" +
            "            <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb-assets/styles/journal-branding/journal-brnading-inteio-1548001506880.css\">\n" +
            "            \n" +
            "            \n" +
            "            \n" +
            "            \n" +
            "            \n" +
            "        </div>\n" +
            "        \n" +
            "        <main class=\"content toc-page journal-branding\">\n" +
            "            \n" +
            "            <div class=\"container\">\n" +
            "                <div class=\"row\">\n" +
            "                    \n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"32e77aae-6ffb-4805-a758-28943a51646b\" class=\"page__content\">\n" +
            "                <div class=\"container\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div>\n" +
            "                            <div class=\"container\">\n" +
            "                                <div class=\"row\">\n" +
            "                                    <div class=\"col-lg-8 col-md-8\">\n" +
            "                                        \n" +
            "                                        <div data-view-fewer-author=\"View fewer authors\" data-view-all-author=\"View all authors\" class=\"truncate-captions hidden\">\n" +
            "                                        </div>\n" +
            "                                        <div class=\"toc-container\">\n" +
            "                                            <div class=\"actionsbar actionsbar__has__sections fixed-element\">\n" +
            "                                            </div>\n" +
            "                                            <!--totalCount38--><!--modified:1585829677000-->\n" +
            "                                            <div class=\"table-of-content\">       table-of-content </div>\n" +
            "                                            \n" +
            "                                        </div>\n" +
            "                                        <div class=\"col-lg-4 col-md-4 col-sm-12\">\n" +
            "                                            \n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "        </main>\n" +
            "        <div>\n" +
            "            \n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "\n" +
            "</body>\n" +
            "</html>";


    private static final String articleHtml = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" class=\"pb-page\"  data-request-id=\"655bf1af-6002-4272-9f3d-40550e726c29\">\n" +
            "<head data-pb-dropzone=\"head\">\n" +
            "    head content\n" +
            "</head>\n" +
            "<body class=\"pb-ui\">\n" +
            "<script type=\"text/javascript\">\n" +
            "         if(false) {\n" +
            "             document.getElementById(\"skipNavigationLink\").onclick =function skipElement () {\n" +
            "                 var element = document.getElementById('');\n" +
            "                 if(element == null || element == undefined) {\n" +
            "                     element = document.getElementsByClassName('').item(0);\n" +
            "                 }\n" +
            "                 element.setAttribute('tabindex','0');\n" +
            "                 element.focus();\n" +
            "             }\n" +
            "         }\n" +
            "\n" +
            "      </script>\n" +
            "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
            "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
            "        <div>\n" +
            "            <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb-assets/styles/journal-branding/journal-brnading-inteio-1548001506880.css\">\n" +
            "            <style>\n" +
            "                  .article__body .fig .popup {\n" +
            "                  visibility: hidden;\n" +
            "                  }\n" +
            "               </style>\n" +
            "            <div class=\"popup login-popup hidden\">\n" +
            "                popup content 1\n" +
            "            </div>\n" +
            "            <div class=\"popup change-password-drawer hidden\">\n" +
            "                popup content 2\n" +
            "            </div>\n" +
            "            <div class=\"popup registration-popup hidden\">\n" +
            "                popup content 3\n" +
            "            </div>\n" +
            "            <div class=\"popup top-drawer request-reset-password-drawer hidden\">\n" +
            "                popup content 4\n" +
            "            </div>\n" +
            "            <div class=\"popup top-drawer request-username-drawer username-popup hidden\">\n" +
            "                popup content 5\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <header class=\"header fixed\">\n" +
            "            header section\n" +
            "        </header>\n" +
            "        <main class=\"content journal-branding article-page\">\n" +
            "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"65aec47b-0e4b-49ba-92fe-95f538c4f4ea\" class=\"page-top-banner\">\n" +
            "                <div class=\"container\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div>\n" +
            "                            <div data-widget-def=\"UX3HTMLWidget\" data-widget-id=\"ac6bb9e6-10e0-4cd3-a9b8-a5d6366a25d5\" class=\"title__row\">\n" +
            "                                <h2 class=\"page__title\"></h2>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div class=\"container\">\n" +
            "                <div class=\"row\">\n" +
            "                    <div class=\"shift-up-content\">\n" +
            "                        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"743a72fe-92d3-4b57-9a2d-9e26c0936b08\" class=\"title-block seg-basic-background\">\n" +
            "                            <div>\n" +
            "                                <div class=\"publication__menu\">\n" +
            "                                    <div class=\"publication__menu__journal__logo\">\n" +
            "                                        <a href=\"/journal/inteio\">\n" +
            "                                            <img src=\"/pb-assets/Logos/journals/inteio-1547138235203.svg\" alt=\"journal logo\">\n" +
            "                                        </a>\n" +
            "                                    </div>\n" +
            "                                    <button data-ctrl-res=\"screen-lg\" data-slide-target=\".publication__menu .publication__menu__list\" data-slide-clone=\"self\" class=\"w-slide__btn publication__nav__toggler\"><span class=\"icon-list\"><span class=\"sr-only open\">Menu</span></span></button>\n" +
            "                                    <ul class=\"rlist publication__menu__list\">\n" +
            "                                       rlist content\n" +
            "                                    </ul>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"32e77aae-6ffb-4805-a758-28943a51646b\" class=\"page__content\">\n" +
            "                <div class=\"container\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div>\n" +
            "                            <div class=\"container\">\n" +
            "                                <div class=\"row\">\n" +
            "                                    <div></div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <article data-figures=\"https://library.seg.org/action/ajaxShowFigures?doi=10.1190%2FINT-2017-1213-SPSEINTRO.1&amp;ajax=true\" data-references=\"https://library.seg.org/action/ajaxShowEnhancedAbstract?doi=10.1190%2FINT-2017-1213-SPSEINTRO.1&amp;ajax=true\" data-enable-mathjax=\"true\" class=\"container\">\n" +
            "                                article content\n" +
            "                            </article>\n" +
            "                            <script>var articleRef = document.querySelector('.article__body:not(.show-references) .article__references');\n" +
            "                              if (articleRef) { articleRef.style.display = \"none\"; }\n" +
            "\n" +
            "                           </script>\n" +
            "                            <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
            "                                figure-viewer content\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </main>\n" +
            "        <div>\n" +
            "            <footer class=\"footer\">\n" +
            "                footer content\n" +
            "            </footer>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<script type=\"text/javascript\" src=\"/wro/product.js\"></script>\n" +
            "</body>\n" +
            "</html>";

    private static final String articleHtmlKept = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" class=\"pb-page\"  data-request-id=\"655bf1af-6002-4272-9f3d-40550e726c29\">\n" +
            "\n" +
            "<body class=\"pb-ui\">\n" +
            "\n" +
            "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
            "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
            "        <div>\n" +
            "            <link rel=\"stylesheet\" type=\"text/css\" href=\"/pb-assets/styles/journal-branding/journal-brnading-inteio-1548001506880.css\">\n" +
            "            \n" +
            "            \n" +
            "            \n" +
            "            \n" +
            "            \n" +
            "            \n" +
            "        </div>\n" +
            "        \n" +
            "        <main class=\"content journal-branding article-page\">\n" +
            "            \n" +
            "            <div class=\"container\">\n" +
            "                <div class=\"row\">\n" +
            "                    \n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"32e77aae-6ffb-4805-a758-28943a51646b\" class=\"page__content\">\n" +
            "                <div class=\"container\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div>\n" +
            "                            <div class=\"container\">\n" +
            "                                <div class=\"row\">\n" +
            "                                    <div></div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <article data-figures=\"https://library.seg.org/action/ajaxShowFigures?doi=10.1190%2FINT-2017-1213-SPSEINTRO.1&amp;ajax=true\" data-references=\"https://library.seg.org/action/ajaxShowEnhancedAbstract?doi=10.1190%2FINT-2017-1213-SPSEINTRO.1&amp;ajax=true\" data-enable-mathjax=\"true\" class=\"container\">\n" +
            "                                article content\n" +
            "                            </article>\n" +
            "                            \n" +
            "                            <div id=\"figure-viewer\" data-ux3-wrapper=\"figure-viewer\" data-ux3-transformed-by=\"figureInit\" data-ux3-role=\"parent\" role=\"dialog\" class=\"figure-viewer\">\n" +
            "                                figure-viewer content\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </main>\n" +
            "        <div>\n" +
            "            \n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "\n" +
            "</body>\n" +
            "</html>";


    public void testArticle() throws Exception {
        InputStream actIn = fact.createFilteredInputStream(mau,
                new StringInputStream(articleHtml),
                Constants.DEFAULT_ENCODING);

        String filteredContent = StringUtil.fromInputStream(actIn);
        assertEquals(articleHtmlKept, filteredContent);
    }

    public void testToc() throws Exception {
        InputStream actIn = fact.createFilteredInputStream(mau,
                new StringInputStream(tocHtml),
                Constants.DEFAULT_ENCODING);

        String filteredContent = StringUtil.fromInputStream(actIn);
        
        assertEquals(tocHtmlKept, filteredContent);
    }
    
}

