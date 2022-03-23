/*
 * $Id$
 */

/*

 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.eastview;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Eastview Information Services
 *  
 */
public class EastviewNewspaperMetadataHelper
implements SourceXmlSchemaHelper {
  
  private static final Logger log = Logger.getLogger(EastviewNewspaperMetadataHelper.class);

  static protected final String PAGE_PDF_PATH = "/ARTICLEDATAS/ARTICLEDATA/TEXT/pagemark/@pagepdf";
  static protected final String PUBLICATION_TITLE_PATH = "/ARTICLEDATAS/ARTICLEDATA/SOURCE";

  /*
   * <PAGE>3</PAGE>
   * or
   * <PAGE>3-6</PAGE>
   */
  private final static NodeValue STARTPAGE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of PAGE - interpret for start page");
      String pageVal = node.getTextContent();
      if (pageVal != null)  {
        return StringUtils.substringBefore(pageVal,"-");
      } else {
        log.debug3("no page value set");
        return null;
      }
    }
  };
  
  private final static NodeValue CLEAN_TEXT = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("Cleaning text value - strip lead/trail space");
      return StringUtils.strip(node.getTextContent());
    }
  };

  // For newspaper, we only get the first article of the xml/ARTICLEDATAS/ARTICLEDATA
  private static final String top = "/ARTICLEDATAS/ARTICLEDATA[1]";

  // The following are all relative to the article node
  // from the immediately preceeding sibling -
  private static String pub_year = "DATE";
  private static String pub_volume = "VOLUME";
  private static String pub_issue = "NUMBER";
  protected static String ART_RAW_ATITLE = "ATITLE";
  protected static String ART_RAW_TITLE = "TITLE";
  private static String art_contrib = "AUTHOR";
  private static String art_sp = "PAGE";
  

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    articleMap.put(PUBLICATION_TITLE_PATH, CLEAN_TEXT);
    //This is raw title, it contains publisher acroname, like this:
    //title: 31-05-1961(DASZD-No.005) НОВЫЕ КНИГИ
    articleMap.put(ART_RAW_TITLE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(ART_RAW_ATITLE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(art_contrib, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PAGE_PDF_PATH, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2.  Top level per-article node */
  static private final String articleNode = top;

  /* 3. Global metadata is the publisher - work around if it gets troublesome */
  static private final Map<String, XPathValue> 
    globalMap = null; 
  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final String AUTHOR_SPLIT_CH = ",";
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(PUBLICATION_TITLE_PATH, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(art_contrib, 
        new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CH)));
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
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
    return null;
  }

}