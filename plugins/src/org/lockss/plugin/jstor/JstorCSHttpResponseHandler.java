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

package org.lockss.plugin.jstor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/*
 * Ignore 503 errors on certain types of files - they're really more of a 404 
 */
public class JstorCSHttpResponseHandler implements CacheResultHandler {
    
  //http://www.jstor.org/px/client/main.min.js,"503 Service Unavailable

  protected static final Pattern NON_FATAL_PAT = 
      Pattern.compile("\\.(js|css|gif)$");
    
  private static final Logger logger = Logger.getLogger(JstorCSHttpResponseHandler.class);

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to JstorCSHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    Matcher mat;
    switch (responseCode) {
      case 403:
        mat = NON_FATAL_PAT.matcher(url);
        if (mat.find()) {
          return new CacheException.NoRetryDeadLinkException("403 Foridden (non-fatal)");
        } else {
          return new CacheException.PermissionException("403 Forbidden");
        }
      case 503:
        // Do not fail the crawl for 503 errors at URLs like the one below should not be fatal
        mat = NON_FATAL_PAT.matcher(url);
        if (mat.find()) {
          return new CacheException.NoRetryDeadLinkException("503 Service Unavailable (non-fatal)");
        } else {
          return new CacheException.RetrySameUrlException("503 Service Unavailable");
        }
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
  }
  
  
}