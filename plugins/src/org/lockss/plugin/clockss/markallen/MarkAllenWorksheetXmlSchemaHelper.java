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

package org.lockss.plugin.clockss.markallen;

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
 *  A helper class that defines a schema for Mark Allen Group books 
 *  The metadata comes from an XML file that was created via export
 *  from an excel spreadsheet.
 *  We count on the format and ordering of the columns to stay
 *  consistent
 *  @author alexohlson
 */
public class MarkAllenWorksheetXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(MarkAllenWorksheetXmlSchemaHelper.class);
  
  /*
   *  Put date in to a format we want
   *  <Data ss:Type="DateTime">2013-02-26T00:00:00.000</Data>
   *  return just the 2013-02-26
   */
  static private final NodeValue DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      // This is a smart function 
      // it returns what is given as the first arg (null, empty or the entire
      // thing if the separator isn't found (in this case the "T"
      return StringUtils.substringBefore(node.getTextContent(), "T");
    }
  };

  /* 
   *  MAG worksheet specific XPATH key definitions that we care about
   */
  

  
  // We count on column positioning being consistent
  private static final String EISBN_COL ="1";
  private static final String ISBN_COL ="2";
  private static final String TITLE_COL ="3";
  private static final String SUBT_COL ="4";
  private static final String ED_COL ="5";
  private static final String DATE_COL ="6";
  private static final String AUTH_COL ="7";
  // A top level for the worksheet table is
  private static String MAG_table = "/Workbook/Worksheet/Table";
  private static String MAG_book = MAG_table + "/Row[position()>1]"; 

  // The first ROW in the table are headers 
  // each subsequent row represents a new record, one book/row 
  
  // these are all relative to the row  
  private static String MAG_eisbn =  "Cell[" + EISBN_COL + "]/Data";
  private static String MAG_isbn =  "Cell[" + ISBN_COL + "]/Data";
  private static String MAG_title = "Cell[" + TITLE_COL + "]/Data";
  public static String MAG_subtitle = "Cell[" + SUBT_COL + "]/Data";
  public static String MAG_edition = "Cell[" + ED_COL + "]/Data";
  private static String MAG_pub_date = "Cell[" + DATE_COL + "]/Data";
  private static String MAG_author = "Cell[" + AUTH_COL + "]/Data";
  

  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> MAG_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    MAG_articleMap.put(MAG_eisbn, XmlDomMetadataExtractor.TEXT_VALUE);
    MAG_articleMap.put(MAG_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    MAG_articleMap.put(MAG_title, XmlDomMetadataExtractor.TEXT_VALUE);
    MAG_articleMap.put(MAG_subtitle, XmlDomMetadataExtractor.TEXT_VALUE);
    MAG_articleMap.put(MAG_edition, XmlDomMetadataExtractor.TEXT_VALUE);
    MAG_articleMap.put(MAG_pub_date, DATE_VALUE);
    MAG_articleMap.put(MAG_author, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  static private final String MAG_bookNode = MAG_book; 

  /* 3. in MARCXML there is no global information because one file/article */
  static private final Map<String,XPathValue> MAG_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(MAG_eisbn, MetadataField.FIELD_EISBN);
    cookMap.put(MAG_isbn, MetadataField.FIELD_ISBN);
    // we will add subtitle/edition as needed in post-cook process
    cookMap.put(MAG_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(MAG_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(MAG_pub_date, MetadataField.FIELD_DATE);
  }


  /**
   * MARCXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return MAG_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return MAG_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return MAG_bookNode;
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
    return MAG_eisbn;
  }

}
