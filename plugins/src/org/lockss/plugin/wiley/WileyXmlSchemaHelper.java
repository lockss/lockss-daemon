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

package org.lockss.plugin.wiley;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.TextValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Wiley metadata
 *  There is one record per pdf file. The pdf filename is identified in the
 *  XML as TODO
 *  @author alexohlson
 */

public class WileyXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(WileyXmlSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";


  /* 
   *  WML specific XPATH key definitions that we care about
   */
  // xpath contants - relative to "/component/"
  static private final String XPATH_PUBLISHER =
      "header/publicationMeta/publisherInfo/publisherName";
  static private final String XPATH_ARTICLE_TITLE =
      "header/contentMeta/titleGroup/title";
  static private final String XPATH_JOURNAL_TITLE = 
      "header/publicationMeta[@level='product']" +
      "/titleGroup/title";
  static private final String XPATH_ISSN = 
      "header/publicationMeta[@level='product']" +
      "/issn[@type='print']";
  static private final String XPATH_EISSN = 
      "header/publicationMeta[@level='product']" +
      "/issn[@type='electronic']";
  static private final String XPATH_PROPRIETARY_IDENTIFIER = 
        "header/publicationMeta[@level='product']" +
        "/idGroup/id[@type='product']/@value";
  static private final String XPATH_VOLUME = 
      "header/publicationMeta[@level='part']" +
      "/numberingGroup/numbering[@type='journalVolume']"; 
  static private final String XPATH_ISSUE = 
      "header/publicationMeta[@level='part']" +
      "/numberingGroup/numbering[@type='journalIssue']"; 
  static private final String XPATH_DATE = 
      "header/publicationMeta[@level='part']" +
      "/coverDate/@startDate";
  static private final String XPATH_START_PAGE = 
      "header/publicationMeta[@level='unit']" +
      "/numberingGroup/numbering[@type='pageFirst']"; 
  static private final String XPATH_END_PAGE = 
      "header/publicationMeta[@level='unit']" +
      "/numberingGroup/numbering[@type='pageLast']"; 
  static private final String XPATH_DOI = 
      "header/publicationMeta[@level='unit']/doi";
  static private final String XPATH_PDF_FILE_NAME =
      "header/publicationMeta/linkGroup/link[@type='toTypesetVersion']/@href";
  static private final String XPATH_KEYWORDS = 
      "header/contentMeta/keywordGroup/keyword"; 
  static private final String XPATH_AUTHOR = 
      "header/contentMeta/creators" +
      "/creator[@creatorRole='author']/personName"; 
  // for future use: contains values like "Cover Picture", 
  // "Research Article", and "Editorial"
  static private final String XPATH_ARTICLE_CATEGORY =
      "header/publicationMeta/titleGroup/title[@type=articleCategory]";                  
  
  // NodeValue for creating value of subfields from author tag
  static private final XPathValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      // Wiley stores author names in two tags: givenNames and familyName
      NodeList nameNodes = node.getChildNodes();
      String givenName = null, familyName = null;
      for (int k = 0; k < nameNodes.getLength(); k++) {
        Node nameNode = nameNodes.item(k);
        if (nameNode.getNodeName().equals("givenNames")) {
          givenName = nameNode.getTextContent();
        } else if (nameNode.getNodeName().equals("familyName")) {
          familyName = nameNode.getTextContent();
        }
      }
      return familyName + AUTHOR_SEPARATOR + " " + givenName;
    }
  };
  
  private static final Pattern LEADING_REGEX =  Pattern.compile("^(file:(//)?)?(.*\\.pdf).*", Pattern.CASE_INSENSITIVE);
    
  // The filename often has leading "file:" or "file://" 
  static private final XPathValue PDF_NAME_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String eVal = node.getTextContent();
      Matcher pMat = LEADING_REGEX.matcher(eVal);
      if (pMat.matches()) {
        return pMat.group(3);
      }
      return eVal;
    }
  };

  // Matches issue number ranges of integers separated by a unicode dash
  // There are variety of dashes. To match a dash, we just match a non-digit
  static final Pattern issuePat = 
                                Pattern.compile("(^[0-9]+)[^0-9]+([0-9]+)$");
  static private final XPathValue ISSUE_VALUE = new TextValue() {
    @Override
    public String getValue(String s) {
      if (StringUtil.isNullString(s)) {
        return null;
      }
      return (issuePat.matcher(s).replaceFirst("$1-$2"));
    }
  };  
  
  // Publisher is hardcoded for all AM records
  static private final XPathValue PUBLISHER_VALUE = new TextValue() {
  @Override
  public String getValue(String s) {
    return "John Wiley & Sons, Inc.";
  }
};   
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> WML_articleMap = 
      new HashMap<String,XPathValue>();
  static {
      // Journal article schema
    WML_articleMap.put(XPATH_ARTICLE_TITLE, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_JOURNAL_TITLE, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_ISSN, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_EISSN, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_PROPRIETARY_IDENTIFIER, 
                  XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_VOLUME, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_ISSUE, ISSUE_VALUE);
    WML_articleMap.put(XPATH_DATE, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_START_PAGE, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_END_PAGE, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_DOI, XmlDomMetadataExtractor.TEXT_VALUE);
    // wiley provides super long keyword phrases that truncate and cause
    // warnings from the database. Since we don't need them just don't pick up
    //WML_articleMap.put(XPATH_KEYWORDS, XmlDomMetadataExtractor.TEXT_VALUE);
    WML_articleMap.put(XPATH_AUTHOR, AUTHOR_VALUE);
      // name of PDF file relative to path of XML file
    WML_articleMap.put(XPATH_PDF_FILE_NAME, PDF_NAME_VALUE);
    WML_articleMap.put(XPATH_PUBLISHER, PUBLISHER_VALUE); //hardcoded
    }

  /* 2. Each item (book) has its own subNode */
  static private final String WML_articleNode = "//component"; 

  /* 3. in ONIX, there is no global information we care about, it is repeated per article */ 
  static private final Map<String,XPathValue> WML_globalMap = null; 

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
      // Journal article schema
      cookMap.put(XPATH_ARTICLE_TITLE, MetadataField.FIELD_ARTICLE_TITLE);
      cookMap.put(XPATH_JOURNAL_TITLE, MetadataField.FIELD_JOURNAL_TITLE);
      cookMap.put(XPATH_ISSN, MetadataField.FIELD_ISSN);
      cookMap.put(XPATH_EISSN, MetadataField.FIELD_EISSN);
      cookMap.put(XPATH_PROPRIETARY_IDENTIFIER, 
                   MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
      cookMap.put(XPATH_VOLUME, MetadataField.FIELD_VOLUME);
      cookMap.put(XPATH_ISSUE, MetadataField.FIELD_ISSUE);
      cookMap.put(XPATH_DATE, MetadataField.FIELD_DATE);
      cookMap.put(XPATH_START_PAGE, MetadataField.FIELD_START_PAGE);
      cookMap.put(XPATH_END_PAGE, MetadataField.FIELD_END_PAGE);
      cookMap.put(XPATH_DOI, MetadataField.FIELD_DOI);
      // wiley provides super long keyword phrases that truncate and cause
      // warnings from the database. Since we don't need them just don't pick up
      //cookMap.put(XPATH_KEYWORDS, MetadataField.FIELD_KEYWORDS);
      cookMap.put(XPATH_AUTHOR, MetadataField.FIELD_AUTHOR);
      cookMap.put(XPATH_PUBLISHER, MetadataField.FIELD_PUBLISHER);
    }

  /**
   * WML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return WML_globalMap;
  }

  /**
   * return WML article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return WML_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return WML_articleNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /**
   * only one record per XML file; no de-duplication needed
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * Only one record per XML file; no deduplication needed
   */
  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * The filenames are based on 
   */
  @Override
  public String getFilenameXPathKey() {
    return XPATH_PDF_FILE_NAME;
  }

}    