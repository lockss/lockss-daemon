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

package org.lockss.plugin.springer;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  the Springer Proprietary tagset for a book
 *  with the filenames based on same name as the .xml file
 *  There is only one record for each book
 *  
 * BOOK xml files are tricky. The depth for various items is variable.
 * The available levels are:
 *   Series ->SubSeries Book -> Part -> Chapter
 *  but a chapter might be directly off a book, not part of series
 *   Book -> Chapter
 *  and other variations.  Each subsection must have a FooInfo section
 *  Another complication is that FooHeader sections can have 
 *  and of AuthorGroup, EditorGroup, CollaboaratorGroup or multiple
 *  pick any group up in raw, and choose in postcook processing
 *
 * Use a very generalized xpath and look for certain items regardless of their level
 * If available, we want
 *   Series: SeriesID, SeriesPrintISSN, SeriesElectronicISSN, SeriesTitle
 *   Book: BookPrintISBN, BookElectronicISBN, BookTitle, BookDOI,  *  
 *  @author alexohlson
 */
public class SpringerBookSourceSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(SpringerBookSourceSchemaHelper.class);


  /** NodeValue for creating value of subfields from AuthorName tag */
  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      
      NodeList nameNodes = node.getChildNodes();
      String givenName = "", familyName = "";
      for (int k = 0; k < nameNodes.getLength(); k++) {
        Node nameNode = nameNodes.item(k);
        if (nameNode.getNodeName().equals("GivenName")) {
          givenName += nameNode.getTextContent();
        } else if (nameNode.getNodeName().equals("FamilyName")) {
          familyName += nameNode.getTextContent();
        }
      }
      return familyName + ", " + givenName;
    }
  };
          
  
  private static String topNode = "/Publisher";
  
  // the top Node will be our start
  // one book chapter per file
  // the rest of these are relative to the top node
  
  //book-in-series-with-conference
  
  private static String seriesID = "Series/SeriesInfo/SeriesID";
  private static String seriesPISSN = "Series/SeriesInfo/SeriesPrintISSN";
  private static String seriesEISSN = "Series/SeriesInfo/SeriesElectronicISSN";
  private static String seriesTitle = "Series/SeriesInfo/SeriesTitle";

  
  private static String bookPISBN = "//Book/BookInfo/BookPrintISBN";
  private static String bookEISBN = "//Book/BookInfo/BookElectronicISBN";
  private static String bookTitle = "//Book/BookInfo/BookTitle";
  public static String bookSubTitle = "//Book/BookInfo/BookSubTitle";
  private static String bookDOI  = "//Book/BookInfo/BookDOI";
  private static String bookPubDate = "//Book/BookInfo/BookCopyright/CopyrightYear";
  public static String bookAuthorEd = "//Book/BookHeader/EditorGroup/Editor/EditorName";
  public static String bookAuthorAu = "//Book/BookHeader/AuthorGroup/Author/AuthorName";
  public static String bookAuthorCo = "//Book/BookHeader/CollaboratorGroup/Collaborator/CollaboratorName";
  private static String chapterStartPage = "//Chapter/ChapterInfoChapterFirstPage";
  private static String chapterEndPage = "//Chapter/ChapterInfo/ChapterLastPage";
  private static String chapterTitle = "//ChapterInfo/ChapterTitle";
  public static String chapterSubTitle = "//ChapterInfo/ChapterSubTitle";
  public static String chapterAuthorAu = "//Chapter/ChapterHeader/AuthorGroup/Author/AuthorName";
  public static String chapterAuthorEd = "//Chapter/ChapterHeader/EditorGroup/Editor/EditorName";
  

  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */
 
  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> nodeMap = 
      new HashMap<String,XPathValue>();
  static {
    // the first three may or may not exist
    nodeMap.put(seriesPISSN, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(seriesEISSN, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(seriesTitle, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(bookTitle, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(bookSubTitle, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(bookPISBN, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(bookEISBN, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(bookPubDate, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(bookDOI, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(bookAuthorAu, AUTHOR_VALUE);
    nodeMap.put(bookAuthorEd, AUTHOR_VALUE);
    nodeMap.put(bookAuthorCo, AUTHOR_VALUE);
    nodeMap.put(bookDOI, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(chapterTitle, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(chapterSubTitle, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(chapterStartPage, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(chapterEndPage, XmlDomMetadataExtractor.TEXT_VALUE);
    nodeMap.put(chapterAuthorAu, AUTHOR_VALUE);
    nodeMap.put(chapterAuthorEd, AUTHOR_VALUE);
  }

  /** Map of raw xpath key to cooked MetadataField */
  static private final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    //xpathMap.put("/Publisher/PublisherInfo/PublisherName", MetadataField.FIELD_PUBLISHER);
    cookMap.put(seriesPISSN, MetadataField.FIELD_ISSN);
    cookMap.put(seriesEISSN, MetadataField.FIELD_EISSN);
    cookMap.put(seriesTitle, MetadataField.FIELD_SERIES_TITLE);
    cookMap.put(bookPubDate, MetadataField.FIELD_DATE);
    cookMap.put(bookTitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(bookPISBN, MetadataField.FIELD_ISBN);
    cookMap.put(bookEISBN, MetadataField.FIELD_EISBN);
    cookMap.put(chapterTitle, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(bookDOI, MetadataField.FIELD_DOI);
    cookMap.put(chapterStartPage, MetadataField.FIELD_START_PAGE);
    cookMap.put(chapterEndPage, MetadataField.FIELD_END_PAGE);
    cookMap.put(chapterAuthorAu, MetadataField.FIELD_AUTHOR);
  }


  /* 2. Each item (article) has its own XML file */
  static private final String bookNode = topNode; 

  /* 3. in JATS there is no global information because one file/article */
  static private final Map<String,XPathValue> globalMap = null;


  /**
   * JATS does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return globalMap;
  }

  /**
   * return JATS article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return nodeMap;
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
