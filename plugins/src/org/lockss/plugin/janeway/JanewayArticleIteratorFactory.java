package org.lockss.plugin.janeway;

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

public class JanewayArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(JanewayArticleIteratorFactory.class);

    //https://www.iastatedigitalpress.com/aglawdigest/article/id/8101
    //https://www.iastatedigitalpress.com/aglawdigest/article/id/8101/print
    //https://www.iastatedigitalpress.com/aglawdigest/article/8101/galley/7872/download

    //https://www.comicsgrid.com/article/id/3579
    //https://www.comicsgrid.com/article/3579/galley/5155/download	
    protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
    protected static final String PATTERN_TEMPLATE =
            "\"%s([^/]+/)?article\", base_url";

    private static final Pattern HTML_PATTERN = Pattern.compile(
            "/(id)/(\\d+)$",
            Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/id/$2";

    private static final Pattern PDF_PATTERN = Pattern.compile(
            "/([^/]+)/(galley/[^/]+/download)",
            Pattern.CASE_INSENSITIVE);
    private static final String PDF_REPLACEMENT = "$1/$2";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        // Use HTML instead of PDF as full text, since download url gave multiple formats, not only PDF
        builder.addAspect(
                HTML_PATTERN,
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_ABSTRACT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        /*
        builder.addAspect(
                PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);
         */


        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}

