package org.lockss.plugin.clockss.edituraase;

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

public class EdituraASEArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(EdituraASEArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%s\",base_url,directory";
    private static final String PATTERN_TEMPLATE = "\"%s%s/.*\",base_url,directory";

    // The delivery does not have many-pdf-to-one-xml matching relationship
    // https://clockss-test.lockss.org/sourcefiles/ease-released/2022_01/60/AE_DOIv_1211_60.xml
    // https://clockss-test.lockss.org/sourcefiles/ease-released/2022_01/60/Article_3097.pdf
    // https://clockss-test.lockss.org/sourcefiles/ease-released/2022_01/60/Article_3099.pdf

    protected static final Pattern XML_PATTERN = Pattern.compile("/.*\\.xml$");
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