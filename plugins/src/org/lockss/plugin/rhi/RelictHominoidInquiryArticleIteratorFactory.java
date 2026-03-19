package org.lockss.plugin.rhi;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
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

public class RelictHominoidInquiryArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(RelictHominoidInquiryArticleIteratorFactory.class);

    /*
    The website has no individual article page, only a summary page
    https://www.isu.edu/rhi/book-reviews
    https://www.isu.edu/media/libraries/rhi/book-reviews/Ameranthropoides-loysi.pdf
    https://www.isu.edu/media/libraries/rhi/book-reviews/BF-Exposed.pdf

    https://www.isu.edu/rhi/brief-communications
    https://www.isu.edu/media/libraries/rhi/brief-communications/ANCIENT-REPRESENTATIONS-OF-THE-WILDMAN-IN-FRANCE.pdf
    https://www.isu.edu/media/libraries/rhi/brief-communications/Footprint-Evidence-of-Chinese-Yeren.pdf

     https://www.isu.edu/rhi/comments--responses
     https://www.isu.edu/media/libraries/rhi/comments-amp-responses/Commentary-on-Sykes.pdf
    https://www.isu.edu/media/libraries/rhi/comments-amp-responses/Forth-Comment_final.pdf

    https://www.isu.edu/rhi/essays
    https://www.isu.edu/media/libraries/rhi/essays/BAADE_Essay_2021_final.pdf
    https://www.isu.edu/media/libraries/rhi/essays/BINDERNAGEL_final.pdf

    https://www.isu.edu/rhi/from-the-editor
    https://www.isu.edu/media/libraries/rhi/from-the-editor/Are-Other.pdf
    https://www.isu.edu/media/libraries/rhi/from-the-editor/Bayanov_-PGF_50th.pdf

    https://www.isu.edu/rhi/news--views
    https://www.isu.edu/media/libraries/rhi/news-amp-views/DINSDALE-AWARD.pdf
    https://www.isu.edu/media/libraries/rhi/news-amp-views/IN-MEMORIAM_PETER-BYRNE_FINAL.pdf

    https://www.isu.edu/rhi/research-papers
    https://www.isu.edu/media/libraries/rhi/research-papers/ANALYSIS-INTEGRITY-OF-THE-PATTERSON-GIMLIN-FILM-IMAGE_final.pdf
    https://www.isu.edu/media/libraries/rhi/research-papers/ARGUE_Yahoos.pdf
     */

    // 1. Loosen the template to find PDFs in any media library subdirectory
    protected static final String PATTERN_TEMPLATE = "^%smedia/libraries/%s/[^/]+/.+\\.pdf$";

    // 2. Update the PDF_PATTERN to match the structure found by PATTERN_TEMPLATE
// We use [^/]+ twice: once for the category (research-papers, etc) and once for the filename
    // Add .* to the front to catch the base_url part of the string
    // 1. Updated Regex: Captures the directory name (Group 1) and filename (Group 2)
// This pattern is more resilient to the start of the URL.
    private static final Pattern PDF_PATTERN = Pattern.compile(
            ".*/media/libraries/[^/]+/([^/]+)/([^/]+\\.pdf)$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {

        Configuration config = au.getConfiguration();
        String base = config.get(ConfigParamDescr.BASE_URL.getKey());
        String jId = config.get("journal_id");

        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target, base,
                String.format(PATTERN_TEMPLATE, base, jId),
                Pattern.CASE_INSENSITIVE);

        builder.addAspect(PDF_PATTERN,
                base + "media/libraries/" + jId + "/$1/$2",
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        // 3. METADATA: Point this to the HTML landing page
        // Example: https://www.isu.edu
        builder.addAspect(PDF_PATTERN,
                base + jId + "/$1",
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);
        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
        return new RelictHominoidInquiryMetadataExtractorFactory.RelictHominoidMetadataExtractor();
    }
}


