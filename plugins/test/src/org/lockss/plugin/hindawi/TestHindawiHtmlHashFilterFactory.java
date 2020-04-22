package org.lockss.plugin.hindawi;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.plugin.hindawi.Hindawi2020HtmlHashFilterFactory;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;
import java.util.Properties;

public class TestHindawiHtmlHashFilterFactory extends LockssTestCase {
    private Hindawi2020HtmlHashFilterFactory fact;

    private ArchivalUnit mau;

    public void setUp() throws Exception {
        super.setUp();
        fact = new Hindawi2020HtmlHashFilterFactory();
        //the hash filter (for backIssues.asp) requires au information
        Properties props = new Properties();
        props.setProperty(ConfigParamDescr.YEAR.getKey(), "2019");
        props.setProperty(ConfigParamDescr.BASE_URL.getKey(), "http://www.foo.com/");
        props.setProperty("download_url", "http://download.foo.com/");
        props.setProperty(ConfigParamDescr.JOURNAL_ID.getKey(), "aag");
        Configuration config = ConfigurationUtil.fromProps(props);

        DefinablePlugin ap = new DefinablePlugin();
        ap.initPlugin(getMockLockssDaemon(),
                "org.lockss.plugin.hindawi.ClockssHindawiPlugin");
        mau = (DefinableArchivalUnit) ap.createAu(config);
    }


