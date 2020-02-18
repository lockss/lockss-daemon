package org.lockss.plugin.silverchair.americanjournalofoccupationaltherapy;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class AmericanJournalofOccupationalTherapySilverchairArticleIteratorFactory  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {


    private static final String ROOT_TEMPLATE = "\"%s\", base_url";
    private static final String PATTERN_TEMPLATE = "\"^%s\", base_url";

    //https://ajot.aota.org/article.aspx?articleid=2360692
    private static final Pattern HTML_PATTERN = Pattern.compile("/article\\.aspx\\?articleid=(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/article.aspx?articleid=$2";

    //https://ajot.aota.org/Citation/Download?resourceId=2360691&resourceType=3&citationFormat=0
    private static final Pattern RIS_PATTERN = Pattern.compile("/Citation/Download\\?resourceId=(\\d+)&resourceType=3&citationFormat=0", Pattern.CASE_INSENSITIVE);
    private static final String RIS_REPLACEMENT = "/Citation/Download?resourceId=$1&resourceType=3&citationFormat=0";

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
                ArticleFiles.ROLE_FULL_TEXT_HTML);
        builder.addAspect(RIS_REPLACEMENT,
                ArticleFiles.ROLE_CITATION_RIS);
        builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                ArticleFiles.ROLE_CITATION_RIS,
                ArticleFiles.ROLE_FULL_TEXT_HTML);
        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}

