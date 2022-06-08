/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.medknow;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * Medknow's Abstract page has links to Full-Text HTML and PDF, and also
 * contains metatdata. Medknow's file type:
 *      type=0  - Abstract
 *      no type - Full-Text HTML
 *      type=2  - Full-text PDF
 *      
 * also contains citation URLs:
 * "http://www.afrjpaedsurg.org/citation.asp?issn=0189-6725;year=2012;volume=9;
 * issue=1;spage=13;epage=16;aulast=Kothari;
 *      
 * full-text HTML URL:
 * "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;
 *  issue=1;spage=1;epage=2;aulast=Rutz"
 */
public class MedknowArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(MedknowArticleIteratorFactory.class);
  
  /* 
   * Because it's hard to uniquely identify something that DOESN'T
   * end with "type=X", have the iterator find the "type=0" (abstract)
   * and generate the others from this
   * NOTE - 5/14/18 - year can vary within a volume - some volumes straddle multiple years
   * the plugin still requires a single year parameter but we just accept any single year in the url
   */
  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = "\"^%sarticle\\.asp\\?issn=%s;year=[0-9]+;volume=%s;(.*;type=[02]$|(?!.+;type=).*aulast=.*)\", base_url, journal_issn, volume_name";
  
  // various aspects of an article
  // DOI's can have "/"s in the suffix
  private static final Pattern PDF_PATTERN = Pattern.compile("/(article\\.asp)(\\?.*);type=2$", Pattern.CASE_INSENSITIVE);
  private static final Pattern ABSTRACT_PATTERN = Pattern.compile("/(article\\.asp)(\\?.*);type=0$", Pattern.CASE_INSENSITIVE);
  private static final Pattern HTML_PATTERN = Pattern.compile("/(article\\.asp)(?!.+;type=)(\\?.*)$", Pattern.CASE_INSENSITIVE);

  // how to change from one form (aspect) of article to another
  private static final String HTML_REPLACEMENT = "/$1$2"; //take off the ";type=0" argument
  private static final String ABSTRACT_REPLACEMENT = "/$1$2;type=0";
  private static final String PDF_REPLACEMENT = "/$1$2;type=2";

  // citation landing page:
  private static final String CITATION_LANDING_REPLACEMENT = "/citation.asp$2";
  private static final String CITATION_RIS_REPLACEMENT = "/citeman.asp$2;t=2";
  

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);


    builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);


    // The order in which these aspects are added is important. They determine which will trigger
    // the ArticleFiles and if you are only counting articles (not pulling metadata) then the 
    // lower aspects aren't looked for, once you get a match.

    // set up full text html to be an aspect that will trigger an ArticleFiles
    builder.addAspect(HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA); // use for metadata if abstract doesn't exist

    // set up pdf to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // set up abstract to be an aspect that triggers an ArticleFiles
    // normally abstract wouldn't be sufficient, but it we can figure out the 
    // full-text html and the pdf from this, but it's hard to define
    // full-text html by itself (lack of "type" argument)
    builder.addAspect(ABSTRACT_PATTERN,
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(CITATION_RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);

    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(CITATION_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_CITATION);

    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_HTML);  

    // set the ROLE_ARTICLE_METADATA to the first one that exists 
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_CITATION_RIS);

    return builder.getSubTreeArticleIterator();
  }
  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

} /** end class MedknowArticleIteratorFactory */
