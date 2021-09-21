/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.highwire;

import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;


public class HighWireJCoreHttpResponseHandler implements CacheResultHandler {
  
  private static final Logger logger = Logger.getLogger(HighWireJCoreHttpResponseHandler.class);
  
  /*examples:
   * http://science.sciencemag.org/highwire/filestream/609439/field_highwire_adjunct_files/0/D%27Angelo.MovieS1.mov 
   * http://www.bloodjournal.org/highwire/filestream/322606/field_highwire_adjunct_files/0/FigureS1.jpg 
   * A child can change this by extending this class and overriding the getter for the 403 pattern
   */
  protected static final Pattern DEFAULT_NON_FATAL_403_PAT = 
      Pattern.compile("/filestream/[^/]+/field_highwire_adjunct_files/");

  protected static final String GLENCOESOFTWARE_DOMAIN = "glencoesoftware.com";
  protected static final String GLENCOESOFTWARE_MESSAGE = "subdomain for this Publisher is likely not configured.";

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to HighWireDrupalHttpResponseHandler.init()");
  }
  
  public static final class NoFailRetryableNetworkException_2_10S
  extends CacheException.RetryableNetworkException_2_10S {
    
    private static final long serialVersionUID = 1L;
    
    public NoFailRetryableNetworkException_2_10S(String message) {
      super(message);
    }
    
    @Override
    protected void setAttributes() {
      super.setAttributes();
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }
  
  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    switch (responseCode) {
      case 403:
        logger.debug2("403 - pattern is " + getNonFatal403Pattern().toString());
        Matcher fmat = getNonFatal403Pattern().matcher(url);
        if (fmat.find()) {
          return new CacheException.NoRetryDeadLinkException("403 Forbidden (non-fatal)");
        }
        return new CacheException.RetrySameUrlException("403 Forbidden");
        
      case 500:
        logger.debug2("500: " + url);
        if (url.endsWith("_manifest.html") || 
            url.endsWith(".toc")) {
          return new CacheException.RetrySameUrlException("500 Internal Server Error");
        }
        return new CacheException.RetryDeadLinkException("500 Internal Server Error (non-fatal)");
        
      case 502:
        logger.debug2("502: " + url);
        if (url.endsWith(".index-by-author")) {
          return new NoFailRetryableNetworkException_2_10S("502 Bad Gateway (non-fatal)");
        }
        if (url.contains(GLENCOESOFTWARE_DOMAIN)) {
          return new CacheException.WarningOnly(GLENCOESOFTWARE_DOMAIN + " 502 Bad Gateway: " + GLENCOESOFTWARE_MESSAGE);
        }
        return new CacheException.RetryableNetworkException_2_10S("502 Bad Gateway");
        
      case 503:
        // http://d1gqps90bl2jsp.cloudfront.net/content/brain/137/12/3284/F7.medium.gif 503 Service Unavailable
        // http://www.bmj.com/content/351/bmj.h6193/related             503 Service Unavailable
        logger.debug2("503: " + url);
        if (url.contains(".cloudfront.net/")) {
          return new NoFailRetryableNetworkException_2_10S("503 Service Unavailable (non-fatal)");
        } else if (url.contains("bmj.") && url.endsWith("/related")) {
          return new NoFailRetryableNetworkException_2_10S("503 Service Unavailable (non-fatal)");
        }
        return new CacheException.RetryableNetworkException_2_10S("503 Service Unavailable");
        
      case 504:
        logger.debug2("504: " + url);
        if (url.contains("/content/")) {
          return new CacheException.RetryableNetworkException_2_10S("504 Gateway Time-out");
        }
        return new NoFailRetryableNetworkException_2_10S("504 Gateway Time-out (non-fatal)");
        
      case 520:
        // http://www.plantcell.org/content/29/2/202.full.pdf 520 Origin Error
        logger.debug2("520: " + url);
        Matcher nfmat = getNonFatal403Pattern().matcher(url);
        if (nfmat.find()) {
          return new CacheException.NoRetryDeadLinkException("520 Origin Error (non-fatal)");
        }

        return new CacheException.RetryableNetworkException_3_10S("520 Origin Error");
        
      case 524:
        // http://advances.sciencemag.org/content/3/1/e1601503.full.pdf
        logger.debug2("524: " + url);
        return new CacheException.RetryableNetworkException_3_10S("524: A timeout occurred"); // Cloudflare
        
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }
  
  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    if (ex instanceof ContentValidationException) {
      logger.debug3("Warning - not storing file " + url);
      // no store cache exception and continue
      return new org.lockss.util.urlconn.CacheException.NoStoreWarningOnly("ContentValidationException" + url);
    }
    if ((ex instanceof UnknownHostException) &&
        url.contains(GLENCOESOFTWARE_DOMAIN)) {
      logger.debug3("Warning - not storing " + GLENCOESOFTWARE_DOMAIN + " url: " + url);
      return new CacheException.NoStoreWarningOnly(GLENCOESOFTWARE_DOMAIN + " Unknown host: " + GLENCOESOFTWARE_MESSAGE);
    }
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
  }
  
  // Use a getter so that this can be overridden by a child plugin
  protected Pattern getNonFatal403Pattern() {
    return DEFAULT_NON_FATAL_403_PAT;
  }
}
