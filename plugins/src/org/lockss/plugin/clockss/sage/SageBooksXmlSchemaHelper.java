/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
