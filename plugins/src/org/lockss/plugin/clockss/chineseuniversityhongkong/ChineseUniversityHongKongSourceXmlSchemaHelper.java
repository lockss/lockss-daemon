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

  protected static final String article_title = "/mods/titleInfo/title";
  protected static final String journal_subtitle = "/mods/relatedItem[@type=\"host\"]/titleInfo/subTitle";
  protected static final String journal_title = "/mods/relatedItem[@type=\"host\"]/titleInfo/title";
  protected static final String author = "/mods/name[@type = \"personal\"]/namePart";
  private static final String publisher = "/mods/originInfo/publisher";
  private static final String art_pubdate = "/mods/originInfo/dateCreated";
  private static final String issue = "/mods/relatedItem[@type = \"host\"]/part/detail[@type = \"issue\"]/number";
  protected static final String start_page = "/mods/relatedItem[@type = \"host\"]/part/extent[@unit = \"pages\"]/start";

  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    articleMap.put(art_pubdate, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(journal_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(journal_subtitle, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(author, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(issue, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(start_page, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  static private final Map<String,XPathValue>     
  globalMap = null;

  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(article_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(author, MetadataField.FIELD_AUTHOR);
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