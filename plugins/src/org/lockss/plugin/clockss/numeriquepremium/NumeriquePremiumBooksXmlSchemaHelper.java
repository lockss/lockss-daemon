/*
 * $Id$
 */

/*

 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.numeriquepremium;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlParserHelperUtilities;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

public class NumeriquePremiumBooksXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(NumeriquePremiumBooksXmlSchemaHelper.class);

  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of paper author");
      String surname = null;
      String givennames = null;
      NodeList nameChildren = node.getChildNodes();
      if (nameChildren == null) return null;
      for (int p = 0; p < nameChildren.getLength(); p++) {
        Node partNode = nameChildren.item(p);
        String partName = partNode.getNodeName();
        if ("surname".equals(partName)) {
          surname  = partNode.getTextContent();
        } else if ("given-names".equals(partName)) {
          givennames = partNode.getTextContent();
        }
      }
      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, whitespace and empty
      if (!StringUtils.isBlank(surname)) {
        valbuilder.append(surname);
        if (!StringUtils.isBlank(givennames)) {
          valbuilder.append(", " + givennames);
        }
        return valbuilder.toString();
      } 
      return null;
    }
  };

  private static String book_pub_date  = "/book/book-meta/pub-date[@date-type=\"pub\"]";

  private final static XmlDomMetadataExtractor.NodeValue PUBDATE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
    @Override
    public String getValue(Node node) {
      return SourceXmlParserHelperUtilities.getPubDateFromPubDateXpathNodeValue(node);
    }
  };
  
  private static final String Book = "/book";
  
  // from the /book top node
  private static final String book_meta = "book-meta/";
  public static final String book_doi = book_meta + "book-id[@pub-id-type = 'doi']";
  public static final String book_title = book_meta + "book-title-group/book-title";
  private static final String book_publisher = book_meta + "publisher/publisher-name";
  public static final String book_isbn = book_meta + "isbn[@content-type=\"ppub\"]";
  private static final String book_eisbn = book_meta + "isbn[@content-type=\"epub\"]";
  private static final String book_author = book_meta + "contrib-group/contrib[@contrib-type = 'author' or @contrib-type = 'editor']/name";

  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  static {
    articleMap.put(book_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_eisbn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_pub_date, PUBDATE_VALUE);
    articleMap.put(book_author, AUTHOR_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  static private final String bookNode = Book;

  /* 3. in MARCXML there is no global information because one file/article */
  static private final Map<String,XPathValue> globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(book_doi, MetadataField.FIELD_DOI);
    cookMap.put(book_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(book_eisbn, MetadataField.FIELD_EISBN);
    cookMap.put(book_publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(book_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(book_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(book_pub_date, MetadataField.FIELD_DATE);
  }


  /**
   * no global map - this is null
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
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
    return bookNode;
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
