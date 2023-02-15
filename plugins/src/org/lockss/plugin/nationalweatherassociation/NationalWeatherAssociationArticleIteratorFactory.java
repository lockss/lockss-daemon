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

package org.lockss.plugin.nationalweatherassociation;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;

public class NationalWeatherAssociationArticleIteratorFactory 
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final String ROOT_TEMPLATE = 
      "\"%s%s\", base_url, journal_id";
  
  // <nwabase>.org/jom/articles/2013/2013-JOM12/2013-JOM12.pdf
  private static final String PATTERN_TEMPLATE = 
      "\"^%s%s/articles/%d/([^/]+/)+[^/]+\\.pdf$\", base_url, journal_id, year";
  
  private static final Pattern PDF_PATTERN = Pattern.compile(
      "/(articles)/(([^/]+/)+)([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  
  // <nwabase>.org/jom/abstracts/2013/2013-JOM12/abstract.php
  private static final String PDF_REPLACEMENT = "/$1/$2$3.pdf";
  private static final String ABSTRACT_REPLACEMENT = "/abstracts/$2abstract.php";
    
  // article content may look like:
  // <nwabase>.org/jom/abstracts/2013/2013-JOM12/abstract.php
  // <nwabase>.org/jom/articles/2013/2013-JOM12/2013-JOM12.pdf
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, 
      MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = 
        new SubTreeArticleIteratorBuilder(au);    
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // full text pdf - aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN, PDF_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_PDF);   
    
    builder.addAspect(ABSTRACT_REPLACEMENT, 
        ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_ARTICLE_METADATA);     
                
     return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(
                                                        MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
