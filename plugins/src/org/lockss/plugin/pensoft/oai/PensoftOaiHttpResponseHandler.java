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

package org.lockss.plugin.pensoft.oai;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;

import java.io.IOException;

public class PensoftOaiHttpResponseHandler implements CacheResultHandler {

  private static final Logger logger = Logger.getLogger(PensoftOaiHttpResponseHandler.class);

  /*
  On https://neotropical.pensoft.net/article/111865/
  <link href="https://fonts.googleapis.com/css2?family=Inter:slnt,wght@-10..0,100..900&display=swap" rel="stylesheet">
  was blocked due to MIME type (“text/html”) mismatch (X-Content-Type-Options: nosniff)."
   */
  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to PensoftOaiHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au, String url, int code) throws PluginException {
    logger.debug("Response: " + code + ", ...");
    switch (code) {
      case 400:
        logger.debug("Response 400 url: " + url);
        if (url.contains("fonts.googleapis.com")) {
          logger.debug2("Expected 400 from fonts.googleapis.com, skipping retry: " + url);
          return null;
        } else {
          logger.debug2("Unexpected 400 response from URL: " + url);
          return new CacheException.UnexpectedNoRetryFailException(
                  "Unexpected 400 error for URL: " + url
          );
        }
      default:
        throw new PluginException("Not expecting response code: " + code + ", url = " + url);
    }
  }
}
