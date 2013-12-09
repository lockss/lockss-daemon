/*
 * $Id: Onix2LongXmlMetadataExtractorHelper.java,v 1.2 2013-12-09 17:49:08 alexandraohlson Exp $
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.onixbooks;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractorHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Onix 2 Long form
 *  with the filenames based on isbn13 plus .pdf or .epub
 *  There can be multiple records for the same item, one for each format
 *  @author alexohlson
 */
public class Onix2LongXmlMetadataExtractorHelper
implements SourceXmlMetadataExtractorHelper {
  static Logger log = Logger.getLogger(Onix2LongXmlMetadataExtractorHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";
  /* 
   *  ONIX 2.1 specific node evaluators to extract the information we want
   */

  /*
   * ProductIdentifier: handles ISBN13, DOI & LCCN
   * NODE=<ProductIdentifier>
   *   ProductIDType/
   *   IDValue/
   * xPath that gets here has already figured out which type of ID it is
   */
  static private final NodeValue ONIX_ID_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of ONIX ID");
      // the TYPE has already been captured by xpath used to get here
      String idVal = null;
      NodeList childNodes = node.getChildNodes();
      // we know childNodes cannot be null due to xPath search term 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m); 
        if ("IDValue".equals(infoNode.getNodeName())) {
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
   * SeriesIdentifier  - issn for series of which the book is a part
   * NODE=<SeriesIdentifier
   *   SeriesIDType/
   *   IDValue/
   *   IDName/ (we don't care about this one)
   */
  static private final NodeValue ONIX_SERIESID_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of series ISSN");
      // the TYPE has already been captured by xpath used to get here
      String idVal = null;
      NodeList childNodes = node.getChildNodes();
      // we know childNodes cannot be null due to xPath search term 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m); 
        if ("IDValue".equals(infoNode.getNodeName())) {
          idVal = infoNode.getTextContent();
          break;
        }
      }
      if (idVal != null)  {
        return idVal;
      } else {
        log.debug3("no issn in this seriesIdentifier");
        return null;
      }
    }
  };



  /*
   * AUTHOR information - similar to 3.0 but sits at different level in tree
   * NODE=<Contributor>     
   *   ContributorRole/
   * not all of these will necessarily be there...  
   *   NamesBeforeKey/
   *   KeyNames/
   *   PersonName/
   *   PersonNameInverted/
   *   TitlesBeforeName/
   *   
   *   Use the PersonNameInverted if there. 
   *   
   *   Our database doesn't have a place for editor or translator but
   *   this will pick up any contributor regardless of role
   *   but in the order given by the publisher
   */
  static private final NodeValue ONIX_CONTRIBUTOR_VALUE = new NodeValue() {
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
      // We are not currently limiting based on role
      if  (auType != null) {
        // first choice, PersonNameInverted
        if (invertedName != null) {
          return invertedName;
        } else if (auKey != null) { // otherwise use KeyNames, NamesBeforeKey
          StringBuilder valbuilder = new StringBuilder();
          valbuilder.append(auKey);
          if (auBeforeKey != null) {
            valbuilder.append(AUTHOR_SEPARATOR + " " + auBeforeKey);
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
   * NODE=<Title>
   * <TitleType>01</TitleType> (01 = distinctive title (book), covere title (serial)
   *     Title on item (serial content item or reviewed resource)
   * <TitleText textcase="#">Title Words</TitleText>
   * <Subtitle>could go here</Subtitle>
   *  ignoring any use of <TitleWithoutPrefix> and <TitlePrefix> instead
   * 
   */
  static private final NodeValue ONIX_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of ONIX title");
      NodeList titleNodes = node.getChildNodes();
      if (titleNodes == null) return null;

      String tLevel = null;
      String tTitle = null;
      String tSubtitle = null;
      // look at each child of the Title 
      if (titleNodes != null) {
        for (int j = 0; j < titleNodes.getLength(); j++) {
          Node checkNode = titleNodes.item(j);
          if ("TitleType".equals(checkNode.getNodeName())) {
            tLevel = checkNode.getTextContent();
            if (!"01".equals(tLevel)) {
              break;
            }
          } else if ("TitleText".equals(checkNode.getNodeName())) {
            tTitle = checkNode.getTextContent();
          } else if ("Subtitle".equals(checkNode.getNodeName())) {
            tSubtitle = checkNode.getTextContent();
          }
        }
      }
      // if not a 01 title, it isn't one we collect
      if("01".equals(tLevel)) {
        StringBuilder valbuilder = new StringBuilder();
        valbuilder.append(tTitle);
        if (tSubtitle != null) {
          valbuilder.append(": " + tSubtitle);
        }
        log.debug3("title found: " + valbuilder.toString());
        return valbuilder.toString();
      } else {
        return null;
      }
    }
  };

  /* 
   *  ONIX2 specific XPATH key definitions that we care about
   */

  private static String ONIX_product_form = "ProductForm";
  protected static String ONIX_idtype_isbn13 =
      "ProductIdentifier[ProductIDType='15']";
  private static String ONIX_idtype_lccn =
      "ProductIdentifier[ProductIDType='13']";
  private static String ONIX_idtype_doi =
      "ProductIdentifier[ProductIDType='06']";
  private static String ONIX_product_title = "Title";
  private static String ONIX_product_contrib = "Contributor";
  private static String ONIX_pub_name = "Publisher/PublisherName";
  private static String ONIX_pub_date = "PublicationDate";
  /* the following are for when book is part of a series */
  private static String ONIX_product_series_title_simple = 
      "Series/TitleOfSeries"; // could come this way
  private static String ONIX_product_series_title_full = 
      "Series/Title"; // or could come this way
  private static String ONIX_product_series_issn = 
      "Series/SeriesIdentifier[SeriesIDType='02']";

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue> ONIX_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    ONIX_articleMap.put("RecordReference", XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_idtype_isbn13, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_idtype_lccn, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_idtype_doi, ONIX_ID_VALUE); 
    ONIX_articleMap.put(ONIX_product_title, ONIX_TITLE_VALUE);
    ONIX_articleMap.put(ONIX_product_series_title_simple, XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_product_series_title_full, ONIX_TITLE_VALUE);
    ONIX_articleMap.put(ONIX_product_series_issn, ONIX_SERIESID_VALUE);
    ONIX_articleMap.put(ONIX_product_contrib, ONIX_CONTRIBUTOR_VALUE);
    ONIX_articleMap.put(ONIX_pub_name, XmlDomMetadataExtractor.TEXT_VALUE);
    ONIX_articleMap.put(ONIX_pub_date, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own subNode */
  static private final String ONIX_articleNode = "/ONIXMessage/Product"; 

  /* 3. in ONIX, there is no global information we care about, it is repeated per article */ 
  static private final Map<String,XPathValue> ONIX_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(ONIX_idtype_isbn13, MetadataField.FIELD_ISBN);
    cookMap.put(ONIX_idtype_doi, MetadataField.FIELD_DOI);
    cookMap.put(ONIX_product_title, MetadataField.FIELD_JOURNAL_TITLE); // book title = journal title
    cookMap.put(ONIX_product_contrib, MetadataField.FIELD_AUTHOR);
    cookMap.put(ONIX_pub_name, MetadataField.FIELD_PUBLISHER);
    cookMap.put(ONIX_pub_date, MetadataField.FIELD_DATE);
    //TODO: book part of series - currently nowhere to put series title information
    //only one of the forms of series title will exist
    //cookMap.put(ONIX_product_series_simple, MetadataField.SERIES_TITLE);
    //cookMap.put(ONIX_product_series_full, MetadataField.SERIES_TITLE);
    //cookMap.put(ONIX_product_series_id, MetadataField.FIELD_ISSN);
    //TODO: Book, BookSeries...currently no key field to put the information in to      
    //cookMap.put(ONIX_product_pub + "/PublishingComposition", ONIX_FIELD_TYPE);
    //TODO: currently no way to store multiple formats in MetadataField (FIELD_FORMAT is a single);
  }

  /**
   * ONIX2 does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return ONIX_globalMap;
  }

  /**
   * return ONIX2 article paths representing metadata of interest  
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
  public String getUniqueIDKey() {
    return ONIX_idtype_isbn13;
  }

  /**
   * Return the path for product form so when multiple records for the same
   * item are combined, the product forms are combined together
   */
  @Override
  public String getConsolidationKey() {
    return "ProductForm";
  }

  /**
   * No filename prefix needed - return null
   */
  @Override
  public String getFilenamePrefix() {
    return null;
  }

  /**
   * Look for both pdf and epub filetype to decide if we should emit
   */
  @Override
  public ArrayList<String> getFilenameSuffixList() {
    ArrayList<String> returnList = new ArrayList<String>();
    returnList.add(".pdf");
    returnList.add(".epub");
    return returnList;
  }

  /**
   * The filenames are based on the isbn13 value 
   */
  @Override
  public String getFilenameKey() {
    return ONIX_idtype_isbn13;
  }

}    