    private static final String tocHtml = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/js-polyfills/0.1.42/polyfill.min.js\"></script>\n" +
            "    <meta charSet=\"utf-8\" class=\"next-head\"/>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" class=\"next-head\"/>\n" +
            "    <title class=\"next-head\">Table of Contents 2019 - Page 1 | Advances in Agriculture | Hindawi</title>\n" +
            "    <link rel=\"canonical\" href=\"https://www.hindawi.com/journals/aag/contents/year/2019/page/1/\" class=\"next-head\"/>\n" +
            "    <meta name=\"title\" content=\"Table of Contents 2019 - Page 1 | Advances in Agriculture | Hindawi\"/>\n" +
            "    <meta name=\"google-site-verification\" content=\"AxEuDsL7vXGOxRe53-uFhOk2ODN0bbXMeuBy6Pfq4ww\"/>\n" +
            "    <script src=\"https://cdn.cookielaw.org/scripttemplates/otSDKStub.js\" type=\"text/javascript\" charset=\"UTF-8\" data-domain-script=\"790048b6-f010-4ac2-b5f6-108356336b09\"></script><script class=\"next-head\">\n" +
            "         (function(w,d,s,l,i){w[l]=w[l]||[];\n" +
            "             w[l].push({'gtm.start': new Date().getTime(),event:'gtm.js', });\n" +
            "             var f=d.getElementsByTagName(s)[0],j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';\n" +
            "             j.async=true;j.src='//www.googletagmanager.com/gtm.js?id='+i+dl\n" +
            "             ;\n" +
            "             f.parentNode.insertBefore(j,f);\n" +
            "         })(window,document,'script','dataLayer','GTM-MQ4MGW');\n" +
            "      </script>\n" +
            "    <link rel=\"preload\" href=\"https://static-01.hindawi.com/next_assets/8pDo_EUBfsG8TZCcaMl31/_next/static/8pDo_EUBfsG8TZCcaMl31/pages/journal_toc.js\" as=\"script\"/>\n" +
            "    <style data-styled=\"elLjbJ gwWYzB gJlPsM fIztWN\" data-styled-version=\"4.4.1\">\n" +
            "         .hgCZVo{font-size:30px;font-weight:500;line-height:30px;margin:26px 20px 16px;} @media (min-width:767.5px){.hgCZVo{margin:30px 40px 20px;font-size:40px;}} @media (min-width:991.5px){.hgCZVo{display:none;}}\n" +
            "      </style>\n" +
            "</head>\n" +
            "<body style=\"margin:0;box-sizing:border-box\">\n" +
            "<div id=\"__next\">\n" +
            "    <div class=\"table_of_content\">\n" +
            "        <div class=\"page no__focus__outline\">\n" +
            "            <header class=\"sc-VigVT ftGovq\">\n" +
            "                header content\n" +
            "            </header>\n" +
            "            <div class=\"sc-bZQynM bNmdOy\"></div>\n" +
            "            <main class=\"baseContent__Main-sc-134g3di-2 cbInhs  article__BaseStyle-sc-10qhnp8-0 gnduKR DADtd11SJ4KyWZ2l6zAnr base_style journal_toc__ArticleBaseStyle-sc-596j7x-2 elLjbJ\">\n" +
            "                <div class=\"sc-bZQynM bNmdOy width_wrapper\">\n" +
            "                    <div id=\"journal__navigation\" class=\"sc-hZSUBg cYkyoQ article__StickyStyle-sc-10qhnp8-1 bomGMi\">\n" +
            "                        journal__navigation content\n" +
            "                    </div>\n" +
            "                    <div class=\"sc-jxGEyO fDPtHo breadcrumb__wrapper toc\"><a class=\"sc-htpNat bUhGXt link sc-gacfCG inRqQX breadCrumb\" href=\"/journals/aag/\" aria-label=\"Advances in Agriculture\">Advances in Agriculture</a> / <span class=\"sc-dEfkYy cxVvOc\">Table of Contents: 2019</span></div>\n" +
            "                    <div class=\"threeColumn__ThreeColumnWrapper-sc-1bf4078-0 uVYyF threeColumnWrapper\">\n" +
            "                        <div class=\"threeColumn__LeftWrapper-sc-1bf4078-1 fkVFlF leftWrapper\">\n" +
            "                            <h2 class=\"journal_toc__TitleMobile-sc-596j7x-3 hgCZVo toc_header_mobile\">Table of Contents</h2>\n" +
            "                            <div class=\"sc-bvTASY fyJFKZ dropdown \">\n" +
            "                                <button class=\"sc-koErNt khiuqG\">2019<span color=\"#6BA439\" class=\"sc-sPYgB jsurxB\"></span></button>\n" +
            "                                <ul id=\"scrollable_area\" height=\"200\" class=\"sc-gJqsIT cuxsDM\">\n" +
            "\n" +
            "                                </ul>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div class=\"threeColumn__ContentWrapper-sc-1bf4078-3 bbzREB contentWrapper\">\n" +
            "                            <div class=\"threeColumn__Content-sc-1bf4078-4 gHfTpg content\">\n" +
            "                                <div class=\"journal_toc__TocWrapper-sc-596j7x-0 jZHyjO toc_wrapper\">\n" +
            "                                    <div class=\"journal_toc__ArticleCardWrapper-sc-596j7x-1 mqRoF article_card_wrapper\">\n" +
            "                                        <h1 class=\"toc_header_label\">\n" +
            "                                            Table of Contents<!-- -->: 2019\n" +
            "                                        </h1>\n" +
            "                                        <div class=\"sc-eXNvrr ftiKeU\">\n" +
            "                                            <div class=\"sc-eKZiaR izJckz toc_article \">\n" +
            "                                                toc_article content 1\n" +
            "                                            </div>\n" +
            "                                            <div class=\"sc-eKZiaR izJckz toc_article \">\n" +
            "                                                toc_article content 2\n" +
            "                                            </div>\n" +
            "                                            <div class=\"sc-cpmKsF jiXzoz\">\n" +
            "                                                <div class=\"sc-kQsIoO dCUEyh\">\n" +
            "                                                    <div class=\"sc-jnlKLf ezaLbO pagination \">\n" +
            "                                                        pagination content 1\n" +
            "                                                    </div>\n" +
            "                                                </div>\n" +
            "                                                <div class=\"sc-gPzReC jnwztg\">\n" +
            "                                                    <div class=\"sc-jnlKLf ezaLbO pagination \">\n" +
            "                                                        pagination content 2\n" +
            "                                                    </div>\n" +
            "                                                </div>\n" +
            "                                            </div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div class=\"threeColumn__RightWrapper-sc-1bf4078-2 sZeeW rightWrapper\">\n" +
            "                            <div class=\"sc-ciodno eQnADE toolbar_part1\">\n" +
            "                                <div class=\"sc-cmTdod emgWUr sc-gGCbJM dYwvqr journalWrapperDiv\">\n" +
            "                                    toolbar_part1 content\n" +
            "                                </div>\n" +
            "                                <a class=\"sc-htpNat bUhGXt link sc-cbkKFq hCNoKM sc-lcpuFF ifPaNV buttongroup__desktop\" href=\"https://mts.hindawi.com/submit/journals/aag\" aria-label=\"\" target=\"_blank\"><span><img alt=\" \" class=\"sc-EHOje fEOMwM sc-krvtoX byuWaL\" title=\"\" role=\"presentation\" src=\"https://static-01.hindawi.com/next_assets/8pDo_EUBfsG8TZCcaMl31/_next/static/node_modules/hindawi-ui/src/icon/svg/addfile.svg\" height=\"24\"/><span class=\"text\">Submit</span></span><img alt=\" \" class=\"sc-EHOje cPuqxU sc-fYiAbW eBEQa fileArrow\" title=\"\" role=\"presentation\" src=\"https://static-01.hindawi.com/next_assets/8pDo_EUBfsG8TZCcaMl31/_next/static/node_modules/hindawi-ui/src/icon/svg/rightArrowWhite.svg\" height=\"16\"/></a>\n" +
            "                                <div class=\"sc-iuDHTM bLEYZJ toolbar_part2\">\n" +
            "                                    toolbar_part2 content \n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </main>\n" +
            "            <footer class=\"sc-kpOJdX fnXcho\">\n" +
            "                footer \n" +
            "            </footer>\n" +
            "            <div class=\"sc-epGmkI gJlPsM  advWrapper hideBanner\">\n" +
            "                advWrapper content\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <noscript>\n" +
            "        <iframe src=\"//www.googletagmanager.com/ns.html?id=GTM-MQ4MGW\"\n" +
            "                height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe>\n" +
            "    </noscript>\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>";


