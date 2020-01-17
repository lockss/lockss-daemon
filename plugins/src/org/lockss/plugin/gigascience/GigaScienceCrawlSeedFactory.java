package org.lockss.plugin.gigascience;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class GigaScienceCrawlSeedFactory implements CrawlSeedFactory {

    @Override
    public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade facade) {
        return new GigaScienceCrawlSeed(facade);
    }

}

