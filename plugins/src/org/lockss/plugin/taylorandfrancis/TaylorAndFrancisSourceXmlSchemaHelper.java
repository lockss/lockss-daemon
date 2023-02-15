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

package org.lockss.plugin.taylorandfrancis;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Taylor And Francis bulk source content (UACP V16 only) which uses a tfdoc
 *  schema.
 *  Most of the issues provide PDF's of each article along with XML files containing metadata
 *  3 of the UACP issues (16-18) also provide html full text versions of the article
 *  in addition to the PDF - though this doesn't effect metadata extraction
 *  @author alexohlson
 */

public class TaylorAndFrancisSourceXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(TaylorAndFrancisSourceXmlSchemaHelper.class);
  
  private static final String AUTHOR_SEPARATOR = ",";

  /*
   * AUTHOR information
   * NODE=<author>     
   * not all of these will necessarily be there...  
   *   givenname/
   *   inits/
   *   surname/
   *   suffix/
   *   degree/
   */
  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of TandF author");
      String given = null;
      String inits = null;
      String surname = null;
      String suffix = null;
      String degree = null;
      NodeList childNodes = node.getChildNodes(); 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        String nodeName = infoNode.getNodeName();
        if ("givenname".equals(nodeName)) {
          given = infoNode.getTextContent();
        } else if ("inits".equals(nodeName)) {
          inits = infoNode.getTextContent();
        } else if ("surname".equals(nodeName)) {
          surname = infoNode.getTextContent();
        } else if ("suffix".equals(nodeName)) {
          suffix = infoNode.getTextContent();
        } else if ("degree".equals(nodeName)) {
          degree = infoNode.getTextContent();
        }
      }
      StringBuilder valbuilder = new StringBuilder();
      if (given != null) {
        valbuilder.append(given);
      }
      if (inits != null) {
        valbuilder.append(" " + inits);
      }
      if (surname != null) {
        valbuilder.append(" " + surname);
      }
      if (suffix != null) {
        valbuilder.append(AUTHOR_SEPARATOR + " " + suffix);
      }
      if (degree != null) {
        valbuilder.append(AUTHOR_SEPARATOR + " " + degree);
      }
      return valbuilder.toString();
    }
  };

  /* 
   *  Taylor & Francis XPATH key definitions that we care about
   */
  private static String ARTICLE = "(article | unarticle)";
  
  //attributes of the top level (article) node
  private static String article_id =  ARTICLE + "/@articleid";
  private static String article_doi =  ARTICLE + "/@doi";
  private static String article_date =  ARTICLE + "/@yearofpub";

  // within the meta tag group
  protected static String meta_volume = ARTICLE + "/meta/@volumenum";
  protected static String meta_issue = ARTICLE + "/meta/@issuenum";
  protected static String meta_spage = ARTICLE + "/meta/@firstpage";
  protected static String meta_epage = ARTICLE + "/meta/@lastpage";
  protected static String meta_filename = ARTICLE + "/meta/@pdffilename";

  protected static String article_JID = ARTICLE + "/meta/journalcode"; 
  protected static String article_authors = ARTICLE + "/meta/author/name";
  protected static String article_issn =
      ARTICLE + "/meta/issn[type = \"print\"]";
  protected static String article_eissn =
      ARTICLE + "/meta/issn[type = \"electronic\"]";

  // their own nodes under <article>
  protected static String article_jtitle = ARTICLE + "/journaltitle";
  protected static String article_title = ARTICLE + "/title";


  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  static {
    articleMap.put(article_id, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(article_doi, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(article_date, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(meta_volume, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(meta_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(meta_spage, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(meta_epage, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(meta_filename, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_JID, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_authors, AUTHOR_VALUE);
    articleMap.put(article_issn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_eissn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_jtitle, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE);

  }

  /* 2. each xml file represents just one article at the top of the file */
  static private final String articleNode = null;

  /* 3. since each XML file represents just one article, no need for global */
  static private final Map<String,XPathValue> globalMap = null;

  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(article_id, MetadataField.FIELD_PROPRIETARY_IDENTIFIER); 
    cookMap.put(article_doi, MetadataField.FIELD_DOI); 
    cookMap.put(article_date, MetadataField.FIELD_DATE); 
    cookMap.put(meta_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(meta_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(meta_spage, MetadataField.FIELD_START_PAGE);
    cookMap.put(meta_epage, MetadataField.FIELD_END_PAGE);
    cookMap.put(article_authors, MetadataField.FIELD_AUTHOR);
    cookMap.put(article_issn, MetadataField.FIELD_ISSN);
    cookMap.put(article_eissn, MetadataField.FIELD_EISSN);
    cookMap.put(article_jtitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(article_title, MetadataField.FIELD_ARTICLE_TITLE);
  }


  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return globalMap; // null
  }

  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return articleMap;
  }

  @Override
  public String getArticleNode() {
    return articleNode; // null - sits at top level
  }

  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  @Override
  public String getDeDuplicationXPathKey() {
    return null; //unnecessary
  }

  @Override
  public String getConsolidationXPathKey() {
    return null; // unnecessary
  }

  @Override
  public String getFilenameXPathKey() {
    return meta_filename;
  }
}