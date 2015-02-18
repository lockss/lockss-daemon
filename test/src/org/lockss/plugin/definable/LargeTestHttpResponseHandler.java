/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.definable;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.*;

public class LargeTestHttpResponseHandler implements CacheResultHandler {

  protected static Logger log = Logger.getLogger("LargeTestHttpResponseHandler");

  public void init(CacheResultMap crmap) {
    log.warning("Unexpected call to init()");
    throw new UnsupportedOperationException();
  }

  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode)
      throws PluginException {
    log.debug2(url);
    switch (responseCode) {
      case 522:
        return new CacheException.RetryDeadLinkException("522 from handler");
      case 500:
        log.debug2("500");
        if (url.contains("cgi/eletters/")) {
          return new CacheException.NoRetryDeadLinkException("500 Internal Server Error (non-fatal)");
        }
        else {
          return new CacheException.RetrySameUrlException("500 Internal Server Error");
        }
      default:
        log.debug2("default");
        throw new UnsupportedOperationException();
    }
  }

  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex)
      throws PluginException {
    return new CacheException.RetryableNetworkException_2(ex);
  }
}
