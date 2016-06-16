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

package org.lockss.plugin.clockss.phildoc;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Philosophy Documentation Center issue TOC XML files
 *  
 */
public class PhilDocSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(PhilDocSchemaHelper.class);


  /* 
   *  Philosophy Documentation Center 
   */
  // The following are all under the article node /doc
  private static String pub_issn = "field[@name=\"issn\"]";
  private static String pub_title = "field[@name=\"publication\"]";
  private static String art_title = "field[@name=\"title\"]";
  public static String art_subtitle = "field[@name=\"subtitle\"]";
  private static String pub_volume = "field[@name=\"volume\"]";
  private static String pub_issue = "field[@name=\"issue\"]";
  private static String pub_year = "field[@name=\"year\"]";
  private static String art_auth = "field[@name=\"author\"]";
  private static String art_sp = "field[@name=\"pagenumber_first\"]";
  private static String art_lp = "field[@name=\"pagenumber_last\"]";
  private static String art_filename = "field[@name=\"imuse_id\"]";  

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  pdoc_articleMap = new HashMap<String,XPathValue>();
  static {
    pdoc_articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(pub_issn, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_subtitle, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_auth, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_filename, XmlDomMetadataExtractor.TEXT_VALUE); 
  }

  /* 2.  Top level per-article node */
  static private final String pdoc_articleNode = "/add/doc";

  /* 3. Global metadata is null */
  static private final Map<String, XPathValue> pdoc_globalNode = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_issn, MetadataField.FIELD_ISSN);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
    cookMap.put(art_lp, MetadataField.FIELD_END_PAGE);
    cookMap.put(art_auth, MetadataField.FIELD_AUTHOR);
  }

  /**
   * PhilDoc has no global metadata
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    //no globalMap, so returning null
    return pdoc_globalNode; 
  }

  /**
   * return  article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return pdoc_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return pdoc_articleNode;
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
    return art_filename;
  }

}