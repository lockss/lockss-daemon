package org.lockss.plugin.gigascience;

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

public class GigaScienceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    private static final String ROOT_TEMPLATE = "\"%s\", base_url";
    private static final String PATTERN_TEMPLATE = "\"%s\", base_url";

    //http://gigadb.org/api/dataset?doi=100300
    private static final Pattern HTML_PATTERN = Pattern.compile("/(dataset\\?doi=.*)", Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/$1";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(HTML_PATTERN,
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.setRoleFromOtherRoles( ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_FULL_TEXT_HTML);
        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}

