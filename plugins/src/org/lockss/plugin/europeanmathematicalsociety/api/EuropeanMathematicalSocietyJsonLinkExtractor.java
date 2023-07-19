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

package org.lockss.plugin.europeanmathematicalsociety.api;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;


public class EuropeanMathematicalSocietyJsonLinkExtractor {

  public static final Logger log = Logger.getLogger(EuropeanMathematicalSocietyJsonLinkExtractor.class);
  // https://content.ems.press/books
  // https://content.ems.press/books/123
  // https://content.ems.press/serials
  // https://content.ems.press/serials filter[code]==jems // journal by code JEMS

  public static String DOWNLOAD_URL = "https://content.ems.press/";
  public static String item;
  public boolean done;

  /**
   * <p>
   * Builds a link extractor for ArchiveIt's WASAPI-based API response (JSON).
   * </p>
   *
   * @since 1.67.5 #FIXME ?
   */
  public EuropeanMathematicalSocietyJsonLinkExtractor() { this.done = false; }

  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException, PluginException {
    item = au.getConfiguration().get("item");;

    // Parse input
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = null;
    try (Reader reader = new InputStreamReader(in, encoding)) {
      rootNode = objectMapper.readTree(reader);
      if (log.isDebug3()) {
        log.debug3(String.format("Input: %s", rootNode));
      }
    }

    int count = rootNode.path("files_count").asInt(-1);
    int last_updated = rootNode.path("item_last_updated").asInt(-1);
    JsonNode filesNode = rootNode.path("files");
    String directory = rootNode.path("dir").asText(null);
    JsonNode domains =  rootNode.path("workable_servers"); // subdomains of archive.org, accessible via "d<n>" as well

    List<Map.Entry<String, Integer>> urlsAndTimes = new ArrayList<>();

    if (!filesNode.isMissingNode() && filesNode.isArray()) {
      Iterator<JsonNode> fileIter = filesNode.elements();
      for (int i = 1 ; fileIter.hasNext() ; ++i) {
        JsonNode file = fileIter.next();
        String filename =  file.path("name").asText(null); // "23422009-2021-11-03-12-16-05.tar.gz"
        String filetype =  file.path("format").asText(null); // GZIP, Metadata
        String md5 =  file.path("md5").asText(null); // checksum
        String sha1 =  file.path("sha1").asText(null); // checksum
        String crc32 =  file.path("crc32").asText(null); // checksum
        int size =  file.path("size").asInt(-1);
        Integer mtime =  file.path("mtime").asInt(-1); // 1636066696

        if (filename != null) {
          urlsAndTimes.add(
            new AbstractMap.SimpleEntry<>(
              DOWNLOAD_URL
                + item
                + "/"
                + filename,
              mtime
            )
          );
        }
      }

      // seems there is no pagination, once we have iterated over the files we are done
      done = true;

      if (urlsAndTimes.size() == 0) {
        // What to do?
        log.debug(String.format("No GZIPS founds: %s", srcUrl));
        return;
      }

      // add the urls to the call back
      for (Map.Entry<String, Integer> fileAndTime : urlsAndTimes) {
        cb.foundLink(fileAndTime);
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
