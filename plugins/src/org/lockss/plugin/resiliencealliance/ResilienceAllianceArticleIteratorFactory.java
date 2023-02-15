package org.lockss.plugin.resiliencealliance;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResilienceAllianceArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(ResilienceAllianceArticleIteratorFactory .class);

    /*
    Since we will not be able to guess our the PDF pattern from html pattern, use html as the article count instead
    Article: https://www.ace-eco.org/vol14/iss1/art1
    Article PDF: https://www.ace-eco.org/vol14/iss1/art1/ACE-ECO-2018-1293.pdf
    Other Appendix: https://www.ace-eco.org/vol14/iss1/art1/appendix1.pdf
     */

    protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
    protected static final String PATTERN_TEMPLATE =
            "\"%svol%s/iss[^/]+/art[^/]+\", base_url, volume_name";

    private static final Pattern HTML_PATTERN = Pattern.compile(
        "/(vol[^/]+/iss[^/]+/art[^/]+)$",
        Pattern.CASE_INSENSITIVE);

    private static final String PDF_REGEX = "/(vol[^/]+/iss[^/]+/art[^/]+)/(?!appendix)[^.]+[.]pdf$";
    private static final Pattern PDF_PATTERN = Pattern.compile(
        PDF_REGEX,
        Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/$1";
    private static final String PDF_REPLACEMENT = "/$1/FOO-BAR.pdf";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        if (searchCusForRegex(au, PDF_REGEX)) {
            log.debug3("found pdf pattern");

            builder.addAspect(
                PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

            builder.addAspect(
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_ABSTRACT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);


            builder.setFullTextFromRoles(
                ArticleFiles.ROLE_FULL_TEXT_PDF,
                ArticleFiles.ROLE_FULL_TEXT_HTML);

        } else {
            // if no pdfs found, defaut to html only.
            builder.addAspect(
                HTML_PATTERN,
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_ABSTRACT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);
        }


        return builder.getSubTreeArticleIterator();
    }

    /*
     * Helper function that iterates over the AUs cachedUrl set
     * and searches for the supplied regex in the cached urls.
     */
    protected boolean searchCusForRegex(ArchivalUnit au,
                                        String urlPattern) {
        Pattern pat = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Matcher mat;
        for (CachedUrl cu : au.getAuCachedUrlSet().getCuIterable()) {
            String cuUrl = cu.getUrl();
            mat = pat.matcher(cuUrl);
            if (mat.find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}


