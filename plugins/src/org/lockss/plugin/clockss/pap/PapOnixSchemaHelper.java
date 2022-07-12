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
package org.lockss.plugin.clockss.pap;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

public class PapOnixSchemaHelper extends Onix3BooksSchemaHelper {

  static Logger log = Logger.getLogger(PapOnixSchemaHelper.class);
  /**
   * Converts YYYYMMDD to YYYY-MM-DD.
   */
  static public final NodeValue PAP_DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String YYYYMMDD = node.getTextContent();
      if (YYYYMMDD.length()==8) {
        // make it W3C format instead of YYYYMMDD
        return YYYYMMDD.substring(0, 4) + //YYYY
            "-" +
            YYYYMMDD.substring(4, 6) + //MM
            "-" +
            YYYYMMDD.substring(6, 8);
      } else {
        return YYYYMMDD; //not sure what the format is, just return it as is
      }
    }
  };

  /**
   * path to each metadata entry
   */
  static private final String Pap_article_node = "//ProductData/Product";

  /* Under an item node, the interesting bits live at these relative locations */
  static private final String Pap_recordId = "RecordReference";
  private static final String ONIX_product_id =  "ProductIdentifier";
  static private final String Pap_Contributor = "Contributor";
  protected static String Pap_idtype_isbn13 =
      ONIX_product_id + "[ProductIDType = \"ISBN-13\"]";
  private static final String Pap_idtype_gtin=
      ONIX_product_id + "[ProductIDType = \"GTIN-13\"]";
  private static final String Pap_idtype_doi =
      ONIX_product_id + "[ProductIDType = \"DOI\"]";
  private static final String Pap_series = "Series/TitleOfSeries";

  private static final String Pap_title = "Title";
  private static final String Pap_author = Pap_Contributor + "[ContributorRole = \"By (author)\"]";
  private static final String Pap_editor = Pap_Contributor + "[ContributorRole = \"Edited by\"]";
  private static final String Pap_pages = "NumberOfPages";
  private static final String Pap_keywords = "Subject[SubjectSchemeIdentifier = \"Keywords\"]";
  private static final String Pap_description = "OtherText[TextTypeCode = \"Main description\"]";
  private static final String Pap_anotation = "OtherText[TextTypeCode = \"Short description/annotation\"]";

  private static final String Pap_pubdate = "PublicationDate";

  private static final Map<String, XmlDomMetadataExtractor.XPathValue> PAP_MAP =
      new HashMap<>();
  static {
    PAP_MAP.put(Pap_recordId, XmlDomMetadataExtractor.TEXT_VALUE);
    PAP_MAP.put(Pap_idtype_isbn13, ONIX_ID_VALUE);
    PAP_MAP.put(Pap_idtype_gtin, ONIX_ID_VALUE);
    PAP_MAP.put(Pap_idtype_doi, ONIX_ID_VALUE);
    PAP_MAP.put(Pap_series,  XmlDomMetadataExtractor.TEXT_VALUE);
    PAP_MAP.put(Pap_title, ONIX_TITLE_VALUE);
    PAP_MAP.put(Pap_author, ONIX_AUTHOR_VALUE);
    PAP_MAP.put(Pap_editor, ONIX_AUTHOR_VALUE);
    PAP_MAP.put(Pap_pages, XmlDomMetadataExtractor.TEXT_VALUE);
    PAP_MAP.put(Pap_keywords, GET_AND_TRIM_CHILD_NODE_VALUE("SubjectHeadingText"));
    PAP_MAP.put(Pap_description, GET_AND_TRIM_CHILD_NODE_VALUE("Text"));
    PAP_MAP.put(Pap_anotation, GET_AND_TRIM_CHILD_NODE_VALUE("Text"));
    PAP_MAP.put(Pap_pubdate, PAP_DATE_VALUE);
  }

  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(Pap_idtype_isbn13, MetadataField.FIELD_ISBN);
    cookMap.put(Pap_idtype_doi, MetadataField.FIELD_DOI);
    cookMap.put(Pap_series,  MetadataField.FIELD_SERIES_TITLE);
    cookMap.put(Pap_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(Pap_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(Pap_pages, MetadataField.FIELD_END_PAGE);
    cookMap.put(Pap_keywords, MetadataField.FIELD_KEYWORDS);
    cookMap.put(Pap_pubdate, MetadataField.FIELD_DATE);
  }

  /**
   * Gets the specified child nodes text, and trims and deduplicates the white space.
   * @param childNodeName the name of the xml node.
   * @return the text of the child node, trimmed and deduplicated of white space.
   */
  public static NodeValue GET_AND_TRIM_CHILD_NODE_VALUE(String childNodeName) {
    return new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }
        log.debug3("getValue of PAP " + childNodeName);
        // the TYPE has already been captured by xpath search in raw key
        String keywords = null;
        NodeList childNodes = node.getChildNodes();
        for (int m = 0; m < childNodes.getLength(); m++) {
          Node infoNode = childNodes.item(m);
          if (infoNode.getNodeName().equals(childNodeName)) {
            keywords = infoNode.getTextContent().replaceAll("\\s{2,}", " ").trim();
            break;
          }
        }
        if (keywords != null)  {
          return keywords;
        } else {
          log.debug3("no " + childNodeName + " in " + node.getNodeName());
          return null;
        }
      }
    };
  }

  /**
   * return ONIX3 article paths representing metadata of interest
   */
  @Override
  public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() { return PAP_MAP; }

  @Override
  public MultiValueMap getCookMap() { return cookMap; }



  @Override
  public String getArticleNode() {
    return Pap_article_node;
  }

  /**
   * Dont need deduplication right now.
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  @Override
  public String getFilenameXPathKey() { return Pap_recordId; }
}
