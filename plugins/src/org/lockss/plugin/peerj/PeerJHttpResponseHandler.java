/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.peerj;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;


public class PeerJHttpResponseHandler implements CacheResultHandler {
  
  private static final Logger logger = Logger.getLogger(PeerJHttpResponseHandler.class);
  
  public static final class RetryableNetworkException_2_10M
  extends CacheException.RetryableNetworkException_2_5M {
    
    private static final long serialVersionUID = 1L;
    
    public RetryableNetworkException_2_10M() {
      super();
    }
    
    public RetryableNetworkException_2_10M(String message) {
      super(message);
    }
    
    @Override
    public long getRetryDelay() {
      // wait twice as long
      return super.getRetryDelay() * 2L;
    }
    
  }
  
  
  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to PeerJHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    switch (responseCode) {
      case 403:
        logger.debug2("403");
        if (url.contains("/articles/") ||
            url.contains("archives/?year=")
            ) {
          return new CacheException.PermissionException("403 Forbidden");
        }
        
        return new CacheException.NoRetryDeadLinkException("403 Forbidden (non-fatal)");
      case 429:
        logger.debug2("429");
        return new RetryableNetworkException_2_10M("429 Too Many Requests");
      case 500:
        logger.debug2("500");
        if (url.contains("/articles/index.html") ||
            url.contains("archives/?year=")
            ) {
          return new CacheException.RetrySameUrlException("500 Internal Server Error");
        }
        
        return new CacheException.RetryDeadLinkException("500 Internal Server Error (non-fatal)");
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