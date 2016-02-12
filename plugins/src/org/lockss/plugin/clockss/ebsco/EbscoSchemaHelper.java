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

package org.lockss.plugin.clockss.ebsco;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.w3c.dom.Node;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Ebsco deliveries
 *  @author alexohlson
 */
public class EbscoSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(EbscoSchemaHelper.class);

  /*                                                                                                                                                                                    
   * TITLE INFORMATION
   * we are sitting at <title></title>                                                                                                                                                                  
   * see if the <subtitle> sibling exists and has information                                                                                                                           
   */
  static private final NodeValue EBSCO_TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of Ebsco book title");
      String tTitle = node.getTextContent();
      String tSubtitle = null;

      //is there a subtitle sibling?                                                                                                                                                    
      Node titleNextSibling = node.getNextSibling();
      // between here and the <subtitle> tag if it exists                                                                                                                               
      while ((titleNextSibling != null) & (!"SubTitle".equals(titleNextSibling.getNodeName()))) {
        titleNextSibling = titleNextSibling.getNextSibling();
      }
      // we're either at subtitle or there wasn't one to check                                                                                                                          
      if (titleNextSibling != null) {
        tSubtitle = titleNextSibling.getTextContent();
      }

      // now build up the full title                                                                                                                                                    
      StringBuilder valbuilder = new StringBuilder();
      if (tTitle != null) {
        valbuilder.append(tTitle);
        if (tSubtitle != null) {
          if (tTitle.endsWith(":")) { // sometimes the title ends with the :                                                                                                            
            valbuilder.append(" " + tSubtitle);
          } else {
            valbuilder.append(": " + tSubtitle);
          }
        }
      } else {
        log.debug3("no title found within title group");
        return null;
      }
      log.debug3("title found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };
 
  /* 
   *  Ebsco uses a simple proprietary schema
   *  It contains multiple <eContent> within an <eContentList>
   *  Within the <eContent> all the information we need is in the <BibData>
   *  The matching content filename is the <ProductID>.(pdf|epub) in a parallel directory
   *  Also - these are books
   */


  //within the file, each book has its own set of data
  private static String ebsco_book_node = "/eContentList/eContent";
  // and these xpath are relative to the article node
  private static String ebsco_eisbn = "BibData/EISBN";
  private static String ebsco_pisbn = "BibData/PISBN";
  private static String ebsco_filename = "BibData/ProductID";
  private static String ebsco_booktitle = "BibData/Title";
  private static String ebsco_pubdate = "BibData/PublicationYear";
  private static String ebsco_publisher = "BibData/ImprintPublisher";
  private static String ebsco_contributorlist = "BibData/Contributor/Name";
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */
  
  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> EbscoMap = 
      new HashMap<String,XPathValue>();
  static {
    EbscoMap.put(ebsco_eisbn, XmlDomMetadataExtractor.TEXT_VALUE);
    EbscoMap.put(ebsco_pisbn, XmlDomMetadataExtractor.TEXT_VALUE);
    EbscoMap.put(ebsco_filename, XmlDomMetadataExtractor.TEXT_VALUE);
    EbscoMap.put(ebsco_pubdate, XmlDomMetadataExtractor.TEXT_VALUE);
    EbscoMap.put(ebsco_contributorlist, XmlDomMetadataExtractor.TEXT_VALUE);
    EbscoMap.put(ebsco_booktitle, EBSCO_TITLE_VALUE);
    EbscoMap.put(ebsco_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
  }
  
  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    // also get PROVIDER from the TDB file
    cookMap.put(ebsco_booktitle, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(ebsco_pisbn, MetadataField.FIELD_ISBN);
    cookMap.put(ebsco_eisbn, MetadataField.FIELD_EISBN);
    // do not use the metadata publisher - it's been bought by sage
    cookMap.put(ebsco_contributorlist, 
        new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(";")));
    cookMap.put(ebsco_pubdate, MetadataField.FIELD_DATE);
    // not sure about the publisher
    cookMap.put(ebsco_publisher, MetadataField.FIELD_PUBLISHER);
    
  }


  /**
   * BAY does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return null;
  }

  /**
   * return BAY article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return EbscoMap;
  }

  /**
   * Return the article node path
   * There is only one article per xml file so the top of the document is the
   * article and all paths are relative do document.
   */
  @Override
  public String getArticleNode() {
    return ebsco_book_node;
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
    return ebsco_filename;
  }

}
