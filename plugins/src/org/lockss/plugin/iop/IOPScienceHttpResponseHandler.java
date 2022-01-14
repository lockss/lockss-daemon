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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

public class IOPScienceHttpResponseHandler implements CacheResultHandler {
  
  protected static Logger logger = Logger.getLogger(IOPScienceHttpResponseHandler.class);
  // Want to match on URLs that contain another host in error (relative should have been absolute)
  // these URLs are invalid and should not cause fatal errors
  // Host string is one alpha + 0 or more chars dot 2 or more chars dot 2 or more chars (excluding slash)
  protected static Pattern PAT_MAL_URL =
      Pattern.compile("^https?://[^/]+/.+/[a-z]+[^./]*[.][^/.]{2,}[.][^/.]{2,}/?.*");
  
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException();
  }
  
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    Matcher mat = PAT_MAL_URL.matcher(url);
    switch (responseCode) {
      case 403:
        logger.debug2("403");
        if (mat.matches() || url.contains("%20")) {
          // http://iopscience.iop.org/0143-0807/35/4/045020/www.imcce.fr/langues/...
          // http://iopscience.iop.org/0026-1394/51/5/361/media/Data%20summary.xlsx
          return new CacheException.MalformedURLException("403 Forbidden (non-fatal)");
        }
        else {
          return new CacheException.RetrySameUrlException("403 Forbidden");
        }
      case 503:
        logger.debug2("503");
        if (mat.matches()) {
          // http://iopscience.iop.org/0264-9381/31/21/215007/www.bu.edu.eg
          return new CacheException.MalformedURLException("503 Service Unavailable (non-fatal)");
        }
        else {
          return new CacheException.RetrySameUrlException("503 Service Unavailable");
        }
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }
  
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException();
  }
  
}
