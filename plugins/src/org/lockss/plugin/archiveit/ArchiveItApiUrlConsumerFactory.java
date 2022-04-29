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

package org.lockss.plugin.archiveit;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;

public class ArchiveItApiUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(ArchiveItApiUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new ArchiveItApiUrlConsumer(crawlFacade, fud);
  }

  public class ArchiveItApiUrlConsumer extends HttpToHttpsUrlConsumer {


    public ArchiveItApiUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }

    public String getWarcFromRedirect(String fUrl) {
      String fetchWarc = null;
      // only attempt the parse if we are reasonably sure the redirect url is to a warc file
      if (fUrl.contains("archive.org") &&
          fUrl.contains(".warc") &&
          ( fUrl.contains("archive.org/download/") ||
            fUrl.contains("/items/")
          )) {
        fetchWarc = StringUtils.substringBefore(
            fUrl.substring(
                fUrl.lastIndexOf("/") + 1
            ),
            "?"
        );
      }
      return fetchWarc;
    }

    /**
     * <p>
     * Determines if the URL is to be stored under its redirect chain's origin
     * URL. There are multi-hop redirects for archiveit
     * origUrl:
     * https://warcs.archive-it.org/webdatafile/
     *   ARCHIVEIT-10181-CRAWL_SELECTED_SEEDS-JOB571324-20180504183605271-00000.warc.gz
     * redirects to:
     *  https://archive.org/download/
     *    ARCHIVEIT-10181-CRAWL_SELECTED_SEEDS-JOB571324-20180504-00000/
     *      ARCHIVEIT-10181-CRAWL_SELECTED_SEEDS-JOB571324-20180504183605271-00000.warc.gz?
     *        archiveit-ARCHIVEIT-10181-CRAWL_SELECTED_SEEDS-JOB571324-20180504-00000=
     *          1626366354-94a20fd166eaa8acaffe73bb04918dba
     * redirects to:
     *  https://ia803108.us.archive.org/4/items/
     *   ARCHIVEIT-10181-CRAWL_SELECTED_SEEDS-JOB571324-20180504-00000/
     *      ARCHIVEIT-10181-CRAWL_SELECTED_SEEDS-JOB571324-20180504183605271-00000.warc.gz?
     *        archiveit-ARCHIVEIT-10181-CRAWL_SELECTED_SEEDS-JOB571324-20180504-00000=
     *            1626366354-94a20fd166eaa8acaffe73bb04918dba
     * </p>
     */
    public boolean shouldStoreAtOrigUrl() {
      boolean should = false;
      if (fud.redirectUrls != null
          && fud.redirectUrls.size() >= 1
          // make sure it is a redirect from a warc file
          && fud.origUrl.contains("webdatafile/")
          // make sure this isn't a redirect to login
          && !fud.fetchUrl.contains("/login?")) {
        String origWarc = StringUtils.substringAfter(fud.origUrl, "webdatafile/");
        String fetchWarc = getWarcFromRedirect(fud.fetchUrl);
        if (fetchWarc != null && origWarc != null) {
          should = origWarc.equals(fetchWarc);
        }
      }
      return should;
    }
  }
}
