package org.lockss.plugin.archiveit;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

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
