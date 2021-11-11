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
// A child plugin can adapt this by changing the getIncludePatternTemplate and getExcludeSubTreePattern
// Zip and Tar are variants based off this iterator.
//
public class SourceXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(SourceXmlArticleIteratorFactory.class);

  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year
  //could handle any number of subdirectories under the year so long as end in .xml
    // pull out explicit use of "year" param since it can now be a "directory" or a "year" param 
    // depending on which generation of parent SourcePlugin we're using - make it just a single depth directory
  protected static final String ALL_XML_PATTERN_TEMPLATE = "\"^%s[^/]+/(.*)\\.xml$\",base_url";  

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  // the default for unpacked deliveries is all archives; override for zip/tar deliveries
  protected static final Pattern NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);
      
  public static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final String XML_REPLACEMENT = "/$1.xml";
  // There will not always be a 1:1 PDF relationship, but when there is use that as the
  // full-text CU. 
  private static final String PDF_REPLACEMENT = "/$1.pdf";


  //
  // The non-archive source content structure looks like this:
  // <root_location>/<dir>/<possible subdirectories>/<STUFF>
  //     where STUFF is a series of files:  <name>.pdf, <name>.epub &
  //    as well as a some number of <othername(s)>.xml which provide the metadata
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
                    .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
                    .setExcludeSubTreePattern(getExcludeSubTreePattern())
                    .setVisitArchiveMembers(getIsArchive())); 
    
    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map 

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(XML_PATTERN,
                      XML_REPLACEMENT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);
 
    // While we can't identify articles that are *just* PDF which is why they
    // can't trigger an articlefiles by themselves, we can identify them
    // by replacement and they should be the full text CU.
    builder.addAspect(PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    //Now set the order for the full text cu
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ARTICLE_METADATA); // though if it comes to this it won't emit    

    return builder.getSubTreeArticleIterator();
  }
  
  // NOTE - for a child to create their own version of this. 
  // This does not work to limit the match of the include, except when 
  // the urls are "below" the pattern - intended for subtree
  // A child might use this to limit entire subtrees - default is to avoid
  // descending in to nested archive files (eg suplementary data)
  protected Pattern getExcludeSubTreePattern() {
    return NESTED_ARCHIVE_PATTERN;
  }

  // NOTE - for a child to create their own version of this
  // Most likely use is to limit which XML files should be used for article iteration
  // since excluding is limited to subtrees, just create more limiting regex pattern
  // to identify the appropriate xml files
  protected String getIncludePatternTemplate() {
    return ALL_XML_PATTERN_TEMPLATE;
  }
  
  // NOTE - for a child to create their own version of this
  // indicates if the iterator should descend in to archives (for tar/zip deliveries)
  protected boolean getIsArchive() {
    return false;
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
