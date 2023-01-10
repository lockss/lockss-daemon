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

package org.lockss.plugin.resiliencealliance;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.UrlUtil;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class ResilienceAllianceUrlHostHelper {

  public List<String> getBaseAndPermissionUrls(ArchivalUnit au) {
    List<String> startAndPermissionUrls = new ArrayList<String>(au.getStartUrls());
    startAndPermissionUrls.addAll(au.getPermissionUrls());
    return startAndPermissionUrls;
  }

  public String getBaseUrlHost(ArchivalUnit au) {
    String baseUrlHost = au.getConfiguration().get("base_url");
    try {
      baseUrlHost = UrlUtil.getHost(baseUrlHost);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    return baseUrlHost;
  }

  public String getWwwComplementFromUrl(String url) {
    String url_w_or_without_www;
    if ( url.startsWith("https://www.") ) {
      url_w_or_without_www = url.replace("https://www.", "https://");
    } else {
      url_w_or_without_www = url.replace("https://", "https://www.");
    }
    return url_w_or_without_www;
  }

}