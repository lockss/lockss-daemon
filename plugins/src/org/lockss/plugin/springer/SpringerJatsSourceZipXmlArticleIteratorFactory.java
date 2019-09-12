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

public class SpringerJatsSourceZipXmlArticleIteratorFactory extends SourceXmlArticleIteratorFactory {

    protected static Logger log = Logger.getLogger(SpringerJatsSourceZipXmlArticleIteratorFactory.class);

    protected static final String ALL_XML_PATTERN_TEMPLATE = "\"^%s[^/]+/(.*)\\.xml\\.Meta$\",base_url";

    protected static final Pattern NESTED_ARCHIVE_PATTERN =
            Pattern.compile(".*/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                    Pattern.CASE_INSENSITIVE);

    public static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml\\.Meta$", Pattern.CASE_INSENSITIVE);
    public static final String XML_REPLACEMENT = "/$1.xml";
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

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);
        
        builder.addAspect(PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
                ArticleFiles.ROLE_ARTICLE_METADATA); // though if it comes to this it won't emit

        return builder.getSubTreeArticleIterator();
    }

    protected Pattern getExcludeSubTreePattern() {
        log.debug3("Fei: NESTED_ARCHIVE_PATTERN = " + NESTED_ARCHIVE_PATTERN);
        return NESTED_ARCHIVE_PATTERN;
    }

    protected String getIncludePatternTemplate() {
        log.debug3("Fei: ALL_XML_PATTERN_TEMPLATE = " + ALL_XML_PATTERN_TEMPLATE);
        return ALL_XML_PATTERN_TEMPLATE;
    }


    protected boolean getIsArchive() {
        return false;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
