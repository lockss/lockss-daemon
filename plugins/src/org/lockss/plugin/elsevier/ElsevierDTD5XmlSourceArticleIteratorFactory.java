/* $Id: ElsevierDTD5XmlSourceArticleIteratorFactory.java,v 1.6 2015-02-11 16:58:05 alexandraohlson Exp $

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.elsevier;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ElsevierDTD5XmlSourceArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(ElsevierDTD5XmlSourceArticleIteratorFactory.class);
  

  // iterate over the top level "dataset.xml" file that lives in the first (the "A") tar of 
  // any given set of related tarballs
  // BASE_URL/2014/CLKS0000000000003A.tar!/CLKS0000000000003/dataset.xml
  protected static final String TOP_METADATA_PATTERN_TEMPLATE = "\"(%s%d/[^/]+)A\\.tar!/([^/]+)/dataset\\.xml$\",base_url,year";
  // set an exclude pattern to increase efficiency. dataset.xml is only in xxxA.tar 
  protected static final String ALL_NOT_A_TAR_FILES_TEMPLATE = "\"(%s%d/[^/]+)[^A]\\.tar$\", base_url, year";
  
  public static final Pattern DATASET_XML_PATTERN = Pattern.compile("/(dataset)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final String XML_REPLACEMENT = "/$1.xml";
  
  //
  // The source content structure looks something like this:
  // <base_location>/<year>/<BigTarBall>1A.tar
  // <base_location>/<year>/<BigTarBall>1B.tar
  // <base_location>/<year>/<BigTarBall>1C.tar
  //     where the underlying structure is a set of directories, each one representing a issn/isbn
  //         under which is a directory representing  a particular volume/issue under
  //         which is even more directories, one for each article. 
  //     there is also a file, dataset.xml in the FIRST of the top level tar balls, 
  //     identifying contents
  //  eg:
  //      CLKS0000000000003A.tar and CLKS0000000000003B.tar combine to create:
  //       CLKS0000000000003/dataset.xml
  //         and            /18759637/v15sC/issue.xml and then article directories
  //         and            /21735794/v89i9/issues.xml and then article directories
  //       each article directory contains the article contents (pdf, figures, etc)
  //       also has a "main.xml" with full text and article specific metadata
  //  The top "dataset.xml" lives only in the <number>A.tar file, so start with that
  //
  //  We only want to iterate over the dataset.xml file that lives at the top. 
  //   This file will tell us almost all the metadata we need for the contents
  //   and also the location of the specific articles "main.xml" file so we can
  //   extract the author and article title from that.

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // no need to limit to ROOT_TEMPLATE
    // Do not need to worry about excluding underlying archive files because our pattern is top-anchored 
    SubTreeArticleIterator.Spec theSpec = builder.newSpec();
    theSpec.setTarget(target);
    theSpec.setPatternTemplate(TOP_METADATA_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    theSpec.setExcludeSubTreePatternTemplate(ALL_NOT_A_TAR_FILES_TEMPLATE);
    /* this is necessary to be able to see what's inside the tar file */
    theSpec.setVisitArchiveMembers(true);
    builder.setSpec(theSpec);
    
    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map 

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(DATASET_XML_PATTERN,
                      XML_REPLACEMENT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(
                                                        MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
