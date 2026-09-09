package org.lockss.plugin.clockss.librairiedroz;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * One mods_YYYYMMDD.xml is a <modsCollection> of N book records; each book's EPUB is a plain
 * file in the parallel epubs_YYYYMMDD/ directory, named after the book's EPUB ISBN:
 *
 *   <base_url>2026_01/mods_20260907.xml
 *   <base_url>2026_01/epubs_20260907/9782600316095.epub
 *
 * The mapping record -> file is therefore exact, not guessed: take the
 * <identifier type="isbn" dislayLabel="epub"> value and append ".epub". Confirmed against the
 * 2026-09-07 directory listing, where every filename is a 97826003xxxxx ISBN — the EPUB ISBN
 * series, not the print (97826000xxxxx) or PDF (97826001xxxxx) series.
 */
public class LibrairieDrozBooksXmlMetadataExtractorFactory
    extends SourceXmlMetadataExtractorFactory {

  private static final Logger log =
      Logger.getLogger(LibrairieDrozBooksXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper drozHelper = null;

  /*
   * Set true to emit metadata even when the EPUB is missing from the delivery. Leave true
   * while the publisher is still sending partial test drops (the 2026-09-07 epubs directory
   * holds 10 books, which will not line up with every record in every MODS file). Flip to
   * false for production so books with no preserved content are not indexed.
   */
  private static final boolean EMIT_WITHOUT_CONTENT_FILE = true;

  /** mods_20260907.xml -> epubs_20260907/ in the same delivery directory. */
  private static final Pattern MODS_FILENAME_PATTERN =
      Pattern.compile("^(.*/)mods_(\\d+)\\.xml$", Pattern.CASE_INSENSITIVE);

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
      String epubDir = deriveEpubDirUrl(cu.getUrl());
      if (epubDir == null) {
        log.warning("Droz: XML url does not match mods_<date>.xml: " + cu.getUrl());
        return null;
      }

      List<String> candidates = new ArrayList<String>();

      // The real mapping: EPUB ISBN + ".epub".
      addCandidate(candidates, epubDir,
          oneAM.getRaw(LibrairieDrozBooksXmlSchemaHelper.KEY_ISBN_EPUB));
      addCandidate(candidates, epubDir,
          oneAM.getRaw(LibrairieDrozBooksXmlSchemaHelper.KEY_ISBN_EPUB_FIXED));

      // Fallbacks, only for the case where a record is missing its EPUB ISBN. Cheap to try
      // and they cost nothing when the primary hits first.
      addCandidate(candidates, epubDir,
          oneAM.getRaw(LibrairieDrozBooksXmlSchemaHelper.KEY_ISBN_PRINT));
      addCandidate(candidates, epubDir,
          oneAM.getRaw(LibrairieDrozBooksXmlSchemaHelper.KEY_ISBN_PRINT_FIXED));

      return candidates;
    }

    /**
     * Deterministic replacement for the base-class check: the first candidate that actually
     * has content wins and becomes the access URL. Written out explicitly rather than
     * inherited because "all files must exist" vs "any file may exist" differs across daemon
     * versions.
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
              log.debug3("Droz: matched EPUB " + url);
              return true;
            }
          } finally {
            AuUtil.safeRelease(fileCu);
          }
        }
      }

      log.warning("Droz: no EPUB found for record; tried "
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
      // <classification> values, or setting the series per AU.
    }
  }

  private static void addCandidate(List<String> candidates, String epubDir, String isbn) {
    String stem = toFilenameStem(isbn);
    if (stem == null) {
      return;
    }
    String url = epubDir + stem + ".epub";
    if (!candidates.contains(url)) {
      candidates.add(url);
    }
  }

  /**
   * .../2026_01/mods_20260907.xml -> .../2026_01/epubs_20260907/
   * Returns null if the URL is not a recognisable MODS delivery file.
   */
  static String deriveEpubDirUrl(String xmlUrl) {
    if (xmlUrl == null) {
      return null;
    }
    Matcher m = MODS_FILENAME_PATTERN.matcher(xmlUrl);
    if (!m.matches()) {
      return null;
    }
    return m.group(1) + "epubs_" + m.group(2) + "/";
  }

  /** ISBN as delivered -> filename stem. Hyphens stripped in case they appear later. */
  static String toFilenameStem(String rawValue) {
    if (rawValue == null) {
      return null;
    }
    String v = rawValue.trim().replace("-", "").replace(" ", "");
    return v.isEmpty() ? null : v;
  }
}
