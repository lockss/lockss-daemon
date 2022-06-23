package org.lockss.plugin.cloudpublish.pap;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.onixbooks.Onix3LongSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

public class PapOnixSchemaHelper extends Onix3LongSchemaHelper {

  static Logger log = Logger.getLogger(PapOnixSchemaHelper.class);

  /**
   * path to each metadata entry
   */
  static private final String Pap_article_node = "/ONIXData/ProductData/Product";

  /* Under an item node, the interesting bits live at these relative locations */
  static private final String Pap_recordId = "RecordReference";
  private static String ONIX_product_id =  "ProductIdentifier";
  static private final String Pap_Contributor = "Contributor";
  protected static String Pap_idtype_isbn13 =
      ONIX_product_id + "[ProductIDType = \"ISBN-13\"]";
  private static String Pap_idtype_gtin=
      ONIX_product_id + "[ProductIDType = \"GTIN-13\"]";
  private static String Pap_idtype_doi =
      ONIX_product_id + "[ProductIDType = \"DOI\"]";
  private static String Pap_series = "Series/TitleOfSeries";

  private static String Pap_title = "Title";
  private static String Pap_author = Pap_Contributor + "[ContributorRole = \"By (author)\"]";
  private static String Pap_editor = Pap_Contributor + "[ContributorRole = \"Edited by\"]";
  private static String Pap_pages = "NumberOfPages";
  private static String Pap_keywords = "Subject[SubjectSchemeIdentifier = \"Keywords\"]";
  private static String Pap_description = "OtherText[TextTypeCode = \"Main description\"]";
  private static String Pap_anotation = "OtherText[TextTypeCode = \"Short description/annotation\"]";

  private static final Map<String, XmlDomMetadataExtractor.XPathValue> PAP_MAP =
      new HashMap<String, XmlDomMetadataExtractor.XPathValue>();
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
  }

  public static final XmlDomMetadataExtractor.NodeValue GET_AND_TRIM_CHILD_NODE_VALUE(String childNodeName) {
    return new XmlDomMetadataExtractor.NodeValue() {
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
            keywords = infoNode.getTextContent().replaceAll("\\s{2,}", " ").trim();;
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
  public String getFilenameXPathKey() { return Pap_recordId; };
}
