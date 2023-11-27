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

package org.lockss.plugin.michigan;

import java.io.*;
import java.util.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import com.fasterxml.jackson.databind.*;

public class IiifJsonLinkExtractor implements LinkExtractor {
  
  public static final Logger log = Logger.getLogger(IiifJsonLinkExtractor.class);
  
  public static final String IIIF_PROTOCOL_IMAGE = "http://iiif.io/api/image";
  
  public static final String IIIF_CONTEXT_IMAGE_2 = "http://iiif.io/api/image/2/context.json";
  
  public static final String IIIF_PROFILE_IMAGE_2_1 = "http://iiif.io/api/image/2/level1.json";
  
  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException, PluginException {
    // Must be called "info.json"
    final String slashinfodotjson = "/info.json";
    if (!srcUrl.endsWith(slashinfodotjson)) {
      log.debug2(String.format("Not a designated IIIF Image Info file: %s", srcUrl));
      return;
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
    
    // Protocol
    String protocol = rootNode.path("protocol").asText(null);
    if (!IIIF_PROTOCOL_IMAGE.equals(protocol)) {
      log.debug2(String.format("Unknown protocol: %s: %s", srcUrl, protocol));
      return;
    }

    // ID
    String id = rootNode.path("@id").asText(null);
    if (id == null) {
      log.debug(String.format("Missing @id: %s", srcUrl));
      return;
    }
    
    // Width and height
    int width = rootNode.path("width").asInt(-1);
    if (width < 0) {
      log.debug2(String.format("Invalid width: %s: %d", srcUrl, width));
      return;
    }
    int height = rootNode.path("height").asInt(-1);
    if (height < 0) {
      log.debug2(String.format("Invalid height: %s: %d", srcUrl, height));
      return;
    }

    // Formats
    List<String> formats = new ArrayList<String>();
    
    // Context
    String context = rootNode.path("@context").asText(null);
    if (IIIF_CONTEXT_IMAGE_2.equals(context)) {
      formats.add("jpg");
    }
    
    // Profile
    JsonNode profileNode = rootNode.path("profile");
    if (!profileNode.isMissingNode() && profileNode.isArray()) {
      Iterator<JsonNode> iter = profileNode.elements();
      if (!iter.hasNext()) {
        log.debug(String.format("Empty profile specification: %s", srcUrl));
        return;
      }
      String profileUrl = iter.next().asText(null);
      if (!IIIF_PROFILE_IMAGE_2_1.equals(profileUrl)) {
        log.debug(String.format("Unknown profile URL: %s: %s", srcUrl, profileUrl));
        return;
      }
      for (int i = 1 ; iter.hasNext() ; ++i) {
        JsonNode obj = iter.next();
        if (!obj.isObject()) {
          log.debug(String.format("Invalid profile specification at index %d: %s: %s", i, srcUrl, obj));
          return;
        }
        JsonNode profileFormats = obj.path("formats");
        if (!profileFormats.isMissingNode()) {
          if (!profileFormats.isArray()) { // FIXME maybe this might be a single string?
            log.debug(String.format("Expected profile list at index %d: %s: %s", i, srcUrl, profileFormats));
            continue;
          }
          for (Iterator<JsonNode> formatIter = profileFormats.elements() ; formatIter.hasNext() ; ) {
            String format = formatIter.next().asText();
            if (!formats.contains(format)) {
              formats.add(format);
            }
          }
        }
      }
    }
    
    if (formats.size() == 0) {
      // What to do?
      log.debug(String.format("No formats found: %s", srcUrl));
      return;
    }
    
    // FIXME "256" is for Fulcrum + Leaflet, it's not a universal solution to this problem
    final int xstep = 256;
    final int ystep = 256;
    
    // For each format...
    for (String format : formats) {
      // Get the full image
      cb.foundLink(String.format("%s/full/full/0/default.%s", id, format));
      /*
       * Disable this for now. One day we'll respond to IIIF requests from the full size images.
       */
//      // And get all the 256x256 pixel "tiles" (see above about 256)
//      for (int x = 0 ; x < width ; x += xstep) {
//        int w = (x + xstep <= width) ? xstep : width - x;
//        for (int y = 0 ; y < height ; y += ystep) {
//          int h = (y + ystep <= height) ? ystep : height - y;
//          String iiifSize = (w == xstep) ? String.format("%d,", xstep)
//                          : (h == ystep) ? String.format(",%d", ystep)
//                          : String.format("%d,", w);
//          cb.foundLink(String.format("%s/%d,%d,%d,%d/%s/0/default.%s", id, x, y, w, h, iiifSize, format));
//        }
//      }
    }
  }

}
