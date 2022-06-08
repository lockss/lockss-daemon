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

package org.lockss.plugin.medknow;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.util.urlconn.CacheException.RetryableNetworkException_2_10S;


public class MedknowHttpResponseHandler implements CacheResultHandler {
  
  private static final Logger logger = Logger.getLogger(MedknowHttpResponseHandler.class);
  
  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to MedknowHttpResponseHandler.init()");
  }
  
  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    switch (responseCode) {
      case 502:
        logger.debug2("502: " + url);
        return new CacheException.RetryableNetworkException_5_5M("502 Bad Gateway");
        
      case 503:
        logger.debug2("503: " + url);
        return new CacheException.RetryableNetworkException_5_5M("503 Service Unavailable");
        
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }
  
  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    // this checks for the specific exceptions before going to the general case and retry
    if (ex instanceof ContentValidationException.WrongLength) {
      logger.warning("Wrong length - storing file " + url);
      // ignore and continue
      return new CacheSuccess();
    }
    
    // handle retryable exceptions ; URL MIME type mismatch
    if (ex instanceof ContentValidationException) {
      logger.warning("Warning - retry/no fail/no store " + url);
      // retry and no store cache exception
      return new RetryableNoFailException_2_10S(ex);
    }
    
    if (ex instanceof javax.net.ssl.SSLHandshakeException) {
      logger.warning("Warning - SSLHandshakeException " + url);
      return new RetryableNetworkException_2_10S(ex);
    }

    if (ex instanceof javax.net.ssl.SSLException) {
      if (au.getStartUrls().contains(url) ||
          au.getPermissionUrls().contains(url)) {
        logger.warning("Warning - Connection reset, ignoring for start and permission urls " + url);
        // ignore and continue
        return new CacheException.NoStoreWarningOnly("Encountered known SSLException. Ignoring.");
      }
      return new CacheException("Encountered unexpected url: " + url + " for SSLException: " + ex);
    }
    
    // we should only get in her cases that we specifically map...be very unhappy
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
  }
  
  /** Retryable & no fail network error; two tries with 10 second delay */
  protected static class RetryableNoFailException_2_10S
    extends RetryableNetworkException_2_10S {
    
    public RetryableNoFailException_2_10S() {
      super();
    }
    
    public RetryableNoFailException_2_10S(String message) {
      super(message);
    }
    
    /** Create this if details of causal exception are more relevant. */
    public RetryableNoFailException_2_10S(Exception e) {
      super(e);
    }
    
    @Override
    protected void setAttributes() {
      super.setAttributes();
      attributeBits.clear(ATTRIBUTE_FAIL);
      attributeBits.set(ATTRIBUTE_NO_STORE);
    }
    
  }
  
}
