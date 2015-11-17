/*
 * $Id:$
 */

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
  private static final String dataset_chapter_date = "book-item-properties/online-publication-date";
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
