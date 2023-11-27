/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

public class OecdBooksArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(OecdBooksArticleIteratorFactory.class);

  private static final String PATTERN_TEMPLATE =
      "\"%s(%s%s|deliver/)\", base_url, pub_path, pub_id";

  // full text html
  // https://www.oecd-ilibrary.org/sites/6ef36f4b-en/index.html?itemId=/content/publication/6ef36f4b-en
  // full text pdf & epub
  // https://www.oecd-ilibrary.org/deliver/6ef36f4b-en.pdf?itemId=%2Fcontent%2Fpublication%2F6ef36f4b-en&mimeType=pdf
  // https://www.oecd-ilibrary.org/deliver/6ef36f4b-en.epub?itemId=%2Fcontent%2Fpublication%2F6ef36f4b-en&mimeType=epub
  // citation files
  // https://www.oecd-ilibrary.org/social-issues-migration-health/covid-19-and-well-being_6ef36f4b-en/cite/endnote

  // more complicated
  // https://www.oecd-ilibrary.org/nuclear-energy/the-economics-of-long-term-operation-of-nuclear-power-plants_9789264992054-en
  // https://www.oecd-ilibrary.org/the-economics-of-long-term-operation-of-nuclear-power-plants_5k409f793gls.pdf?itemId=%2Fcontent%2Fpublication%2F9789264992054-en&mimeType=pdf

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    String pub_path = au.getConfiguration().get("pub_path");
    String pub_id= au.getConfiguration().get("pub_id");

    // landing page
    // https://www.oecd-ilibrary.org/social-issues-migration-health/housing-and-inclusive-growth_6ef36f4b-en
    Pattern LANDING_PATTERN = Pattern.compile(
        String.format("/(%s(%s))$", pub_path, pub_id));
    String LANDING_REPLACEMENT = String.format("/%s$2", pub_path);

    String PDF_REPLACEMENT = "/deliver/$2.pdf?itemId=%2Fcontent%2Fpublication%2F$2&mimeType=pdf";
    //String EPUB_REPLACEMENT = "/deliver/$2.epub?itemId=%2Fcontent%2Fpublication%2F$2&mimeType=epub";
    //String HTML_REPLACEMENT = "/sites/$2/index.html?itemId=/content/publication/$2";

    // for LANDING replacement
    String RIS_REPLACEMENT = "/$1/cite/ris";
    String BIB_REPLACEMENT = "/$1/cite/bib";
    String ENDNOTE_REPLACEMENT = "/$1/cite/endnote";

    builder.setSpec(new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    builder.addAspect(
        LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    /*
    Not every article/book has epub
    builder.addAspect(
        EPUB_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_EPUB);

    builder.addAspect(
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    */

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
        //ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF
        //ArticleFiles.ROLE_FULL_TEXT_EPUB
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
    // use same metadataextractor as journals for finding pdf on page
    return new OecdArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA, true);
  }
}