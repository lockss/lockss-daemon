package org.lockss.plugin.silverchair;

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


public class SilverchairScholarlyPublishingCollectiveArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {
    

    private static String ROOT_TEMPLATE = "\"%s\", base_url";
    private static String PATTERN_TEMPLATE =  "\"%s%s/%s/article/\", base_url, college_id, journal_id";

    private static Pattern HTML_PATTERN = Pattern.compile("/article/([^/]+)/(.*)$", Pattern.CASE_INSENSITIVE);

    private static String HTML_REPLACEMENT = "/article/$1/$2";
    private static String CITATION_REPLACEMENT = "/downloadcitation/$1?format=ris";

    protected static Logger getLog() {
        return Logger.getLogger(SilverchairScholarlyPublishingCollectiveArticleIteratorFactory.class);
    }
    
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {

        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                        ROOT_TEMPLATE,
                        PATTERN_TEMPLATE,
                        Pattern.CASE_INSENSITIVE);


        builder.addAspect(  HTML_PATTERN,
                            HTML_REPLACEMENT,
                            ArticleFiles.ROLE_FULL_TEXT_HTML,
                            ArticleFiles.ROLE_ARTICLE_METADATA);


        builder.addAspect(  CITATION_REPLACEMENT,
                            ArticleFiles.ROLE_CITATION);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
