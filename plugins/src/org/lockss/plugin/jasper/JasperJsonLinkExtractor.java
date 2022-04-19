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

package org.lockss.plugin.jasper;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

/*
{
  "created": 1646775793,
  "d1": "ia904601.us.archive.org",
  "d2": "ia804601.us.archive.org",
  "dir": "/19/items/Fafnir_2342-2009",
  "files_count": 5,
  "item_last_updated": 1636066732,
  "item_size": 108678924,
  "server": "ia904601.us.archive.org",
  "uniq": 456880088,
  "workable_servers": [
    "ia904601.us.archive.org",
    "ia804601.us.archive.org"
  ],
  "metadata": {
    "identifier": "Fafnir_2342-2009",
    "collection": [
      "ProjectJasperTransfers",
      "ia_biblio_metadata",
      "theinternetarchive"
    ],
    "creator": "vault_uploader",
    "noindex": "True",
    "scanner": "Internet Archive Python library 2.1.0",
    "title": "Fafnir 2342-2009",
    "mediatype": "data",
    "uploader": "vault_uploader@archive.org",
    "publicdate": "2021-11-04 22:58:30",
    "access-restricted-item": "true"
  }
  "files": [
    {
      "name": "23422009-2021-11-03-12-16-05.tar.gz",
      "source": "original",
      "mtime": "1636066696",
      "size": "1140513",
      "md5": "cb74b8bb30eb27b18d0e5849a6a307be",
      "crc32": "fdb516c6",
      "sha1": "2f81cf87f90bf9b1b0c22ba759bff12a66c1b1fc",
      "format": "GZIP",
      "private": "true"
    },
    {
      "name": "23422009-2021-11-03-12-16-13.tar.gz",
      "source": "original",
      "mtime": "1636066709",
      "size": "25344772",
      "md5": "8599ddafc9bdfe9afa917c169baac970",
      "crc32": "1971d3a8",
      "sha1": "4e4d52971243332e4c8cf16c6aba52af4ffb7570",
      "format": "GZIP",
      "private": "true"
    },
    {
      "name": "Fafnir_2342-2009_files.xml",
      "source": "original",
      "format": "Metadata",
      "md5": "cbb5b037530ac56463bc813d98f940e7",
      "summation": "md5"
    },
    {
      "name": "Fafnir_2342-2009_meta.sqlite",
      "source": "original",
      "mtime": "1636066732",
      "size": "28672",
      "md5": "d17b3072ffbeab21dd7b3ae8a1d0c656",
      "crc32": "eebe4528",
      "sha1": "10dcd98b533a3a6415168d58ca8fee05c2b4c287",
      "format": "Metadata"
    },
    {
      "name": "Fafnir_2342-2009_meta.xml",
      "source": "original",
      "mtime": "1636066710",
      "size": "586",
      "md5": "34f773b74049588925d85c1a29ac5921",
      "crc32": "47003173",
      "sha1": "0b3b57c5241eed157e2285be14eb8c15f78a5141",
      "format": "Metadata"
    }
  ]
}

 */
public class JasperJsonLinkExtractor {

  public static final Logger log = Logger.getLogger(JasperJsonLinkExtractor.class);
  // https://archive.org/download/Fafnir_2342-2009/23422009-2021-11-03-12-16-13.tar.gz
  public static String DOWNLOAD_URL = "https://archive.org/download/";
  public static String collection;
  public boolean done;

  /**
   * <p>
   * Builds a link extractor for ArchiveIt's WASAPI-based API response (JSON).
   * </p>
   *
   * @since 1.67.5 #FIXME ?
   */
  public JasperJsonLinkExtractor() { this.done = false; }

  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException, PluginException {
    collection = au.getConfiguration().get(ConfigParamDescr.COLLECTION.getKey());

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
                + collection
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
