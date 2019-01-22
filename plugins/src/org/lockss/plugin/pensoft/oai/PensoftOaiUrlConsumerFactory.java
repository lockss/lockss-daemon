/*
 * $Id$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pensoft.oai;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
/**
 * @since 1.68
 * Pensoft redirects PDF to generate link
 * https://subtbiol.pensoft.net/article/23364/download/pdf
 * https://subtbiol.pensoft.net/lib/ajax_srv/generate_pdf.php?document_id=23364&readonly_preview=1
 * 
 */
public class PensoftOaiUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(PensoftOaiUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a Pensoft UrlConsumer");
    return new PensoftOaiUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain (e.g. to support collecting and repairing
   * redirect chains that begin with fixed URLs but go through generation).
   * @since 1.67.5
   */
  public class PensoftOaiUrlConsumer extends SimpleUrlConsumer {

    public static final String DOWNLOADPDF_URL = "article/[^/]+/download/pdf/?$";
    public static final String GENERATE_URL = "/lib/ajax_srv/generate_pdf[.]php[?]document_id=";

    protected Pattern DOWNLOADPDF_PAT = Pattern.compile(DOWNLOADPDF_URL, Pattern.CASE_INSENSITIVE);
    protected Pattern GENERATE_PAT = Pattern.compile(GENERATE_URL, Pattern.CASE_INSENSITIVE);

    public PensoftOaiUrlConsumer(CrawlerFacade facade,
        FetchedUrlData fud) {
      super(facade, fud);
    }

    @Override
    public void consume() throws IOException {
      if (fud.redirectUrls != null && shouldStoreRedirectsAtOrigUrl()) {
        storeAtOrigUrl();
      }
      super.consume();
    }

    protected boolean shouldStoreRedirectsAtOrigUrl() {
      boolean should =
          fud.redirectUrls != null &&
          fud.redirectUrls.size() >= 1 &&
          DOWNLOADPDF_PAT.matcher(fud.origUrl).find() &&
          GENERATE_PAT.matcher(fud.fetchUrl).find();
      return should;
    }
  }
}
