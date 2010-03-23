/*
 * $Id: HighWirePressHttpResponseHandler.java,v 1.2 2010-03-23 22:28:04 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.*;

public class HighWirePressHttpResponseHandler implements CacheResultHandler {

  protected static Logger logger = Logger.getLogger("HighWirePressHttpResponseHandler");

  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException();
  }

  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode)
      throws PluginException {
    logger.debug2(url);
    switch (responseCode) {
      case 404:
        logger.debug3("404");
        return new CacheException.RetryDeadLinkException("404 Not Found");
      case 500:
        logger.debug2("500");
        if (url.contains("cgi/eletters/")) {
          return new CacheException.NoRetryDeadLinkException("500 Internal Server Error (non-fatal)");
        }
        else {
          return new CacheException.RetrySameUrlException("500 Internal Server Error");
        }
      default:
        logger.debug2("default");
        throw new UnsupportedOperationException();
    }
  }

  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex)
      throws PluginException {
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException();
  }

}
