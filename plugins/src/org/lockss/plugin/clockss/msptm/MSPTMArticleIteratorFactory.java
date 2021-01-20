package org.lockss.plugin.clockss.msptm;

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

public class MSPTMArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

    //https://clockss-test.lockss.org/sourcefiles/msptm-released/2021/msptm/pdf/37(3)/TBv37i3_542-550.pdf
    //https://clockss-test.lockss.org/sourcefiles/msptm-released/2021/msptm/xml/37(3)/TBv37i3_542-550.xml
    protected static Logger log = Logger.getLogger(MSPTMArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s\",base_url";
    protected static final String PATTERN_TEMPLATE =
            "\"%s%s/.*\\/(.*)\\.(xml|pdf)$\", base_url, directory";


    public static final Pattern XML_PATTERN = Pattern.compile("/xml/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);
    public static final Pattern PDF_PATTERN = Pattern.compile("/pdf/(.*)\\.pdf$", Pattern.CASE_INSENSITIVE);
    public static final String XML_REPLACEMENT = "/xml/$1.xml";
    private static final String PDF_REPLACEMENT = "/pdf/$1.pdf";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        // no need to limit to ROOT_TEMPLATE
        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_XML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        return builder.getSubTreeArticleIterator();
    }

    protected boolean getIsArchive() {
        return true;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
