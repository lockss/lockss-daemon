/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.wiley;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Iterates article files.  Archived source content zip files include files
 * with mime-type pdf and xml. The xml file contains the metadata and refers
 * to the name of the PDF file.
 * <p>
 * There's no way to consistently get the name
 * of the PDF file from the name of the XML file, so it's necessary to
 * iterate on the XML files and capture the name of the PDF files in the 
 * metadata extractor. Example XML file names include:
 * <pre>
 * 1/117966453266.3.zip!/ j.1365-2796.2009.02095.x.wml.xml
 * A/ADMA23.12.zip!/1427_ftp.wml.xml
 * A/ADMA23.12.zip!/1419_hdp.wml.xml 
 * C/CEAT34.10.zip!/1728_hrp.wml.xml
 * </pre>
 */
public class WileyArticleIteratorFactory 
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = 
                          Logger.getLogger(WileyArticleIteratorFactory.class);
  
  // no need to set ROOT_TEMPLATE since all content is under <base_url>/<year>
  // you cannot assume files end in *.wml.xml - some end in wml2.xml and some just .xml 
  // This pattern will only pick up XML files that live
  //  in a zip files that is 2 levels below year
  protected static final String PATTERN_TEMPLATE = 
      "\"%s%d/[A-Z0-9]/[^/]+\\.zip!/.*\\.xml$\",base_url,year";
    
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
      SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);
      
      // Don't need to rematch for all the stuff at the beginning 
      final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);
      final String XML_REPLACEMENT = "/$1.xml";

      // no need to limit to ROOT_TEMPLATE
      SubTreeArticleIterator.Spec theSpec = new SubTreeArticleIterator.Spec();
      theSpec.setTarget(target);
      theSpec.setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
      theSpec.setVisitArchiveMembers(true);
      builder.setSpec(theSpec);
      
      // NOTE - full_text_cu is set automatically to the url used for the articlefiles
      // ultimately the metadata extractor needs to set the entire facet map 

      // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
      builder.addAspect(XML_PATTERN,
          XML_REPLACEMENT,
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
