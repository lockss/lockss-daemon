/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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