    private static final String tocHtmlKept = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "\n" +
            "<body style=\"margin:0;box-sizing:border-box\">\n" +
            "<div id=\"__next\">\n" +
            "    <div class=\"table_of_content\">\n" +
            "        <div class=\"page no__focus__outline\">\n" +
            "            \n" +
            "            <div class=\"sc-bZQynM bNmdOy\"></div>\n" +
            "            <main class=\"baseContent__Main-sc-134g3di-2 cbInhs  article__BaseStyle-sc-10qhnp8-0 gnduKR DADtd11SJ4KyWZ2l6zAnr base_style journal_toc__ArticleBaseStyle-sc-596j7x-2 elLjbJ\">\n" +
            "                <div class=\"sc-bZQynM bNmdOy width_wrapper\">\n" +
            "                    \n" +
            "                    <div class=\"sc-jxGEyO fDPtHo breadcrumb__wrapper toc\"><a class=\"sc-htpNat bUhGXt link sc-gacfCG inRqQX breadCrumb\" href=\"/journals/aag/\" aria-label=\"Advances in Agriculture\">Advances in Agriculture</a> / <span class=\"sc-dEfkYy cxVvOc\">Table of Contents: 2019</span></div>\n" +
            "                    <div class=\"threeColumn__ThreeColumnWrapper-sc-1bf4078-0 uVYyF threeColumnWrapper\">\n" +
            "                        <div class=\"threeColumn__LeftWrapper-sc-1bf4078-1 fkVFlF leftWrapper\">\n" +
            "                            <h2 class=\"journal_toc__TitleMobile-sc-596j7x-3 hgCZVo toc_header_mobile\">Table of Contents</h2>\n" +
            "                            <div class=\"sc-bvTASY fyJFKZ dropdown \">\n" +
            "                                <button class=\"sc-koErNt khiuqG\">2019<span color=\"#6BA439\" class=\"sc-sPYgB jsurxB\"></span></button>\n" +
            "                                \n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div class=\"threeColumn__ContentWrapper-sc-1bf4078-3 bbzREB contentWrapper\">\n" +
            "                            <div class=\"threeColumn__Content-sc-1bf4078-4 gHfTpg content\">\n" +
            "                                <div class=\"journal_toc__TocWrapper-sc-596j7x-0 jZHyjO toc_wrapper\">\n" +
            "                                    <div class=\"journal_toc__ArticleCardWrapper-sc-596j7x-1 mqRoF article_card_wrapper\">\n" +
            "                                        <h1 class=\"toc_header_label\">\n" +
            "                                            Table of Contents<!-- -->: 2019\n" +
            "                                        </h1>\n" +
            "                                        <div class=\"sc-eXNvrr ftiKeU\">\n" +
            "                                            <div class=\"sc-eKZiaR izJckz toc_article \">\n" +
            "                                                toc_article content 1\n" +
            "                                            </div>\n" +
            "                                            <div class=\"sc-eKZiaR izJckz toc_article \">\n" +
            "                                                toc_article content 2\n" +
            "                                            </div>\n" +
            "                                            <div class=\"sc-cpmKsF jiXzoz\">\n" +
            "                                                <div class=\"sc-kQsIoO dCUEyh\">\n" +
            "                                                    \n" +
            "                                                </div>\n" +
            "                                                <div class=\"sc-gPzReC jnwztg\">\n" +
            "                                                    \n" +
            "                                                </div>\n" +
            "                                            </div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div class=\"threeColumn__RightWrapper-sc-1bf4078-2 sZeeW rightWrapper\">\n" +
            "                            \n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </main>\n" +
            "            \n" +
            "            <div class=\"sc-epGmkI gJlPsM  advWrapper hideBanner\">\n" +
            "                advWrapper content\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    \n" +
            "</div>\n" +
            "</body>\n" +
            "</html>";


