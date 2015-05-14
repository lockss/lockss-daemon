/* $Id$

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

package org.lockss.plugin.elsevier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ElsevierDTD5XmlSourceArticleIteratorFactory
implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = 
      Logger.getLogger(ElsevierDTD5XmlSourceArticleIteratorFactory.class);


  // iterate over the top level "dataset.xml" 
  // (we know that only lives in the "A" tar)
  // BASE_URL/2014/CLKS0000000000003A.tar!/CLKS0000000000003/dataset.xml
  // or any bottom level "main.xml" files
  // BASE_URL/2014/CLKS0000000000003A.tar!/CLKS0000000000003/21735794/v89i9/S2173579414001674/main.xml
  // where the tar's letter could be A, B, C, ...
  protected static final String TOP_METADATA_PATTERN_TEMPLATE = 
      "\"(%s%d/[^/]+)[A-Z]+\\.tar!/([^/]+)/(dataset|.*/main)\\.xml$\",base_url,year";
  // Be sure to exclude all nested archives
  static final Pattern NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/[^/]+[A-Z]\\.tar!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);

  public static final Pattern DATASET_OR_MAIN_XML_PATTERN = 
      Pattern.compile("/(dataset|main)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final String XML_REPLACEMENT = "/$1.xml";

  //
  // The source content structure looks something like this:
  // <base_location>/<year>/<BigTarBall>1A.tar
  // <base_location>/<year>/<BigTarBall>1B.tar
  // <base_location>/<year>/<BigTarBall>1C.tar
  //     where the underlying structure is a set of directories, each one 
  // representing an issn under which is a directory representing  a particular
  // volume/issue under which is even more directories, one for each article. 
  // there is also a file, dataset.xml in the FIRST of the top level tar balls, 
  // the identifying contents
  //  eg:
  //   CLKS0000000000003A.tar and CLKS0000000000003B.tar combine to create:
  //   CLKS0000000000003/dataset.xml
  //     and        /18759637/v15sC/issue.xml and then article directories
  //     and         /21735794/v89i9/issue.xml and then article directories
  //   each article directory contains the article contents (pdf, figures, etc)
  //   also has a "main.xml" with full text and article specific metadata
  // The top "dataset.xml" lives only in the <number>A.tar file, so start with 
  // that
  //
  //  Most of the metadata we need we can get from the one dataset.xlm file
  //   but we'll also need to go to each article level main.xml file for a few
  //   thing - article.title, author, doi
  //  A custom emitter knows how to store up and wait until it has both pieces 
  //  to emit for the article

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    
    //instantiate a static class that will be attached to AF object for sharing
    final ArticleMetadataDataClass storedInfoClass = new ArticleMetadataDataClass();
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au) {
      @Override
      protected BuildableSubTreeArticleIterator instantiateBuildableIterator() {
        return new BuildableSubTreeArticleIterator(au, spec) {
          @Override
          protected ArticleFiles instantiateArticleFiles() {
            return new ElsevierStoredDataArticleFiles(storedInfoClass);
          }
        };
      }
    };

    // no need to limit to ROOT_TEMPLATE
    SubTreeArticleIterator.Spec theSpec = builder.newSpec();
    theSpec.setTarget(target);
    theSpec.setPatternTemplate(TOP_METADATA_PATTERN_TEMPLATE, 
        Pattern.CASE_INSENSITIVE);
    //do not descend in to any underlying archives
    theSpec.setExcludeSubTreePattern(NESTED_ARCHIVE_PATTERN); 
    // necessary to look in to tar files
    theSpec.setVisitArchiveMembers(true);
    builder.setSpec(theSpec);

    builder.addAspect(DATASET_OR_MAIN_XML_PATTERN,
        XML_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }

  // we will use the ROLE_ARTICLE_METADATA for extraction
  public ArticleMetadataExtractor createArticleMetadataExtractor(
      MetadataTarget target)
          throws PluginException {
    return new 
      ElsevierDeferredArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

  
  /*
   * An extended version of an ArticleFiles object. It adds an additional
   * field - the map of ArticleMetadata objects that will be shared
   * amongst all the files being handled by the iterator.
   */
  public static class ElsevierStoredDataArticleFiles
  extends ArticleFiles {

    private ArticleMetadataDataClass storedAMInfo;

    ElsevierStoredDataArticleFiles() {
      super();
    }

    ElsevierStoredDataArticleFiles(ArticleMetadataDataClass data) {
      super();
      storedAMInfo = data;
    }

    public void setDataClass(ArticleMetadataDataClass data) {
      storedAMInfo = data;
    }

    public ArticleMetadataDataClass getDataClass() {
      return storedAMInfo;
    }

  }

  /*
   * A static class that gets instantiated and attached to the AF object 
   * so that information can be 
   * shared between emit calls in the 
   * ElsevierDeferredArticleMetadataExtractor
   */
  public static class ArticleMetadataDataClass {

    private Map<String, ArticleMetadata> theMap;

    ArticleMetadataDataClass() {
      theMap = new <String, ArticleMetadata> HashMap();
    }

    public Map<String, ArticleMetadata> getDataMap() {
      return theMap;
    }

  }

}
