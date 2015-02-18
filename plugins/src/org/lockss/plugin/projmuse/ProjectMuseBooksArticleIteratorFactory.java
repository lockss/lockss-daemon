/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class ProjectMuseBooksArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("ProjectMuseBooksArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%sbooks/\", base_url"; // params from tdb file corresponding to AU
  protected static final String PATTERN_TEMPLATE = "\"^%sbooks/%s/%s-[\\d]+\", base_url, eisbn, eisbn";
  // http://muse.jhu.edu/books/9780299107635/9780299107635-2.pdf
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    final Pattern PDF_PATTERN = Pattern.compile("([0-9]{13})/([0-9]{13})-([0-9]+).pdf$");
    final String PDF_REPL = "$1/$1-$3.pdf";
    final Pattern HTML_PATTERN = Pattern.compile("([0-9]{13})$");
    final String HTML_REPL = "$1";
    builder.setSpec(target, ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    // only meaningful substance on this page is pdfs
    builder.addAspect(PDF_PATTERN, PDF_REPL, ArticleFiles.ROLE_FULL_TEXT_PDF);
    builder.addAspect(HTML_PATTERN, HTML_REPL, ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    return builder.getSubTreeArticleIterator(); 
  }

  // getting metadata from the tdb - BaseArticleMetadataExtractor does that for us!
  // [there is also metadata from a form available, but no time for that!]
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }

}
