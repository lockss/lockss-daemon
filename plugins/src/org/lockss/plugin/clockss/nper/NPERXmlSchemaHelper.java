/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.nper;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;


public class NPERXmlSchemaHelper
implements SourceXmlSchemaHelper {

  private static final Logger log = Logger.getLogger(NPERXmlSchemaHelper.class);

  /*
  <?xml version="1.0" encoding="utf-8"?>
<journal>
    <journal-title-group>
        <journal-title language="EN">Nonpartisan Education Review</journal-title>
        <abbrev-title language="EN">NPE Review</abbrev-title>
    </journal-title-group>
    <issn type="electronic">2150-6477</issn>
    <online-date>
        <year>2005</year>
    </online-date>
    <url>https://nonpartisaneducation.org</url>
    <publisher>
        <publisher-name>Nonpartisan Education Group</publisher-name>
    </publisher>
    <license licenseId="CC-BY-NC-ND-4.0"
             name="Creative Commons Attribution Non Commercial No Derivatives 4.0 International">
        <crossRefs>
            <crossRef>https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode</crossRef>
        </crossRefs>
    </license>
    <pub-attr type="open-access">yes</pub-attr>
    <pub-attr type="academic">yes</pub-attr>
    <pub-attr type="indexedby">DOAJ;RePEc;CNKI;ScienceOpen</pub-attr>

    <article>
        <identifier>StandardsFoundation01_Nonpartisan_Education_Review</identifier>
        <title-group language="EN">
            <title language="EN">Bibliophobia</title>
        </title-group>
        <author-group language="EN">
            <author sequence="1">
                <first-name>Will</first-name>
                <middle-name></middle-name>>>>>>
                <last-name>Fitzhugh</last-name>
            </author>
        </author-group>
        <affiliation-group language="EN">
            <affiliation sequence="1">The Concord Review</affiliation>
            <affiliation sequence="2"></affiliation>
            <affiliation sequence="3"></affiliation>
        </affiliation-group>
        <keywords language="EN">
            <keyword>educational standards</keyword>
            <keyword>student writing</keyword>
            <keyword>school curriculum</keyword>
        </keywords>
        <summary language="EN"></summary>
        <subject language="EN">
            <item>education</item>
            <item>policy</item>
        </subject>
        <language>EN</language>
        <doc-type>Journal Article</doc-type>
        <doi></doi>
        <url>https://nonpartisaneducation.org/Foundation/Bibliophobia.htm</url>
        <url></url>
        <pub-date date-type="pub" publication-format="electronic">
            <month>7</month>
            <year>2006</year>
        </pub-date>
        <volume>StandardsFoundation</volume>
        <issue>1</issue>
        <pages>
            <page>1</page>
            <count>1</count>
            <start-page>1</start-page>
            <end-page>1</end-page>
        </pages>
    </article>
</journal>
  */

  private static final String AUTHOR_SEPARATOR = ",";

  static private final XmlDomMetadataExtractor.NodeValue AUTHOR_VALUE = new XmlDomMetadataExtractor.NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of author");
      NodeList elementChildren = node.getChildNodes();
      // only accept no children if this is a "string-name" node
      if (elementChildren == null &&
              !("string-name".equals(node.getNodeName()))) return null;

      String fname = null;
      String lastname = null;

      if (elementChildren != null) {
        // perhaps pick up iso attr if it's available
        // look at each child
        for (int j = 0; j < elementChildren.getLength(); j++) {
          Node checkNode = elementChildren.item(j);
          String nodeName = checkNode.getNodeName();
          if ("first-name".equals(nodeName)) {
            fname = checkNode.getTextContent();
          } else if ("last-name".equals(nodeName) ) {
            lastname = checkNode.getTextContent();
          }
        }
      } else {
        // we only fall here if the node is a string-name
        // no children - just get the plain text value
        fname = node.getTextContent();
      }

      // where to put the prefix?
      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, empty & whitespace only
      if (!StringUtils.isBlank(fname)) {
        valbuilder.append(fname);
        if (!StringUtils.isBlank(lastname)) {
          valbuilder.append(AUTHOR_SEPARATOR + " " + lastname);
        }
      } else {
        log.debug3("no author found");
        return null;
      }
      log.debug3("author found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  static private final XmlDomMetadataExtractor.NodeValue DATE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of publishing date");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      // perhaps pick up iso attr if it's available
      String tyear = null;
      String tmonth = null;
      // look at each child of the TitleElement for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("month".equals(nodeName) ) {
          tmonth = checkNode.getTextContent();
        } else if ("year".equals(nodeName)) {
          tyear = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tyear != null) {
        valbuilder.append(tyear);
        if (tmonth != null && tmonth.length()>0) {
          valbuilder.append("-" + tmonth);
        }
      } else {
        log.debug3("no date found");
        return null;
      }
      log.debug3("date found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  static private final XmlDomMetadataExtractor.NodeValue TITLE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of title");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      // perhaps pick up iso attr if it's available
      String title = null;
      String subtitle = null;
      // look at each child of the TitleElement for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("sub-title".equals(nodeName) ) {
          subtitle= checkNode.getTextContent();
        } else if ("title".equals(nodeName)) {
          title = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (title != null) {
        valbuilder.append(title);
        if (subtitle != null && subtitle.length()>0) {
          valbuilder.append("-" + subtitle);
        }
      } else {
        log.debug3("no date found");
        return null;
      }
      log.debug3("title found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  static private final String articleNode = "/journal/article|/journal/book";

  protected static final String journal_title = "/journal/title-group/title";
  protected static final String eissn = "/journal/issn[@type=\"electronic\"]";

  private static final String publisher = "/journal/publisher/publisher-name";
  protected static final String article_title =  "title-group";
  protected static final String article_title_alt =  "title";
  protected static final String author =  "author-group/author";
  private static final String art_pubdate =  "pub-date[@date-type=\"pub\"]";
  private static final String volume =  "volume";
  private static final String issue =   "issue";

  protected static final String start_page =  "pages/start-page";
  protected static final String end_page =  "pages/end-page";

  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    articleMap.put(art_pubdate, DATE_VALUE);
    articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_title, TITLE_VALUE);
    articleMap.put(article_title_alt, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(journal_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(author, AUTHOR_VALUE);
    articleMap.put(issue, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(eissn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(volume, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(start_page, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(end_page, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  static private final Map<String,XPathValue>     
  globalMap = null;

  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(article_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(article_title_alt, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(author, MetadataField.FIELD_AUTHOR);
    cookMap.put(art_pubdate, MetadataField.FIELD_DATE);
    cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(eissn, MetadataField.FIELD_EISSN);
    cookMap.put(issue, MetadataField.FIELD_ISSUE);
    cookMap.put(volume, MetadataField.FIELD_VOLUME);
    cookMap.put(start_page, MetadataField.FIELD_START_PAGE);
    cookMap.put(end_page, MetadataField.FIELD_END_PAGE);

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
    return articleNode;
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