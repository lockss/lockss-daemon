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

import org.lockss.config.TdbAu;
import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class BaseAtyponArticleIteratorFactory
implements ArticleIteratorFactory,
ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(BaseAtyponArticleIteratorFactory.class);

  //arbitrary string is used in daemon for this  
  private static final String ABSTRACTS_ONLY = "abstracts";  
  private static final String ROLE_PDFPLUS = "PdfPlus";
  private static final String ROLE_DOIHTML = "DOIHtml";

  private static final String ROOT_TEMPLATE = "\"%sdoi/\", base_url";

  //2017+ ASCE provided only an aspect free link to article landing page
  // http://ascelibrary.org/doi/10.1061/%28ASCE%29ME.1943-5479.0000092
  // For now keep this as a unique iterator

  
  // Only put the 'abs' in the pattern if used for primary; otherwise builder spews errors
  private static final String DEFAULT_PATTERN_TEMPLATE_WITH_ABSTRACT = 
      "\"^%sdoi/((abs|full|e?pdf|e?pdfplus)/)?[.0-9]+/\", base_url";
  private static final String DEFAULT_PATTERN_TEMPLATE = 
      "\"^%sdoi/((full|e?pdf|e?pdfplus)/)?[.0-9]+/\", base_url";

  // various aspects of an article
  // DOI's can have "/"s in the suffix
  private static final Pattern PDF_PATTERN = Pattern.compile("/doi/pdf/([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EPDF_PATTERN = Pattern.compile("/doi/epdf/([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern ABSTRACT_PATTERN = Pattern.compile("/doi/abs/([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern HTML_PATTERN = Pattern.compile("/doi/full/([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern PDFPLUS_PATTERN = Pattern.compile("/doi/pdfplus/([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EPDFPLUS_PATTERN = Pattern.compile("/doi/epdfplus/([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern DOI_PATTERN = Pattern.compile("/doi/([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);

  // how to change from one form (aspect) of article to another
  private static final String HTML_REPLACEMENT = "/doi/full/$1/$2";
  private static final String ABSTRACT_REPLACEMENT = "/doi/abs/$1/$2";
  private static final String PDF_REPLACEMENT = "/doi/pdf/$1/$2";
  private static final String PDFPLUS_REPLACEMENT = "/doi/pdfplus/$1/$2";
  private static final String EPDF_REPLACEMENT = "/doi/epdf/$1/$2";
  private static final String EPDFPLUS_REPLACEMENT = "/doi/epdfplus/$1/$2";
  // in support of books, this is equivalent of full book abstract (landing page)
  private static final String BOOK_REPLACEMENT = "/doi/book/$1/$2";
  private static final String DOI_REPLACEMENT = "/doi/$1/$2";

  // Things not an "article" but in support of an article
  private static final String REFERENCES_REPLACEMENT = "/doi/ref/$1/$2";
  private static final String SUPPL_REPLACEMENT = "/doi/suppl/$1/$2";
  // MassMedical uses this second form for SUPPL materials
  private static final String SECOND_SUPPL_REPLACEMENT = "/action/showSupplements?doi=$1%2F$2";
  // link extractor used forms to pick up this URL

  /* TODO: Note that if the DOI suffix has a "/" this will not work because the 
   * slashes that are part of the DOI will not get encoded so they don't
   * match the CU.  Waiting for builder support for smarter replacement
   * Taylor & Francis works around this because it has current need
   */
  // After normalization, the citation information will live at this URL if it exists
  private static final String RIS_REPLACEMENT = "/action/downloadCitation?doi=$1%2F$2&format=ris&include=cit";
  private static final String RIS_REPLACEMENT_WSLASH = "/action/downloadCitation?doi=$1/$2&format=ris&include=cit";
  // AMetSoc doens't do an "include=cit", only "include=abs"
  // Do these as two separate patterns (not "OR") so we can have a priority choice
  private static final String SECOND_RIS_REPLACEMENT = "/action/downloadCitation?doi=$1%2F$2&format=ris&include=abs";
  private static final String SECOND_RIS_REPLACEMENT_WSLASH = "/action/downloadCitation?doi=$1%/$2&format=ris&include=abs";


  //
  // On an Atypon publisher, article content may look like this but you do not know
  // how many of the aspects will exist for a particular journal
  //
  //  <atyponbase>.org/doi/abs/10.3366/drs.2011.0010 (abstract or summary)
  //  <atyponbase>.org/doi/full/10.3366/drs.2011.0010 (full text html)
  //  <atyponbase>.org/doi/pdf/10.3366/drs.2011.0010 (full text pdf)
  //  <atyponbase>.org/doi/pdfplus/10.3366/drs.2011.0010  (fancy pdf - could be in frameset or could have active links)
  //  <atyponbase>.org/doi/suppl/10.3366/drs.2011.0010 (page from which you can access supplementary info)
  //  <atyponbase>.org/doi/ref/10.3366/drs.2011.0010  (page with references on it)
  //
  // note: at least one publisher has a doi suffix that includes a "/", eg:
  // t&f,writing systems research - vol3, issue2 
  // <base>/doi/pdfplus/10.1093/wsr/wsr0023
  //
  //  There is the possibility of downloaded citation information which will get normalized to look something like this:
  //  <atyponbase>.org/action/downloadCitation?doi=<partone>%2F<parttwo>&format=ris&include=cit
  //

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);

    if (isAbstractOnly(au)) {
      builder.setSpec(target,
          ROOT_TEMPLATE,
          getPatternWithAbstractTemplate(), Pattern.CASE_INSENSITIVE);
    } else {
      builder.setSpec(target,
          ROOT_TEMPLATE,
          getPatternTemplate(), Pattern.CASE_INSENSITIVE);
    }

    // The order in which these aspects are added is important. They determine which will trigger
    // the ArticleFiles and if you are only counting articles (not pulling metadata) then the 
    // lower aspects aren't looked for, once you get a match.

    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(EPDF_PATTERN,
            EPDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);


    // set up PDFPLUS to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDFPLUS_PATTERN,
        PDFPLUS_REPLACEMENT,
        ROLE_PDFPLUS);

    builder.addAspect(EPDFPLUS_PATTERN,
            EPDFPLUS_REPLACEMENT,
            ROLE_PDFPLUS);

    // set up full text html to be an aspect that will trigger an ArticleFiles
    builder.addAspect(HTML_PATTERN,
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA); // use for metadata if abstract doesn't exist

    // set up full text html to be an aspect that will trigger an ArticleFiles
    // for ASCE, this is the full-text HTML now
    builder.addAspect(DOI_PATTERN,
        DOI_REPLACEMENT,
        ROLE_DOIHTML, // just a local name until we know what we have to chose from
        ArticleFiles.ROLE_ARTICLE_METADATA); // use for metadata if abstract doesn't exist
    
    if (isAbstractOnly(au)) {
      // When part of an abstract only AU, set up an abstract to be an aspect
      // that will trigger an articleFiles. 
      // This also means an abstract could be considered a FULL_TEXT_CU until this is deprecated
      builder.addAspect(ABSTRACT_PATTERN,
          ABSTRACT_REPLACEMENT,
          ArticleFiles.ROLE_ABSTRACT,
          ArticleFiles.ROLE_FULL_TEXT_HTML,
          ArticleFiles.ROLE_ARTICLE_METADATA);
    } else {
      // If this isn't an "abstracts only" AU, an abstract alone should not
      // be enough to trigger an ArticleFiles
      builder.addAspect(ABSTRACT_REPLACEMENT,
          ArticleFiles.ROLE_ABSTRACT,
          ArticleFiles.ROLE_ARTICLE_METADATA);
    }

    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(BOOK_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);

    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(REFERENCES_REPLACEMENT,
        ArticleFiles.ROLE_REFERENCES);

    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(Arrays.asList(
        SUPPL_REPLACEMENT, SECOND_SUPPL_REPLACEMENT),
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);

    // set a role, but it isn't sufficient to trigger an ArticleFiles
    // First choice is &include=cit; second choice is &include=abs (AMetSoc)
    builder.addAspect(Arrays.asList(
        RIS_REPLACEMENT, RIS_REPLACEMENT_WSLASH, SECOND_RIS_REPLACEMENT, SECOND_RIS_REPLACEMENT_WSLASH),
        ArticleFiles.ROLE_CITATION_RIS);

    // The order in which we want to define what a full text HTML 
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_FULL_TEXT_HTML,
        ROLE_DOIHTML); // in ASCE it's the only full-text html we get

    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    // For AUs that are all or partially abstract only, add in this option but
    // leave the full-text as the priorities
    if (isAbstractOnly(au)) {
      builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
          ArticleFiles.ROLE_FULL_TEXT_HTML,
          ROLE_PDFPLUS,
          ArticleFiles.ROLE_ABSTRACT);
    } else {
      builder.setFullTextFromRoles(
          ArticleFiles.ROLE_FULL_TEXT_HTML,
          ROLE_DOIHTML,
          ArticleFiles.ROLE_FULL_TEXT_PDF,
          ROLE_PDFPLUS);
    }

    // The order in which we want to define what a PDF is 
    // if we only have PDFPLUS, that should become a FULL_TEXT_PDF
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ROLE_PDFPLUS); // this should be ROLE_PDFPLUS when it's defined


    // set the ROLE_ARTICLE_METADATA to the first one that exists 
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_CITATION_RIS,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML); // this could be doi/full/xxx or doi/xxx

    return builder.getSubTreeArticleIterator();
  }

  // Enclose the method that creates the builder to allow a child to do additional processing
  // for example Taylor&Francis
  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
    return new SubTreeArticleIteratorBuilder(au);
  }
  
  //Use a getter for the pattern and pattern-with-abstract templates so a child can override
  protected String getPatternTemplate() {
	  return DEFAULT_PATTERN_TEMPLATE;
  }
  protected String getPatternWithAbstractTemplate() {
	  return DEFAULT_PATTERN_TEMPLATE_WITH_ABSTRACT;
  }
  

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

  // return true if the AU is of type "abstracts"
  private static boolean isAbstractOnly(ArchivalUnit au) {
    TdbAu tdbAu = au.getTdbAu();
    return tdbAu != null && ABSTRACTS_ONLY.equals(tdbAu.getCoverageDepth());
  }

}
