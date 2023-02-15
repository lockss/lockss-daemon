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

package org.lockss.plugin.springer;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

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
  
  /** 
   * A version of XmlDomMetadataExtractor.TEXT_VALUE
   * that strips out newlines and condenses extra spaces down to one
   * Useful for titles, descriptive text, keywords, etc
   * Springer has <emphasis> <superscript> and other formatting nodes
   * and therefore might write a title across many lines
   * While we strip out the tags, we should also remove any newlines. 
   */
  static private final NodeValue CLEAN_TEXT_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      
      String longVal = node.getTextContent();
      if (longVal != null) {
    	  longVal = longVal.replace("\n", " ");
    	  return longVal.trim().replaceAll(" +", " ");
      }
      return null;
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
    // remove newlines with VALUE extractor
    //nodeMap.put(ArticleTitle, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(ArticleTitle, CLEAN_TEXT_VALUE);
    nodeMap.put(ArticleFirstPage, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(ArticleLastPage, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(AuthorName, AUTHOR_VALUE);
    nodeMap.put(Para, CLEAN_TEXT_VALUE);
    nodeMap.put(Keyword, CLEAN_TEXT_VALUE);
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
