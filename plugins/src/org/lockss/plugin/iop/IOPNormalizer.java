/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.iop;

import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * <p>This URL normalizer goes with the ancient plugin
 * org.lockss.plugin.iop.IOPPlugin, not with current plugins for the
 * IOPscience platform.</p>
 */
public class IOPNormalizer implements UrlNormalizer {

  private static final String host = "http://www.iop.org/";

  public String normalizeUrl(String url, ArchivalUnit au) {
    if (url == null) {
      return null;
    }
    if (!StringUtil.startsWithIgnoreCase(url, host)) {
      return url;
    }
    int startIdx = StringUtil.nthIndexOf(7, url, "/");
    int endIdx = StringUtil.nthIndexOf(8, url, "/");
    if (startIdx < 0 || endIdx < 0) {
      return url;
    }

    StringBuffer sb = new StringBuffer(url.length());
    sb.append(url.substring(0, startIdx));
    sb.append(url.substring(endIdx));

    return sb.toString();
  }

}
