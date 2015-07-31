/*
 * $Id: ScJsonLinkExtractorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/
package org.lockss.plugin.silverchair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ScJsonLinkExtractor implements LinkExtractor {
  /**
   * Parse content on InputStream,  call cb.foundUrl() for each URL found
   *
   * @param au the au for which this link extractor
   * @param in an inputstream containing the json file
   * @param encoding  the file encoding (this had better be utf-8
   * @param srcUrl The URL at which the content lives.  Used as the base for
   * resolving relative URLs (unless/until base set otherwise by content)
   * @param cb the callback to use for found urls.
   */
  @Override
  public void extractUrls(final ArchivalUnit au, final InputStream in,
                          final String encoding,
                          final String srcUrl, final Callback cb)
    throws IOException, PluginException {
    String foundUrl = null;
    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readValue(in, JsonNode.class);
    JsonNode dataNode= rootNode.get("d");
    if(dataNode.isTextual()) { // this is JAMA
    // {d :url}
      foundUrl = dataNode.asText();
    }
    else if (dataNode.get("pdfUrl").isTextual()) { // this spie
    // {d: {hasAccess: true, pdfUrl:url, itemIsFree:true} }
      foundUrl = dataNode.get("pdfUrl").asText();
    }
    if(!StringUtil.isNullString(foundUrl)) {
      URL baseUrl = new URL(srcUrl);
      cb.foundLink(UrlUtil.resolveUri(baseUrl, foundUrl));
    }
  }
}