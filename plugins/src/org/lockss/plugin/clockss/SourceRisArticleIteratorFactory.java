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

package org.lockss.plugin.clockss;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

//
// A generic article iterator for CLOCKSS source plugins that want to iterate on 
// files that end in .xml at some level below the root directory. 
// The metadata extraction will be customized by publisher plugin but will use
// the xml files provided by this article iterator
//
public class SourceRisArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(SourceRisArticleIteratorFactory.class);

  public static final Pattern RIS_PATTERN = Pattern.compile("/(.*)\\.ris$", Pattern.CASE_INSENSITIVE);
  public static final String RIS_REPLACEMENT = "/$1.ris";
  
  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year
  //could handle any number of subdirectories under the year so long as end in .ris

  // pull out explicit use of "year" param since it can now be an arbitrary directory
  protected static final String PATTERN_TEMPLATE = "\"^%s[^/]+/(.*)\\.ris$\",base_url";
  //
  // The source content structure looks like this:
  // <root_location>/<dir>/<possible subdirectories>/<STUFF>
  //     where STUFF is a series of files:  <name>.pdf, <name>.epub &
  //    as well as a some number of <othername(s)>.ris which provide the metadata
  //    for all the content.
  //
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
                    .setTarget(target)
                    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
    
    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map 

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(RIS_PATTERN,
                      RIS_REPLACEMENT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
