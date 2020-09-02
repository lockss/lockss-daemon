package org.lockss.plugin.clockss.numeriquepremium;

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

public class NumeriquePremiumBooksArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(NumeriquePremiumBooksArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%s\",base_url,directory";
    private static final String PATTERN_TEMPLATE = "\"%s%s/.*\",base_url,directory";

    // The one-to-one relationship is not guaranteed by the file name. For example:
    //https://clockss-test.lockss.org/sourcefiles/numerique-released/2020/PUR_24_files/9782753507920/9782753507920.pdf
    //https://clockss-test.lockss.org/sourcefiles/numerique-released/2020/PUR_24_files/9782753507920/9782753507920.xml

    //https://clockss-test.lockss.org/sourcefiles/numerique-released/2020/PUR_24_files/9782753511781/978-2-7535-1178-1.pdf
    //https://clockss-test.lockss.org/sourcefiles/numerique-released/2020/PUR_24_files/9782753511781/9782753511781.xml

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

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
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