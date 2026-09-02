package org.lockss.plugin.clockss.librairiedroz;

import java.util.ArrayList;
import java.util.List;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

/**
 * Metadata extractor for Librairie Droz MODS book deliveries.
 *
 * One <modsCollection> XML -> N book records -> N ArticleFiles. The EPUB that goes with each
 * record lives in a sibling zip:
 *   <dir>/droz_test_mods_20260824.xml
 *   <dir>/droz_test_epubs_20260824.zip!/<something>.epub
 *
 * OPEN QUESTION: nothing in the MODS record names the EPUB file. The record carries three
 * ISBNs (print / epub / pdf) and a DOI, and the filename is almost certainly built from one
 * of them — but until we can list the zip we are guessing. getFilenamesAssociatedWithRecord()
 * therefore tries every plausible stem and logs which one hit, so the first test crawl tells
 * us the answer. Once known, collapse the candidate list to the single real pattern.
 */
public class LibrairieDrozBooksXmlMetadataExtractorFactory
    extends SourceXmlMetadataExtractorFactory {

  private static final Logger log =
      Logger.getLogger(LibrairieDrozBooksXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper drozHelper = null;

  /*
   * Set true to emit metadata even when no matching EPUB is found in the zip. Useful right
   * now: with only two sample files, a wrong filename guess would otherwise silently produce
   * an AU with zero metadata, which looks identical to a broken schema helper. Flip to false
   * before the plugin goes to production, so books without content are not indexed.
   */
  private static final boolean EMIT_WITHOUT_CONTENT_FILE = true;

  /** Extensions to try, best first. PDF is included in case a later delivery adds it. */
  private static final String[] CONTENT_EXTENSIONS = { ".epub", ".pdf" };

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new LibrairieDrozBooksXmlMetadataExtractor();
  }

  public class LibrairieDrozBooksXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      if (drozHelper == null) {
        drozHelper = new LibrairieDrozBooksXmlSchemaHelper();
      }
      return drozHelper;
    }

    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper,
                                                            CachedUrl cu,
                                                            ArticleMetadata oneAM) {
      String zipUrl = deriveContentZipUrl(cu.getUrl());
      if (zipUrl == null) {
        log.warning("Droz: cannot derive content zip from XML url: " + cu.getUrl());
        return null;
      }

      List<String> candidates = new ArrayList<String>();
      for (String rawKey : LibrairieDrozBooksXmlSchemaHelper.filenameCandidateKeys()) {
        String stem = toFilenameStem(oneAM.getRaw(rawKey));
        if (stem == null) {
          continue;
        }
        for (String ext : CONTENT_EXTENSIONS) {
          String candidate = zipUrl + "!/" + stem + ext;
          if (!candidates.contains(candidate)) {
            candidates.add(candidate);
          }
        }
      }
      return candidates;
    }

    /**
     * Deterministic replacement for the base-class check: the first candidate that actually
     * has content wins and becomes the access URL. Written out explicitly rather than
     * inherited because "all files must exist" vs "any file may exist" differs across daemon
     * versions, and with a guessed filename that difference is the whole ballgame.
     */
    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper helper,
                                   CachedUrl cu,
                                   ArticleMetadata oneAM) {
      List<String> candidates = getFilenamesAssociatedWithRecord(helper, cu, oneAM);
      if (candidates != null) {
        for (String url : candidates) {
          CachedUrl fileCu = cu.getArchivalUnit().makeCachedUrl(url);
          try {
            if (fileCu != null && fileCu.hasContent()) {
              oneAM.put(MetadataField.FIELD_ACCESS_URL, url);
              log.debug3("Droz: matched content file " + url);
              return true;
            }
          } finally {
            AuUtil.safeRelease(fileCu);
          }
        }
      }

      log.warning("Droz: no content file found for record; tried "
          + (candidates == null ? "nothing" : candidates.toString()));

      if (EMIT_WITHOUT_CONTENT_FILE) {
        // Fall back to the XML itself so the record still has a valid in-AU access URL.
        oneAM.put(MetadataField.FIELD_ACCESS_URL, cu.getUrl());
        return true;
      }
      return false;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper helper,
                                   CachedUrl cu,
                                   ArticleMetadata oneAM) {
      // Everything in this feed is a monograph.
      oneAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);
      oneAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);

      // The <location><url> in the record points at openurl.droz.org, which is outside the
      // AU — never let it become the access URL. It is kept only as a raw value.

      // <relatedItem type="series"><titleInfo><title/> is empty in every sample record while
      // the volume number is populated, so the series name has to come from somewhere else.
      // If the publisher cannot fill it in, consider deriving the publication title from the
      // <classification> values, or hard-coding the series per AU.
    }
  }

  /**
   * droz_test_mods_20260824.xml -> droz_test_epubs_20260824.zip, in the same directory.
   * Tolerates the "_mods_" token appearing anywhere in the stem, and falls back to matching
   * any sibling *_epubs_*.zip naming convention the publisher settles on.
   */
  static String deriveContentZipUrl(String xmlUrl) {
    if (xmlUrl == null || !xmlUrl.toLowerCase().endsWith(".xml")) {
      return null;
    }
    String base = xmlUrl.substring(0, xmlUrl.length() - ".xml".length());
    if (base.contains("_mods_")) {
      return base.replace("_mods_", "_epubs_") + ".zip";
    }
    if (base.endsWith("_mods")) {
      return base.substring(0, base.length() - "_mods".length()) + "_epubs.zip";
    }
    // Unknown naming: assume the zip shares the XML's stem.
    return base + ".zip";
  }

  /**
   * Turn an identifier into a candidate filename stem.
   * ISBN "9782600365185" -> "9782600365185" (hyphens stripped, in case they appear later).
   * DOI  "10.47421/droz65184" -> "droz65184".
   */
  static String toFilenameStem(String rawValue) {
    if (rawValue == null) {
      return null;
    }
    String v = rawValue.trim();
    if (v.isEmpty()) {
      return null;
    }
    if (v.contains("/")) {
      v = v.substring(v.lastIndexOf('/') + 1);
    }
    v = v.replace("-", "");
    return v.isEmpty() ? null : v;
  }
}