    private static final String articleHtml = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/js-polyfills/0.1.42/polyfill.min.js\"></script>\n" +
            "    <meta charSet=\"utf-8\" class=\"next-head\"/>\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" class=\"next-head\"/>\n" +
            "    <title class=\"next-head\">Quantitative Determination of Cadmium (Cd) in Soil-Plant System in Potato Cropping (Solanum tuberosum var. Huayro)</title>\n" +
            "    <link rel=\"canonical\" href=\"https://www.hindawi.com/journals/aag/2019/9862543/\" class=\"next-head\"/>\n" +
            "    <meta name=\"title\" content=\"Quantitative Determination of Cadmium (Cd) in Soil-Plant System in Potato Cropping (Solanum tuberosum var. Huayro)\"/>\n" +
            "    <script src=\"https://static-01.hindawi.com/next_assets/8pDo_EUBfsG8TZCcaMl31/static/lib/jquery.js\" class=\"next-head\"></script>\n" +
            "    <script class=\"next-head\">\n" +
            "         (function(w,d,s,l,i){w[l]=w[l]||[];\n" +
            "             w[l].push({'gtm.start': new Date().getTime(),event:'gtm.js', });\n" +
            "             var f=d.getElementsByTagName(s)[0],j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';\n" +
            "             j.async=true;j.src='//www.googletagmanager.com/gtm.js?id='+i+dl\n" +
            "             ;\n" +
            "             f.parentNode.insertBefore(j,f);\n" +
            "         })(window,document,'script','dataLayer','GTM-MQ4MGW');\n" +
            "      </script>\n" +
            "    <link rel=\"preload\" href=\"https://static-01.hindawi.com/next_assets/8pDo_EUBfsG8TZCcaMl31/_next/static/8pDo_EUBfsG8TZCcaMl31/pages/journal_article.js\" as=\"script\"/>\n" +
            "    <link rel=\"stylesheet\" href=\"https://static-01.hindawi.com/next_assets/8pDo_EUBfsG8TZCcaMl31/_next/static/css/common.ba477f16.chunk.css\"/>\n" +
            "    <style data-styled=\"grtaJI\" data-styled-version=\"4.4.1\">\n" +
            "         .EwsTm{height:200vh;width:100%;position:fixed;z-index:1;background:rgba(255,255,255,0.8);}\n" +
            "      </style>\n" +
            "</head>\n" +
            "<body style=\"margin:0;box-sizing:border-box\">\n" +
            "<div id=\"__next\">\n" +
            "    <div class=\"static_page\">\n" +
            "        <div class=\"page no__focus__outline\">\n" +
            "            <header class=\"sc-VigVT ftGovq\">\n" +
            "                header section\n" +
            "            </header>\n" +
            "            <div class=\"sc-bZQynM bNmdOy\"></div>\n" +
            "            <main class=\"baseContent__Main-sc-134g3di-2 cbInhs  article__BaseStyle-sc-10qhnp8-0\">\n" +
            "                <div class=\"sc-bZQynM bNmdOy width_wrapper\">\n" +
            "                    <div id=\"journal__navigation\" class=\"sc-hZSUBg cYkyoQ article__StickyStyle-sc-10qhnp8-1 bomGMi\">\n" +
            "                        journal__navigation content\n" +
            "                    </div>\n" +
            "                    <div class=\"sc-jxGEyO fDPtHo breadcrumb__wrapper journal_article__BreadCrumbStyle-sc-12fk6zy-3 eztxCB article\">\n" +
            "                        breadcrumb__wrapper content\n" +
            "                    </div>\n" +
            "                    <div class=\"threeColumn__ThreeColumnWrapper-sc-1bf4078-0 uVYyF threeColumnWrapper\">\n" +
            "                        <div class=\"threeColumn__LeftWrapper-sc-1bf4078-1 fkVFlF leftWrapper\">\n" +
            "                            leftWrapper content\n" +
            "                        </div>\n" +
            "                        <div class=\"threeColumn__ContentWrapper-sc-1bf4078-3 bbzREB contentWrapper\">\n" +
            "                            <div class=\"threeColumn__Content-sc-1bf4078-4 jknsZc content\">\n" +
            "                                <div class=\"articleContent__ArticleContentsWrapper-sc-2yl9jy-0 eSvjPB article_contents\">\n" +
            "                                    <div class=\"articleContent__ArticleContentsSectionsWrapper-sc-2yl9jy-1 jntdRu article_contents__sections_wrapper\">\n" +
            "                                        <div class=\"articleContent__ArticleWrapper-sc-2yl9jy-2 hAzjHP\">\n" +
            "                                            <div class=\"sc-cqCuEk dDomlX article_header_class\">\n" +
            "                                                article_header_class content\n" +
            "                                            </div>\n" +
            "                                            <div class=\"articleContent__EditorWrapper-sc-2yl9jy-3 evMkvU\">\n" +
            "                                                <div class=\"articleContent__TextWrapper-sc-2yl9jy-8 fYrLtk\"><b>Academic Editor: Author Name</div>\n" +
            "                                                <div class=\"articleContent__PublicationTimeLineWrapper-sc-2yl9jy-4 fjviok\">\n" +
            "                                                    <div class=\"articleContent__PublcationCol-sc-2yl9jy-5 hgKieC notPublished\">\n" +
            "                                                        <span class=\"articleContent__PublicationField-sc-2yl9jy-6 cIribm\">Received</span><span class=\"articleContent__PublicationValue-sc-2yl9jy-7 khHUcu\">28 Aug 2019</span>\n" +
            "                                                    </div>\n" +
            "                                                </div>\n" +
            "                                            </div>\n" +
            "                                            <article class=\"sc-kZmsYB kupIuP article_body \">\n" +
            "                                               article content\n" +
            "                                            </article>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div class=\"threeColumn__RightWrapper-sc-1bf4078-2 sZeeW rightWrapper\">\n" +
            "                            <div id=\"rightSectionMenu\" class=\"sc-hZSUBg hwTmaF journal_article__StickyRight-sc-12fk6zy-0 bzVmII\">\n" +
            "                                <div class=\"journal_article__ArticleStickyTopToolbar-sc-12fk6zy-1 iKpUSJ article_sticky_top_toolbar\">\n" +
            "                                    <div class=\"sc-jrIrqw deaPpH article_top_toolbar\">\n" +
            "                                        article_top_toolbar content\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                                <div class=\"journal_article__ArticleStickyBottomToolbar-sc-12fk6zy-2 gvAMNw article_sticky_bottom_toolbar\">\n" +
            "                                    <div class=\"sc-fyjhYU bmCmNg article_bottom_toolbar\">\n" +
            "                                        article_bottom_toolbar content\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </main>\n" +
            "            <footer class=\"sc-kpOJdX fnXcho\">\n" +
            "                footer content\n" +
            "            </footer>\n" +
            "            <div class=\"sc-epGmkI gJlPsM  advWrapper hideBanner\">\n" +
            "                  <span>\n" +
            "                      <p>We are committed to sharing findings related to COVID-19 as quickly and safely as possible.</p>\n" +
            "                  </span>\n" +
            "                <img alt=\" \" class=\"sc-EHOje cPuqxU sc-fCPvlr fIztWN\" title=\"\" role=\"presentation\" src=\"https://static-01.hindawi.com/next_assets/8pDo_EUBfsG8TZCcaMl31/_next/static/node_modules/hindawi-ui/src/icon/svg/crossCircle.svg\" height=\"16\"/>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <noscript>\n" +
            "        <iframe src=\"//www.googletagmanager.com/ns.html?id=GTM-MQ4MGW\"\n" +
            "                height=\"0\" width=\"0\" style=\"display:none;visibility:hidden\"></iframe>\n" +
            "    </noscript>\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>";

