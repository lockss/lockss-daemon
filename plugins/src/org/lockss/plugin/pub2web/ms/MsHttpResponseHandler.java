/*
 *
 *  * $Id: $
 *
 *
 *
 * Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 *
 * /
 */

package org.lockss.plugin.pub2web.ms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;

public class MsHttpResponseHandler implements CacheResultHandler {
  private static final Logger log = Logger.getLogger(MsHttpResponseHandler.class);
  
  protected static final Pattern NON_FATAL_PAT = Pattern.compile("/supp-?data");
  
  @Override
  @Deprecated
  public void init(final CacheResultMap map) throws PluginException {
    log.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to MsHtmlHttpResponseHandler.init()");
  }
  
  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, final int code) throws PluginException {
    log.debug2(code + ": " + url);
    switch (code) {
      case 500:
        Matcher mat = NON_FATAL_PAT.matcher(url);
        //Do not fail the crawl for 500 errors at URLs like the one below should not be fatal
        if (mat.find()) {
          return new CacheException.NoRetryDeadLinkException("500 Internal server (non-fatal)");
        } else {
          return new CacheException.RetrySameUrlException("500 Internal server error");
        }
      default:
        log.warning("Unexpected responseCode (" + code + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + code + ")");
    }
  }
  
  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, final Exception ex)
    throws PluginException {
    
    // we should only get in her cases that we specifically map, report and throw
    log.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException("Unexpected exception ", ex);
  }
  
}
