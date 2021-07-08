package org.lockss.plugin.clockss.gigascience;

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

public class GigaScienceSourceZipXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

    // They sent us two xml, and they guarantee they will always send us two. "crossref" version is good enough for us
    // https://clockss-test.lockss.org/sourcefiles/gigabyte-released/2021_01/gigabyte13.zip!/gigabyte13/gigabyte13-crossref.xml
    // https://clockss-test.lockss.org/sourcefiles/gigabyte-released/2021_01/gigabyte13.zip!/gigabyte13/gigabyte13.xml
    // https://clockss-test.lockss.org/sourcefiles/gigabyte-released/2021_01/gigabyte13.zip!/gigabyte13/gigabyte13.pdf


    protected static Logger log = Logger.getLogger(GigaScienceSourceZipXmlArticleIteratorFactory.class);

    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s[^/]+/.*\\.zip!/(.*)\\.(xml|pdf)$\", base_url";

    // Be sure to exclude all nested archives in case supplemental data is provided this way
    protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
            Pattern.compile(".*/[^/]+\\.zip!/pc.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                    Pattern.CASE_INSENSITIVE);

    protected Pattern getExcludeSubTreePattern() {
        return SUB_NESTED_ARCHIVE_PATTERN;
    }

    protected String getIncludePatternTemplate() {
        return ALL_ZIP_XML_PATTERN_TEMPLATE;
    }

    public static final Pattern XML_PATTERN = Pattern.compile("/(.*)(\\-crossref)\\.xml$", Pattern.CASE_INSENSITIVE);
    public static final Pattern PDF_PATTERN = Pattern.compile("/(.*)\\.pdf$", Pattern.CASE_INSENSITIVE);
    public static final String XML_REPLACEMENT = "/$1-crossref.xml";
    private static final String PDF_REPLACEMENT = "/$1.pdf";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
        
        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
                .setExcludeSubTreePattern(getExcludeSubTreePattern())
                .setVisitArchiveMembers(true)
                .setVisitArchiveMembers(getIsArchive()));
        
        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(XML_PATTERN,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
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
