/*
Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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
package org.lockss.plugin.oecd;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class OecdJournalsArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(OecdJournalsArticleIteratorFactory.class);

  private static final String PATTERN_TEMPLATE =
      "\"%s(%s/.*_|.+\\.pdf\\?itemId=.+)%s-%d\", base_url, topic, journal_id, year";

  // https://www.oecd-ilibrary.org/economics/frequency-based-co-movement-of-inflation-in-selected-euro-area-countries_jbcma-2015-5jm26ttlxdd1
  private static Pattern LANDING_PATTERN;
  private static Pattern PDF_PATTERN;

  private String LANDING_REPLACEMENT;
  private String PDF_REPLACEMENT;
  private String RIS_REPLACEMENT;
  private String BIB_REPLACEMENT;
  private String ENDNOTE_REPLACEMENT;

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    String jid = au.getConfiguration().get("journal_id");
    String topic = au.getConfiguration().get("topic");
    String year = au.getConfiguration().get("year");
    // https://www.oecd-ilibrary.org/economics/frequency-based-co-movement-of-inflation-in-selected-euro-area-countries_jbcma-2015-5jm26ttlxdd1
    LANDING_PATTERN = Pattern.compile(
        String.format("/%s/([^/]+)_(%s-%s)-([^/]+)$",
            topic, jid, year));
    // https://www.oecd-ilibrary.org/frequency-based-co-movement-of-inflation-in-selected-euro-area-countries_5jm26ttlxdd1.pdf?itemId=%2Fcontent%2Fpaper%2Fjbcma-2015-5jm26ttlxdd1&mimeType=pdf
    //PDF_PATTERN = Pattern.compile(
    //    String.format("/(.+)_.+\\.pdf\\?itemId=%%2Fcontent%%2Fpaper%%2F(%s-%s)-([^&]+)&mimeType=pdf$",
    //        jid, year, jid, year));


    LANDING_REPLACEMENT = String.format("/%s/$1_$3-$2", topic);
    RIS_REPLACEMENT = String.format("/%s/$1_$3-$2/cite/ris", topic);
    BIB_REPLACEMENT = String.format("/%s/$1_$3-$2/cite/bib", topic);
    ENDNOTE_REPLACEMENT = String.format("/%s/$1_$3-$2/cite/endnote", topic);
    PDF_REPLACEMENT = "/$1_$3.pdf?itemId=%2Fcontent%2Fpaper%2F$2-$3&mimeType=pdf";

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    builder.addAspect(LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // citation files
    builder.addAspect(
        RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);
    builder.addAspect(
        BIB_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_BIBTEX);
    builder.addAspect(
        ENDNOTE_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_ENDNOTE);

    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_HTML
    );

    builder.setRoleFromOtherRoles(
        ArticleFiles.ROLE_CITATION,
        Arrays.asList(
            ArticleFiles.ROLE_CITATION_RIS,
            ArticleFiles.ROLE_CITATION_BIBTEX,
            ArticleFiles.ROLE_CITATION_ENDNOTE
        )
    );

    return builder.getSubTreeArticleIterator();
  }


  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}