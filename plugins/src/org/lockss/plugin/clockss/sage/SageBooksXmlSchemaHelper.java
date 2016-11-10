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

package org.lockss.plugin.clockss.sage;

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
 *  A helper class that defines a schema for SAGE Ebooks
 *  @author alexohlson
 */
public class SageBooksXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(SageBooksXmlSchemaHelper.class);
  
  static private final NodeValue EDITOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
    //<editor bio="664818"><orgName>CQ Press</orgName></editor>
    //<editor bio="504823"><persName><forename>Peter</forename> <surname>Roberts</surname></persName></editor>
      log.debug3("getValue of SAGE editor");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      // look at each child of the editor for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("orgName".equals(nodeName)) {
          return checkNode.getTextContent();
        } else if ("persName".equals(nodeName)) {
          String surname = null;
          String forename = null;
          NodeList nameChildren = checkNode.getChildNodes();
          if (nameChildren == null) return null;
          for (int p = 0; p < nameChildren.getLength(); p++) {
            Node partNode = nameChildren.item(p);
            String partName = partNode.getNodeName();
            if ("surname".equals(partName)) {
              surname  = partNode.getTextContent();
            } else if ("forename".equals(partName)) {
              forename = partNode.getTextContent();
            }
          }
          StringBuilder valbuilder = new StringBuilder();
          //isBlank checks for null, whitespace and empty
          if (!StringUtils.isBlank(surname)) {
            valbuilder.append(surname);
            if (!StringUtils.isBlank(forename)) {
              valbuilder.append(", " + forename);
            }
            return valbuilder.toString();
          } 
        }
      }
      return null;
    }
  };
  
  private static final String SB_Book = "/SAGE/teiHeader/fileDesc";
  
  private static final String sage_title = "titleStmt/title[@type = 'main']";
  private static final String sage_editor = "titleStmt/editor";
  private static final String sage_publisher = "publicationStmt/publisher";
  private static final String sage_date = "publicationStmt/date";
  private static final String sage_isbn_print = "publicationStmt/idno[@type = 'print']";
  private static final String sage_isbn_online = "publicationStmt/idno[@type = 'online']";
  private static final String sage_doi = "notesStmt/doi";

  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> SB_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    SB_articleMap.put(sage_title, XmlDomMetadataExtractor.TEXT_VALUE);
    SB_articleMap.put(sage_isbn_online, XmlDomMetadataExtractor.TEXT_VALUE);
    SB_articleMap.put(sage_isbn_print, XmlDomMetadataExtractor.TEXT_VALUE);
    SB_articleMap.put(sage_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    SB_articleMap.put(sage_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    SB_articleMap.put(sage_editor, EDITOR_VALUE);
    SB_articleMap.put(sage_date, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  static private final String SB_bookNode = SB_Book; 

  /* 3. in MARCXML there is no global information because one file/article */
  static private final Map<String,XPathValue> SB_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(sage_isbn_online, MetadataField.FIELD_ISBN);
    cookMap.put(sage_doi, MetadataField.FIELD_DOI);
    cookMap.put(sage_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(sage_editor, MetadataField.FIELD_AUTHOR);
    cookMap.put(sage_date, MetadataField.FIELD_DATE);
  }


  /**
   * no global map - this is null
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return SB_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return SB_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return SB_bookNode;
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
    return sage_isbn_print;
  }

}
