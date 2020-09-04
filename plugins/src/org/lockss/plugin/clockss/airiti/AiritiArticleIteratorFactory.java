package org.lockss.plugin.clockss.airiti;

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

public class AiritiArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(AiritiArticleIteratorFactory.class);

    public static final Pattern RIS_PATTERN = Pattern.compile("/([^/]+)\\.ris$", Pattern.CASE_INSENSITIVE);
    public static final String RIS_REPLACEMENT = "/$1.ris";

    public static final Pattern PDF_PATTERN = Pattern.compile("/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
    public static final String PDF_REPLACEMENT = "/$1.pdf";

    protected static final String PATTERN_TEMPLATE = "\"^%s[^/]+/(.*)\\.(?:ris|pdf)$\",base_url";
    //
    // The source content structure looks like this:
    // <root_location>/<dir>/<possible subdirectories>/<STUFF> where STUFF is a series of files:
    // <name>.pdf, <name>.ris. <name>.ris is used to extract metadata
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        // no need to limit to ROOT_TEMPLATE
        builder.setSpec(builder.newSpec()
                .setTarget(target)
                .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(RIS_PATTERN,
                RIS_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}


