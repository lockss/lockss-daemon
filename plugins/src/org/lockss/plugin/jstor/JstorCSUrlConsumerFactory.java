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

package org.lockss.plugin.jstor;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.plugin.base.HttpToHttpsUrlConsumerFactory;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;
/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class JstorCSUrlConsumerFactory extends HttpToHttpsUrlConsumerFactory {
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
  public class JstorCSUrlConsumer extends HttpToHttpsUrlConsumer {

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
          && ((fud.redirectUrls.size() == 1 && fud.redirectUrls.get(0).equals(fud.fetchUrl))
              || (fud.redirectUrls.size() == 2 && fud.redirectUrls.get(1).equals(fud.fetchUrl))
              || (UrlUtil.isHttpUrl(fud.origUrl) && UrlUtil.isHttpsUrl(fud.fetchUrl)))
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
      fud.headers.remove("Content-Type"); //content-type
      fud.headers.remove(CachedUrl.PROPERTY_CONTENT_TYPE); //x-lockss-content-type
      fud.headers.put(LOCKSS_ORIG_CONTENT_TYPE, pub_ctype);
    }

  }

}
