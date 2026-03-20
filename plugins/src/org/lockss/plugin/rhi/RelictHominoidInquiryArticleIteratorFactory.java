/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.rhi;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class RelictHominoidInquiryArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {
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

    protected static Logger log = Logger.getLogger(RelictHominoidInquiryArticleIteratorFactory.class);

    private static final String ROOT_TEMPLATE = "\"%s\", base_url";
    private static final String PATTERN_TEMPLATE =
            "\"^%smedia/libraries/%s/[^/]+/.+\\.pdf$\", base_url, journal_id";

    private static final Pattern PDF_PATTERN = Pattern.compile(
            "/([^/]+)/([^/]+\\.pdf)$", Pattern.CASE_INSENSITIVE);

    // Template strings for replacements
    private static final String PDF_REPLACEMENT_TEMPLATE = "/media/libraries/%s/$1/$2";
    private static final String METADATA_REPLACEMENT_TEMPLATE = "/%s/$1";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {

        // 1. Fetch the actual journal ID from the AU config
        String journalId = au.getConfiguration().get("journal_id");

        // 2. Perform the formatting outside the builder's fluent calls
        String pdfReplacement = String.format(PDF_REPLACEMENT_TEMPLATE, journalId);
        String metadataReplacement = String.format(METADATA_REPLACEMENT_TEMPLATE, journalId);

        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE,
                Pattern.CASE_INSENSITIVE);

        // 3. Use the pre-formatted strings
        builder.addAspect(PDF_PATTERN,
                pdfReplacement,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(PDF_PATTERN,
                metadataReplacement,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);
        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
        return new RelictHominoidInquiryMetadataExtractorFactory.RelictHominoidMetadataExtractor();
    }
}
