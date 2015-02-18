/*
 * $Id$
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
