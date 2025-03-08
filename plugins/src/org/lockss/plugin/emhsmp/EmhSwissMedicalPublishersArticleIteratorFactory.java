package org.lockss.plugin.emhsmp;

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

public class EmhSwissMedicalPublishersArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(EmhSwissMedicalPublishersArticleIteratorFactory.class);


    /*
        Expected new aspect pattern

        https://emhsmp2025.clockss.org/emhsmp/cvm/019/01/00371/index.html
        https://emhsmp2025.clockss.org/emhsmp/cvm/019/01/00371/article_pdf/00371.pdf
        https://emhsmp2025.clockss.org/emhsmp/cvm/019/01/00371/metadata_jats/00371.xml

     */
    protected static final String ROOT_TEMPLATE = "\"%s%s/%s/%s\", base_url, publisher_id, journal_id, volume_name";
    protected static final String PATTERN_TEMPLATE = "/([^/]+)(?:/(article_pdf|metadata_jats)/([^/]+)\\.(xml|pdf)|/index\\.html)$";

    public static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+)/(article_pdf)/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);

    //public static final Pattern XML_PATTERN = Pattern.compile("/([^/]+)/(metadata_jats)/([^/]+)\.xml$", Pattern.CASE_INSENSITIVE);
    //public static final Pattern HTML_PATTERN = Pattern.compile("/([^/]+)/index\\.html$", Pattern.CASE_INSENSITIVE);

    public static final String XML_REPLACEMENT = "/$1/metadata_jats/$2.xml";
    private static final String PDF_REPLACEMENT = "/$1/article_pdf/$2.pdf";
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

        builder.addAspect(
                PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(
                HTML_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);


        builder.addAspect(
                XML_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_XML);


        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
