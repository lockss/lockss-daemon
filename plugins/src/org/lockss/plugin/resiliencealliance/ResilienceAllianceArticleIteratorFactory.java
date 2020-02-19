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
    https://www.ace-eco.org/vol14/iss1/art1
    https://www.ace-eco.org/vol14/iss1/art1/ACE-ECO-2018-1293.pdf
    https://www.ace-eco.org/vol14/iss1/art1/appendix1.pdf
    https://www.ace-eco.org/vol14/iss2/art9/eqn1.gif -- this one will throw exception
     */

    protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
    protected static final String PATTERN_TEMPLATE =
            "\"%s(vol%s/iss[^/]+/art\\.*)\", base_url, volume_name";

    private static final Pattern HTML_PATTERN = Pattern.compile(
            "/(vol[^/]+/iss[^/]+/art[^/]+)",
            Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/$1";

    private static final Pattern PDF_PATTERN = Pattern.compile(
            "/(vol[^/]+/iss[^/]+/art[^/]+)(/ACE.*\\.pdf)",
            Pattern.CASE_INSENSITIVE);
    private static final String PDF_REPLACEMENT = "/$1/$2";


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(
                PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(
                HTML_PATTERN,
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_ABSTRACT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}


