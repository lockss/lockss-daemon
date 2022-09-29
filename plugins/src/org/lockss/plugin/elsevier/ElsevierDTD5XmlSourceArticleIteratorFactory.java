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

/*
 *  * The url structure is like this:
 * 
 * JOURNALS & BOOKS & BOOK-SERIES
 * The deliveries are broken in to chunks. 
 * CLKS000003A.tar, CLKS000003B.tar... combine to make directory CLKS000003/
 * but we do not unpack the individual tars so we must find the needed XML files
 * across the set of tar files.
 * 
 * JOURNALS & BOOK-SERIES
 * individual journal article main.xml files live in each of the fooX.tar files at issn/volume-issue/article level directories
 * fooA.tar!/foo/00121606/v395i2/S0012160614004424/main.xml
 *     which is issn, vol-issue, article-number
 *     
 * BOOKS
 * individual book chapter main.xml files live in each of the fooX.tar files at isbn/TYPE/doi2 level directories
 * fooA.tar!/foo/9780444593788/BODY/B9780444593788000013/main.xml
 *     which is isbn, item-type, chapter-id      
 * 
 *  but books ALSO have a main.xml that sits at the isbn level, eg
 *  fooA.tar!/foo/9780444593788/main.xml
 *  this one does not have corresponding PDF content nor a DOI and we won't be emitting anything for this...ignore it
 */
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
  //Modified pattern to allow for <base_url>/<year>/ and <base_url>/<directory>/
  protected static final String TOP_METADATA_PATTERN_TEMPLATE = 
      "\"(%s[^/]+/[^/]+)[A-Z]+\\.tar!/([^/]+)/(dataset|.*/main)\\.xml$\",base_url";
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
    theSpec.setPatternTemplate(getTopPatternTemplate(),
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

  // NOTE - for a child to create their own version of this
  // allow the delivered version of this plugin to override
  protected String getTopPatternTemplate() {
    return TOP_METADATA_PATTERN_TEMPLATE;
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
