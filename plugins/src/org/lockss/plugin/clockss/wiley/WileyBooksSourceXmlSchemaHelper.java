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

package org.lockss.plugin.clockss.wiley;

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
 *  wiley source files
 *  
 */
public class WileyBooksSourceXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(WileyBooksSourceXmlSchemaHelper.class);

  private static final String AUTHOR_SPLIT_CH = ",";
  private static final String TITLE_SEPARATOR = ":";

  /*
     <creators>
            <creator xml:id="au1" creatorRole="author">
               <personName>
                  <givenNames>Daniel R.</givenNames>
                  <familyName>Schwarz</familyName>
               </personName>
            </creator>
   </creators>
   */

  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
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
        if ("givenNames".equals(nodeName)) {
          tgiven = checkNode.getTextContent();
        } else if ("familyName".equals(nodeName) ) {
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

  /*
   "fmatter.xml" example
   <?xml version="1.0" encoding="UTF-8"?>
<component xmlns="http://www.wiley.com/namespaces/wiley" xmlns:wiley="http://www.wiley.com/namespaces/wiley/wiley" version="1.0.2" type="bookChapter" xml:lang="en">
   <?documentdebug3 RNGSchema="wileyML3G/V102/rnc/wileyML3G.rnc" type="compact" sourceDTD="JWSCHA15"?>
   <header>
      <publicationMeta level="series">
         <titleGroup>
            <title type="main" xml:lang="en">Blackwell Manifestos</title>
         </titleGroup>
      </publicationMeta>
      <publicationMeta level="product" position="220">
         <publisherdebug3>
            <publisherName>wiley‐Blackwell</publisherName>
            <publisherLoc>Oxford, UK</publisherLoc>
         </publisherdebug3>
         <doi origin="wiley" registered="yes">10.1002/9781444304831</doi>
         <isbn type="online-13">9781444304831</isbn>
         <isbn type="printCloth-13">9781405130981</isbn>
         <idGroup>
            <id type="product" value="O9781444304831" />
         </idGroup>
         <titleGroup>
            <title type="main" xml:lang="en" sort="IN DEFENSE OF READING">In Defense of Reading</title>
            <title type="tocForm">In Defense of Reading: Teaching Literature in the Twenty‐First Century</title>
            <title type="subtitle">Teaching Literature in the Twenty‐First Century</title>
            <title type="short">Schwarz/In Defense of Reading</title>
         </titleGroup>
         <copyright ownership="thirdParty">Copyright © 2008 Daniel R. Schwarz</copyright>
         <eventGroup>
            <event type="publishedPrint" date="2008-09-05" />
            <event type="publishedOnlineProduct" date="2009-02-20" />
         </eventGroup>
         <creators>
            <creator xml:id="au1" creatorRole="author">
               <personName>
                  <givenNames>Daniel R.</givenNames>
                  <familyName>Schwarz</familyName>
               </personName>
            </creator>
         </creators>
      </publicationMeta>
      <publicationMeta level="unit" type="frontmatter" position="10">
         <doi origin="wiley" registered="yes">10.1002/9781444304831.fmatter</doi>
         <idGroup>
            <id type="unit" value="fmatter" />
            <id type="file" value="fmatter" />
         </idGroup>
         <countGroup>
            <count type="pageTotal" number="16" />
         </countGroup>
         <copyright ownership="thirdParty">Copyright © 2008 Daniel R. Schwarz</copyright>
  */


  static private final String topNode = "/component/header/publicationMeta[@level = \"product\"]";
  private static final String publisher = topNode + "/publisherdebug3/publisherName";
  private static final String book_title = topNode + "/titleGroup/title[@type = \"main\"]";
  private static final String book_title_alt = topNode + "/titleGroup/title[@type = \"tocForm\"]";
  private static final String isbn = topNode + "/isbn[@type = \"online-13\"]";
  private static final String doi = topNode + "/doi";
  private static final String art_pubdate = topNode + "/eventGroup/event[@type = \"publishedPrint\"]/@date";
  private static final String author =  topNode + "/creators/creator[@creatorRole = \"author\"]/personName\n";


  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(art_pubdate, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(author, AUTHOR_VALUE);
    articleMap.put(book_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_title_alt, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(doi, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  static private final Map<String,XPathValue>     
  globalMap = null;

  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(isbn, MetadataField.FIELD_ISBN);
    cookMap.put(doi, MetadataField.FIELD_DOI);
    cookMap.put(book_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(book_title_alt, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(author, MetadataField.FIELD_AUTHOR);
    cookMap.put(art_pubdate, MetadataField.FIELD_DATE);
  }


  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return null; //globalMap;
  }

  /**
   * return wiley article paths representing metadata of interest  
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
    return topNode;
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