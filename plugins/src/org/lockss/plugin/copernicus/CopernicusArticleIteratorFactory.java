/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.copernicus;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Article lives at:  http://www.<base-url>/<volume>/<startpage#>/<year>/<alphanumericID>
 * <article>.html is the abstract
 * <article>.pdf is the full text pdf
 * * there might additionally be an <article>-supplement.pdf
 * <article>.bib, ris, xml are the citations
 */

public class CopernicusArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(CopernicusArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%s%s/\", base_url, volume_name"; 
  // although the format seems to be consistent, don't box in the alphanum sequence, just the depth
  // since we pick up ".pdf" as well, be sure not to pick up "-supplement.pdf" as well
  //(?<!-supplement) is negative lookbehind and will cancel out the *.pdf if it matches
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/[^/]+/[^/]+/[^/]+(?<!-supplement)\\.(html|pdf)\", base_url,volume_name";
  

  // primary aspects of the article
  final Pattern ABSTRACT_PATTERN = Pattern.compile("(/[^/]+/[^/]+/[^/]+)\\.html$", Pattern.CASE_INSENSITIVE);
  final Pattern PDF_PATTERN = Pattern.compile("(/[^/]+/[^/]+/[^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  final String ABSTRACT_REPLACEMENT = "$1.html";
  final String PDF_REPLACEMENT = "$1.pdf";
  // secondary aspect replacements
  final String XML_REPLACEMENT = "$1.xml";
  final String SUPPL_REPLACEMENT = "$1-supplement.pdf";
  final String SUPPL_ZIP_REPLACEMENT = "$1-supplement.zip";
  final String RIS_REPLACEMENT = "$1.ris";
  final String BIB_REPLACEMENT = "$1.bib";
  
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
      SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
      
      builder.setSpec(target,
          ROOT_TEMPLATE,
          PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
      
      // The order in which these aspects are added is important. They determine which will trigger
      // the ArticleFiles and if you are only counting articles (not pulling metadata) then the 
      // lower aspects aren't looked for, once you get a match.

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
          ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
          ArticleFiles.ROLE_ARTICLE_METADATA);

      // set a role, but it isn't sufficient to trigger an ArticleFiles
      builder.addAspect(XML_REPLACEMENT,
          ArticleFiles.ROLE_FULL_TEXT_XML);

      // set a role, but it isn't sufficient to trigger an ArticleFiles
      builder.addAspect(SUPPL_REPLACEMENT,
          ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
      // set a role, but it isn't sufficient to trigger an ArticleFiles
      builder.addAspect(SUPPL_ZIP_REPLACEMENT,
          ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
      
      // set a role, but it isn't sufficient to trigger an ArticleFiles
      builder.addAspect(RIS_REPLACEMENT,
          ArticleFiles.ROLE_ARTICLE_METADATA,
          ArticleFiles.ROLE_CITATION_RIS);

      // set a role, but it isn't sufficient to trigger an ArticleFiles
      builder.addAspect(BIB_REPLACEMENT,
          ArticleFiles.ROLE_CITATION_BIBTEX);

      // The order in which we want to define full_text_cu.  
      // First one that exists will get the job
      builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
      ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);  

      // set the ROLE_ARTICLE_METADATA to the first one that exists 
      builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
          ArticleFiles.ROLE_CITATION_RIS,
          ArticleFiles.ROLE_ABSTRACT);

      return builder.getSubTreeArticleIterator();
    }

  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
