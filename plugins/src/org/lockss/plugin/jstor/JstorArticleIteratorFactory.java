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

package org.lockss.plugin.jstor;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * JSTOR limited plugin
 * This is a plugin that collects a limited set of content due to redirection.
 * We only pick up TOC and pdf (and in rare cases, full, media, select)
 * Based on information on the TOC page, we also engineer the RIS citation page
 * which is what we'll be using for metadata extraction and is therefore 
 * what we will iterate on.
 * Do a sanity check to make sure the actually have a content file associated 
 * with the engineered RIS url 
 */

public class JstorArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(JstorArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%saction/\", base_url2";

  //  citations live under "https" which is base_url2
  //https://www.jstor.org/action/downloadSingleCitationSec?format=refman&doi=10.2307/41827174
  protected static final String PATTERN_TEMPLATE = "\"^%saction/downloadSingleCitationSec\\?format=refman&doi=\", base_url2";

  private final Pattern RIS_PATTERN = Pattern.compile("&doi=([.0-9]+)/([^?&]+)$", Pattern.CASE_INSENSITIVE);
  private final String RIS_REPLACEMENT = "&doi=$1/$2"; //no replacement...just the one

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
    .setTarget(target)
    .setRootTemplate(ROOT_TEMPLATE)
    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map 

    // set up RIS to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(RIS_PATTERN,
        RIS_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }

  // Enclose the method that creates the builder to allow a child to do additional processing
  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
    return new SubTreeArticleIteratorBuilder(au);
  }


  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}

