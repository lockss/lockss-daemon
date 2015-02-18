/*
 * $Id$
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americaninstituteofphysics;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  AIPJats source files
 *  
 */
public class AIPJatsXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(AIPJatsXmlSchemaHelper.class);

  private static final String NAME_SEPARATOR = ", ";

  /* 
   *  AIPJats specific node evaluators to extract the information we want
   */
  /*
   * AUTHOR information
   * NODE=<Contrib-group>  
   *  contrib/@contrib-type=author
   *  name/
   *  surname/
   *  given-names/
   */
  static private final NodeValue AIPJATS_AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of AIPJATS contributor");
      NodeList childNodes = node.getChildNodes(); 
      String sname = null;
      String gname = null;
      StringBuilder names = new StringBuilder();
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        String nodeName = infoNode.getNodeName();
        /*
         <name-alternatives> adds a level, if present
         child nodes include <name> and <string-name>
          /article/front/article-meta/contrib-group/contrib
          /article/front/article-meta/contrib-group/contrib/@contrib-type=author
          /article/front/article-meta/contrib-group/contrib/name-alternatives/name/@name-style=western
          /article/front/article-meta/contrib-group/contrib/name-alternatives/name/surname=Jiang
          /article/front/article-meta/contrib-group/contrib/name-alternatives/name/given-names=P.
          /article/front/article-meta/contrib-group/contrib/name-alternatives/string-name/@name-style=eastern
         */
        if ("name-alternatives".equals(nodeName)){
          NodeList nNodes = infoNode.getChildNodes();
          for (int y = 0; y < childNodes.getLength(); y++) {
            infoNode = nNodes.item(y);
            nodeName = infoNode.getNodeName();
            if ("name".equals(nodeName))
              break;
          }
        }
        if ("name".equals(nodeName)) {
          NodeList nNodes = infoNode.getChildNodes();
          for (int x = 0; x < nNodes.getLength(); x++){
            Node nameNode = nNodes.item(x);
            String namePart = nameNode.getNodeName();
            if("surname".equals(namePart)){
              sname = nameNode.getTextContent();
            } else if ("given-names".equals(namePart)){
              gname = nameNode.getTextContent();
            }
          }
        }
      }
      if ((sname == null) && (gname == null)) {
        log.debug3("no valid contributor found");
        return null;
      }
      // now at least some of the name is available - return as much as possible
      // if we try to names.append(null) it adds "null" - don't want that!
      if (sname != null) {
        names.append(sname);
        if (gname != null) {
          names.append (NAME_SEPARATOR + gname);
        } // else only a surname
        return names.toString();
      } else {  // else only a givenname
        names.append(gname);
      }
      return names.toString();
    }
  };

  /* 
   * PUBLISHING DATE - could be under one of two nodes
   * NODE=<pub-date/>
   *   <month=>
   *   <day=>
   *   <year=>
   */
  static private final NodeValue AIPJATS_DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of AIPJATS date");
      NodeList childNodes = node.getChildNodes();
      if (childNodes == null) return null;
      String pubType = null;
      String year = null;
      String month = null;
      String day = null;
      String datetype = null;
      String dDate = null;

      if (node.getNodeName().equals("pub-date")) {
        for (int m = 0; m < childNodes.getLength(); m++) {
          Node childNode = childNodes.item(m);
          if ("pub-type".equals(childNode.getNodeName())) {
            pubType = childNode.getTextContent();
          } else if (childNode.getNodeName().equals("day")) {
            day = childNode.getTextContent();
          } else if (childNode.getNodeName().equals("month")) {
            month = childNode.getTextContent();
          } else if (childNode.getNodeName().equals("year")) {
            year = childNode.getTextContent();
          }
        }
      } 

      // make it W3C format instead of YYYYMMDD
      StringBuilder dBuilder = new StringBuilder();
      dBuilder.append(year); //YYYY
      dBuilder.append("-");
      dBuilder.append(month); //MM
      dBuilder.append("-");
      dBuilder.append(day); //DD
      return dBuilder.toString();

    }
  };

  static private final NodeValue AIPJATS_ARTICLE_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of AIPJATS ARTICLE TITLE");
      String title = null;
      String nodeName = null;
      StringBuilder titleVal = new StringBuilder();
      NodeList childNodes = node.getChildNodes(); 

      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        nodeName = infoNode.getNodeName();

        if("#text".equals(nodeName)) {
          title = infoNode.getTextContent();
          titleVal.append(title);
        }
        else if("inline-formula".equals(nodeName)){
          title = infoNode.getTextContent();
          titleVal.append(" ... ");
        }
      }
      if (titleVal.length() != 0)  {
        log.debug3("article title: " + titleVal.toString());
        return titleVal.toString();
      } else {
        log.debug3("no value in this article title");
        return null;
      }
    }
  };

  /* 
   *  AIPJats specific XPATH key definitions that we care about
   */

  /* Under an item node, the interesting bits live at these relative locations */
  private static String AIPJATS_JMETA = "journal-meta";

  private static String AIPJATS_issn = AIPJATS_JMETA + "/issn"; //issn[@pub-type = 'ppub']
  private static String AIPJATS_issntype_ppub = AIPJATS_issn + "[@pub-type = 'ppub']";
  private static String AIPJATS_issntype_epub = AIPJATS_issn + "[@pub-type= 'epub']";
  /* journal id */
  private static String AIPJATS_journal_id = AIPJATS_JMETA + "/journal-id[@journal-id-type = 'coden']";
  private static String AIPJATS_journal_title = AIPJATS_JMETA + "/journal-title-group/journal-title";

  /* components under Publisher */
  private static String AIPJATS_publisher = AIPJATS_JMETA + "/publisher";  
  private static String AIPJATS_publisher_name = AIPJATS_publisher + "/publisher-name";

  private static String AIPJATS_AMETA = "article-meta";
  /* article title */
  private static String AIPJATS_article_title = AIPJATS_AMETA + "/title-group/article-title";
  /* article id */
  private static String AIPJATS_article_id = AIPJATS_AMETA + "/article-id";
  private static String AIPJATS_doi = AIPJATS_article_id + "[@pub-id-type='doi']";
  /* vol, issue */
  private static String AIPJATS_issue = AIPJATS_AMETA + "/issue";
  private static String AIPJATS_vol = AIPJATS_AMETA + "/volume";
  /* published date */
  private static String AIPJATS_pubdate = AIPJATS_AMETA + "/pub-date";

  /* xpath contrib == author */
  private static String AIPJATS_author = AIPJATS_AMETA + "/contrib-group/contrib[@contrib-type = 'author']";

  /* access_url  not set here */

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  AIPJATS_articleMap = new HashMap<String,XPathValue>();
  static {
    AIPJATS_articleMap.put(AIPJATS_issntype_ppub, XmlDomMetadataExtractor.TEXT_VALUE); 
    AIPJATS_articleMap.put(AIPJATS_issntype_epub, XmlDomMetadataExtractor.TEXT_VALUE); 
    AIPJATS_articleMap.put(AIPJATS_publisher_name, XmlDomMetadataExtractor.TEXT_VALUE); 
    AIPJATS_articleMap.put(AIPJATS_journal_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    AIPJATS_articleMap.put(AIPJATS_doi, XmlDomMetadataExtractor.TEXT_VALUE); 
    AIPJATS_articleMap.put(AIPJATS_article_title, AIPJATS_ARTICLE_TITLE_VALUE); 
    AIPJATS_articleMap.put(AIPJATS_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    AIPJATS_articleMap.put(AIPJATS_vol, XmlDomMetadataExtractor.TEXT_VALUE);
    AIPJATS_articleMap.put(AIPJATS_author, AIPJATS_AUTHOR_VALUE);
    AIPJATS_articleMap.put(AIPJATS_journal_id, XmlDomMetadataExtractor.TEXT_VALUE);
    AIPJATS_articleMap.put(AIPJATS_pubdate, AIPJATS_DATE_VALUE);

  }

  /* 2. Each item (book) has its own subNode */
  static private final String AIPJATS_articleNode = "/article/front"; 

  /* 3. in ONIX, there is no global information we care about, it is repeated per article */ 
  static private final Map<String,XPathValue> AIPJATS_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(AIPJATS_issntype_ppub, MetadataField.FIELD_ISSN);
    cookMap.put(AIPJATS_issntype_epub, MetadataField.FIELD_EISSN);
    cookMap.put(AIPJATS_doi, MetadataField.FIELD_DOI);
    cookMap.put(AIPJATS_vol, MetadataField.FIELD_VOLUME);
    cookMap.put(AIPJATS_issue, MetadataField.FIELD_ISSUE);
    //cookMap.put(AIPJATS_journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
    // using deprecated FIELD_JOURNAL_TITLE until updated everywhere
    cookMap.put(AIPJATS_journal_title, MetadataField.FIELD_JOURNAL_TITLE);
    cookMap.put(AIPJATS_article_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(AIPJATS_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(AIPJATS_publisher_name, MetadataField.FIELD_PUBLISHER);
    cookMap.put(AIPJATS_pubdate, MetadataField.FIELD_DATE);
    cookMap.put(AIPJATS_journal_id, MetadataField.FIELD_PROPRIETARY_IDENTIFIER);

  }

  /**
   * ONIX2 does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return null;
  }

  /**
   * return AIPJats article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return AIPJATS_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return AIPJATS_articleNode;
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
    throw new UnsupportedOperationException("getFilenameKey() never called");
  }

}  