/*
 * $Id: PortlandPressHtmlLinkExtractor.java,v 1.1 2009-11-07 00:51:19 thib_gc Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
