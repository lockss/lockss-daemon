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
import org.apache.commons.lang.StringUtils;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for ONIX2
 *  XML. This is an abstract class which defines the Xpath layout only
 *  and a plugin must
 *  use a subclass which defines the
 *  string used by the schema (short or long form)
 *  @author alexohlson
 */
public abstract class Onix2BaseSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(Onix2BaseSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";
  private static final String TITLE_SEPARATOR = ":";

  /*
   * These are instance variables which will get set in the subclasses for 
   * Onix2Short and Onix2Long formats of the schema.  This is done this way
   * because the ONLY difference in the two forms are which strings are used
   * in the schema - all the logic and layout is otherwise identical. 
   * An Onix2 file must be all one or the other.
   * This class is never used directly as the strings haven't been set.
   */
  protected String IDValue_string;
  protected String ContributorRole_string;
  protected String NamesBeforeKey_string;
  protected String KeyNames_string;
  protected String PersonName_string;
  protected String PersonNameInverted_string;
  protected String CorporateName_string;
  protected String TitleType_string;
  protected String TitleLevel_val;
  protected String TitleText_string;
  protected String Subtitle_string;
  protected String ProductForm_string;
  protected String ProductIdentifier_string;
  protected String ProductIDType_string;
  protected String Contributor_string;
  protected String Publisher_string;
  protected String PublisherName_string;
  protected String PublicationDate_string;
  protected String Series_string;
  protected String SeriesIdentifier_string;
  protected String SeriesIDType_string;
  protected String TitleOfSeries_string;
  protected String Title_string;
  protected String ONIXMessage_string;
  protected String Product_string; 
  protected String RecordReference_string;

  /*
   * These instance variables get set by defineSchemaPaths() after the child
   * constructor sets the instance variables from which they're built
   * The child class MUST call defineSchemaPaths as part of its constructor.
   */

  private String ONIX_product_form;
  private  String ONIX_idtype_isbn13;
  private  String ONIX_idtype_lccn;
  private  String ONIX_idtype_doi;
  private  String ONIX_product_title;
  private  String ONIX_product_contrib;
  private  String ONIX_pub_name;
  private  String ONIX_pub_date;
  /* the following are for when book is part of a series */
  private  String ONIX_product_series_title_simple; // could come this way 
  private  String ONIX_product_series_title_full; // or could come this way
  private  String ONIX_product_series_issn;

  private Map<String,XPathValue> ONIX_articleMap;
  private  String ONIX_articleNode;
  private  Map<String,XPathValue> ONIX_globalMap;
  protected  MultiValueMap cookMap;

  /*
   * ProductIdentifier: handles ISBN13, DOI & LCCN
   * NODE=<ProductIdentifier>
   *   ProductIDType/
   *   IDValue/
   * xPath that gets here has already figured out which type of ID it is
   */
  private final NodeValue ONIX_ID_VALUE = new NodeValue() {
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
        if (IDValue_string.equals(infoNode.getNodeName())) {
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
   * PublicationDate  - pubdate for series of which the book is a part
   * NODE=<SeriesIdentifier
   *   SeriesIDType/
   *   IDValue/
   *   IDName/ (we don't care about this one)
   */
  private final NodeValue ONIX_PUBDATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of publication date");
      // the TYPE has already been captured by xpath used to get here
      String dateVal = null;
      NodeList childNodes = node.getChildNodes();
      // we know childNodes cannot be null due to xPath search term 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m); 
        if ("#text".equals(infoNode.getNodeName())) {
          dateVal = infoNode.getTextContent();
          break;
        }
      }
      if (dateVal != null)  {
        
        dateVal = makeValidDate(dateVal);

      } else {
        log.debug3("no pubdate found");
      }
      return dateVal;
    }
  };
  public static String makeValidDate(String dateStr) {
    StringBuilder pubdate = new StringBuilder();

    // if no '-', insert, or return year only
    if (dateStr.length() >= 4) { // year
      pubdate.append(dateStr.substring(0, 4));
    } 
    if (dateStr.length() >= 6) {
      pubdate.append("-"+dateStr.substring(4,6));
    }
    if (dateStr.length() == 8) {
      pubdate.append("-"+dateStr.substring(6,8));
    }
    return pubdate.toString();
  }
  
  /*
   * SeriesIdentifier  - issn for series of which the book is a part
   * NODE=<SeriesIdentifier
   *   SeriesIDType/
   *   IDValue/
   *   IDName/ (we don't care about this one)
   */
  private final NodeValue ONIX_SERIESID_VALUE = new NodeValue() {
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
        if (IDValue_string.equals(infoNode.getNodeName())) {
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
   *   CorporateName/
   *   TitlesBeforeName/
   *   
   *   Use the PersonNameInverted if there. 
   *   
   *   Our database doesn't have a place for editor or translator but
   *   this will pick up any contributor regardless of role
   *   but in the order given by the publisher
   */


  private final NodeValue ONIX_CONTRIBUTOR_VALUE = new NodeValue() {
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
        if (ContributorRole_string.equals(nodeName)) {
          auType = infoNode.getTextContent();
        } else if (NamesBeforeKey_string.equals(nodeName)) {
          auBeforeKey = infoNode.getTextContent();
        } else if (KeyNames_string.equals(nodeName)) {
          auKey = infoNode.getTextContent();
        } else if (PersonName_string.equals(nodeName)) {
          straightName = infoNode.getTextContent();
        } else if (PersonNameInverted_string.equals(nodeName)) {
          invertedName = infoNode.getTextContent();
        } else if (CorporateName_string.equals(nodeName)) {
          straightName = infoNode.getTextContent(); // organization, not person
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
        } else if (straightName != null) { //personName or corporateName
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

  private final NodeValue ONIX_TITLE_VALUE = new NodeValue() {
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
          if (TitleType_string.equals(checkNode.getNodeName())) {
            tLevel = checkNode.getTextContent();
            if (!TitleLevel_val.equals(tLevel)) {
              break;
            }
          } else if (TitleText_string.equals(checkNode.getNodeName())) {
            tTitle = checkNode.getTextContent();
          } else if (Subtitle_string.equals(checkNode.getNodeName())) {
            tSubtitle = checkNode.getTextContent();
          }
        }
      }
      // if not a 01 title, it isn't one we collect
      if(TitleLevel_val.equals(tLevel)) {
        StringBuilder valbuilder = new StringBuilder();
        valbuilder.append(tTitle);
        if (tSubtitle != null) {
          if (!(StringUtils.endsWith(tTitle, TITLE_SEPARATOR))) {
            valbuilder.append(TITLE_SEPARATOR);
          }
          valbuilder.append(" " + tSubtitle);
        }
        log.debug3("title found: " + valbuilder.toString());
        return valbuilder.toString();
      } else {
        return null;
      }
    }
  };


  // This will have to called by the child AFTER setting the values of the 
  // instance variables
  protected void defineSchemaPaths() {
    // XPath definitions for things we care about in this schema
    ONIX_product_form = ProductForm_string;
    ONIX_idtype_isbn13 =
        ProductIdentifier_string + "[" + ProductIDType_string + "='15']";
    ONIX_idtype_lccn =
        ProductIdentifier_string + "[" + ProductIDType_string + "='13']";
    ONIX_idtype_doi =
        ProductIdentifier_string + "[" + ProductIDType_string + "='06']";
    ONIX_product_title = Title_string;
    ONIX_product_contrib = Contributor_string;
    ONIX_pub_name = Publisher_string + "/" + PublisherName_string;
    ONIX_pub_date = PublicationDate_string;
    /* the following are for when book is part of a series */
    ONIX_product_series_title_simple = 
        Series_string + "/" + TitleOfSeries_string; // could come this way 
    ONIX_product_series_title_full = 
        Series_string + "/" + Title_string; // or could come this way
    ONIX_product_series_issn = 
        Series_string + "/" + SeriesIdentifier_string + "[" + 
            SeriesIDType_string + "='02']";

    /* 1.  MAP associating xpath & value type definition or evaluator */
    ONIX_articleMap = 
        new HashMap<String,XPathValue>();
    {
      ONIX_articleMap.put(RecordReference_string, XmlDomMetadataExtractor.TEXT_VALUE);
      ONIX_articleMap.put(ONIX_idtype_isbn13, ONIX_ID_VALUE); 
      ONIX_articleMap.put(ONIX_idtype_lccn, ONIX_ID_VALUE); 
      ONIX_articleMap.put(ONIX_idtype_doi, ONIX_ID_VALUE); 
      ONIX_articleMap.put(ONIX_product_title, ONIX_TITLE_VALUE);
      ONIX_articleMap.put(ONIX_product_series_title_simple, XmlDomMetadataExtractor.TEXT_VALUE);
      ONIX_articleMap.put(ONIX_product_series_title_full, ONIX_TITLE_VALUE);
      ONIX_articleMap.put(ONIX_product_series_issn, ONIX_SERIESID_VALUE);
      ONIX_articleMap.put(ONIX_product_contrib, ONIX_CONTRIBUTOR_VALUE);
      ONIX_articleMap.put(ONIX_pub_name, XmlDomMetadataExtractor.TEXT_VALUE);
      //ONIX_articleMap.put(ONIX_pub_date, XmlDomMetadataExtractor.TEXT_VALUE);
      ONIX_articleMap.put(ONIX_pub_date, ONIX_PUBDATE_VALUE);
    }

    /* 2. Each item (book) has its own subNode */
    ONIX_articleNode = "/" + ONIXMessage_string +
        "/" + Product_string; 

    /* 3. in ONIX, there is no global information we care about, it is repeated per article */ 
    ONIX_globalMap = null;

    /*
     * The emitter will need a map to know how to cook ONIX raw values
     */
    cookMap = new MultiValueMap();
    {
      //do NOT cook publisher_name; get value from the TDB for consistency
      cookMap.put(ONIX_idtype_isbn13, MetadataField.FIELD_ISBN);
      cookMap.put(ONIX_idtype_doi, MetadataField.FIELD_DOI);
      cookMap.put(ONIX_product_title, MetadataField.FIELD_JOURNAL_TITLE); // book title = journal title
      cookMap.put(ONIX_product_contrib, MetadataField.FIELD_AUTHOR);
      cookMap.put(ONIX_pub_date, MetadataField.FIELD_DATE);
      cookMap.put(ONIX_pub_name,MetadataField.FIELD_PUBLISHER); // overridden by tdb

      //TODO: book part of series - currently nowhere to put series title information
      //only one of the forms of series title will exist
      //cookMap.put(ONIX_product_series_simple, MetadataField.SERIES_TITLE);
      //cookMap.put(ONIX_product_series_full, MetadataField.SERIES_TITLE);
      //cookMap.put(ONIX_product_series_id, MetadataField.FIELD_ISSN);
      //TODO: Book, BookSeries...currently no key field to put the information in to      
      //TODO: currently no way to store multiple formats in MetadataField (FIELD_FORMAT is a single);
    }



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
  public String getDeDuplicationXPathKey() {
    return ONIX_idtype_isbn13;
  }

  /**
   * Return the path for product form so when multiple records for the same
   * item are combined, the product forms are combined together
   */
  @Override
  public String getConsolidationXPathKey() {
    return "ProductForm";
  }

  /**
   * The filenames are based on the isbn13 value 
   */
  @Override
  public String getFilenameXPathKey() {
    return ONIX_idtype_isbn13;
  }


}    