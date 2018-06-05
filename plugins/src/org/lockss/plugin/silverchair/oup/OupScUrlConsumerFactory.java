/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.oup;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
/**
 * @since 1.67.5 
 */
public class OupScUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(OupScUrlConsumerFactory.class);

  //https://academic.oup.com/ageing/issue-pdf/45/1/10919248
    //or
  //https://academic.oup.com/ageing/article-pdf/45/2/323/6650409/afw017.pdf
  // it doesn't really matter if it ends in PDF - if it redirects from a canonical to a temporary, we ought to consume
  public static final String DEL_URL = "/(issue|article)-pdf/";
  // will redirect to: 
  // https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/ageing/45/2/10.1093_ageing_afw017/3/afw017.pdf?Expires=
  public static final String DOC_URL = "/backfile/Content_public/Journal/[^?]+";
  public static final String DOC_ARGS = "\\?Expires=[^&]+&Signature=[^&]+&Key-Pair-Id=.+$";
  // or if through watermarking to:
  // https://watermark.silverchair.com/front_matter.pdf?token=AQECAHi208
  public static final String WMARK_URL = "watermark[.]silverchair[.]com/[^?]+";

  public static final String ORIG_FULLTEXT_STRING = DEL_URL;
  public static final String DEST_FULLTEXT_STRING = DOC_URL +  DOC_ARGS;

  protected static final Pattern origFullTextPat = Pattern.compile(ORIG_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern destFullTextPat = Pattern.compile(DEST_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern wMarkFullTextPat = Pattern.compile(WMARK_URL, Pattern.CASE_INSENSITIVE);
  
  public static Pattern getOrigPdfPattern() {
    return origFullTextPat;
  }
  
  public static Pattern getDestPdfPattern() {
    return destFullTextPat;
  }
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    return new OupScUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain (e.g. to support collecting and repairing
   * redirect chains that begin with fixed URLs but end with one-time URLs).
   * 
   * Many article PDFs will now use the crawler stable version url but the consumer
   * is still used for TOC pdfs or supplementary data so leave it in place for any 
   * redirection URLs that aren't normalized to a crawler version
   * </p>
   * 
   */
  public class OupScUrlConsumer extends SimpleUrlConsumer {

    public OupScUrlConsumer(CrawlerFacade facade,
        FetchedUrlData fud) {
      super(facade, fud);
    }
    
    @Override
    public void consume() throws IOException {
      if (shouldStoreAtOrigUrl()) {
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
     * @since 1.67.5
     */
    protected boolean shouldStoreAtOrigUrl() {
      boolean should =  fud.redirectUrls != null
          && fud.redirectUrls.size() == 1
          && fud.redirectUrls.get(0).equals(fud.fetchUrl)
          && getOrigPdfPattern().matcher(fud.origUrl).find()
          && (getDestPdfPattern().matcher(fud.fetchUrl).find() || wMarkFullTextPat.matcher(fud.fetchUrl).find());
      if (fud.redirectUrls != null) {
  	    log.debug3("OUP redirect " + fud.redirectUrls.size() + ": " + fud.redirectUrls.toString());
        log.debug3("OUP redirect: " + " " + fud.origUrl + " to " + fud.fetchUrl + " should consume?: " + should);
      }
      return should;
    }

  }

}
