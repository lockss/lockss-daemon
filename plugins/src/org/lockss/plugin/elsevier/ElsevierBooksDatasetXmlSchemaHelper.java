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

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 *  Elsevier DTD5 Metadata Extractor BOOK DATASET SCHEMA
 *  This is one of four related schema helpers to handle the extraction for Elsevier file-transfer content.
 *  This is the schema definition the dataset.xml file for books
 *  @author alexohlson
 */
public class ElsevierBooksDatasetXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(ElsevierBooksDatasetXmlSchemaHelper.class);

  /*
   * XPATH DEFINITIONS WE CARE ABOUT
   * 
   * Layout is:
   *  <dataset>
   *    <dataset-content>
   *       <book-project>
   *           isbn
   *           book-title
   *           path to book-wide main.xml
   *       <book-item>
   *           chapter information
   *       <book-item>
   *            ....
   *       <book-project>
   *          begin next book in delivery
   */
  
  private static final String dataset_content = "/dataset/dataset-content";

  // This is currently implemented for the BOOK PROJECT 
  private static final String dataset_chapter = dataset_content + "/book-item";
  // these are relative to the dataset_chapter   
  private static final String dataset_chapter_doi = "book-item-unique-ids/doi";
  public static final String dataset_chapter_date = "book-item-properties/online-publication-date";
    // get the book title from the closest preceding  book-project node
  private static final String dataset_book_title = "preceding-sibling::book-project[1]/book-project-properties/collection-title";
  //These are the same between both versions so keep them identical 
  public static final String dataset_metadata = ElsevierJournalsDatasetXmlSchemaHelper.dataset_metadata;
  public static final String dataset_dtd_metadata = ElsevierJournalsDatasetXmlSchemaHelper.dataset_dtd_metadata;
  private static final String dataset_pdf = ElsevierJournalsDatasetXmlSchemaHelper.dataset_pdf;

  //the ISBN, chapter title and author information will come from the lower-level 
  // chapter "main.xml" later in the extraction process

  
  /* 
   * This is a duplicate with the one in ElsevierJournalsDatasetXmlSchemaHelper
   * but we can't know that schema has been set up yet.
   * Date values look like this: 2014-09-22T00:54:27Z
   * and we need them to look like this: 2014-09-22 to be acceptable to our
   * metadata database
   */
  static private final NodeValue DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of date");
      String fulldate = node.getTextContent();
      log.debug3(fulldate);
      return StringUtils.substringBefore(fulldate, "T");
    }
  };

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  static {
    articleMap.put(dataset_chapter_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_chapter_date, DATE_VALUE);
    articleMap.put(dataset_book_title,XmlDomMetadataExtractor.TEXT_VALUE) ;
    articleMap.put(dataset_metadata, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_dtd_metadata, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_pdf, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item for this initial metadata starts at "journal-item" */
  static private final String articleNode = dataset_chapter; 

  /* 3. We do not need to use global information */
  static private final Map<String,XPathValue> globalMap = null;

  /*
   * The emitter will need a map to know how to cook  raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(dataset_chapter_doi, MetadataField.FIELD_DOI);
    cookMap.put(dataset_book_title, MetadataField.FIELD_PUBLICATION_TITLE);
    // Elsevier has indicated that we should prioritize copyright year
    // which comes from main.xml - so don't cook this value
    cookMap.put(dataset_chapter_date, MetadataField.FIELD_DATE);
  }

  /**
   * 
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return globalMap;
  }

  /**
   * return article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return articleNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /**
   * No duplicate data 
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * No consolidation required
   */
  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * The filenames are the same as the XML filenames with .pdf suffix
   * Use the raw metadata for the article level main.xml
   */
  @Override
  public String getFilenameXPathKey() {
    return ElsevierJournalsDatasetXmlSchemaHelper.dataset_metadata;
  }

}
