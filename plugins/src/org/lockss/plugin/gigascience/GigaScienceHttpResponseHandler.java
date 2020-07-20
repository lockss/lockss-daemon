/*
 * $Id$
 */

/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.gigascience;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;


public class GigaScienceHttpResponseHandler implements CacheResultHandler {
  
  protected static Logger log = Logger.getLogger("GigaScienceHttpResponseHandler");

  public void init(CacheResultMap crmap) {
    log.warning("Unexpected call to init()");
    throw new UnsupportedOperationException();
  }

  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    log.debug2(url);
    switch (responseCode) {
      case 500:
        log.debug2("500 error: "+url);
        // don't completely fail if unable to retrieve a dataset
        if (url.matches(".*/api/dataset?doi=\\d+$") ) {
          return new CacheException.NoRetryDeadLinkException("500 Internal Server Error (non-fatal)");
        }
        return new CacheException.RetrySameUrlException("500 Internal Server Error");
      case 503:
        log.debug2("503 error: "+url);
        // retry "503 backend read error" and 503 Service Unavailable
        // as most will resolve eventually
        return new CacheException.RetryableNetworkException_3_30S();
      default:
        log.debug2("default");
        throw new UnsupportedOperationException();
    }
  }

  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    log.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException();
  }
  
}
