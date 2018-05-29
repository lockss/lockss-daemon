/*
 * $Id:
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.codeocean;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

//
// A  variation on the generic CLOCKSS source article iterator
// it iterates over the delivered zip files and allows the metadata extractor to count items.
// a ".tar" file of the same base
//
public class CodeOceanItemArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

  private static final Logger log = Logger.getLogger(CodeOceanItemArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year
  // foo.tar.gz!/....../JID-YR-VOL-ISS-NUM.xml  
  private static final String PATTERN_TEMPLATE = 
      "\"^%s%s/[^/]+\\.zip$\", base_url, directory";
  
  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/[^/]+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);  

  private static final Pattern ZIP_PATTERN = Pattern.compile("/(.*)\\.zip$", Pattern.CASE_INSENSITIVE);
  private static final String 	ZIP_REPLACEMENT = "/$1.xml";
  // might exist, might not
  private static final String TAR_REPLACEMENT = "/$1.tar";
  
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
    

    // set up ZIP to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(ZIP_PATTERN,
        ZIP_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    // tar files represent the docker config of the equivalent zip code modul
    // associate them but they aren't themselves a code module
    builder.addAspect(TAR_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);


    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new CodeOceanItemArticleMetadataExtractor();
  }
  
  
}
