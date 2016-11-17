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

package org.lockss.plugin.clockss.aofoundation;

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


public class PubmedArticleXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(PubmedArticleXmlSchemaHelper.class);

  /*
  * <Author>
  * <FirstName>S</FirstName><MiddleName></MiddleName><LastName>Creasey</LastName><Suffix></Suffix>
  * </Author>
  */
  private static final String AUTHOR_SEPARATOR = ",";
  static private final NodeValue AUTHOR_VAL = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of npg author");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      String ln = null;
      String fn = null;
      // look at each child 
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("FirstName".equals(nodeName)) {
          fn = checkNode.getTextContent();
        } else if ("LastName".equals(nodeName) ) {
          ln = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, empty & whitespace only
      if (!StringUtils.isBlank(ln)) {
        valbuilder.append(ln);
        if (!StringUtils.isBlank(fn)) {
          valbuilder.append(AUTHOR_SEPARATOR);
          // only use the inits if the fname is blank...
            valbuilder.append(" " + fn);
        }
      } else {
        log.debug3("no author found");
        return null;
      }
      log.debug3("author found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  /* 
   *  Pubmed Article specific XPATH key definitions that we care about
   */
    private static String PM_article = "/ArticleSet/Article";
  
  /* these are all relative to the article node */
  private static String PM_issn =  "Journal/Issn";
  private static String PM_ptitle = "Journal/JournalTitle";
  private static String PM_volume = "Journal/Volume";
  private static String PM_issue = "Journal/Issue";
  private static String PM_publisher = "Journal/PublisherName";
  private static String PM_pub_year = "Journal/PubDate/Year";

  private static String PM_atitle = "ArticleTitle";
  private static String PM_spage = "FirstPage";
  private static String PM_epage = "LastPage";
  private static String PM_author = "AuthorList/Author";
  private static String PM_pii = "ArticleIdList/ArticleId[@IdType = 'pii']";
    

  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  static {
    articleMap.put(PM_issn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_ptitle, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_volume, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_atitle, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_pub_year, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_spage, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_epage, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_pii, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(PM_author, AUTHOR_VAL);
  }

  /* 2. Each item (book) has its own XML file */
  static private final String PM_article_node = PM_article; 

  /* 3. in MARCXML there is no global information because one file/article */
  static private final Map<String,XPathValue> PM_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(PM_issn, MetadataField.FIELD_ISSN);
    cookMap.put(PM_ptitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(PM_publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(PM_pub_year, MetadataField.FIELD_DATE);
    cookMap.put(PM_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(PM_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(PM_atitle, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(PM_spage, MetadataField.FIELD_START_PAGE);
    cookMap.put(PM_epage, MetadataField.FIELD_END_PAGE);
    cookMap.put(PM_author, MetadataField.FIELD_AUTHOR);
  }


  /**
   * MARCXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return PM_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
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
    return PM_article_node;
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
    return PM_pii;
  }

}
