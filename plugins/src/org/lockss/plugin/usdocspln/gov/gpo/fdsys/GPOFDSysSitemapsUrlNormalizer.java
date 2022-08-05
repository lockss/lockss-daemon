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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;

public class GPOFDSysSitemapsUrlNormalizer extends HttpToHttpsUtil.BaseUrlHttpHttpsUrlNormalizer {

  @Override
  public String additionalNormalization(String url, ArchivalUnit au) throws PluginException {
    final String prefixPath = "fdsys/search/pagedetails.action?";
    final String packageIdVar = "packageId=";
    final String destination1 = "fdsys/pkg/";
    final String destination2 = "/content-detail.html";

    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String shortBaseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    if (!url.startsWith(shortBaseUrl)) {
      return url; // No transformation
    }
    
    String prefix1 = baseUrl + prefixPath;
    String prefix2 = shortBaseUrl + ":80/" + prefixPath;  
    if (!(url.startsWith(prefix1) || url.startsWith(prefix2))) {
      return url; // No transformation
    }

    int ix = url.indexOf(packageIdVar, shortBaseUrl.length());
    if (ix < 0) {
      return url; // No transformation
    }

    ix = ix + packageIdVar.length();
    int jx = url.indexOf('&', ix);
    String packageIdVal = (jx < 0 ? url.substring(ix) : url.substring(ix, jx));
    return baseUrl + destination1 + packageIdVal + destination2;
  }

}
