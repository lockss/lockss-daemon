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

package org.lockss.plugin.clockss.wolterskluwer;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.PublicationDate;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  WoltersKluwer source files
 *  
 */
public class WoltersKluwerSourceXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(WoltersKluwerSourceXmlSchemaHelper.class);

  private static final String NAME_SEPARATOR = ", ";
  private static final String NAME_SPACE = " ";
  private static final String DATE_SEPARATOR = "-";
  private static final String TITLE_SEPARATOR = ":";
  private static final String ZERO_YEAR = "0";
  private static final String EMPTY_DATE = null;

  /* 
   *  WoltersKluwer specific node evaluators to extract the information we want
   */
  /*
   * AUTHOR information
   * NODE=<article-node>./BB/BY/PN  
   *  FN= 
   *  SN=
   *  MN=
   *  requires at least either a surname or a firstname ("Cher?")
   */
  static private final NodeValue WK_AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of WOLTERSKLUWER contributor");
      NodeList childNodes = node.getChildNodes(); 
      if (childNodes == null) return null;
      String fname = null;
      String sname = null;
      String mname = null;

      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        String nodeName = infoNode.getNodeName();
        /*
          /DG/D/BB/BY/PN/FN=Mishka
          /DG/D/BB/BY/PN/SN=Terplan
          /DG/D/BB/BY/PN/MN=M
         */
        if (WK_FN.equals(nodeName)){
          fname = infoNode.getTextContent();
        } else if (WK_SN.equals(nodeName)){
          sname = infoNode.getTextContent();
        } else if (WK_MN.equals(nodeName)){
          mname = infoNode.getTextContent();
        }
      }      
      if ((sname == null) && (fname == null)) {
        log.debug3("no valid contributor found");
        return null;
      }
      StringBuilder names = new StringBuilder();
      // now at least some of the name is available - return as much as possible
      // if we try to names.append(null) it adds "null" - don't want that!
      if (sname != null) {
        names.append(sname);
        if (fname != null) {
          names.append (NAME_SEPARATOR + fname);
        } 
        if (mname!= null) {
          names.append (NAME_SPACE + mname);
        }
      } else {  // else only a givenname
        names.append(fname);
      }
      return names.toString();
    }
  };

  /* 
   * PUBLISHING DATE - <article-node>/BB/SO/DA
   *   DY=15
   *   MO=March
   *   YR=2014
   */
  static private final NodeValue WK_DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of WOLTERSKLUWER date");
      NodeList childNodes = node.getChildNodes();
      if (childNodes == null) return null;
      String year = null;
      String month = null;
      String day = null;
      String nodeName = null;

      for (int m = 0; m < childNodes.getLength(); m++) {
        Node childNode = childNodes.item(m);
        nodeName = childNode.getNodeName();
        if (WK_DY.equals(nodeName)) {
          day = childNode.getTextContent();
        } else if (WK_MO.equals(nodeName)) {
          month = childNode.getTextContent();
        } else if (WK_YR.equals(nodeName)) {
          year = childNode.getTextContent();
        }
      }

      // make it W3C format YYYY or YYYY-MM or YYYY-MM-DD
      StringBuilder dBuilder = new StringBuilder();
      if (year == null) return EMPTY_DATE;
      dBuilder.append(year); //YYYY
      if (month == null) {
        return dBuilder.toString();     // return just YYYY, if no MM
      } else {
        dBuilder.append(DATE_SEPARATOR + month);
        if (day != null) {
          dBuilder.append(DATE_SEPARATOR + day);
        }
      }
      // WK sometimes uses a text month (eg "June" instead of 06"), which won't 
      // work with the database, so converting. If the date is invalid,
      // return "" (will be a null date in the database)
      PublicationDate date = null;
      try {
        // if the date is invalid, in 1.67 PublicationDate will throw a ParseException
        // To be backward compatible, this will throw/catch its own exception
        date = new PublicationDate(dBuilder.toString());
        if (ZERO_YEAR.equals(date.toString())){
          throw new ParseException(date.toString(), 0);
        }
      } catch (ParseException e) {
        // invalid date (eg year == 0)
        return EMPTY_DATE;
      }
      return date.toString();

    }
  };

  /* 
   * Article Title, Subtitle - 
   *   <article-node>/BB/TG
   *  TI=ValidTitle
   *  STI= with an equally valid subtitle
   * we will take one or both
   */

  static private final NodeValue WK_ARTICLE_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of WOLTERSKLUWER ARTICLE TITLE");
      String title = null;
      String subtitle = null;
      String nodeName = null;
      NodeList childNodes = node.getChildNodes(); 
      if (childNodes == null) return null;

      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        nodeName = infoNode.getNodeName();

        if (WK_TITLE.equals(nodeName)) {
          title = infoNode.getTextContent();
        } else if(WK_SUBTITLE.equals(nodeName)){
          subtitle = infoNode.getTextContent();
        }
      }

      StringBuilder titleVal = new StringBuilder();
      if (title != null) {
        titleVal.append(title);
      }
      if (subtitle != null) {
        titleVal.append(TITLE_SEPARATOR + subtitle);
      }
      if (titleVal.length() != 0)  {
        log.debug3("article title: " + titleVal.toString());
        return titleVal.toString();
      } else {
        log.debug3("no value in this article title");
        return null;
      }
    }
  };

  /*
   * The xpath limits us to exactly the node we want, but we need
   * to do some cleanup of the doi, which is somewhat variable
   * 
   * good value: 10.1097/NCC.0b013e3181ef77a0
   * others that we can figure out:
   *   DOI: 10.1097/NCC.0b013e3181ef77a0
   *      10.1097/NCC.0b013e3181ef77a0 // leading space
   *   DOI : 10.1097/NCC.0b013e3181ef77a0
   *   DOI 10.1097/NCC.0b013e3181ef77a0 // : was &colon; which disappears
   *   http://10.1097/AOG.0b013e31827c60e1
   *   //10.1097/AOG.0b013e318292aea4
   * some we just have to let go, too risky to guess at intent
   *   110.1097/HJH.0000000000000289
   *   10.231/JIM.0b013e318255e8fd
   *   00.0000/000.000010.1097/BRS.0000000000000341
   *   TA.0b013e3182754654
   */
  
  // really? you can have 6 after the dot?? This is from MetadataField isDoi() validator
  private static final String STD_DOI_PATTERN = "10[.][0-9a-z]{4,6}\\/.*";
  private static Pattern DOI_PATTERN = Pattern.compile("^\\s*(DOI\\s*(:)?|http:\\/\\/|\\/\\/)?\\s*(" + STD_DOI_PATTERN + ")\\s*$", Pattern.CASE_INSENSITIVE);
  static private final NodeValue WK_DOI_VALUE = new NodeValue() {

    @Override
    public String getValue(Node node) {
      log.debug3("getValue of  DOI");
      String doiVal = node.getTextContent();
      Matcher iMat = DOI_PATTERN.matcher(doiVal); 
      if(!iMat.matches()){
        log.debug3("no match");
        return null;
      }
      log.debug3("returning " + iMat.group(3));
      return iMat.group(3);
    }
  };
  
  /*
   * ISSN evaluator:
   * We are at ./BB/SO/ISN
   * 
   * Do our best to pull a valid ISSN from given text. The following
   * has shown up:
   * 1070-8022 //correct
   * 1110 -1148
   * in these the &sol; disappears leaving a too long number
   * so just take the first 4-4
   * 0041-1337&sol;12&sol;9402-139
   * 0195-7910&sol;11&sol;0000&ndash;0000
   * 0017-907812&sol;0
   */
  private static final String STD_ISSN_PATTERN_STRING = "(\\d{4})\\s*(-)?\\s*(\\d{3}[\\dXx])";
  private static Pattern ISSN_PATTERN =  Pattern.compile("^\\s*" + STD_ISSN_PATTERN_STRING, Pattern.CASE_INSENSITIVE);

  static private final NodeValue WK_ISSN_VALUE = new NodeValue() {

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
   *  WoltersKluwer specific XPATH key definitions that we care about
   */
  // The following are all under WK_articleNode = "/DG/D"
  // but are issue-level info

  // the pdf name still needs some help to match reality:
  //   add a '0' to the front, add ".pdf" to the end
  private static String WK_pdf = "@AN|@an";
  /* publication info */
  private static String WK_journal_title =  "./BB/SO/PB";
  private static String WK_issn = "./BB/SO/ISN"; 
  private static String WK_issue = "./BB/SO/IS/IP";
  private static String WK_vol ="./BB/SO/V";
  /* article title, subtitle */
  private static String WK_TITLENODE = "TG";
  private static String WK_TITLE = "TI";
  private static String WK_SUBTITLE = "STI";
  private static String WK_article_title = "./BB/"+WK_TITLENODE;
  /* doi */
  private static String WK_doi = "./BB/XUI[@XDB='pub-doi']/@UI"; 
  /* published date */
  private static String WK_DY = "DY";
  private static String WK_MO = "MO";
  private static String WK_YR = "YR";
  private static String WK_pubdate = "./BB/SO/DA";
  /* author */
  private static String WK_FN = "FN";
  private static String WK_MN = "MN";
  private static String WK_SN = "SN";
  private static String WK_author = "./BB/BY/PN";

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  WK_articleMap = new HashMap<String,XPathValue>();
  static {
    WK_articleMap.put(WK_pdf, XmlDomMetadataExtractor.TEXT_VALUE); 
    WK_articleMap.put(WK_issn, WK_ISSN_VALUE);
    WK_articleMap.put(WK_journal_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    WK_articleMap.put(WK_doi, WK_DOI_VALUE); 
    WK_articleMap.put(WK_article_title, WK_ARTICLE_TITLE_VALUE); 
    WK_articleMap.put(WK_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    WK_articleMap.put(WK_vol, XmlDomMetadataExtractor.TEXT_VALUE);
    WK_articleMap.put(WK_author, WK_AUTHOR_VALUE);
    WK_articleMap.put(WK_pubdate, WK_DATE_VALUE);

  }

  /* 2.  Top level Nodepath */
  static private final String WK_articleNode = "/DG/D"; 

  /* 3. WK global value we care about: none, so WK_globalMap is null */ 

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(WK_pdf, MetadataField.FIELD_ACCESS_URL);
    cookMap.put(WK_issn, MetadataField.FIELD_ISSN);
    cookMap.put(WK_doi, MetadataField.FIELD_DOI);
    cookMap.put(WK_vol, MetadataField.FIELD_VOLUME);
    cookMap.put(WK_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(WK_journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(WK_article_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(WK_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(WK_pubdate, MetadataField.FIELD_DATE);
  }

  /**
   * WK has a single global information outside of article records
   * return global map
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    //no globalMap, so returning null
    return null;
  }

  /**
   * return WoltersKluwer article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return WK_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return WK_articleNode;
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
    return WK_pdf;
  }

}