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

package uk.org.lockss.plugin.informationresearch;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class InformationResearchArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger( InformationResearchArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s\",base_url";
  private static final String PATTERN_TEMPLATE = "\"%s(.*)\\.html$\",base_url";

  protected static final Pattern HTML_PATTERN = Pattern.compile("/([^/]+).html$");
  protected static final String HTML_REPLACEMENT = "/$1.html$";


  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
            ROOT_TEMPLATE,
            PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    builder.addAspect(HTML_PATTERN,
            HTML_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA) {
      @Override
      protected boolean isCheckAccessUrl() {
        return false;
      }
    };
  }
}

