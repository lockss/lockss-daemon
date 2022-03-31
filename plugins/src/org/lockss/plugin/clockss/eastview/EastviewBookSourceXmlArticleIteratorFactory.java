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

package org.lockss.plugin.clockss.eastview;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

public class EastviewBookSourceXmlArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(EastviewBookSourceXmlArticleIteratorFactory.class);

  //Based on the following structure, we can only get "1335books.zip!" which is the unzipped folder name from ".txt" file

  //https://clockss-test.lockss.org/sourcefiles/eastviewbooks-released/2020/Eastview%20Ebook%20Content/20H1/1335books.zip!/994802BO.pdf
  //https://clockss-test.lockss.org/sourcefiles/eastviewbooks-released/2020/Eastview%20Ebook%20Content/20H1/1335books.zip!/994803BO.pdf
  //https://clockss-test.lockss.org/sourcefiles/eastviewbooks-released/2020/Eastview%20Ebook%20Content/20H1/1335books_archive_file_list.txt
  //https://clockss-test.lockss.org/sourcefiles/eastviewbooks-released/2020/Eastview%20Ebook%20Content/20H1/CLOCKSS%20-%201.1.20%20-%206.30.20.xml

  protected static final String CUSTOM_ROLE = "TEXT_FILE";
  protected static final String STATIC_TEXT_FILE_PATH = "_archive_file_list.txt";

  protected static final String ROOT_TEMPLATE = "\"%s\", base_url, ";
  private static final String PATTERN_TEMPLATE = "\"%s%s/.*\\.(xml|txt)$\", base_url, directory";

  public static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final Pattern TXT_PATTERN = Pattern.compile("/([^/]+)_archive_file_list\\.txt$", Pattern.CASE_INSENSITIVE);

  /*
  $1 is "CLOCKSS%20-%201.1.20%20-%206.30.20" in ".xml" file
  $1 is "1335books_archive_file_list" for ".txt" file
   */

  //$1 will be 
  public static final String XML_REPLACEMENT = "/$1.xml";
  //$1 will be "1335books" which will be used to get "1335books.zip!" directory
  private static final String TXT_REPLACEMENT = "/$1_archive_file_list.txt";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    SubTreeArticleIterator.Spec theSpec = builder.newSpec();
    theSpec.setRootTemplate(ROOT_TEMPLATE);
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

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(TXT_PATTERN,
            TXT_REPLACEMENT,
            CUSTOM_ROLE);

    return builder.getSubTreeArticleIterator();
  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(
                                                        MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA) {

      class MyEmitter implements FileMetadataExtractor.Emitter {
        private Emitter parent;
        private ArticleFiles af;
       

        MyEmitter(ArticleFiles af, Emitter parent) {
          this.af = af;
          this.parent = parent;
        }

        public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
          if (isAddTdbDefaults()) {
            addTdbDefaults(af, cu, am);
          }
          parent.emitMetadata(af, am);
        }
      }

      @Override
      public void extract(MetadataTarget target, ArticleFiles af,
          Emitter emitter) throws IOException, PluginException {
        MyEmitter myEmitter = new MyEmitter(af, emitter);
        CachedUrl cu = getCuToExtract(af);
        String zipFolderPath = "" ;
        if (log.isDebug3()) log.debug3("extract(" + af + "), cu: " + cu);

        // This part handle the txt file 
        if (cu == null) {

          String textFileURL = af.getFullTextUrl();
          int txtIndex = textFileURL.indexOf(STATIC_TEXT_FILE_PATH);
          String textFileName  = textFileURL.substring(textFileURL.lastIndexOf("/") + 1);

          if (txtIndex > -1) {
            zipFolderPath = textFileName.replace(STATIC_TEXT_FILE_PATH, "");
            MarcRecordMetadataHelper.setZippedFolderName(zipFolderPath);

          }

          //return from here to avoid generating metadata for the ".txt" file
          return;
        }

        // This part to handle xml file
        if (cu != null) {
          try {
            FileMetadataExtractor me = cu.getFileMetadataExtractor(target);

            if (me != null) {
              me.extract(target, cu, myEmitter);
              AuUtil.safeRelease(cu);
              return;
            } 
          } catch (IOException ex) {
            log.warning("Error in FileMetadataExtractor", ex);
          }
        }
        
        if (cu != null) {
          ArticleMetadata am = new ArticleMetadata();
          myEmitter.emitMetadata(cu, am);
          // Set the zipped folder name in the schema helper, so it can be used to construct access url
          AuUtil.safeRelease(cu);
        }
      }
    };
  }
}
