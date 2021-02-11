package org.lockss.plugin.clockss.mpidr;

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

public class MPIDRRisArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(MPIDRRisArticleIteratorFactory.class);

    /*
    https://clockss-test.lockss.org/sourcefiles/mpidr-released/2021/special/3/9/S3-9.pdf
    https://clockss-test.lockss.org/sourcefiles/mpidr-released/2021/special/3/9/article.ris
    https://clockss-test.lockss.org/sourcefiles/mpidr-released/2021/special/3/9/default.htm
    https://clockss-test.lockss.org/sourcefiles/mpidr-released/2021/volumes/vol1/1/1-1.pdf
    https://clockss-test.lockss.org/sourcefiles/mpidr-released/2021/volumes/vol1/1/article.ris
    https://clockss-test.lockss.org/sourcefiles/mpidr-released/2021/volumes/vol1/1/default.htm
     */

    protected static final String PATTERN_TEMPLATE = "\"^%s%s/(.*)\\.(ris|pdf)$\",base_url, directory";

    public static final Pattern RIS_PATTERN = Pattern.compile("/([^/]+)\\.ris$", Pattern.CASE_INSENSITIVE);
    public static final String RIS_REPLACEMENT = "/$1.ris";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        // no need to limit to ROOT_TEMPLATE
        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

        builder.addAspect(RIS_PATTERN,
                RIS_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}


