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

package org.lockss.plugin.springer.link;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpringerLinkHttpResponseHandler implements CacheResultHandler {

  private static final Logger logger = Logger.getLogger(SpringerLinkHttpResponseHandler.class);
  protected static final Pattern NON_FATAL_PAT =
      Pattern.compile("^http(s)?://static-content.springer.com/(esm|image)?/art[^/]+/MediaObjects/");

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to HighWirePressHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode)
      throws PluginException {
    logger.debug2(String.format("URL %s with %d", url, responseCode));
    switch (responseCode) {
      case 403:
        logger.debug3("403");
        Matcher mat = NON_FATAL_PAT.matcher(url);
        if (mat.find()) {
            return new CacheException.NoRetryDeadLinkException("403 Forbidden (non-fatal)");
        } else {
            return new CacheException.NoRetryDeadLinkException("403 Forbidden error");
        }
      case 404:
        logger.debug3("404");
        if(url.contains("MediaObjects")) {
        	return new SpringerLinkRetryDeadLinkException("404 Not Found");
        }
        return new CacheException.RetryDeadLinkException("404 Not Found");
      case 500:
        logger.debug3("500");
        return new CacheException.RetrySameUrlException("500 Internal Server Error");
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex)
      throws PluginException {
    logger.debug2(String.format("URL %s with %s", url, ex.getClass().getName()));
    if (ex instanceof ContentValidationException.WrongLength) {
      logger.debug3("Wrong length");
      return new SpringerLinkRetryDeadLinkException(ex.getMessage());
    }
    logger.warning("Unexpected error type (" + ex.getClass().getName() + ") in handleResult(): AU " + au.getName() + "; URL " + url);
    throw new UnsupportedOperationException("Unexpected error type", ex);
  }
  
  class SpringerLinkRetryDeadLinkException extends CacheException.RetryDeadLinkException {
    
    public SpringerLinkRetryDeadLinkException() {
      super();
    }

    public SpringerLinkRetryDeadLinkException(String message) {
      super(message);
    }

    public int getRetryCount() {
      return 7;
    }
    
  }
  
}
