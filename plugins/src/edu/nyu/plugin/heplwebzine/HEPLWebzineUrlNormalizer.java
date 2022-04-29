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

package edu.nyu.plugin.heplwebzine;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;

public class HEPLWebzineUrlNormalizer extends HttpHttpsParamUrlNormalizer {

  public HEPLWebzineUrlNormalizer() {
    super(ConfigParamDescr.BASE_URL.getKey());
  }
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) {
    url = super.normalizeUrl(url, au);
    /*
     * In issue 4 article 4 (https://webzine.web.cern.ch/4/papers/3/), there are
     * links to Perl scripts and modules that are no longer allowed by default
     * by the CERN Web server. We discovered this because two of them spin
     * forever, eventually failing with a "Connection reset", but otherwise the
     * offending URLs return 404 with a specialized message. It would be
     * difficult to induce an exception for this journal that ceased in 2007,
     * but luckily all the Perl files are also available as .tar.gz
     * (https://webzine.web.cern.ch/4/papers/3/examples.tar.gz) and .zip
     * (https://webzine.web.cern.ch/4/papers/3/examples.zip) archives. Point at
     * one of them instead (say, the .zip).
     */
    if ("4".equals(au.getConfiguration().get("issue_no"))) {
      if (url.endsWith(".pm") || url.endsWith(".pl")) {
        int lastSlash = url.lastIndexOf('/');
        return url.substring(0, lastSlash + 1) + "examples.zip";
      }
    }
    return url;
  }
  
}