    private static final String articleHtmlKept = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "\n" +
            "<body style=\"margin:0;box-sizing:border-box\">\n" +
            "<div id=\"__next\">\n" +
            "    <div class=\"static_page\">\n" +
            "        <div class=\"page no__focus__outline\">\n" +
            "            \n" +
            "            <div class=\"sc-bZQynM bNmdOy\"></div>\n" +
            "            <main class=\"baseContent__Main-sc-134g3di-2 cbInhs  article__BaseStyle-sc-10qhnp8-0\">\n" +
            "                <div class=\"sc-bZQynM bNmdOy width_wrapper\">\n" +
            "                    \n" +
            "                    <div class=\"sc-jxGEyO fDPtHo breadcrumb__wrapper journal_article__BreadCrumbStyle-sc-12fk6zy-3 eztxCB article\">\n" +
            "                        breadcrumb__wrapper content\n" +
            "                    </div>\n" +
            "                    <div class=\"threeColumn__ThreeColumnWrapper-sc-1bf4078-0 uVYyF threeColumnWrapper\">\n" +
            "                        <div class=\"threeColumn__LeftWrapper-sc-1bf4078-1 fkVFlF leftWrapper\">\n" +
            "                            leftWrapper content\n" +
            "                        </div>\n" +
            "                        <div class=\"threeColumn__ContentWrapper-sc-1bf4078-3 bbzREB contentWrapper\">\n" +
            "                            <div class=\"threeColumn__Content-sc-1bf4078-4 jknsZc content\">\n" +
            "                                <div class=\"articleContent__ArticleContentsWrapper-sc-2yl9jy-0 eSvjPB article_contents\">\n" +
            "                                    <div class=\"articleContent__ArticleContentsSectionsWrapper-sc-2yl9jy-1 jntdRu article_contents__sections_wrapper\">\n" +
            "                                        <div class=\"articleContent__ArticleWrapper-sc-2yl9jy-2 hAzjHP\">\n" +
            "                                            <div class=\"sc-cqCuEk dDomlX article_header_class\">\n" +
            "                                                article_header_class content\n" +
            "                                            </div>\n" +
            "                                            <div class=\"articleContent__EditorWrapper-sc-2yl9jy-3 evMkvU\">\n" +
            "                                                <div class=\"articleContent__TextWrapper-sc-2yl9jy-8 fYrLtk\"><b>Academic Editor: Author Name</div>\n" +
            "                                                <div class=\"articleContent__PublicationTimeLineWrapper-sc-2yl9jy-4 fjviok\">\n" +
            "                                                    <div class=\"articleContent__PublcationCol-sc-2yl9jy-5 hgKieC notPublished\">\n" +
            "                                                        <span class=\"articleContent__PublicationField-sc-2yl9jy-6 cIribm\">Received</span><span class=\"articleContent__PublicationValue-sc-2yl9jy-7 khHUcu\">28 Aug 2019</span>\n" +
            "                                                    </div>\n" +
            "                                                </div>\n" +
            "                                            </div>\n" +
            "                                            <article class=\"sc-kZmsYB kupIuP article_body \">\n" +
            "                                               article content\n" +
            "                                            </article>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div class=\"threeColumn__RightWrapper-sc-1bf4078-2 sZeeW rightWrapper\">\n" +
            "                            <div id=\"rightSectionMenu\" class=\"sc-hZSUBg hwTmaF journal_article__StickyRight-sc-12fk6zy-0 bzVmII\">\n" +
            "                                <div class=\"journal_article__ArticleStickyTopToolbar-sc-12fk6zy-1 iKpUSJ article_sticky_top_toolbar\">\n" +
            "                                    <div class=\"sc-jrIrqw deaPpH article_top_toolbar\">\n" +
            "                                        article_top_toolbar content\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                                <div class=\"journal_article__ArticleStickyBottomToolbar-sc-12fk6zy-2 gvAMNw article_sticky_bottom_toolbar\">\n" +
            "                                    <div class=\"sc-fyjhYU bmCmNg article_bottom_toolbar\">\n" +
            "                                        article_bottom_toolbar content\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </main>\n" +
            "            \n" +
            "            <div class=\"sc-epGmkI gJlPsM  advWrapper hideBanner\">\n" +
            "                  <span>\n" +
            "                      <p>We are committed to sharing findings related to COVID-19 as quickly and safely as possible.</p>\n" +
            "                  </span>\n" +
            "                <img alt=\" \" class=\"sc-EHOje cPuqxU sc-fCPvlr fIztWN\" title=\"\" role=\"presentation\" src=\"https://static-01.hindawi.com/next_assets/8pDo_EUBfsG8TZCcaMl31/_next/static/node_modules/hindawi-ui/src/icon/svg/crossCircle.svg\" height=\"16\"/>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    \n" +
            "</div>\n" +
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

