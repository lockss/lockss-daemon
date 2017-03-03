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

package org.lockss.plugin.jstor;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class JstorCSUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(JstorCSUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a Jstor Current Scholarship UrlConsumer");
    return new JstorCSUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain 
   * 
   * The article PDFs are redirected to the a version of the url with the terms&conditions 
   * agreement already accepted.  So while the link will look like this:
   *   http://www.jstor.org/stable/pdf/10.5184/classicalj.110.2.0129.pdf
   * it will end up here:
   *   http://www.jstor.org/stable/pdf/10.5184/classicalj.110.2.0129.pdf?acceptTC=true&coverpage=false
   * and the redirect is a 302 so we need to follow it
   * 
   * Further testing reveals some variants:
   * 
   * - the originating url may look like any of these
   * http://www.jstor.org/stable/pdf/10.5184/foo.pdf
   * http://www.jstor.org/stable/pdf/foo.pdf
   * (and in the case where the article landing doesn't provide html)
   * http://www.jstor.org/stable/10.5184/foo
   * http://www.jstor.org/stable/foo
   * 
   * - the destination url may look like any of these and they do have /pdf/ and ".pdf"
   * http://www.jstor.org/stable/pdf/10.5184/foo.pdf?acceptTC=true&coverpage=false
   * http://www.jstor.org/stable/pdf/foo.pdf?acceptTC=true&coverpage=false
   * 
   * - with one additional potential hiccup, a two-hopper
   * from: http://www.jstor.org/stable/10.5184/foo
   * to: http://www.jstor.org/stable/foo
   * to: http://www.jstor.org/stable/pdf/foo.pdf?acceptTC=true&coverpage=false
   * (see Am Jrnl of Arch TOC: http://www.jstor.org/stable/10.3764/amerjarch.121.issue-1)
   * which requires allowing 1 or 2 hops so long as the originating and terminating
   * urls conform to the expected pattern
   *       
   * @since 1.68.0
   */
  public class JstorCSUrlConsumer extends SimpleUrlConsumer {

    // originating string must be highly flexible because it could take several forms
    // no worry about inadvertently catching issue, because the html wouldn't redirect
    public static final String ORIG_STRING = "/stable(/pdf)?(/[0-9.]+)?/[^/?&]+(\\.pdf)?$";
    // allow for with or without DOI prefix
    public static final String PDF_STRING = "/stable/pdf(/[0-9.]+)?/[^/?&]+\\.pdf";
    //?acceptTC=true&coverpage=false
    public static final String DEST_PDF_STRING = PDF_STRING + "\\?acceptTC=true&coverpage=false";

    // Image files are at none-deterministic URLs and are currently served as text/html
    // though they are PNG, or GIF, or...  and this is a problem for html hash filtering
    // so remove the content-type header if it is wrong and store it instead as 
    // x-lockss-served-content-type.
    // ex: http://www.jstor.org/stable/get_asset/10.5325/jmorahist.15.1.0001?path=long_hashy_string  
    private static final String UNIDENTIFIED_IMAGE_STRING = "/stable/get_asset/[0-9.]+/[^/?&]+\\?path=";
    private static final String LOCKSS_ORIG_CONTENT_TYPE = "x-lockss-served-content-type";
    private Pattern UnDefImagePat = Pattern.compile(UNIDENTIFIED_IMAGE_STRING, Pattern.CASE_INSENSITIVE);


    protected Pattern origFullTextPat = Pattern.compile(ORIG_STRING, Pattern.CASE_INSENSITIVE);
    protected Pattern destFullTextPat = Pattern.compile(DEST_PDF_STRING, Pattern.CASE_INSENSITIVE);

    public JstorCSUrlConsumer(CrawlerFacade facade,
        FetchedUrlData fud) {
      super(facade, fud);
    }

    @Override
    public void consume() throws IOException {
      if (shouldStoreRedirectsAtOrigUrl()) {
        storeAtOrigUrl();
      } else if (imageWithIncorrectContentType()) {
        correctContentTypeHeader();
      }
      super.consume();
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
    protected boolean shouldStoreRedirectsAtOrigUrl() {
      // allow for a two hop in some cases so long as originator and terminator are correct
      if(log.isDebug3() && fud.redirectUrls != null) {
        log.debug3("FUD: " + fud.toString());
      }
      boolean should =  fud.redirectUrls != null
          && ( (fud.redirectUrls.size() == 1 && fud.redirectUrls.get(0).equals(fud.fetchUrl))
              || (fud.redirectUrls.size() == 2 && fud.redirectUrls.get(1).equals(fud.fetchUrl)) ) 
              && destFullTextPat.matcher(fud.fetchUrl).find()
              && origFullTextPat.matcher(fud.origUrl).find();
      return should;
    }

    // If the URL matches the pattern for an in-line image
    // and it has content-type set 
    // and the content type is text/html....
    protected boolean imageWithIncorrectContentType() {
      boolean isWrong = (UnDefImagePat.matcher(fud.origUrl).find() &&
          (fud.headers.get(CachedUrl.PROPERTY_CONTENT_TYPE) != null) &&
          ((String)fud.headers.get(CachedUrl.PROPERTY_CONTENT_TYPE)).contains("text/html"));
      log.debug3("is image original content type incorrect? " + isWrong);
      return isWrong;
    }    

    // We only go in to this routine if we know the content-type is set (not null) 
    private void correctContentTypeHeader() {
      String pub_ctype = (String)fud.headers.get(CachedUrl.PROPERTY_CONTENT_TYPE);
      fud.headers.remove(CachedUrl.PROPERTY_CONTENT_TYPE);
      fud.headers.put(LOCKSS_ORIG_CONTENT_TYPE, pub_ctype);
    }

  }

}
