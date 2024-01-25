/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.michigan.deepblue;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class DeepBlueArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(DeepBlueArticleIteratorFactory.class);

  //https://deepblue.lib.umich.edu/handle/2027.42/151767
  //https://deepblue.lib.umich.edu/bitstream/handle/2027.42/151767/Papers%20on%20Paleontology%2038%2010-10-2019%20-%20High%20Res.pdf?sequence=1&isAllowed=y
  //https://deepblue.lib.umich.edu/bitstream/handle/2027.42/151767/Papers%20on%20Paleontology%2038%2010-10-2019%20-%20low%20res.pdf?sequence=2&isAllowed=y

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";

  protected static final String PATTERN_TEMPLATE = "(bitstream)?(/handle/[\\d\\.]+/\\d+)(/.*\\.pdf\\?sequence=[^&]+&isAllowed=y)?";

  public static final Pattern HTML_PATTERN = Pattern.compile("(/handle/[\\d\\.]+/\\d+)$", Pattern.CASE_INSENSITIVE);
  public static final String HTML_REPLACEMENT = "$2";

  public static final Pattern PDF_PATTERN = Pattern.compile("(bitstream)(/handle/[\\d\\.]+/\\d+)(/.*\\.pdf\\?sequence=[^&]+&isAllowed=y)", Pattern.CASE_INSENSITIVE);
  public static final String PDF_REPLACEMENT = "$1$2$3";


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
            ArticleFiles.ROLE_FULL_TEXT_HTML,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    /*
    builder.addAspect(PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

     */

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    //Do this on purpose, since its PDF has weird string
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA) {
      @Override
      protected boolean isCheckAccessUrl() {
        return false;
      }
    };
  }
}


