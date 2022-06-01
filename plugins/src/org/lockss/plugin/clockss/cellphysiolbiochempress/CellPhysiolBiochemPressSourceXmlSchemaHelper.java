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

package org.lockss.plugin.clockss.cellphysiolbiochempress;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.wiley.WileyMRWSourceXmlSchemaHelper;
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
public class CellPhysiolBiochemPressSourceXmlSchemaHelper
implements SourceXmlSchemaHelper {

  static Logger log = Logger.getLogger(CellPhysiolBiochemPressSourceXmlSchemaHelper.class);

  static private final XmlDomMetadataExtractor.NodeValue AUTHOR_VALUE = new XmlDomMetadataExtractor.NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of wiley author name");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tgiven = null;
      String tsurname = null;
      // look at each child of the TitleElement for debug3rmation
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("FirstName".equals(nodeName)) {
          tgiven = checkNode.getTextContent();
        } else if ("LastName".equals(nodeName) ) {
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

  protected static final String article_title = "/ArticleSet/Article/ArticleTitle";
  protected static final String journal_title = "/ArticleSet/Article/Journal/JournalTitle";
  protected static final String author = "/ArticleSet/Article/AuthorList/Author";
  private static final String publisher = "/ArticleSet/Article/Journal/PublisherName";
  private static final String art_pubdate = "/ArticleSet/Article/Journal/PubDate[@PubStatus = \"ppublish\"]/Year";
  private static final String volume = "/ArticleSet/Article/Journal/Volume";
  private static final String issue = "/ArticleSet/Article/Journal/Issue";
  protected static final String start_page = "/mods/relatedItem[@type = \"host\"]/part/extent[@unit = \"pages\"]/start";
  protected static final String end_page = "/ArticleSet/Article/LastPage";

  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    articleMap.put(art_pubdate, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(journal_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(author, AUTHOR_VALUE);
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