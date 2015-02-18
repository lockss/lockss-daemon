/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.nap;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  the NAP publishing XML format
 *  The pdf filenames use the same name as the .xml file
 *  There is only one record for each file
 *  @author alexohlson
 */
public class NAPXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(NAPXmlSchemaHelper.class);

  private static final String AUTHOR_SPLIT_CHAR = ";";

  /* 
   *  NAP XML  node evaluators to extract the information we want
   */
  
  /* 
   * TITLE INFORMATION
   * see if the <subtitle> sibling exists and has information
   */
  static private final NodeValue NAP_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of NAP title");
      String tTitle = node.getTextContent();
      String tSubtitle = null;

      //is there a subtitle sibling?
      Node titleNextSibling = node.getNextSibling();
      // between here and the <subtitle> tag if it exists
      while ((titleNextSibling != null) & (!"subtitle".equals(titleNextSibling.getNodeName()))) {
        titleNextSibling = titleNextSibling.getNextSibling();
      }
      // we're either at subtitle or there wasn't one to check
      if (titleNextSibling != null) {
        tSubtitle = titleNextSibling.getTextContent();
      }
      
      // now build up the full title
      StringBuilder valbuilder = new StringBuilder();
      if (tTitle != null) {
        valbuilder.append(tTitle);
        if (tSubtitle != null) {
          if (tTitle.endsWith(":")) { // sometimes the title ends with the :
            valbuilder.append(" " + tSubtitle);
          } else {
            valbuilder.append(": " + tSubtitle);
          }
        }
      } else { 
        log.debug3("no title found within title group");
        return null;
      }
      log.debug3("title found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };
  
  /* 
   * DATE INFORMATION
   * We're at the top level of a "<display_date>"
   *     <day>27</day> optional
   *     <month>03></month> optional
   *     <year>1999</year>
   *     <epoch>1377576000</epoch> //don't care about this
   *   </display_date>   
   */
  static private final NodeValue NAP_DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of NAP display date");
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
  
  /* 
   *  NAP specific XPATH key definitions that we care about
   */

  private static String NAP_book = "/book";
  
  /* these are all relative to the /book node */
  private static String NAP_record_id =  "record_id";
  // unfortunately the subtitle is a separate sibling - use an evaluator
  private static String NAP_title =  "title";
  private static String NAP_flatisbn = "flat_isbn";
  // Get the isbn13 value from the product/item node with a type of 'pdf_book'
  private static String NAP_pdf_book_isbn13 = "product/item[type[text()='pdf_book']]/isbn13";  
  private static String NAP_copyyear = "copyright";
  private static String NAP_displaydate =  "display_date";

  // a semicolon separated string regardless of role      
  private static String NAP_author_string = "author";
  //"authoring" or "authors" - perhaps as a fallback? 
  private static String NAP_fullauthor =  "authors";
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> NAP_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    NAP_articleMap.put(NAP_record_id, XmlDomMetadataExtractor.TEXT_VALUE);
    NAP_articleMap.put(NAP_title, NAP_TITLE_VALUE);
    NAP_articleMap.put(NAP_flatisbn, XmlDomMetadataExtractor.TEXT_VALUE);
    NAP_articleMap.put(NAP_pdf_book_isbn13, XmlDomMetadataExtractor.TEXT_VALUE);
    NAP_articleMap.put(NAP_displaydate, NAP_DATE_VALUE);
    NAP_articleMap.put(NAP_copyyear, XmlDomMetadataExtractor.TEXT_VALUE);
    NAP_articleMap.put(NAP_author_string, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (article) has its own XML file */
  static private final String NAP_articleNode = NAP_book; 


  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    // cook the isbn13, if it's not there we'll manually put in flat_isbn value
    cookMap.put(NAP_pdf_book_isbn13, MetadataField.FIELD_ISBN);
    cookMap.put(NAP_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(NAP_author_string, 
        new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CHAR)));
    cookMap.put(NAP_displaydate, MetadataField.FIELD_DATE);
  }


  /**
   * NAP does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    
    return null;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return NAP_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return NAP_articleNode;
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
   */
  @Override
  public String getFilenameXPathKey() {
    return null;
  }
  

}
