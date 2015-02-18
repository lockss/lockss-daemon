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

package org.lockss.plugin.clockss.tangram;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import java.util.*;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Tangram source files
 *  
 */
public class TangramSourceXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(TangramSourceXmlSchemaHelper.class);
  static StringBuilder urlName = new StringBuilder();

  private static final String AUTHOR_SPLIT_CH = ",";
  private static final String TITLE_SEPARATOR = ":";
  /* 
   *  Tangram specific node evaluators to extract the information we want
   */
  /* 
   * Book Title, Subtitle - 
   *  <Titolo>Book Title</Titolo>
   *  <Sottotitolo>SubTitle</Sottotitolo>
   *  if both: Title:SubTitle
   *  or, either Title, or :SubTitle
   */
  
  static private final NodeValue TANGRAM_BOOK_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of Tangram BOOK TITLE");
      String title = null;
      String subtitle = null;
      String nodeName = null;
      Node parent = node.getParentNode();
      NodeList childNodes = parent.getChildNodes(); 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node child = childNodes.item(m);
        if (Tangram_book_title.equals(child.getNodeName()) ){
          title = child.getTextContent();
        } else if (Tangram_book_subtitle.equals(child.getNodeName())) {
          subtitle = child.getTextContent();
        }
        if ((title != null) && (subtitle != null)) {
          break;
        }
      }     
      
      StringBuilder titleVal = new StringBuilder();
      if (title != null) {
        titleVal.append(title);
      }
      // if empty title, but subtitle exists => " :Subtitle" 
      // not a great title, but valuable to know...
      if (subtitle != null) {
        titleVal.append(TITLE_SEPARATOR + subtitle);
      }
      if (titleVal.length() != 0)  {
        log.debug3("book title: " + titleVal.toString());
        return titleVal.toString();
      } else {
        log.debug3("no value in this book title");
        return null;
      }
      
    }
  };

  /*
   * Metadata example
   * /node
   *  /Elenco_autori_curatori_principali [principal author]
   *  /Elenco_autori_curatori_principali_INV [principal author, inverted]
   *  /Titolo [title]
   *  /Sottotitolo [subtitle]
   *  /Categoria [category]
   *  /Formato [format]
   *  /Numero_pagine [# pages]
   *  /ISBN
   *  /PDF
   *  /Prezzo_di_copertina [price]
   *  /Data_pubblicazione [pub date]
   *  /Collana [Series]
   *  /URL_Libro   [book URL] 
   */

  /* 
   *  Tangram specific XPATH key definitions that we care about
   */

  /* Under an item node, the interesting bits live at these relative locations */

  private static final String Tangram_book_title = "Titolo";
  private static final String Tangram_book_subtitle = "Sottotitolo";
  private static final String Tangram_isbn = "ISBN";
  private static final String Tangram_art_pubdate = "Data_pubblicazione";
  /* access.url (relative) */
  private static final String Tangram_book_url = "PDF";
  /* author(s), single or multiple in one tag */
  private static final String Tangram_author =  "Elenco_autori_curatori_principali";
  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  Tangram_articleMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    Tangram_articleMap.put(Tangram_art_pubdate, XmlDomMetadataExtractor.TEXT_VALUE); 
    Tangram_articleMap.put(Tangram_author, XmlDomMetadataExtractor.TEXT_VALUE);
    Tangram_articleMap.put(Tangram_book_title, TANGRAM_BOOK_TITLE_VALUE);
    Tangram_articleMap.put(Tangram_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    Tangram_articleMap.put(Tangram_book_url, XmlDomMetadataExtractor.TEXT_VALUE);
  }


  /* 2. All the metadata is neatly under the <node> for each book
   */
  static private final String Tangram_topNode = "/catalogo/xml/node";

  /* 3. in Tangram, there some global information we gather */ 
  static private final Map<String,XPathValue>     
  Tangram_globalMap = null; //new HashMap<String,XPathValue>();

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(Tangram_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(Tangram_book_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(Tangram_author, 
        new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CH)));
    cookMap.put(Tangram_art_pubdate, MetadataField.FIELD_DATE);
  }


  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return null; //Tangram_globalMap;
  }

  /**
   * return Tangram article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return Tangram_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return Tangram_topNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /* (non-Javadoc)
   * @see org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractorHelper#getDeDuplicationXPathKey()
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractorHelper#getConsolidationXPathKey()
   */
  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractorHelper#getFilenameXPathKey()
   */
  @Override
  public String getFilenameXPathKey() {

    return Tangram_book_url;
  }

}