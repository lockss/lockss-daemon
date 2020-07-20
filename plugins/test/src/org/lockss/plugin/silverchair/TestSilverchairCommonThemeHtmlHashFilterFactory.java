package org.lockss.plugin.silverchair;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;
import java.io.InputStream;

public class TestSilverchairCommonThemeHtmlHashFilterFactory extends LockssTestCase {
    static String ENC = Constants.DEFAULT_ENCODING;

    private SilverchairCommonThemeHtmlHashFilterFactory fact;
    private MockArchivalUnit mau;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        fact = new SilverchairCommonThemeHtmlHashFilterFactory();
        mau = new MockArchivalUnit();
    }

    private static final String articlePage =
            "<!DOCTYPE html>\n" +
                    "<html lang=\"en\" class=\"no-js\">\n" +
                    "<body data-sitename=\"journalofexperimentalmedicine\" class=\"off-canvas pg_ArticleSplitView pg_articlesplitview  \" theme-jem data-sitestyletemplate=\"Journal\" >\n" +
                    "<a href=\"#skipNav\" class=\"skipnav\">Skip to Main Content</a>\n" +
                    "<input id=\"hdnSiteID\" name=\"hdnSiteID\" type=\"hidden\" value=\"1000003\" /><input id=\"hdnAdDelaySeconds\" name=\"hdnAdDelaySeconds\" type=\"hidden\" value=\"3000\" /><input id=\"hdnAdConfigurationTop\" name=\"hdnAdConfigurationTop\" type=\"hidden\" value=\"basic\" /><input id=\"hdnAdConfigurationRightRail\" name=\"hdnAdConfigurationRightRail\" type=\"hidden\" value=\"basic\" />\n" +
                    "<div id=\"main\" class=\"splitview__wrapper ui-base\">\n" +
                    "    <section class=\"splitview__main\">\n" +
                    "        <a href=\"#\" id=\"skipNav\" tabindex=\"-1\"></a>\n" +
                    "        <div class=\"widget-SplitView widget-instance-SplitView_Article\">\n" +
                    "            <div class=\"article js-content-splitview\">\n" +
                    "                <div class=\"widget-ArticleMainView widget-instance-ArticleMainView_Split\">\n" +
                    "                    <div class=\"article-browse-top article-browse-mobile-nav empty\">\n" +
                    "                        <div class=\"article-browse-mobile-nav-inner\">\n" +
                    "                            <button class=\"toggle-left-col toggle-left-col__article btn-as-link\">\n" +
                    "                                Article Navigation\n" +
                    "                            </button>\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                    <div class=\"content-inner-wrap\">\n" +
                    "                        <div class=\"widget-ArticleTopInfo widget-instance-ArticleTopInfo_SplitView\">\n" +
                    "                            <div class=\"module-widget article-top-widget content-metadata_wrap\">\n" +
                    "                                module-widget content\n" +
                    "                                <div>\n" +
                    "                        </div>\n" +
                    "                        <div class=\"widget-ArticleLinks widget-instance-ArticleLinks_SplitView\">\n" +
                    "                        </div>\n" +
                    "                        <!-- /.toolbar-wrap -->\n" +
                    "                        <div class=\"article-body\">\n" +
                    "                            <div id=\"ContentTab\" class=\"content active\">\n" +
                    "                                <div class=\"widget-ArticleFulltext widget-instance-ArticleFulltext_SplitView\">\n" +
                    "                                    <input type=\"hidden\" name=\"js-hfArticleLinksReferencesDoiRegex\" id=\"js-hfArticleLinksReferencesDoiRegex\" value=\"\" />\n" +
                    "                                    <div class=\"module-widget\">\n" +
                    "                                        <div class=\"widget-items\" data-widgetname=\"ArticleFulltext\">\n" +
                    "                                            ArticleFulltext content is here\n" +
                    "                                            <div>\n" +
                    "                                        <!-- /.widget-items -->\n" +
                    "                                    </div>\n" +
                    "                                    <!-- /.module-widget -->\n" +
                    "                                </div>\n" +
                    "                                <div class=\"widget-SolrResourceMetadata widget-instance-SolrResourceMetadata_SplitView\">\n" +
                    "                                </div>\n" +
                    "                                <div id=\"ContentTabFilteredView\"></div>\n" +
                    "                                <div class=\"downloadImagesppt\">\n" +
                    "                                    <a id=\"lnkDownloadAllImages\" href=\"//rup.silverchair-cdn.com/DownloadFile/DownloadImage.aspx?image=&amp;PPTtype=SlideSet&amp;ar=42535&amp;xsltPath=~/UI/app/XSLT&amp;siteId=1000003\"></a>\n" +
                    "                                </div>\n" +
                    "                                <div class=\"comments\">\n" +
                    "                                    <div class=\"widget-UserCommentBody widget-instance-UserCommentBody_SplitView\">\n" +
                    "                                    </div>\n" +
                    "                                    <div class=\"widget-UserComment widget-instance-UserComment_SplitView\">\n" +
                    "                                    </div>\n" +
                    "                                </div>\n" +
                    "                            </div>\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "            <div class=\"aside\">\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "        <div class=\"widget-Lockss widget-instance-Article_Lockss\">\n" +
                    "        </div>\n" +
                    "    </section>\n" +
                    "</div>\n" +
                    "<div class=\"mobile-mask\"></div>\n" +
                    "<!-- /.site-theme-footer -->\n" +
                    "</body>\n" +
                    "</html>";

    private static final String articlePageFiltered = "<!DOCTYPE html>\n" +
            "<html lang=\"en\" class=\"no-js\">\n" +
            "<body data-sitename=\"journalofexperimentalmedicine\" class=\"off-canvas pg_ArticleSplitView pg_articlesplitview  \" theme-jem data-sitestyletemplate=\"Journal\" >\n" +
            "<a href=\"#skipNav\" class=\"skipnav\">Skip to Main Content</a>\n" +
            "<input id=\"hdnSiteID\" name=\"hdnSiteID\" type=\"hidden\" value=\"1000003\" /><input id=\"hdnAdDelaySeconds\" name=\"hdnAdDelaySeconds\" type=\"hidden\" value=\"3000\" /><input id=\"hdnAdConfigurationTop\" name=\"hdnAdConfigurationTop\" type=\"hidden\" value=\"basic\" /><input id=\"hdnAdConfigurationRightRail\" name=\"hdnAdConfigurationRightRail\" type=\"hidden\" value=\"basic\" />\n" +
            "<div id=\"main\" class=\"splitview__wrapper ui-base\">\n" +
            "    <section class=\"splitview__main\">\n" +
            "        <a href=\"#\" id=\"skipNav\" tabindex=\"-1\"></a>\n" +
            "        <div class=\"widget-SplitView widget-instance-SplitView_Article\">\n" +
            "            <div class=\"article js-content-splitview\">\n" +
            "                <div class=\"widget-ArticleMainView widget-instance-ArticleMainView_Split\">\n" +
            "                    <div class=\"article-browse-top article-browse-mobile-nav empty\">\n" +
            "                        <div class=\"article-browse-mobile-nav-inner\">\n" +
            "                            <button class=\"toggle-left-col toggle-left-col__article btn-as-link\">\n" +
            "                                Article Navigation\n" +
            "                            </button>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                    <div class=\"content-inner-wrap\">\n" +
            "                        <div class=\"widget-ArticleTopInfo widget-instance-ArticleTopInfo_SplitView\">\n" +
            "                            <div class=\"module-widget article-top-widget content-metadata_wrap\">\n" +
            "                                module-widget content\n" +
            "                                <div>\n" +
            "                        </div>\n" +
            "                        <div class=\"widget-ArticleLinks widget-instance-ArticleLinks_SplitView\">\n" +
            "                        </div>\n" +
            "                        \n" +
            "                        <div class=\"article-body\">\n" +
            "                            <div id=\"ContentTab\" class=\"content active\">\n" +
            "                                <div class=\"widget-ArticleFulltext widget-instance-ArticleFulltext_SplitView\">\n" +
            "                                    <input type=\"hidden\" name=\"js-hfArticleLinksReferencesDoiRegex\" id=\"js-hfArticleLinksReferencesDoiRegex\" value=\"\" />\n" +
            "                                    <div class=\"module-widget\">\n" +
            "                                        <div class=\"widget-items\" data-widgetname=\"ArticleFulltext\">\n" +
            "                                            ArticleFulltext content is here\n" +
            "                                            <div>\n" +
            "                                        \n" +
            "                                    </div>\n" +
            "                                    \n" +
            "                                </div>\n" +
            "                                <div class=\"widget-SolrResourceMetadata widget-instance-SolrResourceMetadata_SplitView\">\n" +
            "                                </div>\n" +
            "                                <div id=\"ContentTabFilteredView\"></div>\n" +
            "                                <div class=\"downloadImagesppt\">\n" +
            "                                    <a id=\"lnkDownloadAllImages\" href=\"//rup.silverchair-cdn.com/DownloadFile/DownloadImage.aspx?image=&amp;PPTtype=SlideSet&amp;ar=42535&amp;xsltPath=~/UI/app/XSLT&amp;siteId=1000003\"></a>\n" +
            "                                </div>\n" +
            "                                <div class=\"comments\">\n" +
            "                                    <div class=\"widget-UserCommentBody widget-instance-UserCommentBody_SplitView\">\n" +
            "                                    </div>\n" +
            "                                    <div class=\"widget-UserComment widget-instance-UserComment_SplitView\">\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div class=\"aside\">\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div class=\"widget-Lockss widget-instance-Article_Lockss\">\n" +
            "        </div>\n" +
            "    </section>\n" +
            "</div>\n" +
            "<div class=\"mobile-mask\"></div>\n" +
            "\n" +
            "</body>\n" +
            "</html>";

    public void testFiltering() throws Exception {
        InputStream inA;
        String a;

        // nothing kept
        inA = fact.createFilteredInputStream(mau, new StringInputStream(articlePage),
                Constants.DEFAULT_ENCODING);
        a = StringUtil.fromInputStream(inA);
        assertEquals(articlePageFiltered, a);

    }

}

