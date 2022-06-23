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

package org.lockss.plugin.clockss.onixbooks;

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
 *  Onix 3 Long form
 *  with the filenames based on isbn13 plus .pdf or .epub
 *  There can be multiple records for the same item, one for each format
 *  @author alexohlson
 */
public class Onix3LongSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(Onix3LongSchemaHelper.class);

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
        if (infoNode.getNodeName().equals("IDValue")) {
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
        if ("ContributorRole".equals(nodeName)) {
          auType = infoNode.getTextContent();
        } else if ("NamesBeforeKey".equals(nodeName)) {
          auBeforeKey = infoNode.getTextContent();
        } else if ("KeyNames".equals(nodeName)) {
          auKey = infoNode.getTextContent();
        } else if ("PersonName".equals(nodeName)) {
          straightName = infoNode.getTextContent();
        } else if ("PersonNameInverted".equals(nodeName)) {
          invertedName = infoNode.getTextContent();
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
   * You only get here if the TitleElementLevel=01
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

      log.debug3("getValue of ONIX level 01 title");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tTitle = null;
      String tSubtitle = null;
      String tPrefix = null;
      String tNoPrefix = null; 
      // look at each child of the TitleElement for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        if ("TitleText".equals(checkNode.getNodeName())) {
          tTitle = checkNode.getTextContent();
        } else if ("Subtitle".equals(checkNode.getNodeName())) {
          tSubtitle = checkNode.getTextContent();
        } else if ("TitlePrefix".equals(checkNode.getNodeName())) {
          tPrefix = checkNode.getTextContent();
        } else if ("TitleWithoutPrefix".equals(checkNode.getNodeName())) {
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
        log.debug3("no title found at level1");
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
      if (!(node.getNodeName().equals("PublishingDate"))) {
        RoleName = "MarketDateRole";
      }
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node childNode = childNodes.item(m);
        if (childNode.getNodeName().equals(RoleName)) {
          dRole = childNode.getTextContent();
        } else if (childNode.getNodeName().equals("Date")) {
          dDate = childNode.getTextContent();
          dFormat = ((Element)childNode).getAttribute("dateformat");
          if ((dFormat == null) || dFormat.isEmpty()) {
            dFormat = "00";
          }
        }
      }
      if (!(dRole.equals("01") || dRole.equals("11") || dRole.equals("12")) ) {
        // not a type of date role we care about 
        return null;
      } 
      if (dFormat.equals("00")) { 
        // make it W3C format instead of YYYYMMDD
        StringBuilder dBuilder = new StringBuilder();
        dBuilder.append(dDate.substring(0, 4)); //YYYY
        dBuilder.append("-");
        dBuilder.append(dDate.substring(4, 6)); //MM
        dBuilder.append("-");
        dBuilder.append(dDate.substring(6, 8)); //DD
        return dBuilder.toString();
      } else {
        return dDate; //not sure what the format is, just return it as is
      }
    }
  };

  /* 
   *  ONIX specific XPATH key definitions that we care about
   */

  /* Under an item node, the interesting bits live at these relative locations */
  private static String ONIX_product_id =  "ProductIdentifier";
  protected static String ONIX_idtype_isbn13 =
      ONIX_product_id + "[ProductIDType = \"15\"]"; 
  private static String ONIX_idtype_lccn =
      ONIX_product_id + "[ProductIDType = \"13\"]";
  private static String ONIX_idtype_doi =
      ONIX_product_id + "[ProductIDType = \"06\"]";
  /* components under DescriptiveDetail */
  private static String ONIX_product_descrip =
      "DescriptiveDetail";  
  private static String ONIX_product_form =
      ONIX_product_descrip + "/ProductFormDetail";
  /* only pick up level01 title element */
  private static String ONIX_product_title =
      ONIX_product_descrip + "/TitleDetail[TitleType = '01']/TitleElement[TitleElementLevel = '01']";
  private static String ONIX_product_contrib =
      ONIX_product_descrip + "/Contributor";
  private static String ONIX_product_comp =
      ONIX_product_descrip + "/ProductComposition";
  /* components under DescriptiveDetail if this is part of series */
  private static String ONIX_product_seriestitle =
      ONIX_product_descrip + "/Collection/TitleDetail/TitleElement[TitleElementLevel = '01']";
  private static String ONIX_product_seriesISSN =
      ONIX_product_descrip + "/Collection/CollectionIdentifier[CollectionIDType = '02']";
  /* components under PublishingDetail */
  private static String ONIX_product_pub =
      "PublishingDetail";
  private static String ONIX_pub_name =
      ONIX_product_pub + "/Publisher/PublisherName";
  private static String ONIX_pub_date =
      ONIX_product_pub + "/PublishingDate";
  private static String ONIX_copy_date =
      ONIX_product_pub + "/CopyrightStatement/CopyrightYear";
  /* components under MarketPublishingDetail */
  private static String ONIX_product_mktdetail =
      "ProductSupply/MarketPublishingDetail"; 
  private static String ONIX_mkt_date =
      ONIX_product_mktdetail + "/MarketDate";


  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> ONIX_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    ONIX_articleMap.put("RecordReference", XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_idtype_isbn13, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_idtype_lccn, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_idtype_doi, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_product_form, XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_product_title, ONIX_TITLE_VALUE);
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
  static private final String ONIX_articleNode = "/ONIXMessage/Product"; 

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
    cookMap.put(ONIX_product_title, MetadataField.FIELD_JOURNAL_TITLE);
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
    return ONIX_product_descrip + "/ProductFormDetail";
  }

  /**
   * The filenames are based on the isbn13 value 
   */
  @Override
  public String getFilenameXPathKey() {
    return ONIX_idtype_isbn13;
  }

}
