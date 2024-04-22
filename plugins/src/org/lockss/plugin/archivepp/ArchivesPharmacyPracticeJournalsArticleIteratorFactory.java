/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.archivepp;

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

public class ArchivesPharmacyPracticeJournalsArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  //https://archivepp.com/article/authorship-criteria-and-ethical-requirements-for-publishing-in-archives-of-pharmacy-practice
  //https://archivepp.com/storage/models/article/ljBbbET4WSruDmEha9SeMlUpAonZNERUqpKOttXw9pX3H159Wx6O2k6Oqy21/authorship-criteria-and-ethical-requirements-for-publishing-in-archives-of-pharmacy-practice.pdf
  //https://archivepp.com/article/authorship-criteria-and-ethical-requirements-for-publishing-in-archives-of-pharmacy-practice?download_citation=ris

  private static final Logger log = Logger.getLogger(ArchivesPharmacyPracticeJournalsArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%s\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
    "\"%s(storage/models/)?article\", base_url";

  // various aspects of an article
  protected static final Pattern ARTICLE_LANDING_PAGE_PATTERN = Pattern.compile(
          "/article/([a-zA-Z0-9\\-]+)$", Pattern.CASE_INSENSITIVE);
  protected static final Pattern RIS_PAGE_PATTERN = Pattern.compile(
      "/article/([a-zA-Z0-9\\-]+\\?download_citation=ris)", Pattern.CASE_INSENSITIVE);


  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/storage/models/article/([^/]+/[^/]+)", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String ARTICLE_LANDING_PAGE_REPLACEMENT = "$1";
  protected static final String RIS_REPLACEMENT = "$1";
  protected static final String PDF_REPLACEMENT = "$1";
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    // set up abstract page to be an aspect
    builder.addAspect(
            ARTICLE_LANDING_PAGE_PATTERN,
            ARTICLE_LANDING_PAGE_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_HTML,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
            PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(
            RIS_PAGE_PATTERN,
            RIS_REPLACEMENT,
            ArticleFiles.ROLE_CITATION_RIS);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
