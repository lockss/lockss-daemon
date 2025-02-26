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

package org.lockss.plugin.clockss.eastview;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Eastview Information Services
 *  
 */
public class EastviewJournalMetadataHelper
implements SourceXmlSchemaHelper {
  
  private static final Logger log = Logger.getLogger(EastviewJournalMetadataHelper.class);
  static protected final String PAGE_PDF_PATH = "//ARTICLEDATA/TEXT/pagemark/@pagepdf";
  static protected final String PUBLICATION_TITLE_PATH = "//ARTICLEDATA/SOURCE";


  /*
   * <PAGE>3</PAGE>
   * or
   * <PAGE>3-6</PAGE>
   */
  private final static NodeValue STARTPAGE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("getValue of PAGE - interpret for start page");
      String pageVal = node.getTextContent();
      if (pageVal != null)  {
        return StringUtils.substringBefore(pageVal,"-");
      } else {
        log.debug3("no page value set");
        return null;
      }
    }
  };
  
  private final static NodeValue CLEAN_TEXT = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      log.debug3("Cleaning text value - strip lead/trail space");
      return StringUtils.strip(node.getTextContent());
    }
  };

  // For journals, we treat each page as a seperate article
  private static final String top = "//ARTICLEDATA";

  // The following are all relative to the article node
  // from the immediately preceeding sibling -
  private static String pub_title = "SOURCE";
  private static String src_title = "SRC";
  private static String pub_year = "DATE";
  private static String pub_volume = "VOLUME";
  private static String pub_issue = "NUMBER";
  private static String art_title = "ATITLE";
  protected static String ART_RAW_TITLE = "TITLE";
  private static String art_contrib = "AUTHOR";
  private static String art_sp = "PAGE";
  

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    articleMap.put(PUBLICATION_TITLE_PATH, CLEAN_TEXT);
    //This is raw title, it contains publisher acroname, like this:
    //title: 31-05-1961(DASZD-No.005) НОВЫЕ КНИГИ
    articleMap.put(ART_RAW_TITLE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(src_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_contrib, XmlDomMetadataExtractor.TEXT_VALUE); 
    articleMap.put(art_sp, STARTPAGE_VALUE);
    articleMap.put(PAGE_PDF_PATH, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2.  Top level per-article node */
  static private final String articleNode = top;

  /* 3. Global metadata is the publisher - work around if it gets troublesome */
  static private final Map<String, XPathValue> 
    globalMap = null; 
  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final String AUTHOR_SPLIT_CH = ",";
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(PUBLICATION_TITLE_PATH, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(art_contrib, 
        new MetadataField(MetadataField.FIELD_AUTHOR, MetadataField.splitAt(AUTHOR_SPLIT_CH)));
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
  }

  /**
   * publisher comes from a global node
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    //no globalMap, so returning null
    return globalMap; 
  }

  /**
   * return  article paths representing metadata of interest  
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

  /**
   */

  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * Return the path for product form so when multiple records for the same
   * item are combined, the product forms are combined together
   */

  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * using filenamePrefix (see above)
   */

  @Override
  public String getFilenameXPathKey() {
    return null;
  }

}