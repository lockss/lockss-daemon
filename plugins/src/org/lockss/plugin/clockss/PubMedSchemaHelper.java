/*
 * $Id$
 */

/*

 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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
 *  the PubMed delivery schema
 *  
 */
public class PubMedSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(CrossRefSchemaHelper.class);
  
  
  //*******************************
  
  //<PubDate PubStatus = "epublish"> <Year>2017</Year> <Month>March</Month> <Day>24</Day> </PubDate>
  static private final NodeValue FULL_DATE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of PubMed publication date");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tyear = null;
      String tday = null;
      String tmonth = null;
      // look at each child of the TitleElement for information                                                                                                                                     
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("Day".equals(nodeName)) {
          tday = checkNode.getTextContent();
        } else if ("Month".equals(nodeName) ) {
          tmonth = checkNode.getTextContent();
        } else if ("Year".equals(nodeName)) {
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
  
  
  //<Author>
  //  <FirstName>Anna</FirstName>
  //   <MiddleName></MiddleName>
  //   <LastName>Writer</LastName>
  //   <Affiliation>School of PFoo</Affiliation>
  //</Author>
  static private final NodeValue AUTHOR_NAME = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of PubMed author name");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tgiven = null;
      String tsurname = null;
      String tmidname = null;
      // look at each child of the TitleElement for information                                                                                                                                     
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("FirstName".equals(nodeName)) {
          tgiven = checkNode.getTextContent();
        } else if ("LastName".equals(nodeName) ) {
          tsurname = checkNode.getTextContent();
        } else if ("MiddleName".equals(nodeName) ) {
          tmidname = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tsurname != null) {
        valbuilder.append(tsurname);
        if (tgiven != null) {
          valbuilder.append(", " + tgiven);
          if (tmidname != null) {
            valbuilder.append(" " + tmidname);
          }
        }
      } else {
        log.debug3("no name found");
        return null;
      }
      log.debug3("name found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };
  
  //******************
  // The following are all relative to /ArticleSet/Article

  // from the immediately preceding sibling -
  private static String publisher = "Journal/PublisherName";
  private static String pub_title = "Journal/JournalTitle";
  private static String pub_issn = "Journal/Issn";
  private static String pub_date = "Journal/PubDate";
  private static String pub_volume = "Journal/Volume";
  private static String pub_issue = "Journal/Issue";
  
  private static String art_title = "ArticleTitle";
     //no subtitle? 
  private static String art_contrib = "AuthorList/Author";
     //FirstName,MiddleName,LastName - use value method
  private static String art_date = "publication_date";
  private static String art_sp = "FirstPage";
  private static String art_lp = "LastPage";
  //private static String art_doi = "";
    // not yet in xml samples
  

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issn, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_date, FULL_DATE); 
    articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_contrib, AUTHOR_NAME); 
  }

  /* 2.  Top level per-article node */
  private static String articleNode = "/ArticleSet/Article";


  /* 3. Global metadata is the publisher - work around if it gets troublesome */
  static private final Map<String, XPathValue> globalMap = null;
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
    cookMap.put(pub_date, MetadataField.FIELD_DATE);
    cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
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
    return null; // the XML and PDF use the same base filename
  }

}