package org.lockss.plugin.resiliencealliance;

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

public class ResilienceAllianceArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(ResilienceAllianceArticleIteratorFactory .class);

    /*
    Since we will not be able to guess our the PDF pattern from html pattern, use html as the article count instead
    Article: https://www.ace-eco.org/vol14/iss1/art1
    Article related PDF: https://www.ace-eco.org/vol14/iss1/art1/ACE-ECO-2018-1293.pdf
    Other Appendix: https://www.ace-eco.org/vol14/iss1/art1/appendix1.pdf
     */

    protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
    protected static final String PATTERN_TEMPLATE =
            "\"%svol%s/iss[^/]+/art[^/]+$\", base_url, volume_name";

    private static final Pattern HTML_PATTERN = Pattern.compile(
            "/(vol[^/]+/iss[^/]+/art[^/]+)$",
            Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/$1";



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

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}


