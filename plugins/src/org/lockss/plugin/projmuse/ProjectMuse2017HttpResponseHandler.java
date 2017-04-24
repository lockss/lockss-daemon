/*
 *
 *  * $Id:$
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

package org.lockss.plugin.projmuse
;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;
import org.lockss.util.Logger;

import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;
import org.lockss.util.urlconn.CacheSuccess;

public class ProjectMuse2017HttpResponseHandler implements CacheResultHandler {
  private static final Logger logger = Logger.getLogger(ProjectMuse2017HttpResponseHandler.class);

  @Override
  public void init(final CacheResultMap map) throws PluginException {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to ProjectMuse2017HttpResponseHandler.init()");
    
  }
  
  // currently this is not called on
  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, int responseCode) throws PluginException {
    logger.debug(responseCode + ": " + url);
    
    logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
    throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
  }
  
  
  @Override
  public CacheException handleResult(final ArchivalUnit au, final String url, final Exception ex)
    throws PluginException {
    logger.warning(ex.getMessage() + ": " + url);
    
    if (ex instanceof ContentValidationException.WrongLength) {
      logger.debug3("Ignoring Wrong length - storing file");
      // ignore and continue
      return new CacheSuccess();
    }
    if (ex instanceof ContentValidationException) {
      logger.debug3("Not storing file");
      return new CacheException.NoStoreWarningOnly();
    }
    
    if (ex instanceof javax.net.ssl.SSLHandshakeException) {
      logger.debug3("Retrying SSLHandshakeException", ex);
      // retry 2 times
      return new CacheException.RetryableNetworkException_2_10S();
    }
    
    // we should only get in her cases that we specifically map...be very unhappy
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException();

  }

}
