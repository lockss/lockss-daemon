package org.lockss.plugin.silverchair;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.clockss.aiaa.TestAIAAXmlMetadataExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.apache.commons.io.FileUtils;

public class TestSilverchairCommonThemeHtmlHashFilterFactory extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestSilverchairCommonThemeHtmlHashFilterFactory.class);

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
            "\n" +
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
            "                                    \n" +
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

        //System.out.println(a);
        //System.out.println(articlePageFiltered);
        assertEquals(articlePageFiltered, a);
    }


    String jsRawString ="<script>window.jQuery || document.write('<script src=\"//gsw.silverchair-cdn.com/Themes/Silver/app/js/jquery.3.4.1.min.js\" type=\"text/javascript\">\\x3C/script>')</script>";
    String jsRawStringFiltered = "";


    public void testFiltering2() throws Exception {
        InputStream inA;
        String a;

        //log.info("filtered jsString before: " + jsRawString);

        // nothing kept
        inA = fact.createFilteredInputStream(mau, new StringInputStream(jsRawString),
                Constants.DEFAULT_ENCODING);
        a = StringUtil.fromInputStream(inA);
        //log.info("filtered jsString after: " + a);
        assertEquals(jsRawStringFiltered, a);

    }

    //The javascript code from url: https://pubs.geoscienceworld.org/books/book/2082/NEWADVANCES-IN-DEVONIAN-CARBONATES-OUTCROP-ANALOGS
    // pass the test
    String jsRawString1 ="" +
            "<script type=\"text/javascript\">\n" +
            "\n" +
            "        /*******************************************************************************\n" +
            "         * JS here is only being used to assign variables from values in the model\n" +
            "         * logic should be implemented in external JS files, like client.script.js\n" +
            "         *******************************************************************************/\n" +
            "\n" +
            "        var SCM = SCM || {};\n" +
            "        var accessIcons = [{\"id\":114313785,\"icon\":\"icon-availability_unlocked\",\"title\":\"Free\"},{\"id\":114313786,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114313838,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114314128,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114314364,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114314549,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114314643,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114314728,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114314869,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114314879,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114315003,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114315180,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114315261,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114315317,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114315429,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":114315609,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":0,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"}];\n" +
            "        if (SCM.AccessIcons) {\n" +
            "            SCM.AccessIcons = SCM.AccessIcons.concat(accessIcons);\n" +
            "        } else {\n" +
            "            SCM.AccessIcons = accessIcons;\n" +
            "        }\n" +
            "        var accessAttributes =  [{\"id\":114313785,\"availableforpurchase\":false},{\"id\":114313786,\"availableforpurchase\":false},{\"id\":114313838,\"availableforpurchase\":false},{\"id\":114314128,\"availableforpurchase\":false},{\"id\":114314364,\"availableforpurchase\":false},{\"id\":114314549,\"availableforpurchase\":false},{\"id\":114314643,\"availableforpurchase\":false},{\"id\":114314728,\"availableforpurchase\":false},{\"id\":114314869,\"availableforpurchase\":false},{\"id\":114314879,\"availableforpurchase\":false},{\"id\":114315003,\"availableforpurchase\":false},{\"id\":114315180,\"availableforpurchase\":false},{\"id\":114315261,\"availableforpurchase\":false},{\"id\":114315317,\"availableforpurchase\":false},{\"id\":114315429,\"availableforpurchase\":false},{\"id\":114315609,\"availableforpurchase\":false},{\"id\":0,\"availableforpurchase\":false}];\n" +
            "        if (SCM.AccessAttributes) {\n" +
            "            SCM.AccessAttributes = SCM.AccessAttributes.concat(accessAttributes);\n" +
            "        } else {\n" +
            "            SCM.AccessAttributes = accessAttributes;\n" +
            "        }\n" +
            "        \n" +
            "    </script>";
    String jsRawStringFiltered1 = "";

    public void testFilteringScript1() throws Exception {
        InputStream inA;
        String a;

        //log.info("filtered jsString1 before: " + jsRawString1);

        // nothing kept
        inA = fact.createFilteredInputStream(mau, new StringInputStream(jsRawString1),
                Constants.DEFAULT_ENCODING);
        a = StringUtil.fromInputStream(inA);
        //log.info("filtered jsString after: " + a);
        assertEquals(jsRawStringFiltered1, a);

    }

    //The javascript code from url: https://pubs.geoscienceworld.org/gsa/books/monograph/2297/Revising-the-Revisions-James-Hutton-s-Reputation
    // pass the test, from content03 machine
    String jsRawString2 = "" +
            "<script type=\"text/javascript\">\n" +
            "\n" +
            "        /*******************************************************************************\n" +
            "         * JS here is only being used to assign variables from values in the model\n" +
            "         * logic should be implemented in external JS files, like client.script.js\n" +
            "         *******************************************************************************/\n" +
            "\n" +
            "        var SCM = SCM || {};\n" +
            "        var accessIcons = [{\"id\":128386309,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":128386312,\"icon\":\"icon-availability_open\",\"title\":\"Open Access\"},{\"id\":128386315,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":128386318,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":128506320,\"icon\":\"icon-availability_unlocked\",\"title\":\"Free\"},{\"id\":0,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"}];\n" +
            "        if (SCM.AccessIcons) {\n" +
            "            SCM.AccessIcons = SCM.AccessIcons.concat(accessIcons);\n" +
            "        } else {\n" +
            "            SCM.AccessIcons = accessIcons;\n" +
            "        }\n" +
            "        var accessAttributes =  [{\"id\":128386309,\"availableforpurchase\":false},{\"id\":128386312,\"availableforpurchase\":false},{\"id\":128386315,\"availableforpurchase\":false},{\"id\":128386318,\"availableforpurchase\":false},{\"id\":128506320,\"availableforpurchase\":false},{\"id\":0,\"availableforpurchase\":false}];\n" +
            "        if (SCM.AccessAttributes) {\n" +
            "            SCM.AccessAttributes = SCM.AccessAttributes.concat(accessAttributes);\n" +
            "        } else {\n" +
            "            SCM.AccessAttributes = accessAttributes;\n" +
            "        }\n" +
            "        \n" +
            "    </script>";
    String jsRawStringFiltered2 = "";

    public void testFilteringScript2() throws Exception {
        InputStream inA;
        String a;

        //log.info("filtered jsString2 before: " + jsRawString2);

        // nothing kept
        inA = fact.createFilteredInputStream(mau, new StringInputStream(jsRawString2),
                Constants.DEFAULT_ENCODING);
        a = StringUtil.fromInputStream(inA);
        //log.info("filtered jsString2 after: " + a);
        assertEquals(jsRawStringFiltered2, a);

    }

    //The javascript code from url: https://pubs.geoscienceworld.org/gsa/books/monograph/2297/Revising-the-Revisions-James-Hutton-s-Reputation
    // pass the test, from content01 machine

    String jsRawString3 = "" +
            "<script type=\"text/javascript\">\n" +
            "\n" +
            "        /*******************************************************************************\n" +
            "         * JS here is only being used to assign variables from values in the model\n" +
            "         * logic should be implemented in external JS files, like client.script.js\n" +
            "         *******************************************************************************/\n" +
            "\n" +
            "        var SCM = SCM || {};\n" +
            "        var accessIcons = [{\"id\":0,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":128386309,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":128386312,\"icon\":\"icon-availability_open\",\"title\":\"Open Access\"},{\"id\":128386315,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":128386318,\"icon\":\"icon-availability_unlocked\",\"title\":\"Available\"},{\"id\":128506320,\"icon\":\"icon-availability_unlocked\",\"title\":\"Free\"}];\n" +
            "        if (SCM.AccessIcons) {\n" +
            "            SCM.AccessIcons = SCM.AccessIcons.concat(accessIcons);\n" +
            "        } else {\n" +
            "            SCM.AccessIcons = accessIcons;\n" +
            "        }\n" +
            "        var accessAttributes =  [{\"id\":0,\"availableforpurchase\":false},{\"id\":128386309,\"availableforpurchase\":false},{\"id\":128386312,\"availableforpurchase\":false},{\"id\":128386315,\"availableforpurchase\":false},{\"id\":128386318,\"availableforpurchase\":false},{\"id\":128506320,\"availableforpurchase\":false}];\n" +
            "        if (SCM.AccessAttributes) {\n" +
            "            SCM.AccessAttributes = SCM.AccessAttributes.concat(accessAttributes);\n" +
            "        } else {\n" +
            "            SCM.AccessAttributes = accessAttributes;\n" +
            "        }\n" +
            "        \n" +
            "    </script>";
    String jsRawStringFiltered3 = "";

    public void testFilteringScript3() throws Exception {
        InputStream inA;
        String a;

        //log.info("filtered jsString3 before: " + jsRawString3);

        // nothing kept
        inA = fact.createFilteredInputStream(mau, new StringInputStream(jsRawString3),
                Constants.DEFAULT_ENCODING);
        a = StringUtil.fromInputStream(inA);
        //log.info("filtered jsString3 after: " + a);
        assertEquals(jsRawStringFiltered3, a);

    }
}

