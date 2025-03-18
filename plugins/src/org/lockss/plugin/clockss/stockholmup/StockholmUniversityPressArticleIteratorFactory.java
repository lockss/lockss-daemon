package org.lockss.plugin.clockss.stockholmup;

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

public class StockholmUniversityPressArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(StockholmUniversityPressArticleIteratorFactory.class);

    /*
    https://clockss-ingest.lockss.org/sourcefiles/stockholmup-released/2025/jhlr.37.pdf
    https://clockss-ingest.lockss.org/sourcefiles/stockholmup-released/2025/jhlr.37.json
     */

    protected static final String ROOT_TEMPLATE = "\"%s%s\", base_url, directory";
    protected static final String PATTERN_TEMPLATE = "/([^/]+)\\.(json|pdf)$";

    public static final Pattern JSON_PATTERN = Pattern.compile("/([^/]+)\\.json$", Pattern.CASE_INSENSITIVE);

    private static final String PDF_REPLACEMENT = "/$1.pdf";
    public static final String JSON_REPLACEMENT = "/$1.json";


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE,
                Pattern.CASE_INSENSITIVE);

        builder.addAspect(
                JSON_PATTERN,
                JSON_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.addAspect(
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);



        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
