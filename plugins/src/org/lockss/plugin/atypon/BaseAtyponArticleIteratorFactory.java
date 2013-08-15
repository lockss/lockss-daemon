/*
 * $Id: BaseAtyponArticleIteratorFactory.java,v 1.4 2013-08-15 19:25:27 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.atypon;

import java.util.Iterator;
import java.util.regex.Pattern;

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
      Logger.getLogger("BaseAtyponArticleIteratorFactory");

  protected static final String ROOT_TEMPLATE = "\"%sdoi/\", base_url";
 
  protected static final String PATTERN_TEMPLATE = 
      "\"^%sdoi/(abs|full|pdf|pdfplus)/[.0-9]+/\", base_url";
  
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
  //  There is the possibility of downloaded citation information which will get normalized to look something like this:
  //  <atyponbase>.org/action/downloadCitation?doi=<partone>%2F<parttwo>&format=ris&include=cit
  //

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);
    
    // various aspects of an article
    final Pattern PDF_PATTERN = Pattern.compile("/doi/pdf/([.0-9]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
    final Pattern ABSTRACT_PATTERN = Pattern.compile("/doi/abs/([.0-9]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
    final Pattern HTML_PATTERN = Pattern.compile("/doi/full/([.0-9]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
    final Pattern PDFPLUS_PATTERN = Pattern.compile("/doi/pdfplus/([.0-9]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);

    // how to change from one form (aspect) of article to another
    final String HTML_REPLACEMENT = "/doi/full/$1/$2";
    final String ABSTRACT_REPLACEMENT = "/doi/abs/$1/$2";
    final String PDF_REPLACEMENT = "/doi/pdf/$1/$2";
    final String PDFPLUS_REPLACEMENT = "/doi/pdfplus/$1/$2";
    
    // Things not an "article" but in support of an article
    final String REFERENCES_REPLACEMENT = "/doi/ref/$1/$2";
    final String SUPPL_REPLACEMENT = "/doi/suppl/$1/$2";
    // link extractor used forms to pick up this URL
    
    // After normalization, the citation information will live at this URL if it exists
    final String RIS_REPLACEMENT = "/action/downloadCitation?doi=$1%2F$2&format=ris&include=cit";

    
    builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // set up Abstract to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means an abstract could be considered a FULL_TEXT_CU until this is deprecated
    // though the ordered list for role full text will mean if any of the others are there, they will become the FTCU
    builder.addAspect(ABSTRACT_PATTERN,
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // set up full text html to be an aspect that will trigger an ArticleFiles
    builder.addAspect(HTML_PATTERN,
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA); // use for metadata if abstract doesn't exist

    // set up PDFPLUS to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDFPLUS_PATTERN,
        PDFPLUS_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE); //hmmm... only want this one to be this if pdf doesn't exist
    /* ArticleFiles.ROLE_FULL_TEXT_PDFPLUS); */ // this should be ROLE_PDFPLUS when it's defined

    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(REFERENCES_REPLACEMENT,
        ArticleFiles.ROLE_REFERENCES);

    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(SUPPL_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
    
    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);

    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
    ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);  // this should be ROLE_PDFPLUS when it's defined

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
