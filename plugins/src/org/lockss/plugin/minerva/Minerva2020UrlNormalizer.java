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

package org.lockss.plugin.minerva;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * <p>
 * URL normalizer for Minerva2020Plugin. Adds {@code "index.html"} (or
 * {@code index.htm} for volume 18, see {@link Minerva2020CrawlSeedFactory})
 * to unqualified start URLs, and normalizes the case of URL prefixes to match
 * that of {@code volume_name}.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since Minerva2020Plugin 2
 */
public class Minerva2020UrlNormalizer implements UrlNormalizer {

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String volumeName = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());
    String prefixWithoutSlash = baseUrl + volumeName;
    String prefixWithSlash = prefixWithoutSlash + "/";
    // Normalize ${base_url}/${volume_name} and ${base_url}/${volume_name}/
    // to ${base_url}/${volume_name}/index.html (/index.html for volume 18)
    if (url.equalsIgnoreCase(prefixWithoutSlash) || url.equalsIgnoreCase(prefixWithSlash)) {
      return prefixWithSlash + (volumeName.equals("18") ? "index.htm" : "index.html");
    }
    // Normalize case of ${base_url}/${volume_name}/ prefix to match ${volume_name}
    if (StringUtils.startsWithIgnoreCase(url, prefixWithSlash)) {
      return prefixWithSlash + url.substring(prefixWithSlash.length());
    }
    // Leave other URLs alone
    return url;
  }
  
}
