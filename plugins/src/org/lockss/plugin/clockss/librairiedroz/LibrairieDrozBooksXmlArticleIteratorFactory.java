package org.lockss.plugin.clockss.librairiedroz;

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

public class LibrairieDrozBooksXmlArticleIteratorFactory
        implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    // Plain files, no archives:
    // <base_url><directory>/mods_20260907.xml
    // <base_url><directory>/mods_20260907.xml.md5sum
    // <base_url><directory>/epubs_20260907/9782600316095.epub
    // <base_url><directory>/epubs_20260907/9782600316095.epub.md5sum
    //
    // One mods_YYYYMMDD.xml is a <modsCollection> of N books. Each book's epub lives in the
    // parallel epubs_YYYYMMDD/ directory named after its epub ISBN, so xml and epub share no
    // stem and cannot be paired by substitution. Iterate on the xml only; the metadata
    // extractor resolves each record's epub by ISBN.

    protected static Logger log = Logger.getLogger(LibrairieDrozBooksXmlArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%s\",base_url,directory";
    private static final String PATTERN_TEMPLATE = "\"%s%s/.*\\.xml$\",base_url,directory";

    protected static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$");
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

        builder.setFullTextFromRoles(ArticleFiles.ROLE_ARTICLE_METADATA);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
