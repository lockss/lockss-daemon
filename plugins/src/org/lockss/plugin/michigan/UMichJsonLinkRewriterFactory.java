package org.lockss.plugin.michigan;

import java.io.*;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UMichJsonLinkRewriterFactory implements LinkRewriterFactory {

  private static final Logger log = Logger.getLogger(UMichJsonLinkRewriterFactory.class);
  
  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String srcUrl,
                                        LinkTransform xform)
      throws PluginException, IOException {
    // Must be called "info.json"
    final String slashinfodotjson = "/info.json";
    if (!srcUrl.endsWith(slashinfodotjson)) {
      log.debug2(String.format("Not a designated IIIF Image Info file: %s", srcUrl));
      return in;
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
    if (!rootNode.isObject()) {
      log.debug(String.format("Root node is not an object: %s", srcUrl));
      return in; // FIXME
    }
    ObjectNode rootObject = (ObjectNode)rootNode;
    
    // Protocol
    String protocol = rootNode.path("protocol").asText(null);
    if (!IiifJsonLinkExtractor.IIIF_PROTOCOL_IMAGE.equals(protocol)) {
      log.debug2(String.format("Unknown protocol: %s: %s", srcUrl, protocol));
      return in; // FIXME
    }

    // ID
    JsonNode idNode = rootNode.path("@id"); 
    String id = idNode.asText(null);
    if (id == null) {
      log.debug(String.format("Missing @id: %s", srcUrl));
      return in;
    }

    // Replace ID
    rootObject.put("@id", xform.rewrite(id));
    return new ByteArrayInputStream(rootObject.toString().getBytes(encoding));
  }
  
}
