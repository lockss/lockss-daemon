package org.lockss.plugin.clockss.isecs;

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

public class InternationalStructuralEngineeringConstructionSocietyArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(InternationalStructuralEngineeringConstructionSocietyArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
    private static final String PATTERN_TEMPLATE = "\"%s%d.*/[^/]+\",base_url,year";

    // The delivery does not have many-pdf-to-one-xml matching relationship
    // https://clockss-test.lockss.org/sourcefiles/isec-released/2020/ASEA3/XML/ISEC-DOI-XML_ASEA3_07-02-20-HST.xml
    // https://clockss-test.lockss.org/sourcefiles/isec-released/2020/ASEA3/AAE-1_v3_13.pdf

    protected static final Pattern XML_PATTERN = Pattern.compile("/XML/([^.]+)\\.xml$");
    protected static final String XML_REPLACEMENT = "/$1.xml";


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

        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}