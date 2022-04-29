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
 * 
 * Updated 8/29/18
 * Modify pattern to work with both <base_url>/<year>/ and <base_url>/<directory>/
 * Also to handle both with and without a hash-letter subdirectory
 */
public class WileyArticleIteratorFactory 
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = 
                          Logger.getLogger(WileyArticleIteratorFactory.class);
  
  // no need to set ROOT_TEMPLATE since all content is under 
  // <base_url>/<year> or <base_url>/<directory>
  // you cannot assume files end in *.wml.xml - some end in wml2.xml and some just .xml 
  // This pattern will only pick up XML files that live
  //  in a zip files that is 2 levels below year
  protected static final String PATTERN_TEMPLATE = 
      "\"%s[^/]+/([A-Z0-9]/)?[^/]+\\.zip!/.*\\.xml$\",base_url";
    
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
