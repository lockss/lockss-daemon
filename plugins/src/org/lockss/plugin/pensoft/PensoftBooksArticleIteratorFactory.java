package org.lockss.plugin.pensoft;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;

import java.util.Iterator;
import java.util.regex.Pattern;


public class PensoftBooksArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    /*
        https://ab.pensoft.net/article/68634
        https://ab.pensoft.net/article/68634/download/pdf/831178

     */
    private static String ROOT_TEMPLATE = "\"%s\", base_url";
    private static String PATTERN_TEMPLATE = "\"%s\", base_url";


    private static Pattern PDF_LANDING_PAGE_PATTERN = Pattern.compile("/(article/[^/]+)$", Pattern.CASE_INSENSITIVE);
    private static String PDF_LANDING_PAGE_REPLACEMENT = "/$1";

    private static Pattern PDF_PATTERN = Pattern.compile("/(article/[^/]+/download/pdf/\\.*)$", Pattern.CASE_INSENSITIVE);
    private static String PDF_REPLACEMENT = "/$1";
    
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {

        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                        ROOT_TEMPLATE,
                        PATTERN_TEMPLATE,
                        Pattern.CASE_INSENSITIVE);

        builder.addAspect(  PDF_LANDING_PAGE_PATTERN,
                            PDF_LANDING_PAGE_REPLACEMENT,
                            ArticleFiles.ROLE_FULL_TEXT_HTML,
                            ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
