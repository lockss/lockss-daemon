/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.jasper;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrl;
import org.lockss.util.*;

import java.util.Iterator;
import java.util.regex.Pattern;

public class JasperArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(JasperArticleIteratorFactory.class);

  protected static final String TAR_GZ_JSON_PATTERN_TEMPLATE =
      "\"%sdownload/%s/.*\\.tar\\.gz!/.*/data/metadata/metadata.json$\", base_url, collection";

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern NESTED_ARCHIVE_PATTERN =
      Pattern.compile(".*/[^/]+\\.tar\\.gz!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
          Pattern.CASE_INSENSITIVE);

  // ... 2051-5960/00003741594643f4996e2555a01e03c7/data/s40478-018-0619-9.pdf
  // ... 2051-5960/00003741594643f4996e2555a01e03c7/data/metadata/metadata.json
  public static final Pattern PDF_PATTERN = Pattern.compile("/(.*)/data/(.*)\\.pdf$", Pattern.CASE_INSENSITIVE);
  private static final String PDF_REPLACEMENT = "/$1/data/$2.pdf";
  // metadata
  public static final Pattern JSON_PATTERN = Pattern.compile("/data/metadata/metadata\\.json$", Pattern.CASE_INSENSITIVE);
  public static final String JSON_REPLACEMENT = "/data/metadata/metadata.json";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
        .setTarget(target)
        .setPatternTemplate(TAR_GZ_JSON_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
        .setExcludeSubTreePattern(NESTED_ARCHIVE_PATTERN)
        .setVisitArchiveMembers(true));

    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(JSON_PATTERN,
                      JSON_REPLACEMENT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
