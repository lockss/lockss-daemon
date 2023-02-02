/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.iumj;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class IUMJArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  protected static Logger log =
      Logger.getLogger(IUMJArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
      "\"%sIUMJ/FTDLOAD/%d/\", base_url, year";
  
  protected static final String PATTERN_TEMPLATE =
      "\"%sIUMJ/FTDLOAD/(%d)/(%s)/([^/]+)/pdf\", base_url, year, volume_name";
  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // various aspects of an article
    // http://www.iumj.indiana.edu/IUMJ/FTDLOAD/1984/33/33001/pdf
    // http://www.iumj.indiana.edu/IUMJ/ABS/1984/33001
    // http://www.iumj.indiana.edu/IUMJ/FULLTEXT/1984/33/33001
    // http://www.iumj.indiana.edu/META/1984/33001.xml
    // http://www.iumj.indiana.edu/oai/1984/33/33008/33001.html
    // http://www.iumj.indiana.edu/IUMJ/FTDLOAD/1984/33/33001/djvu
    // http://www.iumj.indiana.edu/oai/1984/33/33001/33001_abs.pdf
    // 
    
    final Pattern PDF_PATTERN = Pattern.compile(
        "IUMJ/FTDLOAD/([^/]+)/([^/]+)/([^/]+)/pdf", 
        Pattern.CASE_INSENSITIVE);
    
    // how to change from one form (aspect) of article to another
    final String PDF_REPLACEMENT = "IUMJ/FTDLOAD/$1/$2/$3/pdf";
    final String ABSTRACT_REPLACEMENT = "IUMJ/ABS/$1/$3";
    final String FT_LANDING_REPLACEMENT = "IUMJ/FULLTEXT/$1/$2/$3";
    final String METADATA_REPLACEMENT = "META/$1/$3.xml";
    final String OIA_CITE_REPLACEMENT = "oai/$1/$2/$3/$3.html";
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up pdf to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up abstract to be an aspect
    builder.addAspect(
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    // set up abstract to be an aspect
    builder.addAspect(
        FT_LANDING_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    // set up metadata to be an aspect
    builder.addAspect(
        METADATA_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    // set up oia metadata to be an aspect
    builder.addAspect(
        OIA_CITE_REPLACEMENT,
        ArticleFiles.ROLE_CITATION);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    // In this case, there are two jobs, one for counting articles (abstract is 
    // good) and the other for metadata (PDF is correct)
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF, 
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
