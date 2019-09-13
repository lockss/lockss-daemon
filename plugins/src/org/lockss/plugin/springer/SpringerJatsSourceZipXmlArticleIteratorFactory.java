package org.lockss.plugin.springer;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.clockss.SourceXmlArticleIteratorFactory;
import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class SpringerJatsSourceZipXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

    protected static Logger log = Logger.getLogger(SpringerJatsSourceZipXmlArticleIteratorFactory.class);

    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s[^/]+/.*\\.zip!/.*\\.xml\\.Meta$\", base_url";

    // Be sure to exclude all nested archives in case supplemental data is provided this way
    protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
            Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                    Pattern.CASE_INSENSITIVE);

    protected Pattern getExcludeSubTreePattern() {
        //return SUB_NESTED_ARCHIVE_PATTERN;
        return null;
    }

    protected String getIncludePatternTemplate() {
        return ALL_ZIP_XML_PATTERN_TEMPLATE;
    }

    public static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml\\.Meta$", Pattern.CASE_INSENSITIVE);
    public static final String XML_REPLACEMENT = "/$1.xml";
    // There will not always be a 1:1 PDF relationship, but when there is use that as the
    // full-text CU.
    private static final String PDF_REPLACEMENT = "/$1.pdf";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        // no need to limit to ROOT_TEMPLATE
        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
                .setExcludeSubTreePattern(getExcludeSubTreePattern())
                .setVisitArchiveMembers(getIsArchive()));

        // NOTE - full_text_cu is set automatically to the url used for the articlefiles
        // ultimately the metadata extractor needs to set the entire facet map

        // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        // While we can't identify articles that are *just* PDF which is why they
        // can't trigger an articlefiles by themselves, we can identify them
        // by replacement and they should be the full text CU.
        builder.addAspect(PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);
        //Now set the order for the full text cu
        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
                ArticleFiles.ROLE_ARTICLE_METADATA); // though if it comes to this it won't emit

        return builder.getSubTreeArticleIterator();
    }

    // NOTE - for a child to create their own version of this
    // indicates if the iterator should descend in to archives (for tar/zip deliveries)
    protected boolean getIsArchive() {
        return false;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
