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

package org.lockss.plugin.clockss.sagebaywood;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 *  Although this uses the SourceXmlMetadataExtractor framework, it is a harvest plugin,
 *  so the ArticleIterator will find the ".pdf" files if they exist
 *  it saves the step of calculating the pdf file during metadata extraction
 */
public class SageBaywoodTriggeredArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static final Logger log = Logger.getLogger(SageBaywoodTriggeredArticleIteratorFactory.class);
  
  public static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final String XML_REPLACEMENT = "/$1.xml";
  public static final Pattern PDF_PATTERN = Pattern.compile("/(.*)\\.pdf$", Pattern.CASE_INSENSITIVE);
  public static final String PDF_REPLACEMENT = "/$1.pdf";
  public static final String ABS_REPLACEMENT = "/$1.html";
  
  // all XML files found under the current JID and VOLUME
  protected static final String PATTERN_TEMPLATE = "\"%s%s/BAWOOD_%s_%s_[\\d]/.*\\.(xml|pdf|html)$\",base_url,journal_id,journal_id,volume_name";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
                    .setTarget(target)
                    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
    
    // Because we created the interim website, we KNOW that there will always
    // be an XML and an HTML landing page for each article. There will usually
    // be a PDF, unless one was never provided to us.
    
    // Not all volumes have PDF, but for those that do
    builder.addAspect(PDF_PATTERN,
                      PDF_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_PDF);
    // set up XML to be an aspect that will trigger an ArticleFiles and feed the metadata extractor
    builder.addAspect(XML_PATTERN,
                      XML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_XML,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    // set up ABSTRACT to be a secondary aspect - otherwise you'd try to create
    // an article out of the issue TOC
    builder.addAspect(ABS_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT);
    
    
    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_XML);  

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}