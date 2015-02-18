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

package org.lockss.plugin.clockss.knowledge;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for MARCXML metadata extraction for
 *  the Knowledge Unlimited publishing XML format
 *  The pdf filenames use the same name as the .xml file
 *  There is only one record for each file
 *  @author alexohlson
 */
public class MarcXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(MarcXmlSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";
  private static final String AUTHOR_SPLIT_CHAR = ";";


  /* 
   *  MARCXML specific XPATH key definitions that we care about
   */
  
  // Authors can come from a variety of tag options depending on type of 
  // publication and role - the XSL translators use these ones
  private static String author_tag_options = "@tag = \"100\" or @tag = \"110\"" +
  		" or @tag = \"111\" or @tag = \"700\" or @tag = \"710\"" +
  		" or @tag = \"711\" or @tag = \"720\"";
  
  // A top level for one book is:
  private static String MARC_book = "/collection/record";
  
  /* these are all relative to the /record node */
  private static String MARC_isbn =  "datafield[@tag = \"020\"]/subfield[@code=\"a\"]";
  private static String MARC_title = "datafield[@tag = \"245\"]";
  private static String MARC_author = "datafield[" + author_tag_options + "]/subfield[@code=\"a\"]";
  private static String MARC_publisher = "datafield[@tag = \"264\"]/subfield[@code=\"b\"]";
  private static String MARC_pub_date = "datafield[@tag = \"264\"]/subfield[@code=\"c\"]";

  
  /* 
   *  MARC XML  node evaluators to extract the information we want
   */
  
  
  /*
   *  Remove newlines and white space from around value
   *  used currently by AUTHOR
   */
  static private final NodeValue STRIP_AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      return StringUtils.strip(node.getTextContent(), ", ");
    }
  };
  
  /*
   * Normalize the publisher name
   *   It could have a trailing ,
   */
  static private final NodeValue STRIP_PUB_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of node");
      return StringUtils.strip(node.getTextContent(), ", ");
    }
  };
  
  /*
   *  Normalize the date:
   *    it could have a trailing "." 
   *    it could be in []
   *    Occasionally it might be in an odd form "Jun-14" - let that go through?
   */
  static private final NodeValue STRIP_DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of node");
      return StringUtils.strip(node.getTextContent(), "[]. ");
    }
  };
  
  /*
   * Normalize the ISBN
   *   It might have trailing parenthetical information
   *     9780472029525 (e-book)
   *   pick up all that match
   *   This may need to change, but so far the first one is always the "right" one
   *   for the e-book and when cooking, that one will get used.
   *   If there is no valid ISBN, post-cook will use the filename
   */
  protected static final Pattern isbnPattern = Pattern.compile("^(\\d{13})", Pattern.CASE_INSENSITIVE);
  static private final NodeValue ISBN_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      /*
       * There can be multiple ISBNs listed using the same tag/code "a"
       * And the one we want might not be clear (e-book), (electronic bk) (ebk) (epdf) but not (epub) 
       * pick up all the raw values - it will cook the first one
       * If no isbn is set, then post-cook we will use the filename, which just so happens to be the isbn....
       */
      Matcher isbnMat = isbnPattern.matcher(node.getTextContent());
      if (isbnMat.find()) {
        return MetadataUtil.validateIsbn(isbnMat.group(1), false);
      } 
      log.debug3("isbn value didn't start with 13 digits");
      return null;
    }
  };
  
  /*
   * Title is built up from sibling - subfield@code=a & subfield@code=b
   *   the titles are inconsistent on inclusion of : and padding spaces
   */
  
  static private final NodeValue TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of title data node");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      String tTitle = null;
      String sTitle = null;
      // look at each child of the this node for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("subfield".equals(nodeName)) {
          String code = ((Element)checkNode).getAttribute("code");
          if ("a".equals(code)) {
            // some have :, some not; some spaces, some not - make consistent
            tTitle = StringUtils.strip(checkNode.getTextContent(), "/: ");
          } else if ("b".equals(code)) {
              sTitle = StringUtils.strip(checkNode.getTextContent(), "/ ");
          }
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tTitle != null) {
        valbuilder.append(tTitle);
        if (sTitle != null) {
          valbuilder.append(" : " + sTitle);
        }
      }
      return valbuilder.toString();
    }
  };
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> MARC_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    MARC_articleMap.put(MARC_isbn, ISBN_VALUE);
    MARC_articleMap.put(MARC_title, TITLE_VALUE);
    MARC_articleMap.put(MARC_author, STRIP_AUTHOR_VALUE);
    MARC_articleMap.put(MARC_publisher, STRIP_PUB_VALUE);
    MARC_articleMap.put(MARC_pub_date, STRIP_DATE_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  static private final String MARC_articleNode = MARC_book; 

  /* 3. in MARCXML there is no global information because one file/article */
  static private final Map<String,XPathValue> MARC_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(MARC_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(MARC_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(MARC_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(MARC_publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(MARC_pub_date, MetadataField.FIELD_DATE);
  }


  /**
   * MARCXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return MARC_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return MARC_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return MARC_articleNode;
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
