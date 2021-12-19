/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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
