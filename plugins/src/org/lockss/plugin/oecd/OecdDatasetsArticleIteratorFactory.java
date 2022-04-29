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

public class OecdDatasetsArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(OecdDatasetsArticleIteratorFactory.class);

  /*
    https://www.oecd-ilibrary.org/social-issues-migration-health/data/oecd-social-and-welfare-statistics/benefits-and-wages-adequacy-of-guaranteed-minimum-income-benefits-edition-2019_1d5f37e2-en
    https://www.oecd-ilibrary.org/benefits-and-wages-adequacy-of-guaranteed-minimum-income-benefits-edition-2019_1d5f37e2-en.zip?itemId=%2Fcontent%2Fdata%2F1d5f37e2-en&containerItemId=%2Fcontent%2Fcollection%2Fsocwel-data-en
    https://www.oecd-ilibrary.org/social-issues-migration-health/data/oecd-social-and-welfare-statistics/benefits-and-wages-adequacy-of-guaranteed-minimum-income-benefits-edition-2019_1d5f37e2-en/cite/ris
   */

  private static final String PATTERN_TEMPLATE =
      "\"%s.*(/data/|\\.zip\\?itemId=%%2Fcontent%%2Fdata%%2F.*&amp;containerItemId=).*\", base_url";

  private static Pattern LANDING_PATTERN = Pattern.compile("/(.*/data.*)/([^/]+)_([^/]+)$");
  private static final String LANDING_REPLACEMENT = "/$1";

  private static Pattern ZIP_PATTERN = Pattern.compile("/(.+)\\.zip\\?itemId=%2Fcontent%2Fdata%2F(.*)&amp;containerItemId=(.*)");
  private static final String ZIP_REPLACEMENT = "/$2_$3.zip?itemId=%2Fcontent%2Fdata%2F$3&amp;containerItemId=%2Fcontent%2Fcollection%2Fsocwel-data-en";

  private static final String RIS_REPLACEMENT = "/$1/cite/ris";
  private static final String BIB_REPLACEMENT = "/$1/cite/bib";
  private static final String ENDNOTE_REPLACEMENT = "/$1/cite/endnote";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    // make this one primary by defining it first
    builder.addAspect(ZIP_PATTERN,
        ZIP_REPLACEMENT,
        ArticleFiles.ROLE_TABLES);

    builder.addAspect(LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_ARTICLE_METADATA);
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
        ArticleFiles.ROLE_TABLES
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