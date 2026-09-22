package org.lockss.plugin.silverchair;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Synthesizes the Glencoe video-metadata URL for a Silverchair (RUP) article,
 * but ONLY when the article page actually contains a video block.
 *
 *   https://movie-usa.glencoesoftware.com/metadata/10.1083/jcb.202109010
 *
 * Previously this fired on every <meta name="citation_doi">, which produced one
 * speculative fetch per article. The vast majority 404 ({"error": "Not found"})
 * and the host intermittently returns 502, which surfaces as a crawl error.
 *
 * Evidence used (verified against live rupress.org, Sept 2026):
 *   article WITH videos    -> div.video.video-section x2, div.video.video-modal x2,
 *                             [data-cadmoreid]="jcb_202109010_v1" ...
 *   article WITHOUT videos -> zero of the above
 *
 * Do NOT gate on <li data-content-filter="video"> -- that list item is emitted on
 * every article page and merely carries class="hide" when there is no video.
 */
public class SilverchairVideoHtmlLinkExtractor extends JsoupHtmlLinkExtractor {

  private static final Logger log =
      Logger.getLogger(SilverchairVideoHtmlLinkExtractor.class);

  private static final String VIDEO_METADATA_URL =
      "https://movie-usa.glencoesoftware.com/metadata/%s";

  /**
   * DOI prefixes Glencoe actually hosts video for: 10.1083 (JCB), 10.1084 (JEM),
   * 10.1085 (JGP). Deliberately excludes LSA (10.26508), whose metadata responses
   * are always the empty object {}.
   */
  private static final Pattern DOI_PAT =
      Pattern.compile("^10\\.108[345]/\\S+$");

  /**
   * Only the full-text article page carries the video markup. Skips
   * /article-abstract/, /article-pdf/, /article-standard/ and TOC pages so the
   * document is not re-parsed for nothing.
   */
  private static final Pattern ARTICLE_URL_PAT =
      Pattern.compile("/(article|article-split)/");

  /** Any one of these means the article really has video. */
  private static final String VIDEO_SELECTOR =
      "div.video.video-section, div.video.video-modal, [data-cadmoreid]";

  private static final String DOI_SELECTOR =
      "meta[name=citation_doi]";

  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException, PluginException {

    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }

    if (!ARTICLE_URL_PAT.matcher(srcUrl).find()) {
      super.extractUrls(au, in, encoding, srcUrl, cb);
      return;
    }

    // Buffer once so the document can be handed to super and then re-scanned.
    byte[] bytes = IOUtils.toByteArray(in);

    super.extractUrls(au, new ByteArrayInputStream(bytes), encoding, srcUrl, cb);

    try {
      emitVideoMetadataUrl(new ByteArrayInputStream(bytes), encoding, srcUrl, cb);
    } catch (IOException e) {
      // Never let the speculative video URL break normal link extraction.
      log.warning("Video metadata scan failed for " + srcUrl + ": " + e.getMessage());
    }
  }

  protected void emitVideoMetadataUrl(InputStream in,
                                      String encoding,
                                      String srcUrl,
                                      Callback cb) throws IOException {

    Document doc = Jsoup.parse(in, encoding, srcUrl);

    // selectFirst() short-circuits on the first match (jsoup >= 1.11.1; daemon
    // ships 1.22.2). On an older jsoup, use .select(..).first() instead.
    Element video = doc.selectFirst(VIDEO_SELECTOR);
    if (video == null) {
      log.debug3("No video block on " + srcUrl + " -- not requesting Glencoe metadata");
      return;
    }

    Element meta = doc.selectFirst(DOI_SELECTOR);
    if (meta == null) {
      log.debug2("Video block present but no citation_doi on " + srcUrl);
      return;
    }

    String doi = meta.attr("content").trim();
    if (!DOI_PAT.matcher(doi).matches()) {
      log.debug3("DOI " + doi + " is not a Glencoe-hosted prefix; skipping (" + srcUrl + ")");
      return;
    }

    String url = String.format(VIDEO_METADATA_URL, doi);
    log.debug2("Video block found on " + srcUrl + " -> " + url);
    cb.foundLink(url);
  }
}
