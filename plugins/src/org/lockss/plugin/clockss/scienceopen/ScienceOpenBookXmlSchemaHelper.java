/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.scienceopen;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

/**
 *  A helper class that defines a schema for Carl Grossman books
 *  @author markom
 */
public class ScienceOpenBookXmlSchemaHelper
    implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(ScienceOpenBookXmlSchemaHelper.class);

  /*
  Parse the surname and given names of all the authors into a comma seperated list.
  <contrib-group>
      <contrib contrib-type="author">
          <name>
              <surname>Bekker</surname>
               <given-names>Simon</given-names>
          </name>
      </contrib>
      <contrib contrib-type="author">
          <name>
              <surname>Croese</surname>
              <given-names>Sylvia</given-names>
          </name>
      </contrib>
      <contrib contrib-type="author">
          <name>
              <surname>Pieterse</surname>
              <given-names>Edgar</given-names>
          </name>
      </contrib>
  </contrib-group>
   */
  static private final XmlDomMetadataExtractor.NodeValue AUTHOR_VALUE = new XmlDomMetadataExtractor.NodeValue() {
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


  /*
    <pub-date publication-format="print" iso-8601-date="2021-01-20">
        <day>20</day>
        <month>1</month>
        <year>2021</year>
    </pub-date>
   */

  static private final XmlDomMetadataExtractor.NodeValue DATE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of book pubdate");
      String day = null;
      String month = null;
      String year = null;
      NodeList nameChildren = node.getChildNodes();
      if (nameChildren == null) return null;
      for (int p = 0; p < nameChildren.getLength(); p++) {
        Node partNode = nameChildren.item(p);
        String partName = partNode.getNodeName();
        if ("year".equals(partName)) {
          year  = partNode.getTextContent();
        } else if ("month".equals(partName)) {
          month = partNode.getTextContent();
      } else if ("day".equals(partName)) {
        day = partNode.getTextContent();
        }
      }
      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, whitespace and empty
      if (StringUtils.isBlank(year)) {
        return null;
      }
      valbuilder.append(year);
      if ((!StringUtils.isBlank(month) && (!StringUtils.isBlank(day)))) {
        valbuilder.append("-" + month + "-" + day);
      }
      return valbuilder.toString();
    }
  };

  //There are two levels of XML files - full book (conference) listing
  // which would be the equivalent of a TOC - this has the publisher and
  // the date (maybe) and the isbn (maybe). Nested many levels down are the
  // individual article titles and doi, but not the PDF name nor the contributors
  // Then there are the individual article level XML where the filename matches that
  // of the content file.
  // We should actually parse all of them.  Don't look for content at the book-level
  // XML but do provide up the book level information - it will get collected in the
  // database wth its corresponding articles
  private static final String bookNode = "/book";

  // set parent of book meta
  private static final String book_meta = "book-meta/";
  // set children
  public static final String book_doi = book_meta + "book-id[@book-id-type = 'doi']";
  public static final String book_title = book_meta + "book-title-group/book-title";
  public static final String book_publisher = book_meta + "publisher/publisher-name";
  public static final String book_pub_date = book_meta + "pub-date";
  public static final String book_isbn = book_meta + "isbn[@publication-format = 'print']";
  public static final String book_eisbn = book_meta + "isbn[@publication-format = 'electronic']";
  public static final String book_editor = book_meta + "contrib-group/contrib[@contrib-type = 'editor']/name";
  public static final String book_page_count = book_meta + "counts/book-page-count/@count";

  // set parent of chapter meta
  public static final String chapter_meta = "book-body/book-part[@book-part-type = 'chapter']/book-part-meta/";
  // set children
  public static final String chapter_doi = chapter_meta + "book-part-id[@book-part-id-type = 'doi']";
  public static final String chapter_title = chapter_meta + "title-group/title";
  public static final String chapter_author = chapter_meta + "contrib-group/contrib[@contrib-type = 'author']/name";
  public static final String chapter_copyright_year = chapter_meta + "permissions/copyright-year";
  public static final String chapter_pdf = chapter_meta + "alternate-form[@alternate-form-type = 'pdf']/@href";

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String, XmlDomMetadataExtractor.XPathValue> articleMap =
      new HashMap<String, XmlDomMetadataExtractor.XPathValue>();
  static {
    articleMap.put(book_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_eisbn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_pub_date, DATE_VALUE);
    articleMap.put(chapter_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(chapter_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(book_editor, AUTHOR_VALUE);
    articleMap.put(chapter_author, AUTHOR_VALUE);
    articleMap.put(chapter_copyright_year, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(chapter_pdf, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 3. in MARCXML there is no global information because one file/article */
  //static private final Map<String, XmlDomMetadataExtractor.XPathValue> Aty_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(book_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(book_eisbn, MetadataField.FIELD_EISBN);
    cookMap.put(book_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(book_publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(book_page_count, MetadataField.FIELD_END_PAGE);
    cookMap.put(chapter_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(chapter_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(book_editor, MetadataField.FIELD_AUTHOR);
    cookMap.put(chapter_doi, MetadataField.FIELD_DOI);
    cookMap.put(book_pub_date, MetadataField.FIELD_DATE);
  }


  /**
   * no global map - this is null
   * return NULL
   */
  @Override
  public Map<String, XmlDomMetadataExtractor.XPathValue> getGlobalMetaMap() { return null; }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
    return articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() { return bookNode; }

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
