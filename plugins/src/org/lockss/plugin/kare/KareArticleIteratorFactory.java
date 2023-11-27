/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.kare;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Kare's abstract page has links to Full-Text pdf and also
 * contains a handful of metatdata types.
 *
 *  article
 *  https://agridergisi.com/jvi.aspx?pdir=agri&plng=eng&un=AGRI-42800
 *  abstract
 *  https://agridergisi.com/jvi.aspx?pdir=agri&plng=eng&un=AGRI-42800&look4=
 *  pdf
 *  https://jag.journalagent.com/z4/download_fulltext.asp?pdir=agri&plng=eng&un=AGRI-42800
 *  citation
 *  https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=AGRI-42800&format=RIS
 */
public class KareArticleIteratorFactory
        implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    private static final Logger log = Logger.getLogger(KareArticleIteratorFactory.class);
    private static String PATTERN_TEMPLATE = "\"^(%sjvi.aspx\\?pdir=%s&plng=eng&un=|https://jag.journalagent.com/.+(download_fulltext|gencitation))\", base_url, journal_id";

    private static final Pattern ABSTRACT_PATTERN = Pattern.compile("https://agridergisi\\.com/jvi\\.aspx\\?pdir=agri&plng=eng&un=([^&]+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern PDF_PATTERN = Pattern.compile("https://jag\\.journalagent\\.com/z4/download_fulltext\\.asp\\?pdir=agri&plng=eng&un=([^&]+)$", Pattern.CASE_INSENSITIVE);
    private static final String ABSTRACT_REPLACEMENT = "https://agridergisi.com/jvi.aspx?pdir=agri&plng=eng&un=$1";
    private static final String PDF_REPLACEMENT = "https://jag.journalagent.com/z4/download_fulltext.asp?pdir=agri&plng=eng&un=$1";
    private static final String CITATION_RIS_REPLACEMENT = "https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=$1&format=RIS";
    private static final String CITATION_BIBTEX_REPLACEMENT = "https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=$1&format=BibTeX";
    private static final String CITATION_ENDNOTE_REPLACEMENT = "https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=$1&format=EndNote";
    private static final String CITATION_PROCITE_REPLACEMENT = "https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=$1&format=Procite";
    private static final String CITATION_MEDLARS_REPLACEMENT = "https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=$1&format=Medlars";
    private static final String CITATION_REFMANAGER_REPLACEMENT = "https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=$1&format=referenceManager";


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(new SubTreeArticleIterator.Spec()
                .setTarget(target)
                .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

        // set up pdf to be an aspect that will trigger an ArticleFiles
        builder.addAspect(PDF_PATTERN,
                PDF_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(ABSTRACT_PATTERN,
                ABSTRACT_REPLACEMENT,
                ArticleFiles.ROLE_ABSTRACT);

        /* the various citation files */
        builder.addAspect(CITATION_RIS_REPLACEMENT,
                ArticleFiles.ROLE_CITATION_RIS);

        builder.addAspect(CITATION_BIBTEX_REPLACEMENT,
                ArticleFiles.ROLE_CITATION_BIBTEX);

        builder.addAspect(CITATION_ENDNOTE_REPLACEMENT,
                ArticleFiles.ROLE_CITATION_ENDNOTE);

        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                ArticleFiles.ROLE_CITATION_RIS);

        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}