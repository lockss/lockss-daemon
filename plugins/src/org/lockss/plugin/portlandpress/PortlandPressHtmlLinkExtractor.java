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

package org.lockss.plugin.portlandpress;

import java.io.IOException;
import java.net.*;

import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;

/**
 * <p>Extracts image URLs from Portland Press browseable figures
 * interface, which has static links to thumbnails but
 * Javascript-generated links to full-size images.</p>
 * <p>The full-size images have links embedded in Javascript code,
 * which could be extracted -- but it's easier to take advantage of
 * the correspondence between the thumbnail image name and its
 * full-size image name. An image named <code>foo_th.jpg</code> is the
 * thumbnail for an image named <code>foo.jpg</code>.</p>
 * @author Thib Guicherd-Callin
 */
public class PortlandPressHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                      ArchivalUnit au,
                                      Callback cb)
      throws IOException {
    char ch = link.charAt(0);
    if ((ch == 'i' || ch == 'I') && beginsWithTag(link, IMGTAG)) {
      String img = getAttributeValue("src", link);
      if (img != null) {
        // Look for a derivable link
        int ind = img.lastIndexOf("_th.");
        if (ind >= 0) {
          try {
            // Emit the derived link
            String emi = img.substring(0, ind) + img.substring(ind + 3);
            if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
            emit(cb, resolveUri(baseUrl, emi));
          }
          catch (MalformedURLException mue) {
            // Ignore, let parseLink() run into the same problem with the actual link
          }
        }
        
        // Emit the actual link
        return img;
      }
    }

    return super.extractLinkFromTag(link, au, cb);
  }

  
  
}
