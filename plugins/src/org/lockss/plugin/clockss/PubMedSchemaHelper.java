/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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
  // could be @PubStatus = (ppublish|epublish)
  private static String pub_date = "Journal/PubDate"; 
  private static String pub_volume = "Journal/Volume";
  private static String pub_issue = "Journal/Issue";
  
  private static String art_title = "ArticleTitle";
     //no subtitle? 
  private static String art_contrib = "AuthorList/Author";
     //FirstName,MiddleName,LastName - use value method
  private static String art_date = "publication_date";
  public static String art_sp = "FirstPage";
  public static String art_lp = "LastPage";

  //private static String art_doi = "";
    // not yet in xml samples


  // In oct/2020, their upload a new folder with different structure. PDF file is named using
  // <ELocationID EIdType="pii">1287</ELocationID>
  // /verduci-released/2020/WCRJ/2019%20VOLUME%206/III/e1287.pdf

  public static String elocation_id = "ELocationID[@EIdType = \"pii\"]";

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
    articleMap.put(elocation_id, XmlDomMetadataExtractor.TEXT_VALUE);
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