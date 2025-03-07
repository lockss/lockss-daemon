package org.lockss.plugin.emhswiss;

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

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class EmhSwissMedicalTriggeredPluginArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(EmhSwissMedicalTriggeredPluginArticleIteratorFactory.class);

    /*
    emhsmp/cvm/019/01/00378/article_pdf/cvm-2016-00378.pdf
    emhsmp/cvm/019/01/00378/index.html
    emhsmp/cvm/019/01/00378/metadata_jats/cvm-2016-00378.xml
    */

    protected static final String ROOT_TEMPLATE = "\"%s%s\",base_url,publisher_id";
    protected static final String PATTERN_TEMPLATE = "\"/%s/%s/(?:([^/]+/[^/]+/(?:article_pdf|metadata_jats)/[^/]+)\\.(xml|pdf)|([^/]+/[^/]+)/index\\.html)$\", journal_id, volume_name";

    public static final Pattern XML_PATTERN = Pattern.compile("/([^/]+/[^/]+/(?:article_pdf|metadata_jats)/[^/]+)\\.xml$", Pattern.CASE_INSENSITIVE);
    public static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+/[^/]+/(?:article_pdf|metadata_jats)/[^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    public static final Pattern HTML_PATTERN = Pattern.compile("/([^/]+/[^/]+)/index\\.html$", Pattern.CASE_INSENSITIVE);

    public static final String XML_REPLACEMENT = "/$1.xml";
    private static final String PDF_REPLACEMENT = "/$1.pdf";
    public static final String HTML_REPLACEMENT = "/$1/index.html";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE,
                Pattern.CASE_INSENSITIVE);

        builder.addAspect(HTML_PATTERN,
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_XML);
        

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML);

        /*
         builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML);
                ArticleFiles.ROLE_FULL_TEXT_PDF,
                ArticleFiles.ROLE_FULL_TEXT_XML));

        builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_FULL_TEXT_PDF,
                ArticleFiles.ROLE_FULL_TEXT_XML));
        */

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
