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
 */
public class ElsevierDeferredArticleMetadataExtractor extends BaseArticleMetadataExtractor{
  private static final String DATASET_FILENAME = "dataset.xml"; 
  final static Pattern XML_LOCATION_PATTERN = 
      Pattern.compile("(.*/)[^/]+([A-Z])\\.tar!/([^/]+)/(dataset\\.xml|.*/main\\.xml)$", 
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
     * by the FILE level metadata extarctor. 
     * We are either receiving an AM object filled in from parsing a 
     * dataset.xml file which 
     * contains much of the metadata for every article in the tar set or
     * an AM object filled in by parsing a main.xml file which contains the 
     * article-specific 
     * metadata associated with just the one article 
     * (article title, doi, author) 
     *
     * If we've never seen any metadata associated with the path to the 
     * XML file, then we store in the AM object in a map, 
     * using the path to its "main.xml" as its key.
     * If the main.xml path already lives in the map, then we know we already 
     * saw the other half of this
     * article's data and we add the info together and emit.
     *
     */
    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
  
      Matcher mat = XML_LOCATION_PATTERN.matcher(cu.getUrl());
      if (!(mat.matches())) {
        log.debug3("this url shouldn't have been in an AF metadata role");
        return;
      }

      // (.*/)[^/]+([A-Z])\\.tar!/([^/]+)/(dataset\\.xml|.*/main\\.xml)$
      // CLKS0000000000003A.tar!/CLKS0000000000003/dataset.xml
      // CLKS0000000000003C.tar!/CLKS0000000000003/21735794/v89i9/S2173579414001674/main.xml
      String tar_letter = mat.group(2); //which letter of the tar set
      String tar_number = mat.group(3); //number of top directory in tar
      String xml_path = mat.group(4); // path + xml filename

      // The static class that holds the map of already stored AM objects lives on the AF
      ArticleMetadataDataClass deferInfoClass = ((ElsevierStoredDataArticleFiles)(af)).getDataClass();
      // this won't ever be null if the class is instantiated
      Map<String, ArticleMetadata> sharedMap = deferInfoClass.getDataMap();
      log.debug3("sharedMap size: " + sharedMap.size());
      // dataset.xml always lives at the top level so path = filename by itself
      if (DATASET_FILENAME.equals(xml_path)) {
        String main_xml_key = tar_number + "/" + am.getRaw(ElsevierDatasetXmlSchemaHelper.dataset_article_metadata);
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
   *    articleMD (from main.xml) has 
   *       FIELD_AUTHOR, FIELD_ARTICLE_TITLE, FIELD_ACCESS_URL
   *    from datasetMD (from dataset.xml) get
   *       FIELD_DOI, FIELD_ISSN, FIELD_PUBLICATION_TITLE, FIELD_DATE
   *    fallbacks to pick up from raw
   *       if FIELD_DATE is null, use raw "common_copyright" if available
   *       if FIELD_DOI is null, use raw "common_doi" if available
   *       if ARTICLE_TITLE is null, use "common_dochead" if available
   */
  private static void mergeAMData(ArticleMetadata datasetMD, ArticleMetadata articleMD) {
    // using putIfBetter avoids copying over a null value, which otherwise fills field
    articleMD.putIfBetter(MetadataField.FIELD_DOI, datasetMD.get(MetadataField.FIELD_DOI));
    articleMD.putIfBetter(MetadataField.FIELD_ISSN, datasetMD.get(MetadataField.FIELD_ISSN));
    articleMD.putIfBetter(MetadataField.FIELD_PUBLICATION_TITLE, datasetMD.get(MetadataField.FIELD_PUBLICATION_TITLE));
    articleMD.putIfBetter(MetadataField.FIELD_DATE, datasetMD.get(MetadataField.FIELD_DATE));
    // copy over any raw values from datasetMD to this one
    for (String key : datasetMD.rawKeySet()) {
      articleMD.putRaw(key, datasetMD.getRaw(key));
    }
    // logic for getting fall-back values - we don't add from TDB in our emitter
    articleMD.putIfBetter(MetadataField.FIELD_PUBLISHER, "Elsevier");
    articleMD.putIfBetter(MetadataField.FIELD_PROVIDER, "Elsevier");
    articleMD.putIfBetter(MetadataField.FIELD_DATE, articleMD.getRaw(ElsevierMainDTD5XmlSchemaHelper.common_copyright));
    articleMD.putIfBetter(MetadataField.FIELD_DOI,articleMD.getRaw(ElsevierMainDTD5XmlSchemaHelper.common_doi));
    //docheading, such as "Book Review" or "Index" or "Research Article"
    articleMD.putIfBetter(MetadataField.FIELD_ARTICLE_TITLE, articleMD.getRaw(ElsevierMainDTD5XmlSchemaHelper.common_dochead));
  }


}