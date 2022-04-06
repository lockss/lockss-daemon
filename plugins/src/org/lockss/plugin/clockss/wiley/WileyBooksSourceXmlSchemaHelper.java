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

      log.info("getValue of wiley author name");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      String tgiven = null;
      String tsurname = null;
      // look at each child of the TitleElement for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("givenNames".equals(nodeName)) {
          log.info("givenNames found" );
          tgiven = checkNode.getTextContent();
        } else if ("familyName".equals(nodeName) ) {
          log.info("familyName found" );
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
      log.info("name found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  /*
   "fmatter.xml" example
   <?xml version="1.0" encoding="UTF-8"?>
<component xmlns="http://www.wiley.com/namespaces/wiley" xmlns:wiley="http://www.wiley.com/namespaces/wiley/wiley" version="1.0.2" type="bookChapter" xml:lang="en">
   <?documentInfo RNGSchema="wileyML3G/V102/rnc/wileyML3G.rnc" type="compact" sourceDTD="JWSCHA15"?>
   <header>
      <publicationMeta level="series">
         <titleGroup>
            <title type="main" xml:lang="en">Blackwell Manifestos</title>
         </titleGroup>
      </publicationMeta>
      <publicationMeta level="product" position="220">
         <publisherInfo>
            <publisherName>wiley‐Blackwell</publisherName>
            <publisherLoc>Oxford, UK</publisherLoc>
         </publisherInfo>
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
  private static final String publisher = topNode + "/publisherInfo/publisherName";
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