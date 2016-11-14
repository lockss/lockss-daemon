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

package org.lockss.plugin.clockss.nature;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for Nature Publishing Group
 *  XML files (NPG DTD)
 *  @author alexohlson
 */
public class NpgXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(NpgXmlSchemaHelper.class);
  
  private static final String AUTHOR_SEPARATOR = ",";

  /* 
   * AUTHOR INFORMATION
   * <au><fnm>Xerxes</fnm><snm>Pundole</snm><inits>X</inits><orf rid="a1"/></au>
   */
  static private final NodeValue AUTHOR_VAL = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of npg author");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      String sn = null;
      String fn = null;
      String inits = null;
      // look at each child 
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("snm".equals(nodeName)) {
          sn = checkNode.getTextContent();
        } else if ("fnm".equals(nodeName) ) {
          fn = checkNode.getTextContent();
        } else if ("inits".equals(nodeName) ) {
          inits = checkNode.getTextContent();
        } 
      }

      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, empty & whitespace only
      if (!StringUtils.isBlank(sn)) {
        valbuilder.append(sn);
        if (!StringUtils.isBlank(fn) || !StringUtils.isBlank(inits)) {
          valbuilder.append(AUTHOR_SEPARATOR);
          // only use the inits if the fname is blank...
          if (!StringUtils.isBlank(fn)) {
            valbuilder.append(" " + fn);
          } else if (!StringUtils.isBlank(inits)) {
          valbuilder.append(" " + inits);
          }
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
   *  NPG XML specific XPATH key definitions that we care about
   */
    private static final String NPG_issn = "pubfm/issn";
    private static final String NPG_ptitle = "pubfm/jtl";
    private static final String NPG_publisher = "pubfm/cpg/cpn"; 
    public static final String NPG_copyyear = "pubfm/cpg/cpy"; 
    private static final String NPG_volume = "pubfm/vol";
    private static final String NPG_issue = "pubfm/issue";
    private static final String NPG_atitle = "fm/atl";
    private static final String NPG_doi = "pubfm/doi";
    private static final String NPG_author = "(fm/aug/cau | fm/aug/au)";
    private static final String NPG_pub_year = "fm/pubdate";

    static private final Map<String,XPathValue> NPG_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    NPG_articleMap.put(NPG_issn, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_ptitle, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_volume, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_atitle, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_author, AUTHOR_VAL);
    NPG_articleMap.put(NPG_pub_year, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_copyyear, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  static private final String NPG_article = "/article"; 

  /* 3. in MARCXML there is no global information because one file/article */
  static private final Map<String,XPathValue> NPG_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(NPG_issn, MetadataField.FIELD_ISSN);
    cookMap.put(NPG_ptitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(NPG_atitle, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(NPG_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(NPG_doi, MetadataField.FIELD_DOI);
    cookMap.put(NPG_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(NPG_publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(NPG_pub_year, MetadataField.FIELD_DATE);
  }


  /**
   * NPGXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return NPG_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return NPG_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return NPG_article;
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
