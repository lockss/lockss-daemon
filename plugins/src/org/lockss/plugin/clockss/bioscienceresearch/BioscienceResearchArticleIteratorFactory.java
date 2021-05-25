package org.lockss.plugin.clockss.bioscienceresearch;

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

public class BioscienceResearchArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(BioscienceResearchArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%s\",base_url,directory";
    private static final String PATTERN_TEMPLATE = "\"%s%s.*/[^/]+\",base_url,directory";

    // https://clockss-test.lockss.org/sourcefiles/bioscienceresearch-released/2021_01/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/Scholar548.pdf
    // https://clockss-test.lockss.org/sourcefiles/bioscienceresearch-released/2021_01/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/548.xml

    // <base_url><directory>/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/548.xml
    // <base_url><directory>/Frontiers%20in%20Bioscience-Scholar/FBS%20Volume%2013%20(2021)/FBS%20Volume%2013%20issue%201/Scholar548.pdf
    // this is a lot of group capturing, but since we need the bit between the "-" and second "/"
    // this string occurs before the \d+.pdf url
    protected static final Pattern XML_PATTERN = Pattern.compile("/([^-/]*)-([^/]*)/([^/]*/[^/]*)/(\\D*)(\\d+)\\.xml$");
    protected static final String XML_REPLACEMENT = "/$1-$2/$3/$5.xml";
    protected static final Pattern PDF_PATTERN = Pattern.compile("/([^-/]*)-([^/]*)/([^/]*/[^/]*)/(\\D*)(\\d+)\\.pdf$");
    protected static final String PDF_REPLACEMENT = "/$1-$2/$3/$2$5.pdf";


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
            ROOT_TEMPLATE,
            PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(XML_PATTERN,
            XML_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.addAspect(PDF_PATTERN,
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