/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;

/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class HighWireDrupalUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(HighWireDrupalUrlConsumerFactory.class);
  
  // Looking for 2 patterns in the original URL, one for PDFs and the other for TOCs
  protected static final String ORIG_STRING = "/content(/.+/[^/]+[.]full[.]pdf|/.+[.]toc)$";
  // Should match either PDF URL that has an added sub-directory after /content/ or a TOC URL without the .toc extension
  protected static final String DEST_STRING = "/content/([^/]+)(/.+/[^/]+[.]full[.]pdf|/[^/.]+)$";
  
  protected static final Pattern origPat = Pattern.compile(ORIG_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern destPat = Pattern.compile(DEST_STRING, Pattern.CASE_INSENSITIVE);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a UrlConsumer");
    return new HighWireDrupalUrlConsumer(facade, fud);
  }
  
  public static Pattern getOrigPattern() {
    return origPat;
  }
  
  public static Pattern getDestPattern() {
    return destPat;
  }
  
  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain 
   * 
   * The article PDFs are redirected to a url with 
   * So while the link will look like this:
   *   http://www.bmj.com/content/349/bmj.g7460.full.pdf
   * it will end up here:
   *   http://www.bmj.com/content/bmj/349/bmj.g7460.full.pdf
   * 
   * Another example is
   *   http://www.bmj.com/content/350/7989.toc
   * redirects to
   *   http://www.bmj.com/content/350/7989
   * </p>
   * 
   * @since 1.68.0
   */
  public class HighWireDrupalUrlConsumer extends HttpToHttpsUrlConsumer {
    
    protected Configuration auconfig;
    
    public HighWireDrupalUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
      auconfig = au.getConfiguration();
    }
    
    /**
     * <p>
     * Determines if a particular redirect chain should cause content to be stored
     * only at the origin URL ({@link FetchedUrlData#origUrl}).
     * </p>
     * 
     * @return True if and only if the fetched URL data represents a particular
     *         redirect chain that should cause content to be stored only at the
     *         origin URL.
     */
    @Override
    public boolean shouldStoreAtOrigUrl() {
      boolean should = super.shouldStoreAtOrigUrl();
      if (!should) {
        Matcher destMat = null;
        should = (fud.redirectUrls != null
            && fud.redirectUrls.size() == 1
            && fud.redirectUrls.get(0).equals(fud.fetchUrl)
            && (destMat = destPat.matcher(fud.fetchUrl)).find()
            && origPat.matcher(fud.origUrl).find());
        if (should) {
          if (fud.origUrl.endsWith(".toc")) {
            // if the origUrl is a TOC, check that the first group of the dest {/content/(<vol>)(/<iss>)} matches the AU volume
            should = destMat.group(1).equals(auconfig.get("volume_name"));
          } else if (fud.origUrl.endsWith(".pdf")) {
            // if the origUrl is a PDF, check that the second group of the dest {/content/(sub-dir)(/<vol>/<iss>/<article_id>} ends with .pdf
            should = destMat.group(2).endsWith(".pdf");
          } else {
            log.warning("Huh! Should not happen: " + fud.fetchUrl + " " + fud.origUrl);
          }
        }
      }
      return should;
    }
    
  }
  
}
