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

package org.lockss.plugin.clockss.duke;

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
 *  euclid content journal_issue XML
 *  
 */
public class EuclidSchemaHelper
implements SourceXmlSchemaHelper {
  
  private static final Logger log = Logger.getLogger(EuclidSchemaHelper.class);


  /*
   * author/name/
   *   given_name
   *   surname
   */
  private final static NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      NodeList nameChildren = node.getChildNodes();
      if (nameChildren == null) return null;

      String surname = null;
      String forename = null;

      if (nameChildren == null) return null;
      for (int p = 0; p < nameChildren.getLength(); p++) {
        Node partNode = nameChildren.item(p);
        String partName = partNode.getNodeName();
        if ("surname".equals(partName)) {
          surname  = partNode.getTextContent();
        } else if ("given_name".equals(partName)) {
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
      return null;
    }
  };
  
  // they deliver newlines in their titles - clear them out
  private final static NodeValue TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String tTitle = node.getTextContent();
      tTitle = tTitle.replace("\n", " ");
      return tTitle.trim().replaceAll(" +", " "); //clear out duplicate white space
    }
  };

  //top of file
  private static final String top = "/euclid_content/";
  
  //global - publication and issue level information
  private static String pub_title = top + "euclid_publication/publication_title";
  private static String pub_year = top + "content/euclid_issue/issue/issue_data/issue_publ_date";
  private static String pub_volume = top + "content/euclid_issue/issue/issue_data/journal_vol_number";
  private static String pub_issue = top + "content/euclid_issue/issue/issue_data/issue_number";
  private static String pub_p_issn = top + "euclid_publication/publication_id[@type = 'p-issn']";
  private static String pub_e_issn = top + "euclid_publication/publication_id[@type = 'e-issn']";
  
  

  // The following are all relative to the article node ("record")
  // from the immediately preceeding sibling -
  private static String art_title = "title[@type = 'main']";
  private static String art_contrib = "author/name";
  private static String art_doi = "identifiers/identifier[@type = 'doi']";
  private static String art_sp = "start_page";
  private static String art_ep = "end_page";
  private static String art_record = "record_filename";
  

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    articleMap.put(art_title, TITLE_VALUE); 
    articleMap.put(art_contrib, AUTHOR_VALUE); 
    articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(art_ep, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(art_record, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2.  Top level per-article node - optional div level*/
  static private final String articleNode = top + "content/euclid_issue/issue/record | " + top + "content/euclid_issue/issue/div/record";

  /* 3. Global metadata is the publisher - work around if it gets troublesome */
  static private final Map<String, XPathValue> 
    globalMap = new HashMap<String,XPathValue>();
  static {
    globalMap.put(pub_title, TITLE_VALUE);
    globalMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE);
    globalMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE);
    globalMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    globalMap.put(pub_p_issn, XmlDomMetadataExtractor.TEXT_VALUE);
    globalMap.put(pub_e_issn, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  
  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final String AUTHOR_SPLIT_CH = ",";
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(pub_p_issn, MetadataField.FIELD_ISSN);
    cookMap.put(pub_e_issn, MetadataField.FIELD_EISSN);
    cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(art_contrib, 
        new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CH)));
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
    cookMap.put(art_ep, MetadataField.FIELD_END_PAGE);
    cookMap.put(art_doi, MetadataField.FIELD_DOI);
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
    return art_record;
  }

}