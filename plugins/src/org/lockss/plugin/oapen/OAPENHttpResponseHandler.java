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

package org.lockss.plugin.oapen;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;
import org.lockss.util.urlconn.CacheException.RetryableNetworkException_3;

public class OAPENHttpResponseHandler implements CacheResultHandler {

  private static final Logger logger = Logger.getLogger(OAPENHttpResponseHandler.class);

  /*
  We got a lot of 429 errors for the following kind of png files.
  https://library.oapen.org/themes/Mirage2/vendor/jquery-ui/themes/base/images/ui-bg_glass_55_fbf9ee_1x400.png
  https://library.oapen.org/themes/Mirage2/vendor/jquery-ui/themes/base/images/ui-bg_glass_95_fef1ec_1x400.png
  https://library.oapen.org/themes/Mirage2/vendor/jquery-ui/themes/base/images/ui-icons_454545_256x240.png
  https://library.oapen.org/themes/Mirage2/vendor/jquery-ui/themes/base/images/ui-bg_flat_0_aaaaaa_40x100.png
  429 Too Many Requests
   */
  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to OAPENHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au, String url, int code) throws PluginException {
    logger.debug("Response: " + code + ", ...");
    switch (code) {
      case 429:
        logger.debug("Response 429 url: " + url);
        if (url.contains("themes/Mirage2/vendor/jquery-ui/themes")) {
          logger.debug2("Expected 429 from jquery related urls, skipping retry: " + url);
          return new RetryableNetworkException_3_90S();
        } else {
          logger.debug2("Unexpected 429 response from URL: " + url);
          return new CacheException.UnexpectedNoRetryFailException(
                  "Unexpected 429 error for URL: " + url
          );
        }
      default:
        throw new PluginException("Not expecting response code: " + code + ", url = " + url);
    }
  }

  /** Retryable & no fail network error; two tries with 90 second delay */
  public static class RetryableNetworkException_3_90S
          extends RetryableNetworkException_3 {
    public RetryableNetworkException_3_90S() {
      super();
    }

    public RetryableNetworkException_3_90S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_3_90S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 90 * Constants.SECOND;
    }

    @Override
    protected void setAttributes() {
      super.setAttributes();
      attributeBits.clear(ATTRIBUTE_FAIL);
      attributeBits.set(ATTRIBUTE_NO_STORE);
    }
  }
}
