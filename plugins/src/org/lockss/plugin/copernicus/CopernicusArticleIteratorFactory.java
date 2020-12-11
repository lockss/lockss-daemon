/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.copernicus;

import java.util.Arrays;
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
 * * there might additionally be an <article>-corrigendum.pdf
 * <article>.bib, ris, xml are the citations
 *
 *
 */

  /*
  website update url pattern in 2020
  https://acp.copernicus.org/articles/20/14889/2020
  https://acp.copernicus.org/articles/20/14889/2020/acp-20-14889-2020-supplement.pd
  https://acp.copernicus.org/articles/20/14889/2020/acp-20-14889-2020-t01.xlsx
  https://acp.copernicus.org/articles/20/14889/2020/acp-20-14889-2020-t02.xlsx
  https://acp.copernicus.org/articles/20/14889/2020/acp-20-14889-2020.bib
  https://acp.copernicus.org/articles/20/14889/2020/acp-20-14889-2020.html
  https://acp.copernicus.org/articles/20/14889/2020/acp-20-14889-2020.pdf
  https://acp.copernicus.org/articles/20/14889/2020/acp-20-14889-2020.ris
  https://acp.copernicus.org/articles/20/14889/2020/acp-20-14889-2020.xml
   */

public class CopernicusArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(CopernicusArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s%s/\", base_url, volume_name";
  protected static final String ROOT_TEMPLATE_V2 = "\"%sarticles/%s/\", base_url, volume_name";
  // although the format seems to be consistent, don't box in the alphanum sequence, just the depth
  // since we pick up ".pdf" as well, be sure not to pick up "-supplement.pdf", nor "-assets.html" as well
  //(?<!-supplement) is negative lookbehind and will cancel out the *.pdf if it matches
  // Use pdf here, since there are other formats of html page we do not want
  protected static final String PATTERN_TEMPLATE = "\"/[^/]+/[^/]+/[^/]+(?<!-(supplement|assets|corrigendum))\\.pdf\"";

  // primary aspects of the article
  final Pattern ABSTRACT_PATTERN = Pattern.compile("(/[^/]+/[^/]+/[^/]+)\\.html$", Pattern.CASE_INSENSITIVE);
  final Pattern PDF_PATTERN = Pattern.compile("(/[^/]+/[^/]+/[^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  final String ABSTRACT_REPLACEMENT = "$1.html";
  final String PDF_REPLACEMENT = "$1.pdf";
  // secondary aspect replacements
  final String XML_REPLACEMENT = "$1.xml";
  final String SUPPL_REPLACEMENT = "$1-supplement.pdf";
  final String CORRIG_REPLACEMENT = "$1-corrigendum.pdf";
  final String ASSETS_REPLACEMENT = "$1-assets.html";
  final String SUPPL_ZIP_REPLACEMENT = "$1-supplement.zip";
  final String RIS_REPLACEMENT = "$1.ris";
  final String BIB_REPLACEMENT = "$1.bib";
  
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
      SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
      
      builder.setSpec(target,
              Arrays.asList(ROOT_TEMPLATE, ROOT_TEMPLATE_V2),
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
      builder.addAspect(
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
      builder.addAspect(CORRIG_REPLACEMENT,
          "ArticleCorrigendum");
      
      // set a role, but it isn't sufficient to trigger an ArticleFiles
      // this is an html page that contains supporting article references
      // but isn't the references page
      builder.addAspect(ASSETS_REPLACEMENT,
          "ArticleAssets");
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
