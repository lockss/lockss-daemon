/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.elsevier;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;


public class ElsevierMainDTD5XmlSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(ElsevierMainDTD5XmlSchemaHelper.class);

  static final String AUTHOR_SEPARATOR = ",";
  static final String AUTHOR_SPLIT_CHAR = ";";

  /*
   * XPATH DEFINITIONS WE CARE ABOUT
   */


  /*
   *  article-level main.xml  - definitions used in the MetadataExtraactor to pull
   *  the remaining metadata from the article metadata file 
   */

  private static final String top_node = "(/article | /simple-article | /exam | /book-review)";
  
  // relative to the top_node, these two nodes are siblings and contain all the info we need
  private static final String top_head = "(head | simple-head | book-review-head)";
  private static final String top_info = "item-info";


  static public final String common_doi = top_info + "/doi";
  static public final String common_copyright = top_info + "/copyright/@year";
  
  static public final String common_title = top_head + "/title";
  static public final String common_author_group = top_head + "/author-group";
  static public final String common_dochead = top_head + "/dochead/textfn";
  
  
  
  /* "ce:blah" - the 'ce' portion would not be needed for xpath as it just 
   * defines a namespace. But since these are used for direct comparison within
   * a node evaluator, we need the "ce:" portion 
   * */
  static public final String authorNodeName = "ce:author";
  static public final String subtitleNodeName = "ce:subtitle";
  static public final String surnameNodeName = "ce:surname";
  static public final String givennameNodeName = "ce:given-name";
  static public final String commonText = "ce:text";
  static public final String authorCollaborator = "ce:collaboration";

  /* 
   * TITLE INFORMATION
   * <ce:title>
   * see if the <ce:subtitle> sibling exists and has information
   */
  static private final NodeValue TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of article title");
      String tTitle = node.getTextContent();
      log.debug3(tTitle);
      String tSubtitle = null;

      //is there a subtitle sibling?
      Node titleNextSibling = node.getNextSibling();
      // between here and the <subtitle> tag if it exists
      if (titleNextSibling != null) log.debug3("next sibling is :" + titleNextSibling.getNodeName());

      while ((titleNextSibling != null) && (!subtitleNodeName.equals(titleNextSibling.getNodeName()))) {
        titleNextSibling = titleNextSibling.getNextSibling();
      }
      // we're either at subtitle or there wasn't one to check
      if (titleNextSibling != null) {
        tSubtitle = titleNextSibling.getTextContent();
      }

      // now build up the full title
      StringBuilder valbuilder = new StringBuilder();
      if (tTitle != null) {
        valbuilder.append(tTitle);
        if (tSubtitle != null) {
          if (tTitle.endsWith(":")) { // sometimes the title ends with the :
            valbuilder.append(" " + tSubtitle);
          } else {
            valbuilder.append(": " + tSubtitle);
          }
        }
      } else { 
        log.debug3("no title found within title group");
        return null;
      }
      log.debug3("title found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  /*
   * AUTHOR GROUP
   * We have to process the entire group at once since the articleMDMap is a hashmap and you only 
   * get the option to put in a value once. Subsequent puts would overwrite the previous
   * author, if you did them one at a time.
   * NODE=<ce:author-group/
   *   <ce:author>
   *     ce:given-name
   *     ce:surname
   *   </ce:author>
   */
  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of author-group");
      StringBuilder valbuilder = new StringBuilder();
      NodeList groupChildNodes = node.getChildNodes();
      for (int n = 0; n< groupChildNodes.getLength(); n++) {
        Node nextNode = groupChildNodes.item(n);
        String surName = null;
        String givenName = null;
        if (authorNodeName.equals(nextNode.getNodeName())) {
          // an author node
          NodeList childNodes = nextNode.getChildNodes();
          for (int m = 0; m < childNodes.getLength(); m++) {
            Node infoNode = childNodes.item(m);
            String nodeName = infoNode.getNodeName();
            if (surnameNodeName.equals(nodeName)) {
              surName = infoNode.getTextContent();
            } else if (givennameNodeName.equals(nodeName)) {
              givenName = infoNode.getTextContent();
            }
          }
        } else if (authorCollaborator.equals(nextNode.getNodeName())) {
          // instead of an author, you might have a collaboration name
          NodeList childNodes = nextNode.getChildNodes();
          for (int m = 0; m < childNodes.getLength(); m++) {
            Node infoNode = childNodes.item(m);
            String nodeName = infoNode.getNodeName();
            if (commonText.equals(nodeName)) {
              surName = infoNode.getTextContent();
            }
          }
        }
        // We may choose to limit the type of roles, but not sure which yet
        if  (surName != null) {
          valbuilder.append(surName);
          if (givenName != null) {
            valbuilder.append(ElsevierMainDTD5XmlSchemaHelper.AUTHOR_SEPARATOR +  " " + givenName);
          }
          valbuilder.append(ElsevierMainDTD5XmlSchemaHelper.AUTHOR_SPLIT_CHAR);
        }
      }
      int vlen;
      if ( (vlen = valbuilder.length()) > 0) {
        valbuilder.deleteCharAt(vlen-1); // remove final splitter ";"
        log.debug3("author found: " + valbuilder.toString());
        return valbuilder.toString();
      }
      log.debug3("No valid contributor in this contributor node.");
      return null;
    }
  };

  static public final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  {
    articleMap.put(ElsevierMainDTD5XmlSchemaHelper.common_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(ElsevierMainDTD5XmlSchemaHelper.common_title, TITLE_VALUE);
    articleMap.put(ElsevierMainDTD5XmlSchemaHelper.common_author_group, AUTHOR_VALUE);
    articleMap.put(ElsevierMainDTD5XmlSchemaHelper.common_dochead, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(ElsevierMainDTD5XmlSchemaHelper.common_copyright, XmlDomMetadataExtractor.TEXT_VALUE);
  }
  
  
  /* 2. Each item for this initial metadata starts at "journal-item" */
  static private final String articleNode = top_node;

  /* 3. We do not need to use global information */
  static private final Map<String,XPathValue> globalMap = null;

  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(common_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(common_author_group, 
        new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CHAR)));
  }

  /**
   * 
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return globalMap;
  }

  /**
   * return article map to identify xpaths of interest
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
   * Use the raw metadata for the article level main.xml
   */
  @Override
  public String getFilenameXPathKey() {
    return null;
  }

}
