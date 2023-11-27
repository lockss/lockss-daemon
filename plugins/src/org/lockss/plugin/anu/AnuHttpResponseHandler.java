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

package org.lockss.plugin.anu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;


public class AnuHttpResponseHandler implements CacheResultHandler {
  
  private static final Logger logger = Logger.getLogger(AnuHttpResponseHandler.class);
  
  protected static final Pattern NON_FATAL_PAT = 
      Pattern.compile("[.](gif|jpg)$");
  
  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to AnuHttpResponseHandler.init()");
  }
  
  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    
    Matcher mat = NON_FATAL_PAT.matcher(url);
    switch (responseCode) {
      case 403:
        logger.debug2("403: " + url);
        // Do not fail the crawl for 403 errors at URLs like the one below should not be fatal
        if (mat.find()) {
          return new CacheException.NoRetryDeadLinkException("403 Forbidden (non-fatal)");
        }
        return new CacheException.RetrySameUrlException("403 Forbidden errror");
        
      case 500:
        logger.debug2("500: " + url);
        if (mat.find()) {
          return new CacheException.NoRetryDeadLinkException("500 Internal Server Error (non-fatal)");
        }
        return new CacheException.RetrySameUrlException("500 Internal Server Error");
        
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
