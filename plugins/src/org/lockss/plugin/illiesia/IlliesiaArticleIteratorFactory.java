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

package org.lockss.plugin.illiesia;

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
 * Each article consists of a full text pdf and nothing else.  All abstracts
 * are listed in one page. ROLE_ARTICLE_METADATA can not be set.
 * BaseArticleMetadataExtractor provides default metadata from tdb file.
 *   Issue toc containing abstracts - <illiesiabase>/html/2013.html
 *   pdf - <illiesiabase>/papers/Illiesia09-01.pdf
 */
public class IlliesiaArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(IlliesiaArticleIteratorFactory.class);

  // Thib suggests to add /papers to ROOT_TEMPLATE and remove
  // from PATTERN_TEMPLATE
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%spapers/Illiesia[0-9]+-[0-9]+\\.pdf$\", base_url";
  
  // Thib recommends adding '/' at the beginning of of the pattern
  // as "/(.+\\.pdf)$" and replacement would be "/$1"
  private Pattern PDF_PATTERN = 
      Pattern.compile("(.+\\.pdf)$", Pattern.CASE_INSENSITIVE);

  private static String PDF_REPLACEMENT = "$1";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, 
      MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = 
                                        new SubTreeArticleIteratorBuilder(au);    
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
 
    // full text pdf - aspect that will trigger an ArticleFiles
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT, ArticleFiles.ROLE_FULL_TEXT_PDF);
                            
    return builder.getSubTreeArticleIterator();
  }  
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(
      MetadataTarget target) throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }

}
