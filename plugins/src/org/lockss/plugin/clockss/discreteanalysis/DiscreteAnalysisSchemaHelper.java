/*
 * $Id:$
 */

/*

 Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.discreteanalysis;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
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
 *  the MATHSCINET tagset with the filenames based on same name as the .xml file
 *  There is only one record for each file
 */
public class DiscreteAnalysisSchemaHelper implements SourceXmlSchemaHelper {
  
  private static final Logger log = Logger.getLogger(DiscreteAnalysisSchemaHelper.class);
  
  private static final String GNAME = "given_name";
  private static final String SNAME = "surname";
  private static final String SUFFIX = "suffix";
  
  /* 
   * AUTHOR INFORMATION - we're at an 'Author' node. 
   *     there could be more than one author
   *   <author>
   *     <given_name>Manuel</given_name>
   *     <surname>Kauers</surname>
   *     <institution>Johannes Kepler Universität</institution>
   *   </author>
   * 
   * There can be multiple authors
   */
  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of author");
      
      String givenName = null;
      String surName = null;
      String suffix = null;
      NodeList childNodes = node.getChildNodes(); 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        String nodeName = infoNode.getNodeName();
        if (GNAME.equals(nodeName)) {
          givenName = StringUtils.strip(infoNode.getTextContent());
        } else if (SNAME.equals(nodeName)) {
          surName = StringUtils.strip(infoNode.getTextContent());
        } else if (SUFFIX.equals(nodeName)) {
          suffix = StringUtils.strip(infoNode.getTextContent());
        }
      }
      if (givenName == null && surName == null) {
        log.debug3("No recognizable author schema in this author");
        return null;
      }
      // names are either in "givenName" in their entirety, or spread
      // across "given" + "surname" + "suffix"
      StringBuilder valbuilder = new StringBuilder();
      valbuilder.append(givenName);
      if (surName != null) {
        valbuilder.append(" " + surName);
      }
      if (!StringUtils.isBlank(suffix)) {
        // <suffix/> yields an single WS string
        valbuilder.append(", " + suffix);
      }
      return valbuilder.toString();
    }
  };
  
  
  /* 
   *  Specific XPATH key definitions that we care about
   *  There is only one article per xml file and the 
   *  filename.xml == filename.pdf == Article PII (internal ID)
   */
  
  private static String DA_article_node = "/journal_article";
  
  // article level
  private static String DA_doi =  DA_article_node + "/doi";
  private static String DA_title = DA_article_node + "/title";
//  private static String DA_language = DA_article_node + "/language";
  private static String DA_fpage = DA_article_node + "/first_page";
  private static String DA_lpage = DA_article_node + "/last_page"; 
  private static String DA_author = DA_article_node + "/author"; 
  private static String DA_year = DA_article_node + "/year";
  private static String DA_eissn =  DA_article_node + "/eissn";
  private static String DA_jtitle =  DA_article_node + "/journal_title";
  private static String DA_source =  DA_article_node + "/source";
  // not mapping: abstract, msc ()
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */
  
  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> DA_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    DA_articleMap.put(DA_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    DA_articleMap.put(DA_title, XmlDomMetadataExtractor.TEXT_VALUE);
    DA_articleMap.put(DA_fpage, XmlDomMetadataExtractor.TEXT_VALUE);
    DA_articleMap.put(DA_lpage, XmlDomMetadataExtractor.TEXT_VALUE);
    DA_articleMap.put(DA_author, AUTHOR_VALUE);
    DA_articleMap.put(DA_year, XmlDomMetadataExtractor.TEXT_VALUE);
    DA_articleMap.put(DA_eissn, XmlDomMetadataExtractor.TEXT_VALUE);
    DA_articleMap.put(DA_jtitle, XmlDomMetadataExtractor.TEXT_VALUE);
    DA_articleMap.put(DA_source, XmlDomMetadataExtractor.TEXT_VALUE);
  }
  
  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    // XXX should also get PROVIDER from the TDB file??? XXX
    // cookMap.put(DA_source, MetadataField.FIELD_PROVIDER);
    cookMap.put(DA_jtitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(DA_eissn, MetadataField.FIELD_EISSN);
    cookMap.put(DA_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(DA_doi, MetadataField.FIELD_DOI);
    cookMap.put(DA_fpage, MetadataField.FIELD_START_PAGE);
    cookMap.put(DA_lpage, MetadataField.FIELD_END_PAGE);
    cookMap.put(DA_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(DA_year, MetadataField.FIELD_DATE);
    
  }


  /**
   * DA does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return null;
  }

  /**
   * return DA article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return DA_articleMap;
  }

  /**
   * Return the article node path
   * There is only one article per xml file so the top of the document is the
   * article and all paths are relative do document.
   */
  @Override
  public String getArticleNode() {
    return null;
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
