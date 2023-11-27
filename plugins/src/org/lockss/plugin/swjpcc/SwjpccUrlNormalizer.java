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

package org.lockss.plugin.swjpcc;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


/*
 * Removes unnecessary suffixes
 */
public class SwjpccUrlNormalizer implements UrlNormalizer {
  protected static Logger log = 
      Logger.getLogger(SwjpccUrlNormalizer.class);

  protected static final String CACHE_SUFFIX = "?__SQUARESPACE_CACHEVERSION=";

  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {

    // Only do this normalization for urls with arguments
    int qmark = url.indexOf("?");
    // just remove any undesired suffixes
    // if the suffix doesn't exist the url doesn't change, no sense wasting
    // cycles checking if it exists before trying to remove
    if (qmark >= 0) {
      url = StringUtils.substringBeforeLast(url, CACHE_SUFFIX);
    }
    return url;
  }

}