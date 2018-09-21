/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.swjpcc;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
/**
 * @since 1.68
 * SWJPCC pdfs redirect to the storage CDN.
 * http://www.swjpcc.com/storage/manuscripts/volume-17/issue-1-july-2018/085-18/085-18.pdf
 * goes to:
 * http://static1.1.sqspcdn.com/static/f/654826/27940362/1530586028657/085-18.pdf?token=xyzq
 * 
 * or 
 * http://www.swjpcc.com/storage/pdf-version-of-articles/volume-2/010-11.pdf
 * 
 * or
 * http://www.swjpcc.com/storage/manuscripts/volume-5/swjpcc-086-12/SWJPCC%20086-12%20Figure%201.jpg
 * goes to:
 * http://static1.1.sqspcdn.com/static/f/654826/20910580/1352391929677/SWJPCC+086-12+Figure+1.jpg?token=xyzq
 * note the filename gets normalized or unnormalized so not exactly the same 
 * 
 * The token doesn't really matter and could be stripped off but since we're storing at the original url 
 * this doesn't really matter
 */
public class SwjpccUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(SwjpccUrlConsumerFactory.class);
  
  // Looking for PDF pattern in the original URL
  protected static final String ORIG_STRING = "/storage/(manuscripts|pdf-version-of-articles)/.+/[^/]+[.](pdf|jpe?g)$";
  // Probably shouldn't limit this to just jpeg and pdf - need to rework after viewing collection
  protected static final String DEST_STRING = "/static/.+/[^/]+[.](pdf|jpe?g)(\\?token=[^/?]+)?$";
  
  protected static final Pattern origPat = Pattern.compile(ORIG_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern destPat = Pattern.compile(DEST_STRING, Pattern.CASE_INSENSITIVE);
    

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a SWJPCC UrlConsumer");
    return new SwjpccUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain (e.g. to support collecting and repairing
   * redirect chains.
   * @since 1.67.5
   */
  public class SwjpccUrlConsumer extends SimpleUrlConsumer {


    public SwjpccUrlConsumer(CrawlerFacade facade,
        FetchedUrlData fud) {
      super(facade, fud);
    }

    @Override
    public void consume() throws IOException {
        if (shouldStoreRedirectsAtOrigUrl()) {
          storeAtOrigUrl();
        }
        super.consume();
      }


    protected boolean shouldStoreRedirectsAtOrigUrl() {
      boolean should =  fud.redirectUrls != null
          && fud.redirectUrls.size() >= 1
              && (destPat.matcher(fud.fetchUrl).find()
              && origPat.matcher(fud.origUrl).find());
      if (fud.redirectUrls != null) {
    	    log.debug3("SWJPCC redirect " + fud.redirectUrls.size() + ": " + fud.redirectUrls.toString());
        log.debug3("SWJPCC redirect: " + " " + fud.origUrl + " to " + fud.fetchUrl + " should consume?: " + should);
      }
      return should;
    }

  }

}
