package org.lockss.plugin.cloudpublish.liverpool;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class LupUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(LupUrlConsumerFactory.class);

  public static final String READ_ITEM_TYPE = "read/?item_type=journal_article&item_id=";

  @Override
  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new LupUrlConsumer(crawlFacade, fud);
  }
  public class LupUrlConsumer extends HttpToHttpsUrlConsumer {


    public LupUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }

    /**
     * <p>
     * Determines if the URL is to be stored under its redirect chain's origin
     * URL.
     *
     * http://dev-liverpoolup.cloudpublish.co.uk/read/?item_type=journal_article&item_id=20082
     *
     * http://dev-liverpoolup.cloudpublish.co.uk/read/?item_type=journal_article&item_id=20194&mode=download
     *  -->
     *    https://liverpoolup.cloudpublish.co.uk/read/?id=20082&type=journal_article&cref=LUP0923&peref=&drm=soft&acs=1&exit=http%3A%2F%2Fdev-liverpoolup.cloudpublish.co.uk%2Fjournals%2Farticle%2F20082%2F&p=6&uid=LUP&t=1639011561&h=fece739719aad172763a1aa10664d1c2
     * </p>
     *
     */
    public boolean shouldStoreAtOrigUrl() {
      boolean should = false;
      should = fud.origUrl.contains(READ_ITEM_TYPE);;
      if (AuUtil.isBaseUrlHttp(au)
          && fud.redirectUrls != null
          && fud.redirectUrls.size() >= 1
          && UrlUtil.isHttpUrl(fud.origUrl)
          && UrlUtil.isHttpsUrl(fud.fetchUrl)
          && !should) {
        String origBase = StringUtils.substringBefore(UrlUtil.stripProtocol(fud.origUrl),"?");
        String fetchBase = StringUtils.substringBefore(UrlUtil.stripProtocol(fud.fetchUrl),"?");
        should = (
            origBase.equals(fetchBase) ||
                origBase.equals(fetchBase.replaceFirst("/doi/[^/]+/", "/doi/")) ||
                origBase.replaceFirst("/doi/[^/]+/", "/doi/").equals(fetchBase.replaceFirst("/doi/[^/]+/", "/doi/")) ||
                origBase.equals(fetchBase.replace("%2F","/")));
        if (fud.redirectUrls != null) {
          log.debug3("BA redirect " + fud.redirectUrls.size() + ": " + fud.redirectUrls.toString());
          log.debug3("BA redirect: " + " " + fud.origUrl + " to " + fud.fetchUrl + " should consume?: " + should);
        }
      }
      return should;
    }
  }
}
