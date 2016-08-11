/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.associationforcomputingmachinery;

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
 *  ACM source files that use the <whole_books> schema
 *  
   *  ACM specific node evaluators to extract the information we want
   *  It looks like this 
   *  <whole_books>
   *     <book_rec>
   *        isbn, title, authors, date
   *        <book_content>
   *            indiv chapter doi and pdf name
   *        </book_content>
   *        ....
   *        filename to full text pdf of book
   *  
   *  so we treat the information in <book_rec> as global information and use
   *  it for each chapter
   *  but then we need to also create an article_node for the whole book...       
   
 */
public class ACMBooksXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(ACMBooksXmlSchemaHelper.class);

  private static final String NAME_SEPARATOR = ", ";
  
  
  /*
   * AUTHOR information example
   * NODE = <au>
   * <whole_books>
   *   <book_rec>
   *     <authors>
            * <au>
   * <person_id=PERSONID
   * <first_name>=John
   * <middle_name>=A.
   * <last_name>=AuthorName
   * <suffix>
   * <affiliation=Writer
   * <role>=Author
   *            </au>....
   * one for each and we'll handle each one at a time
   */
  static private final NodeValue ACM_AUTHOR_VALUE = new NodeValue() {   
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of ACM contributor");
      String fname = null;
      String mname = null;
      String lname = null;
      boolean isAuthor = false;
      /* first parse out the information */
      NodeList childNodes = node.getChildNodes(); 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        String nodeName = infoNode.getNodeName();
        if ("first_name".equalsIgnoreCase(nodeName)) {
          fname = infoNode.getTextContent();
        } else if ("middle_name".equalsIgnoreCase(nodeName)){
          mname = infoNode.getTextContent();
        } else if("last_name".equalsIgnoreCase(nodeName)){
          lname = infoNode.getTextContent();
        } else if ("role".equalsIgnoreCase(nodeName)) {
          String type = infoNode.getTextContent();
          if ("Author".equalsIgnoreCase(type)) {
            isAuthor = true;
          } else
            continue;
        } else 
          continue;
      }
      /* now build up our result */
      StringBuilder names = new StringBuilder();
      if (isAuthor) {
        if (!(lname == null || lname.isEmpty())) {
          names.append(lname);
        }
        if (!(fname == null || fname.isEmpty())) {
          names.append(NAME_SEPARATOR);
          names.append(fname);
        }
        // only do middle name if have first name
        if(!(mname == null || mname.isEmpty())){
            names.append(' ');
            names.append(mname);
        }
      } 
      if (names.length() == 0) {
        log.debug3("no valid contributor found");
        return null;
      }
      return names.toString();
    }
  };


  
  private static final String ACM_booknode = "/whole_books/book_rec";
  private static final String ACM_chapternode = "/whole_books/book_rec/book_content/toc_L1";

  /* global - relative to book_rec */
  private static final String ACM_isbn = "isbn13";
  private static final String ACM_booktitle = "book_title";
  private static final String ACM_booksubtitle = "book_subtitle";
  private static final String ACM_bookauthor =  "authors/au";
  private static final String ACM_bookcopyright =  "copyright_year";
  private static final String ACM_bookpubdate =  "publication_date";
  
  /* relative to chapter */
  private static final String ACM_chapterdoi = "doi";
  private static final String ACM_chaptertitle = "title";
  private static final String ACM_chapterpage = "page_from";

  /* filename (relative to both global and to chapter node) */
  private static final String ACM_pdf_url = "fulltext/file/fname";

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  ACM_chapterMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    ACM_chapterMap.put(ACM_chapterdoi, XmlDomMetadataExtractor.TEXT_VALUE);    
    ACM_chapterMap.put(ACM_pdf_url, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_chapterMap.put(ACM_chaptertitle, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_chapterMap.put(ACM_chapterpage, XmlDomMetadataExtractor.TEXT_VALUE); 
  }

  /* 3. in ACM, there some global information we gather */ 
  static private final Map<String,XPathValue>     
  ACM_bookMap = new HashMap<String,XPathValue>();
  static {
    ACM_bookMap.put(ACM_booknode + "/" + ACM_isbn, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_bookMap.put(ACM_booknode + "/" + ACM_booktitle, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_bookMap.put(ACM_booknode + "/" + ACM_booksubtitle, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_bookMap.put(ACM_booknode + "/" + ACM_bookcopyright, XmlDomMetadataExtractor.TEXT_VALUE); 
    ACM_bookMap.put(ACM_booknode + "/" + ACM_bookauthor, ACM_AUTHOR_VALUE);
  }

  /*
   * The emitter will need a map to know how to cook raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // book global
    cookMap.put(ACM_booknode + "/" + ACM_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(ACM_booknode + "/" + ACM_bookauthor, MetadataField.FIELD_AUTHOR);
    cookMap.put(ACM_booknode + "/" + ACM_booktitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(ACM_booknode + "/" + ACM_bookcopyright, MetadataField.FIELD_DATE);
    // chapter level only
    cookMap.put(ACM_chapterdoi, MetadataField.FIELD_DOI);    
    cookMap.put(ACM_chaptertitle, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(ACM_chapterpage, MetadataField.FIELD_START_PAGE);
    // both book and chapter
    cookMap.put(ACM_pdf_url, MetadataField.FIELD_ACCESS_URL);
  }
  

  /**
   * ACM does have some global info (journal || issue)
   * putting front_matter and backmatter in globalMap
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return ACM_bookMap;
  }

  /**
   * return ACM article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return ACM_chapterMap; 
  }

  /**
   * Return the article node path
   * In this case we take BOTH a full book as a node and the underlying chapter nodes
   * This makes me a little nervous because the node evaluator removes the found node from its
   * list after evaluating it and the booknode is a parent of the other article nodes, but 
   * it seems to work...
   */
  @Override
  public String getArticleNode() {
    String bothNodes = ACM_booknode  + " | " + ACM_chapternode;
    return bothNodes;
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

    return ACM_pdf_url;
  }

}