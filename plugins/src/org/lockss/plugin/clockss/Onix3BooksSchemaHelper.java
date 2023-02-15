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
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Onix 3 both short and long form
 *  @author alexohlson
 */
public class Onix3BooksSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(Onix3BooksSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";

  /* 
   *  ONIX 3.0 specific node evaluators to extract the information we want
   */
  /*
   * ProductIdentifier: handles ISBN13, DOI & LCCN
   * NODE=<ProductIdentifier>
   *   ProductIDType/
   *   IDValue/
   * xPath that gets here has already figured out which type of ID it is
   */
  static public final NodeValue ONIX_ID_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of ONIX ID");
      // the TYPE has already been captured by xpath search in raw key
      String idVal = null;
      NodeList childNodes = node.getChildNodes(); 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m); 
        if ( "IDValue".equals(infoNode.getNodeName()) | "b244".equals(infoNode.getNodeName())) {
          idVal = infoNode.getTextContent();
          break;
        }
      }
      if (idVal != null)  {
        return idVal;
      } else {
        log.debug3("no IDVal in this productIdentifier");
        return null;
      }
    }
  };

  /*
   * AUTHOR information
   * NODE=<Contributor>     
   *   ContributorRole/
   * not all of these will necessarily be there...  
   *   NamesBeforeKey/
   *   KeyNames/
   *   PersonName/
   *   PersonNameInverted
   */
  static public final NodeValue ONIX_AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of ONIX contributor");
      String auType = null;
      String auKey = null;
      String auBeforeKey = null;
      String straightName = null;
      String invertedName = null;
      NodeList childNodes = node.getChildNodes(); 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        String nodeName = infoNode.getNodeName();
        if ("ContributorRole".equals(nodeName) || "b035".equals(nodeName) ) {
          auType = infoNode.getTextContent();
        } else if ("NamesBeforeKey".equals(nodeName) || "b039".equals(nodeName)) {
          auBeforeKey = infoNode.getTextContent();
        } else if ("KeyNames".equals(nodeName) || "b040".equals(nodeName)) {
          auKey = infoNode.getTextContent();
        } else if ("PersonName".equals(nodeName) || "b036".equals(nodeName)) {
          straightName = infoNode.getTextContent();
        } else if ("PersonNameInverted".equals(nodeName) || "b037".equals(nodeName)) {
          invertedName = infoNode.getTextContent();
        } else if ("CorporateName".equals(nodeName) || "b047".equals(nodeName)) {
          straightName = infoNode.getTextContent(); // organization, not person
        }
      }
      // We may choose to limit the type of roles, but not sure which yet
      if  (auType != null) {
        // first choice, PersonNameInverted
        if (invertedName != null) {
          return invertedName;
        } else if (auKey != null) { // otherwise use KeyNames, NamesBeforeKey
          StringBuilder valbuilder = new StringBuilder();
          valbuilder.append(auKey);
          if (auBeforeKey != null) {
            valbuilder.append(AUTHOR_SEPARATOR +  " " + auBeforeKey);
          } 
          return valbuilder.toString();
        } else if (straightName != null) { //otherwise use PersonName
          return straightName;
        }
        log.debug3("No valid contributor in this contributor node.");
        return null;
      }
      return null;
    }
  };

  /* 
   * TITLE INFORMATION
   * TitleElementLevel= 01 is the book title
   * TitleElementLevel= 03 is the chapter title and indicates this is a book chapter
   *  <TitleElement>
   *     TitleElementLevel=01 <--this is the one we want
   *     TitleText
   *     Subtitle
   *   or sometimes:
   *     TitlePrefix
   *     TitleWithoutPrefix
   *   </TitleElement>
   */
  static public final NodeValue ONIX_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of ONIX level 01 title or level 03 chapter title ");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tTitle = null;
      String tSubtitle = null;
      String tPrefix = null;
      String tNoPrefix = null; 
      // look at each child of the TitleElement for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        if ("TitleText".equals(checkNode.getNodeName()) || "b203".equals(checkNode.getNodeName())) {
          tTitle = checkNode.getTextContent();
        } else if ("Subtitle".equals(checkNode.getNodeName()) || "b029".equals(checkNode.getNodeName())) {
          tSubtitle = checkNode.getTextContent();
        } else if ("TitlePrefix".equals(checkNode.getNodeName()) || "b030".equals(checkNode.getNodeName())) {
          tPrefix = checkNode.getTextContent();
        } else if ("TitleWithoutPrefix".equals(checkNode.getNodeName()) || "b031".equals(checkNode.getNodeName())) {
          tNoPrefix = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tTitle != null) {
        valbuilder.append(tTitle);
        if (tSubtitle != null) {
          valbuilder.append(": " + tSubtitle);
        }
      } else if (tNoPrefix != null) {
        if (tPrefix != null) { 
          valbuilder.append(tPrefix + " ");
        }
        valbuilder.append(tNoPrefix);
        if (tSubtitle != null) {
          valbuilder.append(": " + tSubtitle);
        }
      } else {
        log.debug3("no title found");
        return null;
      }
      log.debug3("title found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  /* 
   * PUBLISHING DATE - could be under one of two nodes
   * NODE=<PublishingDate/>
   *   <PublishingDateRole/>
   *   <Date dateformat="xx"/>  // unspecified format means YYYYMMDD 
   *   
   * NODE=<MarketDate/>
   *   <MarketDateRole/>
   *   <Date dateformat="xx"/>   // unspecified format means YYYYMMDD 
   */
  static private final NodeValue ONIX_DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of ONIX date");
      NodeList childNodes = node.getChildNodes();
      if (childNodes == null) return null;

      String dRole = null;
      String dFormat = null;
      String dDate = null;

      String RoleName = "PublishingDateRole";
      String shortRoleName = "x448";
      // short form is just all lower case
      if (!(node.getNodeName().equalsIgnoreCase("PublishingDate"))) {
        RoleName = "MarketDateRole";
        shortRoleName = "j408";
      }
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node childNode = childNodes.item(m);
        if ((childNode.getNodeName().equals(RoleName)) || childNode.getNodeName().equals(shortRoleName)){
          dRole = childNode.getTextContent();
        } else if ((childNode.getNodeName().equals("Date")) || childNode.getNodeName().equals("b306")) {
          dDate = childNode.getTextContent();
          // get the format - try both short and long...
          dFormat = ((Element)childNode).getAttribute("dateformat");
          if ((dFormat == null) || dFormat.isEmpty()) {
            dFormat = ((Element)childNode).getAttribute("j260");
            if ((dFormat == null) || dFormat.isEmpty()) {
              //default
              dFormat = "00";
            }
          }
        }
      }
      if (!(dRole.equals("01") || dRole.equals("11") || dRole.equals("12")) ) {
        // not a type of date role we care about 
        return null;
      } 
      if (dFormat.equals("00") && (dDate.length() > 7)) {
        // do a length check in case the date format is mis-labeled
        // make it W3C format instead of YYYYMMDD
        StringBuilder dBuilder = new StringBuilder();
        dBuilder.append(dDate.substring(0, 4)); //YYYY
        dBuilder.append("-");
        dBuilder.append(dDate.substring(4, 6)); //MM
        dBuilder.append("-");
        dBuilder.append(dDate.substring(6, 8)); //DD
        return dBuilder.toString();
      } else if (dFormat.equals("01") || dFormat.equals("02") 
          || dFormat.equals("03") 
          || dFormat.equals("04") || dFormat.equals("05")) {
        // the year is the first four chars of the string in any of these 
        return (dDate.substring(0, 4)); //YYYY
      } else    {
        return dDate; //not sure what the format is, just return it as is
      }
    }
  };

  /* 
   *  ONIX specific XPATH key definitions that we care about
   */

  public static String ONIX_RR = "RecordReference | a001";
  /* Under an item node, the interesting bits live at these relative locations */
  protected static String ONIX_idtype_isbn13 =
      "ProductIdentifier[ProductIDType='15'] | productidentifier[b221='15']";
  private static String ONIX_idtype_lccn =
      "ProductIdentifier[ProductIDType='13'] | productidentifier[b221='13']";
  public static String ONIX_idtype_doi =
      "ProductIdentifier[ProductIDType='06'] | productidentifier[b221='06']";
  // this one may have different meanings for different publishers
  // so just collect it by default in to the raw metadata
  public static String ONIX_idtype_proprietary =
      "ProductIdentifier[ProductIDType='01'] | productidentifier[b221='01']";
  /* components under DescriptiveDetail */
  private static String ONIX_product_form =
      "DescriptiveDetail/ProductFormDetail | descriptivedetail/b333"; 
  /* only pick up level01 title element - allow for no leading 0...(bradypus) */
  private static String ONIX_product_title =
      "DescriptiveDetail/TitleDetail[TitleType = '01' or TitleType = '1']/TitleElement[TitleElementLevel = '01'] | descriptivedetail/titledetail[b202 = '01' or b202 = '1']/titleelement[x409 = '01']";
  private static String ONIX_chapter_title =
      "DescriptiveDetail/TitleDetail[TitleType = '01' or TitleType = '1']/TitleElement[TitleElementLevel = '04'] | descriptivedetail/titledetail[b202 = '01' or b202 = '1']/titleelement[x409 = '04']";
  private static String ONIX_product_contrib =
      "DescriptiveDetail/Contributor | descriptivedetail/contributor";
  private static String ONIX_product_comp =
      "DescriptiveDetail/ProductComposition | descriptivedetail/x314";
  /* components under DescriptiveDetail if this is part of series */
  private static String ONIX_product_seriestitle =
      "DescriptiveDetail/Collection/TitleDetail/TitleElement[TitleElementLevel = '01'] | descriptivedetail/collection/titledetail/titleelement[x409 = '01']";
  private static String ONIX_product_seriesISSN =
      "DescriptiveDetail/Collection/CollectionIdentifier[CollectionIDType = '02'] | descriptivedetail/collection/collectionidentifier[x344= '02']";
  /* components under PublishingDetail */
  private static String ONIX_pub_name =
      "PublishingDetail/Publisher/PublisherName | publishingdetail/publisher/b081";
  private static String ONIX_pub_date =
      "PublishingDetail/PublishingDate | publishingdetail/publishingdate";
  // expose this for access from post-cook
  public static String ONIX_copy_date =
      "PublishingDetail/CopyrightStatement/CopyrightYear | publishingdetail/copyrightstatement/b087";
  /* components under MarketPublishingDetail */
  private static String ONIX_mkt_date =
      "ProductSupply/MarketPublishingDetail/MarketDate | productsupply/marketpublishingdetail/marketdate"; 

  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> ONIX_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    ONIX_articleMap.put(ONIX_RR, XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_idtype_isbn13, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_idtype_lccn, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_idtype_doi, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_idtype_proprietary, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_product_form, XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_product_title, ONIX_TITLE_VALUE);
    ONIX_articleMap.put(ONIX_chapter_title, ONIX_TITLE_VALUE);
    ONIX_articleMap.put(ONIX_product_contrib, ONIX_AUTHOR_VALUE);
    ONIX_articleMap.put(ONIX_product_comp, XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_pub_name, XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_pub_date, ONIX_DATE_VALUE);
    ONIX_articleMap.put(ONIX_mkt_date, ONIX_DATE_VALUE);
    ONIX_articleMap.put(ONIX_copy_date, XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_product_seriestitle, ONIX_TITLE_VALUE);
    ONIX_articleMap.put(ONIX_product_seriesISSN, ONIX_ID_VALUE);

  }

  /* 2. Each item (book) has its own subNode */
  static private final String ONIX_articleNode = "//Product|//product"; 

  /* 3. in ONIX, there is no global information we care about, it is repeated per article */ 
  static private final Map<String,XPathValue> ONIX_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(ONIX_idtype_isbn13, MetadataField.FIELD_ISBN);
    cookMap.put(ONIX_idtype_doi, MetadataField.FIELD_DOI);
    cookMap.put(ONIX_product_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(ONIX_chapter_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(ONIX_product_contrib, MetadataField.FIELD_AUTHOR);
    cookMap.put(ONIX_pub_date, MetadataField.FIELD_DATE);
    cookMap.put(ONIX_pub_name, MetadataField.FIELD_PUBLISHER);
    // TODO - after priority setting is allowed in cooking
    //cookMap.put(ONIX_mkt_date, MetadataField.FIELD_DATE);
    //cookMap.put(ONIX_copy_date, MetadataField.FIELD_DATE);

    //TODO: If book is part of series, currently no way to store title,issn
    //TODO: If book is part of series, currently no way to store title,issn
    //cookMap.put(ONIX_product_seriestitle, MetadataField.FIELD_SERIES_TITLE);
    //cookMap.put(ONIX_product_seriesISSN, MetadataField.FIELD_SERIES_ISSN);
    //TODO: Book, BookSeries...currently no key field to put the information in to      
    //cookMap.put(ONIX_product_pub + "/PublishingComposition", ONIX_FIELD_TYPE);
    //TODO: currently no way to store multiple formats in MetadataField (FIELD_FORMAT is a single);
  }


  /**
   * ONIX3 does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return ONIX_globalMap;
  }

  /**
   * return ONIX3 article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return ONIX_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return ONIX_articleNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /**
   * Return the path for isbn13 so multiple records for the same item
   * can be combined
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return ONIX_idtype_isbn13;
  }

  /**
   * Return the path for product form so when multiple records for the same
   * item are combined, the product forms are combined together
   */
  @Override
  public String getConsolidationXPathKey() {
    return ONIX_product_form;
  }

  /**
   * The filenames are based on the isbn13 value 
   */
  @Override
  public String getFilenameXPathKey() {
    return ONIX_idtype_isbn13;
  }

}
