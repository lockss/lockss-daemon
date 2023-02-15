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

package org.lockss.plugin.taylorandfrancis;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

//
// A slight variation on the generic CLOCKSS source zip article iterator
// this one just excludes xml files that start with CATS as they are 
// redundant and in a different schema - probably the catalog version of 
// information, not the xml of the actual articles
// But not available for every volume so we're using the other xml schema
// to extract metadata
//
public class TaylorAndFrancisSourceZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  protected static Logger log = Logger.getLogger(TaylorAndFrancisSourceZipXmlArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year
  // This pattern is specific to TandF - exclude the XML that start with "CATS_"
    // remove explicit use of "year" and allow any arbitrary directory for new generation source plugin
  private static final String PATTERN_TEMPLATE = 
      "\"%s[^/]+/.*\\.zip!/(?!.*CATS_).*\\.xml$\", base_url";

  public static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final String XML_REPLACEMENT = "/$1.xml";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
                    .setTarget(target)
                    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
                    .setVisitArchiveMembers(true)); // to be able to see what is in zip
    
    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map 

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(XML_PATTERN,
                      XML_REPLACEMENT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }
  
}
