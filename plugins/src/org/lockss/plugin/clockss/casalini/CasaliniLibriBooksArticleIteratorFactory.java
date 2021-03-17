package org.lockss.plugin.clockss.casalini;

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

public class CasaliniLibriBooksArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(CasaliniLibriBooksArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s\",base_url";
    private static final String PATTERN_TEMPLATE = "\"%s%s/.*\\.(mrc|xml)$\",base_url,directory";

    // In 2020, the content is delivered using ".mrc" file.
    // https://clockss-test.lockss.org/sourcefiles/casalini-released/2020/Sample%20Material/SubsetSampleRecords.mrc
    protected static final Pattern MRC_PATTERN = Pattern.compile("/(.*).mrc$");
    protected static final String MRC_REPLACEMENT = "/$1.mrc";


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(MRC_PATTERN,
                MRC_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}