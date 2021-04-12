package org.lockss.plugin.atypon.besbjs;

import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

public class TestBESBJSHtmlHashFilterFactory extends LockssTestCase {
    private BESBJHtmlHashFilterFactory fact;

    private ArchivalUnit mau;

    public void setUp() throws Exception {
        super.setUp();
        fact = new BESBJHtmlHashFilterFactory();
        //the hash filter (for backIssues.asp) requires au information
        Properties props = new Properties();
        Configuration conf = ConfigManager.newConfiguration();
        props.setProperty(ConfigParamDescr.BASE_URL.getKey(), "http://www.foo.com/");
        props.setProperty(ConfigParamDescr.JOURNAL_ID.getKey(), "aag");
        props.setProperty(ConfigParamDescr.VOLUME_NAME.getKey(), "99");
        Configuration config = ConfigurationUtil.fromProps(props);

        DefinablePlugin ap = new DefinablePlugin();
        ap.initPlugin(getMockLockssDaemon(),
                "org.lockss.plugin.atypon.besbjs.BESBJSPlugin");
        mau = (DefinableArchivalUnit) ap.createAu(config);
    }


    private static final String html = "<!DOCTYPE html>\n" +
            "<html lang=\"en\"     class=\"pb-page\"  data-request-id=\"89ebada4-2e2f-4138-b6ec-71832b275804\"\n" +
            ">\n" +
            "<head data-pb-dropzone=\"head\">\n" +
            "    header content\n" +
            "</head>\n" +
            "<body class=\"pb-ui\">\n" +
            "<div id=\"pb-page-content\" data-ng-non-bindable>\n" +
            "    <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">\n" +
            "        <div class=\"header base fixed\" data-db-parent-of=\"sb1\">\n" +
            "            <header class=\"bj360-journal\">\n" +
            "                header content\n" +
            "            </header>\n" +
            "        </div>\n" +
            "        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"1ffd8995-c3b2-4c5a-8d1f-a6108a2903de\" class=\"bj360-journal\">\n" +
            "            <main class=\"content\">\n" +
            "                <div class=\"top-image\"></div>\n" +
            "                <div class=\"container will-stick\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\"card card--shadow card--gutter meta__body journal-banner\">\n" +
            "                            <div class=\"meta__left-side left-side-image\">\n" +
            "                                <a href=\"/journal/bj360\"><img src=\"/na101/home/literatum/publisher/bandj/journals/covergifs/bj360/2020/cover.jpg\" alt=\"Bone &amp; Joint 360 cover\"></a>\n" +
            "                                <h1 class=\"meta__title\"> <a href=\"/journal/bj360\">Bone & Joint 360</a></h1>\n" +
            "                                <div class=\"meta__info\">\n" +
            "                                    meta_info content\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <div class=\"meta__right-side\">\n" +
            "                                <a href=\"/bj360/information-for-subscribers\" class=\"btn btn--inverse btn--journal-meta\">Subscribe</a>\n" +
            "                                <a href=\"/action/doUpdateAlertSettings?action=addJournal&amp;journalCode=bj360&amp;referrer=/toc/bj360/9/1\" class=\"btn btn--inverse btn--journal-meta\">E-Alerts</a>\n" +
            "                                <p style=\"margin-top:5px;\">\n" +
            "                                    <a href=\"https://twitter.com/BoneJoint360\">\n" +
            "                                        <img src=\"/pb-assets/images/social-icons/twitter-1596015670197.png\" alt=\"\" title=\"Visit BJ360's Twitter page\" height=\"36\" width=\"36\">\n" +
            "                                    </a>\n" +
            "                                    <a href=\"https://podcasts.apple.com/us/podcast/bj360-podcasts/id1534282007\">\n" +
            "                                        <img src=\"/pb-assets/images/social-icons/Apple_Podcast_Icon-1602682575897.png\" alt=\"\" title=\"Visit BJ360 Podcasts\" height=\"36\" width=\"36\">\n" +
            "                                    </a>\n" +
            "                                </p>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div data-widget-id=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6\" class=\"loi-wrapper scrollThenFix\">\n" +
            "                            <div class=\"loi__banner coolBar stickybar\">\n" +
            "                                <div class=\"coolBar__wrapper stickybar__wrapper\">\n" +
            "                                    <a href=\"#\" data-slide-target=\"#loi-banner\" class=\"hidden-md hidden-lg loi-slide__ctrl w-slide__btn\"><i class=\"icon-toc\"></i><span>Journal</span></a>\n" +
            "                                    <div class=\"loi__banner-list\">\n" +
            "                                        <div id=\"loi-banner\" class=\"hidden-sm hidden-xs pull-left\"><a href=\"/toc/bj360/current\" class=\"loi__currentIssue\">Current Issue</a><a href=\"#\" data-db-target-for=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6__content\" data-slide-target=\"#59bcc2e2-d0d9-48dd-a226-4444726d59e6__content\" data-slide-clone=\"self\" class=\"loi__archive w-slide__btn loi__dropBlock\">Archive</a></div>\n" +
            "                                        <div class=\"loi__banner__right-side pull-right\">\n" +
            "                                            <div class=\"coolBar__section coolBar-tools pub-pages-about\">\n" +
            "                                                <a href=\"/personalize/addFavoritePublication?doi=10.1302/bj360&amp;publicationCode=bj360\" title=\"Add to Favorites\"><i class=\"icon-favorites\"></i></a>    <a href=\"#\" data-db-target-for=\"pub_pages_about\" data-db-switch=\"icon-close_thin\" aria-haspopup=\"true\" aria-controls=\"pub_pages_about_Pop\" role=\"button\" id=\"pub_pages_about_Ctrl\" data-slide-target=\"#pub_pages_about_Pop\" class=\"about__ctrl w-slide__btn\" target=\"_self\"><i aria-hidden=\"true\" class=\"icon-info\"></i> <span class=\"about_menu\">About</span></a>\n" +
            "                                                <div data-db-target-of=\"pub_pages_about\" aria-labelledby=\"pub_pages_about_Ctrl\" role=\"menu\" id=\"pub_pages_about_Pop\" class=\"about__block fixed\">\n" +
            "                                                    <ul class=\"rlist w-slide--list\">\n" +
            "                                                        rlist content\n" +
            "                                                    </ul>\n" +
            "                                                </div>\n" +
            "                                            </div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                    <div data-db-target-of=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6__content\" id=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6__content\" class=\"dropBlock__holder dropBlock-loi__holder\">\n" +
            "                                        <div data-db-parent-of=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6\" data-ajax=\"ajax\" data-widget-id=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6\" data-order=\"descendingOrder\" class=\"loi-wrapper list-of-issues-detailed\">\n" +
            "                                            <div class=\"loi__banner\">\n" +
            "                                                <div class=\"loi__banner-list\">\n" +
            "                                                    loi__banner-list content\n" +
            "                                                </div>\n" +
            "                                            </div>\n" +
            "                                        </div>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div class=\"container\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\" col-xs-12\">\n" +
            "                            <h3 class=\"sep citation section__header border-bottom\">Volume 9,&nbsp;Issue 1&nbsp;/&nbsp;February 2020</h3>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                <div class=\"container\">\n" +
            "                    <div class=\"row\">\n" +
            "                        <div class=\" col-md-8\">\n" +
            "                            <a href=\"/doi/reader/10.1302/bj360.2020.9.issue-1\" class=\"full_view\">Full issue view</a><!--totalCount16--><!--modified:1617125832189-->\n" +
            "                            <div class=\"table-of-content\">\n" +
            "                                <h4 class=\"toc__heading section__header to-section\" id=\"d41732e62\">Cochrane Corner</h4>\n" +
            "                                <div class=\"issue-item\">\n" +
            "                                    <div class=\"badges\"><span class=\"badge-type badge-fa\">Free</span></div>\n" +
            "                                    <h5 class=\"issue-item__title\"><a href=\"/doi/10.1302/2048-0105.91.360743\" title=\"Cochrane Corner\">Cochrane Corner</a></h5>\n" +
            "                                    <ul class=\"rlist--inline loa\" aria-label=\"author\">\n" +
            "                                        <li>A. Das</li>\n" +
            "                                    </ul>\n" +
            "                                    <ul class=\"rlist--inline separator toc-item__detail\">\n" +
            "                                    </ul>\n" +
            "                                    <div class=\"toc-item__footer\">\n" +
            "                                        <ul class=\"rlist--inline separator toc-item__detail\">\n" +
            "                                            <li><a title=\"Abstract\" href=\"/doi/fpi/10.1302/2048-0105.91.360743\"><span>First Page</span><i class=\"icon icon-abstract\"></i></a></li>\n" +
            "                                            <li><a title=\"Full text\" href=\"/doi/full/10.1302/2048-0105.91.360743\"><span>Full text</span><i class=\"icon icon-full-text\"></i></a></li>\n" +
            "                                            <li><a title=\"PDF/EPUB\" href=\"/doi/reader/10.1302/2048-0105.91.360743\"><span>PDF/EPUB</span><i class=\"icon icon-file-pdf\"></i></a></li>\n" +
            "                                        </ul>\n" +
            "                                    </div>\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                        </div>\n" +
            "                        <div class=\" col-md-4\">\n" +
            "                            <div data-widget-def=\"literatumAd\" data-widget-id=\"d21c7d76-590c-41b4-af29-208bd1b52506\" class=\"side-tower visible-lg\">\n" +
            "                            </div>\n" +
            "                            <a class=\"twitter-timeline\" data-height=\"600\" data-theme=\"light\" href=\"https://twitter.com/BoneJoint360\">Tweets by BoneJoint360</a> <script async src=\"//platform.twitter.com/widgets.js\" charset=\"utf-8\"></script>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </main>\n" +
            "        </div>\n" +
            "        <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"5795d127-63d9-4ab4-81d8-2e7cbcb371a7\" class=\"bj360-journal\">\n" +
            "            <footer>\n" +
            "                footer content\n" +
            "            </footer>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>";


