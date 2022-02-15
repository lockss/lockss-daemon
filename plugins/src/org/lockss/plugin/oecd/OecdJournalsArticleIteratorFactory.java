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
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class OecdJournalsArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(OecdJournalsArticleIteratorFactory.class);

  private static final String PATTERN_TEMPLATE =
      "\"%s%s/.*(_|/volume)\", base_url, topic";

  // whole issue, that sometimes is all there is
  // https://www.oecd-ilibrary.org/nuclear-energy/nuclear-law-bulletin/volume-2002/issue-2_nuclear_law-v2002-2-en

  // https://www.oecd-ilibrary.org/economics/frequency-based-co-movement-of-inflation-in-selected-euro-area-countries_jbcma-2015-5jm26ttlxdd1
  // https://www.oecd-ilibrary.org/governance/budgeting-practices-to-improve-health-system-performance_2fc826dd-en
  // https://www.oecd-ilibrary.org/governance/public-private-partnerships-review-of-kazakhstan_f7696c94-en
  // https://www.oecd-ilibrary.org/economics/a-generalized-dynamic-factor-model-for-the-belgian-economy_jbcma-v2005-art4-en

  private static Pattern LANDING_ARTICLE_PATTERN;
  private static Pattern LANDING_ISSUE_PATTERN;
  // https://www.oecd-ilibrary.org/frequency-based-co-movement-of-inflation-in-selected-euro-area-countries_5jm26ttlxdd1.pdf?itemId=%2Fcontent%2Fpaper%2Fjbcma-2015-5jm26ttlxdd1&mimeType=pdf
  // https://www.oecd-ilibrary.org/deliver/f7696c94-en.pdf?itemId=%2Fcontent%2Fpaper%2Ff7696c94-en&mimeType=pdf
  // https://www.oecd-ilibrary.org/a-generalized-dynamic-factor-model-for-the-belgian-economy_5l4th8x7mb36.pdf?itemId=%2Fcontent%2Fpaper%2Fjbcma-v2005-art4-en&mimeType=pdf

  // issue pdf
  // https://www.oecd-ilibrary.org/nuclear-law-bulletin-volume-2002-issue-2_5lmqcr2kd2ln.pdf?itemId=%2Fcontent%2Fpublication%2Fnuclear_law-v2002-2-en&mimeType=pdf
  //private static final Pattern PDF_PATTERN =
  //    Pattern.compile("(?<!deliver)/([^_/]+)(_.+)?\\.pdf\\?itemId=%2Fcontent%2Fpaper%2F(.+)&mimeType=pdf$");

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    String topic = au.getConfiguration().get("topic");

    LANDING_ARTICLE_PATTERN = Pattern.compile(
        String.format("/(%s/([^/]+)_(([^-]+-[^-]+)-([^/]+(-en)?)|[^-]+-en))$", topic));

    LANDING_ISSUE_PATTERN = Pattern.compile(
        String.format("/(%s/([^/]+)/(volume[^/]+)/((issue-[^-]+-)?([^/]+)))$", topic));

    String PDF_REPLACEMENT = "/$2_$5.pdf?itemId=%2Fcontent%2Fpaper%2F$3&mimeType=pdf";
    String PDF_REPLACEMENT2 = "/deliver/$3.pdf?itemId=%2Fcontent%2Fpaper%2F$3&mimeType=pdf";

    // for PDF_PATTERN replacement
    String LANDING_REPLACEMENT = String.format("/%s/$1_$3", topic);
    //String RIS_REPLACEMENT = String.format("%s/cite/ris", LANDING_REPLACEMENT);
    //String BIB_REPLACEMENT = String.format("%s/cite/bib", LANDING_REPLACEMENT);
    //String ENDNOTE_REPLACEMENT = String.format("%s/cite/endnote", LANDING_REPLACEMENT);

    // for LANDING replacement
    String RIS_REPLACEMENT = "/$1/cite/ris";
    String BIB_REPLACEMENT = "/$1/cite/bib";
    String ENDNOTE_REPLACEMENT = "/$1/cite/endnote";

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));


    builder.addAspect(
        Arrays.asList(
            LANDING_ARTICLE_PATTERN,
            LANDING_ISSUE_PATTERN
        ),
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(//PDF_PATTERN,
        Arrays.asList(
            PDF_REPLACEMENT,
            PDF_REPLACEMENT2
        ),
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
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF
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
    return new OecdArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA, false);
  }
}