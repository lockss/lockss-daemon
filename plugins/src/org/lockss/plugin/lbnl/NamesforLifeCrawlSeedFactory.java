package org.lockss.plugin.lbnl;

import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler;

public class NamesforLifeCrawlSeedFactory implements CrawlSeedFactory {

    @Override
    public CrawlSeed createCrawlSeed(Crawler.CrawlerFacade facade) {
        return new NamesforLifeCrawlSeed(facade);
    }

}

