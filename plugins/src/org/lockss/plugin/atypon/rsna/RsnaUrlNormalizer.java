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

package org.lockss.plugin.atypon.rsna;

import java.util.regex.Pattern;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.util.Logger;

/*
 * Adds in the RSNA specific normalizations
 */
public class RsnaUrlNormalizer extends BaseAtyponUrlNormalizer {
  protected static Logger log = Logger.getLogger(RsnaUrlNormalizer.class);
  /*
    https://pubs.rsna.org/products/rsna/fonts/icomoon/icomoon.eot?yq99jl
    https://pubs.rsna.org/products/rsna/fonts/icomoon/icomoon.svg?ubwa67
    https://pubs.rsna.org/products/rsna/fonts/icomoon/icomoon.ttf?yq99jl
    https://pubs.rsna.org/products/rsna/fonts/icomoon/icomoon.woff?ubwa67
   */
  protected static final Pattern FONT_ARG_PATTERN = Pattern.compile("(\\.(:?eot|svg|ttf|woff))\\?.+$");

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    // some font files have an argument that isn't needed
    String returnString = FONT_ARG_PATTERN.matcher(url).replaceFirst("$1");
    if (!returnString.equals(url)) {
      log.debug3("normalized font url: " + returnString);
      url = returnString;
    }

    // http to https
    HttpHttpsUrlHelper helper = new HttpHttpsUrlHelper(au,
        ConfigParamDescr.BASE_URL.getKey(),
        "base_url");
    url = helper.normalize(url);

    return super.normalizeUrl(url, au);
  }
}