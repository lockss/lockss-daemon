/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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
