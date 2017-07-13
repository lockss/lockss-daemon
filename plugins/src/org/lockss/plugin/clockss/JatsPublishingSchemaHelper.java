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

package org.lockss.plugin.clockss;

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
 *  the JATS for the "Journal Publishing" tagset
 *  with the filenames based on same name as the .xml file
 *  There is only one record for each file
 *  @author alexohlson
 */
public class JatsPublishingSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(JatsPublishingSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";

  /* 
   *  JATS specific node evaluators to extract the information we want
   */
  
  /* 
   * TITLE INFORMATION
   * We're at the top level of a "<journal-title-group>" or "<title-group>"
   *   <journal-title-group>
   *     <journal-title>text</journal-title>
   *     <journal-subtitle>text</journal-subtitle> optional
   *  </journal-title-group>
   * or
   *   <title-group>
   *     <article-title>text</journal-title>
   *     <subtitle>text</journal-subtitle> optional
   *     <alt-title>ascii text</alt-title> optional
   *  </title-group>
   */
  static private final NodeValue JATS_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of JATS title group");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tTitle = null;
      String tSubtitle = null;
      String tAltTitle = null;
      // look at each child of the TitleElement for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("journal-title".equals(nodeName) | "article-title".equals(nodeName)) {
          tTitle = checkNode.getTextContent();  
          // they deliver newlines in their XML titles
          tTitle = tTitle.replace("\n", " ");
        } else if ("subtitle".equals(nodeName) | "journal-subtitle".equals(nodeName)) {
          tSubtitle = checkNode.getTextContent();
          tSubtitle = tSubtitle.replace("\n", " ");
        } else if ("alt-title".equals(nodeName)) {
          tAltTitle = checkNode.getTextContent();
          tAltTitle = tAltTitle.replace("\n",  " ");
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, whitespace and empty
      if (!StringUtils.isBlank(tTitle)) {
        valbuilder.append(tTitle);
        if (!StringUtils.isBlank(tSubtitle)) {
          valbuilder.append(": " + tSubtitle);
        }
      } else if (!StringUtils.isBlank(tAltTitle)) {
          valbuilder.append(tAltTitle);
      } else {
        log.debug3("no title found within title group");
        return null;
      }
      log.debug3("title found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };
  
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
  static private final NodeValue JATS_DATE_VALUE = new NodeValue() {
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
   * AUTHOR INFORMATION
   * We're at the top level of a 
   *   "<contrib><name>" or
   *   "<contrib><name-alternatives><name name-style="western">"
   * 
   *   <name>
   *     <surname>
   *     <given-names>
   *     <prefix>
   *   </name
   */
  static private final NodeValue JATS_AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of JATS author");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      // perhaps pick up iso attr if it's available 
      String tsurname = null;
      String tgiven = null;
      String tprefix = null;
      // look at each child 
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("surname".equals(nodeName)) {
          tsurname = checkNode.getTextContent();
        } else if ("given-names".equals(nodeName) ) {
          tgiven = checkNode.getTextContent();
        } else if ("prefix".equals(nodeName)) {
          tprefix = checkNode.getTextContent();
        }
      }

      // where to put the prefix?
      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, empty & whitespace only
      if (!StringUtils.isBlank(tsurname)) {
        valbuilder.append(tsurname);
        if (!StringUtils.isBlank(tgiven)) {
          valbuilder.append(AUTHOR_SEPARATOR + " " + tgiven);
        }
      } else {
        log.debug3("no author found");
        return null;
      }
      log.debug3("author found: " + valbuilder.toString());
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

 static private final NodeValue JATS_ISSN_VALUE = new NodeValue() {

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
   *  JATS specific XPATH key definitions that we care about
   */

  private static String JATS_article = "/article";
  
  /* these are all relative to the /article node */
  private static String JATS_jmeta =  "front/journal-meta";
  private static String JATS_ameta =  "front/article-meta";
  
  private static String JATS_jtitle = JATS_jmeta + "/journal-title-group";
  public static String JATS_jid_pubmed = JATS_jmeta + "/journal-id[@journal-id-type = \"pubmed\"]";
  public static String JATS_jid_publisher = JATS_jmeta + "/journal-id[@journal-id-type = \"publisher\"]";
  // early versions of JATS (2.2 see CambridgePress) have title/subtitle as
  // direct journal-meta children 
  private static String JATS_jtitle_early = JATS_jmeta + "/journal-title";
  private static String JATS_jsubtitle_early = JATS_jmeta + "/journal-subtitle";
  // no attribute at all, or might specify type of issn
  private static String JATS_issn = JATS_jmeta + "/issn[not(@*)]";
  // leave public for post-processing
  //pub-type is deprecated; publication-format new, but we must handle all variant
  public static String JATS_pissn = JATS_jmeta + "/issn[@pub-type = \"ppub\" or @publication-format=\"print\"]";
  private static String JATS_eissn = JATS_jmeta + "/issn[@pub-type = \"epub\" or @publication-format=\"electronic\"]";
  public static String JATS_pubname = JATS_jmeta + "/publisher/publisher-name";
  
  private static String JATS_doi =  JATS_ameta + "/article-id[@pub-id-type = \"doi\"]";
  private static String JATS_atitle = JATS_ameta + "/title-group";
  private static String JATS_volume = JATS_ameta + "/volume";
  private static String JATS_issue = JATS_ameta + "/issue";
  private static String JATS_fpage = JATS_ameta + "/fpage";
  private static String JATS_lpage = JATS_ameta + "/lpage";
  // used by WARC in post-commit
  public static String JATS_self_uri = JATS_ameta + "/self-uri";
  
  public static String JATS_copydate = JATS_ameta + "/permissions/copyright-year";
  // The date could be identified by new or by older tag attributes
  // as a backup
  private static String pubdate_attr_options = "@date-type = \"pub\" or @pub-type = \"ppub\"" +
      " or @pub-type = \"pub\"";
  private static String epubdate_attr_options = "@pub-type = \"epub\" or @pub-type = \"epub-ppub\"";

  public static String JATS_date = JATS_ameta + "/pub-date[" + pubdate_attr_options +"]";
  public static String JATS_edate = JATS_ameta + "/pub-date[" + epubdate_attr_options +"]";
  /* extra level for westernized version of chinese character names */
  private static String JATS_contrib = JATS_ameta + "/contrib-group/contrib/name | " + JATS_ameta + "/contrib-group/contrib/name-alternatives/name[@name-style = \"western\"]";
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */
  
  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> JATS_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    JATS_articleMap.put(JATS_jtitle, JATS_TITLE_VALUE);
    // only in earlier versions
    JATS_articleMap.put(JATS_jtitle_early, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_jsubtitle_early, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_jid_pubmed, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_jid_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    

    JATS_articleMap.put(JATS_issn, JATS_ISSN_VALUE);
    JATS_articleMap.put(JATS_pissn, JATS_ISSN_VALUE);
    JATS_articleMap.put(JATS_eissn, JATS_ISSN_VALUE);
    JATS_articleMap.put(JATS_pubname, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_atitle, JATS_TITLE_VALUE);
    JATS_articleMap.put(JATS_volume, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_fpage, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_lpage, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_self_uri, XmlDomMetadataExtractor.TEXT_VALUE);
    JATS_articleMap.put(JATS_date, JATS_DATE_VALUE);
    JATS_articleMap.put(JATS_edate, JATS_DATE_VALUE);
    JATS_articleMap.put(JATS_copydate, XmlDomMetadataExtractor.TEXT_VALUE); 
    JATS_articleMap.put(JATS_contrib, JATS_AUTHOR_VALUE);

  }

  /* 2. Each item (article) has its own XML file */
  static private final String JATS_articleNode = JATS_article; 

  /* 3. in JATS there is no global information because one file/article */
  static private final Map<String,XPathValue> JATS_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    // you either get the jtitle or the jtitle_early, not both
    cookMap.put(JATS_jtitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(JATS_jtitle_early, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(JATS_atitle, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(JATS_doi, MetadataField.FIELD_DOI);
    // pick up both pissn and issn...unlikely both are present
    cookMap.put(JATS_issn, MetadataField.FIELD_ISSN);
    cookMap.put(JATS_pissn, MetadataField.FIELD_ISSN);
    cookMap.put(JATS_eissn, MetadataField.FIELD_EISSN);
    //cookMap.put(JATS_pubname, MetadataField.FIELD_PUBLISHER);
    cookMap.put(JATS_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(JATS_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(JATS_fpage, MetadataField.FIELD_START_PAGE);
    cookMap.put(JATS_lpage, MetadataField.FIELD_END_PAGE);
    cookMap.put(JATS_contrib, MetadataField.FIELD_AUTHOR);
    cookMap.put(JATS_copydate, MetadataField.FIELD_DATE);
    
  }


  /**
   * JATS does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return JATS_globalMap;
  }

  /**
   * return JATS article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return JATS_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return JATS_articleNode;
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
