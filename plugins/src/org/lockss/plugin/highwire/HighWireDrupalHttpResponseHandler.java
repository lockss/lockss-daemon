/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;


public class HighWireDrupalHttpResponseHandler implements CacheResultHandler {
  
  private static final Logger logger = Logger.getLogger(HighWireDrupalHttpResponseHandler.class);
  
  /*examples:
   * http://science.sciencemag.org/highwire/filestream/609439/field_highwire_adjunct_files/0/D%27Angelo.MovieS1.mov 
   * http://www.bloodjournal.org/highwire/filestream/322606/field_highwire_adjunct_files/0/FigureS1.jpg 
   * A child can change this by extending this class and overriding the getter for the 403 pattern
   */
  protected static final Pattern DEFAULT_NON_FATAL_403_PAT = 
      Pattern.compile("/filestream/[^/]+/field_highwire_adjunct_files/");  
  
  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to HighWireDrupalHttpResponseHandler.init()");
  }
  
  public static final class NoFailRetryableNetworkException_2_10S
  extends CacheException.RetryableNetworkException_2_10S {
    
    private static final long serialVersionUID = 1L;
    
    public NoFailRetryableNetworkException_2_10S(String message) {
      super(message);
    }
    
    @Override
    protected void setAttributes() {
      super.setAttributes();
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }
  
  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    switch (responseCode) {
      case 403:
        logger.debug2("403 - pattern is " + getNonFatal403Pattern().toString());
        Matcher fmat = getNonFatal403Pattern().matcher(url);
        if (fmat.find()) {
          return new CacheException.NoRetryDeadLinkException("403 Foridden (non-fatal)");
        }
        return new CacheException.RetrySameUrlException("403 Forbidden");
        
      case 500:
        logger.debug2("500: " + url);
        if (url.endsWith("_manifest.html") || 
            url.endsWith(".toc")) {
          return new CacheException.RetrySameUrlException("500 Internal Server Error");
        }
        return new CacheException.RetryDeadLinkException("500 Internal Server Error (non-fatal)");
        
      case 502:
        logger.debug2("502: " + url);
        if (url.endsWith(".index-by-author")) {
          return new NoFailRetryableNetworkException_2_10S("502 Bad Gateway (non-fatal)");
        }
        return new CacheException.RetryableNetworkException_2_10S("502 Bad Gateway");
        
      case 503:
        // http://d1gqps90bl2jsp.cloudfront.net/content/brain/137/12/3284/F7.medium.gif 503 Service Unavailable
        // http://www.bmj.com/content/351/bmj.h6193/related             503 Service Unavailable
        logger.debug2("503: " + url);
        if (url.contains(".cloudfront.net/")) {
          return new NoFailRetryableNetworkException_2_10S("503 Service Unavailable (non-fatal)");
        } else if (url.contains("bmj.") && url.endsWith("/related")) {
          return new NoFailRetryableNetworkException_2_10S("503 Service Unavailable (non-fatal)");
        }
        return new CacheException.RetryableNetworkException_2_10S("503 Service Unavailable");
        
      case 504:
        logger.debug2("504: " + url);
        if (url.contains("/content/")) {
          return new CacheException.RetryableNetworkException_2_10S("504 Gateway Time-out");
        }
        return new NoFailRetryableNetworkException_2_10S("504 Gateway Time-out (non-fatal)");
        
      case 520:
        // http://www.plantcell.org/content/29/2/202.full.pdf 520 Origin Error
        logger.debug2("520: " + url);
        return new CacheException.RetryableNetworkException_3_10S("520 Origin Error");
        
      case 524:
        // http://advances.sciencemag.org/content/3/1/e1601503.full.pdf
        logger.debug2("524: " + url);
        return new CacheException.RetryableNetworkException_3_10S("524: A timeout occurred"); // Cloudflare
        
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }
  
  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    if (ex instanceof ContentValidationException) {
      logger.debug3("Warning - not storing file " + url);
      // no store cache exception and continue
      return new org.lockss.util.urlconn.CacheException.NoStoreWarningOnly("ContentValidationException" + url);
    } 
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
  }
  
  // Use a getter so that this can be overridden by a child plugin
  protected Pattern getNonFatal403Pattern() {
    return DEFAULT_NON_FATAL_403_PAT;
  }
}
