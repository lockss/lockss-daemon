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

package org.lockss.plugin.usdocspln.gov.govinfo;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheException.RetryableException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GovInfoSitemapsHttpResponseHandler implements CacheResultHandler {

  private static final Logger logger = Logger.getLogger(GovInfoSitemapsHttpResponseHandler.class);

  /*examples:
    app/details/lib/bootstrap/ico/apple-touch-icon-167.png
    app/details/lib/bootstrap/ico/apple-touch-icon-76.png
    apple-touch-icon-72.png
    sites/all/apple-touch-icon-152.png
    sites/all/apple-touch-icon-72.png
    sites/all/themes/custom/misc/menu-collapsed.png
    sites/all/themes/custom/misc/menu-expanded.png
    sites/all/themes/custom/misc/menu-leaf.png
    app/dynamic/stylesheets/fonts/glyphicons-halflings-regular.svg
    sites/all/themes/custom/bootstrap-fdsys/bootstrap/fonts/glyphicons-halflings-regular.eot
    sites/all/themes/custom/bootstrap-fdsys/bootstrap/fonts/glyphicons-halflings-regular.woff2
    sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.eot
    sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.eot%3Fv=4.3.0
    sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.svg%3Fv=4.3.0
   */
  protected static final Pattern NON_FATAL_GRAPHICS_PATTERN =
      Pattern.compile("\\.(bmp|css|eot|gif|ico|jpe?g|js|png|svg|tif?f|ttc|ttf|woff.?|dfont|otf)");

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to GovInfoSitemapsHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
      logger.debug2(String.format("URL: %s, response code: %d", url, responseCode));
      switch (responseCode) {
        case 504:
          Matcher fmat = NON_FATAL_GRAPHICS_PATTERN.matcher(url);
          if (fmat.find()) {
            return new GovInfoRetryNoFailException(responseCode + " Gateway Timeout (non-fatal)");
          }
          return new GovInfoRetryFailException(responseCode + " Gateway Timeout");
        case 520:
        case 525:
          return new CacheException.RetrySameUrlException();
        default:
          logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
          throw new UnsupportedOperationException("Unexpected response code: " + responseCode);
      }
    }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception exc)
      throws PluginException {
    logger.debug2(String.format("URL: %s, exception: %s", url, exc));
    Class<? extends Exception> cla = exc.getClass();
    if (cla == IOException.class) {
      if ("chunked stream ended unexpectedly".equals(exc.getMessage())) {
        return new CacheException.RetryableNetworkException();
      }
      return new CacheException.UnknownExceptionException("Unmapped exception: " + exc, exc);
    }
    throw new UnsupportedOperationException("Unexpected exception: " + exc);
  }
  
  public static class GovInfoRetryableException extends RetryableException {
    
    public GovInfoRetryableException() {
      super();
    }
    
    public GovInfoRetryableException(String msg) {
      super(msg);
    }

    @Override
    protected void setAttributes() {
      super.setAttributes();
      attributeBits.set(ATTRIBUTE_RETRY);
    }
    
    @Override
    public int getRetryCount() {
      return 60;
    }

    @Override
    public long getRetryDelay() {
      return 60 * Constants.SECOND;
    }
    
  }
  
  public static class GovInfoRetryFailException extends GovInfoRetryableException {
    
    public GovInfoRetryFailException() {
      super();
    }
    
    public GovInfoRetryFailException(String msg) {
      super(msg);
    }
    
    @Override
    protected void setAttributes() {
      super.setAttributes();
      attributeBits.set(ATTRIBUTE_FAIL);
    }
    
  }
  
  public static class GovInfoRetryNoFailException extends GovInfoRetryableException {
    
    public GovInfoRetryNoFailException() {
      super();
    }
    
    public GovInfoRetryNoFailException(String msg) {
      super(msg);
    }
    
    @Override
    protected void setAttributes() {
      super.setAttributes();
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
    
  }
  
}