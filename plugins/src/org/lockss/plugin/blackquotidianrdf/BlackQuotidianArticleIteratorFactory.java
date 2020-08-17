package org.lockss.plugin.blackquotidianrdf;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class BlackQuotidianArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(BlackQuotidianArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
    protected static final String PATTERN_TEMPLATE =
            "\"%sbq/\", base_url";

    private static final Pattern HTML_PATTERN = Pattern.compile(
            "/(bq)/([^/]+)",
            Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/bq/$2";

    private static final Pattern PDF_PATTERN = Pattern.compile(
            "/(bq/media)/(.*)\\.pdf",
            Pattern.CASE_INSENSITIVE);
    private static final String PDF_REPLACEMENT = "/pdf/$2.pdf";


    //It has only html and PDF, no separate pages other aspects, like  abstract/full_tex
    //
    //http://blackquotidian.supdigital.org/bq/1966-ncaa-basketball-championship---texas-western-vs-kentucky
    //http://blackquotidian.supdigital.org/bq/august-2-1958
    //http://blackquotidian.supdigital.org/bq/media/ADW%201-20-43.pdf
    //http://blackquotidian.supdigital.org/bq/media/CD%203-6-15.pdf
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(
                HTML_PATTERN,
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_ABSTRACT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.addAspect(
                PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}

