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

package org.lockss.plugin.heterocycles;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class Heterocycles2023ArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(Heterocycles2023ArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%sclockss/\", base_url";


  protected static final String PATTERN_TEMPLATE =
          "\"^%sclockss/libraries/journal/%s/[^/]+$\", base_url, volume_name";
  
  private Pattern PDF_PATTERN = Pattern.compile(
      "/libraries/journal/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);

  private static String ISSUE_LEVEL_METADATA_REPLACEMENT = "/libraries/journal/$1/$2";
      
  // article content may look like:
  // <heterocyclesbase>.com/clockss/libraries/fulltext/21568/83/1
  // <heterocyclesbase>.com/clockss/libraries/abst/21568/83/1 (hidden url)
  // <heterocyclesbase>.com/clockss/downloads/PDF/23208/83/1
  // <heterocyclesbase>.com/clockss/downloads/PDFwithLinks/23208/83/1
  // <heterocyclesbase>.com/clockss/downloads/PDFsi/23208/83/1
  // IssuePage: http://www.heterocycles.jp/clockss/libraries/journal/102/8
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, 
      MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = 
                                        new SubTreeArticleIteratorBuilder(au);    
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);


    builder.addAspect(PDF_PATTERN,
            ISSUE_LEVEL_METADATA_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);



    return builder.getSubTreeArticleIterator();
  }  

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA) {
      @Override
      protected boolean isCheckAccessUrl() {
        return true;
      }
    };
  }

}
