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

package org.lockss.plugin.clockss.casalini;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.w3c.dom.Node;

/**
 *  A helper class that defines a schema for Casalini Libri
 *  Monographs which were delivred with a monographs.mrc
 *  marc data record collection which we turned in to a marcxml 21
 *  XML file 
 *  @author alexohlson
 */
public class CasaliniMarcXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(CasaliniMarcXmlSchemaHelper.class);
  

  
  //MARCXML is opaque. Each item is a datafield with a numbered tag
  // with values stored in subfields identified by a code, eg
  // <marc:datafield tag="097" ind1="0" ind2=" ">
  //    <marc:subfield code="a">2249531</marc:subfield>
  //    <marc:subfield code="b">015</marc:subfield>
  //    <marc:subfield code="c">2279430</marc:subfield>
  //    <marc:subfield code="d">0001</marc:subfield>
  // </marc:datafield>
  // Define tags/codes here for the bits we can use
  private static final String ISBN_TAG ="020";
  private static final String isbn_code ="a";
  
  private static final String LOCATOR1_TAG ="092";
  private static final String LOCATOR2_TAG ="097";
  private static final String dir_code ="a";
  private static final String file_code ="c";

  private static final String TITLE_TAG ="245";
  private static final String title_code ="a";
  private static final String subtitle_code ="b";
  private static final String author_code ="c";

  private static final String PUBLICATION_TAG ="260";
  private static final String pub_code ="b";
  private static final String pubdate_code ="c";

  private static final String NAME_TAG ="700";
  private static final String name_code ="a";
  
  private static final String ID_TAG ="773";
    private static final String isbn_id_code ="z";
  
  

  // A top level for the worksheet table is
  private static String MARC_record = "/collection/record";

  // these are all relative to the record
  public static String MARC_dir =  
      "datafield[@tag = \"" + LOCATOR2_TAG + "\"]" +
          "/subfield[@code = \"" + dir_code + "\"]";
  public static String MARC_file =  
      "datafield[@tag = \"" + LOCATOR2_TAG + "\"]" +
          "/subfield[@code = \"" + file_code + "\"]";
  private static String MARC_id_isbn =  
      "datafield[@tag = \"" + ID_TAG + "\"]" +
          "/subfield[@code = \"" + isbn_id_code + "\"]";
  public static String MARC_isbn =  
      "datafield[@tag = \"" + ISBN_TAG + "\"]" +
          "/subfield[@code = \"" + isbn_code + "\"]";
  public static String MARC_title = 
      "datafield[@tag = \"" + TITLE_TAG + "\"]" +
          "/subfield[@code = \"" + title_code + "\"]";
  public static String MARC_subtitle = 
      "datafield[@tag = \"" + TITLE_TAG + "\"]" +
          "/subfield[@code = \"" + subtitle_code + "\"]";
  private static String MARC_pub_date = 
      "datafield[@tag = \"" + PUBLICATION_TAG + "\"]" +
          "/subfield[@code = \"" + pubdate_code + "\"]";
  public static String MARC_publisher = 
      "datafield[@tag = \"" + PUBLICATION_TAG + "\"]" +
          "/subfield[@code = \"" + pub_code + "\"]";
  private static String MARC_author = 
      "datafield[@tag = \"" + NAME_TAG + "\"]" +
          "/subfield[@code = \"" + name_code + "\"]";
  private static String MARC_altauthor = 
      "datafield[@tag = \"" + TITLE_TAG + "\"]" +
          "/subfield[@code = \"" + author_code + "\"]";
  
  
  /*
   *  Put date in to a format we want
   *  Variants given to us are:
   *  2000
   *  2000 (printed 1999)
   *  2000.
   *  2000-
   *  2000-2002
   *  2000 (2005 printing)
   *  [2000]
   *  Strip off punctuation
   *  Take the first four digit year date
   */
  
  static private final NodeValue ADATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String cleandate =  StringUtils.stripStart(node.getTextContent(), ".-[] ");
      return StringUtils.substring(cleandate,0,4);
    }
  };
  
  /*
   *  Cleanup the title or subtitle so they look nice 
   */
  static private final NodeValue TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      // remove extraneous spaces and punctuation
      // so that when put together Title: Subtitle it looks correct
      //
      return StringUtils.strip(node.getTextContent(), ",/: "); 
    }
  };
  
  // could be 13 or 10, take out hyphens
  static private final NodeValue ISBN_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String justisbn = StringUtils.strip(node.getTextContent(), "(* ");
      return StringUtils.remove(justisbn, "-");
    }
  };

  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> casalini_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    casalini_articleMap.put(MARC_isbn, ISBN_VALUE);
    casalini_articleMap.put(MARC_id_isbn, ISBN_VALUE);
    casalini_articleMap.put(MARC_title, TITLE_VALUE);
    casalini_articleMap.put(MARC_subtitle, TITLE_VALUE);
    casalini_articleMap.put(MARC_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_pub_date,  ADATE_VALUE);
    casalini_articleMap.put(MARC_author, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_dir, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_file, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_altauthor, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. there is only one XML file */
  static private final String casalini_recordNode = MARC_record;

  /* 3. in MARCXML there is no global information we care about */
  static private final Map<String,XPathValue> MARC_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // More of the records have this rather than the MARC_isbn
    // postCookProcess to add in missing values from raw data
    cookMap.put(MARC_id_isbn, MetadataField.FIELD_ISBN);
    // we defer attributing this until postCookProcess when we can 
    // determin if it is an article (chapter) title or a publication title
    //cookMap.put(MARC_title, MetadataField.FIELD_PUBLICATION_TITLE);
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
    return casalini_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return casalini_recordNode;
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
    return MARC_dir;
  }

}
