package org.lockss.plugin.clockss.librairiedroz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Schema helper for Librairie Droz MODS book deliveries.
 *
 * The delivered file is a <modsCollection> holding N <mods> records — the 2026-08-24 sample
 * has 3 books in one file — so one XML yields many ArticleFiles, not one.
 *
 * NAMESPACE NOTE: the file declares a default namespace (http://www.loc.gov/mods/v3).
 * LOCKSS's XPathXmlMetadataParser builds a namespace-unaware Document, so the plain paths
 * below match. If a test run shows zero records found, that assumption is wrong for your
 * daemon build — switch the paths to the local-name() form, e.g.
 *   "/ *[local-name()='modsCollection']/ *[local-name()='mods']"   (without the spaces)
 * and confirm before doing anything else.
 */
public class LibrairieDrozBooksXmlSchemaHelper implements SourceXmlSchemaHelper {

  private static final Logger log = Logger.getLogger(LibrairieDrozBooksXmlSchemaHelper.class);

  /*
   * PUBLISHER DATA BUGS in the sample — see the notes file. Two of them are load-bearing:
   *  1. the attribute is spelled "dislayLabel", not "displayLabel". Both spellings are
   *     matched below so the plugin survives the publisher fixing it.
   *  2. <identifier type="doi"> carries a full URL, not a bare DOI. Stripped in DOI_VALUE.
   */

  /** <name type="personal"> -> "Family, Given", authors only (marcrelator role "aut"). */
  static private final NodeValue MODS_AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      if (node == null) {
        return null;
      }
      String family = null;
      String given = null;
      String display = null;
      String roleCode = null;

      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        String childName = localName(child);
        if ("namePart".equals(childName)) {
          String type = attr(child, "type");
          String text = trim(child.getTextContent());
          if (text == null) {
            continue;
          }
          if ("family".equals(type)) {
            family = text;
          } else if ("given".equals(type)) {
            given = text;
          } else if (type == null || type.isEmpty()) {
            // corporate / undifferentiated name written as a single namePart
            display = text;
          }
        } else if ("role".equals(childName)) {
          NodeList roleKids = child.getChildNodes();
          for (int j = 0; j < roleKids.getLength(); j++) {
            Node roleKid = roleKids.item(j);
            if ("roleTerm".equals(localName(roleKid))) {
              String code = trim(roleKid.getTextContent());
              if (code != null) {
                roleCode = code;
              }
            }
          }
        }
      }

      // Only true authors go in FIELD_AUTHOR. Editors ("edt"), translators ("trl"),
      // contributors etc. are dropped rather than silently promoted to author.
      if (roleCode != null && !"aut".equalsIgnoreCase(roleCode)) {
        return null;
      }
      if (family != null && given != null) {
        return family + ", " + given;
      }
      if (family != null) {
        return family;
      }
      return display;
    }
  };

  /** <identifier type="doi">https://doi.org/10.47421/droz65184</identifier> -> bare DOI. */
  static private final NodeValue DOI_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String raw = trim(node == null ? null : node.getTextContent());
      if (raw == null) {
        return null;
      }
      String doi = raw.replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "");
      doi = doi.replaceFirst("(?i)^doi:\\s*", "");
      return doi.isEmpty() ? null : doi;
    }
  };

  /** Publisher appends a stray '>' to every <classification>. Strip it. */
  static private final NodeValue CLASSIFICATION_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String raw = trim(node == null ? null : node.getTextContent());
      if (raw == null) {
        return null;
      }
      String cleaned = raw.replaceAll("[>\\s]+$", "");
      return cleaned.isEmpty() ? null : cleaned;
    }
  };

  private static String localName(Node n) {
    if (n == null) {
      return null;
    }
    return n.getLocalName() != null ? n.getLocalName() : n.getNodeName();
  }

  private static String attr(Node n, String name) {
    if (n == null || n.getAttributes() == null) {
      return null;
    }
    Node a = n.getAttributes().getNamedItem(name);
    return a == null ? null : a.getNodeValue();
  }

  private static String trim(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  /* ---------------- raw XPaths, relative to one <mods> ---------------- */

  private static final String MODS_TITLE = "titleInfo/title";
  private static final String MODS_PUBLISHER = "originInfo/publisher";
  private static final String MODS_PUB_PLACE = "originInfo/place/placeTerm";
  private static final String MODS_DATE = "originInfo/dateIssued";
  private static final String MODS_COPYRIGHT_DATE = "originInfo/copyrightDate";
  private static final String MODS_LANGUAGE = "language/languageTerm";
  private static final String MODS_GENRE = "genre[@authority='local']";
  private static final String MODS_ABSTRACT_EN = "abstract[@lang='en']";
  private static final String MODS_CLASSIFICATION = "classification";
  private static final String MODS_SERIES_TITLE = "relatedItem[@type='series']/titleInfo/title";
  private static final String MODS_SERIES_VOLUME =
      "relatedItem[@type='series']/part/detail[@type='volume']/number";
  private static final String MODS_ACCESS_URL = "location/url";
  private static final String MODS_DOI = "identifier[@type='doi']";

  // The typo'd attribute as delivered, plus the correct spelling as a forward-compatible twin.
  private static final String MODS_ISBN_PRINT =
      "identifier[@type='isbn'][@dislayLabel='print']";
  private static final String MODS_ISBN_PRINT_FIXED =
      "identifier[@type='isbn'][@displayLabel='print']";
  private static final String MODS_ISBN_EPUB =
      "identifier[@type='isbn'][@dislayLabel='epub']";
  private static final String MODS_ISBN_EPUB_FIXED =
      "identifier[@type='isbn'][@displayLabel='epub']";
  private static final String MODS_ISBN_PDF =
      "identifier[@type='isbn'][@dislayLabel='pdf']";
  private static final String MODS_ISBN_PDF_FIXED =
      "identifier[@type='isbn'][@displayLabel='pdf']";

  // Exposed so the metadata extractor can read them back out of the raw map when it has to
  // guess the EPUB filename inside the delivery zip.
  public static final String KEY_ISBN_PRINT = MODS_ISBN_PRINT;
  public static final String KEY_ISBN_PRINT_FIXED = MODS_ISBN_PRINT_FIXED;
  public static final String KEY_ISBN_EPUB = MODS_ISBN_EPUB;
  public static final String KEY_ISBN_EPUB_FIXED = MODS_ISBN_EPUB_FIXED;
  public static final String KEY_ISBN_PDF = MODS_ISBN_PDF;
  public static final String KEY_ISBN_PDF_FIXED = MODS_ISBN_PDF_FIXED;
  public static final String KEY_DOI = MODS_DOI;

  private static final String MODS_AUTHOR = "name[@type='personal']";

  private static final Map<String, XPathValue> articleMap = new HashMap<String, XPathValue>();
  static {
    articleMap.put(MODS_TITLE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_PUBLISHER, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_PUB_PLACE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_DATE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_COPYRIGHT_DATE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_LANGUAGE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_GENRE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_ABSTRACT_EN, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_SERIES_TITLE, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_SERIES_VOLUME, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_ACCESS_URL, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_ISBN_PRINT, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_ISBN_PRINT_FIXED, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_ISBN_EPUB, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_ISBN_EPUB_FIXED, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_ISBN_PDF, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_ISBN_PDF_FIXED, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(MODS_DOI, DOI_VALUE);
    articleMap.put(MODS_CLASSIFICATION, CLASSIFICATION_VALUE);
    articleMap.put(MODS_AUTHOR, MODS_AUTHOR_VALUE);
  }

  /** One ArticleFiles per <mods> record. */
  private static final String MODS_ARTICLE_NODE = "/modsCollection/mods";

  /*
   * No global (per-file) metadata worth hoisting: publisher and place repeat inside every
   * record, so leave the global map empty rather than reading it once and assuming it holds.
   */
  private static final Map<String, XPathValue> globalMap = null;

  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(MODS_TITLE, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(MODS_TITLE, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(MODS_AUTHOR, MetadataField.FIELD_AUTHOR);
    cookMap.put(MODS_DATE, MetadataField.FIELD_DATE);
    cookMap.put(MODS_PUBLISHER, MetadataField.FIELD_PUBLISHER);
    cookMap.put(MODS_LANGUAGE, MetadataField.FIELD_LANGUAGE);
    cookMap.put(MODS_DOI, MetadataField.FIELD_DOI);
    // Only one spelling will ever be present; whichever it is wins.
    cookMap.put(MODS_ISBN_PRINT, MetadataField.FIELD_ISBN);
    cookMap.put(MODS_ISBN_PRINT_FIXED, MetadataField.FIELD_ISBN);
    cookMap.put(MODS_ISBN_EPUB, MetadataField.FIELD_EISBN);
    cookMap.put(MODS_ISBN_EPUB_FIXED, MetadataField.FIELD_EISBN);
    cookMap.put(MODS_SERIES_VOLUME, MetadataField.FIELD_VOLUME);
  }

  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return globalMap;
  }

  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return articleMap;
  }

  @Override
  public String getArticleNode() {
    return MODS_ARTICLE_NODE;
  }

  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /**
   * Deduplication key. The print ISBN is the only stable per-book identifier that is present
   * on every record in the sample; the DOI would work too. Returning null would fall back to
   * "no dedup", which is wrong here because a resend of the same collection is likely.
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return MODS_ISBN_PRINT;
  }

  /** Books are not grouped into an issue-level container in this feed. */
  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * Nothing in the record names the EPUB file, so there is no filename XPath to hand back.
   * The extractor derives the candidate filename from the ISBN/DOI instead —
   * see LibrairieDrozBooksXmlMetadataExtractorFactory.getFilenamesAssociatedWithRecord().
   */
  @Override
  public String getFilenameXPathKey() {
    return null;
  }

  /** Convenience for the extractor: every identifier we could plausibly build a filename from. */
  public static List<String> filenameCandidateKeys() {
    List<String> keys = new ArrayList<String>();
    keys.add(KEY_ISBN_EPUB);
    keys.add(KEY_ISBN_EPUB_FIXED);
    keys.add(KEY_ISBN_PRINT);
    keys.add(KEY_ISBN_PRINT_FIXED);
    keys.add(KEY_ISBN_PDF);
    keys.add(KEY_ISBN_PDF_FIXED);
    keys.add(KEY_DOI);
    return keys;
  }
}
