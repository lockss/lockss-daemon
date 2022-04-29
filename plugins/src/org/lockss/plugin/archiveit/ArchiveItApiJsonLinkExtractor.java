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

package org.lockss.plugin.archiveit;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.*;

public class ArchiveItApiJsonLinkExtractor {

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

    List<Map.Entry<String, String>> warcUrlsAndTimes = new ArrayList<>();

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
              warcUrlsAndTimes.add(
                  new AbstractMap.SimpleEntry<>(location, crawlTime)
              );
            }
          }
        }

      }

      if (next == null || next.isEmpty() || next.equals("null")) {
        done = true;
      }

      if (warcUrlsAndTimes.size() == 0) {
        // What to do?
        log.debug(String.format("No WARCs founds: %s", srcUrl));
        return;
      }

      // add the urls to the call back
      for (Map.Entry<String, String> warcFileAndTime : warcUrlsAndTimes) {
        cb.foundLink(warcFileAndTime);
      }
    }

  }

  /**
   * Custom Callback to return a url and the last crawl time for that url as reported by WASAPI
   * Allows us to determine if we need to refetch the url or not.
   */
  public interface Callback {
    public void foundLink(Map.Entry urlAndTime);
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
