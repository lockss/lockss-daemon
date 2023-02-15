package org.lockss.plugin.silverchair;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.InputStream;

public class TestSilverchairCommonThemeHtmlCrawlFilterFactory extends LockssTestCase {
    static String ENC = Constants.DEFAULT_ENCODING;

    private SilverchairCommonThemeHtmlCrawlFilterFactory fact;
    private MockArchivalUnit mau;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        fact = new SilverchairCommonThemeHtmlCrawlFilterFactory();
        mau = new MockArchivalUnit();
    }

    private static final String empty = "";
    private static final String bad_doi_href =
        "<a class=\"link\" href=\"10.1029/2011TC002981\" target=\"_blank\">10.1029/2011TC002981</a>";

    private static final String pagefooter =
        "<div class=\"widget widget-SitePageFooter widget-instance-SitePageFooter\">\n" +
        "            <div class=\"journal-footer journal-bg\">\n" +
        "        <div class=\"journal-footer_content clearfix foot-left\">\n" +
        "    <div class=\"journal-footer-affiliations aff-left\">\n" +
        "        <!-- <h3>Affiliations</h3> -->\n" +
        "            <a href=\"https://geobookstore.uwyo.edu/rmg\" target=\"_blank\">\n" +
        "                <img id=\"footer-logo-UniversityofWyominglogo\" class=\"journal-footer-affiliations-logo\" src=\"https://pubs.geoscienceworld.org/data/SiteBuilderAssets/Live/Images/rmg/UWyo_logo_white-120062604.png\" alt=\"University of Wyoming logo\">\n" +
        "            </a>\n" +
        "    </div>\n" +
        "    <div class=\"journal-footer-menu\">\n" +
        "            <ul>\n" +
        "                <li class=\"link-0\">\n" +
        "                    <a href=\"/rmg/list-of-years\">Archive</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-1\">\n" +
        "                    <a href=\"/rmg/issue/current\">Current Issue</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-2\">\n" +
        "                    <a href=\"/rmg/pages/themed-issues\">Themed Issues</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-3\">\n" +
        "                    <a href=\"/rmg/pages/special-issues\">Special Issues</a>\n" +
        "                </li>\n" +
        "            </ul>\n" +
        "            <ul>\n" +
        "                <li class=\"link-0\">\n" +
        "                    <a href=\"http://geobookstore.uwyo.edu/rmg\" target=\"_blank\">About the Journal</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-1\">\n" +
        "                    <a href=\"http://geobookstore.uwyo.edu/rmg/submissions\" target=\"_blank\">Submit an Article</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-2\">\n" +
        "                    <a href=\"http://geobookstore.uwyo.edu/rmg/editors\" target=\"_blank\">Editorial Board</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-3\">\n" +
        "                    <a href=\"https://geobookstore.uwyo.edu/rmg/open-access\" target=\"_blank\">Open Access Policy</a>\n" +
        "                </li>\n" +
        "            </ul>\n" +
        "            <ul>\n" +
        "                <li class=\"link-0\">\n" +
        "                    <a href=\"http://www.uwyo.edu/geolgeophys/about/\" target=\"_blank\">About the Publisher</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-1\">\n" +
        "                    <a href=\"http://geobookstore.uwyo.edu/\" target=\"_blank\">Publisher Bookstore</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-2\">\n" +
        "                    <a href=\"http://www.uwyo.edu/geolgeophys/\" target=\"_blank\">Publisher Homepage</a>\n" +
        "                </li>\n" +
        "                <li class=\"link-3\">\n" +
        "                    <a href=\"http://geobookstore.uwyo.edu/contact\" target=\"_blank\">Contact the Publisher</a>\n" +
        "                </li>\n" +
        "            </ul>\n" +
        "            <ul>\n" +
        "                <li class=\"link-0\">\n" +
        "                    <a href=\"https://geobookstore.uwyo.edu/rmg/open-access\" target=\"_blank\">Open Access Policy</a>\n" +
        "                </li>\n" +
        "            </ul>\n" +
        "        \n" +
        "    </div>\n" +
        "        </div>\n" +
        "    <div class=\"journal-footer-colophon\">\n" +
        "        <ul>\n" +
        "                <li><span>Online ISSN</span> 1555-7340</li>\n" +
        "                            <li><span>Print ISSN</span> 1555-7332</li>\n" +
        "                            <li>Copyright © 2022 University of Wyoming</li>\n" +
        "        </ul>\n" +
        "    </div>\n" +
        "    </div>\n" +
        " \n" +
        "    </div>";

    private static final String themefooter =
        "<div class=\"site-theme-footer\">\n" +
        "    <div class=\"widget widget-SelfServeContent widget-instance-UmbrellaFooterSelfServe\">\n" +
        "        \n" +
        "<div class=\"global-footer-link-wrap\">\n" +
        "<div class=\"global-footer-block\">\n" +
        "<ul class=\"global-footer-link-list\">\n" +
        "    <li class=\"list-title\">\n" +
        "    <strong>About GSW</strong>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"https://community.geoscienceworld.org/about/?utm_source=pubs+platform&amp;utm_medium=footer\" target=\"_blank\">Our Story</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"mailto:gswinfo@geoscienceworld.org\">Contact Us</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"/pages/subscribe\">Subscribe</a>\n" +
        "    </li>\n" +
        "</ul>\n" +
        "<ul class=\"global-footer-link-list\">\n" +
        "    <li class=\"list-title\">\n" +
        "    <strong>Resources</strong>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"/pages/information_for_librarians\">For Librarians</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"https://community.geoscienceworld.org/for-industry/?utm_source=pubs+platform&amp;utm_medium=footer\" target=\"_blank\">For Industry</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"/pages/society-members\">For Society Members</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"https://community.geoscienceworld.org/for-societies/?utm_source=pubs+platform&amp;utm_medium=footer\" target=\"_blank\">For Authors</a></li>\n" +
        "    <li>\n" +
        "    <a href=\"/pages/help\">FAQs</a>\n" +
        "    </li>\n" +
        "</ul>\n" +
        "<ul class=\"global-footer-link-list\">\n" +
        "    <li class=\"list-title\">\n" +
        "    <strong>Explore</strong>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"/journals\">Journals</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"/books\">Books</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"/georef\">GeoRef</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"https://pubs.geoscienceworld.org/map-results?page=1&amp;quicknav=1&amp;q=\">OpenGeoSci</a>\n" +
        "    </li>\n" +
        "</ul>\n" +
        "<ul class=\"global-footer-link-list\">\n" +
        "    <li class=\"list-title\">\n" +
        "    <strong>Connect</strong>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"http://www.facebook.com/GeoScienceWorld.org\" target=\"_blank\">Facebook</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"https://www.linkedin.com/company/geoscienceworld\" target=\"_blank\">LinkedIn</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"https://twitter.com/GeoScienceWorld\" target=\"_blank\">Twitter</a>\n" +
        "    </li>\n" +
        "    <li>\n" +
        "    <a href=\"http://www.youtube.com/geoscienceworld\" target=\"_blank\">YouTube</a>\n" +
        "    </li>\n" +
        "</ul>\n" +
        "</div>\n" +
        "<div class=\"global-footer-block global-footer-site-info\">\n" +
        "<a href=\"/\">\n" +
        "<img src=\"/UI/app/svg/umbrella/logo-gsw.svg\" alt=\"GeoScienceWorld\" class=\"site-theme-footer-image\">\n" +
        "</a>\n" +
        "<ul class=\"global-footer-site-details\">\n" +
        "    <li>1750 Tysons Boulevard, Suite 1500</li>\n" +
        "    <li>McLean, Va 22102</li>\n" +
        "    <li>Telephone: 1-800-341-1851</li>\n" +
        "    <li>Copyright © 2022 GeoScienceWorld</li>\n" +
        "</ul>\n" +
        "</div>\n" +
        "</div>\n" +
        "    </div>\n" +
        "            </div>";

    private static final String articlePrefix =
        "<div class=\"article\">" +
        "  <div class=\"widget widget-ArticleMainView widget-instance-ArticleMainView_Split\"> foo foo foo </div>";
    private static final String articlecontent =
        articlePrefix +
        pagefooter +
        themefooter +
        "</div>";
    public void filterAndCompare(String orig, String expected) throws Exception {
        InputStream inA;
        String a;
        inA = fact.createFilteredInputStream(mau, new StringInputStream(orig),
                Constants.DEFAULT_ENCODING);
        a = StringUtil.fromInputStream(inA);
        assertEquals(expected, a);
    }

    public void testBadDoi() throws Exception {
        filterAndCompare(bad_doi_href, empty);
    }

    public void testArticleContent() throws Exception {
        filterAndCompare(articlecontent, articlePrefix + "</div>");
    }

}

