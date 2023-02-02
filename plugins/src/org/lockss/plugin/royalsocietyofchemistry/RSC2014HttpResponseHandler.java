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

package org.lockss.plugin.royalsocietyofchemistry;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;
import org.lockss.util.Logger;

import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;
import org.lockss.util.urlconn.CacheException.RetryableNetworkException_2_10S;
import org.lockss.util.urlconn.CacheSuccess;

public class RSC2014HttpResponseHandler implements CacheResultHandler {
  private static final Logger log = Logger.getLogger(RSC2014HttpResponseHandler.class);
  
  @Override
  @Deprecated
  public void init(final CacheResultMap map) throws PluginException {
    log.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to RSC2014HttpResponseHandler.init()");
    
  }
  
  // currently this is only called on 400 codes by the books plugin
  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, int responseCode) throws PluginException {
    log.debug(responseCode + ": " + url);
    switch (responseCode) {
      case 400:
          return new CacheException.RetrySameUrlException();
      default:
        log.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }
  
  
  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, final Exception ex)
    throws PluginException {
    
    // this checks for the specific exceptions before going to the general case and retry
    if (ex instanceof ContentValidationException.WrongLength) {
      if (url.contains("pdf/")) {
        log.warning("Wrong length - not storing file " + url);
        // retry and no store cache exception
        return new RetryableNoFailException_2_10S(ex);
      } else {
        log.debug("Ignoring Wrong length - storing file");
        // ignore and continue
        return new CacheSuccess();
      }
    }
    
    // handle retryable exceptions ; URL MIME type mismatch for pages like 
    //   http://pubs.rsc.org/en/content/articlepdf/2014/gc/c4gc90017k will never return good PDF content
    if (ex instanceof ContentValidationException) {
      log.warning("Warning - retry/no fail/no store " + url);
      // retry and no store cache exception
      return new RetryableNoFailException_2_10S(ex);
    }
    
    // org.lockss.util.StreamUtil$InputException: java.net.SocketTimeoutException: Read timed out
    if (ex instanceof org.lockss.util.StreamUtil.InputException) {
      log.warning("Warning - InputException " + url);
      return new RetryableNoFailException_2_10S(ex);
    }
    
    // we should only get in her cases that we specifically map...be very unhappy
    log.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException();
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
