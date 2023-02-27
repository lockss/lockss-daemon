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
