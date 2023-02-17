/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
  
  /*
   * http://www.swjpcc.com/storage/pdf-version-of-articles/volume-3/SWJPCC%20029-11.pdf
   * redirects to:
   * http://static1.1.sqspcdn.com/static/f/654826/13862046/1314373579613/SWJPCC+029-11.pdf?token=PG%2Fy2HQCXRvnMJZG0cSC%2FZu3c1M%3D
   */
  
  // This could be many suffix - pdf, image, exel, etc - require that it end with a dot-suffix
  //protected static final String ORIG_STRING = "/storage/(manuscripts|manuscript-lists|pdf-version-of-articles|website-stuff)/.+[.][a-z]+$";
  // Don't be specific - if it redirects to static/f just consume it
  protected static final String ORIG_STRING = "/storage/[^/]+/.+[.][a-z]+$";
  // Will have the same filename-dot-suffix but probably not worth doing an actual comparison
  protected static final String DEST_STRING = "/static/f/[0-9]+/.+[.][a-z]+(\\?token=[^/?]+)?$";
  
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

  public static Pattern getOrigPattern() {
    // TODO Auto-generated method stub
    return origPat;
  }

  public static Pattern getDestPattern() {
    // TODO Auto-generated method stub
    return destPat;
  }
  

}
