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
   * </p>
   * ALSO - JSTOR doesn't allow the crawler to get to the article landing html and instead
   * directs the article landing page
   * http://www.jstor.org/stable/10.2972/hesperia.84.4.bm
   * to redirect to the PDF version of the file, with it's arguments
   * Store this, even though it is a little redundant, at the original URL for the purposes of 
   *       
   * @since 1.68.0
   */
  public class JstorCSUrlConsumer extends SimpleUrlConsumer {

    //  original pdf will look like this: 
    // ...stable/pdf/10.5184/classicalj.110.2.0129.pdf
    // or (no /pdf/ nor .pdf) for an article landing page 
    // ...stable/10.2972/hesperia.84.4.bm
   // an issue would be similar 
    // stable/10.2972/hesperia.84.issue-4 
    // but it wouldn't redirect, so no need to exclude
    public static final String PDF_STRING = "/stable(/pdf)?/[^/]+/[^/?&]+(\\.pdf)?";
    public static final String ORIG_PDF_STRING = PDF_STRING + "$"; // no arguments
    //?acceptTC=true&coverpage=false
    public static final String DEST_PDF_STRING = PDF_STRING + "\\?acceptTC=true&coverpage=false";

 
    protected Pattern origFullTextPat = Pattern.compile(ORIG_PDF_STRING, Pattern.CASE_INSENSITIVE);
    protected Pattern destFullTextPat = Pattern.compile(DEST_PDF_STRING, Pattern.CASE_INSENSITIVE);

    public JstorCSUrlConsumer(CrawlerFacade facade,
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
      boolean should =  fud.redirectUrls != null
          && fud.redirectUrls.size() == 1
          && fud.redirectUrls.get(0).equals(fud.fetchUrl)
          && destFullTextPat.matcher(fud.fetchUrl).find()
          && origFullTextPat.matcher(fud.origUrl).find();
      return should;
    }

  }

}
