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

import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 *  Elsevier DTD5 Metadata Extractor
 *  This is a little more complicated than other Clockss Source XML based plugins
 *  1. The deliveries are broken in to chunks. 
 *       CLKS000003A.tar, CLKS000003B.tar... combine to make directory CLKS000003/
 *       but we do not unpack the individual tars so we must figure out which tar
 *       contents live in
 *  2.  We are iterating over only the A tarball, which contains a "dataset.xml" file describing
 *       all the contents for related tarballs
 *  3. We pick up almost all the metadata we need from top dataset.xml but we need to 
 *       open an article level "main.xml" file to get the article title and author information.
 *       The dataset.xml will tell us the relative link to the needed main.xml but NOT
 *       which tarball it lives in.
 *       
 *   The approach will be thus
 *       - use the dataset.xml to get the easy-to-get metadata for the delivery
 *       - make a table of relative_path/main.xml with which absolute tarball location
 *       - post-process the ArticleMetadata record list and open each of the 
 *            needed main.xml files one at a time to get the remaining information
 *            and add it to the AM before cooking and emitting.             
 *  @author alexohlson
 */
public class ElsevierDTD5XmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(ElsevierDTD5XmlSchemaHelper.class);

  static final String AUTHOR_SEPARATOR = ",";
  static final String AUTHOR_SPLIT_CHAR = ";";

  /*
   * XPATH DEFINITIONS WE CARE ABOUT
   */

  private static final String dataset_content = "/dataset/dataset-content";
  // Each article starts at its only unique "dataset_article" node

  // These work for ARTICLES, BOOK-REVIEWS and SIMPLE-ARTICLES
  private static final String dataset_article = dataset_content + "/journal-item";
  // these are relative to the dataset_article   
  private static final String dataset_article_doi = "journal-item-unique-ids/doi";
  private static final String dataset_article_issn = "journal-item-unique-ids/jid-aid/issn";
  private static final String dataset_article_jid = "journal-item-unique-ids/jid-aid/jid";
  private static final String dataset_article_date = "journal-item-properties/online-publication-date";
  public static final String dataset_article_metadata = "files-info/ml/pathname";
  public static final String dataset_dtd_metadata = "files-info/ml/dtd-version";
  private static final String dataset_article_pdf = "files-info/web-pdf/pathname";
  // get the journal title from the closest preceeding journal-info node
  private static final String dataset_journal_title = "preceding-sibling::journal-issue[1]";
  /* These are used for article level metadata */
  static public final String common_title = "title";
  static public final String common_author_group = "author-group";
  static public final String common_dochead = "dochead/textfn";
  static public final String common_copyright = "copyright[@year]";

  /* 
   * Date values look like this: 2014-09-22T00:54:27Z
   * and we need them to look like this: 2014-09-22 to be acceptable to our
   * metadata database
   */
  static private final NodeValue DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of date");
      String fulldate = node.getTextContent();
      log.debug3(fulldate);
      return StringUtils.substringBefore(fulldate, "T");
    }
  };

  /* 
   * <journal-issue>
   *   <journal-issue-properties>
   *     <collection-title>
   *  the reason we can't just have used the full path to the node we want
   *  as our xpath expression and just picked up its text content is because
   *  we had to find the closest previous "journal-issue" preceeding sibling
   *  and that meant we had to search on a <journal-issue> and not the lower
   *  down child node we wanted.  
   */
  static private final NodeValue JOURNAL_ISSUE_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("get value of previous journal-issue sibling");
      NodeList groupChildNodes = node.getChildNodes();
      for (int n = 0; n< groupChildNodes.getLength(); n++) {
        Node cNode = groupChildNodes.item(n);
        if ("journal-issue-properties".equals(cNode.getNodeName())) {
          NodeList propChildNodes = cNode.getChildNodes();
          for (int m = 0; m< propChildNodes.getLength(); m++) {
            Node pNode = propChildNodes.item(m);
            if ("collection-title".equals(pNode.getNodeName())) {
              return pNode.getTextContent();
            }
          }
        }
      }
      log.debug3("No publication title found");
      return null;
    }
  };

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  static {
    articleMap.put(dataset_article_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_article_issn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_article_jid, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_article_date, DATE_VALUE);
    articleMap.put(dataset_article_metadata, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_dtd_metadata, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_article_pdf, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(dataset_journal_title, JOURNAL_ISSUE_TITLE_VALUE);
  }

  /* 2. Each item for this initial metadata starts at "journal-item" */
  static private final String articleNode = dataset_article; 

  /* 3. We do not need to use global information */
  static private final Map<String,XPathValue> globalMap = null;

  /*
   * The emitter will need a map to know how to cook  raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(dataset_article_doi, MetadataField.FIELD_DOI);
    cookMap.put(dataset_article_issn, MetadataField.FIELD_ISSN);
    cookMap.put(dataset_journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(dataset_article_date, MetadataField.FIELD_DATE);
    // the author information and article title are set in MetadataExtractor
    // but cooked with the rest
    cookMap.put(common_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(common_author_group, 
        new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CHAR)));
  }

  /*
   *  article-level main.xml  - definitions used in the MetadataExtraactor to pull
   *  the remaining metadata from the article metadata file 
   */

  /* "ce:blah" - the 'ce' portion would not be needed for xpath as it just 
   * defines a namespace. But since these are used for direct comparison with 
   * node-name. Need the "ce:" portion 
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
            valbuilder.append(ElsevierDTD5XmlSchemaHelper.AUTHOR_SEPARATOR +  " " + givenName);
          }
          valbuilder.append(ElsevierDTD5XmlSchemaHelper.AUTHOR_SPLIT_CHAR);
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

  static public final Map<String,XPathValue> articleLevelMDMap = 
      new HashMap<String,XPathValue>();
  {
    articleLevelMDMap.put(ElsevierDTD5XmlSchemaHelper.common_title, TITLE_VALUE);
    articleLevelMDMap.put(ElsevierDTD5XmlSchemaHelper.common_author_group, AUTHOR_VALUE);
    articleLevelMDMap.put(ElsevierDTD5XmlSchemaHelper.common_dochead, XmlDomMetadataExtractor.TEXT_VALUE);
    articleLevelMDMap.put(ElsevierDTD5XmlSchemaHelper.common_copyright, XmlDomMetadataExtractor.TEXT_VALUE);
  }
  /*
   * END OF DEFINITION OF SCHEMA FOR LOW-LEVEL ARTICLE XML
   */


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
    return dataset_article_metadata;
  }

}
