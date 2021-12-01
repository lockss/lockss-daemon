package org.lockss.plugin.archiveit;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CuIterable;
import org.lockss.util.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.*;

public class ArchiveItApiJsonLinkExtractor implements LinkExtractor {

  public static final Logger log = Logger.getLogger(ArchiveItApiJsonLinkExtractor.class);

  public static final String WEBDATAFILE_URL = "https://warcs.archive-it.org/webdatafile/";

  private boolean done;

  /**
   * <p>
   * Builds a link extractor for ArchiveIt's WASAPI-based API response (JSON).
   * </p>
   *
   * @since 1.67.5 #FIXME ?
   */
  public ArchiveItApiJsonLinkExtractor() {
    this.done = false;
  }

  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException, PluginException {

    // Parse input
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = null;
    try (Reader reader = new InputStreamReader(in, encoding)) {
      rootNode = objectMapper.readTree(reader);
      if (log.isDebug3()) {
        log.debug3(String.format("Input: %s", rootNode));
      }
    }
    int count = rootNode.path("count").asInt(-1);
    String next = rootNode.path("next").asText(null);
    String previous = rootNode.path("previous").asText(null);
    JsonNode filesNode = rootNode.path("files");

    List<String> warcUrls = new ArrayList<String>();
    CachedUrlSet cachedUrls = au.getAuCachedUrlSet();
    CuIterable cachedUrlIter = cachedUrls.getCuIterable();

    String apiUrl;

    if (!filesNode.isMissingNode() && filesNode.isArray()) {
      Iterator<JsonNode> fileIter = filesNode.elements();
      for (int i = 1 ; fileIter.hasNext() ; ++i) {
        JsonNode file = fileIter.next();

        String filename =  file.path("filename").asText(null);
        String filetype =  file.path("filetype").asText(null);
        JsonNode checksums =  file.path("checksums"); // list of sha1 & md5
        int account =  file.path("account").asInt(-1);
        int size =  file.path("size").asInt(-1);
        int collection =  file.path("collection").asInt(-1);
        int crawl =  file.path("crawl").asInt(-1);
        String crawlTime =  file.path("crawl-time").asText(null); // 2019-05-17T18:05:22.502000Z
        String crawlStart =  file.path("crawl-start").asText(null);
        String storeTime =  file.path("store-time").asText(null);
        JsonNode locationsNode =  file.path("locations"); // url to warcs.archive-it.org & archive.org

        if (!locationsNode.isMissingNode() && locationsNode.isArray()) {
          Iterator<JsonNode> locationIter = locationsNode.elements();
          for (int j = 1 ; locationIter.hasNext() ; ++j) {
            String location = locationIter.next().asText(null);
            if (location.contains(WEBDATAFILE_URL)) {
              boolean storeIt = true;
              // lets check if this url is already stored
              if (cachedUrls.containsUrl(location)) {
                CachedUrl cu = au.makeCachedUrl(location);
                if (cu.hasContent()) {
                  // lets check if the WASAPI content has been updated since the last time this url was collected
                  LocalDateTime archiveItCrawlTime = LocalDateTime.parse(crawlTime, DateTimeFormatter.ISO_DATE_TIME);
                  String dateStr = cu.getProperties().get("date").toString(); // Thu, 26 Aug 2021 18:21:55 GMT
                  LocalDateTime cachedUrlTime = LocalDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME);
                  if (archiveItCrawlTime.isBefore(cachedUrlTime)) {
                    // the content on WASAPI is older than the stored content, no need to refetch
                    log.info("Archive It file is older than stored content. Skipping.");
                    storeIt = false;
                  }
                }
              }
              if (storeIt) {
                warcUrls.add(location);
              }
            }
          }
        }

      }

      if (next == null || next.isEmpty() || next.equals("null")) {
        done = true;
      }

      if (warcUrls.size() == 0) {
        // What to do?
        log.debug(String.format("No WARCs founds: %s", srcUrl));
        return;
      }

      // add the urls to the call back
      for (String warcFile : warcUrls) {
        cb.foundLink(warcFile);
      }
    }

  }

  /**
   * <p>
   * Determines if this link extractor is done processing records for the
   * current query. If next is null, then we are done.
   * </p>
   *
   * @return Whether this link extractor is done processing records for the
   *         current query.
   * @since 1.67.5
   */
  public boolean isDone() {
    return done;
  }

}
