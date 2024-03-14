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


import com.fasterxml.jackson.core.JsonProcessingException;
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
http://movie-usa.glencoesoftware.com/metadata/10.1083/jcb.202111095

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

    // Need to handle JSON responses like these, video provider confirm these are valid from their perspective
    String jsonResponseEmpty = "{}";
    String jsonResponseError = "{\"error\": \"Not found\"}";

    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = null;
    Scanner scanner = new Scanner(in);

    try {
      scanner.useDelimiter("\\A");
      String inputString = scanner.hasNext() ? scanner.next() : "";

      log.debug3("srcUrl = " + srcUrl + ", encoding = " + encoding + ", input = " + inputString);

      rootNode = objectMapper.readTree(inputString);

      // Check if the JSON response matches the criteria
      if (rootNode.isObject() && !rootNode.fields().hasNext()) {
        log.debug3("JSON response is an empty JSON object, srcUrl = " + srcUrl);
      } else if (rootNode.has("error") && rootNode.get("error").isTextual() && rootNode.get("error").asText().equals("Not found")) {
        log.debug3("JSON response Contains error: Not found , srcUrl = " + srcUrl);
      } else {
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();

        while (fields.hasNext()) {
          Map.Entry<String, JsonNode> entry = fields.next();
          String videoName = entry.getKey();
          JsonNode videoNode = entry.getValue();


              /*
              Only use source_href, and ignore others as suggested by the video provider.

              """
              With respect to the FLVs, it's not an access issue; several years ago we stopped transcoding to FLV.
              For all the transcoded content there is no guarantee that all the transcoded assets are available.
              I would suggest sticking with just the assets referred to by the source_href as those are the original videos.
              Otherwise you are preserving several copies of the same thing in slightly different formats with different
              playback targets.

              A 404 not found from the metadata API means we know nothing about the DOI, we have no metadata for the article
              at all. Empty JSON means we know about the article and have metadata about it but it contains no videos
              that we're aware of. RUP is a special case for us as we have nearly all their article XML.
              """
               */

          String sourceHref = videoNode.get("source_href").asText();
          String mp4Href = videoNode.get("mp4_href").asText();
          String ogvHref = videoNode.get("ogv_href").asText();
          String webmHref = videoNode.get("webm_href").asText();

          log.debug3("Video Name: " + videoName + ", sourceHref: " + sourceHref
                  + ", mp4Href: " + mp4Href
                  + ", ogvHref: " + ogvHref
                  + ", webmHref: " + webmHref
          );

          if (sourceHref != null && !sourceHref.isEmpty()) {

            log.debug3("srcUrl = " + srcUrl + ", adding new video sourceHref = " + sourceHref);

            cb.foundLink(sourceHref);
          }

          if (mp4Href != null && !mp4Href.isEmpty()) {

            log.debug3("srcUrl = " + srcUrl + ", adding new video mp4Href = " + mp4Href);

            cb.foundLink(mp4Href);
          }

          if (ogvHref != null && !ogvHref.isEmpty()) {

            log.debug3("srcUrl = " + srcUrl + ", adding new video ogvHref = " + ogvHref);

            cb.foundLink(ogvHref);
          }

          if (webmHref != null && !webmHref.isEmpty()) {

            log.debug3("srcUrl = " + srcUrl + ", adding new video webmHref = " + webmHref);

            cb.foundLink(webmHref);
          }
        }
      }
    } catch (IOException e) {
      log.error("IOException occurred while reading input stream: " + e.getMessage());
    } finally {
      // Close the scanner
      scanner.close();
    }
  }
}
