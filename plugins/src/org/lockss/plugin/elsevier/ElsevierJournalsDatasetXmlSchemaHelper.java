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
  public static final String dataset_article_date = "journal-item-properties/online-publication-date";
  // get the journal title from the closest preceding journal-info node
  private static final String dataset_journal_title = "preceding-sibling::journal-issue[1]/journal-issue-properties/collection-title";
  // this will be there if part of a book series (which looks like a journal)
  // pick up the raw value but we won't cook it - too manh false positives turning journals to book-series
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
    // pick up the raw value but do not use it. Too many false positives turning journals --> book-series
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
    //DO NOT COOK THIS...too many false positives turning journals in to book-series
    // not sure why Elsevier is categorizing collections of journals as book series
    //cookMap.put(dataset_series_isbn, MetadataField.FIELD_ISBN);

    // Elsevier has indicated that we should prioritize copyright year
    // which comes from main.xml - so don't cook this value
    //cookMap.put(dataset_article_date, MetadataField.FIELD_DATE);
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
