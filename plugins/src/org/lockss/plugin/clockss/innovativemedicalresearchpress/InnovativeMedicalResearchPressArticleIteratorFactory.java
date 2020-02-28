package org.lockss.plugin.clockss.innovativemedicalresearchpress;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import java.util.Iterator;
import java.util.regex.Pattern;

public class InnovativeMedicalResearchPressArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(InnovativeMedicalResearchPressArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%d\",base_url,year";
    private static final String PATTERN_TEMPLATE = "\"%s%d\",base_url,year";

    //There is one-to-one relation between the xml and PDF file, but it is not by file naming convention
    //PDF file name is referenced inside xml <related-article related-article-type="pdf" specific-use="online">1555335526845-369563756.pdf</related-article>
    //http://content5.lockss.org/sourcefiles/imrpress-released/2020/JIN%20Volume%2018%20(2019)/JIN%20Volume%2018%20issue%201/1757-448X-18-1-1.xml
    //http://content5.lockss.org/sourcefiles/imrpress-released/2020/JIN%20Volume%2018%20(2019)/JIN%20Volume%2018%20issue%201/1555335529553-1407663168.pdf.md5sum

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