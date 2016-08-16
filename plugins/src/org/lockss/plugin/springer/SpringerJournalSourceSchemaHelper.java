/*


 * $Id$
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

package org.lockss.plugin.springer;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  the Springer Proprietary tagset for journals
 *  with the filenames based on same name as the .xml file
 *  There is only one record for each file
 *  This is pretty much directly encapsulated from the SpringerSourceMetadataExtractor verbatim
 *  to allow for alternate BookSchema...
 *  @author alexohlson
 */
public class SpringerJournalSourceSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(SpringerJournalSourceSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";

  /** NodeValue for creating value of subfields from AuthorName tag */
  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      
      NodeList nameNodes = node.getChildNodes();
      String givenName = "", familyName = "";
      for (int k = 0; k < nameNodes.getLength(); k++) {
        Node nameNode = nameNodes.item(k);
        if (nameNode.getNodeName().equals("GivenName")) {
          givenName += nameNode.getTextContent();
        } else if (nameNode.getNodeName().equals("FamilyName")) {
          familyName += nameNode.getTextContent();
        }
      }
      return familyName + ", " + givenName;
    }
  };
          
  /** NodeValue for creating value of subfields from OnlineDate tag **/
  static private final NodeValue DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      
      NodeList nameNodes = node.getChildNodes();
      String year = null, month = null, day = null;
      for (int k = 0; k < nameNodes.getLength(); k++) {
        Node nameNode = nameNodes.item(k);
        if (nameNode.getNodeName().equals("Year")) {
          year = nameNode.getTextContent();
        } else if (nameNode.getNodeName().equals("Month")) {
          month = nameNode.getTextContent();
        } else if (nameNode.getNodeName().equals("Day")) {
          day = nameNode.getTextContent();
        }
      }
      
      return year + "-" + month + (day != null ? "-" + day : "");
    }
  };
  
  private static String journalNode = "/Publisher/Journal";
  
  // the journalNode will be our article node to start since there is only 
  // one article per file
  // the rest of these are relative to Journal
  
private static String JournalID = "JournalInfo/JournalID";
private static String JournalPrintISSN = "JournalInfo/JournalPrintISSN";
private static String JournalElectronicISSN = "JournalInfo/JournalElectronicISSN";
private static String JournalTitle = "JournalInfo/JournalTitle";
private static String VolumeIDStart = "Volume/VolumeInfo/VolumeIDStart";
private static String IssueIDStart = "Volume/Issue/IssueInfo/IssueIDStart";
private static String CoverDate = "Volume/Issue/IssueInfo/IssueHistory/CoverDate";
private static String CopyrightYear = "Volume/Issue/IssueInfo/IssueCopyright/CopyrightYear";
private static String ArticleDOI = "Volume/Issue/Article/ArticleInfo/ArticleDOI";
private static String Language = "Volume/Issue/Article/ArticleInfo/ArticleTitle/@Language";
private static String ArticleTitle = "Volume/Issue/Article/ArticleInfo/ArticleTitle";
private static String ArticleFirstPage = "Volume/Issue/Article/ArticleInfo/ArticleFirstPage";
private static String ArticleLastPage = "Volume/Issue/Article/ArticleInfo/ArticleLastPage";
private static String AuthorName = "Volume/Issue/Article/ArticleHeader/AuthorGroup/Author/AuthorName";
private static String Para = "Volume/Issue/Article/ArticleHeader/Abstract/Para";
private static String Keyword = "Volume/Issue/Article/ArticleHeader/KeywordGroup/Keyword";
  

  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */
 
  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> nodeMap = 
      new HashMap<String,XPathValue>();
  static {
    // normal journal article schema
    //nodeMap.put("/Publisher/PublisherInfo/PublisherName", XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(JournalID, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(JournalPrintISSN, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(JournalElectronicISSN, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(JournalTitle, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(VolumeIDStart, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(IssueIDStart, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(CoverDate, DATE_VALUE);
    nodeMap.put(CopyrightYear, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(ArticleDOI, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(Language, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(ArticleTitle, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(ArticleFirstPage, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(ArticleLastPage, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(AuthorName, AUTHOR_VALUE);
    nodeMap.put(Para, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(Keyword, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /** Map of raw xpath key to cooked MetadataField */
  static private final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    //xpathMap.put("/Publisher/PublisherInfo/PublisherName", MetadataField.FIELD_PUBLISHER);
    cookMap.put(JournalID, MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
    cookMap.put(JournalPrintISSN, MetadataField.FIELD_ISSN);
    cookMap.put(JournalElectronicISSN, MetadataField.FIELD_EISSN);
    cookMap.put(JournalTitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(VolumeIDStart, MetadataField.FIELD_VOLUME);
    cookMap.put(IssueIDStart, MetadataField.FIELD_ISSUE);
    cookMap.put(CoverDate, MetadataField.FIELD_DATE);
    cookMap.put(CopyrightYear, MetadataField.DC_FIELD_RIGHTS);
    cookMap.put(ArticleDOI, MetadataField.FIELD_DOI);
    cookMap.put(Language, MetadataField.DC_FIELD_LANGUAGE);
    cookMap.put(ArticleTitle, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(ArticleFirstPage, MetadataField.FIELD_START_PAGE);
    cookMap.put(ArticleLastPage, MetadataField.FIELD_END_PAGE);
    cookMap.put(AuthorName, MetadataField.FIELD_AUTHOR);
    cookMap.put(Para, MetadataField.DC_FIELD_DESCRIPTION);
    cookMap.put(Keyword, MetadataField.FIELD_KEYWORDS);
  }


  /* 2. Each item (article) has its own XML file */
  static private final String journalArticleNode = journalNode; 

  /* 3. in JATS there is no global information because one file/article */
  static private final Map<String,XPathValue> globalMap = null;


  /**
   * JATS does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return globalMap;
  }

  /**
   * return JATS article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return nodeMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return journalArticleNode;
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
