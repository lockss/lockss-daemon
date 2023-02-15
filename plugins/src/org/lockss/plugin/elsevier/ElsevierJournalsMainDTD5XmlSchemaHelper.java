/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

/**
 *  Elsevier DTD5 Metadata Extractor JOURNAL MAIN SCHEMA
 *  This is one of four related schema helpers to handle the extraction for Elsevier file-transfer content.
 *  This is the schema definition the item-level main.xml file for journals and book-series
 *  @author alexohlson
 */
public class ElsevierJournalsMainDTD5XmlSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(ElsevierJournalsMainDTD5XmlSchemaHelper.class);

  static final String AUTHOR_SEPARATOR = ",";
  static final String AUTHOR_SPLIT_CHAR = ";";

  /*
   * XPATH DEFINITIONS WE CARE ABOUT
   */


  /*
   *  article-level main.xml  - definitions used in the MetadataExtraactor to pull
   *  the remaining metadata from the article metadata file 
   */

  private static final String top_node = "(/article | /simple-article | /converted-article | /exam | /book-review)";
  
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
            valbuilder.append(ElsevierJournalsMainDTD5XmlSchemaHelper.AUTHOR_SEPARATOR +  " " + givenName);
          }
          valbuilder.append(ElsevierJournalsMainDTD5XmlSchemaHelper.AUTHOR_SPLIT_CHAR);
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
    articleMap.put(ElsevierJournalsMainDTD5XmlSchemaHelper.common_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(ElsevierJournalsMainDTD5XmlSchemaHelper.common_title, TITLE_VALUE);
    articleMap.put(ElsevierJournalsMainDTD5XmlSchemaHelper.common_author_group, AUTHOR_VALUE);
    articleMap.put(ElsevierJournalsMainDTD5XmlSchemaHelper.common_dochead, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(ElsevierJournalsMainDTD5XmlSchemaHelper.common_copyright, XmlDomMetadataExtractor.TEXT_VALUE);
  }
  
  
  /* 2. Each item for this initial metadata starts at "journal-item" */
  static private final String articleNode = top_node;

  /* 3. We do not need to use global information */
  static private final Map<String,XPathValue> globalMap = null;

  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(common_title, MetadataField.FIELD_ARTICLE_TITLE);
    // Elsevier has indicated that this should take priority of online date
	cookMap.put(common_copyright, MetadataField.FIELD_DATE);
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
