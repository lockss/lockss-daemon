/*
 * $Id$
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

package org.lockss.plugin.scielo;

import java.io.IOException;
import java.net.URL;
import java.util.regex.*;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;

public class SciEloHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  public static final String ONCLICK = "onclick";

  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                      ArchivalUnit au,
                                      Callback cb)
      throws IOException {
    char ch = link.charAt(0);
    if ((ch == 'a' || ch == 'A') && Character.isWhitespace(link.charAt(1))) {
      String onclick = getAttributeValue(ONCLICK, link);
      if (onclick != null) {
        String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
        Pattern onclickPat = Pattern.compile(String.format("[\"'][ \t]*(%s[^ \t\"']*)[ \t]*[\"']", base_url),
                                             Pattern.CASE_INSENSITIVE);
        Matcher onclickMat = onclickPat.matcher(onclick);
        if (onclickMat.find()) {
          if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
          return resolveUri(baseUrl, onclickMat.group(1));
        }
      }
    }
    
    return super.extractLinkFromTag(link, au, cb);
  }
	
}
