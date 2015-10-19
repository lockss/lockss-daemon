/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package dk.dkdemopln;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

//
// An article iterator for Demo source plugin that wants to iterate on 
// files that end in .pdf at any level below the root directory.
// The metadata extraction will use the .mods.xml files provided by this article iterator
//
public class DkDemoSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(DkDemoSourceArticleIteratorFactory.class);
  
  public static final Pattern PDF_PATTERN =
      Pattern.compile("/(.+)[.]pdf$", Pattern.CASE_INSENSITIVE);
  public static final String PDF_REPLACEMENT = "/$1.pdf";
  public static final String XML_REPLACEMENT = "/$1.mods.xml";
  
  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is 
  // the entire tree under base/sd/jid/yr
  // can handle any number of sub-directories under the year so long as end in .pdf
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/%s/%d/.*[.]pdf$\"," +
      "base_url, dir_name, journal_id, year";
  //  
  // The source content structure looks like this:
  // <root_location>/<sub-domain>/<jid>/<year>/<subdirectories>/<STUFF>
  //     where STUFF is a pdf file:  <name>.pdf and metadata file <name>.mods.xml
  //    as well as a <name>.htm file that lists the above file names (not emitted)
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
    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(
        XML_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
