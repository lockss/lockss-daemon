package org.lockss.plugin.silverchair;

import org.lockss.util.Logger;
import java.util.regex.Pattern;


public class SilverchairCommonThemeArticleIteratorFactory extends BaseScArticleIteratorFactory {

    //https://pubs.geoscienceworld.org/gsa/gsabulletin/article/132/1-2/113/570559/Vegetation-and-habitat-change-in-southern
    //https://academic.oup.com/psychsocgerontology/article/66B/1/109/580460

    private static String ROOT_TEMPLATE = "\"%s\", base_url";
    private static String PATTERN_TEMPLATE =  "\"%s([^/]+)?/%s/article(-abstract)?\", base_url, journal_id";

    private static final Pattern HTML_PATTERN = Pattern.compile("/article/([0-9i]+)/(.*)$", Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/article/$1/$2";
    private static final String ABSTRACT_REPLACEMENT = "/article-abstract/$1/$2";
    // <meta name="citation_pdf_url"
    protected static final Pattern PDF_PATTERN = Pattern.compile(
            "<meta[\\s]*name=\"citation_pdf_url\"[\\s]*content=\"(.+/article-pdf/[^.]+\\.pdf)\"", Pattern.CASE_INSENSITIVE);

    protected static Logger getLog() {
        return Logger.getLogger(SilverchairCommonThemeArticleIteratorFactory.class);
    }

    @Override
    protected String getRootTemplate() {
        return ROOT_TEMPLATE;
    }

    @Override
    protected String getPatternTemplate() {
        return PATTERN_TEMPLATE;
    }

    protected static Pattern getHtmlPattern() {
        return HTML_PATTERN;
    }

    protected static String getHtmlReplacement() {
        return HTML_REPLACEMENT;
    }

    protected static String getAbstractReplacement() {
        return ABSTRACT_REPLACEMENT;
    }

    protected static Pattern getPdfPattern() {
        return PDF_PATTERN;
    }
}
