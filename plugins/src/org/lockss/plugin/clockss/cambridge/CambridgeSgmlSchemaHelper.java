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

package org.lockss.plugin.clockss.cambridge;

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
 *  A helper class that defines a schema for XML metadata extraction for
 *  cambridge legacy SGML metadata files
 *  
 */
public class CambridgeSgmlSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(CambridgeSgmlSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";
  
  /* 
   * AUTHOR INFORMATION
   * <au>
   *   <fnms>GUSTAF</fnms>
   *   <snm>ARRHENIUS</snm>
   *   <norm>Arrhenius G</norm>
   *   <orf rid="a1">
   * </au>
   */
  static private final NodeValue CAMB_AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of sgml author");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      String tsurname = null;
      String tgiven = null;
      // look at each child 
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("snm".equals(nodeName)) {
          tsurname = checkNode.getTextContent();
        } else if ("fnms".equals(nodeName) ) {
          tgiven = checkNode.getTextContent();
        } 
      }

      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, empty & whitespace only
      if (!StringUtils.isBlank(tsurname)) {
        valbuilder.append(tsurname);
        if (!StringUtils.isBlank(tgiven)) {
          valbuilder.append(AUTHOR_SEPARATOR + " " + tgiven);
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
   *  Cambridge SGML specific XPATH key definitions that we care about
   */
  // The following are all under the article node
  private static String pub_name = "issue/pinfo/pnm";
  private static String pub_jid = "issue/jinfo/jid";
  private static String pub_title = "issue/jinfo/jtl";
  private static String pub_issn = "issue/jinfo/issn";
  private static String pub_eissn = "issue/jinfo/eissn";
  private static String pub_volume = "issue/pubinfo/vid";
  private static String pub_issue = "issue/pubinfo/iid";
  private static String pub_year = "issue/pubinfo/cd[@year]";
  // article level
  // The title that does not have the purpose attribute set is the choice
  private static String art_title = "artcon/genhdr/tig/atl[not(@purpose)]";
  private static String art_auth = "artcon/genhdr/aug/au";
  private static String art_doi = "artcon/genhdr/artinfo/doi";
  private static String art_sp = "artcon/genhdr/artinfo/ppf";
  private static String art_lp = "artcon/genhdr/artinfo/ppl";

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  camb_articleMap = new HashMap<String,XPathValue>();
  static {
    camb_articleMap.put(pub_name, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_jid, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_issn, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_eissn, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_auth, CAMB_AUTHOR_VALUE);

  }

  /* 2.  Top level Nodepath */
  //static private final String camb_articleNode = "/article/header";
  // could be /article/header or /header...
  static private final String camb_articleNode = "(/article/header | /header)"; 

  /* 3. WK global value we care about: none, so WK_globalMap is null */ 

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_issn, MetadataField.FIELD_ISSN);
    cookMap.put(pub_eissn, MetadataField.FIELD_EISSN);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(art_doi, MetadataField.FIELD_DOI);
    cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
    cookMap.put(art_lp, MetadataField.FIELD_END_PAGE);
    cookMap.put(art_auth, MetadataField.FIELD_AUTHOR);
  }

  /**
   * WK has a single global information outside of article records
   * return global map
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    //no globalMap, so returning null
    return null;
  }

  /**
   * return  article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return camb_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return camb_articleNode;
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