/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  the CrossRef delivery schema 
 *  
 */
public class CrossRefSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(CrossRefSchemaHelper.class);


  /* 
   *  CrossRef DOI deliver schema - used by several publishers
   */
  
  //<publication_date media_type="online">
  //  <month>01</month>
  //  <day>10</day>
  //  <year>2001</year>
  //</publication_date>
  static private final NodeValue FULL_DATE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of CrossRef publication date");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tyear = null;
      String tday = null;
      String tmonth = null;
      // look at each child of the TitleElement for information                                                                                                                                     
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("day".equals(nodeName)) {
          tday = checkNode.getTextContent();
        } else if ("month".equals(nodeName) ) {
          tmonth = checkNode.getTextContent();
        } else if ("year".equals(nodeName)) {
          tyear = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tyear != null) {
        valbuilder.append(tyear);
        if (tday != null && tmonth != null) {
          valbuilder.append("-" + tmonth + "-" + tday);
        }
      } else {
        log.debug3("no date found");
        return null;
      }
      log.debug3("date found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };
  
  
  //  <person_name sequence="first" contributor_role="author">
  //    <given_name>S</given_name>
  //    <surname>Hengsberger</surname>
  //  </person_name>
  static private final NodeValue AUTHOR_NAME = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of CrossRef person name");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tgiven = null;
      String tsurname = null;
      // look at each child of the TitleElement for information                                                                                                                                     
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("given_name".equals(nodeName)) {
          tgiven = checkNode.getTextContent();
        } else if ("surname".equals(nodeName) ) {
          tsurname = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tsurname != null) {
        valbuilder.append(tsurname);
        if (tgiven != null) {
          valbuilder.append(", " + tgiven);
        }
      } else {
        log.debug3("no name found");
        return null;
      }
      log.debug3("name found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };
  
  // this is global for all articles in the file
  private static final String publisher = "/doi_batch/head/registrant";
  
  

  // The following are all relative to the article node
  // from the immediately preceding sibling -
  private static String pub_title = "preceding-sibling::journal_metadata[1]/full_title";
  public static String pub_abbrev = "preceding-sibling::journal_metadata[1]/abbrev_title";
  private static String pub_issn = "preceding-sibling::journal_metadata[1]/issn";
  public static String pub_year = "preceding-sibling::journal_issue[1]/publication_date/year";
  private static String pub_volume = "preceding-sibling::journal_issue[1]/journal_volume/volume";
  public static String pub_issue = "preceding-sibling::journal_issue[1]/issue";
  private static String pub_issue_month = "preceding-sibling::journal_issue[1]/publication_date/month";
  
  private static String art_title = "titles/title";
    private static String art_contrib = "contributors/person_name";
     //given_name, surname
  private static String art_date = "publication_date";
  public static String art_sp = "pages/first_page";
  private static String art_lp = "pages/last_page";
  private static String art_doi = "doi_data/doi";
  public static String art_resource = "doi_data/resource";


  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
	articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE); 
	articleMap.put(pub_abbrev, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issn, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issue_month, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE); 
  //  articleMap.put(art_subtitle, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_contrib, AUTHOR_NAME); 
    articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_resource, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_date, FULL_DATE); 
  }

  /* 2.  Top level per-article node */
  static private final String articleNode = "/doi_batch/body/journal/journal_article";

  /* 3. Global metadata is the publisher - work around if it gets troublesome */
  static private final Map<String, XPathValue> 
    globalMap = new HashMap<String,XPathValue>();
  static {
    globalMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE); 
  }
  /*
   * The emitter will need a map to know how to cook raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_issn, MetadataField.FIELD_ISSN);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(art_doi, MetadataField.FIELD_DOI);
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
    cookMap.put(art_lp, MetadataField.FIELD_END_PAGE);
    cookMap.put(art_contrib, MetadataField.FIELD_AUTHOR);
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
    return art_doi;
  }

}