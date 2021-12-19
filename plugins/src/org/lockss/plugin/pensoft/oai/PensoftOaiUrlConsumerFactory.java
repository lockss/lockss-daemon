/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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
 * May 2019 - seeing additional PDF pattern of 
 * https://biodiscovery.pensoft.net/article/8964/download/pdf/283658
 * which redirects through 
 * /lib/ajax_srv/generate_pdf.php?document_id=8964&readonly_preview=1&file_id=283658
 * (the file_id for a normal PDF is 0)
 *
 * In Dec/2021, the final destination of PDF moved at the following steps:
 * https://africaninvertebrates.pensoft.net/article/10772/download/pdf/284108
 * Location: /lib/ajax_srv/generate_pdf.php?document_id=10772&readonly_preview=1&skip_metric_check=0&file_id=284108 [following]
 * https://public.pensoft.net/items/?p=7TVeXpoqfNYT89tyrm3ifrTeG9Wv8P676JSQp%2FH2pj9hhtoybol4GF7LEbj3fxHT5Fo8esHstdwAZJFgZxHDbAnCGfkWCPk5a45KiSTKEwfASBIF%2FwVwKbjI5HKmbQ%3D%3D&n=gxNxS8c6dMEeufc3%2Fy68L4LPEOft8f6t4Q%3D%3D
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

	//PDF now has optional terminating file_id number or the (optional?) terminating slash
    public static final String DOWNLOADPDF_URL = "article/[^/]+/download/pdf(/|/[0-9]+)?$";
    //public static final String GENERATE_URL = "/lib/ajax_srv/generate_pdf[.]php[?]document_id=";
    public static final String DESTINATION_URL = "items/\\?p=";

    protected Pattern DOWNLOADPDF_PAT = Pattern.compile(DOWNLOADPDF_URL, Pattern.CASE_INSENSITIVE);
    protected Pattern DESTINATION_PAT = Pattern.compile(DESTINATION_URL, Pattern.CASE_INSENSITIVE);

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
          DESTINATION_PAT.matcher(fud.fetchUrl).find();
      return should;
    }
  }
}
