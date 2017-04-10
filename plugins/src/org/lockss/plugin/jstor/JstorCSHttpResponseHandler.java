/*
 * $Id:$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.jstor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/*
 * Ignore 503 errors on certain types of files - they're really more of a 404 
 */
public class JstorCSHttpResponseHandler implements CacheResultHandler {
    
  //http://www.jstor.org/px/client/main.min.js,"503 Service Unavailable

  protected static final Pattern NON_FATAL_PAT = 
      Pattern.compile("\\.js$");
    
  private static final Logger logger = Logger.getLogger(JstorCSHttpResponseHandler.class);

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to JstorCSHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    switch (responseCode) {
      case 503:
        // Do not fail the crawl for 503 errors at URLs like the one below should not be fatal
        Matcher mat = NON_FATAL_PAT.matcher(url);
        if (mat.find()) {
          return new CacheException.NoRetryDeadLinkException("503 Service Unavailable (non-fatal)");
        } else {
          return new CacheException.RetrySameUrlException("503 Service Unavailable");
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