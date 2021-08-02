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

package org.lockss.plugin.highwire.elife;

import org.lockss.plugin.*;
import org.lockss.plugin.highwire.HighWireJCoreHttpResponseHandler;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;


public class ELifeJCoreHttpResponseHandler extends HighWireJCoreHttpResponseHandler {
  
  protected static Logger logger = Logger.getLogger(ELifeJCoreHttpResponseHandler.class);
  
  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to ELifeDrupalHttpResponseHandler.init()");
  }
  
  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug3(url);
    switch (responseCode) {
      case 403:
        // no example as HighWire did 'fix' the problem url
        logger.debug3("403");
        if (url.contains("/download")) {
          return new CacheException.RetryDeadLinkException("403 Forbidden (non-fatal)");
        }
        return new CacheException.RetrySameUrlException("403 Forbidden");
        
      case 500:
        // ex. http://elifesciences.org/highwire/citation/8378/ris
        logger.debug3("500");
        if (url.contains("lockss-manifest/")) {
          return new CacheException.RetrySameUrlException("500 Internal Server Error");
        }
        return super.handleResult(au, url, responseCode);
        
      case 504:
        // JIC: parent handles, as we get Gateway Timeout often enough
        return super.handleResult(au, url, responseCode);
        
      default:
        return super.handleResult(au, url, responseCode);
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
