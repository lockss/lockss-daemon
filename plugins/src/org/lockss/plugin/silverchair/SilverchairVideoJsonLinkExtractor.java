/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.silverchair;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

/*
{
  "Video 4": {
    "source_href": "http://static-movie-usa.glencoesoftware.com/source/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V4.mp4",
    "doi": "10.1083/jcb.202111095.v4",
    "flv_href": "http://static-movie-usa.glencoesoftware.com/flv/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V4.flv",
    "uuid": "2366b129-3868-4590-a9c6-b6539999dc76",
    "title": "Video 4.",
    "video_id": "video-4",
    "solo_href": "http://movie.rupress.org/video/10.1083/jcb.202111095/video-4",
    "height": 718,
    "ogv_href": "http://static-movie-usa.glencoesoftware.com/ogv/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V4.ogv",
    "width": 1138,
    "legend": "<div class=\"caption\"><p><b>Structure of </b><i><b>S. cerevisiae</b></i><b> Fmp27 generated with RoseTTAFold and ChimeraX: β-sheets (blue), α-helices (red), and coil (green).</b> The movie shows rotation around the longitudinal axis. Frame rate = 5 frames per second.</p></div>",
    "href": "JCB_202111095_V4.mp4",
    "webm_href": "http://static-movie-usa.glencoesoftware.com/webm/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V4.webm",
    "jpg_href": "http://static-movie-usa.glencoesoftware.com/jpg/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V4.jpg",
    "duration": 7.2,
    "mp4_href": "http://static-movie-usa.glencoesoftware.com/mp4/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V4.mp4",
    "id": "video4",
    "size": 4644291
  },
  "Video 5": {
    "source_href": "http://static-movie-usa.glencoesoftware.com/source/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V5.mp4",
    "doi": "10.1083/jcb.202111095.v5",
    "flv_href": "http://static-movie-usa.glencoesoftware.com/flv/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V5.flv",
    "uuid": "47839814-f1ab-4f4b-9393-243ea5d92912",
    "title": "Video 5.",
    "video_id": "video-5",
    "solo_href": "http://movie.rupress.org/video/10.1083/jcb.202111095/video-5",
    "height": 718,
    "ogv_href": "http://static-movie-usa.glencoesoftware.com/ogv/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V5.ogv",
    "width": 1138,
    "legend": "<div class=\"caption\"><p><b>Structure of </b><i><b>S. cerevisiae</b></i><b> Hob2 generated with RoseTTAFold and ChimeraX: β-sheets (blue), α-helices (red), and coil (green).</b> The movie shows rotation around the longitudinal axis. Frame rate = 5 frames per second.</p></div>",
    "href": "JCB_202111095_V5.mp4",
    "webm_href": "http://static-movie-usa.glencoesoftware.com/webm/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V5.webm",
    "jpg_href": "http://static-movie-usa.glencoesoftware.com/jpg/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V5.jpg",
    "duration": 7.2,
    "mp4_href": "http://static-movie-usa.glencoesoftware.com/mp4/10.1083/157/6573459fd8d5825e210c60409e3c3decaf0e8378/JCB_202111095_V5.mp4",
    "id": "video5",
    "size": 4221638
  }
}

 */
public class SilverchairVideoJsonLinkExtractor implements LinkExtractor {


  public static final Logger log = Logger.getLogger(SilverchairVideoJsonLinkExtractor.class);

  public SilverchairVideoJsonLinkExtractor() {
    log.debug3("SilverchairVideoJsonLinkExtractor is called");
  }


  @Override
  public void extractUrls(ArchivalUnit au, InputStream in, String encoding, String srcUrl, Callback cb) throws IOException, PluginException {

    log.debug3("Parsing " + srcUrl);

    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }

    // Parse input
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = null;
    try (Reader reader = new InputStreamReader(in, encoding)) {
      rootNode = objectMapper.readTree(reader);
      if (log.isDebug3()) {
        log.debug3(String.format("Input: %s", rootNode));
      }
    }

    Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();

    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();

      String videoName = entry.getKey();
      JsonNode videoNode = entry.getValue();

      String sourceHref = videoNode.get("source_href").asText();
      String mp4Href = videoNode.get("mp4_href").asText();
      String flvHref = videoNode.get("flv_href").asText();
      String ogvHref = videoNode.get("ogv_href").asText();
      String webmHref = videoNode.get("webm_href").asText();


      log.debug3("Video Name: " + videoName + ", sourceHref: " + sourceHref
              + ", mp4Href: " + mp4Href
              + ", flvHref: " + flvHref
              + ", ogvHref: " + ogvHref
              + ", webmHref: " + webmHref


      );

      if (sourceHref != null && !sourceHref.isEmpty()) {
        cb.foundLink(sourceHref);

      }

      if (mp4Href != null && !mp4Href.isEmpty()) {
        cb.foundLink(mp4Href);
      }

      if (flvHref != null && !flvHref.isEmpty()) {
        cb.foundLink(flvHref);
      }

      if (ogvHref != null && !ogvHref.isEmpty()) {
        cb.foundLink(ogvHref);
      }

      if (webmHref != null && !webmHref.isEmpty()) {
        cb.foundLink(webmHref);
      }

    }
  }
}
