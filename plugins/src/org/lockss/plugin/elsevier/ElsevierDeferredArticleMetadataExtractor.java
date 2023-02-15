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

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.elsevier.ElsevierDTD5XmlSourceArticleIteratorFactory.ArticleMetadataDataClass;
import org.lockss.plugin.elsevier.ElsevierDTD5XmlSourceArticleIteratorFactory.ElsevierStoredDataArticleFiles;
import org.lockss.util.Logger;


/*
 * This ArticleMetadata extractor creates a custom emitter which allows deferring the emit
 * of individual ArticleMetadat objects. It collects up the AM objects in a structure that is 
 * stored on the ArticleFiles object until the stored AM object is determined to be 'complete'
 * 
 * We do this because the metadata for an Elsevier item is delivered in two different files.
 * We first iterate over a dataset.xml file and create objects for the items defined
 * Then we get to an item-level main.xml file and fill in the remaining information and check
 * for the existence of the actual PDF of the item content.
 * 
 * We emit once we have complete information about an item and know we have its content
 * 
 */
public class ElsevierDeferredArticleMetadataExtractor extends BaseArticleMetadataExtractor{
  private static final String DATASET_FILENAME = "dataset.xml"; 

  // dataset.xml in the first diretory after the tar OR
  // main.xml in the in the fourth directory after the tar (tarnum/(isnb|issn)/(vol|type)/itemnum/main.xml
  final static Pattern XML_LOCATION_PATTERN = 
      Pattern.compile("(.*/)[^/]+([A-Z])\\.tar!/([^/]+)/(dataset\\.xml|([^/]+)/([^/]+)/([^/]+)/main\\.xml)$", 
          Pattern.CASE_INSENSITIVE);

  private static final Logger log = Logger.getLogger(ElsevierDeferredArticleMetadataExtractor.class);

  public ElsevierDeferredArticleMetadataExtractor(String roleArticleMetadata) {
    super(roleArticleMetadata);
  }

  @Override
  public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
      throws IOException, PluginException {

    ElsevierDeferredEmitter emit = new ElsevierDeferredEmitter(af, emitter);

    CachedUrl cu = af.getFullTextCu();
    FileMetadataExtractor me = null;

    if (cu != null) {
      try {
        me = cu.getFileMetadataExtractor(target);

        if (me != null) {
          me.extract(target, cu, emit);
        }

      } catch (RuntimeException e) {
        log.debug("for af (" + af + ")", e);
      } finally {
        AuUtil.safeRelease(cu);
      }
    }
  }
  
  
   static class ElsevierDeferredEmitter implements FileMetadataExtractor.Emitter {

    private Emitter parent;
    private ArticleFiles af;

    ElsevierDeferredEmitter(ArticleFiles af, Emitter parent) {
      this.parent = parent;
      this.af = af;
    }

    /*
     * emitMetadata is called once for each ArticleMetadata that is deemed 
     * ready to emit
     * by the ITEM level metadata extractor. 
     * We are either receiving an AM object filled in from parsing a 
     * dataset.xml file which 
     * contains much of the metadata for every article in the tar set or
     * an AM object filled in by parsing a main.xml file which contains the 
     * item-specific 
     * metadata associated with just the one item (article or chapter or book-series book) 
     * (article title, doi, author) 
     *
     * If we've never seen any metadata associated with the path to the 
     * XML file, then we store in the AM object in a map, 
     * using the path to its "main.xml" as its key.
     * If the main.xml path already lives in the map, then we know we already 
     * saw the other half of this
     * item's data and we add the info together and emit.
     *
     */
    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
      Matcher mat = XML_LOCATION_PATTERN.matcher(cu.getUrl());
      if (!(mat.matches())) {
        log.debug3("this url shouldn't have been in an AF metadata role");
        return;
      }

      // (.*/)[^/]+([A-Z])\\.tar!/([^/]+)/(dataset\\.xml|([^/]+)/([^/]+)/([^/]+)/main\\.xml)$
      // CLKS0000000000003A.tar!/CLKS0000000000003/dataset.xml
      // journal article
      // CLKS0000000000003C.tar!/CLKS0000000000003/21735794/v89i9/S2173579414001674/main.xml
      //chapter
      // CLKSB000000000002D.tar!/CLKSB000000000002/9780080965321/FRONT/B9780080965321010566/main.xml

