package org.lockss.plugin;

import org.lockss.daemon.Crawler.CrawlerFacade;

/**
 * Factory used for the creation of a DispatchingUrlFetcher
 */
public class DispatchingUrlFetcherFactory implements UrlFetcherFactory {
  @Override
  public UrlFetcher createUrlFetcher(final CrawlerFacade crawlFacade,
                                     final String url) {
    return new DispatchingUrlFetcher(crawlFacade, url);
  }
}
