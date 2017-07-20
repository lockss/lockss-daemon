/*
 * $Id$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  the CrossRef delivery schema 
 *  
 */
public class CrossRefSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(CrossRefSchemaHelper.class);


  /* 
   *  CrossRef DOI deliver schema - used by several publishers
   */
  
  //<publication_date media_type="online">
  //  <month>01</month>
  //  <day>10</day>
  //  <year>2001</year>
  //</publication_date>
  static private final NodeValue FULL_DATE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of CrossRef publication date");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tyear = null;
      String tday = null;
      String tmonth = null;
      // look at each child of the TitleElement for information                                                                                                                                     
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("day".equals(nodeName)) {
          tday = checkNode.getTextContent();
        } else if ("month".equals(nodeName) ) {
          tmonth = checkNode.getTextContent();
        } else if ("year".equals(nodeName)) {
          tyear = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tyear != null) {
        valbuilder.append(tyear);
        if (tday != null && tmonth != null) {
          valbuilder.append("-" + tmonth + "-" + tday);
        }
      } else {
        log.debug3("no date found");
        return null;
      }
      log.debug3("date found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };
  
  
  //  <person_name sequence="first" contributor_role="author">
  //    <given_name>S</given_name>
  //    <surname>Hengsberger</surname>
  //  </person_name>
  static private final NodeValue AUTHOR_NAME = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of CrossRef person name");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tgiven = null;
      String tsurname = null;
      // look at each child of the TitleElement for information                                                                                                                                     
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("given_name".equals(nodeName)) {
          tgiven = checkNode.getTextContent();
        } else if ("surname".equals(nodeName) ) {
          tsurname = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tsurname != null) {
        valbuilder.append(tsurname);
        if (tgiven != null) {
          valbuilder.append(", " + tgiven);
        }
      } else {
        log.debug3("no name found");
        return null;
      }
      log.debug3("name found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };
  
  // this is global for all articles in the file
  private static final String publisher = "/doi_batch/head/registrant";
  
  

  // The following are all relative to the article node
  // from the immediately preceding sibling -
  private static String pub_title = "preceding-sibling::journal_metadata[1]/full_title";
  private static String pub_issn = "preceding-sibling::journal_metadata[1]/issn";
  private static String pub_year = "preceding-sibling::journal_issue[1]/publication_date/year";
  private static String pub_volume = "preceding-sibling::journal_issue[1]/journal_volume/volume";
  private static String pub_issue = "preceding-sibling::journal_issue[1]/issue";
  private static String pub_issue_month = "preceding-sibling::journal_issue[1]/publication_date/month";
  
  private static String art_title = "titles/title";
  private static String art_contrib = "contributors/person_name";
     //given_name, surname
  private static String art_date = "publication_date";
  private static String art_sp = "pages/first_page";
  private static String art_lp = "pages/last_page";
  private static String art_doi = "doi_data/doi";
  public static String art_resource = "doi_data/resource";


  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issn, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issue_month, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE); 
  //  articleMap.put(art_subtitle, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_contrib, AUTHOR_NAME); 
    articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_resource, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_date, FULL_DATE); 
  }

  /* 2.  Top level per-article node */
  static private final String articleNode = "/doi_batch/body/journal/journal_article";

  /* 3. Global metadata is the publisher - work around if it gets troublesome */
  static private final Map<String, XPathValue> 
    globalMap = new HashMap<String,XPathValue>();
  static {
    globalMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE); 
  }
  /*
   * The emitter will need a map to know how to cook raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_issn, MetadataField.FIELD_ISSN);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(art_doi, MetadataField.FIELD_DOI);
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
    cookMap.put(art_lp, MetadataField.FIELD_END_PAGE);
    cookMap.put(art_contrib, MetadataField.FIELD_AUTHOR);
  }

  /**
   * publisher comes from a global node
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    //no globalMap, so returning null
    return globalMap; 
  }

  /**
   * return  article paths representing metadata of interest  
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
   */

  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * Return the path for product form so when multiple records for the same
   * item are combined, the product forms are combined together
   */

  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * using filenamePrefix (see above)
   */

  @Override
  public String getFilenameXPathKey() {
    return art_doi;
  }

}