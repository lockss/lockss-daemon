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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;


public class SilverchairCommonThemeArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    // Article and PDF patterns look like the following
    //https://pubs.geoscienceworld.org/gsa/geosphere/article/14/1/286/524169/Alteration-mass-analysis-and-magmatic-compositions
    //https://pubs.geoscienceworld.org/gsa/geosphere/article-pdf/14/1/286/4120162/286.pdf
    //https://pubs.geoscienceworld.org/gsa/geosphere/article-standard/14/6/2533/565887/pages/remote-access-information

    //https://portlandpress.com/biochemsoctrans/article/48/1/1/222098/Biomarkers-of-inflammation-and-the-etiology-of
    //https://portlandpress.com/biochemsoctrans/article-pdf/48/1/1/868576/bst-2019-0029c.pdf

    //https://rupress.org/jem/article/216/12/2689/132506/Modulation-of-the-fungal-mycobiome-is-regulated-by
    //https://rupress.org/jem/article-pdf/216/12/2689/1170997/jem_20182244.pdf

    //https://research.aota.org/ajot/article/74/1/7401090010p1/6652/Making-Functional-Cognition-a-Professional
    //https://research.aota.org/ajot/article/74/1/7401090010p1/6652/ajot/pages/authorguidelines
    //https://research.aota.org/ajot/article/74/1/7401090010p1/6652/ajot/pages/subscribe
    //https://research.aota.org/ajot/article/74/1/7401090010p1/ajot
    //https://research.aota.org/ajot/article/74/1/7401090010p1/contact-us


    private static String ROOT_TEMPLATE = "\"%s\", base_url";
    private static String PATTERN_TEMPLATE =  "\"%s([^/]+/)?%s/article-abstract/\", base_url, journal_id";

    private static Pattern HTML_PATTERN = Pattern.compile("/article/([^/]+)/(.*)$", Pattern.CASE_INSENSITIVE);
    private static Pattern HTML_ABSTRACT_PATTERN = Pattern.compile("/article-abstract/([^/]+)/(.*)$", Pattern.CASE_INSENSITIVE);


    private static String HTML_REPLACEMENT = "/article/$1/$2";
    private static String ABSTRACT_REPLACEMENT = "/article-abstract/$1/$2";
    private static String CITATION_REPLACEMENT = "/downloadcitation/$1?format=ris";

    protected static Logger getLog() {
        return Logger.getLogger(SilverchairCommonThemeArticleIteratorFactory.class);
    }
    
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {

        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                        ROOT_TEMPLATE,
                        PATTERN_TEMPLATE,
                        Pattern.CASE_INSENSITIVE);
        
        builder.addAspect(  HTML_ABSTRACT_PATTERN,
                            ABSTRACT_REPLACEMENT,
                            ArticleFiles.ROLE_ABSTRACT,
                            ArticleFiles.ROLE_ARTICLE_METADATA);

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
