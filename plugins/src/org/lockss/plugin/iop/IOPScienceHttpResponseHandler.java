/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

public class IOPScienceHttpResponseHandler implements CacheResultHandler {
  
  protected static Logger logger = Logger.getLogger(IOPScienceHttpResponseHandler.class);
  // Want to match on URLs that contain another host in error (relative should have been absolute)
  // these URLs are invalid and should not cause fatal errors
  // Host string is one alpha + 0 or more chars dot 2 or more chars dot 2 or more chars (excluding slash)
  protected static Pattern PAT_MAL_URL =
      Pattern.compile("^https?://[^/]+/.+/[a-z]+[^./]*[.][^/]{2,}[.][^/]{2,}");
  
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException();
  }
  
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    Matcher mat = PAT_MAL_URL.matcher(url);
    switch (responseCode) {
      case 403:
        logger.debug2("403");
        if (mat.matches()) {
          // http://iopscience.iop.org/0143-0807/35/4/045020/www.imcce.fr/langues/...
          return new CacheException.MalformedURLException("403 Forbidden (non-fatal)");
        }
        else {
          return new CacheException.RetrySameUrlException("403 Forbidden");
        }
      case 503:
        logger.debug2("503");
        if (mat.matches()) {
          // http://iopscience.iop.org/0264-9381/31/21/215007/www.bu.edu.eg
          return new CacheException.MalformedURLException("503 Service Unavailable Error (non-fatal)");
        }
        else {
          return new CacheException.RetrySameUrlException("503 Service Unavailable Error");
        }
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }
  
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException();
  }
  
}
