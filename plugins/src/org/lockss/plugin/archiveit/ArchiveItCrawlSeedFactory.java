package org.lockss.plugin.archiveit;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class ArchiveItCrawlSeedFactory implements CrawlSeedFactory {

  @Override
  public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade facade) {
    return new ArchiveItCrawlSeed(facade);
  }

}
