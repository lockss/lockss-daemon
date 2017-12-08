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

package org.lockss.plugin.silverchair;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
/**
 * @since 1.68
 */
public class ScUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(ScUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a Silverchair UrlConsumer");
    return new ScUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain (e.g. to support collecting and repairing
   * redirect chains that begin with fixed URLs but end with one-time URLs).
   * American Speech Language Hearing uses Silverchair.
   * PDF links start at:
   *   http://lshss.pubs.asha.org/data/Journals/LSHSS/935447/LSHSS_47_3_181.pdf
   *   http://lshss.pubs.asha.org/data/Journals/LSHSS/935254/LSHSS-15-0011edwards_SupplAppB.pdf
   * and redirect to:
   *    pdfaccess.ashx?url=/data/journals/lshss/935254/lshss_47_2_123.pdf
   *    pdfaccess.ashx?url=/data/journals/lshss/935254/lshss-15-0011edwards_supplappb.pdf
   * store the content at the original url (which is also identified as the 
   * <meta name="citation_pdf_url" 
   *      content="http://lshss.pubs.asha.org/data/journals/lshss/935447/lshss_47_3_181.pdf" /> 
   * 
   * @since 1.67.5
   */
  public class ScUrlConsumer extends SimpleUrlConsumer {

    public static final String CANON_PDF_URL = "data/journals/[^&?]+\\.pdf$";
    public static final String TO_PDF_URL = ".ashx\\?url=/data/journals/[^&?]+\\.pdf$";
 
    protected Pattern canonPdfPat = Pattern.compile(CANON_PDF_URL, Pattern.CASE_INSENSITIVE);
    protected Pattern destPdfPat = Pattern.compile(TO_PDF_URL, Pattern.CASE_INSENSITIVE);

    public ScUrlConsumer(CrawlerFacade facade,
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
          && fud.redirectUrls.size() == 1
          && fud.redirectUrls.get(0).equals(fud.fetchUrl)
          && destPdfPat.matcher(fud.fetchUrl).find()
          && canonPdfPat.matcher(fud.origUrl).find();
      if (!should) {
        log.debug3("NOT swallowing this redirect");
      }
      return should;
    }

  }

}
