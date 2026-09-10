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

package org.lockss.plugin.americanmathematicalsociety;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class AmericanMathematicalSocietyArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {
  
  protected static Logger log = 
      Logger.getLogger(AmericanMathematicalSocietyArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE_BASE =
      "\"%sjournals/%s/\", to_https(base_url), journal_id";
  protected static final String ROOT_TEMPLATE_NEW =
      "\"https://pubs.ams.org/%s/\", journal_id";
    
  protected static final String PATTERN_TEMPLATE =
      "\"^%sjournals/%s/%d-[0-9-]+/([^/?&.]+)(?:/\\1[.]pdf|/viewer|/?\\?active=current)?$\", to_https(base_url), journal_id, year";
  
  /*
    various files
      html - https://www.ams.org/journals/bull/2023-60-04/S0273-0979-2023-01805-3/viewer
      pdf - https://www.ams.org/journals/bull/2023-60-04/S0273-0979-2023-01805-3/S0273-0979-2023-01805-3.pdf
      abstract - https://www.ams.org/journals/bull/2023-60-04/S0273-0979-2023-01805-3/?active=current

      UPDATED 2026, note the different base URLs:
      html - https://www.ams.org/journals/jams/2020-33-02/S0894-0347-2019-00935-5/viewer
      pdf - https://www.ams.org/journals/jams/2020-33-02/S0894-0347-2019-00935-5/S0894-0347-2019-00935-5.pdf
      abstract - https://pubs.ams.org/JAMS/2020-33-02/S0894-0347-2019-00935-5
   */
  
  final String NEW_BASE_URL = "https://pubs.ams.org/";

  final Pattern PDF_PATTERN = Pattern.compile(
      "^(https://[^/]+/)journals/([^/]+/[0-9-]+)/([^/?.]+)/\\3[.]pdf$",
      Pattern.CASE_INSENSITIVE);
  final String PDF_REPLACEMENT = "$1journals/$2/$3/$3.pdf";
  final String HTML_REPLACEMENT = "$1journals/$2/$3/viewer";
  final String ABSTRACT_REPLACEMENT = NEW_BASE_URL + "$2/$3";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        Arrays.asList(ROOT_TEMPLATE_BASE, ROOT_TEMPLATE_NEW), PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    builder.addAspect(
        PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML, ArticleFiles.ROLE_FULL_TEXT_PDF);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
