/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;

import java.util.Iterator;
import java.util.regex.Pattern;

public class ProjectMuse2020BooksArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  // book landing page:
  // https://muse.jhu.edu/book/28449
  // book chaper pages:
  // https://muse.jhu.edu/chapter/1178214
  // https://muse.jhu.edu/chapter/1178214/pdf
  // https://muse.jhu.edu/chapter/1178215
  // https://muse.jhu.edu/chapter/1178215/pdf
  protected static final String ROOT_TEMPLATE = "\"%sbook/\", base_url";
  protected static final String PATTERN_TEMPLATE = "\"%sbook/%s$\", base_url, book_id";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new SubTreeArticleIterator(au,
                                      new SubTreeArticleIterator.Spec()
                                        .setTarget(target)
                                        .setRootTemplate(ROOT_TEMPLATE)
                                        .setPatternTemplate(PATTERN_TEMPLATE,
                                                            Pattern.CASE_INSENSITIVE)) {
      @Override
      protected ArticleFiles createArticleFiles(CachedUrl cu) {
        ArticleFiles af = super.createArticleFiles(cu);
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
        return af;
      }
    };
  }

  // getting metadata from the tdb - BaseArticleMetadataExtractor does that for us!
  // [there is also metadata from a form available, but no time for that!]
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
