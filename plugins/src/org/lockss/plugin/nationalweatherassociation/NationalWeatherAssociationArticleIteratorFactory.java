/*
 * $Id: NationalWeatherAssociationArticleIteratorFactory.java,v 1.1 2014-01-28 17:53:36 ldoan Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.nationalweatherassociation;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class NationalWeatherAssociationArticleIteratorFactory 
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static Logger log = Logger.getLogger(
      NationalWeatherAssociationArticleIteratorFactory.class);

  private static final String ROOT_TEMPLATE = 
      "\"%s%s\", base_url, journal_id";
  
  // <nwabase>.org/jom/abstracts/2013/2013-JOM22/abstract.php
  // <nwabase>.org/jom/articles/2013/2013-JOM12/2013-JOM12.pdf
  private static final String PATTERN_TEMPLATE = 
      "\"^%s%s/(abstracts|articles)/[0-9]{4}/[0-9]{4}-[^/]+[0-9]+"
        + "/((abstract\\.php)|([0-9]{4}-[^/]+[0-9]+\\.pdf))$\", "
        + "base_url, journal_id";
  
  private static Pattern PDF_PATTERN = Pattern.compile(
      "/(articles)/([0-9]{4})/([0-9]{4}-[^/]+[0-9]+)"
        + "/([0-9]{4}-[^/]+[0-9]+\\.pdf)$", Pattern.CASE_INSENSITIVE);

  // <nwabase>.org/jom/abstracts/2013/2013-JOM12/abstract.php
  // <nwabase>.org/jom/articles/2013/2013-JOM12/2013-JOM12.pdf
  private static String PDF_REPLACEMENT = "/$1/$2/$3/$4";
  private static String ABSTRACT_REPLACEMENT = "/abstracts/$2/$3/abstract.php";
    
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
    
    // The order in which these aspects are added is important. They determine
    // which will trigger the ArticleFiles.

    // full text pdf - aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN, PDF_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_PDF);   
    
    builder.addAspect(ABSTRACT_REPLACEMENT, 
        ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_ARTICLE_METADATA);   

    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF, ArticleFiles.ROLE_ABSTRACT);  
                
     return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(
                                                        MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
