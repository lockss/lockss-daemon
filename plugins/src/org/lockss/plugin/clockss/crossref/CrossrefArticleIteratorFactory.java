/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.crossref;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Special file-transfer snapshot
 * We go to their site and download a tar.gz that is generated monthly.
 * Currently we only do this annually.
 * The targ.gz contents are stored in tar.gz.manifest but isn't a complete listing
 * because many of the items have nested contents.
 * We do not open the archive we simply preserve and record the basic information
 * and size in the metadata database.
 * 
 */

public class CrossrefArticleIteratorFactory
implements ArticleIteratorFactory,
ArticleMetadataExtractorFactory {

  private static final Logger log =
      Logger.getLogger(CrossrefArticleIteratorFactory.class);  
  
  //../crossref-released/2018/20180709_all.xml.tar.gz
  private static final String PATTERN_TEMPLATE = 
      "\"^%s%s/[^/]+\\.tar\\.gz$\", base_url, directory";
  
  // Be sure to exclude all nested archives
  protected static final Pattern NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);  

  private static final Pattern ITEM_PATTERN = Pattern.compile("/(.*)\\.tar\\.gz$", Pattern.CASE_INSENSITIVE);
  private static final String 	ITEM_REPLACEMENT = "/$1.tar.gz";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
        .setTarget(target)
        .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
        .setExcludeSubTreePattern(NESTED_ARCHIVE_PATTERN));
    
    // set up tar.gz as the only item we care to count
    builder.addAspect(ITEM_PATTERN,
        ITEM_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new CrossrefArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);

  }


}
