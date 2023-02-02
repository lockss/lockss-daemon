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

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HeterocyclesArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(HeterocyclesArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%sclockss/\", base_url";
  
  protected static final String PATTERN_TEMPLATE = 
      "\"^%sclockss/(downloads|libraries)/(fulltext|PDF|PDFwithLinks|PDFsi)/"
        + "([^/]+/%s/[^/]+)$\", base_url, volume_name";
  
  private Pattern PDF_PATTERN = Pattern.compile(
      "/(downloads)/(PDF)/([^/]+/[^/]+/[^/]+)$", Pattern.CASE_INSENSITIVE);

  private static String PDF_REPLACEMENT = "/$1/$2/$3";
  private static String PDFWITHLINKS_REPLACEMENT = "/$1/PDFwithLinks/$3";
  private static String PDFSI_REPLACEMENT = "/$1/PDFsi/$3";
  
  private static String FULL_TEXT_REPLACEMENT = "/libraries/fulltext/$3";
  private static String HIDDEN_ABSTRACT_REPLACEMENT = "/libraries/abst/$3";

  public static final String ROLE_PDF_WITH_LINKS = "PdfWithLinks";
  public static final String ROLE_HIDDEN_ABSTRACT = "HiddenAbstract";
      
  // article content may look like:
  // <heterocyclesbase>.com/clockss/libraries/fulltext/21568/83/1
  // <heterocyclesbase>.com/clockss/libraries/abst/21568/83/1 (hidden url)
  // <heterocyclesbase>.com/clockss/downloads/PDF/23208/83/1
  // <heterocyclesbase>.com/clockss/downloads/PDFwithLinks/23208/83/1
  // <heterocyclesbase>.com/clockss/downloads/PDFsi/23208/83/1
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, 
      MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = 
                                        new SubTreeArticleIteratorBuilder(au);    
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // The order in which these aspects are added is important. They determine
    // which will trigger the ArticleFiles and if you are only counting 
    // articles (not pulling metadata) then the lower aspects aren't looked 
    // for, once you get a match.

    // PDF - aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT, ArticleFiles.ROLE_FULL_TEXT_PDF);   

    // full text html - aspect that will trigger an ArticleFiles
    builder.addAspect(FULL_TEXT_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_HTML, ArticleFiles.ROLE_ABSTRACT);

    builder.addAspect(HIDDEN_ABSTRACT_REPLACEMENT, ROLE_HIDDEN_ABSTRACT);
        
    builder.addAspect(PDFWITHLINKS_REPLACEMENT, ROLE_PDF_WITH_LINKS);   

    builder.addAspect(
        PDFSI_REPLACEMENT, ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);   

    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF, ArticleFiles.ROLE_FULL_TEXT_HTML);  
                
     return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }

}
