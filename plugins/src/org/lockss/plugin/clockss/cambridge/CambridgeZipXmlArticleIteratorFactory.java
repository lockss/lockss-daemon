/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.cambridge;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

//
// Cannot use the generic Zip XML article iterator because we also need to pick
// up ".sgm" files...simpler just to redo
//
public class CambridgeZipXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(CambridgeZipXmlArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year
  
  // For Cambridge, we only want to iterate on the blahblah/12345h.(sgm|xml) files, not the 12345w.xmlfiles
  /*July 2019 - delivery source and format changed slightly. 
  * Previously it was
  *   fooa.pdf, (fooh.xml and foow.xml) or fooh.sgm  - we trapped on the xxxh.(sgm|xml) and ignored xxxw.xml
  * now it's only 
  *   fooa.pdf and foo.xml
  * Making the plugin work forwards and backwards compatibility 
  * The JATS schema was also updated and the internal layout changed - no more subdirectories but these 
  * changes should be transparent
  */
  
  protected static final String ONLY_PLAIN_OR_H_XML_OR_SGM_TEMPLATE =
      "\"%s%d/.*\\.zip!/(?!.*w\\.(xml|sgm)).*\\.(xml|sgm)$\", base_url, year";

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);

  //this is the part that requires this to be its own special snowflake
  public static final Pattern META_PATTERN = Pattern.compile("/(.*)\\.(xml|sgm)$", Pattern.CASE_INSENSITIVE);
  public static final String META_REPLACEMENT = "/$1.$2";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
                    .setTarget(target)
                    .setPatternTemplate(ONLY_PLAIN_OR_H_XML_OR_SGM_TEMPLATE, Pattern.CASE_INSENSITIVE)
                    .setExcludeSubTreePattern(NESTED_ARCHIVE_PATTERN)
                    .setVisitArchiveMembers(true)); // to be able to see what is in zip
    
    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map 

    // set up XML/SGML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(META_PATTERN,
                      META_REPLACEMENT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }
  
 
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
