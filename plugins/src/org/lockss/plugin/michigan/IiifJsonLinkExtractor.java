package org.lockss.plugin.michigan;

import java.io.*;
import java.util.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
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
      // And get all the 256x256 pixel "tiles" (see above about 256)
      for (int x = 0 ; x < width ; x += xstep) {
        int w = (x + xstep <= width) ? xstep : width - x;
        for (int y = 0 ; y < height ; y += ystep) {
          int h = (y + ystep <= height) ? ystep : height - y;
          String iiifSize = (w == xstep) ? String.format("%d,", xstep)
                          : (h == ystep) ? String.format(",%d", ystep)
                          : String.format("%d,", w);
          cb.foundLink(String.format("%s/%d,%d,%d,%d/%s/0/default.%s", id, x, y, w, h, iiifSize, format));
        }
      }
    }
  }

}
