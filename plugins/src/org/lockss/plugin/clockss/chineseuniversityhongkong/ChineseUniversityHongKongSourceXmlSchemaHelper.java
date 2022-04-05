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

package org.lockss.plugin.clockss.chineseuniversityhongkong;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;


/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Chinese University of Hong Kong source files
 *  
 */
public class ChineseUniversityHongKongSourceXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(ChineseUniversityHongKongSourceXmlSchemaHelper.class);

  /*
   <?xml version="1.0" encoding="UTF-8"?>
<mods xmlns="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-4.xsd">
    <titleInfo>
        <title>版權、稿約及稿例、目錄</title>
    </titleInfo>
    <typeOfResource>text</typeOfResource>
    <genre>article</genre>
    <originInfo>
        <place>
            <placeTerm type="text">香港</placeTerm>
        </place>
        <publisher>道風山基督教叢林</publisher>
        <dateCreated>1994-06</dateCreated>
        <issuance>serial</issuance>
    </originInfo>
    <language>
        <languageTerm type="text">Chi</languageTerm>
    </language>
    <physicalDescription>
        <form>digital</form>
    </physicalDescription>
    <relatedItem type="host">
        <titleInfo>
            <title>道風</title>
            <subTitle>漢語神學學刊</subTitle>
        </titleInfo>
        <part>
            <detail type="issue">
                <number>第一期</number>
            </detail>
            <extent unit="pages"><start>1-7</start></extent>
            <date>1994-06</date>
        </part>
    </relatedItem>
    <location>
        <url>http://library.cuhk.edu.hk/record=b2057943</url>
    </location>
    <accessCondition type="useAndReproduction">Use of this resource is governed by the terms and conditions of the Creative Commons “Attribution-NonCommercial-NoDerivatives 4.0 International” License (http://creativecommons.org/licenses/by-nc-nd/4.0/)</accessCondition>
    <identifier type="local" displayLabel="FileName">i01_p001007</identifier>
    <identifier type="local"></identifier>
    <recordInfo>
        <recordOrigin>Created by CUHK Library using Google Refine</recordOrigin>
    </recordInfo></mods>

  */

  static private final NodeValue ARTICLE_ID = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of Chinese University of Hong Kong Article ID");
      String issue = null;
      Node parent = node.getParentNode();
      NodeList childNodes = parent.getChildNodes();
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node child = childNodes.item(m);
        if (article_title.equals(child.getNodeName()) ){
          issue = child.getTextContent();
        }
        if (issue != null) {
          break;
        }
      }

      StringBuilder issueVal = new StringBuilder();
      if (issue != null) {
        issueVal.append(issue);
      }

      if (issueVal.length() != 0)  {
        log.debug3("Article ID: " + issueVal.toString());
        return issueVal.toString();
      } else {
        log.debug3("no value in this Article ID");
        return null;
      }

    }
  };



  protected static final String article_title = "/mods/relatedItem[@type=\"host\"]/titleInfo/title";
  protected static final String article_subtitle = "/mods/relatedItem[@type=\"host\"]/titleInfo/subTitle";
  private static final String publisher = "/mods/originInfo/publisher";
  private static final String art_pubdate = "/mods/originInfo/dateCreated";
  private static final String article_id = "/mods/identifier";
  private static final String issue = "/mods/relatedItem[@type=\"host\"]/part/detail[@type=\"issue\"]/number";

  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    articleMap.put(art_pubdate, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_subtitle, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(issue, XmlDomMetadataExtractor.TEXT_VALUE);
    //articleMap.put(article_id, ARTICLE_ID);
    //articleMap.put(article_id, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  static private final Map<String,XPathValue>     
  globalMap = null;

  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(art_pubdate, MetadataField.FIELD_DATE);
    cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(issue, MetadataField.FIELD_ISSUE);
  }


  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return null; //globalMap;
  }

  /**
   * return Chinese University of Hong Kong article paths representing metadata of interest  
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
    return null;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  @Override
  public String getFilenameXPathKey() {
    return null;
  }
}