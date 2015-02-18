/*
 * $Id$
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

package org.lockss.plugin.pensoft;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class PensoftUrlNormalizer implements UrlNormalizer {
  protected static Logger log = 
    Logger.getLogger(PensoftUrlNormalizer.class);

  // code copied from HttpClientUrlConnect.createMethod - without normalizing
  // both encoded and unencoded urls will be preserved
  public String normalizeUrl(String urlString, ArchivalUnit au)
      throws PluginException {
    String u_str = urlString;
    /* if the urlString is not "readable" ascii (0x1F < x < 0x7f), then
     * normalize/encode the string 
     */
    if(!isReadableAscii(urlString)) {
        if(log.isDebug2()) log.debug2("in:" + u_str);
        u_str = UrlUtil.encodeUri(urlString, Constants.ENCODING_UTF_8);
        if(log.isDebug2()) log.debug2("out:" + u_str);
      }
      return u_str;
  }
  /* defining isReadableAscii() rather than using StringUtil.isAscii() to catch
   * weird control characters (<= 31) frequently found in Pensoft article urls
   */
  private static boolean isReadableAscii(String s) {
    for (int ix = 0; ix < s.length(); ix++) {
      if ((s.charAt(ix) > 0x7F) || (s.charAt(ix) < 0x20)) {
        return false;
      }
    }
    return true;
  }
}