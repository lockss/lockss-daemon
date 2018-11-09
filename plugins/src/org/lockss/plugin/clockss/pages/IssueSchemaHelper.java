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

package org.lockss.plugin.clockss.pages;

import org.apache.commons.collections.map.MultiValueMap;
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
 *  the issue-xml (otherwise like jats) for the "Journal Publishing" tagset
 *  with the filenames based on same name as the
 *  Currently pages is doing a whole issue at a time, not toc article information 
 *  @author alexohlson
 */
public class IssueSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(IssueSchemaHelper.class);

 
  /* 
   * DATE INFORMATION
   * This has variations according to when the tag set was used
   * earlier combined both type of publication and type of date
   * @date-type = (ppub, epub, epub-ppub) <- mean print or epublication
   *     as opposed to pcorrection, or pretraction
   * later, the tag set separated type of publication from type of date    
   * @date-type = pub, corrected
   * @publication-format = print or electronic 
   * 
   * We're at the top level of a "<pub-date>" and 
   * @date-type = "pub",  "epub", "ppub", "epub-ppub"
   *   <pub-date date-type="pub"> or
   *   <pub-date date-type="pub" iso-8601-date="1999-03-27">
   *     <day>27</day> optional
   *     <month>03></month> optional
   *     <year>1999</year> 
   *   </pub-date>   
   */
  static private final NodeValue IX_DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of JATS publishing date");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      // perhaps pick up iso attr if it's available 
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
  
 
  
  
  /* 
   * ISSN evaluator - really a validator                                                                                                                                                                              
  *                                                                                                                                                                                               
  * Do our best to pull a valid ISSN from given text. The following                                                                                                                               
  * has shown up:                                                                                                                                                                                 
  * 1070-8022 //correct                                                                                                                                                                           
  * 1110 -1148                                                                                                                                                                                                                                                                                                                                                   
  */
 private static final String STD_ISSN_PATTERN_STRING = "(\\d{4})\\s*(-)?\\s*(\\d{3}[\\dXx])";
 private static Pattern ISSN_PATTERN =  Pattern.compile("^\\s*" + STD_ISSN_PATTERN_STRING, Pattern.CASE_INSENSITIVE);

 static private final NodeValue IX_ISSN_VALUE = new NodeValue() {

   @Override
   public String getValue(Node node) {
     log.debug3("getValue of WOLTERSKLUWER ISSN");
     String issnVal = node.getTextContent();
     Matcher iMat = ISSN_PATTERN.matcher(issnVal);
     if(!iMat.find()){ //use find not match to ignore trailing stuff                                                                                                                              
       log.debug3("no match");
       return null;
     }
     StringBuilder retVal = new StringBuilder();
     retVal.append(iMat.group(1));
     retVal.append("-");
     retVal.append(iMat.group(3));
     log.debug3("returning " + retVal.toString());
     return retVal.toString();
   }
 };


  /* 
   *  ISSUE-XML specific XPATH key definitions that we care about
   */

  private static String IX_wholeissue = "/issue-xml";
  
  /* these are all relative to the /issue-xml node */
  private static String IX_jmeta =  "journal-meta";
  
  private static String IX_jtitle = IX_jmeta + "/abbrev-journal-title";
  public static String IX_jid_publisher = IX_jmeta + "/journal-id[@journal-id-type = \"publisher\"]";
  // no attribute at all, or might specify type of issn
  private static String IX_issn = IX_jmeta + "/issn[not(@*)]";
  // leave public for post-processing
  //pub-type is deprecated; publication-format new, but we must handle all variant
  public static String IX_pissn = IX_jmeta + "/issn[@pub-type = \"ppub\" or @publication-format=\"print\"]";
  private static String IX_eissn = IX_jmeta + "/issn[@pub-type = \"epub\" or @publication-format=\"electronic\"]";
  public static String IX_pubname = IX_jmeta + "/publisher/publisher-name";
  

  public static String IX_imeta = "issue-meta";
  
  private static String pubdate_attr_options = "@date-type = \"pub\" or @pub-type = \"ppub\" or not(@pub-type)" +
      " or @pub-type = \"pub\"";
  private static String epubdate_attr_options = "@pub-type = \"epub\" or @pub-type = \"epub-ppub\"";

  public static String IX_date = IX_imeta + "/pub-date[" + pubdate_attr_options +"]";
  public static String IX_edate = IX_imeta + "/pub-date[" + epubdate_attr_options +"]";
  private static String IX_title = IX_imeta + "/issue-title";
  private static String IX_volume = IX_imeta + "/volume";
  private static String IX_issue = IX_imeta + "/issue";
  private static String IX_doi =  IX_imeta + "/issue-id[@pub-id-type = \"doi\"]";
  
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */
  
  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> IX_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    IX_articleMap.put(IX_jtitle, XmlDomMetadataExtractor.TEXT_VALUE);
    IX_articleMap.put(IX_jid_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    IX_articleMap.put(IX_issn, IX_ISSN_VALUE);
    IX_articleMap.put(IX_pissn, IX_ISSN_VALUE);
    IX_articleMap.put(IX_eissn, IX_ISSN_VALUE);
    IX_articleMap.put(IX_pubname, XmlDomMetadataExtractor.TEXT_VALUE);
    IX_articleMap.put(IX_title,  XmlDomMetadataExtractor.TEXT_VALUE);
    IX_articleMap.put(IX_volume, XmlDomMetadataExtractor.TEXT_VALUE);
    IX_articleMap.put(IX_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    IX_articleMap.put(IX_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    IX_articleMap.put(IX_date, IX_DATE_VALUE);
    IX_articleMap.put(IX_edate, IX_DATE_VALUE);

  }

  /* 2. Each item (article) has its own XML file */
  static private final String IX_articleNode = IX_wholeissue; 

  /* 3. in JATS there is no global information because one file/article */
  static private final Map<String,XPathValue> IX_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    // you either get the jtitle or the jtitle_early, not both
    cookMap.put(IX_jtitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(IX_title, MetadataField.FIELD_ARTICLE_TITLE);
    // pick up both pissn and issn...unlikely both are present
    cookMap.put(IX_issn, MetadataField.FIELD_ISSN);
    cookMap.put(IX_pissn, MetadataField.FIELD_ISSN);
    cookMap.put(IX_eissn, MetadataField.FIELD_EISSN);
    //cookMap.put(IX_pubname, MetadataField.FIELD_PUBLISHER);
    cookMap.put(IX_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(IX_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(IX_date, MetadataField.FIELD_DATE);
    
  }


  /**
   * JATS does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return IX_globalMap;
  }

  /**
   * return JATS article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return IX_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return IX_articleNode;
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
