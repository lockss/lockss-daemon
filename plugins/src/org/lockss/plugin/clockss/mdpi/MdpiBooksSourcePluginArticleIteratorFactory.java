package org.lockss.plugin.clockss.mdpi;

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

public class MdpiBooksSourcePluginArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(MdpiBooksSourcePluginArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%s/\",base_url,directory";
    private static final String PATTERN_TEMPLATE = "\"%s%s/([^/]+)\\.pdf\",base_url,directory";


    protected static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+)\\.pdf$");
    protected static final String PDF_REPLACEMENT = "/$1.pdf";

    protected static final Pattern XML_PATTERN = Pattern.compile("/([^/]+\\.onix.xml)$");
    protected static final String XML_REPLACEMENT = "/$1.onix.xml";

    protected static final String XML_REPLACEMENT2 = "/$1.xml";


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

        builder.addAspect(PDF_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_XML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.addAspect(PDF_PATTERN,
                XML_REPLACEMENT2,
                ArticleFiles.ROLE_FULL_TEXT_XML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