      String tar_letter = mat.group(2); //which letter of the tar set
      String tar_number = mat.group(3); //number of top directory in tar
      String xml_path = mat.group(4); // path below top directory + xml filename

      // The static class that holds the map of already stored AM objects lives on the AF
      ArticleMetadataDataClass deferInfoClass = ((ElsevierStoredDataArticleFiles)(af)).getDataClass();
      // this won't ever be null if the class is instantiated
      Map<String, ArticleMetadata> sharedMap = deferInfoClass.getDataMap();
      log.debug3("sharedMap size: " + sharedMap.size());
      // dataset.xml always lives at the top level so path = filename by itself
      if (DATASET_FILENAME.equals(xml_path)) {
        //the dataset_metadata xpath is the same for both schema
        String main_xml_key = tar_number + "/" + am.getRaw(ElsevierJournalsDatasetXmlSchemaHelper.dataset_metadata);
        log.debug3("emitMetadata: an AM object from dataset.xml - " + tar_number);
        ArticleMetadata mainAM = sharedMap.remove(main_xml_key);
        if (mainAM == null) {
          // we got here first, just store the AM info under the article xml path and move on
          log.debug3("just store the record under " + main_xml_key);
          sharedMap.put(main_xml_key, am);
          return; // continue on until we get the rest of its data
        } 
        
        // we already had the other half of this article data, consolidate and emit
        log.debug3("record was already here - do a merge and emit");
        mergeAMData(am, mainAM);
        parent.emitMetadata(af, mainAM);
      } else {
        String xml_path_key = tar_number + "/" + xml_path;
        log.debug3("emitMetadata: main.xml AM " + tar_letter + ":" + xml_path_key);
        // this is a "main.xml" level AM object
        ArticleMetadata globAM = sharedMap.remove(xml_path_key);
        if (globAM == null) {
          log.debug3("just store the record under " + xml_path_key);
          // If we're not in the A tar, we should definitely already have the dataset info - or we never will
          if (!("A".equals(tar_letter))) {
            log.debug3("EGADS! this shouldn't happen....dataset is in A file");
          } else {
            // We are in the A tar and may have gotten here before dataset
            sharedMap.put(xml_path_key,  am);
          }
          return; // don't emit this partial data
        }
        log.debug3("record was already here - do a merge and emit");
        mergeAMData(globAM, am);
        parent.emitMetadata(af, am);
      }
    }

  }

  /*
   *  merge FROM datasetMD TO articleMD
   *  
   *  
   *  Journals/Book-Series  articleMD (from main.xml) has 
   *       FIELD_AUTHOR, FIELD_ARTICLE_TITLE, FIELD_ACCESS_URL, FIELD_DATE
   *    from datasetMD (from dataset.xml) get
   *       FIELD_DOI, FIELD_ISSN, FIELD_PUBLICATION_TITLE, 
   *       NOTE - no longer picking up FIELD_ISBN for journals - it was often a false positive
   *    fallbacks to pick up from raw
   *       if FIELD_DATE is null, use raw "online-publication-date" if available
   *       if FIELD_DOI is null, use raw "common_doi" if available
   *       if ARTICLE_TITLE is null, use "common_dochead" if available
   *       
   *  Books articleMD (from main.xml) has
   *      FIELD_ISBN, FIELD_ISSN? (if series), FIELD_ARTICLE_TITLE, FIELD_AUTHOR, FIELD_ACCESS_URL, FIELD_DATE
   *    from datasetMD (from dataset.xml) get
   *      FIELD_DOI, FIELD_PUBLICATION_TITLE, 
   *    fallbacks to pick up from raw       
   *       if FIELD_DATE is null, use raw "online-publication-date" if available
   *       if FIELD_DOI is null, use raw "common_doi" if available
   *       
   *  ALSO - because this is a little complicated, this will also explicitly set the
   *  PUBLICATION_TYPE and ARTICLE_TYPE based on the metadata.       
   */
  private static void mergeAMData(ArticleMetadata datasetMD, ArticleMetadata articleMD) {
    // using putIfBetter avoids copying over a null value, which otherwise fills field
    articleMD.putIfBetter(MetadataField.FIELD_DOI, datasetMD.get(MetadataField.FIELD_DOI));
    articleMD.putIfBetter(MetadataField.FIELD_PUBLICATION_TITLE, datasetMD.get(MetadataField.FIELD_PUBLICATION_TITLE));
    // we now prioritize the main.xml copyright date
    //articleMD.putIfBetter(MetadataField.FIELD_DATE, datasetMD.get(MetadataField.FIELD_DATE));
    //books will not have this, but it won't hurt to do the call anyway to handle journals
    articleMD.putIfBetter(MetadataField.FIELD_ISSN, datasetMD.get(MetadataField.FIELD_ISSN));
    // copy over any raw values from datasetMD to this one
    for (String key : datasetMD.rawKeySet()) {
      articleMD.putRaw(key, datasetMD.getRaw(key));
    }
    
    // logic for getting fall-back values - we don't add from TDB in our emitter
    articleMD.putIfBetter(MetadataField.FIELD_PUBLISHER, "Elsevier");
    articleMD.putIfBetter(MetadataField.FIELD_PROVIDER, "Elsevier");
    // This section has to fork because dataset schema is different for books and journals
    String dtd = datasetMD.getRaw(ElsevierJournalsDatasetXmlSchemaHelper.dataset_dtd_metadata);
    if ((dtd != null) && dtd.startsWith("BOOK")) {
      // BOOK CHAPTER
      articleMD.putIfBetter(MetadataField.FIELD_DATE, articleMD.getRaw(ElsevierBooksDatasetXmlSchemaHelper.dataset_chapter_date));
      articleMD.putIfBetter(MetadataField.FIELD_DOI,articleMD.getRaw(ElsevierBooksMainDTD5XmlSchemaHelper.common_doi));
      // for now we only have the xpath to support chapters, which is what they are giving us
      articleMD.put(MetadataField.FIELD_ARTICLE_TYPE,  MetadataField.ARTICLE_TYPE_BOOKCHAPTER);
      articleMD.put(MetadataField.FIELD_PUBLICATION_TYPE,  MetadataField.PUBLICATION_TYPE_BOOK);
    } else {
      // JOURNAL - BOOK-SERIES
      // NO BOOK-SERIES; for now we are not choosing books series by
      // not cooking the isbn in the raw metadata to a FIELD_ISBN
      // there were a lot of false positives for collections that were causing problems. Just make them
      // journals and count them as articles
      articleMD.putIfBetter(MetadataField.FIELD_DATE, articleMD.getRaw(ElsevierJournalsDatasetXmlSchemaHelper.dataset_article_date));
      articleMD.putIfBetter(MetadataField.FIELD_DOI,articleMD.getRaw(ElsevierJournalsMainDTD5XmlSchemaHelper.common_doi));
      //docheading, such as "Book Review" or "Index" or "Research Article"
      articleMD.putIfBetter(MetadataField.FIELD_ARTICLE_TITLE, articleMD.getRaw(ElsevierJournalsMainDTD5XmlSchemaHelper.common_dochead));
      // if the dataset had an ISBN then we know we're part of a book series
      String isbn;
      if ((isbn = datasetMD.get(MetadataField.FIELD_ISBN))!= null) {
        // BOOK SERIES because it has an isbn
        articleMD.putIfBetter(MetadataField.FIELD_ISBN, isbn);
        articleMD.put(MetadataField.FIELD_ARTICLE_TYPE,  MetadataField.ARTICLE_TYPE_BOOKCHAPTER);
        articleMD.put(MetadataField.FIELD_PUBLICATION_TYPE,  MetadataField.PUBLICATION_TYPE_BOOKSERIES);
        articleMD.put(MetadataField.FIELD_SERIES_TITLE, datasetMD.get(MetadataField.FIELD_PUBLICATION_TITLE));
      } else {
        articleMD.put(MetadataField.FIELD_ARTICLE_TYPE,  MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
        articleMD.put(MetadataField.FIELD_PUBLICATION_TYPE,  MetadataField.PUBLICATION_TYPE_JOURNAL);
      }
    }
  }


}