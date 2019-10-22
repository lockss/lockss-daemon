package org.lockss.plugin.clockss.casalini;

import org.apache.xerces.xs.StringList;
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

public class CasaliniLibriMarcXmlSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(CasaliniLibriMarcXmlSourceArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
    private static final String PATTERN_TEMPLATE = "\"%s%d.*/[^/]+\",base_url,year";


    protected static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+)\\.pdf$");
    protected static final String PDF_REPLACEMENT = "/$1.pdf";

    protected static final Pattern XML_PATTERN = Pattern.compile("/([^/]+)\\.xml$");
    protected static final String XML_REPLACEMENT = "/$1.xml";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
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