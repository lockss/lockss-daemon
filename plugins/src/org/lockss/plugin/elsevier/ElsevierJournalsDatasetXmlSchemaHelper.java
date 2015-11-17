/*
 * $Id$
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
 *  Elsevier DTD5 Metadata Extractor JOURNAL DATASET SCHEMA
 *  This is one of four related schema helpers to handle the extraction for Elsevier file-transfer content.
 *  This is the schema definition the dataset.xml file for journals and book-series
 *  @author alexohlson
 */
public class ElsevierJournalsDatasetXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(ElsevierJournalsDatasetXmlSchemaHelper.class);

  /*
   * XPATH DEFINITIONS WE CARE ABOUT
   */

  private static final String dataset_content = "/dataset/dataset-content";
  // Each article starts at its only unique "dataset_article" node

  // This is currently implemented for the JOURNAL ARTICLE dataset
  // These work for ARTICLES, BOOK-REVIEWS and SIMPLE-ARTICLES
  private static final String dataset_article = dataset_content + "/journal-item";
  // these are relative to the dataset_article   
  private static final String dataset_article_doi = "journal-item-unique-ids/doi";
  private static final String dataset_article_issn = "journal-item-unique-ids/jid-aid/issn";
  private static final String dataset_article_jid = "journal-item-unique-ids/jid-aid/jid";
  private static final String dataset_article_date = "journal-item-properties/online-publication-date";
  // get the journal title from the closest preceding journal-info node
  private static final String dataset_journal_title = "preceding-sibling::journal-issue[1]/journal-issue-properties/collection-title";
  // this will be there if part of a book series (which looks like a journal)
  private static final String dataset_series_isbn = "preceding-sibling::journal-issue[1]/journal-issue-properties/isbn";
  // these ones are the same for both JOURNALS and BOOKS so make them public      
  public static final String dataset_metadata = "files-info/ml/pathname";
  public static final String dataset_dtd_metadata = "files-info/ml/dtd-version";
  public static final String dataset_pdf = "files-info/web-pdf/pathname";

  /* 
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

  /* 
   * <journal-issue>
   *   <journal-issue-properties>
   *     <collection-title>
   *  the reason we can't just have used the full path to the node we want
   *  as our xpath expression and just picked up its text content is because
   *  we had to find the closest previous "journal-issue" preceeding sibling
   *  and that meant we had to search on a <journal-issue> and not the lower
   *  down child node we wanted.  
   */
  static private final NodeValue JOURNAL_ISSUE_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("get value of previous journal-issue sibling");
      NodeList groupChildNodes = node.getChildNodes();
      for (int n = 0; n< groupChildNodes.getLength(); n++) {
        Node cNode = groupChildNodes.item(n);
        if ("journal-issue-properties".equals(cNode.getNodeName())) {
          NodeList propChildNodes = cNode.getChildNodes();
          for (int m = 0; m< propChildNodes.getLength(); m++) {
            Node pNode = propChildNodes.item(m);
            if ("collection-title".equals(pNode.getNodeName())) {
              return pNode.getTextContent();
            }
          }
        }
      }
      log.debug3("No publication title found");
      return null;
    }
  };
  

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  static {
    articleMap.put(dataset_article_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_article_issn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_article_jid, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_article_date, DATE_VALUE);
    articleMap.put(dataset_metadata, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_dtd_metadata, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_pdf, XmlDomMetadataExtractor.TEXT_VALUE);
    //articleMap.put(dataset_journal_title, JOURNAL_ISSUE_TITLE_VALUE);
    articleMap.put(dataset_journal_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_series_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item for this initial metadata starts at "journal-item" */
  static private final String articleNode = dataset_article; 

  /* 3. We do not need to use global information */
  static private final Map<String,XPathValue> globalMap = null;

  /*
   * The emitter will need a map to know how to cook  raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(dataset_article_doi, MetadataField.FIELD_DOI);
    cookMap.put(dataset_article_issn, MetadataField.FIELD_ISSN);
    cookMap.put(dataset_journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(dataset_series_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(dataset_article_date, MetadataField.FIELD_DATE);
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
    return dataset_metadata;
  }

}
