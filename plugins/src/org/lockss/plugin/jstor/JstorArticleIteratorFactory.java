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

