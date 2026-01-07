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

package org.lockss.plugin.projmuse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;
import org.lockss.util.Logger;

import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;
import org.lockss.util.urlconn.CacheSuccess;

public class ProjectMuse2017HttpResponseHandler implements CacheResultHandler {
  private static final Logger logger = Logger.getLogger(ProjectMuse2017HttpResponseHandler.class);

  protected static final Pattern IMAGE_PAT = Pattern.compile("/images/.*[.](gif|png)");

  @Override
  public void init(final CacheResultMap map) throws PluginException {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to ProjectMuse2017HttpResponseHandler.init()");
    
  }
  
  // currently this is not called on
  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, int responseCode) throws PluginException {

    logger.debug2(url);  
      switch (responseCode) {
        case 403: 
            logger.debug3("found 403");
            Matcher imageMat = IMAGE_PAT.matcher(url);
            if (imageMat.find()) {
              logger.debug3("Bad media file or forbidden access, downgrading to warning.");
              return new CacheException.NoRetryDeadLinkException("403 Forbidden (non-fatal)");
            }
            return new CacheException.RetrySameUrlException("403 Forbidden error");
        default:
            logger.debug2("default");
            throw new UnsupportedOperationException();
        }
  }
  
  
  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, final Exception ex)
    throws PluginException {
    logger.warning(ex.getMessage() + ": " + url);
    
    if (ex instanceof ContentValidationException.WrongLength) {
      logger.debug3("Ignoring Wrong length - storing file");
      // ignore and continue
      return new CacheSuccess();
    }
    if (ex instanceof ContentValidationException) {
      logger.debug3("Not storing file");
      return new CacheException.NoStoreWarningOnly();
    }
    
    if (ex instanceof javax.net.ssl.SSLHandshakeException) {
      logger.debug3("Retrying SSLHandshakeException", ex);
      // retry 2 times
      return new CacheException.RetryableNetworkException_2_10S();
    }
    
    // we should only get in her cases that we specifically map...be very unhappy
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException();

  }

}
