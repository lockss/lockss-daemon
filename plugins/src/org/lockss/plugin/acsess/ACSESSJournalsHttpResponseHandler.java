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

package org.lockss.plugin.acsess;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/*
 * A problem with supplementary data of type ".txt" - in two AUs they return
 * a 403 Forbidden. Only 1 file of each in Plant Genome V6 and Plant Genome V7
 * eg:
 * https://dl.sciencesocieties.org/publications/tpg/supplements/8/plantgenome2014.09.0046-supplement1.txt
 * 
 */
public class ACSESSJournalsHttpResponseHandler implements CacheResultHandler {
    

  protected static final Pattern NON_FATAL_PAT = 
      Pattern.compile("publications/[^/]+/supplements/[^/]+/[^/]+\\.txt");
    
  private static final Logger logger = Logger.getLogger(ACSESSJournalsHttpResponseHandler.class);

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to ACSESSJournalsHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    switch (responseCode) {
      case 403:
        // Do not fail the crawl for 403 errors at URLs like the one below should not be fatal
        Matcher mat = NON_FATAL_PAT.matcher(url);
        if (mat.find()) {
          return new CacheException.NoRetryDeadLinkException("403 Forbidden (non-fatal)");
        } else {
          return new CacheException.RetrySameUrlException("403 Forbidden errror");
        }
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