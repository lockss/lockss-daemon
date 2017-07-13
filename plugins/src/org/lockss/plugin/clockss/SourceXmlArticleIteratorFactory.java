/*
 * $Id$
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
  protected static final String ALL_XML_PATTERN_TEMPLATE = "\"^%s%d/(.*)\\.xml$\",base_url,year";  

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
  // <root_location>/<year>/<possible subdirectories>/<STUFF>
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
