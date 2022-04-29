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

package edu.harvard.plugin.AppliedSemiotics;

import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;

public class AppliedSemiotics2022UrlNormalizer implements UrlNormalizer {

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    Pattern baseUrlPat = Pattern.compile("^([^:])+://([^.]+)\\.([^/]+)/([^/]+)/$", Pattern.CASE_INSENSITIVE);
    Matcher baseUrlMat = baseUrlPat.matcher(baseUrl);
    if (baseUrlMat.matches()) {
      String protocol = baseUrlMat.group(1);
      String subdomain = baseUrlMat.group(2);
      String domain = baseUrlMat.group(3);
      String dir = baseUrlMat.group(4);
      Pattern urlPat = Pattern.compile(String.format("^%s://www\\.%s/%s/%s/(.*)", protocol, domain, subdomain, dir), Pattern.CASE_INSENSITIVE);
      Matcher urlMat = urlPat.matcher(url);
      if (urlMat.matches()) {
        return baseUrl + urlMat.group(1);
      }
    }
    return url;
  }
  
}
