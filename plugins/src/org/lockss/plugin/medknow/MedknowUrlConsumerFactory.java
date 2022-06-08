/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.medknow;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
/**
 * @since 1.68
 * Medknow redirects some content through a showCaptcha after a certain number of 
 * hits.  They seem to have turned it off at their side for PDF (?) but we are still
 * hitting it for other URLs so swallowing it in case it gets turned back on
 * 
 * Medknow also now redirects PDF to downloadPdf
 *     article.asp?issn=0257-7941;year=2000;volume=19;issue=3;spage=123;epage=129;aulast=Jolly;type=2
 * downloadpdf.asp?issn=0257-7941;year=2000;volume=19;issue=3;spage=123;epage=129;aulast=Jolly;type=2
 * 
 * Medknow seems to have updated some or all sites to no longer have /downloadpdf.asp links
 * only article.asp links that redirect and get stored AtOrigUrl
 * 
 */
public class MedknowUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(MedknowUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a medknow UrlConsumer");
    return new MedknowUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain (e.g. to support collecting and repairing
   * redirect chains that begin with fixed URLs but go through showCaptcha).
   * @since 1.67.5
   */
  public class MedknowUrlConsumer extends SimpleUrlConsumer {

	    public static final String CAPTCHA_URL = "/showcaptcha\\.asp";
	    public static final String DOWNLOADPDF_URL = "/downloadpdf\\.asp";
 
    protected Pattern captchapat = Pattern.compile(CAPTCHA_URL, Pattern.CASE_INSENSITIVE);
    protected Pattern downloadpdfpat = Pattern.compile(DOWNLOADPDF_URL, Pattern.CASE_INSENSITIVE);

    public MedknowUrlConsumer(CrawlerFacade facade,
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
          && (captchapat.matcher(fud.redirectUrls.get(0)).find() ||
        		  (downloadpdfpat.matcher(fud.fetchUrl).find()));
      if (fud.redirectUrls != null) {
    	    log.debug3("MED redirect " + fud.redirectUrls.size() + ": " + fud.redirectUrls.toString());
        log.debug3("MED redirect: " + " " + fud.origUrl + " to " + fud.fetchUrl + " should consume?: " + should);
      }
      return should;
    }

  }

}
