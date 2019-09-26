package org.lockss.plugin.innovativemedicalresearch;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class InnovativeMedicalResearchJatsSourceZipXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(InnovativeMedicalResearchJatsSourceZipXmlArticleIteratorFactory.class);

    //http://content5.lockss.org/sourcefiles/imrpress-released/2019/JMCM2018-Volume%201%20Issue%202.zip!/JMCM2018-Volume 1 Issue 2/2617-5282-2018-2/2617-5282-1-2-107.xml
    //http://content5.lockss.org/sourcefiles/imrpress-released/2019/JMCM2018-Volume%201%20Issue%202.zip!/JMCM2018-Volume 1 Issue 2/2617-5282-2018-2/1545826577946-771105881.pdf
    //Their folder name combines uppercase and lowercase
    protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
            "\"%s%s/[^/]+\\.zip!/[^/]+/[^/]+/[^/]+$\", base_url, directory";

    // Be sure to exclude all nested archives in case supplemental data is provided this way
    protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
            Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                    Pattern.CASE_INSENSITIVE);
    /*
    This is the matching group for the following pattern
    0	/JMCM2018-Volume%201%20Issue%202.zip!/JMCM2018-Volume 1 Issue 2/2617-5282-2018-2/1545826577946-771105881.pdf
    1	JMCM2018-Volume 1 Issue 2
    2	2617-5282-2018-2
    3	1545826577946-771105881.pdf
    4
    5	1545826577946-771105881.pdf

    0	/JMCM2018-Volume%201%20Issue%202.zip!/JMCM2018-Volume 1 Issue 2/2617-5282-2018-2/1545826577946-771105881.pdf
    1	JMCM2018-Volume 1 Issue 2
    2	2617-5282-2018-2
    3	2617-5282-1-2-107.xml
    4	2617-5282-1-2-107.xml
    5
     */

    protected static final String COMMON_PATTERN_STRING = "/([^/]+)/([^/]+)/((\\d+\\-\\d+\\-\\d+\\-[\\d\\-]+\\.xml)|(\\d+\\-\\d+\\.pdf))";
    protected static final Pattern COMMON_PATTERN = Pattern.compile(COMMON_PATTERN_STRING);

    protected static final String PDF_REPLACEMENT = "/$1/$2/$5";
    protected static final String XML_REPLACEMENT = "/$1/$2/$4";


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setPatternTemplate(getIncludePatternTemplate())
                .setExcludeSubTreePattern(getExcludeSubTreePattern())
                .setVisitArchiveMembers(getIsArchive()));

        builder.addAspect(COMMON_PATTERN_STRING,
                XML_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.addAspect(COMMON_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }

    protected Pattern getExcludeSubTreePattern() {
        return SUB_NESTED_ARCHIVE_PATTERN;
    }

    protected String getIncludePatternTemplate() {
        return ALL_ZIP_XML_PATTERN_TEMPLATE;
    }

    // NOTE - for a child to create their own version of this
    // indicates if the iterator should descend in to archives (for tar/zip deliveries)
    protected boolean getIsArchive() {
        return true;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}