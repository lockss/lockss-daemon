package org.lockss.plugin.cloudpublish.liverpool;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcherFactory;
import org.lockss.util.Logger;

public class LupUrlFetcherFactory implements UrlFetcherFactory {
  private static final Logger log = Logger.getLogger(LupUrlFetcherFactory.class);

  @Override
  public UrlFetcher createUrlFetcher(Crawler.CrawlerFacade crawlFacade, String url) {

    return null;
  }
}