    private static final String htmlKept = " <!DOCTYPE html> <html lang=\"en\" class=\"pb-page\" > <body class=\"pb-ui\"> <div id=\"pb-page-content\" data-ng-non-bindable> <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\"> <div class=\"header base fixed\" data-db-parent-of=\"sb1\"> </div> <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"1ffd8995-c3b2-4c5a-8d1f-a6108a2903de\" class=\"bj360-journal\"> <main class=\"content\"> <div class=\"top-image\"> </div> <div class=\"container will-stick\"> <div class=\"row\"> <div class=\"card card--shadow card--gutter meta__body journal-banner\"> <div class=\"meta__left-side left-side-image\"> <a href=\"/journal/bj360\"> <img src=\"/na101/home/literatum/publisher/bandj/journals/covergifs/bj360/2020/cover.jpg\" alt=\"Bone &amp; Joint 360 cover\"> </a> <h1 class=\"meta__title\"> <a href=\"/journal/bj360\">Bone & Joint 360 </a> </h1> <div class=\"meta__info\"> meta_info content </div> </div> <div class=\"meta__right-side\"> <a href=\"/bj360/information-for-subscribers\" class=\"btn btn--inverse btn--journal-meta\">Subscribe </a> <a href=\"/action/doUpdateAlertSettings?action=addJournal&amp;journalCode=bj360&amp;referrer=/toc/bj360/9/1\" class=\"btn btn--inverse btn--journal-meta\">E-Alerts </a> <p style=\"margin-top:5px;\"> <a href=\"https://twitter.com/BoneJoint360\"> <img src=\"/pb-assets/images/social-icons/twitter-1596015670197.png\" alt=\"\" title=\"Visit BJ360's Twitter page\" height=\"36\" width=\"36\"> </a> <a href=\"https://podcasts.apple.com/us/podcast/bj360-podcasts/id1534282007\"> <img src=\"/pb-assets/images/social-icons/Apple_Podcast_Icon-1602682575897.png\" alt=\"\" title=\"Visit BJ360 Podcasts\" height=\"36\" width=\"36\"> </a> </p> </div> </div> <div data-widget-id=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6\" class=\"loi-wrapper scrollThenFix\"> <div class=\"loi__banner coolBar stickybar\"> <div class=\"coolBar__wrapper stickybar__wrapper\"> <a href=\"#\" data-slide-target=\"#loi-banner\" class=\"hidden-md hidden-lg loi-slide__ctrl w-slide__btn\"> <i class=\"icon-toc\"> </i> <span>Journal </span> </a> <div class=\"loi__banner-list\"> <div id=\"loi-banner\" class=\"hidden-sm hidden-xs pull-left\"> <a href=\"/toc/bj360/current\" class=\"loi__currentIssue\">Current Issue </a> <a href=\"#\" data-db-target-for=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6__content\" data-slide-target=\"#59bcc2e2-d0d9-48dd-a226-4444726d59e6__content\" data-slide-clone=\"self\" class=\"loi__archive w-slide__btn loi__dropBlock\">Archive </a> </div> <div class=\"loi__banner__right-side pull-right\"> <div class=\"coolBar__section coolBar-tools pub-pages-about\"> <a href=\"/personalize/addFavoritePublication?doi=10.1302/bj360&amp;publicationCode=bj360\" title=\"Add to Favorites\"> <i class=\"icon-favorites\"> </i> </a> <a href=\"#\" data-db-target-for=\"pub_pages_about\" data-db-switch=\"icon-close_thin\" aria-haspopup=\"true\" aria-controls=\"pub_pages_about_Pop\" role=\"button\" id=\"pub_pages_about_Ctrl\" data-slide-target=\"#pub_pages_about_Pop\" class=\"about__ctrl w-slide__btn\" target=\"_self\"> <i aria-hidden=\"true\" class=\"icon-info\"> </i> <span class=\"about_menu\">About </span> </a> <div data-db-target-of=\"pub_pages_about\" aria-labelledby=\"pub_pages_about_Ctrl\" role=\"menu\" id=\"pub_pages_about_Pop\" class=\"about__block fixed\"> <ul class=\"rlist w-slide--list\"> rlist content </ul> </div> </div> </div> </div> <div data-db-target-of=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6__content\" id=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6__content\" class=\"dropBlock__holder dropBlock-loi__holder\"> <div data-db-parent-of=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6\" data-ajax=\"ajax\" data-widget-id=\"59bcc2e2-d0d9-48dd-a226-4444726d59e6\" data-order=\"descendingOrder\" class=\"loi-wrapper list-of-issues-detailed\"> <div class=\"loi__banner\"> <div class=\"loi__banner-list\"> loi__banner-list content </div> </div> </div> </div> </div> </div> </div> </div> </div> <div class=\"container\"> <div class=\"row\"> <div class=\" col-xs-12\"> <h3 class=\"sep citation section__header border-bottom\">Volume 9, Issue 1 / February 2020 </h3> </div> </div> </div> <div class=\"container\"> <div class=\"row\"> <div class=\" col-md-8\"> <a href=\"/doi/reader/10.1302/bj360.2020.9.issue-1\" class=\"full_view\">Full issue view </a> <div class=\"table-of-content\"> <h4 class=\"toc__heading section__header to-section\" id=\"d41732e62\">Cochrane Corner </h4> <div class=\"issue-item\"> <div class=\"badges\"> <span class=\"badge-type badge-fa\">Free </span> </div> <h5 class=\"issue-item__title\"> <a href=\"/doi/10.1302/2048-0105.91.360743\" title=\"Cochrane Corner\">Cochrane Corner </a> </h5> <ul class=\"rlist--inline loa\" aria-label=\"author\"> <li>A. Das </li> </ul> <ul class=\"rlist--inline separator toc-item__detail\"> </ul> </div> </div> </div> <div class=\" col-md-4\"> <div data-widget-def=\"literatumAd\" data-widget-id=\"d21c7d76-590c-41b4-af29-208bd1b52506\" class=\"side-tower visible-lg\"> </div> </div> </div> </div> </main> </div> <div data-widget-def=\"ux3-layout-widget\" data-widget-id=\"5795d127-63d9-4ab4-81d8-2e7cbcb371a7\" class=\"bj360-journal\"> </div> </div> </div> </body> </html>";

    public void testToc() throws Exception {
        InputStream actIn = fact.createFilteredInputStream(mau,
                new StringInputStream(html),
                Constants.DEFAULT_ENCODING);

        String filteredContent = StringUtil.fromInputStream(actIn);
        
        assertEquals(htmlKept, filteredContent);
    }
    
}

