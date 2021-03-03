/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon;

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


/*
 * Atypon Book and Journals could actually share an iterator because the underlying item
 * layout is the same - doi/(pdf|abs|pdfplus|full)/doi1/doi2...
 * except that, at least for now, we only want to emit metadata for the entire book,
 * even when chapters are also provided individually. We collect everything.
 * The book landing page is /doi/book/doi1/doiFOO
 * and the full book pdf will be at  /doi/pdf/doi1/doiFOO whereas 
 * individual chapters will live at doi/pdf/doi1/someotherdoi2 
 * It seemed cleaner to create the limited fullbook iterator and leave support for books AND chapters
 * as part of the general article iterator for when/if we choose that route
 */
public class BaseAtyponFullBookArticleIteratorFactory
implements ArticleIteratorFactory,
ArticleMetadataExtractorFactory {

  protected static final Logger log = 
      Logger.getLogger(BaseAtyponFullBookArticleIteratorFactory.class);

  private static final String ROLE_PDFPLUS = "PdfPlus";

  private static final String ROOT_TEMPLATE = "\"%sdoi/book/\", base_url";

  
  private static final String PATTERN_TEMPLATE = 
      "\"^%sdoi/book/[.0-9]+/\", base_url";

  // various aspects of an article
  // DOI's can have "/"s in the suffix
  private static final Pattern BOOK_LANDING_PATTERN = Pattern.compile("/doi/book/([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);

  // how to change from the book landing to alternative aspects of the book
  private static final String BOOK_REPLACEMENT = "/doi/book/$1/$2"; //self
  private static final String PDF_REPLACEMENT = "/doi/pdf/$1/$2"; //pdf
  private static final String PDFPLUS_REPLACEMENT = "/doi/pdfplus/$1/$2"; //not seen, but possible
  private static final String HTML_REPLACEMENT = "/doi/full/$1/$2"; //not seen, but possible
  private static final String ABSTRACT_REPLACEMENT = "/doi/abs/$1/$2"; //not seen, but possible
  private static final String SUPPL_REPLACEMENT = "/doi/suppl/$1/$2"; //not seen, but possible

  /* TODO: Note that if the DOI suffix has a "/" this will not work because the 
   * slashes that are part of the DOI will not get encoded so they don't
   * match the CU.  Waiting for builder support for smarter replacement
   * Taylor & Francis works around this because it has current need
   */
  // After normalization, the citation information will live at this URL if it exists
  private static final String RIS_REPLACEMENT = "/action/downloadCitation?doi=$1%2F$2&format=ris&include=cit";
  // AMetSoc doens't do an "include=cit", only "include=abs"
  // Do these as two separate patterns (not "OR") so we can have a priority choice
  private static final String SECOND_RIS_REPLACEMENT = "/action/downloadCitation?doi=$1%2F$2&format=ris&include=abs";


  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);


      builder.setSpec(target,
          ROOT_TEMPLATE,
          PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
   

    // The order in which these aspects are added is important. They determine which will trigger
    // the ArticleFiles and if you are only counting articles (not pulling metadata) then the 
    // lower aspects aren't looked for, once you get a match.

    // set up book landing to be the ONLY aspect that will trigger an ArticleFiles
    builder.addAspect(BOOK_LANDING_PATTERN,
        BOOK_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    

    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF); 

    builder.addAspect(
        PDFPLUS_REPLACEMENT,
        ROLE_PDFPLUS); 

    builder.addAspect(
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA); // use for metadata if abstract doesn't exist

      builder.addAspect(
          ABSTRACT_REPLACEMENT,
          ArticleFiles.ROLE_ABSTRACT,
          ArticleFiles.ROLE_ARTICLE_METADATA);

    // First choice is &include=cit; second choice is &include=abs (AMetSoc)
    builder.addAspect(Arrays.asList(
        RIS_REPLACEMENT, SECOND_RIS_REPLACEMENT),
        ArticleFiles.ROLE_CITATION_RIS);

    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    // The last one isn't technically full text, but it will only happen if it's all we've got
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ROLE_PDFPLUS,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);  

    // The order in which we want to define what a PDF is 
    // if we only have PDFPLUS, that should become a FULL_TEXT_PDF
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ROLE_PDFPLUS); // this should be ROLE_PDFPLUS when it's defined

    // set the ROLE_ARTICLE_METADATA to the first one that exists 
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_CITATION_RIS,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    return builder.getSubTreeArticleIterator();
  }

  // Enclose the method that creates the builder to allow a child to do additional processing
  // for example Taylor&Francis
  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
    return new SubTreeArticleIteratorBuilder(au);
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
