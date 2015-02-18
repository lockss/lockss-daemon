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

package org.lockss.plugin.businesssystemslaboratory;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * This publisher provides inconsistent url structure, so not possible for 
 * the article iterator builder to guess the abstract where some metadata can
 * be found, from the full text pdf url.  The abstract html also has syntax 
 * error near where some metadata found. Hence, the article file contains only
 * pdf urls
 */
public class BusinessSystemsLaboratoryArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(BusinessSystemsLaboratoryArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%sBSR[.-]Vol[.-]%s[.-](?![^/]+[.-]Complete([.-]Issue)?\\.pdf)"
                                  + "[^/]+\\.pdf$\", base_url, volume_name";

  
  private static Pattern PDF_PATTERN = 
      Pattern.compile("/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);

  private static String PDF_REPLACEMENT = "/$1.pdf";

  // article content may look like:
  // Volume 1 (2012):
  // http://www.business-systems-review.org/
  //            Aiello.et.al.(2012).Complex.Products.1.1.htm
  // http://www.business-systems-review.org/
  //            BSR.Vol.1-Iss.1-Aiello.Esposito.Ferri.Complex.Products.pdf
  // Volume 2 (2013):
  // http://www.business-systems-review.org/
  //            Bardy.&.Massaro.(2013).Sustainability.Value.Index.2.1.htm
  // http://www.business-systems-review.org/
  //            BSR.Vol.2-Iss.1-Massaro.et.al.Organising.Innovation.pdf

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

    // full text pdf - aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT, ArticleFiles.ROLE_FULL_TEXT_PDF);
                            
    return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }

}
