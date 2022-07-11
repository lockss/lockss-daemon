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

package org.lockss.plugin.highwire.bmj;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class BMJJCoreArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(BMJJCoreArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%scontent/\", base_url";
  
  // pattern has vol/page as well as older style vip (vol/issue/page)
  // Cannot use volume_name in vol/page pattern as the BMJ mixes articles
  //        (ie. /346/bmj.f4217 in vol 347, issue 7915)
  protected static final String PATTERN_TEMPLATE =
    "\"^%scontent/([^/]{1,4}/bmj[.](?:[^./?&]+)|%s/[^/]+/(?![^/]+[.](full|alerts|altmetrics|citation|info|responses|share))[^/?]+)$\", base_url, volume_name";
  
  // various aspects of an article
  // http://www.bmj.com/content/345/bmj.e7558
  // http://www.bmj.com/content/325/7373/1156
  //
  // http://www.bmj.com/content/345/bmj.e7558.full.pdf
  // http://www.bmj.com/content/345/bmj.f7558.full.pdf+html
  //
  // http://www.bmj.com/content/325/7373/1156.full.pdf+html
  // http://www.bmj.com/content/325/7373/1156.full-text.print (not preserved)

  // https://bmj.com/content/362/bmj.k4007
  // https://bmj.com/content/362/bmj.k4007/peer-review
  // https://bmj.com/content/362/bmj.k4007/rapid-responses
  // https://bmj.com/content/362/bmj.k4007.full.pdf
  // https://bmj.com/content/362/bmj.k4007.full.txt

  // https://www.bmj.com/content/362/bmj.k4007
  // https://www.bmj.com/content/362/bmj.k4007/rapid-responses
  // https://www.bmj.com/content/362/bmj.k4007.full.pdf

  // Note: double wrap the secodn capture group to allow substitution to work for both pattern-replacements
  protected static final Pattern VOL_PAGEID_PATTERN = Pattern.compile(
      "/content/([^/]{1,4})/((bmj[.][^./?&]+))$", Pattern.CASE_INSENSITIVE);
  protected static final Pattern VIP_PATTERN = Pattern.compile(
      "/content/([^/]+/(?!bmj[.])[^/]+)/(?!.*[.](full|abstract))([^/?]+)$", Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String LANDING_PAGE_REPLACEMENT = "/content/$1/$3";
  protected static final String PDF_REPLACEMENT = "/content/$1/$3.full.pdf";
  protected static final String PDF_LANDING_REPLACEMENT = "/content/$1/$3.full.pdf+html";
  protected static final String ABSTRACT_REPLACEMENT = "/content/$1/$3.abstract";
  
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    // set up vol/pageid & vip to be aspects that will trigger an ArticleFiles
    // NOTE - for the moment this also means they are considered a FULL_TEXT_CU
    // until this is deprecated
    builder.addAspect(
        Arrays.asList(VOL_PAGEID_PATTERN, VIP_PATTERN),
        LANDING_PAGE_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);
    
    builder.addAspect(
        PDF_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
